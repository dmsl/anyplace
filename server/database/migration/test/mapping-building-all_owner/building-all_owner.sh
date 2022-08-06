#!/bin/bash
source ../functions.sh
initialize
jsonObj=$(loadObjectWithApiKey)
exec_curl_mdb "/anyplace/mapping/building/all_owner" $jsonObj
# call script.python (make it pretty)
exec_curl_cdb "/anyplace/mapping/building/all_owner" $jsonObj
# call script.python (make it pretty)
python3 compare.py "buildings"

