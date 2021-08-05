#!/bin/bash
source ../functions.sh
initialize
jsonObj=$(loadObjectWithApiKey2)
echo $jsonObj
exec_curl_mdb "/anyplace/mapping/building/add" $jsonObj
# call script.python (make it pretty)
exec_curl_cdb "/anyplace/mapping/building/add" $jsonObj

