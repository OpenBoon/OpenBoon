FROM ubuntu:focal

USER root

RUN groupadd -r boonai && useradd --no-log-init -r -m -g boonai boonai

RUN --mount=type=cache,target=/var/cache/apt,sharing=locked --mount=type=cache,target=/var/lib/apt,sharing=locked \
    apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    python3 \
    python3-pip \
    libmagic-dev \
    wget \
    curl \
     --no-install-recommends

# apt install a bunch of binary packages
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends apt-utils
RUN apt-get install -y --no-install-recommends build-essential libjpeg-dev libpng-dev cmake \
    libtiff-dev libopencv-dev python3-opencv libsm6 lsb-release curl

COPY requirements.txt .
ENV PATH=$PATH:~/.local/bin
RUN pip3 install --ignore-installed -r requirements.txt

COPY signing-key.json /home/boonai/

RUN chown boonai.boonai /home/boonai/signing-key.json

USER boonai
ENV PYTHONPATH="/sandbox"
ENV PATH=$PATH:~/.local/bin

ENV BOONAI_SERVER="http://api-gateway:80"
ENV BOONAI_APIKEY_FILE="/home/boonai/signing-key.json"
WORKDIR /sandbox

CMD streamlit run sandbox.py
