package io.github.amdespotopoulou.sentimentactivelearning.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

/**
 * Strongly-typed configuration properties for the Claude AI oracle HTTP client.
 *
 * <p>This class binds all {@code claude.api.*} keys defined in
 * {@code application.properties} into a single validated bean. Any
 * misconfiguration — a blank API key, a zero timeout, etc. — causes the
 * application to fail fast at startup with a clear error message rather than
 * producing a cryptic runtime failure on the first Claude API call.
 *
 * <h2>Available properties</h2>
 * <pre>
 * claude.api.url                (String,  required)
 * claude.api.key                (String,  required, injected from CLAUDE_API_KEY env var)
 * claude.api.model              (String,  required)
 * claude.api.max-tokens         (int,     min 1,    default 16)
 * claude.api.connect-timeout-ms (long,    min 1,    default 5000)
 * claude.api.read-timeout-ms    (long,    min 1,    default 15000)
 * </pre>
 *
 * <h2>Security note</h2>
 * <p>The {@code claude.api.key} value resolves from the {@code CLAUDE_API_KEY}
 * environment variable at runtime. It is never hardcoded in any committed file.
 * In local development it is set in {@code application-dev.properties}, which
 * is excluded from version control via {@code .gitignore}.
 *
 * @author Angela-Maria Despotopoulou
 */
@Getter
@Setter
@Validated
@Configuration
@ConfigurationProperties(prefix = "claude.api")
public class ClaudeProps {

    /**
     * Base URL of the Anthropic Claude REST API.
     * Must include the full path to the messages endpoint.
     * Example: {@code https://api.anthropic.com/v1/messages}
     */
    @NotBlank(message = "claude.api.url must not be blank")
    private String url;

    /**
     * Anthropic API key used to authenticate all Claude API requests.
     *
     * <p>Resolved from the {@code CLAUDE_API_KEY} environment variable.
     * Never hardcode this value in any committed file.
     */
    @NotBlank(message = "claude.api.key must not be blank")
    private String key;

    /**
     * Claude model identifier to use for all labelling requests.
     *
     * <p>Centralised here so the model can be upgraded across the entire
     * application by changing a single property, without touching any code.
     * Example: {@code claude-sonnet-4-20250514}
     */
    @NotBlank(message = "claude.api.model must not be blank")
    private String model;

    /**
     * Maximum number of tokens Claude may return per labelling request.
     *
     * <p>Kept deliberately low because we only need a single
     * {@code POSITIVE} or {@code NEGATIVE} token in the response.
     * A higher value wastes tokens and increases cost. Defaults to {@code 16}.
     */
    @Min(value = 1, message = "claude.api.max-tokens must be at least 1")
    private int maxTokens = 16;

    /**
     * Maximum time in milliseconds to wait when establishing a TCP connection
     * to the Anthropic API endpoint.
     *
     * <p>If a connection cannot be established within this time, the call is
     * aborted and a
     * {@link io.github.amdespotopoulou.sentimentactivelearning.exception.ClaudeApiException}
     * is thrown. Defaults to {@code 5000} ms (5 seconds).
     */
    @Min(value = 1, message = "claude.api.connect-timeout-ms must be at least 1")
    private long connectTimeoutMs = 5000;

    /**
     * Maximum time in milliseconds to wait for a response after a connection
     * has been established to the Anthropic API endpoint.
     *
     * <p>If no response arrives within this time, the call is aborted and a
     * {@link io.github.amdespotopoulou.sentimentactivelearning.exception.ClaudeApiException}
     * is thrown. Defaults to {@code 15000} ms (15 seconds).
     */
    @Min(value = 1, message = "claude.api.read-timeout-ms must be at least 1")
    private long readTimeoutMs = 15000;
}