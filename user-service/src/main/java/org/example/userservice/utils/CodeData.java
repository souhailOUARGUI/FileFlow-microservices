package org.example.userservice.utils;

import java.time.LocalDateTime;

public class CodeData {
    String code;
    LocalDateTime expireAt;

    public CodeData(String code, LocalDateTime expireAt) {
        this.code = code;
        this.expireAt = expireAt;
    }

    public String getCode() {
        return code;
    }

    public LocalDateTime getExpireAt() {
        return expireAt;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expireAt);
    }
}
