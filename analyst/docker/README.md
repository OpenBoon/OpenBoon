# Using Docker

Docker containers wrap up a piece of software in a complete Linux filesystem that contains everything it needs to run: code, runtime, system tools, system libraries – anything you can install on a server. This guarantees that it will always run the same, regardless of the environment it is running in.

## Dockerfile

The Dockerfile contains the instructures for building a docker image.  The Dockerfile creates a build environment for Java, checks out all relevant sources, and builds them.   A jar built on MacOSX or Windows
may or may not run on a Linux docker, it depends on if the application requires native code.  Since the
Analyst has lots of native code, it must be built on Linux to run on Linux.

## Instructions

First you need to install docker.
https://docs.docker.com/mac/step_one

Next you need to provide a private RSA key so Docker can clone the Zorroa sources.  Make sure github is setup to accept the key, and copy the key id_rsa file into the same directory as Dockerfile.

To build the image:
```docker build -t unbuntu-analyst .```

To execute and enter the image:
```docker run -it unbuntu-analyst bash```




