FROM zmlp/plugins-models:latest

RUN apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    cython3 \
    git \
    curl \
    unzip

COPY requirements.txt .
RUN pip3 install torch==1.4.0+cpu torchvision==0.5.0+cpu -f https://download.pytorch.org/whl/torch_stable.html
RUN pip3 install -U 'git+https://github.com/cocodataset/cocoapi.git#subdirectory=PythonAPI'
RUN pip3 install -r requirements.txt
RUN pip3 install 'git+https://github.com/facebookresearch/detectron2.git'
RUN rm requirements.txt

COPY pylib /zvi/pylib

#RUN pip3 install torch==1.4.0+cpu torchvision==0.5.0+cpu -f https://download.pytorch.org/whl/torch_stable.html

