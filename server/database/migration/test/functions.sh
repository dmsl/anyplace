#!/bin/bash

function exec_curl_mdb() {
	set -o allexport; source ../.env; set +o allexport
	endpoint=$1
    	input=$2
    	url=$MDB_HOST:$MDB_PORT$endpoint
	output="mongo.json"
	exec_curl "$url" "$input" "$output"
}

function exec_curl_cdb() {
	set -o allexport; source ../.env; set +o allexport
	endpoint=$1
	input=$2
	url=$CDB_HOST:$CDB_PORT$endpoint
	output="couch.json"
	exec_curl "$url" "$input" "$output"    
}

function exec_curl() {
	url=$1
	jsonObj=$2
	output=$3
	# NOTE: using insecure, otherwise we get this: https://curl.haxx.se/docs/sslcerts.html
  	# time curl...
	# curl -s -S --insecure -H "'Content-Type: application/json'" -i -d "$jsonObj" -X POST $url > $output
	curl --compressed -s -S --insecure -H 'Content-Type: application/json' -d $jsonObj -X POST $url > $output
}

function initialize() {
	set -o allexport; source ../.env; set +o allexport
	if [ -z $MDB_HOST ] || [ -z $MDB_PORT ] || [ -z $CDB_HOST ] || [ -z $CDB_PORT ] || [ -z $API_KEY ] || [ -z $EXEC_TIMES ]; then
		echo ".env not loaded"
		exit 1
	fi
	echo ".env is initialized"
}

# Call this if input.json={}
function loadObjectWithApiKey() {
	set -o allexport; source ../.env; set +o allexport
	jsonObject=$(cat input.json)
	apiKey=$API_KEY
	echo ${jsonObject::-1}"\"access_token\":\"$apiKey\"}" 
}

# Call this if input.json={"key":"value"}
function loadObjectWithApiKey2() {
	set -o allexport; source ../.env; set +o allexport
	jsonObject=$(cat input.json)
	apiKey=$API_KEY
	echo ${jsonObject::-1}",\"access_token\":\"$apiKey\"}" 
}
