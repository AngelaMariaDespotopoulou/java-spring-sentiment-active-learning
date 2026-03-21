package io.github.amdespotopoulou.sentimentactivelearning.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * SpringDoc OpenAPI / Swagger UI configuration for the Sentiment Active Learning
 * application.
 *
 * <p>This class registers the {@link OpenAPI} bean that SpringDoc uses to build
 * the OpenAPI 3 specification served at {@code /v3/api-docs} and rendered in the
 * interactive Swagger UI at {@code /swagger-ui/index.html}.
 *
 * <h2>Metadata</h2>
 * <p>The API title, version and description are defined in
 * {@code application.properties} under the {@code api.info.*} keys. The contact
 * name, e-mail address and URL are also externalised under {@code api.contact.*}
 * and resolved from environment variables in production, so they can be updated
 * without recompiling the application.
 *
 * <h2>Security scheme</h2>
 * <p>The Swagger UI itself is protected at the HTTP level by Spring Security
 * (see {@link SecurityConfig}). The OpenAPI spec additionally declares a
 * {@code basicAuth} security scheme so that the "Authorise" button appears in
 * the UI, allowing testers to enter credentials and execute live API calls
 * directly from the browser.
 *
 * <p>The {@code basicAuth} scheme declared here applies to the documented API
 * endpoints. The Swagger UI login challenge itself is handled entirely by
 * Spring Security's HTTP Basic filter.
 *
 * @author Angela-Maria Despotopoulou
 */
@Configuration
public class OpenApiConfig {

    /**
     * API title shown in the Swagger UI header.
     * Resolved from {@code api.info.title} in {@code application.properties}.
     */
    @Value("${api.info.title}")
    private String apiTitle;

    /**
     * API version shown in the Swagger UI header.
     * Resolved from {@code api.info.version} in {@code application.properties}.
     */
    @Value("${api.info.version}")
    private String apiVersion;

    /**
     * API description shown in the Swagger UI overview panel.
     * Resolved from {@code api.info.description} in {@code application.properties}.
     */
    @Value("${api.info.description}")
    private String apiDescription;

    /**
     * Contact name shown in the Swagger UI info section.
     * Resolved from {@code api.contact.name}, which maps to the
     * {@code API_CONTACT_NAME} environment variable in production.
     */
    @Value("${api.contact.name}")
    private String contactName;

    /**
     * Contact e-mail address shown in the Swagger UI info section.
     * Resolved from {@code api.contact.email}, which maps to the
     * {@code API_CONTACT_EMAIL} environment variable in production.
     */
    @Value("${api.contact.email}")
    private String contactEmail;

    /**
     * Contact URL shown in the Swagger UI info section.
     * Resolved from {@code api.contact.url}, which maps to the
     * {@code API_CONTACT_URL} environment variable in production.
     */
    @Value("${api.contact.url}")
    private String contactUrl;

    /**
     * Builds and registers the custom {@link OpenAPI} descriptor bean.
     *
     * <p>Defines API metadata (title, version, description, contact, licence)
     * and declares a global HTTP Basic security scheme applied to all operations.
     * All metadata values are injected from {@code application.properties} via
     * {@code @Value} so they can be updated without recompiling.
     *
     * @return the fully configured {@link OpenAPI} instance used by SpringDoc
     */
    @Bean
    public OpenAPI sentimentLearnerOpenApi() {
        final String basicAuthSchemeName = "basicAuth";

        return new OpenAPI()
                .info(new Info()
                        .title(apiTitle)
                        .version(apiVersion)
                        .description(apiDescription)
                        .contact(new Contact()
                                .name(contactName)
                                .email(contactEmail)
                                .url(contactUrl))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .components(new Components()
                        .addSecuritySchemes(basicAuthSchemeName, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("basic")
                                .description(
                                        "HTTP Basic credentials for Swagger UI access. " +
                                                "Configured via SWAGGER_USERNAME / SWAGGER_PASSWORD " +
                                                "environment variables in production.")))
                .addSecurityItem(new SecurityRequirement().addList(basicAuthSchemeName));
    }
}