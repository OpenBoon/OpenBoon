FROM zmlp-py3-base:latest

RUN mkdir /build
COPY ./ /build
WORKDIR /build
RUN python3 setup.py install
RUN rm -rf /build

RUN mkdir -p /zps
RUN echo '/zps/pylib' >> /usr/lib/python3/dist-packages/zps.pth
WORKDIR /zps

USER zorroa

