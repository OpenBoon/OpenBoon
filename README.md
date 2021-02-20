# Boon AI Platform

## Components

### External Libs
   * boonsdk - Client facing library for interacting with platform.
   * boonlab - Client facing library for viewing Boon AI data in Jupyter.

### Internal Libs   
   * boonczar - Library for project and ES index administration.
   * boonjvm - Library for shared JVM interfaces.
   * boondocks - Service for executing Boon AI docker plugin images.
   * boonflow - A Library for describing and executing Boon AI jobs.

### Plugin Containers
   * plugins-core - The core Boon AI processors for reading file types.
   * plugins-models - A base container for shared models.
   * plugins-analysis - Boon AI processors for ML services
   * plugins-train - Boon AI processors for training models.

### Services
   * analyst - Service launches boondocks servers for running plugin images.
   * archivist - Service which exposes core Boon AI platform API.
   * auth-server - Service for validating API keys
   * metrics - Service for keeping track of ML API usage
   * mlbbq - Service for exposing various ML related endpoints.
   * officer - Service for processing documents.
   * reporter - Not sure

## Running a local dev deployment.
Refer to the [gitbook](https://app.gitbook.com/@zorroa/s/developers/guidelines/local-development).

## Local Deployment Quickstart
To quickly get the latest deployed code up and running do the following. This will pull all of the 
latest deployed images from docker hub for all of the services and start a local docker deployment
running. The admin console can be accessed at http://localhost and the ZMLP API is located at
http://localhost:8080.

### Prerequisites
- Latest Docker and docker-compose installed.

### Steps
1. Pull the latest dev Docker images: `docker-compose pull`
1. Run the compose environemt:  `docker-compose up`

#### To Stop the Compose Environment

    * Ctrl-C in the running terminal window
    * Or, from another terminal window run `docker-compose stop`

#### Cleanup & Fresh Instance

    * Clean up old images & containers: `docker system prune -a`
    * To reset your Compose instance to a fresh state, after stopping your running environment: `docker-compose down`

#### View Logs for running Service

    * Dump the logs: `docker-compose logs ${ServiceName}`
    * Continually stream the logs: `docker-compose logs -f ${ServiceName}`

* More information can be found in the [Docker](https://docs.docker.com/reference/)  and [Docker Compose Documentation](https://docs.docker.com/compose/reference/).
