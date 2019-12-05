FROM node:12.13.1 as node_build
COPY applications/wallet/app /app
WORKDIR /app/frontend
RUN npm install && npm run build

FROM nginx:1.17.1-alpine
RUN apk update && apk upgrade && apk add --no-cache ffmpeg postgresql postgresql-dev gcc python3-dev musl-dev python3 py3-gunicorn libffi-dev g++
COPY applications/wallet /applications/wallet
COPY sdk /sdk
WORKDIR /applications/wallet

# Setup and Run uWSGI server for django.
ENV PYTHONPATH /applications/wallet/app:$PATH
RUN pip3 install --upgrade pip \
    && pip3 install pipenv \
    && pipenv install --system --deploy
COPY --from=node_build /app/frontend/build /applications/wallet/app/frontend/build
COPY applications/wallet/app/start-server.sh /start-server.sh

# Collect static files and configs for nginx.
WORKDIR /applications/wallet/app
RUN python3 ./manage.py collectstatic --no-input
COPY applications/wallet/nginx/local.conf /etc/nginx/conf.d/default.conf

ENTRYPOINT ["sh", "/start-server.sh"]
