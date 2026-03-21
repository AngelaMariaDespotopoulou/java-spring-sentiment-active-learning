package io.github.amdespotopoulou.sentimentactivelearning.commons.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.request.TrainingRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Outbound DTO carrying the result of a completed model training run.
 *
 * <p>Returned by:
 * <ul>
 *   <li>POST {@code /api/model/train} — after a manual or automatic training
 *       run completes successfully.</li>
 * </ul>
 *
 * <p>Provides immediate feedback to the caller about what was trained, how
 * the model performed on the hold-out evaluation set, and whether the trained
 * model was successfully serialised to disk. This allows the caller to assess
 * model quality and persistence health in a single response without needing
 * to follow up with a separate stats call.
 *
 * @author Angela-Maria Despotopoulou
 */
@Value
@Builder
@AllArgsConstructor
@JsonPropertyOrder({
        "trainedAt", "samplesUsed", "positiveCount", "negativeCount",
        "accuracyScore", "precisionScore", "recallScore",
        "modelSavedToDisk", "modelStoragePath", "note"
})
@Schema(description = "Result of a completed model training run.")
public class TrainingResponse {

    /**
     * The timestamp at which this training run completed.
     */
    @JsonProperty("trainedAt")
    @Schema(description = "Timestamp when the training run completed.", example = "2026-03-21T18:00:00")
    LocalDateTime trainedAt;

    /**
     * The total number of labelled samples used in this training run.
     */
    @JsonProperty("samplesUsed")
    @Schema(description = "Total labelled samples used in this training run.", example = "165")
    long samplesUsed;

    /**
     * The number of {@code POSITIVE} samples in the training corpus used
     * for this run.
     */
    @JsonProperty("positiveCount")
    @Schema(description = "Number of POSITIVE samples used.", example = "89")
    long positiveCount;

    /**
     * The number of {@code NEGATIVE} samples in the training corpus used
     * for this run.
     */
    @JsonProperty("negativeCount")
    @Schema(description = "Number of NEGATIVE samples used.", example = "76")
    long negativeCount;

    /**
     * The accuracy of the trained model on the hold-out evaluation set,
     * expressed as a value between 0.0 and 1.0.
     *
     * <p>Accuracy = correct predictions / total predictions on the hold-out set.
     */
    @JsonProperty("accuracyScore")
    @Schema(
            description = "Model accuracy on the hold-out evaluation set. Between 0.0 and 1.0.",
            example = "0.91"
    )
    double accuracyScore;

    /**
     * The precision of the trained model on the hold-out evaluation set,
     * expressed as a value between 0.0 and 1.0.
     *
     * <p>Precision = true positives / (true positives + false positives).
     * High precision means the model rarely labels a negative review as positive.
     */
    @JsonProperty("precisionScore")
    @Schema(
            description = "Model precision on the hold-out set. Between 0.0 and 1.0.",
            example = "0.89"
    )
    double precisionScore;

    /**
     * The recall of the trained model on the hold-out evaluation set,
     * expressed as a value between 0.0 and 1.0.
     *
     * <p>Recall = true positives / (true positives + false negatives).
     * High recall means the model rarely misses a genuinely positive review.
     */
    @JsonProperty("recallScore")
    @Schema(
            description = "Model recall on the hold-out set. Between 0.0 and 1.0.",
            example = "0.93"
    )
    double recallScore;

    /**
     * Whether the trained model was successfully serialised to disk after
     * this training run.
     *
     * <p>{@code false} if the serialisation step failed — for example, due
     * to a file system permission error or a missing bind mount. The model
     * is still available in memory for classification, but will not survive
     * a container restart.
     */
    @JsonProperty("modelSavedToDisk")
    @Schema(
            description = "True if the model was successfully serialised to disk after training.",
            example = "true"
    )
    boolean modelSavedToDisk;

    /**
     * The path where the serialised model file was written, or {@code null}
     * if serialisation failed.
     */
    @JsonProperty("modelStoragePath")
    @Schema(
            description = "Path where the model was saved. Null if serialisation failed.",
            example = "/var/data/sentiment/sentiment-model.ser",
            nullable = true
    )
    String modelStoragePath;

    /**
     * The human-readable note supplied in the original
     * {@link TrainingRequest}, if any. Echoed back for audit convenience.
     * {@code null} if no note was provided.
     */
    @JsonProperty("note")
    @Schema(
            description = "The audit note from the original training request, if provided.",
            example = "Manual retrain after bulk seed data import.",
            nullable = true
    )
    String note;
}
