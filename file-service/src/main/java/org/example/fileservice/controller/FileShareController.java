package org.example.fileservice.controller;

import org.example.fileservice.dto.FileDTO;
import org.example.fileservice.dto.ShareNotificationDTO;
import org.example.fileservice.exception.FileNotFoundException;
import org.example.fileservice.exception.UserNotFoundException;
import org.example.fileservice.service.FileShareService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
@Slf4j
public class FileShareController {
    
    private final FileShareService fileShareService;

    @PostMapping("/share/{fileId}")
    public ResponseEntity<String> shareFile(
            @PathVariable Long fileId, 
            @RequestParam String userEmail) {
        try {
            ShareNotificationDTO fileShare = fileShareService.shareFileWithUser(fileId, userEmail);
            // Note: WebSocket notification should be handled separately in microservices
            // You can publish to a message broker (Kafka/RabbitMQ) here
            return ResponseEntity.ok("File shared successfully");
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        } catch (FileNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
        } catch (Exception e) {
            log.error("Error sharing file", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/share/requests")
    public ResponseEntity<List<ShareNotificationDTO>> shareRequests(
            @RequestParam Long userId) {
        try {
            List<ShareNotificationDTO> shareNotificationDTOList = fileShareService.getShareRequests(userId);
            return ResponseEntity.ok(shareNotificationDTOList);
        } catch (Exception e) {
            log.error("Error getting share requests", e);
            throw new RuntimeException(e);
        }
    }

    @PostMapping("/share/response/{shareFileId}")
    public ResponseEntity<?> shareResponse(
            @PathVariable Long shareFileId,
            @RequestParam boolean response) {
        try {
            FileDTO fileDTO = fileShareService.shareResponse(shareFileId, response);
            return ResponseEntity.ok(fileDTO);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Share request not found");
        }
    }

    @GetMapping("/shared/{fileId}/with")
    public ResponseEntity<List<Long>> getFileUserIds(@PathVariable Long fileId) {
        return ResponseEntity.ok(fileShareService.getUserIdsWhoShareMyFile(fileId));
    }

    @GetMapping("/shared")
    public ResponseEntity<List<FileDTO>> getSharedFilesWithMe(@RequestParam Long userId) {
        return ResponseEntity.ok(fileShareService.getSharedFilesWithMe(userId));
    }

    @GetMapping("/shared/by-me")
    public ResponseEntity<List<FileDTO>> sharedFilesByMe(@RequestParam Long userId) {
        return ResponseEntity.ok(fileShareService.getSharedFilesByMe(userId));
    }

    @DeleteMapping("/{fileId}/share")
    public ResponseEntity<?> unshareFile(
            @PathVariable Long fileId,
            @RequestParam String userEmail) {
        try {
            fileShareService.unshareFile(fileId, userEmail);
            return ResponseEntity.ok().build();
        } catch (UserNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found");
        } catch (FileNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("File not found");
        }
    }
}
