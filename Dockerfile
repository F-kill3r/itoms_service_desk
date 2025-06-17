FROM gradle:8-jdk21-alpine AS builder
WORKDIR /workspace
COPY settings.gradle build.gradle gradle/ ./
COPY src ./src
RUN gradle clean bootJar --no-daemon
FROM eclipse-temurin:21-jre-alpine
ENV TZ=Asia/Seoul
WORKDIR /app
COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-XX:+UseContainerSupport","-Dfile.encoding=UTF-8","-jar","/app/app.jar"]