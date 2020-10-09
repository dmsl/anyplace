#!/bin/bash
#
# Starts Anyplace Server
#

JKS_KEY=$1
KEY_STORE_KEY=$2
PLAY_SECRET=$3
PORT_HTTPS=$4
PORT_HTTP=$5
COUCHBASE_HOSTNAME=$6
COUCHBASE_CLUSTER=$7
COUCHBASE_PORT=$8
COUCHBASE_USER=$9
COUCHBASE_PASS="${10}"
COUCHBASE_BUCKET="${11}"

COUCHBASE=$COUCHBASE_HOSTNAME:$COUCHBASE_PORT

LOG_FILE=/opt/log/anyplace.log

log() {
  echo "$(date +"%d/%m/%y") $(date +"%H:%M:%S"): $@"
}

echo "Starting anyplace server:"
echo "Couchbase: $COUCHBASE"
echo "HTTPS: Port: $PORT_HTTPS"
echo "HTTP: Port: $PORT_HTTP"

log "Docker: anyplace up" >> $LOG_FILE

CHECK_COUCHBASE_STARTED() {
  curl --silent $COUCHBASE/pools > /dev/null
  echo $?
}

CHECK_COUCHBASE_INITIALIZED() {
  q=$(curl --silent -u $COUCHBASE_USER:$COUCHBASE_PASS \
    $COUCHBASE/pools/default/buckets/$COUCHBASE_BUCKET)

    if [[ $q == *"not found"* ]]; then
        echo "0" # true: not initialized
    else
        echo "1" # false: initialized
    fi
}

# Wait until couchbase service is ready
delay=2
waitingTime=0
until [[ $(CHECK_COUCHBASE_STARTED) = 0 ]]; do
  >&2 log "     - Waiting CouchbaseDB ($COUCHBASE)."
  sleep $delay
done

RED='\033[0;31m'
NOCOLOR='\033[0m'

if [[ $(CHECK_COUCHBASE_INITIALIZED) = "0" ]]; then
  >&2 log " "
  >&2 log " "
  >&2 log "     - ${RED}CouchbaseDB not initialized! ${NOCOLOR}"
  >&2 log "       Please run: ${RED}./post_install.sh${NOCOLOR}"
  >&2 log "       Sleeping for a minute.."
  >&2 log "       If awoke while post_install.sh runs restart the containers."
sleep 60
fi

# Wait until couchbase is initialized (only on first run)
until [[ $(CHECK_COUCHBASE_INITIALIZED) = "1" ]]; do
  >&2 log "     - CouchbaseDB not initialized. Sleeping.."
  sleep 60
done

# copy certificate
cp -R /opt/cert /opt/cp/

# RUN ANYPLACE SERVER:
# Any further configuration is passed through the environment to application.conf,
# without any intervention from this script.
# application.conf is preconfigured for develop or production deployments.
# Such configuration concerns:
# . couchbase
#       . username
#       . password
#       . bucket
#       . port
# . play
#       . ip
#       . port
#       . filesystem:
#               . floor_plans
#               . radiomaps_raw
#               . radiomaps_frozen
ssl_dir=/opt/cp/cert/
echo "SSL Certificates: $ssl_dir" 
ls -l $ssl_dir 
echo $JKS_KEY
echo $KEY_STORE_KEY
ls -l $JKS_KEY
cat $KEY_STORE_KEY

log "Starting anyplace server" >> $LOG_FILE 
/opt/app/bin/anyplace \
        -Dhttps.port=$PORT_HTTPS -Dhttp.port=$PORT_HTTP \
        -Dpidfile.path=/dev/null -Dplay.crypto.secret=$PLAY_SECRET \
        -Dplay.server.https.keyStore.path=$JKS_KEY \
        -Dplay.server.https.keyStore.password=$(cat $KEY_STORE_KEY) \
        >> $LOG_FILE 2>&1
