package io.github.amdespotopoulou.sentimentactivelearning.service.core;

import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.request.TrainingRequest;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.response.TrainingResponse;
import io.github.amdespotopoulou.sentimentactivelearning.exception.ResourceConflictException;

/**
 * Contract for the model training component of the active-learning pipeline.
 *
 * <p>Implementations of this interface are responsible for the full training
 * lifecycle: fetching labelled data from the persistence layer, building a
 * Tribuo dataset, training the model, evaluating it on a hold-out set,
 * hot-swapping the live model in {@link Classifier}, and persisting the
 * trained model to disk so it survives application restarts.
 *
 * <p>Implementations also handle the startup sequence: on application
 * readiness, they attempt to restore a previously trained model from disk,
 * retraining from the database if the saved model is stale or missing.
 *
 * <h2>Design rationale</h2>
 * <p>Declaring the trainer as an interface serves two purposes:
 * <ul>
 *   <li><b>Testability</b> — the active-learning service can be unit-tested
 *       by injecting a stub implementation that records training invocations
 *       without requiring a real Tribuo pipeline or database.</li>
 *   <li><b>Replaceability</b> — the Naive Bayes training implementation can
 *       be swapped for any other algorithm without touching orchestration or
 *       controller code.</li>
 * </ul>
 *
 * <p>The current production implementation is {@link TrainingService}.
 *
 * @author Angela-Maria Despotopoulou
 */
public interface ModelTrainer {

    /**
     * Executes a full model training run using all currently labelled review
     * samples in the database.
     *
     * <p>The training sequence is:
     * <ol>
     *   <li>Verify that sufficient labelled samples exist (unless
     *       {@code forceRetrain} is set to {@code true} in the
     *       {@link TrainingRequest}).</li>
     *   <li>Fetch all labelled samples from the persistence layer.</li>
     *   <li>Split into training and hold-out evaluation sets.</li>
     *   <li>Extract bag-of-words features from each review text.</li>
     *   <li>Train the Multinomial Naive Bayes model.</li>
     *   <li>Evaluate accuracy, precision and recall on the hold-out set.</li>
     *   <li>Hot-swap the live model in {@link Classifier}.</li>
     *   <li>Serialise the trained model to the configured storage path.</li>
     * </ol>
     *
     * @param request the training configuration; must not be {@code null}
     * @return a {@link TrainingResponse} containing evaluation metrics,
     *         training corpus statistics, and persistence status
     * @throws ResourceConflictException with error code
     *         {@link io.github.amdespotopoulou.sentimentactivelearning.commons.enums.ErrorCode#INSUFFICIENT_TRAINING_DATA}
     *         if fewer labelled samples exist than the configured minimum
     *         and {@code forceRetrain} is {@code false}
     */
    TrainingResponse train(TrainingRequest request);

    /**
     * Executes the startup model restoration sequence.
     *
     * <p>Called automatically when the Spring application context is fully
     * initialised. The sequence is:
     * <ol>
     *   <li>If a serialised model file exists at the configured storage path
     *       and fewer new labels have been added since it was saved than the
     *       configured staleness threshold, the model is deserialised and
     *       loaded directly into {@link Classifier}.</li>
     *   <li>If the file exists but the model is stale (too many new labels
     *       added since it was saved), or if the file does not exist but
     *       sufficient labelled data is available, the model is retrained
     *       from scratch and the result is saved to disk.</li>
     *   <li>If neither condition is met (no file, insufficient data), the
     *       application starts in an untrained state and waits for more
     *       labelled data before training.</li>
     * </ol>
     *
     * <p>This method is a no-op if
     * {@link io.github.amdespotopoulou.sentimentactivelearning.config.ActiveLearningProps}
     * property {@code active-learning.retrain-on-startup} is {@code false}.
     */
    void onStartup();
}