FROM zorroaevi/8-jre-slim-gcloud
VOLUME /tmp
MAINTAINER Zorroa <support@zorroa.com>

ARG project
ENV project ${project:-zorroa-evi-dev}
ARG gcs_config
ENV GCS_CONFIG ${gcs_config}

RUN mkdir /service
COPY target/archivist.jar /service/archivist.jar

# Create a couple directories for service extensions.
RUN mkdir /service-ext
RUN mkdir /service-ext-installed
COPY ext/* /service-ext/

COPY run_server.sh run_server.sh

RUN groupadd -r archivist && useradd --no-log-init -r -g archivist archivist
USER archivist

EXPOSE 8080
ENTRYPOINT ["./run_server.sh"]
