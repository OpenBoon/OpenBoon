# Ingestors

Archivist ingestors that use the OpenCV libraries, including OpenCV Java bindings
and Caffe C++ library.

We include the runtime libraries for ingestors in the lib directory, but they must
be installed to the proper system locations to be available. Libraries must be installed
to /usr/local using:

`travis/travis-local-libs.sh`

And the OpenCV Java wrapers installed to the local maven repository with:

`mvn install:install-file -Dfile=lib/face/opencv-2412.jar -DgroupId=org.opencv -DartifactId=opencv -Dversion=2.4.12 -Dpackaging=jar`

Some of the model files are too big to store in GitHub. You can download and install
those files by running:

`travis/travis-ingestor-files.sh`


## Testing ingestors

Ingestors are tested in two maven phases: the standard "test" phase and the "integration-test"
phase which runs the Archivist and performs an ingest by dynamically loading the ingestor.jar
file. The integration-test phase is used because it runs after the package phase which builds
the ingestor.jar file dynamically loaded by the Archivist. Tests are run by the surefire and
failsafe JUNIT plugins and use the naming of Java test files to determine which tests to run
during each phase. Files with the TestsIT.java suffix are run during integration tests.

To run all the tests use:

`mvn integration-test`

When adding a new ingestor, add at least two tests: one regular test that simply instantiates
the ingestor to test the basic JNI loading and a second test in one of the TestsIT.java files
that will be run during the integration-test phase.


## Installing and running ingestors in Archivist

The Archivist will load JAR files containing ingestors using the `ZORROA_SITE_PATH`
environment variable.

If your ingestor uses a third party JAR file, it must be in the ZORROA_SITE_PATH.
For example, the FaceIngestor uses the opencv-2411.jar, so you must put the
opencv-2412.jar file in the `ZORROA_SITE_PATH`.

Some ingestors will load model files using the `ZORROA_OPENCV_MODEL_PATH` environment
variable. This generally points to the top of a shared model path and each processor
uses files from a subdirectory. Set it to, e.g. `<ingestors-project>/models` and place
the Caffe models under `<ingestors-project>/models/caffe/imagenet`.

Since `DYLD_FALLBACK_LIBRARY_PATH` no longer works on El Capitan, we must install all
of the required libraries to one of the system standard directories, such as /usr/local/lib.
This action is performed using the `travis/travis-local-libs.sh` script which copies the
libraries from Ingestors/lib/local to /usr/local and makes symlinks for /usr/local/opt.

```
#!/bin/bash
set -x
INGESTORS_PROJECT=/Users/wex/Zorroa/src/Ingestors
export ZORROA_OPENCV_MODEL_PATH=${INGESTORS_PROJECT}/models
export ZORROA_SITE_PATH=${INGESTORS_PROJECT}/target
java -Djava.class.path=${INGESTORS_PROJECT}/lib/face -Djava.library.path=${INGESTORS_PROJECT}/target/jni:${INGESTORS_PROJECT}/lib/face -jar target/archivist.jar
```

Once the server is started using the proper environment and VM variables, you can ingest a
directory of images with the following:

1. Create a pipeline that contains both the Caffe and Face ingestors.
2. Create an ingest that uses the pipeline and specifies a source folder.
3. Execute the ingest.


See Curator/travis/travis-ingest.sh for an example of how to run these commands using curl.


## Using C++ in Ingestors via JNI

You can use the jni.sh script to compile and link C++ code into an ingestors.
The script can generate JNI headers using the "javah" command or create the libraries
using the jnilibs command. Additional arguments are parsed to compile or linker phases.

Here's an example of how to call it for the CaffeIngestor which uses libcaffe,
OpenCV and a bunch of other third party libs. Note that this doesn't currently
package the third party libraries into the server JAR file.

```
jni.sh jnilib caffe.CaffeIngestor \
    -Isrc/main/C++/com/zorroa/ingestors/caffe/include \
    -Isrc/main/C++/com/zorroa/ingestors/caffe/include/CUDA-7.0 \
    -I/System/Library/Frameworks/Accelerate.framework/Versions/A/Frameworks/vecLib.framework/Versions/A/Headers \
    -Llib/caffe -lcaffe -lglog -lopencv_core -lopencv_highgui -lopencv_imgproc  -lstdc++
```

The Java code should use loadLibrary("CaffeIngestor") to load the compiled library
and you need to specify the path to the libFoo.jnilib file using the -Djava.library.path
command line option when starting the server:

```
java -Djava.library.path=/Users/wex/Zorroa/src/Archivist/target/jni/caffe \
    -jar target/archivist-1.0.0.jar
```
