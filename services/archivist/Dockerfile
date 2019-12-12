FROM maven:3.6-jdk-11-slim as build

RUN mkdir /build
COPY ./ /build
WORKDIR /build
RUN mvn clean package -Dmaven.test.skip=true

FROM openjdk:11-jdk-slim
MAINTAINER Zorroa <support@zorroa.com>

VOLUME /tmp

RUN groupadd -r archivist && useradd -m --no-log-init -r -g archivist archivist

COPY ./docker /service
COPY --from=build /build/archivist/target/archivist.jar /service/service.jar

USER archivist
EXPOSE 8080
WORKDIR /service
ENTRYPOINT ["./run_server.sh"]
