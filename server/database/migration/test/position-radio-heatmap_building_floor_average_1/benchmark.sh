#!/bin/bash
set -o allexport; source ../.env; set +o allexport
source ../functions.sh

jsonObj=$(cat input.json)
initialize

for ((i=0;i<$EXEC_TIMES;i++))
do
	(time exec_curl_mdb "/anyplace/position/radio/heatmap_building_floor_average_1" $jsonObj) &>> exec1.log
done
cat exec1.log | grep real | tr '\n' ' ' | tr '\t' ' ' | sed "s/real //g" | sed "s/0m//g" | tr ' ' ',' | sed "s/^/mongo: /" | sed "s/.$//" > exec.log
echo "" >> exec.log
for ((i=0;i<$EXEC_TIMES;i++))
do
	(time exec_curl_cdb "/anyplace/position/radio/heatmap_building_floor_average_1" $jsonObj) &>> exec2.log
done
cat exec2.log | grep real | tr '\n' ' ' | tr '\t' ' ' | sed "s/real //g" | sed "s/0m//g" | tr ' ' ',' | sed "s/^/couch: /" | sed "s/.$//" >> exec.log
rm exec1.log
rm exec2.log
