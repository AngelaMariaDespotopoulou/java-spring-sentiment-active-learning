package io.github.amdespotopoulou.sentimentactivelearning.commons.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Outbound DTO carrying a snapshot of the active-learning cycle's current
 * state and health.
 *
 * <p>Returned by:
 * <ul>
 *   <li>GET {@code /api/model/active-learning-status} — retrieve the current
 *       state of the active-learning cycle.</li>
 * </ul>
 *
 * <p>This response complements {@link ModelStatsResponse} by focusing on the
 * cycle dynamics rather than model quality — specifically, how far through the
 * current retrain batch the system is, how many cycles have completed, and
 * whether the system is currently in a healthy operating state.
 *
 * <h2>Reading the progress fields</h2>
 * <p>A consumer can derive the current cycle progress percentage as:
 * <pre>{@code
 * progressPercent = (claudeLabelledSinceLastRetrain / retrainBatchSize) * 100
 * }</pre>
 * This value is also pre-computed and returned directly as
 * {@code progressPercent} for convenience.
 *
 * @author Angela-Maria Despotopoulou
 */
@Value
@Builder
@AllArgsConstructor
@JsonPropertyOrder({
        "cycleActive", "totalRetrainingRuns", "lastRetrainedAt",
        "claudeLabelledSinceLastRetrain", "retrainBatchSize", "progressPercent",
        "totalUnlabelled", "modelStoragePath", "modelFileExists"
})
@Schema(description = "Snapshot of the active-learning cycle state and health.")
public class ActiveLearningStatusResponse {

    /**
     * Whether the active-learning cycle is currently operational — i.e. the
     * model has been trained at least once and is accepting classifications.
     */
    @JsonProperty("cycleActive")
    @Schema(
            description = "True if the active-learning cycle is operational " +
                          "and the model is trained and available.",
            example = "true"
    )
    boolean cycleActive;

    /**
     * The total number of completed retraining runs since the application
     * was first started.
     */
    @JsonProperty("totalRetrainingRuns")
    @Schema(
            description = "Total number of completed retraining runs since first startup.",
            example = "4"
    )
    int totalRetrainingRuns;

    /**
     * The timestamp of the most recent retraining run, or {@code null} if
     * no retraining has occurred yet.
     */
    @JsonProperty("lastRetrainedAt")
    @Schema(
            description = "Timestamp of the most recent retraining run. Null if never retrained.",
            example = "2026-03-21T18:00:00",
            nullable = true
    )
    LocalDateTime lastRetrainedAt;

    /**
     * The number of new Claude-labelled samples accumulated since the most
     * recent retraining run. This counter resets to zero after each retrain.
     */
    @JsonProperty("claudeLabelledSinceLastRetrain")
    @Schema(
            description = "New Claude-labelled samples since the last retraining run. " +
                          "Resets to zero after each retrain.",
            example = "7"
    )
    int claudeLabelledSinceLastRetrain;

    /**
     * The configured retrain batch size — the target number of new
     * Claude-labelled samples required to trigger the next automatic retrain.
     */
    @JsonProperty("retrainBatchSize")
    @Schema(
            description = "Configured batch size threshold that triggers an automatic retrain.",
            example = "10"
    )
    int retrainBatchSize;

    /**
     * Pre-computed progress percentage towards the next automatic retrain,
     * between 0 and 100.
     *
     * <p>Computed as:
     * {@code (claudeLabelledSinceLastRetrain / retrainBatchSize) * 100},
     * capped at 100.
     */
    @JsonProperty("progressPercent")
    @Schema(
            description = "Progress towards next automatic retrain, between 0 and 100.",
            example = "70"
    )
    int progressPercent;

    /**
     * The total number of review samples currently in the database that have
     * not yet been assigned a label — either awaiting manual labelling or
     * queued for Claude oracle labelling.
     */
    @JsonProperty("totalUnlabelled")
    @Schema(
            description = "Total review samples currently awaiting labelling.",
            example = "12"
    )
    long totalUnlabelled;

    /**
     * The configured path where the serialised model file is stored on disk.
     * Useful for operators verifying that the model persistence path is
     * correctly configured.
     */
    @JsonProperty("modelStoragePath")
    @Schema(
            description = "Configured path where the serialised model file is stored.",
            example = "/var/data/sentiment/sentiment-model.ser"
    )
    String modelStoragePath;

    /**
     * Whether a serialised model file currently exists at
     * {@link #modelStoragePath}.
     *
     * <p>{@code false} immediately after a fresh deployment before the first
     * training run, or if the model file was manually deleted or the volume
     * was not correctly mounted.
     */
    @JsonProperty("modelFileExists")
    @Schema(
            description = "True if a serialised model file exists at the configured storage path.",
            example = "true"
    )
    boolean modelFileExists;
}
