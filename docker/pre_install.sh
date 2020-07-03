#!/bin/bash
if [ ! -f .env ]; then
	echo "$0: ERROR: .env file not reachable"
	exit 1
fi

# Load .env file
set -o allexport; source .env; set +o allexport
apdir=$ANYPLACE_HOME_DIR

if [ ! -f $apdir  ]; then
    #mkdir -p $ANYPLACE_DATA_PLAY/app
    mkdir -p $ANYPLACE_DATA_PLAY/log
    mkdir -p $ANYPLACE_DATA_PLAY/cert

    mkdir -p $ANYPLACE_HOST_FS/$F_RMAP_RAW
    mkdir -p $ANYPLACE_HOST_FS/$F_RMAP_FROZEN
    mkdir -p $ANYPLACE_HOST_FS/$F_FLOOR_PLANS
    mkdir -p $ANYPLACE_HOST_FS/$F_ACCES
    #mkdir -p $ANYPLACE_DATA_LOGS
    mkdir -p $ANYPLACE_DATA_COUCHBASE
    mkdir -p $ANYPLACE_DATA_INFLUX
    chown -R $(id -u $USER):$(id -g $USER) $ANYPLACE_DATA
    echo "Initialized $apdir."
else 
    echo "Already initialized: $apdir."
fi
