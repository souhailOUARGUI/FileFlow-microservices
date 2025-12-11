package org.example.fileservice.event;

import org.example.fileservice.repository.FileRepository;
import org.example.fileservice.repository.FileShareRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * Event consumers for user-related events from user-service.
 * Handles user creation, deletion, and storage updates.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class UserEventConsumer {

    private final FileRepository fileRepository;
    private final FileShareRepository fileShareRepository;
    
    // Cache for user storage limits (userId -> storageLimit)
    private final Map<Long, Long> userStorageLimits = new ConcurrentHashMap<>();

    /**
     * Consumer for user-created events.
     * Initialize any user-specific data or cache when a new user is created.
     */
    @Bean
    public Consumer<UserEventDTO> userCreatedConsumer() {
        return user -> {
            log.info("Received user-created event for userId: {}, email: {}", user.getId(), user.getEmail());
            try {
                // Cache the user's storage limit for quick access
                if (user.getStorageLimit() != null) {
                    userStorageLimits.put(user.getId(), user.getStorageLimit());
                    log.debug("Cached storage limit for userId {}: {} bytes", user.getId(), user.getStorageLimit());
                }
                
                // Additional initialization can be done here
                // e.g., create default folders, initialize quotas, etc.
                
                log.info("Successfully processed user-created event for userId: {}", user.getId());
            } catch (Exception e) {
                log.error("Error processing user-created event for userId: {}", user.getId(), e);
            }
        };
    }

    /**
     * Consumer for user-deleted events.
     * Clean up all files and file shares associated with the deleted user.
     */
    @Bean
    @Transactional
    public Consumer<Long> userDeletedConsumer() {
        return userId -> {
            log.info("Received user-deleted event for userId: {}", userId);
            try {
                // Delete all file shares where user is target
                int deletedShares = fileShareRepository.deleteByTargetUserId(userId);
                log.info("Deleted {} file shares for target userId: {}", deletedShares, userId);
                
                // Delete all files owned by the user
                // Note: This will also trigger physical file deletion via FileService
                var userFiles = fileRepository.findByUserId(userId);
                if (!userFiles.isEmpty()) {
                    log.info("Found {} files to delete for userId: {}", userFiles.size(), userId);
                    
                    // Delete file shares for these files first
                    userFiles.forEach(file -> {
                        fileShareRepository.deleteByFileId(file.getId());
                    });
                    
                    // Delete the files
                    fileRepository.deleteAll(userFiles);
                    log.info("Deleted {} files for userId: {}", userFiles.size(), userId);
                    
                    // TODO: Also delete physical files from storage
                    // This should be handled by a separate cleanup service
                }
                
                // Remove from cache
                userStorageLimits.remove(userId);
                
                log.info("Successfully processed user-deleted event for userId: {}", userId);
            } catch (Exception e) {
                log.error("Error processing user-deleted event for userId: {}", userId, e);
            }
        };
    }

    /**
     * Consumer for storage-updated events.
     * Update cached storage limits when user quota changes.
     */
    @Bean
    public Consumer<StorageUpdateEvent> storageUpdatedConsumer() {
        return event -> {
            log.info("Received storage-updated event for userId: {}", event.getUserId());
            try {
                // Update cached storage limit
                if (event.getStorageLimit() != null) {
                    userStorageLimits.put(event.getUserId(), event.getStorageLimit());
                    log.debug("Updated cached storage limit for userId {}: {} bytes", 
                             event.getUserId(), event.getStorageLimit());
                }
                
                log.info("Successfully processed storage-updated event for userId: {}", event.getUserId());
            } catch (Exception e) {
                log.error("Error processing storage-updated event for userId: {}", event.getUserId(), e);
            }
        };
    }
    
    /**
     * Get cached storage limit for a user.
     * Returns null if not cached (caller should fetch from user-service).
     */
    public Long getCachedStorageLimit(Long userId) {
        return userStorageLimits.get(userId);
    }
}
