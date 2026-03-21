package io.github.amdespotopoulou.sentimentactivelearning.persistence.dao;

import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.LabelSource;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.SentimentLabel;
import io.github.amdespotopoulou.sentimentactivelearning.persistence.entity.ReviewSample;

import java.util.List;
import java.util.Optional;

/**
 * Technology-agnostic Data Access Object contract for {@link ReviewSample}
 * entities.
 *
 * <p>This interface defines all data operations the service layer requires,
 * expressed entirely in business terms — no JPA annotations, no
 * {@link org.springframework.data.domain.Page}, no repository types leak
 * through this boundary. The service layer depends only on this interface
 * and is therefore completely decoupled from the underlying persistence
 * technology.
 *
 * <p>The current implementation,
 * {@link io.github.amdespotopoulou.sentimentactivelearning.persistence.dao.impl.ReviewSampleDaoImpl},
 * delegates to a Spring Data JPA repository. To migrate to a different storage
 * technology (PostgreSQL, MongoDB, etc.), provide a new implementation of this
 * interface and update the Spring bean wiring — no service or controller code
 * needs to change.
 *
 * @author Angela-Maria Despotopoulou
 */
public interface ReviewSampleDao {

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * Persists a new {@link ReviewSample} to the data store.
     *
     * <p>Implementations must enforce the uniqueness of {@code reviewText}
     * before attempting an insert, throwing a
     * {@link io.github.amdespotopoulou.sentimentactivelearning.exception.ResourceConflictException}
     * with {@link io.github.amdespotopoulou.sentimentactivelearning.commons.enums.ErrorCode#REVIEW_ALREADY_EXISTS}
     * if a duplicate is detected.
     *
     * @param reviewSample the entity to persist; must not be {@code null},
     *                     must have a non-blank {@code reviewText}
     * @return the persisted entity with its database-assigned {@code id}
     *         and audit timestamps populated
     * @throws io.github.amdespotopoulou.sentimentactivelearning.exception.ResourceConflictException
     *         if a review sample with the same text already exists
     */
    ReviewSample save(ReviewSample reviewSample);

    /**
     * Updates an existing {@link ReviewSample} in the data store.
     *
     * <p>Used primarily to assign or correct a sentiment label and label
     * source on a previously unlabelled or Claude-labelled record.
     *
     * @param reviewSample the entity to update; must not be {@code null},
     *                     must have a non-null {@code id} referencing an
     *                     existing record
     * @return the updated entity with refreshed audit timestamps
     * @throws io.github.amdespotopoulou.sentimentactivelearning.exception.ResourceNotFoundException
     *         if no record exists for the given {@code id}
     */
    ReviewSample update(ReviewSample reviewSample);

    // -------------------------------------------------------------------------
    // Read operations — single record
    // -------------------------------------------------------------------------

    /**
     * Returns the review sample with the given identifier, if it exists.
     *
     * @param id the database-assigned identifier to look up; must not be
     *           {@code null}
     * @return an {@link Optional} containing the matching record, or empty
     *         if no record exists for the given {@code id}
     */
    Optional<ReviewSample> findById(Long id);

    /**
     * Returns the review sample with the given text, if it exists.
     *
     * @param reviewText the exact review text to search for; must not be
     *                   {@code null} or blank
     * @return an {@link Optional} containing the matching record, or empty
     *         if no record exists with the given text
     */
    Optional<ReviewSample> findByReviewText(String reviewText);

    // -------------------------------------------------------------------------
    // Read operations — collections
    // -------------------------------------------------------------------------

    /**
     * Returns all review samples stored in the data store, labelled and
     * unlabelled alike.
     *
     * @return a list of all review samples; never {@code null}, may be empty
     */
    List<ReviewSample> findAll();

    /**
     * Returns all review samples that have been assigned a sentiment label,
     * regardless of label source.
     *
     * <p>This is the primary training data query used by {@code TrainingService}
     * to build the Tribuo dataset for a training run. Records with a
     * {@code null} label are excluded.
     *
     * @return a list of all labelled review samples; never {@code null},
     *         may be empty
     */
    List<ReviewSample> findAllLabelled();

    /**
     * Returns all review samples that have not yet been assigned a sentiment
     * label.
     *
     * <p>Used by the active-learning pipeline to identify reviews that are
     * pending labelling — either awaiting manual input via the API or queued
     * for Claude AI oracle labelling.
     *
     * @return a list of all unlabelled review samples; never {@code null},
     *         may be empty
     */
    List<ReviewSample> findAllUnlabelled();

    /**
     * Returns all review samples labelled by the given {@link LabelSource}.
     *
     * <p>Used to report on data provenance and to filter samples by origin
     * when computing evaluation metrics.
     *
     * @param labelSource the label source to filter by; must not be {@code null}
     * @return a list of review samples with the given label source; never
     *         {@code null}, may be empty
     */
    List<ReviewSample> findByLabelSource(LabelSource labelSource);

    /**
     * Returns all review samples carrying the given {@link SentimentLabel}.
     *
     * <p>Used to inspect label distribution and verify that the training
     * corpus is not heavily skewed towards one class.
     *
     * @param label the sentiment label to filter by; must not be {@code null}
     * @return a list of review samples with the given label; never {@code null},
     *         may be empty
     */
    List<ReviewSample> findByLabel(SentimentLabel label);

    // -------------------------------------------------------------------------
    // Count operations
    // -------------------------------------------------------------------------

    /**
     * Returns the total number of labelled review samples in the data store.
     *
     * <p>Used by {@code TrainingService} to verify that the minimum training
     * sample threshold has been reached before attempting a training run.
     *
     * @return the count of records where {@code label} is not {@code null}
     */
    long countLabelled();

    /**
     * Returns the number of review samples labelled by the given
     * {@link LabelSource}.
     *
     * <p>Used by {@code ActiveLearningService} to track how many Claude-labelled
     * samples have accumulated since the last retraining run, determining when
     * the retrain batch size threshold defined in
     * {@link io.github.amdespotopoulou.sentimentactivelearning.config.ActiveLearningProps}
     * ({@code active-learning.retrain-batch-size}) has been reached.
     *
     * @param labelSource the label source to count; must not be {@code null}
     * @return the count of records with the given label source
     */
    long countByLabelSource(LabelSource labelSource);

    // -------------------------------------------------------------------------
    // Existence checks
    // -------------------------------------------------------------------------

    /**
     * Returns whether a review sample with the given text already exists.
     *
     * <p>Used to produce a meaningful conflict error before attempting an
     * insert, rather than catching a raw database constraint violation.
     *
     * @param reviewText the review text to check; must not be {@code null}
     * @return {@code true} if a record with the given text already exists
     */
    boolean existsByReviewText(String reviewText);
}