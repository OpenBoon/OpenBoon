FROM zmlp/plugins-base:latest

COPY pylib /zvi/pylib
COPY requirements.txt .

RUN pip3 install -r requirements.txt
RUN rm requirements.txt

USER zorroa
