# Pixel ML

## Running a local dev deployment.
Below are instructions for starting a deployment of all services within this repository. 
Docker Compose is leveraged to accomplish. To understand more about the deployment look at 
docker-compose.yml and read up on the Docker Compose documentation.

### Prerequisites
- Latest docker & docker-compose installed. Instructions are 
[here](https://docs.docker.com/v17.09/engine/installation/).

### Instructions
1. `cd` to the root of this repo.
1. `docker-compose build`
1. `docker-compose up`

#### Docker Compose Override
There is a `docker-compose.override.yml` file in the base of this repo that can be used
to specifiy values different from the defaults in the `docker-compose.yml` file. Any 
values in this file will automatically be used over the values found in the base file. For
example, this was initially used to override the Platform and Archivist server settings
for the Wallet application in order to have it connect to the Zorroa ZVI Dev env.