/**
 * Persistence layer for the Sentiment Active Learning application.
 *
 * <p>This package and its sub-packages provide all data-access capabilities.
 * The service layer interacts exclusively with the DAO interfaces defined in
 * {@code persistence.dao} — it never imports JPA repositories or entities
 * directly. This indirection means the underlying storage technology (H2,
 * PostgreSQL, MongoDB, etc.) can be replaced by providing a new
 * {@code persistence.dao.impl} implementation without touching any service code.
 *
 * <h2>Sub-packages</h2>
 * <ul>
 *   <li>{@code dao}        — Technology-agnostic DAO interfaces defining all
 *       data operations in business terms.</li>
 *   <li>{@code dao.impl}   — JPA-backed implementations of the DAO interfaces,
 *       delegating to Spring Data repositories.</li>
 *   <li>{@code entity}     — JPA entity classes mapped to H2 tables.</li>
 *   <li>{@code repository} — Spring Data JPA repository interfaces; used only
 *       by DAO implementations, never by the service layer.</li>
 *   <li>{@code listener}   — JPA entity listeners that log persistence lifecycle
 *       events (pre-persist, post-persist, post-update) for audit and
 *       diagnostics purposes.</li>
 * </ul>
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning.persistence;
