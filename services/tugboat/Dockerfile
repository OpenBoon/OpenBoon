FROM google/cloud-sdk

RUN apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    docker \
     --no-install-recommends

RUN pip install Flask gunicorn docker

ENV PYTHONUNBUFFERED True

ENV APP_HOME /app
WORKDIR $APP_HOME
COPY server.py ./
COPY cloudbuild.yaml ./
COPY tmpl ./tmpl

CMD exec gunicorn --bind :$PORT --workers 1 --threads 8 --timeout 0 server:app
