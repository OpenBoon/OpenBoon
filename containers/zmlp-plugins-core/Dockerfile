# syntax=docker/dockerfile:experimental
FROM zmlp/plugins-base:latest

COPY requirements.txt .

RUN --mount=type=cache,target=/root/.cache/pip pip3 install -r requirements.txt
RUN rm requirements.txt

COPY pylib /zvi/pylib
USER zorroa
