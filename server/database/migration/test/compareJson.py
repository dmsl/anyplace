import json
import sys

def defineKey(key):
    if key == "buildings":
        return 'buid'
    if key == "floorplans":
        return 'fuid'


def compareObjects(couchObjects, mongoObjects, type):
    keys = ["buid", "address", "is_published", "coordinates_lat", "name", "description", "bucode", "coordinates_lon", "url"]
    uniqueKey = defineKey(type)
    if len(couchObjects) != len(mongoObjects):
        print("Different number of jsons.")
        print("CouchDB: ", len(couchObjects))
        print("MongoDB: ", len(mongoObjects))
        return 0
    for i in range(len(couchObjects)):
        found = False
        jsCouch = json.loads(couchObjects[i])
        for j in range(len(mongoObjects)):
            jsMongo = json.loads(mongoObjects[j])
            if jsCouch[uniqueKey] == jsMongo[uniqueKey]:
                for key in keys:
                    if key in jsCouch and key in jsMongo:
                        if jsCouch[key] != jsMongo[key]:
                            return 0
                found = True
                continue
        if not found:
            print("Didn't find " + jsCouch['buid'])
            return 0
    return 1

def getJsonsInList(file):
    try:
        file = open(file, encoding="utf8")
    except:
        print("Path was not correct.")
        exit()
    jsonObjects = []
    while True:
        line = file.readline()  # read new obj
        if not line:
            break
        if "{" in line:
            str = line.split("}")
            for newStr in str:
                if newStr != "]" and len(newStr) != 0:
                    if "{\"buildings\":[{" in newStr or "{\"building\":[{" in newStr:
                        newStr = newStr.replace("{\"buildings\":[{", "")
                        newStr = newStr.replace("{\"building\":[{", "")
                    newStr = newStr.replace(",{", "")
                    newStr = "{" + newStr + "}"
                    jsonObjects.insert(0, newStr)
    return jsonObjects


# main
if len(sys.argv) - 1 != 1:
    print("CompareJsons::Provide type of endpoint.")
    exit()
couchObjects = getJsonsInList("couch.json")
mongoObjects = getJsonsInList("mongo.json")
isSame = compareObjects(couchObjects, mongoObjects, sys.argv[1])
if isSame == 1:
    print("Files are same.")

