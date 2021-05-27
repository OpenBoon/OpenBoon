FROM pytorch/torchserve

RUN pip install captum

COPY config.properties /home/model-server/config.properties
COPY model.mar /home/model-server/model-store
