FROM nginx:mainline
RUN apt-get -y update && apt-get -y upgrade && apt-get -y install python3 python3-pip npm libsm6 libxrender-dev libxext6

# Front-end setup goes here

# Setup Python
COPY Pipfile /applications/scrounger/Pipfile
COPY Pipfile.lock /applications/scrounger/Pipfile.lock
WORKDIR /applications/scrounger
ENV PYTHONPATH /applications/scrounger/api:$PATH
RUN pip3 install --upgrade pip \
    && pip3 install pipenv \
    && pipenv install --system --deploy

# Copy Nginx conf
COPY nginx/local.conf /etc/nginx/conf.d/default.conf

# Copy Scrounger API
COPY api /applications/scrounger/api

# Collect static files
WORKDIR /applications/scrounger/api
RUN python3 ./manage.py collectstatic --no-input

# Start the servers
COPY gunicornconfig.py /applications/scrounger/gunicornconfig.py
COPY start-servers.sh /applications/scrounger/start-servers.sh

WORKDIR /
ENTRYPOINT ["sh", "/applications/scrounger/start-servers.sh"]