# This Dockerfile creates an image for building and testing the
# zorroa-plugin-sdk repository.


FROM ubuntu:xenial

RUN apt-get -qq update && apt-get -qq upgrade && apt-get -qq install \
    libboost-all-dev \
    libilmbase12 \
    libopenexr22 \
    libopencv-dev \
    libraw15 \
    libgif7 \
    libdcmtk5 \
    ffmpeg \
    maven \
    wget \
    && rm -rf /var/lib/apt/lists/*

# There are currently no working apt packages for Oracle's Java SDK 8.
# The following downloads the tarball from Zorroa's download server
# and installs it.

RUN wget https://dl.zorroa.com/public/jdk-8u162-linux-x64.tar.gz && \
    tar zxf jdk-8u162-linux-x64.tar.gz && \
    rm jdk-8u162-linux-x64.tar.gz

ENV PATH="/jdk1.8.0_162/bin:${PATH}"

RUN wget https://dl.zorroa.com/public/oiio-1.8.7-Ubuntu16.04-zorroa-amd64.deb && \
    apt --quiet install -y ./oiio-1.8.7-Ubuntu16.04-zorroa-amd64.deb && \
    rm oiio-1.8.7-Ubuntu16.04-zorroa-amd64.deb

COPY ./travis/entrypoint.sh /

CMD ["/entrypoint.sh"]
