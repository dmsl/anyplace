import json
import sys


def defineKey(key):
    if key == "buildings":
        return 'buid'
    if key == "floorplans":
        return 'fuid'


def compareObjs(couchObjects, mongoObjects, type):
    keys = ["buid", "is_published", "coordinates_lat", "name", "coordinates_lon",
            "address", "url", "username_creator", "description", "bucode"]
    possibleEmpty = ["address", "url", "username_creator", "description", "bucode"]
    isSame = 1
    emptyFields = 0
    emptyAddress = 0
    emptyUrl = 0
    emptyUsernameCreator = 0
    emptyBucode = 0
    emptyDescription = 0
    uniqueKey = defineKey(type)
    couchCount = 0
    mongoCount = 0
    for jsCouch in couchObjects:
        couchCount += 1
    for jsMongo in mongoObjects:
        mongoCount += 1
    if couchCount != mongoCount:
        isSame = 2
    print("CouchDB total json objects: ", couchCount)
    print("MongoDB total json objects: ", mongoCount)
    for jsCouch in couchObjects:
        for jsMongo in mongoObjects:
            if jsCouch[uniqueKey] == jsMongo[uniqueKey]:
                for key in keys:
                    if key in jsCouch:
                        if key in jsMongo:
                            if jsCouch[key] != jsMongo[key]:
                                print(jsCouch[uniqueKey], "differ at ", key)
                                print("CouchDB: ", jsCouch[key])
                                print("MongoDB: ", jsMongo[key])
                                isSame = 3
                                # return 0
                        else:
                            if key not in possibleEmpty:
                                print(key, "cant be - or \"\" or null")
                            # print(key, "is - or \"\" or null")
                            emptyFields += 1
                            if key == "address":
                                emptyAddress += 1
                            elif key == "bucode":
                                emptyBucode += 1
                            elif key == "description":
                                emptyDescription += 1
                            elif key == "username_creator":
                                emptyUsernameCreator +=1
                            elif key == "url":
                                emptyUrl += 1
    print("There are", emptyFields, "- or \"\" or null fields")
    print("address",emptyAddress)
    print("description", emptyDescription)
    print("url", emptyUrl)
    print("bucode", emptyBucode)
    print("username_creator", emptyUsernameCreator)
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


