ARG BASE_IMG_TAG=latest

FROM boonai/py3-base AS build

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

RUN /app/prepare.sh

ENV BOONFLOW_IN_FLASK=yes
USER boonai
