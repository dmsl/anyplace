# Migration guide
This guide will help you migrate from CouchbaseDB to MongoDB.

## 0. Create config.py
Copy [helpers/config.example.py](helpers/config.example.py) to helpers/config.py
and modify as necessary.

## 1. [step1_pullCouchbase.py](step1_pullCouchbase.py)

<details >
<summary>
Details
</summary>

Might need to create the migration folder:
```bash
sudo mkdir -p /data/db/migration-couchbase/
sudo chown anyplace:anyplace /data/db/migration-couchbase/
```

It also assumes that cbexport is installed at:
`opt/couchbase/bin/cbexport`

## Report from official dataset:
#### Completion time: ~15mins
#### Objects
```
Date:  05/08/2021

Buildings:       4884
Campus:          295
Edge:            48260
Fingerprints:    12626054
Floorplans:      4356
Pois:            52677
Users:           4782
Undefined:       2
```
</details>

## 2. [step2_pushToMongo.py](step2_pushToMongo.py)

<details >
<summary>
Details
</summary>

#### Dependencies: 
Install the **pymongo** library to your python environment.

Sample code:
```bash
sudo apt install python-pip
pip install pymongo
```

## Report from official dataset:
#### Completion time: ~4 hours
188.1MB after space optimization
#### Objects
```
FingerprintsWifi:  429731 were pushed in total.
Floorplans:           4356
POIs:                 52677
Users:                4782
Spaces:               4450
Edges:                45281
Campuses:             258
------
AccessPointsWifi:
Heatmaps:             
     - HeatmapWifi1
     - HeatmapWifi2
     - HeatmapWifi3
     - HeatmapWifiTimestamp1
     - HeatmapWifiTimestamp2
     - HeatmapWifiTimestamp3

                      Those are cached, and dynamically created,
                      therefore they are excluded.
```


##### Caching fingerprints while migrating:
Fetches from the `fingerprintWifi` exported raw data, according to the buildings (`buid`).
If a particular `buid` does not yet exist in the intermediate files, it adds its fingerprints
to the MongoDB collection.

This allows the script to resume from that point in case the migration is interrupted.
For the Anyplace database the migration takes roughly the below:
- 4 hours needed


</details>

---

## Unused scripts

### [testDefineCollections.py](testDefineCollections.py)
Uses a cached copy of pulled json objects from CDB and defines the collections.
Used only for tests.

### [testGenreateHeatmaps.py](testGenreateHeatmaps.py)
Used to generate all of the Heatmaps from python.
No longer in use as the play app automatically creates the heatmap caches
on the intial request and deletes (cascading) on relevant object updates.  


i.e., when new fingerprints are uploaded, all heatmap caches are deleted.  
Then, on a subsequent request they will be generated once again.

---

## [CHANGES.COLLECTIONS.md](CHANGES.COLLECTIONS.md)  
Contains some more detailed changes made when the project moved from Couchbase from MongoDB.
