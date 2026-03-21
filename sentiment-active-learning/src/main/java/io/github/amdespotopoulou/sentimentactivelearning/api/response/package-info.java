/**
 * API response envelope types for the Sentiment Active Learning application.
 *
 * <p>This package contains the canonical error envelope returned for every
 * non-2xx API response. All error paths — bean-validation failures, business-rule
 * violations, resource conflicts, and unexpected exceptions — produce exactly
 * the same JSON structure, so consumers can always locate error information in
 * the same fields regardless of what went wrong.
 *
 * <h2>JSON envelope shape</h2>
 * <pre>{@code
 * {
 *   "httpStatus"     : 404,
 *   "httpStatusText" : "Not Found",
 *   "errorCode"      : "REVIEW_NOT_FOUND",
 *   "message"        : "Review sample with ID 42 was not found."
 * }
 * }</pre>
 *
 * <p>The {@code errorCode} field always contains a value from
 * {@link io.github.amdespotopoulou.sentimentactivelearning.commons.enums.ErrorCode},
 * which is stable across releases and safe for programmatic branching.
 * The {@code message} field is human-readable and may contain dynamic context;
 * it must not be parsed programmatically.
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning.api.response;
