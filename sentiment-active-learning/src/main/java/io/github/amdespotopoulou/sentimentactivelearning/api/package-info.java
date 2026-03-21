/**
 * REST API layer for the Sentiment Active Learning application.
 *
 * <p>This package and its sub-packages form the outermost layer of the
 * application. They are the only layer permitted to handle HTTP concerns —
 * request binding, response serialisation, HTTP status codes, and Swagger
 * documentation annotations.
 *
 * <p>Controllers delegate all business logic to the service layer and never
 * access the persistence layer directly. DTOs cross this boundary in both
 * directions; entities never leave the service or persistence layers.
 *
 * <h2>Sub-packages</h2>
 * <ul>
 *   <li>{@code controller} — Spring MVC {@code @RestController} classes.</li>
 *   <li>{@code response}   — The canonical {@code ApiErrorResponse} error
 *       envelope returned for all non-2xx responses.</li>
 * </ul>
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning.api;
