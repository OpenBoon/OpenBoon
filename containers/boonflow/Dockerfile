# syntax = docker/dockerfile:experimental
FROM boonai/boonsdk:latest
ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get -y upgrade && apt-get -y install \
    libmagic-dev \
    libjpeg-turbo8-dev \
    libpng-dev \
    libraw-dev \
    libtiff-dev \
    libpugixml-dev \
    libhdf5-dev \
    libwebp-dev \
    libgif-dev \
    libilmbase24 \
    openimageio-tools \
    xz-utils \
    libglib2.0-0 \
    exiftool \
    mediainfo \
    imagemagick \
    poppler-utils \
    wget \
    curl \
    unzip

RUN mkdir /build
COPY ./ /build
WORKDIR /build
RUN --mount=type=cache,target=/root/.cache/pip pip3 install .

ARG ffmver=4.2.2-amd64
COPY ffmpeg-amd64-static.tar.xz .
RUN tar -xJf ffmpeg-amd64-static.tar.xz
RUN cp ffmpeg-$ffmver-static/ffmpeg /usr/bin
RUN cp ffmpeg-$ffmver-static/ffprobe /usr/bin
RUN rm -rf ffmpeg*

WORKDIR /boonai

RUN rm -rf /build
