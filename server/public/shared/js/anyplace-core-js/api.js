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

// TODO:NN once all done, and commited. rename ONLY this to API
var API = {};
API.old = "../anyplace";
API.url = "../api"

API.VERSION = API.url + "/version";

/**
 * MAPPING API
 */
API.Mapping = {};
API.Navigation = {};
API.Other = {};

API.Mapping.APs = "/wifi/access_points/floor";
API.Mapping.APs_URL = API.url + API.Mapping.APs;
API.Mapping.GET_APS_IDS = "/wifi/access_points/ids";
API.Mapping.GET_APS_IDS_URL = API.url + API.Mapping.GET_APS_IDS;

API.Mapping.FINGERPRINTS_DELETE = "/auth/radiomap/delete";
API.Mapping.FINGERPRINTS_DELETE_URL = API.url + API.Mapping.FINGERPRINTS_DELETE;
API.Mapping.FINGERPRINTS_DELETE_TIME = "/auth/radiomap/delete/time";
API.Mapping.FINGERPRINTS_DELETE_TIME_URL = API.url + API.Mapping.FINGERPRINTS_DELETE_TIME;
API.Mapping.FINGERPRINTS_TIME = "/radiomap/time";
API.Mapping.FINGERPRINTS_TIME_URL = API.url + API.Mapping.FINGERPRINTS_TIME;

API.Mapping.RADIO_HEATMAP_RSS_1 = "/heatmap/floor/average/1";
API.Mapping.RADIO_HEATMAP_RSS_URL_1 = API.url + API.Mapping.RADIO_HEATMAP_RSS_1;
API.Mapping.RADIO_HEATMAP_RSS_2 = "/heatmap/floor/average/2";
API.Mapping.RADIO_HEATMAP_RSS_URL_2 = API.url + API.Mapping.RADIO_HEATMAP_RSS_2;
API.Mapping.RADIO_HEATMAP_RSS_3 = "/heatmap/floor/average/3";
API.Mapping.RADIO_HEATMAP_RSS_URL_3 = API.url + API.Mapping.RADIO_HEATMAP_RSS_3;
API.Mapping.RADIO_HEATMAP_RSS_3_TILES = "/heatmap/floor/average/3/tiles";
API.Mapping.RADIO_HEATMAP_RSS_URL_3_TILES = API.url + API.Mapping.RADIO_HEATMAP_RSS_3_TILES;

API.Mapping.RADIO_HEATMAP_RSS_BY_TIME_1 = "/heatmap/floor/average/timestamp/1";
API.Mapping.RADIO_HEATMAP_RSS_BY_TIME_URL_1 = API.url + API.Mapping.RADIO_HEATMAP_RSS_BY_TIME_1;
API.Mapping.RADIO_HEATMAP_RSS_BY_TIME_2 = "/heatmap/floor/average/timestamp/2";
API.Mapping.RADIO_HEATMAP_RSS_BY_TIME_URL_2 = API.url + API.Mapping.RADIO_HEATMAP_RSS_BY_TIME_2;
API.Mapping.RADIO_HEATMAP_RSS_BY_TIME_3 = "/heatmap/floor/average/timestamp/3";
API.Mapping.RADIO_HEATMAP_RSS_BY_TIME_URL_3 = API.url + API.Mapping.RADIO_HEATMAP_RSS_BY_TIME_3;
API.Mapping.RADIO_HEATMAP_BY_TIME_TILES = "/heatmap/floor/average/timestamp/tiles";
API.Mapping.RADIO_HEATMAP_BY_TIME_TILES_URL = API.url + API.Mapping.RADIO_HEATMAP_BY_TIME_TILES;

API.Mapping.RADIOMAP_DELETE = "/position/radio/heatmap_building_floor_delete";
API.Mapping.RADIOMAP_DELETE_URL = API.old + API.Mapping.RADIOMAP_DELETE;
// AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_ACCES = "/position/radio/acces";
// AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_ACCES_URL = AnyplaceAPI.BASE_URL + AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_ACCES;
API.Mapping.RADIO_HEATMAP_POI = "/mapping/radio/radio_heatmap_bbox";
API.Mapping.RADIO_HEATMAP_URL_POI = API.old + API.Mapping.RADIO_HEATMAP_POI;

