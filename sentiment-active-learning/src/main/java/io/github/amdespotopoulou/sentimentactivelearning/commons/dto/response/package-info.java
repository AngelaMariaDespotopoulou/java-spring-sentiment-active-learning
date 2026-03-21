/**
 * Outbound response DTOs for the Sentiment Active Learning REST API.
 *
 * <p>Every class in this package represents data returned to an API consumer
 * in an HTTP response body. No validation constraints are applied — these are
 * read-only representations of system state, assembled and populated by the
 * service layer.
 *
 * <h2>Invariants enforced across all response DTOs</h2>
 * <ul>
 *   <li>All classes are immutable, built with Lombok {@code @Value} and
 *       {@code @Builder}.</li>
 *   <li>Field ordering is declared explicitly via
 *       {@link com.fasterxml.jackson.annotation.JsonPropertyOrder} to produce
 *       consistent, predictable JSON output regardless of field declaration
 *       order in the class.</li>
 *   <li>Nullable fields are documented as such in both Javadoc and
 *       {@link io.swagger.v3.oas.annotations.media.Schema#nullable()}.</li>
 * </ul>
 *
 * <h2>Contents</h2>
 * <ul>
 *   <li>{@code ReviewSampleResponse} — a single review sample as stored in
 *       the system, including audit timestamps.</li>
 *   <li>{@code ClassifyResponse} — the result of a sentiment classification
 *       operation, including confidence score and uncertainty flag.</li>
 *   <li>{@code TrainingResponse} — the result of a completed model training
 *       run, including evaluation metrics and persistence status.</li>
 *   <li>{@code ModelStatsResponse} — a snapshot of the trained model health
 *       and training corpus statistics.</li>
 *   <li>{@code ActiveLearningStatusResponse} — a snapshot of the
 *       active-learning cycle state, progress, and model persistence health.</li>
 * </ul>
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning.commons.dto.response;