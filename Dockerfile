# ==============================================================
# Multi-Stage Dockerfile for Secure Document Vault
# ==============================================================

# Stage 1: Build stage with Gradle and Eclipse Temurin JDK 17
FROM eclipse-temurin:17-jdk-alpine AS builder
WORKDIR /workspace

# Copy build descriptor and gradle wrapper
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

# Make gradlew executable & pre-fetch dependencies (layer caching)
RUN chmod +x ./gradlew && ./gradlew --version

# Copy source code and build production jar
COPY src ./src
RUN ./gradlew clean bootJar --no-daemon -x test

# Stage 2: Production Runtime with minimal JRE footprint & non-root user
FROM eclipse-temurin:17-jre-alpine AS runner
WORKDIR /app

# Create dedicated non-root user and group (UID/GID 10001) for container security
RUN addgroup -g 10001 -S vaultgroup && \
    adduser -u 10001 -S vaultuser -G vaultgroup && \
    mkdir -p /app/vault-storage && \
    chown -R vaultuser:vaultgroup /app

# Copy compiled JAR from builder stage
COPY --from=builder --chown=vaultuser:vaultgroup /workspace/build/libs/secure-document-vault.jar app.jar

# Expose HTTP application port
EXPOSE 8080

# Switch to non-root user
USER vaultuser:vaultgroup

# Container health check querying Spring Boot Actuator probe
HEALTHCHECK --interval=30s --timeout=3s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# Environment configurations
ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
