FROM docker.elastic.co/elasticsearch/elasticsearch-oss:6.2.4

COPY es-similarity.zip /usr/share/elasticsearch/plugins/es-similarity.zip
COPY es-kwconf.zip /usr/share/elasticsearch/plugins/es-kwconf.zip

WORKDIR /usr/share/elasticsearch/plugins
RUN unzip es-similarity.zip
RUN mv elasticsearch zorroa-similarity
RUN rm es-similarity.zip

RUN unzip es-kwconf.zip
RUN mv elasticsearch zorroa-kwconf
RUN rm es-kwconf.zip

WORKDIR /usr/share/elasticsearch
