# zorroa-server

| Branch | Status |
|--------|--------|
DEVELOPMENT | [![pipeline status](https://gitlab.com/zorroa-zvi/zorroa-server/badges/development/pipeline.svg)](https://gitlab.com/zorroa-zvi/zorroa-server/commits/development) |
| QA | [![pipeline status](https://gitlab.com/zorroa-zvi/zorroa-server/badges/qa/pipeline.svg)](https://gitlab.com/zorroa-zvi/zorroa-server/commits/qa) |
| MASTER | [![pipeline status](https://gitlab.com/zorroa-zvi/zorroa-server/badges/master/pipeline.svg)](https://gitlab.com/zorroa-zvi/zorroa-server/commits/master) |

## Quickstart Guide - Running the ZVI Backend services locally (if you are a frontend developer, this is for you).

### Prerequisites:
1. [Docker](https://www.docker.com/docker-mac)
1. `cd` to the root of the this repo.

### Building the server code.
1. Check out the correct branch. 
 - `development` will be the bleeding edge.
 - `qa` will be latest code currently being QA'd for release.
 - `master` will be the latest stable code.
1. `docker-compose run build`

### Running the services.
1. `docker-compose up -d`

### Prepping elastic search (this is only required the first time you launch the services.)
1. `cd elasticsearch`
1. `./create_local_index.sh`

### You now have the following services running locally:
- postgres on port 5432
- elasticsearch on port 9200 & 9300
- archivist on port 8080
- analyst on port 8082

### Shutting down services
1. `docker-compose down`

### Starting services back up
1. `docker-compose up -d`

You will only need to rebuild the code if you pull new code or change branches.

## Using the docker-compose local dev environment.
The local development is orchestrated in by the docker-compose.yml file found in the root of the repo. Docker compose is 
used to run a series of docker containers that serve all the services for a complete ZVI backend. Below are some examples 
of useful commands. You can check the help for docker-compose to learn more.

Note that this environment does not include the curator frontend. You'll need to follow the instruction in that repo
for running the curator locally.

#### Example commands:

##### Create the development environment for the first time:

```
docker-compose run build
docker-compose up -d
cd elasticsearch
./create_local_index.sh
```

##### Rebuild all java code: ```docker-compose run build```

##### Start all services: ```docker-compose up -d```

##### Stop all services: ```docker-compose down```

##### Restart the archivist: ```docker-compose restart archivist```

##### Pause the analyst: ```docker-compose pause analyst```

##### Unpause the analyst: ```docker-compose unpause analyst```

##### Monitor the archivist logs: ```docker-compose logs -f archivist```

##### View the currently running services: ```docker-compose ps```

##### View all running processes on the analyst: ```docker-compose top analyst```

##### Rebuild the archivist docker container: ```docker-compose build archivist```

##### Access a shell on the analyst: ```docker-compose exec analyst bash```

##### Destroy the elastic search database and start over: 

```
docker-compose down
docker-compose rm es
docker-compose up -d
cd elasticsearch
./create_local_index.sh
```


## Full dev environment instructions (MacOS)

These instructions will walk you though setting up Archivist and Analyst for testing and development.

### Prequisites:
1. [Java 8 SDK](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)
1. [ffmpeg & ffprobe](https://evermeet.cx/ffmpeg/) (must be in your PATH, /usr/local/bin/ is suggested)
1. [Docker](https://www.docker.com/docker-mac)
1. [Homebrew](https://brew.sh/)
1. git: Installed via Homebrew ```brew install git```
1. [oiiotool](https://dl.zorroa.com/public/osx/oiiotool) (must be in your PATH, /usr/local/bin/ is suggested) or [build your own](https://github.com/OpenImageIO/oiio/blob/master/INSTALL.md)
1. SSH keys [configured on GitHub](https://help.github.com/articles/adding-a-new-ssh-key-to-your-github-account/) 
1. [Maven](https://maven.apache.org/)

### Clone the Repos

Clone the necessary repos into a zorroa projects directory. 

```
mkdir ~/zorroa
cd ~/zorroa
git clone git@github.com:Zorroa/zorroa-test-data.git
git clone git@github.com:Zorroa/zorroa-plugin-sdk.git
git clone git@github.com:Zorroa/zorroa-server.git
```

## Enable HTTPS (Optional) TODO: Everything below needs to be updated.

You can enable https and also force https.

1. Run the following from the root of your repo.
```
keytool -genkey -alias undertow -storetype PKCS12 -keyalg RSA -keysize 2048 -keystore keystore.p12 -validity 3650
```

1. Add the following to your local properties file - zorroa-server/archivist/dist/build/local/config/application.properties
```
server.ssl.enabled = true
server.ssl.key-store = <PATH_TO_REPO>/keystore.p12
server.ssl.key-store-password: zorroa
server.ssl.keyStoreType: PKCS12
server.ssl.keyAlias: undertow
```

1. Optional: To force a redirect from http to https and not allow http also add the following property.
```
security.require_ssl = true
```

## Intellij IDEA Debug Server Setup (Optional)

If you are using IDEA for development there are built-in tools for running the servers that all for adding breakpoints throughout the code as well as a myriad other useful tools. Instructions for setup can be found [here](https://wiki.zorroa.com/display/TECH/Intellij+IDEA+Debug+Server+Setup).


