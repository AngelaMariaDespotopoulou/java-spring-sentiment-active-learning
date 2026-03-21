package io.github.amdespotopoulou.sentimentactivelearning.persistence.repository;

import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.LabelSource;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.SentimentLabel;
import io.github.amdespotopoulou.sentimentactivelearning.persistence.entity.ReviewSample;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for {@link ReviewSample} entities.
 *
 * <p>This interface is the lowest-level data access component in the
 * persistence stack. It is used exclusively by
 * {@link io.github.amdespotopoulou.sentimentactivelearning.persistence.dao.impl.ReviewSampleDaoImpl}
 * and must never be injected directly into service or controller classes.
 * All data access from the service layer must go through the DAO interface.
 *
 * <p>Spring Data JPA generates the implementation of all methods at startup.
 * Custom JPQL queries are provided where the derived query syntax would be
 * less readable or ambiguous.
 *
 * @author Angela-Maria Despotopoulou
 */
@Repository
public interface ReviewSampleRepository extends JpaRepository<ReviewSample, Long> {

    // -------------------------------------------------------------------------
    // Existence checks
    // -------------------------------------------------------------------------

    /**
     * Returns whether a review sample with the given text already exists.
     *
     * <p>Used by the DAO layer to enforce the uniqueness of review text before
     * attempting an insert, producing a meaningful
     * {@link io.github.amdespotopoulou.sentimentactivelearning.exception.ResourceConflictException}
     * rather than a raw database constraint violation.
     *
     * @param reviewText the review text to check; must not be {@code null}
     * @return {@code true} if a record with the given text exists
     */
    boolean existsByReviewText(String reviewText);

    // -------------------------------------------------------------------------
    // Single-record lookups
    // -------------------------------------------------------------------------

    /**
     * Returns the review sample with the given text, if one exists.
     *
     * <p>Used to retrieve an existing record when a duplicate submission is
     * detected, so the caller can return the existing record rather than an
     * error in idempotent scenarios.
     *
     * @param reviewText the exact review text to search for; must not be {@code null}
     * @return an {@link Optional} containing the matching record, or empty if none exists
     */
    Optional<ReviewSample> findByReviewText(String reviewText);

    // -------------------------------------------------------------------------
    // Training data queries
    // -------------------------------------------------------------------------

    /**
     * Returns all review samples that have been assigned a sentiment label,
     * regardless of label source.
     *
     * <p>This is the primary training data query — it fetches every record
     * ready for use in a Tribuo training run. Records with a {@code null}
     * label (awaiting labelling) are excluded.
     *
     * @return a list of all labelled review samples; never {@code null},
     *         may be empty
     */
    @Query("SELECT r FROM ReviewSample r WHERE r.label IS NOT NULL")
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
    @Query("SELECT r FROM ReviewSample r WHERE r.label IS NULL")
    List<ReviewSample> findAllUnlabelled();

    /**
     * Returns all review samples labelled by the given {@link LabelSource}.
     *
     * <p>Used to report on data provenance — for example, to count how many
     * samples were labelled by Claude vs manually, or to filter seed data
     * from evaluation metrics.
     *
     * @param labelSource the label source to filter by; must not be {@code null}
     * @return a list of review samples with the given label source; never
     *         {@code null}, may be empty
     */
    List<ReviewSample> findByLabelSource(LabelSource labelSource);

    /**
     * Returns all review samples carrying the given {@link SentimentLabel}.
     *
     * <p>Used to report on label distribution — for example, to verify that
     * the training corpus is not heavily skewed towards one class, which would
     * degrade Naive Bayes performance.
     *
     * @param label the sentiment label to filter by; must not be {@code null}
     * @return a list of review samples with the given label; never {@code null},
     *         may be empty
     */
    List<ReviewSample> findByLabel(SentimentLabel label);

    // -------------------------------------------------------------------------
    // Count queries
    // -------------------------------------------------------------------------

    /**
     * Returns the total number of labelled review samples in the database.
     *
     * <p>Used by {@code TrainingService} to verify that the minimum training
     * sample threshold defined in
     * {@link io.github.amdespotopoulou.sentimentactivelearning.config.ActiveLearningProps}
     * ({@code active-learning.min-training-samples}) has been reached before
     * attempting a training run.
     *
     * @return the count of records where {@code label} is not {@code null}
     */
    @Query("SELECT COUNT(r) FROM ReviewSample r WHERE r.label IS NOT NULL")
    long countLabelled();

    /**
     * Returns the number of review samples labelled by the given
     * {@link LabelSource}.
     *
     * <p>Used by {@code ActiveLearningService} to track how many Claude-labelled
     * samples have accumulated since the last retraining run, determining when
     * the retrain batch size threshold has been reached.
     *
     * @param labelSource the label source to count; must not be {@code null}
     * @return the count of records with the given label source
     */
    long countByLabelSource(LabelSource labelSource);
}