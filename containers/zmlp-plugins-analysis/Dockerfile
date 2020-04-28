FROM zmlp/plugins-models:latest

RUN apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    tesseract-ocr \
    libtesseract-dev

COPY requirements.txt .
RUN pip3 install --no-cache-dir -r requirements.txt
RUN rm requirements.txt

COPY pylib /zvi/pylib
USER zorroa


