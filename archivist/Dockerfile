FROM zorroaevi/8-jre-slim-gcloud
VOLUME /tmp
MAINTAINER Zorroa <support@zorroa.com>

ARG project
ENV project ${project:-zorroa-evi-dev}
ARG gcs_config
ENV GCS_CONFIG ${gcs_config}

RUN groupadd -r archivist && useradd --no-log-init -r -g archivist archivist

RUN mkdir /service
COPY target/archivist.jar /service/archivist.jar

# Create a couple directories for service extensions.
RUN mkdir /extensions
RUN mkdir /extensions/inactive
RUN mkdir /extensions/active

# Copy any build extensions to the inactive folder.
COPY ext/*.jar /extensions/inactive/
# Change ownership so startup script can copy files.
RUN chown -R archivist:archivist /extensions

COPY run_server.sh run_server.sh

USER archivist
EXPOSE 8080
ENTRYPOINT ["./run_server.sh"]
