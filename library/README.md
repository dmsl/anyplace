anyplaceAnyplace Library
================

A standalone java library that provides access to a set of endpoints from the [Anyplace API](https://ap.cs.ucy.ac.cy/developers/). The library connects directly with the Anyplace API and is written in JAVA. The clients of this library collect the WiFi fingerprints from a building and use this library for localization. The library has been testing on linux, mac and android systems.

Preamble
---

Author(s): Constandinos Demetriou, Christakis Achilleos, Marcos Antonios Charalambous

Co-supervisor: Paschalis Mpeis

Supervisor: Demetrios Zeinalipour-Yazti

Copyright (c) 2019 Data Management Systems Laboratory, University of Cyprus

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program.
If not, see [http://www.gnu.org/licenses/]([http://www.gnu.org/licenses/)

Installation
----
Make sure you have [java](https://www.java.com/en/download/) tool installed in your system.
The following command clone the repository:
```bash
git clone https://github.com/dmsl/anyplace.git && cd library
```

You will need an `API KEY`, which can be obtained from architect as follows:
![alt text]( https://dmsl.cs.ucy.ac.cy/images/github-demos/architect_api_key_demo.png "Anyplace API KEY")

Usage
---
#### Linux and Mac Client
```bash
java -jar anyplace.jar -<endpoint> <parameters>
```

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
java -jar bin/anyplace-lib.jar -estimatePosOffline mac username_1373876832005 1 1
```

Implemented Endpoints
---
#### Navigation
* __poiDetails__: Get Point of Interest details.
* __navigationXY__: Get Navigation instructions from a given location to a POI.
* __navPoiToPoi__: Get Navigation instructions between 2 POIs

#### Mapping(Public)
* __buildingAll__: Get all annotated buildings.
* __buildingsByCampus__: Get all buildings for a campus.
* __buildingsByBuildingCode__: Get all buildings with the same code.
* __nearbyBuildings__: Get annotated buildings near you (50 meters radius).
* __allBuildingFloors__: Get all floors of a building.
* __allBuildingPOIs__: Get all POIs inside a building.
* __allBuildingFloorPOIs__: Get all POIs inside a floor.
* __connectionsByFloor__: Get all POI connections inside a floor.
* __heatmapBuidFloor__: Get all positions with their respective Wi-Fi radio measurements.

#### Blueprints
* __floor64__: Downloads the floor plan in a base64 png format (w/o prefix).
* __floortiles__: Fetches the floor plan tiles zip link.

#### Position
* __radioByCoordinatesFloor__: Radiomap using all the entries near the coordinate parameters.
* __radioBuidFloor__: Radiomap using all the entries near the coordinate parameters.
* __radioBuidFloorRange__: Radiomap using all the entries near the coordinate parameters.
* __estimatePosition__: Estimate the location of the user.
* __estimatePosOffline__: Estimate the location of the user offline. Needs the radiomap file.

Credits
---
* Constandinos Demetriou
* Christakis Achilleos
* Marcos Antonios Charalambous

TEAM
---
* [https://anyplace.cs.ucy.ac.cy/](https://anyplace.cs.ucy.ac.cy/)
