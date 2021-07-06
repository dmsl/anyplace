#!/bin/bash
set -o allexport; source ../.env; set +o allexport
source ../functions.sh

jsonObj=$(loadObjectWithApiKey2)
initialize

for ((i=0;i<$EXEC_TIMES;i++))
do
	(time exec_curl_mdb "/anyplace/position/radio_by_floor_bbox" $jsonObj) &>> exec1.log
done
cat exec1.log | grep real | tr '\n' ' ' | tr '\t' ' ' | sed "s/real //g" | sed "s/0m//g" | tr ' ' ',' | sed "s/^/mongo: /" | sed "s/.$//" > exec.log
rm exec1.log
