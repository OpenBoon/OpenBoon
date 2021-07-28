FROM boonai/plugins-analysis
USER root

RUN apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    libgomp1 \
    libquadmath0 \
     --no-install-recommends

RUN mkdir /app
COPY . /app
WORKDIR /app
ENV APP_HOME /app

COPY ./requirements.txt /app/requirements.txt
RUN pip3 uninstall opencv-python
RUN pip3 install -r /app/requirements.txt
RUN rm /app/requirements.txt

ENV BOONFLOW_IN_FLASK=true
ENV PYTHONPATH="/app/pylib"
ENV PYTHONUNBUFFERED True

USER boonai
EXPOSE 8282

CMD exec gunicorn --bind :8282 --workers 1 --threads 8 --timeout 0 mlbbq.main:app
