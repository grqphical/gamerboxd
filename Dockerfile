FROM maven:3.9-eclipse-temurin-25-alpine AS build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine
COPY --from=build /target/gamerboxd-0.0.1-SNAPSHOT.jar gamerboxd.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","gamerboxd.jar"]