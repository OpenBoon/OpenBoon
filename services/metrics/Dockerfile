FROM nginx:mainline
RUN apt-get -y update && apt-get -y upgrade && apt-get -y install python3 python3-pip postgresql libsm6 libxrender-dev libxext6

# Install python dependencies.
COPY services/metrics/Pipfile /service/metrics/Pipfile
COPY services/metrics/Pipfile.lock /service/metrics/Pipfile.lock
COPY client /client
WORKDIR /service/metrics
ENV PYTHONPATH /service/metrics/app:$PATH
RUN pip3 install --upgrade pip \
    && pip3 install pipenv \
    && pipenv install --system --deploy

# Copy Nginx conf
COPY services/metrics/nginx/local.conf /etc/nginx/conf.d/default.conf

# Copy Metrics Service
COPY services/metrics/app /service/metrics/app

# Collect static files
WORKDIR /service/metrics/app
RUN python3 ./manage.py collectstatic --no-input

# Copy server scripts
COPY services/metrics/gunicornconfig.py /service/metrics/gunicornconfig.py
COPY services/metrics/start-servers.sh /service/metrics/start-servers.sh
WORKDIR /

# Create a boonai user so nothing runs as root.
RUN groupadd -r boonai && useradd --no-log-init -r -m -g boonai boonai

ENTRYPOINT ["sh", "/service/metrics/start-servers.sh"]
