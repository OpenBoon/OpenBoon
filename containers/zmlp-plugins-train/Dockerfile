# syntax=docker/dockerfile:experimental
FROM zmlp/plugins-models:latest

RUN --mount=type=cache,target=/var/cache/apt,sharing=locked --mount=type=cache,target=/var/lib/apt,sharing=locked \
    apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    cython3 \
    git \
    curl \
    unzip

COPY requirements.txt .
RUN pip3 install -r requirements.txt
RUN rm requirements.txt

COPY pylib /zvi/pylib

#RUN pip3 install torch==1.4.0+cpu torchvision==0.5.0+cpu -f https://download.pytorch.org/whl/torch_stable.html

