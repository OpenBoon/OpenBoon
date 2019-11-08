FROM node:12.13.0 as node_build
COPY app /app
WORKDIR /app/frontend
RUN npm install && npm run build

#FROM tiangolo/uwsgi-nginx:python3.7-alpine3.8
FROM nginx:1.17.1-alpine
ENV PYTHONPATH /app:$PATH
RUN apk update && apk upgrade && apk add --no-cache ffmpeg postgresql postgresql-dev gcc python3-dev musl-dev python3 py3-gunicorn
COPY Pipfile /Pipfile
COPY Pipfile.lock /Pipefile.lock
RUN pip3 install --upgrade pip \
    && pip3 install pipenv \
    && cd / && pipenv lock --requirements > requirements.txt \
    && pip3 install --user -r requirements.txt
COPY app /app
COPY --from=node_build /app/frontend/build /app/frontend/build
COPY app/start-server.sh /start-server.sh
WORKDIR /app

# Collect static files for nginx.
RUN python3 ./manage.py collectstatic --no-input
COPY /app/wallet/static /opt/services/djangoapp/static
COPY nginx/local.conf /etc/nginx/conf.d/default.conf
