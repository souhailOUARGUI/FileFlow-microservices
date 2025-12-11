package org.example.userservice.event;

import org.example.userservice.dto.UserDTO;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Event publisher for user-related events using Spring Cloud Stream.
 * Publishes events to Kafka topics for other microservices to consume.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserEventPublisher {
    
    private final StreamBridge streamBridge;
    
    /**
     * Publish event when a new user is created.
     * Other services (file-service, folder-service) can listen to initialize user-related data.
     */
    public void publishUserCreated(UserDTO user) {
        log.info("Publishing user-created event for user: {}", user.getEmail());
        boolean sent = streamBridge.send("user-created-out-0", user);
        if (sent) {
            log.debug("User-created event sent successfully for userId: {}", user.getId());
        } else {
            log.error("Failed to send user-created event for userId: {}", user.getId());
        }
    }
    
    /**
     * Publish event when a user is deleted.
     * Other services should clean up user-related data (files, folders, shares, etc.)
     */
    public void publishUserDeleted(Long userId) {
        log.info("Publishing user-deleted event for userId: {}", userId);
        boolean sent = streamBridge.send("user-deleted-out-0", userId);
        if (sent) {
            log.debug("User-deleted event sent successfully for userId: {}", userId);
        } else {
            log.error("Failed to send user-deleted event for userId: {}", userId);
        }
    }
    
    /**
     * Publish event when user storage quota is updated.
     * File-service can listen to validate storage limits.
     */
    public void publishStorageUpdated(Long userId, Long storageUsed, Long storageLimit) {
        log.info("Publishing storage-updated event for userId: {}", userId);
        StorageUpdateEvent event = new StorageUpdateEvent(userId, storageUsed, storageLimit);
        boolean sent = streamBridge.send("storage-updated-out-0", event);
        if (sent) {
            log.debug("Storage-updated event sent successfully for userId: {}", userId);
        } else {
            log.error("Failed to send storage-updated event for userId: {}", userId);
        }
    }
    
    /**
     * Inner class for storage update events
     */
    public record StorageUpdateEvent(Long userId, Long storageUsed, Long storageLimit) {}
}
