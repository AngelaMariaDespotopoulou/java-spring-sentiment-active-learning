/**
 * Technology-agnostic Data Access Object interfaces for the
 * Sentiment Active Learning application.
 *
 * <p>Interfaces in this package define the data operations the service layer
 * requires, expressed entirely in business terms — no JPA annotations,
 * no {@code Page}, no {@code Optional} leaking from the repository tier.
 * The service layer depends only on these interfaces; it never imports
 * anything from {@code persistence.repository} or {@code persistence.entity}.
 *
 * <p>This contract-first approach means that migrating from H2 to any other
 * data store requires only a new implementation class in
 * {@code persistence.dao.impl} and a Spring bean swap — zero changes to
 * service or API code.
 *
 * <h2>Contents</h2>
 * <ul>
 *   <li>{@code ReviewSampleDao} — all read and write operations on
 *       {@link io.github.amdespotopoulou.sentimentactivelearning.persistence.entity.ReviewSample}
 *       records, including retrieval of unlabelled samples for the
 *       active-learning pipeline.</li>
 * </ul>
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning.persistence.dao;
