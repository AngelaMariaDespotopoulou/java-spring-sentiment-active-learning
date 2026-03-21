package io.github.amdespotopoulou.sentimentactivelearning.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Spring Data JPA configuration for the Sentiment Active Learning application.
 *
 * <h2>Auditing</h2>
 * <p>{@link EnableJpaAuditing} activates Spring Data's automatic population of
 * {@code @CreatedDate} and {@code @LastModifiedDate} fields on JPA entities.
 * Without this annotation, both fields on
 * {@link io.github.amdespotopoulou.sentimentactivelearning.persistence.entity.ReviewSample}
 * would remain {@code null} after persistence operations, causing
 * {@code NOT NULL} constraint violations at the database level.
 *
 * <h2>Repository scanning</h2>
 * <p>{@link EnableJpaRepositories} explicitly points Spring Data at the
 * {@code persistence.repository} package, ensuring only our own repository
 * interfaces are scanned and no unintended repositories from transitive
 * dependencies are registered.
 *
 * @author Angela-Maria Despotopoulou
 */
@Configuration
@EnableJpaAuditing
@EnableJpaRepositories(
        basePackages = "io.github.amdespotopoulou.sentimentactivelearning.persistence.repository"
)
public class JpaAuditingConfig {
}