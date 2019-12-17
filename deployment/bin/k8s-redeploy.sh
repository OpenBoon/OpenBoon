#!/usr/bin/env sh

TYPE=$1
PROJECT=$2
DEPLOYMENT=$3
VERSION=$4
NAMESPACE=$5

set -e
echo 'Patching $DEPLOYMENT in $PROJECT'
kubectl patch $TYPE $DEPLOYMENT -p "{\"spec\":{\"template\":{\"metadata\":{\"labels\":{\"ci-job-id\":\"$CI_JOB_ID\"}}}}}" --namespace=default
