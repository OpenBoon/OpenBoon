FROM python:3.7.4-alpine

ENV PYTHONPATH /app/wallet:$PATH
RUN apk update && apk upgrade && apk add ffmpeg postgresql postgresql-dev gcc musl-dev
COPY requirements.txt /app/requirements.txt
RUN pip install --upgrade pip && pip install -r /app/requirements.txt
COPY app /app/wallet
COPY app/start-server.sh /app/start-server.sh
WORKDIR /app/wallet

EXPOSE 8080
EXPOSE 5000
