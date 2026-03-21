package io.github.amdespotopoulou.sentimentactivelearning.commons.mapper;

import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.request.ReviewRequest;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.response.ClassifyResponse;
import io.github.amdespotopoulou.sentimentactivelearning.commons.dto.response.ReviewSampleResponse;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.LabelSource;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.SentimentLabel;
import io.github.amdespotopoulou.sentimentactivelearning.persistence.entity.ReviewSample;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

/**
 * MapStruct mapper for conversions between {@link ReviewSample} entities and
 * their corresponding DTOs.
 *
 * <p>All implementations are generated at compile time by MapStruct and
 * registered as Spring {@code @Component} beans via
 * {@code componentModel = "spring"} (configured globally in the
 * {@code maven-compiler-plugin} annotation processor options). Inject this
 * mapper via constructor injection wherever conversions are needed.
 *
 * <h2>Design principles</h2>
 * <ul>
 *   <li>The mapper never sets audit timestamps ({@code createdAt},
 *       {@code updatedAt}) — these are owned by
 *       {@link org.springframework.data.jpa.domain.support.AuditingEntityListener}.</li>
 *   <li>The mapper never sets the entity {@code id} when mapping from a
 *       request DTO — IDs are assigned by the database on insert.</li>
 *   <li>{@code null} source values in update mappings are ignored
 *       ({@link NullValuePropertyMappingStrategy#IGNORE}) so that partial
 *       updates via PATCH never accidentally null out existing fields.</li>
 * </ul>
 *
 * @author Angela-Maria Despotopoulou
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ReviewSampleMapper {

    // -------------------------------------------------------------------------
    // ReviewRequest → ReviewSample (entity creation)
    // -------------------------------------------------------------------------

    /**
     * Maps a {@link ReviewRequest} to a new {@link ReviewSample} entity ready
     * for persistence.
     *
     * <p>The following fields are intentionally excluded from mapping:
     * <ul>
     *   <li>{@code id} — assigned by the database on insert.</li>
     *   <li>{@code label} — not yet assigned at creation time.</li>
     *   <li>{@code labelSource} — not yet assigned at creation time.</li>
     *   <li>{@code createdAt} — populated by {@code AuditingEntityListener}.</li>
     *   <li>{@code updatedAt} — populated by {@code AuditingEntityListener}.</li>
     * </ul>
     *
     * @param request the inbound review submission; must not be {@code null}
     * @return a new {@link ReviewSample} with only {@code reviewText} populated
     */
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "label",       ignore = true)
    @Mapping(target = "labelSource", ignore = true)
    @Mapping(target = "createdAt",   ignore = true)
    @Mapping(target = "updatedAt",   ignore = true)
    ReviewSample toEntity(ReviewRequest request);

    // -------------------------------------------------------------------------
    // ReviewSample → ReviewSampleResponse
    // -------------------------------------------------------------------------

    /**
     * Maps a {@link ReviewSample} entity to a {@link ReviewSampleResponse}
     * outbound DTO.
     *
     * <p>All fields map directly by name. {@code label} and {@code labelSource}
     * may be {@code null} for unlabelled samples — this is intentional and
     * reflected in the response DTO.
     *
     * @param entity the entity to map; must not be {@code null}
     * @return a fully populated {@link ReviewSampleResponse}
     */
    ReviewSampleResponse toResponse(ReviewSample entity);

    /**
     * Maps a list of {@link ReviewSample} entities to a list of
     * {@link ReviewSampleResponse} outbound DTOs.
     *
     * <p>Delegates to {@link #toResponse(ReviewSample)} for each element.
     * An empty input list produces an empty output list, never {@code null}.
     *
     * @param entities the list of entities to map; must not be {@code null}
     * @return a list of {@link ReviewSampleResponse} DTOs
     */
    List<ReviewSampleResponse> toResponseList(List<ReviewSample> entities);

    // -------------------------------------------------------------------------
    // ReviewSample → ClassifyResponse
    // -------------------------------------------------------------------------

    /**
     * Maps a {@link ReviewSample} entity to a {@link ClassifyResponse},
     * enriched with the classification-specific fields that are not stored
     * on the entity itself.
     *
     * <p>The {@code confidenceScore} and {@code uncertain} fields cannot be
     * derived from the entity — they are produced by the Tribuo classifier
     * at prediction time and must be supplied explicitly by the caller.
     *
     * @param entity          the classified review sample; must not be {@code null}
     * @param confidenceScore the classifier's confidence score, between 0.0 and 1.0
     * @param uncertain       {@code true} if the confidence fell below the
     *                        configured uncertainty threshold
     * @return a fully populated {@link ClassifyResponse}
     */
    @Mapping(target = "confidenceScore", expression = "java(confidenceScore)")
    @Mapping(target = "uncertain",       expression = "java(uncertain)")
    ClassifyResponse toClassifyResponse(
            ReviewSample entity,
            double confidenceScore,
            boolean uncertain);

    // -------------------------------------------------------------------------
    // Update mapping (PATCH support)
    // -------------------------------------------------------------------------

    /**
     * Applies a new label and label source to an existing {@link ReviewSample}
     * entity in place, for use in PATCH label operations.
     *
     * <p>Only {@code label} and {@code labelSource} are updated. All other
     * fields — including {@code reviewText}, {@code id}, and audit timestamps —
     * are left untouched. The {@code updatedAt} timestamp is refreshed
     * automatically by {@code AuditingEntityListener} when the entity is
     * subsequently saved.
     *
     * <p>This method mutates the supplied {@code target} entity directly
     * rather than producing a new instance, consistent with JPA's managed
     * entity model where the persistence context tracks changes on the
     * existing reference.
     *
     * @param label       the new sentiment label to apply; must not be {@code null}
     * @param labelSource the origin of the new label; must not be {@code null}
     * @param target      the managed entity to update; must not be {@code null}
     */
    @Mapping(target = "id",          ignore = true)
    @Mapping(target = "reviewText",  ignore = true)
    @Mapping(target = "createdAt",   ignore = true)
    @Mapping(target = "updatedAt",   ignore = true)
    @Mapping(target = "label",       expression = "java(label)")
    @Mapping(target = "labelSource", expression = "java(labelSource)")
    void applyLabel(
            SentimentLabel label,
            LabelSource labelSource,
            @MappingTarget ReviewSample target);
}