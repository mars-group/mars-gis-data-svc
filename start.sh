#!/usr/bin/env bash

# Builds the gis-data JAR, builds a docker image, pushes the image to Nexus and restarts the pod.

DOCKER_REGISTRY="nexus.informatik.haw-hamburg.de"
SERVIE_NAME="gis-data-svc"

mvn clean package

docker build -t $DOCKER_REGISTRY/$SERVIE_NAME:dev .
docker push $DOCKER_REGISTRY/$SERVIE_NAME:dev

kubectl delete pod $(kubectl get pod |grep gis-data |awk '{print $1;}') --force
