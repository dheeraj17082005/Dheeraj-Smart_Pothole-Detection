package com.pothole.client.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pothole.dto.ai.AiErrorResponse;
import com.pothole.dto.ai.AiImageDetectionResponse;
import com.pothole.exception.AiInferenceFailedException;
import com.pothole.exception.AiInvalidImageException;
import com.pothole.exception.AiServiceException;
import com.pothole.exception.AiServiceTimeoutException;
import com.pothole.exception.AiServiceUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.io.IOException;
import java.net.SocketTimeoutException;

@Component
public class FastApiInferenceClient implements AiInferenceClient {

    private static final Logger log = LoggerFactory.getLogger(FastApiInferenceClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public FastApiInferenceClient(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiImageDetectionResponse detectImage(Resource imageResource, String filename, Double confidenceThreshold) {
        String safeFilename = (filename != null && !filename.isBlank()) ? filename : "image.jpg";

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        try {
            byte[] bytes = imageResource.getInputStream().readAllBytes();
            ByteArrayResource namedResource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return safeFilename;
                }
            };
            body.add("file", namedResource);
        } catch (IOException e) {
            throw new AiInvalidImageException("Could not read image input stream: " + e.getMessage());
        }

        try {
            return restClient.post()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/detect/image");
                        if (confidenceThreshold != null) {
                            uriBuilder.queryParam("confidence_threshold", confidenceThreshold);
                        }
                        return uriBuilder.build();
                    })
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        HttpStatusCode status = response.getStatusCode();
                        byte[] responseBody = response.getBody().readAllBytes();
                        String errorMessage = extractErrorMessage(responseBody, status);

