FROM plugins-py3-media:latest
USER root

COPY pylib /zps/pylib
COPY requirements.txt .

RUN pip3 install -r requirements.txt
RUN rm requirements.txt

