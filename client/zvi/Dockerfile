# syntax = docker/dockerfile:experimental
FROM zmlp/py3-client:latest

RUN mkdir /build
COPY ./ /build
WORKDIR /build
RUN --mount=type=cache,target=/root/.cache/pip pip install .
WORKDIR /zps

RUN rm -rf /build
