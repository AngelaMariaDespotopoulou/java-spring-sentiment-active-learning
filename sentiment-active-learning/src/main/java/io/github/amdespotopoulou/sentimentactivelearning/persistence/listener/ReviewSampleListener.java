package io.github.amdespotopoulou.sentimentactivelearning.persistence.listener;

import io.github.amdespotopoulou.sentimentactivelearning.persistence.entity.ReviewSample;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA entity listener for {@link ReviewSample} that logs persistence lifecycle
 * events for audit and diagnostic purposes.
 *
 * <p>This listener is registered on {@link ReviewSample} via
 * {@link jakarta.persistence.EntityListeners} and is invoked automatically by
 * the JPA provider at each lifecycle transition.
 *
 * <h2>Responsibilities</h2>
 * <p>This listener is purely observational — it logs events and nothing else.
 * It must never:
 * <ul>
 *   <li>Modify entity state.</li>
 *   <li>Invoke service methods or inject Spring beans.</li>
 *   <li>Interact with the database directly.</li>
 * </ul>
 *
 * <h2>Logged events</h2>
 * <ul>
 *   <li>{@link PrePersist} — logged at {@code DEBUG} level before the record
 *       is inserted, useful for tracing exactly what data enters the database.</li>
 *   <li>{@link PostPersist} — logged at {@code INFO} level after a successful
 *       insert, confirming the assigned database ID.</li>
 *   <li>{@link PostUpdate} — logged at {@code INFO} level after a successful
 *       update, recording any label or label-source changes for traceability
 *       across active-learning cycles.</li>
 * </ul>
 *
 * <p>Review text is intentionally truncated in log output to keep log lines
 * readable and to avoid flooding log files with large text bodies.
 *
 * @author Angela-Maria Despotopoulou
 */
@Slf4j
public class ReviewSampleListener {

    /**
     * Maximum number of characters of review text included in log messages.
     * Text longer than this is truncated with an ellipsis.
     */
    private static final int LOG_TEXT_MAX_LENGTH = 60;

    /**
     * Invoked by JPA before a new {@link ReviewSample} is inserted into the
     * database.
     *
     * <p>Logs the review text (truncated) and label source at {@code DEBUG}
     * level. Useful for tracing exactly what data is about to be persisted,
     * particularly when debugging seed data loading or oracle labelling.
     *
     * @param sample the {@link ReviewSample} entity about to be persisted;
     *               the {@code id} field will still be {@code null} at this point
     */
    @PrePersist
    public void onPrePersist(ReviewSample sample) {
        log.debug("Persisting new ReviewSample — text: '{}', label: {}, source: {}",
                truncate(sample.getReviewText()),
                sample.getLabel(),
                sample.getLabelSource());
    }

    /**
     * Invoked by JPA after a new {@link ReviewSample} has been successfully
     * inserted into the database.
     *
     * <p>Logs the assigned database ID and label at {@code INFO} level,
     * confirming the record was written successfully.
     *
     * @param sample the {@link ReviewSample} entity that was persisted;
     *               the {@code id} field is now populated with the
     *               database-assigned value
     */
    @PostPersist
    public void onPostPersist(ReviewSample sample) {
        log.info("ReviewSample persisted — id: {}, label: {}, source: {}",
                sample.getId(),
                sample.getLabel(),
                sample.getLabelSource());
    }

    /**
     * Invoked by JPA after an existing {@link ReviewSample} has been
     * successfully updated in the database.
     *
     * <p>Logs the record ID and the new label and label source at {@code INFO}
     * level. This is particularly useful for tracing label changes — for example,
     * when a {@link io.github.amdespotopoulou.sentimentactivelearning.commons.enums.LabelSource#CLAUDE}
     * label is overridden by a subsequent
     * {@link io.github.amdespotopoulou.sentimentactivelearning.commons.enums.LabelSource#MANUAL}
     * correction.
     *
     * @param sample the {@link ReviewSample} entity that was updated
     */
    @PostUpdate
    public void onPostUpdate(ReviewSample sample) {
        log.info("ReviewSample updated — id: {}, label: {}, source: {}, updatedAt: {}",
                sample.getId(),
                sample.getLabel(),
                sample.getLabelSource(),
                sample.getUpdatedAt());
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Truncates the given text to {@link #LOG_TEXT_MAX_LENGTH} characters,
     * appending an ellipsis if truncation occurred.
     *
     * <p>Used to keep log lines readable and avoid flooding log files with
     * the full content of large review texts.
     *
     * @param text the text to truncate; may be {@code null}
     * @return the truncated text, or {@code "(null)"} if {@code text} is {@code null}
     */
    private String truncate(String text) {
        if (text == null) {
            return "(null)";
        }
        return text.length() <= LOG_TEXT_MAX_LENGTH
                ? text
                : text.substring(0, LOG_TEXT_MAX_LENGTH) + "...";
    }
}