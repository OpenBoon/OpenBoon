# zorroa-server

## Test Build Instructions

These instructions will walk you though setting up Archivist and Analyst for testing and development.

### Plugin SDK Build

First checkout and compile the plugin sdk.
git@github.com:Zorroa/zorroa-plugin-sdk.git

```
git clone git@github.com:Zorroa/zorroa-plugin-sdk.git
cd zorroa-plugin-sdk
git checkout plugin-refactor
mvn clean install
```

### Server Build

```
git clone git@github.com:Zorroa/zorroa-server.git
cd zorroa-server
git checkout plugin-refactor
mvn clean install
```

Next, copy the standard plugins built by the Plugin SDK into the plugin directory.  These are not installed into the
server automatically to avoid inadvertanly making the Archvist depend on plugins. That being said, 
there is still one place where Generator plugins are refereneced by name, so those will be moved to the SDK shortly.

```
cp ../zorroa-plugin-sdk/dist/*.zip plugins
```

Next, start the archivist and analyst with their respective run scripts.:

```
./run.sh
```

Finally, once the servers are installed and plugins/models are unpacked, copy the Caffe model file.

```
cp bvlc_reference_caffenet.caffemodel shared/models/zorroa-core/caffe/imagenet
```

Now you can hit localhost to login:

```
http://localhost:8099
```
