/**
 * Shared enumerations used across all layers of the Sentiment Active Learning
 * application.
 *
 * <p>Enumerations in this package represent stable, closed sets of values that
 * are meaningful to more than one layer. Placing them in {@code commons.enums}
 * avoids duplicating constants and prevents any single layer from owning a type
 * that others depend on.
 *
 * <h2>Contents</h2>
 * <ul>
 *   <li>{@code SentimentLabel} — the two possible classification outcomes:
 *       {@code POSITIVE} and {@code NEGATIVE}. Used by the classifier, the
 *       entity, the DAO, the DTOs, and the Claude oracle response parser.</li>
 *   <li>{@code LabelSource} — tracks who assigned a label to a review sample:
 *       {@code SEED} (bundled training data), {@code MANUAL} (supplied by a
 *       human via the API), or {@code CLAUDE} (assigned by the Claude AI oracle
 *       during an active-learning cycle).</li>
 *   <li>{@code ErrorCode} — stable, machine-readable identifiers for every
 *       error condition the API can produce. Each constant maps to exactly one
 *       HTTP status code and is used by
 *       {@link io.github.amdespotopoulou.sentimentactivelearning.api.response.ApiErrorResponse}
 *       to give consumers a reliable programmatic handle on errors. Values will
 *       not be renamed or removed across minor releases.</li>
 * </ul>
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning.commons.enums;
