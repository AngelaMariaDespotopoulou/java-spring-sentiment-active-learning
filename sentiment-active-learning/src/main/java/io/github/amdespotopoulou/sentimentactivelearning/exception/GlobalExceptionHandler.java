package io.github.amdespotopoulou.sentimentactivelearning.exception;

import io.github.amdespotopoulou.sentimentactivelearning.api.response.ApiErrorResponse;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Global exception handler for the Sentiment Active Learning REST API.
 *
 * <p>Every exception thrown by a controller or service is intercepted here
 * and translated into a uniform
 * {@link ApiErrorResponse} envelope. Consumers always receive the same
 * four-field JSON structure regardless of what went wrong, making error
 * handling predictable and automatable.
 *
 * <h2>Handled exception types</h2>
 * <ul>
 *   <li>{@link MethodArgumentNotValidException} — bean-validation constraint
 *       violations on incoming DTOs; returns 400 with all field errors
 *       collected into a single message.</li>
 *   <li>{@link HttpMessageNotReadableException} — malformed or unparseable
 *       request body; returns 400.</li>
 *   <li>{@link ResourceNotFoundException} — missing resource; returns 404.</li>
 *   <li>{@link ResourceConflictException} — state or uniqueness conflict;
 *       returns 409. Also catches its subclass
 *       {@link ModelNotTrainedException}.</li>
 *   <li>{@link InvalidInputException} — domain-level field validation failure;
 *       returns 400.</li>
 *   <li>{@link ClaudeApiException} — Claude AI oracle failure; returns 502.</li>
 *   <li>{@link Exception} — catch-all for any unexpected error; returns 500.</li>
 * </ul>
 *
 * <p>All handlers log at {@code WARN} level for expected business errors and
 * at {@code ERROR} level for unexpected exceptions, always including the
 * full stack trace for the latter to aid log correlation.
 *
 * @author Angela-Maria Despotopoulou
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -------------------------------------------------------------------------
    // 400 Bad Request
    // -------------------------------------------------------------------------

    /**
     * Handles bean-validation constraint violations on request DTOs.
     *
     * <p>All field errors are collected and concatenated into a single
     * human-readable message so that the consumer sees every validation
     * failure in one response rather than one at a time.
     *
     * @param ex the {@link MethodArgumentNotValidException} raised by Spring MVC
     * @return a 400 response containing all field validation errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex) {

        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.warn("Validation failure: {}", message);

        return buildResponse(ApiErrorResponse.of(ErrorCode.VALIDATION_ERROR, message));
    }

    /**
     * Handles malformed or unparseable HTTP request bodies.
     *
     * <p>Triggered when the request body cannot be deserialised — for example,
     * invalid JSON syntax or a missing required content-type.
     *
     * @param ex the {@link HttpMessageNotReadableException} raised by Spring MVC
     * @return a 400 response indicating a malformed request
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleMessageNotReadable(
            HttpMessageNotReadableException ex) {

        log.warn("Malformed request body: {}", ex.getMessage());

        return buildResponse(ApiErrorResponse.of(
                ErrorCode.MALFORMED_REQUEST,
                "The request body could not be parsed. Please check the JSON syntax and content type."));
    }

    /**
     * Handles domain-level field and variable validation failures.
     *
     * <p>Triggered by {@link InvalidInputException} thrown from service or
     * validation logic for rules that go beyond bean-validation constraints.
     *
     * @param ex the {@link InvalidInputException} carrying the error detail
     * @return a 400 response with the domain validation message
     */
    @ExceptionHandler(InvalidInputException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidInput(
            InvalidInputException ex) {

        log.warn("Invalid input [{}]: {}", ex.getErrorCode(), ex.getMessage());

        return buildResponse(ApiErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // 404 Not Found
    // -------------------------------------------------------------------------

    /**
     * Handles missing resource conditions.
     *
     * <p>Triggered when a requested entity (e.g. a review sample by ID)
     * does not exist in the database.
     *
     * @param ex the {@link ResourceNotFoundException} carrying the error detail
     * @return a 404 response identifying the missing resource
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex) {

        log.warn("Resource not found [{}]: {}", ex.getErrorCode(), ex.getMessage());

        return buildResponse(ApiErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // 409 Conflict
    // -------------------------------------------------------------------------

    /**
     * Handles state and uniqueness conflict conditions.
     *
     * <p>Triggered by {@link ResourceConflictException} and its subclass
     * {@link ModelNotTrainedException}. Covers duplicate submissions,
     * untrained model access, and insufficient training data conditions.
     *
     * @param ex the {@link ResourceConflictException} carrying the error detail
     * @return a 409 response identifying the conflict type
     */
    @ExceptionHandler(ResourceConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleResourceConflict(
            ResourceConflictException ex) {

        log.warn("Resource conflict [{}]: {}", ex.getErrorCode(), ex.getMessage());

        return buildResponse(ApiErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // 502 Bad Gateway
    // -------------------------------------------------------------------------

    /**
     * Handles Claude AI oracle communication failures.
     *
     * <p>Triggered when the outbound Claude API call fails due to a network
     * error, timeout, or an unparseable response from the Anthropic API.
     *
     * @param ex the {@link ClaudeApiException} carrying the failure detail
     * @return a 502 response indicating an upstream integration failure
     */
    @ExceptionHandler(ClaudeApiException.class)
    public ResponseEntity<ApiErrorResponse> handleClaudeApiException(
            ClaudeApiException ex) {

        log.warn("Claude API failure [{}]: {}", ex.getErrorCode(), ex.getMessage());

        return buildResponse(ApiErrorResponse.of(ex.getErrorCode(), ex.getMessage()));
    }

    // -------------------------------------------------------------------------
    // 500 Internal Server Error — catch-all
    // -------------------------------------------------------------------------

    /**
     * Catch-all handler for any unexpected exception not covered above.
     *
     * <p>Logs at {@code ERROR} level with the full stack trace to ensure
     * unexpected failures are always visible in the application logs.
     * The response message is intentionally generic — internal details
     * are never exposed to API consumers.
     *
     * @param ex the unexpected {@link Exception}
     * @return a 500 response with a generic internal error message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception ex) {

        log.error("Unexpected error: {}", ex.getMessage(), ex);

        return buildResponse(ApiErrorResponse.of(
                ErrorCode.INTERNAL_ERROR,
                "An unexpected error occurred. Please try again later or contact support."));
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Wraps an {@link ApiErrorResponse} in a {@link ResponseEntity} using the
     * HTTP status derived from {@link ErrorCode#getHttpStatus()}.
     *
     * <p>This ensures the HTTP response status always matches the
     * {@code httpStatus} field in the JSON body — they are never out of sync.
     *
     * @param response the fully populated {@link ApiErrorResponse} to wrap
     * @return a {@link ResponseEntity} with the matching HTTP status
     */
    private ResponseEntity<ApiErrorResponse> buildResponse(ApiErrorResponse response) {
        return ResponseEntity
                .status(response.getErrorCode().getHttpStatus())
                .body(response);
    }
}
