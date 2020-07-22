# syntax=docker/dockerfile:experimental
FROM zmlp/plugins-models:latest

RUN --mount=type=cache,target=/var/cache/apt,sharing=locked --mount=type=cache,target=/var/lib/apt,sharing=locked \
    apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    cython3 \
    git \
    curl \
    unzip \
    nvidia-cuda-toolkit

COPY requirements.txt .
RUN --mount=type=cache,target=/root/.cache/pip pip3 install -r requirements.txt
RUN rm requirements.txt

COPY pylib /zvi/pylib

