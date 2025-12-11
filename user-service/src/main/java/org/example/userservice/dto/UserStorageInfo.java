package org.example.userservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStorageInfo {
    private Long storageUsed;
    private Long maxStorage;
    private Double storageUsedPercentage;
    private Long availableStorage;
}
