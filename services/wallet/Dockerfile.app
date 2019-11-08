FROM node:12.13.0 as node_build
COPY app /app
WORKDIR /app/frontend
RUN npm install && npm run build


FROM python:3.7.4-alpine

ENV PYTHONPATH /app:$PATH
RUN apk update && apk upgrade && apk add ffmpeg postgresql postgresql-dev gcc musl-dev
COPY Pipfile /Pipfile
#COPY Pipfile.lock /Pipefile.lock
RUN pip install --upgrade pip \
    && pip install pipenv \
    && cd / && pipenv lock --requirements > requirements.txt \
    && pip install -r requirements.txt
COPY app /app
COPY --from=node_build /app/frontend/build /app/frontend/build
COPY app/start-server.sh /start-server.sh
WORKDIR /app

EXPOSE 8080
EXPOSE 5000
