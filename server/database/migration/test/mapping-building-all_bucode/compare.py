import json
import sys


def defineKey(key):
    if key == "buildings":
        return 'buid'
    if key == "floorplans":
        return 'fuid'


def compareObjs(couchObjects, mongoObjects, type):
    keys = ["buid", "address", "is_published", "coordinates_lat", "name", "description", "bucode", "coordinates_lon",
            "url"]
    isSame = 1
    uniqueKey = defineKey(type)
    count = 0
    couchCount = 0
    mongoCount = 0
    notFound = []
    for jsCouch in couchObjects:
        couchCount += 1
        notFound.insert(0, jsCouch)
    for jsMongo in mongoObjects:
        mongoCount += 1
    if couchCount != mongoCount:
        isSame = 2
    print("MongoDB total json objects: ", mongoCount)
    print("CouchDB total json objects: ", couchCount)
    for jsCouch in couchObjects:
        for jsMongo in mongoObjects:
            if jsCouch[uniqueKey] == jsMongo[uniqueKey]:
                for key in keys:
                    if key in jsCouch and key in jsMongo:
                        if jsCouch[key] != jsMongo[key]:
                            print(jsCouch[uniqueKey], "differ at ", key)
                            print("CouchDB: ", jsCouch[key])
                            print("MongoDB: ", jsMongo[key])
                            isSame = 3
                            # return 0
                        else:
                            for x in notFound:
                                if x == jsCouch:
                                    notFound.remove(x)
    for x in notFound:
        print("Didn't find: ", x)
    return isSame


def parseEndpoint(file):
    try:
        file = open(file, encoding="utf8")
    except:
        print("Path was not correct.")
        exit()
    jsonKey = "buildings"
    return json.loads(file.readline())[jsonKey]


# main
if len(sys.argv) - 1 != 1:
	print("CompareJsons::Provide type of endpoint.")
	exit()
couchObjects = parseEndpoint("couch.json")
mongoObjects = parseEndpoint("mongo.json")
# isSame: 1 = same, 2 = different # of objects, 3 = at least 1 object has different values
isSame = compareObjs(couchObjects, mongoObjects, sys.argv[1])
if isSame == 1:
    print("Files are same.")
elif isSame == 2:
    print("Different number of Jsons")
elif isSame == 3:
    print("At least one CouchDB json object has different key-value from MongoDB")

