package io.github.amdespotopoulou.sentimentactivelearning.commons.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.SentimentLabel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * Inbound DTO for submitting a human correction to a model prediction.
 *
 * <p>Used as the request body for:
 * <ul>
 *   <li>POST {@code /api/reviews/{id}/feedback} — correct the sentiment label
 *       predicted by the model for a specific review sample.</li>
 * </ul>
 *
 * <p>This is a human-in-the-loop mechanism. When a user disagrees with the
 * model's prediction, they submit the correct label via this DTO. The
 * correction is stored as a
 * {@link io.github.amdespotopoulou.sentimentactivelearning.commons.enums.LabelSource#MANUAL}
 * label, overriding any previous label. Human corrections are the
 * highest-quality signal in the training corpus and feed directly into
 * the next retraining cycle.
 *
 * <p>All required fields are validated at the controller boundary via
 * {@code @Valid}. Constraint violations produce a {@code 400 Bad Request}
 * response with an
 * {@link io.github.amdespotopoulou.sentimentactivelearning.api.response.ApiErrorResponse}
 * envelope.
 *
 * @author Angela-Maria Despotopoulou
 */
@Value
@Builder
@AllArgsConstructor
@Schema(description = "Request body for submitting a human correction to a model prediction.")
public class PredictionFeedbackRequest {

    /**
     * The corrected sentiment label that the human operator believes is
     * correct for the review in question.
     *
     * <p>Must be one of the values defined in {@link SentimentLabel}:
     * {@code POSITIVE} or {@code NEGATIVE}. Must not be {@code null}.
     */
    @NotNull(message = "Corrected label must not be null. Accepted values: POSITIVE, NEGATIVE")
    @JsonProperty("correctedLabel")
    @Schema(
            description = "The correct sentiment label as determined by the human operator.",
            example = "NEGATIVE",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    SentimentLabel correctedLabel;

    /**
     * An optional human-readable note explaining why the model prediction
     * was incorrect.
     *
     * <p>Purely informational — logged for diagnostic purposes and not
     * persisted to the database. Useful for identifying systematic model
     * failure patterns during review. Maximum 500 characters.
     */
    @Size(max = 500, message = "Feedback note must not exceed 500 characters")
    @JsonProperty("feedbackNote")
    @Schema(
            description = "Optional note explaining why the model prediction was wrong. " +
                          "Logged for diagnostics. Not persisted. Max 500 characters.",
            example = "The model missed the sarcastic tone — this is clearly a negative review despite the positive vocabulary.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Builder.Default
    String feedbackNote = null;
}
