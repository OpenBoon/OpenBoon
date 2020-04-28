FROM zmlp/plugins-base:latest

COPY requirements.txt .

RUN pip3 install -r requirements.txt
RUN rm requirements.txt

COPY pylib /zvi/pylib
USER zorroa
