# TASKS:PM

## PROBLEMS OF LOGGER:
1. not loading all buildings initially
.. and file caching may be slower..

2. not zooming the map once location is found.

3. TODO hide floor buttons if no building is selected (or if buildign has a single floor)

#### BUG: if all buildings loaded, then do NOT load again.

#### BUG: tiles not shown...
maybe is it due to this?
 : `Error while creating Radio Map on-the-fly!`

#### saving spinner buildings
- and building selection activity: completely change this

#### Deprecate ObjectCache

#### inspect downloading radiomaps?
/storage/emulated/0/Android/data/cy.ac.ucy.cs.anyplace.logger/files/radiomaps/building_134a0aff-4471-4233-9d2e-a2a3e2c724e9_1591812004372fl_0/fl_0_indoor-radiomap.txt

#### exception:
2021-04-21 23:01:30.015 15945-16033/cy.ac.ucy.cs.anyplace.logger D/MapTileProvider: readMapLocations exception with length=1; index=1

#### FetchBuildingsTask:
make this modern.
also make a variant:
- fetch nearby buildings (at least on the country level)
  + not all world buildings are needed...


#### BUG:
https://stackoverflow.com/a/64512794/776345
 java.lang.IllegalArgumentException: Unmanaged descriptor
          at com.google.maps.api.android.lib6.common.m.g(:com.google.android.gms.dynamite_mapsdynamite@211213081@21.12.13 (120400-0):0)
          at com.google.maps.api.android.lib6.impl.v.c(:com.google.android.gms.dynamite_mapsdynamite@211213081@21.12.13 (120400-0):1)
          at com.google.maps.api.android.lib6.impl.db.v(:com.google.android.gms.dynamite_mapsdynamite@211213081@21.12.13 (120400-0):2)

### Files permission:
TODO when permission is denied ask again!
- Better to remove completely SDCARD.. and keep separate caches for 2 apps
  or find a way to share them..

#### BUG:
storage/emulated/0/Android/data/cy.ac.ucy.cs.anyplace.logger/files/floor_plans/building_134a0aff-4471-4233-9d2e-a2a3e2c724e9_1591812004372/0/tiles_archive created
I/System.out: Bad response

    LOG.E(TAG, "disableRecordButton")

#### BUG2
- when a building is selected it crashes.

--- 

## MERGE CACHES OF LOGGER / NAVIGATOR
Currently we have 2 caches, e.g.:
/data/user/0/cy.ac.ucy.cs.anyplace.navigator/files/app/json/buildings.all.json
/data/user/0/cy.ac.ucy.cs.anyplace.logger/files/app/json/buildings.all.json

We need <NAVIGATOR> caches, and logger must be able to read them.

## Crashes on cache:
Files will be stored at:
/storage/emulated/0/Android/data/cy.ac.ucy.cs.anyplace.logger/files

Legacy Anyplace stores them at:
/sdcard/Android/data/com.dmsl.anyplace/files

# COROUTINES
## CLEANUP OF TASKS:
### FetchPoiByPuidTask

# DEPRECATE
## jcenter() (easypermissions)
Very soon will go down. should use mavenCentral()
easypermissions is still migrating though.
MORE: https://github.com/googlesamples/easypermissions/issues/319

# CLEANUP
## possible whatever class implements Serializable
- Use ROOM + Gson
- Models must be Models. That is data classes.
    - They should not extend or implement anything, at all.
    - just fields + getters + setters

