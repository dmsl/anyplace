import pymongo
import json
from helpers.config import *
import os

def pushBuilding(database):
    path = getCollectionsPath() + "/buildings.json"
    count = 0
    try:
        file = open(path, encoding="utf8")
    except:
        print("Path was not correct.")
        return
    count = 0
    print("Pushing Buildings..")
    col = database["buildings"]
    while True:
        line = file.readline()
        if not line:
            break
        count += 1
        col.insert_one(json.loads(line))
    if count == 0:
        print("File is empty.")
    else:
        print(count, "Buildings were pushed.")
    return

def pushCampuses(database):
    path = getCollectionsPath() + "/campuses.json"
    count = 0
    try:
        file = open(path, encoding="utf8")
    except:
        print("Path was not correct.")
        return
    count = 0
    print("Pushing Campuses..")
    col = database["campuses"]
    while True:
        line = file.readline()
        if not line:
            break
        count += 1
        col.insert_one(json.loads(line))
    if count == 0:
        print("File is empty.")
    else:
        print(count, "Campuses were pushed.")
    return


def pushEdges(database):
    path = getCollectionsPath() + "/edges.json"
    count = 0
    try:
        file = open(path, encoding="utf8")
    except:
        print("Path was not correct.")
        return
    count = 0
    print("Pushing Edges..")
    col = database["edges"]
    while True:
        line = file.readline()
        if not line:
            break
        count += 1
        col.insert_one(json.loads(line))
    if count == 0:
        print("File is empty.")
    else:
        print(count, "Edges were pushed.")
    return


# "D:\JsonFiles\\fingerprintswifi.json", collection: FingerprintWifi
def pushFingerprintsWifi(database):
    path = getCollectionsPath() + "/fingerprintsWifi"
    dupes = 0
    countAll = 0
    filesPaths = os.listdir(path)
    print("Pushing FingerprintsWifi..")
    for fileP in filesPaths: 
        try:
            file = open(path + "/" + fileP, encoding="utf8")
        except:
            print("Path was not correct.")
            return
        count = 0
        col = database["fingerprintsWifi"]
        query = {"buid": fileP}
        doc = col.find_one(query)
        if doc is None:
            print("There are no fingerprints in database with buid: " + fileP)
            print("Adding fingerprints of building: " + fileP)
        else:
            # print("Found fingreprints for building: " + fileP)
            continue
        visited = []
        while True:
            line = file.readline()  # read new obj
            if not line:
                break
            count += 1
            found = False
            obj = json.loads(line)
            newFingerprint = createNewFingerprint(obj)  # create new fingerprint with "measurements"
            if len(visited) == 0:  # if its the first obj then add it to visited fingerprints
                visited.insert(0, newFingerprint)  # mark as visited
            else:
                for jsonFingeprint in visited:  # for every visited json
                    if obj["timestamp"] == jsonFingeprint["timestamp"] and obj["buid"] == jsonFingeprint["buid"] \
                            and obj["floor"] == jsonFingeprint["floor"] and obj["geometry"] == jsonFingeprint["geometry"] \
                            and obj["heading"] == jsonFingeprint["heading"]:  # locate visited
                        # dupes += 1
                        found = True
                        updateExistingFingerprint(jsonFingeprint, obj)  # update visited
                if not found:
                    visited.insert(0, newFingerprint)  # if first seen, add it
        if count == 0:
            print("File is empty.")
        else:
            print(len(visited), "FingerprintsWifi were pushed for building" + fileP +"\n")
        for j in visited:
            col.insert_one(j)
            createAndPushFingerprintsHeatmap(database, j)
            countAll += 1
    print(countAll, "FingerprintsWifi were pushed in total.")
    # print("visited length = ", len(visited))
    # print("dupes = ", dupes)
    return


def createNewFingerprint(obj):
    newFin = obj
    newFin["measurements"] = [[obj["MAC"], obj["rss"]]]
    del newFin["MAC"]
    del newFin["rss"]
    return newFin


def updateExistingFingerprint(old, new):
    updFin = old
    newMeasurement = new["measurements"]
    updFin["measurements"] += newMeasurement


def createAndPushFingerprintsHeatmap(database, obj):
    finHeatmap = obj
    del finHeatmap["x"]
    del finHeatmap["y"]
    del finHeatmap["heading"]
    finHeatmap["total"] = len(finHeatmap["measurements"])
    del finHeatmap["measurements"]
    if "strongestWifi" in obj.keys():
        del finHeatmap["strongestWifi"]
    finHeatmap["location"] = finHeatmap["geometry"]
    del finHeatmap["geometry"]
    col = database["fingerprintsHeatmap"]
    col.insert_one(finHeatmap)


def pushFingerprintsBle(database):
    path = getCollectionsPath() + "/fingerprintsBle.json"
    count = 0
    print("Pushing FingerprintsBle..")
    print(count, "json files were pushed.")
    return


