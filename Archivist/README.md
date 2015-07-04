# Archivist
Media ingestion, storage, and retrieval service.

## Requirements

Install Java 8 and Maven:

1. [Download Java 8u45](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html)

2. Doubleclick on the .dmg and follow instructions.

3. Verify with "java -version": 1.8.0_45

4. Install Maven via homebrew ("brew update" if you get a 404): brew install maven

Requires that the ArchivistSDK project in the local maven repository. Download the source for ArchivistSDK
from GitHub, build and install to the local maven repository.

## Starting the Server

```
$ mvn package
$ java -jar target/archivist-1.0.0.jar
```

To use external ingest processors, set the `ZORROA_SITE_PATH` to the absolute path to the directory
containing JAR files. Properly loading processors that use dynamic shared libraries also requires
configuring the `DYLD_FALLBACK_LIBRARY_PATH` to include paths for any shared libraries (.dyld) and
setting the `java.library.path` to point at the directories containing the JNI bindings for each
native Java class (.jnilib).

For example, to run the CaffeProcessor in the OpenCVProcessors project:

1. Build the project with `mvn install`
2. Set `ZORROA_SITE_PATH` to `<path-to-OpenCVProcessors>/target`, which should contain OpenCVProcessors-1.0.0.jar
3. Set `DYLD_FALLBACK_LIBRARY_PATH` to `<path-to-OpenCVProcessors>/lib`, which contains the caffe and other dynamic libraries
4. [Download the Caffe test models](http://zorroa.com/caffe/caffe-models.tgz)
5. Un-tar the models to a directory, e.g. `<path-to-OpenCVProcessors>/models`
6. Set `ZORROA_OPENCV_MODELS_PATH` to `<path-to-OpenCVProcessors>/models`
7. Run the server with the following VM option: `-Djava.library.path=<path-to-OpenCVProcessors>target/jni/caffe`, which should contain CaffeProcessor.jnilib


## TCP Ports of Note

   * 8066 - REST interface
   * 8087 - Event interface
   * 9200 - ElasticSearch direct HTTP

## REST Endpoint Quick Reference

| Endpoint                   | Method | Description                                                    |
|----------------------------|--------|----------------------------------------------------------------|
| /api/v1/assets/_search     | POST   | Perform an asset search, ES params in body                     |
| /api/v1/assets/_count      | POST   | Perform an asset search but return only the total result count |
| /api/v1/assets/_suggest    | POST   | Perform a suggest search, ES params in body                    |
| /api/v1/pipelines          | POST   | Create a new ingest pipeline                                   |
| /api/v1/pipelines/{id}     | GET    | Get a particular ingest pipeline                               |
| /api/v1/pipelines          | GET    | Get a list of all ingest pipelines                             |
| /api/v1/ingests            | GET    | Get a list of all ingests. Optional state=pending parameter    |
| /api/v1/ingests            | POST   | Create a new ingests, pass parameters in body                  |
| /api/v1/ingests/{id}       | GET    | Get a particular ingest                                        |
| /api/v1/ingests/{id}/_ingest  | POST   | Execute the particular ingest                                  |
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

Archivist:
```
curl -b /tmp/cookies -c /tmp/cookies -u admin:admin -XGET -i 'http://localhost:8066/api/v1/proxy-configs/standard'
```

### Standard Ingest Pipeline

An Ingest Pipeline determines all the steps that occur during an ingest.  The Archivst ships with
a 'standard' ingest pipeline which is currently suitable for photos.

```
curl -b /tmp/cookies -c /tmp/cookies -u admin:admin -XGET -i 'http://localhost:8066/api/v1/pipelines/standard'
```

### Performing an Ingest

Create a new ingest with:
```
curl  -b /tmp/cookies -c /tmp/cookies -u admin:admin -XPOST -i 'http://localhost:8066/api/v1/ingests' -d '{"path":"/Users/foo"}'
```

Get a list of all ingests with:
```
curl  -b /tmp/cookies -c /tmp/cookies -u admin:admin -XGET -i 'http://localhost:8066/api/v1/ingests'
```

Perform the ingestion for a previously created ingest:
```
curl  -b /tmp/cookies -c /tmp/cookies -u admin:admin -XPOST -i 'http://localhost:8066/api/v1/ingests/1/_ingest'
```

### Searching

Once you have ingested, you can search using the full power of the ElasticSearch query language. To count the number of assets use:

```
curl -XPOST 'http://localhost:9200/_count?pretty' -d '{"query":{"match_all":{}}}'
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
curl -XPOST -i 'http://localhost:9200/archivist_01/asset/_search?pretty' -d '{
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

