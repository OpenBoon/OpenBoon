# syntax=docker/dockerfile:experimental
FROM boonai/boonflow:latest

RUN mkdir /build
COPY ./ /build
WORKDIR /build
RUN --mount=type=cache,target=/root/.cache/pip pip3 install .

RUN mkdir -p /boonai
WORKDIR /boonai
RUN echo '/boonai/pylib' >> /usr/lib/python3/dist-packages/boonai.pth

RUN rm -rf /build


