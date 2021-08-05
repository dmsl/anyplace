#!/usr/bin/env python3
import pymongo
import json
from helpers.config import *

def printJson(obj):
    print(json.dumps(obj, indent=2, sort_keys=True))

def pushFingerprintsHeatmap(database):
    colFW = db["fingerprintsWifi"]
    fingerprints = colFW.find()
    count = 0
    for fingerprint in fingerprints:
        count += 1
        if (count % 2000) == 0:
            print("fingerprints processed: ", count)
        col = database["fingerprintsHeatmap"]
        finHeatmap = fingerprint
        query = {"buid": finHeatmap["buid"], "floor": finHeatmap["floor"], "location": finHeatmap["geometry"]}
        doc = col.find_one(query)
        total = 0
        for measurement in finHeatmap["measurements"]:
            total -= int(measurement[1]) * -1
        if doc is None:
            del finHeatmap["_id"]
            del finHeatmap["x"]
            del finHeatmap["y"]
            del finHeatmap["heading"]
            finHeatmap["timestamp"] = [finHeatmap["timestamp"]] # convert to array
            finHeatmap["location"] = finHeatmap["geometry"]
            finHeatmap["count"] = len(finHeatmap["measurements"])
            finHeatmap["total"] = total
            del finHeatmap["measurements"]
            if "strongestWifi" in fingerprint.keys():
                del finHeatmap["strongestWifi"]
            del finHeatmap["geometry"]
            finHeatmap["average"] = finHeatmap["total"] / finHeatmap["count"]
            col.insert_one(finHeatmap)
        else:
            del doc["_id"]
            newDoc = doc
            newTimestamps = []
            for x in doc["timestamp"]:
                newTimestamps.append(x)
            newTimestamps.append(finHeatmap["timestamp"])
            newDoc["timestamp"] = newTimestamps
            newDoc["total"] = doc["total"] + total
            newDoc["count"] = doc["count"] + len(finHeatmap["measurements"])
            newDoc["average"] = newDoc["total"] / newDoc["count"]
            col.replace_one(query, newDoc)


#  MAIN
uri = 'mongodb://' + MDB_USER + ':' + MDB_PASSWORD + '@' + MDB_DOMAIN_NAME + ':' + MDB_PORT + '/'
client = pymongo.MongoClient(uri)
db = client["anyplace"]
pushFingerprintsHeatmap(db)



