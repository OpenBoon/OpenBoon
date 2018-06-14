FROM docker.elastic.co/elasticsearch/elasticsearch:6.2.4
COPY es-similarity-0.40.0-SNAPSHOT.zip /usr/share/elasticsearch/plugins/es-similarity-0.40.0-SNAPSHOT.zip
WORKDIR /usr/share/elasticsearch/plugins
RUN unzip es-similarity-0.40.0-SNAPSHOT.zip
RUN mv elasticsearch zorroa-similarity
RUN rm es-similarity-0.40.0-SNAPSHOT.zip
