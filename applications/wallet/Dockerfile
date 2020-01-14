FROM nginx:alpine
RUN apk update && apk upgrade && apk add --no-cache postgresql postgresql-dev gcc python3-dev musl-dev python3 py3-gunicorn libffi-dev g++ nodejs-current npm

# Copy Wallet
COPY applications/wallet /applications/wallet

# Copy SDK
COPY client /client

# Copy Nginx conf
COPY applications/wallet/nginx/local.conf /etc/nginx/conf.d/default.conf

# Set up Django
WORKDIR /applications/wallet
ENV PYTHONPATH /applications/wallet/app:$PATH
RUN pip3 install --upgrade pip \
    && pip3 install pipenv \
    && pipenv install --system --deploy

# Set up Node.js
WORKDIR /applications/wallet/frontend
RUN npm ci && npm run build

# Collect static files
WORKDIR /applications/wallet/app
RUN python3 ./manage.py collectstatic --no-input

# Start servers
WORKDIR /
ENTRYPOINT ["sh", "/applications/wallet/start-servers.sh"]
