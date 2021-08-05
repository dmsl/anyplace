# Migration guide

***
This guide will help you migrate from CouchbaseDB to MongoDB.
***

## 1. Create config.py
Copy [helpers/config.example.py](helpers/config.example.py) to helpers/config.py
and modify as necessary.

## 2. [step1_pullCouchbase.py](step1_pullCouchbase.py)

## 2. [step3_pullCouchbase.py](step1_pullCouchbase.py)

Pull data from couchbase

### [migration_0.py](migration_0.py):
Fetches from the `fingerprintWifi` bucket, data according to the buildings (`buid`).
If a particular `buid` does not yet exist in the intermediate files, it adds its fingerprints
to the MongoDB collection.

This allows the script to resume from that point in case the migration is interrupted.
For the Anyplace database the migration takes roughly the below:
- `fingerPrintsWiFi`: 6 hours TODO:NN (fill the below..)
- `otherCollection`: N hours TODO:NN

