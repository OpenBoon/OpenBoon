# Archivist
Media ingestion, storage, and retrieval service.

## Starting the Server

```
$ mvn package
$ java -jar target/mymodule-0.0.1-SNAPSHOT.jar
```

## Talking to the Archivst using Curl

Any URL on TCP port 9200 is a raw ElasticSearch endpoint and used for debugging.  This will be disabled
in a production setup for security purposes and we'll have official SDK endpoints for all of this data.

### Standard Proxy Processor Configuration

A Proxy Configuration determines what proxy sizes and bit depth are made.  The Archivst ships with
a 'standard' proxy configuration, and users may add their own.

```
curl -XGET -i 'http://localhost:9200/archivist/proxy-config/standard'
```

### Standard Ingest Pipeline

An Ingest Pipeline determines all the steps that occur during an ingest.  The Archivst ships with
a 'standard' ingest pipeline which is currently suitable for photos.

```
curl -XGET -i 'http://localhost:9200/archivist/pipeline/standard'
```

### Performing an Ingest

At minimum, to perform an ingest you must provide a path to search.  This will use the standard IngestPipeline which
is setup to use the Standard proxy config.

```
curl -XPOST -i 'http://localhost:8066/pipeline/standard/_ingest' -d '{"path":"/Users/chambers/Pictures/iphoto/Masters/2015"}'
```

