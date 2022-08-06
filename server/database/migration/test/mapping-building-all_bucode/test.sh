#!/bin/bash
source ../functions.sh
jsonObj=$(cat input.json)

exec_curl_mdb "/anyplace/mapping/building/all_bucode" $jsonObj
exec_curl_cdb "/anyplace/mapping/building/all_bucode" $jsonObj
python3 compare.py "buildings"

