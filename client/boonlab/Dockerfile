# syntax = docker/dockerfile:experimental
FROM boonai/boonsdk:latest

# apt install a bunch of binary packages
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends apt-utils
RUN apt-get install -y --no-install-recommends build-essential libjpeg-dev libpng-dev cmake \
    libtiff-dev libopencv-dev python3-opencv libsm6 lsb-release curl

RUN mkdir /build
COPY ./ /build
WORKDIR /build
RUN --mount=type=cache,target=/root/.cache/pip pip3 install .
WORKDIR /boonai

RUN rm -rf /build
