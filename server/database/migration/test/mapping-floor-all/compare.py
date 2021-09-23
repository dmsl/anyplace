import json
import sys


def defineKey(key):
    if key == "buildings":
        return 'buid'
    if key == "floorplans":
        return 'fuid'


def compareObjs(couchObjects, mongoObjects, type):
    keys = ["buid", "is_published", "floor_name", "floor_number", "bottom_left_lat",
            "bottom_left_lng", "top_right_lat", "username_creator", "description", "top_right_lng",
            "width", "height", "zoom"]
    possibleEmpty = ["username_creator", "description"]
    isSame = 1
    emptyFields = 0
    emptyUsernameCreator = 0
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
            if jsCouch["floor_name"] == jsMongo["floor_name"]:
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
                            if key == "description":
                                emptyDescription += 1
                            elif key == "username_creator":
                                emptyUsernameCreator +=1
    print("There are", emptyFields, "- or \"\" or null fields")
    print("description", emptyDescription)
    print("username_creator", emptyUsernameCreator)
    return isSame


def parseEndpoint(file):
    try:
        file = open(file, encoding="utf8")
    except:
        print("Path was not correct.")
        exit()
    jsonKey = "floors"
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


