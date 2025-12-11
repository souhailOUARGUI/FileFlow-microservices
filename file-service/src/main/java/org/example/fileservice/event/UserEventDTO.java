package org.example.fileservice.event;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * DTO representing a user event received from user-service.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEventDTO {
    private Long id;
    private String email;
    private String firstName;
    private String lastName;
    private Long storageUsed;
    private Long storageLimit;
}
