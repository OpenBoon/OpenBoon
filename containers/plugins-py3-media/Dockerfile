FROM plugins-py3-base:latest
USER root

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
    wget \
    xz-utils \
     --no-install-recommends

RUN pip3 install opencv-python

RUN mkdir /src
WORKDIR src

COPY ffmpeg-4.2.1-amd64-static.tar.xz .
RUN tar -xJf ffmpeg-4.2.1-amd64-static.tar.xz
RUN cp ffmpeg-4.2.1-amd64-static/ffmpeg /usr/bin
RUN cp ffmpeg-4.2.1-amd64-static/ffprobe /usr/bin
WORKDIR /
RUN rm -rf /src



