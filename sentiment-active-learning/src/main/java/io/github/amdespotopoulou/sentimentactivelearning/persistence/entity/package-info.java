/**
 * JPA entity classes for the Sentiment Active Learning application.
 *
 * <p>Entities in this package are mapped to H2 database tables via JPA
 * annotations. They are internal to the persistence layer and must never be
 * returned directly from service or controller methods. The service layer
 * works with DTOs from {@code commons.dto}; MapStruct mappers in
 * {@code commons.mapper} handle the conversion.
 *
 * <h2>Contents</h2>
 * <ul>
 *   <li>{@code ReviewSample} — the central entity representing a single movie
 *       review together with its assigned sentiment label, the source of that
 *       label (manual, Claude oracle, or seed data), and audit timestamps.</li>
 * </ul>
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning.persistence.entity;
