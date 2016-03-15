#!/usr/bin/env bash

# This builds and restarts the gis-import docker container independent from the Websuite.

mvn package
docker-compose -f docker-compose_dev.yml build
docker-compose -f docker-compose_dev.yml stop gisimport
docker-compose -f docker-compose_dev.yml rm -f gisimport
docker-compose -f docker-compose_dev.yml up gisimport
