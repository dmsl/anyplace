#!/bin/bash
USERNAME=""
PASSWORD=""
BUCKET=""
IP=localhost
PORT=8092

# override values from .env (docker_compose)
USERNAME="${COUCHBASE_BUCKET_USER:-$USERNAME}"
PASSWORD="${COUCHBASE_BUCKET_PASS:-$PASSWORD}"
BUCKET="${COUCHBASE_BUCKET:-$BUCKET}"

if [[ -z "$USERNAME" ]] || [[ -z "$PASSWORD" ]] || [[ -z "$BUCKET" ]]; then
    echo "Please initialize variables"
    exit 1
fi

couchbase="http://$IP:$PORT/$BUCKET"
contentType="Content-Type: application/json"
designDoc=$couchbase/_design

##########
# DEV Views
##########
echo "Creating dev views.."

curl -X PUT -H "$contentType" $designDoc/dev_accounts -d @accounts.json -u $USERNAME:$PASSWORD
curl -X PUT -H "$contentType" $designDoc/dev_admin -d @admin.json -u $USERNAME:$PASSWORD
curl -X PUT -H "$contentType" $designDoc/dev_campus -d @accounts.json -u $USERNAME:$PASSWORD
curl -X PUT -H "$contentType" $designDoc/dev_floor -d @floor.json -u $USERNAME:$PASSWORD
curl -X PUT -H "$contentType" $designDoc/dev_magnetic -d @magnetic.json -u $USERNAME:$PASSWORD
curl -X PUT -H "$contentType" $designDoc/dev_nav -d @nav.json -u $USERNAME:$PASSWORD
curl -X PUT -H "$contentType" $designDoc/dev_radio -d @radio.json -u $USERNAME:$PASSWORD
curl -X PUT -H "$contentType" $designDoc/dev_heatmaps -d @heatmaps.json -u $USERNAME:$PASSWORD

##########
# PUBLISH
##########
echo "Publishing views.."
curl -X PUT -H "$contentType" $designDoc/accounts -d @accounts.json -u $USERNAME:$PASSWORD
curl -X PUT -H "$contentType" $designDoc/admin -d @admin.json -u $USERNAME:$PASSWORD
curl -X PUT -H "$contentType" $designDoc/campus -d @accounts.json -u $USERNAME:$PASSWORD
curl -X PUT -H "$contentType" $designDoc/floor -d @floor.json -u $USERNAME:$PASSWORD
curl -X PUT -H "$contentType" $designDoc/magnetic -d @magnetic.json -u $USERNAME:$PASSWORD
curl -X PUT -H "$contentType" $designDoc/nav -d @nav.json -u $USERNAME:$PASSWORD
curl -X PUT -H "$contentType" $designDoc/radio -d @radio.json -u $USERNAME:$PASSWORD
curl -X PUT -H "$contentType" $designDoc/heatmaps -d @heatmaps.json -u $USERNAME:$PASSWORD

if [ -z $GENERATE_SPATIAL_VIEWS  ]; then
    echo "Creating spatial views.."
    curl -X PUT -H "$contentType" $designDoc/dev_radio_spatial -d @spatial_radio.json -u $USERNAME:$PASSWORD
    curl -X PUT -H "$contentType" $designDoc/dev_nav_spatial -d @spatial_nav.json -u $USERNAME:$PASSWORD
    curl -X PUT -H "$contentType" $designDoc/dev_heatmaps_spatial -d @spatial_heatmaps.json -u $USERNAME:$PASSWORD
    curl -X PUT -H "$contentType" $designDoc/radio_spatial -d @spatial_radio.json -u $USERNAME:$PASSWORD
    curl -X PUT -H "$contentType" $designDoc/nav_spatial -d @spatial_nav.json -u $USERNAME:$PASSWORD
    curl -X PUT -H "$contentType" $designDoc/heatmaps_spatial -d @spatial_heatmaps.json -u $USERNAME:$PASSWORD
fi
