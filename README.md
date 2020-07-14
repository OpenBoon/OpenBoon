# Pixel ML

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
