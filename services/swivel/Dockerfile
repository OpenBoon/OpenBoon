# syntax = docker/dockerfile:experimental
FROM boonai/boonsdk:latest
RUN apt-get install ca-certificates -y

RUN mkdir /app

# We copy just the requirements.txt first to leverage Docker cache
COPY ./requirements.txt /app/requirements.txt
COPY ./server.py /app/server.py

WORKDIR /app
RUN  --mount=type=cache,target=/root/.cache/pip pip install -r requirements.txt

USER boonai
ENTRYPOINT [ "python3" ]
CMD [ "server.py" ]
