FROM node:8.16.2 as node_build
COPY services/wallet/app /app
WORKDIR /app/nextjs
RUN npm install && npm run build

FROM nginx:1.17.1-alpine
RUN apk update && apk upgrade && apk add --no-cache ffmpeg postgresql postgresql-dev gcc python3-dev musl-dev python3 py3-gunicorn libffi-dev g++
COPY services/wallet /services/wallet
COPY sdk /sdk
WORKDIR /services/wallet

# Setup and Run uWSGI server for django.
ENV PYTHONPATH /services/wallet/app:$PATH
RUN pip3 install --upgrade pip \
    && pip3 install pipenv \
    && pipenv lock --requirements --keep-outdated > requirements.txt \
    && pip3 install --user -r requirements.txt
COPY --from=node_build /app/nextjs/build /services/wallet/app/frontend/build
COPY services/wallet/app/start-server.sh /start-server.sh

# Collect static files and configs for nginx.
WORKDIR /services/wallet/app
RUN python3 ./manage.py collectstatic --no-input
COPY services/wallet/nginx/local.conf /etc/nginx/conf.d/default.conf

ENTRYPOINT ["sh", "/start-server.sh"]
