FROM boonai/py3-base AS build

WORKDIR /tmp
ARG MODEL_URL
RUN curl -s -o model.zip $MODEL_URL

RUN unzip model.zip

FROM boonai/boonfunction
WORKDIR /app

USER root
COPY --from=build /tmp/function.py /app/function.py

#RUN /app/install-deps.sh

USER boonai
