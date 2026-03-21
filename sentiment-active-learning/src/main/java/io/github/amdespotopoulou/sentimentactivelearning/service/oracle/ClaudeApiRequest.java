package io.github.amdespotopoulou.sentimentactivelearning.service.oracle;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.util.List;

/**
 * Internal DTO representing the HTTP request body sent to the Anthropic
 * Claude Messages API.
 *
 * <p>This class is not part of the public API contract — it is an internal
 * transport object used exclusively by {@link ClaudeOracleService} to
 * serialise the outbound request payload. It is intentionally package-private
 * to enforce this boundary.
 *
 * <h2>Serialised JSON shape</h2>
 * <pre>{@code
 * {
 *   "model"      : "claude-sonnet-4-20250514",
 *   "max_tokens" : 16,
 *   "system"     : "You are a sentiment classifier...",
 *   "messages"   : [
 *     { "role": "user", "content": "Review: This movie was fantastic!" }
 *   ]
 * }
 * }</pre>
 *
 * @author Angela-Maria Despotopoulou
 */
@Value
@Builder
class ClaudeApiRequest {

    /**
     * The Claude model identifier to use for this request.
     * Sourced from {@link io.github.amdespotopoulou.sentimentactivelearning.config.ClaudeProps}
     * ({@code claude.api.model}).
     */
    @JsonProperty("model")
    String model;

    /**
     * Maximum number of tokens Claude may return.
     * Kept low — we only need a single POSITIVE or NEGATIVE token.
     * Sourced from {@link io.github.amdespotopoulou.sentimentactivelearning.config.ClaudeProps}
     * ({@code claude.api.max-tokens}).
     */
    @JsonProperty("max_tokens")
    int maxTokens;

    /**
     * The system prompt that instructs Claude to act as a sentiment classifier
     * and respond with exactly one word.
     */
    @JsonProperty("system")
    String system;

    /**
     * The list of conversation messages. For labelling requests this always
     * contains exactly one user message carrying the review text.
     */
    @JsonProperty("messages")
    List<Message> messages;

    // -------------------------------------------------------------------------
    // Nested message type
    // -------------------------------------------------------------------------

    /**
     * A single message in the Claude conversation turn.
     *
     * @author Angela-Maria Despotopoulou
     */
    @Value
    @Builder
    static class Message {

        /**
         * The role of the message author.
         * Always {@code "user"} for labelling requests.
         */
        @JsonProperty("role")
        String role;

        /**
         * The text content of the message — the movie review to be labelled,
         * formatted with a brief prefix for clarity.
         */
        @JsonProperty("content")
        String content;
    }
}