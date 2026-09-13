package com.pothole.exception;

public class AiServiceUnavailableException extends AiServiceException {

    public AiServiceUnavailableException(String message) {
        super("AI_SERVICE_UNAVAILABLE", message);
    }

    public AiServiceUnavailableException(String message, Throwable cause) {
        super("AI_SERVICE_UNAVAILABLE", message, cause);
    }
}