API.Mapping.RADIO_BY_BUILDING_FLOOR_ALL = "/radiomap/floor/all";
API.Mapping.RADIO_BY_BUILDING_FLOOR_ALL_URL = API.url + API.Mapping.RADIO_BY_BUILDING_FLOOR_ALL;
API.Mapping.RADIO_BY_BUILDING_FLOOR_ALL_TXT = "/position/radio_by_building_floor_all_text";
API.Mapping.RADIO_BY_BUILDING_FLOOR_ALL_TXT_URL = API.old + API.Mapping.RADIO_BY_BUILDING_FLOOR_ALL_TXT;

// TODO:NN replace BUILDING to SPACE everywhere here..
API.Mapping.BUILDING_ADD = "/auth/mapping/space/add";
API.Mapping.BUILDING_ADD_URL = API.url + API.Mapping.BUILDING_ADD;
API.Mapping.BUILDING_ONE = "/mapping/space/get";
API.Mapping.BUILDING_ONE_URL = API.url + API.Mapping.BUILDING_ONE;
API.Mapping.BUILDING_UPDATE = "/auth/mapping/space/update";
API.Mapping.BUILDING_UPDATE_URL = API.url + API.Mapping.BUILDING_UPDATE;
API.Mapping.BUILDING_DELETE = "/auth/mapping/space/delete";
API.Mapping.BUILDING_DELETE_URL = API.url + API.Mapping.BUILDING_DELETE;
API.Mapping.BUILDING_ALL = "/mapping/space/all";
API.Mapping.BUILDING_ALL_URL = API.url + API.Mapping.BUILDING_ALL;
API.Mapping.BUILDING_ALL_OWNER = "/auth/mapping/space/user";
API.Mapping.BUILDING_ALL_OWNER_URL = API.url + API.Mapping.BUILDING_ALL_OWNER;

API.Mapping.CAMPUS_ALL = "/auth/mapping/campus/user";
API.Mapping.CAMPUS_ALL_URL = API.url + API.Mapping.CAMPUS_ALL;
API.Mapping.CAMPUS_UPDATE = "/auth/mapping/campus/update";
API.Mapping.CAMPUS_UPDATE_URL = API.url + API.Mapping.CAMPUS_UPDATE;
API.Mapping.CAMPUS_DELETE = "/auth/mapping/campus/delete";
API.Mapping.CAMPUS_DELETE_URL = API.url + API.Mapping.CAMPUS_DELETE;
API.Mapping.BUILDINGSET_ADD = "/auth/mapping/campus/add";
API.Mapping.BUILDINGSET_ADD_URL = API.url + API.Mapping.BUILDINGSET_ADD;
API.Mapping.BUILDINGSET_ALL = "/mapping/campus/get";
API.Mapping.BUILDINGSET_ALL_URL = API.url + API.Mapping.BUILDINGSET_ALL;

API.Mapping.FLOOR_ADD = "/auth/mapping/floor/add";
API.Mapping.FLOOR_ADD_URL = API.url + API.Mapping.FLOOR_ADD;
API.Mapping.FLOOR_UPDATE = "/auth/mapping/floor/update";
API.Mapping.FLOOR_UPDATE_URL = API.url + API.Mapping.FLOOR_UPDATE;
API.Mapping.FLOOR_DELETE = "/auth/mapping/floor/delete";
API.Mapping.FLOOR_DELETE_URL = API.url + API.Mapping.FLOOR_DELETE;
API.Mapping.FLOOR_ALL = "/mapping/floor/all";
API.Mapping.FLOOR_ALL_URL = API.url + API.Mapping.FLOOR_ALL;
API.Mapping.FLOOR_PLAN_UPLOAD = "/mapping/floor/floorplan/upload";
API.Mapping.FLOOR_PLAN_UPLOAD_URL = API.url + API.Mapping.FLOOR_PLAN_UPLOAD;
API.Mapping.FLOOR_PLAN_DOWNLOAD = "/floorplans64/";
API.Mapping.FLOOR_PLAN_DOWNLOAD_URL = API.url + API.Mapping.FLOOR_PLAN_DOWNLOAD;
API.Mapping.FLOOR_PLAN_DOWNLOAD_ALL = "/floorplans64/all/";
API.Mapping.FLOOR_PLAN_DOWNLOAD_URL_ALL = API.url + API.Mapping.FLOOR_PLAN_DOWNLOAD_ALL;

