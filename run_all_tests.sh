#!/bin/bash -e

BROWSER=$1
ENVIRONMENT=$2

sbt -J-Xmx3G -Denvironment="${ENVIRONMENT:=local}" pre-commit
