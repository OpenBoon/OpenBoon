FROM curlimages/curl AS build

WORKDIR /tmp
ARG MODEL_URL
RUN curl -s -o model.mar $MODEL_URL


FROM pytorch/torchserve

RUN pip install captum

COPY config.properties /home/model-server/config.properties
COPY --from=build /tmp/model.mar /home/model-server/model-store
