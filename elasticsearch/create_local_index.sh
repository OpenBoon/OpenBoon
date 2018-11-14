#!/bin/bash

CESDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

curl -XPUT 'http://localhost:9200/zorroa_v10' -H 'Content-Type: application/json' -d @${CESDIR}/asset_v40.json

