FROM zorroaevi/8-jre-slim-gcloud
VOLUME /tmp
MAINTAINER Zorroa <support@zorroa.com>

ARG project
ENV project ${project:-zorroa-evi-dev}
ARG gcs_config
ENV GCS_CONFIG ${gcs_config}

RUN mkdir /service

RUN groupadd -r archivist && useradd --no-log-init -r -g archivist archivist
USER archivist

COPY target/archivist.jar /service/archivist.jar
COPY run_server.sh run_server.sh

EXPOSE 8080
ENTRYPOINT ["./run_server.sh"]
