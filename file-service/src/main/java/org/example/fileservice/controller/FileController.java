package org.example.fileservice.controller;

import org.example.fileservice.dto.FileDTO;
import org.example.fileservice.service.FileService;
import org.example.fileservice.utils.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "File Management", description = "File management APIs")
public class FileController {

    private final FileService fileService;

    @PostMapping("/upload")
    @Operation(summary = "Upload a file")
    public ResponseEntity<ApiResponse<FileDTO>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folderId", required = false) Long folderId,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            FileDTO uploadedFile = fileService.uploadFile(file, userId, folderId);
            return ResponseEntity.ok(ApiResponse.success("File uploaded successfully", uploadedFile));
        } catch (Exception e) {
            log.error("Error uploading file", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get user files")
    public ResponseEntity<ApiResponse<?>> getUserFiles(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String search,
            @RequestHeader("X-User-Id") Long userId) {

        log.info("Received GET /api/files with page={}, size={}, search={}", page, size, search);

        try {
            List<FileDTO> files;

            if (search != null && !search.isBlank()) {
                files = fileService.searchFiles(userId, search.trim());
            } else {
                int pageNumber = page != null ? page : 0;
                int pageSize = size != null ? size : 20;
                Pageable pageable = PageRequest.of(pageNumber, pageSize);
                files = fileService.getUserFiles(userId, pageable).getContent();
            }

            return ResponseEntity.ok(ApiResponse.success("Files retrieved", files));
        } catch (Exception e) {
            log.error("Error in GET /api/files", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get file details")
    public ResponseEntity<ApiResponse<FileDTO>> getFileDetails(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            FileDTO file = fileService.getFileDetails(id, userId);
            return ResponseEntity.ok(ApiResponse.success("File details retrieved", file));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/{id}/download")
    @Operation(summary = "Download a file")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            Resource resource = fileService.downloadFile(id, userId);
            FileDTO fileMetadata = fileService.getFileDetails(id, userId);
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileMetadata.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "attachment; filename=\"" + fileMetadata.getOriginalFileName() + "\"")
                .body(resource);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping("/{id}/rename")
    @Operation(summary = "Rename a file")
    public ResponseEntity<ApiResponse<FileDTO>> renameFile(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            String newName = request.get("fileName");
            
            if (newName == null || newName.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File name is required"));
            }
            
            FileDTO renamedFile = fileService.renameFile(id, userId, newName.trim());
            return ResponseEntity.ok(ApiResponse.success("File renamed successfully", renamedFile));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a file")
    public ResponseEntity<ApiResponse<Void>> deleteFile(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            fileService.deleteFile(id, userId);
            return ResponseEntity.ok(ApiResponse.success("File deleted successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/statistics")
    @Operation(summary = "Get detailed file statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFileStatistics(
            @RequestHeader("X-User-Id") Long userId) {
        try {
            Map<String, Object> statistics = fileService.getFileStatistics(userId);
            return ResponseEntity.ok(ApiResponse.success("File statistics retrieved successfully", statistics));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/folder/{folderId}")
    @Operation(summary = "Get files in a specific folder")
    public ResponseEntity<ApiResponse<List<FileDTO>>> getFilesByFolder(
            @PathVariable Long folderId,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            List<FileDTO> files = fileService.getFilesByFolder(folderId, userId);
            return ResponseEntity.ok(ApiResponse.success("Files retrieved successfully", files));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PutMapping("/bulk/move")
    @Operation(summary = "Move multiple files to a folder")
    public ResponseEntity<ApiResponse<String>> bulkMoveFiles(
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> fileIds = (List<Integer>) request.get("fileIds");
            Integer destinationFolderId = (Integer) request.get("destinationFolderId");
            
            if (fileIds == null || fileIds.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File IDs are required"));
            }
            
            List<Long> longFileIds = fileIds.stream()
                .map(Integer::longValue)
                .toList();
            
            Long destinationId = destinationFolderId != null ? destinationFolderId.longValue() : null;
            
            fileService.bulkMoveFiles(longFileIds, destinationId, userId);
            return ResponseEntity.ok(ApiResponse.success(
                fileIds.size() + " files moved successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/bulk/copy")
    @Operation(summary = "Copy multiple files to a folder")
    public ResponseEntity<ApiResponse<String>> bulkCopyFiles(
            @RequestBody Map<String, Object> request,
            @RequestHeader("X-User-Id") Long userId) {
        try {
            @SuppressWarnings("unchecked")
            List<Integer> fileIds = (List<Integer>) request.get("fileIds");
            Integer destinationFolderId = (Integer) request.get("destinationFolderId");
            
            if (fileIds == null || fileIds.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File IDs are required"));
            }
            
            List<Long> longFileIds = fileIds.stream()
                .map(Integer::longValue)
                .toList();
            
            Long destinationId = destinationFolderId != null ? destinationFolderId.longValue() : null;
            
            fileService.bulkCopyFiles(longFileIds, destinationId, userId);
            return ResponseEntity.ok(ApiResponse.success(
                fileIds.size() + " files copied successfully", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getMessage()));
        }
    }
}
