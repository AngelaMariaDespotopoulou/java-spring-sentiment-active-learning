# =============================================================================
# Dockerfile — sentiment-active-learning
#
# Multi-stage build:
#   Stage 1 (build)  — Maven + JDK 21 compiles the source and produces the
#                       fat JAR via spring-boot:repackage.
#   Stage 2 (runtime) — Eclipse Temurin JRE 21 runs the JAR. No build tools,
#                       no source code, no Maven cache in the final image.
#
# The model directory is mounted as a Docker volume at runtime so that the
# trained Tribuo model file persists across container restarts.
#
# Build:
#   docker build -t sentiment-active-learning:1.0.0 .
#
# Run (standalone, without docker-compose):
#   docker run -p 8080:8080 \
#     --env-file .env \
#     -v sal-model:/app/model \
#     sentiment-active-learning:1.0.0
# =============================================================================


# -----------------------------------------------------------------------------
# Stage 1 — Build
# Uses the full JDK image so Maven can compile and package the application.
# This stage is discarded after the build; it never appears in the final image.
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /workspace

# Copy dependency descriptors first. Docker caches this layer separately from
# the source layer — unchanged dependencies are not re-downloaded on every build.
COPY pom.xml .

# Download all dependencies into the local Maven cache.
# The -B flag runs Maven in non-interactive batch mode (no ANSI colours).
# The -q flag suppresses most output — remove for verbose builds.
RUN apk add --no-cache maven \
    && mvn -B -q dependency:go-offline

# Copy the full source tree and build the fat JAR.
# spring-boot:repackage is bound to the package phase automatically.
COPY src/ src/
RUN mvn -B -q package -DskipTests

# Unpack the fat JAR into layered directories so Docker can cache
# application layers independently of dependency layers.
RUN mkdir -p target/extracted \
    && java -Djarmode=layertools \
            -jar target/sentiment-active-learning-*.jar \
            extract --destination target/extracted


# -----------------------------------------------------------------------------
# Stage 2 — Runtime
# Uses the slim JRE image. No compiler, no Maven, no source files.
# The layered JAR extraction above means unchanged dependency layers
# are cached by Docker and not re-uploaded on every deploy.
# -----------------------------------------------------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

# ── Security: run as a non-root user ─────────────────────────────────────────
# Running as root inside a container is a security risk even with Docker's
# namespace isolation. A dedicated user limits the blast radius of any exploit.
RUN addgroup -S salgroup && adduser -S saluser -G salgroup

# ── Application directory ─────────────────────────────────────────────────────
WORKDIR /app

# ── Layered JAR copy ─────────────────────────────────────────────────────────
# Copy the four Spring Boot JAR layers from the build stage in order of
# change frequency (least → most). Docker caches each COPY as a separate
# layer, so a code-only change only invalidates the last layer.
COPY --from=build --chown=saluser:salgroup \
    /workspace/target/extracted/dependencies/ ./
COPY --from=build --chown=saluser:salgroup \
    /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=build --chown=saluser:salgroup \
    /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=saluser:salgroup \
    /workspace/target/extracted/application/ ./

# ── Model directory ───────────────────────────────────────────────────────────
# Create the directory where the trained model file will be written.
# Mounted as a named Docker volume in docker-compose so the file persists
# across container restarts and deployments.
RUN mkdir -p /app/model && chown -R saluser:salgroup /app/model

# ── Switch to non-root user ───────────────────────────────────────────────────
USER saluser

# ── Port ─────────────────────────────────────────────────────────────────────
# The application listens on the port defined by APP_PORT (default 8080).
# Expose is documentation only — the actual port binding is in docker-compose.
EXPOSE 8080

# ── Health check ─────────────────────────────────────────────────────────────
# Docker checks this every 30 seconds after an initial 45-second grace period.
# The container is marked UNHEALTHY if three consecutive checks fail.
# Depends on the Spring Boot Actuator health endpoint being accessible.
HEALTHCHECK --interval=30s \
            --timeout=10s \
            --start-period=45s \
            --retries=3 \
    CMD wget -qO- http://localhost:${APP_PORT:-8080}/actuator/health \
        | grep -q '"status":"UP"' || exit 1

# ── Entrypoint ────────────────────────────────────────────────────────────────
# JVM flags:
#   -XX:+UseContainerSupport      — respect cgroup CPU/memory limits (default
#                                   on JDK 11+, explicit here for clarity)
#   -XX:MaxRAMPercentage=75.0     — cap heap at 75% of container memory limit
#                                   to leave room for the JVM itself and OS
#   -Djava.security.egd=...       — use /dev/urandom instead of /dev/random to
#                                   avoid blocking on entropy in containers
#   SPRING_PROFILES_ACTIVE env var — Spring Boot reads this automatically from
#                           the container environment set by docker-compose.
#                           No -D flag needed; shell variable substitution does
#                           not work inside exec-form ENTRYPOINT JSON arrays.
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "org.springframework.boot.loader.launch.JarLauncher"]