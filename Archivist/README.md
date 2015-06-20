# Archivist
Media ingestion, storage, and retrieval service.

## Requirements

Install Java 8 and Maven:

1. [Download Java 8u45](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

2. Doubleclick on the .dmg and follow instructions.

3. Verify with "java -version": 1.8.0_45

4. Install Maven via homebrew ("brew update" if you get a 404): brew install maven

## Starting the Server

```
$ mvn package
$ java -jar target/archivist-1.0.0.jar
```

## TCP Ports of Note

   * 8066 - REST interface
   * 8087 - Event interface
   * 9200 - ElasticSearch direct HTTP

## REST Endpoint Quick Reference

| Endpoint                   | Method | Description                                                    |
|----------------------------|--------|----------------------------------------------------------------|
| /api/v1/assets/_search     | GET    | Perform an asset search                                        |
| /api/v1/assets/_count      | GET    | Perform an asset search but return only the total result count |
| /api/v1/pipelines          | POST   | Create a new ingest pipeline                                   |
| /api/v1/pipelines/{id}        | GET    | Get a particular ingest pipeline                               |
| /api/v1/pipelines          | GET    | Get a list of all ingest pipelines                             |
| /api/v1/proxy-configs      | GET    | Get a list of all proxy generation configurations              |
| /api/v1/proxy-configs/{id} | GET    | Get a specific proxy generation configuration                  |
| /api/v1/proxy/image/{id}   | GET    | Get a specific proxy image                                     |
| /api/v1/rooms/{id}/_join   | PUT    | Assigns the authenticated session to the given room            |
| /api/v1/rooms/{id}         | GET    | Get information for particular room                            |
| /api/v1/rooms              | GET    | Get a list of all rooms                                        |
| /api/v1/rooms              | POST   | Add a new room                                                 |
| /api/v1/login              | POST   | Login via simple HTTP authentication                           |
| /api/v1/logout             | POST   | Log the authenticated user out                                 |
| /api/v1/users              | GET    | Get a list of all users                                        |

## Talking to the Archivst using Curl

Any URL on TCP port 9200 is a raw ElasticSearch endpoint and used for debugging.  This will be disabled
in a production setup for security purposes and we'll have official SDK endpoints for all of this data.

Clear out the database:
```
curl -XDELETE 'http://localhost:9200/archivist/_all'
```

Clear out the proxies and all data:

```
rm -rf data proxies
```

### Standard Proxy Processor Configuration

A Proxy Configuration determines what proxy sizes and bit depth are made.  The Archivst ships with
a 'standard' proxy configuration, and users may add their own.

Elastic:
```
curl -XGET -i 'http://localhost:9200/archivist/proxy-config/standard'
```

Archivist:
```
curl -b /tmp/cookies -c /tmp/cookies -u admin:admin -XGET -i 'http://localhost:8066/api/v1/proxy-configs/standard'
```

### Standard Ingest Pipeline

An Ingest Pipeline determines all the steps that occur during an ingest.  The Archivst ships with
a 'standard' ingest pipeline which is currently suitable for photos.

Elastic:
```
curl -XGET -i 'http://localhost:9200/archivist/pipeline/standard'
```

Archivist:
```
curl -b /tmp/cookies -c /tmp/cookies -u admin:admin -XGET -i 'http://localhost:8066/api/v1/pipelines/standard'
```

### Performing an Ingest

At minimum, to perform an ingest you must provide a path to search.  This will use the standard IngestPipeline which
is setup to use the Standard proxy config. Change the "standard" entry in the enpoint to change pipelines.
Note that the ingestion port is on 8066, not 9200, which is the Elasticsearch port, which will eventually be removed for security.

```
curl  -b /tmp/cookies -c /tmp/cookies -u admin:admin -H 'Content-Type: application/json' \
  -XPOST -i 'http://localhost:8066/api/v1/pipelines/standard/_ingest' -d '{
    "path":"/Users/chambers/Pictures/iphoto/Masters/2015"
  }'
```

To define a new pipeline, augmenting the standard pipeline with a CaffeProcessor:

```
curl -XPOST -i 'http://localhost:9200/archivist/pipeline/caffe' -d '{
  "processors":[
    {"klass":"com.zorroa.archivist.processors.AssetMetadataProcessor","args":{}},
    {"klass":"com.zorroa.archivist.processors.ProxyProcessor","args":{}}
    {"klass":"com.zorroa.archivist.processors.CaffeProcessor","args":{}},
  ]
}'
```

### Searching

Once you have ingested, you can search using the full power of the ElasticSearch query language. To count the number of assets use:

```
curl -XGET 'http://localhost:9200/_count?pretty' -d '{"query":{"match_all":{}}}'
```

Should return something similar to:

```
{
  "count" : 467,
  "_shards" : {
    "total" : 5,
    "successful" : 5,
    "failed" : 0
  }
}
```

You can list the asset document mapping with:

```
curl -XGET 'http://localhost:9200/archivist_01/_mapping/asset?pretty'
```

Or all of the document types with:

```
curl -XGET 'http://localhost:9200/_all/_mapping/pretty'
```


Most people will do what are called query string searches.

```
curl -XGET -i 'http://localhost:9200/archivist_01/asset/_search?pretty' -d '{
  "query": {
    "query_string" : {
      "query" : "dog AND food"
    }
  }
}'
```

You can search for all items in the collection Wex using:

```
curl -XPOST -i 'http://localhost:9200/archivist_01/_search?pretty' -d '{
  "query" : {
    "filtered" : {
      "filter" : {
        "terms" : {
          "collections" : ["Wex"]
        }
      }
    }
  }
}'
```

### Using C++ in Processors via JNI

You can use the jni.sh script to compile and link C++ code into an ingest processor.
The script can generate JNI headers using the "javah" command or create the libraries
using the jnilibs command. Additional arguments are parsed to compile or linker phases.

Here's an example of how to call it for the CaffeProcessor which uses libcaffe,
OpenCV and a bunch of other third party libs. Note that this doesn't currently
package the third party libraries into the server JAR file.

```
jni.sh jnilib CaffeProcessor \
    -I../caffe/include/ -I/Developer/NVIDIA/CUDA-7.0/include \
    -I../caffe/.build_release/src/ \
    -I/System/Library/Frameworks/Accelerate.framework/Versions/A/Frameworks/vecLib.framework/Versions/A/Headers/ \
    -lcaffe \
    -L/Users/wex/anaconda/lib -L/usr/local/lib -L/usr/lib \
    -L../caffe/.build_release/lib \
    -lglog -lm -lopencv_core -lopencv_highgui -lopencv_imgproc  -lstdc++
```

The Java code should use loadLibrary("CaffeProcessor") to load the compiled library
and you need to specify the path to the libFoo.jnilib file using the -Djava.library.path
 command line option when starting the server:

```
java -Djava.library.path=/Users/wex/Zorroa/src/Archivist/target/jni/com/zorroa/archivist/processors/CaffeProcessor \
    -jar target/archivist-1.0.0.jar
```
