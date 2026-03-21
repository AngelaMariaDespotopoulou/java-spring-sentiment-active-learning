package io.github.amdespotopoulou.sentimentactivelearning.exception;

import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.ErrorCode;

/**
 * Thrown when a classification or evaluation is requested but the Naive Bayes
 * model has not yet been trained.
 *
 * <p>Maps to HTTP 409 Conflict via {@link ErrorCode#MODEL_NOT_TRAINED}.
 * This condition arises when the application starts fresh and no training run
 * has been completed yet. At least the number of samples configured under
 * {@code active-learning.min-training-samples} must be labelled and a
 * training run must have been triggered before classification is available.
 *
 * <p>This exception always carries the fixed error code
 * {@link ErrorCode#MODEL_NOT_TRAINED} and requires only a descriptive message,
 * making it a convenience specialisation of {@link ResourceConflictException}
 * with no extra constructor arguments needed at call sites.
 *
 * <p>Example usage:
 * <pre>{@code
 * if (!classifierService.isModelTrained()) {
 *     throw new ModelNotTrainedException(
 *             "The model has not been trained yet. " +
 *             "Please submit labelled samples and trigger a training run first.");
 * }
 * }</pre>
 *
 * @author Angela-Maria Despotopoulou
 */
public class ModelNotTrainedException extends ResourceConflictException {

    /**
     * Constructs a {@code ModelNotTrainedException} with the given human-readable
     * message. The error code is always {@link ErrorCode#MODEL_NOT_TRAINED}.
     *
     * @param message a human-readable description of the untrained model state;
     *                must not be {@code null} or blank
     */
    public ModelNotTrainedException(String message) {
        super(ErrorCode.MODEL_NOT_TRAINED, message);
    }
}
