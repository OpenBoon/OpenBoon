FROM zmlp/py3-zvi:latest
USER root
RUN ls

# apt install a bunch of binary packages
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends apt-utils
RUN apt-get install -y --no-install-recommends build-essential libjpeg-dev libpng-dev cmake \
    libtiff-dev libopencv-dev python3-opencv libsm6 lsb-release curl

COPY requirements.txt .
ENV PATH=$PATH:~/.local/bin
RUN pip3 install --ignore-installed -r requirements.txt

COPY signing-key.json /home/zorroa/

RUN chown zorroa.zorroa /home/zorroa/signing-key.json

USER zorroa
ENV PYTHONPATH="/sandbox"
ENV PATH=$PATH:~/.local/bin

ENV BOONAI_SERVER="http://api-gateway:80"
ENV BOONAI_APIKEY_FILE="/home/zorroa/signing-key.json"
WORKDIR /sandbox

CMD streamlit run sandbox.py
