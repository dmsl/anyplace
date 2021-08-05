# Migration guide

***
This guide will help you migrate from CouchbaseDB to MongoDB.
***

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

### Report from official database:
#### Completion time: ~15mins
#### Objects
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


##### Caching fingerprints while migrating:
Fetches from the `fingerprintWifi` exported raw data, according to the buildings (`buid`).
If a particular `buid` does not yet exist in the intermediate files, it adds its fingerprints
to the MongoDB collection.

This allows the script to resume from that point in case the migration is interrupted.
For the Anyplace database the migration takes roughly the below:
- `fingerPrintsWiFi`: 6 hours TODO:NN (fill the below..)
- `otherCollection`: N hours TODO:NN
</details>

---

# Other files

## [testDefineCollections.py](testDefineCollections.py)
Uses a cached copy of pulled json objects from CDB and defines the collections.
Used only for tests.

- [CHANGES.COLLECTIONS.md](CHANGES.COLLECTIONS.md)  
Contains some more detailed changes made when the project moved from Couchbase from MongoDB.