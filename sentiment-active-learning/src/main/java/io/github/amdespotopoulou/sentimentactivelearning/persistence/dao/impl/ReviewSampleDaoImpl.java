package io.github.amdespotopoulou.sentimentactivelearning.persistence.dao.impl;

import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.ErrorCode;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.LabelSource;
import io.github.amdespotopoulou.sentimentactivelearning.commons.enums.SentimentLabel;
import io.github.amdespotopoulou.sentimentactivelearning.exception.ResourceConflictException;
import io.github.amdespotopoulou.sentimentactivelearning.exception.ResourceNotFoundException;
import io.github.amdespotopoulou.sentimentactivelearning.persistence.dao.ReviewSampleDao;
import io.github.amdespotopoulou.sentimentactivelearning.persistence.entity.ReviewSample;
import io.github.amdespotopoulou.sentimentactivelearning.persistence.repository.ReviewSampleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * JPA-backed implementation of {@link ReviewSampleDao}.
 *
 * <p>This class is the only component in the application that is permitted
 * to import and interact with {@link ReviewSampleRepository} directly. All
 * service-layer data access must go through {@link ReviewSampleDao} — never
 * through this class or its repository directly.
 *
 * <p>Annotated with {@link org.springframework.stereotype.Repository} so that
 * Spring wraps JPA persistence exceptions in
 * {@link org.springframework.dao.DataAccessException} subclasses, providing a
 * consistent, technology-agnostic exception hierarchy to callers. Business-level
 * exceptions ({@link ResourceNotFoundException},
 * {@link ResourceConflictException}) are thrown explicitly where appropriate,
 * before any database interaction occurs, so that error messages are always
 * readable and actionable.
 *
 * @author Angela-Maria Despotopoulou
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class ReviewSampleDaoImpl implements ReviewSampleDao {

    /**
     * Spring Data JPA repository delegate.
     * Injected via constructor by {@code @RequiredArgsConstructor}.
     */
    private final ReviewSampleRepository repository;

    // -------------------------------------------------------------------------
    // Write operations
    // -------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     *
     * <p>Checks for a duplicate review text before attempting the insert.
     * If a record with the same text already exists, throws a
     * {@link ResourceConflictException} immediately so the caller receives a
     * clean, readable error rather than a raw database constraint violation.
     */
    @Override
    public ReviewSample save(ReviewSample reviewSample) {
        if (repository.existsByReviewText(reviewSample.getReviewText())) {
            throw new ResourceConflictException(
                    ErrorCode.REVIEW_ALREADY_EXISTS,
                    "A review sample with the same text already exists.");
        }
        return repository.save(reviewSample);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Verifies that the record exists before attempting the update,
     * throwing a {@link ResourceNotFoundException} if no record is found
     * for the given {@code id}.
     */
    @Override
    public ReviewSample update(ReviewSample reviewSample) {
        if (!repository.existsById(reviewSample.getId())) {
            throw new ResourceNotFoundException(
                    ErrorCode.REVIEW_NOT_FOUND,
                    "Review sample with ID " + reviewSample.getId() + " was not found.");
        }
        return repository.save(reviewSample);
    }

    // -------------------------------------------------------------------------
    // Read operations — single record
    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public Optional<ReviewSample> findById(Long id) {
        return repository.findById(id);
    }

    /** {@inheritDoc} */
    @Override
    public Optional<ReviewSample> findByReviewText(String reviewText) {
        return repository.findByReviewText(reviewText);
    }

    // -------------------------------------------------------------------------
    // Read operations — collections
    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public List<ReviewSample> findAll() {
        return repository.findAll();
    }

    /** {@inheritDoc} */
    @Override
    public List<ReviewSample> findAllLabelled() {
        return repository.findAllLabelled();
    }

    /** {@inheritDoc} */
    @Override
    public List<ReviewSample> findAllUnlabelled() {
        return repository.findAllUnlabelled();
    }

    /** {@inheritDoc} */
    @Override
    public List<ReviewSample> findByLabelSource(LabelSource labelSource) {
        return repository.findByLabelSource(labelSource);
    }

    /** {@inheritDoc} */
    @Override
    public List<ReviewSample> findByLabel(SentimentLabel label) {
        return repository.findByLabel(label);
    }

    // -------------------------------------------------------------------------
    // Count operations
    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public long countLabelled() {
        return repository.countLabelled();
    }

    /** {@inheritDoc} */
    @Override
    public long countByLabelSource(LabelSource labelSource) {
        return repository.countByLabelSource(labelSource);
    }

    // -------------------------------------------------------------------------
    // Existence checks
    // -------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public boolean existsByReviewText(String reviewText) {
        return repository.existsByReviewText(reviewText);
    }
}