# Build stage
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

ARG APP_VERSION=1.0.0

# Copy project structure (context is project root)
COPY pom.xml pom.xml
COPY claims-domain/pom.xml claims-domain/pom.xml
COPY claims-domain/src claims-domain/src
COPY claims-api/pom.xml claims-api/pom.xml
COPY claims-api/src claims-api/src

# Build and package
RUN apk add --no-cache maven && mvn clean package -DskipTests -pl claims-domain,claims-api

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

# Create non-root user
RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

COPY --from=builder /app/claims-api/target/claims-api-1.0.0-SNAPSHOT.jar app.jar

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
