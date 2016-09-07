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
$ mvn spring-boot:run
```

The `jps -l` command will list all of the java processes currently managed by the Java virtual
machine. The following command will kill any locally running archivist server:

```
kill $(jps -l | grep archivist | cut -d' ' -f1)
```

To use external ingest processors, set the `ZORROA_SITE_PATH` to the absolute path to the directory
containing JAR files. Properly loading processors that use dynamic shared libraries also requires
configuring the `DYLD_FALLBACK_LIBRARY_PATH` to include paths for any shared libraries (.dyld) and
setting the `java.library.path` to point at the directories containing the JNI bindings for each
native Java class (.jnilib) and the `java.class.path` to point at directories containing and third
party JAR files, such as the opencv jar.

See the Ingester README for an example of how to run an ingest with external processors.


## TCP Ports of Note

   * 8066 - REST interface
   * 8087 - Event interface
   * 9200 - ElasticSearch direct HTTP

## REST Endpoints

The Archivist REST endpoints are used in the Python and ObjectiveC SDKs
to create, update and delete objects (CRUD) and perform searches.

### Quick Reference

| Endpoint                   | Method | Description                                                    |
|----------------------------|--------|----------------------------------------------------------------|
| /api/v1/assets/_search     | POST   | Perform an asset search, ES params in body                     |
| /api/v1/assets/_count      | POST   | Perform an asset search but return only the total result count |
| /api/v1/assets/_suggest    | POST   | Perform a suggest search, ES params in body                    |
| /api/v1/pipelines          | POST   | Create a new ingest pipeline                                   |
| /api/v1/pipelines/{id}     | GET    | Get a particular ingest pipeline                               |
| /api/v1/pipelines          | GET    | Get a list of all ingest pipelines                             |
| /api/v1/ingests            | GET    | Get a list of all ingests.                                     |
| /api/v1/ingests            | POST   | Create a new ingests, pass parameters in body                  |
| /api/v1/ingests/{id}       | GET    | Get a particular ingest                                        |
| /api/v1/ingests/{id}/_execute  | POST   | Execute the particular ingest                              |
| /api/v1/ingests/_search    | POST   | Search for ingests by state or pipeline                        |
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
| /health                    | GET    | Show the health details for the server

### Java Spelunking

Look at these Java files to get the arguments and return values for each REST endpoint:
```
<archivist>/src/main/java/com/zorroa/archivist/web/api
```
Contains all the "web" endpoints, grouped by type into FooController.java classes.
For example, AssetController contains all of the asset-related endpoints for creating,
updating and deleting assets, as well as *some* of the search endpoints, others are in
the RoomController because they affect the search for the current room.

All arguments and return values use JSON. The JSON layout for any argument or return
value will exactly match the layout of the type, usually a Builder for arguments, and
a domain argument for return values. Most of these are specified in the Java SDK
in the zorroa-plugin-sdk repo in:
```
<zorroa-plugin-sdk>/sdk/src/main/java/com/zorroa/sdk/domain
```
Fields in the domain objects are converted to JSON automatically using the variable
name and the type of the object. Anything marked with @JsonIgnore is skipped.

The `@RequestMapping` annotation marks each endpoint and defines the name. Each endpoint
will indicate the HTTP method (GET, PUT, POST, or DELETE) for that name. The `@PathVariable`
annotation marks the name for variables in the endpoint name. For example, `/api/v1/assets/{id}/_stream`
has a `@PathVariable` for the {id} variable, which is read as a string that represents the
asset's unique identifier.

Return values for endpoints can be specified in (at least) two ways:
1. The return value of the Java method.
2. As a string using various Java "response" methods.

## Persistent Data Files

The Archivist uses two separate databases, Elasticsearch holds the asset documents (metadata) and
the folders. A traditional database stores the users, pipelines, ingests, and rooms. Ingests also
compute proxy images of different resolutions for each source image. All of these files are stored
in files and persist when the archivist is stopped and restarted.

By default, all of the files are stored in the current working directory where you run the archivist.
To clear out all the data to a completely clean state, remove the following files and directories
before starting the archivist:

```
rm -rf data proxies snapshots archivist.mv.db
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

You can search for all items in the folder Wex using:

```
curl -XPOST -i 'http://localhost:9200/archivist_01/_search?pretty' -d '{
  "query" : {
    "filtered" : {
      "filter" : {
        "terms" : {
          "folders" : ["<Wex-folder-guid>"]
        }
      }
    }
  }
}'
```

