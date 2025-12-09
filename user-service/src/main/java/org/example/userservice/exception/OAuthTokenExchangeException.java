package org.example.userservice.exception;

public class OAuthTokenExchangeException extends RuntimeException {
    public OAuthTokenExchangeException(String message) {
        super(message);
    }

    public OAuthTokenExchangeException(String message, Throwable cause) {
        super(message, cause);
    }
}
