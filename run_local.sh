#!/bin/bash

sbt "run -Drun.mode=Dev -Dhttp.port=9695 -Ddeskpro-horizon.api-key=${DESKPRO_KEY} $*"
