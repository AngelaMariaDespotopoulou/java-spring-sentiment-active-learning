package io.github.amdespotopoulou.sentimentactivelearning.commons.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * Outbound DTO carrying a snapshot of the trained model's health and
 * training corpus statistics.
 *
 * <p>Returned by:
 * <ul>
 *   <li>GET {@code /api/model/stats} — retrieve current model metrics.</li>
 *   <li>POST {@code /api/model/train} — returned after a training run
 *       completes, reflecting the newly trained model's statistics.</li>
 * </ul>
 *
 * <p>The distinction between the model and the data is important:
 * {@code totalLabelled} reflects the current database state, while
 * {@code samplesUsedInLastRun} reflects what the model was actually
 * trained on. If new labels have been added since the last training run,
 * these two numbers will diverge — a signal that the model may be stale.
 *
 * @author Angela-Maria Despotopoulou
 */
@Value
@Builder
@AllArgsConstructor
@JsonPropertyOrder({
        "trained", "lastTrainedAt", "samplesUsedInLastRun",
        "totalLabelled", "positiveCount", "negativeCount",
        "seedLabelledCount", "manualLabelledCount", "claudeLabelledCount",
        "accuracyLastRun", "retrainBatchSize", "retrainBatchProgress"
})
@Schema(description = "Snapshot of the trained model health and training corpus statistics.")
public class ModelStatsResponse {

    /**
     * Whether the model has been trained at least once since the application
     * started or loaded a serialised model from disk.
     */
    @JsonProperty("trained")
    @Schema(description = "True if a trained model is available for classification.", example = "true")
    boolean trained;

    /**
     * The timestamp of the most recent training run, or {@code null} if the
     * model has never been trained.
     */
    @JsonProperty("lastTrainedAt")
    @Schema(
            description = "Timestamp of the most recent training run. Null if never trained.",
            example = "2026-03-21T18:00:00",
            nullable = true
    )
    LocalDateTime lastTrainedAt;

    /**
     * The number of labelled samples used in the most recent training run.
     *
     * <p>May differ from {@code totalLabelled} if new labels have been added
     * since the last training run, indicating that the model is stale.
     */
    @JsonProperty("samplesUsedInLastRun")
    @Schema(
            description = "Number of labelled samples used in the most recent training run.",
            example = "150"
    )
    long samplesUsedInLastRun;

    /**
     * The total number of labelled review samples currently in the database,
     * regardless of when they were labelled or which training run used them.
     */
    @JsonProperty("totalLabelled")
    @Schema(description = "Total labelled samples currently in the database.", example = "165")
    long totalLabelled;

    /**
     * The number of labelled samples carrying a {@code POSITIVE} label.
     */
    @JsonProperty("positiveCount")
    @Schema(description = "Number of POSITIVE labelled samples.", example = "89")
    long positiveCount;

    /**
     * The number of labelled samples carrying a {@code NEGATIVE} label.
     */
    @JsonProperty("negativeCount")
    @Schema(description = "Number of NEGATIVE labelled samples.", example = "76")
    long negativeCount;

    /**
     * The number of samples labelled from seed data loaded at startup.
     */
    @JsonProperty("seedLabelledCount")
    @Schema(description = "Number of samples labelled from seed data.", example = "50")
    long seedLabelledCount;

    /**
     * The number of samples labelled manually via the REST API.
     */
    @JsonProperty("manualLabelledCount")
    @Schema(description = "Number of samples labelled manually via the API.", example = "30")
    long manualLabelledCount;

    /**
     * The number of samples labelled by the Claude AI oracle during
     * active-learning cycles.
     */
    @JsonProperty("claudeLabelledCount")
    @Schema(description = "Number of samples labelled by the Claude AI oracle.", example = "85")
    long claudeLabelledCount;

    /**
     * The accuracy achieved by the model on the hold-out evaluation set
     * during the most recent training run, expressed as a value between
     * 0.0 and 1.0.
     *
     * <p>A value of {@code 0.0} indicates the model has never been evaluated,
     * or that evaluation failed.
     */
    @JsonProperty("accuracyLastRun")
    @Schema(
            description = "Model accuracy on the hold-out set during the last training run. " +
                          "Between 0.0 and 1.0. Zero if never evaluated.",
            example = "0.91"
    )
    double accuracyLastRun;

    /**
     * The configured retrain batch size — the number of new Claude-labelled
     * samples required to trigger an automatic retraining cycle.
     */
    @JsonProperty("retrainBatchSize")
    @Schema(
            description = "Configured number of new Claude-labelled samples that trigger a retrain.",
            example = "10"
    )
    int retrainBatchSize;

    /**
     * The number of new Claude-labelled samples accumulated since the last
     * retraining run, expressed as a progress value towards
     * {@code retrainBatchSize}.
     *
     * <p>When this value reaches {@code retrainBatchSize}, an automatic
     * retraining cycle is triggered.
     */
    @JsonProperty("retrainBatchProgress")
    @Schema(
            description = "New Claude-labelled samples since last retrain. " +
                          "When equal to retrainBatchSize, a retrain is triggered.",
            example = "7"
    )
    int retrainBatchProgress;
}
