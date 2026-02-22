package com.jkingai.classroomclarity.exception;

import com.jkingai.classroomclarity.dto.ErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/test");

    @Test
    void handleDocumentNotFoundReturns404() {
        UUID id = UUID.randomUUID();
        DocumentNotFoundException ex = new DocumentNotFoundException(id);

        ResponseEntity<ErrorResponse> response = handler.handleDocumentNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("DOCUMENT_NOT_FOUND");
        assertThat(response.getBody().path()).isEqualTo("/api/v1/test");
    }

    @Test
    void handleInvalidFileTypeReturns400() {
        InvalidFileTypeException ex = new InvalidFileTypeException("application/vnd.ms-excel");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidFileType(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("INVALID_FILE_TYPE");
        assertThat(response.getBody().message()).contains("application/vnd.ms-excel");
    }

    @Test
    void handleNoRelevantContextReturns422() {
        NoRelevantContextException ex = new NoRelevantContextException(0.7);

        ResponseEntity<ErrorResponse> response = handler.handleNoRelevantContext(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("NO_RELEVANT_CONTEXT");
        assertThat(response.getBody().message()).contains("0.7");
    }

    @Test
    void handleLlmServiceErrorReturns502() {
        LlmServiceException ex = new LlmServiceException("Vertex AI timeout");

        ResponseEntity<ErrorResponse> response = handler.handleLlmServiceError(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("LLM_ERROR");
    }

    @Test
    void handleDocumentProcessingReturns500() {
        DocumentProcessingException ex = new DocumentProcessingException("Extraction failed");

        ResponseEntity<ErrorResponse> response = handler.handleDocumentProcessing(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("PROCESSING_FAILED");
    }

    @Test
    void handleMaxUploadSizeReturns400() {
        org.springframework.web.multipart.MaxUploadSizeExceededException ex =
                new org.springframework.web.multipart.MaxUploadSizeExceededException(50 * 1024 * 1024);

        ResponseEntity<ErrorResponse> response = handler.handleMaxUploadSize(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("FILE_TOO_LARGE");
    }

    @Test
    void handleGenericExceptionReturns500WithNoStackTrace() {
        Exception ex = new RuntimeException("Something unexpected");

        ResponseEntity<ErrorResponse> response = handler.handleGeneric(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).doesNotContain("RuntimeException");
        assertThat(response.getBody().message()).doesNotContain("Something unexpected");
    }
}
