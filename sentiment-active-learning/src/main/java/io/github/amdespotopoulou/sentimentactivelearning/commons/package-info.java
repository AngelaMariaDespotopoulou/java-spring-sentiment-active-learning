/**
 * Shared types used across all layers of the Sentiment Active Learning application.
 *
 * <p>This package and its sub-packages act as the shared contract layer of the
 * application. Types defined here may be imported by any layer without creating
 * upward or circular dependencies. No Spring beans, no JPA annotations, and no
 * business logic reside here — only plain data carriers, enumerations, and
 * structural mappers.
 *
 * <h2>Sub-packages</h2>
 * <ul>
 *   <li>{@code dto}    — Request and response DTOs shared between the API and
 *       service layers. All fields are validated with Jakarta Bean Validation
 *       annotations.</li>
 *   <li>{@code enums}  — Enumerations shared across layers:
 *       {@code SentimentLabel}, {@code LabelSource}, and {@code ErrorCode}.</li>
 *   <li>{@code mapper} — MapStruct mapper interfaces that convert between DTOs
 *       (commons.dto) and JPA entities (persistence.entity).</li>
 * </ul>
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning.commons;
