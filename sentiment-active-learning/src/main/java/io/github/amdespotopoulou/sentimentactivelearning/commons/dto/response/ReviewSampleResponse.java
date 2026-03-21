package io.github.amdespotopoulou.sentimentactivelearning.commons.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.LabelSource;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.SentimentLabel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Outbound DTO representing a single review sample as returned by the REST API.
 *
 * <p>Used as the response body for:
 * <ul>
 *   <li>POST {@code /api/reviews} — returns the newly created review sample.</li>
 *   <li>GET {@code /api/reviews/{id}} — returns a specific review sample.</li>
 *   <li>GET {@code /api/reviews} — returned as elements in the list response.</li>
 *   <li>PATCH {@code /api/reviews/{id}/label} — returns the updated sample.</li>
 *   <li>POST {@code /api/reviews/{id}/feedback} — returns the corrected sample.</li>
 * </ul>
 *
 * <p>Audit timestamps ({@code createdAt}, {@code updatedAt}) are included in
 * the response so consumers can display or sort by them, but they are never
 * accepted as input on any request DTO — they are owned exclusively by the
 * system.
 *
 * @author Angela-Maria Despotopoulou
 */
@Value
@Builder
@AllArgsConstructor
@JsonPropertyOrder({"id", "reviewText", "label", "labelSource", "createdAt", "updatedAt"})
@Schema(description = "A single review sample as stored in the system.")
public class ReviewSampleResponse {

    /**
     * The database-assigned unique identifier of the review sample.
     */
    @JsonProperty("id")
    @Schema(description = "Unique identifier of the review sample.", example = "42")
    Long id;

    /**
     * The raw text of the movie review.
     */
    @JsonProperty("reviewText")
    @Schema(
            description = "The raw text of the movie review.",
            example = "An absolute masterpiece. The performances were outstanding."
    )
    String reviewText;

    /**
     * The sentiment label assigned to this review, or {@code null} if the
     * review has not yet been labelled.
     */
    @JsonProperty("label")
    @Schema(
            description = "The assigned sentiment label. Null if not yet labelled.",
            example = "POSITIVE",
            nullable = true
    )
    SentimentLabel label;

    /**
     * The source of the sentiment label, or {@code null} if the review has
     * not yet been labelled.
     */
    @JsonProperty("labelSource")
    @Schema(
            description = "The origin of the label: SEED, MANUAL, or CLAUDE. Null if not yet labelled.",
            example = "CLAUDE",
            nullable = true
    )
    LabelSource labelSource;

    /**
     * The timestamp at which this record was first created.
     * Populated automatically by the system — never supplied by the client.
     */
    @JsonProperty("createdAt")
    @Schema(
            description = "Timestamp when the review sample was first created. System-managed.",
            example = "2026-03-21T17:45:00"
    )
    LocalDateTime createdAt;

    /**
     * The timestamp at which this record was most recently updated.
     * Populated automatically by the system — never supplied by the client.
     */
    @JsonProperty("updatedAt")
    @Schema(
            description = "Timestamp when the review sample was last updated. System-managed.",
            example = "2026-03-21T18:02:00"
    )
    LocalDateTime updatedAt;
}
