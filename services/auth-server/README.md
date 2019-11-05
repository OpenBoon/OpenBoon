# Zorroa Authentication Server

The Authentication server is a service for validating JWTS.

## Building

To build locally simply:

```mvn clean package```

To build a docker image, there is no need to have the java installed on your machine.

```docker build . -t auth-server```

## Docker Options

In the 'docker' directory you will find various files that make up the docker image.  All
of these can be overriden with a docker mount. 

For example, to override the external super admin key:

```docker run -it -v /tmp/key.json:/service/config/key.json auth-server```

