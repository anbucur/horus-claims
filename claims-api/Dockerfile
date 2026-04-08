FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY claims-domain/pom.xml claims-domain/
COPY claims-api/pom.xml claims-api/
COPY pom.xml ./
RUN apk add --no-cache maven
RUN mvn dependency:go-offline -pl claims-domain,claims-api
COPY claims-domain/src claims-domain/src
COPY claims-api/src claims-api/src
RUN mvn package -pl claims-domain,claims-api -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/claims-api/target/*.jar app.jar
COPY --from=builder /app/claims-domain/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
