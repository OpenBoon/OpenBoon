# Sandbox

This is a Streamlit sandbox. Streamlit (https://www.streamlit.io/) is a framework that allows for quick development of data apps.

By default, Sandbox creates a simple asset viewer that features a metadata spreadsheet and similarity search.

## Getting Started

### Prerequisites

- Make sure you can run `docker-compose up` on the main zmlp repo directory

### Build the docker container

```
docker buildx bake -f docker-compose.yml
```

### Run

From the main zmlp repo folder:

```
docker-compose up -d
docker-compose -f docker-compose.yml -f applications/sandbox/docker-compose.yml up sandbox
```

Then navigate to http://localhost:8501

To change the Streamlit app, edit applications/sandbox/sandbox.sandbox.py. It is not necessary to restart the docker image when you edit the app, which makes it very easy to
develop. Once you save a change to this file, the live Streamlit app will show a refresh button.

!["Sandbox"](sandbox_pom.png "Sandbox")
