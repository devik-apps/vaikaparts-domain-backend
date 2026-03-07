FROM gradle:8.10-jdk21-alpine AS build

WORKDIR /app

RUN apk add --no-cache bash maven

COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./
COPY .shell ./.shell

RUN --mount=type=secret,id=github_actor \
    --mount=type=secret,id=github_token \
    GITHUB_ACTOR=$(cat /run/secrets/github_actor) \
    GITHUB_TOKEN=$(cat /run/secrets/github_token) \
    ./gradlew resolveClientVersion --no-daemon -PskipClientPublish

RUN --mount=type=secret,id=github_actor \
    --mount=type=secret,id=github_token \
    GITHUB_ACTOR=$(cat /run/secrets/github_actor) \
    GITHUB_TOKEN=$(cat /run/secrets/github_token) \
    ./gradlew dependencies --no-daemon -PskipClientPublish

COPY doc ./doc
COPY src ./src

RUN --mount=type=secret,id=github_actor \
    --mount=type=secret,id=github_token \
    GITHUB_ACTOR=$(cat /run/secrets/github_actor) \
    GITHUB_TOKEN=$(cat /run/secrets/github_token) \
    ./gradlew bootJar --no-daemon -PskipClientPublish

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache wget && \
    addgroup -S spring && \
    adduser -S spring -G spring && \
    rm -rf /var/cache/apk/*

COPY --from=build /app/build/libs/*.jar app.jar
COPY docker-start.sh /app/docker-start.sh

RUN chmod +x /app/docker-start.sh && \
    chown -R spring:spring /app

USER spring:spring
EXPOSE 9090

HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:9090/actuator/health || exit 1

ENTRYPOINT ["/app/docker-start.sh"]
