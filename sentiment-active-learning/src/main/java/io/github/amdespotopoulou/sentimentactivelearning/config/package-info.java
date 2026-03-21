/**
 * Spring configuration beans for the Sentiment Active Learning application.
 *
 * <p>This package contains all {@code @Configuration} classes that wire
 * infrastructure concerns. No business logic lives here.
 *
 * <h2>Contents</h2>
 * <ul>
 *   <li>{@code SecurityConfig} — HTTP Basic authentication protecting the Swagger UI
 *       and OpenAPI spec endpoints. All API and Actuator routes are left open.</li>
 *   <li>{@code OpenApiConfig} — SpringDoc OpenAPI 3 descriptor: title, contact,
 *       licence, and the {@code basicAuth} security scheme shown in Swagger UI.</li>
 *   <li>{@code ClaudeClientConfig} — Configures the {@link org.springframework.web.client.RestClient}
 *       bean used by the Claude AI oracle service, including base URL, API key header,
 *       and connect / read timeouts.</li>
 *   <li>{@code ActiveLearningProps} — {@code @ConfigurationProperties} binding for
 *       all {@code active-learning.*} keys defined in {@code application.properties}.
 *       Centralises threshold and batch-size tuning in one validated bean.</li>
 * </ul>
 *
 * @author Angela-Maria Despotopoulou
 */
package io.github.amdespotopoulou.sentimentactivelearning.config;
