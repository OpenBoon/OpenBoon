# syntax=docker/dockerfile:experimental
FROM boonai/plugins-models:latest

RUN apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    cython3 \
    git \
    curl \
    unzip

COPY requirements.txt .
RUN --mount=type=cache,target=/root/.cache/pip pip3 install -r requirements.txt
RUN rm requirements.txt

COPY pylib /boonai/pylib
