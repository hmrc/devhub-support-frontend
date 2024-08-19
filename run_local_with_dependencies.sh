#!/bin/bash

if [[ -z ${DESKPRO_KEY} ]]; then
  echo Please set the environment variable DESKPRO_KEY
  exit 1
fi

sm2 -start MONGO DATASTREAM 

sm2 -start THIRD_PARTY_DEVELOPER API_PLATFORM_MICROSERVICE API_DEFINITION
sm2 -start API_PLATFORM_DESKPRO --appendArgs '{"API_PLATFORM_DESKPRO":["-Ddeskpro.api-key='${DESKPRO_KEY}'"]}'

./run_local.sh
