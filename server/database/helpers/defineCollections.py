import json
from helpers.fixSchema import *
from helpers.config import * 
from pathlib import Path
import os
import shutil

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

def splitToBuid(json_obj, fingPath):
    filepath = fingPath + "/" + json_obj["buid"]
    file = open(filepath, 'a')
    file.write(json.dumps(json_obj))
    file.write("\n")
    file.close()


def defineCollections(file):
    count = 0
    buildings = 0
    floorplans = 0
    fingerprints = 0
    edges = 0
    users = 0
    campus = 0
    pois = 0
    undefined = 0
    collectionsPath = getCollectionsPath()
    Path(collectionsPath).mkdir(parents=True, exist_ok=True)
    pathB = collectionsPath + "/buildings.json"
    pathC = collectionsPath + "/campuses.json"
    pathE = collectionsPath + "/edges.json"
    pathFL = collectionsPath + "/floorplans.json"
    pathP = collectionsPath + "/pois.json"
    pathU = collectionsPath + "/users.json"
    pathUND = collectionsPath + "/undefined.json"
    fingPath = collectionsPath + "/fingerprintsWifi"
	#  create or clear fingerprints path if exists 
    if not os.path.exists(fingPath):  
        os.makedirs(fingPath)
    else:
        shutil.rmtree(fingPath)
        os.makedirs(fingPath)
    b = open(pathB, "w")
    c = open(pathC, "w")
    e = open(pathE, "w")
    fl = open(pathFL, "w")
    p = open(pathP, "w")
    u = open(pathU, "w")
    und = open(pathUND, "w")
    i = 0
    keepIt = True
    while True:
        line = file.readline()
        if not line:
            break
        obj = json.loads(line)
        count += 1
        if isBuilding(obj) and keepIt:
            fixed_obj = fixBUILDING(obj)
            b.write(json.dumps(fixed_obj))
            b.write("\n")
            buildings += 1
        elif isCampus(obj) and keepIt:
            fixed_obj = fixCAMPUS(obj)
            c.write(json.dumps(fixed_obj))
            c.write("\n")
            campus += 1
        elif isEdge(obj) and keepIt:
            fixed_obj = fixEDGES(obj)
            e.write(json.dumps(fixed_obj))
            e.write("\n")
            edges +=1 
        elif isFingerprint(obj) and keepIt:
            fixed_obj = fixFINGERPRINT(obj)
            splitToBuid(fixed_obj, fingPath)
            fingerprints += 1
        elif isFloorPlan(obj):
            fixed_obj = fixFLOORPLAN(obj)
            fl.write(json.dumps(fixed_obj))
            fl.write("\n")
            floorplans += 1
        elif isPois(obj) and keepIt:
            fixed_obj = fixPOIS(obj)
            p.write(json.dumps(fixed_obj))
            p.write("\n")
            pois += 1
        elif isUser(obj) and keepIt:
            fixed_obj = fixUSER(obj)
            u.write(json.dumps(fixed_obj))
            u.write("\n")
            users += 1
        else:
            und.write(json.dumps(obj))
            und.write("\n")
            undefined += 1
    b.close()
    c.close()
    e.close()
    fl.close()
    p.close()
    u.close()
    print("OBJECT REPORT:\n", "Buildinds: ", buildings, 
		"\nCampus:", campus, "\nEdge: ", edges, "\nFingerprints: ", fingerprints,
        "\nFloorplans: ", floorplans, "\nPois: ", pois, "\nUsers: ", users, "\nUndefined: ", undefined)

