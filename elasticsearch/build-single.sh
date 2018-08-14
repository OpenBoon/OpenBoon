#!/bin/sh
PROJECT=`gcloud config get-value project`
IMAGE="gcr.io/${PROJECT}/zorroa-elasticsearch-single"
echo "##################################"
echo "Project: $PROJECT"
echo "Image: $IMAGE"
echo "##################################"
docker build -t $IMAGE .
#docker push $IMAGE
