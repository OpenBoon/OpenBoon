FROM python:3

COPY client /client
COPY services/reporter /services/reporter
WORKDIR /services/reporter
RUN pip3 install --upgrade pip \
    && pip3 install pipenv \
    && pipenv sync
ENTRYPOINT ["pipenv", "run", "./reporter"]

