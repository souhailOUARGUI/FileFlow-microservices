package org.example.folderservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for file information received from file-service.
 * Used for folder content display.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileDTO {
    private Long id;
    private String fileName;
    private String originalFileName;
    private String name;
    private Long fileSize;
    private String contentType;
    private String fileUuid;
    private Boolean isFavorite;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String fileExtension;
    private Long folderId;
}
