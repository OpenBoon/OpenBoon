FROM maven:3.6-jdk-11-slim as mvn_build
COPY es-kwconf es-kwconf
COPY es-similarity es-similarity
COPY es-similarity-query es-similarity-query
COPY pom.xml pom.xml
RUN mvn clean install -q -Dmaven.test.skip=true

FROM docker.elastic.co/elasticsearch/elasticsearch:7.11.0

COPY ./docker/jvm.options /usr/share/elasticsearch/config
COPY ./docker/docker-entrypoint.sh /usr/local/bin
RUN chown elasticsearch:root /usr/local/bin/docker-entrypoint.sh
RUN chmod 755 /usr/local/bin/docker-entrypoint.sh

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

# Installs plugins to allow recovery snapshots to be stored in gcs or s3.
RUN yes | elasticsearch-plugin install repository-gcs -s
RUN yes | elasticsearch-plugin install repository-s3 -s

WORKDIR /usr/share/elasticsearch

# Setup for AWS Minio backup repository
ARG MINIO_ACCESS_KEY=qwerty123
ARG MINIO_SECRET_KEY=123qwerty

ENV MINIO_URL http://minio:9000
ENV MINIO_DEFAULT_PROTOCOL http

RUN echo 's3.client.default.endpoint: ${MINIO_URL}' >> ./config/elasticsearch.yml
RUN echo 's3.client.default.protocol: ${MINIO_DEFAULT_PROTOCOL}' >> ./config/elasticsearch.yml
RUN echo 's3.client.default.path_style_access: true'  >> ./config/elasticsearch.yml
RUN echo $MINIO_ACCESS_KEY | ./bin/elasticsearch-keystore add --stdin --force s3.client.default.access_key
RUN echo $MINIO_SECRET_KEY | ./bin/elasticsearch-keystore add --stdin --force s3.client.default.secret_key

