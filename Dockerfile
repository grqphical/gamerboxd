# Build stage
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# Copy Maven wrapper and pom first for better layer caching
COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw mvnw.cmd ./
COPY src/ src/

# Build the application (skip tests for faster Docker builds)
RUN ./mvnw -B package -DskipTests

# Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app

# Copy the built jar from the build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
