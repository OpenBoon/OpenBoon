FROM zmlp/py3-client
USER root

RUN mkdir /build
COPY ./pylib /build
WORKDIR /build

RUN pip3 install --no-cache-dir -r requirements.txt
RUN rm requirements.txt; rm properties.yaml

WORKDIR /build/zvi-cloud-proxy

ENTRYPOINT ["python3", "crawler.py"]
