package io.github.amdespotopoulou.sentimentactivelearning.service.oracle;

import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.ErrorCode;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.SentimentLabel;
import io.github.amdespotopoulou.sentimentactivelearning.exception.ClaudeApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Service responsible for consulting the Claude AI oracle to assign a
 * sentiment label to an uncertain movie review.
 *
 * <p>This service is invoked by the active-learning pipeline when the Naive
 * Bayes classifier's confidence falls below the configured uncertainty
 * threshold. It sends the review text to the Anthropic Claude API and parses
 * the single-word response ({@code POSITIVE} or {@code NEGATIVE}) into a
 * {@link SentimentLabel}.
 *
 * <h2>Error handling</h2>
 * <ul>
 *   <li>Network errors, connection timeouts, and non-2xx HTTP responses from
 *       the Anthropic API throw a {@link ClaudeApiException} with error code
 *       {@link ErrorCode#CLAUDE_API_UNAVAILABLE}.</li>
 *   <li>A response that cannot be parsed into a valid {@link SentimentLabel}
 *       (e.g. Claude responded with an explanation instead of one word) throws
 *       a {@link ClaudeApiException} with error code
 *       {@link ErrorCode#CLAUDE_RESPONSE_INVALID}.</li>
 * </ul>
 *
 * <h2>Cost management</h2>
 * <p>The request is assembled by {@link ClaudeRequestMapper} with a
 * deliberately low {@code max_tokens} value (default: 16) to enforce brevity
 * and minimise API cost. Claude is prompted to respond with exactly one word.
 *
 * @author Angela-Maria Despotopoulou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ClaudeOracleService {

    /**
     * Pre-configured HTTP client for the Claude API.
     * Base URL, authentication headers, and timeouts are set in
     * {@link io.github.amdespotopoulou.sentimentactivelearning.config.ClaudeClientConfig}.
     */
    private final @Qualifier("claudeRestClient") RestClient restClient;

    /**
     * Assembles Claude API request payloads from raw review text.
     */
    private final ClaudeRequestMapper requestMapper;

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Consults the Claude AI oracle to obtain a sentiment label for the given
     * review text.
     *
     * <p>The full call sequence is:
     * <ol>
     *   <li>Assemble the request payload via {@link ClaudeRequestMapper}.</li>
     *   <li>POST the payload to the Anthropic Messages API.</li>
     *   <li>Extract the first text content block from the response.</li>
     *   <li>Parse the text into a {@link SentimentLabel}.</li>
     * </ol>
     *
     * @param reviewText the raw movie review text to label; must not be
     *                   {@code null} or blank
     * @return the {@link SentimentLabel} assigned by Claude —
     *         {@code POSITIVE} or {@code NEGATIVE}
     * @throws ClaudeApiException with {@link ErrorCode#CLAUDE_API_UNAVAILABLE}
     *         if the HTTP call fails due to a network error, timeout, or
     *         non-2xx response from the Anthropic API
     * @throws ClaudeApiException with {@link ErrorCode#CLAUDE_RESPONSE_INVALID}
     *         if the response body cannot be parsed into a valid
     *         {@link SentimentLabel}
     */
    public SentimentLabel label(String reviewText) {
        log.debug("Consulting Claude oracle for review of length: {}", reviewText.length());

        ClaudeApiRequest request = requestMapper.buildRequest(reviewText);
        ClaudeApiResponse response = callClaudeApi(request);
        return parseLabel(response, reviewText);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Sends the assembled request to the Anthropic Claude API and returns the
     * parsed response.
     *
     * <p>Wraps all {@link RestClientException} subtypes in a
     * {@link ClaudeApiException} with {@link ErrorCode#CLAUDE_API_UNAVAILABLE},
     * preserving the original exception as the cause for log correlation.
     *
     * @param request the fully assembled request payload
     * @return the parsed {@link ClaudeApiResponse}
     * @throws ClaudeApiException if the HTTP call fails for any reason
     */
    private ClaudeApiResponse callClaudeApi(ClaudeApiRequest request) {
        try {
            ClaudeApiResponse response = restClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(ClaudeApiResponse.class);

            if (response == null) {
                throw new ClaudeApiException(
                        ErrorCode.CLAUDE_API_UNAVAILABLE,
                        "Claude API returned a null response body.");
            }

            log.debug("Claude API call successful.");
            return response;

        } catch (RestClientException ex) {
            log.warn("Claude API call failed: {}", ex.getMessage());
            throw new ClaudeApiException(
                    ErrorCode.CLAUDE_API_UNAVAILABLE,
                    "Claude API call failed: " + ex.getMessage(),
                    ex);
        }
    }

    /**
     * Extracts and parses the sentiment label from the Claude API response.
     *
     * <p>Expects the first content block to contain exactly {@code "POSITIVE"}
     * or {@code "NEGATIVE"} (case-insensitive, trimmed). Any other value
     * triggers a {@link ClaudeApiException} with
     * {@link ErrorCode#CLAUDE_RESPONSE_INVALID}.
     *
     * @param response   the response received from the Claude API
     * @param reviewText the original review text, used for error context
     * @return the parsed {@link SentimentLabel}
     * @throws ClaudeApiException if the response is empty, missing content
     *         blocks, or contains an unrecognised label value
     */
    private SentimentLabel parseLabel(ClaudeApiResponse response, String reviewText) {
        if (response.getContent() == null || response.getContent().isEmpty()) {
            throw new ClaudeApiException(
                    ErrorCode.CLAUDE_RESPONSE_INVALID,
                    "Claude API response contained no content blocks.");
        }

        String rawText = response.getContent().get(0).getText();

        if (rawText == null || rawText.isBlank()) {
            throw new ClaudeApiException(
                    ErrorCode.CLAUDE_RESPONSE_INVALID,
                    "Claude API response contained an empty text block.");
        }

        String normalised = rawText.trim().toUpperCase();

        try {
            SentimentLabel label = SentimentLabel.valueOf(normalised);
            log.info("Claude oracle assigned label [{}] to review of length: {}",
                    label, reviewText.length());
            return label;

        } catch (IllegalArgumentException ex) {
            log.warn("Claude returned unrecognised label value: '{}' for review of length: {}",
                    rawText.trim(), reviewText.length());
            throw new ClaudeApiException(
                    ErrorCode.CLAUDE_RESPONSE_INVALID,
                    "Claude returned an unrecognised sentiment label: '" + rawText.trim() +
                    "'. Expected POSITIVE or NEGATIVE.");
        }
    }
}
