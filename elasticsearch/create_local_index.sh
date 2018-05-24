#!/bin/sh

curl -XPUT 'http://localhost:9200/archivist' -H 'Content-Type: application/json' -d @asset_v40.json

