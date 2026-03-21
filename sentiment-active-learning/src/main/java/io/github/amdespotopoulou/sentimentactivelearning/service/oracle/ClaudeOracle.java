package io.github.amdespotopoulou.sentimentactivelearning.service.oracle;

import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.SentimentLabel;
import io.github.amdespotopoulou.sentimentactivelearning.exception.ClaudeApiException;

/**
 * Contract for a labelling oracle that assigns a sentiment label to a movie
 * review on behalf of the active-learning pipeline.
 *
 * <p>The oracle is consulted by the active-learning pipeline when the Naive
 * Bayes classifier's confidence falls below the configured uncertainty
 * threshold. The oracle is expected to return a definitive
 * {@link SentimentLabel} for the given review text.
 *
 * <h2>Design rationale</h2>
 * <p>Declaring the oracle as an interface rather than a concrete class
 * serves two purposes:
 * <ul>
 *   <li><b>Testability</b> — the active-learning pipeline can be unit-tested
 *       without a real API key by injecting a stub implementation that returns
 *       hardcoded labels.</li>
 *   <li><b>Replaceability</b> — the Claude AI implementation can be swapped
 *       for any other oracle (OpenAI GPT, a human review queue, a rules-based
 *       classifier) without touching any service or controller code.</li>
 * </ul>
 *
 * <p>The current production implementation is
 * {@link ClaudeOracleService}, which delegates to the Anthropic Claude
 * Messages API.
 *
 * @author Angela-Maria Despotopoulou
 */
public interface ClaudeOracle {

    /**
     * Assigns a sentiment label to the given movie review text.
     *
     * <p>Implementations must return exactly one of the values defined in
     * {@link SentimentLabel} — {@code POSITIVE} or {@code NEGATIVE} — and
     * must never return {@code null}.
     *
     * @param reviewText the raw movie review text to label; must not be
     *                   {@code null} or blank
     * @return the {@link SentimentLabel} assigned by the oracle;
     *         never {@code null}
     * @throws ClaudeApiException if the oracle is unavailable or returns
     *         an unusable response
     */
    SentimentLabel label(String reviewText);
}