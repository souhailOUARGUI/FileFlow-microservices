package org.example.fileservice.service;

import org.example.fileservice.config.FileStorageConfig;
import org.example.fileservice.dto.FileDTO;
import org.example.fileservice.dto.UserDTO;
import org.example.fileservice.entity.File;
import org.example.fileservice.exception.FileNotFoundException;
import org.example.fileservice.exception.ForbiddenException;
import org.example.fileservice.exception.StorageQuotaExceededException;
import org.example.fileservice.feign.UserServiceClient;
import org.example.fileservice.repository.FileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    private final FileRepository fileRepository;
    private final UserServiceClient userServiceClient;
    private final FileStorageConfig fileStorageConfig;

    @Value("${file.max-size}")
    private Long maxFileSize;

    public FileDTO uploadFile(MultipartFile file, Long userId, Long folderId) {
        // Validate file
        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("File size exceeds maximum allowed size");
        }

        // Check storage quota via User Service
        Boolean hasSpace = userServiceClient.hasStorageSpace(userId, file.getSize());
        if (hasSpace == null || !hasSpace) {
            throw new StorageQuotaExceededException("Storage quota exceeded");
        }

        try {
            // Create user directory if it doesn't exist
            Path userDir = Paths.get(fileStorageConfig.getUploadDir(), userId.toString());
            if (!Files.exists(userDir)) {
                Files.createDirectories(userDir);
            }

            // Generate unique filename
            String fileUuid = UUID.randomUUID().toString();
            String originalFileName = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFileName != null && originalFileName.contains(".")) {
                fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
            }
            String fileName = fileUuid + fileExtension;

            // Save file to disk
            Path filePath = userDir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Save metadata to database
            File fileMetadata = new File();
            fileMetadata.setFileName(fileName);
            fileMetadata.setOriginalFileName(originalFileName);
            fileMetadata.setFilePath(filePath.toString());
            fileMetadata.setContentType(file.getContentType());
            fileMetadata.setFileSize(file.getSize());
            fileMetadata.setFileUuid(fileUuid);
            fileMetadata.setUserId(userId);
            fileMetadata.setFolderId(folderId);

            File savedFile = fileRepository.save(fileMetadata);

            // Update user storage via User Service
            userServiceClient.updateStorageUsed(userId, file.getSize());

            return convertToDTO(savedFile);

        } catch (IOException e) {
            throw new RuntimeException("Failed to store file", e);
        }
    }

    public FileDTO uploadFile(MultipartFile file, Long userId) {
        return uploadFile(file, userId, null);
    }

    public List<FileDTO> getUserFiles(Long userId) {
        List<File> files = fileRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return files.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public List<FileDTO> getFilesByOriginalFileName(Long userId, String name) {
        List<File> files = fileRepository.findByUserIdAndOriginalFileNameContainingIgnoreCase(userId, name);
        return files.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public Page<FileDTO> getUserFiles(Long userId, Pageable pageable) {
        Page<File> files = fileRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return files.map(this::convertToDTO);
    }

    public FileDTO getFileDetails(Long fileId, Long userId) {
        File file = fileRepository.findByIdAndUserId(fileId, userId)
            .orElseThrow(() -> new FileNotFoundException("File not found"));
        return convertToDTO(file);
    }

    public Resource downloadFile(Long fileId, Long userId) {
        File file = fileRepository.findByIdAndUserId(fileId, userId)
            .orElseThrow(() -> new FileNotFoundException("File not found"));

        try {
            Path filePath = Paths.get(file.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new FileNotFoundException("File not found on disk");
            }
        } catch (MalformedURLException e) {
            throw new FileNotFoundException("File not found");
        }
    }

    public FileDTO renameFile(Long fileId, Long userId, String newName) {
        File file = fileRepository.findByIdAndUserId(fileId, userId)
            .orElseThrow(() -> new FileNotFoundException("File not found"));
        
        if (file.isShared()) {
            throw new ForbiddenException("You are not allowed to edit a shared file");
        }
        
        // Check if new name already exists for this user
        if (fileRepository.existsByFileNameAndUserId(newName, userId)) {
            throw new RuntimeException("File with this name already exists");
        }
        
        renameFileAndCopies(file, newName);
        File savedFile = fileRepository.save(file);
        return convertToDTO(savedFile);
    }

    private void renameFileAndCopies(File file, String newName) {
        file.setOriginalFileName(newName);

        if (file.getFileCopies() != null) {
            for (File copy : file.getFileCopies()) {
                renameFileAndCopies(copy, newName);
            }
        }
    }

    public void deleteFile(Long fileId, Long userId) {
        File file = fileRepository.findByIdAndUserId(fileId, userId)
            .orElseThrow(() -> new FileNotFoundException("File not found"));

        try {
            // Delete file from disk
            Path filePath = Paths.get(file.getFilePath());
            Files.deleteIfExists(filePath);

            // Update user storage via User Service
            userServiceClient.updateStorageUsed(userId, -file.getFileSize());

            // Delete metadata from database
            fileRepository.delete(file);

        } catch (IOException e) {
            throw new RuntimeException("Failed to delete file", e);
        }
    }

    public List<FileDTO> getFavoriteFiles(Long userId) {
        List<File> files = fileRepository.findByUserIdAndIsFavoriteTrueOrderByCreatedAtDesc(userId);
        return files.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public FileDTO toggleFavorite(Long fileId, Long userId) {
        File file = fileRepository.findByIdAndUserId(fileId, userId)
            .orElseThrow(() -> new FileNotFoundException("File not found"));

        file.setIsFavorite(!file.getIsFavorite());
        File savedFile = fileRepository.save(file);
        return convertToDTO(savedFile);
    }

    public List<FileDTO> searchFiles(Long userId, String searchTerm) {
        List<File> files = fileRepository.searchFilesByUserIdAndName(userId, searchTerm);
        return files.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public FileDTO convertToDTO(File file) {
        FileDTO dto = new FileDTO();
        dto.setId(file.getId());
        dto.setFileName(file.getFileName());
        dto.setOriginalFileName(file.getOriginalFileName());
        dto.setName(file.getOriginalFileName());
        dto.setContentType(file.getContentType());
        dto.setFileSize(file.getFileSize());
        dto.setFileUuid(file.getFileUuid());
        dto.setIsFavorite(file.getIsFavorite());
        dto.setCreatedAt(file.getCreatedAt());
        dto.setUpdatedAt(file.getUpdatedAt());
        dto.setFileExtension(file.getFileExtension());
        dto.setFolderId(file.getFolderId());
        return dto;
    }

    public Map<String, Object> getFileStatistics(Long userId) {
        List<File> userFiles = fileRepository.findByUserIdOrderByCreatedAtDesc(userId);

        Map<String, Object> statistics = new HashMap<>();

        // General statistics
        statistics.put("totalFiles", userFiles.size());
        statistics.put("totalSize", userFiles.stream().mapToLong(File::getFileSize).sum());
        statistics.put("favoriteFiles", userFiles.stream().mapToLong(f -> f.getIsFavorite() ? 1 : 0).sum());

        // File type statistics
        Map<String, Long> fileTypeStats = userFiles.stream()
            .collect(Collectors.groupingBy(
                file -> file.getFileExtension() != null ? file.getFileExtension().toLowerCase() : "unknown",
                Collectors.counting()
            ));
        statistics.put("fileTypeDistribution", fileTypeStats);

        // Size statistics
        Map<String, Long> sizeStats = userFiles.stream()
            .collect(Collectors.groupingBy(
                file -> {
                    long size = file.getFileSize();
                    if (size < 1024 * 1024) return "< 1MB";
                    else if (size < 10 * 1024 * 1024) return "1-10MB";
                    else if (size < 100 * 1024 * 1024) return "10-100MB";
                    else return "> 100MB";
                },
                Collectors.counting()
            ));
        statistics.put("fileSizeDistribution", sizeStats);

        // Recent files (last 7 days)
        long recentFiles = userFiles.stream()
            .filter(file -> file.getCreatedAt().isAfter(
                java.time.LocalDateTime.now().minusDays(7)
            ))
            .count();
        statistics.put("recentFiles", recentFiles);

        // Top 5 largest files
        List<Map<String, Object>> largestFiles = userFiles.stream()
            .sorted((f1, f2) -> Long.compare(f2.getFileSize(), f1.getFileSize()))
            .limit(5)
            .map(file -> {
                Map<String, Object> fileInfo = new HashMap<>();
                fileInfo.put("name", file.getOriginalFileName());
                fileInfo.put("size", file.getFileSize());
                fileInfo.put("extension", file.getFileExtension());
                return fileInfo;
            })
            .collect(Collectors.toList());
        statistics.put("largestFiles", largestFiles);

        return statistics;
    }

    public List<FileDTO> getFilesByFolder(Long folderId, Long userId) {
        List<File> files = fileRepository.findByFolderIdAndUserId(folderId, userId);
        return files.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    public void bulkMoveFiles(List<Long> fileIds, Long destinationFolderId, Long userId) {
        List<File> files = fileRepository.findAllById(fileIds);
        for (File file : files) {
            if (!file.getUserId().equals(userId)) {
                throw new RuntimeException("File with id " + file.getId() + " does not belong to user");
            }
        }

        for (File file : files) {
            file.setFolderId(destinationFolderId);
            fileRepository.save(file);

            try {
                movePhysicalFile(file, destinationFolderId);
            } catch (IOException e) {
                throw new RuntimeException("Failed to move physical file: " + e.getMessage());
            }
        }
    }

    public void bulkCopyFiles(List<Long> fileIds, Long destinationFolderId, Long userId) {
        List<File> files = fileRepository.findAllById(fileIds);
        for (File file : files) {
            if (!file.getUserId().equals(userId)) {
                throw new RuntimeException("File with id " + file.getId() + " does not belong to user");
            }
        }

        for (File file : files) {
            try {
                copyFile(file, destinationFolderId, userId);
            } catch (IOException e) {
                throw new RuntimeException("Failed to copy file: " + e.getMessage());
            }
        }
    }

    private void movePhysicalFile(File file, Long destinationFolderId) throws IOException {
        Path oldPath = Paths.get(fileStorageConfig.getUploadDir(), String.valueOf(file.getUserId()), file.getFileName());
        
        String newFolderPath = destinationFolderId != null ? 
            String.valueOf(destinationFolderId) : String.valueOf(file.getUserId());
        Path newPath = Paths.get(fileStorageConfig.getUploadDir(), newFolderPath, file.getFileName());
        
        Files.createDirectories(newPath.getParent());
        Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
    }

    private void copyFile(File originalFile, Long destinationFolderId, Long userId) throws IOException {
        File copiedFile = new File();
        copiedFile.setOriginalFileName(originalFile.getOriginalFileName());
        copiedFile.setFileName(generateUniqueFileName(originalFile.getOriginalFileName()));
        copiedFile.setFileSize(originalFile.getFileSize());
        copiedFile.setContentType(originalFile.getContentType());
        copiedFile.setFileUuid(UUID.randomUUID().toString());
        copiedFile.setUserId(userId);
        copiedFile.setFolderId(destinationFolderId);

        copiedFile = fileRepository.save(copiedFile);

        Path sourcePath = Paths.get(fileStorageConfig.getUploadDir(), String.valueOf(originalFile.getUserId()), originalFile.getFileName());
        
        String destinationFolderPath = destinationFolderId != null ? 
            String.valueOf(destinationFolderId) : String.valueOf(userId);
        Path destinationPath = Paths.get(fileStorageConfig.getUploadDir(), destinationFolderPath, copiedFile.getFileName());
        
        Files.createDirectories(destinationPath.getParent());
        Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        
        // Update storage
        userServiceClient.updateStorageUsed(userId, originalFile.getFileSize());
    }

    private String generateUniqueFileName(String originalFileName) {
        String extension = "";
        String nameWithoutExtension = originalFileName;
        
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            extension = originalFileName.substring(lastDotIndex);
            nameWithoutExtension = originalFileName.substring(0, lastDotIndex);
        }
        
        return nameWithoutExtension + "_copy_" + System.currentTimeMillis() + extension;
    }
}
