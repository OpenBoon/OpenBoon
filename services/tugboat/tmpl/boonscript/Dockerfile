FROM boonai/py3-base AS build

WORKDIR /tmp
ARG MODEL_URL
RUN curl -s -o model.zip $MODEL_URL

RUN unzip model.zip

FROM boonai/boonscript

USER root
COPY --from=build /tmp/endpoint.py /app/endpoint.py
COPY --from=build /tmp/requirements.txt* /tmp/requirements.txt

RUN /app/install-deps.sh
RUN rm -rf /tmp/requirements.txt

USER boonai