API.Mapping.POIS_ADD = "/auth/mapping/pois/add";
API.Mapping.POIS_ADD_URL = API.url + API.Mapping.POIS_ADD;
API.Mapping.POIS_UPDATE = "/auth/mapping/pois/update";
API.Mapping.POIS_UPDATE_URL = API.url + API.Mapping.POIS_UPDATE;
API.Mapping.POIS_DELETE = "/auth/mapping/pois/delete";
API.Mapping.POIS_DELETE_URL = API.url + API.Mapping.POIS_DELETE;
API.Mapping.POIS_ALL_FLOOR = "/mapping/pois/floor/all";
API.Mapping.POIS_ALL_FLOOR_URL = API.url + API.Mapping.POIS_ALL_FLOOR;
API.Mapping.POIS_ALL_BUILDING = "/mapping/pois/space/all";
API.Mapping.POIS_ALL_BUILDING_URL = API.url + API.Mapping.POIS_ALL_BUILDING;
API.Mapping.ALL_POIS = "/mapping/pois/search";
API.Mapping.ALL_POIS_URL = API.url + API.Mapping.ALL_POIS;

API.Mapping.CONNECTION_ADD = "/auth/mapping/connection/add";
API.Mapping.CONNECTION_ADD_URL = API.url + API.Mapping.CONNECTION_ADD;
API.Mapping.CONNECTION_UPDATE = "/auth/mapping/connection/update";
API.Mapping.CONNECTION_UPDATE_URL = API.old + API.Mapping.CONNECTION_UPDATE;
API.Mapping.CONNECTION_DELETE = "/mapping/connection/delete";
API.Mapping.CONNECTION_DELETE_URL = API.url + API.Mapping.CONNECTION_DELETE;
API.Mapping.CONNECTION_ALL_FLOOR = "/mapping/connection/all_floor";
API.Mapping.CONNECTION_ALL_FLOOR_URL = API.url + API.Mapping.CONNECTION_ALL_FLOOR;

API.Mapping.LOGIN_GOOGLE = "/user/login/google";
API.Mapping.LOGIN_GOOGLE_URL = API.url + API.Mapping.LOGIN_GOOGLE;

API.Mapping.LOGIN_LOCAL = "/user/login";
API.Mapping.LOGIN_LOCAL_URL = API.url + API.Mapping.LOGIN_LOCAL;

API.Mapping.LOGIN_REFRESH_LOCAL = "/user/refresh";
API.Mapping.LOGIN_REFRESH_LOCAL_URL = API.url + API.Mapping.LOGIN_REFRESH_LOCAL;

API.Mapping.REGISTER_LOCAL = "/user/register";
API.Mapping.REGISTER_LOCAL_URL = API.url + "/user/register";

API.Navigation.POIS_ROUTE = "/navigation/route";
API.Navigation.POIS_ROUTE = API.url + API.Navigation.POIS_ROUTE;

API.Other.GOOGLE_URL_SHORTNER_URL = "https://www.googleapis.com/urlshortener/v1/url?key=AIzaSyDLSYNnIC93KfPnMYRL-7xI7yXjOhgulk8";

if (app == undefined) { LOG.F("api.js must be loaded after app.js in GruntFile)") }

