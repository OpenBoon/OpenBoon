# syntax=docker/dockerfile:experimental
FROM maven:3.6-jdk-11-slim as build

RUN mkdir /build
COPY ./ /build
WORKDIR /build
RUN --mount=type=cache,target=/root/.m2 mvn clean package -Dmaven.test.skip=true

FROM openjdk:11-jdk-slim
RUN apt-get update; apt-get install -y --no-install-recommends python3


VOLUME /tmp

RUN groupadd -r archivist && useradd -m --no-log-init -r -g archivist archivist

COPY ./docker /service
COPY --from=build /build/archivist/target/archivist.jar /service/service.jar

COPY ./models /models

USER archivist
EXPOSE 8080
WORKDIR /service
ENTRYPOINT ["./run_server.sh"]
