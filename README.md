# zorroa-server

## Test Build Instructions (MacOS)

These instructions will walk you though setting up Archivist and Analyst for testing and development.

### Prequisites:
1. Java SDK: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html
1. ffmpeg & ffprobe (must be in your PATH, /usr/local/bin/ is suggested): https://evermeet.cx/ffmpeg/
1. Postgres.app: https://postgresapp.com/
1. Homebrew: https://brew.sh/
1. git: Can be installed through Homebrew ```brew install git```
1. oiiotool (must be in your PATH, /usr/local/bin/ is suggested): https://dl.zorroa.com/public/osx/oiiotool or build your own at https://github.com/OpenImageIO/oiio/blob/master/INSTALL.md
1. SSH keys configured on github: https://help.github.com/articles/adding-a-new-ssh-key-to-your-github-account/


### Test Data

Clone the test data repo. This is required for tests that run during the build process.

```git clone git@github.com:Zorroa/zorroa-test-data.git```

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
mvn clean install
```

Next, start the archivist and analyst with their respective run scripts.:

```
./run.sh
```

Now you can hit localhost to login:

```
http://localhost:8066
```
