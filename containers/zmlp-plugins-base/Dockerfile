FROM zmlp/py3-sdk:latest

RUN mkdir /build
COPY ./ /build
WORKDIR /build
RUN pip3 install .

RUN mkdir -p /zvi
WORKDIR /zvi
RUN echo '/zvi/pylib' >> /usr/lib/python3/dist-packages/zvi.pth

RUN rm -rf /build


