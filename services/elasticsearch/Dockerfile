FROM maven:3.6.2-jdk-11-slim as mvn_build
COPY es-kwconf es-kwconf
COPY es-similarity es-similarity
COPY pom.xml pom.xml
RUN mvn clean install -Dmaven.test.skip=true


FROM docker.elastic.co/elasticsearch/elasticsearch-oss:6.4.3

COPY --from=mvn_build ./es-kwconf/target/releases/es-kwconf.zip /usr/share/elasticsearch/plugins/es-kwconf.zip
COPY --from=mvn_build ./es-similarity/target/releases/es-similarity.zip /usr/share/elasticsearch/plugins/es-similarity.zip

WORKDIR /usr/share/elasticsearch/plugins

RUN unzip es-similarity.zip
RUN mv elasticsearch zorroa-similarity
RUN rm es-similarity.zip

RUN unzip es-kwconf.zip
RUN mv elasticsearch zorroa-kwconf
RUN rm es-kwconf.zip

RUN mkdir /usr/share/elasticsearch/plugins/prometheus-exporter
WORKDIR /usr/share/elasticsearch/plugins/prometheus-exporter

RUN wget https://distfiles.compuscene.net/elasticsearch/elasticsearch-prometheus-exporter-6.4.3.0.zip
RUN unzip elasticsearch-prometheus-exporter-6.4.3.0.zip
RUN rm elasticsearch-prometheus-exporter-6.4.3.0.zip

WORKDIR /usr/share/elasticsearch
