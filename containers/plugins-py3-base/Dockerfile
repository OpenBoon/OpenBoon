FROM ubuntu:disco
USER root

RUN apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    python3 \
    python3-pip \
    python3-setuptools \
    libmagic-dev \
    wget \
     --no-install-recommends

RUN pip3 install wheel

RUN mkdir -p /zps
RUN echo '/zps/pylib' >> /usr/lib/python3/dist-packages/zps.pth
WORKDIR /zps

COPY zorroa-1.0.0-py3-none-any.whl .
RUN pip3 install zorroa-1.0.0-py3-none-any.whl

ENTRYPOINT /usr/local/bin/zpsd
