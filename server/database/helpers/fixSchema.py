from helpers.config import *


def fixBUILDING(obj):
    fixed = obj
    updateSchema(fixed)
    #fixLocation(fixed)
    #  fixBooleans(fixed)
    fixDashesOrNulls(fixed)
    return fixed


def fixCAMPUS(obj):
    fixed = obj
    updateSchema(fixed)
    fixDashesOrNulls(fixed)
    return fixed


def fixEDGES(obj):
    fixed = obj
    updateSchema(fixed)
    #  fixBooleans(fixed)
    # if "weight" in obj.keys():
    #     if obj["weight"] is None:
    #         del obj["weight"]
    #     else:
    #         obj["weight"] = float(obj["weight"])
    # fixFloorNumber(fixed)
    return fixed


def fixFINGERPRINT(obj):
    fixed = obj
    updateSchema(fixed)
    #  fixed['timestamp'] = int(fixed['timestamp'])
    #  fixed['heading'] = float(fixed['heading'])
    #  fixed['rss'] = int(fixed['rss'])
    #fixLocation(fixed)
    # fixFloorNumber(fixed)
    return fixed


def fixFLOORPLAN(obj):
    fixed = obj
    updateSchema(fixed)
    #  fixBooleans(fixed)
    fixDashesOrNulls(fixed)
    # fixFloorNumber(fixed)
    #  if "zoom" in obj.keys():
    #       obj["zoom"] = int(obj["zoom"])
    #fixRectangle(fixed)
    return fixed


def fixPOIS(obj):
    fixed = obj
    updateSchema(fixed)
    #fixLocation(fixed)
    #  fixBooleans(fixed)
    fixDashesOrNulls(fixed)
    # fixFloorNumber(fixed)
    return fixed


def fixUSER(obj):
    fixed = obj
    updateSchema(fixed)
    if "doc_type" in obj.keys():
        del obj["doc_type"]
    #if "owner_id" in obj.keys():
    #    obj["id"] = obj["owner_id"]
    #    del obj["owner_id"]
    if "type" in obj.keys():
        obj["external"] = obj["type"]
        del obj["type"]
    if "owner_id" in obj.keys():
        if obj["owner_id"] in ADMINS:
            obj["type"] = "admin"
        else:
            obj["type"] = "user"
    else:
        obj["type"] = "user"
    return fixed


def fixRectangle(obj):
    if "bottom_left_lat" in obj.keys() and "bottom_left_lng" in obj.keys() and "top_right_lat" in obj.keys() and \
            "top_right_lng" in obj.keys():
        x1 = obj["bottom_left_lng"]
        y1 = obj["bottom_left_lat"]
        x2 = obj["top_right_lng"]
        y2 = obj["top_right_lat"]
        obj['location'] = {"coordinates": [[[float(x1), float(y1)], [float(x2), float(y1)], [float(x2), float(y2)], [float(x1), float(y2)]]], "type": "Polygon"}

        # obj["location_bottom_left"] = {"coordinates": [x1, y1], "type": "Point"}
        # obj["location_top_right"] = {"coordinates": [x2, y2], "type": "Point"}

        #del obj["bottom_left_lng"]
        #del obj["bottom_left_lat"]
        #del obj["top_right_lng"]
        #del obj["top_right_lat"]


def fixBooleans(obj):
    for key in obj.keys():
        if obj[key] == "True" or obj[key] == "true" or obj[key] == "TRUE":
            obj[key] = True
        if obj[key] == "False" or obj[key] == "false" or obj[key] == "FALSE":
            obj[key] = False


def fixDashesOrNulls(obj):
    listToDel = []
    for key in obj.keys():
        if key == "address" or key == "url" or key == "description" or key == "name" or key == "bucode" or key == "username_creator":
            if obj[key] == "-" or obj[key] == "":
                listToDel.insert(0, key)
    for x in listToDel:
        del obj[x]
    pass


def fixFloorNumber(obj):
    if "floor_number" in obj.keys():
        obj["floor_number"] = int(obj["floor_number"])
    if "floor" in obj.keys():
        obj["floor"] = int(obj["floor"])
    if "floor_a" in obj.keys() and "floor_b" in obj.keys():
        obj["floor_a"] = int(obj["floor_a"])
        obj["floor_b"] = int(obj["floor_b"])


def fixLocation(obj):
    if "x" in obj:
        del obj['x']
    if "y" in obj:
        del obj['y']
    if "coordinates_lat" in obj:
        del obj['coordinates_lat']
    if "coordinates_lon" in obj:
        del obj['coordinates_lon']
    if "geometry" in obj.keys():
        obj['location'] = obj['geometry']
        del obj['geometry']


def updateSchema(obj):
    obj['_schema'] = 0
