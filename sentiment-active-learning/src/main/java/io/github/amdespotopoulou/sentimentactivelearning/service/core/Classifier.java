package io.github.amdespotopoulou.sentimentactivelearning.service.core;

import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.SentimentLabel;
import io.github.amdespotopoulou.sentimentactivelearning.exception.ModelNotTrainedException;
import org.tribuo.Model;
import org.tribuo.classification.Label;

/**
 * Contract for the sentiment classification component of the active-learning
 * pipeline.
 *
 * <p>Implementations of this interface hold a trained Tribuo model in memory,
 * expose classification and uncertainty detection operations to the rest of
 * the application, and support hot-swapping the model after a retraining run
 * without application restart.
 *
 * <h2>Design rationale</h2>
 * <p>Declaring the classifier as an interface serves two purposes:
 * <ul>
 *   <li><b>Testability</b> — controllers and the active-learning service can
 *       be unit-tested by injecting a stub implementation that returns
 *       hardcoded predictions without requiring a trained Tribuo model.</li>
 *   <li><b>Replaceability</b> — the Naive Bayes implementation can be swapped
 *       for any other classification algorithm (decision tree, logistic
 *       regression, etc.) without touching any other class.</li>
 * </ul>
 *
 * <p>The current production implementation is {@link ClassifierService}.
 *
 * @author Angela-Maria Despotopoulou
 */
public interface Classifier {

    /**
     * Classifies the given review text and returns the predicted sentiment
     * label.
     *
     * <p>If the model's confidence score for the prediction falls below the
     * configured uncertainty threshold, the result is flagged as uncertain via
     * {@link ClassificationResult#uncertain()}. The caller — typically
     * {@code ActiveLearningService} — is then responsible for consulting the
     * Claude AI oracle to obtain a more reliable label.
     *
     * @param reviewText the raw movie review text to classify; must not be
     *                   {@code null} or blank
     * @return a {@link ClassificationResult} containing the predicted label,
     *         confidence score, and uncertainty flag; never {@code null}
     * @throws ModelNotTrainedException if no trained model is currently
     *         available — i.e. {@link #isTrained()} returns {@code false}
     */
    ClassificationResult classify(String reviewText);

    /**
     * Returns whether a trained model is currently available for
     * classification.
     *
     * <p>Returns {@code false} on fresh startup before the first training run
     * completes, and {@code true} at all other times — including after a model
     * has been deserialised from the configured model storage path on disk
     * during application startup.
     *
     * @return {@code true} if a trained model is held in memory and ready for
     *         classification; {@code false} otherwise
     */
    boolean isTrained();

    /**
     * Replaces the currently held model with the newly trained one.
     *
     * <p>Called by {@code TrainingService} after every successful training run
     * to hot-swap the model without restarting the application. The swap is
     * atomic — in-flight classification requests using the old model complete
     * normally; new requests immediately use the new model.
     *
     * @param newModel the newly trained Tribuo model; must not be {@code null}
     */
    void updateModel(Model<Label> newModel);

    /**
     * Returns the currently held trained model, if one exists.
     *
     * <p>Used by {@code TrainingService} during the startup sequence to check
     * whether a model deserialised from disk is stale — i.e. new labelled
     * samples have been added since the model was last saved — and therefore
     * needs to be retrained before use.
     *
     * @return the current {@link Model}, or {@code null} if no model has been
     *         trained or loaded yet
     */
    Model<Label> getCurrentModel();

    // -------------------------------------------------------------------------
    // Result type
    // -------------------------------------------------------------------------

    /**
     * Immutable value object carrying the result of a single classification
     * operation.
     *
     * <p>Bundles the predicted label, the model's confidence score, and the
     * uncertainty flag into a single cohesive object so that callers never
     * have to make multiple calls to retrieve related data.
     *
     * @param label           the predicted {@link SentimentLabel}
     * @param confidenceScore the model's confidence in its prediction,
     *                        between 0.0 and 1.0
     * @param uncertain       {@code true} if {@code confidenceScore} fell
     *                        below the configured uncertainty threshold,
     *                        meaning the Claude oracle should be consulted
     *
     * @author Angela-Maria Despotopoulou
     */
    record ClassificationResult(
            SentimentLabel label,
            double confidenceScore,
            boolean uncertain) {
    }
}