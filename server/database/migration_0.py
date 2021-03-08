import pymongo
import json
from helpers.config import *


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
    print("Pushing Campus..")
    print(count, "json files were pushed.")
    return


def pushEdges(database):
    path = getCollectionsPath() + "/edges.json"
    count = 0
    print("Pushing Edges..")
    print(count, "json files were pushed.")
    return


# "D:\JsonFiles\\fingerprintswifi.json", collection: FingerprintWifi
def pushFingerprintsWifi(database):
    path = getCollectionsPath() + "/fingerprintsWifi.json"
    # path = input("Enter the path of json file with FingerprintsWifi:\n")
    path = "D:\JsonFiles\\fingerprintswifi.json"
    dupes = 0
    try:
        file = open(path, encoding="utf8")
    except:
        print("Path was not correct.")
        return
    count = 0
    print("Pushing FingerprintsWifi..")
    col = database["FingerprintWifi"]
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
                        and obj["floor"] == jsonFingeprint["floor"] and obj["location"] == jsonFingeprint["location"] \
                        and obj["heading"] == jsonFingeprint["heading"]:  # locate visited
                    dupes += 1
                    found = True
                    updateExistingFingerprint(jsonFingeprint, obj)  # update visited
            if not found:
                visited.insert(0, newFingerprint)  # if first seen, add it
    if count == 0:
        print("File is empty.")
    else:
        print(len(visited), "FingerprintsWifi were pushed.")
    for j in visited:
        col.insert_one(j)
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


def pushFingerprintsBle(database):
    path = getCollectionsPath() + "/fingerprintsBle.json"
    count = 0
    print("Pushing FingerprintsBle..")
    print(count, "json files were pushed.")
    return


def pushFloorplans(database):
    path = getCollectionsPath() + "/floorplans.json"
    
    
    
    
    count = 0
    print("Pushing Floorplans..")
    print(count, "json files were pushed.")
    return


def pushPois(database):
    path = getCollectionsPath() + "/pois.json"
    try:
        file = open(path, encoding="utf8")
    except:
        print("Path was not correct.")
        return
    count = 0
    print("Pushing Pois..")
    col = database["pois"]
    print(count, "json files were pushed.")
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
if countCollections == 8:  # if database has 8 collections
    print("Checking collections..")
    for x in collections:  # are those the correct colletions?
        if x != "buildings" and x != "campuses" and x != "edges" and x != "fingerprintsBle" and x != "fingerprintsWifi" \
                and x != "floorplans" and x != "pois" and x != "users":
            print("Collection ", x, "has wrong name.")
            print("Exiting..")
            exit()
    print("All collections were found.")
    print("Drop all collections? [y/n]")
    value = input()
    if value == "y" or value == "Y" or value == "yes" or value == "YES":
        dropAllCollections(db, collections)
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

