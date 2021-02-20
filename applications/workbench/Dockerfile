# Creates a container that runs a Jupyter notebook server with all the necessary
# Zorroa libraries installed.
FROM zmlp/py3-zvi:latest
USER root

# apt install a bunch of binary packages
ENV DEBIAN_FRONTEND=noninteractive
RUN apt-get update && apt-get install -y --no-install-recommends apt-utils
RUN apt-get install -y --no-install-recommends build-essential libjpeg-dev libpng-dev cmake \
    libtiff-dev libopencv-dev python3-opencv libsm6 lsb-release curl




COPY requirements.txt .
ENV PATH=$PATH:~/.local/bin
RUN pip3 install torch==1.4.0+cpu torchvision==0.5.0+cpu -f https://download.pytorch.org/whl/torch_stable.html
RUN pip3 install --ignore-installed -r requirements.txt

COPY jupyter_notebook_config.py /home/zorroa/.jupyter/jupyter_notebook_config.py
COPY jupyter_custom /home/zorroa/.jupyter/custom
COPY signing-key.json /home/zorroa/

RUN chown -R zorroa.zorroa /home/zorroa/.jupyter
RUN chown zorroa.zorroa /home/zorroa/signing-key.json

USER zorroa
ENV PYTHONPATH="/data"
ENV PATH=$PATH:~/.local/bin

ENV BOONAI_SERVER="http://archivist:8080"
ENV BOONAI_APIKEY_FILE="/home/zorroa/signing-key.json"


# Enable Jupyter extensions
RUN jupyter contrib nbextension install --user && \
    jupyter nbextension enable codefolding/main && \
    jupyter nbextension enable snippets/main && \
    jupyter nbextension enable executetime/main && \
    jupyter nbextension enable collapsible_headings/main && \
    jupyter nbextension enable toc2/main

WORKDIR /data/notebooks

ENTRYPOINT jupyter-notebook --ip=0.0.0.0 --no-browser
