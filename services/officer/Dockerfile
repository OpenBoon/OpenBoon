# syntax=docker/dockerfile:experimental
FROM maven:3.6-jdk-11-slim as build

RUN mkdir /build
COPY . /build
WORKDIR /build
RUN --mount=type=cache,target=/root/.m2 mvn dependency:go-offline
RUN --mount=type=cache,target=/root/.m2 mvn clean package -Dmaven.test.skip=true

FROM openjdk:11-jdk-slim

RUN sed -e 's/$/ contrib non-free/' -i /etc/apt/sources.list
RUN apt-get update; apt-get install -y --no-install-recommends python3 libfontconfig \
    libfreetype6 xfonts-utils libexpat1 fonts-dejavu-core fontconfig-config libpng16-16 curl \
    cabextract wget
RUN groupadd -r analyst && useradd --no-log-init -r -g analyst analyst

RUN wget http://ftp.us.debian.org/debian/pool/contrib/m/msttcorefonts/ttf-mscorefonts-installer_3.8_all.deb
RUN dpkg -i ttf-mscorefonts-installer_3.8_all.deb
RUN rm ttf-mscorefonts-installer_3.8_all.deb

COPY ./docker /service

WORKDIR /service
COPY --from=build /build/target/officer.jar /service/service.jar

USER analyst
# run application with this command line

EXPOSE 7078
CMD ["./run_server.sh"]
