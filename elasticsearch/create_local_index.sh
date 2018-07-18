#!/bin/sh

curl -XPUT 'http://localhost:9200/zorroa_v10' -H 'Content-Type: application/json' -d @asset_v40.json

