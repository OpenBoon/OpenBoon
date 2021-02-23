#!/bin/bash
SDIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"

unset BOONAI_APIKEY_FILE
unset BOONAI_APIKEY
export BOONAI_SERVER="http://localhost:8080"
export BOONAI_APIKEY_FILE="${PWD}/../../../dev/config/keys/signing-key.json"
export PYTHONPATH="${SDIR}/../pylib:${SDIR}/../../../containers/boonflow/pylib:${SDIR}/../../boonczar/pylib"
env | grep BOONAI
echo $PYTHONPATH
