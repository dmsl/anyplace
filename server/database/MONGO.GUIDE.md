# MongoDB Tutorial
Some quick tutorials on MongoDB




### Scala Sample Queries:

<details> 
<summary>
Scala queries
</summary>
 TODO:NN MDB TUT  
</details>



### MongoDB Compass 

<details> 
<summary>
MongoDB Compass Queries
</summary>
  
[MongoDB Compass](https://www.mongodb.com/products/compass) is the primary interface of mongoDB where users can interact with their data. 
  
<details> 
<summary>
Documents: filters
</summary>
  
![documents](https://user-images.githubusercontent.com/36662690/128304201-f98b595d-7e95-4f27-ad92-20f9c41ee8e9.PNG)

Provide the filters on the first section in order to use them. Make sure you are in the correct collection.

Sort a collection based on insertion time:
```bash
filter: 
{ _id: -1}
```
  
Find objects with-in a bounding box:
```bash
{geometry: { $geoWithin: { $box:  [ [ 33.0, 33.0 ], [ 35.0, 35.0 ] ] } }}
```

Find fingerprints with-in a time-span:
```
{ timestamp : { $gt :  "0000000000000", $lt : "1532759230143"}}
```

Find objects with-in a bounding box on a time-span:
```
{geometry: { $geoWithin: { $box:  [ [ 33.0, 33.0 ], [ 35.0, 35.0 ] ] } }, timestamp : { $gt :  "0000000000000", $lt : "1617117985695"}, buid: "building_8d9753f0-9dae-4772-81a6-942940ade718_1616948897991"}
```

Find objects where floor are not -1, 0, 1, 2.
(Used in fingerprints collection)
```
{$and:[ {buid: "username_1373876832005"}, {floor: {$ne: "1"}}, {floor: {$ne: "2"}}, {floor: {$ne: "0"}},{floor: {$ne: "-1"}} ]}
```

</details>

<details> 
<summary>
Validation
</summary>

Prevent collection fingerprints to add JSON objects without the field geometry.
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

