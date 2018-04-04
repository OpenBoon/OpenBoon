# zorroa-server

## Test Build Instructions (MacOS)

These instructions will walk you though setting up Archivist and Analyst for testing and development.

### Prequisites:
1. ffmpeg & ffprobe (must be in your PATH, /usr/local/bin/ is suggested): https://evermeet.cx/ffmpeg/
2. Postgres.app: https://postgresapp.com/
3. git: Can be installed through Homebrew (https://brew.sh/)
4. SSH keys configured on github: https://help.github.com/articles/adding-a-new-ssh-key-to-your-github-account/
5. Java SDK: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

### Plugin SDK Build

First checkout and compile the plugin sdk.
git@github.com:Zorroa/zorroa-plugin-sdk.git

```
git clone git@github.com:Zorroa/zorroa-plugin-sdk.git
cd zorroa-plugin-sdk
mvn clean install
```

### Server Build

```
git clone git@github.com:Zorroa/zorroa-server.git
cd zorroa-server
mvn clean install -Dmaven.test.skip=true
```

Next, start the archivist and analyst with their respective run scripts.:

```
./run.sh
```

Now you can hit localhost to login:

```
http://localhost:8066
```
