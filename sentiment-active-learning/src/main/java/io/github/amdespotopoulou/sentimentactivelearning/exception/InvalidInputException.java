package io.github.amdespotopoulou.sentimentactivelearning.exception;

import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.ErrorCode;
import lombok.Getter;

/**
 * Thrown when a field or variable fails a domain-level validation rule that
 * goes beyond standard Jakarta Bean Validation constraints.
 *
 * <p>Maps to HTTP 400 Bad Request. Bean-validation failures (e.g. {@code @NotBlank},
 * {@code @Size}) are handled separately by the
 * {@link GlobalExceptionHandler} via
 * {@link org.springframework.web.bind.MethodArgumentNotValidException}.
 * This exception is reserved for richer domain rules that cannot be expressed
 * as annotations — for example:
 * <ul>
 *   <li>A confidence threshold value outside the 0.0–1.0 range.</li>
 *   <li>A review text that consists entirely of whitespace after trimming.</li>
 *   <li>A batch size that is zero or negative.</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * if (threshold < 0.0 || threshold > 1.0) {
 *     throw new InvalidInputException(
 *             ErrorCode.INVALID_FIELD_VALUE,
 *             "Uncertainty threshold must be between 0.0 and 1.0, got: " + threshold);
 * }
 * }</pre>
 *
 * @author Angela-Maria Despotopoulou
 */
@Getter
public class InvalidInputException extends RuntimeException {

    /**
     * The machine-readable error code identifying the validation failure type.
     */
    private final ErrorCode errorCode;

    /**
     * Constructs an {@code InvalidInputException} with the given error code
     * and human-readable message.
     *
     * @param errorCode the {@link ErrorCode} identifying the validation failure;
     *                  must not be {@code null}
     * @param message   a human-readable description of the invalid input,
     *                  ideally including the offending value and the valid range;
     *                  must not be {@code null} or blank
     */
    public InvalidInputException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}
