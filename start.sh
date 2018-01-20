#!/usr/bin/env bash

# Builds the gis-data JAR, builds a docker image, pushes the image to Nexus and restarts the pod.

DOCKER_REGISTRY="nexus.informatik.haw-hamburg.de"
SERVICE_NAME="gis-data-svc"

mvn clean package

docker build -t ${DOCKER_REGISTRY}/${SERVICE_NAME}:dev -f Dockerfile_dev .
docker push ${DOCKER_REGISTRY}/${SERVICE_NAME}:dev

kubectl -n mars-mars-beta delete pod --selector=service=gis-data-svc --force
