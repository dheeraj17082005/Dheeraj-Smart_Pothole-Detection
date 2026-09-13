package com.pothole.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("handleObjectNotFound returns 404 ProblemDetail")
    void testHandleObjectNotFound() {
        ObjectNotFoundException ex = new ObjectNotFoundException("Object raw/2026/09/sample.jpg not found");
        ProblemDetail problem = exceptionHandler.handleObjectNotFound(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Object Not Found");
        assertThat(problem.getDetail()).isEqualTo("Object raw/2026/09/sample.jpg not found");
        assertThat(problem.getType()).isEqualTo(URI.create("urn:problem-type:object-not-found"));
        assertThat(problem.getProperties()).containsEntry("errorCode", "OBJECT_NOT_FOUND");
    }

    @Test
    @DisplayName("handleStorageUnavailable returns 503 ProblemDetail")
    void testHandleStorageUnavailable() {
        StorageUnavailableException ex = new StorageUnavailableException("Connection refused");
        ProblemDetail problem = exceptionHandler.handleStorageUnavailable(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE.value());
        assertThat(problem.getTitle()).isEqualTo("Storage Service Unavailable");
        assertThat(problem.getDetail()).isEqualTo("Connection refused");
        assertThat(problem.getType()).isEqualTo(URI.create("urn:problem-type:storage-unavailable"));
        assertThat(problem.getProperties()).containsEntry("errorCode", "STORAGE_UNAVAILABLE");
    }

    @Test
    @DisplayName("handleStorageException returns 500 ProblemDetail")
    void testHandleStorageException() {
        StorageException ex = new StorageException("Generic storage failure", "STORAGE_ERROR");
        ProblemDetail problem = exceptionHandler.handleStorageException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Storage Operation Failed");
        assertThat(problem.getDetail()).isEqualTo("Generic storage failure");
        assertThat(problem.getType()).isEqualTo(URI.create("urn:problem-type:storage-error"));
        assertThat(problem.getProperties()).containsEntry("errorCode", "STORAGE_ERROR");
    }

    @Test
    @DisplayName("handleGenericException returns 500 ProblemDetail with sanitized message")
    void testHandleGenericException() {
        Exception ex = new NullPointerException("Secret internal database connection pointer is null at /internal/path");
        ProblemDetail problem = exceptionHandler.handleGenericException(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
        assertThat(problem.getDetail()).isEqualTo("An unexpected internal server error occurred.");
        assertThat(problem.getDetail()).doesNotContain("Secret internal database");
        assertThat(problem.getType()).isEqualTo(URI.create("urn:problem-type:internal-server-error"));
        assertThat(problem.getProperties()).containsEntry("errorCode", "INTERNAL_SERVER_ERROR");
    }

    @Test
    @DisplayName("handleIllegalArguments returns 400 ProblemDetail")
    void testHandleIllegalArguments() {
        IllegalArgumentException ex = new IllegalArgumentException("Latitude out of range");
        ProblemDetail problem = exceptionHandler.handleIllegalArguments(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid Request");
        assertThat(problem.getDetail()).isEqualTo("Latitude out of range");
        assertThat(problem.getProperties()).containsEntry("errorCode", "BAD_REQUEST");
    }

    @Test
    @DisplayName("handleInvalidMedia returns 400 ProblemDetail")
    void testHandleInvalidMedia() {
        InvalidMediaException ex = new InvalidMediaException("Corrupt image file");
        ProblemDetail problem = exceptionHandler.handleInvalidMedia(ex);

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Invalid Media");
        assertThat(problem.getDetail()).isEqualTo("Corrupt image file");
        assertThat(problem.getProperties()).containsEntry("errorCode", "INVALID_MEDIA");
    }
}
