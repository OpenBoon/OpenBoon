# syntax=docker/dockerfile:experimental
FROM zmlp/plugins-base:latest

RUN apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    python3-dev \
    build-essential \
    nvidia-cuda-toolkit \
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
