# syntax = docker/dockerfile:experimental
FROM boonai/boonsdk:latest

RUN mkdir /app

COPY ./requirements.txt /app/requirements.txt
COPY ./server.py /app/server.py
COPY ./install-deps.sh /app/install-deps.sh

WORKDIR /app
RUN --mount=type=cache,target=/root/.cache/pip pip install -r requirements.txt
RUN rm /app/requirements.txt

# This won't run as is, we need a subclass container to copy in endpoint.py
USER boonai
ENTRYPOINT [ "python3" ]
CMD [ "server.py" ]
