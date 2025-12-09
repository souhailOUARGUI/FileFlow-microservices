package org.example.userservice.dto;

import lombok.Data;

@Data
public class RefreshTokenResponse {
    private String refreshToken;
    private String accessToken;

    public RefreshTokenResponse(String refreshToken, String accessToken) {
        this.refreshToken = refreshToken;
        this.accessToken = accessToken;
    }
}
