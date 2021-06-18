# syntax=docker/dockerfile:experimental
FROM boonai/plugins-models:latest
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    tesseract-ocr \
    libtesseract-dev

COPY requirements.txt .
RUN --mount=type=cache,target=/root/.cache/pip pip3 install -r requirements.txt
RUN rm requirements.txt

WORKDIR /models
RUN curl -s https://storage.googleapis.com/zorroa-public/models/resnet.tar.gz | tar -xz

COPY pylib /boonai/pylib

WORKDIR /boonai
USER boonai

COPY model_setup.py .
RUN python3 model_setup.py > /dev/null 2>&1