                        if (status.equals(HttpStatus.BAD_REQUEST) || status.equals(HttpStatus.UNPROCESSABLE_ENTITY)) {
                            throw new AiInvalidImageException(errorMessage);
                        } else if (status.equals(HttpStatus.SERVICE_UNAVAILABLE)) {
                            throw new AiServiceUnavailableException(errorMessage);
                        } else if (status.equals(HttpStatus.INTERNAL_SERVER_ERROR)) {
                            throw new AiInferenceFailedException(errorMessage);
                        } else {
                            throw new AiServiceException("HTTP_" + status.value(), errorMessage);
                        }
                    })
                    .body(AiImageDetectionResponse.class);

        } catch (RestClientResponseException e) {
            // In case an unhandled response exception escaped
            String errorMessage = extractErrorMessage(e.getResponseBodyAsByteArray(), e.getStatusCode());
            throw new AiServiceException("HTTP_" + e.getStatusCode().value(), errorMessage, e);

        } catch (ResourceAccessException e) {
            log.error("Network or timeout error communicating with FastAPI AI service: {}", e.getMessage());
            if (e.getCause() instanceof SocketTimeoutException || e.getMessage().toLowerCase().contains("timed out")) {
                throw new AiServiceTimeoutException("AI service request timed out: " + e.getMessage(), e);
            }
            throw new AiServiceUnavailableException("Unable to connect to AI service: " + e.getMessage(), e);

        } catch (AiServiceException e) {
            // Rethrow domain exceptions as is
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error during AI image inference: {}", e.getMessage(), e);
            throw new AiServiceException("AI_CLIENT_ERROR", "Unexpected error executing AI inference: " + e.getMessage(), e);
        }
    }

    @Override
    public com.pothole.dto.ai.AiAnnotatedDetectionResult detectAndAnnotateImage(Resource imageResource, String filename, Double confidenceThreshold) {
        String safeFilename = (filename != null && !filename.isBlank()) ? filename : "image.jpg";

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        try {
            byte[] bytes = imageResource.getInputStream().readAllBytes();
            ByteArrayResource namedResource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return safeFilename;
                }
            };
            body.add("file", namedResource);
        } catch (IOException e) {
            throw new AiInvalidImageException("Could not read image input stream: " + e.getMessage());
        }

        try {
            var responseEntity = restClient.post()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/detect/image/annotate");
                        if (confidenceThreshold != null) {
                            uriBuilder.queryParam("confidence_threshold", confidenceThreshold);
                        }
                        return uriBuilder.build();
                    })
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        HttpStatusCode status = response.getStatusCode();
                        byte[] responseBody = response.getBody().readAllBytes();
                        String errorMessage = extractErrorMessage(responseBody, status);

                        if (status.equals(HttpStatus.BAD_REQUEST) || status.equals(HttpStatus.UNPROCESSABLE_ENTITY)) {
                            throw new AiInvalidImageException(errorMessage);
                        } else if (status.equals(HttpStatus.SERVICE_UNAVAILABLE)) {
                            throw new AiServiceUnavailableException(errorMessage);
                        } else if (status.equals(HttpStatus.INTERNAL_SERVER_ERROR)) {
                            throw new AiInferenceFailedException(errorMessage);
                        } else {
                            throw new AiServiceException("HTTP_" + status.value(), errorMessage);
                        }
                    })
                    .toEntity(byte[].class);

            byte[] annotatedBytes = responseEntity.getBody();
            String metadataJson = responseEntity.getHeaders().getFirst("X-Detection-Metadata");
            AiImageDetectionResponse metadata;
            if (metadataJson != null && !metadataJson.isBlank()) {
                metadata = objectMapper.readValue(metadataJson, AiImageDetectionResponse.class);
            } else {
                throw new AiInferenceFailedException("Missing X-Detection-Metadata response header from AI service");
            }

            return new com.pothole.dto.ai.AiAnnotatedDetectionResult(metadata, annotatedBytes);

        } catch (RestClientResponseException e) {
            String errorMessage = extractErrorMessage(e.getResponseBodyAsByteArray(), e.getStatusCode());
            throw new AiServiceException("HTTP_" + e.getStatusCode().value(), errorMessage, e);

        } catch (ResourceAccessException e) {
            log.error("Network or timeout error communicating with FastAPI AI service: {}", e.getMessage());
            if (e.getCause() instanceof SocketTimeoutException || e.getMessage().toLowerCase().contains("timed out")) {
                throw new AiServiceTimeoutException("AI service request timed out: " + e.getMessage(), e);
            }
            throw new AiServiceUnavailableException("Unable to connect to AI service: " + e.getMessage(), e);

        } catch (AiServiceException e) {
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error during AI image inference and annotation: {}", e.getMessage(), e);
            throw new AiServiceException("AI_CLIENT_ERROR", "Unexpected error executing AI inference and annotation: " + e.getMessage(), e);
        }
    }

    @Override
    public com.pothole.dto.ai.AiVideoDetectionResponse detectVideo(
            Resource videoResource,
            String filename,
            Double sampleFps,
            Double confidenceThreshold
    ) {
        String safeFilename = (filename != null && !filename.isBlank()) ? filename : "video.mp4";

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        try {
            byte[] bytes = videoResource.getInputStream().readAllBytes();
            ByteArrayResource namedResource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return safeFilename;
                }
            };
            body.add("file", namedResource);
        } catch (IOException e) {
            throw new AiInvalidImageException("Could not read video input stream: " + e.getMessage());
        }

        try {
            return restClient.post()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/detect/video");
                        if (sampleFps != null) {
                            uriBuilder.queryParam("sample_fps", sampleFps);
                        }
                        if (confidenceThreshold != null) {
                            uriBuilder.queryParam("confidence_threshold", confidenceThreshold);
                        }
                        return uriBuilder.build();
                    })
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        HttpStatusCode status = response.getStatusCode();
                        byte[] responseBody = response.getBody().readAllBytes();
                        String errorMessage = extractErrorMessage(responseBody, status);

                        if (status.equals(HttpStatus.BAD_REQUEST) || status.equals(HttpStatus.UNPROCESSABLE_ENTITY)) {
                            throw new AiInvalidImageException(errorMessage);
                        } else if (status.equals(HttpStatus.SERVICE_UNAVAILABLE)) {
                            throw new AiServiceUnavailableException(errorMessage);
                        } else if (status.equals(HttpStatus.INTERNAL_SERVER_ERROR)) {
                            throw new AiInferenceFailedException(errorMessage);
                        } else {
                            throw new AiServiceException("HTTP_" + status.value(), errorMessage);
                        }
                    })
                    .body(com.pothole.dto.ai.AiVideoDetectionResponse.class);

        } catch (RestClientResponseException e) {
            String errorMessage = extractErrorMessage(e.getResponseBodyAsByteArray(), e.getStatusCode());
            throw new AiServiceException("HTTP_" + e.getStatusCode().value(), errorMessage, e);

        } catch (ResourceAccessException e) {
            log.error("Network or timeout error communicating with FastAPI AI service during video inference: {}", e.getMessage());
            if (e.getCause() instanceof SocketTimeoutException || e.getMessage().toLowerCase().contains("timed out")) {
                throw new AiServiceTimeoutException("AI service request timed out during video processing: " + e.getMessage(), e);
            }
            throw new AiServiceUnavailableException("Unable to connect to AI service: " + e.getMessage(), e);

        } catch (AiServiceException e) {
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error during AI video inference: {}", e.getMessage(), e);
            throw new AiServiceException("AI_CLIENT_ERROR", "Unexpected error executing AI video inference: " + e.getMessage(), e);
        }
    }

    @Override
    public byte[] annotateVideoFrame(
            Resource videoResource,
            String filename,
            Integer frameIndex,
            Double confidenceThreshold
    ) {
        String safeFilename = (filename != null && !filename.isBlank()) ? filename : "video.mp4";

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        try {
            byte[] bytes = videoResource.getInputStream().readAllBytes();
            ByteArrayResource namedResource = new ByteArrayResource(bytes) {
                @Override
                public String getFilename() {
                    return safeFilename;
                }
            };
            body.add("file", namedResource);
        } catch (IOException e) {
            throw new AiInvalidImageException("Could not read video input stream: " + e.getMessage());
        }

        try {
            return restClient.post()
                    .uri(uriBuilder -> {
                        uriBuilder.path("/detect/video/annotate-frame");
                        if (frameIndex != null) {
                            uriBuilder.queryParam("frame_index", frameIndex);
                        }
                        if (confidenceThreshold != null) {
                            uriBuilder.queryParam("confidence_threshold", confidenceThreshold);
                        }
                        return uriBuilder.build();
                    })
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .accept(MediaType.IMAGE_JPEG)
                    .body(body)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        HttpStatusCode status = response.getStatusCode();
                        byte[] responseBody = response.getBody().readAllBytes();
                        String errorMessage = extractErrorMessage(responseBody, status);

                        if (status.equals(HttpStatus.BAD_REQUEST) || status.equals(HttpStatus.UNPROCESSABLE_ENTITY)) {
                            throw new AiInvalidImageException(errorMessage);
                        } else if (status.equals(HttpStatus.SERVICE_UNAVAILABLE)) {
                            throw new AiServiceUnavailableException(errorMessage);
                        } else if (status.equals(HttpStatus.INTERNAL_SERVER_ERROR)) {
                            throw new AiInferenceFailedException(errorMessage);
                        } else {
                            throw new AiServiceException("HTTP_" + status.value(), errorMessage);
                        }
                    })
                    .body(byte[].class);

        } catch (RestClientResponseException e) {
            String errorMessage = extractErrorMessage(e.getResponseBodyAsByteArray(), e.getStatusCode());
            throw new AiServiceException("HTTP_" + e.getStatusCode().value(), errorMessage, e);

        } catch (ResourceAccessException e) {
            log.error("Network or timeout error communicating with FastAPI AI service during frame annotation: {}", e.getMessage());
            if (e.getCause() instanceof SocketTimeoutException || e.getMessage().toLowerCase().contains("timed out")) {
                throw new AiServiceTimeoutException("AI service request timed out during frame annotation: " + e.getMessage(), e);
            }
            throw new AiServiceUnavailableException("Unable to connect to AI service: " + e.getMessage(), e);

        } catch (AiServiceException e) {
            throw e;

        } catch (Exception e) {
            log.error("Unexpected error during AI video frame annotation: {}", e.getMessage(), e);
            throw new AiServiceException("AI_CLIENT_ERROR", "Unexpected error executing AI frame annotation: " + e.getMessage(), e);
        }
    }

    private String extractErrorMessage(byte[] responseBody, HttpStatusCode status) {
        if (responseBody != null && responseBody.length > 0) {
            try {
                AiErrorResponse errorResp = objectMapper.readValue(responseBody, AiErrorResponse.class);
                if (errorResp != null && errorResp.error() != null && errorResp.error().message() != null) {
                    return errorResp.error().message();
                }
            } catch (Exception ignored) {
                // Fall back to plain text
                return new String(responseBody);
            }
        }
        return "AI service responded with status " + status;
    }
}
