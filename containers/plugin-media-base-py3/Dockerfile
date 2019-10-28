FROM ubuntu:bionic
USER root

RUN mkdir /src
WORKDIR src

RUN apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    python3 \
    python3-pip \
    python3-setuptools \
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
    wget \
    xz-utils \
     --no-install-recommends

COPY ffmpeg-4.2.1-amd64-static.tar.xz .
RUN tar -xJf ffmpeg-4.2.1-amd64-static.tar.xz
RUN cp ffmpeg-4.2.1-amd64-static/ffmpeg /usr/bin
RUN cp ffmpeg-4.2.1-amd64-static/ffprobe /usr/bin
RUN rm -rf ffmpeg-4.2.1-amd64-static*

RUN pip3 install opencv-python
