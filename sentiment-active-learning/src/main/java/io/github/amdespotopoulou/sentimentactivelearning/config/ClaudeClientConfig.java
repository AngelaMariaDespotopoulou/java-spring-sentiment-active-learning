package io.github.amdespotopoulou.sentimentactivelearning.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * Spring configuration for the Claude AI oracle HTTP client.
 *
 * <p>This class produces a single {@link RestClient} bean pre-configured with
 * everything needed to communicate with the Anthropic Claude REST API:
 *
 * <ul>
 *   <li><b>Base URL</b> — {@code https://api.anthropic.com/v1/messages},
 *       resolved from {@code claude.api.url} in {@code application.properties}.
 *       All relative paths used in {@code ClaudeOracleService} resolve against
 *       this base, so the full URL never needs to be repeated.</li>
 *   <li><b>Authentication header</b> — {@code x-api-key} carrying the Anthropic
 *       API key resolved from the {@code CLAUDE_API_KEY} environment variable.
 *       Added as a default header so every request is authenticated automatically,
 *       without {@code ClaudeOracleService} ever handling the raw key.</li>
 *   <li><b>API version header</b> — {@code anthropic-version: 2023-06-01},
 *       required by the Anthropic API to select the stable request/response
 *       contract. Pinned here so it can be updated in one place.</li>
 *   <li><b>Content-Type</b> — {@code application/json} set as a default header
 *       since all Claude API requests carry a JSON body.</li>
 *   <li><b>Timeouts</b> — connect and read timeouts resolved from
 *       {@code claude.api.connect-timeout-ms} and {@code claude.api.read-timeout-ms},
 *       preventing the application from hanging indefinitely if the Anthropic
 *       API is slow or unreachable.</li>
 * </ul>
 *
 * <p>The resulting bean is injected into
 * {@code ClaudeOracleService} by name ({@code claudeRestClient}), keeping the
 * oracle service free of any HTTP infrastructure concerns.
 *
 * @author Angela-Maria Despotopoulou
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class ClaudeClientConfig {

    /**
     * Anthropic API version header name, required on every request.
     */
    private static final String ANTHROPIC_VERSION_HEADER = "anthropic-version";

    /**
     * Anthropic API version value. Pinned to the stable contract version.
     * Update here when migrating to a newer Anthropic API contract.
     */
    private static final String ANTHROPIC_VERSION_VALUE = "2023-06-01";

    /**
     * Anthropic API key header name.
     */
    private static final String ANTHROPIC_API_KEY_HEADER = "x-api-key";

    /**
     * Bound configuration properties for the Claude API client.
     * Injected via constructor by {@code @RequiredArgsConstructor}.
     */
    private final ClaudeProps claudeProps;

    /**
     * Produces a {@link RestClient} bean pre-configured for all Claude AI
     * oracle calls.
     *
     * <p>The bean is named {@code claudeRestClient} so it can be injected
     * unambiguously in {@code ClaudeOracleService} alongside any other
     * {@link RestClient} beans that may be added in future.
     *
     * <p>Default headers — API key, API version, and content type — are set
     * once here. {@code ClaudeOracleService} only needs to supply the request
     * body; all infrastructure concerns are handled by this bean.
     *
     * @return a fully configured, immutable {@link RestClient} instance
     */
    @Bean(name = "claudeRestClient")
    public RestClient claudeRestClient() {
        log.info("Initialising Claude REST client — base URL: {}, model: {}, " +
                        "connectTimeout: {}ms, readTimeout: {}ms",
                claudeProps.getUrl(),
                claudeProps.getModel(),
                claudeProps.getConnectTimeoutMs(),
                claudeProps.getReadTimeoutMs());

        return RestClient.builder()
                .baseUrl(claudeProps.getUrl())
                .defaultHeader(ANTHROPIC_API_KEY_HEADER, claudeProps.getKey())
                .defaultHeader(ANTHROPIC_VERSION_HEADER, ANTHROPIC_VERSION_VALUE)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(buildRequestFactory())
                .build();
    }

    /**
     * Builds a {@link org.springframework.http.client.SimpleClientHttpRequestFactory}
     * with the connect and read timeouts sourced from {@link ClaudeProps}.
     *
     * <p>Using a factory with explicit timeouts prevents the {@link RestClient}
     * from blocking indefinitely if the Anthropic API is slow or unreachable,
     * ensuring the active-learning pipeline degrades gracefully rather than
     * freezing the application thread.
     *
     * @return a timeout-aware HTTP request factory
     */
    private org.springframework.http.client.SimpleClientHttpRequestFactory buildRequestFactory() {
        var factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(claudeProps.getConnectTimeoutMs()));
        factory.setReadTimeout(Duration.ofMillis(claudeProps.getReadTimeoutMs()));
        return factory;
    }
}