/**
 * Shared Data Transfer Objects used across all layers of the
 * Sentiment Active Learning application.
 *
 * <p>This package is the parent of two sub-packages that separate inbound
 * and outbound data contracts:
 *
 * <ul>
 *   <li>{@code dto.request} — inbound DTOs carrying data supplied by API
 *       consumers. All fields are validated with Jakarta Bean Validation
 *       constraints. Audit timestamps ({@code createdAt}, {@code updatedAt})
 *       never appear here — they are owned exclusively by the system.</li>
 *   <li>{@code dto.response} — outbound DTOs carrying data returned to API
 *       consumers. No validation constraints — these are read-only
 *       representations of system state.</li>
 * </ul>
 *
 * <p>No Spring beans, no JPA annotations, and no business logic reside in
 * this package or its sub-packages — only plain immutable data carriers
 * built with Lombok {@code @Value} and {@code @Builder}.
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning.commons.dto;