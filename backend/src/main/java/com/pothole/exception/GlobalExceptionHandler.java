package com.pothole.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AiInvalidImageException.class)
    public ProblemDetail handleAiInvalidImage(AiInvalidImageException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Image Payload");
        problem.setType(URI.create("urn:problem-type:invalid-image"));
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AiServiceUnavailableException.class)
    public ProblemDetail handleAiServiceUnavailable(AiServiceUnavailableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setTitle("AI Service Unavailable");
        problem.setType(URI.create("urn:problem-type:ai-service-unavailable"));
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AiServiceTimeoutException.class)
    public ProblemDetail handleAiServiceTimeout(AiServiceTimeoutException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.GATEWAY_TIMEOUT, ex.getMessage());
        problem.setTitle("AI Service Timeout");
        problem.setType(URI.create("urn:problem-type:ai-service-timeout"));
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AiInferenceFailedException.class)
    public ProblemDetail handleAiInferenceFailed(AiInferenceFailedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problem.setTitle("AI Inference Failed");
        problem.setType(URI.create("urn:problem-type:ai-inference-failed"));
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(AiServiceException.class)
    public ProblemDetail handleGenericAiServiceException(AiServiceException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problem.setTitle("AI Service Error");
        problem.setType(URI.create("urn:problem-type:ai-service-error"));
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(ObjectNotFoundException.class)
    public ProblemDetail handleObjectNotFound(ObjectNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Object Not Found");
        problem.setType(URI.create("urn:problem-type:object-not-found"));
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(StorageUnavailableException.class)
    public ProblemDetail handleStorageUnavailable(StorageUnavailableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage());
        problem.setTitle("Storage Service Unavailable");
        problem.setType(URI.create("urn:problem-type:storage-unavailable"));
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(StorageException.class)
    public ProblemDetail handleStorageException(StorageException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problem.setTitle("Storage Operation Failed");
        problem.setType(URI.create("urn:problem-type:storage-error"));
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(InvalidMediaException.class)
    public ProblemDetail handleInvalidMedia(InvalidMediaException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Media");
        problem.setType(URI.create("urn:problem-type:invalid-media"));
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(PotholeNotReportableException.class)
    public ProblemDetail handlePotholeNotReportable(PotholeNotReportableException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Pothole Not Reportable");
        problem.setType(URI.create("urn:problem-type:pothole-not-reportable"));
        problem.setProperty("errorCode", ex.getErrorCode());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(org.springframework.validation.BindException.class)
    public ProblemDetail handleValidationException(org.springframework.validation.BindException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .reduce((a, b) -> a + "; " + b)
                .orElse("Validation failed");

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        problem.setTitle("Validation Failed");
        problem.setType(URI.create("urn:problem-type:validation-error"));
        problem.setProperty("errorCode", "VALIDATION_FAILED");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ProblemDetail handleIllegalArguments(RuntimeException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Request");
        problem.setType(URI.create("urn:problem-type:bad-request"));
        problem.setProperty("errorCode", "BAD_REQUEST");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParams(org.springframework.web.bind.MissingServletRequestParameterException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Missing Request Parameter");
        problem.setType(URI.create("urn:problem-type:missing-parameter"));
        problem.setProperty("errorCode", "MISSING_PARAMETER");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(org.springframework.web.server.ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        String detail = ex.getReason() != null ? ex.getReason() : ex.getMessage();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(status.getReasonPhrase());
        problem.setType(URI.create("urn:problem-type:auth-error"));
        problem.setProperty("errorCode", status.name());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ProblemDetail handleAuthenticationException(org.springframework.security.core.AuthenticationException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("Authentication Failed");
        problem.setType(URI.create("urn:problem-type:unauthorized"));
        problem.setProperty("errorCode", "UNAUTHORIZED");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(org.springframework.security.access.AccessDeniedException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access is denied");
        problem.setTitle("Access Denied");
        problem.setType(URI.create("urn:problem-type:access-denied"));
        problem.setProperty("errorCode", "FORBIDDEN");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception ex) {
        log.error("Unhandled exception caught in GlobalExceptionHandler: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected internal server error occurred.");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("urn:problem-type:internal-server-error"));
        problem.setProperty("errorCode", "INTERNAL_SERVER_ERROR");
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}

