package io.github.amdespotopoulou.sentimentactivelearning.commons.enums;

/**
 * Tracks the origin of the sentiment label assigned to a review sample.
 *
 * <p>Every labelled
 * {@link io.github.amdespotopoulou.sentimentactivelearning.persistence.entity.ReviewSample}
 * carries a {@code LabelSource} so that the active-learning pipeline can reason
 * about data provenance, report labelling statistics, and filter samples by
 * origin when needed (e.g. evaluating model performance on human-labelled data
 * only vs. Claude-labelled data only).
 *
 * <p>The string value of each constant is persisted to the database via
 * {@code @Enumerated(EnumType.STRING)} — do not rename constants without a
 * corresponding database migration.
 *
 * @author Angela-Maria Despotopoulou
 */
public enum LabelSource {

    /**
     * The label was provided as part of the bundled seed dataset loaded at
     * application startup.
     *
     * <p>Seed samples form the initial training corpus that allows the model
     * to produce its first predictions before any active-learning cycles have run.
     */
    SEED,

    /**
     * The label was supplied by a human operator via the REST API.
     *
     * <p>Manual labels are considered the highest-quality signal and are
     * always included in training runs.
     */
    MANUAL,

    /**
     * The label was assigned by the Claude AI oracle during an active-learning
     * cycle, because the classifier's confidence fell below the configured
     * uncertainty threshold.
     *
     * <p>Claude-labelled samples are treated as high-quality but not authoritative;
     * they may be overridden by a subsequent manual label on the same review.
     */
    CLAUDE
}
