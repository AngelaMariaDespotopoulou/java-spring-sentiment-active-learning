/**
 * JPA-backed Data Access Object implementations for the
 * Sentiment Active Learning application.
 *
 * <p>Each class in this package implements a DAO interface from
 * {@link io.github.amdespotopoulou.sentimentactivelearning.persistence.dao}
 * using Spring Data JPA repositories as the underlying mechanism. This is the
 * only package in the application that is permitted to import JPA repositories
 * directly.
 *
 * <p>Implementations are annotated with {@link org.springframework.stereotype.Repository}
 * so that Spring wraps JPA exceptions in
 * {@link org.springframework.dao.DataAccessException} subclasses, providing a
 * consistent exception hierarchy to callers.
 *
 * <h2>Contents</h2>
 * <ul>
 *   <li>{@code ReviewSampleDaoImpl} — JPA implementation of
 *       {@link io.github.amdespotopoulou.sentimentactivelearning.persistence.dao.ReviewSampleDao},
 *       delegating to {@code ReviewSampleRepository} for all database
 *       interactions.</li>
 * </ul>
 *
 * <p>To migrate to a different persistence technology, replace the
 * implementations in this package and update the Spring bean wiring —
 * no other packages need to change.
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning.persistence.dao.impl;
