# syntax=docker/dockerfile:experimental
FROM ubuntu:focal
RUN groupadd -r boonai && useradd --no-log-init -r -m -g boonai boonai

RUN --mount=type=cache,target=/var/cache/apt,sharing=locked --mount=type=cache,target=/var/lib/apt,sharing=locked \
    apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    python3 \
    python3-pip \
    libmagic-dev \
    wget \
    curl \
     --no-install-recommends

# Auto-updating pip sometimes causes issues.
RUN --mount=type=cache,target=/root/.cache/pip pip3 install -U pip
COPY requirements.txt .
RUN --mount=type=cache,target=/root/.cache/pip pip3 install -r requirements.txt
RUN rm requirements.txt
