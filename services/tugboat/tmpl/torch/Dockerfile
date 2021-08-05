FROM boonai/py3-base AS build

RUN apt-get update && apt-get -y install curl unzip

WORKDIR /tmp
ARG MODEL_URL
RUN curl -s -o model.mar $MODEL_URL
# Tests to see the mar is really a zip
RUN unzip -l model.mar


FROM pytorch/torchserve

RUN pip install captum

COPY config.properties /home/model-server/config.properties
COPY --from=build /tmp/model.mar /home/model-server/model-store
