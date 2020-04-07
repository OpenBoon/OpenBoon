FROM zmlp/py3-client
USER root

RUN mkdir /build; mkdir /build/pylib
COPY ./pylib /build/pylib
COPY ./setup.py /build

WORKDIR /build
RUN pip3 install .

RUN rm pylib/proxy/properties.yaml pylib/proxy/sqlite.db

WORKDIR /build/pylib/proxy/app

ENTRYPOINT ["python3", "crawler.py"]
