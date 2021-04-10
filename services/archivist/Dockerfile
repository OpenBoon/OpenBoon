# syntax=docker/dockerfile:experimental
FROM maven:3.8-jdk-11-slim as build

ENV MXNET_OS linux

RUN mkdir /build
COPY ./ /build
WORKDIR /build
RUN --mount=type=cache,target=/root/.m2 mvn clean package -q -Dmaven.test.skip=true

FROM openjdk:11-jdk-slim
RUN apt-get update -y; apt-get upgrade -y; apt-get install -y --no-install-recommends python3


VOLUME /tmp

RUN groupadd -r archivist && useradd -m --no-log-init -r -g archivist archivist

COPY ./docker /service
COPY --from=build /build/archivist/target/archivist.jar /service/service.jar

USER archivist
EXPOSE 8080
WORKDIR /service
ENTRYPOINT ["./run_server.sh"]
