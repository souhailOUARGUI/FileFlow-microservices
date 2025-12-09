package org.example.fileservice.service;

import org.example.fileservice.dto.FileDTO;
import org.example.fileservice.dto.ShareNotificationDTO;
import org.example.fileservice.dto.UserDTO;
import org.example.fileservice.entity.File;
import org.example.fileservice.entity.FileShare;
import org.example.fileservice.exception.FileNotFoundException;
import org.example.fileservice.exception.ShareFileException;
import org.example.fileservice.exception.UserNotFoundException;
import org.example.fileservice.feign.UserServiceClient;
import org.example.fileservice.repository.FileRepository;
import org.example.fileservice.repository.FileShareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileShareService {
    
    private final FileService fileService;
    private final UserServiceClient userServiceClient;
    private final FileRepository fileRepository;
    private final FileShareRepository fileShareRepository;

    public ShareNotificationDTO shareFileWithUser(Long fileId, String userEmail) {
        // Get target user from User Service
        UserDTO targetUser = userServiceClient.getUserByEmail(userEmail);
        if (targetUser == null) {
            throw new UserNotFoundException("User not found with email: " + userEmail);
        }
        
        File fileToShare = fileRepository.findById(fileId)
            .orElseThrow(() -> new FileNotFoundException("File not found"));

        // Check if already shared
        if (fileShareRepository.existsByFileIdAndTargetUserId(fileId, targetUser.getId())) {
            throw new RuntimeException("File is already shared with this user");
        }

        FileShare fileShare = FileShare.builder()
            .file(fileToShare)
            .targetUserId(targetUser.getId())
            .response(false)
            .shareType("direct")
            .build();
        
        fileShareRepository.save(fileShare);
        return convertToNotificationDTO(fileShare, targetUser);
    }

    public void unshareFile(Long fileId, String userEmail) {
        UserDTO user = userServiceClient.getUserByEmail(userEmail);
        if (user == null) {
            throw new UserNotFoundException("User not found with email: " + userEmail);
        }
        
        File file = fileRepository.findByUserIdAndOriginalFileId(user.getId(), fileId)
            .orElse(null);

        FileShare fileShare = fileShareRepository.findByFileIdAndTargetUserId(fileId, user.getId())
            .orElseThrow(() -> new RuntimeException("Share not found"));

        fileShareRepository.delete(fileShare);
        
        if (file != null) {
            fileRepository.delete(file);
        }
    }

    public FileDTO shareResponse(Long shareFileId, boolean response) {
        FileShare fileShare = fileShareRepository.findById(shareFileId)
            .orElseThrow(() -> new ShareFileException("Share not found"));
        
        if (response) {
            fileShare.setResponse(true);
            File originalFile = fileShare.getFile();
            
            // Create a copy for the target user
            File file = originalFile.toBuilder()
                .id(null)
                .isShared(true)
                .originalFile(originalFile)
                .fileCopies(null)
                .fileShares(null)
                .isFavorite(false)
                .folderId(null)
                .userId(fileShare.getTargetUserId())
                .build();
            
            fileShareRepository.save(fileShare);
            File savedFile = fileRepository.save(file);
            return fileService.convertToDTO(savedFile);
        } else {
            fileShareRepository.delete(fileShare);
        }
        return null;
    }

    public List<ShareNotificationDTO> getShareRequests(Long userId) {
        List<FileShare> fileShares = fileShareRepository.findFileSharesByTargetUserId(userId);
        List<ShareNotificationDTO> shareNotifications = new ArrayList<>();
        
        for (FileShare fs : fileShares) {
            // Get owner info from User Service
            UserDTO owner = userServiceClient.getUserById(fs.getFile().getUserId());
            shareNotifications.add(convertToNotificationDTO(fs, owner, true));
        }
        
        return shareNotifications;
    }

    public List<FileDTO> getSharedFilesWithMe(Long userId) {
        List<File> files = fileShareRepository.findByTargetId(userId);
        List<FileDTO> fileDtos = new ArrayList<>();
        for (File file : files) {
            fileDtos.add(fileService.convertToDTO(file));
        }
        return fileDtos;
    }

    public List<FileDTO> getSharedFilesByMe(Long userId) {
        List<File> files = fileShareRepository.findSharedFilesByMe(userId);
        List<FileDTO> fileDtos = new ArrayList<>();
        for (File file : files) {
            fileDtos.add(fileService.convertToDTO(file));
        }
        return fileDtos;
    }

    public List<Long> getUserIdsWhoShareMyFile(Long fileId) {
        return fileShareRepository.getSharedUserIdsByFileId(fileId);
    }

    private ShareNotificationDTO convertToNotificationDTO(FileShare fileShare, UserDTO targetUser) {
        File file = fileShare.getFile();
        UserDTO owner = userServiceClient.getUserById(file.getUserId());
        
        ShareNotificationDTO dto = new ShareNotificationDTO();
        dto.setId(fileShare.getId());
        dto.setFileName(file.getOriginalFileName());
        dto.setUserId(targetUser.getId());
        dto.setOwner(owner.getFirstName() + " " + owner.getLastName());
        dto.setFileId(file.getId());
        return dto;
    }
    
    private ShareNotificationDTO convertToNotificationDTO(FileShare fileShare, UserDTO owner, boolean isOwner) {
        File file = fileShare.getFile();
        
        ShareNotificationDTO dto = new ShareNotificationDTO();
        dto.setId(fileShare.getId());
        dto.setFileName(file.getOriginalFileName());
        dto.setUserId(fileShare.getTargetUserId());
        dto.setOwner(owner.getFirstName() + " " + owner.getLastName());
        dto.setFileId(file.getId());
        return dto;
    }
}
