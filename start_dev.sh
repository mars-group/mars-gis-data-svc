#!/usr/bin/env bash

gradle build
docker-compose -f docker-compose_dev.yml build
docker-compose -f docker-compose_dev.yml stop
docker-compose -f docker-compose_dev.yml up