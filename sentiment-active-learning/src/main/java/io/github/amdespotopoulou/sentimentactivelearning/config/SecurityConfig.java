package io.github.amdespotopoulou.sentimentactivelearning.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security configuration for the Sentiment Active Learning application.
 *
 * <h2>Security model</h2>
 * <p>Only the Swagger UI and OpenAPI spec endpoints are protected with HTTP Basic
 * authentication. All API endpoints ({@code /api/**}) and the Actuator health
 * endpoint remain publicly accessible, as they are designed to be called by
 * internal services and tools inside the same Docker network.
 *
 * <h2>Credentials</h2>
 * <p>The single Swagger user's credentials are injected from the environment at
 * startup via the {@code swagger.auth.username} and {@code swagger.auth.password}
 * properties. In production these resolve from environment variables defined in
 * the Docker Compose {@code .env} file. The password is BCrypt-hashed before
 * being stored in the in-memory {@link UserDetailsService}, so the plaintext
 * value is never held in memory after startup.
 *
 * <h2>Session policy</h2>
 * <p>The security filter chain is stateless ({@link SessionCreationPolicy#STATELESS}).
 * The browser caches the Basic Auth header for the duration of the tab session,
 * which is sufficient for Swagger UI usage without requiring server-side session
 * state.
 *
 * @author Angela-Maria Despotopoulou
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Swagger UI username, resolved from {@code swagger.auth.username}.
     *
     * <p>In production this resolves from the {@code SWAGGER_USERNAME} environment
     * variable via Docker Compose. In development it is hardcoded in
     * {@code application-dev.properties}. The plaintext value is BCrypt-hashed
     * at startup and not retained thereafter.
     */
    @Value("${swagger.auth.username}")
    private String swaggerUsername;

    /**
     * Swagger UI password, resolved from {@code swagger.auth.password}.
     *
     * <p>In production this resolves from the {@code SWAGGER_PASSWORD} environment
     * variable via Docker Compose. In development it is hardcoded in
     * {@code application-dev.properties}. Stored only as a BCrypt hash after startup.
     */
    @Value("${swagger.auth.password}")
    private String swaggerPassword;

    /**
     * Configures the HTTP security filter chain.
     *
     * <ul>
     *   <li>CSRF disabled — the API is stateless and consumed by non-browser clients
     *       or by the Swagger UI, which does not rely on CSRF tokens.</li>
     *   <li>Swagger UI and OpenAPI spec paths require HTTP Basic authentication.</li>
     *   <li>All other requests — API endpoints and Actuator — are permitted without
     *       authentication.</li>
     *   <li>Session policy is {@code STATELESS} — no {@code JSESSIONID} is created.</li>
     * </ul>
     *
     * @param http the {@link HttpSecurity} builder provided by Spring Security
     * @return the configured {@link SecurityFilterChain}
     * @throws Exception if the Spring Security DSL encounters a configuration error
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs"
                        ).authenticated()
                        .anyRequest().permitAll()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    /**
     * Registers the single in-memory Swagger UI user.
     *
     * <p>The password is BCrypt-hashed on first call and the plaintext value is
     * not retained. Spring Security uses the {@link PasswordEncoder} bean declared
     * below to verify credentials at each request.
     *
     * @return an {@link InMemoryUserDetailsManager} containing the Swagger user
     */
    @Bean
    public UserDetailsService userDetailsService() {
        var swaggerUser = User.builder()
                .username(swaggerUsername)
                .password(passwordEncoder().encode(swaggerPassword))
                .roles("SWAGGER")
                .build();

        return new InMemoryUserDetailsManager(swaggerUser);
    }

    /**
     * Declares the {@link BCryptPasswordEncoder} used to hash and verify the
     * Swagger UI password.
     *
     * <p>BCrypt is chosen because it is deliberately slow (adaptive cost factor),
     * making offline brute-force attacks against a leaked hash impractical.
     *
     * @return a {@link BCryptPasswordEncoder} with the default cost factor (10)
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}