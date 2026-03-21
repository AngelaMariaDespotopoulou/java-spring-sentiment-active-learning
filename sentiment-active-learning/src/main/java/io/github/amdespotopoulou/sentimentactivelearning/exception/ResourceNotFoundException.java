package io.github.amdespotopoulou.sentimentactivelearning.exception;

import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.ErrorCode;
import lombok.Getter;

/**
 * Thrown when a requested resource does not exist in the system.
 *
 * <p>Maps to HTTP 404 Not Found. The {@link ErrorCode} carried by this
 * exception determines the exact error code embedded in the
 * {@link io.github.amdespotopoulou.sentimentactivelearning.api.response.ApiErrorResponse}
 * envelope, allowing callers to distinguish between different missing
 * resource types (e.g. {@link ErrorCode#REVIEW_NOT_FOUND}).
 *
 * <p>Example usage:
 * <pre>{@code
 * throw new ResourceNotFoundException(
 *         ErrorCode.REVIEW_NOT_FOUND,
 *         "Review sample with ID " + id + " was not found.");
 * }</pre>
 *
 * @author Angela-Maria Despotopoulou
 */
@Getter
public class ResourceNotFoundException extends RuntimeException {

    /**
     * The machine-readable error code identifying the missing resource type.
     */
    private final ErrorCode errorCode;

    /**
     * Constructs a {@code ResourceNotFoundException} with the given error code
     * and human-readable message.
     *
     * @param errorCode the {@link ErrorCode} identifying the missing resource;
     *                  must not be {@code null}
     * @param message   a human-readable description of what was not found;
     *                  must not be {@code null} or blank
     */
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
