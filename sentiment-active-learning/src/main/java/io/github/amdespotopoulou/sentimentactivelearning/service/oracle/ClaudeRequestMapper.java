package io.github.amdespotopoulou.sentimentactivelearning.service.oracle;

import io.github.amdespotopoulou.sentimentactivelearning.config.ClaudeProps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Assembles the {@link ClaudeApiRequest} payload sent to the Anthropic Claude
 * API for sentiment labelling.
 *
 * <p>This class is responsible solely for prompt construction — it knows how
 * to format a review text into the exact request structure that produces a
 * reliable {@code POSITIVE} or {@code NEGATIVE} single-token response from
 * Claude. Keeping prompt logic here ensures it can be unit-tested and evolved
 * independently of the HTTP transport in {@link ClaudeOracleService}.
 *
 * <h2>Prompt design</h2>
 * <p>The system prompt instructs Claude to:
 * <ul>
 *   <li>Act exclusively as a binary sentiment classifier.</li>
 *   <li>Respond with exactly one word — either {@code POSITIVE} or
 *       {@code NEGATIVE} — and nothing else.</li>
 *   <li>Base its decision solely on the review text provided.</li>
 * </ul>
 *
 * <p>The {@code max_tokens} value is kept deliberately low (default: 16) to
 * enforce brevity and reduce API cost — a single-word response never needs
 * more than a few tokens.
 *
 * @author Angela-Maria Despotopoulou
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ClaudeRequestMapper {

    /**
     * The system prompt sent with every labelling request.
     *
     * <p>Instructs Claude to act as a binary sentiment classifier and respond
     * with exactly one word. The prompt is intentionally terse to minimise
     * token usage while remaining unambiguous.
     */
    private static final String SYSTEM_PROMPT =
            "You are a binary sentiment classifier for movie reviews. " +
            "Your task is to classify the sentiment of the review provided by the user. " +
            "Respond with exactly one word — either POSITIVE or NEGATIVE — and nothing else. " +
            "Do not add punctuation, explanation, or any other text.";

    /**
     * The prefix added before the review text in the user message,
     * to clearly delimit the input for Claude.
     */
    private static final String REVIEW_PREFIX = "Review: ";

    /**
     * The role identifier for the user turn in the Claude conversation.
     */
    private static final String USER_ROLE = "user";

    /**
     * Configuration properties for the Claude API client.
     * Provides model identifier and max tokens.
     */
    private final ClaudeProps claudeProps;

    /**
     * Assembles a {@link ClaudeApiRequest} for labelling the given review text.
     *
     * <p>The assembled request contains:
     * <ul>
     *   <li>The model identifier from {@link ClaudeProps}.</li>
     *   <li>The max tokens limit from {@link ClaudeProps}.</li>
     *   <li>The fixed system prompt instructing Claude to respond with one word.</li>
     *   <li>A single user message containing the review text prefixed with
     *       {@value #REVIEW_PREFIX}.</li>
     * </ul>
     *
     * @param reviewText the raw movie review text to label; must not be
     *                   {@code null} or blank
     * @return a fully populated {@link ClaudeApiRequest} ready for
     *         serialisation and transmission
     */
    public ClaudeApiRequest buildRequest(String reviewText) {
        log.debug("Building Claude labelling request for review text of length: {}",
                reviewText.length());

        return ClaudeApiRequest.builder()
                .model(claudeProps.getModel())
                .maxTokens(claudeProps.getMaxTokens())
                .system(SYSTEM_PROMPT)
                .messages(List.of(
                        ClaudeApiRequest.Message.builder()
                                .role(USER_ROLE)
                                .content(REVIEW_PREFIX + reviewText)
                                .build()
                ))
                .build();
    }
}
