# Archivist
Media ingestion, storage, and retrieval service.

# Starting the Server

```
$ mvn package
$ java -jar target/mymodule-0.0.1-SNAPSHOT.jar
```

# Looking at Data using Curl

## Standard Proxy Processor Configuration

```
curl -XGET -i 'http://localhost:9200/archivist/proxy-config/standard'
```



