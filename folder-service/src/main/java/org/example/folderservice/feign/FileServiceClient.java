package org.example.folderservice.feign;

import org.example.folderservice.dto.FileDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Feign client for communicating with file-service.
 * Used to manage files within folders.
 */
@FeignClient(name = "file-service", url = "${services.file-service.url:http://localhost:8082}")
public interface FileServiceClient {
    
    @GetMapping("/api/files/folder/{folderId}")
    List<FileDTO> getFilesByFolderId(@PathVariable("folderId") Long folderId,
                                      @RequestHeader("X-User-Id") Long userId);
    
    @GetMapping("/api/files/folder/{folderId}/count")
    Integer getFileCountByFolderId(@PathVariable("folderId") Long folderId,
                                    @RequestHeader("X-User-Id") Long userId);
    
    @GetMapping("/api/files/folder/{folderId}/size")
    Long getTotalSizeByFolderId(@PathVariable("folderId") Long folderId,
                                 @RequestHeader("X-User-Id") Long userId);
    
    @DeleteMapping("/api/files/folder/{folderId}")
    void deleteFilesByFolderId(@PathVariable("folderId") Long folderId,
                                @RequestHeader("X-User-Id") Long userId);
    
    @PostMapping("/api/files/{fileId}/copy")
    FileDTO copyFile(@PathVariable("fileId") Long fileId,
                     @RequestParam("destinationFolderId") Long destinationFolderId,
                     @RequestHeader("X-User-Id") Long userId);
}
