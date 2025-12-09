package org.example.folderservice.service;

import org.example.folderservice.dto.FolderShareRequest;
import org.example.folderservice.dto.FolderShareDTO;
import org.example.folderservice.dto.UserDTO;
import org.example.folderservice.entity.Folder;
import org.example.folderservice.entity.FolderShare;
import org.example.folderservice.exception.FolderNotFoundException;
import org.example.folderservice.exception.ForbiddenException;
import org.example.folderservice.exception.UserNotFoundException;
import org.example.folderservice.feign.UserServiceClient;
import org.example.folderservice.repository.FolderRepository;
import org.example.folderservice.repository.FolderShareRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class FolderShareService {

    private final FolderShareRepository folderShareRepository;
    private final FolderRepository folderRepository;
    private final UserServiceClient userServiceClient;
    private final PasswordEncoder passwordEncoder;
    // Note: In microservices, WebSocket notifications should use a message broker
    // private final SimpMessagingTemplate messagingTemplate;

    public FolderShareService(FolderShareRepository folderShareRepository, 
                             FolderRepository folderRepository,
                             UserServiceClient userServiceClient,
                             PasswordEncoder passwordEncoder) {
        this.folderShareRepository = folderShareRepository;
        this.folderRepository = folderRepository;
        this.userServiceClient = userServiceClient;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Share a folder with another user
     */
    public FolderShareDTO shareFolder(Long folderId, Long ownerId, FolderShareRequest request) {
        // Validate folder ownership
        Folder folder = folderRepository.findByIdAndUserId(folderId, ownerId)
            .orElseThrow(() -> new FolderNotFoundException("Folder not found or access denied"));

        // Find target user by email via user-service
        UserDTO targetUser;
        try {
            targetUser = userServiceClient.getUserByEmail(request.getTargetUserEmail());
        } catch (Exception e) {
            throw new UserNotFoundException("User with email " + request.getTargetUserEmail() + " not found");
        }

        // Get owner info
        UserDTO owner;
        try {
            owner = userServiceClient.getUserById(ownerId);
        } catch (Exception e) {
            throw new UserNotFoundException("Owner not found");
        }

        // Check if folder is already shared with this user
        if (folderShareRepository.findByFolderIdAndTargetUserId(folderId, targetUser.getId()).isPresent()) {
            throw new RuntimeException("Folder is already shared with this user");
        }

        // Prevent self-sharing
        if (ownerId.equals(targetUser.getId())) {
            throw new RuntimeException("Cannot share folder with yourself");
        }

        // Create folder share
        FolderShare folderShare = new FolderShare();
        folderShare.setFolder(folder);
        folderShare.setOwnerId(ownerId);
        folderShare.setTargetUserId(targetUser.getId());
        folderShare.setPermissions(request.getPermissions() != null ? request.getPermissions() : "read");
        folderShare.setMessage(request.getMessage());
        folderShare.setExpiresAt(request.getExpiresAt());
        folderShare.setRequiresApproval(request.isRequiresApproval());

        // Handle password protection
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            folderShare.setPasswordHash(passwordEncoder.encode(request.getPassword()));
            folderShare.setRequiresPassword(true);
        }

        folderShare = folderShareRepository.save(folderShare);
        
        // TODO: In microservices, send notification via message broker (e.g., Kafka, RabbitMQ)
        // Example: Send to notification-service topic
        /*
        Map<String, Object> notification = new HashMap<>();
        notification.put("id", folderShare.getId());
        notification.put("owner", owner.getEmail());
        notification.put("folderName", folder.getName());
        notification.put("type", "folder");
        notification.put("message", request.getMessage());
        notification.put("permissions", folderShare.getPermissions());
        notification.put("targetUserId", targetUser.getId());
        kafkaTemplate.send("share-notifications", notification);
        */
        
        log.info("Folder '{}' (ID: {}) shared with user '{}' by '{}'", 
                folder.getName(), folderId, targetUser.getEmail(), owner.getEmail());

        return convertToDTO(folderShare, owner.getEmail(), targetUser.getEmail());
    }

    /**
     * Get all pending folder share requests for a user
     */
    public List<FolderShareDTO> getPendingSharesForUser(Long userId) {
        List<FolderShare> pendingShares = folderShareRepository.findPendingSharesForUser(userId);
        return pendingShares.stream()
            .map(share -> convertToDTOWithUserLookup(share))
            .collect(Collectors.toList());
    }

    /**
     * Get all folder shares created by a user
     */
    public List<FolderShareDTO> getSharesCreatedByUser(Long userId) {
        List<FolderShare> shares = folderShareRepository.findSharesCreatedByUser(userId);
        return shares.stream()
            .map(share -> convertToDTOWithUserLookup(share))
            .collect(Collectors.toList());
    }

    /**
     * Get all shares for a specific folder
     */
    public List<FolderShareDTO> getFolderShares(Long folderId, Long ownerId) {
        // Validate folder ownership
        folderRepository.findByIdAndUserId(folderId, ownerId)
            .orElseThrow(() -> new FolderNotFoundException("Folder not found or access denied"));

        List<FolderShare> shares = folderShareRepository.findByFolderId(folderId);
        return shares.stream()
            .map(share -> convertToDTOWithUserLookup(share))
            .collect(Collectors.toList());
    }

    /**
     * Respond to a folder share request (accept/reject)
     */
    public FolderShareDTO respondToShare(Long shareId, Long userId, boolean accept) {
        FolderShare folderShare = folderShareRepository.findById(shareId)
            .orElseThrow(() -> new RuntimeException("Share request not found"));

        // Validate that the user is the target of this share
        if (!folderShare.getTargetUserId().equals(userId)) {
            throw new ForbiddenException("Access denied - not authorized to respond to this share");
        }

        // Update share status
        folderShare.setStatus(accept ? "accepted" : "rejected");
        folderShare.setRespondedAt(LocalDateTime.now());
        
        folderShare = folderShareRepository.save(folderShare);

        log.info("Folder share request {} by user {}", 
                accept ? "accepted" : "rejected", userId);

        return convertToDTOWithUserLookup(folderShare);
    }

    /**
     * Revoke a folder share
     */
    public void revokeShare(Long shareId, Long ownerId) {
        FolderShare folderShare = folderShareRepository.findById(shareId)
            .orElseThrow(() -> new RuntimeException("Share not found"));

        // Validate that the user is the owner of the shared folder
        if (!folderShare.getOwnerId().equals(ownerId)) {
            throw new ForbiddenException("Access denied - only the owner can revoke shares");
        }

        folderShare.setStatus("revoked");
        folderShareRepository.save(folderShare);

        log.info("Folder share revoked by owner for folder: {}", folderShare.getFolder().getName());
    }

    /**
     * Get shared folders that a user has access to
     */
    public List<FolderShareDTO> getSharedFoldersForUser(Long userId) {
        List<FolderShare> acceptedShares = folderShareRepository.findByTargetUserId(userId)
            .stream()
            .filter(share -> "accepted".equals(share.getStatus()))
            .collect(Collectors.toList());

        return acceptedShares.stream()
            .map(share -> convertToDTOWithUserLookup(share))
            .collect(Collectors.toList());
    }

    /**
     * Remove a user from folder sharing
     */
    public void removeUserFromFolder(Long folderId, Long ownerId, String targetUserEmail) {
        // Validate folder ownership
        folderRepository.findByIdAndUserId(folderId, ownerId)
            .orElseThrow(() -> new FolderNotFoundException("Folder not found or access denied"));

        UserDTO targetUser;
        try {
            targetUser = userServiceClient.getUserByEmail(targetUserEmail);
        } catch (Exception e) {
            throw new UserNotFoundException("User not found");
        }

        folderShareRepository.deleteByFolderIdAndTargetUserId(folderId, targetUser.getId());

        log.info("User '{}' removed from folder sharing for folder ID: {}", targetUserEmail, folderId);
    }

    /**
     * Check if user has access to a folder
     */
    public boolean hasAccessToFolder(Long folderId, Long userId) {
        // Check if user owns the folder
        if (folderRepository.findByIdAndUserId(folderId, userId).isPresent()) {
            return true;
        }

        // Check if user has accepted share access
        return folderShareRepository.findByFolderIdAndTargetUserId(folderId, userId)
            .filter(share -> "accepted".equals(share.getStatus()))
            .isPresent();
    }

    /**
     * Get user's permission level for a folder
     */
    public String getUserPermissionForFolder(Long folderId, Long userId) {
        // Check if user owns the folder
        if (folderRepository.findByIdAndUserId(folderId, userId).isPresent()) {
            return "admin";
        }

        // Check shared permissions
        return folderShareRepository.findByFolderIdAndTargetUserId(folderId, userId)
            .filter(share -> "accepted".equals(share.getStatus()))
            .map(FolderShare::getPermissions)
            .orElse("none");
    }

    /**
     * Convert FolderShare entity to DTO with user lookup
     */
    private FolderShareDTO convertToDTOWithUserLookup(FolderShare folderShare) {
        String ownerEmail = "unknown";
        String targetUserEmail = "unknown";
        
        try {
            UserDTO owner = userServiceClient.getUserById(folderShare.getOwnerId());
            ownerEmail = owner.getEmail();
        } catch (Exception e) {
            log.warn("Could not fetch owner email for share {}", folderShare.getId());
        }
        
        try {
            UserDTO targetUser = userServiceClient.getUserById(folderShare.getTargetUserId());
            targetUserEmail = targetUser.getEmail();
        } catch (Exception e) {
            log.warn("Could not fetch target user email for share {}", folderShare.getId());
        }
        
        return convertToDTO(folderShare, ownerEmail, targetUserEmail);
    }

    /**
     * Convert FolderShare entity to DTO
     */
    private FolderShareDTO convertToDTO(FolderShare folderShare, String ownerEmail, String targetUserEmail) {
        FolderShareDTO dto = new FolderShareDTO();
        dto.setId(folderShare.getId());
        dto.setFolderId(folderShare.getFolder().getId());
        dto.setFolderName(folderShare.getFolder().getName());
        dto.setOwnerEmail(ownerEmail);
        dto.setTargetUserEmail(targetUserEmail);
        dto.setPermissions(folderShare.getPermissions());
        dto.setMessage(folderShare.getMessage());
        dto.setSharedAt(folderShare.getSharedAt());
        dto.setExpiresAt(folderShare.getExpiresAt());
        dto.setStatus(folderShare.getStatus());
        dto.setRequiresPassword(folderShare.isRequiresPassword());
        return dto;
    }
}
