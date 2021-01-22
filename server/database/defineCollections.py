import json
from fixSchema import *
from helpers.config import * 


def getKey(obj):
    s = ""
    for key in obj.keys():
        # print(key ," ", end = '')
        s += key + "_"
    return s


def isFingerprint(json_obj):
    if "buid" in json_obj.keys() and "rss" in json_obj.keys() and "MAC" in json_obj.keys() \
            and "geometry" in json_obj.keys():
        return True
    return False


# def isFingerprintStrongestWifi(json_obj):


def isFloorPlan(json_obj):
    # "bottom_left_lat" in json_obj.keys() and "bottom_left_lng" in json_obj.keys() and \
    # "top_right_lat" in json_obj.keys() and "top_right_lng" in json_obj.keys() and \
    if "buid" in json_obj.keys() and "description" in json_obj.keys() and "floor_name" in json_obj.keys() and \
            "floor_number" in json_obj.keys() and "geometry" not in json_obj.keys() and \
            "address" not in json_obj.keys():
        return True
    return False


def isPois(json_obj):
    if ("buid" in json_obj.keys() and "coordinates_lat" in json_obj.keys() and "coordinates_lon" in json_obj.keys() and
            "description" in json_obj.keys() and "floor_name" in json_obj.keys() and "floor_number" in json_obj.keys()
            and "geometry" in json_obj.keys() and "pois_type" in json_obj.keys() and "name" in json_obj.keys()
            and "puid" in json_obj.keys()):
        return True
    return False


def isBuilding(json_obj):
    if "buid" in json_obj.keys() and "address" in json_obj.keys() and "coordinates_lon" in json_obj.keys() and \
            "coordinates_lat" in json_obj.keys() and "description" in json_obj.keys() and "url" in json_obj.keys() and \
            "MyProperty" not in json_obj.keys():
        return True
    return False


def isUser(json_obj):
    if ("doc_type" in json_obj.keys() and "owner_id" in json_obj.keys() and "type" in json_obj.keys()) or \
            "doctype" in json_obj.keys():
        return True
    return False


def isCampus(json_obj):
    if "buids" in json_obj.keys() and "cuid" in json_obj.keys() and "description" in json_obj.keys() and \
            "name" in json_obj.keys() and "owner_id" in json_obj.keys() and "address" not in json_obj.keys():
        return True
    return False


def isEdge(json_obj):
    if "buid_a" in json_obj.keys() and "buid_b" in json_obj.keys() and "buid" in json_obj.keys() and \
            "cuid" in json_obj.keys() and "edge_type" in json_obj.keys() and "MyProperty" not in json_obj.keys():
        return True
    return False


def isFound(param, listOfDict, uniq_key):
    if len(listOfDict) == 0:
        listOfDict.insert(0, uniq_key)
        uniq_key.update({'Name': param})
    else:
        for x in listOfDict:
            if param in x.values():  # if this collection exist update it
                c1 = 0
                c2 = 0
                v1 = json.loads(json.dumps(x["Json"]))  # already in
                for k in v1.keys():
                    c1 += 1
                v2 = uniq_key["Json"]
                for k in v2.keys():  # about to put in
                    c2 += 1
                if c1 > c2:  # old one (v1) is bigger, must be replaced (with v2)
                    big = v1
                    small = v2
                    x["Json"] = v2
                else:  # old one is smaller
                    big = v2
                    small = v1
                for k in big.keys():
                    if k not in small and k not in x["Extras"]:
                        x["Extras"] = x["Extras"] + k + " "
                return listOfDict
        listOfDict.insert(0, uniq_key)
        uniq_key.update({'Name': param})
    return listOfDict


def defineCollections(file):
    count = 0
    buildings = 0
    floorplans = 0
    fingerprints = 0
    edges = 0
    users = 0
    campus = 0
    pois = 0
    und = 0
	collectionsPath = getCollectionsPath()
	os.mkdirs(collectionsPath)
    pathB = collectionsPath + "/buildings.json"
    pathC = collectionsPath + "/campus.json"
    pathE = collectionsPath + "/edges.json"
    pathFIN = collectionsPath + "/fingerprintswifi.json"
    pathFL = collectionsPath + "/floorplans.json"
    pathP = collectionsPath + "/pois.json"
    pathU = collectionsPath + "/users.json"
    pathUND = collectionsPath + "/undefined.json"
    b = open(pathB, "w")
    c = open(pathC, "w")
    e = open(pathE, "w")
    fin = open(pathFIN, "w")
    fl = open(pathFL, "w")
    p = open(pathP, "w")
    u = open(pathU, "w")
    und = open(pathUND, "w")
    known_keys = set()
    collections = []
    i = 0
    print("Reading from.. ", getDocumentsPath())
    while True:
        line = file.readline()
        if not line:
            break
        obj = json.loads(line)
        obj2 = json.dumps(obj, sort_keys=True)
        str = ""
        str = obj2
        obj3 = json.loads(str)
        count += 1
        obj_key = getKey(obj3)
        collection_info = {'Name': 'UNDEFINED', 'Json': obj3, 'Extras': ''}
        known_keys.add(obj_key)
        if isBuilding(obj):
            collections = isFound("Building", collections, collection_info)  # DONE
            fixed_obj = fixBUILDING(obj)
            b.write(json.dumps(fixed_obj))
            b.write("\n")
            buildings += 1
        elif isCampus(obj):
            collections = isFound("Campus", collections, collection_info)  # DONE
            fixed_obj = fixCAMPUS(obj)
            c.write(json.dumps(fixed_obj))
            c.write("\n")
            campus += 1
        elif isEdge(obj):
            collections = isFound("Edge", collections, collection_info)  # DONE
            fixed_obj = fixEDGES(obj)
            e.write(json.dumps(fixed_obj))
            e.write("\n")
            edges +=1 
        elif isFingerprint(obj):
            collections = isFound("Fingerprint", collections, collection_info)  # fingerprints
            fixed_obj = fixFINGERPRINT(obj)
            fin.write(json.dumps(fixed_obj))
            fin.write("\n")
            fingerprints += 1
        elif isFloorPlan(obj):
            collections = isFound("FloorPlan", collections, collection_info)  # DONE
            fixed_obj = fixFLOORPLAN(obj)
            fl.write(json.dumps(fixed_obj))
            fl.write("\n")
            floorplans += 1
        elif isPois(obj):
            collections = isFound("Pois", collections, collection_info)  # DONE
            fixed_obj = fixPOIS(obj)
            p.write(json.dumps(fixed_obj))
            p.write("\n")
            pois += 1
        elif isUser(obj):
            collections = isFound("User", collections, collection_info)  # DONE
            fixed_obj = fixUSER(obj)
            u.write(json.dumps(fixed_obj))
            u.write("\n")
            users += 1
        else:
            collections.insert(0, collection_info)
            und.write(json.dumps(obj))
            und.write("\n")
            und += 1
    b.close()
    c.close()
    e.close()
    fin.close()
    fl.close()
    p.close()
    u.close()
    print("Found:\n", buildings, "Buildinds\n", campus, "Campus\n", edges, "Edge\n", fingerprints, "Fingerprints\n",
          floorplans, "Floorplans\n", pois, "Pois\n", users, "Users\n", und, "Undefined")

#  MAIN
f = open(PATH, encoding="utf8")
defineCollections(f)  # printing unique json keys with extra fields representing similar json keys
f.close()
