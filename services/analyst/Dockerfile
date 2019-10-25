FROM python:3-buster

RUN mkdir /service
RUN mkdir /service/certs
COPY . service
WORKDIR /service

RUN pip install -r /service/requirements.txt
RUN python -c "import time; v = open('VERSION').read().strip(); print('{}-{}'.format(v, int(time.time())))" > ./BUILD

RUN groupadd -r analyst && useradd --no-log-init -r -g analyst analyst; chown analyst:analyst /service/certs
USER analyst

# Mount path for a shared directory for extensions hosted outside of the image such as
# custom processsor modules.
ARG EXT_MOUNT="/zorroa"

ENV PYTHONPATH="/service/pylib"
ENTRYPOINT ["/service/bin/analystd"]

