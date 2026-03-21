package io.github.amdespotopoulou.sentimentactivelearning.commons.enums;

import org.springframework.http.HttpStatus;

/**
 * Stable, machine-readable identifiers for every error condition the API can produce.
 *
 * <p>Each constant in this enumeration maps to exactly one HTTP status code and
 * is embedded in the
 * {@link io.github.amdespotopoulou.sentimentactivelearning.api.response.ApiErrorResponse}
 * envelope returned for all non-2xx responses. Consumers may branch
 * programmatically on {@code errorCode} values; they are guaranteed not to be
 * renamed or removed across minor releases.
 *
 * <h2>HTTP status mapping</h2>
 * <p>Each constant carries its associated {@link HttpStatus} via
 * {@link #getHttpStatus()}, so the {@code GlobalExceptionHandler} can derive
 * the response status directly from the error code without a separate switch
 * statement.
 *
 * <h2>Swagger documentation</h2>
 * <p>Every controller endpoint documents its possible error codes using
 * {@code @ApiResponse} with {@code @ExampleObject}, showing the exact
 * {@link io.github.amdespotopoulou.sentimentactivelearning.api.response.ApiErrorResponse}
 * JSON shape for each code. The constant names here must match the
 * {@code "errorCode"} values shown in those examples.
 *
 * @author Angela-Maria Despotopoulou
 */
public enum ErrorCode {

    // -------------------------------------------------------------------------
    // 400 Bad Request — invalid input / field validation failures
    // -------------------------------------------------------------------------

    /**
     * A required request field is blank, null, or otherwise missing.
     * Triggered by bean-validation constraint violations on incoming DTOs.
     */
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),

    /**
     * A field value fails a domain-level validation rule that goes beyond
     * standard bean-validation constraints — for example, a confidence
     * threshold outside the 0.0–1.0 range, or a review text that contains
     * only whitespace after trimming.
     */
    INVALID_FIELD_VALUE(HttpStatus.BAD_REQUEST),

    /**
     * The request body could not be parsed or is structurally malformed
     * (e.g. invalid JSON, wrong content type).
     */
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST),

    // -------------------------------------------------------------------------
    // 404 Not Found
    // -------------------------------------------------------------------------

    /**
     * No review sample exists for the supplied identifier.
     */
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND),

    // -------------------------------------------------------------------------
    // 409 Conflict — business-rule violations and state conflicts
    // -------------------------------------------------------------------------

    /**
     * A review sample with the same text already exists in the database.
     * Duplicate submissions are rejected to keep the training corpus clean.
     */
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT),

    /**
     * A classification or evaluation was requested but the model has not
     * yet been trained. At least {@code active-learning.min-training-samples}
     * labelled samples must exist before the first training run can succeed.
     */
    MODEL_NOT_TRAINED(HttpStatus.CONFLICT),

    /**
     * A training run was requested but there are insufficient labelled samples
     * to produce a meaningful model. Collect more labelled data first.
     */
    INSUFFICIENT_TRAINING_DATA(HttpStatus.CONFLICT),

    // -------------------------------------------------------------------------
    // 502 Bad Gateway — upstream / integration failures
    // -------------------------------------------------------------------------

    /**
     * The Claude AI oracle call failed due to a network error, connection
     * timeout, or an HTTP error response from the Anthropic API.
     */
    CLAUDE_API_UNAVAILABLE(HttpStatus.BAD_GATEWAY),

    /**
     * The Claude AI oracle returned a response that could not be parsed into
     * a valid {@link SentimentLabel}. This may indicate a prompt engineering
     * issue or an unexpected model behaviour.
     */
    CLAUDE_RESPONSE_INVALID(HttpStatus.BAD_GATEWAY),

    // -------------------------------------------------------------------------
    // 500 Internal Server Error — unexpected failures
    // -------------------------------------------------------------------------

    /**
     * An unexpected error occurred that does not map to any of the above
     * categories. Inspect the application logs for the full stack trace.
     */
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    // -------------------------------------------------------------------------

    /**
     * The HTTP status code associated with this error condition.
     */
    private final HttpStatus httpStatus;

    /**
     * Constructs an {@code ErrorCode} bound to the given HTTP status.
     *
     * @param httpStatus the {@link HttpStatus} that should be returned
     *                   when this error code is used in a response
     */
    ErrorCode(HttpStatus httpStatus) {
        this.httpStatus = httpStatus;
    }

    /**
     * Returns the {@link HttpStatus} associated with this error code.
     *
     * <p>Used by {@code GlobalExceptionHandler} to derive the HTTP response
     * status directly from the error code, avoiding a separate mapping switch.
     *
     * @return the associated {@link HttpStatus}; never {@code null}
     */
    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
