# Collections

## users
#### total: 4351
```json
{
  "doc_type": "account",
  "owner_id": "100428071994993122990_google",
  "type": "google"
}
```
Extra Keys:
auid clients doctype email isadmin nickname password scope username 
### Changes: 
- doc_type deleted
- type -> external
- new key: type (user, admin) (user by default) 
### V1 Changes:
- NONE

---
# TODO: 

## buildings
#### total: 4438
```json
{
  "address": "-",
  "buid": "building_1936df30-fce0-4aa3-a62a-7eedde9fbe43_1424273494826",
  "coordinates_lat": "35.13004820351493",
  "coordinates_lon": "33.37128013372421",
  "description": "\u03a9\u03a1\u0395\u03a3 \u039b\u0395\u0399\u03a4\u039f\u03a5\u03a1\u0393\u0399\u0391\u03a3\n\n\u039a\u03b1\u03c4\u03b1\u03c3\u03c4\u03ae\u03bc\u03b1\u03c4\u03b1\n\u0394\u03b5\u03c5\u03c4\u03ad\u03c1\u03b1-\u03a3\u03ac\u03b2\u03b2\u03b1\u03c4\u03bf: 09:30-20:00\n\u039a\u03c5\u03c1\u03b9\u03b1\u03ba\u03ae: 11:00-19:00\n \n\u03a5\u03c0\u03b5\u03c1\u03b1\u03b3\u03bf\u03c1\u03ac Carrefour\n\u0394\u03b5\u03c5\u03c4\u03ad\u03c1\u03b1-\u03a0\u03b1\u03c1\u03b1\u03c3\u03ba\u03b5\u03c5\u03ae: 09:30-20:00\n\u03a3\u03ac\u03b2\u03b2\u03b1\u03c4\u03bf: 09:00-20:00\n\u039a\u03c5\u03c1\u03b9\u03b1\u03ba\u03ae: 11:00-19:0\n\n\n\u03a4\u0391\u03a7\u03a5\u0394\u03a1\u039f\u039c\u0399\u039a\u0397 \u0394\u0399\u0395\u03a5\u0398\u03a5\u039d\u03a3\u0397\n\nThe Mall of Cyprus\n\u0395\u03bc\u03c0\u03bf\u03c1\u03b9\u03ba\u03cc \u03a0\u03ac\u03c1\u03ba\u03bf \"\u03a3\u03b9\u03b1\u03ba\u03cc\u03bb\u03b1\"\n\u0392\u03b5\u03c1\u03b3\u03af\u03bd\u03b1\u03c2 3\n2025 \u039b\u03b5\u03c5\u03ba\u03c9\u03c3\u03af\u03b1, \u039a\u03cd\u03c0\u03c1\u03bf\u03c2\n \n\u03a4.\u0398. 22534\n1522 \u039b\u03b5\u03c5\u03ba\u03c9\u03c3\u03af\u03b1, \u039a\u03cd\u03c0\u03c1\u03bf\u03c2\n \n\u03a4\u03b7\u03bb: 77776255 (7777MALL)\n\u0397\u03bb. \u03a4\u03b1\u03c7: info@ittl.com.cy",
  "geometry": {
    "coordinates": [
      35.13004820351493,
      33.37128013372421
    ],
    "type": "Point"
  },
  "is_published": "false",
  "name": "Mall of Cyprus, Nicosia, Cyprus",
  "url": "-"
}
```
Extra Keys:
bucode geometry username_creator co_owners owner_id 
### Changes: 
- coordinates_lat deleted 
- coordinates_lon deleted 
- geometry -> location
- dashes and nulls removed
### V1 Changes:
- is_published: String -> Boolean 

## campuses
#### total: 258
```json
{
  "buids": [
    "building_d710b1f2-d7a6-4e7c-8695-1efe76dc5f92_1471407420610"
  ],
  "cuid": "cuid_39f86bb1-0dac-4149-dd9c-e603f71f2c54_1471409069418",
  "description": "-",
  "name": "Temasek Polytechnic",
  "owner_id": "106981129559673255140_google"
}
```
Extra Keys:
greeklish 
### Changes:
- dashes and nulls removed

## edges
#### total: 45255
```json
{
  "buid": "building_e4beb0ff-0ed6-41e6-9049-905bcb109171_1510383763577",
  "buid_a": "building_e4beb0ff-0ed6-41e6-9049-905bcb109171_1510383763577",
  "buid_b": "building_e4beb0ff-0ed6-41e6-9049-905bcb109171_1510383763577",
  "cuid": "conn_poi_609217a7-8b96-4f37-bbff-7560a4aa042a_poi_42bcbffc-93fa-482b-8efd-06c972bb8c2a",
  "edge_type": "hallway",
  "floor_a": "5",
  "floor_b": "5",
  "is_published": "true",
  "pois_a": "poi_609217a7-8b96-4f37-bbff-7560a4aa042a",
  "pois_b": "poi_42bcbffc-93fa-482b-8efd-06c972bb8c2a",
  "weight": "0.007403379821926812"
}
```
### Changes: 
- NONE
### V1 Changes:
- floors: String -> (remain String?) 
- is_published: String -> Boolean 
- weight: String -> Float
### Notes: 
- some weights are None
- total edges:  45255 
- total weights:  45255 
- total none:  16837

