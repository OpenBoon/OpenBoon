FROM zmlp/py3-sdk:latest
USER root

RUN apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    python3-dev \
    build-essential

RUN mkdir /service
RUN mkdir /service/certs
COPY . /service
WORKDIR /service

COPY ./requirements.txt /service/requirements.txt
RUN pip install -r /service/requirements.txt
RUN python3 -c "import time; v = open('VERSION').read().strip(); print('{}-{}'.format(v, int(time.time())))" > ./BUILD
RUN rm /service/requirements.txt

RUN groupadd -r analyst && useradd --no-log-init -r -g analyst analyst; chown analyst:analyst /service/certs
# Need to run as root for now for docker.
# USER analyst

# Mount path for a shared directory for extensions hosted outside of the image such as
# custom processsor modules.
ARG EXT_MOUNT="/zorroa"
ENV PYTHONPATH="/service/pylib"

EXPOSE 5000
ENTRYPOINT ["/service/bin/analystd"]

