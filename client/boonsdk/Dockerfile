# syntax = docker/dockerfile:experimental
FROM boonai/py3-base:latest

RUN apt-get update && apt-get -y upgrade

RUN mkdir /build
COPY ./ /build
WORKDIR /build
RUN --mount=type=cache,target=/root/.cache/pip pip3 install .
WORKDIR /boonai

RUN rm -rf /build
