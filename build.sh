#!/usr/bin/env bash

# This builds the gis-import

mvn package
docker-compose -f docker-compose_dev.yml build
