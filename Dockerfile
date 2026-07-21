# Build Stage
FROM gradle:8.8-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle buildFatJar --no-daemon --info

# Run Stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /home/gradle/src/build/libs/*-all.jar /app/echo-library-backend.jar

# Configure default container environment
ENV PORT=8080
ENV KTOR_ENV=production

# Expose the port (Railway will override this with its dynamic PORT at runtime)
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/echo-library-backend.jar"]
