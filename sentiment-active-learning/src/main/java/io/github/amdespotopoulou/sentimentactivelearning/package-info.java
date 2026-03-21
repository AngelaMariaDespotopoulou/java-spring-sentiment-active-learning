/**
 * Root package for the Sentiment Active Learning application.
 *
 * <p>This Spring Boot application classifies movie-review sentiment using a
 * Naive Bayes model built with Oracle Tribuo. It implements a full active-learning
 * cycle: the model detects its own prediction uncertainty, consults Claude AI as a
 * labelling oracle, persists the newly labelled samples, and retrains itself
 * continuously to improve accuracy over time.
 *
 * <h2>Package structure</h2>
 * <pre>
 * sentimentactivelearning
 * ├── config          — Spring configuration beans (Security, OpenAPI, HTTP clients, properties)
 * ├── exception       — Global exception handler and all custom exception types
 * ├── api             — REST layer
 * │   ├── controller  — Spring MVC controllers
 * │   └── response    — API error envelope ({@link io.github.amdespotopoulou.sentimentactivelearning.api.response.ApiErrorResponse})
 * ├── service         — Business logic layer
 * │   ├── core        — Classifier, training and active-learning orchestration
 * │   └── oracle      — Claude AI integration (labelling oracle)
 * ├── persistence     — Data access layer
 * │   ├── dao         — DAO interfaces (technology-agnostic contracts)
 * │   │   └── impl    — JPA-backed DAO implementations
 * │   ├── entity      — JPA entities
 * │   ├── repository  — Spring Data JPA repositories
 * │   └── listener    — JPA entity listeners for audit logging
 * └── commons         — Shared types used across layers
 *     ├── dto         — Request and response DTOs
 *     ├── enums       — Shared enumerations (SentimentLabel, LabelSource, ErrorCode)
 *     └── mapper      — MapStruct mappers (DTO ↔ entity)
 * </pre>
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning;