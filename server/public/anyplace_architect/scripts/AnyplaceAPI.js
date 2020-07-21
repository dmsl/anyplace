/**
 *
 The MIT License (MIT)

 Copyright (c) 2015, Kyriakos Georgiou, Data Management Systems Laboratory (DMSL)
 Department of Computer Science, University of Cyprus, Nicosia, CYPRUS,
 dmsl@cs.ucy.ac.cy, http://dmsl.cs.ucy.ac.cy/

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in
 all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 THE SOFTWARE.
 */

var AnyplaceAPI = {};
AnyplaceAPI.FULL_SERVER ="../anyplace";
AnyplaceAPI.VERSION = AnyplaceAPI.FULL_SERVER + "/version";

/**
 * MAPPING API
 */
AnyplaceAPI.Mapping = {};
AnyplaceAPI.Mapping.RADIO_HEATMAP = "/mapping/radio/heatmap_building_floor";
AnyplaceAPI.Mapping.RADIO_HEATMAP_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.RADIO_HEATMAP;

AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS = "/mapping/radio/heatmap_building_floor_avarage";
AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS;

AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_1 = "/position/radio/heatmap_building_floor_average_1";
AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_URL_1 = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_1;

AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_2 = "/position/radio/heatmap_building_floor_average_2";
AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_URL_2 = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_2;

AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_3 = "/position/radio/heatmap_building_floor_average_3";
AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_URL_3 = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_3;

AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_3_TILES = "/position/radio/heatmap_building_floor_average_3_tiles";
AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_URL_3_TILES = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_3_TILES;


AnyplaceAPI.Mapping.APs = "/position/radio/APs_building_floor";
AnyplaceAPI.Mapping.APs_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.APs;

AnyplaceAPI.Mapping.FINGERPRINTS_DELETE = "/position/radio/delete";
AnyplaceAPI.Mapping.FINGERPRINTS_DELETE_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.FINGERPRINTS_DELETE;

AnyplaceAPI.Mapping.FINGERPRINTS_DELETE_TIME = "/position/radio/delete/time";
AnyplaceAPI.Mapping.FINGERPRINTS_DELETE_TIME_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.FINGERPRINTS_DELETE_TIME;

AnyplaceAPI.Mapping.GET_APS_IDS = "/position/radio/aps_ids";
AnyplaceAPI.Mapping.GET_APS_IDS_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.GET_APS_IDS;

AnyplaceAPI.Mapping.FINGERPRINTS_TIME = "/position/radio/time";
AnyplaceAPI.Mapping.FINGERPRINTS_TIME_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.FINGERPRINTS_TIME;

AnyplaceAPI.Mapping.RADIO_HEATMAP_BY_TIME = "/position/radio/heatmap_building_floor_timestamp";
AnyplaceAPI.Mapping.RADIO_HEATMAP_BY_TIME_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.RADIO_HEATMAP_BY_TIME;


AnyplaceAPI.Mapping.RADIO_HEATMAP_BY_TIME_TILES = "/position/radio/heatmap_building_floor_timestamp_tiles";
AnyplaceAPI.Mapping.RADIO_HEATMAP_BY_TIME_TILES_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.RADIO_HEATMAP_BY_TIME_TILES;


AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_BY_TIME_1 = "/position/radio/heatmap_building_floor_timestamp_average_1";
AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_BY_TIME_URL_1 = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_BY_TIME_1;


AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_BY_TIME_2 = "/position/radio/heatmap_building_floor_timestamp_average_2";
AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_BY_TIME_URL_2 = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_BY_TIME_2;


AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_BY_TIME_3 = "/position/radio/heatmap_building_floor_timestamp_average_3";
AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_BY_TIME_URL_3 = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_BY_TIME_3;

AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_ACCES = "/position/radio/acces";
AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_ACCES_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_ACCES;

AnyplaceAPI.Mapping.RADIO_HEATMAP_POI = "/mapping/radio/radio_heatmap_bbox";
AnyplaceAPI.Mapping.RADIO_HEATMAP_URL_POI = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.RADIO_HEATMAP_POI;

AnyplaceAPI.Mapping.RADIO_BY_BUILDING_FLOOR_ALL = "/position/radio_by_building_floor_all";
AnyplaceAPI.Mapping.RADIO_BY_BUILDING_FLOOR_ALL_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.RADIO_BY_BUILDING_FLOOR_ALL;

AnyplaceAPI.Mapping.RADIO_BY_BUILDING_FLOOR_ALL_TXT = "/position/radio_by_building_floor_all_text";
AnyplaceAPI.Mapping.RADIO_BY_BUILDING_FLOOR_ALL_TXT_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.RADIO_BY_BUILDING_FLOOR_ALL_TXT;

AnyplaceAPI.Mapping.BUILDING_ADD = "/mapping/building/add";
AnyplaceAPI.Mapping.BUILDING_ADD_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.BUILDING_ADD;

AnyplaceAPI.Mapping.CAMPUS_ALL = "/mapping/campus/all_owner";
AnyplaceAPI.Mapping.CAMPUS_ALL_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.CAMPUS_ALL;

AnyplaceAPI.Mapping.CAMPUS_UPDATE = "/mapping/campus/update";
AnyplaceAPI.Mapping.CAMPUS_UPDATE_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.CAMPUS_UPDATE;

AnyplaceAPI.Mapping.CAMPUS_DELETE = "/mapping/campus/delete";
AnyplaceAPI.Mapping.CAMPUS_DELETE_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.CAMPUS_DELETE;

AnyplaceAPI.Mapping.BUILDINGSET_ADD = "/mapping/campus/add";
AnyplaceAPI.Mapping.BUILDINGSET_ADD_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.BUILDINGSET_ADD;

AnyplaceAPI.Mapping.POISCATEGORY_ADD = "/mapping/pois/add_category";
AnyplaceAPI.Mapping.POISCATEGORY_ADD_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.POISCATEGORY_ADD;


AnyplaceAPI.Mapping.BUILDING_UPDATE = "/mapping/building/update";
AnyplaceAPI.Mapping.BUILDING_UPDATE_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.BUILDING_UPDATE;

AnyplaceAPI.Mapping.BUILDING_DELETE = "/mapping/building/delete";
AnyplaceAPI.Mapping.BUILDING_DELETE_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.BUILDING_DELETE;

AnyplaceAPI.Mapping.RADIOMAP_DELETE = "/position/radio/heatmap_building_floor_delete";
AnyplaceAPI.Mapping.RADIOMAP_DELETE_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.RADIOMAP_DELETE;

AnyplaceAPI.Mapping.BUILDING_ALL = "/mapping/building/all_owner";
AnyplaceAPI.Mapping.BUILDING_ALL_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.BUILDING_ALL;

AnyplaceAPI.Mapping.FLOOR_ADD = "/mapping/floor/add";
AnyplaceAPI.Mapping.FLOOR_ADD_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.FLOOR_ADD;
AnyplaceAPI.Mapping.FLOOR_UPDATE = "/mapping/floor/update";
AnyplaceAPI.Mapping.FLOOR_UPDATE_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.FLOOR_UPDATE;
AnyplaceAPI.Mapping.FLOOR_DELETE = "/mapping/floor/delete";
AnyplaceAPI.Mapping.FLOOR_DELETE_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.FLOOR_DELETE;
AnyplaceAPI.Mapping.FLOOR_ALL = "/mapping/floor/all";
AnyplaceAPI.Mapping.FLOOR_ALL_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.FLOOR_ALL;
AnyplaceAPI.Mapping.FLOOR_PLAN_UPLOAD = "/mapping/floor/uploadWithZoom";
AnyplaceAPI.Mapping.FLOOR_PLAN_UPLOAD_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.FLOOR_PLAN_UPLOAD;
AnyplaceAPI.Mapping.FLOOR_PLAN_DOWNLOAD = "/floorplans64/";
AnyplaceAPI.Mapping.FLOOR_PLAN_DOWNLOAD_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.FLOOR_PLAN_DOWNLOAD;
AnyplaceAPI.Mapping.FLOOR_PLAN_DOWNLOAD_ALL = "/floorplans64all/";
AnyplaceAPI.Mapping.FLOOR_PLAN_DOWNLOAD_URL_ALL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.FLOOR_PLAN_DOWNLOAD_ALL;

AnyplaceAPI.Mapping.POIS_ADD = "/mapping/pois/add";
AnyplaceAPI.Mapping.POIS_ADD_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.POIS_ADD;
AnyplaceAPI.Mapping.POIS_UPDATE = "/mapping/pois/update";
AnyplaceAPI.Mapping.POIS_UPDATE_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.POIS_UPDATE;
AnyplaceAPI.Mapping.POIS_DELETE = "/mapping/pois/delete";
AnyplaceAPI.Mapping.POIS_DELETE_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.POIS_DELETE;
AnyplaceAPI.Mapping.POIS_ALL_FLOOR = "/mapping/pois/all_floor";
AnyplaceAPI.Mapping.POIS_ALL_FLOOR_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.POIS_ALL_FLOOR;
AnyplaceAPI.Mapping.POIS_ALL_BUILDING = "/mapping/pois/all_building";
AnyplaceAPI.Mapping.POIS_ALL_BUILDING_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.POIS_ALL_BUILDING;

//POIS_TYPES_URL
AnyplaceAPI.Mapping.POIS_TYPES = "/mapping/pois/types";
AnyplaceAPI.Mapping.POIS_TYPES_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.POIS_TYPES;

AnyplaceAPI.Mapping.CONNECTION_ADD = "/mapping/connection/add";
AnyplaceAPI.Mapping.CONNECTION_ADD_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.CONNECTION_ADD;
AnyplaceAPI.Mapping.CONNECTION_UPDATE = "/mapping/connection/update";
AnyplaceAPI.Mapping.CONNECTION_UPDATE_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.CONNECTION_UPDATE;
AnyplaceAPI.Mapping.CONNECTION_DELETE = "/mapping/connection/delete";
AnyplaceAPI.Mapping.CONNECTION_DELETE_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.CONNECTION_DELETE;
AnyplaceAPI.Mapping.CONNECTION_ALL_FLOOR = "/mapping/connection/all_floor";
AnyplaceAPI.Mapping.CONNECTION_ALL_FLOOR_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.CONNECTION_ALL_FLOOR;

AnyplaceAPI.Mapping.SIGN = "/mapping/accounts/sign";
AnyplaceAPI.Mapping.SIGN_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.SIGN;


app.factory('AnyplaceAPIService', ['$http', '$q', 'formDataObject', function ($http, $q, formDataObject) {

    $http.defaults.useXDomain = true;
    delete $http.defaults.headers.common['X-Requested-With'];

    var apiService = {};

    apiService.version = function (json_req) {
        return $http({
            method: "POST",
            url: AnyplaceAPI.VERSION,
            data: json_req
        }).
        success(function (data, status) {
            return data;
        }).
        error(function (data, status) {
            return data;
        });

    };

    apiService.getRadioHeatmap = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.RADIO_HEATMAP_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };


    apiService.getRadioHeatmapRSS = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.getRadioHeatmapRSS_1 = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_URL_1,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.getRadioHeatmapRSS_2 = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_URL_2,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.getRadioHeatmapRSS_3 = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_URL_3,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.getRadioHeatmapRSS_3_Tiles = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_URL_3_TILES,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };


    apiService.getRadioHeatmapRSSByTime_1 = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_BY_TIME_URL_1,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.getRadioHeatmapRSSByTime_2 = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_BY_TIME_URL_2,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.getRadioHeatmapRSSByTime_3 = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_BY_TIME_URL_3,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.getRadioHeatmapRSSByTime = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.RADIO_HEATMAP_BY_TIME_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.getRadioHeatmapRSSByTime_Tiles = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.RADIO_HEATMAP_BY_TIME_TILES_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };


    apiService.getAPs = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.APs_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };


    apiService.getAPsIds = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.GET_APS_IDS_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };


    apiService.deleteFingerprints = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.FINGERPRINTS_DELETE_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };

    apiService.deleteFingerprintsByTime = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.FINGERPRINTS_DELETE_TIME_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };

    apiService.getFingerprintsTime = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.FINGERPRINTS_TIME_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };

    apiService.getHeatmapAcces = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_ACCES_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };



//retrievePoisTypes

    apiService.retrievePoisTypes = function (json_req) {

        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.POIS_TYPES_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.retrievePoisByBuilding = function (json_req) {
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.POIS_ALL_BUILDING_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.getRadioHeatmapPoi = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.RADIO_HEATMAP_URL_POI,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.getRadioByBuildingFloorAll = function (json_req) {
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.RADIO_BY_BUILDING_FLOOR_ALL_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.getRadioByBuildingFloorTxt = function (json_req) {
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.RADIO_BY_BUILDING_FLOOR_ALL_TXT_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    /**************************************************
     * BUILDING FUNCTIONS
     */
    apiService.addBuilding = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.BUILDING_ADD_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.addBuildingSet = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.BUILDINGSET_ADD_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.addCategory = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.POISCATEGORY_ADD_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.updateBuilding = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.BUILDING_UPDATE_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };

    apiService.updateCampus = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.CAMPUS_UPDATE_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };

    apiService.deleteBuilding = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.BUILDING_DELETE_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };

    // lsolea01
    apiService.deleteRadiomaps = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.RADIOMAP_DELETE_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };

    apiService.deleteCampus = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.CAMPUS_DELETE_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };

    apiService.allBuildings = function (json_req) {
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.BUILDING_ALL_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };

    apiService.allCampus = function (json_req) {
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.CAMPUS_ALL_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };


    /****************************************************
     * FLOOR FUNCTIONS
     */

    apiService.addFloor = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.FLOOR_ADD_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.updateFloor = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.FLOOR_UPDATE_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.deleteFloor = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.FLOOR_DELETE_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };

    apiService.allBuildingFloors = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.FLOOR_ALL_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };


    apiService.uploadFloorPlan = function (json_req, file) {
        //alert("make the request: " + json_req);
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.FLOOR_PLAN_UPLOAD_URL,
            headers: {
                'Content-Type': 'multipart/form-data'
            },
            data: {
                floorplan: file,
                json: json_req
            },
            transformRequest: formDataObject
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.uploadFloorPlan64 = function (json_req, file) {
        //alert( "make the request: " + json_req );
        var fl_data = file.replace('data:image/png;base64,', '');
        var uarray = LPUtils.Base64Binary.decode(fl_data);
        var blob = new Blob([uarray]);
        fl_data = "";
        for (var i = 0; i < uarray.length; i++) {
            fl_data += uarray[i];
        }

        var formData = new FormData();
        formData.append("json", json_req);
        formData.append("floorplan", blob);

        return $http.post(AnyplaceAPI.Mapping.FLOOR_PLAN_UPLOAD_URL, formData, {
            transformRequest: angular.identity,
            headers: {
                'Content-Type': undefined
            }
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.downloadFloorPlan = function (json_req, buid, floor_number) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.FLOOR_PLAN_DOWNLOAD_URL + buid + "/" + floor_number,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.downloadFloorPlanAll = function (json_req, buid, floor_number) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.FLOOR_PLAN_DOWNLOAD_URL_ALL + buid + "/" + floor_number,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };


    /******************************************************
     * POIS FUNCTIONS
     */
    apiService.addPois = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.POIS_ADD_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.updatePois = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.POIS_UPDATE_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.deletePois = function (json_req) {
        //alert( "make the request: " + json_req );
        //var deferred = $q.defer(); // thiz can be used instead of returning the $http

        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.POIS_DELETE_URL,
            data: json_req
        }).success(function (data, status) {
            //deferred.resolve(data);
            return data;
        }).error(function (data, status) {
            //deferred.resolve(data);
            return data;
        });
        //return deferred.promise;
    };

    apiService.retrievePoisByBuildingFloor = function (json_req) {
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.POIS_ALL_FLOOR_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    }


    /****************************************************
     * CONNECTION FUNCTIONS
     */
    apiService.addConnection = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.CONNECTION_ADD_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.updateConnection = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.CONNECTION_UPDATE_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.deleteConnection = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.CONNECTION_DELETE_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.retrieveConnectionsByBuildingFloor = function (json_req) {
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.CONNECTION_ALL_FLOOR_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.signAccount = function (json_req) {
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.SIGN_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };



    // we return apiService controller in order to be able to use it in ng-click
    return apiService;
}]);
