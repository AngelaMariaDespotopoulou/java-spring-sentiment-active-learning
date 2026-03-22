package io.github.amdespotopoulou.sentimentactivelearning.service.core;

import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.request.LabelRequest;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.request.PredictionFeedbackRequest;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.request.ReviewRequest;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.response.ActiveLearningStatusResponse;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.response.ClassifyResponse;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.response.ReviewSampleResponse;

/**
 * Contract for the active-learning orchestration component.
 *
 * <p>This is the central coordinator of the sentiment analysis pipeline.
 * It receives review submissions from the API layer, drives classification
 * through {@link Classifier}, consults the Claude AI oracle via
 * {@link io.github.amdespotopoulou.sentimentactivelearning.service.oracle.ClaudeOracle}
 * when the classifier is uncertain, persists all labels via the DAO layer,
 * tracks the retrain batch counter, and triggers {@link ModelTrainer} when
 * enough new labels have accumulated.
 *
 * <h2>The active-learning cycle</h2>
 * <ol>
 *   <li>A review is submitted — it is persisted and classified.</li>
 *   <li>If the classifier is confident, the label is saved immediately.</li>
 *   <li>If the classifier is uncertain, the Claude oracle is consulted and
 *       its label is saved instead.</li>
 *   <li>Each Claude-labelled sample increments a counter. When the counter
 *       reaches the configured retrain batch size, a retraining run is
 *       triggered automatically.</li>
 *   <li>After retraining, the counter resets to zero and the cycle
 *       continues with a smarter model.</li>
 * </ol>
 *
 * <h2>Design rationale</h2>
 * <p>Declaring the orchestrator as an interface enables unit-testing of the
 * API controllers without a running ML pipeline, and makes the orchestration
 * strategy replaceable independently of the classifier and trainer
 * implementations.
 *
 * <p>The current production implementation is {@link ActiveLearningService}.
 *
 * @author Angela-Maria Despotopoulou
 */
public interface ActiveLearner {

    /**
     * Submits a new review for classification by the active-learning pipeline.
     *
     * <p>The review is persisted, classified, and — if the classifier is
     * uncertain — forwarded to the Claude AI oracle. The result reflects
     * whichever label source was used. If the submission triggers an automatic
     * retraining run (because the Claude-labelled batch threshold was reached),
     * the response reflects the label assigned before retraining.
     *
     * @param request the review submission; must not be {@code null}
     * @return a {@link ClassifyResponse} containing the predicted label,
     *         confidence score, uncertainty flag, and label source
     * @throws io.github.amdespotopoulou.sentimentactivelearning.exception.ResourceConflictException
     *         if a review with the same text already exists
     * @throws io.github.amdespotopoulou.sentimentactivelearning.exception.ModelNotTrainedException
     *         if no trained model is available yet
     */
    ClassifyResponse submitReview(ReviewRequest request);

    /**
     * Manually assigns a sentiment label to an existing unlabelled review
     * sample.
     *
     * <p>The label is stored with label source {@code MANUAL} and is treated
     * as the highest-quality signal in the training corpus. A manual label
     * may override an existing {@code CLAUDE} label on the same review.
     *
     * @param reviewId the identifier of the review sample to label;
     *                 must not be {@code null}
     * @param request  the label assignment; must not be {@code null}
     * @return the updated {@link ReviewSampleResponse}
     * @throws io.github.amdespotopoulou.sentimentactivelearning.exception.ResourceNotFoundException
     *         if no review sample exists for the given identifier
     */
    ReviewSampleResponse labelReview(Long reviewId, LabelRequest request);

    /**
     * Submits a human correction to a model prediction.
     *
     * <p>When a user disagrees with the model's prediction, they submit the
     * correct label via this method. The correction is stored with label
     * source {@code MANUAL}, overriding any previous label. Human corrections
     * are the highest-quality training signal and feed directly into the next
     * retraining cycle.
     *
     * @param reviewId the identifier of the misclassified review sample;
     *                 must not be {@code null}
     * @param request  the correction details; must not be {@code null}
     * @return the updated {@link ReviewSampleResponse} reflecting the
     *         corrected label
     * @throws io.github.amdespotopoulou.sentimentactivelearning.exception.ResourceNotFoundException
     *         if no review sample exists for the given identifier
     */
    ReviewSampleResponse submitFeedback(Long reviewId,
                                        PredictionFeedbackRequest request);

    /**
     * Returns a snapshot of the active-learning cycle's current state and
     * health.
     *
     * <p>Includes the number of Claude-labelled samples accumulated since the
     * last retraining run, progress towards the next automatic retrain, and
     * model persistence health.
     *
     * @return a fully populated {@link ActiveLearningStatusResponse};
     *         never {@code null}
     */
    ActiveLearningStatusResponse getStatus();
}
