#!/usr/bin/env bash

# Builds the gis-data JAR and builds the Docker image inside minikube, then restarts the pod.

mvn clean package
eval $(minikube docker-env)
docker build -t nexus.informatik.haw-hamburg.de/gis-data-svc .
kubectl delete pod $(kubectl get pod |grep gis-data |awk '{print $1;}')
