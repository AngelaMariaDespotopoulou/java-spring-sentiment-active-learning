package io.github.amdespotopoulou.sentimentactivelearning.commons.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

/**
 * Inbound DTO for triggering a manual model training run via the REST API.
 *
 * <p>Used as the request body for:
 * <ul>
 *   <li>POST {@code /api/model/train} — trigger an immediate training run
 *       using all currently labelled review samples.</li>
 * </ul>
 *
 * <p>Under normal operation, retraining is triggered automatically by the
 * active-learning cycle when the Claude-labelled sample batch size threshold
 * is reached. This endpoint allows an operator to force a retrain at any time
 * — for example, after bulk-importing manually labelled seed data.
 *
 * <p>No fields in this DTO carry bean-validation constraints because both
 * fields are optional with well-defined defaults. Domain-level validation
 * (e.g. verifying that sufficient labelled data exists before training) is
 * performed in {@code TrainingService}.
 *
 * @author Angela-Maria Despotopoulou
 */
@Value
@Builder
@AllArgsConstructor
@Schema(description = "Request body for triggering a manual model training run.")
public class TrainingRequest {

    /**
     * Whether to bypass the minimum training sample threshold and force a
     * training run even if fewer samples than
     * {@link io.github.amdespotopoulou.sentimentactivelearning.config.ActiveLearningProps}
     * ({@code active-learning.min-training-samples})
     * are available.
     *
     * <p>Use with caution — training on too few samples produces an unreliable
     * model. Intended for development and testing purposes only. Defaults to
     * {@code false}.
     */
    @JsonProperty("forceRetrain")
    @Schema(
            description = "If true, bypasses the minimum sample threshold and forces training immediately. " +
                    "Use with caution — may produce an unreliable model on small datasets.",
            example = "false",
            defaultValue = "false"
    )
    @Builder.Default
    boolean forceRetrain = false;

    /**
     * Human-readable note describing the reason for triggering this manual
     * training run. Optional — purely for audit and logging purposes.
     *
     * <p>Logged by {@code TrainingService} at INFO level when present, providing
     * a human-readable audit trail of manual training interventions.
     * Not persisted to the database.
     */
    @JsonProperty("note")
    @Schema(
            description = "Optional human-readable note describing why this training run was triggered. " +
                    "Logged for audit purposes. Not persisted.",
            example = "Manual retrain after bulk seed data import.",
            requiredMode = Schema.RequiredMode.NOT_REQUIRED
    )
    @Builder.Default
    String note = null;
}