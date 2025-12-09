package org.example.folderservice.service;

import org.example.folderservice.dto.BreadcrumbItem;
import org.example.folderservice.dto.FileDTO;
import org.example.folderservice.dto.FolderDTO;
import org.example.folderservice.dto.UserDTO;
import org.example.folderservice.entity.Folder;
import org.example.folderservice.entity.FolderShare;
import org.example.folderservice.exception.FolderNotFoundException;
import org.example.folderservice.exception.UserNotFoundException;
import org.example.folderservice.feign.FileServiceClient;
import org.example.folderservice.feign.UserServiceClient;
import org.example.folderservice.repository.FolderRepository;
import org.example.folderservice.repository.FolderShareRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
@Slf4j
public class FolderService {

    private final FolderRepository folderRepository;
    private final FolderShareRepository folderShareRepository;
    private final UserServiceClient userServiceClient;
    private final FileServiceClient fileServiceClient;

    public FolderService(FolderRepository folderRepository,
                         FolderShareRepository folderShareRepository,
                         UserServiceClient userServiceClient,
                         FileServiceClient fileServiceClient) {
        this.folderRepository = folderRepository;
        this.folderShareRepository = folderShareRepository;
        this.userServiceClient = userServiceClient;
        this.fileServiceClient = fileServiceClient;
    }

    public FolderDTO createFolder(String name, Long parentId, Long userId, String description, String color) {
        // Verify user exists via user-service
        try {
            userServiceClient.getUserById(userId);
        } catch (Exception e) {
            throw new UserNotFoundException(userId);
        }

        // Check if a folder with the same name already exists
        if (parentId != null) {
            if (folderRepository.findByUserIdAndNameAndParentId(userId, name, parentId).isPresent()) {
                throw new RuntimeException("A folder with this name already exists in this location");
            }
        } else {
            if (folderRepository.findByUserIdAndNameAndParentIsNull(userId, name).isPresent()) {
                throw new RuntimeException("A folder with this name already exists in the root directory");
            }
        }

        Folder folder = new Folder();
        folder.setName(name);
        folder.setUserId(userId);
        
        // Set description and color if provided
        if (description != null && !description.trim().isEmpty()) {
            folder.setDescription(description.trim());
        }
        if (color != null && !color.trim().isEmpty()) {
            folder.setColor(color.trim());
        }

        if (parentId != null) {
            Folder parent = folderRepository.findByIdAndUserId(parentId, userId)
                .orElseThrow(() -> new FolderNotFoundException("Parent folder not found"));
            folder.setParent(parent);
            folder.setPath(parent.getPath() + "/" + name);
        } else {
            folder.setPath("/" + name);
        }

        folder = folderRepository.save(folder);
        return convertToDTO(folder, false);
    }

