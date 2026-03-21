package io.github.amdespotopoulou.sentimentactivelearning.service.oracle;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Value;

import java.util.List;

/**
 * Internal DTO representing the HTTP response body received from the
 * Anthropic Claude Messages API.
 *
 * <p>Only the fields required to extract the sentiment label are mapped.
 * All other fields in the Anthropic response are silently ignored via
 * {@link JsonIgnoreProperties#ignoreUnknown()}, making this DTO resilient
 * to future additions to the Anthropic API response schema.
 *
 * <p>This class is not part of the public API contract — it is an internal
 * transport object used exclusively by {@link ClaudeOracleService}.
 *
 * <h2>Expected JSON shape (relevant fields only)</h2>
 * <pre>{@code
 * {
 *   "content": [
 *     { "type": "text", "text": "POSITIVE" }
 *   ]
 * }
 * }</pre>
 *
 * @author Angela-Maria Despotopoulou
 */
@Value
@JsonIgnoreProperties(ignoreUnknown = true)
class ClaudeApiResponse {

    /**
     * The list of content blocks returned by Claude.
     * For our labelling requests this always contains exactly one text block.
     */
    @JsonProperty("content")
    List<ContentBlock> content;

    // -------------------------------------------------------------------------
    // Nested content block type
    // -------------------------------------------------------------------------

    /**
     * A single content block in the Claude response.
     *
     * @author Angela-Maria Despotopoulou
     */
    @Value
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ContentBlock {

        /**
         * The type of this content block.
         * Expected value: {@code "text"} for our labelling responses.
         */
        @JsonProperty("type")
        String type;

        /**
         * The text content of this block — expected to be exactly
         * {@code "POSITIVE"} or {@code "NEGATIVE"}.
         */
        @JsonProperty("text")
        String text;
    }
}
