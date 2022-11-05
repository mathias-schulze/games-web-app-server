# syntax = docker/dockerfile:1.2

FROM gradle:6.9-jdk11-alpine AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN --mount=type=secret,id=env_sh,dst=/home/gradle/src/env_sh && chmod +x env.sh && ./env.sh
RUN gradle build sonarqube -x check --no-daemon 

FROM openjdk:11-jre-slim

EXPOSE 8080

RUN mkdir /app

COPY --from=build /home/gradle/src/build/libs/*.jar /app/spring-boot-application.jar

ENTRYPOINT ["java", "-jar","/app/spring-boot-application.jar"]