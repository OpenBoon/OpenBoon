ARG BASE_IMG_TAG=latest

FROM boonai/py3-base AS build

RUN apt-get update && apt-get -y install curl unzip

RUN mkdir /tmp/function
WORKDIR /tmp/function

ARG MODEL_URL
RUN curl -s -o model.zip $MODEL_URL
RUN unzip model.zip


FROM boonai/boonfunction:$BASE_IMG_TAG

WORKDIR /app

USER root
COPY --from=build /tmp/function /app/function/
RUN touch /app/function/__init__.py
RUN chmod 444 /app/function/*

RUN /app/prepare.sh

ENV PYTHONUNBUFFERED True

USER boonai
