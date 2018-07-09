FROM docker.elastic.co/elasticsearch/elasticsearch:6.2.4
COPY es-similarity.zip /usr/share/elasticsearch/plugins/es-similarity.zip
WORKDIR /usr/share/elasticsearch/plugins
RUN unzip es-similarity.zip
RUN mv elasticsearch zorroa-similarity
RUN rm es-similarity.zip
WORKDIR /usr/share/elasticsearch

