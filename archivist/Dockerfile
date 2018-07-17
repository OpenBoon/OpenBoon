FROM openjdk:8-jre-slim
VOLUME /tmp
MAINTAINER Zorroa <support@zorroa.com>
ARG project
ARG gcs_config
ENV GCS_CONFIG ${gcs_config}
ENV project ${project:-zorroa-evi-dev}
RUN mkdir /service
RUN mkdir /service/ext

COPY target/archivist.jar /service/app.jar
COPY deploy/${project}/ext service/ext
COPY run_server.sh run_server.sh

RUN apt-get update -y
RUN apt-get install curl python -y
RUN curl -sSL https://sdk.cloud.google.com | bash

EXPOSE 8080
ENTRYPOINT ["./run_server.sh"]
