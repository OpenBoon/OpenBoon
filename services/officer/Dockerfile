FROM maven:3.6-jdk-11-slim as build

RUN mkdir /build
COPY . /build
WORKDIR /build
RUN mvn clean package -Dmaven.test.skip=true

FROM openjdk:11-jdk-slim

RUN sed -e 's/$/ contrib non-free/' -i /etc/apt/sources.list
RUN apt-get update; apt-get install -y --no-install-recommends libfontconfig libfreetype6 ttf-mscorefonts-installer libexpat1 fonts-dejavu-core fontconfig-config libpng16-16 curl
RUN groupadd -r analyst && useradd --no-log-init -r -g analyst analyst

# copy WAR into image
RUN mkdir /service
WORKDIR /service
COPY --from=build --chown=analyst:analyst /build/target/officer.jar /service
COPY --chown=analyst:analyst run_server.sh /service

USER analyst
# run application with this command line

CMD ["./run_server.sh"]
