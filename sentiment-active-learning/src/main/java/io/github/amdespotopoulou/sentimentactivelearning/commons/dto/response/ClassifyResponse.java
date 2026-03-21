package io.github.amdespotopoulou.sentimentactivelearning.commons.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.LabelSource;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.SentimentLabel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * Outbound DTO carrying the result of a sentiment classification operation.
 *
 * <p>Returned by:
 * <ul>
 *   <li>POST {@code /api/reviews} — after a new review is submitted and
 *       classified by the active-learning pipeline.</li>
 * </ul>
 *
 * <h2>Interpretation guide</h2>
 * <ul>
 *   <li>If {@code uncertain} is {@code false} — the model was confident.
 *       The {@code label} is the model's own prediction.</li>
 *   <li>If {@code uncertain} is {@code true} and {@code labelSource} is
 *       {@code CLAUDE} — the model was uncertain and the Claude AI oracle
 *       was consulted. The {@code label} is Claude's answer.</li>
 *   <li>If {@code uncertain} is {@code true} and {@code labelSource} is
 *       {@code null} — the model was uncertain but the Claude oracle call
 *       failed or is pending. The review is queued for labelling.</li>
 * </ul>
 *
 * @author Angela-Maria Despotopoulou
 */
@Value
@Builder
@AllArgsConstructor
@JsonPropertyOrder({"id", "reviewText", "label", "labelSource", "confidenceScore", "uncertain"})
@Schema(description = "Result of a sentiment classification operation.")
public class ClassifyResponse {

    /**
     * The database-assigned identifier of the review sample that was
     * classified or created during this operation.
     */
    @JsonProperty("id")
    @Schema(description = "Identifier of the classified review sample.", example = "42")
    Long id;

    /**
     * The review text that was classified.
     */
    @JsonProperty("reviewText")
    @Schema(
            description = "The review text that was submitted for classification.",
            example = "An absolute masterpiece. The performances were outstanding."
    )
    String reviewText;

    /**
     * The sentiment label assigned to this review.
     *
     * <p>May be {@code null} if the model was uncertain and the Claude oracle
     * call is still pending or failed.
     */
    @JsonProperty("label")
    @Schema(
            description = "The assigned sentiment label. Null if classification is pending.",
            example = "POSITIVE",
            nullable = true
    )
    SentimentLabel label;

    /**
     * The source of the assigned label — whether it came from the model
     * itself or was assigned by the Claude AI oracle.
     *
     * <p>May be {@code null} if labelling is still pending.
     */
    @JsonProperty("labelSource")
    @Schema(
            description = "Who assigned the label: SEED, MANUAL, or CLAUDE. Null if pending.",
            example = "CLAUDE",
            nullable = true
    )
    LabelSource labelSource;

    /**
     * The model's confidence score for this prediction, between 0.0 and 1.0.
     *
     * <p>Represents the probability the model assigned to the predicted label.
     * A score below the configured uncertainty threshold indicates the model
     * was not confident enough to self-label and deferred to the Claude oracle.
     */
    @JsonProperty("confidenceScore")
    @Schema(
            description = "Model confidence score between 0.0 and 1.0. " +
                          "Below the uncertainty threshold the oracle is consulted.",
            example = "0.87"
    )
    double confidenceScore;

    /**
     * Whether the model considered this prediction uncertain.
     *
     * <p>{@code true} if the confidence score fell below the configured
     * uncertainty threshold, meaning the Claude AI oracle was consulted
     * to assign the label. {@code false} if the model was confident in
     * its own prediction.
     */
    @JsonProperty("uncertain")
    @Schema(
            description = "True if the model was uncertain and the Claude oracle was consulted.",
            example = "false"
    )
    boolean uncertain;
}
