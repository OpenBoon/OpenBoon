# syntax=docker/dockerfile:experimental
FROM maven:3.8-jdk-11-slim as build

RUN mkdir /build
COPY . /build
WORKDIR /build
RUN --mount=type=cache,target=/root/.m2 mvn clean install -q -Dmaven.test.skip=true

FROM openjdk:11-jdk-slim
RUN apt-get update; apt-get install -y --no-install-recommends python3

RUN groupadd -r service && useradd --no-log-init -r -g service service

COPY ./docker /service
COPY --from=build /build/auth-server/target/auth-server.jar /service/service.jar

WORKDIR /service
USER service
EXPOSE 9090
CMD ["./run_server.sh"]
