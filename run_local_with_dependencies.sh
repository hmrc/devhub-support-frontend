#!/bin/bash

sm2 -start MONGO DATASTREAM 

sm2 -start THIRD_PARTY_DEVELOPER API_PLATFORM_MICROSERVICE API_DEFINITION

./run_local.sh
