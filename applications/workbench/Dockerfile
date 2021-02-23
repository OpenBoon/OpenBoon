# Creates a container that runs a Jupyter notebook server with all the necessary
# Zorroa libraries installed.
FROM ubuntu:focal

USER root

RUN groupadd -r boonai && useradd --no-log-init -r -m -g boonai boonai

RUN --mount=type=cache,target=/var/cache/apt,sharing=locked --mount=type=cache,target=/var/lib/apt,sharing=locked \
    apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    python3 \
    python3-pip \
    libmagic-dev \
    wget \
    curl \
     --no-install-recommends

# apt install a bunch of binary packages
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends apt-utils
RUN apt-get install -y --no-install-recommends build-essential libjpeg-dev libpng-dev cmake \
    libtiff-dev libopencv-dev python3-opencv libsm6 lsb-release curl

# Auto-updating pip sometimes causes issues.
RUN pip3 install -U pip

COPY requirements.txt .
ENV PATH=$PATH:~/.local/bin
RUN pip3 install torch==1.4.0+cpu torchvision==0.5.0+cpu -f https://download.pytorch.org/whl/torch_stable.html
RUN pip3 install --ignore-installed -r requirements.txt

COPY jupyter_notebook_config.py /home/boonai/.jupyter/jupyter_notebook_config.py
COPY jupyter_custom /home/boonai/.jupyter/custom
COPY signing-key.json /home/boonai/

RUN chown -R boonai.boonai /home/boonai/.jupyter
RUN chown boonai.boonai /home/boonai/signing-key.json

USER boonai
ENV PYTHONPATH="/data"
ENV PATH=$PATH:~/.local/bin

ENV BOONAI_SERVER="http://archivist:8080"
ENV BOONAI_APIKEY_FILE="/home/boonai/signing-key.json"


# Enable Jupyter extensions
RUN jupyter contrib nbextension install --user && \
    jupyter nbextension enable codefolding/main && \
    jupyter nbextension enable snippets/main && \
    jupyter nbextension enable executetime/main && \
    jupyter nbextension enable collapsible_headings/main && \
    jupyter nbextension enable toc2/main

WORKDIR /data/notebooks

ENTRYPOINT jupyter-notebook --ip=0.0.0.0 --no-browser
