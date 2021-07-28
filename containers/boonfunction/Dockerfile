# syntax = docker/dockerfile:experimental
FROM boonai/boonflow:latest

RUN mkdir /app

COPY ./requirements.txt /app/requirements.txt
COPY ./server.py /app/server.py
COPY ./prepare.sh /app/prepare.sh

WORKDIR /app
RUN --mount=type=cache,target=/root/.cache/pip pip install -r requirements.txt
RUN rm /app/requirements.txt

ENV BOONFLOW_IN_FLASK=true
ENV PYTHONUNBUFFERED True
ENV APP_HOME /app

# This won't run as is, we need a subclass container to copy in endpoint.py
USER boonai

CMD exec gunicorn --bind :$PORT --workers 1 --threads 8 --timeout 0 server:app
