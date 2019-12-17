FROM zmlp/plugins-base:latest
USER root

RUN apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    python3-dev \
    cmake \
    build-essential \
    libglib2.0-0

COPY pylib /zps/pylib

COPY requirements.txt .
RUN pip3 install --no-cache-dir -r requirements.txt
RUN rm requirements.txt
RUN pip3 uninstall -y protobuf
RUN pip3 install protobuf

USER zorroa

COPY --chown=zorroa:zorroa get_models.py /tmp
RUN python3 /tmp/get_models.py; rm /tmp/get_models.py



