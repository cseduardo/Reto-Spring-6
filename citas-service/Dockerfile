# Fase 1: Construcción de la aplicación con Maven
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

# Fase 2: Creación de la imagen final con solo lo necesario para ejecutar
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY --from=build /app/target/citas-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