    public List<FolderDTO> getRootFolders(Long userId) {
        try {
            List<Folder> folders = folderRepository.findByUserIdAndParentIsNullOrderByNameAsc(userId);
            if (folders == null || folders.isEmpty()) {
                return new ArrayList<>();
            }
            
            return folders.stream()
                .map(folder -> {
                    try {
                        return convertToDTO(folder, true);
                    } catch (Exception e) {
                        log.error("Error converting folder to DTO: {}", folder.getId(), e);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Error getting root folders for user: {}", userId, e);
            throw new RuntimeException("Could not retrieve folders. Please try again later.", e);
        }
    }

    public List<FolderDTO> getSubfolders(Long parentId, Long userId) {
        List<Folder> folders = folderRepository.findByUserIdAndParentIdOrderByNameAsc(userId, parentId);
        return folders.stream()
            .map(folder -> convertToDTO(folder, true))
            .collect(Collectors.toList());
    }

    public FolderDTO getFolderDetails(Long folderId, Long userId) {
        Folder folder = folderRepository.findByIdAndUserId(folderId, userId)
            .orElseThrow(() -> new FolderNotFoundException(folderId));
        return convertToDTO(folder, true);
    }

    public FolderDTO updateFolder(Long folderId, String name, String color, String description, Long userId) {
        Folder folder = folderRepository.findByIdAndUserId(folderId, userId)
            .orElseThrow(() -> new FolderNotFoundException(folderId));

        if (name != null && !name.equals(folder.getName())) {
            // Check if a folder with the same name already exists
            if (folder.getParent() != null) {
                if (folderRepository.findByUserIdAndNameAndParentId(userId, name, folder.getParent().getId()).isPresent()) {
                    throw new RuntimeException("A folder with this name already exists in this location");
                }
            } else {
                if (folderRepository.findByUserIdAndNameAndParentIsNull(userId, name).isPresent()) {
                    throw new RuntimeException("A folder with this name already exists in the root directory");
                }
            }
            folder.setName(name);
            updateFolderPath(folder);
        }

        if (color != null) {
            folder.setColor(color);
        }

        if (description != null) {
            folder.setDescription(description);
        }

        folder = folderRepository.save(folder);
        return convertToDTO(folder, true);
    }

    public FolderDTO toggleFavorite(Long folderId, Long userId) {
        Folder folder = folderRepository.findByIdAndUserId(folderId, userId)
            .orElseThrow(() -> new FolderNotFoundException(folderId));

        folder.setIsFavorite(!folder.getIsFavorite());
        folder = folderRepository.save(folder);
        return convertToDTO(folder, false);
    }

    @Transactional
    public void deleteFolder(Long folderId, Long userId) {
        Folder folder = folderRepository.findByIdAndUserId(folderId, userId)
            .orElseThrow(() -> new FolderNotFoundException(folderId));

        // Delete all folder shares first
        cleanupFolderShares(folder);
        
        // Delete all files in the folder via file-service
        deleteFolderContentsRecursively(folder, userId);
        
        // Now delete the folder itself
        folderRepository.delete(folder);
    }
    
    private void cleanupFolderShares(Folder folder) {
        // Delete all shares for this folder
        List<FolderShare> folderShares = folderShareRepository.findByFolderId(folder.getId());
        if (!folderShares.isEmpty()) {
            folderShareRepository.deleteAll(folderShares);
            log.info("Deleted {} folder shares for folder {}", folderShares.size(), folder.getId());
        }
        
        // Recursively clean up shares for subfolders
        for (Folder subfolder : folder.getSubfolders()) {
            cleanupFolderShares(subfolder);
        }
    }
    
    private void deleteFolderContentsRecursively(Folder folder, Long userId) {
        // Delete all files in this folder via file-service
        try {
            fileServiceClient.deleteFilesByFolderId(folder.getId(), userId);
        } catch (Exception e) {
            log.warn("Could not delete files for folder {}: {}", folder.getId(), e.getMessage());
        }
        
        // Recursively delete all subfolders
        if (!folder.getSubfolders().isEmpty()) {
            for (Folder subfolder : new ArrayList<>(folder.getSubfolders())) {
                cleanupFolderShares(subfolder);
                deleteFolderContentsRecursively(subfolder, userId);
                folderRepository.delete(subfolder);
            }
        }
    }

    public List<FolderDTO> getFavoriteFolders(Long userId) {
        List<Folder> folders = folderRepository.findFavoriteFoldersByUserId(userId);
        return folders.stream()
            .map(folder -> convertToDTO(folder, true))
            .collect(Collectors.toList());
    }

    public List<FolderDTO> searchFolders(String query, Long userId) {
        List<Folder> folders = folderRepository.searchFoldersByName(userId, query);
        return folders.stream()
            .map(folder -> convertToDTO(folder, true))
            .collect(Collectors.toList());
    }

    /**
     * Move a folder to a new parent folder
     */
    public FolderDTO moveFolder(Long folderId, Long newParentId, Long userId) {
        Folder folder = folderRepository.findByIdAndUserId(folderId, userId)
            .orElseThrow(() -> new FolderNotFoundException(folderId));

        // Prevent moving a folder into itself or its descendants
        if (newParentId != null && isDescendant(folder, newParentId)) {
            throw new RuntimeException("Cannot move folder into itself or its descendants");
        }

        // Check if a folder with the same name already exists in the new location
        if (newParentId != null) {
            if (folderRepository.findByUserIdAndNameAndParentId(userId, folder.getName(), newParentId).isPresent()) {
                throw new RuntimeException("A folder with this name already exists in the destination");
            }
            
            Folder newParent = folderRepository.findByIdAndUserId(newParentId, userId)
                .orElseThrow(() -> new FolderNotFoundException("Destination folder not found"));
            folder.setParent(newParent);
        } else {
            // Moving to root
            if (folderRepository.findByUserIdAndNameAndParentIsNull(userId, folder.getName()).isPresent()) {
                throw new RuntimeException("A folder with this name already exists in the root directory");
            }
            folder.setParent(null);
        }

        // Update paths for this folder and all its descendants
        updateFolderPath(folder);
        
        folder = folderRepository.save(folder);
        log.info("Moved folder '{}' (ID: {}) to new parent (ID: {})", folder.getName(), folderId, newParentId);
        
        return convertToDTO(folder, true);
    }

    /**
     * Copy/Duplicate a folder with all its contents
     */
    @Transactional
    public FolderDTO copyFolder(Long folderId, Long newParentId, String newName, Long userId) {
        Folder originalFolder = folderRepository.findByIdAndUserId(folderId, userId)
            .orElseThrow(() -> new FolderNotFoundException(folderId));

        // Use original name if no new name provided
        String finalName = (newName != null && !newName.trim().isEmpty()) ? newName.trim() : originalFolder.getName();
        
        // Check if a folder with the target name already exists in the destination
        if (newParentId != null) {
            if (folderRepository.findByUserIdAndNameAndParentId(userId, finalName, newParentId).isPresent()) {
                throw new RuntimeException("A folder with this name already exists in the destination");
            }
        } else {
            if (folderRepository.findByUserIdAndNameAndParentIsNull(userId, finalName).isPresent()) {
                throw new RuntimeException("A folder with this name already exists in the root directory");
            }
        }

        try {
            // Create the copy (database operations only)
            Folder copiedFolder = copyFolderStructureRecursively(originalFolder, newParentId, finalName, userId);
            
            // Copy files via file-service
            copyAllFilesInFolderHierarchy(originalFolder, copiedFolder, userId);
            
            log.info("Successfully copied folder '{}' (ID: {}) to '{}' (ID: {})", 
                    originalFolder.getName(), folderId, copiedFolder.getName(), copiedFolder.getId());
            
            return convertToDTO(copiedFolder, true);
        } catch (Exception e) {
            log.error("Failed to copy folder '{}': {}", originalFolder.getName(), e.getMessage(), e);
            throw new RuntimeException("Failed to copy folder: " + e.getMessage());
        }
    }

    /**
     * Check if a folder is a descendant of another folder
     */
    private boolean isDescendant(Folder folder, Long potentialAncestorId) {
        if (folder.getId().equals(potentialAncestorId)) {
            return true;
        }
        
        for (Folder child : folder.getSubfolders()) {
            if (isDescendant(child, potentialAncestorId)) {
                return true;
            }
        }
        
        return false;
    }

    /**
     * Copy folder structure (database operations only) - no file copying
     */
    private Folder copyFolderStructureRecursively(Folder original, Long newParentId, String newName, Long userId) {
        Folder copy = new Folder();
        copy.setName(newName);
        copy.setUserId(userId);
        copy.setColor(original.getColor());
        copy.setDescription(original.getDescription() != null ? 
            original.getDescription() + " (Copy)" : "Copy of " + original.getName());
        copy.setIsFavorite(false); // Copies are not favorites by default

        // Set parent relationship
        if (newParentId != null) {
            Folder newParent = folderRepository.findByIdAndUserId(newParentId, userId)
                .orElseThrow(() -> new FolderNotFoundException("Destination folder not found"));
            copy.setParent(newParent);
            copy.setPath(newParent.getPath() + "/" + newName);
        } else {
            copy.setPath("/" + newName);
        }

        // Save the folder first to get an ID
        copy = folderRepository.save(copy);

        // Copy subfolders recursively (database operations only)
        for (Folder subfolder : original.getSubfolders()) {
            copyFolderStructureRecursively(subfolder, copy.getId(), subfolder.getName(), userId);
        }
        
        return copy;
    }

    /**
     * Copy all files in folder hierarchy via file-service
     */
    private void copyAllFilesInFolderHierarchy(Folder originalFolder, Folder copiedFolder, Long userId) {
        // Copy files in the current folder via file-service
        try {
            List<FileDTO> files = fileServiceClient.getFilesByFolderId(originalFolder.getId(), userId);
            for (FileDTO file : files) {
                try {
                    fileServiceClient.copyFile(file.getId(), copiedFolder.getId(), userId);
                } catch (Exception e) {
                    log.warn("Failed to copy file {} to folder {}: {}", file.getId(), copiedFolder.getId(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Could not get files for folder {}: {}", originalFolder.getId(), e.getMessage());
        }
        
        // Copy files in subfolders recursively
        List<Folder> originalSubfolders = new ArrayList<>(originalFolder.getSubfolders());
        List<Folder> copiedSubfolders = new ArrayList<>(copiedFolder.getSubfolders());
        
        // Match original subfolders with copied subfolders by name
        for (int i = 0; i < originalSubfolders.size() && i < copiedSubfolders.size(); i++) {
            copyAllFilesInFolderHierarchy(originalSubfolders.get(i), copiedSubfolders.get(i), userId);
        }
    }

    private void updateFolderPath(Folder folder) {
        String newPath = folder.getParent() != null 
            ? folder.getParent().getPath() + "/" + folder.getName()
            : "/" + folder.getName();
        folder.setPath(newPath);

        // Recursively update paths of subfolders
        for (Folder subfolder : folder.getSubfolders()) {
            updateFolderPath(subfolder);
        }
    }

    private FolderDTO convertToDTO(Folder folder, boolean includeChildren) {
        if (folder == null) {
            return null;
        }

        FolderDTO dto = new FolderDTO();
        dto.setId(folder.getId());
        dto.setName(folder.getName());
        dto.setPath(folder.getPath());
        dto.setFullPath(folder.getFullPath());
        dto.setIsFavorite(folder.getIsFavorite());
        dto.setColor(folder.getColor());
        dto.setDescription(folder.getDescription());
        dto.setCreatedAt(folder.getCreatedAt());
        dto.setUpdatedAt(folder.getUpdatedAt());
        dto.setUserId(folder.getUserId());

        // Parent info
        if (folder.getParent() != null) {
            dto.setParentId(folder.getParent().getId());
            dto.setParentName(folder.getParent().getName());
        }

        // Statistics - get file count and size from file-service
        try {
            Integer fileCount = fileServiceClient.getFileCountByFolderId(folder.getId(), folder.getUserId());
            dto.setFileCount(fileCount != null ? fileCount : 0);
            
            Long totalSize = fileServiceClient.getTotalSizeByFolderId(folder.getId(), folder.getUserId());
            dto.setTotalSize(totalSize != null ? totalSize : 0L);
            dto.setFormattedSize(formatFileSize(dto.getTotalSize()));
        } catch (Exception e) {
            log.debug("Could not fetch file statistics for folder {}: {}", folder.getId(), e.getMessage());
            dto.setFileCount(0);
            dto.setTotalSize(0L);
            dto.setFormattedSize("0 B");
        }
        
        dto.setSubfolderCount(folder.getSubfolderCount());

        // Load children if requested
        if (includeChildren) {
            // Subfolders (one level only)
            if (folder.getSubfolders() != null && !folder.getSubfolders().isEmpty()) {
                dto.setSubfolders(folder.getSubfolders().stream()
                    .map(subfolder -> {
                        FolderDTO subDto = new FolderDTO();
                        subDto.setId(subfolder.getId());
                        subDto.setName(subfolder.getName());
                        subDto.setPath(subfolder.getPath());
                        subDto.setIsFavorite(subfolder.getIsFavorite());
                        subDto.setColor(subfolder.getColor());
                        subDto.setSubfolderCount(subfolder.getSubfolderCount());
                        return subDto;
                    })
                    .collect(Collectors.toList()));
            }

            // Files (from file-service)
            try {
                List<FileDTO> files = fileServiceClient.getFilesByFolderId(folder.getId(), folder.getUserId());
                dto.setFiles(files != null ? files : new ArrayList<>());
            } catch (Exception e) {
                log.debug("Could not fetch files for folder {}: {}", folder.getId(), e.getMessage());
                dto.setFiles(new ArrayList<>());
            }
        }

        // Breadcrumb
        if (folder.getParent() != null) {
            dto.setBreadcrumb(buildBreadcrumb(folder));
        }

        return dto;
    }

    private List<BreadcrumbItem> buildBreadcrumb(Folder folder) {
        List<BreadcrumbItem> breadcrumb = new ArrayList<>();
        Folder current = folder;
        while (current != null) {
            breadcrumb.add(0, new BreadcrumbItem(current.getId(), current.getName()));
            current = current.getParent();
        }
        return breadcrumb;
    }

    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        int exp = (int) (Math.log(size) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", size / Math.pow(1024, exp), pre);
    }

    // =========================
    // BULK OPERATIONS
    // =========================

    /**
     * Bulk move multiple folders to a new parent folder
     */
    @Transactional
    public List<FolderDTO> bulkMoveFolder(List<Long> folderIds, Long newParentId, Long userId) {
        if (folderIds == null || folderIds.isEmpty()) {
            throw new RuntimeException("No folders specified for bulk move");
        }

        // Validate all folders exist and belong to user
        List<Folder> foldersToMove = new ArrayList<>();
        for (Long folderId : folderIds) {
            Folder folder = folderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new FolderNotFoundException(folderId));
            foldersToMove.add(folder);
        }

        // Validate target parent (if not null)
        Folder newParent = null;
        if (newParentId != null) {
            newParent = folderRepository.findByIdAndUserId(newParentId, userId)
                .orElseThrow(() -> new FolderNotFoundException("Target folder not found"));
        }

        // Validate operations - prevent circular references and conflicts
        for (Folder folder : foldersToMove) {
            validateMoveOperation(folder, newParent, userId);
        }

        // Perform bulk move
        List<FolderDTO> movedFolders = new ArrayList<>();
        for (Folder folder : foldersToMove) {
            folder.setParent(newParent);
            updateFolderPath(folder);
            Folder savedFolder = folderRepository.save(folder);
            movedFolders.add(convertToDTO(savedFolder, false));
        }

        return movedFolders;
    }

    /**
     * Bulk copy multiple folders to a new parent folder
     */
    @Transactional
    public List<FolderDTO> bulkCopyFolder(List<Long> folderIds, Long newParentId, Long userId) {
        if (folderIds == null || folderIds.isEmpty()) {
            throw new RuntimeException("No folders specified for bulk copy");
        }

        // Validate all folders exist and belong to user
        List<Folder> foldersToCopy = new ArrayList<>();
        for (Long folderId : folderIds) {
            Folder folder = folderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new FolderNotFoundException(folderId));
            foldersToCopy.add(folder);
        }

        // Validate target parent (if not null)
        Folder newParent = null;
        if (newParentId != null) {
            newParent = folderRepository.findByIdAndUserId(newParentId, userId)
                .orElseThrow(() -> new FolderNotFoundException("Target folder not found"));
        }

        // Perform bulk copy
        List<FolderDTO> copiedFolders = new ArrayList<>();
            
        for (Folder folder : foldersToCopy) {
            // Generate unique name for copy
            String copyName = generateCopyName(folder.getName(), newParent, userId);
            
            // Create copy using existing method
            Long newParentIdForCopy = newParent != null ? newParent.getId() : null;
            Folder copiedFolder = copyFolderStructureRecursively(folder, newParentIdForCopy, copyName, userId);
            
            // Copy files via file-service
            copyAllFilesInFolderHierarchy(folder, copiedFolder, userId);
            
            copiedFolders.add(convertToDTO(copiedFolder, false));
        }

        return copiedFolders;
    }

    /**
     * Bulk delete multiple folders
     */
    @Transactional
    public int bulkDeleteFolder(List<Long> folderIds, Long userId) {
        if (folderIds == null || folderIds.isEmpty()) {
            throw new RuntimeException("No folders specified for bulk delete");
        }

        // Validate all folders exist and belong to user
        List<Folder> foldersToDelete = new ArrayList<>();
        for (Long folderId : folderIds) {
            Folder folder = folderRepository.findByIdAndUserId(folderId, userId)
                .orElseThrow(() -> new FolderNotFoundException(folderId));
            foldersToDelete.add(folder);
        }

        // Perform bulk delete
        int deletedCount = 0;
        for (Folder folder : foldersToDelete) {
            try {
                // Clean up folder shares first
                cleanupFolderShares(folder);
                
                // Delete folder contents recursively
                deleteFolderContentsRecursively(folder, userId);
                
                // Delete the folder itself
                folderRepository.delete(folder);
                deletedCount++;
            } catch (Exception e) {
                log.error("Failed to delete folder {}: {}", folder.getId(), e.getMessage());
            }
        }

        return deletedCount;
    }

    /**
     * Helper method to validate move operations
     */
    private void validateMoveOperation(Folder folder, Folder newParent, Long userId) {
        // Check if trying to move folder into itself
        if (newParent != null && folder.getId().equals(newParent.getId())) {
            throw new RuntimeException("Cannot move folder into itself");
        }

        // Check if trying to move folder into its descendant
        if (newParent != null && isDescendant(folder, newParent.getId())) {
            throw new RuntimeException("Cannot move folder into its descendant");
        }

        // Check for name conflicts
        String folderName = folder.getName();
        if (newParent != null) {
            if (folderRepository.findByUserIdAndNameAndParentId(userId, folderName, newParent.getId()).isPresent()) {
                throw new RuntimeException("A folder with name '" + folderName + "' already exists in the target location");
            }
        } else {
            if (folderRepository.findByUserIdAndNameAndParentIsNull(userId, folderName).isPresent()) {
                throw new RuntimeException("A folder with name '" + folderName + "' already exists in the root directory");
            }
        }
    }

    /**
     * Helper method to generate unique copy names
     */
    private String generateCopyName(String originalName, Folder parent, Long userId) {
        String baseName = originalName;
        String copyName = baseName + " - Copy";
        int counter = 1;

        // Keep trying until we find a unique name
        while (nameExistsInLocation(copyName, parent, userId)) {
            counter++;
            copyName = baseName + " - Copy (" + counter + ")";
        }

        return copyName;
    }

    /**
     * Helper method to check if name exists in location
     */
    private boolean nameExistsInLocation(String name, Folder parent, Long userId) {
        if (parent != null) {
            return folderRepository.findByUserIdAndNameAndParentId(userId, name, parent.getId()).isPresent();
        } else {
            return folderRepository.findByUserIdAndNameAndParentIsNull(userId, name).isPresent();
        }
    }
}
