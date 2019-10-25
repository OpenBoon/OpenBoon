FROM docker.elastic.co/elasticsearch/elasticsearch-oss:6.4.3

COPY ./dist /usr/share/elasticsearch/plugins/

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
