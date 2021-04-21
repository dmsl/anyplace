# TASKS:PM
## saving spinner buildings
- and building selection activity: completely change this

## Deprecate ObjectCache

### inspect downloading radiomaps?
/storage/emulated/0/Android/data/cy.ac.ucy.cs.anyplace.logger/files/radiomaps/building_134a0aff-4471-4233-9d2e-a2a3e2c724e9_1591812004372fl_0/fl_0_indoor-radiomap.txt

### exception:
2021-04-21 23:01:30.015 15945-16033/cy.ac.ucy.cs.anyplace.logger D/MapTileProvider: readMapLocations exception with length=1; index=1


## Files permission:
TODO when permission is denied ask again!

## FIXME Crashes on cache:
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


