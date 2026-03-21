/**
 * Business logic layer for the Sentiment Active Learning application.
 *
 * <p>This package and its sub-packages contain all domain behaviour. The service
 * layer sits between the API layer (which handles HTTP concerns) and the
 * persistence layer (which handles data access). It depends on DAO interfaces
 * only — never on JPA repositories or entities directly — so the persistence
 * technology can be swapped without touching service code.
 *
 * <p>Services are injected via constructor injection throughout, ensuring
 * immutability and testability without a running Spring context.
 *
 * <h2>Sub-packages</h2>
 * <ul>
 *   <li>{@code core}   — Classifier, training, and active-learning orchestration.</li>
 *   <li>{@code oracle} — Claude AI integration: HTTP calls, response parsing,
 *       and retry/fallback logic for the labelling oracle.</li>
 * </ul>
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning.service;
