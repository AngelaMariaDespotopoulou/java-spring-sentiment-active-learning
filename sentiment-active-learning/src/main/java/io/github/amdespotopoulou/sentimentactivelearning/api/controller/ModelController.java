package io.github.amdespotopoulou.sentimentactivelearning.api.controller;

import io.github.amdespotopoulou.sentimentactivelearning.api.response.ApiErrorResponse;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.request.TrainingRequest;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.response.ActiveLearningStatusResponse;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.response.ModelStatsResponse;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.response.TrainingResponse;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.LabelSource;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.SentimentLabel;
import io.github.amdespotopoulou.sentimentactivelearning.config.ActiveLearningProps;
import io.github.amdespotopoulou.sentimentactivelearning.persistence.dao.ReviewSampleDao;
import io.github.amdespotopoulou.sentimentactivelearning.service.core.ActiveLearner;
import io.github.amdespotopoulou.sentimentactivelearning.service.core.Classifier;
import io.github.amdespotopoulou.sentimentactivelearning.service.core.ModelTrainer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for model training, statistics, and active-learning
 * cycle management.
 *
 * <p>Exposes the following endpoints:
 * <ul>
 *   <li>{@code POST /api/model/train}                  — trigger a manual training run</li>
 *   <li>{@code GET  /api/model/stats}                  — retrieve model health snapshot</li>
 *   <li>{@code GET  /api/model/active-learning-status} — retrieve cycle status</li>
 * </ul>
 *
 * <p>All error responses follow the {@link ApiErrorResponse} envelope.
 * Each endpoint documents every possible
 * {@link io.github.amdespotopoulou.sentimentactivelearning.commons.enums.ErrorCode}
 * it can produce using named {@code @ExampleObject} entries so Swagger UI
 * shows the exact JSON shape for each failure scenario.
 *
 * @author Angela-Maria Despotopoulou
 */
@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping(path = "/api/model", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(
        name = "Model",
        description = "Manage model training runs, query model health statistics, " +
                "and monitor the active-learning cycle."
)
public class ModelController {

    /** Active-learning configuration properties. */
    private final ActiveLearningProps activeLearningProps;

    /** The live sentiment classifier — used to check trained state. */
    private final Classifier classifier;

    /** The model trainer — triggers training runs and holds historical stats. */
    private final ModelTrainer modelTrainer;

    /** The active-learning orchestrator — provides cycle status. */
    private final ActiveLearner activeLearner;

    /** DAO for live corpus statistics. */
    private final ReviewSampleDao reviewSampleDao;

    // -------------------------------------------------------------------------
    // POST /api/model/train
    // -------------------------------------------------------------------------

    /**
     * Triggers a manual model training run using all currently labelled review
     * samples.
     *
     * @param request the training configuration body; may be {@code null} or
     *                empty — defaults apply
     * @return a {@link TrainingResponse} with evaluation metrics and
     *         persistence status
     */
    @PostMapping(path = "/train", consumes = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Trigger a manual training run",
            description = "Trains the Naive Bayes model on all currently labelled review samples, " +
                    "evaluates it on a hold-out set, hot-swaps the live model, and saves " +
                    "the result to disk. An empty request body is accepted — defaults apply " +
                    "(forceRetrain=false, no note)."
    )
    @ApiResponse(responseCode = "200",
            description = "Training completed successfully.")
    @ApiResponse(responseCode = "409",
            description = "Insufficient labelled data.",
            content = @Content(
                    schema = @Schema(implementation = ApiErrorResponse.class),
                    examples = @ExampleObject(
                            name = "INSUFFICIENT_TRAINING_DATA",
                            value = """
                                    {
                                      "httpStatus"     : 409,
                                      "httpStatusText" : "Conflict",
                                      "errorCode"      : "INSUFFICIENT_TRAINING_DATA",
                                      "message"        : "Insufficient labelled samples for training. Required: 20, available: 5. Submit more labelled reviews or set forceRetrain=true."
                                    }""")))
    public TrainingResponse triggerTraining(
            @Valid @RequestBody(required = false) TrainingRequest request) {
        TrainingRequest effectiveRequest = request != null
                ? request
                : TrainingRequest.builder().build();
        log.info("POST /api/model/train — forceRetrain: {}",
                effectiveRequest.isForceRetrain());
        return modelTrainer.train(effectiveRequest);
    }

    // -------------------------------------------------------------------------
    // GET /api/model/stats
    // -------------------------------------------------------------------------

    /**
     * Returns a snapshot of the trained model's health and training corpus
     * statistics.
     *
     * <p>Combines live database counts with historical metrics from the most
     * recent training run. If the model has not been trained yet, historical
     * metrics are reported as zero.
     *
     * @return a fully populated {@link ModelStatsResponse}
     */
    @GetMapping("/stats")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Get model health statistics",
            description = "Returns a snapshot combining live corpus counts from the database " +
                    "with historical evaluation metrics from the most recent training run. " +
                    "If the model has not been trained yet, historical metrics are zero."
    )
    @ApiResponse(responseCode = "200", description = "Statistics retrieved successfully.")
    public ModelStatsResponse getModelStats() {
        log.debug("GET /api/model/stats — assembling model statistics.");

        ModelTrainer.LastRunStats lastRun = modelTrainer.getLastRunStats();

        long totalLabelled = reviewSampleDao.countLabelled();
        long positiveCount = reviewSampleDao.findByLabel(SentimentLabel.POSITIVE).size();
        long negativeCount = reviewSampleDao.findByLabel(SentimentLabel.NEGATIVE).size();
        long seedCount     = reviewSampleDao.countByLabelSource(LabelSource.SEED);
        long manualCount   = reviewSampleDao.countByLabelSource(LabelSource.MANUAL);
        long claudeCount   = reviewSampleDao.countByLabelSource(LabelSource.CLAUDE);

        int batchSize       = activeLearningProps.getRetrainBatchSize();
        int claudeSinceLast = activeLearner.getStatus().getClaudeLabelledSinceLastRetrain();

        return ModelStatsResponse.builder()
                .trained(classifier.isTrained())
                .lastTrainedAt(lastRun.lastTrainedAt())
                .samplesUsedInLastRun(lastRun.samplesUsedInLastRun())
                .totalLabelled(totalLabelled)
                .positiveCount(positiveCount)
                .negativeCount(negativeCount)
                .seedLabelledCount(seedCount)
                .manualLabelledCount(manualCount)
                .claudeLabelledCount(claudeCount)
                .accuracyLastRun(lastRun.accuracyLastRun())
                .retrainBatchSize(batchSize)
                .retrainBatchProgress(claudeSinceLast)
                .build();
    }

    // -------------------------------------------------------------------------
    // GET /api/model/active-learning-status
    // -------------------------------------------------------------------------

    /**
     * Returns a snapshot of the active-learning cycle's current state and
     * health.
     *
     * @return a fully populated {@link ActiveLearningStatusResponse}
     */
    @GetMapping("/active-learning-status")
    @ResponseStatus(HttpStatus.OK)
    @Operation(
            summary = "Get active-learning cycle status",
            description = "Returns a snapshot of the active-learning cycle: progress towards " +
                    "the next automatic retrain, total completed retraining runs, number " +
                    "of unlabelled reviews awaiting labelling, and model file health."
    )
    @ApiResponse(responseCode = "200", description = "Status retrieved successfully.")
    public ActiveLearningStatusResponse getActiveLearningStatus() {
        log.debug("GET /api/model/active-learning-status — assembling cycle status.");
        return activeLearner.getStatus();
    }
}