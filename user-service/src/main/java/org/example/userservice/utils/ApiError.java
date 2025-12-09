package org.example.userservice.utils;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ApiError {
    private int status;
    private String message;
    private List<String> errors;
    private Map<String, String> fieldErrors;
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();

    public static ApiError of(int status, String message) {
        return ApiError.builder()
                .status(status)
                .message(message)
                .build();
    }

    public static ApiError of(int status, String message, List<String> errors) {
        return ApiError.builder()
                .status(status)
                .message(message)
                .errors(errors)
                .build();
    }

    public static ApiError of(int status, String message, Map<String, String> fieldErrors) {
        return ApiError.builder()
                .status(status)
                .message(message)
                .fieldErrors(fieldErrors)
                .build();
    }
}
