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

COPY pylib /zps/pylib
COPY requirements.txt .

RUN pip3 install --no-cache-dir -r requirements.txt
RUN rm requirements.txt


ARG ffmver=4.2.1-amd64
COPY ffmpeg-$ffmver-static.tar.xz .
RUN tar -xJf ffmpeg-$ffmver-static.tar.xz
RUN cp ffmpeg-$ffmver-static/ffmpeg /usr/bin
RUN cp ffmpeg-$ffmver-static/ffprobe /usr/bin
RUN rm -rf ffmpeg-$ffmver-static*

USER zorroa
