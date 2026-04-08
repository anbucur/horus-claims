FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy project structure (context is project root)
COPY pom.xml pom.xml
COPY claims-domain/pom.xml claims-domain/pom.xml
COPY claims-domain/src claims-domain/src
COPY claims-api/pom.xml claims-api/pom.xml
COPY claims-api/src claims-api/src

# Build and package
RUN apk add --no-cache maven
RUN mvn package -DskipTests -pl claims-domain,claims-api

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/claims-api/target/claims-api-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
