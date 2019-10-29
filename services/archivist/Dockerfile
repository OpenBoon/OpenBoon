from maven:3.6-jdk-11-slim as build

RUN mkdir /build
COPY ./ /build
WORKDIR /build
RUN mvn clean package -Dmaven.test.skip=true

FROM zorroaevi/11-jre-slim-gcloud
MAINTAINER Zorroa <support@zorroa.com>

VOLUME /tmp

ARG project
ENV project ${project:-zorroa-evi-dev}
ARG gcs_config
ENV GCS_CONFIG ${gcs_config}

RUN groupadd -r archivist && useradd -m --no-log-init -r -g archivist archivist

RUN mkdir /shared
RUN mkdir /logs
RUN mkdir /config
RUN mkdir /service

COPY --from=build /build/archivist/target/archivist.jar /service/archivist.jar

# Copy over startup scripts for docker.
COPY ./archivist/docker/jvm_options_parser /jvm_options_parser
COPY ./archivist/docker/jvm.options /config/jvm.options
COPY ./archivist/docker/run_server.sh /run_server.sh
COPY ./archivist/docker/rotated_logs.xml /config/rotated_logs.xml
COPY ./archivist/docker/application.properties /config/application.properties


# Change perms
RUN chmod -R 777 /config
RUN chmod -R 777 /logs
RUN chmod -R 777 /shared
RUN chmod -R 777 /service

USER archivist

EXPOSE 8080
ENTRYPOINT ["./run_server.sh"]
