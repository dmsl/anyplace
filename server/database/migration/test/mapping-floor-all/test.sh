#!/bin/bash
source ../functions.sh
jsonObj=$(cat input.json)

exec_curl_mdb "/anyplace/mapping/floor/all" $jsonObj
# call script.python (make it pretty)
exec_curl_cdb "/anyplace/mapping/floor/all" $jsonObj
# call script.python (make it pretty)
python3 compare.py "floorplans"

