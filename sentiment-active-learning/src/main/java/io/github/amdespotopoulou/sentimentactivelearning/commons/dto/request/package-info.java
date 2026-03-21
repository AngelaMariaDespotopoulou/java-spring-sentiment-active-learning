/**
 * Inbound request DTOs for the Sentiment Active Learning REST API.
 *
 * <p>Every class in this package represents data submitted by an API consumer
 * in an HTTP request body. All fields are annotated with Jakarta Bean
 * Validation constraints ({@code @NotBlank}, {@code @NotNull}, {@code @Size},
 * etc.) so that invalid input is rejected at the controller boundary before
 * reaching any business logic.
 *
 * <h2>Invariants enforced across all request DTOs</h2>
 * <ul>
 *   <li>Audit timestamps ({@code createdAt}, {@code updatedAt}) never appear
 *       in any request DTO — they are owned exclusively by the system and
 *       populated automatically by
 *       {@link org.springframework.data.jpa.domain.support.AuditingEntityListener}.</li>
 *   <li>Database identifiers ({@code id}) never appear in request bodies —
 *       they are path variables or are assigned by the database on insert.</li>
 *   <li>All classes are immutable, built with Lombok {@code @Value} and
 *       {@code @Builder}.</li>
 * </ul>
 *
 * <h2>Contents</h2>
 * <ul>
 *   <li>{@code ReviewRequest} — submits a raw review text for classification
 *       or labelling.</li>
 *   <li>{@code LabelRequest} — manually assigns a sentiment label to an
 *       existing review sample.</li>
 *   <li>{@code TrainingRequest} — triggers a manual model training run,
 *       optionally bypassing the minimum sample threshold.</li>
 *   <li>{@code PredictionFeedbackRequest} — submits a human correction to
 *       a model prediction (human-in-the-loop mechanism).</li>
 * </ul>
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning.commons.dto.request;