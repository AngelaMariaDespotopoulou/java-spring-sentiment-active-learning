/**
 * Spring Data JPA repository interfaces for the Sentiment Active Learning application.
 *
 * <p>Repositories in this package extend
 * {@link org.springframework.data.jpa.repository.JpaRepository} and are used
 * exclusively by DAO implementations in
 * {@link io.github.amdespotopoulou.sentimentactivelearning.persistence.dao.impl}.
 * No class outside the persistence layer should import or inject these interfaces.
 *
 * <h2>Contents</h2>
 * <ul>
 *   <li>{@code ReviewSampleRepository} — provides CRUD operations and custom
 *       query methods (e.g. find by label, find unlabelled samples) for the
 *       {@link io.github.amdespotopoulou.sentimentactivelearning.persistence.entity.ReviewSample}
 *       entity.</li>
 * </ul>
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning.persistence.repository;
