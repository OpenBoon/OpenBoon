FROM zmlp/py3-client
USER root

RUN mkdir /build; mkdir /build/app
COPY ./pylib/proxy/app /build/app
COPY ./pylib/proxy/requirements.txt /build
WORKDIR /build

RUN pip3 install --no-cache-dir -r requirements.txt
RUN rm requirements.txt

WORKDIR /build/app

ENTRYPOINT ["python3", "crawler.py"]
