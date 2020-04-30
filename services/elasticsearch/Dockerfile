FROM maven:3.6.2-jdk-11-slim as mvn_build
COPY es-kwconf es-kwconf
COPY es-similarity es-similarity
COPY es-similarity-query es-similarity-query
COPY pom.xml pom.xml
RUN mvn clean install -Dmaven.test.skip=true

FROM docker.elastic.co/elasticsearch/elasticsearch:7.6.2

COPY ./docker/jvm.options /usr/share/elasticsearch/config
COPY --from=mvn_build ./es-kwconf/target/releases/es-kwconf.zip /usr/share/elasticsearch/plugins/es-kwconf.zip
COPY --from=mvn_build ./es-similarity/target/releases/es-similarity.zip /usr/share/elasticsearch/plugins/es-similarity.zip
COPY --from=mvn_build /usr/local/openjdk-11 /usr/local/openjdk-11
RUN yum install -y unzip wget

ENV PATH="/usr/local/openjdk-11/bin:${PATH}"
ENV JAVA_HOME="/usr/local/openjdk-11"

WORKDIR /usr/share/elasticsearch/plugins

RUN unzip es-similarity.zip
RUN mv elasticsearch zorroa-similarity
RUN rm es-similarity.zip

RUN unzip es-kwconf.zip
RUN mv elasticsearch zorroa-kwconf
RUN rm es-kwconf.zip

RUN yum remove -y unzip wget

# Uncomment when this version comes out.
#RUN mkdir /usr/share/elasticsearch/plugins/prometheus-exporter
#WORKDIR /usr/share/elasticsearch/plugins/prometheus-exporter

#RUN wget https://distfiles.compuscene.net/elasticsearch/elasticsearch-prometheus-exporter-7.5.1.0.zip
#RUN unzip elasticsearch-prometheus-exporter-7.5.1.0.zip
#RUN rm elasticsearch-prometheus-exporter-7.5.1.0.zip

# Installs plugins to allow recovery snapshots to be stored in gcs or s3.
RUN yes | elasticsearch-plugin install repository-gcs -s
RUN yes | elasticsearch-plugin install repository-s3 -s

WORKDIR /usr/share/elasticsearch

# Add credentials for backup snapshot bucket
#COPY service-account-test.json service-account-test.json
#RUN ./bin/elasticsearch-keystore add-file --force gcs.client.default.credentials_file service-account-test.json
#RUN rm ./service-account-test.json

# Setup for AWS Minio backup repository
ARG MINIO_ACCESS_KEY
ARG MINIO_SECRET_KEY
ARG MINIO_URL
ARG MINIO_DEFAULT_PROTOCOL

RUN echo "s3.client.default.endpoint: $MINIO_URL" >> ./config/elasticsearch.yml
RUN echo "s3.client.default.protocol: $MINIO_DEFAULT_PROTOCOL" >> ./config/elasticsearch.yml
RUN echo "s3.client.default.path_style_access: true"  >> ./config/elasticsearch.yml
RUN echo $MINIO_ACCESS_KEY | ./bin/elasticsearch-keystore add --stdin --force s3.client.default.access_key
RUN echo $MINIO_SECRET_KEY | ./bin/elasticsearch-keystore add --stdin --force s3.client.default.secret_key

