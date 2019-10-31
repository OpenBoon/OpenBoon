FROM plugins-py3-base:latest
USER root

COPY pylib /zps/pylib

COPY requirements.txt .
RUN pip3 install --no-cache-dir -r requirements.txt
RUN rm requirements.txt

USER zorroa

COPY --chown=zorroa:zorroa get_models.py /tmp
RUN python3 /tmp/get_models.py; rm /tmp/get_models.py



