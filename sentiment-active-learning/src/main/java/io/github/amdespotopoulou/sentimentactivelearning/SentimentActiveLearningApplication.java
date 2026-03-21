package io.github.amdespotopoulou.sentimentactivelearning;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the Sentiment Active Learning application.
 *
 * <p>This Spring Boot application classifies movie-review sentiment using a
 * Naive Bayes model built with Oracle Tribuo. It implements a full active-learning
 * cycle: the model detects its own uncertainty, consults Claude AI as a labelling
 * oracle, and retrains itself continuously to improve accuracy over time.
 *
 * <p>The application exposes a REST API (documented via Swagger UI at
 * {@code /swagger-ui/index.html}) and persists labelled review samples in an
 * H2 database. Swagger UI is protected by HTTP Basic authentication; all other
 * endpoints are open for internal network access.
 *
 * <p>Supported Spring profiles:
 * <ul>
 *   <li>{@code dev}  — in-memory H2, debug logging, Swagger UI open</li>
 *   <li>{@code prod} — file-backed H2, info logging, Swagger UI secured</li>
 * </ul>
 *
 * @author Angela-Maria Despotopoulou
 * @see <a href="https://github.com/amdespotopoulou/java-spring-sentiment-active-learning">
 *      GitHub Repository</a>
 */
@SpringBootApplication
public class SentimentActiveLearningApplication {

    /**
     * Application entry point.
     *
     * <p>Bootstraps the Spring application context and starts the embedded
     * Tomcat server. All auto-configuration, component scanning, and bean
     * registration are triggered from this method.
     *
     * @param args command-line arguments passed to the JVM at startup;
     *             forwarded to {@link SpringApplication#run} and may be used
     *             to override any {@code application.properties} key via
     *             {@code --key=value} syntax
     */
    public static void main(String[] args) {
        SpringApplication.run(SentimentActiveLearningApplication.class, args);
    }

}