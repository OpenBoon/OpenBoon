FROM nginx:alpine
RUN apk update && apk upgrade && apk add --no-cache postgresql postgresql-dev gcc python3-dev musl-dev python3 py3-gunicorn libffi-dev g++ nodejs-current npm
COPY applications/wallet /applications/wallet
COPY sdk /sdk
WORKDIR /applications/wallet

# Set up Django.
ENV PYTHONPATH /applications/wallet/app:$PATH
RUN pip3 install --upgrade pip \
    && pip3 install pipenv \
    && pipenv install --system --deploy
COPY applications/wallet/app/start-server.sh /start-server.sh

# Set up Node.
WORKDIR /applications/wallet/app/frontend
RUN npm ci && npm run build

# Collect static files and configs for nginx.
WORKDIR /applications/wallet/app
RUN python3 ./manage.py collectstatic --no-input
COPY applications/wallet/nginx/local.conf /etc/nginx/conf.d/default.conf

ENTRYPOINT ["sh", "/start-server.sh"]
