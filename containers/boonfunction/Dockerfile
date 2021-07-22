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

# This won't run as is, we need a subclass container to copy in endpoint.py
USER boonai
ENTRYPOINT [ "python3" ]
CMD [ "server.py" ]
