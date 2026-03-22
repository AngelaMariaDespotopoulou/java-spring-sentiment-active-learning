/**
 * Claude AI oracle integration for the Sentiment Active Learning application.
 *
 * <p>This package isolates all outbound communication with the Anthropic Claude
 * REST API. When the classifier is uncertain about a prediction, the active-learning
 * pipeline delegates to the oracle to obtain a authoritative sentiment label.
 *
 * <h2>Contents</h2>
 * <ul>
 *   <li>{@code ClaudeOracle} — technology-agnostic interface declaring the
 *       labelling oracle contract. The active-learning pipeline depends on this
 *       interface rather than the concrete implementation, enabling stub
 *       injection during unit testing without a real API key.</li>
 *   <li>{@code ClaudeOracleService} — production implementation of
 *       {@code ClaudeOracle}. Sends a review text to the Claude API,
 *       parses the {@code POSITIVE} / {@code NEGATIVE} label from the response,
 *       and returns it as a
 *       {@link io.github.amdespotopoulou.sentimentactivelearning.commons.enums.SentimentLabel}.
 *       Throws {@link io.github.amdespotopoulou.sentimentactivelearning.exception.ClaudeApiException}
 *       on network errors, timeouts, or unparseable responses.</li>
 *   <li>{@code ClaudeRequestMapper} — assembles the Claude API request payload
 *       (model, max tokens, system prompt, user message) from a raw review string.
 *       Keeping prompt construction here ensures it can be unit-tested and
 *       evolved independently of the HTTP transport.</li>
 * </ul>
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning.service.oracle;