package org.example.fileservice.event;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Event representing storage quota updates from user-service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageUpdateEvent {
    private Long userId;
    private Long storageUsed;
    private Long storageLimit;
}
