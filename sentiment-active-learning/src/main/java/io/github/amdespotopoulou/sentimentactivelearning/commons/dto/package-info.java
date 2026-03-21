/**
 * Data Transfer Objects shared between the API and service layers of the
 * Sentiment Active Learning application.
 *
 * <p>DTOs in this package are plain Java records or classes carrying only data.
 * They are the exclusive currency of communication between the controller and
 * service layers — entities never cross the service/API boundary. All fields
 * are annotated with Jakarta Bean Validation constraints ({@code @NotBlank},
 * {@code @NotNull}, {@code @Size}, etc.) so that invalid input is rejected at
 * the controller boundary before reaching business logic.
 *
 * <h2>Contents</h2>
 * <ul>
 *   <li>{@code ReviewRequest}       — inbound DTO carrying the raw review text
 *       submitted for classification or manual labelling.</li>
 *   <li>{@code ClassifyResponse}    — outbound DTO carrying the predicted
 *       {@code SentimentLabel}, the confidence score, and a flag indicating
 *       whether the prediction was considered uncertain.</li>
 *   <li>{@code ModelStatsResponse}  — outbound DTO carrying current model
 *       metrics: accuracy, precision, recall, training sample count, and
 *       active-learning cycle statistics.</li>
 *   <li>{@code TrainingRequest}     — inbound DTO for triggering a manual
 *       training run, optionally specifying seed data parameters.</li>
 * </ul>
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning.commons.dto;
