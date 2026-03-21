package io.github.amdespotopoulou.sentimentactivelearning.exception;

import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.ErrorCode;
import lombok.Getter;

/**
 * Thrown when an operation would violate a uniqueness or state constraint.
 *
 * <p>Maps to HTTP 409 Conflict. Used for situations where the request is
 * structurally valid but cannot be fulfilled due to the current state of
 * the system — for example, submitting a duplicate review or requesting
 * classification before the model has been trained.
 *
 * <p>The {@link ErrorCode} carried by this exception determines the exact
 * error code embedded in the
 * {@link io.github.amdespotopoulou.sentimentactivelearning.api.response.ApiErrorResponse}
 * envelope, allowing callers to distinguish between different conflict
 * types (e.g. {@link ErrorCode#REVIEW_ALREADY_EXISTS},
 * {@link ErrorCode#MODEL_NOT_TRAINED},
 * {@link ErrorCode#INSUFFICIENT_TRAINING_DATA}).
 *
 * <p>Example usage:
 * <pre>{@code
 * throw new ResourceConflictException(
 *         ErrorCode.REVIEW_ALREADY_EXISTS,
 *         "A review sample with the same text already exists.");
 * }</pre>
 *
 * @author Angela-Maria Despotopoulou
 */
@Getter
public class ResourceConflictException extends RuntimeException {

    /**
     * The machine-readable error code identifying the conflict type.
     */
    private final ErrorCode errorCode;

    /**
     * Constructs a {@code ResourceConflictException} with the given error code
     * and human-readable message.
     *
     * @param errorCode the {@link ErrorCode} identifying the conflict;
     *                  must not be {@code null}
     * @param message   a human-readable description of the conflict;
     *                  must not be {@code null} or blank
     */
    public ResourceConflictException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
