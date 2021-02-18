# syntax=docker/dockerfile:experimental
FROM boonai/boondocks:latest

COPY requirements.txt .

RUN --mount=type=cache,target=/root/.cache/pip pip3 install -r requirements.txt
RUN rm requirements.txt

COPY pylib /boonai/pylib
USER boonai
