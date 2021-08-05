# Migration guide

***
This guide will help you migrate from CouchbaseDB to MongoDB.
***

## 0. Create config.py
Copy [helpers/config.example.py](helpers/config.example.py) to helpers/config.py
and modify as necessary.

## 2. [step1_pullCouchbase.py](step1_pullCouchbase.py)

Might need to create the migration folder:
```bash
sudo mkdir -p /data/db/migration-couchbase/
sudo chown anyplace:anyplace /data/db/migration-couchbase/
```

It also assumes that cbexport is installed at:
`opt/couchbase/bin/cbexport`



> 2021-08-05T13:18:52.896+03:00 WARN: Value of key `<ud>NaNNaN89.0253614805139326628a:15:14:43:76:e1</ud>` is not valid json, skipping -- jsondata.(*jsonLineCallbacks).Mutation() at lines_exporter.go:69
>


#### Object Report from official database
```
Date:  05/08/2021

Buildinds:       4439
Campus:          258
Edge:            45269
Fingerprints:    11142975
Floorplans:      3975
Pois:            49037
Users:           4353
Undefined:       2
```

## 2. [step2_pushToMongo.py](step2_pushToMongo.py)


##### Regarding fingerprints:
## TODO:NN review step2.. <-
Fetches from the `fingerprintWifi` bucket, data according to the buildings (`buid`).
If a particular `buid` does not yet exist in the intermediate files, it adds its fingerprints
to the MongoDB collection.

This allows the script to resume from that point in case the migration is interrupted.
For the Anyplace database the migration takes roughly the below:
- `fingerPrintsWiFi`: 6 hours TODO:NN (fill the below..)
- `otherCollection`: N hours TODO:NN



---

# Other files

- [CHANGES.COLLECTIONS.md](CHANGES.COLLECTIONS.md)  
Contains some more detailed changes made when the project moved from Couchbase from MongoDB.