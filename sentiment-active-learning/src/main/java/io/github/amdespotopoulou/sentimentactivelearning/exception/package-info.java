/**
 * Global exception handling and custom exception hierarchy for the
 * Sentiment Active Learning application.
 *
 * <p>All non-2xx API responses are produced exclusively by
 * {@code GlobalExceptionHandler}, which maps each exception type to a specific
 * HTTP status and wraps the error in the canonical
 * {@link io.github.amdespotopoulou.sentimentactivelearning.api.response.ApiErrorResponse}
 * envelope. Consumers can always find error information in the same four
 * fields regardless of what went wrong.
 *
 * <h2>Exception hierarchy</h2>
 * <ul>
 *   <li>{@code ResourceNotFoundException} — 404 Not Found. Thrown when a
 *       requested resource (e.g. a review sample by ID) does not exist.</li>
 *   <li>{@code ResourceConflictException} — 409 Conflict. Thrown when an
 *       operation would violate a uniqueness or state constraint (e.g.
 *       submitting a duplicate review).</li>
 *   <li>{@code InvalidInputException} — 400 Bad Request. Thrown when a
 *       field or variable fails a domain-level validation rule that goes
 *       beyond bean-validation constraints (e.g. a confidence score outside
 *       the 0.0–1.0 range).</li>
 *   <li>{@code ModelNotTrainedException} — 409 Conflict. Thrown when a
 *       classification or evaluation is requested before the model has been
 *       trained for the first time.</li>
 *   <li>{@code ClaudeApiException} — 502 Bad Gateway. Thrown when the
 *       Claude AI oracle call fails due to a network error, timeout, or
 *       an unexpected response from the Anthropic API.</li>
 * </ul>
 *
 * <p>Bean-validation constraint violations ({@code @NotBlank}, {@code @Size},
 * etc.) are also caught by {@code GlobalExceptionHandler} and translated into
 * 400 responses using the same envelope.
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning.exception;
