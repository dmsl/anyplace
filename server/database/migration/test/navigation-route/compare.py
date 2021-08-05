import json
import sys


def defineKey(key):
    if key == "buildings":
        return 'buid'
    if key == "floorplans":
        return 'fuid'


def compareJsonArray(cObj, mObj, uKey, keys):
    if len(cObj) != len(mObj):
        print(uKey, "= couch:", len(cObj), "mongo:", len(mObj))
        return False
    print("pois: couchDB = ", len(cObj), " mongoDB =", len(mObj))
    for i in range(len(cObj)):
        for j in range(len(mObj)):
            if cObj[i][uKey] == mObj[j][uKey]:
                for key in keys:
                    if key in cObj[i] and key in mObj[j]:
                        if cObj[i][key] != mObj[j][key]:
                            return False
    return True


def compare(cObject, mObject):
    keys = ["num_of_pois", "pois"]

    poisSubKeys = ["lat", "lon", "puid", "buid", "floor_number", "pois_type"]
    isSame2 = 1
    for key in keys:
        if key in cObject and key in mObject:
            if key == "pois":
                if not compareJsonArray(cObject['pois'], mObject['pois'], "puid", poisSubKeys):
                    isSame2 = 3
            elif cObject[key] != mObject[key]:
                print(cObject[uniqueKey], "differ at ", key)
                print("CouchDB: ", cObject[key])
                print("MongoDB: ", mObject[key])
                sSame2 = 3
    return isSame2


def parseEndpoint(file):
    try:
        file = open(file, encoding="utf8")
    except:
        print("Path was not correct.")
        exit()
    return json.loads(file.readline())


# main
couchObjects = parseEndpoint("couch.json")
mongoObjects = parseEndpoint("mongo.json")
# isSame: 1 = same, 2 = different # of objects, 3 = at least 1 object has different values
isSame = compare(couchObjects, mongoObjects)
if isSame == 1:
    print("Files are same.")
elif isSame == 2:
    print("Different number of Jsons")
elif isSame == 3:
    print("At least one CouchDB json object has different key-value from MongoDB")

