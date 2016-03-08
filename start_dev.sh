#!/usr/bin/env bash

# This builds the gis-import and restarts the gisimport docker container inside the websuite.
# It requires the websuite to be running and that both git repos are cloned to the same folder.

mvn package
docker-compose -f docker-compose_dev.yml build
cd ../marscloudinanutshell
docker-compose -f new_import_websuite_development.yml stop gisimport
docker-compose -f new_import_websuite_development.yml rm -f gisimport
docker-compose -f new_import_websuite_development.yml up gisimport