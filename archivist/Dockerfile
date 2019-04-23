FROM zorroaevi/8-jre-slim-gcloud
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
COPY target/archivist.jar /service/archivist.jar

# Create a couple directories for service extensions.
RUN mkdir /extensions
RUN mkdir /extensions/inactive
RUN mkdir /extensions/active

# Copy any build extensions to the inactive folder.
COPY ext/*.jar /extensions/inactive/

# Copy over startup scripts for docker.
COPY ./docker/jvm_options_parser /jvm_options_parser
COPY ./docker/jvm.options /config/jvm.options
COPY ./docker/run_server.sh /run_server.sh
COPY ./docker/rotated_logs.xml /config/rotated_logs.xml
COPY ./docker/application.properties /config/application.properties

# Change ownership
RUN chown -R archivist:archivist /extensions
RUN chown -R archivist:archivist /config
RUN chown -R archivist:archivist /logs
RUN chown -R archivist:archivist /shared

USER archivist
EXPOSE 8080
ENTRYPOINT ["./run_server.sh"]
