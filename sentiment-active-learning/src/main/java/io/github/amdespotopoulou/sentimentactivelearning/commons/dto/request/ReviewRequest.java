package io.github.amdespotopoulou.sentimentactivelearning.commons.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * Inbound DTO carrying a raw movie review text submitted for classification
 * or manual labelling via the REST API.
 *
 * <p>Used as the request body for:
 * <ul>
 *   <li>POST {@code /api/reviews} — submit a new review for sentiment
 *       classification by the active-learning pipeline.</li>
 *   <li>POST {@code /api/reviews/seed} — submit a pre-labelled review as
 *       seed training data.</li>
 * </ul>
 *
 * <p>All fields are validated at the controller boundary via
 * {@code @Valid}. Constraint violations produce a {@code 400 Bad Request}
 * response with an
 * {@link io.github.amdespotopoulou.sentimentactivelearning.api.response.ApiErrorResponse}
 * envelope and error code
 * {@link io.github.amdespotopoulou.sentimentactivelearning.commons.enums.ErrorCode#VALIDATION_ERROR}.
 *
 * @author Angela-Maria Despotopoulou
 */
@Value
@Builder
@AllArgsConstructor
@Schema(description = "Request body for submitting a movie review for classification or labelling.")
public class ReviewRequest {

    /**
     * The raw text of the movie review to be classified or labelled.
     *
     * <p>Submitted as-is; no normalisation is applied before storage.
     * Must not be blank and must not exceed 5000 characters, consistent
     * with the database column length on
     * {@link io.github.amdespotopoulou.sentimentactivelearning.persistence.entity.ReviewSample}
     * ({@code review_text} column, max 5000 characters).
     */
    @NotBlank(message = "Review text must not be blank")
    @Size(max = 5000, message = "Review text must not exceed 5000 characters")
    @JsonProperty("reviewText")
    @Schema(
            description = "The raw text of the movie review to classify or label.",
            example = "An absolute masterpiece. The performances were outstanding and the storyline kept me gripped from start to finish.",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    String reviewText;
}