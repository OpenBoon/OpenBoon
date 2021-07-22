FROM boonai/plugins-analysis
USER root

RUN apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    libgomp1 \
    libquadmath0 \
     --no-install-recommends

RUN mkdir /service
COPY . /service
WORKDIR /service

COPY ./requirements.txt /service/requirements.txt
RUN pip3 uninstall opencv-python
RUN pip3 install -r /service/requirements.txt
RUN rm /service/requirements.txt

ENV BOONFLOW_IN_FLASK=true
ENV PYTHONPATH="/service/pylib"
USER boonai
EXPOSE 8282
ENTRYPOINT ["/service/bin/server.py"]
