FROM boonai/py3-base:latest

RUN mkdir /build
COPY ./ /build
WORKDIR /build
RUN pip3 install .

RUN mkdir -p /boonai
WORKDIR /boonai
RUN echo '/boonai/pylib' >> /usr/lib/python3/dist-packages/boonai.pth

RUN rm -rf /build


