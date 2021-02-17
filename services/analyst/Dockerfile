FROM boonai/boonflow:latest
USER root

RUN apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    python3-dev \
    build-essential

RUN mkdir /service
COPY . /service
WORKDIR /service

COPY ./requirements.txt /service/requirements.txt
RUN pip3 install -r /service/requirements.txt
RUN python3 -c "import time; v = open('VERSION').read().strip(); print('{}-{}'.format(v, int(time.time())))" > ./BUILD
RUN rm /service/requirements.txt

RUN groupadd -r analyst && useradd --no-log-init -r -g analyst analyst
# Need to run as root for now for docker.
# USER analyst

ENV PYTHONPATH="/service/pylib"
ENV ANALYST_CACHE_ROOT="/tmp"

EXPOSE 5000
ENTRYPOINT ["/service/bin/analystd"]

