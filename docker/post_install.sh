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

echo -e "\n\n"
echo "+------------------------------------------------------------"
echo "| Post install configuration finished!"
echo "| Make sure that no errors have occured."
echo "| "
echo "| NOTE: If you are using the custom generated SSL certificate"
echo "|       please 'trust it' through your operating system!"
echo "| "
echo "| Couchbase will be available at:"
echo "| $PLAY_SERVER_PROTOCOL$IP:$PORT"
echo "| "
echo "| Anyplace will be available at:"
echo "| $PLAY_SERVER_PROTOCOL$IP/"
echo "| $PLAY_SERVER_PROTOCOL$IP/viewer"
echo "| $PLAY_SERVER_PROTOCOL$IP/architect"
echo "| "
echo "| "
echo "| Thanks for using anyplace!"
echo "| https://github.com/dmsl/anyplace"
echo "+------------------------------------------------------------"
