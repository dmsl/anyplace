#!/bin/bash
ROOT_USER=$COUCHBASE_ROOT_USER
ROOT_PASS=$COUCHBASE_ROOT_PASS
USER=$COUCHBASE_USER
PASS=$COUCHBASE_PASS
CLUSTER=$COUCHBASE_CLUSTER
BUCKET=$COUCHBASE_BUCKET
RAM=$COUCHBASE_RAM
RAM_INDEX=$COUCHBASE_RAM_INDEX
RAM_BUCKET=$COUCHBASE_RAM_BUCKET
CONTAINER=$CONTAINER_COUCHBASE

DOCKER="docker"
LOCALHOST=127.0.0.1
# Use local host as we are outside docker environment
IP=$LOCALHOST
#IP=$CONTAINER

couchbase="$DOCKER exec -t $CONTAINER couchbase-cli"
#docker exec anyplace-couchbase couchbase-cli

echo "Initializing couchbase: $CONTAINER"
echo "ROOT_USER: $ROOT_USER"
echo "ROOT_PASS: $ROOT_PASS"
echo "USER: $USER"
echo "PASS: $PASS"
echo "CLUSTER: $CLUSTER"
echo "BUCKET: $BUCKET"

echo "RAM: $RAM"
echo "RAM_INDEX: $RAM_INDEX"

set -m # job control
set -e # error propagation

# CHECK is this to set it up?
# Run the server and send it to the background
#/entrypoint.sh couchbase-server &

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
$couchbase cluster-init -c $IP --cluster-username $ROOT_USER \
    --cluster-password $ROOT_PASS \
    --cluster-name $CLUSTER --services data,index,query,fts \
    --cluster-ramsize $RAM --cluster-index-ramsize $RAM_INDEX  \
    --index-storage-setting default

#log "Setting up: root user"
# Setup Administrator username and password
#curl -v http://$IP:8091/settings/web -d port=8091 \
#    -d username=$ROOT_USER -d password=$ROOT_PASS
#sleep 15

# Create the buckets
log "Creating bucket: $BUCKET"
#sleep 15
$couchbase bucket-create -c $IP \
    --username $ROOT_USER --password $ROOT_PASS \
    --bucket-type couchbase  --bucket $BUCKET \
    --bucket-ramsize $RAM_BUCKET \
    --bucket-replica 0

# NOT USING RBAC
log "Setting up RBAC: $USER"
#sleep 15
# Setup RBAC user using CLI
$couchbase user-manage -c $IP:8091 \
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

log "Rebalancing to enable cluster"
#sleep 15
# Rebalance (needed to fully enable added nodes)
$couchbase rebalance  -c $IP:8091 \
  --username "$ROOT_USER" --password "$ROOT_PASS"
#--cluster ${cluster_url} \
#  --no-wait

echo "Couchbase configured!"
#fg 1
