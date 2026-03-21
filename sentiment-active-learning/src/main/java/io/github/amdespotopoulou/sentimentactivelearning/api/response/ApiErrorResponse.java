package io.github.amdespotopoulou.sentimentactivelearning.api.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;
import org.springframework.http.HttpStatus;

/**
 * Canonical error envelope returned for every non-2xx API response.
 *
 * <p>All error paths — bean-validation failures, business-rule conflicts,
 * resource-not-found conditions, and unexpected exceptions — produce exactly
 * this structure. Consumers can always locate error information in the same
 * four fields, regardless of what went wrong.
 *
 * <h2>JSON shape</h2>
 * <pre>{@code
 * {
 *   "httpStatus"     : 404,
 *   "httpStatusText" : "Not Found",
 *   "errorCode"      : "REVIEW_NOT_FOUND",
 *   "message"        : "Review sample with ID 42 was not found."
 * }
 * }</pre>
 *
 * <ul>
 *   <li>{@code httpStatus} — the numeric HTTP status code; mirrors the actual
 *       HTTP response status so clients that cannot inspect headers still have
 *       the code in the body.</li>
 *   <li>{@code httpStatusText} — the standard HTTP reason phrase for that code
 *       (e.g. {@code "Not Found"}, {@code "Conflict"}), derived directly from
 *       Spring's {@link HttpStatus} enum. Intended for human readers and for
 *       clients that log or display raw response bodies.</li>
 *   <li>{@code errorCode} — a stable {@link ErrorCode} enum value for
 *       programmatic branching. Will not be renamed or removed across minor
 *       releases.</li>
 *   <li>{@code message} — a human-readable explanation. May include dynamic
 *       context (field names, IDs, etc.). Do not parse programmatically.</li>
 * </ul>
 *
 * <p>This class is immutable. Prefer {@link #of(ErrorCode, String)} for all
 * construction — it derives the HTTP status automatically from the error code,
 * so callers never need to supply it separately. Use the Lombok
 * {@code @Builder} only when assembling from multiple sources in tests.
 *
 * @author Angela-Maria Despotopoulou
 */
@Value
@Builder
@JsonPropertyOrder({"httpStatus", "httpStatusText", "errorCode", "message"})
@Schema(description = "Error envelope returned for all non-2xx API responses.")
public class ApiErrorResponse {

    /**
     * Numeric HTTP status code (e.g. {@code 400}, {@code 404}, {@code 500}).
     */
    @JsonProperty("httpStatus")
    @Schema(description = "Numeric HTTP status code.", example = "404")
    int httpStatus;

    /**
     * Standard HTTP reason phrase corresponding to {@link #httpStatus}.
     *
     * <p>Derived from {@link HttpStatus#getReasonPhrase()} — never hardcoded.
     * Examples: {@code "Not Found"}, {@code "Conflict"},
     * {@code "Internal Server Error"}.
     */
    @JsonProperty("httpStatusText")
    @Schema(description = "Standard HTTP reason phrase for the status code.",
            example = "Not Found")
    String httpStatusText;

    /**
     * Machine-readable error identifier from {@link ErrorCode}.
     *
     * <p>Stable across releases; safe to use in conditional logic.
     * Each value maps to exactly one HTTP status code.
     */
    @JsonProperty("errorCode")
    @Schema(description = "Stable machine-readable error code.",
            example = "REVIEW_NOT_FOUND")
    ErrorCode errorCode;

    /**
     * Human-readable description of the error.
     *
     * <p>May contain dynamic context such as field names, entity IDs, or
     * threshold values. Do not parse programmatically — use {@link #errorCode}
     * for branching logic instead.
     */
    @JsonProperty("message")
    @Schema(
            description = "Human-readable error description. May include dynamic context.",
            example = "Review sample with ID 42 was not found."
    )
    String message;

    // -------------------------------------------------------------------------
    // Factory helper
    // -------------------------------------------------------------------------

    /**
     * Convenience factory — the preferred construction path throughout the application.
     *
     * <p>The {@code httpStatus} and {@code httpStatusText} fields are populated
     * automatically from {@link ErrorCode#getHttpStatus()}, so callers never
     * need to supply the HTTP status separately. This keeps exception handlers
     * free of status-code switch statements.
     *
     * @param errorCode the machine-readable {@link ErrorCode} identifying the
     *                  error condition; must not be {@code null}
     * @param message   the human-readable description of what went wrong;
     *                  must not be {@code null} or blank
     * @return a fully populated, immutable {@code ApiErrorResponse}
     */
    public static ApiErrorResponse of(ErrorCode errorCode, String message) {
        HttpStatus status = errorCode.getHttpStatus();
        return ApiErrorResponse.builder()
                .httpStatus(status.value())
                .httpStatusText(status.getReasonPhrase())
                .errorCode(errorCode)
                .message(message)
                .build();
    }
}