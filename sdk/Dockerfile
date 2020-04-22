# syntax = docker/dockerfile:experimental
FROM zmlp/py3-client:latest

RUN apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    libjpeg-turbo8-dev \
    libpng-dev \
    libraw-dev \
    libtiff-dev \
    libpugixml-dev \
    libhdf5-dev \
    libwebp-dev \
    libgif-dev \
    libilmbase-dev \
    libdlib-dev \
    openimageio-tools \
    xz-utils \
    libglib2.0-0 \
    imagemagick \
     --no-install-recommends

RUN mkdir /build
COPY ./ /build
WORKDIR /build
RUN pip3 install .

ARG ffmver=4.2.1-amd64
COPY ffmpeg-$ffmver-static.tar.xz .
RUN tar -xJf ffmpeg-$ffmver-static.tar.xz
RUN cp ffmpeg-$ffmver-static/ffmpeg /usr/bin
RUN cp ffmpeg-$ffmver-static/ffprobe /usr/bin
RUN rm -rf ffmpeg-$ffmver-static*

WORKDIR /zps

RUN rm -rf /build
