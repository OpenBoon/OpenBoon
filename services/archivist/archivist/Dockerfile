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
COPY target/archivist.jar /service/archivist.jar

# Copy over startup scripts for docker.
COPY ./docker/jvm_options_parser /jvm_options_parser
COPY ./docker/jvm.options /config/jvm.options
COPY ./docker/run_server.sh /run_server.sh
COPY ./docker/rotated_logs.xml /config/rotated_logs.xml
COPY ./docker/application.properties /config/application.properties


# Change perms
RUN chmod -R 777 /config
RUN chmod -R 777 /logs
RUN chown -R 777 /shared

USER archivist

EXPOSE 8080
ENTRYPOINT ["./run_server.sh"]
