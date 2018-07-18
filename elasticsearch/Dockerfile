FROM docker.elastic.co/elasticsearch/elasticsearch-oss:6.2.4
ARG node_type
COPY configs/${node_type}.yml /usr/share/elasticsearch/config/elasticsearch.yml
COPY es-similarity.zip /usr/share/elasticsearch/plugins/es-similarity.zip
WORKDIR /usr/share/elasticsearch/plugins
RUN unzip es-similarity.zip
RUN mv elasticsearch zorroa-similarity
RUN rm es-similarity.zip
WORKDIR /usr/share/elasticsearch

