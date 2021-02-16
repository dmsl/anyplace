#!/bin/bash

# INFO this has a lot of similarities with initialize
# TODO make an automatic install through docker-compose build command
# currently the installation process halts/hangs..

echo "ARGS: $#."

if [ $# -ne 9 ]; then
    echo "Error while building couchbase image! Wrong arguments given."
    exit 1
fi

ROOT_USER=$1
ROOT_PASS=$2
USER=$3
PASS=$4
CLUSTER=$5
BUCKET=$6
RAM=$7
RAM_INDEX=$8
RAM_BUCKET=$9

LOCALHOST=127.0.0.1
IP=$LOCALHOST

echo "ROOT_USER: $ROOT_USER"
echo "ROOT_PASS: $ROOT_PASS"
echo "USER: $USER"
echo "PASS: $PASS"
echo "CLUSTER: $CLUSTER"
echo "BUCKET: $BUCKET"

echo "RAM: $RAM"
echo "RAM_INDEX: $RAM_INDEX"

set -m # Enables job control
set -e # Enables error propagation

# CHECK is this to set it up?
# Run the server and send it to the background
/entrypoint.sh couchbase-server &

# Check if couchbase server is up
check_db() {
  curl --silent http://$IP:8091/pools > /dev/null
  echo $?
}

# Variable used in echo
i=1
# Echo with
log() {
  echo "[$i] [$(date +"%T")] $@"
  i=`expr $i + 1`
}

# Wait until it's ready
until [[ $(check_db) = 0 ]]; do
  >&2 log "Couchbase starting up.."
  sleep 2
done

# Setup index and memory quota
log "Initializing couchbase ........."
couchbase-cli cluster-init -c $IP --cluster-username $ROOT_USER \
    --cluster-password $ROOT_PASS \
    --cluster-name $CLUSTER --services data,index,query,fts \
    --cluster-ramsize $RAM --cluster-index-ramsize $RAM_INDEX  \
    --index-storage-setting default

sleep 15

log "Setting up: root user"
# Setup Administrator username and password
curl -v http://$IP:8091/settings/web -d port=8091 \
    -d username=$ROOT_USER -d password=$ROOT_PASS

sleep 15

# Create the buckets
log "Creating bucket: $BUCKET"
couchbase-cli bucket-create -c $IP \
    --username $ROOT_USER --password $ROOT_PASS \
    --bucket-type couchbase  --bucket $BUCKET \
    --bucket-ramsize $RAM_BUCKET

sleep 15
log "Setting up RBAC: $USER"
# Setup RBAC user using CLI
couchbase-cli user-manage -c $IP:8091 \
    --username $ROOT_USER --password $ROOT_PASS \
    --set --rbac-username $USER --rbac-password $PASS --rbac-name \"$USER\" \
    --roles bucket_full_access[$BUCKET] \
    --auth-domain local


# add node to cluster (can't do since single node)
#COUCHBASE_SERVICES="data,index,query,fts"
#couchbase-cli server-add -c $IP:8091 \
#    --username "$ROOT_USER" --password "$ROOT_PASS" \
#    --server-add $IP:8091 \
#    --server-add-username "$ROOT_USER" --server-add-password "$ROOT_PASS" \
#    --services "$COUCHBASE_SERVICES"



sleep 15
log "Rebalancing to enable cluster"
# Rebalance (needed to fully enable added nodes)
couchbase-cli rebalance  -c $IP:8091 \
  --username "$ROOT_USER" --password "$ROOT_PASS"
#--cluster ${cluster_url} \
#  --no-wait

echo "Couchbase configured!"
fg 1
