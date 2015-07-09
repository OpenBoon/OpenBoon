# Ingestors

Archivist ingestors that use the OpenCV libraries, including OpenCV Java bindings
and Caffe C++ library.

## Testing ingestors

The project includes a simple test scaffold that instantiates individual ingestors and
passes them a set of locally manufactured Assets. The test scaffold currently does *NOT*
run the Archivist server. Instantiating and processing locally allows you to test linking
and basic processing behavior without starting the full server. 

Testing in the Archivist server must be done separately using production code for now.


## Installing and running ingestors in Archivist

The Archivist will load JAR files containing ingestors using the `ZORROA_SITE_PATH`
environment variable.

If your ingestor uses a third party JAR file, it must be on the java.class.path.
For example, the FaceIngestor uses the opencv-2411.jar, so you must include the
directory containing that file in the class path using, e.g.: `-Djava.class.path=<ingestors-project>/lib/face`

Some ingestors will load model files using the `ZORROA_OPENCV_MODEL_PATH` environment
variable. This generally points to the top of a shared model path and each processor
uses files from a subdirectory. Set it to, e.g. `<ingestors-project>/models` and place
the Caffe models under `<ingestors-project>/models/caffe/imagenet`.

Ingestors that use dynamic shared libraries, e.g. libcaffe or OpenCV must also set
the `DYLD_FALLBACK_LIBRARY_PATH` to point at the required .dyld libraries, typically
stored in `lib/caffe` and set the VM option java.library.path to point at the directories
containing the .jnilib files for each native class, typically in, e.g. `target/jni/caffe`.

For example, to run an ingest with both the CaffeIngestor and FaceIngestor:

1. `cd <archivist-sdk-project>`
2. `mvn install`
3. `cd <ingestor-project>`
4. `mvn validate`, only need to do this once to install opencv-2411.jar to the local maven repo, unless you clear the maven repository
5. `mvn package`
6. `cd <archivist>`
7. `mvn package`
8. The remaining steps are encapsulated in the bash script below.
9. Set `ZORROA_SITE_PATH` to `<path-to-ingestors-project>/target`, which should contain Ingestors-1.0.0.jar
10. Set `DYLD_FALLBACK_LIBRARY_PATH` to `<path-to-ingestors-project>/lib/caffe:<path-to-ingestors-project>/lib/face`
11. [Download the Caffe test models](http://zorroa.com/caffe/caffe-models.tgz)
12. Un-tar the models to a directory, e.g. `<path-to-ingestors-project>/models/caffe/imagenet` (the tar file does this if you unpack it from the top of the ingestors project)
13. Set `ZORROA_OPENCV_MODELS_PATH` to `<path-to-ingestors-project>/models`
14. Run the server with the following VM options: `-Djava.class.path=<path-to-ingestors-project>/lib/face -Djava.library.path=<path-to-ingestors-project>target/jni:<path-to-ingestors>/lib/face`

Here's a script that runs the server with the proper environment and VM variables, assuming you set
the absolute path at the top to point at your copy of the INGESTORS_PROJECT:

```
#!/bin/bash
set -x
INGESTORS_PROJECT=/Users/wex/Zorroa/src/Ingestors
export ZORROA_OPENCV_MODEL_PATH=${INGESTORS_PROJECT}/models
export DYLD_FALLBACK_LIBRARY_PATH=${INGESTORS_PROJECT}/lib/caffe:${INGESTORS_PROJECT}/lib/face
export ZORROA_SITE_PATH=${INGESTORS_PROJECT}/target
java -Djava.class.path=${INGESTORS_PROJECT}/lib/face -Djava.library.path=${INGESTORS_PROJECT}/target/jni:${INGESTORS_PROJECT}/lib/face -jar target/archivist-1.0.0.jar
```

Once the server is started using the proper environment and VM variables, you can ingest a
directory of images with the following:

1. Create a pipeline that contains both the Caffe and Face ingestors.
2. Create an ingest that uses the pipeline and specifies a source folder.
3. Execute the ingest.


The following commands will do the three steps above, assuming you update the absolute paths:

```
curl -b /tmp/cookies -c /tmp/cookies -H 'Content-Type: application/json' -u admin:admin \
  -XPOST -i 'http://localhost:8066/api/v1/pipelines' \
  -d '{"name":"full","processors":[{"klass":"com.zorroa.archivist.processors.AssetMetadataProcessor","args":{}},{"klass":"com.zorroa.archivist.processors.ProxyProcessor","args":{}},{"klass":"com.zorroa.ingestors.CaffeIngestor","args":{}},{"klass":"com.zorroa.ingestors.FaceIngestor","args":{}}]}'
```

Replace the PATH_TO_IMAGE_DIR directory below to a valid location with images:

```
curl -b /tmp/cookies -c /tmp/cookies -H 'Content-Type: application/json' -u admin:admin \
  -XPOST -i 'http://localhost:8066/api/v1/ingests' \
  -d '{"path":"PATH_TO_IMAGE_DIR", "pipeline":"full"}'
```

```
curl -b /tmp/cookies -c /tmp/cookies -H 'Content-Type: application/json' -u admin:admin \
  -XPOST -i 'http://localhost:8066/api/v1/ingests/1/_execute'
```

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
