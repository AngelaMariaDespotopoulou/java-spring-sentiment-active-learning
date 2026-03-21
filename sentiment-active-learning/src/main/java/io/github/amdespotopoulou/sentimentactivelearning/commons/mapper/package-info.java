/**
 * MapStruct mapper interfaces for the Sentiment Active Learning application.
 *
 * <p>Mappers in this package convert between DTOs in
 * {@link io.github.amdespotopoulou.sentimentactivelearning.commons.dto} and
 * JPA entities in
 * {@link io.github.amdespotopoulou.sentimentactivelearning.persistence.entity}.
 * All implementations are generated at compile time by MapStruct and registered
 * as Spring {@code @Component} beans, making them injectable via constructor
 * injection throughout the application.
 *
 * <p>Mappers are placed in {@code commons} rather than in a specific layer because
 * they bridge two layers ({@code commons.dto} and {@code persistence.entity}) and
 * must be accessible to both the service layer (which uses DTOs) and the DAO
 * implementations (which work with entities).
 *
 * <h2>Contents</h2>
 * <ul>
 *   <li>{@code ReviewSampleMapper} — converts between
 *       {@link io.github.amdespotopoulou.sentimentactivelearning.commons.dto.ReviewRequest}
 *       and
 *       {@link io.github.amdespotopoulou.sentimentactivelearning.persistence.entity.ReviewSample},
 *       and maps entities to response DTOs for the API layer.</li>
 * </ul>
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning.commons.mapper;
