#!/bin/bash

# leave blank for latest
COUCHDB_VERSION=
COUCHDB="couchdb"

nm="anyplace"
AP_DATA_DIR="$HOME/data"
FS_DIR=$AP_DATA_DIR"/fs/"
COUCHDB_DIR=$AP_DATA_DIR"/couchdb/"
INFLUXDB_DIR=$AP_DATA_DIR"/influxdb/"

if [[ $COUCHDB_VERSION != "" ]]; then
    COUCHDB=$COUCHDB":$COUCHDB_VERSION"
fi
