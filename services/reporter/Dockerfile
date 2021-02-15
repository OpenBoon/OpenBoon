FROM boonai/py3-base:latest

# Install gcloud SDK
RUN echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] https://packages.cloud.google.com/apt cloud-sdk main" | tee -a /etc/apt/sources.list.d/google-cloud-sdk.list && \
apt-get install -y apt-transport-https ca-certificates gnupg && \
curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | apt-key --keyring /usr/share/keyrings/cloud.google.gpg add - && \
apt-get update -y && apt-get install -y google-cloud-sdk

# Install Python Dependencies
COPY client /client
COPY services/reporter/Pipfile /services/reporter/Pipfile
COPY services/reporter/Pipfile.lock /services/reporter/Pipfile.lock
WORKDIR /services/reporter
RUN pip3 install --upgrade pip \
    && pip3 install pipenv \
    && pipenv sync

# Copy over source code.
COPY services/reporter /services/reporter

# Entrypoint runs the reporter script.
ENTRYPOINT ["pipenv", "run", "./reporter"]
