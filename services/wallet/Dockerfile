FROM node:12.13.0 as node_build
COPY app /app
WORKDIR /app/frontend
RUN npm install && npm run build

FROM nginx:1.17.1-alpine
RUN apk update && apk upgrade && apk add --no-cache ffmpeg postgresql postgresql-dev gcc python3-dev musl-dev python3 py3-gunicorn
COPY app /app

# Setup and Run uWSGI server for django.
ENV PYTHONPATH /app:$PATH
COPY Pipfile /Pipfile
COPY Pipfile.lock /Pipefile.lock
RUN pip3 install --upgrade pip \
    && pip3 install pipenv \
    && cd / && pipenv lock --requirements > requirements.txt \
    && pip3 install --user -r requirements.txt
COPY --from=node_build /app/frontend/build /app/frontend/build
COPY app/start-server.sh /start-server.sh

# Collect static files and configs for nginx.
WORKDIR /app
RUN python3 ./manage.py collectstatic --no-input
COPY nginx/local.conf /etc/nginx/conf.d/default.conf

ENTRYPOINT ["sh", "/start-server.sh"]
