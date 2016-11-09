#!/usr/bin/env bash

# This builds the gis-import and restarts the gisimport docker container inside the websuite.
# It requires the websuite to be running and that both git repos are cloned to the same folder.

mvn package
docker-compose -f docker-compose_dev.yml build
cd ../mars-cloudinanutshell
docker-compose -f lennart.yml stop gisimport
docker-compose -f lennart.yml up -d gisimport
docker-compose -f lennart.yml logs -f gisimport
