/**
 * JPA entity listeners for the Sentiment Active Learning application.
 *
 * <p>Listeners in this package are registered on JPA entities via the
 * {@link jakarta.persistence.EntityListeners} annotation. They intercept
 * JPA lifecycle callbacks ({@code @PrePersist}, {@code @PostPersist},
 * {@code @PostUpdate}) to log persistence events at the appropriate level
 * using SLF4J, providing an audit trail and an early-warning signal for
 * unexpected data operations.
 *
 * <p>Listeners contain no business logic. They must not modify entity state,
 * invoke service methods, or interact with the database. Their sole
 * responsibility is observability — logging and diagnostics.
 *
 * <h2>Contents</h2>
 * <ul>
 *   <li>{@code ReviewSampleListener} — logs create and update events on
 *       {@link io.github.amdespotopoulou.sentimentactivelearning.persistence.entity.ReviewSample},
 *       including the assigned label and label source, to aid debugging of
 *       the active-learning pipeline.</li>
 * </ul>
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning.persistence.listener;
