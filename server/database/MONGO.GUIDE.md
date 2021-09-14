# MongoDB Tutorial
Some quick tutorials on MongoDB

See also:
- [SCHEMA](./schema.png)
- [How endpoints are invoked](./howto_endpoints.pdf)

### Scala Sample Queries:

<details> 
<summary>
Scala queries
</summary>

  Find all published buildings.
  ```scala
  val collection = mdb.getCollection(SCHEMA.cSpaces)
  val query = BsonDocument(SCHEMA.fIsPublished -> "true")
  val buildings = collection.find(query)
  val awaited = Await.result(buildings.toFuture(), Duration.Inf)
  val res = awaited.toList
  ```
  
  Get all near-by buildings, using bounding-box.
  ```scala
  val bbox = GeoPoint.getGeoBoundingBox(lat, lng, range)
  val collection = mdb.getCollection(SCHEMA.cSpaces)
  val buildingLookUp = collection.find(and(geoWithinBox(SCHEMA.fGeometry, bbox(0).dlat, bbox(0).dlon, bbox(1).dlat,
      bbox(1).dlon),
      or(equal(SCHEMA.fIsPublished, "true"),
      and(equal(SCHEMA.fIsPublished, "false"), equal(SCHEMA.fOwnerId, owner_id)))))
  val awaited = Await.result(buildingLookUp.toFuture(), Duration.Inf)
  val res = awaited.toList
  ```
  
  Get fingerprints between two timestamps and sort them.
  ```scala
  val collection = mdb.getCollection(SCHEMA.cFingerprintsWifi)
  val fingerprints = collection.find(and(
    and(gt(SCHEMA.fTimestamp, "0"), lt(SCHEMA.fTimestamp, "999999999999999")),
    and(equal(SCHEMA.fBuid, buid)), equal(SCHEMA.fFloor, floor))
  ).sort(orderBy(ascending(SCHEMA.fTimestamp)))
  val awaited = Await.result(fingerprints.toFuture(), Duration.Inf)
  val res = awaited.toList
  ```
  
  Get heatmaps based on buid and floor, but only project locatio, sum and count.
  ```scala
  val collection = mdb.getCollection(SCHEMA.cHeatmapWifi1)
  val query = BsonDocument(SCHEMA.fBuid -> buid, SCHEMA.fFloor -> floor)
  val radioPoints = collection.aggregate(Seq(
    Aggregates.filter(query),
    project(
      Document(SCHEMA.fLocation -> "$location", "sum" -> "$sum", "count" -> "$count")
    )))
  val awaited = Await.result(radioPoints.toFuture(), Duration.Inf)
  val res = awaited.toList
  ```
  
  Delete all edges of a floor.
  ```scala
  var collection = mdb.getCollection(SCHEMA.cEdges)
  val queryBuidA = BsonDocument(SCHEMA.fBuidA -> buid, SCHEMA.fFloorA -> floor_number)
  var deleted = collection.deleteMany(queryBuidA)
  ```

</details>


### MongoDB Compass Queries

<details> 
<summary>
Compass Queries
</summary>
  
[MongoDB Compass](https://www.mongodb.com/products/compass) is the primary interface of mongoDB where users can interact with their data. 
  
#### Filters
<details> 
<summary>
Documents: filters
</summary>
  
![documents](https://user-images.githubusercontent.com/36662690/128304201-f98b595d-7e95-4f27-ad92-20f9c41ee8e9.PNG)

Provide the filters on the first section in order to use them. Make sure you are in the correct collection.

##### Sort a collection based on insertion time:
```bash
filter: 
{ _id: -1}
```

##### Regex: [link](https://docs.mongodb.com/manual/reference/operator/query/regex/)
```bash
{ 'name': /George/ }
```

##### Find objects within a bounding box:
```bash
{geometry: { $geoWithin: { $box:  [ [ 33.0, 33.0 ], [ 35.0, 35.0 ] ] } }}
```

##### Find fingerprints within a time-span:
```
{ timestamp : { $gt :  "0000000000000", $lt : "1532759230143"}}
```

##### Find objects within a bounding box on a time-span:
```
{geometry: { $geoWithin: { $box:  [ [ 33.0, 33.0 ], [ 35.0, 35.0 ] ] } }, timestamp : { $gt :  "0000000000000", $lt : "1617117985695"}, buid: "building_8d9753f0-9dae-4772-81a6-942940ade718_1616948897991"}
```

##### Find objects where floor are not -1, 0, 1, 2.
(Used in fingerprints collection)
```
{$and:[ {buid: "username_1373876832005"}, {floor: {$ne: "1"}}, {floor: {$ne: "2"}}, {floor: {$ne: "0"}},{floor: {$ne: "-1"}} ]}
```

</details>

#### Validation
<details> 
<summary>
Documents: Validation
</summary>

##### Prevent object addition to `fingerprints` collection
The object must have geometry with valid coordinates:
- ranges: -90 to 90, and -180 to 180

```
{
  $jsonSchema: {
    required: [
      'geometry',
      'geometry.type',
      'geometry.coordinates'
    ],
    properties: {
      'geometry.type': {
        bsonType: 'string',
        'enum': [
          'Point',
          'Polygon'
        ]
      },
      'geometry.coordinates.0': {
        bsonType: 'double',
        minimum: -90,
        maximum: 90
      },
      'geometry.coordinates.1': {
        bsonType: 'double',
        minimum: -180,
        maximum: 180
      }
    }
  }
}
```
  
</details>
  
  
</details>


### Mongo Shell: `mongosh`

<details> 
<summary>
mongosh
</summary>

To open `mongosh` you can use:
- the [admin/mongosh.sh](admin/mongosh.sh) wrapper
- the terminal: `mongo --host HOST --port 27018 --username admin --password PASS`

- Mongo Compass:

![mongosh](https://user-images.githubusercontent.com/36662690/128305911-4a5fce2f-1307-4a25-bd23-2e820f90fb8b.PNG)


Mongo shell is an interactive JavaScript shell interface to MongoDB. Replaced by mongoDB compass. 
Upon download of mongodb compass mongosh is also download. Can be accessed from mongodb compass, bottom of the GUI.
  
  
#### Switch to a database:
```
use anyplace
```

#### Delete local anyplace accounts:
```bash
db.users.deleteMany({external: "anyplace"})
```

### Delete a cache collection:
```
db.heatmapWifiTimestamp1.deleteMany()
```

### Delete a database that only admin as access to:
**NOTE:** proceed with caution
```
use admin;
# not required with mongodb.sh
db.grantRolesToUser("admin", ["root"]);

use databaseToDelete;
db.dropDatabase();
```

</details>