app.factory('AnyplaceAPIService', ['$http', '$q', 'formDataObject', function ($http, $q, formDataObject) {

    $http.defaults.useXDomain = true;
    delete $http.defaults.headers.common['X-Requested-With'];

    var apiService = {};

    apiService.version = function (json_req) {
        return $http({
            method: "GET",
            url: API.VERSION,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.getRadioHeatmapRSS_1 = function (json_req) {
        LOG.D2("getRadioHeatmapRSS_1");
        return $http({
            method: "POST",
            url: API.Mapping.RADIO_HEATMAP_RSS_URL_1,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.getRadioHeatmapRSS_2 = function (json_req) {
        LOG.D2("getRadioHeatmapRSS_2");
        return $http({
            method: "POST",
            url: API.Mapping.RADIO_HEATMAP_RSS_URL_2,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.getRadioHeatmapRSS_3 = function (json_req) {
        LOG.D2("getRadioHeatmapRSS_3");
        return $http({
            method: "POST",
            url: API.Mapping.RADIO_HEATMAP_RSS_URL_3,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.getRadioHeatmapRSS_3_Tiles = function (json_req) {
        LOG.D2("getRadioHeatmapRSS_3_Tiles");
        return $http({
            method: "POST",
            url: API.Mapping.RADIO_HEATMAP_RSS_URL_3_TILES,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };


    apiService.getRadioHeatmapRSSByTime_1 = function (json_req) {
        LOG.D2("getRadioHeatmapRSSByTime_1");
        return $http({
            method: "POST",
            url: API.Mapping.RADIO_HEATMAP_RSS_BY_TIME_URL_1,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.getRadioHeatmapRSSByTime_2 = function (json_req) {
        LOG.D2("getRadioHeatmapRSSByTime_2");
        return $http({
            method: "POST",
            url: API.Mapping.RADIO_HEATMAP_RSS_BY_TIME_URL_2,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.getRadioHeatmapRSSByTime_3 = function (json_req) {
        LOG.D2("getRadioHeatmapRSSByTime_3");
        return $http({
            method: "POST",
            url: API.Mapping.RADIO_HEATMAP_RSS_BY_TIME_URL_3,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.getRadioHeatmapRSSByTime_Tiles = function (json_req) {
        LOG.D2("getRadioHeatmapRSSByTime_Tiles");
        return $http({
            method: "POST",
            url: API.Mapping.RADIO_HEATMAP_BY_TIME_TILES_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };


    apiService.getAPs = function (json_req) {
        return $http({
            method: "POST",
            url: API.Mapping.APs_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };


    apiService.getAPsIds = function (json_req) {
        return $http({
            method: "POST",
            url: API.Mapping.GET_APS_IDS_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };


    apiService.deleteFingerprints = function (json_req) {
        return $http({
            method: "POST",
            url: API.Mapping.FINGERPRINTS_DELETE_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };

    apiService.deleteFingerprintsByTime = function (json_req) {
        return $http({
            method: "POST",
            url: API.Mapping.FINGERPRINTS_DELETE_TIME_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.getFingerprintsTime = function (json_req) {
        return $http({
            method: "POST",
            url: API.Mapping.FINGERPRINTS_TIME_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };

    apiService.getHeatmapAcces = function (json_req) {
        return null;
        // DEPRECATED
        // return $http({
        //     method: "POST",
        //     url: AnyplaceAPI.Mapping.RADIO_HEATMAP_RSS_ACCES_URL,
        //     data: json_req
        // }).success(function (data, status) {
        //     return data;
        // }).error(function (data, status) {
        //     return data;
        // });

    };

    apiService.retrievePoisByBuilding = function (json_req) {
        return $http({
            method: "POST",
            url: API.Mapping.POIS_ALL_BUILDING_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.getRadioHeatmapPoi = function (json_req) {
        return $http({
            method: "POST",
            url: API.Mapping.RADIO_HEATMAP_URL_POI,
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
            url: API.Mapping.RADIO_BY_BUILDING_FLOOR_ALL_URL,
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
            url: API.Mapping.RADIO_BY_BUILDING_FLOOR_ALL_TXT_URL,
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
        return $http({
            method: "POST",
            url: API.Mapping.BUILDING_ADD_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.addBuildingSet = function (json_req) {
        return $http({
            method: "POST",
            url: API.Mapping.BUILDINGSET_ADD_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.updateBuilding = function (jsonReq) {
        return $http({
            method: "POST",
            url: API.Mapping.BUILDING_UPDATE_URL,
            data: jsonReq
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };

    apiService.updateCampus = function (json_req) {
        return $http({
            method: "POST",
            url: API.Mapping.CAMPUS_UPDATE_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };

    apiService.deleteBuilding = function (json_req) {
        return $http({
            method: "POST",
            url: API.Mapping.BUILDING_DELETE_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };

    // lsolea01
    apiService.deleteRadiomaps = function (json_req) {
        return $http({
            method: "POST",
            url: API.Mapping.RADIOMAP_DELETE_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };

    apiService.deleteCampus = function (json_req) {
        return $http({
            method: "POST",
            url: API.Mapping.CAMPUS_DELETE_URL,
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
            url: API.Mapping.BUILDING_ALL_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };

    apiService.allOwnerBuildings = function (json_req) {
        return $http({
            method: "POST",
            url: API.Mapping.BUILDING_ALL_OWNER_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };

    apiService.allCucodeCampus = function (json_req) {
        return $http({
            method: "POST",
            url: API.Mapping.BUILDINGSET_ALL_URL,
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
            url: API.Mapping.CAMPUS_ALL_URL,
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
        return $http({
            method: "POST",
            url: API.Mapping.FLOOR_ADD_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.deleteFloor = function (json_req) {
        return $http({
            method: "POST",
            url: API.Mapping.FLOOR_DELETE_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };

    apiService.allBuildingFloors = function (json_req) {
        return $http({
            method: "POST",
            url: API.Mapping.FLOOR_ALL_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };


    apiService.uploadFloorPlan = function (json_req, file) {
        return $http({
            method: "POST",
            url: API.Mapping.FLOOR_PLAN_UPLOAD_URL,
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
        return $http.post(API.Mapping.FLOOR_PLAN_UPLOAD_URL, formData, {
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
        return $http({
            method: "POST",
            url: API.Mapping.FLOOR_PLAN_DOWNLOAD_URL + buid + "/" + floor_number,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.downloadFloorPlanAll = function (json_req, buid, floor_number) {
        return $http({
            method: "POST",
            url: API.Mapping.FLOOR_PLAN_DOWNLOAD_URL_ALL + buid + "/" + floor_number,
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
        return $http({
            method: "POST",
            url: API.Mapping.POIS_ADD_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.updatePois = function (json_req) {
        return $http({
            method: "POST",
            url: API.Mapping.POIS_UPDATE_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.deletePois = function (json_req) {
        //var deferred = $q.defer(); // thiz can be used instead of returning the $http

        return $http({
            method: "POST",
            url: API.Mapping.POIS_DELETE_URL,
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
            url: API.Mapping.POIS_ALL_FLOOR_URL,
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
        return $http({
            method: "POST",
            url: API.Mapping.CONNECTION_ADD_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.deleteConnection = function (json_req) {
        return $http({
            method: "POST",
            url: API.Mapping.CONNECTION_DELETE_URL,
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
            url: API.Mapping.CONNECTION_ALL_FLOOR_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.loginGoogle = function (json_req) {
        LOG.D4("loginGoogle")
        LOG.D4(json_req)
        return $http({
            method: "POST",
            url: API.Mapping.LOGIN_GOOGLE_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.refreshLocalAccount = function (json_req) {
        return $http({
            method: "POST",
            url: API.Mapping.LOGIN_REFRESH_LOCAL_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.loginLocalAccount = function (json_req) {
        return $http({
            method: "POST",
            url: API.Mapping.LOGIN_LOCAL_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.registerLocalAccount = function (json_req) {
        LOG.D2("api.js");
        return $http({
            method: "POST",
            url: API.Mapping.REGISTER_LOCAL_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.getOneBuilding = function (json_req) {
        return $http({
            method: "POST",
            url: API.Mapping.BUILDING_ONE_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

    };


    apiService.retrieveALLPois = function (json_req) {
        var a = $http({
            method: "POST",
            url: API.Mapping.ALL_POIS_URL,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });

        return a;
    }

    apiService.retrieveRouteFromPoiToPoi = function (json_req) {
        return $http({
            method: "POST",
            url: API.Navigation.POIS_ROUTE,
            data: json_req
        }).success(function (data, status) {
            return data;
        }).error(function (data, status) {
            return data;
        });
    };

    apiService.googleUrlShortener = function (json_req) {
        return $http({
            method: "POST",
            url: API.Other.GOOGLE_URL_SHORTNER_URL,
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