def pushFloorplans(database):
    path = getCollectionsPath() + "/floorplans.json"
    count = 0
    try:
        file = open(path, encoding="utf8")
    except:
        print("Path was not correct.")
        return
    count = 0
    print("Pushing Floorplans..")
    col = database["floorplans"]
    while True:
        line = file.readline()
        if not line:
            break
        count += 1
        col.insert_one(json.loads(line))
    if count == 0:
        print("File is empty.")
    else:
        print(count, "Floorplans were pushed.")
    return


def pushPois(database):
    path = getCollectionsPath() + "/pois.json"
    count = 0
    try:
        file = open(path, encoding="utf8")
    except:
        print("Path was not correct.")
        return
    count = 0
    print("Pushing Pois..")
    col = database["pois"]
    while True:
        line = file.readline()
        if not line:
            break
        count += 1
        col.insert_one(json.loads(line))
    if count == 0:
        print("File is empty.")
    else:
        print(count, "Pois were pushed.")
    return


def pushUsers(database):
    path = getCollectionsPath() + "/users.json"
    try:
        file = open(path, encoding="utf8")
    except:
        print("Path was not correct.")
        return
    count = 0
    print("Pushing Users..")
    col = database["users"]
    while True:
        line = file.readline()
        if not line:
            break
        count += 1
        col.insert_one(json.loads(line))
    if count == 0:
        print("File is empty.")
    else:
        print(count, "Users were pushed.")
    return


def dropAllCollections(database, colls):
    print("Dropping all collections..")
    for x in colls:
        mycol = database[x]
        print("Dropping ", x)
        mycol.drop()


def createCollections(database):
    print("\nCreating collections..")
    temp = {"created": True}
    colList = ["buildings", "campuses", "edges", "fingerprintsBle", "fingerprintsWifi", "floorplans", "pois", "users"]
    for x in colList:
        coll = database[x]
        coll.insert_one(temp)
        coll.delete_one(temp)
        print("Created ", x)


# Main
uri = 'mongodb://' + MDB_USER + ':' + MDB_PASSWORD + '@' + MDB_DOMAIN_NAME + ':' + MDB_PORT + '/'
client = pymongo.MongoClient(uri)
db = client[MDB_DATABASE]
collections = db.collection_names()
countCollections = len(collections)
print("Current number of collections: ", countCollections)
# dropAllCollections(db, collections);  exit()
if countCollections == 9:  # if database has 8 collections
    print("Checking collections..")
    for x in collections:  # are those the correct colletions?
        if x != "buildings" and x != "campuses" and x != "edges" and x != "fingerprintsBle" and x != "fingerprintsWifi" \
                and x != "floorplans" and x != "pois" and x != "users" and x != "accessPointsWifi" and x != "fingerprintsHeatmap":
            print("Collection ", x, "has wrong name.")
            print("Exiting..")
            exit()
    print("All collections were found.")
    print("Drop all collections? [y/n]")
    value = input()
    if value == "y" or value == "Y" or value == "yes" or value == "YES":
        dropAllCollections(db, collections)
    else:
        print("Do you want to check fingerprintsWifi? [Y/N]. (Recommended if the script crashed before adding all fingerprints)")
        value = input()
        if value == "y" or value == "Y" or value == "yes" or value == "YES":
            pushFingerprintsWifi(db)
        else:
            print("Exiting..")
            exit()
elif countCollections < 8:
    buildings = False
    campuses = False
    edges = False
    fingerprintsBle = False
    fingerprintsWifi = False
    floorplans = False
    pois = False
    users = False
    if "buildings" in collections:
        buildings = True
    if "campuses" in collections:
        campuses = True
    if "edges" in collections:
        edges = True
    if "fingerprintsBle" in collections:
        fingerprintsBle = True
    if "fingerprintsWifi" in collections:
        fingerprintsWifi = True
    if "floorplans" in collections:
        floorplans = True
    if "pois" in collections:
        pois = True
    if "users" in collections:
        users = True
    if buildings == False:
        pushBuilding(db)
    else: 
        print("Buildings already exists.")
    if campuses == False:
        pushCampuses(db)
    else:
        print("Campuses already exists.")
    if edges == False:
        pushEdges(db)
    else:
        print("Edges already exists.")
    if fingerprintsWifi == False:
        pushFingerprintsWifi(db)
    else:
        print("Fingerprints Wifi already exists.")
        print("Do you want to check them anyway? [Y/N]. (Recommended if the script crashed before adding all fingerprins)")
        value = input()
        if value == "y" or value == "Y" or value == "yes" or value == "YES":
            pushFingerprintsWifi(db)
    if fingerprintsBle == False:
        pushFingerprintsBle(db)  # creating empty collection for now
    else:
        print("Fingerprints Ble already exists.")
    if floorplans == False:
        pushFloorplans(db)
    else:
        print("Floorplans already exists.")
    if pois == False:
        pushPois(db)
    else:
        print("Pois already exists.")
    if users == False:
        pushUsers(db)
    else:
        print("Users already exists.")

