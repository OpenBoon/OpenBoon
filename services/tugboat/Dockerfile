FROM google/cloud-sdk:361.0.0

RUN apt-get update && apt-get -y upgrade && apt-get --no-install-recommends -y install \
    docker \
     --no-install-recommends

ENV PYTHONUNBUFFERED True

ENV APP_HOME /app
WORKDIR $APP_HOME

COPY ./requirements.txt /app/requirements.txt
RUN pip install -r requirements.txt

COPY server.py ./
COPY tmpl ./tmpl

ENTRYPOINT [ "python3" ]
CMD [ "server.py" ]
