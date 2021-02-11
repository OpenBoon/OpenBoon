# syntax=docker/dockerfile:experimental
FROM zmlp/plugins-models:latest

RUN apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    tesseract-ocr \
    libtesseract-dev

COPY requirements.txt .
RUN --mount=type=cache,target=/root/.cache/pip pip3 install -r requirements.txt
RUN rm requirements.txt

COPY pylib /zvi/pylib
WORKDIR /models

RUN curl https://storage.googleapis.com/zorroa-public/models/resnet.tar.gz | tar -xz

WORKDIR /zvi
USER zorroa


