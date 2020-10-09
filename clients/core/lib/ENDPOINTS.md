ENDPOINTS
================


#### Examples
Get all annotated buildings:
```bash
java -jar anyplace.jar -buildingAll
```

Get all floors of a building:
```bash
java -jar anyplace.jar -allBuildingFloors <buid>
java -jar anyplace.jar -allBuildingFloors username_1373876832005
```
Get all POI connections inside a floor:
```bash
java -jar anyplace.jar -connectionsByFloor <buid> <floor>
java -jar anyplace.jar -connectionsByFloor username_1373876832005 1
```

Get all positions with their respective Wi-Fi radio measurements:
```bash
java -jar anyplace.jar -heatmapBuidFloor <buid> <floor>
java -jar anyplace.jar -heatmapBuidFloor username_1373876832005 1
```

Radiomap using all the entries near the coordinate parameters:
```bash
java -jar anyplace.jar -radioBuidFloor <buid> <floor>
java -jar anyplace.jar -radioBuidFloor username_1373876832005 1
```

Estimate the location of the user:
```bash
java -jar anyplace.jar -estimatePosition <operating_system> <buid> <floor> <algorithm>
java -jar anyplace.jar -estimatePosition linux username_1373876832005 1 1
java -jar anyplace.jar -estimatePosition mac username_1373876832005 1 1
```

Estimate the location of the user offline. Needs the radiomap file:
```bash
java -jar anyplace.jar -estimatePosOffline <operating_system> <buid> <floor> <algorithm>
java -jar anyplace.jar -estimatePosOffline linux username_1373876832005 1 1
java -jar anyplace.jar -estimatePosOffline mac username_1373876832005 1 1
```