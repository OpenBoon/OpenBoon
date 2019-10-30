FROM python:3.7.4-alpine

ENV PYTHONPATH /app/wallet:$PATH
RUN apk update && apk upgrade && apk add ffmpeg postgresql postgresql-dev gcc musl-dev
COPY Pipfile /app/Pipfile
COPY Pipfile.lock /app/Pipefile.lock
RUN pip install --upgrade pip \
    && pip install pipenv \
    && cd /app && pipenv lock --requirements > requirements.txt \
    && pip install -r requirements.txt
COPY app /app/wallet
COPY app/start-server.sh /app/start-server.sh
WORKDIR /app/wallet

EXPOSE 8080
EXPOSE 5000