## fingerprintsWifi
```
total: 11,142,975
building fingerprints: 23067 (username_1380538751400)
[x,y] fingerprints: 806 (48.198984618065495, 16.369674503803253)
```
```json
{
  "MAC": "24:01:c7:19:ef:01",
  "buid": "username_1380538751400",
  "floor": "1",
  "geometry": {
    "coordinates": [
      48.198984618065495,
      16.369674503803253
    ],
    "type": "Point"
  },
  "heading": "91.0",
  "rss": "-91",
  "timestamp": "1380552837415",
  "x": "48.198984618065495",
  "y": "16.369674503803253"
}
```
Extra Keys:
strongestWifi  
### Changes: 
- x deleted 
- y deleted
- geometry -> location
- measurements[] = array [Mac, rss] (not during pulling, but during inserting)
### V1 Changes:
- rss: String -> Int
- timestamp: String -> Int 
- heading: String -> Float
- floor: String -> (remain String?)

## floorPlans
#### total: 3974
```json
{
  "buid": "building_76e3928d-0ef4-40c4-902a-74dd784df9a2_1469781464845",
  "description": "5",
  "floor_name": "5",
  "floor_number": "5",
  "fuid": "building_76e3928d-0ef4-40c4-902a-74dd784df9a2_1469781464845_5",
  "is_published": "true"
}
```
Extra Keys:
username_creator bottom_left_lat bottom_left_lng top_right_lat top_right_lng height width zoom 
### Changes: 
- dashes and nulls removed
- bottom_left_lat, bottom_left_lng, top_right_lat, top_right_lng -> area (GeoJson polygon)
### V1 Changes:
- floors: String -> (remain String?) 
- is_published: String -> Boolean 
- zoom: String -> Int
- width, height -> ?? (see note bellow)
### Notes: 
- Width and height? are they all strings? (yes, but only 1 object)

## pois
#### total: 49022
```json
{
  "buid": "building_af44febe-5e5c-4c31-a8b6-a1a9277b6363_1480867788505",
  "coordinates_lat": "24.713481903079302",
  "coordinates_lon": "46.67234256863594",
  "description": "Raina Meeting Room\n\u063a\u0631\u0641\u0629 \u0627\u062c\u062a\u0645\u0627\u0639\u0627\u062a \u0631\u0627\u0646\u064a\u0627",
  "floor_name": "3",
  "floor_number": "3",
  "geometry": {
    "coordinates": [
      24.713481903079302,
      46.67234256863594
    ],
    "type": "Point"
  },
  "is_building_entrance": "false",
  "is_door": "false",
  "is_published": "true",
  "name": "Raina Meeting Room",
  "pois_type": "Meeting Room",
  "puid": "poi_50c8db73-0929-4165-9c70-7f51cc1ba1a1",
  "url": "-"
}
```
Extra Keys:
username_creator image 
### Changes: 
- coordinates_lat deleted 
- coordinates_lon deleted 
- geometry -> location
- dashes and nulls removed
### V1 Changes:
- floors: String -> (remain String?) 
- is_published: String -> Boolean 
- is_door: String -> Boolean 
- is_building_entrance: String -> Boolean 

# UNDEFINED

## CAMPUSx1
```json
{
  "address": "-",
  "buid-set": [
    "building_da6aaaab-a53c-4f43-ab20-366ef02bc3da_1424020987563",
    "building_da6aaaab-a53c-4f43-ab20-366ef02bc3da_1424020987563"
  ],
  "co_owners": [
    "112997031510415584062_google"
  ],
  "cuid": "campus_94A1A172-8BA0-4A1B-B232-DBC3CDB66B87",
  "description": "University of Cyprus Campus",
  "owner_id": "112997031510415584062_google",
  "type": "Campus",
  "url": "-",
  "zoom_coordinates_lat": "52.52816874290241",
  "zoom_coordinates_lon": "13.457137495279312"
}
```

## UNDEFINEDx2
```json
{
  "MyProperty": null,
  "address": null,
  "bottom_left_lat": null,
  "bottom_left_lng": null,
  "bucode": null,
  "buid": null,
  "buid_a": null,
  "buid_b": null,
  "buids": [
    "building_7f789d37-9fc4-448e-b5d4-5bad412c26b6_1505117259085"
  ],
  "coordinates_lat": null,
  "coordinates_lon": null,
  "cuid": "cuid_283fe8b9-824f-45b3-3039-7c3e5cccdf29_1505117284142",
  "description": "-",
  "edge_type": null,
  "floor_a": null,
  "floor_b": null,
  "floor_name": null,
  "floor_number": null,
  "floorplan": null,
  "greeklish": "false",
  "image": null,
  "is_building_entrance": null,
  "is_door": null,
  "is_published": null,
  "json": null,
  "name": "Food court",
  "owner_id": "115933152534461697908_google",
  "password": null,
  "pois_a": null,
  "pois_b": null,
  "pois_type": null,
  "pois_type2": null,
  "puid": null,
  "top_right_lat": null,
  "top_right_lng": null,
  "url": null,
  "username": null
}
```