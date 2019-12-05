FROM maven:3.6-jdk-11-slim as build

RUN mkdir /build
COPY . /build
WORKDIR /build
RUN mvn clean package -Dmaven.test.skip=true

FROM openjdk:11-jdk-slim
RUN apt-get update; apt-get install -y --no-install-recommends python3

RUN groupadd -r service && useradd --no-log-init -r -g service service

# copy WAR into image
RUN mkdir /service;
COPY ./docker/config /service/config

COPY --from=build /build/target/auth-server.jar /service/service.jar
COPY ./docker/run_server.sh /service

WORKDIR /service
USER service
EXPOSE 9090
CMD ["./run_server.sh"]
