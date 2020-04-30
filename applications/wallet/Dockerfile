FROM nginx:mainline
RUN apt-get -y update && apt-get -y upgrade && apt-get -y install python3 python3-pip npm postgresql libsm6 libxrender-dev libxext6

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
ARG CI_COMMIT_SHA
RUN npm ci && CI_COMMIT_SHA=$CI_COMMIT_SHA npm run build

# Collect static files
WORKDIR /applications/wallet/app
RUN python3 ./manage.py collectstatic --no-input

# Start servers
WORKDIR /
ENTRYPOINT ["sh", "/applications/wallet/start-servers.sh"]
