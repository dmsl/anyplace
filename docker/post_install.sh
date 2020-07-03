#!/bin/bash

# Run this on the first `docker-compose up` to initialize couchbase.
if [ ! -f .env ]; then
	echo "$0: ERROR: .env file not reachable"
	exit 1
fi

# Load .env file
set -o allexport; source .env; set +o allexport

export COUCHBASE_BUCKET_USER=$COUCHBASE_USER
export COUCHBASE_BUCKET_PASS=$COUCHBASE_PASS
echo $COUCHBASE_BUCKET

./couchbase/initialize.sh
cwd=$(pwd)
cd couchbase/anyplace_views
./create-views.sh
cd $cwd
