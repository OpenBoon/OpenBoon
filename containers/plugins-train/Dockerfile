# syntax=docker/dockerfile:experimental
FROM boonai/plugins-models:latest

USER boonai
WORKDIR /home/boonai
RUN curl -s https://storage.googleapis.com/zorroa-public/models/keras_base.tar.gz | tar -xz
WORKDIR /boonai

USER root

RUN apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    cython3 \
    git \
    curl \
    unzip

COPY requirements.txt .
RUN --mount=type=cache,target=/root/.cache/pip pip3 install -r requirements.txt
RUN rm requirements.txt

COPY pylib /boonai/pylib

USER boonai
