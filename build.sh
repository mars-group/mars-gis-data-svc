#!/usr/bin/env bash

# Builds the gis-data JAR and builds the Docker image inside minikube, then restarts the pod.

mvn clean package
eval $(minikube docker-env)
docker build -t artifactory.mars.haw-hamburg.de:5002/gis-data-svc_master .
kubectl delete pod $(kubectl get pod |grep gis-data |awk '{print $1;}')
