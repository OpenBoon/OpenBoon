FROM zorroaevi/8-jre-slim-gcloud
VOLUME /tmp
MAINTAINER Zorroa <support@zorroa.com>

ARG project
ENV project ${project:-zorroa-evi-dev}
ARG gcs_config
ENV GCS_CONFIG ${gcs_config}

RUN mkdir /service

COPY target/analyst.jar /service/analyst.jar
COPY run_server.sh run_server.sh

EXPOSE 8080
ENTRYPOINT ["./run_server.sh"]
