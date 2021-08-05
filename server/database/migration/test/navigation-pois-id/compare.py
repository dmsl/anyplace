import json
import sys


def defineKey(key):
    if key == "buildings":
        return 'buid'
    if key == "pois":
        return 'puid'
    if key == "floorplans":
        return 'fuid'


def compareBuildingId(cObject, mObject, type):
    keys = ["buid", "image", "is_published", "coordinates_lat", "name", "description", "is_door", "coordinates_lon",
            "url", "floor_number", "floor_name", "is_building_entrance", "puid", "pois_type", "geometry"]
    isSame2 = 1
    uniqueKey = defineKey(type)
    if cObject[uniqueKey] == mObject[uniqueKey]:
        for key in keys:
            if key in cObject and key in mObject:
                if cObject[key] != mObject[key]:
                    print(cObject[uniqueKey], "differ at ", key)
                    print("CouchDB: ", cObject[key])
                    print("MongoDB: ", mObject[key])
                    isSame2 = 3
    return isSame2


def parseEndpoint(file):
    try:
        file = open(file, encoding="utf8")
    except:
        print("Path was not correct.")
        exit()
    return json.loads(file.readline())


# main
if len(sys.argv) - 1 != 1:
        print("CompareJsons::Provide type of endpoint.")
        exit()
couchObjects = parseEndpoint("couch.json")
mongoObjects = parseEndpoint("mongo.json")
# isSame: 1 = same, 2 = different # of objects, 3 = at least 1 object has different values
isSame = compareBuildingId(couchObjects, mongoObjects, sys.argv[1])
if isSame == 1:
    print("Files are same.")
elif isSame == 2:
    print("Different number of Jsons")
elif isSame == 3:
    print("At least one CouchDB json object has different key-value from MongoDB")

