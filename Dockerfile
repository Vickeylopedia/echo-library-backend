# ---- Build stage ----
FROM gradle:8.10-jdk21 AS build
WORKDIR /home/gradle/src
COPY --chown=gradle:gradle . .
RUN gradle shadowJar --no-daemon

# ---- Runtime stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /home/gradle/src/build/libs/echo-library-backend-1.0.0-all.jar /app/app.jar
EXPOSE 8080
CMD ["java", "-jar", "/app/app.jar"]
