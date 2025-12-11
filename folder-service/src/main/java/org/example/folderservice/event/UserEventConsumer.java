package org.example.folderservice.event;

import org.example.folderservice.repository.FolderRepository;
import org.example.folderservice.repository.FolderShareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Consumer;

/**
 * Event consumers for user-related events from user-service.
 * Handles user creation and deletion for folder cleanup.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final FolderRepository folderRepository;
    private final FolderShareRepository folderShareRepository;

    /**
     * Consumer for user-created events.
     * Initialize any user-specific folder structure when a new user is created.
     */
    @Bean
    public Consumer<UserEventDTO> userCreatedConsumer() {
        return user -> {
            log.info("Received user-created event for userId: {}, email: {}", user.getId(), user.getEmail());
            try {
                // Optional: Create default folders for new user
                // e.g., "My Documents", "Shared", "Trash" etc.
                // This can be customized based on business requirements
                
                log.info("Successfully processed user-created event for userId: {}", user.getId());
            } catch (Exception e) {
                log.error("Error processing user-created event for userId: {}", user.getId(), e);
            }
        };
    }

    /**
     * Consumer for user-deleted events.
     * Clean up all folders and folder shares associated with the deleted user.
     */
    @Bean
    @Transactional
    public Consumer<Long> userDeletedConsumer() {
        return userId -> {
            log.info("Received user-deleted event for userId: {}", userId);
            try {
                // Delete all folder shares where user is target
                var targetShares = folderShareRepository.findByTargetUserId(userId);
                if (!targetShares.isEmpty()) {
                    folderShareRepository.deleteAll(targetShares);
                    log.info("Deleted {} folder shares where userId {} was target", targetShares.size(), userId);
                }
                
                // Delete all folder shares owned by the user
                var ownedShares = folderShareRepository.findByOwnerId(userId);
                if (!ownedShares.isEmpty()) {
                    folderShareRepository.deleteAll(ownedShares);
                    log.info("Deleted {} folder shares owned by userId {}", ownedShares.size(), userId);
                }
                
                // Get all folders owned by the user (root folders first, then subfolders)
                var userFolders = folderRepository.findByUserIdOrderByCreatedAtDesc(userId);
                if (!userFolders.isEmpty()) {
                    log.info("Found {} folders to delete for userId: {}", userFolders.size(), userId);
                    
                    // Delete folders (JPA cascade should handle subfolders)
                    // Start with folders that have no subfolders to avoid constraint violations
                    folderRepository.deleteAll(userFolders);
                    log.info("Deleted {} folders for userId: {}", userFolders.size(), userId);
                }
                
                log.info("Successfully processed user-deleted event for userId: {}", userId);
            } catch (Exception e) {
                log.error("Error processing user-deleted event for userId: {}", userId, e);
            }
        };
    }
}
