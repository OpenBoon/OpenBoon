FROM zmlp/plugins-base:latest

RUN apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    python3-dev \
    build-essential

COPY requirements.txt .
RUN pip3 install -r requirements.txt
RUN rm requirements.txt
RUN pip3 uninstall -y protobuf
RUN pip3 install protobuf

USER zorroa
COPY --chown=zorroa:zorroa get_models.py /tmp
COPY --chown=zorroa:zorroa fruit.jpg /tmp/fruit.jpg
RUN python3 /tmp/get_models.py; rm /tmp/get_models.py; rm /tmp/fruit.jpg

RUN mkdir -p /home/zorroa/.keras/models
COPY imagenet_class_index.json /home/zorroa/.keras/models

USER root
RUN mkdir /models
COPY resnet-152 /models/resnet-152

