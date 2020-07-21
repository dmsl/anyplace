#!/bin/bash

# THIS TESTS THAT THE SERVER WORKS AS EXPECTED


echo "SERVER: "
cat ~/.anyplace | grep host

N=1
echo "Get all annotated buildings:"
sleep $N
java -jar anyplace.jar -buildingAll
echo -e "Get all annotated buildings: [DONE]\n"

echo "Get all floors of a building:"
sleep $N
java -jar anyplace.jar -allBuildingFloors username_1373876832005
echo -e "Get all floors of a building: [DONE]\n"

echo "Get all POI connections inside a floor:"
sleep $N
java -jar anyplace.jar -connectionsByFloor username_1373876832005 1
echo -e "Get all POI connections inside a floor: [DONE]\n"

echo "Get all positions with their respective Wi-Fi radio measurements:"
sleep $N
java -jar anyplace.jar -heatmapBuidFloor username_1373876832005 1
echo -e "Get all positions with their respective Wi-Fi radio measurements: [DONE]\n"

echo "Radiomap using all the entries near the coordinate parameters:"
sleep $N
java -jar anyplace.jar -radioBuidFloor username_1373876832005 1
echo -e "Radiomap using all the entries near the coordinate parameters: [DONE]\n"

# CHECK IF IS MAC OR LINUXT
echo "Estimate the location of the user:"
sleep $N
#java -jar anyplace.jar -estimatePosition linux username_1373876832005 1 1
java -jar anyplace.jar -estimatePosition mac username_1373876832005 1 1
echo -e "Estimate the location of the user: [DONE]\n"

echo "Estimate the location of the user offline. Needs the radiomap file:"
sleep $N
java -jar anyplace.jar -estimatePosOffline mac username_1373876832005 1 1
echo -e "Estimate the location of the user offline. Needs the radiomap file: [DONE]\n"


echo "FINISHED: SERVER USED: "
cat ~/.anyplace | grep host
