package org.example.userservice.exception;

public class CodeInvalidException extends RuntimeException {
    public CodeInvalidException(String message) {
        super(message);
    }
}
