package io.github.amdespotopoulou.sentimentactivelearning.exception;

import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.ErrorCode;
import lombok.Getter;

/**
 * Thrown when communication with the Claude AI oracle fails or produces an
 * unusable response.
 *
 * <p>Maps to HTTP 502 Bad Gateway. Two distinct failure modes are covered:
 * <ul>
 *   <li>{@link ErrorCode#CLAUDE_API_UNAVAILABLE} — the HTTP call to the
 *       Anthropic API failed due to a network error, connection timeout,
 *       read timeout, or a non-2xx HTTP response from the upstream server.</li>
 *   <li>{@link ErrorCode#CLAUDE_RESPONSE_INVALID} — the API call succeeded
 *       but the response body could not be parsed into a valid
 *       {@link io.github.amdespotopoulou.sentimentactivelearning.commons.enums.SentimentLabel}.
 *       This may indicate a prompt engineering issue or unexpected model
 *       behaviour.</li>
 * </ul>
 *
 * <p>The original cause is always preserved via {@link #getCause()} to aid
 * debugging and log correlation.
 *
 * <p>Example usage:
 * <pre>{@code
 * try {
 *     return restClient.post()...retrieve()...body(ClaudeResponse.class);
 * } catch (RestClientException e) {
 *     throw new ClaudeApiException(
 *             ErrorCode.CLAUDE_API_UNAVAILABLE,
 *             "Claude API call failed: " + e.getMessage(), e);
 * }
 * }</pre>
 *
 * @author Angela-Maria Despotopoulou
 */
@Getter
public class ClaudeApiException extends RuntimeException {

    /**
     * The machine-readable error code identifying the Claude failure mode.
     */
    private final ErrorCode errorCode;

    /**
     * Constructs a {@code ClaudeApiException} without a root cause.
     *
     * <p>Use this constructor when the failure is detected programmatically
     * (e.g. an unparseable response) rather than via a caught exception.
     *
     * @param errorCode the {@link ErrorCode} identifying the failure mode;
     *                  must not be {@code null}
     * @param message   a human-readable description of what went wrong;
     *                  must not be {@code null} or blank
     */
    public ClaudeApiException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    /**
     * Constructs a {@code ClaudeApiException} with a root cause.
     *
     * <p>Use this constructor when wrapping a caught transport-level exception
     * (e.g. {@link org.springframework.web.client.RestClientException}) so that
     * the original stack trace is preserved for log correlation.
     *
     * @param errorCode the {@link ErrorCode} identifying the failure mode;
     *                  must not be {@code null}
     * @param message   a human-readable description of what went wrong;
     *                  must not be {@code null} or blank
     * @param cause     the underlying exception that triggered this failure;
     *                  may be {@code null} if no cause is available
     */
    public ClaudeApiException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }
}
