#!/bin/bash
source ../functions.sh
jsonObj=$(cat input.json)

exec_curl_mdb "/anyplace/navigation/route" $jsonObj
# call script.python (make it pretty)
exec_curl_cdb "/anyplace/navigation/route" $jsonObj
# call script.python (make it pretty)
python3 compare.py "pois"
