# Build stage
FROM gradle:8.6-jdk21 AS build
WORKDIR /app
COPY . .
RUN gradle build --no-daemon

# Run stage
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# Copy the built artifact from build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Set environment variables
ENV JAVA_OPTS="-Xms512m -Xmx1024m"
ENV SPRING_PROFILES_ACTIVE="prod"

# Expose the application port
EXPOSE 8080

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"] 