package io.github.amdespotopoulou.sentimentactivelearning.commons.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.SentimentLabel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * Inbound DTO for manually assigning a sentiment label to an existing,
 * unlabelled review sample.
 *
 * <p>Used as the request body for:
 * <ul>
 *   <li>PATCH {@code /api/reviews/{id}/label} — assign or correct the
 *       sentiment label of a specific review sample.</li>
 * </ul>
 *
 * <p>Manual labels are recorded with
 * {@link io.github.amdespotopoulou.sentimentactivelearning.commons.enums.LabelSource#MANUAL}
 * and are treated as the highest-quality signal in the training corpus.
 * A manual label may override an existing
 * {@link io.github.amdespotopoulou.sentimentactivelearning.commons.enums.LabelSource#CLAUDE}
 * label on the same review.
 *
 * <p>All fields are validated at the controller boundary via {@code @Valid}.
 * Constraint violations produce a {@code 400 Bad Request} response with an
 * {@link io.github.amdespotopoulou.sentimentactivelearning.api.response.ApiErrorResponse}
 * envelope.
 *
 * @author Angela-Maria Despotopoulou
 */
@Value
@Builder
@AllArgsConstructor
@Schema(description = "Request body for manually assigning a sentiment label to a review sample.")
public class LabelRequest {

    /**
     * The sentiment label to assign to the review sample.
     *
     * <p>Must be one of the values defined in {@link SentimentLabel}:
     * {@code POSITIVE} or {@code NEGATIVE}. A {@code null} value is
     * rejected with a {@code 400} validation error.
     */
    @NotNull(message = "Label must not be null. Accepted values: POSITIVE, NEGATIVE")
    @JsonProperty("label")
    @Schema(
            description = "The sentiment label to assign. Must be POSITIVE or NEGATIVE.",
            example = "POSITIVE",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    SentimentLabel label;
}
