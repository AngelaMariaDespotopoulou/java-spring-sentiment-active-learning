package io.github.amdespotopoulou.sentimentactivelearning.commons.enums;

/**
 * Represents the two possible sentiment outcomes of a movie-review classification.
 *
 * <p>This enumeration is the central vocabulary for sentiment throughout the
 * application. It is used by the Tribuo classifier as the prediction target,
 * stored on the
 * {@link io.github.amdespotopoulou.sentimentactivelearning.persistence.entity.ReviewSample}
 * entity, returned in response DTOs, and parsed from Claude AI oracle responses.
 *
 * <p>The string value of each constant (via {@link #name()}) is used as the
 * Tribuo {@code Label} string, as the JPA {@code @Enumerated(EnumType.STRING)}
 * column value, and as the expected token in Claude oracle responses. All three
 * usages rely on the same stable name — do not rename constants without
 * updating the Claude prompt in {@code ClaudeOracleService}.
 *
 * @author Angela-Maria Despotopoulou
 */
public enum SentimentLabel {

    /**
     * The review expresses a positive opinion about the movie.
     *
     * <p>Examples: praise for acting, direction, storyline, or overall enjoyment.
     */
    POSITIVE,

    /**
     * The review expresses a negative opinion about the movie.
     *
     * <p>Examples: criticism of acting, pacing, plot holes, or disappointment.
     */
    NEGATIVE
}
