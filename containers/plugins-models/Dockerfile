# syntax=docker/dockerfile:experimental
FROM boonai/boondocks:latest

RUN apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    python3-dev \
    build-essential \
    nvidia-cuda-toolkit

COPY requirements.txt .
RUN pip3 install -r requirements.txt
RUN rm requirements.txt
RUN pip3 uninstall -y protobuf
RUN pip3 install protobuf

USER boonai
WORKDIR /home/boonai
RUN curl -s https://storage.googleapis.com/zorroa-public/models/models.tar.gz | tar -xz

WORKDIR /boonai
USER root
RUN mkdir /models

