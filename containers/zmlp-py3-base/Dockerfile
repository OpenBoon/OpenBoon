FROM ubuntu:disco
RUN groupadd -r zorroa && useradd --no-log-init -r -m -g zorroa zorroa

RUN apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    python3 \
    python3-pip \
    libmagic-dev \
    wget \
     --no-install-recommends

RUN pip3 install --no-cache-dir -U pip
COPY requirements.txt .
RUN pip3 install --no-cache-dir -r requirements.txt
RUN rm requirements.txt
