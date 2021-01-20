FROM nginx:mainline
RUN apt-get -y update && apt-get -y upgrade \
    && curl -sL https://deb.nodesource.com/setup_14.x | bash - \
    && apt-get -y install nodejs python3 python3-pip postgresql libsm6 libxrender-dev libxext6

# Set up Node.js
COPY applications/wallet/frontend /applications/wallet/frontend
WORKDIR /applications/wallet/frontend
ARG CI_COMMIT_SHA

RUN npm cache verify \
    && npm ci \
    && CI_COMMIT_SHA=$CI_COMMIT_SHA npm run build

# Install python dependencies.
COPY applications/wallet/Pipfile /applications/wallet/Pipfile
COPY applications/wallet/Pipfile.lock /applications/wallet/Pipfile.lock
COPY client /client
WORKDIR /applications/wallet
ENV PYTHONPATH /applications/wallet/app:$PATH
RUN pip3 install --upgrade pip \
    && pip3 install pipenv \
    && pipenv install --system --deploy

# Copy Nginx conf
COPY applications/wallet/nginx/local.conf /etc/nginx/conf.d/default.conf

# Copy Wallet
COPY applications/wallet/app /applications/wallet/app

# Collect static files
WORKDIR /applications/wallet/app
RUN python3 ./manage.py collectstatic --no-input

# Start servers
COPY applications/wallet/gunicornconfig.py /applications/wallet/gunicornconfig.py
COPY applications/wallet/start-servers.sh /applications/wallet/start-servers.sh
COPY applications/wallet/start-marketplace-tools.sh /applications/wallet/start-marketplace-tools.sh
WORKDIR /
ENTRYPOINT ["sh", "/applications/wallet/start-servers.sh"]
