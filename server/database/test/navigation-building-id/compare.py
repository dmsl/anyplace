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
    for i in range(len(cObj)):
        for j in range(len(mObj)):
            if cObj[i][uKey] == mObj[j][uKey]:
                for key in keys:
                    if key in cObj[i] and key in mObj[j]:
                        if cObj[i][key] != mObj[j][key]:
                            return False
    return True


def compareBuildingId(cObject, mObject, type):
    keys = ["buid", "address", "is_published", "coordinates_lat", "name", "description", "bucode", "coordinates_lon",
            "url", "co_owners", "owner_id", "floors", "pois"]

    floorSubKeys = ["floor_name", "buid", "top_right_lat", "is_published", "username_creator", "bottom_left_lat",
                    "width", "description", "floor_number", "top_right_lng", "bottom_left_lng", "height",
                    "image_height", "fuid"]

    poisSubKeys = ["floor_name", "buid", "top_right_lat", "is_published", "username_creator", "bottom_left_lat",
                   "width", "description", "floor_number", "top_right_lng", "bottom_left_lng", "height"]

    isSame2 = 1
    uniqueKey = defineKey(type)
    if cObject[uniqueKey] == mObject[uniqueKey]:
        for key in keys:
            if key in cObject and key in mObject:
                if key == "floors":
                    if not compareJsonArray(cObject['floors'], mObject['floors'], "floor_name", floorSubKeys):
                        isSame2 = 3
                elif key == "pois":
                    print("Skipping pois comparison due to CouchDB bug.(Fetching floors instead of Pois.)")
                    #if not compareJsonArray(cObject['pois'], mObject['pois'], "puid", poisSubKeys):
                    #    isSame2 = 3
                elif cObject[key] != mObject[key]:
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

