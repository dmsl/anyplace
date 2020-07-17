Anyplace-core Gradle Library
================

The core functionality of anyplace clients.

Preamble
---

Author(s): Christakis Achilleos

Co-supervisor: Paschalis Mpeis

Supervisor: Demetrios Zeinalipour-Yazti

Copyright (c) 2020 Data Management Systems Laboratory, University of Cyprus

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program.
If not, see [http://www.gnu.org/licenses/]([http://www.gnu.org/licenses/)

Usage
---
#### Linux and Mac Client
```bash
java -jar anyplace.jar -<endpoint> <parameters>
```

#### Testing
To build the jar:
```bash
./gradlew jar
```

To run all tests within the JUnit test file.
```bash
./gradlew :test --tests "Tester"
```

To run a specific test within the JUnit test file use . and the name of the test
i.e to test estimatePosition:
```bash
./gradlew :test --tests "Tester.testEtimatePosition"
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
* __heatmapByBuldingFloor__: Get all positions with their respective Wi-Fi radio measurements.

#### Blueprints
* __floor64__: Downloads the floor plan in a base64 png format (w/o prefix).
* __floortiles__: Fetches the floor plan tiles zip link.

#### Position
* __radioByCoordinatesFloor__: Radiomap of a floor using coordinates.
* __radioByBuildingFloor__: Radiomap of a floor.
* __radioByBuildingFloorRange__: Radiomap of a floor within a specified range.
* __estimatePosition__: Estimate the location of the user.
* __estimatePosOffline__: Estimate the location of the user offline. Needs the radiomap file.


TEAM
---
* [https://anyplace.cs.ucy.ac.cy/](https://anyplace.cs.ucy.ac.cy/)
