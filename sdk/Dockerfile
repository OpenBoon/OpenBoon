# syntax = docker/dockerfile:experimental
FROM zmlp/py3-client:latest
USER root

# pip20 issues
#RUN pip install -U pip

RUN mkdir /build
COPY ./ /build
WORKDIR /build
RUN --mount=type=cache,target=/root/.cache/pip python3 setup.py install

WORKDIR /zps

RUN rm -rf /build
USER zorroa
