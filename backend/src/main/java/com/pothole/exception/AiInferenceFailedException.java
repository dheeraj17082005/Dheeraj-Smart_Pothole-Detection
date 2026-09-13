package com.pothole.exception;

public class AiInferenceFailedException extends AiServiceException {

    public AiInferenceFailedException(String message) {
        super("INFERENCE_FAILED", message);
    }
}
