#!/bin/bash
# Copies certificate from .anyplace directory
# Load .env file
set -o allexport; source .env; set +o allexport
cert=$ANYPLACE_DATA_PLAY"/cert"

if [ -d $cert ]; then
    echo "Caching certificate from: $cert"
    cp -R $cert anyplace/cache
else 
    echo "ERROR certificate does not exist: $cert"
fi
