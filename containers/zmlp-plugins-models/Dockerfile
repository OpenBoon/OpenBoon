# syntax=docker/dockerfile:experimental
FROM zmlp/plugins-base:latest

RUN --mount=type=cache,target=/var/cache/apt,sharing=locked --mount=type=cache,target=/var/lib/apt,sharing=locked \
    apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    python3-dev \
    build-essential \
    curl

COPY requirements.txt .
RUN pip3 install -r requirements.txt
RUN rm requirements.txt
RUN pip3 uninstall -y protobuf
RUN pip3 install protobuf

USER zorroa
WORKDIR /home/zorroa
RUN curl https://storage.googleapis.com/zorroa-public/models/models.tar.gz -O -J -L
RUN tar -zxvf models.tar.gz
RUN rm models.tar.gz

USER root
RUN mkdir /models
COPY resnet-152 /models/resnet-152

