# zorroa-server

## Test Build Instructions (MacOS)

These instructions will walk you though setting up Archivist and Analyst for testing and development.

### Prequisites:
1. Java SDK: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
1. ffmpeg & ffprobe (must be in your PATH, /usr/local/bin/ is suggested): https://evermeet.cx/ffmpeg/
1. Postgres.app: https://postgresapp.com/
1. Homebrew: https://brew.sh/
1. git: Installed via Homebrew ```brew install git```
1. oiiotool (must be in your PATH, /usr/local/bin/ is suggested): https://dl.zorroa.com/public/osx/oiiotool or build your own at https://github.com/OpenImageIO/oiio/blob/master/INSTALL.md
1. SSH keys configured on github: https://help.github.com/articles/adding-a-new-ssh-key-to-your-github-account/

### Start Postgres

Postgres must be running and have a database named after your username. This can be achieved by running Postgres.app 
clicking "initialize" the first time the app is started.

### Clone the Repos

Clone the necessary repos into a zorroa projects directory. 

```
mkdir ~/zorroa
cd ~/zorroa
git clone git@github.com:Zorroa/zorroa-test-data.git
git clone git@github.com:Zorroa/zorroa-plugin-sdk.git
git clone git@github.com:Zorroa/zorroa-server.git
``` 

### Build Plugin SDK

Build the Zorroa plugin SDK.

```
cd ~/zorroa/zorroa-plugin-sdk
mvn clean install
```

### Build Plugin SDK

Build the Zorroa server.

```
cd ~/zorroa/zorroa-server
mvn clean install
```

### Run Servers

Run the Archivist and Analyst servers. More information about these servers can be found below:

https://dl.zorroa.com/public/docs/0.39/server/archivist.html

https://dl.zorroa.com/public/docs/0.39/server/analyst.html

*Archivist*

In a fresh shell:
```
cd ~/zorroa/zorroa-server/archivist
./run.sh
```

*Analyst*

In a fresh shell:
```
cd ~/zorroa/zorroa-server/analyst
./run.sh
```

### Visit the Server

You can access the server at the link below. You will need a username and password to access the API endpoints. A default superuser will have been pre-populated in database, please ask your neighbor for the credentials. 

[http://localhost:8066/api/v1/settings]()

## Intellij IDEA Debug Server Setup (Optional)

If you are using IDEA for development there are built-in tools for running the servers that all for adding breakpoints throughout the code as well as a myriad other useful tools. Instructions for setup can be found [here](https://wiki.zorroa.com/display/TECH/Intellij+IDEA+Debug+Server+Setup).


```
http://localhost:8066
```

