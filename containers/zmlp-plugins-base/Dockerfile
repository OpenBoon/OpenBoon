FROM zmlp/py3-sdk:latest
USER root

RUN mkdir /build
COPY ./ /build
WORKDIR /build
RUN pip3 install .

RUN mkdir -p /zps
WORKDIR /zps
RUN echo '/zps/pylib' >> /usr/lib/python3/dist-packages/zps.pth

RUN rm -rf /build
USER zorroa

