#!/usr/bin/env bash

# Builds the gis-data JAR and restarts the pod.

mvn clean package
kubectl delete pod $(kubectl get pod |grep gis-data |awk '{print $1;}')
