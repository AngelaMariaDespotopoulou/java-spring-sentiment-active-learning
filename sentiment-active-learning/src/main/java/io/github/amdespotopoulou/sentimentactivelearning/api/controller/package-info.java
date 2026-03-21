/**
 * Spring MVC REST controllers for the Sentiment Active Learning application.
 *
 * <p>Controllers in this package are the sole entry points for inbound HTTP
 * requests. Each controller is annotated with
 * {@link org.springframework.web.bind.annotation.RestController} and
 * {@link org.springframework.web.bind.annotation.RequestMapping}, and is
 * documented with SpringDoc {@code @Tag} and {@code @Operation} annotations
 * so that the Swagger UI reflects the full API contract.
 *
 * <h2>Controllers</h2>
 * <ul>
 *   <li>{@code ReviewController} — endpoints for submitting reviews for
 *       classification, retrieving labelled samples, and manually supplying
 *       labels for uncertain predictions.</li>
 *   <li>{@code ModelController} — endpoints for triggering training runs,
 *       querying model statistics, and managing the active-learning cycle.</li>
 * </ul>
 *
 * <p>All error responses follow the
 * {@link io.github.amdespotopoulou.sentimentactivelearning.api.response.ApiErrorResponse}
 * envelope and are documented per-endpoint using {@code @ApiResponse} with
 * {@code @ExampleObject} showing the exact JSON shape for each error code.
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning.api.controller;
