package io.github.amdespotopoulou.sentimentactivelearning.service.core;

import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.request.LabelRequest;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.request.PredictionFeedbackRequest;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.request.ReviewRequest;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.request.TrainingRequest;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.response.ActiveLearningStatusResponse;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.response.ClassifyResponse;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.response.ReviewSampleResponse;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.ErrorCode;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.LabelSource;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.SentimentLabel;
import io.github.amdespotopoulou.sentimentactivelearning.config.ActiveLearningProps;
import io.github.amdespotopoulou.sentimentactivelearning.exception.ResourceNotFoundException;
import io.github.amdespotopoulou.sentimentactivelearning.commons.mapper.ReviewSampleMapper;
import io.github.amdespotopoulou.sentimentactivelearning.persistence.dao.ReviewSampleDao;
import io.github.amdespotopoulou.sentimentactivelearning.persistence.entity.ReviewSample;
import io.github.amdespotopoulou.sentimentactivelearning.service.oracle.ClaudeOracle;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Production implementation of {@link ActiveLearner} — the central
 * orchestrator of the active-learning pipeline.
 *
 * <h2>Cycle orchestration</h2>
 * <p>This service coordinates all components of the pipeline:
 * <ul>
 *   <li>{@link Classifier} — classifies submitted reviews and detects
 *       uncertainty.</li>
 *   <li>{@link ClaudeOracle} — labels uncertain reviews when the classifier's
 *       confidence is too low.</li>
 *   <li>{@link ModelTrainer} — retrains the model when the configured batch
 *       of new Claude-labelled samples has accumulated.</li>
 *   <li>{@link ReviewSampleDao} — persists all reviews and labels.</li>
 *   <li>{@link ReviewSampleMapper} — converts between entities and DTOs.</li>
 * </ul>
 *
 * <h2>Retrain batch counter</h2>
 * <p>An {@link AtomicInteger} ({@code claudeLabelledSinceLastRetrain}) tracks
 * how many Claude-labelled samples have been added since the last retraining
 * run. When this counter reaches the configured batch size, a retraining run
 * is triggered automatically and the counter resets to zero.
 *
 * <p>The counter is in-memory only and resets on application restart. This is
 * intentional — on restart the model is restored from disk (or retrained from
 * scratch), so the baseline is always clean.
 *
 * <h2>Thread safety</h2>
 * <p>All mutable state ({@code claudeLabelledSinceLastRetrain},
 * {@code lastRetrainedAt}, {@code totalRetrainingRuns}) is managed via atomic
 * types to ensure safe concurrent access from multiple HTTP request threads.
 *
 * @author Angela-Maria Despotopoulou
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ActiveLearningService implements ActiveLearner {

    // -------------------------------------------------------------------------
    // Dependencies
    // -------------------------------------------------------------------------

    /** Active-learning configuration properties. */
    private final ActiveLearningProps activeLearningProps;

    /** The live sentiment classifier. */
    private final Classifier classifier;

    /** The Claude AI labelling oracle. */
    private final ClaudeOracle claudeOracle;

    /** The model trainer — triggered when the batch threshold is reached. */
    private final ModelTrainer modelTrainer;

    /** Data access object for persisting and retrieving review samples. */
    private final ReviewSampleDao reviewSampleDao;

    /** Mapper for converting entities to response DTOs. */
    private final ReviewSampleMapper reviewSampleMapper;

    // -------------------------------------------------------------------------
    // Cycle state (thread-safe)
    // -------------------------------------------------------------------------

    /**
     * Number of Claude-labelled samples added since the most recent
     * retraining run.
     *
     * <p>Incremented atomically on each Claude oracle call. Reset to zero
     * after each automatic retraining run. When this counter reaches
     * {@link ActiveLearningProps} {@code active-learning.retrain-batch-size},
     * an automatic retraining run is triggered.
     */
    private final AtomicInteger claudeLabelledSinceLastRetrain = new AtomicInteger(0);

    /**
     * Total number of completed retraining runs since the application started.
     * Incremented atomically after each successful automatic retrain.
     */
    private final AtomicInteger totalRetrainingRuns = new AtomicInteger(0);

    /**
     * Timestamp of the most recent completed retraining run.
     * {@code null} until the first retraining run completes.
     */
    private final AtomicReference<LocalDateTime> lastRetrainedAt =
            new AtomicReference<>(null);

    // -------------------------------------------------------------------------
    // ActiveLearner interface implementation
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Full submission sequence:
     * <ol>
     *   <li>Persist the review as an unlabelled {@link ReviewSample}.</li>
     *   <li>Classify it using the live {@link Classifier}.</li>
     *   <li>If confident: save the model's label with source {@code SEED}
     *       (auto-labelled by the model itself).</li>
     *   <li>If uncertain: consult {@link ClaudeOracle}, save Claude's label
     *       with source {@code CLAUDE}, increment the batch counter, and
     *       trigger retraining if the threshold is reached.</li>
     * </ol>
     */
    @Override
    @Transactional
    public ClassifyResponse submitReview(ReviewRequest request) {
        ReviewSample sample = reviewSampleMapper.toEntity(request);
        sample = reviewSampleDao.save(sample);
        log.debug("Review persisted with ID: {}", sample.getId());

        Classifier.ClassificationResult result =
                classifier.classify(request.getReviewText());

        SentimentLabel finalLabel;
        LabelSource    finalSource;

        if (!result.uncertain()) {
            finalLabel  = result.label();
            finalSource = LabelSource.SEED;
            log.debug("Classifier confident — label: {}, confidence: {}",
                    finalLabel, result.confidenceScore());
        } else {
            log.debug("Classifier uncertain (confidence: {}) — consulting Claude oracle.",
                    result.confidenceScore());
            finalLabel  = claudeOracle.label(request.getReviewText());
            finalSource = LabelSource.CLAUDE;
            log.info("Claude oracle assigned label [{}] to review ID: {}",
                    finalLabel, sample.getId());
            handleClaudeLabelAdded();
        }

        reviewSampleMapper.applyLabel(finalLabel, finalSource, sample);
        reviewSampleDao.update(sample);

        return reviewSampleMapper.toClassifyResponse(
                sample,
                result.confidenceScore(),
                result.uncertain());
    }

    /**
     * {@inheritDoc}
     *
     * <p>Saves the label with source {@code MANUAL}, which is the
     * highest-quality signal in the training corpus and overrides any
     * previous label on the same review.
     */
    @Override
    @Transactional
    public ReviewSampleResponse labelReview(Long reviewId, LabelRequest request) {
        ReviewSample sample = reviewSampleDao.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.REVIEW_NOT_FOUND,
                        "Review sample with ID " + reviewId + " was not found."));

        reviewSampleMapper.applyLabel(request.getLabel(), LabelSource.MANUAL, sample);
        sample = reviewSampleDao.update(sample);

        log.info("Manual label [{}] applied to review ID: {}", request.getLabel(), reviewId);

        return reviewSampleMapper.toResponse(sample);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Logs the optional feedback note for diagnostic purposes, then
     * applies the corrected label with source {@code MANUAL}, overriding
     * any previous label.
     */
    @Override
    @Transactional
    public ReviewSampleResponse submitFeedback(Long reviewId,
                                               PredictionFeedbackRequest request) {
        ReviewSample sample = reviewSampleDao.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        ErrorCode.REVIEW_NOT_FOUND,
                        "Review sample with ID " + reviewId + " was not found."));

        if (request.getFeedbackNote() != null && !request.getFeedbackNote().isBlank()) {
            log.info("Prediction feedback for review ID {}: '{}'",
                    reviewId, request.getFeedbackNote());
        }

        reviewSampleMapper.applyLabel(
                request.getCorrectedLabel(), LabelSource.MANUAL, sample);
        sample = reviewSampleDao.update(sample);

        log.info("Feedback correction [{}] applied to review ID: {}",
                request.getCorrectedLabel(), reviewId);

        return reviewSampleMapper.toResponse(sample);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ActiveLearningStatusResponse getStatus() {
        int batchSize    = activeLearningProps.getRetrainBatchSize();
        int claudeCount  = claudeLabelledSinceLastRetrain.get();
        int progressPct  = batchSize > 0
                ? Math.min(100, (claudeCount * 100) / batchSize)
                : 0;

        String modelPath     = activeLearningProps.getModelStoragePath();
        boolean modelExists  = Files.exists(Paths.get(modelPath));

        return ActiveLearningStatusResponse.builder()
                .cycleActive(classifier.isTrained())
                .totalRetrainingRuns(totalRetrainingRuns.get())
                .lastRetrainedAt(lastRetrainedAt.get())
                .claudeLabelledSinceLastRetrain(claudeCount)
                .retrainBatchSize(batchSize)
                .progressPercent(progressPct)
                .totalUnlabelled(reviewSampleDao.findAllUnlabelled().size())
                .modelStoragePath(modelPath)
                .modelFileExists(modelExists)
                .build();
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Increments the Claude-labelled sample counter and triggers an automatic
     * retraining run if the configured batch size threshold has been reached.
     *
     * <p>Called after every successful Claude oracle labelling. The check and
     * trigger are performed atomically to prevent multiple concurrent requests
     * from triggering multiple simultaneous retraining runs.
     */
    private void handleClaudeLabelAdded() {
        int count = claudeLabelledSinceLastRetrain.incrementAndGet();
        int batchSize = activeLearningProps.getRetrainBatchSize();

        log.debug("Claude-labelled since last retrain: {}/{}", count, batchSize);

        if (count >= batchSize) {
            triggerAutomaticRetrain();
        }
    }

    /**
     * Triggers an automatic retraining run and resets the batch counter.
     *
     * <p>The counter is reset to zero before the retraining run begins —
     * not after — so that any Claude labels added concurrently during the
     * retraining run are counted towards the next batch rather than lost.
     *
     * <p>Retraining failures are caught and logged rather than propagated —
     * a failed retrain is not a reason to reject the label that triggered it.
     * The counter is not reset on failure so the next Claude label will
     * immediately re-trigger the attempt.
     */
    private void triggerAutomaticRetrain() {
        claudeLabelledSinceLastRetrain.set(0);
        log.info("Retrain batch threshold reached. Triggering automatic retraining run.");

        try {
            TrainingRequest retrainRequest = TrainingRequest.builder()
                    .forceRetrain(false)
                    .note("Automatic retrain triggered by active-learning batch threshold.")
                    .build();
            modelTrainer.train(retrainRequest);
            totalRetrainingRuns.incrementAndGet();
            lastRetrainedAt.set(LocalDateTime.now());
            log.info("Automatic retraining run completed. " +
                     "Total runs: {}", totalRetrainingRuns.get());
        } catch (Exception ex) {
            log.error("Automatic retraining run failed: {}. " +
                      "Counter not reset — will retry on next Claude label.",
                    ex.getMessage(), ex);
            claudeLabelledSinceLastRetrain.set(
                    activeLearningProps.getRetrainBatchSize());
        }
    }
}
