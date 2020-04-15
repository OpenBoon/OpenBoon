FROM ubuntu:focal
RUN groupadd -r zorroa && useradd --no-log-init -r -m -g zorroa zorroa

RUN apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    python3 \
    python3-pip \
    libmagic-dev \
    wget \
     --no-install-recommends

# Auto-updating pip sometimes causes issues.
RUN pip3 install pip==20.0.2
COPY requirements.txt .
RUN pip3 install -r requirements.txt
RUN rm requirements.txt
