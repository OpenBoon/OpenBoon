FROM zmlp/py3-base:latest

RUN mkdir /build
COPY ./ /build
WORKDIR /build
RUN python3 setup.py install
WORKDIR /zps

RUN rm -rf /build
USER zorroa

