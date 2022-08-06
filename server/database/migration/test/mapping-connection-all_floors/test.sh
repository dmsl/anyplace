#!/bin/bash
source ../functions.sh
jsonObj=$(cat input.json)

exec_curl_mdb "/anyplace/mapping/connection/all_floors" $jsonObj
# call script.python (make it pretty)
exec_curl_cdb "/anyplace/mapping/connection/all_floors" $jsonObj
# call script.python (make it pretty)
python3 compare.py "edges"

