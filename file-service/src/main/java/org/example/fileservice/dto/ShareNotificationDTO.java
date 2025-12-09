package org.example.fileservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShareNotificationDTO {
    private Long id;
    private String fileName;
    private Long userId;
    private String owner;
    private Long fileId;
}
