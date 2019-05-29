/**
 *
 The MIT License (MIT)

 Copyright (c) 2015, Kyriakos Georgiou, Marileni Angelidou, Data Management Systems Laboratory (DMSL)
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

var app = angular.module('anyArchitect', ['ngCookies', 'angularjs-dropdown-multiselect', 'ui.bootstrap', 'ui.select', 'ngSanitize']);

app.service('GMapService', function () {

    this.gmap = {};

    var self = this;

    var element = document.getElementById("map-canvas");

    /**
     * @constructor
     * @implements {google.maps.MapType}
     */
    function CoordMapType(tileSize) {
        this.tileSize = tileSize;
    }

    CoordMapType.prototype.maxZoom = 22;
    CoordMapType.prototype.name = 'Tile #s';
    CoordMapType.prototype.alt = 'Tile Coordinate Map Type';

    CoordMapType.prototype.getTile = function (coord, zoom, ownerDocument) {
        var div = ownerDocument.createElement('div');
        div.innerHTML = coord;
        div.style.width = this.tileSize.width + 'px';
        div.style.height = this.tileSize.height + 'px';
        div.style.fontSize = '10';
        div.style.borderStyle = 'solid';
        div.style.borderWidth = '1px';
        div.style.borderColor = '#AAAAAA';
        div.style.backgroundColor = '#E5E3DF';
        return div;
    };

    /**
     * @constructor
     * @implements {google.maps.MapType}
     */
    function OSMMapType(tileSize) {
        this.tileSize = tileSize;
    }

    OSMMapType.prototype.maxZoom = 22;
    OSMMapType.prototype.name = 'OSM';
    OSMMapType.prototype.alt = 'Tile OSM Map Type';
    OSMMapType.prototype.getTile = function (coord, zoom, ownerDocument) {
        if (zoom > 19)
            return null;
        var tilesPerGlobe = 1 << zoom;
        var x = coord.x % tilesPerGlobe;
        if (x < 0) {
            x = tilesPerGlobe + x;
        }
        var tile = ownerDocument.createElement('img');
        // Wrap y (latitude) in a like manner if you want to enable vertical infinite scroll
        tile.src = "http://tile.openstreetmap.org/" + zoom + "/" + x + "/" + coord.y + ".png";
        ;
        tile.style.width = this.tileSize.width + 'px';
        tile.style.height = this.tileSize.height + 'px';
        return tile;
    };

    /**
     * @constructor
     * @implements {google.maps.MapType}
     */
    function CartoLightMapType(tileSize) {
        this.tileSize = tileSize;
    }

    CartoLightMapType.prototype.maxZoom = 22;
    CartoLightMapType.prototype.name = 'Carto Light';
    CartoLightMapType.prototype.alt = 'Tile Carto Light Map Type';
    CartoLightMapType.prototype.getTile = function (coord, zoom, ownerDocument) {
        var url = "https://cartodb-basemaps-a.global.ssl.fastly.net/light_all/{z}/{x}/{y}.png";

        url = url.replace('{x}', coord.x)
            .replace('{y}', coord.y)
            .replace('{z}', zoom);
        var tile = ownerDocument.createElement('img');
        // Wrap y (latitude) in a like manner if you want to enable vertical infinite scroll
        tile.src = url;
        tile.style.width = this.tileSize.width + 'px';
        tile.style.height = this.tileSize.height + 'px';
        return tile;
    };

    /**
     * @constructor
     * @implements {google.maps.MapType}
     */
    function CartoDarkMapType(tileSize) {
        this.tileSize = tileSize;
    }

    CartoDarkMapType.prototype.maxZoom = 22;
    CartoDarkMapType.prototype.name = 'Carto Dark';
    CartoDarkMapType.prototype.alt = 'Tile Carto Dark Map Type';
    CartoDarkMapType.prototype.getTile = function (coord, zoom, ownerDocument) {
        var url = "https://cartodb-basemaps-a.global.ssl.fastly.net/dark_all/{z}/{x}/{y}.png";

        url = url.replace('{x}', coord.x)
            .replace('{y}', coord.y)
            .replace('{z}', zoom);
        var tile = ownerDocument.createElement('img');
        // Wrap y (latitude) in a like manner if you want to enable vertical infinite scroll
        tile.src = url;
        tile.style.width = this.tileSize.width + 'px';
        tile.style.height = this.tileSize.height + 'px';
        return tile;
    };

    var mapTypeId = "OSM";
    if (typeof(Storage) !== "undefined" && localStorage) {
        if (localStorage.getItem('mapTypeId'))
            mapTypeId = localStorage.getItem('mapTypeId');
        else
            localStorage.setItem("mapTypeId", "OSM");
    }


    self.gmap = new google.maps.Map(element, {
        center: new google.maps.LatLng(57, 21),
        zoomControl: true,
        zoomControlOptions: {
            style: google.maps.ZoomControlStyle.LARGE,
            position: google.maps.ControlPosition.LEFT_CENTER
        },
        scaleControl: true,
        streetViewControl: false,
        overviewMapControl: true,
        zoom: 3,
        mapTypeId: mapTypeId,
        mapTypeControlOptions: {
            mapTypeIds: ['OSM', /* 'CartoDark',*/ 'CartoLight', /* 'coordinate',*/ 'roadmap', 'satellite'],
            style: google.maps.MapTypeControlStyle.DROPDOWN_MENU,
            position: google.maps.ControlPosition.LEFT_CENTER
        }
    });

    self.gmap.addListener('maptypeid_changed', function () {
        var showStreetViewControl = self.gmap.getMapTypeId() === 'roadmap' || self.gmap.getMapTypeId() === 'satellite';
        localStorage.setItem("mapTypeId",self.gmap.getMapTypeId());
        self.gmap.setOptions({
            streetViewControl: showStreetViewControl
        });
    });


    //Define OSM map type pointing at the OpenStreetMap tile server
    self.gmap.mapTypes.set("OSM", new OSMMapType(new google.maps.Size(256, 256)));
    //Define Carto Dark map type pointing at the OpenStreetMap tile server
    // self.gmap.mapTypes.set("CartoDark", new CartoDarkMapType(new google.maps.Size(256, 256)));
    //Define Carto Light map type pointing at the OpenStreetMap tile server
    self.gmap.mapTypes.set("CartoLight", new CartoLightMapType(new google.maps.Size(256, 256)));
    // Now attach the coordinate map type to the map's registry.
    //self.gmap.mapTypes.set('coordinate', new CoordMapType(new google.maps.Size(256, 256)));

    // Initialize search box for places
    var input = (document.getElementById('pac-input'));
    self.gmap.controls[google.maps.ControlPosition.TOP_LEFT].push(input);
    self.searchBox = new google.maps.places.SearchBox((input));

    google.maps.event.addListener(self.searchBox, 'places_changed', function () {
        var places = self.searchBox.getPlaces();

        if (places.length == 0) {
            return;
        }

        self.gmap.panTo(places[0].geometry.location);
        self.gmap.setZoom(17);
    });

    // Bias the SearchBox results towards places that are within the bounds of the
    // current map's viewport.
    self.gmap.addListener(self.gmap, 'bounds_changed', function () {
        var bounds = self.gmap.getBounds();
        self.searchBox.setBounds(bounds);
    });
});


app.factory('AnyplaceService', function () {

    var anyService = {};

    anyService.selectedBuilding = undefined;
    anyService.selectedFloor = undefined;
    anyService.selectedPoi = undefined;
    anyService.selectedCampus = undefined;
    anyService.ShowShareProp = undefined;
    anyService.progress = undefined;
    anyService.allPois = {};
    anyService.allConnections = {};
    anyService.radioHeatmapRSSMode = false;
    anyService.fingerPrintsTimeMode = false;
    anyService.radioHeatmapRSSTimeMode = false;


    anyService.alerts = [];

    anyService.jsonReq = {
        username: 'username',
        password: 'password'
    };
    anyService.BASE_URL = "https://ap.cs.ucy.ac.cy";

    anyService.getBuilding = function () {
        return this.selectedBuilding;
    };

    anyService.getCampus = function () {
        return this.selectedCampus;
    };

    anyService.getBuildingId = function () {
        if (!this.selectedBuilding) {
            return undefined;
        }
        return this.selectedBuilding.buid;
    };

    anyService.getBuildingName = function () {
        if (!this.selectedBuilding) {
            return 'N/A';
        }
        return this.selectedBuilding.name;
    };

    anyService.getCampusName = function () {
        if (!this.selectedCampus) {
            return 'N/A';
        }
        return this.selectedCampus.name;
    };

    anyService.getFloor = function () {
        return this.selectedFloor;
    };

    anyService.getFloorNumber = function () {
        if (!this.selectedFloor) {
            return 'N/A';
        }
        return String(this.selectedFloor.floor_number);
    };

    anyService.getFloorName = function () {
        return this.selectedFloor.floor_name;
    };

    anyService.setBuilding = function (b) {
        this.selectedBuilding = b;
    };

    anyService.setFloor = function (f) {
        this.selectedFloor = f;
    };

    anyService.addAlert = function (type, msg) {
        // this.alerts[0] = ({msg: msg, type: type});
        this.alerts[0] = ({msg: msg, type: type});

    };

    anyService.closeAlert = function (index) {
        this.alerts.splice(index, 1);
    };

    anyService.getBuildingViewerUrl = function () {
        if (!this.selectedBuilding || !this.selectedBuilding.buid) {
            return "N/A";
        }
        return this.selectedBuilding.buid;
    };

    anyService.getBuildingViewerUrlEncoded = function () {
        if (!this.selectedBuilding || !this.selectedBuilding.buid) {
            return "N/A";
        }
        return encodeURIComponent("https://ap.cs.ucy.ac.cy/viewer/?buid=" + this.selectedBuilding.buid);
    };

    anyService.getCampusViewerUrl = function () {
        if (!this.selectedCampus || !this.selectedCampus.cuid) {
            return "N/A";
        }
        return "https://ap.cs.ucy.ac.cy/viewer/?cuid=" + this.selectedCampus.cuid;
    };

    anyService.getCampusViewerUrlEncoded = function () {
        if (!this.selectedCampus || !this.selectedCampus.cuid) {
            return "N/A";
        }
        return encodeURIComponent("https://ap.cs.ucy.ac.cy/viewer/?cuid=" + this.selectedCampus.cuid);
    };

    anyService.setAllPois = function (p) {
        this.allPois = {};
        this.allPois = p;
    };

    anyService.setAllConnection = function (c) {
        this.allConnections = {};
        this.allConnections = c;
    };

    anyService.getAllPois = function () {
        if (!this.allPois) {
            return 'N/A';
        }
        return this.allPois;
    };

    anyService.getAllConnections = function () {
        if (!this.allConnections) {
            return 'N/A';
        }
        return this.allConnections;
    };

    anyService.clearAllData = function () {
        anyService.selectedPoi = undefined;
        anyService.selectedFloor = undefined;
        anyService.selectedBuilding = undefined;
        anyService.selectedCampus = undefined;
        anyService.ShowShareProp = undefined;
        anyService.allPois = {};
        anyService.allConnections = {};
    };

    return anyService;
});

app.factory('Alerter', function () {
    var alerter = {};

    alerter.AlertCtrl = '-';

    return alerter;
});

app.factory('formDataObject', function () {
    return function (data, headersGetter) {
        var formData = new FormData();
        angular.forEach(data, function (value, key) {
            formData.append(key, value);
        });

        var headers = headersGetter();
        delete headers['Content-Type'];
        return formData;
    };
});

app.config(['$locationProvider', function ($location) {
    //now there won't be a hashbang within URLs for browsers that support HTML5 history
    $location.html5Mode(true);
}]);

app.filter('propsFilter', function () {
    return function (items, props) {
        var out = [];

        if (angular.isArray(items)) {
            items.forEach(function (item) {
                var itemMatches = false;

                var keys = Object.keys(props);
                for (var i = 0; i < keys.length; i++) {
                    var prop = keys[i];
                    var text = props[prop].toLowerCase();
                    if (item[prop].toString().toLowerCase().indexOf(text) !== -1) {
                        itemMatches = true;
                        break;
                    }
                }

                if (itemMatches) {
                    out.push(item);
                }
            });
        } else {
            // Let the output be the input untouched
            out = items;
        }

        return out;
    }
});

app.factory('myInterceptor', [function () {
    var requestInterceptor = {
        request: function (config) {

            if (config.url !== undefined)
                if (config.url.indexOf(AnyplaceAPI.FULL_SERVER) !== 0) {
                    return config;
                }

            if (config.data) {
                config.data.access_token = app.access_token;
            }

            return config;
        }
    };

    return requestInterceptor;
}]);

app.config(['$httpProvider', function ($httpProvider) {
    $httpProvider.interceptors.push('myInterceptor');
}]);

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

 AnyplaceAPI.FULL_SERVER = "http://localhost:9000/anyplace";

 // AnyplaceAPI.FULL_SERVER = "https://ap.cs.ucy.ac.cy/anyplace";

// AnyplaceAPI.FULL_SERVER = "https://ap-dev.cs.ucy.ac.cy/anyplace";


/**
 *
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

// ship add
AnyplaceAPI.Mapping.SHIP_ADD = "/mapping/ship/add";
AnyplaceAPI.Mapping.SHIP_ADD_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.SHIP_ADD;

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


//lsolea01
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

    apiService.addShip = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.SHIP_ADD_URL,
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


    apiService.getShipPosition = function () {
        //alert( "make the request: " + json_req );
        return $http({
            method: "GET",
            url: "services.marinetraffic.com/api/exportvessel/v:5/78e2e7fd31c8707894aca549b06823598368d250/timespan:20/protocol:jsono/mmsi:310627000",
            // data: json_req
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

CanvasOverlay.prototype = new google.maps.OverlayView();

// https://github.com/wbyoko/angularjs-google-maps-components

/** @constructor */
function CanvasOverlay(image, map) {

    // Now initialize all properties.
    this.top = 0;
    this.left = 0;
    this.width = image.width;
    this.height = image.height;

    while (window && (this.width > window.innerWidth || this.height > window.innerHeight)) {
        this.width /= 2;
        this.height /= 2;
    }

    this.image_ = image;
    this.map_ = map;

    // We define a property to hold the canvas's
    // div. We'll actually create this div
    // upon receipt of the add() method so we'll
    // leave it null for now.
    this.div_ = null;
    this.canvas = null;
    this.ctx = null;
    this.angle = 0;
    this.scale = 1;

    this.latlng = map.getCenter();


    this.new_left = 0;
    this.new_top = 0;


    // Explicitly call setMap on this overlay
    this.setMap(map);
}

CanvasOverlay.prototype.onAdd = function () {


    // Note: an overlay's receipt of add() indicates that
    // the map's panes are now available for attaching
    // the overlay to the map via the DOM.

    // Create the DIV and set some basic attributes.
    var div = document.createElement('div');
    div.id = "canvas_editor";
    div.style.border = 'none';
    div.style.borderWidth = '0px';
    div.style.position = 'relative';
    div.style.top = this.top;
    div.style.left = this.left;

    // initialize the position of the outer div according to the center of the overlay
    // which is the center of the map
    var projection;
    if ((projection = this.getProjection()) != null) {
        var xy = projection.fromLatLngToDivPixel(this.latlng);
        div.style.top = (xy.y - this.height / 2) + 'px';
        div.style.left = (xy.x - this.width / 2) + 'px';
    }

    var self = this;

//    https://github.com/unlocomqx/jQuery-ui-resizable-rotation-patch/blob/master/resizable-rotation.patch.js
//    fixes problems in resizing roatated image
    function n(e) {
        return parseInt(e, 10) || 0
    }

    //patch: totally based on andyzee work here, thank you
    //patch: https://github.com/andyzee/jquery-resizable-rotation-patch/blob/master/resizable-rotation.patch.js
    //patch: search for "patch:" comments for modifications
    //patch: based on version jquery-ui-1.10.3
    //patch: can be easily reproduced with your current version
    //patch: start of patch
    /**
     * Calculate the size correction for resized rotated element
     * @param {Number} init_w
     * @param {Number} init_h
     * @param {Number} delta_w
     * @param {Number} delta_h
     * @param {Number} angle in degrees
     * @returns {object} correction css object {left, top}
     */
    jQuery.getCorrection = function(init_w, init_h, delta_w, delta_h, angle){
        //Convert angle from degrees to radians
        var angle = angle * Math.PI / 180

        //Get position after rotation with original size
        var x = -init_w/2;
        var y = init_h/2;
        var new_x = y * Math.sin(angle) + x * Math.cos(angle);
        var new_y = y * Math.cos(angle) - x * Math.sin(angle);
        var diff1 = {left: new_x - x, top: new_y - y};

        var new_width = init_w + delta_w;
        var new_height = init_h + delta_h;

        //Get position after rotation with new size
        var x = -new_width/2;
        var y = new_height/2;
        var new_x = y * Math.sin(angle) + x * Math.cos(angle);
        var new_y = y * Math.cos(angle) - x * Math.sin(angle);
        var diff2 = {left: new_x - x, top: new_y - y};

        //Get the difference between the two positions
        var offset = {left: diff2.left - diff1.left, top: diff2.top - diff1.top};
        return offset;
    }

    jQuery.ui.resizable.prototype._mouseStart = function(event) {

        var curleft, curtop, cursor,
            o = this.options,
            el = this.element;

        this.resizing = true;

        this._renderProxy();

        curleft = n(this.helper.css("left"));
        curtop = n(this.helper.css("top"));

        if (o.containment) {
            curleft += $(o.containment).scrollLeft() || 0;
            curtop += $(o.containment).scrollTop() || 0;
        }

        this.offset = this.helper.offset();
        this.position = { left: curleft, top: curtop };

        this.size = this._helper ? {
            width: this.helper.width(),
            height: this.helper.height()
        } : {
            width: el.width(),
            height: el.height()
        };

        this.originalSize = this._helper ? {
            width: el.outerWidth(),
            height: el.outerHeight()
        } : {
            width: el.width(),
            height: el.height()
        };

        this.sizeDiff = {
            width: el.outerWidth() - el.width(),
            height: el.outerHeight() - el.height()
        };

        this.originalPosition = { left: curleft, top: curtop };
        this.originalMousePosition = { left: event.pageX, top: event.pageY };

        //patch: object to store previous data
        this.lastData = this.originalPosition;

        this.aspectRatio = (typeof o.aspectRatio === "number") ?
            o.aspectRatio :
            ((this.originalSize.width / this.originalSize.height) || 1);

        cursor = $(".ui-resizable-" + this.axis).css("cursor");
        $("body").css("cursor", cursor === "auto" ? this.axis + "-resize" : cursor);

        el.addClass("ui-resizable-resizing");
        this._propagate("start", event);
        return true;
    };

    jQuery.ui.resizable.prototype._mouseDrag = function(event) {
        //patch: get the angle
        var angle = getAngle(this.element[0]);
        var angle_rad = angle * Math.PI / 180;

        var data,
            el = this.helper, props = {},
            smp = this.originalMousePosition,
            a = this.axis,
            prevTop = this.position.top,
            prevLeft = this.position.left,
            prevWidth = this.size.width,
            prevHeight = this.size.height,
            dx = (event.pageX-smp.left)||0,
            dy = (event.pageY-smp.top)||0,
            trigger = this._change[a];

        var init_w = this.size.width;
        var init_h = this.size.height;

        if (!trigger) {
            return false;
        }

        //patch: cache cosine & sine
        var _cos = Math.cos(angle_rad);
        var _sin = Math.sin(angle_rad);

        //patch: calculate the correct mouse offset for a more natural feel
        var ndx = dx * _cos + dy * _sin;
        var ndy = dy * _cos - dx * _sin;
        dx = ndx;
        dy = ndy;

        // Calculate the attrs that will be change
        data = trigger.apply(this, [event, dx, dy]);

        // Put this in the mouseDrag handler since the user can start pressing shift while resizing
        this._updateVirtualBoundaries(event.shiftKey);
        if (this._aspectRatio || event.shiftKey) {
            data = this._updateRatio(data, event);
        }

        data = this._respectSize(data, event);

        //patch: backup the position
        var oldPosition = {left: this.position.left, top: this.position.top};

        this._updateCache(data);

        //patch: revert to old position
        this.position = {left: oldPosition.left, top: oldPosition.top};

        //patch: difference between datas
        var diffData = {
            left: _parseFloat(data.left || this.lastData.left) - _parseFloat(this.lastData.left),
            top:  _parseFloat(data.top || this.lastData.top)  - _parseFloat(this.lastData.top),
        }

        //patch: calculate the correct position offset based on angle
        var new_data = {};
        new_data.left = diffData.left * _cos - diffData.top  * _sin;
        new_data.top  = diffData.top  * _cos + diffData.left * _sin;

        //patch: round the values
        new_data.left = _round(new_data.left);
        new_data.top  = _round(new_data.top);

        //patch: update the position
        this.position.left += new_data.left;
        this.position.top  += new_data.top;

        //patch: save the data for later use
        this.lastData = {
            left: _parseFloat(data.left || this.lastData.left),
            top:  _parseFloat(data.top  || this.lastData.top)
        };

        // plugins callbacks need to be called first
        this._propagate("resize", event);

        //patch: calculate the difference in size
        var diff_w = init_w - this.size.width;
        var diff_h = init_h - this.size.height;

        //patch: get the offset based on angle
        var offset = $.getCorrection(init_w, init_h, diff_w, diff_h, angle);

        //patch: update the position
        this.position.left += offset.left;
        this.position.top -= offset.top;

        if (this.position.top !== prevTop) {
            props.top = this.position.top + "px";
        }
        if (this.position.left !== prevLeft) {
            props.left = this.position.left + "px";
        }
        if (this.size.width !== prevWidth) {
            props.width = this.size.width + "px";
        }
        if (this.size.height !== prevHeight) {
            props.height = this.size.height + "px";
        }
        el.css(props);

        if (!this._helper && this._proportionallyResizeElements.length) {
            this._proportionallyResize();
        }

        // Call the user callback if the element was resized
        if ( ! $.isEmptyObject(props) ) {
            this._trigger("resize", event, this.ui());
        }

        return false;
    }

    //patch: get the angle
    function getAngle(el) {
        var st = window.getComputedStyle(el, null);
        var tr = st.getPropertyValue("-webkit-transform") ||
            st.getPropertyValue("-moz-transform") ||
            st.getPropertyValue("-ms-transform") ||
            st.getPropertyValue("-o-transform") ||
            st.getPropertyValue("transform") ||
            null;
        if(tr && tr != "none"){
            var values = tr.split('(')[1];
            values = values.split(')')[0];
            values = values.split(',');

            var a = values[0];
            var b = values[1];

            var angle = Math.round(Math.atan2(b, a) * (180/Math.PI));
            while(angle >= 360) angle = 360-angle;
            while(angle < 0) angle = 360+angle;
            return angle;
        }
        else
            return 0;
    }

    function _parseFloat(e) {
        return isNaN(parseFloat(e)) ? 0: parseFloat(e);
    }

    function _round(e) {
        return Math.round((e + 0.00001) * 100) / 100
    }
    /* end of patch functions */

    jQuery(div).resizable({
        // aspectRatio: (this.image_.width / this.image_.height),
        ghost: true,
        handles: "sw, se, nw, ne",
        helper: "resizable-helper",
        aspectRatio: false,
        stop: function (event, ui) {
            self.ctx.clearRect(0, 0, self.ctx.canvas.width, self.ctx.canvas.height);

            self.width = ui.size.width;
            self.height = ui.size.height;

            self.setCanvasSize();

            self.ctx.save();
            self.ctx.translate((self.ctx.canvas.width / 2), (self.ctx.canvas.height / 2));

            self.ctx.drawImage(self.image_, -(self.width / 2), -(self.height / 2), self.width, self.height);
            self.ctx.restore();
        }
    }).rotatable({
        stop: function (event, ui) {
            //self.angle = ui.angle.stop;
        }
    });

    jQuery(div).draggable({
        stop: function (event, ui) {
            // update the coordinates
            if (projection != null) {
                var left = $(this).position().left;
                var top = $(this).position().top;
                self.latlng = projection.fromDivPixelToLatLng(new google.maps.Point(left, top), true);
            }
        }
    });

    // Create a Canvas element and attach it to the DIV.
    var canvas = document.createElement('canvas');
    canvas.id = "canvas";
    div.appendChild(canvas);

    // Set the overlay's div_ property to this DIV
    this.div_ = div;
    this.canvas = canvas;
    this.ctx = canvas.getContext("2d");

    // load the floor
    this.initImage();

    // We add an overlay to a map via one of the map's panes.
    // We'll add this overlay to the overlayImage pane.
    var panes = this.getPanes();
    panes.overlayImage.appendChild(this.div_);
////////////////////////////////////////
    var that=this;
    var container=div;
    google.maps.event.addDomListener(this.get('map').getDiv(),
        'mouseleave',
        function(){
            google.maps.event.trigger(container,'mouseup');
        }
    );


    google.maps.event.addDomListener(container,
        'mousedown',
        function(e){
            this.style.cursor='move';
            that.map.set('draggable',false);
            that.set('origin',e);        }
    );

    google.maps.event.addDomListener(container,'mouseup',function(){
        that.map.set('draggable',true);
        this.style.cursor='default';
        google.maps.event.removeListener(that.moveHandler);
    });

    return this;
}

CanvasOverlay.prototype.draw = function () {
    var div = this.div_;

    if (this.canvas == null) {
        alert("error creating the canvas");
    }
}

CanvasOverlay.prototype.onRemove = function () {
    this.div_.parentNode.removeChild(this.div_);
}

// Note that the visibility property must be a string enclosed in quotes
CanvasOverlay.prototype.hide = function () {
    if (this.div_) {
        this.div_.style.visibility = 'hidden';
    }
}

CanvasOverlay.prototype.show = function () {
    if (this.div_) {
        this.div_.style.visibility = 'visible';
    }
}

CanvasOverlay.prototype.toggle = function () {
    if (this.div_) {
        if (this.div_.style.visibility == 'hidden') {
            this.show();
        } else {
            this.hide();
        }
    }
}

CanvasOverlay.prototype.toggleDOM = function () {
    if (this.getMap()) {
        this.setMap(null);
    } else {
        this.setMap(this.map_);
    }
}

/*************************
 * CANVAS METHODS
 */
CanvasOverlay.prototype.getCanvas = function () {
    return this.canvas;
}

CanvasOverlay.prototype.getContext2d = function () {
    return this.ctx;
}


/*****************************
 * EDITING METHODS
 */

CanvasOverlay.prototype.setCanvasSize = function () {
    this.ctx.canvas.width = this.width;
    this.ctx.canvas.height = this.height;

    // we need to change the width and height of the div #canvas_editor
    // which is the element being rotated by the slider
    this.div_.style.width = this.width + 'px';
    this.div_.style.height = this.height + 'px';
}

CanvasOverlay.prototype.initImage = function () {
    this.setCanvasSize();
    this.ctx.save();

    this.ctx.translate((this.ctx.canvas.width / 2), (this.ctx.canvas.height / 2));
    this.ctx.rotate(this.angle);

    this.ctx.drawImage(this.image_, -(this.width / 2), -(this.height / 2), this.width, this.height);
    this.ctx.restore();
}

CanvasOverlay.prototype.drawBoundingCanvas = function () {
    // convert degress rotation to angle radians
    var degrees = getRotationDegrees($('#canvas_editor'));
    //var degrees= parseFloat($('#rot_degrees').val());
    var rads = deg2rad(degrees);

    $('#canvas_editor').css({
        '-moz-transform': 'rotate(0deg)',
        '-webkit-transform': 'rotate(0deg)',
        '-ms-transform': 'rotate(0deg)',
        '-o-transform': 'rotate(0deg)',
        'transform': 'rotate(0deg)'
    });

    // get the center which we use to rotate the image
    // this is the center when the canvas is rotated at 0 degrees
    var oldCenter = getPositionData($('#canvas_editor'));
    var oldLeft = oldCenter.left;
    var oldTop = oldCenter.top;
    var oldCenterX = oldLeft + this.width / 2;
    var oldCenterY = oldTop + this.height / 2;

    // calculate the 4 new corners - http://stackoverflow.com/a/622172/1066790
    //                             - https://en.wikipedia.org/wiki/Transformation_matrix#Rotation
    var top_left_x = oldCenterX + (oldLeft - oldCenterX) * Math.cos(rads) + (oldTop - oldCenterY) * Math.sin(rads);
    var top_left_y = oldCenterY - (oldLeft - oldCenterX) * Math.sin(rads) + (oldTop - oldCenterY) * Math.cos(rads);
    var top_right_x = oldCenterX + (oldLeft + this.width - oldCenterX) * Math.cos(rads) + (oldTop - oldCenterY) * Math.sin(rads);
    var top_right_y = oldCenterY - (oldLeft + this.width - oldCenterX) * Math.sin(rads) + (oldTop - oldCenterY) * Math.cos(rads);
    var bottom_left_x = oldCenterX + (oldLeft - oldCenterX) * Math.cos(rads) + (oldTop + this.height - oldCenterY) * Math.sin(rads);
    var bottom_left_y = oldCenterY - (oldLeft - oldCenterX) * Math.sin(rads) + (oldTop + this.height - oldCenterY) * Math.cos(rads);
    var bottom_right_x = oldCenterX + (oldLeft + this.width - oldCenterX) * Math.cos(rads) + (oldTop + this.height - oldCenterY) * Math.sin(rads);
    var bottom_right_y = oldCenterY - (oldLeft + this.width - oldCenterX) * Math.sin(rads) + (oldTop + this.height - oldCenterY) * Math.cos(rads);
    // calculate new coordinates finding the top left
    var maxx = Math.max(top_left_x, top_right_x, bottom_left_x, bottom_right_x);
    var maxy = Math.max(top_left_y, top_right_y, bottom_left_y, bottom_right_y);
    var minx = Math.min(top_left_x, top_right_x, bottom_left_x, bottom_right_x);
    var miny = Math.min(top_left_y, top_right_y, bottom_left_y, bottom_right_y);
    var newTop = miny;
    var newLeft = minx;

    var w = maxx - minx;
    var h = maxy - miny;

    // move the canvas to the new top left position
    $('#canvas_editor').css({
        'top': newTop + 'px',
        'left': newLeft + 'px'
    })

    this.ctx.canvas.width = w;
    this.ctx.canvas.height = h;
    this.ctx.save();
    this.ctx.clearRect(0, 0, this.ctx.canvas.width, this.ctx.canvas.height);
    this.ctx.translate(oldCenterX - newLeft, oldCenterY - newTop);
    this.ctx.rotate(rads);
    this.ctx.drawImage(this.image_, -(this.width / 2), -(this.height / 2), this.width, this.height);
    this.ctx.restore();

    // we should now update the coordinates for the new image
    this.top = newTop;
    this.left = newLeft;
    var projection;
    if ((projection = this.getProjection()) != null) {
        this.bottom_left_coords = projection.fromContainerPixelToLatLng(new google.maps.Point(this.left, this.top + h), true);
        this.top_right_coords = projection.fromContainerPixelToLatLng(new google.maps.Point(this.left + w, this.top), true);
    }
}

/***************************************
 * HELPER FUNCTIONS
 */
function deg2rad(deg) {
    return deg * Math.PI / 180;
}
function rad2deg(rad) {
    return rad / Math.PI * 180;
}

function getRotationDegrees(obj) {
    var matrix = obj.css("-webkit-transform") ||
        obj.css("-moz-transform") ||
        obj.css("-ms-transform") ||
        obj.css("-o-transform") ||
        obj.css("transform");
    if (matrix !== 'none') {
        var values = matrix.split('(')[1].split(')')[0].split(',');
        var a = values[0];
        var b = values[1];
        var c = values[2];
        var d = values[3];

        var sin = b / scale;

        var scale = Math.sqrt(a * a + b * b);
        var angle = Math.round(Math.atan2(b, a) * (180 / Math.PI));
        //var angle = Math.atan2(b, a) * (180/Math.PI);
    } else {
        var angle = 0;
    }
    return angle;
}
// src= http://jsfiddle.net/Y8d6k/
var getPositionData = function (el) {
    return $.extend({
        width: el.outerWidth(false),
        height: el.outerHeight(false)
    }, el.offset());
};
/**
 *
 The MIT License (MIT)

 Copyright (c) 2015, Lambros Petrou, Data Management Systems Laboratory (DMSL)
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

var LPUtils = {};

LPUtils.isNullOrUndefined = function( t ){
    return (typeof(t) === 'undefined' || t === null);
}

LPUtils.isStringEmptyNullUndefined = function( s ){
    return ( !s || 0 === s.length );
    //return ( typeof(s) === 'undefined' || s === null || 0 === s.length );
}

LPUtils.isStringBlankNullUndefined = function( s ){
    return ( !s || /^\s*$/.test(s) );
}

/************************************
 * UI Functions
 */

LPUtils.alert = function( msg ){
    var np = document.getElementById( "notification_panel" );
    np.innerHTML = "<alert type=\"error\" close=\"LPUtils.closeAlert()\">"+ msg +"</alert>";
}
LPUtils.closeAlert = function(){
    var np = document.getElementById( "notification_panel" );
    np.getEl.innerHTML = "";
}

LPUtils.success = function( msg ){
    var np = document.getElementById( "notification_panel" );
    np.innerHTML = "<alert type=\"success\">"+ msg +"</alert>";
}


function hexToBase64(str) {
    return btoa(String.fromCharCode.apply(null, str.replace(/\r|\n/g, "").replace(/([\da-fA-F]{2}) ?/g, "0x$1 ").replace(/ +$/, "").split(" ")));
}

function base64_encode (data) {
    // http://kevin.vanzonneveld.net
    // +   original by: Tyler Akins (http://rumkin.com)
    // +   improved by: Bayron Guevara
    // +   improved by: Thunder.m
    // +   improved by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
    // +   bugfixed by: Pellentesque Malesuada
    // +   improved by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
    // +   improved by: Rafał Kukawski (http://kukawski.pl)
    // *     example 1: base64_encode('Kevin van Zonneveld');
    // *     returns 1: 'S2V2aW4gdmFuIFpvbm5ldmVsZA=='
    // mozilla has this native
    // - but breaks in 2.0.0.12!
    //if (typeof this.window['btoa'] == 'function') {
    //    return btoa(data);
    //}
    var b64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
    var o1, o2, o3, h1, h2, h3, h4, bits, i = 0,
        ac = 0,
        enc = "",
        tmp_arr = [];

    if (!data) {
        return data;
    }

    do { // pack three octets into four hexets
        o1 = data.charCodeAt(i++);
        o2 = data.charCodeAt(i++);
        o3 = data.charCodeAt(i++);

        bits = o1 << 16 | o2 << 8 | o3;

        h1 = bits >> 18 & 0x3f;
        h2 = bits >> 12 & 0x3f;
        h3 = bits >> 6 & 0x3f;
        h4 = bits & 0x3f;

        // use hexets to index into b64, and append result to encoded string
        tmp_arr[ac++] = b64.charAt(h1) + b64.charAt(h2) + b64.charAt(h3) + b64.charAt(h4);
    } while (i < data.length);

    enc = tmp_arr.join('');

    var r = data.length % 3;

    return (r ? enc.slice(0, r - 3) : enc) + '==='.slice(r || 3);

}

LPUtils.Base64 = {

// private property
    _keyStr : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",

// public method for encoding
    encode : function (input) {
        var output = "";
        var chr1, chr2, chr3, enc1, enc2, enc3, enc4;
        var i = 0;

        input = LPUtils.Base64._utf8_encode(input);

        while (i < input.length) {

            chr1 = input.charCodeAt(i++);
            chr2 = input.charCodeAt(i++);
            chr3 = input.charCodeAt(i++);

            enc1 = chr1 >> 2;
            enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
            enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
            enc4 = chr3 & 63;

            if (isNaN(chr2)) {
                enc3 = enc4 = 64;
            } else if (isNaN(chr3)) {
                enc4 = 64;
            }

            output = output +
                this._keyStr.charAt(enc1) + this._keyStr.charAt(enc2) +
                this._keyStr.charAt(enc3) + this._keyStr.charAt(enc4);

        }

        return output;
    },

// public method for decoding
    decode : function (input) {
        var output = "";
        var chr1, chr2, chr3;
        var enc1, enc2, enc3, enc4;
        var i = 0;

        input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");

        while (i < input.length) {

            enc1 = this._keyStr.indexOf(input.charAt(i++));
            enc2 = this._keyStr.indexOf(input.charAt(i++));
            enc3 = this._keyStr.indexOf(input.charAt(i++));
            enc4 = this._keyStr.indexOf(input.charAt(i++));

            chr1 = (enc1 << 2) | (enc2 >> 4);
            chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
            chr3 = ((enc3 & 3) << 6) | enc4;

            output = output + String.fromCharCode(chr1);

            if (enc3 != 64) {
                output = output + String.fromCharCode(chr2);
            }
            if (enc4 != 64) {
                output = output + String.fromCharCode(chr3);
            }

        }

        output = LPUtils.Base64._utf8_decode(output);

        return output;

    },

// private method for UTF-8 encoding
    _utf8_encode : function (string) {
        string = string.replace(/\r\n/g,"\n");
        var utftext = "";

        for (var n = 0; n < string.length; n++) {

            var c = string.charCodeAt(n);

            if (c < 128) {
                utftext += String.fromCharCode(c);
            }
            else if((c > 127) && (c < 2048)) {
                utftext += String.fromCharCode((c >> 6) | 192);
                utftext += String.fromCharCode((c & 63) | 128);
            }
            else {
                utftext += String.fromCharCode((c >> 12) | 224);
                utftext += String.fromCharCode(((c >> 6) & 63) | 128);
                utftext += String.fromCharCode((c & 63) | 128);
            }

        }

        return utftext;
    },

// private method for UTF-8 decoding
    _utf8_decode : function (utftext) {
        var string = "";
        var i = 0;

        while ( i < utftext.length ) {

            var c = utftext.charCodeAt(i);

            if (c < 128) {
                string += String.fromCharCode(c);
                i++;
            }
            else if((c > 191) && (c < 224)) {
                var c2 = utftext.charCodeAt(i+1);
                string += String.fromCharCode(((c & 31) << 6) | (c2 & 63));
                i += 2;
            }
            else {
                var c2 = utftext.charCodeAt(i+1);
                var c3 = utftext.charCodeAt(i+2);
                string += String.fromCharCode(((c & 15) << 12) | ((c2 & 63) << 6) | (c3 & 63));
                i += 3;
            }

        }

        return string;
    }

}


/**
 * Uses the new array typed in javascript to binary base64 encode/decode
 * at the moment just decodes a binary base64 encoded
 * into either an ArrayBuffer (decodeArrayBuffer)
 * or into an Uint8Array (decode)
 *
 * References:
 * https://developer.mozilla.org/en/JavaScript_typed_arrays/ArrayBuffer
 * https://developer.mozilla.org/en/JavaScript_typed_arrays/Uint8Array
 */

LPUtils.Base64Binary = {
    _keyStr : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",

    /* will return a  Uint8Array type */
    decodeArrayBuffer: function(input) {
        var bytes = (input.length/4) * 3;
        var ab = new ArrayBuffer(bytes);
        this.decode(input, ab);

        return ab;
    },

    decode: function(input, arrayBuffer) {
        //get last chars to see if are valid
        var lkey1 = this._keyStr.indexOf(input.charAt(input.length-1));
        var lkey2 = this._keyStr.indexOf(input.charAt(input.length-2));

        var bytes = (input.length/4) * 3;
        if (lkey1 == 64) bytes--; //padding chars, so skip
        if (lkey2 == 64) bytes--; //padding chars, so skip

        var uarray;
        var chr1, chr2, chr3;
        var enc1, enc2, enc3, enc4;
        var i = 0;
        var j = 0;

        if (arrayBuffer)
            uarray = new Uint8Array(arrayBuffer);
        else
            uarray = new Uint8Array(bytes);

        input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");

        for (i=0; i<bytes; i+=3) {
            //get the 3 octects in 4 ascii chars
            enc1 = this._keyStr.indexOf(input.charAt(j++));
            enc2 = this._keyStr.indexOf(input.charAt(j++));
            enc3 = this._keyStr.indexOf(input.charAt(j++));
            enc4 = this._keyStr.indexOf(input.charAt(j++));

            chr1 = (enc1 << 2) | (enc2 >> 4);
            chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
            chr3 = ((enc3 & 3) << 6) | enc4;

            uarray[i] = chr1;
            if (enc3 != 64) uarray[i+1] = chr2;
            if (enc4 != 64) uarray[i+2] = chr3;
        }

        return uarray;
    }
}

USGSOverlay.prototype = new google.maps.OverlayView();
/** @constructor */
function USGSOverlay(bounds, image, map) {

    // Now initialize all properties.
    this.bounds_ = bounds;
    this.image_ = image;
    this.image64 = image;
    this.map_ = map;

    // We define a property to hold the image's div. We'll
    // actually create this div upon receipt of the onAdd()
    // method so we'll leave it null for now.
    this.div_ = null;

    // Explicitly call setMap on this overlay
    this.setMap(map);
}

USGSOverlay.prototype.onAdd = function() {

    // Note: an overlay's receipt of onAdd() indicates that
    // the map's panes are now available for attaching
    // the overlay to the map via the DOM.

    // Create the DIV and set some basic attributes.
    var div = document.createElement('div');
    div.id="ground_overlay";
    div.style.borderStyle = 'none';
    div.style.borderWidth = '0px';
    div.style.position = 'absolute';

    // Create an IMG element and attach it to the DIV.
    var img = document.createElement('img');
    img.src = this.image64;
    img.style.width = '100%';
    img.style.height = '100%';
    img.style.position = 'absolute';
    div.appendChild(img);

    // Set the overlay's div_ property to this DIV
    this.div_ = div;

    // We add an overlay to a map via one of the map's panes.
    // We'll add this overlay to the overlayLayer pane.
    var panes = this.getPanes();
    panes.overlayLayer.appendChild(div);
}

USGSOverlay.prototype.draw = function() {

    // Size and position the overlay. We use a southwest and northeast
    // position of the overlay to peg it to the correct position and size.
    // We need to retrieve the projection from this overlay to do this.
    var overlayProjection = this.getProjection();

    // Retrieve the southwest and northeast coordinates of this overlay
    // in latlngs and convert them to pixels coordinates.
    // We'll use these coordinates to resize the DIV.
    var sw = overlayProjection.fromLatLngToDivPixel(this.bounds_.getSouthWest());
    var ne = overlayProjection.fromLatLngToDivPixel(this.bounds_.getNorthEast());

    // Resize the image's DIV to fit the indicated dimensions.
    var div = this.div_;
    div.style.left = sw.x + 'px';
    div.style.top = ne.y + 'px';
    div.style.width = (ne.x - sw.x) + 'px';
    div.style.height = (sw.y - ne.y) + 'px';
}

USGSOverlay.prototype.onRemove = function() {
    this.div_.parentNode.removeChild(this.div_);
    this.div_ = null;
}
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

app.controller("AlertController", ['$rootScope', '$scope', 'AnyplaceService', function ($rootScope, $scope, AnyplaceService) {

    $scope.anyService = AnyplaceService;

    $scope.alerts = AnyplaceService.alerts;

    $scope.closeable = true;

}]);
/**
 *
 The MIT License (MIT)

 Copyright (c) 2015, Kyriakos Georgiou, Marileni Angelidou,Loukas Solea, Data Management Systems Laboratory (DMSL)
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



app.controller('BuildingController', ['$cookieStore', '$scope', '$compile', 'GMapService', 'AnyplaceService', 'AnyplaceAPIService', function ($cookieStore, $scope, $compile, GMapService, AnyplaceService, AnyplaceAPIService) {

    $scope.myMarkers = {};
    $scope.myMarkerId = 0;
    $scope.laship=33.0000;
    $scope.loship=33.0000;
    $scope.moveShip=0;
    $scope.ShipInterval;
    $scope.gmapService = GMapService;
    $scope.anyService = AnyplaceService;
    $scope.anyAPI = AnyplaceAPIService;
    $scope.example9model = [];
    $scope.example9data = [];
    $scope.example9settings = {enableSearch: true, scrollable: true};

    $scope.example9modeledit = [];
    $scope.example9dataedit = [];
    $scope.example9settingsedit = {enableSearch: true, scrollable: true};

    $scope.myBuildings = [];
    $scope.myShips = [];



    $scope.myBuildingsHashT = {};
    $scope.myCampus = [];
    $scope.old_campus = [];

    $scope.crudTabSelected = 1;

    $scope.fileToUpload = "";
    $scope.logfile = "";

    $scope.poisTypes = {};
    $scope.catTypes = {};

    $scope.pageLoad = false;

    $scope.crudTabSelected = 1;


//     // Replace this with your URL.
//     var BUILDING_TILE_URL = AnyplaceAPI.FULL_SERVER + '/floortiles/{building}/{floor}/{z}/z{z}x{x}y{y}.png';
//
// // Name the layer anything you like.
//     var layerID = 'building_layer';
//
// // Create a new ImageMapType layer. static_tiles/19/z19x310801y207411.png
//     var maptiler = new google.maps.ImageMapType({
//         name: layerID,
//         getTileUrl: function (coord, zoom) {
//             var buid = $scope.anyService.getBuildingId();
//
//             var floor = $scope.anyService.getFloorNumber();
//             var url = BUILDING_TILE_URL
//                 .replace('{building}', buid)
//                 .replace('{floor}', floor)
//                 .replace('{z}', zoom)
//                 .replace('{x}', coord.x)
//                 .replace('{y}', coord.y)
//                 .replace('{z}', zoom);
//             return url;
//         },
//         tileSize: new google.maps.Size(256, 256),
//         isPng: true
//     });
//
//     // Register the new layer, then activate it.
//     $scope.gmapService.gmap.overlayMapTypes.insertAt(0, maptiler);

    $scope.setCrudTabSelected = function (n) {

        $scope.crudTabSelected = n;
        if (!$scope.anyService.getBuilding()) {
            _err("No building is selected.");
            return;
        }

        var b = $scope.myBuildingsHashT[$scope.anyService.getBuildingId()];



        if (!b) {
            return;
        }

        var m = b.marker;
        if (!m) {
            return;
        }
        // edit building
        if (n === 2) {
            m.setDraggable(true);
        } else {
            m.setDraggable(false);
        }

    };
    $scope.isCrudTabSelected = function (n) {
        return $scope.crudTabSelected === n;
    };

    $scope.$on("loggedIn", function (event, mass) {
        //_suc('Successfully logged in.');
        $scope.fetchAllBuildings();
        $scope.fetchAllCampus();
        //$scope.fetchAllPoisTypes();
    });

    $scope.$on("loggedOff", function (event, mass) {
        _clearBuildingMarkersAndModels();
        $scope.myMarkers = {};
        $scope.myMarkerId = 0;
        $scope.myBuildings = [];
        $scope.myBuildingsHashT = {};
    });

    /**
     * @return {string}
     */
    function S4() {
        return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
    }

    var guid = (S4() + S4() + "-" + S4() + "-4" + S4().substr(0, 3) + "-" + S4() + "-" + S4() + S4() + S4()).toLowerCase();
    var d = new Date();
    document.getElementById("CampusID").value = "cuid_" + guid + "_" + d.getTime();

    var logoPlanInputElement = $('#input-logo');

    logoPlanInputElement.change(function handleImage(e) {
        var reader = new FileReader();
        reader.onload = function (event) {
            var imgObj = new Image();
            imgObj.src = event.target.result;
        };
        reader.readAsDataURL(e.target.files[0]);
    });

    $scope.setLogoPlan = function (cuid) {

        var newFl = {
            is_published: 'true',
            cuid: cuid,
            logo: String($scope.newFloorNumber),
            description: String($scope.newFloorNumber),
            floor_number: String($scope.newFloorNumber)
        };

        $scope.myFloors[$scope.myFloorId] = newFl;
        $scope.myFloorId++;

        // create the proper image inside the canvas
        canvasOverlay.drawBoundingCanvas();

        // create the ground overlay and destroy the canvasOverlay object
        // and also set the floor_plan_coords in $scope.data
        var bl = canvasOverlay.bottom_left_coords;
        var tr = canvasOverlay.top_right_coords;
        $scope.data.floor_plan_coords.bottom_left_lat = bl.lat();
        $scope.data.floor_plan_coords.bottom_left_lng = bl.lng();
        $scope.data.floor_plan_coords.top_right_lat = tr.lat();
        $scope.data.floor_plan_coords.top_right_lng = tr.lng();
        $scope.data.floor_plan_coords.zoom = GMapService.gmap.getZoom() + "";
        var data = canvasOverlay.getCanvas().toDataURL("image/png"); // defaults to png
        $scope.data.floor_plan_base64_data = data;
        var imageBounds = new google.maps.LatLngBounds(
            new google.maps.LatLng(bl.lat(), bl.lng()),
            new google.maps.LatLng(tr.lat(), tr.lng()));
        $scope.data.floor_plan_groundOverlay = new USGSOverlay(imageBounds, data, GMapService.gmap);

        canvasOverlay.setMap(null); // remove the canvas overlay since the groundoverlay is placed
        $('#input-floor-plan').prop('disabled', false);
        $scope.isCanvasOverlayActive = false;

        if (_floorNoExists($scope.newFloorNumber)) {
            for (var i = 0; i < $scope.xFloors.length; i++) {
                var f = $scope.xFloors[i];
                if (!LPUtils.isNullOrUndefined(f)) {
                    if (f.floor_number === String($scope.newFloorNumber)) {
                        $scope.uploadFloorPlanBase64($scope.anyService.selectedBuilding, f, $scope.data);
                        break;
                    }
                }
            }
        } else {
            $scope.addFloorObject(newFl, $scope.anyService.selectedBuilding, $scope.data);
        }

    };

    $scope.$watch('anyService.selectedBuilding', function (newVal, oldVal) {
        if (newVal && newVal.coordinates_lat && newVal.coordinates_lon) {
            // Pan map to selected building
            $scope.gmapService.gmap.panTo(_latLngFromBuilding(newVal));
            $scope.gmapService.gmap.setZoom(19);
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem("lastBuilding", newVal.buid);
            }
        }
        //if (newVal && newVal.buid && newVal.poistypeid) {
        // $scope.fetchAllPoisTypes(newVal.poistypeid);
        // }
        // else {
        $scope.poisTypes = [
            "Disabled Toilets",
            "Elevator",
            "Entrance",
            "Fire Extinguisher",
            "First Aid/AED",
            "Kitchen",
            "Office",
            "Ramp",
            "Room",
            "Security/Guard",
            "Stair",
            "Toilets",
            "Other"
        ];
        //}
    });

    $scope.$watch('anyService.selectedCampus', function (newVal, oldVal) {
        if (newVal && newVal.cuid) {
            // Pan map to selected building
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem("lastCampus", newVal.cuid);
            }
        }

    });


    var _clearBuildingMarkersAndModels = function () {
        for (var b in $scope.myBuildingsHashT) {
            if ($scope.myBuildingsHashT.hasOwnProperty(b)) {
                $scope.myBuildingsHashT[b].marker.setMap(null);
                delete $scope.myBuildingsHashT[b];
            }
        }
    };

    var _err = function (msg) {
        $scope.anyService.addAlert('danger', msg);
    };

    var _suc = function (msg) {
        $scope.anyService.addAlert('success', msg);
    };

    var _latLngFromBuilding = function (b) {
        if (b && b.coordinates_lat && b.coordinates_lon) {
            return {
                lat: parseFloat(b.coordinates_lat),
                lng: parseFloat(b.coordinates_lon)
            }
        }
        return undefined;
    };
    $scope.fetchAllPoisTypes = function () {

        var jsonReq = {};

        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;
        jsonReq.access_token = $scope.gAuth.access_token;

        jsonReq.owner_id = $scope.owner_id;

        if (!jsonReq.owner_id) {
            _err("Could nor authorize user. Please refresh.1");
            return;
        }
        var promise = $scope.anyAPI.retrievePoisTypes(jsonReq);
        promise.then(
            function (resp) {
                var data = resp.data;
                $scope.catTypes = data.poistypes;
            },
            function (resp) {
                var data = resp.data;
                _err("Something went wrong while fetching POIs types");
            }
        );
    };


    $scope.fetchAllBuildings = function () {
        var jsonReq = {};
        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;
        jsonReq.owner_id = $scope.owner_id;

        if (!jsonReq.owner_id) {
            _err("Could nor authorize user. Please refresh.");
            return;
        }

        var promise = $scope.anyAPI.allBuildings(jsonReq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                $scope.myBuildings = [];
                $scope.myShips = [];

                //var bs = JSON.parse( data.buildings );
                for(var i in data.buildings){
                    var bid=data.buildings[i].buid;
                    var getship=bid.split("_",1);

                    if(getship=="ship") {
                        $scope.myShips.push(data.buildings[i]);
                    }
                    else{
                        $scope.myBuildings.push(data.buildings[i]);
                    }

                }



                var infowindow = new google.maps.InfoWindow({
                    content: '-',
                    maxWidth: 500
                });

                var localStoredBuildingIndex = -1;
                var localStoredBuildingId = undefined;
                if (typeof(Storage) !== "undefined" && localStorage && localStorage.getItem('lastBuilding')) {
                    localStoredBuildingId = localStorage.getItem('lastBuilding');
                }


                //show all buildings and ships on map
                for (var i = 0; i < data.buildings.length; i++) {

                    var b = data.buildings[i];

                    $scope.example9data[i] = {id: b.buid, label: b.name};
                    $scope.example9dataedit[i] = {id: b.buid, label: b.name};

                    if (localStoredBuildingId && localStoredBuildingId === b.buid) {
                        localStoredBuildingIndex = i;
                    }

                    if (b.is_published === 'true' || b.is_published == true) {
                        b.is_published = true;
                    } else {
                        b.is_published = false;
                    }
                    var bid=b.buid;
                    var getship=bid.split("_",1);

                    if(getship=="ship") {
                        var marker = new google.maps.Marker({
                            position: _latLngFromBuilding(b),
                            map: GMapService.gmap,
                            icon: new google.maps.MarkerImage(
                                'build/images/ship_icon.png',
                                null, /* size is determined at runtime */
                                null, /* origin is 0,0 */
                                null, /* anchor is bottom center of the scaled image */
                                new google.maps.Size(54, 54)),
                            draggable: false
                        });


                        var htmlContent = '<div class="infowindow-scroll-fix">'
                            + '<h5>Ship:</h5>'
                            + '<span>' + b.name + '</span>'
                            + '<h5>Description:</h5>'
                            + '<textarea class="infowindow-text-area"  rows="3" readonly>' + b.description + '</textarea>'
                            + '</div>';




                    }
                    else {
                        var marker = new google.maps.Marker({
                            position: _latLngFromBuilding(b),
                            map: GMapService.gmap,
                            icon: new google.maps.MarkerImage(
                                'build/images/building-icon.png',
                                null, /* size is determined at runtime */
                                null, /* origin is 0,0 */
                                null, /* anchor is bottom center of the scaled image */
                                new google.maps.Size(54, 54)),
                            draggable: false
                        });



                        var htmlContent = '<div class="infowindow-scroll-fix">'
                            + '<h5>Building:</h5>'
                            + '<span>' + b.name + '</span>'
                            + '<h5>Description:</h5>'
                            + '<textarea class="infowindow-text-area"  rows="3" readonly>' + b.description + '</textarea>'
                            + '</div>';

                    }



                    marker.infoContent = htmlContent;
                    marker.building = b;

                    $scope.myBuildingsHashT[b.buid] = {
                        marker: marker,
                        model: b
                    };

                    google.maps.event.addListener(marker, 'click', function () {
                        infowindow.setContent(this.infoContent);
                        infowindow.open(GMapService.gmap, this);
                        var self = this;
                        // $scope.$apply(function () {
                        $scope.anyService.selectedBuilding = self.building;
                        var bid=self.building.buid;
                        var getship=bid.split("_",1);


                        if(getship=="ship") {
                            if($scope.moveShip==0){
                                $scope.moveShip=1;
                                console.log("stop");
                            }
                            else {
                                $scope.moveShip=0;
                            }

                            //ship here


                            function myFunction(){
                                var b = $scope.myBuildingsHashT[bid];

                                var m = b.marker;
                                $scope.laship=parseFloat($scope.laship) + 0.0001;
                                $scope.loship=parseFloat($scope.loship) + 0.0001;
                                var myLatLng = {lat: parseFloat($scope.laship), lng: parseFloat($scope.loship)};

                                m.setPosition(myLatLng);

                                console.log("its work");



                            }

                            if ($scope.moveShip==0) {
                                clearInterval($scope.ShipInterval);
                                var model=$scope.myBuildingsHashT[bid];
                                console.log($scope.myBuildingsHashT[bid]);
                                console.log(model.model.coordinates_lat);

                                var myLatLng = {lat: parseFloat(model.model.coordinates_lat), lng: parseFloat(model.model.coordinates_lon)};
                                console.log(myLatLng);
                                model.marker.setPosition(myLatLng);
                                // console.log(b);
                            }
                            else{
                                myFunction();

                                $scope.ShipInterval=setInterval(function(){
                                    myFunction()}, 10000);

                            }


                            // Open a new connection, using the GET request on the URL endpoint
                            // request.open('GET', 'ship.json', true);
                            // request.send(null);
                            // request.onreadystatechange = function(){

                            //     //check if the status is 200(means everything is okay)
                            //     if (this.status === 200&& this.readyState==4) {
                            //         //return server response as an object with JSON.parse
                            //
                            //
                            //          json=JSON.parse(request.responseText);
                            //         console.log(json[0].LAT);



                            // var json=JSON.parse(request.responseText);
                            //
                            // console.log(request.responseText);
                            //  var myLatLng = {lat: parseFloat(json[0].LAT), lng:parseFloat( json[0].LON)};






                            // console.log(JSON.parse(this.request));
                            // }
                            // }

                            // Send request











                            // }

                            // for (var b in $scope.myBuildingsHashT) {
                            //         $scope.myBuildingsHashT[b].marker.setMap();
                            //
                            // }

                        }


                        // });



                    });

                    console.log("dfgg ");
                }

                // using the latest building form localStorage
                if (localStoredBuildingIndex >= 0) {
                    $scope.anyService.selectedBuilding = $scope.myBuildings[localStoredBuildingIndex];
                } else if ($scope.myBuildings[0]) {
                    $scope.anyService.selectedBuilding = $scope.myBuildings[0];
                }

                // _suc('Successfully fetched buildings.');
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err('Something went wrong while fetching buildings.');
            }
        );
    };

    $scope.addNewBuilding = function (id) {

        if ($scope.myMarkers[id] && $scope.myMarkers[id].marker) {

            var building = $scope.myMarkers[id].model;

            // set owner id
            building.owner_id = $scope.owner_id;

            if (!building.owner_id) {
                _err("Could not authorize user. Please refresh.");
                return;
            }


            building.coordinates_lat = String($scope.myMarkers[id].marker.position.lat());
            building.coordinates_lon = String($scope.myMarkers[id].marker.position.lng());

            if (building.coordinates_lat === undefined || building.coordinates_lat === null) {
                _err("Invalid building latitude.");
                return;
            }

            if (building.coordinates_lon === undefined || building.coordinates_lon === null) {
                _err("Invalid building longitude.");
                return;
            }

            if (building.is_published === true) {
                building.is_published = "true";
            } else {
                building.is_published = "false";
            }

            if (!building.description) {
                building.description = "-";
            }

            if (building.owner_id && building.name && building.description && building.is_published && building.url && building.address) {
                var promise ;
                var shipboolean;
                if (shipselect==0) {
                    promise = $scope.anyAPI.addBuilding(building);
                    shipboolean=1;
                }
                else{
                    promise = $scope.anyAPI.addShip(building)
                    shipboolean=0;
                }
                promise.then(
                    function (resp) {
                        // on success
                        var data = resp.data;
                        console.log("new buid: " + data.buid);
                        building.buid = data.buid;

                        if (building.is_published === 'true' || building.is_published == true) {
                            building.is_published = true;
                        } else {
                            building.is_published = false;
                        }

                        // insert the newly created building inside the loadedBuildings
                        if(shipselect==0) {
                            $scope.myBuildings.push(building);
                            $scope.anyService.selectedBuilding = $scope.myBuildings[$scope.myBuildings.length - 1];

                        }
                        else{
                            $scope.myShips.push(building);
                            // $scope.anyService.selectedBuilding = $scope.myBuildings[$scope.myBuildings.length - 1];


                        }

                        $scope.myMarkers[id].marker.setDraggable(false);

                        $scope.myBuildingsHashT[building.buid] = {
                            marker: $scope.myMarkers[id].marker,
                            model: building
                        };

                        if ($scope.myMarkers[id].infowindow) {
                            $scope.myMarkers[id].infowindow.setContent($scope.myMarkers[id].marker.tpl2[0]);
                            $scope.myMarkers[id].infowindow.close();
                        }
                        if(shipboolean){

                            _suc("Building added successfully.");
                        }
                        else {
                            _suc("Ship added successfully.");
                        }
                    },
                    function (resp) {
                        // on error
                        var data = resp.data;
                        _err("Something went wrong while adding the building. " + data.message);
                    }
                );


            } else {
                _err("Some required fields are missing.");
            }
        }
    };

    $scope.deleteBuilding = function () {

        var b = $scope.anyService.getBuilding();

        var reqObj = $scope.creds;

        if (!$scope.owner_id) {
            _err("Could not identify user. Please refresh and sign in again.");
            return;
        }

        reqObj.owner_id = $scope.owner_id;

        console.log( reqObj.owner_id);

        if (!b || !b.buid) {
            _err("No building selected for deletion.");
            return;
        }

        reqObj.buid = b.buid;

        var promise = $scope.anyAPI.deleteBuilding(reqObj);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                console.log("building deleted: ", b);

                // delete the building from the loadedBuildings
                $scope.myBuildingsHashT[b.buid].marker.setMap(null);
                delete $scope.myBuildingsHashT[b.buid];

                var bs = $scope.myBuildings;
                var sz = bs.length;
                for (var i = 0; i < sz; i++) {
                    if (bs[i].buid == b.buid) {
                        bs.splice(i, 1);
                        break;
                    }
                }

                // update the selected building
                if ($scope.myBuildings && $scope.myBuildings.length > 0) {
                    $scope.anyService.selectedBuilding = $scope.myBuildings[0];
                }

                $scope.setCrudTabSelected(1);

                _suc("Successfully deleted building.");
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err("Something went wrong. It's likely that everything related to the building is deleted but please refresh to make sure or try again.");
            }
        );

    };


    //lsolea01

    $scope.deleteRadiomaps = function () {

        var jsonReq = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber()};

        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;

        var promise = $scope.anyAPI.deleteRadiomaps(jsonReq);

        promise.then(
            function (resp) {

                _suc("Successfully Delete floor radiomap")


            },

            function (resp) {
                // on error
                var data = resp.data;
                _err("Something went wrong.The file is not exist ");
            }
        );


    };


    $scope.updateBuilding = function () {
        var b = $scope.anyService.getBuilding();

        if (LPUtils.isNullOrUndefined(b) || LPUtils.isNullOrUndefined(b.buid)) {
            _err("No building selected found.");
            return;
        }

        var reqObj = {};

        // from controlBarController
        reqObj = $scope.creds;
        if (!$scope.owner_id) {
            _err("Could not identify user. Please refresh and sign in again.");
            return;
        }

        reqObj.owner_id = $scope.owner_id;

        reqObj.buid = b.buid;

        if (b.description) {
            reqObj.description = b.description;
        }

        if (b.name) {
            reqObj.name = b.name;
        }

        if (b.is_published === true || b.is_published == "true") {
            reqObj.is_published = "true";
        } else {
            reqObj.is_published = "false";
        }

        if (b.bucode) {
            reqObj.bucode = b.bucode;
        }

        var marker = $scope.myBuildingsHashT[b.buid].marker;
        if (marker) {
            var latLng = marker.position;
            if (latLng && latLng.lat() && latLng.lng()) {
                reqObj.coordinates_lat = String(latLng.lat());
                reqObj.coordinates_lon = String(latLng.lng());
            }
        }

        var promise = $scope.anyAPI.updateBuilding(reqObj);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                if (b.is_published === 'true' || b.is_published == true) {
                    b.is_published = true;
                } else {
                    b.is_published = false;
                }

                _suc("Successfully updated building.")
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err("Something went wrong while updating building. " + data.message);
            }
        );

    };

    $scope.fetchAllCampus = function () {
        var jsonReq = {};
        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;
        jsonReq.owner_id = $scope.owner_id;
        $scope.myCampus = [];
        if (!jsonReq.owner_id) {
            _err("Could nor authorize user. Please refresh.");
            return;
        }

        var promise = $scope.anyAPI.allCampus(jsonReq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;
                //var bs = JSON.parse( data.buildings );
                $scope.myCampus = data.buildingsets;
                var localStoredCampusIndex = -1;
                var localStoredCampusId = undefined;
                if (typeof(Storage) !== "undefined" && localStorage && localStorage.getItem('lastCampus')) {
                    localStoredCampusId = localStorage.getItem('lastCampus');
                }

                for (var i = 0; i < $scope.myCampus.length; i++) {

                    var b = $scope.myCampus[i];

                    if (localStoredCampusId && localStoredCampusId === b.cuid) {
                        localStoredCampusIndex = i;
                    }
                }

                // using the latest building set form localStorage
                if (localStoredCampusIndex >= 0) {
                    $scope.anyService.selectedCampus = $scope.myCampus[localStoredCampusIndex];
                } else if ($scope.myCampus[0]) {
                    $scope.anyService.selectedCampus = $scope.myCampus[0];
                }
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err('Something went wrong while fetching buildings.');
            }
        );
    };

    $scope.updateCampus = function () {
        var b = $scope.anyService.getCampus();

        if (LPUtils.isNullOrUndefined(b) || LPUtils.isNullOrUndefined(b.cuid)) {
            _err("No campus selected found.");
            return;
        }

        var reqObj = {};

        // from controlBarController
        reqObj = $scope.creds;
        if (!$scope.owner_id) {
            _err("Could not identify user. Please refresh and sign in again.");
            return;
        }

        reqObj.owner_id = $scope.owner_id;

        reqObj.cuid = b.cuid;

        if (b.description) {
            reqObj.description = b.description;
        }

        if (b.name) {
            reqObj.name = b.name;
        }

        if (b.newcuid) {
            reqObj.newcuid = b.newcuid;
        }

        var sz = $scope.example9modeledit.length;

        if (sz == 0) {
            _err("No buildings selected.");
            return;
        }
        var buids = "\"buids\":[";
        for (var i = sz - 1; i > 0; i--) {
            buids = buids + "\"" + $scope.example9modeledit[i].id + "\",";
        }
        buids = buids + "\"" + $scope.example9modeledit[0].id + "\"]";

        reqObj.greeklish = document.getElementById("Greeklish-OnOffedit").checked;

        reqObj.buids = buids;


        var promise = $scope.anyAPI.updateCampus(reqObj);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;
                document.getElementById("CampusIDeditnew").value = "";
                _suc("Successfully updated campus.")
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err("Something went wrong while updating campus. " + data.message);
            }
        );

    };

    $scope.deleteCampus = function () {

        var b = $scope.anyService.getCampus();

        var reqObj = $scope.creds;

        if (!$scope.owner_id) {
            _err("Could not identify user. Please refresh and sign in again.");
            return;
        }

        reqObj.owner_id = $scope.owner_id;

        if (!b || !b.cuid) {
            _err("No Campus selected for deletion.");
            return;
        }

        reqObj.cuid = b.cuid;

        var promise = $scope.anyAPI.deleteCampus(reqObj);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                console.log("campus deleted: ", b);


                var bs = $scope.myCampus;
                var sz = bs.length;
                for (var i = 0; i < sz; i++) {
                    if (bs[i].cuid == b.cuid) {
                        bs.splice(i, 1);
                        break;
                    }
                }

                // update the selected building
                if ($scope.myCampus && $scope.myCampus.length > 0) {
                    $scope.anyService.selectedCampus = $scope.myCampus[0];
                }
                else if ($scope.myCampus.length == 0) {
                    $scope.anyService.selectedCampus = undefined;
                }

                $scope.setCrudTabSelected(1);

                _suc("Successfully deleted campus.");
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err("Something went wrong. It's likely that everything related to the campus is deleted but please refresh to make sure or try again.");
            }
        );

    };

    $scope.addCampus = function () {

        var name_element = document.getElementById("CampusName");
        var name = "\"name\":\"" + name_element.value + "\"";

        if (document.getElementById("CampusDescription").value.localeCompare("") == 0) {
            document.getElementById("CampusDescription").value = "-";
        }

        var des = document.getElementById("CampusDescription");
        var des = "\"description\":\"" + des.value + "\"";

        var mycuid = document.getElementById("CampusID");
        var mycuid = "\"cuid\":\"" + mycuid.value + "\"";

        var greeklish = document.getElementById("Greeklish-OnOff").checked;
        greeklish = "\"greeklish\":\"" + greeklish + "\"";
        var sz = $scope.example9model.length;

        if (sz == 0) {
            _err("No buildings selected.");
            return;
        }
        var buids = "\"buids\":[";
        for (var i = sz - 1; i > 0; i--) {
            buids = buids + "\"" + $scope.example9model[i].id + "\",";
        }
        buids = buids + "\"" + $scope.example9model[0].id + "\"]";

        var jreq = "{" + greeklish + "," + buids + "," + mycuid + "," + des + "," + name + ",\"owner_id\":\"" + $scope.owner_id + "\",\"access_token\":\"" + $scope.gAuth.access_token + "\"}";
        //alert(document.getElementById("Greeklish-OnOff").checked);
        var promise = $scope.anyAPI.addBuildingSet(jreq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;
                var new_campus = {};
                new_campus.name = document.getElementById("CampusName").value;
                new_campus.buids = buids.buids;
                new_campus.description = document.getElementById("CampusDescription").value;
                new_campus.cuid = data.cuid;
                $scope.myCampus.push(new_campus);
                $scope.anyService.selectedCampus = $scope.myCampus[$scope.myCampus.length - 1];
                _suc("Successfully added campus.");

                function S4() {
                    return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
                }

                var guid = (S4() + S4() + "-" + S4() + "-4" + S4().substr(0, 3) + "-" + S4() + "-" + S4() + S4() + S4()).toLowerCase();
                var d = new Date();
                document.getElementById("CampusID").value = "cuid_" + guid + "_" + d.getTime();
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err("Something went wrong while adding the building. " + data.message);
            }
        );

    };

    var overlay = new google.maps.OverlayView();
    overlay.draw = function () {
    };
    overlay.setMap(GMapService.gmap);

    $("#draggable-building").draggable({
        helper: 'clone',
        stop: function (e) {
            var point = new google.maps.Point(e.pageX, e.pageY);
            var ll = overlay.getProjection().fromContainerPixelToLatLng(point);
            $scope.placeMarker(ll);
            shipselect=0;
        }
    });

// ship drag
    $("#draggable-ship").draggable({

        helper: 'clone',
        stop: function (e) {
            var point = new google.maps.Point(e.pageX, e.pageY);
            var ll = overlay.getProjection().fromContainerPixelToLatLng(point);
            $scope.placeMarkership(ll);
            shipselect=1;
        }
    });

    $scope.placeMarkership = function (location) {

        var prevMarker = $scope.myMarkers[$scope.myMarkerId - 1];

        if (prevMarker && prevMarker.marker && prevMarker.marker.getMap() && prevMarker.marker.getDraggable()) {
            // TODO: alert for already pending building.
            console.log('there is a building pending, please add 1 at a time');
            return;
        }

        var marker = new google.maps.Marker({
            position: location,
            map: GMapService.gmap,
            icon: 'build/images/ship_icon.png',
            draggable: true
        });

        var infowindow = new google.maps.InfoWindow({
            content: '-',
            maxWidth: 500
        });

        $scope.$apply(function () {
            marker.myId = $scope.myMarkerId;
            $scope.myMarkers[marker.myId] = {};
            $scope.myMarkers[marker.myId].model = {
                description: "",
                name: undefined,
                is_published: true,
                address: "-",
                url: "-",
                bucode: ""
            };
            $scope.myMarkers[marker.myId].marker = marker;
            $scope.myMarkers[marker.myId].infowindow = infowindow;
            $scope.myMarkerId++;
        });

        var htmlContent = '<form name="buildingForm" class="infowindow-scroll-fix">'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.bucode" id="building-code" type="text" class="form-control" placeholder="Building Code (Optional)"/>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.name" id="building-name" type="text" class="form-control" placeholder="Building Name *"/>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<textarea ng-model="myMarkers[' + marker.myId + '].model.description" id="building-description" type="text" class="form-control" placeholder="Building Description (Optional)"></textarea>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.is_published" id="building-published" type="checkbox"><span> Make building public to view.</span>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '</fieldset class="form-group">'
            + '<div style="text-align: center;">'
            + '<fieldset class="form-group" style="display: inline-block; width: 75%;">'
            + '<button type="submit" class="btn btn-success add-any-button" ng-click="addNewBuilding(' + marker.myId + ')">'
            + '<span class="glyphicon glyphicon-plus"></span> Add'
            + '</button>'
            + '</fieldset>'
            + '<fieldset class="form-group" style="display: inline-block;width: 23%;">'
            + '<button class="btn btn-danger add-any-button" style="margin-left:2px" ng-click="deleteTempBuilding(' + marker.myId + ')"><span class="glyphicon glyphicon-remove"></span>'
            + '</button>'
            + '</fieldset>'
            + '</div>'
            + '</form>';

        var htmlContent2 = '<div class="infowindow-scroll-fix">'
            + '<h5 style="margin: 0">Building:</h5>'
            + '<span>{{myMarkers[' + marker.myId + '].model.name}}</span>'
            + '<h5 style="margin: 8px 0 0 0">Description:</h5>'
            + '<span>{{myMarkers[' + marker.myId + '].model.description}}</span>'
            + '</div>';

        var tpl = $compile(htmlContent)($scope);
        marker.tpl2 = $compile(htmlContent2)($scope);

        infowindow.setContent(tpl[0]);
        infowindow.open(GMapService.gmap, marker);

        google.maps.event.addListener(marker, 'click', function () {
            if (!infowindow.getMap()) {
                infowindow.open(GMapService.gmap, marker);
            }
        });
    };


    $scope.placeMarker = function (location) {

        var prevMarker = $scope.myMarkers[$scope.myMarkerId - 1];

        if (prevMarker && prevMarker.marker && prevMarker.marker.getMap() && prevMarker.marker.getDraggable()) {
            // TODO: alert for already pending building.
            console.log('there is a building pending, please add 1 at a time');
            return;
        }

        var marker = new google.maps.Marker({
            position: location,
            map: GMapService.gmap,
            icon: 'build/images/building-icon.png',
            draggable: true
        });

        var infowindow = new google.maps.InfoWindow({
            content: '-',
            maxWidth: 500
        });

        $scope.$apply(function () {
            marker.myId = $scope.myMarkerId;
            $scope.myMarkers[marker.myId] = {};
            $scope.myMarkers[marker.myId].model = {
                description: "",
                name: undefined,
                is_published: true,
                address: "-",
                url: "-",
                bucode: ""
            };
            $scope.myMarkers[marker.myId].marker = marker;
            $scope.myMarkers[marker.myId].infowindow = infowindow;
            $scope.myMarkerId++;
        });

        var htmlContent = '<form name="buildingForm" class="infowindow-scroll-fix">'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.bucode" id="building-code" type="text" class="form-control" placeholder="Building Code (Optional)"/>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.name" id="building-name" type="text" class="form-control" placeholder="Building Name *"/>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<textarea ng-model="myMarkers[' + marker.myId + '].model.description" id="building-description" type="text" class="form-control" placeholder="Building Description (Optional)"></textarea>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.is_published" id="building-published" type="checkbox"><span> Make building public to view.</span>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '</fieldset class="form-group">'
            + '<div style="text-align: center;">'
            + '<fieldset class="form-group" style="display: inline-block; width: 75%;">'
            + '<button type="submit" class="btn btn-success add-any-button" ng-click="addNewBuilding(' + marker.myId + ')">'
            + '<span class="glyphicon glyphicon-plus"></span> Add'
            + '</button>'
            + '</fieldset>'
            + '<fieldset class="form-group" style="display: inline-block;width: 23%;">'
            + '<button class="btn btn-danger add-any-button" style="margin-left:2px" ng-click="deleteTempBuilding(' + marker.myId + ')"><span class="glyphicon glyphicon-remove"></span>'
            + '</button>'
            + '</fieldset>'
            + '</div>'
            + '</form>';

        var htmlContent2 = '<div class="infowindow-scroll-fix">'
            + '<h5 style="margin: 0">Building:</h5>'
            + '<span>{{myMarkers[' + marker.myId + '].model.name}}</span>'
            + '<h5 style="margin: 8px 0 0 0">Description:</h5>'
            + '<span>{{myMarkers[' + marker.myId + '].model.description}}</span>'
            + '</div>';

        var tpl = $compile(htmlContent)($scope);
        marker.tpl2 = $compile(htmlContent2)($scope);

        infowindow.setContent(tpl[0]);
        infowindow.open(GMapService.gmap, marker);

        google.maps.event.addListener(marker, 'click', function () {
            if (!infowindow.getMap()) {
                infowindow.open(GMapService.gmap, marker);
            }
        });
    };

    $scope.deleteTempBuilding = function (id) {
        var myMarker = $scope.myMarkers[id];
        if (myMarker && myMarker.marker) {
            myMarker.marker.setMap(null);
        }
    };

    /**
     * building {
     *  name:
     *  description:
     *  coordinates_lat:
     *  coordinates_lon:
     *  floors: [
     *      0: {
     *          pois: [
     *              name:
     *              description:
     *              pois_type:
     *              is_building_entrance:
     *              coordinates_lat:
     *              coordinates_lon:
     *          ]
     *      }
     *  ];
     * }
     */
    $scope.exportBuildingToJson = function () {
        var result = {
            building: {
                floors: []
            }
        };

        var building = $scope.anyService.selectedBuilding;
        if (LPUtils.isNullOrUndefined(building)) {
            _err('No building selected');
            return;
        }
        result.building.buid = building.buid;
        result.building.name = building.name;
        result.building.description = building.description;
        result.building.coordinates_lat = building.coordinates_lat;
        result.building.coordinates_lon = building.coordinates_lon;

        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = building.buid;

        var count = 0;

        var promise = AnyplaceAPIService.allBuildingFloors(jsonReq);
        promise.then(
            function (resp) {
                var floors = resp.data.floors;

                var resFloors = [];

                if (floors) {
                    for (var i = 0; i < floors.length; i++) {

                        (function (jreq) {
                            var promise = AnyplaceAPIService.retrievePoisByBuildingFloor(jreq);
                            promise.then(
                                function (resp) {
                                    var data = resp.data;

                                    var poisArray = data.pois;

                                    if (poisArray) {

                                        var flPois = [];

                                        var fNo = poisArray[0].floor_number;

                                        for (var j = 0; j < poisArray.length; j++) {
                                            var sPoi = poisArray[j];

                                            if (sPoi.pois_type == "None") {
                                                continue;
                                            }
                                            if (sPoi.overwrite) {
                                                var tmp = {
                                                    name: sPoi.name,
                                                    description: sPoi.description,
                                                    puid: sPoi.puid,
                                                    pois_type: sPoi.pois_type,
                                                    coordinates_lat: sPoi.coordinates_lat,
                                                    coordinates_lon: sPoi.coordinates_lon,
                                                    overwrite: sPoi.overwrite
                                                };
                                            }
                                            else {
                                                var tmp = {
                                                    name: sPoi.name,
                                                    description: sPoi.description,
                                                    puid: sPoi.puid,
                                                    pois_type: sPoi.pois_type,
                                                    coordinates_lat: sPoi.coordinates_lat,
                                                    coordinates_lon: sPoi.coordinates_lon,
                                                    overwrite: "false"
                                                };
                                            }
                                            flPois.push(tmp);
                                        }

                                        resFloors.push(
                                            {
                                                floor_number: fNo,
                                                pois: flPois
                                            }
                                        );

                                        count++;
                                        if (count === floors.length) {
                                            result.building.floors = resFloors;
                                            _suc('Successfully exported ' + building.name + ' to JSON.');
                                            var blob = new Blob([JSON.stringify(result, null, 4)], {type: "text/plain;charset=utf-8"});
                                            saveAs(blob, building.name.toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
                                        }

                                    }
                                },
                                function (resp) {
                                    var data = resp.data;
                                    console.log(data.message);
                                });
                        }({
                            buid: building.buid,
                            floor_number: floors[i].floor_number
                        }));
                    }
                }
            },
            function (resp) {
                // TODO: alert failure
                console.log(resp.data.message);
            }
        );
    };

    /**
     * campus {
   "buids": [
   ],
   "description": "",
   "name": "",
   "owner_id": "",
   "access_token": "",
   "cuid": ""
    }
     * }
     */
    $scope.exportCampusToJson = function () {
        var result = {
            campus: {
                buids: []
            }
        };

        var campus = $scope.anyService.selectedCampus;
        if (LPUtils.isNullOrUndefined(campus)) {
            _err('No campus selected');
            return;
        }

        result.campus.name = campus.name;
        result.campus.description = campus.description;
        result.campus.buids = campus.buids;

        _suc('Successfully exported ' + campus.name + ' to JSON.');

        var blob = new Blob([JSON.stringify(result, null, 4)], {type: "text/plain;charset=utf-8"});
        saveAs(blob, campus.name.toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
    };

    function readSingleFile(e) {
        var file = e.target.files[0];
        if (!file) {
            return;
        }
        var reader = new FileReader();
        reader.onload = function (e) {
            var contents = e.target.result;
            $scope.fileToUpload = contents;
        };
        reader.readAsText(file);
    }

    document.getElementById('file-input')
        .addEventListener('change', readSingleFile, false);

    $scope.anyService.downloadlogfile = false;

    $scope.importBuildingFromJson = function () {

        $scope.anyService.progress = 0;
        var i, j, count = 0, countok = 0;
        if ($scope.fileToUpload == "") {
            _err("Something went wrong no file selected");
        }
        var obj = JSON.parse($scope.fileToUpload);
        for (i = 0; i < obj.building.floors.length; i++) {
            for (j = 0; j < obj.building.floors[i].pois.length; j++) {
                if (obj.building.floors[i].pois[j].overwrite == "true") {
                    count++;
                }
            }
        }
        if (count == 0) {
            _err("Something went wrong no pois to update");
        }

        for (i = 0; i < obj.building.floors.length; i++) {
            for (j = 0; j < obj.building.floors[i].pois.length; j++) {
                if (obj.building.floors[i].pois[j].overwrite == "true") {
                    countok++;
                    $scope.updatePoifromFile(obj.building.floors[i].pois[j].puid,
                        obj.building.floors[i].pois[j].coordinates_lat,
                        obj.building.floors[i].pois[j].coordinates_lon,
                        obj.building.floors[i].pois[j].name,
                        obj.building.floors[i].pois[j].description,
                        obj.building.floors[i].pois[j].pois_type,
                        obj.building.floors[i].pois[j].overwrite,
                        obj.building.buid, i, j, count, countok,
                        obj.building.name
                    );
                }
            }
        }
    }

    $scope.allpois = {};
    $scope.pois = {};

    $scope.importBuildingFromExcel = function (oEvent) {
        $scope.anyService.progress = 0;
        // Get The File From The Input
        var oFile = document.getElementById('my_file_input').files[0];
        $scope.fileToUpload = oFile.name;
        // Create A File Reader HTML5
        var reader = new FileReader();
        // Ready The Event For When A File Gets Selected
        reader.onload = function (e) {
            var data = e.target.result;
            var cfb = XLS.CFB.read(data, {type: 'binary'});
            var wb = XLS.parse_xlscfb(cfb);
            // Loop Over Each Sheet
            wb.SheetNames.forEach(function (sheetName) {
                // Obtain The Current Row As CSV
                var oJS = XLS.utils.sheet_to_row_object_array(wb.Sheets[sheetName]);
                var last_buid = "";
                $scope.uploadloop(0, oJS, last_buid);
            });
        };

        // Tell JS To Start Reading The File.. You could delay this if desired
        reader.readAsBinaryString(oFile);
    }

    $scope.uploadloop = function (potition, oJS, last_buid) {

        for (var i = potition; i < oJS.length; i++) {
            if (last_buid != oJS[i].buid && oJS[i].buid != undefined) {
                $scope.pois = {};
                last_buid = oJS[i].buid;
                $scope.fetchAllPoisForBuilding(oJS[i].buid, oJS, i, last_buid);
                return;
            }
            var description = "";

            if (oJS[i].des3 != undefined && oJS[i].des3 != "" && oJS[i].des3 != null) {
                if (oJS[i].des4 != undefined && oJS[i].des4 != "" && oJS[i].des4 != null) {
                    description = description + oJS[i].des3 + " " + oJS[i].des4;
                }
                else {
                    description = description + oJS[i].des3;
                }
            }
            else {
                if (oJS[i].des4 != undefined && oJS[i].des4 != "" && oJS[i].des4 != null) {
                    description = description + oJS[i].des4;
                }
            }

            if (oJS[i].des1 != undefined && oJS[i].des1 != "" && oJS[i].des1 != null) {
                if (description != "") {
                    description = description + "\n" + oJS[i].des1;
                }
                else {
                    description = description + oJS[i].des1;
                }
            }

            if (oJS[i].des2 != undefined && oJS[i].des2 != "" && oJS[i].des2 != null) {
                if (description != "") {
                    description = description + "\n" + oJS[i].des2;
                }
                else {
                    description = description + oJS[i].des2;
                }
            }

            $scope.anyService.progress = (i / (oJS.length - 1)) * 100;
            if ((oJS.length - 1) == 0) $scope.anyService.progress = 100;

            if ($scope.pois[oJS[i].name]) {
                $scope.updatePoifromExcel($scope.pois[oJS[i].name].puid,
                    $scope.pois[oJS[i].name].coordinates_lat,
                    $scope.pois[oJS[i].name].coordinates_lon,
                    $scope.pois[oJS[i].name].is_building_entrance,
                    oJS[i].name,
                    description,
                    $scope.pois[oJS[i].name].pois_type,
                    $scope.pois[oJS[i].name].overwrite,
                    $scope.pois[oJS[i].name].buid,
                    ""
                );
            }
            else {
                $scope.updatePoifromExcel("",
                    "",
                    "",
                    "",
                    oJS[i].name,
                    description,
                    "",
                    "",
                    "",
                    ""
                );
            }

        }
    };

    $scope.fetchAllPoisForBuilding = function (building, oJS, position, last_buid) {
        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = building;

        var promise = AnyplaceAPIService.retrievePoisByBuilding(jsonReq);
        promise.then(
            function (resp) {
                var data = resp.data;

                $scope.myPois = data.pois;

                var sz = $scope.myPois.length;
                for (var i = sz - 1; i >= 0; i--) {
                    var name = $scope.myPois[i].name;
                    $scope.pois[name] = $scope.myPois[i];
                }
                $scope.uploadloop(position, oJS, last_buid);
            },
            function (resp) {
                var data = resp.data;
                _err("Something went wrong while fetching POIs");
            }
        );
    };
    $scope.logfile = {
        pois: []
    };
    $scope.updatePoifromExcel = function (id, lat, lng, building_entrance, nm, des, ptype, ovwrite, bid, buildingname) {

        var obj = {};

        obj.username = $scope.creds.username;
        obj.password = $scope.creds.password;
        obj.owner_id = $scope.owner_id;

        obj.coordinates_lat = lat;
        obj.coordinates_lon = lng;

        obj.is_building_entrance = building_entrance;
        obj.name = nm;
        obj.puid = id;
        obj.description = des;
        obj.pois_type = ptype;
        obj.overwrite = "false";
        obj.buid = bid;
        // make the request at AnyplaceAPI
        var promise = $scope.anyAPI.updatePois(obj);
        promise.then(
            function (resp) {
                var data = resp.data;
                if ($scope.anyService.progress == 100) {

                    if ($scope.anyService.downloadlogfile) {
                        $scope.anyService.downloadlogfile = false;
                        _suc("Successfully updated POIs.A log file will be downloaded");
                        var blob = new Blob([JSON.stringify($scope.logfile, null, 4)], {type: "text/plain;charset=utf-8"});
                        saveAs(blob, ($scope.fileToUpload + "-log_file").toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                        }, 2500);
                    }
                    else {
                        _suc("Successfully updated POIs.");
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                            location.reload();
                        }, 2500);
                    }
                }
            },
            function (resp) {
                var data = resp.data;
                $scope.logfile.pois.push({
                    "name": nm,
                    "puid": id,
                    "description": des,
                    "status": "Something went wrong while updating POI."
                });
                $scope.anyService.downloadlogfile = true;
                if ($scope.anyService.progress == 100) {
                    $scope.anyService.downloadlogfile = false;
                    _suc("Successfully updated POIs.A log file will be downloaded");
                    var blob = new Blob([JSON.stringify($scope.logfile, null, 4)], {type: "text/plain;charset=utf-8"});
                    saveAs(blob, ($scope.fileToUpload + "-log_file").toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
                    $scope.logfile.pois = [];

                    setTimeout(function () {
                        $scope.anyService.progress = undefined;
                    }, 2500);
                }
            }
        );
    };


    $scope.updatePoifromFile = function (id, lat, lng, nm, des, ptype, ovwrite, bid, i, j, count, countok, buildingname) {

        var obj = {};

        obj.username = $scope.creds.username;
        obj.password = $scope.creds.password;
        obj.owner_id = $scope.owner_id;

        obj.coordinates_lat = lat;
        obj.coordinates_lon = lng;

        obj.name = nm;
        obj.puid = id;
        obj.description = des;
        obj.pois_type = ptype;
        obj.overwrite = "false";
        obj.buid = bid;
        // make the request at AnyplaceAPI
        var promise = $scope.anyAPI.updatePois(obj);
        promise.then(
            function (resp) {
                var data = resp.data;
                $scope.anyService.progress = countok / count * 100;
                if ($scope.anyService.progress == 100) {
                    if ($scope.anyService.downloadlogfile) {
                        _suc("Successfully updated POIs.A log file will be downloaded");
                        var blob = new Blob([JSON.stringify($scope.logfile, null, 4)], {type: "text/plain;charset=utf-8"});
                        saveAs(blob, (buildingname + "-log_file").toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                        }, 2500);
                    }
                    else {
                        _suc("Successfully updated POIs.");
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                            location.reload();
                        }, 2500);
                    }
                }
            },
            function (resp) {
                var data = resp.data;

                $scope.logfile.pois.push({
                    "name": nm,
                    "puid": id,
                    "description": des,
                    "status": "Something went wrong while updating POI."
                });

                $scope.anyService.downloadlogfile = true;
                $scope.anyService.progress = countok / count * 100;
                if ($scope.anyService.progress == 100) {

                    if ($scope.anyService.downloadlogfile) {
                        _suc("Successfully updated POIs.A log file will be downloaded");
                        var blob = new Blob([JSON.stringify($scope.logfile, null, 4)], {type: "text/plain;charset=utf-8"});
                        saveAs(blob, (buildingname + "-log_file").toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
                        $scope.logfile.pois = [];
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                        }, 2500);
                    }
                    else {
                        _suc("Successfully updated POIs.");
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                            location.reload();
                        }, 2500);
                    }
                }
            }
        );
    };

    $scope.Poisresult = {
        building: {
            floors: []
        }
    };

    $scope.Connectionsresult = {
        building: {}
    };

    $scope.zip = new JSZip();
    $scope.DownloadBackup = function () {

        var b = $scope.anyService.selectedBuilding;
        var xFloors = [];
        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = b.buid;
        $scope.anyService.progress = 0;
        var promise = AnyplaceAPIService.allBuildingFloors(jsonReq);
        promise.then(
            function (resp) {
                xFloors = resp.data.floors;
                var floor = 0;
                var floor_number = "";
                for (var i = 0; i < xFloors.length; i++) {
                    if (i == 0) {
                        floor_number = xFloors[i].floor_number;
                    }
                    else {
                        floor_number = floor_number + " " + xFloors[i].floor_number;
                    }
                }
                $scope.anyService.progress = 10;
                var buid = b.buid;
                var jsonReq2 = AnyplaceService.jsonReq;
                var promise2 = AnyplaceAPIService.downloadFloorPlanAll(jsonReq2, buid, floor_number);
                promise2.then(
                    function (resp) {
                        // on success
                        var data = resp.data;
                        var img = $scope.zip.folder("floor_plans");
                        for (var si = 0; si < data.all_floors.length; si++) {
                            if (data.all_floors[si] != "") {
                                img.file(xFloors[si].floor_number + ".png", data.all_floors[si], {base64: true});
                            }
                        }
                        $scope.anyService.progress = 25;
                        var jsonReq3 = AnyplaceService.jsonReq;
                        jsonReq3.buid = buid;
                        jsonReq3.floor = floor_number;
                        var promise3 = AnyplaceAPIService.getRadioByBuildingFloorAll(jsonReq3);
                        promise3.then(
                            function (resp) {
                                var data2 = resp.data;
                                var logs = $scope.zip.folder("radiomaps");
                                if (data2.rss_log_files) {
                                    var urls = "";
                                    for (var si2 = 0; si2 < data2.rss_log_files.length; si2++) {
                                        logs.file(xFloors[si2].floor_number + "-radiomap.txt", data2.rss_log_files[si2]);
                                    }
                                }
                                $scope.anyService.progress = 70;
                                $scope.exportPoisBuildingToJson();
                            },
                            function (resp) {

                            }
                        );
                    },
                    function (resp) {
                    }
                );
            },
            function (resp) {
                console.log(resp.data.message);
                _err("Something went wrong while fetching all floors");
            }
        );


    }

    $scope.exportPoisBuildingToJson = function () {

        var building = $scope.anyService.selectedBuilding;
        if (LPUtils.isNullOrUndefined(building)) {
            _err('No building selected');
            return;
        }
        $scope.Poisresult.building.buid = building.buid;
        $scope.Poisresult.building.name = building.name;
        $scope.Poisresult.building.description = building.description;
        $scope.Poisresult.building.coordinates_lat = building.coordinates_lat;
        $scope.Poisresult.building.coordinates_lon = building.coordinates_lon;

        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = building.buid;

        var count = 0;

        var promise = AnyplaceAPIService.allBuildingFloors(jsonReq);
        promise.then(
            function (resp) {
                var floors = resp.data.floors;

                var resFloors = [];
                $scope.anyService.progress = 80;
                if (floors) {
                    for (var i = 0; i < floors.length; i++) {

                        (function (jreq) {
                            var promise = AnyplaceAPIService.retrievePoisByBuildingFloor(jreq);
                            promise.then(
                                function (resp) {
                                    var data = resp.data;

                                    var poisArray = data.pois;

                                    if (poisArray) {

                                        var flPois = [];

                                        if (poisArray[0] != undefined) {
                                            var fNo = poisArray[0].floor_number;

                                            for (var j = 0; j < poisArray.length; j++) {
                                                var sPoi = poisArray[j];

                                                if (sPoi.pois_type == "None") {
                                                    continue;
                                                }
                                                if (sPoi.overwrite) {
                                                    var tmp = {
                                                        name: sPoi.name,
                                                        description: sPoi.description,
                                                        puid: sPoi.puid,
                                                        pois_type: sPoi.pois_type,
                                                        coordinates_lat: sPoi.coordinates_lat,
                                                        coordinates_lon: sPoi.coordinates_lon,
                                                        overwrite: sPoi.overwrite
                                                    };
                                                }
                                                else {
                                                    var tmp = {
                                                        name: sPoi.name,
                                                        description: sPoi.description,
                                                        puid: sPoi.puid,
                                                        pois_type: sPoi.pois_type,
                                                        coordinates_lat: sPoi.coordinates_lat,
                                                        coordinates_lon: sPoi.coordinates_lon,
                                                        overwrite: "false"
                                                    };
                                                }

                                                if (sPoi.is_building_entrance == 'true') {
                                                    tmp.is_building_entrance = 'true';
                                                }
                                                else {
                                                    tmp.is_building_entrance = 'false';
                                                }

                                                flPois.push(tmp);
                                            }

                                            resFloors.push(
                                                {
                                                    floor_number: fNo,
                                                    pois: flPois
                                                }
                                            );
                                        }
                                        count++;
                                        if (count === floors.length) {
                                            $scope.Poisresult.building.floors = resFloors;
                                            $scope.zip.file("allpois.json", JSON.stringify($scope.Poisresult, null, 4));
                                            $scope.anyService.progress = 90;
                                            $scope.exportConnectionBuildingToJson();
                                        }

                                    }
                                },
                                function (resp) {
                                    var data = resp.data;
                                    console.log(data.message);
                                });
                        }({
                            buid: building.buid,
                            floor_number: floors[i].floor_number
                        }));
                    }
                }
            },
            function (resp) {
                // TODO: alert failure
                console.log(resp.data.message);
            }
        );
    };

    $scope.exportConnectionBuildingToJson = function () {

        var building = $scope.anyService.selectedBuilding;
        if (LPUtils.isNullOrUndefined(building)) {
            _err('No building selected');
            return;
        }
        $scope.Connectionsresult.building.buid = building.buid;
        $scope.Connectionsresult.building.name = building.name;
        $scope.Connectionsresult.building.description = building.description;
        $scope.Connectionsresult.building.coordinates_lat = building.coordinates_lat;
        $scope.Connectionsresult.building.coordinates_lon = building.coordinates_lon;

        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = building.buid;

        var count = 0;

        var promise = AnyplaceAPIService.allBuildingFloors(jsonReq);
        promise.then(
            function (resp) {
                var floors = resp.data.floors;

                var resFloors = [];

                if (floors) {
                    for (var i = 0; i < floors.length; i++) {

                        (function (jreq) {
                            var promise = AnyplaceAPIService.retrieveConnectionsByBuildingFloor(jreq);
                            promise.then(
                                function (resp) {
                                    $scope.anyService.progress = 100;
                                    var data = resp.data;

                                    var connArray = data.connections;

                                    if (connArray) {

                                        var flConnections = [];

                                        if (connArray[0] != undefined) {

                                            var fNo = connArray[0].floor_a;

                                            for (var j = 0; j < connArray.length; j++) {
                                                var sConnection = connArray[j];

                                                var tmp = {
                                                    name: sConnection.name,
                                                    description: sConnection.description,
                                                    cuid: sConnection.puid,
                                                    weight: sConnection.weight,
                                                    pois_a: sConnection.pois_a,
                                                    pois_b: sConnection.pois_b,
                                                    floor_a: sConnection.floor_a,
                                                    floor_b: sConnection.floor_b,
                                                    is_published: sConnection.is_published,
                                                    buid_a: sConnection.buid_a,
                                                    buid_b: sConnection.buid_b
                                                };

                                                flConnections.push(tmp);
                                            }

                                            resFloors.push(
                                                {
                                                    floor_number: fNo,
                                                    connections: flConnections
                                                }
                                            );
                                        }
                                        count++;

                                        if (count === floors.length) {

                                            $scope.Connectionsresult.building.floors = resFloors;
                                            $scope.zip.file("allconnections.json", JSON.stringify($scope.Connectionsresult, null, 4));

                                            $scope.zip.generateAsync({type: "blob"})
                                                .then(function (content) {
                                                    // see FileSaver.js
                                                    saveAs(content, building.buid + ".zip");
                                                });
                                            $scope.anyService.progress = undefined;
                                        }
                                    }
                                },
                                function (resp) {
                                    var data = resp.data;
                                    console.log(data.message);
                                });
                        }({
                            buid: building.buid,
                            floor_number: floors[i].floor_number
                        }));
                    }
                }
            },
            function (resp) {
                console.log(resp.data.message);
            }
        );
    };

    //set cookies

    $('#dismiss').on('click', function () {
        // set expire date.
        if (typeof(Storage) !== "undefined" && localStorage) {
            localStorage.setItem('dismissClicked', 'YES');
        }

    });

    if (localStorage.getItem('dismissClicked') !== 'YES') {
        function showWelcomeMessage() {
            $('#myModal_Welcome').modal('show');
        }

        window.onload = showWelcomeMessage;
    }

}]);


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
app.controller('ControlBarController', ['$scope', '$rootScope', 'AnyplaceService', 'GMapService', 'AnyplaceAPIService', function ($scope, $rootScope, AnyplaceService, GMapService, AnyplaceAPIService) {

    $scope.anyService = AnyplaceService;
    $scope.gmapService = GMapService;

    $scope.isAuthenticated = false;

    $scope.signInType = "google";

    $scope.gAuth = {};
    $scope.person = undefined;

    $scope.creds = {
        username: 'username',
        password: 'password'
    };

    $scope.owner_id = undefined;
    $scope.displayName = undefined;


    var self = this; //to be able to reference to it in a callback, you could use $scope instead


    $scope.setAuthenticated = function (bool) {
        $scope.isAuthenticated = bool;
    };

    $scope.showFullControls = true;

    $scope.toggleFullControls = function () {
        $scope.showFullControls = !$scope.showFullControls;
    };

    var apiClientLoaded = function () {
        gapi.client.plus.people.get({userId: 'me'}).execute(handleEmailResponse);
    };

    var handleEmailResponse = function (resp) {
        $scope.personLookUp(resp);
    };

    $scope.showGoogleID = function () {
        if (!$scope.person) {
            return;
        }
        AnyplaceService.addAlert('success', 'Your Google ID is: ' + $scope.person.id);
    };

    $scope.showGoogleAuth = function () {
        if (!$scope.gAuth) {
            return;
        }
        AnyplaceService.addAlert('success', 'access_token: ' + $scope.gAuth.access_token);
    };

    $scope.onSignIn = function (googleUser) {

        if ($scope.getCookie("username") === "") {
            $scope.setCookie("username", "true", 365);
            location.reload();
        }

        //location.reload();
        $scope.setAuthenticated(true);

        $scope.gAuth = gapi.auth2.getAuthInstance().currentUser.get().getAuthResponse();

        $scope.gAuth.access_token = $scope.gAuth.id_token;

        app.access_token = $scope.gAuth.id_token;

        $scope.personLookUp(googleUser);
    };


    $scope.onSignInFailure = function () {
        console.log('Sign-in state: Error');
    };

    window.onSignIn = $scope.onSignIn;
    window.onSignInFailure = $scope.onSignInFailure;

    $scope.personLookUp = function (resp) {
        $scope.person = resp.getBasicProfile();
        $scope.person.image = $scope.person.getImageUrl();
        $scope.person.id = $scope.person.getId();
        $scope.person.displayName = $scope.person.getName();
        // compose user id
        $scope.owner_id = $scope.person.id + '_' + $scope.signInType;
        $scope.displayName = $scope.person.displayName;

        if ($scope.person && $scope.person.id) {
            $scope.$broadcast('loggedIn', []);
        }

        var promise = AnyplaceAPIService.signAccount({
            name: $scope.person.displayName,
            type: "google"
        });

        promise.then(
            function (resp) {
            },
            function (resp) {
            }
        );
    };

    $scope.signOut = function () {
        $scope.setCookie("username", "", 365);
        var auth2 = gapi.auth2.getAuthInstance();
        auth2.signOut().then(function () {
            console.log('User signed out.');
        });
        $scope.isAuthenticated = false;

        $scope.$broadcast('loggedOff', []);
        $scope.gAuth = {};
        $scope.owner_id = undefined;
        $scope.person = undefined;

    };

    $scope.getCookie = function (cname) {
        var name = cname + "=";
        var ca = document.cookie.split(';');
        for (var i = 0; i < ca.length; i++) {
            var c = ca[i];
            while (c.charAt(0) == ' ') {
                c = c.substring(1);
            }
            if (c.indexOf(name) == 0) {
                return c.substring(name.length, c.length);
            }
        }
        return "";
    };

    $scope.setCookie = function (cname, cvalue, exdays) {
        var d = new Date();
        d.setTime(d.getTime() + (exdays * 24 * 60 * 60 * 1000));
        var expires = "expires=" + d.toUTCString();
        document.cookie = cname + "=" + cvalue + "; " + expires;
    };

    $scope.tab = 1;

    $scope.setTab = function (num) {
        $scope.tab = num;
    };

    $scope.isTabSet = function (num) {
        return $scope.tab === num;
    };


    $scope.tab = 1;

    $scope.setTab = function (num) {
        $scope.tab = num;

    };

    $scope.isTabSet = function (num) {
        return $scope.tab === num;
    };

    var _err = function (msg) {
        $scope.anyService.addAlert('danger', msg);
    };

    var myLocMarker = undefined;

    $scope.userPosition = undefined;

    var pannedToUserPosOnce = false;

    $scope.isUserLocVisible = false;

    $scope.getIsUserLocVisible = function () {
        return $scope.isUserLocVisible;
    };

    $scope.panToUserLocation = function () {
        if (!$scope.userPosition)
            return;

        GMapService.gmap.panTo($scope.userPosition);
        GMapService.gmap.setZoom(20);
    };

    $scope.displayMyLocMarker = function (posLatlng) {
        if (myLocMarker && myLocMarker.getMap()) {
            myLocMarker.setPosition(posLatlng);
            return;
        }
    };

    $scope.displayMyLocMarker = function (posLatlng) {
        if (myLocMarker && myLocMarker.getMap()) {
            myLocMarker.setPosition(posLatlng);
            return;
        }
        var s = new google.maps.Size(20, 20);
        if ($scope.isFirefox)
            s = new google.maps.Size(48, 48);

        myLocMarker = new google.maps.Marker({
            position: posLatlng,
            map: GMapService.gmap,
            draggable: false,
            icon: {
                url: 'build/images/location-radius-centre.png',
                origin: new google.maps.Point(0, 0), /* origin is 0,0 */
                anchor: new google.maps.Point(10, 10), /* anchor is bottom center of the scaled image */
                size: s,
                scaledSize: new google.maps.Size(20, 20)
            }
        });
    };

    $scope.hideUserLocation = function () {
        if (myLocMarker)
            myLocMarker.setMap(null);
        //if (accuracyRadius)
        //    accuracyRadius.setMap(null);

        $scope.isUserLocVisible = false;
    };


    $scope.showUserLocation = function () {

        if ($scope.getIsUserLocVisible()) {
            $scope.hideUserLocation();

            if (navigator.geolocation)
                navigator.geolocation.clearWatch(watchPosNum);

            return;
        }

        if (navigator.geolocation) {
            watchPosNum = navigator.geolocation.watchPosition(
                function (position) {

                    var posLatlng = {lat: position.coords.latitude, lng: position.coords.longitude};
                    //var radius = position.coords.accuracy;

                    $scope.userPosition = posLatlng;

                    $scope.displayMyLocMarker(posLatlng);

                    var infowindow = new google.maps.InfoWindow({
                        content: 'Your current location.',
                        maxWidth: 500
                    });

                    google.maps.event.addListener(myLocMarker, 'click', function () {
                        var self = this;
                        $scope.$apply(function () {
                            infowindow.open(GMapService.gmap, self);
                        })
                    });

                    if (!$scope.isUserLocVisible) {
                        $scope.$apply(function () {
                            $scope.isUserLocVisible = true;
                        });
                    }

                    if (!pannedToUserPosOnce) {
                        GMapService.gmap.panTo(posLatlng);
                        GMapService.gmap.setZoom(19);
                        pannedToUserPosOnce = true;
                    }
                },
                function (err) {
                    $scope.$apply(function () {
                        if (err.code == 1) {
                            _err("Permission denied. Anyplace was not able to retrieve your Geolocation.")
                        } else if (err.code == 2) {
                            _err("Position unavailable. The network is down or the positioning satellites couldn't be contacted.")
                        } else if (err.code == 3) {
                            _err("Timeout. The request for retrieving your Geolocation was timed out.")
                        } else {
                            _err("There was an error while retrieving your Geolocation. Please try again.");
                        }
                    });
                });
        } else {
            _err("The Geolocation feature is not supported by this browser.");
        }
    };

    $scope.centerViewToSelectedItem = function () {
        if ($scope.anyService.selectedBuilding == null || $scope.anyService.selectedBuilding == undefined) {
            _err("You have to select a building first");
            return;

        }
        var position = {};
        if ($scope.anyService.selectedPoi) {
            var p = $scope.anyService.selectedPoi;
            position = {lat: parseFloat(p.coordinates_lat), lng: parseFloat(p.coordinates_lon)};
        } else if ($scope.anyService.selectedBuilding) {
            var b = $scope.anyService.selectedBuilding;
            position = {lat: parseFloat(b.coordinates_lat), lng: parseFloat(b.coordinates_lon)};
        } else {
            _err("No building is selected.");
            return;
        }

        $scope.gmapService.gmap.panTo(position);
        $scope.gmapService.gmap.setZoom(20);
    }

}]);

/**
 *
 The MIT License (MIT)


 Copyright (c) 2015, Kyriakos Georgiou, Marileni Angelidou, Data Management Systems Laboratory (DMSL)
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

var app = angular.module('anyArchitect', ['ngCookies', 'angularjs-dropdown-multiselect', 'ui.bootstrap', 'ui.select', 'ngSanitize']);

app.service('GMapService', function () {

    this.gmap = {};

    var self = this;

    var element = document.getElementById("map-canvas");

    /**
     * @constructor
     * @implements {google.maps.MapType}
     */
    function CoordMapType(tileSize) {
        this.tileSize = tileSize;
    }

    CoordMapType.prototype.maxZoom = 22;
    CoordMapType.prototype.name = 'Tile #s';
    CoordMapType.prototype.alt = 'Tile Coordinate Map Type';

    CoordMapType.prototype.getTile = function (coord, zoom, ownerDocument) {
        var div = ownerDocument.createElement('div');
        div.innerHTML = coord;
        div.style.width = this.tileSize.width + 'px';
        div.style.height = this.tileSize.height + 'px';
        div.style.fontSize = '10';
        div.style.borderStyle = 'solid';
        div.style.borderWidth = '1px';
        div.style.borderColor = '#AAAAAA';
        div.style.backgroundColor = '#E5E3DF';
        return div;
    };

    /**
     * @constructor
     * @implements {google.maps.MapType}
     */
    function OSMMapType(tileSize) {
        this.tileSize = tileSize;
    }

    OSMMapType.prototype.maxZoom = 22;
    OSMMapType.prototype.name = 'OSM';
    OSMMapType.prototype.alt = 'Tile OSM Map Type';
    OSMMapType.prototype.getTile = function (coord, zoom, ownerDocument) {
        if (zoom > 19)
            return null;
        var tilesPerGlobe = 1 << zoom;
        var x = coord.x % tilesPerGlobe;
        if (x < 0) {
            x = tilesPerGlobe + x;
        }
        var tile = ownerDocument.createElement('img');
        // Wrap y (latitude) in a like manner if you want to enable vertical infinite scroll
        tile.src = "http://tile.openstreetmap.org/" + zoom + "/" + x + "/" + coord.y + ".png";
        ;
        tile.style.width = this.tileSize.width + 'px';
        tile.style.height = this.tileSize.height + 'px';
        return tile;
    };

    /**
     * @constructor
     * @implements {google.maps.MapType}
     */
    function CartoLightMapType(tileSize) {
        this.tileSize = tileSize;
    }

    CartoLightMapType.prototype.maxZoom = 22;
    CartoLightMapType.prototype.name = 'Carto Light';
    CartoLightMapType.prototype.alt = 'Tile Carto Light Map Type';
    CartoLightMapType.prototype.getTile = function (coord, zoom, ownerDocument) {
        var url = "https://cartodb-basemaps-a.global.ssl.fastly.net/light_all/{z}/{x}/{y}.png";

        url = url.replace('{x}', coord.x)
            .replace('{y}', coord.y)
            .replace('{z}', zoom);
        var tile = ownerDocument.createElement('img');
        // Wrap y (latitude) in a like manner if you want to enable vertical infinite scroll
        tile.src = url;
        tile.style.width = this.tileSize.width + 'px';
        tile.style.height = this.tileSize.height + 'px';
        return tile;
    };

    /**
     * @constructor
     * @implements {google.maps.MapType}
     */
    function CartoDarkMapType(tileSize) {
        this.tileSize = tileSize;
    }

    CartoDarkMapType.prototype.maxZoom = 22;
    CartoDarkMapType.prototype.name = 'Carto Dark';
    CartoDarkMapType.prototype.alt = 'Tile Carto Dark Map Type';
    CartoDarkMapType.prototype.getTile = function (coord, zoom, ownerDocument) {
        var url = "https://cartodb-basemaps-a.global.ssl.fastly.net/dark_all/{z}/{x}/{y}.png";

        url = url.replace('{x}', coord.x)
            .replace('{y}', coord.y)
            .replace('{z}', zoom);
        var tile = ownerDocument.createElement('img');
        // Wrap y (latitude) in a like manner if you want to enable vertical infinite scroll
        tile.src = url;
        tile.style.width = this.tileSize.width + 'px';
        tile.style.height = this.tileSize.height + 'px';
        return tile;
    };

    var mapTypeId = "OSM";
    if (typeof(Storage) !== "undefined" && localStorage) {
        if (localStorage.getItem('mapTypeId'))
            mapTypeId = localStorage.getItem('mapTypeId');
        else
            localStorage.setItem("mapTypeId", "OSM");
    }


    self.gmap = new google.maps.Map(element, {
        center: new google.maps.LatLng(57, 21),
        zoomControl: true,
        zoomControlOptions: {
            style: google.maps.ZoomControlStyle.LARGE,
            position: google.maps.ControlPosition.LEFT_CENTER
        },
        scaleControl: true,
        streetViewControl: false,
        overviewMapControl: true,
        zoom: 3,
        mapTypeId: mapTypeId,
        mapTypeControlOptions: {
            mapTypeIds: ['OSM', /* 'CartoDark',*/ 'CartoLight', /* 'coordinate',*/ 'roadmap', 'satellite'],
            style: google.maps.MapTypeControlStyle.DROPDOWN_MENU,
            position: google.maps.ControlPosition.LEFT_CENTER
        }
    });

    self.gmap.addListener('maptypeid_changed', function () {
        var showStreetViewControl = self.gmap.getMapTypeId() === 'roadmap' || self.gmap.getMapTypeId() === 'satellite';
        localStorage.setItem("mapTypeId", self.gmap.getMapTypeId());
        self.gmap.setOptions({
            streetViewControl: showStreetViewControl
        });
    });


    //Define OSM map type pointing at the OpenStreetMap tile server
    self.gmap.mapTypes.set("OSM", new OSMMapType(new google.maps.Size(256, 256)));
    //Define Carto Dark map type pointing at the OpenStreetMap tile server
    // self.gmap.mapTypes.set("CartoDark", new CartoDarkMapType(new google.maps.Size(256, 256)));
    //Define Carto Light map type pointing at the OpenStreetMap tile server
    self.gmap.mapTypes.set("CartoLight", new CartoLightMapType(new google.maps.Size(256, 256)));
    // Now attach the coordinate map type to the map's registry.
    //self.gmap.mapTypes.set('coordinate', new CoordMapType(new google.maps.Size(256, 256)));

    // Initialize search box for places
    var input = (document.getElementById('pac-input'));
    self.gmap.controls[google.maps.ControlPosition.TOP_LEFT].push(input);
    self.searchBox = new google.maps.places.SearchBox((input));

    google.maps.event.addListener(self.searchBox, 'places_changed', function () {
        var places = self.searchBox.getPlaces();

        if (places.length == 0) {
            return;
        }

        self.gmap.panTo(places[0].geometry.location);
        self.gmap.setZoom(17);
    });

    // Bias the SearchBox results towards places that are within the bounds of the
    // current map's viewport.
    self.gmap.addListener(self.gmap, 'bounds_changed', function () {
        var bounds = self.gmap.getBounds();
        self.searchBox.setBounds(bounds);
    });
});


app.factory('AnyplaceService', function () {

    var anyService = {};

    anyService.selectedBuilding = undefined;
    anyService.selectedFloor = undefined;
    anyService.selectedPoi = undefined;
    anyService.selectedCampus = undefined;
    anyService.ShowShareProp = undefined;
    anyService.progress = undefined;
    anyService.allPois = {};
    anyService.allConnections = {};
    anyService.radioHeatmapRSSMode = false;
    anyService.radioHeatmapLocalization = false; //lsolea01
    anyService.fingerPrintsTimeMode = false;
    anyService.radioHeatmapRSSTimeMode = false;


    anyService.alerts = [];

    anyService.jsonReq = {
        username: 'username',
        password: 'password'
    };
    anyService.BASE_URL = "https://ap.cs.ucy.ac.cy";

    anyService.getBuilding = function () {
        return this.selectedBuilding;
    };

    anyService.getCampus = function () {
        return this.selectedCampus;
    };

    anyService.getBuildingId = function () {
        if (!this.selectedBuilding) {
            return undefined;
        }
        return this.selectedBuilding.buid;
    };

    anyService.getBuildingName = function () {
        if (!this.selectedBuilding) {
            return 'N/A';
        }
        return this.selectedBuilding.name;
    };

    anyService.getCampusName = function () {
        if (!this.selectedCampus) {
            return 'N/A';
        }
        return this.selectedCampus.name;
    };

    anyService.getFloor = function () {
        return this.selectedFloor;
    };

    anyService.getFloorNumber = function () {
        if (!this.selectedFloor) {
            return 'N/A';
        }
        return String(this.selectedFloor.floor_number);
    };

    anyService.getFloorName = function () {
        return this.selectedFloor.floor_name;
    };

    anyService.setBuilding = function (b) {
        this.selectedBuilding = b;
    };

    anyService.setFloor = function (f) {
        this.selectedFloor = f;
    };

    anyService.addAlert = function (type, msg) {
        // this.alerts[0] = ({msg: msg, type: type});
        this.alerts[0] = ({msg: msg, type: type});

    };

    anyService.closeAlert = function (index) {
        this.alerts.splice(index, 1);
    };

    anyService.getBuildingViewerUrl = function () {
        if (!this.selectedBuilding || !this.selectedBuilding.buid) {
            return "N/A";
        }
        return this.selectedBuilding.buid;
    };

    anyService.getBuildingViewerUrlEncoded = function () {
        if (!this.selectedBuilding || !this.selectedBuilding.buid) {
            return "N/A";
        }
        return encodeURIComponent("https://ap.cs.ucy.ac.cy/viewer/?buid=" + this.selectedBuilding.buid);
    };

    anyService.getCampusViewerUrl = function () {
        if (!this.selectedCampus || !this.selectedCampus.cuid) {
            return "N/A";
        }
        return "https://ap.cs.ucy.ac.cy/viewer/?cuid=" + this.selectedCampus.cuid;
    };

    anyService.getCampusViewerUrlEncoded = function () {
        if (!this.selectedCampus || !this.selectedCampus.cuid) {
            return "N/A";
        }
        return encodeURIComponent("https://ap.cs.ucy.ac.cy/viewer/?cuid=" + this.selectedCampus.cuid);
    };

    anyService.setAllPois = function (p) {
        this.allPois = {};
        this.allPois = p;
    };

    anyService.setAllConnection = function (c) {
        this.allConnections = {};
        this.allConnections = c;
    };

    anyService.getAllPois = function () {
        if (!this.allPois) {
            return 'N/A';
        }
        return this.allPois;
    };

    anyService.getAllConnections = function () {
        if (!this.allConnections) {
            return 'N/A';
        }
        return this.allConnections;
    };

    anyService.clearAllData = function () {
        anyService.selectedPoi = undefined;
        anyService.selectedFloor = undefined;
        anyService.selectedBuilding = undefined;
        anyService.selectedCampus = undefined;
        anyService.ShowShareProp = undefined;
        anyService.allPois = {};
        anyService.allConnections = {};
    };

    return anyService;
});

app.factory('Alerter', function () {
    var alerter = {};

    alerter.AlertCtrl = '-';

    return alerter;
});

app.factory('formDataObject', function () {
    return function (data, headersGetter) {
        var formData = new FormData();
        angular.forEach(data, function (value, key) {
            formData.append(key, value);
        });

        var headers = headersGetter();
        delete headers['Content-Type'];
        return formData;
    };
});

app.config(['$locationProvider', function ($location) {
    //now there won't be a hashbang within URLs for browsers that support HTML5 history
    $location.html5Mode(true);
}]);

app.filter('propsFilter', function () {
    return function (items, props) {
        var out = [];

        if (angular.isArray(items)) {
            items.forEach(function (item) {
                var itemMatches = false;

                var keys = Object.keys(props);
                for (var i = 0; i < keys.length; i++) {
                    var prop = keys[i];
                    var text = props[prop].toLowerCase();
                    if (item[prop].toString().toLowerCase().indexOf(text) !== -1) {
                        itemMatches = true;
                        break;
                    }
                }

                if (itemMatches) {
                    out.push(item);
                }
            });
        } else {
            // Let the output be the input untouched
            out = items;
        }

        return out;
    }
});

app.factory('myInterceptor', [function () {
    var requestInterceptor = {
        request: function (config) {

            if (config.url !== undefined)
                if (config.url.indexOf(AnyplaceAPI.FULL_SERVER) !== 0) {
                    return config;
                }

            if (config.data) {
                config.data.access_token = app.access_token;
            }

            return config;
        }
    };

    return requestInterceptor;
}]);

app.config(['$httpProvider', function ($httpProvider) {
    $httpProvider.interceptors.push('myInterceptor');
}]);

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

AnyplaceAPI.FULL_SERVER = "http://localhost:9000/anyplace";

// AnyplaceAPI.FULL_SERVER = "https://ap.cs.ucy.ac.cy/anyplace";

//AnyplaceAPI.FULL_SERVER = "https://ap-dev.cs.ucy.ac.cy/anyplace";


/**
 *
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

// ship add
AnyplaceAPI.Mapping.SHIP_ADD = "/mapping/ship/add";
AnyplaceAPI.Mapping.SHIP_ADD_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.SHIP_ADD;

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


//lsolea01
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

    apiService.addShip = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.SHIP_ADD_URL,
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


    apiService.getShipPosition = function () {
        //alert( "make the request: " + json_req );
        return $http({
            method: "GET",
            url: "services.marinetraffic.com/api/exportvessel/v:5/78e2e7fd31c8707894aca549b06823598368d250/timespan:20/protocol:jsono/mmsi:310627000",
            // data: json_req
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

CanvasOverlay.prototype = new google.maps.OverlayView();

// https://github.com/wbyoko/angularjs-google-maps-components

/** @constructor */
function CanvasOverlay(image, map) {

    // Now initialize all properties.
    this.top = 0;
    this.left = 0;
    this.width = image.width;
    this.height = image.height;

    while (window && (this.width > window.innerWidth || this.height > window.innerHeight)) {
        this.width /= 2;
        this.height /= 2;
    }

    this.image_ = image;
    this.map_ = map;

    // We define a property to hold the canvas's
    // div. We'll actually create this div
    // upon receipt of the add() method so we'll
    // leave it null for now.
    this.div_ = null;
    this.canvas = null;
    this.ctx = null;
    this.angle = 0;
    this.scale = 1;

    this.latlng = map.getCenter();


    this.new_left = 0;
    this.new_top = 0;


    // Explicitly call setMap on this overlay
    this.setMap(map);
}

CanvasOverlay.prototype.onAdd = function () {


    // Note: an overlay's receipt of add() indicates that
    // the map's panes are now available for attaching
    // the overlay to the map via the DOM.

    // Create the DIV and set some basic attributes.
    var div = document.createElement('div');
    div.id = "canvas_editor";
    div.style.border = 'none';
    div.style.borderWidth = '0px';
    div.style.position = 'relative';
    div.style.top = this.top;
    div.style.left = this.left;

    // initialize the position of the outer div according to the center of the overlay
    // which is the center of the map
    var projection;
    if ((projection = this.getProjection()) != null) {
        var xy = projection.fromLatLngToDivPixel(this.latlng);
        div.style.top = (xy.y - this.height / 2) + 'px';
        div.style.left = (xy.x - this.width / 2) + 'px';
    }

    var self = this;

//    https://github.com/unlocomqx/jQuery-ui-resizable-rotation-patch/blob/master/resizable-rotation.patch.js
//    fixes problems in resizing roatated image
    function n(e) {
        return parseInt(e, 10) || 0
    }

    //patch: totally based on andyzee work here, thank you
    //patch: https://github.com/andyzee/jquery-resizable-rotation-patch/blob/master/resizable-rotation.patch.js
    //patch: search for "patch:" comments for modifications
    //patch: based on version jquery-ui-1.10.3
    //patch: can be easily reproduced with your current version
    //patch: start of patch
    /**
     * Calculate the size correction for resized rotated element
     * @param {Number} init_w
     * @param {Number} init_h
     * @param {Number} delta_w
     * @param {Number} delta_h
     * @param {Number} angle in degrees
     * @returns {object} correction css object {left, top}
     */
    jQuery.getCorrection = function (init_w, init_h, delta_w, delta_h, angle) {
        //Convert angle from degrees to radians
        var angle = angle * Math.PI / 180

        //Get position after rotation with original size
        var x = -init_w / 2;
        var y = init_h / 2;
        var new_x = y * Math.sin(angle) + x * Math.cos(angle);
        var new_y = y * Math.cos(angle) - x * Math.sin(angle);
        var diff1 = {left: new_x - x, top: new_y - y};

        var new_width = init_w + delta_w;
        var new_height = init_h + delta_h;

        //Get position after rotation with new size
        var x = -new_width / 2;
        var y = new_height / 2;
        var new_x = y * Math.sin(angle) + x * Math.cos(angle);
        var new_y = y * Math.cos(angle) - x * Math.sin(angle);
        var diff2 = {left: new_x - x, top: new_y - y};

        //Get the difference between the two positions
        var offset = {left: diff2.left - diff1.left, top: diff2.top - diff1.top};
        return offset;
    }

    jQuery.ui.resizable.prototype._mouseStart = function (event) {

        var curleft, curtop, cursor,
            o = this.options,
            el = this.element;

        this.resizing = true;

        this._renderProxy();

        curleft = n(this.helper.css("left"));
        curtop = n(this.helper.css("top"));

        if (o.containment) {
            curleft += $(o.containment).scrollLeft() || 0;
            curtop += $(o.containment).scrollTop() || 0;
        }

        this.offset = this.helper.offset();
        this.position = {left: curleft, top: curtop};

        this.size = this._helper ? {
            width: this.helper.width(),
            height: this.helper.height()
        } : {
            width: el.width(),
            height: el.height()
        };

        this.originalSize = this._helper ? {
            width: el.outerWidth(),
            height: el.outerHeight()
        } : {
            width: el.width(),
            height: el.height()
        };

        this.sizeDiff = {
            width: el.outerWidth() - el.width(),
            height: el.outerHeight() - el.height()
        };

        this.originalPosition = {left: curleft, top: curtop};
        this.originalMousePosition = {left: event.pageX, top: event.pageY};

        //patch: object to store previous data
        this.lastData = this.originalPosition;

        this.aspectRatio = (typeof o.aspectRatio === "number") ?
            o.aspectRatio :
            ((this.originalSize.width / this.originalSize.height) || 1);

        cursor = $(".ui-resizable-" + this.axis).css("cursor");
        $("body").css("cursor", cursor === "auto" ? this.axis + "-resize" : cursor);

        el.addClass("ui-resizable-resizing");
        this._propagate("start", event);
        return true;
    };

    jQuery.ui.resizable.prototype._mouseDrag = function (event) {
        //patch: get the angle
        var angle = getAngle(this.element[0]);
        var angle_rad = angle * Math.PI / 180;

        var data,
            el = this.helper, props = {},
            smp = this.originalMousePosition,
            a = this.axis,
            prevTop = this.position.top,
            prevLeft = this.position.left,
            prevWidth = this.size.width,
            prevHeight = this.size.height,
            dx = (event.pageX - smp.left) || 0,
            dy = (event.pageY - smp.top) || 0,
            trigger = this._change[a];

        var init_w = this.size.width;
        var init_h = this.size.height;

        if (!trigger) {
            return false;
        }

        //patch: cache cosine & sine
        var _cos = Math.cos(angle_rad);
        var _sin = Math.sin(angle_rad);

        //patch: calculate the correct mouse offset for a more natural feel
        var ndx = dx * _cos + dy * _sin;
        var ndy = dy * _cos - dx * _sin;
        dx = ndx;
        dy = ndy;

        // Calculate the attrs that will be change
        data = trigger.apply(this, [event, dx, dy]);

        // Put this in the mouseDrag handler since the user can start pressing shift while resizing
        this._updateVirtualBoundaries(event.shiftKey);
        if (this._aspectRatio || event.shiftKey) {
            data = this._updateRatio(data, event);
        }

        data = this._respectSize(data, event);

        //patch: backup the position
        var oldPosition = {left: this.position.left, top: this.position.top};

        this._updateCache(data);

        //patch: revert to old position
        this.position = {left: oldPosition.left, top: oldPosition.top};

        //patch: difference between datas
        var diffData = {
            left: _parseFloat(data.left || this.lastData.left) - _parseFloat(this.lastData.left),
            top: _parseFloat(data.top || this.lastData.top) - _parseFloat(this.lastData.top),
        }

        //patch: calculate the correct position offset based on angle
        var new_data = {};
        new_data.left = diffData.left * _cos - diffData.top * _sin;
        new_data.top = diffData.top * _cos + diffData.left * _sin;

        //patch: round the values
        new_data.left = _round(new_data.left);
        new_data.top = _round(new_data.top);

        //patch: update the position
        this.position.left += new_data.left;
        this.position.top += new_data.top;

        //patch: save the data for later use
        this.lastData = {
            left: _parseFloat(data.left || this.lastData.left),
            top: _parseFloat(data.top || this.lastData.top)
        };

        // plugins callbacks need to be called first
        this._propagate("resize", event);

        //patch: calculate the difference in size
        var diff_w = init_w - this.size.width;
        var diff_h = init_h - this.size.height;

        //patch: get the offset based on angle
        var offset = $.getCorrection(init_w, init_h, diff_w, diff_h, angle);

        //patch: update the position
        this.position.left += offset.left;
        this.position.top -= offset.top;

        if (this.position.top !== prevTop) {
            props.top = this.position.top + "px";
        }
        if (this.position.left !== prevLeft) {
            props.left = this.position.left + "px";
        }
        if (this.size.width !== prevWidth) {
            props.width = this.size.width + "px";
        }
        if (this.size.height !== prevHeight) {
            props.height = this.size.height + "px";
        }
        el.css(props);

        if (!this._helper && this._proportionallyResizeElements.length) {
            this._proportionallyResize();
        }

        // Call the user callback if the element was resized
        if (!$.isEmptyObject(props)) {
            this._trigger("resize", event, this.ui());
        }

        return false;
    }

    //patch: get the angle
    function getAngle(el) {
        var st = window.getComputedStyle(el, null);
        var tr = st.getPropertyValue("-webkit-transform") ||
            st.getPropertyValue("-moz-transform") ||
            st.getPropertyValue("-ms-transform") ||
            st.getPropertyValue("-o-transform") ||
            st.getPropertyValue("transform") ||
            null;
        if (tr && tr != "none") {
            var values = tr.split('(')[1];
            values = values.split(')')[0];
            values = values.split(',');

            var a = values[0];
            var b = values[1];

            var angle = Math.round(Math.atan2(b, a) * (180 / Math.PI));
            while (angle >= 360) angle = 360 - angle;
            while (angle < 0) angle = 360 + angle;
            return angle;
        }
        else
            return 0;
    }

    function _parseFloat(e) {
        return isNaN(parseFloat(e)) ? 0 : parseFloat(e);
    }

    function _round(e) {
        return Math.round((e + 0.00001) * 100) / 100
    }

    /* end of patch functions */

    jQuery(div).resizable({
        // aspectRatio: (this.image_.width / this.image_.height),
        ghost: true,
        handles: "sw, se, nw, ne",
        helper: "resizable-helper",
        aspectRatio: false,
        stop: function (event, ui) {
            self.ctx.clearRect(0, 0, self.ctx.canvas.width, self.ctx.canvas.height);

            self.width = ui.size.width;
            self.height = ui.size.height;

            self.setCanvasSize();

            self.ctx.save();
            self.ctx.translate((self.ctx.canvas.width / 2), (self.ctx.canvas.height / 2));

            self.ctx.drawImage(self.image_, -(self.width / 2), -(self.height / 2), self.width, self.height);
            self.ctx.restore();
        }
    }).rotatable({
        stop: function (event, ui) {
            //self.angle = ui.angle.stop;
        }
    });

    jQuery(div).draggable({
        stop: function (event, ui) {
            // update the coordinates
            if (projection != null) {
                var left = $(this).position().left;
                var top = $(this).position().top;
                self.latlng = projection.fromDivPixelToLatLng(new google.maps.Point(left, top), true);
            }
        }
    });

    // Create a Canvas element and attach it to the DIV.
    var canvas = document.createElement('canvas');
    canvas.id = "canvas";
    div.appendChild(canvas);

    // Set the overlay's div_ property to this DIV
    this.div_ = div;
    this.canvas = canvas;
    this.ctx = canvas.getContext("2d");

    // load the floor
    this.initImage();

    // We add an overlay to a map via one of the map's panes.
    // We'll add this overlay to the overlayImage pane.
    var panes = this.getPanes();
    panes.overlayImage.appendChild(this.div_);
////////////////////////////////////////
    var that = this;
    var container = div;
    google.maps.event.addDomListener(this.get('map').getDiv(),
        'mouseleave',
        function () {
            google.maps.event.trigger(container, 'mouseup');
        }
    );


    google.maps.event.addDomListener(container,
        'mousedown',
        function (e) {
            this.style.cursor = 'move';
            that.map.set('draggable', false);
            that.set('origin', e);
        }
    );

    google.maps.event.addDomListener(container, 'mouseup', function () {
        that.map.set('draggable', true);
        this.style.cursor = 'default';
        google.maps.event.removeListener(that.moveHandler);
    });

    return this;
}

CanvasOverlay.prototype.draw = function () {
    var div = this.div_;

    if (this.canvas == null) {
        alert("error creating the canvas");
    }
}

CanvasOverlay.prototype.onRemove = function () {
    this.div_.parentNode.removeChild(this.div_);
}

// Note that the visibility property must be a string enclosed in quotes
CanvasOverlay.prototype.hide = function () {
    if (this.div_) {
        this.div_.style.visibility = 'hidden';
    }
}

CanvasOverlay.prototype.show = function () {
    if (this.div_) {
        this.div_.style.visibility = 'visible';
    }
}

CanvasOverlay.prototype.toggle = function () {
    if (this.div_) {
        if (this.div_.style.visibility == 'hidden') {
            this.show();
        } else {
            this.hide();
        }
    }
}

CanvasOverlay.prototype.toggleDOM = function () {
    if (this.getMap()) {
        this.setMap(null);
    } else {
        this.setMap(this.map_);
    }
}

/*************************
 * CANVAS METHODS
 */
CanvasOverlay.prototype.getCanvas = function () {
    return this.canvas;
}

CanvasOverlay.prototype.getContext2d = function () {
    return this.ctx;
}


/*****************************
 * EDITING METHODS
 */

CanvasOverlay.prototype.setCanvasSize = function () {
    this.ctx.canvas.width = this.width;
    this.ctx.canvas.height = this.height;

    // we need to change the width and height of the div #canvas_editor
    // which is the element being rotated by the slider
    this.div_.style.width = this.width + 'px';
    this.div_.style.height = this.height + 'px';
}

CanvasOverlay.prototype.initImage = function () {
    this.setCanvasSize();
    this.ctx.save();

    this.ctx.translate((this.ctx.canvas.width / 2), (this.ctx.canvas.height / 2));
    this.ctx.rotate(this.angle);

    this.ctx.drawImage(this.image_, -(this.width / 2), -(this.height / 2), this.width, this.height);
    this.ctx.restore();
}

CanvasOverlay.prototype.drawBoundingCanvas = function () {
    // convert degress rotation to angle radians
    var degrees = getRotationDegrees($('#canvas_editor'));
    //var degrees= parseFloat($('#rot_degrees').val());
    var rads = deg2rad(degrees);

    $('#canvas_editor').css({
        '-moz-transform': 'rotate(0deg)',
        '-webkit-transform': 'rotate(0deg)',
        '-ms-transform': 'rotate(0deg)',
        '-o-transform': 'rotate(0deg)',
        'transform': 'rotate(0deg)'
    });

    // get the center which we use to rotate the image
    // this is the center when the canvas is rotated at 0 degrees
    var oldCenter = getPositionData($('#canvas_editor'));
    var oldLeft = oldCenter.left;
    var oldTop = oldCenter.top;
    var oldCenterX = oldLeft + this.width / 2;
    var oldCenterY = oldTop + this.height / 2;

    // calculate the 4 new corners - http://stackoverflow.com/a/622172/1066790
    //                             - https://en.wikipedia.org/wiki/Transformation_matrix#Rotation
    var top_left_x = oldCenterX + (oldLeft - oldCenterX) * Math.cos(rads) + (oldTop - oldCenterY) * Math.sin(rads);
    var top_left_y = oldCenterY - (oldLeft - oldCenterX) * Math.sin(rads) + (oldTop - oldCenterY) * Math.cos(rads);
    var top_right_x = oldCenterX + (oldLeft + this.width - oldCenterX) * Math.cos(rads) + (oldTop - oldCenterY) * Math.sin(rads);
    var top_right_y = oldCenterY - (oldLeft + this.width - oldCenterX) * Math.sin(rads) + (oldTop - oldCenterY) * Math.cos(rads);
    var bottom_left_x = oldCenterX + (oldLeft - oldCenterX) * Math.cos(rads) + (oldTop + this.height - oldCenterY) * Math.sin(rads);
    var bottom_left_y = oldCenterY - (oldLeft - oldCenterX) * Math.sin(rads) + (oldTop + this.height - oldCenterY) * Math.cos(rads);
    var bottom_right_x = oldCenterX + (oldLeft + this.width - oldCenterX) * Math.cos(rads) + (oldTop + this.height - oldCenterY) * Math.sin(rads);
    var bottom_right_y = oldCenterY - (oldLeft + this.width - oldCenterX) * Math.sin(rads) + (oldTop + this.height - oldCenterY) * Math.cos(rads);
    // calculate new coordinates finding the top left
    var maxx = Math.max(top_left_x, top_right_x, bottom_left_x, bottom_right_x);
    var maxy = Math.max(top_left_y, top_right_y, bottom_left_y, bottom_right_y);
    var minx = Math.min(top_left_x, top_right_x, bottom_left_x, bottom_right_x);
    var miny = Math.min(top_left_y, top_right_y, bottom_left_y, bottom_right_y);
    var newTop = miny;
    var newLeft = minx;

    var w = maxx - minx;
    var h = maxy - miny;

    // move the canvas to the new top left position
    $('#canvas_editor').css({
        'top': newTop + 'px',
        'left': newLeft + 'px'
    })

    this.ctx.canvas.width = w;
    this.ctx.canvas.height = h;
    this.ctx.save();
    this.ctx.clearRect(0, 0, this.ctx.canvas.width, this.ctx.canvas.height);
    this.ctx.translate(oldCenterX - newLeft, oldCenterY - newTop);
    this.ctx.rotate(rads);
    this.ctx.drawImage(this.image_, -(this.width / 2), -(this.height / 2), this.width, this.height);
    this.ctx.restore();

    // we should now update the coordinates for the new image
    this.top = newTop;
    this.left = newLeft;
    var projection;
    if ((projection = this.getProjection()) != null) {
        this.bottom_left_coords = projection.fromContainerPixelToLatLng(new google.maps.Point(this.left, this.top + h), true);
        this.top_right_coords = projection.fromContainerPixelToLatLng(new google.maps.Point(this.left + w, this.top), true);
    }
}

/***************************************
 * HELPER FUNCTIONS
 */
function deg2rad(deg) {
    return deg * Math.PI / 180;
}

function rad2deg(rad) {
    return rad / Math.PI * 180;
}

function getRotationDegrees(obj) {
    var matrix = obj.css("-webkit-transform") ||
        obj.css("-moz-transform") ||
        obj.css("-ms-transform") ||
        obj.css("-o-transform") ||
        obj.css("transform");
    if (matrix !== 'none') {
        var values = matrix.split('(')[1].split(')')[0].split(',');
        var a = values[0];
        var b = values[1];
        var c = values[2];
        var d = values[3];

        var sin = b / scale;

        var scale = Math.sqrt(a * a + b * b);
        var angle = Math.round(Math.atan2(b, a) * (180 / Math.PI));
        //var angle = Math.atan2(b, a) * (180/Math.PI);
    } else {
        var angle = 0;
    }
    return angle;
}

// src= http://jsfiddle.net/Y8d6k/
var getPositionData = function (el) {
    return $.extend({
        width: el.outerWidth(false),
        height: el.outerHeight(false)
    }, el.offset());
};
/**
 *
 The MIT License (MIT)

 Copyright (c) 2015, Lambros Petrou, Data Management Systems Laboratory (DMSL)
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

var LPUtils = {};

LPUtils.isNullOrUndefined = function (t) {
    return (typeof(t) === 'undefined' || t === null);
}

LPUtils.isStringEmptyNullUndefined = function (s) {
    return (!s || 0 === s.length);
    //return ( typeof(s) === 'undefined' || s === null || 0 === s.length );
}

LPUtils.isStringBlankNullUndefined = function (s) {
    return (!s || /^\s*$/.test(s));
}

/************************************
 * UI Functions
 */

LPUtils.alert = function (msg) {
    var np = document.getElementById("notification_panel");
    np.innerHTML = "<alert type=\"error\" close=\"LPUtils.closeAlert()\">" + msg + "</alert>";
}
LPUtils.closeAlert = function () {
    var np = document.getElementById("notification_panel");
    np.getEl.innerHTML = "";
}

LPUtils.success = function (msg) {
    var np = document.getElementById("notification_panel");
    np.innerHTML = "<alert type=\"success\">" + msg + "</alert>";
}


function hexToBase64(str) {
    return btoa(String.fromCharCode.apply(null, str.replace(/\r|\n/g, "").replace(/([\da-fA-F]{2}) ?/g, "0x$1 ").replace(/ +$/, "").split(" ")));
}

function base64_encode(data) {
    // http://kevin.vanzonneveld.net
    // +   original by: Tyler Akins (http://rumkin.com)
    // +   improved by: Bayron Guevara
    // +   improved by: Thunder.m
    // +   improved by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
    // +   bugfixed by: Pellentesque Malesuada
    // +   improved by: Kevin van Zonneveld (http://kevin.vanzonneveld.net)
    // +   improved by: Rafał Kukawski (http://kukawski.pl)
    // *     example 1: base64_encode('Kevin van Zonneveld');
    // *     returns 1: 'S2V2aW4gdmFuIFpvbm5ldmVsZA=='
    // mozilla has this native
    // - but breaks in 2.0.0.12!
    //if (typeof this.window['btoa'] == 'function') {
    //    return btoa(data);
    //}
    var b64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=";
    var o1, o2, o3, h1, h2, h3, h4, bits, i = 0,
        ac = 0,
        enc = "",
        tmp_arr = [];

    if (!data) {
        return data;
    }

    do { // pack three octets into four hexets
        o1 = data.charCodeAt(i++);
        o2 = data.charCodeAt(i++);
        o3 = data.charCodeAt(i++);

        bits = o1 << 16 | o2 << 8 | o3;

        h1 = bits >> 18 & 0x3f;
        h2 = bits >> 12 & 0x3f;
        h3 = bits >> 6 & 0x3f;
        h4 = bits & 0x3f;

        // use hexets to index into b64, and append result to encoded string
        tmp_arr[ac++] = b64.charAt(h1) + b64.charAt(h2) + b64.charAt(h3) + b64.charAt(h4);
    } while (i < data.length);

    enc = tmp_arr.join('');

    var r = data.length % 3;

    return (r ? enc.slice(0, r - 3) : enc) + '==='.slice(r || 3);

}

LPUtils.Base64 = {

// private property
    _keyStr: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",

// public method for encoding
    encode: function (input) {
        var output = "";
        var chr1, chr2, chr3, enc1, enc2, enc3, enc4;
        var i = 0;

        input = LPUtils.Base64._utf8_encode(input);

        while (i < input.length) {

            chr1 = input.charCodeAt(i++);
            chr2 = input.charCodeAt(i++);
            chr3 = input.charCodeAt(i++);

            enc1 = chr1 >> 2;
            enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
            enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
            enc4 = chr3 & 63;

            if (isNaN(chr2)) {
                enc3 = enc4 = 64;
            } else if (isNaN(chr3)) {
                enc4 = 64;
            }

            output = output +
                this._keyStr.charAt(enc1) + this._keyStr.charAt(enc2) +
                this._keyStr.charAt(enc3) + this._keyStr.charAt(enc4);

        }

        return output;
    },

// public method for decoding
    decode: function (input) {
        var output = "";
        var chr1, chr2, chr3;
        var enc1, enc2, enc3, enc4;
        var i = 0;

        input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");

        while (i < input.length) {

            enc1 = this._keyStr.indexOf(input.charAt(i++));
            enc2 = this._keyStr.indexOf(input.charAt(i++));
            enc3 = this._keyStr.indexOf(input.charAt(i++));
            enc4 = this._keyStr.indexOf(input.charAt(i++));

            chr1 = (enc1 << 2) | (enc2 >> 4);
            chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
            chr3 = ((enc3 & 3) << 6) | enc4;

            output = output + String.fromCharCode(chr1);

            if (enc3 != 64) {
                output = output + String.fromCharCode(chr2);
            }
            if (enc4 != 64) {
                output = output + String.fromCharCode(chr3);
            }

        }

        output = LPUtils.Base64._utf8_decode(output);

        return output;

    },

// private method for UTF-8 encoding
    _utf8_encode: function (string) {
        string = string.replace(/\r\n/g, "\n");
        var utftext = "";

        for (var n = 0; n < string.length; n++) {

            var c = string.charCodeAt(n);

            if (c < 128) {
                utftext += String.fromCharCode(c);
            }
            else if ((c > 127) && (c < 2048)) {
                utftext += String.fromCharCode((c >> 6) | 192);
                utftext += String.fromCharCode((c & 63) | 128);
            }
            else {
                utftext += String.fromCharCode((c >> 12) | 224);
                utftext += String.fromCharCode(((c >> 6) & 63) | 128);
                utftext += String.fromCharCode((c & 63) | 128);
            }

        }

        return utftext;
    },

// private method for UTF-8 decoding
    _utf8_decode: function (utftext) {
        var string = "";
        var i = 0;

        while (i < utftext.length) {

            var c = utftext.charCodeAt(i);

            if (c < 128) {
                string += String.fromCharCode(c);
                i++;
            }
            else if ((c > 191) && (c < 224)) {
                var c2 = utftext.charCodeAt(i + 1);
                string += String.fromCharCode(((c & 31) << 6) | (c2 & 63));
                i += 2;
            }
            else {
                var c2 = utftext.charCodeAt(i + 1);
                var c3 = utftext.charCodeAt(i + 2);
                string += String.fromCharCode(((c & 15) << 12) | ((c2 & 63) << 6) | (c3 & 63));
                i += 3;
            }

        }

        return string;
    }

}


/**
 * Uses the new array typed in javascript to binary base64 encode/decode
 * at the moment just decodes a binary base64 encoded
 * into either an ArrayBuffer (decodeArrayBuffer)
 * or into an Uint8Array (decode)
 *
 * References:
 * https://developer.mozilla.org/en/JavaScript_typed_arrays/ArrayBuffer
 * https://developer.mozilla.org/en/JavaScript_typed_arrays/Uint8Array
 */

LPUtils.Base64Binary = {
    _keyStr: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",

    /* will return a  Uint8Array type */
    decodeArrayBuffer: function (input) {
        var bytes = (input.length / 4) * 3;
        var ab = new ArrayBuffer(bytes);
        this.decode(input, ab);

        return ab;
    },

    decode: function (input, arrayBuffer) {
        //get last chars to see if are valid
        var lkey1 = this._keyStr.indexOf(input.charAt(input.length - 1));
        var lkey2 = this._keyStr.indexOf(input.charAt(input.length - 2));

        var bytes = (input.length / 4) * 3;
        if (lkey1 == 64) bytes--; //padding chars, so skip
        if (lkey2 == 64) bytes--; //padding chars, so skip

        var uarray;
        var chr1, chr2, chr3;
        var enc1, enc2, enc3, enc4;
        var i = 0;
        var j = 0;

        if (arrayBuffer)
            uarray = new Uint8Array(arrayBuffer);
        else
            uarray = new Uint8Array(bytes);

        input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");

        for (i = 0; i < bytes; i += 3) {
            //get the 3 octects in 4 ascii chars
            enc1 = this._keyStr.indexOf(input.charAt(j++));
            enc2 = this._keyStr.indexOf(input.charAt(j++));
            enc3 = this._keyStr.indexOf(input.charAt(j++));
            enc4 = this._keyStr.indexOf(input.charAt(j++));

            chr1 = (enc1 << 2) | (enc2 >> 4);
            chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
            chr3 = ((enc3 & 3) << 6) | enc4;

            uarray[i] = chr1;
            if (enc3 != 64) uarray[i + 1] = chr2;
            if (enc4 != 64) uarray[i + 2] = chr3;
        }

        return uarray;
    }
}

USGSOverlay.prototype = new google.maps.OverlayView();

/** @constructor */
function USGSOverlay(bounds, image, map) {

    // Now initialize all properties.
    this.bounds_ = bounds;
    this.image_ = image;
    this.image64 = image;
    this.map_ = map;

    // We define a property to hold the image's div. We'll
    // actually create this div upon receipt of the onAdd()
    // method so we'll leave it null for now.
    this.div_ = null;

    // Explicitly call setMap on this overlay
    this.setMap(map);
}

USGSOverlay.prototype.onAdd = function () {

    // Note: an overlay's receipt of onAdd() indicates that
    // the map's panes are now available for attaching
    // the overlay to the map via the DOM.

    // Create the DIV and set some basic attributes.
    var div = document.createElement('div');
    div.id = "ground_overlay";
    div.style.borderStyle = 'none';
    div.style.borderWidth = '0px';
    div.style.position = 'absolute';

    // Create an IMG element and attach it to the DIV.
    var img = document.createElement('img');
    img.src = this.image64;
    img.style.width = '100%';
    img.style.height = '100%';
    img.style.position = 'absolute';
    div.appendChild(img);

    // Set the overlay's div_ property to this DIV
    this.div_ = div;

    // We add an overlay to a map via one of the map's panes.
    // We'll add this overlay to the overlayLayer pane.
    var panes = this.getPanes();
    panes.overlayLayer.appendChild(div);
}

USGSOverlay.prototype.draw = function () {

    // Size and position the overlay. We use a southwest and northeast
    // position of the overlay to peg it to the correct position and size.
    // We need to retrieve the projection from this overlay to do this.
    var overlayProjection = this.getProjection();

    // Retrieve the southwest and northeast coordinates of this overlay
    // in latlngs and convert them to pixels coordinates.
    // We'll use these coordinates to resize the DIV.
    var sw = overlayProjection.fromLatLngToDivPixel(this.bounds_.getSouthWest());
    var ne = overlayProjection.fromLatLngToDivPixel(this.bounds_.getNorthEast());

    // Resize the image's DIV to fit the indicated dimensions.
    var div = this.div_;
    div.style.left = sw.x + 'px';
    div.style.top = ne.y + 'px';
    div.style.width = (ne.x - sw.x) + 'px';
    div.style.height = (sw.y - ne.y) + 'px';
}

USGSOverlay.prototype.onRemove = function () {
    this.div_.parentNode.removeChild(this.div_);
    this.div_ = null;
}
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

app.controller("AlertController", ['$rootScope', '$scope', 'AnyplaceService', function ($rootScope, $scope, AnyplaceService) {

    $scope.anyService = AnyplaceService;

    $scope.alerts = AnyplaceService.alerts;

    $scope.closeable = true;

}]);
/**
 *
 The MIT License (MIT)

 Copyright (c) 2015, Kyriakos Georgiou, Marileni Angelidou, Data Management Systems Laboratory (DMSL)
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

app.controller('BuildingController', ['$cookieStore', '$scope', '$compile', 'GMapService', 'AnyplaceService', 'AnyplaceAPIService', function ($cookieStore, $scope, $compile, GMapService, AnyplaceService, AnyplaceAPIService) {

    $scope.myMarkers = {};
    $scope.myMarkerId = 0;
    $scope.laship=33.0000;
    $scope.loship=33.0000;
    $scope.moveShip=0;
    $scope.ShipInterval;
    $scope.gmapService = GMapService;
    $scope.anyService = AnyplaceService;
    $scope.anyAPI = AnyplaceAPIService;
    $scope.example9model = [];
    $scope.example9data = [];
    $scope.example9settings = {enableSearch: true, scrollable: true};

    $scope.example9modeledit = [];
    $scope.example9dataedit = [];
    $scope.example9settingsedit = {enableSearch: true, scrollable: true};

    $scope.myBuildings = [];
    $scope.myShips = [];



    $scope.myBuildingsHashT = {};
    $scope.myCampus = [];
    $scope.old_campus = [];

    $scope.crudTabSelected = 1;

    $scope.fileToUpload = "";
    $scope.logfile = "";

    $scope.poisTypes = {};
    $scope.catTypes = {};

    $scope.pageLoad = false;

    $scope.crudTabSelected = 1;


//     // Replace this with your URL.
//     var BUILDING_TILE_URL = AnyplaceAPI.FULL_SERVER + '/floortiles/{building}/{floor}/{z}/z{z}x{x}y{y}.png';
//
// // Name the layer anything you like.
//     var layerID = 'building_layer';
//
// // Create a new ImageMapType layer. static_tiles/19/z19x310801y207411.png
//     var maptiler = new google.maps.ImageMapType({
//         name: layerID,
//         getTileUrl: function (coord, zoom) {
//             var buid = $scope.anyService.getBuildingId();
//
//             var floor = $scope.anyService.getFloorNumber();
//             var url = BUILDING_TILE_URL
//                 .replace('{building}', buid)
//                 .replace('{floor}', floor)
//                 .replace('{z}', zoom)
//                 .replace('{x}', coord.x)
//                 .replace('{y}', coord.y)
//                 .replace('{z}', zoom);
//             return url;
//         },
//         tileSize: new google.maps.Size(256, 256),
//         isPng: true
//     });
//
//     // Register the new layer, then activate it.
//     $scope.gmapService.gmap.overlayMapTypes.insertAt(0, maptiler);

    $scope.setCrudTabSelected = function (n) {

        $scope.crudTabSelected = n;
        if (!$scope.anyService.getBuilding()) {
            _err("No building is selected.");
            return;
        }

        var b = $scope.myBuildingsHashT[$scope.anyService.getBuildingId()];



        if (!b) {
            return;
        }

        var m = b.marker;
        if (!m) {
            return;
        }
        // edit building
        if (n === 2) {
            m.setDraggable(true);
        } else {
            m.setDraggable(false);
        }

    };
    $scope.isCrudTabSelected = function (n) {
        return $scope.crudTabSelected === n;
    };

    $scope.$on("loggedIn", function (event, mass) {
        //_suc('Successfully logged in.');
        $scope.fetchAllBuildings();
        $scope.fetchAllCampus();
        //$scope.fetchAllPoisTypes();
    });

    $scope.$on("loggedOff", function (event, mass) {
        _clearBuildingMarkersAndModels();
        $scope.myMarkers = {};
        $scope.myMarkerId = 0;
        $scope.myBuildings = [];
        $scope.myBuildingsHashT = {};
    });

    /**
     * @return {string}
     */
    function S4() {
        return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
    }

    var guid = (S4() + S4() + "-" + S4() + "-4" + S4().substr(0, 3) + "-" + S4() + "-" + S4() + S4() + S4()).toLowerCase();
    var d = new Date();
    document.getElementById("CampusID").value = "cuid_" + guid + "_" + d.getTime();

    var logoPlanInputElement = $('#input-logo');

    logoPlanInputElement.change(function handleImage(e) {
        var reader = new FileReader();
        reader.onload = function (event) {
            var imgObj = new Image();
            imgObj.src = event.target.result;
        };
        reader.readAsDataURL(e.target.files[0]);
    });

    $scope.setLogoPlan = function (cuid) {

        var newFl = {
            is_published: 'true',
            cuid: cuid,
            logo: String($scope.newFloorNumber),
            description: String($scope.newFloorNumber),
            floor_number: String($scope.newFloorNumber)
        };

        $scope.myFloors[$scope.myFloorId] = newFl;
        $scope.myFloorId++;

        // create the proper image inside the canvas
        canvasOverlay.drawBoundingCanvas();

        // create the ground overlay and destroy the canvasOverlay object
        // and also set the floor_plan_coords in $scope.data
        var bl = canvasOverlay.bottom_left_coords;
        var tr = canvasOverlay.top_right_coords;
        $scope.data.floor_plan_coords.bottom_left_lat = bl.lat();
        $scope.data.floor_plan_coords.bottom_left_lng = bl.lng();
        $scope.data.floor_plan_coords.top_right_lat = tr.lat();
        $scope.data.floor_plan_coords.top_right_lng = tr.lng();
        $scope.data.floor_plan_coords.zoom = GMapService.gmap.getZoom() + "";
        var data = canvasOverlay.getCanvas().toDataURL("image/png"); // defaults to png
        $scope.data.floor_plan_base64_data = data;
        var imageBounds = new google.maps.LatLngBounds(
            new google.maps.LatLng(bl.lat(), bl.lng()),
            new google.maps.LatLng(tr.lat(), tr.lng()));
        $scope.data.floor_plan_groundOverlay = new USGSOverlay(imageBounds, data, GMapService.gmap);

        canvasOverlay.setMap(null); // remove the canvas overlay since the groundoverlay is placed
        $('#input-floor-plan').prop('disabled', false);
        $scope.isCanvasOverlayActive = false;

        if (_floorNoExists($scope.newFloorNumber)) {
            for (var i = 0; i < $scope.xFloors.length; i++) {
                var f = $scope.xFloors[i];
                if (!LPUtils.isNullOrUndefined(f)) {
                    if (f.floor_number === String($scope.newFloorNumber)) {
                        $scope.uploadFloorPlanBase64($scope.anyService.selectedBuilding, f, $scope.data);
                        break;
                    }
                }
            }
        } else {
            $scope.addFloorObject(newFl, $scope.anyService.selectedBuilding, $scope.data);
        }

    };

    $scope.$watch('anyService.selectedBuilding', function (newVal, oldVal) {
        if (newVal && newVal.coordinates_lat && newVal.coordinates_lon) {
            // Pan map to selected building
            $scope.gmapService.gmap.panTo(_latLngFromBuilding(newVal));
            $scope.gmapService.gmap.setZoom(19);
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem("lastBuilding", newVal.buid);
            }
        }
        //if (newVal && newVal.buid && newVal.poistypeid) {
        // $scope.fetchAllPoisTypes(newVal.poistypeid);
        // }
        // else {
        $scope.poisTypes = [
            "Disabled Toilets",
            "Elevator",
            "Entrance",
            "Fire Extinguisher",
            "First Aid/AED",
            "Kitchen",
            "Office",
            "Ramp",
            "Room",
            "Security/Guard",
            "Stair",
            "Toilets",
            "Other"
        ];
        //}
    });

    $scope.$watch('anyService.selectedCampus', function (newVal, oldVal) {
        if (newVal && newVal.cuid) {
            // Pan map to selected building
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem("lastCampus", newVal.cuid);
            }
        }

    });


    var _clearBuildingMarkersAndModels = function () {
        for (var b in $scope.myBuildingsHashT) {
            if ($scope.myBuildingsHashT.hasOwnProperty(b)) {
                $scope.myBuildingsHashT[b].marker.setMap(null);
                delete $scope.myBuildingsHashT[b];
            }
        }
    };

    var _err = function (msg) {
        $scope.anyService.addAlert('danger', msg);
    };

    var _suc = function (msg) {
        $scope.anyService.addAlert('success', msg);
    };

    var _latLngFromBuilding = function (b) {
        if (b && b.coordinates_lat && b.coordinates_lon) {
            return {
                lat: parseFloat(b.coordinates_lat),
                lng: parseFloat(b.coordinates_lon)
            }
        }
        return undefined;
    };
    $scope.fetchAllPoisTypes = function () {

        var jsonReq = {};

        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;
        jsonReq.access_token = $scope.gAuth.access_token;

        jsonReq.owner_id = $scope.owner_id;

        if (!jsonReq.owner_id) {
            _err("Could nor authorize user. Please refresh.1");
            return;
        }
        var promise = $scope.anyAPI.retrievePoisTypes(jsonReq);
        promise.then(
            function (resp) {
                var data = resp.data;
                $scope.catTypes = data.poistypes;
            },
            function (resp) {
                var data = resp.data;
                _err("Something went wrong while fetching POIs types");
            }
        );
    };


    $scope.fetchAllBuildings = function () {
        var jsonReq = {};
        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;
        jsonReq.owner_id = $scope.owner_id;

        if (!jsonReq.owner_id) {
            _err("Could nor authorize user. Please refresh.");
            return;
        }

        var promise = $scope.anyAPI.allBuildings(jsonReq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                $scope.myBuildings = [];
                $scope.myShips = [];

                //var bs = JSON.parse( data.buildings );
                for(var i in data.buildings){
                    var bid=data.buildings[i].buid;
                    var getship=bid.split("_",1);

                    if(getship=="ship") {
                        $scope.myShips.push(data.buildings[i]);
                    }
                    else{
                        $scope.myBuildings.push(data.buildings[i]);
                    }

                }



                var infowindow = new google.maps.InfoWindow({
                    content: '-',
                    maxWidth: 500
                });

                var localStoredBuildingIndex = -1;
                var localStoredBuildingId = undefined;
                if (typeof(Storage) !== "undefined" && localStorage && localStorage.getItem('lastBuilding')) {
                    localStoredBuildingId = localStorage.getItem('lastBuilding');
                }


                //show all buildings and ships on map
                for (var i = 0; i < data.buildings.length; i++) {

                    var b = data.buildings[i];

                    $scope.example9data[i] = {id: b.buid, label: b.name};
                    $scope.example9dataedit[i] = {id: b.buid, label: b.name};

                    if (localStoredBuildingId && localStoredBuildingId === b.buid) {
                        localStoredBuildingIndex = i;
                    }

                    if (b.is_published === 'true' || b.is_published == true) {
                        b.is_published = true;
                    } else {
                        b.is_published = false;
                    }
                    var bid=b.buid;
                    var getship=bid.split("_",1);

                    if(getship=="ship") {
                        var marker = new google.maps.Marker({
                            position: _latLngFromBuilding(b),
                            map: GMapService.gmap,
                            icon: new google.maps.MarkerImage(
                                'build/images/ship_icon.png',
                                null, /* size is determined at runtime */
                                null, /* origin is 0,0 */
                                null, /* anchor is bottom center of the scaled image */
                                new google.maps.Size(54, 54)),
                            draggable: false
                        });


                        var htmlContent = '<div class="infowindow-scroll-fix">'
                            + '<h5>Ship:</h5>'
                            + '<span>' + b.name + '</span>'
                            + '<h5>Description:</h5>'
                            + '<textarea class="infowindow-text-area"  rows="3" readonly>' + b.description + '</textarea>'
                            + '</div>';




                    }
                    else {
                        var marker = new google.maps.Marker({
                            position: _latLngFromBuilding(b),
                            map: GMapService.gmap,
                            icon: new google.maps.MarkerImage(
                                'build/images/building-icon.png',
                                null, /* size is determined at runtime */
                                null, /* origin is 0,0 */
                                null, /* anchor is bottom center of the scaled image */
                                new google.maps.Size(54, 54)),
                            draggable: false
                        });



                        var htmlContent = '<div class="infowindow-scroll-fix">'
                            + '<h5>Building:</h5>'
                            + '<span>' + b.name + '</span>'
                            + '<h5>Description:</h5>'
                            + '<textarea class="infowindow-text-area"  rows="3" readonly>' + b.description + '</textarea>'
                            + '</div>';

                    }



                    marker.infoContent = htmlContent;
                    marker.building = b;

                    $scope.myBuildingsHashT[b.buid] = {
                        marker: marker,
                        model: b
                    };

                    google.maps.event.addListener(marker, 'click', function () {
                        infowindow.setContent(this.infoContent);
                        infowindow.open(GMapService.gmap, this);
                        var self = this;
                        // $scope.$apply(function () {
                        $scope.anyService.selectedBuilding = self.building;
                        var bid=self.building.buid;
                        var getship=bid.split("_",1);


                        if(getship=="ship") {
                            if($scope.moveShip==0){
                                $scope.moveShip=1;
                                console.log("stop");
                            }
                            else {
                                $scope.moveShip=0;
                            }

                            //ship here


                            function myFunction(){
                                var b = $scope.myBuildingsHashT[bid];

                                var m = b.marker;
                                $scope.laship=parseFloat($scope.laship) + 0.0001;
                                $scope.loship=parseFloat($scope.loship) + 0.0001;
                                var myLatLng = {lat: parseFloat($scope.laship), lng: parseFloat($scope.loship)};

                                m.setPosition(myLatLng);

                                console.log("its work");



                            }

                            if ($scope.moveShip==0) {
                                clearInterval($scope.ShipInterval);
                                var model=$scope.myBuildingsHashT[bid];
                                console.log($scope.myBuildingsHashT[bid]);
                                console.log(model.model.coordinates_lat);

                                var myLatLng = {lat: parseFloat(model.model.coordinates_lat), lng: parseFloat(model.model.coordinates_lon)};
                                console.log(myLatLng);
                                model.marker.setPosition(myLatLng);
                                // console.log(b);
                            }
                            else{
                                myFunction();

                                $scope.ShipInterval=setInterval(function(){
                                    myFunction()}, 10000);

                            }


                            // Open a new connection, using the GET request on the URL endpoint
                            // request.open('GET', 'ship.json', true);
                            // request.send(null);
                            // request.onreadystatechange = function(){

                            //     //check if the status is 200(means everything is okay)
                            //     if (this.status === 200&& this.readyState==4) {
                            //         //return server response as an object with JSON.parse
                            //
                            //
                            //          json=JSON.parse(request.responseText);
                            //         console.log(json[0].LAT);



                            // var json=JSON.parse(request.responseText);
                            //
                            // console.log(request.responseText);
                            //  var myLatLng = {lat: parseFloat(json[0].LAT), lng:parseFloat( json[0].LON)};






                            // console.log(JSON.parse(this.request));
                            // }
                            // }

                            // Send request











                            // }

                            // for (var b in $scope.myBuildingsHashT) {
                            //         $scope.myBuildingsHashT[b].marker.setMap();
                            //
                            // }

                        }


                        // });



                    });

                    console.log("dfgg ");
                }

                // using the latest building form localStorage
                if (localStoredBuildingIndex >= 0) {
                    $scope.anyService.selectedBuilding = $scope.myBuildings[localStoredBuildingIndex];
                } else if ($scope.myBuildings[0]) {
                    $scope.anyService.selectedBuilding = $scope.myBuildings[0];
                }

                // _suc('Successfully fetched buildings.');
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err('Something went wrong while fetching buildings.');
            }
        );
    };

    $scope.addNewBuilding = function (id) {

        if ($scope.myMarkers[id] && $scope.myMarkers[id].marker) {

            var building = $scope.myMarkers[id].model;

            // set owner id
            building.owner_id = $scope.owner_id;

            if (!building.owner_id) {
                _err("Could not authorize user. Please refresh.");
                return;
            }


            building.coordinates_lat = String($scope.myMarkers[id].marker.position.lat());
            building.coordinates_lon = String($scope.myMarkers[id].marker.position.lng());

            if (building.coordinates_lat === undefined || building.coordinates_lat === null) {
                _err("Invalid building latitude.");
                return;
            }

            if (building.coordinates_lon === undefined || building.coordinates_lon === null) {
                _err("Invalid building longitude.");
                return;
            }

            if (building.is_published === true) {
                building.is_published = "true";
            } else {
                building.is_published = "false";
            }

            if (!building.description) {
                building.description = "-";
            }

            if (building.owner_id && building.name && building.description && building.is_published && building.url && building.address) {
                var promise ;
                var shipboolean;
                if (shipselect==0) {
                    promise = $scope.anyAPI.addBuilding(building);
                    shipboolean=1;
                }
                else{
                    promise = $scope.anyAPI.addShip(building)
                    shipboolean=0;
                }
                promise.then(
                    function (resp) {
                        // on success
                        var data = resp.data;
                        console.log("new buid: " + data.buid);
                        building.buid = data.buid;

                        if (building.is_published === 'true' || building.is_published == true) {
                            building.is_published = true;
                        } else {
                            building.is_published = false;
                        }

                        // insert the newly created building inside the loadedBuildings
                        if(shipselect==0) {
                            $scope.myBuildings.push(building);
                            $scope.anyService.selectedBuilding = $scope.myBuildings[$scope.myBuildings.length - 1];

                        }
                        else{
                            $scope.myShips.push(building);
                            // $scope.anyService.selectedBuilding = $scope.myBuildings[$scope.myBuildings.length - 1];


                        }

                        $scope.myMarkers[id].marker.setDraggable(false);

                        $scope.myBuildingsHashT[building.buid] = {
                            marker: $scope.myMarkers[id].marker,
                            model: building
                        };

                        if ($scope.myMarkers[id].infowindow) {
                            $scope.myMarkers[id].infowindow.setContent($scope.myMarkers[id].marker.tpl2[0]);
                            $scope.myMarkers[id].infowindow.close();
                        }
                        if(shipboolean){

                            _suc("Building added successfully.");
                        }
                        else {
                            _suc("Ship added successfully.");
                        }
                    },
                    function (resp) {
                        // on error
                        var data = resp.data;
                        _err("Something went wrong while adding the building. " + data.message);
                    }
                );


            } else {
                _err("Some required fields are missing.");
            }
        }
    };

    $scope.deleteBuilding = function () {

        var b = $scope.anyService.getBuilding();

        var reqObj = $scope.creds;

        if (!$scope.owner_id) {
            _err("Could not identify user. Please refresh and sign in again.");
            return;
        }

        reqObj.owner_id = $scope.owner_id;

        console.log( reqObj.owner_id);

        if (!b || !b.buid) {
            _err("No building selected for deletion.");
            return;
        }

        reqObj.buid = b.buid;

        var promise = $scope.anyAPI.deleteBuilding(reqObj);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                console.log("building deleted: ", b);

                // delete the building from the loadedBuildings
                $scope.myBuildingsHashT[b.buid].marker.setMap(null);
                delete $scope.myBuildingsHashT[b.buid];

                var bs = $scope.myBuildings;
                var sz = bs.length;
                for (var i = 0; i < sz; i++) {
                    if (bs[i].buid == b.buid) {
                        bs.splice(i, 1);
                        break;
                    }
                }

                // update the selected building
                if ($scope.myBuildings && $scope.myBuildings.length > 0) {
                    $scope.anyService.selectedBuilding = $scope.myBuildings[0];
                }

                $scope.setCrudTabSelected(1);

                _suc("Successfully deleted building.");
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err("Something went wrong. It's likely that everything related to the building is deleted but please refresh to make sure or try again.");
            }
        );

    };


    //lsolea01

    $scope.deleteRadiomaps = function () {

        var jsonReq = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber()};

        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;

        var promise = $scope.anyAPI.deleteRadiomaps(jsonReq);

        promise.then(
            function (resp) {

                _suc("Successfully Delete floor radiomap")


            },

            function (resp) {
                // on error
                var data = resp.data;
                _err("Something went wrong.The file is not exist ");
            }
        );


    };


    $scope.updateBuilding = function () {
        var b = $scope.anyService.getBuilding();

        if (LPUtils.isNullOrUndefined(b) || LPUtils.isNullOrUndefined(b.buid)) {
            _err("No building selected found.");
            return;
        }

        var reqObj = {};

        // from controlBarController
        reqObj = $scope.creds;
        if (!$scope.owner_id) {
            _err("Could not identify user. Please refresh and sign in again.");
            return;
        }

        reqObj.owner_id = $scope.owner_id;

        reqObj.buid = b.buid;

        if (b.description) {
            reqObj.description = b.description;
        }

        if (b.name) {
            reqObj.name = b.name;
        }

        if (b.is_published === true || b.is_published == "true") {
            reqObj.is_published = "true";
        } else {
            reqObj.is_published = "false";
        }

        if (b.bucode) {
            reqObj.bucode = b.bucode;
        }

        var marker = $scope.myBuildingsHashT[b.buid].marker;
        if (marker) {
            var latLng = marker.position;
            if (latLng && latLng.lat() && latLng.lng()) {
                reqObj.coordinates_lat = String(latLng.lat());
                reqObj.coordinates_lon = String(latLng.lng());
            }
        }

        var promise = $scope.anyAPI.updateBuilding(reqObj);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                if (b.is_published === 'true' || b.is_published == true) {
                    b.is_published = true;
                } else {
                    b.is_published = false;
                }

                _suc("Successfully updated building.")
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err("Something went wrong while updating building. " + data.message);
            }
        );

    };

    $scope.fetchAllCampus = function () {
        var jsonReq = {};
        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;
        jsonReq.owner_id = $scope.owner_id;
        $scope.myCampus = [];
        if (!jsonReq.owner_id) {
            _err("Could nor authorize user. Please refresh.");
            return;
        }

        var promise = $scope.anyAPI.allCampus(jsonReq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;
                //var bs = JSON.parse( data.buildings );
                $scope.myCampus = data.buildingsets;
                var localStoredCampusIndex = -1;
                var localStoredCampusId = undefined;
                if (typeof(Storage) !== "undefined" && localStorage && localStorage.getItem('lastCampus')) {
                    localStoredCampusId = localStorage.getItem('lastCampus');
                }

                for (var i = 0; i < $scope.myCampus.length; i++) {

                    var b = $scope.myCampus[i];

                    if (localStoredCampusId && localStoredCampusId === b.cuid) {
                        localStoredCampusIndex = i;
                    }
                }

                // using the latest building set form localStorage
                if (localStoredCampusIndex >= 0) {
                    $scope.anyService.selectedCampus = $scope.myCampus[localStoredCampusIndex];
                } else if ($scope.myCampus[0]) {
                    $scope.anyService.selectedCampus = $scope.myCampus[0];
                }
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err('Something went wrong while fetching buildings.');
            }
        );
    };

    $scope.updateCampus = function () {
        var b = $scope.anyService.getCampus();

        if (LPUtils.isNullOrUndefined(b) || LPUtils.isNullOrUndefined(b.cuid)) {
            _err("No campus selected found.");
            return;
        }

        var reqObj = {};

        // from controlBarController
        reqObj = $scope.creds;
        if (!$scope.owner_id) {
            _err("Could not identify user. Please refresh and sign in again.");
            return;
        }

        reqObj.owner_id = $scope.owner_id;

        reqObj.cuid = b.cuid;

        if (b.description) {
            reqObj.description = b.description;
        }

        if (b.name) {
            reqObj.name = b.name;
        }

        if (b.newcuid) {
            reqObj.newcuid = b.newcuid;
        }

        var sz = $scope.example9modeledit.length;

        if (sz == 0) {
            _err("No buildings selected.");
            return;
        }
        var buids = "\"buids\":[";
        for (var i = sz - 1; i > 0; i--) {
            buids = buids + "\"" + $scope.example9modeledit[i].id + "\",";
        }
        buids = buids + "\"" + $scope.example9modeledit[0].id + "\"]";

        reqObj.greeklish = document.getElementById("Greeklish-OnOffedit").checked;

        reqObj.buids = buids;


        var promise = $scope.anyAPI.updateCampus(reqObj);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;
                document.getElementById("CampusIDeditnew").value = "";
                _suc("Successfully updated campus.")
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err("Something went wrong while updating campus. " + data.message);
            }
        );

    };

    $scope.deleteCampus = function () {

        var b = $scope.anyService.getCampus();

        var reqObj = $scope.creds;

        if (!$scope.owner_id) {
            _err("Could not identify user. Please refresh and sign in again.");
            return;
        }

        reqObj.owner_id = $scope.owner_id;

        if (!b || !b.cuid) {
            _err("No Campus selected for deletion.");
            return;
        }

        reqObj.cuid = b.cuid;

        var promise = $scope.anyAPI.deleteCampus(reqObj);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                console.log("campus deleted: ", b);


                var bs = $scope.myCampus;
                var sz = bs.length;
                for (var i = 0; i < sz; i++) {
                    if (bs[i].cuid == b.cuid) {
                        bs.splice(i, 1);
                        break;
                    }
                }

                // update the selected building
                if ($scope.myCampus && $scope.myCampus.length > 0) {
                    $scope.anyService.selectedCampus = $scope.myCampus[0];
                }
                else if ($scope.myCampus.length == 0) {
                    $scope.anyService.selectedCampus = undefined;
                }

                $scope.setCrudTabSelected(1);

                _suc("Successfully deleted campus.");
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err("Something went wrong. It's likely that everything related to the campus is deleted but please refresh to make sure or try again.");
            }
        );

    };

    $scope.addCampus = function () {

        var name_element = document.getElementById("CampusName");
        var name = "\"name\":\"" + name_element.value + "\"";

        if (document.getElementById("CampusDescription").value.localeCompare("") == 0) {
            document.getElementById("CampusDescription").value = "-";
        }

        var des = document.getElementById("CampusDescription");
        var des = "\"description\":\"" + des.value + "\"";

        var mycuid = document.getElementById("CampusID");
        var mycuid = "\"cuid\":\"" + mycuid.value + "\"";

        var greeklish = document.getElementById("Greeklish-OnOff").checked;
        greeklish = "\"greeklish\":\"" + greeklish + "\"";
        var sz = $scope.example9model.length;

        if (sz == 0) {
            _err("No buildings selected.");
            return;
        }
        var buids = "\"buids\":[";
        for (var i = sz - 1; i > 0; i--) {
            buids = buids + "\"" + $scope.example9model[i].id + "\",";
        }
        buids = buids + "\"" + $scope.example9model[0].id + "\"]";

        var jreq = "{" + greeklish + "," + buids + "," + mycuid + "," + des + "," + name + ",\"owner_id\":\"" + $scope.owner_id + "\",\"access_token\":\"" + $scope.gAuth.access_token + "\"}";
        //alert(document.getElementById("Greeklish-OnOff").checked);
        var promise = $scope.anyAPI.addBuildingSet(jreq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;
                var new_campus = {};
                new_campus.name = document.getElementById("CampusName").value;
                new_campus.buids = buids.buids;
                new_campus.description = document.getElementById("CampusDescription").value;
                new_campus.cuid = data.cuid;
                $scope.myCampus.push(new_campus);
                $scope.anyService.selectedCampus = $scope.myCampus[$scope.myCampus.length - 1];
                _suc("Successfully added campus.");

                function S4() {
                    return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
                }

                var guid = (S4() + S4() + "-" + S4() + "-4" + S4().substr(0, 3) + "-" + S4() + "-" + S4() + S4() + S4()).toLowerCase();
                var d = new Date();
                document.getElementById("CampusID").value = "cuid_" + guid + "_" + d.getTime();
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err("Something went wrong while adding the building. " + data.message);
            }
        );

    };

    var overlay = new google.maps.OverlayView();
    overlay.draw = function () {
    };
    overlay.setMap(GMapService.gmap);

    $("#draggable-building").draggable({
        helper: 'clone',
        stop: function (e) {
            var point = new google.maps.Point(e.pageX, e.pageY);
            var ll = overlay.getProjection().fromContainerPixelToLatLng(point);
            $scope.placeMarker(ll);
            shipselect=0;
        }
    });

// ship drag
    $("#draggable-ship").draggable({

        helper: 'clone',
        stop: function (e) {
            var point = new google.maps.Point(e.pageX, e.pageY);
            var ll = overlay.getProjection().fromContainerPixelToLatLng(point);
            $scope.placeMarkership(ll);
            shipselect=1;
        }
    });

    $scope.placeMarkership = function (location) {

        var prevMarker = $scope.myMarkers[$scope.myMarkerId - 1];

        if (prevMarker && prevMarker.marker && prevMarker.marker.getMap() && prevMarker.marker.getDraggable()) {
            // TODO: alert for already pending building.
            console.log('there is a building pending, please add 1 at a time');
            return;
        }

        var marker = new google.maps.Marker({
            position: location,
            map: GMapService.gmap,
            icon: 'build/images/ship_icon.png',
            draggable: true
        });

        var infowindow = new google.maps.InfoWindow({
            content: '-',
            maxWidth: 500
        });

        $scope.$apply(function () {
            marker.myId = $scope.myMarkerId;
            $scope.myMarkers[marker.myId] = {};
            $scope.myMarkers[marker.myId].model = {
                description: "",
                name: undefined,
                is_published: true,
                address: "-",
                url: "-",
                bucode: ""
            };
            $scope.myMarkers[marker.myId].marker = marker;
            $scope.myMarkers[marker.myId].infowindow = infowindow;
            $scope.myMarkerId++;
        });

        var htmlContent = '<form name="buildingForm" class="infowindow-scroll-fix">'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.bucode" id="building-code" type="text" class="form-control" placeholder="Building Code (Optional)"/>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.name" id="building-name" type="text" class="form-control" placeholder="Building Name *"/>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<textarea ng-model="myMarkers[' + marker.myId + '].model.description" id="building-description" type="text" class="form-control" placeholder="Building Description (Optional)"></textarea>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.is_published" id="building-published" type="checkbox"><span> Make building public to view.</span>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '</fieldset class="form-group">'
            + '<div style="text-align: center;">'
            + '<fieldset class="form-group" style="display: inline-block; width: 75%;">'
            + '<button type="submit" class="btn btn-success add-any-button" ng-click="addNewBuilding(' + marker.myId + ')">'
            + '<span class="glyphicon glyphicon-plus"></span> Add'
            + '</button>'
            + '</fieldset>'
            + '<fieldset class="form-group" style="display: inline-block;width: 23%;">'
            + '<button class="btn btn-danger add-any-button" style="margin-left:2px" ng-click="deleteTempBuilding(' + marker.myId + ')"><span class="glyphicon glyphicon-remove"></span>'
            + '</button>'
            + '</fieldset>'
            + '</div>'
            + '</form>';

        var htmlContent2 = '<div class="infowindow-scroll-fix">'
            + '<h5 style="margin: 0">Building:</h5>'
            + '<span>{{myMarkers[' + marker.myId + '].model.name}}</span>'
            + '<h5 style="margin: 8px 0 0 0">Description:</h5>'
            + '<span>{{myMarkers[' + marker.myId + '].model.description}}</span>'
            + '</div>';

        var tpl = $compile(htmlContent)($scope);
        marker.tpl2 = $compile(htmlContent2)($scope);

        infowindow.setContent(tpl[0]);
        infowindow.open(GMapService.gmap, marker);

        google.maps.event.addListener(marker, 'click', function () {
            if (!infowindow.getMap()) {
                infowindow.open(GMapService.gmap, marker);
            }
        });
    };


    $scope.placeMarker = function (location) {

        var prevMarker = $scope.myMarkers[$scope.myMarkerId - 1];

        if (prevMarker && prevMarker.marker && prevMarker.marker.getMap() && prevMarker.marker.getDraggable()) {
            // TODO: alert for already pending building.
            console.log('there is a building pending, please add 1 at a time');
            return;
        }

        var marker = new google.maps.Marker({
            position: location,
            map: GMapService.gmap,
            icon: 'build/images/building-icon.png',
            draggable: true
        });

        var infowindow = new google.maps.InfoWindow({
            content: '-',
            maxWidth: 500
        });

        $scope.$apply(function () {
            marker.myId = $scope.myMarkerId;
            $scope.myMarkers[marker.myId] = {};
            $scope.myMarkers[marker.myId].model = {
                description: "",
                name: undefined,
                is_published: true,
                address: "-",
                url: "-",
                bucode: ""
            };
            $scope.myMarkers[marker.myId].marker = marker;
            $scope.myMarkers[marker.myId].infowindow = infowindow;
            $scope.myMarkerId++;
        });

        var htmlContent = '<form name="buildingForm" class="infowindow-scroll-fix">'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.bucode" id="building-code" type="text" class="form-control" placeholder="Building Code (Optional)"/>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.name" id="building-name" type="text" class="form-control" placeholder="Building Name *"/>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<textarea ng-model="myMarkers[' + marker.myId + '].model.description" id="building-description" type="text" class="form-control" placeholder="Building Description (Optional)"></textarea>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.is_published" id="building-published" type="checkbox"><span> Make building public to view.</span>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '</fieldset class="form-group">'
            + '<div style="text-align: center;">'
            + '<fieldset class="form-group" style="display: inline-block; width: 75%;">'
            + '<button type="submit" class="btn btn-success add-any-button" ng-click="addNewBuilding(' + marker.myId + ')">'
            + '<span class="glyphicon glyphicon-plus"></span> Add'
            + '</button>'
            + '</fieldset>'
            + '<fieldset class="form-group" style="display: inline-block;width: 23%;">'
            + '<button class="btn btn-danger add-any-button" style="margin-left:2px" ng-click="deleteTempBuilding(' + marker.myId + ')"><span class="glyphicon glyphicon-remove"></span>'
            + '</button>'
            + '</fieldset>'
            + '</div>'
            + '</form>';

        var htmlContent2 = '<div class="infowindow-scroll-fix">'
            + '<h5 style="margin: 0">Building:</h5>'
            + '<span>{{myMarkers[' + marker.myId + '].model.name}}</span>'
            + '<h5 style="margin: 8px 0 0 0">Description:</h5>'
            + '<span>{{myMarkers[' + marker.myId + '].model.description}}</span>'
            + '</div>';

        var tpl = $compile(htmlContent)($scope);
        marker.tpl2 = $compile(htmlContent2)($scope);

        infowindow.setContent(tpl[0]);
        infowindow.open(GMapService.gmap, marker);

        google.maps.event.addListener(marker, 'click', function () {
            if (!infowindow.getMap()) {
                infowindow.open(GMapService.gmap, marker);
            }
        });
    };

    $scope.deleteTempBuilding = function (id) {
        var myMarker = $scope.myMarkers[id];
        if (myMarker && myMarker.marker) {
            myMarker.marker.setMap(null);
        }
    };

    /**
     * building {
     *  name:
     *  description:
     *  coordinates_lat:
     *  coordinates_lon:
     *  floors: [
     *      0: {
     *          pois: [
     *              name:
     *              description:
     *              pois_type:
     *              is_building_entrance:
     *              coordinates_lat:
     *              coordinates_lon:
     *          ]
     *      }
     *  ];
     * }
     */
    $scope.exportBuildingToJson = function () {
        var result = {
            building: {
                floors: []
            }
        };

        var building = $scope.anyService.selectedBuilding;
        if (LPUtils.isNullOrUndefined(building)) {
            _err('No building selected');
            return;
        }
        result.building.buid = building.buid;
        result.building.name = building.name;
        result.building.description = building.description;
        result.building.coordinates_lat = building.coordinates_lat;
        result.building.coordinates_lon = building.coordinates_lon;

        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = building.buid;

        var count = 0;

        var promise = AnyplaceAPIService.allBuildingFloors(jsonReq);
        promise.then(
            function (resp) {
                var floors = resp.data.floors;

                var resFloors = [];

                if (floors) {
                    for (var i = 0; i < floors.length; i++) {

                        (function (jreq) {
                            var promise = AnyplaceAPIService.retrievePoisByBuildingFloor(jreq);
                            promise.then(
                                function (resp) {
                                    var data = resp.data;

                                    var poisArray = data.pois;

                                    if (poisArray) {

                                        var flPois = [];

                                        var fNo = poisArray[0].floor_number;

                                        for (var j = 0; j < poisArray.length; j++) {
                                            var sPoi = poisArray[j];

                                            if (sPoi.pois_type == "None") {
                                                continue;
                                            }
                                            if (sPoi.overwrite) {
                                                var tmp = {
                                                    name: sPoi.name,
                                                    description: sPoi.description,
                                                    puid: sPoi.puid,
                                                    pois_type: sPoi.pois_type,
                                                    coordinates_lat: sPoi.coordinates_lat,
                                                    coordinates_lon: sPoi.coordinates_lon,
                                                    overwrite: sPoi.overwrite
                                                };
                                            }
                                            else {
                                                var tmp = {
                                                    name: sPoi.name,
                                                    description: sPoi.description,
                                                    puid: sPoi.puid,
                                                    pois_type: sPoi.pois_type,
                                                    coordinates_lat: sPoi.coordinates_lat,
                                                    coordinates_lon: sPoi.coordinates_lon,
                                                    overwrite: "false"
                                                };
                                            }
                                            flPois.push(tmp);
                                        }

                                        resFloors.push(
                                            {
                                                floor_number: fNo,
                                                pois: flPois
                                            }
                                        );

                                        count++;
                                        if (count === floors.length) {
                                            result.building.floors = resFloors;
                                            _suc('Successfully exported ' + building.name + ' to JSON.');
                                            var blob = new Blob([JSON.stringify(result, null, 4)], {type: "text/plain;charset=utf-8"});
                                            saveAs(blob, building.name.toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
                                        }

                                    }
                                },
                                function (resp) {
                                    var data = resp.data;
                                    console.log(data.message);
                                });
                        }({
                            buid: building.buid,
                            floor_number: floors[i].floor_number
                        }));
                    }
                }
            },
            function (resp) {
                // TODO: alert failure
                console.log(resp.data.message);
            }
        );
    };

    /**
     * campus {
   "buids": [
   ],
   "description": "",
   "name": "",
   "owner_id": "",
   "access_token": "",
   "cuid": ""
    }
     * }
     */
    $scope.exportCampusToJson = function () {
        var result = {
            campus: {
                buids: []
            }
        };

        var campus = $scope.anyService.selectedCampus;
        if (LPUtils.isNullOrUndefined(campus)) {
            _err('No campus selected');
            return;
        }

        result.campus.name = campus.name;
        result.campus.description = campus.description;
        result.campus.buids = campus.buids;

        _suc('Successfully exported ' + campus.name + ' to JSON.');

        var blob = new Blob([JSON.stringify(result, null, 4)], {type: "text/plain;charset=utf-8"});
        saveAs(blob, campus.name.toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
    };

    function readSingleFile(e) {
        var file = e.target.files[0];
        if (!file) {
            return;
        }
        var reader = new FileReader();
        reader.onload = function (e) {
            var contents = e.target.result;
            $scope.fileToUpload = contents;
        };
        reader.readAsText(file);
    }

    document.getElementById('file-input')
        .addEventListener('change', readSingleFile, false);

    $scope.anyService.downloadlogfile = false;

    $scope.importBuildingFromJson = function () {

        $scope.anyService.progress = 0;
        var i, j, count = 0, countok = 0;
        if ($scope.fileToUpload == "") {
            _err("Something went wrong no file selected");
        }
        var obj = JSON.parse($scope.fileToUpload);
        for (i = 0; i < obj.building.floors.length; i++) {
            for (j = 0; j < obj.building.floors[i].pois.length; j++) {
                if (obj.building.floors[i].pois[j].overwrite == "true") {
                    count++;
                }
            }
        }
        if (count == 0) {
            _err("Something went wrong no pois to update");
        }

        for (i = 0; i < obj.building.floors.length; i++) {
            for (j = 0; j < obj.building.floors[i].pois.length; j++) {
                if (obj.building.floors[i].pois[j].overwrite == "true") {
                    countok++;
                    $scope.updatePoifromFile(obj.building.floors[i].pois[j].puid,
                        obj.building.floors[i].pois[j].coordinates_lat,
                        obj.building.floors[i].pois[j].coordinates_lon,
                        obj.building.floors[i].pois[j].name,
                        obj.building.floors[i].pois[j].description,
                        obj.building.floors[i].pois[j].pois_type,
                        obj.building.floors[i].pois[j].overwrite,
                        obj.building.buid, i, j, count, countok,
                        obj.building.name
                    );
                }
            }
        }
    }

    $scope.allpois = {};
    $scope.pois = {};

    $scope.importBuildingFromExcel = function (oEvent) {
        $scope.anyService.progress = 0;
        // Get The File From The Input
        var oFile = document.getElementById('my_file_input').files[0];
        $scope.fileToUpload = oFile.name;
        // Create A File Reader HTML5
        var reader = new FileReader();
        // Ready The Event For When A File Gets Selected
        reader.onload = function (e) {
            var data = e.target.result;
            var cfb = XLS.CFB.read(data, {type: 'binary'});
            var wb = XLS.parse_xlscfb(cfb);
            // Loop Over Each Sheet
            wb.SheetNames.forEach(function (sheetName) {
                // Obtain The Current Row As CSV
                var oJS = XLS.utils.sheet_to_row_object_array(wb.Sheets[sheetName]);
                var last_buid = "";
                $scope.uploadloop(0, oJS, last_buid);
            });
        };

        // Tell JS To Start Reading The File.. You could delay this if desired
        reader.readAsBinaryString(oFile);
    }

    $scope.uploadloop = function (potition, oJS, last_buid) {

        for (var i = potition; i < oJS.length; i++) {
            if (last_buid != oJS[i].buid && oJS[i].buid != undefined) {
                $scope.pois = {};
                last_buid = oJS[i].buid;
                $scope.fetchAllPoisForBuilding(oJS[i].buid, oJS, i, last_buid);
                return;
            }
            var description = "";

            if (oJS[i].des3 != undefined && oJS[i].des3 != "" && oJS[i].des3 != null) {
                if (oJS[i].des4 != undefined && oJS[i].des4 != "" && oJS[i].des4 != null) {
                    description = description + oJS[i].des3 + " " + oJS[i].des4;
                }
                else {
                    description = description + oJS[i].des3;
                }
            }
            else {
                if (oJS[i].des4 != undefined && oJS[i].des4 != "" && oJS[i].des4 != null) {
                    description = description + oJS[i].des4;
                }
            }

            if (oJS[i].des1 != undefined && oJS[i].des1 != "" && oJS[i].des1 != null) {
                if (description != "") {
                    description = description + "\n" + oJS[i].des1;
                }
                else {
                    description = description + oJS[i].des1;
                }
            }

            if (oJS[i].des2 != undefined && oJS[i].des2 != "" && oJS[i].des2 != null) {
                if (description != "") {
                    description = description + "\n" + oJS[i].des2;
                }
                else {
                    description = description + oJS[i].des2;
                }
            }

            $scope.anyService.progress = (i / (oJS.length - 1)) * 100;
            if ((oJS.length - 1) == 0) $scope.anyService.progress = 100;

            if ($scope.pois[oJS[i].name]) {
                $scope.updatePoifromExcel($scope.pois[oJS[i].name].puid,
                    $scope.pois[oJS[i].name].coordinates_lat,
                    $scope.pois[oJS[i].name].coordinates_lon,
                    $scope.pois[oJS[i].name].is_building_entrance,
                    oJS[i].name,
                    description,
                    $scope.pois[oJS[i].name].pois_type,
                    $scope.pois[oJS[i].name].overwrite,
                    $scope.pois[oJS[i].name].buid,
                    ""
                );
            }
            else {
                $scope.updatePoifromExcel("",
                    "",
                    "",
                    "",
                    oJS[i].name,
                    description,
                    "",
                    "",
                    "",
                    ""
                );
            }

        }
    };

    $scope.fetchAllPoisForBuilding = function (building, oJS, position, last_buid) {
        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = building;

        var promise = AnyplaceAPIService.retrievePoisByBuilding(jsonReq);
        promise.then(
            function (resp) {
                var data = resp.data;

                $scope.myPois = data.pois;

                var sz = $scope.myPois.length;
                for (var i = sz - 1; i >= 0; i--) {
                    var name = $scope.myPois[i].name;
                    $scope.pois[name] = $scope.myPois[i];
                }
                $scope.uploadloop(position, oJS, last_buid);
            },
            function (resp) {
                var data = resp.data;
                _err("Something went wrong while fetching POIs");
            }
        );
    };
    $scope.logfile = {
        pois: []
    };
    $scope.updatePoifromExcel = function (id, lat, lng, building_entrance, nm, des, ptype, ovwrite, bid, buildingname) {

        var obj = {};

        obj.username = $scope.creds.username;
        obj.password = $scope.creds.password;
        obj.owner_id = $scope.owner_id;

        obj.coordinates_lat = lat;
        obj.coordinates_lon = lng;

        obj.is_building_entrance = building_entrance;
        obj.name = nm;
        obj.puid = id;
        obj.description = des;
        obj.pois_type = ptype;
        obj.overwrite = "false";
        obj.buid = bid;
        // make the request at AnyplaceAPI
        var promise = $scope.anyAPI.updatePois(obj);
        promise.then(
            function (resp) {
                var data = resp.data;
                if ($scope.anyService.progress == 100) {

                    if ($scope.anyService.downloadlogfile) {
                        $scope.anyService.downloadlogfile = false;
                        _suc("Successfully updated POIs.A log file will be downloaded");
                        var blob = new Blob([JSON.stringify($scope.logfile, null, 4)], {type: "text/plain;charset=utf-8"});
                        saveAs(blob, ($scope.fileToUpload + "-log_file").toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                        }, 2500);
                    }
                    else {
                        _suc("Successfully updated POIs.");
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                            location.reload();
                        }, 2500);
                    }
                }
            },
            function (resp) {
                var data = resp.data;
                $scope.logfile.pois.push({
                    "name": nm,
                    "puid": id,
                    "description": des,
                    "status": "Something went wrong while updating POI."
                });
                $scope.anyService.downloadlogfile = true;
                if ($scope.anyService.progress == 100) {
                    $scope.anyService.downloadlogfile = false;
                    _suc("Successfully updated POIs.A log file will be downloaded");
                    var blob = new Blob([JSON.stringify($scope.logfile, null, 4)], {type: "text/plain;charset=utf-8"});
                    saveAs(blob, ($scope.fileToUpload + "-log_file").toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
                    $scope.logfile.pois = [];

                    setTimeout(function () {
                        $scope.anyService.progress = undefined;
                    }, 2500);
                }
            }
        );
    };


    $scope.updatePoifromFile = function (id, lat, lng, nm, des, ptype, ovwrite, bid, i, j, count, countok, buildingname) {

        var obj = {};

        obj.username = $scope.creds.username;
        obj.password = $scope.creds.password;
        obj.owner_id = $scope.owner_id;

        obj.coordinates_lat = lat;
        obj.coordinates_lon = lng;

        obj.name = nm;
        obj.puid = id;
        obj.description = des;
        obj.pois_type = ptype;
        obj.overwrite = "false";
        obj.buid = bid;
        // make the request at AnyplaceAPI
        var promise = $scope.anyAPI.updatePois(obj);
        promise.then(
            function (resp) {
                var data = resp.data;
                $scope.anyService.progress = countok / count * 100;
                if ($scope.anyService.progress == 100) {
                    if ($scope.anyService.downloadlogfile) {
                        _suc("Successfully updated POIs.A log file will be downloaded");
                        var blob = new Blob([JSON.stringify($scope.logfile, null, 4)], {type: "text/plain;charset=utf-8"});
                        saveAs(blob, (buildingname + "-log_file").toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                        }, 2500);
                    }
                    else {
                        _suc("Successfully updated POIs.");
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                            location.reload();
                        }, 2500);
                    }
                }
            },
            function (resp) {
                var data = resp.data;

                $scope.logfile.pois.push({
                    "name": nm,
                    "puid": id,
                    "description": des,
                    "status": "Something went wrong while updating POI."
                });

                $scope.anyService.downloadlogfile = true;
                $scope.anyService.progress = countok / count * 100;
                if ($scope.anyService.progress == 100) {

                    if ($scope.anyService.downloadlogfile) {
                        _suc("Successfully updated POIs.A log file will be downloaded");
                        var blob = new Blob([JSON.stringify($scope.logfile, null, 4)], {type: "text/plain;charset=utf-8"});
                        saveAs(blob, (buildingname + "-log_file").toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
                        $scope.logfile.pois = [];
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                        }, 2500);
                    }
                    else {
                        _suc("Successfully updated POIs.");
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                            location.reload();
                        }, 2500);
                    }
                }
            }
        );
    };

    $scope.Poisresult = {
        building: {
            floors: []
        }
    };

    $scope.Connectionsresult = {
        building: {}
    };

    $scope.zip = new JSZip();
    $scope.DownloadBackup = function () {

        var b = $scope.anyService.selectedBuilding;
        var xFloors = [];
        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = b.buid;
        $scope.anyService.progress = 0;
        var promise = AnyplaceAPIService.allBuildingFloors(jsonReq);
        promise.then(
            function (resp) {
                xFloors = resp.data.floors;
                var floor = 0;
                var floor_number = "";
                for (var i = 0; i < xFloors.length; i++) {
                    if (i == 0) {
                        floor_number = xFloors[i].floor_number;
                    }
                    else {
                        floor_number = floor_number + " " + xFloors[i].floor_number;
                    }
                }
                $scope.anyService.progress = 10;
                var buid = b.buid;
                var jsonReq2 = AnyplaceService.jsonReq;
                var promise2 = AnyplaceAPIService.downloadFloorPlanAll(jsonReq2, buid, floor_number);
                promise2.then(
                    function (resp) {
                        // on success
                        var data = resp.data;
                        var img = $scope.zip.folder("floor_plans");
                        for (var si = 0; si < data.all_floors.length; si++) {
                            if (data.all_floors[si] != "") {
                                img.file(xFloors[si].floor_number + ".png", data.all_floors[si], {base64: true});
                            }
                        }
                        $scope.anyService.progress = 25;
                        var jsonReq3 = AnyplaceService.jsonReq;
                        jsonReq3.buid = buid;
                        jsonReq3.floor = floor_number;
                        var promise3 = AnyplaceAPIService.getRadioByBuildingFloorAll(jsonReq3);
                        promise3.then(
                            function (resp) {
                                var data2 = resp.data;
                                var logs = $scope.zip.folder("radiomaps");
                                if (data2.rss_log_files) {
                                    var urls = "";
                                    for (var si2 = 0; si2 < data2.rss_log_files.length; si2++) {
                                        logs.file(xFloors[si2].floor_number + "-radiomap.txt", data2.rss_log_files[si2]);
                                    }
                                }
                                $scope.anyService.progress = 70;
                                $scope.exportPoisBuildingToJson();
                            },
                            function (resp) {

                            }
                        );
                    },
                    function (resp) {
                    }
                );
            },
            function (resp) {
                console.log(resp.data.message);
                _err("Something went wrong while fetching all floors");
            }
        );


    }

    $scope.exportPoisBuildingToJson = function () {

        var building = $scope.anyService.selectedBuilding;
        if (LPUtils.isNullOrUndefined(building)) {
            _err('No building selected');
            return;
        }
        $scope.Poisresult.building.buid = building.buid;
        $scope.Poisresult.building.name = building.name;
        $scope.Poisresult.building.description = building.description;
        $scope.Poisresult.building.coordinates_lat = building.coordinates_lat;
        $scope.Poisresult.building.coordinates_lon = building.coordinates_lon;

        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = building.buid;

        var count = 0;

        var promise = AnyplaceAPIService.allBuildingFloors(jsonReq);
        promise.then(
            function (resp) {
                var floors = resp.data.floors;

                var resFloors = [];
                $scope.anyService.progress = 80;
                if (floors) {
                    for (var i = 0; i < floors.length; i++) {

                        (function (jreq) {
                            var promise = AnyplaceAPIService.retrievePoisByBuildingFloor(jreq);
                            promise.then(
                                function (resp) {
                                    var data = resp.data;

                                    var poisArray = data.pois;

                                    if (poisArray) {

                                        var flPois = [];

                                        if (poisArray[0] != undefined) {
                                            var fNo = poisArray[0].floor_number;

                                            for (var j = 0; j < poisArray.length; j++) {
                                                var sPoi = poisArray[j];

                                                if (sPoi.pois_type == "None") {
                                                    continue;
                                                }
                                                if (sPoi.overwrite) {
                                                    var tmp = {
                                                        name: sPoi.name,
                                                        description: sPoi.description,
                                                        puid: sPoi.puid,
                                                        pois_type: sPoi.pois_type,
                                                        coordinates_lat: sPoi.coordinates_lat,
                                                        coordinates_lon: sPoi.coordinates_lon,
                                                        overwrite: sPoi.overwrite
                                                    };
                                                }
                                                else {
                                                    var tmp = {
                                                        name: sPoi.name,
                                                        description: sPoi.description,
                                                        puid: sPoi.puid,
                                                        pois_type: sPoi.pois_type,
                                                        coordinates_lat: sPoi.coordinates_lat,
                                                        coordinates_lon: sPoi.coordinates_lon,
                                                        overwrite: "false"
                                                    };
                                                }

                                                if (sPoi.is_building_entrance == 'true') {
                                                    tmp.is_building_entrance = 'true';
                                                }
                                                else {
                                                    tmp.is_building_entrance = 'false';
                                                }

                                                flPois.push(tmp);
                                            }

                                            resFloors.push(
                                                {
                                                    floor_number: fNo,
                                                    pois: flPois
                                                }
                                            );
                                        }
                                        count++;
                                        if (count === floors.length) {
                                            $scope.Poisresult.building.floors = resFloors;
                                            $scope.zip.file("allpois.json", JSON.stringify($scope.Poisresult, null, 4));
                                            $scope.anyService.progress = 90;
                                            $scope.exportConnectionBuildingToJson();
                                        }

                                    }
                                },
                                function (resp) {
                                    var data = resp.data;
                                    console.log(data.message);
                                });
                        }({
                            buid: building.buid,
                            floor_number: floors[i].floor_number
                        }));
                    }
                }
            },
            function (resp) {
                // TODO: alert failure
                console.log(resp.data.message);
            }
        );
    };

    $scope.exportConnectionBuildingToJson = function () {

        var building = $scope.anyService.selectedBuilding;
        if (LPUtils.isNullOrUndefined(building)) {
            _err('No building selected');
            return;
        }
        $scope.Connectionsresult.building.buid = building.buid;
        $scope.Connectionsresult.building.name = building.name;
        $scope.Connectionsresult.building.description = building.description;
        $scope.Connectionsresult.building.coordinates_lat = building.coordinates_lat;
        $scope.Connectionsresult.building.coordinates_lon = building.coordinates_lon;

        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = building.buid;

        var count = 0;

        var promise = AnyplaceAPIService.allBuildingFloors(jsonReq);
        promise.then(
            function (resp) {
                var floors = resp.data.floors;

                var resFloors = [];

                if (floors) {
                    for (var i = 0; i < floors.length; i++) {

                        (function (jreq) {
                            var promise = AnyplaceAPIService.retrieveConnectionsByBuildingFloor(jreq);
                            promise.then(
                                function (resp) {
                                    $scope.anyService.progress = 100;
                                    var data = resp.data;

                                    var connArray = data.connections;

                                    if (connArray) {

                                        var flConnections = [];

                                        if (connArray[0] != undefined) {

                                            var fNo = connArray[0].floor_a;

                                            for (var j = 0; j < connArray.length; j++) {
                                                var sConnection = connArray[j];

                                                var tmp = {
                                                    name: sConnection.name,
                                                    description: sConnection.description,
                                                    cuid: sConnection.puid,
                                                    weight: sConnection.weight,
                                                    pois_a: sConnection.pois_a,
                                                    pois_b: sConnection.pois_b,
                                                    floor_a: sConnection.floor_a,
                                                    floor_b: sConnection.floor_b,
                                                    is_published: sConnection.is_published,
                                                    buid_a: sConnection.buid_a,
                                                    buid_b: sConnection.buid_b
                                                };

                                                flConnections.push(tmp);
                                            }

                                            resFloors.push(
                                                {
                                                    floor_number: fNo,
                                                    connections: flConnections
                                                }
                                            );
                                        }
                                        count++;

                                        if (count === floors.length) {

                                            $scope.Connectionsresult.building.floors = resFloors;
                                            $scope.zip.file("allconnections.json", JSON.stringify($scope.Connectionsresult, null, 4));

                                            $scope.zip.generateAsync({type: "blob"})
                                                .then(function (content) {
                                                    // see FileSaver.js
                                                    saveAs(content, building.buid + ".zip");
                                                });
                                            $scope.anyService.progress = undefined;
                                        }
                                    }
                                },
                                function (resp) {
                                    var data = resp.data;
                                    console.log(data.message);
                                });
                        }({
                            buid: building.buid,
                            floor_number: floors[i].floor_number
                        }));
                    }
                }
            },
            function (resp) {
                console.log(resp.data.message);
            }
        );
    };

    //set cookies

    $('#dismiss').on('click', function () {
        // set expire date.
        if (typeof(Storage) !== "undefined" && localStorage) {
            localStorage.setItem('dismissClicked', 'YES');
        }

    });

    if (localStorage.getItem('dismissClicked') !== 'YES') {
        function showWelcomeMessage() {
            $('#myModal_Welcome').modal('show');
        }

        window.onload = showWelcomeMessage;
    }

}]);

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
app.controller('ControlBarController', ['$scope', '$rootScope', 'AnyplaceService', 'GMapService', 'AnyplaceAPIService', function ($scope, $rootScope, AnyplaceService, GMapService, AnyplaceAPIService) {

    $scope.anyService = AnyplaceService;
    $scope.gmapService = GMapService;

    $scope.isAuthenticated = false;

    $scope.signInType = "google";

    $scope.gAuth = {};
    $scope.person = undefined;

    $scope.creds = {
        username: 'username',
        password: 'password'
    };

    $scope.owner_id = undefined;
    $scope.displayName = undefined;


    var self = this; //to be able to reference to it in a callback, you could use $scope instead


    $scope.setAuthenticated = function (bool) {
        $scope.isAuthenticated = bool;
    };

    $scope.showFullControls = true;

    $scope.toggleFullControls = function () {
        $scope.showFullControls = !$scope.showFullControls;
    };

    var apiClientLoaded = function () {
        gapi.client.plus.people.get({userId: 'me'}).execute(handleEmailResponse);
    };

    var handleEmailResponse = function (resp) {
        $scope.personLookUp(resp);
    };

    $scope.showGoogleID = function () {
        if (!$scope.person) {
            return;
        }
        AnyplaceService.addAlert('success', 'Your Google ID is: ' + $scope.person.id);
    };

    $scope.showGoogleAuth = function () {
        if (!$scope.gAuth) {
            return;
        }
        AnyplaceService.addAlert('success', 'access_token: ' + $scope.gAuth.access_token);
    };

    $scope.onSignIn = function (googleUser) {

        if ($scope.getCookie("username") === "") {
            $scope.setCookie("username", "true", 365);
            location.reload();
        }

        //location.reload();
        $scope.setAuthenticated(true);

        $scope.gAuth = gapi.auth2.getAuthInstance().currentUser.get().getAuthResponse();

        $scope.gAuth.access_token = $scope.gAuth.id_token;

        app.access_token = $scope.gAuth.id_token;

        $scope.personLookUp(googleUser);
    };


    $scope.onSignInFailure = function () {
        console.log('Sign-in state: Error');
    };

    window.onSignIn = $scope.onSignIn;
    window.onSignInFailure = $scope.onSignInFailure;

    $scope.personLookUp = function (resp) {
        $scope.person = resp.getBasicProfile();
        $scope.person.image = $scope.person.getImageUrl();
        $scope.person.id = $scope.person.getId();
        $scope.person.displayName = $scope.person.getName();
        // compose user id
        $scope.owner_id = $scope.person.id + '_' + $scope.signInType;
        $scope.displayName = $scope.person.displayName;

        if ($scope.person && $scope.person.id) {
            $scope.$broadcast('loggedIn', []);
        }

        var promise = AnyplaceAPIService.signAccount({
            name: $scope.person.displayName,
            type: "google"
        });

        promise.then(
            function (resp) {
            },
            function (resp) {
            }
        );
    };

    $scope.signOut = function () {
        $scope.setCookie("username", "", 365);
        var auth2 = gapi.auth2.getAuthInstance();
        auth2.signOut().then(function () {
            console.log('User signed out.');
        });
        $scope.isAuthenticated = false;

        $scope.$broadcast('loggedOff', []);
        $scope.gAuth = {};
        $scope.owner_id = undefined;
        $scope.person = undefined;

    };

    $scope.getCookie = function (cname) {
        var name = cname + "=";
        var ca = document.cookie.split(';');
        for (var i = 0; i < ca.length; i++) {
            var c = ca[i];
            while (c.charAt(0) == ' ') {
                c = c.substring(1);
            }
            if (c.indexOf(name) == 0) {
                return c.substring(name.length, c.length);
            }
        }
        return "";
    };

    $scope.setCookie = function (cname, cvalue, exdays) {
        var d = new Date();
        d.setTime(d.getTime() + (exdays * 24 * 60 * 60 * 1000));
        var expires = "expires=" + d.toUTCString();
        document.cookie = cname + "=" + cvalue + "; " + expires;
    };

    $scope.tab = 1;

    $scope.setTab = function (num) {
        $scope.tab = num;
    };

    $scope.isTabSet = function (num) {
        return $scope.tab === num;
    };


    $scope.tab = 1;

    $scope.setTab = function (num) {
        $scope.tab = num;

    };

    $scope.isTabSet = function (num) {
        return $scope.tab === num;
    };

    var _err = function (msg) {
        $scope.anyService.addAlert('danger', msg);
    };

    var myLocMarker = undefined;

    $scope.userPosition = undefined;

    var pannedToUserPosOnce = false;

    $scope.isUserLocVisible = false;

    $scope.getIsUserLocVisible = function () {
        return $scope.isUserLocVisible;
    };

    $scope.panToUserLocation = function () {
        if (!$scope.userPosition)
            return;

        GMapService.gmap.panTo($scope.userPosition);
        GMapService.gmap.setZoom(20);
    };

    $scope.displayMyLocMarker = function (posLatlng) {
        if (myLocMarker && myLocMarker.getMap()) {
            myLocMarker.setPosition(posLatlng);
            return;
        }
    };

    $scope.displayMyLocMarker = function (posLatlng) {
        if (myLocMarker && myLocMarker.getMap()) {
            myLocMarker.setPosition(posLatlng);
            return;
        }
        var s = new google.maps.Size(20, 20);
        if ($scope.isFirefox)
            s = new google.maps.Size(48, 48);

        myLocMarker = new google.maps.Marker({
            position: posLatlng,
            map: GMapService.gmap,
            draggable: false,
            icon: {
                url: 'build/images/location-radius-centre.png',
                origin: new google.maps.Point(0, 0), /* origin is 0,0 */
                anchor: new google.maps.Point(10, 10), /* anchor is bottom center of the scaled image */
                size: s,
                scaledSize: new google.maps.Size(20, 20)
            }
        });
    };

    $scope.hideUserLocation = function () {
        if (myLocMarker)
            myLocMarker.setMap(null);
        //if (accuracyRadius)
        //    accuracyRadius.setMap(null);

        $scope.isUserLocVisible = false;
    };


    $scope.showUserLocation = function () {

        if ($scope.getIsUserLocVisible()) {
            $scope.hideUserLocation();

            if (navigator.geolocation)
                navigator.geolocation.clearWatch(watchPosNum);

            return;
        }

        if (navigator.geolocation) {
            watchPosNum = navigator.geolocation.watchPosition(
                function (position) {

                    var posLatlng = {lat: position.coords.latitude, lng: position.coords.longitude};
                    //var radius = position.coords.accuracy;

                    $scope.userPosition = posLatlng;

                    $scope.displayMyLocMarker(posLatlng);

                    var infowindow = new google.maps.InfoWindow({
                        content: 'Your current location.',
                        maxWidth: 500
                    });

                    google.maps.event.addListener(myLocMarker, 'click', function () {
                        var self = this;
                        $scope.$apply(function () {
                            infowindow.open(GMapService.gmap, self);
                        })
                    });

                    if (!$scope.isUserLocVisible) {
                        $scope.$apply(function () {
                            $scope.isUserLocVisible = true;
                        });
                    }

                    if (!pannedToUserPosOnce) {
                        GMapService.gmap.panTo(posLatlng);
                        GMapService.gmap.setZoom(19);
                        pannedToUserPosOnce = true;
                    }
                },
                function (err) {
                    $scope.$apply(function () {
                        if (err.code == 1) {
                            _err("Permission denied. Anyplace was not able to retrieve your Geolocation.")
                        } else if (err.code == 2) {
                            _err("Position unavailable. The network is down or the positioning satellites couldn't be contacted.")
                        } else if (err.code == 3) {
                            _err("Timeout. The request for retrieving your Geolocation was timed out.")
                        } else {
                            _err("There was an error while retrieving your Geolocation. Please try again.");
                        }
                    });
                });
        } else {
            _err("The Geolocation feature is not supported by this browser.");
        }
    };

    $scope.centerViewToSelectedItem = function () {
        if ($scope.anyService.selectedBuilding == null || $scope.anyService.selectedBuilding == undefined) {
            _err("You have to select a building first");
            return;

        }
        var position = {};
        if ($scope.anyService.selectedPoi) {
            var p = $scope.anyService.selectedPoi;
            position = {lat: parseFloat(p.coordinates_lat), lng: parseFloat(p.coordinates_lon)};
        } else if ($scope.anyService.selectedBuilding) {
            var b = $scope.anyService.selectedBuilding;
            position = {lat: parseFloat(b.coordinates_lat), lng: parseFloat(b.coordinates_lon)};
        } else {
            _err("No building is selected.");
            return;
        }

        $scope.gmapService.gmap.panTo(position);
        $scope.gmapService.gmap.setZoom(20);
    }

}]);
/**
 *
 The MIT License (MIT)

 Copyright (c) 2015, Kyriakos Georgiou, Marileni Angelidou, Data Management Systems Laboratory (DMSu
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



var changedfloor = false;

app.controller('FloorController', ['$scope', 'AnyplaceService', 'GMapService', 'AnyplaceAPIService', function ($scope, AnyplaceService, GMapService, AnyplaceAPIService) {
    $scope.anyService = AnyplaceService;
    $scope.anyAPI = AnyplaceAPIService;
    $scope.gmapService = GMapService;
    //$scope.controlBarService = ControlBarController;
    $scope.xFloors = [];

    $scope.myFloors = {};
    $scope.myFloorId = 0;

    $scope.newFloorNumber = 0;
    var heatmap;
    var shipselect=0;

    $scope.crudTabSelected = 1;
    $scope.setCrudTabSelected = function (n) {
        $scope.crudTabSelected = n;
    };
    $scope.isCrudTabSelected = function (n) {

        return $scope.crudTabSelected === n;
    };


    $scope.data = {
        floor_plan_coords: {},
        floor_plan_base64_data: {},
        floor_plan_groundOverlay: null
    };

    $scope.$on("loggedOff", function (event, mass) {
        _clearFloors();
    });

    var _clearFloors = function () {
        $scope.removeFloorPlan();
        $scope.xFloors = [];
        $scope.myFloorId = 0;
        $scope.myFloors = {};
    };

    var _latLngFromPoi = function (p) {
        if (p && p.coordinates_lat && p.coordinates_lon) {
            return {lat: parseFloat(p.coordinates_lat), lng: parseFloat(p.coordinates_lon)}
        }
        return undefined;
    };

    $scope.$watch('anyService.selectedBuilding', function (newVal, oldVal) {
        if (newVal) {
            changedfloor = false;

            $scope.fetchAllFloorsForBuilding(newVal);
        }
    });

    /**
     $scope.$watch('anyService.selectedPoi', function (newVal, oldVal) {
        if (newVal && _latLngFromPoi(newVal)) {
            $scope.showRadioHeatmapPoi();
        }
    });
     */
    $scope.$watch('newFloorNumber', function (newVal, oldVal) {
        //if (_floorNoExists(newVal)) {
        //    _setNextFloor();
        //}
    });

    var _err = function (msg) {
        $scope.anyService.addAlert('danger', msg);
    };

    var _suc = function (msg) {
        $scope.anyService.addAlert('success', msg);
    };

    var _latLngFromBuilding = function (b) {
        if (b && b.coordinates_lat && b.coordinates_lon) {
            return {
                lat: parseFloat(b.coordinates_lat),
                lng: parseFloat(b.coordinates_lon)
            }
        }
        return undefined;
    };

    $scope.$watch('anyService.selectedFloor', function (newVal, oldVal) {
        if (newVal !== undefined && newVal !== null && !_.isEqual(newVal, oldVal)) {
            $scope.fetchFloorPlanOverlay(newVal);
            GMapService.gmap.panTo(_latLngFromBuilding($scope.anyService.selectedBuilding));
            GMapService.gmap.setZoom(19);

            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem("lastFloor", newVal.floor_number);
            }

            changedfloor = false;
        }

    });

    $scope.fetchAllFloorsForBuilding = function (b) {
        // TODO: check for b.buid
        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = b.buid;

        var promise = AnyplaceAPIService.allBuildingFloors(jsonReq);
        promise.then(
            function (resp) {

                $scope.xFloors = resp.data.floors;

                $scope.xFloors = $scope.xFloors.sort(function (a, b) {
                    return parseInt(a.floor_number) - parseInt(b.floor_number)
                });


                $scope.anyService.availableFloors = [];
                $scope.anyService.availableFloors = $scope.xFloors;

                // give priority to floor in url parameter - if exists
                if ($scope.urlFloor) {
                    for (var k = 0; k < $scope.xFloors.length; k++) {
                        if ($scope.urlFloor == $scope.xFloors[k].floor_number) {
                            $scope.anyService.selectedFloor = $scope.xFloors[k];
                            return;
                        }
                    }
                }

                // Set default selected
                if (typeof(Storage) !== "undefined" && localStorage && !LPUtils.isNullOrUndefined(localStorage.getItem('lastBuilding')) && !LPUtils.isNullOrUndefined(localStorage.getItem('lastFloor'))) {
                    for (var i = 0; i < $scope.xFloors.length; i++) {
                        if (String($scope.xFloors[i].floor_number) === String(localStorage.getItem('lastFloor'))) {
                            $scope.anyService.selectedFloor = $scope.xFloors[i];
                            return;
                        }
                    }
                }

                // Set default the first floor if selected floor
                if ($scope.xFloors && $scope.xFloors.length > 0) {
                    $scope.anyService.selectedFloor = $scope.xFloors[0];
                } else {
                    $scope.anyService.selectedFloor = undefined;
                }

                _setNextFloor();

//                _suc("Successfully fetched all floors.");

            },
            function (resp) {
                console.log(resp.data.message);
                _err("Something went wrong while fetching all floors");
            }
        );

    };

    var _setNextFloor = function () {
        var max = -1;
        for (var i = 0; i < $scope.xFloors.length; i++) {
            if (parseInt($scope.xFloors[i].floor_number) >= max) {
                max = parseInt($scope.xFloors[i].floor_number);
            }
        }
        $scope.newFloorNumber = max + 1;
    };

    var _isValidFloorNumber = function (fl) {
        if (fl === null || fl == undefined) {
            return false;
        }

        if (fl.floor_number === null || fl.floor_number === undefined) {
            return false;
        }

        return true;
    };

    $scope.fetchFloorPlanOverlay = function () {

        if (!_isValidFloorNumber(this.anyService.selectedFloor)) {
            // TODO: alert
            console.log('something is wrong with the floor');
            return;
        }

        var floor_number = this.anyService.selectedFloor.floor_number;
        var buid = this.anyService.selectedBuilding.buid;

        var promise = AnyplaceAPIService.downloadFloorPlan(this.anyService.jsonReq, buid, floor_number);
        promise.then(
            function (resp) {

                $scope.data.floor_plan_file = null;
                $scope.data.floor_plan = null;
                if ($scope.data.floor_plan_groundOverlay != null) {
                    $scope.data.floor_plan_groundOverlay.setMap(null);
                    $scope.data.floor_plan_groundOverlay = null;
                }

                // on success
                var data = resp.data;

                // load the correct coordinates from the selected floor
                var fl = $scope.anyService.selectedFloor;
                var imageBounds = new google.maps.LatLngBounds(
                    new google.maps.LatLng(fl.bottom_left_lat, fl.bottom_left_lng),
                    new google.maps.LatLng(fl.top_right_lat, fl.top_right_lng));

                $scope.data.floor_plan_groundOverlay = new USGSOverlay(imageBounds, "data:image/png;base64," + data, GMapService.gmap);

                // TODO: alert success
            },
            function (resp) {
                // on error
                console.log('error downloading floor plan');
                // TODO: alert failure
            }
        );
    };

    var canvasOverlay = null;
    $scope.isCanvasOverlayActive = false;

    var floorPlanInputElement = $('#input-floor-plan');

    floorPlanInputElement.change(function handleImage(e) {
        var reader = new FileReader();
        reader.onload = function (event) {
            var imgObj = new Image();
            imgObj.src = event.target.result;
            imgObj.onload = function () {
                canvasOverlay = new CanvasOverlay(imgObj, GMapService.gmap);
                $scope.$apply($scope.isCanvasOverlayActive = true);

                // hide previous floorplan
                if ($scope.data.floor_plan_groundOverlay && $scope.data.floor_plan_groundOverlay.getMap()) {
                    $scope.data.floor_plan_groundOverlay.setMap(null);
                }

            }

        };
        reader.readAsDataURL(e.target.files[0]);

        this.disabled = true;
    });

    $scope.setFloorPlan = function () {
        if (!canvasOverlay) {
            return;
        }

        if (GMapService.gmap.getZoom() < 20) {
            _err("You have provided zoom level " + GMapService.gmap.getZoom() + ". You have to zoom at least to level 20 to upload the floorplan.");
            return;
        }

        if (AnyplaceService.getBuildingId() === null || AnyplaceService.getBuildingId() === undefined) {
            console.log('building is undefined');
            _err("Something went wrong. It seems like there is no building selected");
        }

        var newFl = {
            is_published: 'true',
            buid: String(AnyplaceService.getBuildingId()),
            floor_name: String($scope.newFloorNumber),
            description: String($scope.newFloorNumber),
            floor_number: String($scope.newFloorNumber)
        };

        $scope.myFloors[$scope.myFloorId] = newFl;
        $scope.myFloorId++;

        // create the proper image inside the canvas
        canvasOverlay.drawBoundingCanvas();

        // create the ground overlay and destroy the canvasOverlay object
        // and also set the floor_plan_coords in $scope.data
        var bl = canvasOverlay.bottom_left_coords;
        var tr = canvasOverlay.top_right_coords;
        $scope.data.floor_plan_coords.bottom_left_lat = bl.lat();
        $scope.data.floor_plan_coords.bottom_left_lng = bl.lng();
        $scope.data.floor_plan_coords.top_right_lat = tr.lat();
        $scope.data.floor_plan_coords.top_right_lng = tr.lng();
        $scope.data.floor_plan_coords.zoom = GMapService.gmap.getZoom() + "";
        var data = canvasOverlay.getCanvas().toDataURL("image/png"); // defaults to png
        $scope.data.floor_plan_base64_data = data;
        var imageBounds = new google.maps.LatLngBounds(
            new google.maps.LatLng(bl.lat(), bl.lng()),
            new google.maps.LatLng(tr.lat(), tr.lng()));
        $scope.data.floor_plan_groundOverlay = new USGSOverlay(imageBounds, data, GMapService.gmap);

        canvasOverlay.setMap(null); // remove the canvas overlay since the groundoverlay is placed
        $('#input-floor-plan').prop('disabled', false);
        $scope.isCanvasOverlayActive = false;

        if (_floorNoExists($scope.newFloorNumber)) {
            for (var i = 0; i < $scope.xFloors.length; i++) {
                var f = $scope.xFloors[i];
                if (!LPUtils.isNullOrUndefined(f)) {
                    if (f.floor_number === String($scope.newFloorNumber)) {
                        $scope.uploadFloorPlanBase64($scope.anyService.selectedBuilding, f, $scope.data);
                        break;
                    }
                }
            }
        } else {
            $scope.addFloorObject(newFl, $scope.anyService.selectedBuilding, $scope.data);
        }

    };

    var _checkFloorFormat = function (bobj) {
        if (bobj === null) {
            bobj = {}
        } else {
            bobj = JSON.parse(JSON.stringify(bobj))
        }
        if (LPUtils.isNullOrUndefined(bobj.buid)) {
            bobj.buid = ""
        }
        if (LPUtils.isNullOrUndefined(bobj.floor_name)) {
            bobj.floor_name = ""
        }
        if (LPUtils.isNullOrUndefined(bobj.floor_number)) {
            bobj.floor_number = ""
        }
        if (LPUtils.isNullOrUndefined(bobj.description)) {
            bobj.description = ""
        }
        if (LPUtils.isNullOrUndefined(bobj.is_published)) {
            bobj.is_published = 'true'
        }
        return bobj;
    };

    $scope.addFloorObject = function (flJson, selectedBuilding, flData) {
        var obj = _checkFloorFormat(flJson);

        obj.owner_id = $scope.owner_id;

        if (!obj.owner_id) {
            _err("Could not authorize user. Please refresh.");
            return;
        }

        // make the request at AnyplaceAPI
        var promise = $scope.anyAPI.addFloor(obj);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;
                // insert the newly created building inside the loadedBuildings
                $scope.xFloors.push(obj);

                $scope.anyService.selectedFloor = $scope.xFloors[$scope.xFloors.length - 1];

                _suc("Successfully added new floor");

                $scope.uploadFloorPlanBase64(selectedBuilding, obj, flData);

            },
            function (resp) {
                var data = resp.data;
                console.log(data.message);
                _err("Something went wrong while adding a new floor.");
            }
        );

    };

    $scope.removeFloorPlan = function () {
        $scope.data.floor_plan_file = null;
        $scope.data.floor_plan = null;

        if (canvasOverlay) {
            canvasOverlay.setMap(null);
        }


        if ($scope.data.floor_plan_groundOverlay) {
            $scope.data.floor_plan_groundOverlay.setMap(null);
        }

        var x = $('#input-floor-plan');
        x.replaceWith(x = x.clone(true));

        x.prop('disabled', false);

        $scope.isCanvasOverlayActive = false;
    };

    $scope.deleteFloor = function () {

        var bobj = $scope.anyService.getFloor();

        if (LPUtils.isNullOrUndefined(bobj) || LPUtils.isStringBlankNullUndefined(bobj.floor_number) || LPUtils.isStringBlankNullUndefined(bobj.buid)) {
            _err("No floor seems to be selected.");
            return;
        }

        bobj.username = $scope.creds.username;
        bobj.password = $scope.creds.password;
        bobj.owner_id = $scope.owner_id;

        console.log(bobj);

        // make the request at AnyplaceAPI
        var promise = $scope.anyAPI.deleteFloor(bobj);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                // delete the building from the loadedBuildings
                var lf = $scope.xFloors;
                var sz = lf.length;

                for (var i = 0; i < sz; i++) {
                    if (lf[i].floor_number == bobj.floor_number) {
                        lf.splice(i, 1);
                        break;
                    }
                }

                if ($scope.data.floor_plan_groundOverlay != null) {
                    $scope.data.floor_plan_groundOverlay.setMap(null);
                    $scope.data.floor_plan_groundOverlay = null;
                }

                if ($scope.xFloors && $scope.xFloors.length > 0) {
                    $scope.anyService.selectedFloor = $scope.xFloors[0];
                } else {
                    $scope.anyService.selectedFloor = undefined;
                }

                _suc("Successfully deleted floor.");
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err("Something went wrong while deleting the floor.");
            }
        );
    };


    var _floorNoExists = function (n) {
        for (var i = 0; i < $scope.xFloors.length; i++) {
            var f = $scope.xFloors[i];

            if (!LPUtils.isNullOrUndefined(f)) {
                if (f.floor_number === String(n)) {
                    return true;
                }
            }
        }
        return false;
    };

    var _cloneCoords = function (obj) {
        if (LPUtils.isNullOrUndefined(obj)) {
            return {}
        }
        var n = JSON.parse(JSON.stringify(obj));
        return n;
    };

    $scope.uploadFloorPlanBase64 = function (sb, sf, flData) {
        if (LPUtils.isNullOrUndefined(canvasOverlay)) {
            return;
        }

        var bobj = _cloneCoords(flData.floor_plan_coords);

        if (LPUtils.isNullOrUndefined(bobj) || LPUtils.isStringBlankNullUndefined(bobj.bottom_left_lat)
            || LPUtils.isStringBlankNullUndefined(bobj.bottom_left_lng)
            || LPUtils.isStringBlankNullUndefined(bobj.top_right_lat)
            || LPUtils.isStringBlankNullUndefined(bobj.top_right_lng)) {

            console.log('error with floor coords');
            _err("Something went wrong. It seems like no valid coordinates have been set up for this floor plan.");
            return;
        }

        bobj.bottom_left_lat = String(bobj.bottom_left_lat);
        bobj.bottom_left_lng = String(bobj.bottom_left_lng);
        bobj.top_right_lat = String(bobj.top_right_lat);
        bobj.top_right_lng = String(bobj.top_right_lng);

        sf.bottom_left_lat = bobj.bottom_left_lat;
        sf.bottom_left_lng = bobj.bottom_left_lng;
        sf.top_right_lat = bobj.top_right_lat;
        sf.top_right_lng = bobj.top_right_lng;

        if (!LPUtils.isNullOrUndefined(sb)) {
            if (!LPUtils.isNullOrUndefined(sb.buid)) {
                bobj.buid = sb.buid;
            } else {
                _err("Something went wrong with the selected building's id.");
                return;
            }
        } else {
            // no building selected
            _err("Something went wrong. It seems like there is no building selected.");
            return;
        }

        if (!LPUtils.isNullOrUndefined(sf)) {
            if (!LPUtils.isNullOrUndefined(sf.floor_number)) {
                bobj.floor_number = sf.floor_number;
            } else {
                _err("Something went wrong. It seems there is no floor number associated with the selected floor.");
                return;
            }
        } else {
            // no floor selected
            _err("Something went wrong. It seems there is no floor selected.");
            return;
        }

        bobj.owner_id = $scope.owner_id;

        var json_req = JSON.stringify(bobj);

        if (LPUtils.isNullOrUndefined(flData.floor_plan_base64_data)
            || LPUtils.isStringEmptyNullUndefined(flData.floor_plan_base64_data)) {
            console.log('no floor plan file');
            return;
        }

        // make the request at AnyplaceAPI
        var promise = $scope.anyAPI.uploadFloorPlan64(json_req, $scope.data.floor_plan_base64_data);

        promise.then(
            function (resp) {
                // on success
                var data = resp.data;
                _suc("Successfully uploaded new floor plan.");
            },
            function (resp) {
                // on error
                var data = resp.data;
                //TODO: alert error
                _suc("Successfully uploaded new floor plan.");
            });

    };

    $scope.toggleRadioHeatmap = function () {
        if (heatmap && heatmap.getMap()) {
            heatmap.setMap(null);
            return;
        }

        $scope.showRadioHeatmap();
    };


    $scope.showRadioHeatmapPoi = function () {
        var jsonReq = {
            "buid": $scope.anyService.getBuildingId(),
            "floor": $scope.anyService.getFloorNumber(),
            "coordinates_lat": $scope.anyService.selectedPoi.coordinates_lat,
            "coordinates_lon": $scope.anyService.selectedPoi.coordinates_lon,
            "range": "1"
        };

        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;

        var promise = $scope.anyAPI.getRadioHeatmapPoi(jsonReq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                var heatMapData = [];

                var i = resp.data.radioPoints.length;

                if (i <= 0) {
                    _err("This floor seems not to be WiFi mapped. Download the Anyplace app from the Google Play store to map the floor.");
                    return;
                }

                while (i--) {
                    var rp = resp.data.radioPoints[i];
                    heatMapData.push(
                        {location: new google.maps.LatLng(rp.x, rp.y), weight: 1}
                    );
                    resp.data.radioPoints.splice(i, 1);
                }

                if (heatmap && heatmap.getMap()) {
                    heatmap.setMap(null);
                }

                heatmap = new google.maps.visualization.HeatmapLayer({
                    data: heatMapData
                });
                heatmap.setMap($scope.gmapService.gmap);
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err('Something went wrong while fetching radio heatmap.');
            }
        );
    }

    $scope.showRadioHeatmap = function () {
        var jsonReq = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber()};

        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;

        var promise = $scope.anyAPI.getRadioHeatmap(jsonReq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                var heatMapData = [];

                var i = resp.data.radioPoints.length;

                if (i <= 0) {
                    _err("This floor seems not to be WiFi mapped. Download the Anyplace app from the Google Play store to map the floor.");
                    return;
                }

                while (i--) {
                    var rp = resp.data.radioPoints[i];
                    heatMapData.push(
                        {location: new google.maps.LatLng(rp.x, rp.y), weight: 1}
                    );
                    resp.data.radioPoints.splice(i, 1);
                }

                if (heatmap && heatmap.getMap()) {
                    heatmap.setMap(null);
                }

                heatmap = new google.maps.visualization.HeatmapLayer({
                    data: heatMapData
                });
                heatmap.setMap($scope.gmapService.gmap);
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err('Something went wrong while fetching radio heatmap.');
            }
        );
    }

}
]);
/**
 *
 The MIT License (MIT)

 Copyright (c) 2015, Kyriakos Georgiou, Marileni Angelidou, Data Management Systems Laboratory (DMSL)
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

app.controller('PoiController', ['$scope', '$compile', 'GMapService', 'AnyplaceService', 'AnyplaceAPIService', function ($scope, $compile, GMapService, AnyplaceService, AnyplaceAPIService) {
    _CONNECTIONS_IS_ON = true;
    _POIS_IS_ON = true;
    $scope.anyService = AnyplaceService;
    $scope.anyAPI = AnyplaceAPIService;

    $scope.myMarkers = {};
    $scope.myMarkerId = 0;

    $scope.myPaths = {};
    $scope.myPathId = 0;

    $scope.myPois = [];
    $scope.myPoisHashT = {};

    $scope.myConnectionsHashT = {};

    $scope.edgeMode = false;
    $scope.connectPois = {
        prev: undefined,
        next: undefined
    };


    $scope.crudTabSelected = 1;
    $scope.setCrudTabSelected = function (n) {
        $scope.crudTabSelected = n;
    };
    $scope.isCrudTabSelected = function (n) {
        return $scope.crudTabSelected === n;
    };


    $scope.orderByName = function (value) {
        return value.name;
    };

    $scope.$on("loggedOff", function (event, mass) {
        $scope.clearConnectionsOnMap();
        $scope.clearPoisOnMap();
        $scope.myPaths = {};
        $scope.myPathId = 0;
        $scope.myMarkers = {};
        $scope.myMarkerId = 0;
        $scope.myPois = [];
        $scope.myPoisHashT = {};
        $scope.myConnectionsHashT = {};
        $scope.anyService.setAllPois($scope.myPoisHashT);
        $scope.anyService.setAllConnection($scope.myConnectionsHashT);
        $scope.edgeMode = false;
        $scope.connectPois = {
            prev: undefined,
            next: undefined
        };
    });

    $scope.$watch('anyService.selectedBuilding', function (newVal, oldVal) {
        //if (newVal && newVal.buid && newVal.poistypeid) {
        //$scope.fetchAllPoisTypes(newVal.poistypeid);
        //}
        //else {
        $scope.poisTypes = [
            "Disabled Toilets",
            "Elevator",
            "Entrance",
            "Fire Extinguisher",
            "First Aid/AED",
            "Kitchen",
            "Office",
            "Ramp",
            "Room",
            "Security/Guard",
            "Stair",
            "Toilets",
            "Other"
        ];

        $scope.poicategories = [{
            poicat: "Disabled Toilets",
            poicatPlaceholder: "name",
            id: "type1",
            disenable: "false"
        }, {
            poicat: "Elevator",
            poicatPlaceholder: "name",
            id: "type2",
            disenable: "true"
        }, {
            poicat: "Entrance",
            poicatPlaceholder: "name",
            id: "type3",
            disenable: "true"
        }, {
            poicat: "Fire Extinguisher",
            poicatPlaceholder: "name",
            id: "type4",
            disenable: "false"
        }, {
            poicat: "First Aid/AED",
            poicatPlaceholder: "name",
            id: "type5",
            disenable: "false"
        }, {
            poicat: "Kitchen",
            poicatPlaceholder: "name",
            id: "type6",
            disenable: "false"
        }, {
            poicat: "Office",
            poicatPlaceholder: "name",
            id: "type7",
            disenable: "false"
        }, {
            poicat: "Ramp",
            poicatPlaceholder: "name",
            id: "type8",
            disenable: "false"
        }, {
            poicat: "Room",
            poicatPlaceholder: "name",
            id: "type9",
            disenable: "false"
        }, {
            poicat: "Security/Guard",
            poicatPlaceholder: "name",
            id: "type10",
            disenable: "false"
        }, {
            poicat: "Stair",
            poicatPlaceholder: "name",
            id: "type11",
            disenable: "true"
        }, {
            poicat: "Toilets",
            poicatPlaceholder: "name",
            id: "type12",
            disenable: "false"
        }, {
            poicat: "Other",
            poicatPlaceholder: "name",
            id: "type13",
            disenable: "false"
        }
        ];
        //}
    });

    $scope.$watch('anyService.selectedFloor', function (newVal, oldVal) {
        if (newVal !== undefined && newVal !== null && !_.isEqual(newVal, oldVal)) {
            $scope.fetchAllPoisForFloor(newVal);
        } else {
            $scope.anyService.selectedPoi = undefined;

        }
    });

    $scope.$watch('anyService.selectedPoi', function (newVal, oldVal) {
        if (newVal && _latLngFromPoi(newVal)) {
            if (newVal) {
                GMapService.gmap.panTo(_latLngFromPoi(newVal));
                //GMapService.gmap.setZoom(19);
                if (newVal.puid) {
                    var marker = $scope.myPoisHashT[newVal.puid].marker;
                    if (marker && marker.infowindow && marker.tpl2) {
                        marker.infowindow.setContent(marker.tpl2[0]);
                        marker.infowindow.open(GMapService.gmap, marker);
                    }

                    if (typeof(Storage) !== "undefined" && localStorage) {
                        localStorage.setItem("lastPoi", newVal.puid);
                    }
                }
            }
        }
    });

    $scope.add = function () {
        if ($scope.poicategories[$scope.poicategories.length - 1].poicat != "") {
            $scope.poicategories.push({
                poicat: "",
                poicatPlaceholder: "name",
                disenable: "false"
            });
        }
        else {
            _err("Complete the last input to continue!");
        }
    };

    $scope.deletetype = function (id) {

        for (var i = 0; i < $scope.poicategories.length; i++) {
            if ($scope.poicategories[i].id == id) {
                $scope.poicategories[i].id = "";
                $("#".id).remove();
                break;
            }
        }
    };

    $scope.addcategory = function () {

        var name_element = document.getElementById("poistype");
        var name = "\"poistype\":\"" + name_element.value + "\"";

        function S4() {
            return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
        }

        var guid = (S4() + S4() + "-" + S4() + "-4" + S4().substr(0, 3) + "-" + S4() + "-" + S4() + S4() + S4()).toLowerCase();
        var d = new Date();


        var poistypeid = "poistypeid_" + guid + "_" + d.getTime();
        poistypeid = "\"poistypeid\":\"" + poistypeid + "\"";

        var sz = $scope.poicategories.length;

        if (sz == 0) {
            _err("No categories added.");
            return;
        }

        var types = "\"types\":[";
        for (var i = sz - 1; i > 0; i--) {
            if ($scope.poicategories[i].poicat != "") {
                types = types + "\"" + $scope.poicategories[i].poicat + "\",";
            }
        }
        types = types + "\"" + $scope.poicategories[0].poicat + "\"]";

        var jreq = "{" + name + "," + poistypeid + "," + types + ",\"owner_id\":\"" + $scope.owner_id + "\",\"access_token\":\"" + $scope.gAuth.access_token + "\"}";

        var promise = $scope.anyAPI.addCategory(jreq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;
                _suc("Successfully added category.");
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err("Something went wrong while adding the category. " + data.message);
            }
        );

    };


    $scope.fetchAllPoisTypes = function (poistypeid) {

        //TODO: validation

        var jsonReq = $scope.anyService.jsonReq;

        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;
        jsonReq.owner_id = $scope.owner_id;
        jsonReq.access_token = $scope.gAuth.access_token;
        jsonReq.poistypeid = poistypeid;

        if (!jsonReq.owner_id) {
            _err("Could nor authorize user. Please refresh.");
            return;
        }
        var promise = $scope.anyAPI.retrievePoisTypes(jsonReq);
        promise.then(
            function (resp) {
                var data = resp.data;

                var poistypes = data.poistypes;

                var sz = poistypes.length;
                for (var i = sz - 1; i >= 0; i--) {
                    if (poistypes[i].poistypeid == poistypeid) {
                        var types = poistypes[i].types;
                        break;
                    }
                }

                var sz = types.length;
                for (var i = sz - 1; i >= 0; i--) {
                    $scope.poisTypes[i] = types[i];
                }
            },
            function (resp) {
                var data = resp.data;
                _err("Something went wrong while fetching POIs types");
            }
        );
    };


    $scope.onInfoWindowKeyDown = function (e) {
        // esc key
        if (e.keyCode == 27) {
            if ($scope.anyService.selectedPoi) {
                var p = $scope.anyService.selectedPoi;
                if ($scope.myPoisHashT[p.puid] && $scope.myPoisHashT[p.puid].marker && $scope.myPoisHashT[p.puid].marker.infowindow) {
                    $scope.myPoisHashT[p.puid].marker.infowindow.setMap(null);
                    $scope.anyService.setAllPois($scope.myPoisHashT);
                }
            }
        }
    };

    $scope.clearPoisOnMap = function () {

        for (var i = 0; i < $scope.myPois.length; i++) {
            var p = $scope.myPois[i];
            if (p && $scope.myPoisHashT[p.puid] && $scope.myPoisHashT[p.puid].marker) {
                $scope.myPoisHashT[p.puid].marker.setMap(null);
                delete $scope.myPoisHashT[p.puid];
            }
        }
        $scope.myPoisHashT = {};
        $scope.anyService.setAllPois($scope.myPoisHashT);
    };

    $scope.clearConnectionsOnMap = function () {
        if ($scope.myConnectionsHashT) {

            for (var con in $scope.myConnectionsHashT) {
                if (con && $scope.myConnectionsHashT.hasOwnProperty(con) && $scope.myConnectionsHashT[con] && $scope.myConnectionsHashT[con].polyLine) {
                    $scope.myConnectionsHashT[con].polyLine.setMap(null);
                    delete $scope.myConnectionsHashT[con];
                }
            }
            $scope.myConnectionsHashT = {};
            $scope.anyService.setAllConnection($scope.myConnectionsHashT);
        }

    };

    $scope.fetchConnections = function () {

        var tobj = $scope.anyService.jsonReq;
        var buid, floor_number;

        // we must check that a floor and a building has been selected
        var sb = $scope.anyService.getBuilding();
        if (!LPUtils.isNullOrUndefined(sb)) {
            if (!LPUtils.isNullOrUndefined(sb.buid)) {
                buid = $scope.anyService.getBuildingId();
            } else {
                _err("No valid building has been selected!");
                return;
            }
        } else {
            // no building selected
            _err("No building has been selected!");
            return;
        }

        var sf = $scope.anyService.getFloor();
        if (!LPUtils.isNullOrUndefined(sf)) {
            if (!LPUtils.isNullOrUndefined(sf.floor_number)) {
                floor_number = $scope.anyService.getFloorNumber()
            } else {
                _err("No valid floor has been selected!");
                return;
            }
        } else {
            _err("No floor has been selected!");
            return;
        }

        tobj.buid = buid;
        tobj.floor_number = floor_number;

        if (LPUtils.isNullOrUndefined($scope.myPois) || LPUtils.isNullOrUndefined($scope.myPoisHashT)) {
            _err("Please load the POIs of this floor first.");
        }

        if ($scope.myPois.length == 0) {
            // _err("This floor is empty.");
            return;
        }

        var json_req = tobj;

        // we must query the db to get all the pois for the floor
        var promise = $scope.anyAPI.retrieveConnectionsByBuildingFloor(json_req);
        promise.then(
            function (resp) {
                var data = resp.data;

                $scope.clearConnectionsOnMap();
                //var connections = JSON.parse( data.connections );
                var connections = data.connections;

                var hasht = {};
                var sz = connections.length;
                for (var i = 0; i < sz; i++) {
                    // insert the pois inside the hashtable
                    hasht[connections[i].cuid] = connections[i];
                }
                $scope.myConnectionsHashT = hasht;
                $scope.anyService.setAllConnection($scope.myConnectionsHashT);
                // draw the markers
                // $scope.data.MainController.clearConnectionsOnMap();

                $scope.drawConnectionsOnMap();

                //_suc("Connections were loaded successfully.");
            },
            function (resp) {
                var data = resp.data;
                _err("Something went wrong while loading the POI connections");
            }
        );

    };

    $scope.drawConnectionsOnMap = function () {

        for (var cuid in $scope.myConnectionsHashT) {
            if ($scope.myConnectionsHashT.hasOwnProperty(cuid)) {
                var conn = $scope.myConnectionsHashT[cuid];

                if (!conn || !conn.pois_a || !conn.pois_b || !$scope.myPoisHashT[conn.pois_a] || !$scope.myPoisHashT[conn.pois_b] || !$scope.myPoisHashT[conn.pois_a].model || !$scope.myPoisHashT[conn.pois_b].model) {
                    continue;
                }

                var flightPlanCoordinates = [
                    new google.maps.LatLng($scope.myPoisHashT[conn.pois_a].model.coordinates_lat, $scope.myPoisHashT[conn.pois_a].model.coordinates_lon),
                    new google.maps.LatLng($scope.myPoisHashT[conn.pois_b].model.coordinates_lat, $scope.myPoisHashT[conn.pois_b].model.coordinates_lon)
                ];

                var flightPath = new google.maps.Polyline({
                    path: flightPlanCoordinates,
                    strokeColor: "#0000FF",
                    strokeOpacity: 0.5,
                    strokeWeight: 4
                });
                if (_CONNECTIONS_IS_ON)
                    flightPath.setMap(GMapService.gmap);
                flightPath.model = conn;

                $scope.myConnectionsHashT[cuid].polyLine = flightPath;
                $scope.anyService.setAllConnection($scope.myConnectionsHashT);


                google.maps.event.addListener(flightPath, 'click', function () {
                    $scope.$apply(_deleteConnection(this));
                    $scope.anyService.setAllConnection($scope.myConnectionsHashT);
                });

            }
        }
    };

    var _POI_CONNECTOR_IMG = 'build/images/edge-connector-icon.png';
    var _POI_EXISTING_IMG = 'build/images/any-poi-icon.png';
    var _POI_NEW_IMG = 'build/images/poi-icon.png';

    var _latLngFromPoi = function (p) {
        if (p && p.coordinates_lat && p.coordinates_lon) {
            return {lat: parseFloat(p.coordinates_lat), lng: parseFloat(p.coordinates_lon)}
        }
        return undefined;
    };

    var _err = function (msg) {
        $scope.anyService.addAlert('danger', msg);
    };

    var _suc = function (msg) {
        $scope.anyService.addAlert('success', msg);
    };

    var _warn = function (msg) {
        $scope.anyService.addAlert('warning', msg);
    };

    var _isPoiNearFloor = function (coords) {
        var D = 0.001;

        var pLat = coords.lat();
        var pLng = coords.lng();


        var floor = $scope.anyService.getFloor();

        if (LPUtils.isNullOrUndefined(floor)) {
            _err("No selected floor found.");
            return false;
        }

        var topRightLat = floor.top_right_lat;
        var topRightLng = floor.top_right_lng;


        var bottomLeftLat = floor.bottom_left_lat;
        var bottomLeftLng = floor.bottom_left_lng;

        if (!topRightLat || !topRightLng || !bottomLeftLat || !bottomLeftLng) {
            _err("Floor coordinates not found.");
            return false;
        }

        return pLat <= parseFloat(topRightLat) + D && pLng <= parseFloat(topRightLng) + D && pLat >= parseFloat(bottomLeftLat) - D && pLng >= parseFloat(bottomLeftLng) - D;
    };

    $scope.drawPoisOnMap = function () {

        var infowindow = new google.maps.InfoWindow({
            content: '-',
            maxWidth: 500
        });

        var localPuid = undefined;
        var localPoiIndex = -1;
        if (typeof(Storage) !== "undefined" && localStorage
            && !LPUtils.isNullOrUndefined(localStorage.getItem('lastBuilding'))
            && !LPUtils.isNullOrUndefined(localStorage.getItem('lastFloor'))
            && !LPUtils.isNullOrUndefined(localStorage.getItem('lastPoi'))) {

            localPuid = localStorage.getItem('lastPoi');
        }


        for (var i = 0; i < $scope.myPois.length; i++) {
            $scope.myPois[i].pois_type2 = $scope.myPois[i].pois_type;
            var p = $scope.myPois[i];

            if (localPuid && p.puid === localPuid) {
                localPoiIndex = i;
            }

            var marker;
            var htmlContent = '-';
            p.pois_type2 = p.pois_type;
            if (p.pois_type == "None") {
                marker = new google.maps.Marker({
                    position: _latLngFromPoi(p),
                    draggable: true,
                    icon: new google.maps.MarkerImage(
                        _POI_CONNECTOR_IMG,
                        null, /* size is determined at runtime */
                        null, /* origin is 0,0 */
                        null, /* anchor is bottom center of the scaled image */
                        new google.maps.Size(21, 21)
                    )
                });
                if (_POIS_IS_ON)
                    marker.setMap(GMapService.gmap);

                htmlContent = '<div class="infowindow-scroll-fix" style="text-align: center; width:170px">'
                    + '<fieldset class="form-group" style="display: inline-block; width: 73%;">'
                    + '<button type="submit" class="btn btn-success add-any-button" ng-click="updatePoi(\'' + p.puid + '\')"><span class="glyphicon glyphicon-pencil"></span> Update'
                    + '</button>'
                    + '</fieldset>'
                    + '<fieldset class="form-group" style="display: inline-block;width: 23%;">'
                    + '<button type="submit" class="btn btn-danger add-any-button" style="margin-left:2px" ng-click="deletePoi(\'' + p.puid + '\')"><span class="glyphicon glyphicon-remove"></span>'
                    + '</button>'
                    + '</fieldset>'
                    + '</div>';
            } else {
                var imgType = _POI_EXISTING_IMG;
                var size = new google.maps.Size(21, 32);

//                if (p.pois_type === "Entrance") {
//                    imgType = "images/door.png";
//                    size = new google.maps.Size(32, 32)
//                } else if (p.pois_type === "Toilets") {
//                    imgType = "images/toilets.png";
//                    size = new google.maps.Size(32, 32)
//                } else if (p.pois_type === "Ramp") {
//                    imgType = "images/wheel_chair_accessible.png";
//                    size = new google.maps.Size(32, 32)
//                }

                marker = new google.maps.Marker({
                    position: _latLngFromPoi(p),
                    draggable: true,
                    icon: new google.maps.MarkerImage(
                        imgType,
                        null, /* size is determined at runtime */
                        null, /* origin is 0,0 */
                        null, /* anchor is bottom center of the scaled image */
                        size
                    )
                });

                if (_POIS_IS_ON)
                    marker.setMap(GMapService.gmap);

                htmlContent = '<div class="infowindow-scroll-fix" ng-keydown="onInfoWindowKeyDown($event)">'
                    + '<form name="poiForm">'
                    + '<fieldset class="form-group">'
                    + '<textarea ng-model="myPois[' + i + '].name" id="poi-name" type="text" class="form-control" placeholder="poi name" tabindex="1" autofocus></textarea>'
                    + '</fieldset>'
                    + '<fieldset class="form-group">'
                    + '<textarea ng-model="myPois[' + i + '].description" id="poi-description" type="text" class="form-control" placeholder="poi description" tabindex="5"></textarea>'
                    + '</fieldset>'
                    + '<fieldset class="form-group">'
                    + '<select ng-model="myPois[' + i + '].pois_type" class="form-control" ng-options="type for type in poisTypes" title="POI Types" tabindex="2">'
                    + '<option value="">Select POI Type</option>'
                    + '</select>'
                    + '</fieldset class="form-group">'
                    + '<fieldset class="form-group">Or enter your own type:'
                    + '<input ng-model="myPois[' + i + '].pois_type2" id="poi-pois_type2" type="text" class="form-control" placeholder="POI Type" tabindex="2">'
                    + '</fieldset>'
                    + '<fieldset class="form-group">'
                    + '<input ng-model="myPois[' + i + '].is_building_entrance" id="poi-entrance" type="checkbox" tabindex="4"><span> is building entrance?</span>'
                    + '</fieldset>'
                    + '<div style="text-align: center;">'
                    + '<fieldset class="form-group" style="display: inline-block; width: 75%;">'
                    + '<button type="submit" class="btn btn-success add-any-button" ng-click="updatePoi(\'' + p.puid + '\')" tabindex="3"><span class="glyphicon glyphicon-pencil"></span> Update'
                    + '</button>'
                    + '</fieldset>'
                    + '<fieldset class="form-group" style="display: inline-block;width: 23%;">'
                    + '<button type="submit" class="btn btn-danger add-any-button" style="margin-left:2px" ng-click="deletePoi(\'' + p.puid + '\')" tabindex="6"><span class="glyphicon glyphicon-remove"></span>'
                    + '</button>'
                    + '</fieldset>'
                    + '</div>'
                    + '</form>'
                    + '</div>';
            }

            var tpl = $compile(htmlContent)($scope);

            marker.tpl2 = tpl;
            marker.model = p;

            marker.infowindow = infowindow;

            $scope.myPoisHashT[p.puid].marker = marker;
            $scope.anyService.setAllPois($scope.myPoisHashT);

            google.maps.event.addListener(marker, 'click', function () {

                if ($scope.edgeMode) {
                    if ($scope.connectPois.prev) {

                        if ($scope.connectPois.prev == this) {
                            $scope.connectPois.prev = undefined;

                            if (this.model.pois_type === "None") {
                                this.setIcon(_getConnectorIconNormal());
                            } else {
                                this.setIcon(_getNormalPoiIconNormal());
                            }
                            return;
                        }

                        // No longer the beginning of an edge, make smaller.
                        if ($scope.connectPois.prev.model.pois_type === "None") {
                            $scope.connectPois.prev.setIcon(_getConnectorIconNormal());
                        } else {
                            $scope.connectPois.prev.setIcon(_getNormalPoiIconNormal());
                        }

                        var flightPath = new google.maps.Polyline({
                            path: [this.position, $scope.connectPois.prev.position],
                            geodesic: true,
                            strokeColor: '#FF0000',
                            strokeOpacity: 0.5,
                            strokeWeight: 4
                        });

                        flightPath.setMap(GMapService.gmap);

                        // Construct the request
                        var poiA = $scope.connectPois.prev.model;
                        var poiB = this.model;

                        if (!poiA || !poiB || !poiA.puid || !poiB.puid || !poiA.buid || !poiB.buid || LPUtils.isNullOrUndefined(poiA.floor_number) || LPUtils.isNullOrUndefined(poiB.floor_number)) {
                            _err("One or both of the POIs attempted to be connected seem to be be malformed. Please refresh.");
                            return;
                        }

                        var jsonReq = {

                            username: $scope.creds.username,
                            password: $scope.creds.password,
                            owner_id: $scope.owner_id,

                            pois_a: poiA.puid,
                            floor_a: poiA.floor_number,
                            buid_a: poiA.buid,

                            // insert the connection buid ( needed by the Dijkstra algorithm at the moment )
                            buid: poiA.buid,

                            pois_b: poiB.puid,
                            floor_b: poiB.floor_number,
                            buid_b: poiB.buid,

                            is_published: 'true',
                            edge_type: 'hallway'
                        };

                        flightPath.model = jsonReq;

                        // make the request at AnyplaceAPI

                        var promise = $scope.anyAPI.addConnection(jsonReq);
                        promise.then(
                            function (resp) {
                                var data = resp.data;
                                var cuid = data.cuid;

                                flightPath.model.cuid = cuid;
                                var cloneModel = flightPath.model;
                                cloneModel.polyLine = flightPath;
                                $scope.myConnectionsHashT[cuid] = cloneModel;
                                $scope.anyService.setAllConnection($scope.myConnectionsHashT);

                                flightPath.setOptions({
                                    strokeColor: '#0000FF',
                                    strokeOpacity: 0.5
                                });

                                google.maps.event.addListener(flightPath, 'click', function () {
                                    $scope.$apply(_deleteConnection(this));
                                });

                            },
                            function (resp) {
                                var data = resp.data;
                                _err("Something went wrong while attempting to connect the two POIs.");
                                flightPath.setMap(null);
                            }
                        );

                        $scope.connectPois.prev = undefined;

                    } else {

                        $scope.connectPois.prev = this;

                        // Increase the size of markers to indicate as the start of an edge
                        if (this.model.pois_type === "None") {
                            this.setIcon(_getConnectorIconBigger());
                        } else {
                            this.setIcon(_getNormalPoiIconBigger());
                        }
                    }

                } else {
                    infowindow.setContent(this.tpl2[0]);
                    infowindow.open(GMapService.gmap, this);
                    var self = this;
                    $scope.$apply(function () {
                        if (self.model && self.model.puid) {
                            $scope.anyService.selectedPoi = self.model;
                        }
                    })
                }
            });

            google.maps.event.addListener(marker, "dragend", function (event) {
                if (this.model && this.model.puid) {
                    $scope.updatePoiPosition(this);
                }
            });
        }

        if (localPoiIndex >= 0) {
            $scope.anyService.selectedPoi = $scope.myPois[localPoiIndex];
        }
        $scope.fetchConnections();
    };

    var _getConnectorIconBigger = function () {
        return new google.maps.MarkerImage(
            _POI_CONNECTOR_IMG,
            null, /* size is determined at runtime */
            null, /* origin is 0,0 */
            null, /* anchor is bottom center of the scaled image */
            new google.maps.Size(41, 41)
        )
    };

    var _getConnectorIconNormal = function () {
        return new google.maps.MarkerImage(
            _POI_CONNECTOR_IMG,
            null, /* size is determined at runtime */
            null, /* origin is 0,0 */
            null, /* anchor is bottom center of the scaled image */
            new google.maps.Size(21, 21)
        )
    };

    var _getNormalPoiIconBigger = function () {
        return new google.maps.MarkerImage(
            _POI_EXISTING_IMG,
            null, /* size is determined at runtime */
            null, /* origin is 0,0 */
            null, /* anchor is bottom center of the scaled image */
            new google.maps.Size(31, 48)
        )
    };

    var _getNormalPoiIconNormal = function () {
        return new google.maps.MarkerImage(
            _POI_EXISTING_IMG,
            null, /* size is determined at runtime */
            null, /* origin is 0,0 */
            null, /* anchor is bottom center of the scaled image */
            new google.maps.Size(21, 32)
        )
    };

    var _checkConnectionFormat = function (bobj) {
        if (bobj === null) {
            bobj = {}
        }
        if (LPUtils.isNullOrUndefined(bobj.buid)) {
            bobj.buid = ""
        }
        if (LPUtils.isNullOrUndefined(bobj.is_published)) {
            bobj.is_published = false
        }
        if (LPUtils.isNullOrUndefined(bobj.edge_type)) {
            bobj.edge_type = ""
        }
        if (LPUtils.isNullOrUndefined(bobj.pois_a)) {
            bobj.pois_a = ""
        }
        if (LPUtils.isNullOrUndefined(bobj.pois_b)) {
            bobj.pois_b = ""
        }
        if (LPUtils.isNullOrUndefined(bobj.floor_a)) {
            bobj.floor_a = ""
        }
        if (LPUtils.isNullOrUndefined(bobj.floor_b)) {
            bobj.floor_b = ""
        }
        if (LPUtils.isNullOrUndefined(bobj.buid_a)) {
            bobj.buid_a = ""
        }
        if (LPUtils.isNullOrUndefined(bobj.buid_b)) {
            bobj.buid_b = ""
        }
        return bobj;
    };

    var _deleteConnection = function (fp) {

        if (!$scope.edgeMode) {
            _err("Enable \"Edge Mode\" so you can delete connections by clicking on them.");
            return;
        }

        if (!fp || !fp.model || !fp.model.cuid) {
            _err("No valid connection selected.");
            return;
        }

        var conn = fp.model;
        var cuid = fp.model.cuid;

        if (!$scope.myConnectionsHashT[cuid]) {
            _err("The connection attempted to delete does not exist in the system.");
            return;
        }

        // there is a connection selected and loaded so use it for update
        var obj = _checkConnectionFormat(conn);
        obj.username = $scope.creds.username;
        obj.password = $scope.creds.password;
        obj.owner_id = $scope.owner_id;

        if (LPUtils.isNullOrUndefined(obj.pois_a) || LPUtils.isStringBlankNullUndefined(obj.pois_a)) {
            _err("No valid connection has been selected. Missing POI A.");
            return;
        } else if (LPUtils.isNullOrUndefined(obj.pois_b) || LPUtils.isStringBlankNullUndefined(obj.pois_b)) {
            _err("No valid connection has been selected. Missing POI B.");
            return;
        }

        // visually remove it immediately, on failure we restore it.
        fp.setMap(null);

        // TODO: Figure proper data structures for two way data hold.
        var temp = fp.model.polyLine;
        delete fp.model.polyLine;

        // make the request at AnyplaceAPI
        var promise = $scope.anyAPI.deleteConnection(obj);

        promise.then(
            function (resp) {
                var data = resp.data;

                delete $scope.myConnectionsHashT[cuid];
                $scope.anyService.setAllConnection($scope.myConnectionsHashT);
            },
            function (resp) {
                var data = resp.data;
                fp.setMap(GMapService.gmap);
                fp.model.polyLine = temp;
                _err("Something went wrong. Connection could not be deleted.");
            }
        );

    };

    $scope.fetchAllPoisForFloor = function (fl) {

        //TODO: validation
        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = AnyplaceService.getBuildingId();
        jsonReq.floor_number = AnyplaceService.getFloorNumber();

        var promise = AnyplaceAPIService.retrievePoisByBuildingFloor(jsonReq);
        promise.then(
            function (resp) {
                var data = resp.data;

                $scope.clearPoisOnMap();

                $scope.myPois = data.pois;

                var sz = $scope.myPois.length;
                for (var i = sz - 1; i >= 0; i--) {
                    // insert the pois inside the hashtable
                    // TODO clone (?)
                    var puid = $scope.myPois[i].puid;

                    if ($scope.myPois[i].is_building_entrance == 'true') {
                        $scope.myPois[i].is_building_entrance = true;
                    } else {
                        $scope.myPois[i].is_building_entrance = false;
                    }

                    $scope.myPois[i].pois_type2 = $scope.myPois[i].pois_type;

                    $scope.myPoisHashT[puid] = {};
                    $scope.myPoisHashT[puid].model = $scope.myPois[i];
                    $scope.anyService.setAllPois($scope.myPoisHashT);
                }

                // draw the markers
                $scope.drawPoisOnMap();

                //_suc("Successfully fetched all POIs.");
            },
            function (resp) {
                var data = resp.data;
                _err("Something went wrong while fetching POIs");
            }
        );
    };

    var overlay = new google.maps.OverlayView();
    overlay.draw = function () {
    };
    overlay.setMap(GMapService.gmap);

    $scope.addPoi = function (id) {
        if ($scope.myMarkers[id] && $scope.myMarkers[id].marker) {

            if ($scope.myMarkers[id].model.pois_type2.localeCompare("") != 0) {
                $scope.myMarkers[id].model.pois_type = $scope.myMarkers[id].model.pois_type2;
            }

            var poi = $scope.myMarkers[id].model;

            // set owner id
            poi.owner_id = $scope.owner_id;

            poi.coordinates_lat = String($scope.myMarkers[id].marker.position.lat());
            poi.coordinates_lon = String($scope.myMarkers[id].marker.position.lng());

            poi.is_building_entrance = String(poi.is_building_entrance);

            if (poi.coordinates_lat === undefined || poi.coordinates_lat === null) {
                _err("POI has invalid latitude format");
                return;
            }

            if (poi.coordinates_lon === undefined || poi.coordinates_lon === null) {
                _err("POI has invalid longitude format");
                return;
            }

            if (poi.owner_id && poi.name && poi.buid && poi.pois_type
                && !LPUtils.isNullOrUndefined(poi.is_building_entrance)
                && !LPUtils.isNullOrUndefined(poi.is_published)
                && !LPUtils.isNullOrUndefined(poi.is_door)
                && !LPUtils.isNullOrUndefined(poi.floor_name)
                && !LPUtils.isNullOrUndefined(poi.floor_number)) {

                if (poi.is_building_entrance == 'true') {
                    poi.is_building_entrance = 'true';
                } else {
                    poi.is_building_entrance = 'false';
                }

                var json_req = poi;

                // make the request at AnyplaceAPI
                var promise = $scope.anyAPI.addPois(json_req);
                promise.then(
                    function (resp) {
                        var data = resp.data;

                        poi.puid = data.puid;

                        if (poi.is_building_entrance == 'true') {
                            poi.is_building_entrance = true;
                        } else {
                            poi.is_building_entrance = false;
                        }

                        // insert the newly created building inside the loadedBuildings
                        $scope.myPois.push(poi);
                        $scope.anyService.selectedPoi = $scope.myPois[$scope.myPois.length - 1];

                        // update the hashtable

                        $scope.myPoisHashT[poi.puid] = {
                            model: poi,
                            marker: $scope.myMarkers[id].marker
                        };
                        $scope.anyService.setAllPois($scope.myPoisHashT);

                        $scope.myMarkers[id].model = poi;

                        if ($scope.myMarkers[id].infowindow) {
                            $scope.myMarkers[id].infowindow.close();
                            $scope.myMarkers[id].infowindow.setContent($scope.myMarkers[id].marker.tpl2[0]);
                        }

                        $scope.myMarkers[id].marker.model = poi;

                        if ($scope.myMarkers[id].marker.model.pois_type == "None") {
                            $scope.myMarkers[id].marker.setIcon(_getConnectorIconNormal());
                        } else {
                            $scope.myMarkers[id].marker.setIcon(_getNormalPoiIconNormal());
                        }

                        google.maps.event.clearListeners($scope.myMarkers[id].marker, 'click');

                        var infowindow = $scope.myMarkers[id].infowindow;

                        google.maps.event.addListener($scope.myMarkers[id].marker, 'click', function () {
                            if ($scope.edgeMode) {
                                if ($scope.connectPois.prev) {

                                    if ($scope.connectPois.prev == this) {
                                        $scope.connectPois.prev = undefined;

                                        if (this.model.pois_type === "None") {
                                            this.setIcon(_getConnectorIconNormal());
                                        } else {
                                            this.setIcon(_getNormalPoiIconNormal());
                                        }
                                        return;
                                    }

                                    // No longer the beginning of an edge, make smaller.
                                    if ($scope.connectPois.prev.model.pois_type === "None") {
                                        $scope.connectPois.prev.setIcon(_getConnectorIconNormal());
                                    } else {
                                        $scope.connectPois.prev.setIcon(_getNormalPoiIconNormal());
                                    }

                                    var flightPath = new google.maps.Polyline({
                                        path: [this.position, $scope.connectPois.prev.position],
                                        geodesic: true,
                                        strokeColor: '#FF0000',
                                        strokeOpacity: 0.5,
                                        strokeWeight: 4
                                    });

                                    flightPath.setMap(GMapService.gmap);

                                    // Construct the request
                                    var poiA = $scope.connectPois.prev.model;
                                    var poiB = this.model;

                                    if (!poiA || !poiB || !poiA.puid || !poiB.puid || !poiA.buid || !poiB.buid || LPUtils.isNullOrUndefined(poiA.floor_number) || LPUtils.isNullOrUndefined(poiB.floor_number)) {
                                        _err("One or both of the POIs attempted to be connected seem to be be malformed. Please refresh.");
                                        return;
                                    }

                                    var jsonReq = {

                                        username: $scope.creds.username,
                                        password: $scope.creds.password,
                                        owner_id: $scope.owner_id,

                                        pois_a: poiA.puid,
                                        floor_a: poiA.floor_number,
                                        buid_a: poiA.buid,

                                        // insert the connection buid ( needed by the Dijkstra algorithm at the moment )
                                        buid: poiA.buid,

                                        pois_b: poiB.puid,
                                        floor_b: poiB.floor_number,
                                        buid_b: poiB.buid,

                                        is_published: 'true',
                                        edge_type: 'hallway'
                                    };

                                    flightPath.model = jsonReq;

                                    // make the request at AnyplaceAPI

                                    var promise = $scope.anyAPI.addConnection(jsonReq);
                                    promise.then(
                                        function (resp) {
                                            var data = resp.data;
                                            var cuid = data.cuid;

                                            flightPath.model.cuid = cuid;
                                            var cloneModel = flightPath.model;
                                            cloneModel.polyLine = flightPath;
                                            $scope.myConnectionsHashT[cuid] = cloneModel;
                                            $scope.anyService.setAllConnection($scope.myConnectionsHashT);

                                            flightPath.setOptions({
                                                strokeColor: '#0000FF',
                                                strokeOpacity: 0.5
                                            });

                                            google.maps.event.addListener(flightPath, 'click', function () {
                                                $scope.$apply(_deleteConnection(this));
                                            });

                                        },
                                        function (resp) {
                                            var data = resp.data;
                                            _err("Something went wrong while attempting to connect the two POIs.");
                                            flightPath.setMap(null);
                                        }
                                    );

                                    $scope.connectPois.prev = undefined;
                                } else {
                                    $scope.connectPois.prev = this;

                                    // Increase the size of markers to indicate as the start of an edge
                                    if (this.model.pois_type === "None") {
                                        this.setIcon(_getConnectorIconBigger());
                                    } else {
                                        this.setIcon(_getNormalPoiIconBigger());
                                    }
                                }
                            } else {
                                infowindow.setContent(this.tpl2[0]);
                                infowindow.open(GMapService.gmap, this);
                                var self = this;
                                $scope.$apply(function () {
                                    if (self.model && self.model.puid) {
                                        $scope.anyService.selectedPoi = self.model;
                                    }
                                })
                            }
                        });

                        google.maps.event.addListener($scope.myMarkers[id].marker, "dragend", function (event) {
                            if (this.model && this.model.puid) {
                                $scope.updatePoiPosition(this);
                            }
                        });

                        // Too spammy.
                        //_suc("Successfully added new POI.");

                    },
                    function (resp) {
                        var data = resp.data;
                        _err("Something went wrong while adding the new POI.");
                    }
                );
            } else {
                _err("Cannot add new POI. Some required fields are missing.");
            }
        }
    };

    $scope.deletePoi = function (id) {

        var bobj = $scope.myPoisHashT[id];

        if (!bobj || !bobj.model || !bobj.model.puid) {
            if ($scope.myPoisHashT && $scope.myMarkers && $scope.myMarkers[id] && $scope.myMarkers[id].model && $scope.myPoisHashT[$scope.myMarkers[id].model.puid]) {

                bobj = $scope.myPoisHashT[$scope.myMarkers[id].model.puid];

                if (!bobj || !bobj.model || !bobj.model.puid) {
                    _err("No valid POI selected to be deleted.");
                    return;
                }

            } else {
                _err("No valid POI selected to be deleted.");
                return;
            }
        }

        // check that the pois is not connected to any connection and abort if it
        var cf;
        var lf = $scope.myConnectionsHashT;
        for (var cuid in lf) {
            if (lf.hasOwnProperty(cuid)) {
                cf = lf[cuid];
                if (cf.pois_a == bobj.model.puid || cf.pois_b == bobj.model.puid) {
                    // abort deletion since there are connections to the pois
                    _err("Please delete all the connections attached to this POI first.");
                    return;
                }
            }
        }

        var obj = bobj.model;
        obj.username = $scope.creds.username;
        obj.password = $scope.creds.password;

        obj.owner_id = $scope.owner_id;

        var delPuid = obj.puid;
        // make the request at AnyplaceAPI

        if (obj.is_building_entrance === true || obj.is_building_entrance === 'true') {
            obj.is_building_entrance = "true";
        } else {
            obj.is_building_entrance = "false";
        }

        var promise = $scope.anyAPI.deletePois(obj);

        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                if (obj.is_building_entrance === "true") {
                    obj.is_building_entrance = true;
                } else {
                    obj.is_building_entrance = false;
                }

                // delete the building from the loadedBuildings
                var lp = $scope.myPois;
                var sz = lp.length;
                for (var i = 0; i < sz; i++) {
                    if (lp[i].puid == delPuid) {
                        lp.splice(i, 1); // delete the pois from the loadPois list

                        if ($scope.myPoisHashT[delPuid] && $scope.myPoisHashT[delPuid].marker) {
                            $scope.myPoisHashT[delPuid].marker.setMap(null);
                        }
                        delete $scope.myPoisHashT[delPuid]; // delete the POIS from the hashtable
                        $scope.anyService.setAllPois($scope.myPoisHashT);
                        break;
                    }
                }
                _suc("Successfully deleted POI.");
            },
            function (resp) {
                // on error
                var data = resp.data;

                var lp = $scope.myPois;
                var sz = lp.length;
                for (var i = 0; i < sz; i++) {
                    if (lp[i].puid == delPuid) {
                        lp.splice(i, 1); // delete the pois from the loadPois list

                        if ($scope.myPoisHashT[delPuid] && $scope.myPoisHashT[delPuid].marker) {
                            $scope.myPoisHashT[delPuid].marker.setMap(null);
                        }
                        delete $scope.myPoisHashT[delPuid]; // delete the POIS from the hashtable
                        $scope.anyService.setAllPois($scope.myPoisHashT);
                        break;
                    }
                }

                _err("Something went wrong while deleting POI.");
            });

    };

    $scope.updatePoi = function (id) {
        var bobj = $scope.myPoisHashT[id];

        if (!bobj || !bobj.model || !bobj.model.puid) {
            if ($scope.myPoisHashT && $scope.myMarkers && $scope.myMarkers[id] && $scope.myMarkers[id].model && $scope.myPoisHashT[$scope.myMarkers[id].model.puid]) {

                bobj = $scope.myPoisHashT[$scope.myMarkers[id].model.puid];

                if (!bobj || !bobj.model || !bobj.model.puid) {
                    _err("No valid POI selected to be updated.");
                    return;
                }

            } else {
                _err("No valid POI selected to be updated.");
                return;
            }
        }

        if (bobj.model.pois_type2.localeCompare("") != 0) {
            bobj.model.pois_type = bobj.model.pois_type2;
        }

        var obj = bobj.model;
        obj.username = $scope.creds.username;
        obj.password = $scope.creds.password;
        obj.owner_id = $scope.owner_id;

        var marker = bobj.marker;
        if (marker) {
            var latLng = marker.position;
            if (latLng && latLng.lat() && latLng.lng()) {
                obj.coordinates_lat = String(latLng.lat());
                obj.coordinates_lon = String(latLng.lng());
            }
        }

        if (obj.is_building_entrance) {
            obj.is_building_entrance = 'true';
        } else {
            obj.is_building_entrance = 'false';
        }

        // make the request at AnyplaceAPI
        var promise = $scope.anyAPI.updatePois(obj);
        promise.then(
            function (resp) {
                // success
                var data = resp.data;

                if (marker) {
                    marker.infowindow.setMap(null);
                }

                if (obj.is_building_entrance == 'true') {
                    obj.is_building_entrance = true;
                } else {
                    obj.is_building_entrance = false;
                }

                for (var c in $scope.myConnectionsHashT) {
                    if ($scope.myConnectionsHashT.hasOwnProperty(c)) {
                        if ($scope.myConnectionsHashT[c].pois_a == obj.puid
                            || $scope.myConnectionsHashT[c].pois_b == obj.puid) {

                            var pa = $scope.myConnectionsHashT[c].pois_a;
                            var pb = $scope.myConnectionsHashT[c].pois_b;

                            var flightPlanCoordinates = [
                                new google.maps.LatLng($scope.myPoisHashT[pa].model.coordinates_lat, $scope.myPoisHashT[pa].model.coordinates_lon),
                                new google.maps.LatLng($scope.myPoisHashT[pb].model.coordinates_lat, $scope.myPoisHashT[pb].model.coordinates_lon)
                            ];

                            $scope.myConnectionsHashT[c].polyLine.setPath(flightPlanCoordinates);
                            $scope.anyService.setAllConnection($scope.myConnectionsHashT);
                            $scope.anyService.setAllPois($scope.myPoisHashT);
                        }
                    }
                }

                //_suc("Successfully updated POI.");
            },
            function (resp) {
                // error
                var data = resp.data;
                _err("Something went wrong while updating POI. Please refresh and try again.");
            }
        );
    };

    $scope.updatePoiPosition = function (marker) {

        var obj;

        if (marker && marker.model) {
            obj = marker.model;
        }

        obj.username = $scope.creds.username;
        obj.password = $scope.creds.password;
        obj.owner_id = $scope.owner_id;

        var latLng = marker.position;
        if (latLng && latLng.lat() && latLng.lng()) {
            obj.coordinates_lat = String(latLng.lat());
            obj.coordinates_lon = String(latLng.lng());
        }

        if (obj.is_building_entrance) {
            obj.is_building_entrance = 'true';
        } else {
            obj.is_building_entrance = 'false';
        }

        // make the request at AnyplaceAPI
        var promise = $scope.anyAPI.updatePois(obj);
        promise.then(
            function (resp) {
                // success
                var data = resp.data;

                if (obj.is_building_entrance == 'true' || obj.is_building_entrance === true) {
                    obj.is_building_entrance = true;
                } else {
                    obj.is_building_entrance = false;
                }

                for (var c in $scope.myConnectionsHashT) {
                    if ($scope.myConnectionsHashT.hasOwnProperty(c)) {
                        if ($scope.myConnectionsHashT[c].pois_a == marker.model.puid
                            || $scope.myConnectionsHashT[c].pois_b == marker.model.puid) {

                            var pa = $scope.myConnectionsHashT[c].pois_a;
                            var pb = $scope.myConnectionsHashT[c].pois_b;

                            var flightPlanCoordinates = [
                                new google.maps.LatLng($scope.myPoisHashT[pa].model.coordinates_lat, $scope.myPoisHashT[pa].model.coordinates_lon),
                                new google.maps.LatLng($scope.myPoisHashT[pb].model.coordinates_lat, $scope.myPoisHashT[pb].model.coordinates_lon)
                            ];

                            $scope.myConnectionsHashT[c].polyLine.setPath(flightPlanCoordinates);
                            $scope.anyService.setAllConnection($scope.myConnectionsHashT);
                            $scope.anyService.setAllPois($scope.myPoisHashT);
                        }
                    }
                }

            },
            function (resp) {
                // error
                var data = resp.data;
                _err("Something went wrong while moving POI.");
            }
        );
    };

    $scope.removeMarker = function (id) {
        if ($scope.myMarkers[id]) {
            $scope.myMarkers[id].marker.setMap(null);
            delete $scope.myMarkers[id];
        } else {
            _err("It seems that the marker to be deleted does not exist.");
        }
    };

    $scope.placeMarker = function (location, iconImage, size, type) {

        var marker = new google.maps.Marker({
            position: location,
            map: GMapService.gmap,
            icon: new google.maps.MarkerImage(
                iconImage,
                null, /* size is determined at runtime */
                null, /* origin is 0,0 */
                null, /* anchor is bottom center of the scaled image */
                size
            ),
            draggable: true
        });

        if (AnyplaceService.getBuildingId() === null || AnyplaceService.getBuildingId() === undefined) {
            _err("It seems there is no building selected. Please refresh.");
            return;
        }

        var infowindow = new google.maps.InfoWindow({
            content: '-',
            maxWidth: 500
        });

        $scope.$apply(function () {
            marker.myId = $scope.myMarkerId;
            $scope.myMarkers[marker.myId] = {};
            $scope.myMarkers[marker.myId].model = {
                buid: AnyplaceService.getBuildingId(),
                floor_number: AnyplaceService.getFloorNumber(),
                floor_name: AnyplaceService.getFloorName(),
                is_door: "false",
                is_building_entrance: "false",
                is_published: "true",
                description: type === 'connector' ? "Connector" : "",
                name: type === 'connector' ? "Connector" : "",
                pois_type: type === 'connector' ? "None" : "",
                pois_type2: type === 'connector' ? "None" : "",
                url: "-",
                image: "url_to_pois_image"
            };
            $scope.myMarkers[marker.myId].marker = marker;
            $scope.myMarkers[marker.myId].model.pois_type2 = $scope.myMarkers[marker.myId].model.pois_type;
            $scope.myMarkerId++;
        });

        var htmlContent = '<div class="infowindow-scroll-fix" ng-keydown="onInfoWindowKeyDown($event)">'
            + '<form name="poiForm">'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.name" id="poi-name" type="text" class="form-control" placeholder="poi name" tabindex="1" autofocus/>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<textarea ng-model="myMarkers[' + marker.myId + '].model.description" id="poi-description" type="text" class="form-control" placeholder="poi description" tabindex="5"></textarea>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<select ng-model="myMarkers[' + marker.myId + '].model.pois_type" class="form-control" ng-options="type for type in poisTypes" title="POI Types" tabindex="2">'
            + '<option value="">Select POI Type</option>'
            + '</select>'
            + '</fieldset class="form-group">'
            + '<fieldset class="form-group">Or ender your one type name:'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.pois_type2" id="poi-pois_type2" type="text" class="form-control" placeholder="POI Type" tabindex="2">'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.is_building_entrance" id="poi-entrance" type="checkbox" tabindex="4"><span> is building entrance?</span>'
            + '</fieldset>'
            + '<div style="text-align: center;">'
            + '<fieldset class="form-group" style="display: inline-block; width: 75%;">'
            + '<button type="submit" class="btn btn-success add-any-button" ng-click="addPoi(' + marker.myId + ')" tabindex="3"><span class="glyphicon glyphicon-plus"></span> Add'
            + '</button>'
            + '</fieldset>'
            + '<fieldset class="form-group" style="display: inline-block;width: 23%;">'
            + '<button type="submit" class="btn btn-danger add-any-button" style="margin-left:2px" ng-click="deleteTempPoi(' + marker.myId + ')" tabindex="6"><span class="glyphicon glyphicon-remove"></span>'
            + '</button>'
            + '</fieldset>'
            + '</div>'
            + '</form>'
            + '</div>';

        //var htmlContent2 = '<div class="infowindow-scroll-fix">'
        //    + '<h5 style="margin: 0">Name:</h5>'
        //    + '<span>{{myMarkers[' + marker.myId + '].model.name}}</span>'
        //    + '<h5 style="margin: 8px 0 0 0">Description:</h5>'
        //    + '<span>{{myMarkers[' + marker.myId + '].model.description}}</span>'
        //    + '<h5 style="margin: 8px 0 0 0">Type:</h5>'
        //    + '<span>{{myMarkers[' + marker.myId + '].model.pois_type}}</span>'
        //    + '</div>';
        var htmlContent2 = '<div class="infowindow-scroll-fix" ng-keydown="onInfoWindowKeyDown($event)">'
            + '<form name="poiForm">'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.name" id="poi-name" type="text" class="form-control" placeholder="poi name" tabindex="1" autofocus/>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<textarea ng-model="myMarkers[' + marker.myId + '].model.description" id="poi-description" type="text" class="form-control" placeholder="poi description" tabindex="5"></textarea>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<select ng-model="myMarkers[' + marker.myId + '].model.pois_type" class="form-control" ng-options="type for type in poisTypes" title="POI Types" tabindex="2">'
            + '<option value="">Select POI Type</option>'
            + '</select>'
            + '</fieldset class="form-group">'
            + '<fieldset class="form-group">Or ender your one type name:'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.pois_type" id="poi-pois_type2" type="text" class="form-control" placeholder="POI Type" tabindex="2">'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.is_building_entrance" id="poi-entrance" type="checkbox" tabindex="4"><span> is building entrance?</span>'
            + '</fieldset>'
            + '<div style="text-align: center;">'
            + '<fieldset class="form-group" style="display: inline-block; width: 75%;">'
            + '<button type="submit" class="btn btn-success add-any-button" ng-click="updatePoi(' + marker.myId + ')" tabindex="3"><span class="glyphicon glyphicon-pencil"></span> Update'
            + '</button>'
            + '</fieldset>'
            + '<fieldset class="form-group" style="display: inline-block;width: 23%;">'
            + '<button type="submit" class="btn btn-danger add-any-button" style="margin-left:2px" ng-click="deletePoi(' + marker.myId + ')" tabindex="6"><span class="glyphicon glyphicon-remove"></span>'
            + '</button>'
            + '</fieldset>'
            + '</div>'
            + '</form>'
            + '</div>';

        var htmlConnector = '<div class="infowindow-scroll-fix" style="text-align: center; width:170px">'
            + '<div style="margin-bottom: 5px">POI Connector</div>'
            + '<fieldset class="form-group" style="display: inline-block; width: 73%;">'
            + '<button type="submit" class="btn btn-success add-any-button" ng-click="addPoi(' + marker.myId + ')"><span class="glyphicon glyphicon-plus"></span> Add'
            + '</button>'
            + '</fieldset>'
            + '<fieldset class="form-group" style="display: inline-block;width: 23%;">'
            + '<button type="submit" class="btn btn-danger add-any-button" style="margin-left:2px" ng-click="deleteTempPoi(' + marker.myId + ')"><span class="glyphicon glyphicon-remove"></span>'
            + '</button>'
            + '</fieldset>'
            + '</div>';

        var htmlConnector2 = '<div class="infowindow-scroll-fix" style="text-align: center; width:170px">'
            + '<div style="margin-bottom: 5px">POI Connector</div>'
            + '<fieldset class="form-group">'
            + '<button type="submit" class="btn btn-danger add-any-button" style="margin-left:2px" ng-click="deletePoi(' + marker.myId + ')"><span class="glyphicon glyphicon-remove"></span> Remove'
            + '</button>'
            + '</fieldset>'
            + '</div>';

        if (type == 'poi') {
            var tpl = $compile(htmlContent)($scope);
            marker.tpl2 = $compile(htmlContent2)($scope);
            infowindow.setContent(tpl[0]);
            infowindow.open(GMapService.gmap, marker);
        } else if (type == 'connector') {
            var tplConn = $compile(htmlConnector)($scope);
            marker.tpl2 = $compile(htmlConnector2)($scope);
            infowindow.setContent(tplConn[0]);
            //infowindow.open(GMapService.gmap, marker);

            $scope.addPoi(marker.myId);
        }

        $scope.$apply(
            $scope.myMarkers[marker.myId].infowindow = infowindow
        );

        google.maps.event.addListener(marker, 'click', function () {
            if ($scope.edgeMode) {
                $scope.$apply(_warn("Only submitted objects can be connected together."));
            }
            infowindow.open(GMapService.gmap, marker);
        });


    };

    $scope.deleteTempPoi = function (i) {
        if (!$scope.myMarkers || !$scope.myMarkers[i].marker) {
            _err("No valid POI marker to delete found.");
            return;
        }
        $scope.myMarkers[i].marker.setMap(null);
    };

    $scope.toggleEdgeMode = function () {
        $scope.edgeMode = !$scope.edgeMode;
        if (!$scope.edgeMode) {
            $scope.connectPois.prev = undefined;
        }
    };

    $("#draggable-poi").draggable({
        helper: 'clone',
        stop: function (e) {
            var point = new google.maps.Point(e.pageX, e.pageY);
            var ll = overlay.getProjection().fromContainerPixelToLatLng(point);
            if (!_isPoiNearFloor(ll)) {
                $scope.$apply(_warn("The marker was placed too far away from the selected building."));
                return;
            }
            $scope.placeMarker(ll, _POI_NEW_IMG, new google.maps.Size(21, 32), 'poi');
        }
    });

    $("#draggable-connector").draggable({
        helper: 'clone',
        stop: function (e) {
            var point = new google.maps.Point(e.pageX, e.pageY);
            var ll = overlay.getProjection().fromContainerPixelToLatLng(point);
            if (!_isPoiNearFloor(ll)) {
                $scope.$apply(_warn("The marker was placed too far away from the selected building."));
                return;
            }
            $scope.placeMarker(ll, _POI_CONNECTOR_IMG, new google.maps.Size(21, 21), 'connector');
        }
    });

}
])
;
/**
 *
 The MIT License (MIT)

 Copyright (c) 2015, Marileni Angelidou, Data Management Systems Laboratory (DMSL)
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


app.controller('SelectFloorController', ['$scope', 'AnyplaceService', 'GMapService', 'AnyplaceAPIService', function ($scope, AnyplaceService, GMapService, AnyplaceAPIService) {
    $scope.anyService = AnyplaceService;
    $scope.anyAPI = AnyplaceAPIService;
    $scope.gmapService = GMapService;
    $scope.xFloors = [];

    $scope.$watch('anyService.availableFloors', function (newVal, oldVal) {
        if (newVal) {
            $scope.xFloors = [];
            $scope.xFloors = $scope.anyService.availableFloors;
        }
    });

    var _err = function (msg) {
        $scope.anyService.addAlert('danger', msg);
    };

    $scope.orderByFloorNo = function (floor) {
        if (!floor || LPUtils.isNullOrUndefined(floor.floor_number)) {
            return 0;
        }
        return parseInt(floor.floor_number);
    };


    $scope.floorUp = function () {
        //here new
        changedfloor = true;
        var next;
        for (var i = 0; i < $scope.xFloors.length; i++) {
            next = i + 1;
            if ($scope.xFloors[i].floor_number === $scope.anyService.selectedFloor.floor_number) {

                if (next < $scope.xFloors.length) {
                    $scope.anyService.selectedFloor = $scope.xFloors[next];
                    return;
                } else {
                    //_warn("There is no other floor above.");
                    return;
                }
            }
        }

        _err("Floor not found.");
    };

    $scope.floorDown = function () {

        changedfloor = true;
        var prev;
        for (var i = 0; i < $scope.xFloors.length; i++) {
            prev = i - 1;
            if ($scope.xFloors[i].floor_number === $scope.anyService.selectedFloor.floor_number) {

                if (prev >= 0) {
                    $scope.anyService.selectedFloor = $scope.xFloors[prev];
                    return;
                } else {
                    //_warn("There is no other floor below.");
                    return;
                }
            }
        }

        _err("Floor not found.");
    };


}]);
/**
 *
 The MIT License (MIT)

 Copyright (c) 2015, Marileni Angelidou , Data Management Systems Laboratory (DMSL)
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



var heatMap = [];


var heatMap_Location = [];


var APmap = [];
var heatmap;
var heatmapFingerprints = [];
var userTimeData = [];
var fingerPrintsMap = [];
var heatmapAcc;
var connectionsMap = {};
var POIsMap = {};
var drawingManager;
var _HEATMAP_RSS_IS_ON = false;
var _HEATMAP_Localization = false; //lsolea01
var _APs_IS_ON = false;
var _FINGERPRINTS_IS_ON = false;
var _DELETE_FINGERPRINTS_IS_ON = false;
var _HEATMAP_F_IS_ON = false;
var _CONNECTIONS_IS_ON = false;
var _POIS_IS_ON = false;
var changedfloor = false;
var colorBarGreenClicked = false;
var colorBarYellowClicked = false;
var colorBarOrangeClicked = false;
var colorBarPurpleClicked = false;
var colorBarRedClicked = false;
var levelOfZoom = 1;


app.controller('WiFiController', ['$cookieStore', '$scope', 'AnyplaceService', 'GMapService', 'AnyplaceAPIService', function ($cookieStore, $scope, AnyplaceService, GMapService, AnyplaceAPIService) {
    $scope.anyService = AnyplaceService;
    $scope.anyAPI = AnyplaceAPIService;
    $scope.gmapService = GMapService;
    $scope.example9model = [];
    $scope.example9data = [];
    $scope.example9settings = {enableSearch: true, scrollable: true};
    $scope.example8model = [];
    $scope.example8data = [];
    $scope.deleteButtonWarning = false;
    $scope.radioHeatmapRSSMode = false;
    $scope.radioHeatmapLocalization = false; //lsolea01

    $scope.radioHeatmapRSSTimeMode = false;
    $scope.fingerPrintsMode = false;
    $scope.fingerPrintsTimeMode = false;
    $scope.APsMode = false;
    $scope.filterByMAC = false;
    $scope.filterByMAN = false;
    $scope.radioHeatmapRSSHasGreen = false;
    $scope.radioHeatmapRSSHasYellow = false;
    $scope.radioHeatmapRSSHasOrange = false;
    $scope.radioHeatmapRSSHasPurple = false;
    $scope.radioHeatmapRSSHasRed = false;
    $scope.localizationAccMode = false;
    $scope.selected = "Filters:";
    $scope.initializeTime = false;
    $scope.initializeFingerPrints = false;
    $scope.initializeRadioHeatmapRSS = false;
    $scope.initializeAPs = false;
    $scope.initializeConnections = false;
    $scope.initializePOIs = false;
    $scope.initializeAcces = false;


    var MAX = 1000;
    var MIN_ZOOM_FOR_HEATMAPS = 19;
    var MAX_ZOOM_FOR_HEATMAPS = 21;
    var _MAX_ZOOM_LEVEL = 22;
    var _NOW_ZOOM;
    var _PREV_ZOOM;


    $scope.crudTabSelected = 1;
    $scope.setCrudTabSelected = function (n) {
        $scope.crudTabSelected = n;
    };
    $scope.isCrudTabSelected = function (n) {

        return $scope.crudTabSelected === n;
    };


    $scope.data = {
        floor_plan_coords: {},
        floor_plan_base64_data: {},
        floor_plan_groundOverlay: null
    };

    function clearRadioHeatmap() {


        var check = 0;
        if (heatMap[check] !== undefined && heatMap[check] !== null) {

            var i = heatMap.length;
            while (i--) {
                heatMap[i].rectangle.setMap(null);
                heatMap[i] = null;
            }
            heatMap = [];
            document.getElementById("radioHeatmapRSS-mode").classList.remove('draggable-border-green');
            _HEATMAP_RSS_IS_ON = false;
            setColorClicked('g', false);
            setColorClicked('y', false);
            setColorClicked('o', false);
            setColorClicked('p', false);
            setColorClicked('r', false);
            $scope.radioHeatmapRSSMode = false;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('radioHeatmapRSSMode', 'NO');
            }
            $scope.anyService.radioHeatmapRSSMode = false;
            $scope.radioHeatmapRSSHasGreen = false;
            $scope.radioHeatmapRSSHasYellow = false;
            $scope.radioHeatmapRSSHasOrange = false;
            $scope.radioHeatmapRSSHasPurple = false;
            $scope.radioHeatmapRSSHasRed = false;
            $cookieStore.put('RSSClicked', 'NO');

        }
    }

    function clearLocalization() { //lsolea01


        var check = 0;
        if (heatMap_[check] !== undefined && heatMap[check] !== null) {

            var i = heatMap.length;
            while (i--) {
                heatMap[i].rectangle.setMap(null);
                heatMap[i] = null;
            }
            heatMap = [];
            document.getElementById("radioHeatmapRSS-mode").classList.remove('draggable-border-green');
            _HEATMAP_Localization = false;
            setColorClicked('g', false);
            setColorClicked('y', false);
            setColorClicked('o', false);
            setColorClicked('p', false);
            setColorClicked('r', false);
            $scope.radioHeatmapLocalization = false;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('radioHeatmapLocalization', 'NO');
            }
            $scope.anyService.radioHeatmapLocalization = false;
            $scope.radioHeatmapRSSHasGreen = false;
            $scope.radioHeatmapRSSHasYellow = false;
            $scope.radioHeatmapRSSHasOrange = false;
            $scope.radioHeatmapRSSHasPurple = false;
            $scope.radioHeatmapRSSHasRed = false;
            $cookieStore.put('RSSClicked', 'NO');

        }
    }

    function clearFingerPrints() {

        // if ($scope.fingerPrintsMode) {
        //     document.getElementById("fingerPrints-mode").classList.add('draggable-border-green');
        // } else {
        //     document.getElementById("fingerPrints-mode").classList.remove('draggable-border-green');
        // }

        var check = 0;

        if (fingerPrintsMap[check] !== undefined && fingerPrintsMap[check] !== null) {
            var i = fingerPrintsMap.length;

            //hide fingerPrints
            while (i--) {
                fingerPrintsMap[i].setMap(null);
                fingerPrintsMap[i] = null;
            }
            fingerPrintsMap = [];
            _FINGERPRINTS_IS_ON = false;
            document.getElementById("fingerPrints-mode").classList.remove('draggable-border-green');


        }

        if (heatmap && heatmap.getMap()) {
            //hide fingerPrints heatmap

            heatmap.setMap(null);
            _FINGERPRINTS_IS_ON = false;
            document.getElementById("fingerPrints-mode").classList.remove('draggable-border-green');
            _HEATMAP_F_IS_ON = false;
            var i = heatmapFingerprints.length;
            while (i--) {
                heatmapFingerprints[i] = null;
            }
            heatmapFingerprints = [];

        }

        // document.getElementById("fingerPrints-mode").classList.add('draggable-border-green');

    }

    function initializeTimeFunction() {
        if ($scope.fingerPrintsMode) {

            $scope.fingerPrintsTimeMode = true;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('fingerPrintsTimeMode', 'YES');
            }
            $scope.anyService.fingerPrintsTimeMode = true;
            $scope.fingerPrintsMode = true;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('fingerprintsMode', 'YES');
            }
            // document.getElementById("fingerPrints-time-mode").classList.add('draggable-border-green');
        }

        if ($scope.radioHeatmapRSSMode) {
            $scope.radioHeatmapRSSTimeMode = true;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('fingerPrintsTimeMode', 'YES');
            }
            $scope.radioHeatmapRSSMode = true;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('radioHeatmapRSSMode', 'YES');
            }
            $scope.anyService.radioHeatmapRSSTimeMode = true;
            $scope.anyService.radioHeatmapRSSMode = true;
            // document.getElementById("radioHeatmapRSS-time-mode").classList.add('draggable-border-green');
        }
        document.getElementById("fingerPrints-time-mode").classList.add('draggable-border-green');


        if ($scope.radioHeatmapLocalization) { //lsolea01
            $scope.radioHeatmapRSSTimeMode = true;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('fingerPrintsTimeMode', 'YES');
            }
            $scope.radioHeatmapLocalization = true;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('radioHeatmapLocalization', 'YES');
            }
            $scope.anyService.radioHeatmapRSSTimeMode = true;
            $scope.anyService.radioHeatmapLocalization = true;
            // document.getElementById("radioHeatmapRSS-time-mode").classList.add('draggable-border-green');
        }
        document.getElementById("fingerPrints-time-mode").classList.add('draggable-border-green');

    }


    $scope.$watch('anyService.selectedBuilding', function (newVal, oldVal) { //lsolea01
        if (newVal) {


            if (localStorage.getItem('fingerprintsMode') !== undefined) {

                if (localStorage.getItem('fingerprintsMode') === 'YES') {
                    $scope.initializeFingerPrints = true;

                }
            }

            if (localStorage.getItem('radioHeatmapRSSMode') !== undefined) {

                if (localStorage.getItem('radioHeatmapRSSMode') === 'YES') {
                    $scope.initializeRadioHeatmapRSS = true;

                }
            }

            if (localStorage.getItem('radioHeatmapLocalization') !== undefined) {

                if (localStorage.getItem('radioHeatmapLocalization') === 'YES') {
                    $scope.initializeRadioHeatmapRSS = true;

                }
            }

            if (localStorage.getItem('APsMode') !== undefined) {

                if (localStorage.getItem('APsMode') === 'YES') {
                    $scope.initializeAPs = true;

                }
            }

            if (localStorage.getItem('localizationAccMode') !== undefined) {

                if (localStorage.getItem('localizationAccMode') === 'YES') {
                    $scope.initializeAcces = true;

                }
            }

            if (localStorage.getItem('connectionsMode') !== undefined) {

                if (localStorage.getItem('connectionsMode') === 'NO') {
                    $scope.initializeConnections = true;

                }
            }

            if (localStorage.getItem('POIsMode') !== undefined) {

                if (localStorage.getItem('POIsMode') === 'NO') {
                    $scope.initializePOIs = true;
                }
            }

            if (localStorage.getItem('fingerPrintsTimeMode') !== undefined) {

                if (localStorage.getItem('fingerPrintsTimeMode') === 'YES') {
                    $scope.initializeTime = true;

                }
            }

            function initializeFingerPrints() {

                $('#wifiTab').click();
                $('#FPs').click();
                $('#FPsButton').click();
            }

            function initializeRadioHeatmapRSS() {
                $('#wifiTab').click();
                $('#HMs').click();
                $('#HMsButton').click();
            }

            function initializeAPs() {
                $('#wifiTab').click();
                $('#HMs').click();
                $('#APsButton').click();
            }

            function initializeAcces() {

                $('#wifiTab').click();
                $('#LAs').click();
                $('#LAButton').click();
            }

            function initializeConnections() {
                $('#FPs').click();
                $('#connectionsButton').click();
            }

            function initializePOIs() {
                $('#FPs').click();
                $('#POIsButton').click();
            }

            function initializeTime() {

                $('#wifiTab').click();
                $('#FPs').click();
                $('#FPsTimeButton').click();
            }

            window.onload = function () {
                if ($scope.initializeFingerPrints) {
                    initializeFingerPrints();
                }
                if ($scope.initializeRadioHeatmapRSS) {
                    initializeRadioHeatmapRSS();
                }
                if ($scope.initializeAPs) {
                    initializeAPs();
                }
                if ($scope.initializeAcces) {
                    initializeAcces();
                }
                if ($scope.initializeConnections) {
                    initializeConnections();
                }
                if ($scope.initializePOIs) {
                    initializePOIs();
                }
                if ($scope.initializeTime) {
                    initializeTime();
                }
            };

            if (_HEATMAP_RSS_IS_ON) {

                var i = heatMap.length;
                while (i--) {
                    heatMap[i].rectangle.setMap(null);
                    heatMap[i] = null;
                }
                heatMap = [];

                $scope.showRadioHeatmapRSS();

                if ($scope.radioHeatmapRSSTimeMode) {
                    d3.selectAll("svg > *").remove();
                    $("svg").remove();
                    $scope.getFingerPrintsTime();
                }

            }

            if (_HEATMAP_Localization) { //lsolea01

                var i = heatMap_Location.length;
                while (i--) {
                    heatMap_Location[i].setMap(null);
                    heatMap_Location[i] = null;
                }
                heatMap_Location = [];

                $scope.showLocalizationAccHeatmap();


            }

            if (_APs_IS_ON) {
                var i = APmap.length;

                //hide Access Points
                while (i--) {
                    APmap[i].setMap(null);
                    APmap[i] = null;
                    $scope.example9data[i] = null;
                    $scope.example9model[i] = null;
                }

                i = $scope.example8data.length;
                while (i--) {
                    $scope.example8data[i] = null;
                    $scope.example8model[i] = null;
                }
                APmap = [];
                $scope.example9data = [];
                $scope.example9model = [];
                $scope.example8data = [];
                $scope.example8model = [];
                $scope.showAPs();


            }
            if (_FINGERPRINTS_IS_ON) {
                var i = fingerPrintsMap.length;

                //hide fingerPrints
                while (i--) {
                    fingerPrintsMap[i].setMap(null);
                    fingerPrintsMap[i] = null;
                }
                fingerPrintsMap = [];

                $scope.showFingerPrints();

                if ($scope.fingerPrintsTimeMode && !$scope.radioHeatmapRSSTimeMode) {
                    d3.selectAll("svg > *").remove();
                    $("svg").remove();
                    $scope.getFingerPrintsTime();
                }


            }


            if (heatmap && heatmap.getMap()) {
                //hide fingerPrints heatmap
                heatmap.setMap(null);
                var i = heatmapFingerprints.length;
                while (i--) {
                    heatmapFingerprints[i] = null;
                }
                heatmapFingerprints = [];
                _HEATMAP_F_IS_ON = false;

                $scope.showFingerPrints();

                if ($scope.fingerPrintsTimeMode && !$scope.radioHeatmapRSSTimeMode) {

                    d3.selectAll("svg > *").remove();
                    $("svg").remove();
                    $scope.getFingerPrintsTime();

                }
            }

            // if (heatmapAcc && heatmapAcc.getMap()) { //lsolea01
            //     //hide acces heatmap
            //
            //     heatmapAcc.setMap(null);
            //     $scope.showLocalizationAccHeatmap();
            //
            // }

            var check = 0;
            if (!_CONNECTIONS_IS_ON) {
                connectionsMap = $scope.anyService.getAllConnections();
                var key = Object.keys(connectionsMap);
                if (connectionsMap[key[check]] !== undefined) {
                    if (connectionsMap[key[check]].polyLine !== undefined) {
                        if (connectionsMap[key[check]].polyLine.getMap() !== null) {
                            for (var key in connectionsMap) {
                                if (connectionsMap.hasOwnProperty(key)) {
                                    var con = connectionsMap[key];
                                    if (con && con.polyLine) {
                                        con.polyLine.setMap(null);
                                    }
                                }

                            }
                            $scope.anyService.setAllConnection(connectionsMap);
                            connectionsMap = {};
                        }
                    }
                }

            }

            if (!_POIS_IS_ON) {
                POIsMap = $scope.anyService.getAllPois();
                if (POIsMap !== undefined) {
                    var key = Object.keys(POIsMap);
                    if (POIsMap[key[check]] !== undefined) {

                        if (POIsMap[key[check]].marker.getMap() !== null) {

                            for (var key in POIsMap) {
                                if (POIsMap.hasOwnProperty(key)) {

                                    var p = POIsMap[key];
                                    if (p && p.marker) {
                                        p.marker.setMap(null);

                                    }
                                }
                            }

                            $scope.anyService.setAllPois(POIsMap);
                            POIsMap = {};

                        }
                    }
                }
            }

            changedfloor = false;

        }
    });

    $scope.$watch('newFloorNumber', function (newVal, oldVal) {
        //if (_floorNoExists(newVal)) {
        //    _setNextFloor();
        //}
    });

    var _err = function (msg) {
        $scope.anyService.addAlert('danger', msg);
    };

    var _suc = function (msg) {
        $scope.anyService.addAlert('success', msg);
    };


    $scope.$watch('anyService.selectedFloor', function (newVal, oldVal) {
        if (newVal !== undefined && newVal !== null && !_.isEqual(newVal, oldVal)) {

            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem("lastFloor", newVal.floor_number);
            }

            if (_HEATMAP_RSS_IS_ON) {

                var i = heatMap.length;
                while (i--) {
                    heatMap[i].rectangle.setMap(null);
                    heatMap[i] = null;
                }
                heatMap = [];

                $scope.showRadioHeatmapRSS();

                if ($scope.radioHeatmapRSSTimeMode) {
                    d3.selectAll("svg > *").remove();
                    $("svg").remove();
                    $scope.getFingerPrintsTime();
                }

            }
            if (_HEATMAP_Localization) {

                var i = heatMap_Location.length;
                while (i--) {
                    heatMap_Location[i].setMap(null);
                    heatMap_Location[i] = null;
                }
                heatMap_Location = [];

                $scope.showLocalizationAccHeatmap();

            }

            if (_APs_IS_ON) {
                var i = APmap.length;

                //hide Access Points
                while (i--) {
                    APmap[i].setMap(null);
                    APmap[i] = null;
                    $scope.example9data[i] = null;
                    $scope.example9model[i] = null;
                }
                i = $scope.example8data.length;
                while (i--) {
                    $scope.example8data[i] = null;
                    $scope.example8model[i] = null;
                }

                APmap = [];
                $scope.example9data = [];
                $scope.example9model = [];
                $scope.example8data = [];
                $scope.example8model = [];
                $scope.showAPs();

            }
            if (_FINGERPRINTS_IS_ON) {
                var i = fingerPrintsMap.length;

                //hide fingerPrints
                while (i--) {
                    fingerPrintsMap[i].setMap(null);
                    fingerPrintsMap[i] = null;
                }
                fingerPrintsMap = [];

                $scope.showFingerPrints();
                if ($scope.fingerPrintsTimeMode && !$scope.radioHeatmapRSSTimeMode) {

                    d3.selectAll("svg > *").remove();
                    $("svg").remove();
                    $scope.getFingerPrintsTime();

                }
            }


            if (heatmap && heatmap.getMap()) {
                //hide fingerPrints heatmap
                heatmap.setMap(null);
                var i = heatmapFingerprints.length;
                while (i--) {
                    heatmapFingerprints[i] = null;
                }
                heatmapFingerprints = [];
                _HEATMAP_F_IS_ON = false;

                $scope.showFingerPrints();
                if ($scope.fingerPrintsTimeMode && !$scope.radioHeatmapRSSTimeMode) {

                    d3.selectAll("svg > *").remove();
                    $("svg").remove();
                    $scope.getFingerPrintsTime();

                }
            }

            // if (heatmapAcc && heatmapAcc.getMap()) { lsolea01
            //     //hide acces heatmap
            //
            //     heatmapAcc.setMap(null);
            //     $scope.showLocalizationAccHeatmap();
            //
            // }

            var check = 0;
            if (!_CONNECTIONS_IS_ON) {
                connectionsMap = $scope.anyService.getAllConnections();
                var key = Object.keys(connectionsMap);
                if (connectionsMap[key[check]] !== undefined) {
                    if (connectionsMap[key[check]].polyLine !== undefined) {
                        if (connectionsMap[key[check]].polyLine.getMap() !== null) {
                            for (var key in connectionsMap) {
                                if (connectionsMap.hasOwnProperty(key)) {
                                    var con = connectionsMap[key];
                                    if (con && con.polyLine) {
                                        con.polyLine.setMap(null);
                                    }
                                }

                            }
                            $scope.anyService.setAllConnection(connectionsMap);
                            connectionsMap = {};
                        }
                    }
                }

            }

            if (!_POIS_IS_ON) {
                POIsMap = $scope.anyService.getAllPois();
                if (POIsMap !== undefined) {
                    var key = Object.keys(POIsMap);
                    if (POIsMap[key[check]] !== undefined) {

                        if (POIsMap[key[check]].marker.getMap() !== null) {

                            for (var key in POIsMap) {
                                if (POIsMap.hasOwnProperty(key)) {

                                    var p = POIsMap[key];
                                    if (p && p.marker) {
                                        p.marker.setMap(null);

                                    }
                                }
                            }

                            $scope.anyService.setAllPois(POIsMap);
                            POIsMap = {};

                        }
                    }
                }
            }
            changedfloor = false;
        }

    });

    $scope.toggleRadioHeatmapRSS = function () { //lsole

        var check = 0;
        if ((heatMap[check] !== undefined && heatMap[check] !== null) || $scope.radioHeatmapRSSTimeMode) {
            var i = heatMap.length;
            while (i--) {
                heatMap[i].rectangle.setMap(null);
                heatMap[i] = null;
            }
            heatMap = [];
            document.getElementById("radioHeatmapRSS-mode").classList.remove('draggable-border-green');
            //document.getElementById("radioHeatmapRSS-time-mode").classList.remove('draggable-border-green');
            _HEATMAP_RSS_IS_ON = false;
            setColorClicked('g', false);
            setColorClicked('y', false);
            setColorClicked('o', false);
            setColorClicked('p', false);
            setColorClicked('r', false);
            $scope.radioHeatmapRSSMode = false;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('radioHeatmapRSSMode', 'NO');
            }
            $scope.anyService.radioHeatmapRSSMode = false;
            $scope.radioHeatmapRSSTimeMode = false;
            if (typeof(Storage) !== "undefined" && localStorage && !$scope.fingerPrintsTimeMode) {
                localStorage.setItem('fingerPrintsTimeMode', 'NO');
            }
            $scope.anyService.radioHeatmapRSSTimeMode = false;
            $scope.radioHeatmapRSSHasGreen = false;
            $scope.radioHeatmapRSSHasYellow = false;
            $scope.radioHeatmapRSSHasOrange = false;
            $scope.radioHeatmapRSSHasPurple = false;
            $scope.radioHeatmapRSSHasRed = false;
            if (!$scope.fingerPrintsMode) {
                document.getElementById("fingerPrints-time-mode").classList.remove('draggable-border-green');
            }
            $cookieStore.put('RSSClicked', 'NO');
            return;
        }

        if ($scope.fingerPrintsTimeMode) {
            $scope.radioHeatmapRSSTimeMode = true;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('fingerPrintsTimeMode', 'YES');
            }
            $scope.anyService.radioHeatmapRSSTimeMode = true;
        }

        document.getElementById("radioHeatmapRSS-mode").classList.add('draggable-border-green');
        $scope.radioHeatmapRSSMode = true;
        if (typeof(Storage) !== "undefined" && localStorage) {
            localStorage.setItem('radioHeatmapRSSMode', 'YES');
        }

        $scope.anyService.radioHeatmapRSSMode = true;

        $scope.showRadioHeatmapRSS();
        return;
    };

    $scope.toggleAPs = function () {

        var check = 0;

        if (APmap[check] !== undefined && APmap[check] !== null) {
            var i = APmap.length;

            //hide Access Points
            while (i--) {
                APmap[i].setMap(null);
                APmap[i] = null;
                $scope.example9data[i] = null;
                $scope.example9model[i] = null;
            }
            i = $scope.example8data.length;
            while (i--) {
                $scope.example8data[i] = null;
                $scope.example8model[i] = null;
            }

            APmap = [];
            $scope.example9data = [];
            $scope.example9model = [];
            $scope.example8data = [];
            $scope.example8model = [];
            _APs_IS_ON = false;
            $scope.filterByMAC = false;
            $scope.filterByMAN = false;
            document.getElementById("APs-mode").classList.remove('draggable-border-green');
            $scope.APsMode = false;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('APsMode', 'NO');
            }
            return;

        }
        _APs_IS_ON = true;

        $scope.APsMode = true;

        if (typeof(Storage) !== "undefined" && localStorage) {
            localStorage.setItem('APsMode', 'YES');
        }

        document.getElementById("APs-mode").classList.add('draggable-border-green');

        $scope.showAPs();

    };
    $scope.toggleFingerPrints = function () {
        $scope.fingerPrintsMode = !$scope.fingerPrintsMode;

        if ($scope.fingerPrintsMode) {
            document.getElementById("fingerPrints-mode").classList.add('draggable-border-green');
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('fingerprintsMode', 'YES');
            }
        } else {
            document.getElementById("fingerPrints-mode").classList.remove('draggable-border-green');
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('fingerprintsMode', 'NO');
            }
        }

        var check = 0;

        if (fingerPrintsMap[check] !== undefined && fingerPrintsMap[check] !== null) {
            var i = fingerPrintsMap.length;

            //hide fingerPrints
            while (i--) {
                fingerPrintsMap[i].setMap(null);
                fingerPrintsMap[i] = null;
            }
            fingerPrintsMap = [];
            _FINGERPRINTS_IS_ON = false;
            document.getElementById("fingerPrints-mode").classList.remove('draggable-border-green');
            //document.getElementById("fingerPrints-time-mode").classList.remove('draggable-border-green');
            $scope.fingerPrintsMode = false;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('fingerprintsMode', 'NO');
            }
            $scope.fingerPrintsTimeMode = false;
            if (typeof(Storage) !== "undefined" && localStorage && !$scope.radioHeatmapRSSTimeMode) {
                localStorage.setItem('fingerPrintsTimeMode', 'NO');
            }
            $scope.anyService.fingerPrintsTimeMode = false;
            if (!$scope.radioHeatmapRSSMode) {
                document.getElementById("fingerPrints-time-mode").classList.remove('draggable-border-green');
            }
            return;

        }

        if (heatmap && heatmap.getMap()) {
            //hide fingerPrints heatmap

            heatmap.setMap(null);
            _FINGERPRINTS_IS_ON = false;
            document.getElementById("fingerPrints-mode").classList.remove('draggable-border-green');
            //document.getElementById("fingerPrints-time-mode").classList.remove('draggable-border-green');
            $scope.fingerPrintsMode = false;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('fingerprintsMode', 'NO');
            }
            $scope.fingerPrintsTimeMode = false;
            if (typeof(Storage) !== "undefined" && localStorage && !$scope.radioHeatmapRSSTimeMode) {
                localStorage.setItem('fingerPrintsTimeMode', 'NO');
            }
            $scope.anyService.fingerPrintsTimeMode = false;
            _HEATMAP_F_IS_ON = false;
            if (!$scope.radioHeatmapRSSMode) {
                document.getElementById("fingerPrints-time-mode").classList.remove('draggable-border-green');
            }
            var i = heatmapFingerprints.length;
            while (i--) {
                heatmapFingerprints[i] = null;
            }
            heatmapFingerprints = [];
            return;
        }

        document.getElementById("fingerPrints-mode").classList.add('draggable-border-green');
        $scope.fingerPrintsMode = true;
        if (typeof(Storage) !== "undefined" && localStorage) {
            localStorage.setItem('fingerprintsMode', 'YES');
        }
        if ($scope.radioHeatmapRSSTimeMode) {
            $scope.fingerPrintsTimeMode = true;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('fingerPrintsTimeMode', 'YES');
            }
            $scope.anyService.fingerPrintsTimeMode = true;
        }

        $scope.showFingerPrints();
    };


    $scope.toggleLocalizationAccurancy = function () {
        // $scope.localizationAccMode = !$scope.localizationAccMode;
        //         //
        //         // if (heatmapAcc && heatmapAcc.getMap()) {
        //         //     //hide fingerPrints heatmap
        //         //
        //         //     heatmapAcc.setMap(null);
        //         //     document.getElementById("localizationAccurancy-mode").classList.remove('draggable-border-green');
        //         //
        //         //     if (typeof(Storage) !== "undefined" && localStorage) {
        //         //         localStorage.setItem('localizationAccMode', 'NO');
        //         //     }
        //         //     return;
        //         // }
        //         //
        //         // document.getElementById("localizationAccurancy-mode").classList.add('draggable-border-green');
        //         //
        //         // if (typeof(Storage) !== "undefined" && localStorage) {
        //         //     localStorage.setItem('localizationAccMode', 'YES');
        //         // }
        //         // $scope.showLocalizationAccHeatmap();


        var check = 0;
        if ((heatMap_Location[check] !== undefined && heatMap_Location[check] !== null) || $scope.radioHeatmapLocalization) {
            var i = heatMap_Location.length;
            while (i--) {
                heatMap_Location[i].setMap(null);
                heatMap_Location[i] = null;
            }
            heatMap_Location = [];

            _HEATMAP_Localization = false;
            setColorClicked('g', false);
            setColorClicked('y', false);
            setColorClicked('o', false);
            setColorClicked('p', false);
            setColorClicked('r', false);
            document.getElementById("localizationAccurancy-mode").classList.remove('draggable-border-green');
            $scope.radioHeatmapLocalization = false;

            $scope.localizationAccMode = false;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('localizationAccMode', 'NO');
            }

            // document.getElementById("localizationAccurancy-mode").classList.add('draggable-border-green');
            // $scope.anyService.radioHeatmapLocalization = false;
            // localStorage.setItem('localizationAccMode', 'YES');
            // $scope.radioHeatmapRSSTimeMode = false;
            return;

        }


        //   $scope.anyService.radioHeatmapLocalization=false;

        $scope.showLocalizationAccHeatmap();
        // $scope.localizationAccMode=false;

        document.getElementById("localizationAccurancy-mode").classList.add('draggable-border-green');
        return;
    };


    $scope.toggleFingerPrintsTime = function () {
        if ($scope.fingerPrintsMode) {
            $scope.fingerPrintsTimeMode = !$scope.fingerPrintsTimeMode;
            if ($scope.fingerPrintsTimeMode) {
                if (typeof(Storage) !== "undefined" && localStorage) {
                    localStorage.setItem('fingerPrintsTimeMode', 'YES');
                }
            } else {
                if (typeof(Storage) !== "undefined" && localStorage && !$scope.radioHeatmapRSSTimeMode) {
                    localStorage.setItem('fingerPrintsTimeMode', 'NO');
                }
            }
            $scope.anyService.fingerPrintsTimeMode = !$scope.anyService.fingerPrintsTimeMode;
        }

        if ($scope.radioHeatmapRSSMode) {
            $scope.radioHeatmapRSSTimeMode = !$scope.radioHeatmapRSSTimeMode;
            if ($scope.radioHeatmapRSSTimeMode) {
                if (typeof(Storage) !== "undefined" && localStorage) {
                    localStorage.setItem('fingerPrintsTimeMode', 'YES');
                }
            } else {
                if (typeof(Storage) !== "undefined" && localStorage && !$scope.fingerPrintsTimeMode) {
                    localStorage.setItem('fingerPrintsTimeMode', 'NO');
                }
            }
            $scope.anyService.radioHeatmapRSSTimeMode = !$scope.anyService.radioHeatmapRSSTimeMode;
        }

        if (!$scope.fingerPrintsTimeMode && $scope.fingerPrintsMode) {
            clearFingerPrints();
            $scope.showFingerPrints();
            document.getElementById("fingerPrints-mode").classList.add('draggable-border-green');
            document.getElementById("fingerPrints-time-mode").classList.remove('draggable-border-green');
        }

        if (!$scope.radioHeatmapRSSTimeMode && $scope.radioHeatmapRSSMode) {
            clearRadioHeatmap();
            $scope.showRadioHeatmapRSS();
            $scope.radioHeatmapRSSMode = true;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('radioHeatmapRSSMode', 'YES');
            }
            $scope.anyService.radioHeatmapRSSMode = true;
            document.getElementById("radioHeatmapRSS-mode").classList.add('draggable-border-green');
            document.getElementById("fingerPrints-time-mode").classList.remove('draggable-border-green');
        }

        if ($scope.radioHeatmapLocalization) { //lsolea01
            clearLocalization();
            $scope.showRadioHeatmapRSS();
            $scope.radioHeatmapRSSMode = true;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('radioHeatmapRSSMode', 'YES');
            }
            $scope.anyService.radioHeatmapRSSMode = true;
            document.getElementById("radioHeatmapRSS-mode").classList.add('draggable-border-green');
            document.getElementById("fingerPrints-time-mode").classList.remove('draggable-border-green');

        }


        if (!$scope.radioHeatmapRSSTimeMode && $scope.radioHeatmapRSSMode) {
            clearRadioHeatmap();
            $scope.showRadioHeatmapRSS();
            $scope.radioHeatmapRSSMode = true;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('radioHeatmapRSSMode', 'YES');
            }
            $scope.anyService.radioHeatmapRSSMode = true;
            document.getElementById("radioHeatmapRSS-mode").classList.add('draggable-border-green');
            document.getElementById("fingerPrints-time-mode").classList.remove('draggable-border-green');
        }

        if ($scope.radioHeatmapRSSTimeMode || $scope.fingerPrintsTimeMode) {
            $scope.getFingerPrintsTime();

        }

    };


    $scope.togglePOIs = function () {

        POIsMap = $scope.anyService.getAllPois();
        var key = Object.keys(POIsMap);
        var check = 0;
        if (!POIsMap.hasOwnProperty(key[check])) {
            _err("No POIs yet.");
            return;
        }

        if (POIsMap[key[check]].marker.getMap() !== null && POIsMap[key[check]].marker.getMap() !== undefined) {

            for (var key in POIsMap) {
                if (POIsMap.hasOwnProperty(key)) {

                    var p = POIsMap[key];
                    if (p && p.marker) {
                        p.marker.setMap(null);
                    }
                }
            }

            $scope.anyService.setAllPois(POIsMap);
            POIsMap = {};
            _POIS_IS_ON = false;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('POIsMode', 'NO');
            }
            return;
        }

        for (var key in POIsMap) {
            if (POIsMap.hasOwnProperty(key)) {

                var p = POIsMap[key];
                if (p && p.marker) {
                    p.marker.setMap(GMapService.gmap);
                }
            }
        }
        $scope.anyService.setAllPois(POIsMap);
        _POIS_IS_ON = true;
        if (typeof(Storage) !== "undefined" && localStorage) {
            localStorage.setItem('POIsMode', 'YES');
        }
        return;
    };


    $scope.toggleConnections = function () {

        connectionsMap = $scope.anyService.getAllConnections();
        var key = Object.keys(connectionsMap);
        var check = 0;
        if (!connectionsMap.hasOwnProperty(key[check])) {
            _err("No edges yet.")
            return;
        }

        if (connectionsMap[key[check]].polyLine !== undefined) {

            if (connectionsMap[key[check]].polyLine.getMap() !== undefined) {
                if (connectionsMap[key[check]].polyLine.getMap() !== null) {

                    for (var key in connectionsMap) {
                        if (connectionsMap.hasOwnProperty(key)) {

                            var con = connectionsMap[key];
                            if (con && con.polyLine) {

                                con.polyLine.setMap(null);
                            }
                        }

                    }

                    $scope.anyService.setAllConnection(connectionsMap);
                    connectionsMap = {};
                    _CONNECTIONS_IS_ON = false;
                    if (typeof(Storage) !== "undefined" && localStorage) {
                        localStorage.setItem('connectionsMode', 'NO');
                    }
                    return;
                }
            }
        }

        $scope.showConnections();
    };


    $scope.getHeatMapButtonText = function () {

        var check = 0;
        return heatMap[check] !== undefined && heatMap[check] !== null ? "Hide WiFi Map" : "Show WiFi Map";

    };

    $scope.getAPsButtonText = function () {
        var check = 0;
        return APmap[check] !== undefined && APmap[check] !== null ? "Hide Estimated Wi-Fi AP Position" : "Show Estimated Wi-Fi AP Position";
    };

    $scope.getFingerPrintsButtonText = function () {
        var check = 0;
        return (fingerPrintsMap[check] !== undefined && fingerPrintsMap[check] !== null) || (heatmap && heatmap.getMap()) ? "Hide FingerPrints" : "Show FingerPrints";
    };

    $scope.getFingerPrintTimeButtonText = function () {

        return $scope.fingerPrintsTimeMode ? "Hide FingerPrints By Time" : "Show FingerPrints By Time";
    };

    $scope.getHeatMapTimeButtonText = function () {

        return $scope.radioHeatmapRSSTimeMode ? "Hide WiFi Map By Time" : "Show WiFi Map By Time";
    };

    $scope.getLocalizationAccurancyText = function () {
        return $scope.localizationAccMode ? "Hide Acces Map" : "Show Acces Map";
    }


    $scope.getradiomapDelete = function () {
        return $scope.localizationAccMode1 ? "Radiomap Delete Successful " : "Show Acces Map";
    }


    $scope.getPOIsButtonText = function () {

        POIsMap = $scope.anyService.getAllPois();
        var key = Object.keys(POIsMap);
        var check = 0;
        if (POIsMap.hasOwnProperty(key[check])) {
            if (POIsMap[key[check]].marker.getMap() !== undefined) {
                if (POIsMap[key[check]].marker.getMap() !== null) {
                    document.getElementById("POIs-mode").classList.add('draggable-border-green');
                    $scope.POIsMode = true;
                    return "Hide POIs";
                }
            }
        }
        document.getElementById("POIs-mode").classList.remove('draggable-border-green');
        $scope.POIsMode = false;
        return "Show POIs";

    };

    $scope.getConnectionsButtonText = function () {
        connectionsMap = $scope.anyService.getAllConnections();
        var key = Object.keys(connectionsMap);
        var check = 0;
        if (connectionsMap.hasOwnProperty(key[check])) {
            if (connectionsMap[key[check]].polyLine !== undefined) {
                if (connectionsMap[key[check]].polyLine.getMap() !== undefined) {
                    if (connectionsMap[key[check]].polyLine.getMap() !== null) {
                        document.getElementById("connections-mode").classList.add('draggable-border-green');
                        $scope.connectionsMode = true;
                        return "Hide Edges";
                    }
                }
            }
        }
        document.getElementById("connections-mode").classList.remove('draggable-border-green');
        $scope.connectionsMode = false;
        return "Show Edges";
        //return (connectionsMap!==undefined && connectionsMap!==null)  ? "Hide Edges" : "Show Edges";
    };

    $('#HMs_1').unbind().click(function () {
        $('#wifiTab').click();
        $('#HMs').click();
        $('#HMsButton').click();
    });

    $('#APs_1').unbind().click(function () {
        $('#wifiTab').click();
        $('#HMs').click();
        $('#APsButton').click();
    });

    $('#FPs_1').unbind().click(function () {
        $('#wifiTab').click();
        $('#FPs').click();
        $('#FPsButton').click();
    });

    $('#DL_1').unbind().click(function () {
        $('#wifiTab').click();
        $('#FPs').click();
        $('#deleteButton').click();
    });

    $('#FPs_2').unbind().click(function () {
        $('#wifiTab').click();
        $('#FPs').click();
        $('#FPsTimeButton').click();

    });

    $('#LA_1').unbind().click(function () {
        $('#wifiTab').click();
        $('#LAs').click();
        $('#LAButton').click();

        //_err("Not available yet. Please check in the next release.");
    });

    $('#POIs_1').unbind().click(function () {
        $('#FPs').click();
        $('#POIsButton').click();
    });

    $('#CNs_1').unbind().click(function () {
        $('#FPs').click();
        $('#connectionsButton').click();
    });


    $scope.getHeatmapModeText = function () {
        return $scope.radioHeatmapRSSMode ? "WiFi Map is online" : "WiFi Map is offline";

    };

    $scope.getAPsModeText = function () {
        return $scope.APsMode ? "Estimated Wi-Fi AP Position is online" : "Estimated Wi-Fi AP Position is offline";

    };

    $scope.getFingerPrintsModeText = function () {
        return $scope.fingerPrintsMode ? "FingerPrints are online" : "FingerPrints are offline";

    };

    $scope.getDeleteFingerPrintsModeText = function () {
        return $scope.deleteFingerPrintsMode ? "ON" : "OFF";

    };

    $scope.getFingerPrintsTimeModeText = function () {
        return $scope.fingerPrintsTimeMode || $scope.radioHeatmapRSSTimeMode ? "ON" : "OFF";

    };

    $scope.getLocalizationAccurancyModeText = function () {
        return $scope.localizationAccMode ? "Localization is online" : "Localization is offline";

    };

    $scope.getPOIsModeText = function () {
        return $scope.POIsMode ? "POIs are online" : "POIs are offline";

    };

    $scope.getConnectionsModeText = function () {
        return $scope.connectionsMode ? "Edges are online" : "Edges are offline";

    };

    $scope.deleteFingerPrints = function () {


        if (_DELETE_FINGERPRINTS_IS_ON) {

            drawingManager.setMap(null);
            $scope.deleteButtonWarning = false;
            document.getElementById("delete-mode").classList.remove('draggable-border-green');
            _DELETE_FINGERPRINTS_IS_ON = false;
            $scope.deleteFingerPrintsMode = false;
            return;

        }

        if (!_FINGERPRINTS_IS_ON && (!heatmap || !heatmap.getMap())) {
            _err("You have to press show fingerPrints button first");
            return;
        }

        $scope.deleteButtonWarning = true;
        $scope.deleteFingerPrintsMode = true;
        _DELETE_FINGERPRINTS_IS_ON = true;
        document.getElementById("delete-mode").classList.add('draggable-border-green');
        drawingManager = new google.maps.drawing.DrawingManager({
            drawingMode: google.maps.drawing.OverlayType.RECTANGLE,
            drawingControl: false,
            rectangleOptions: {
                strokeColor: "#13B3E7",
                fillColor: "#ADD8E6",
                fillOpacity: 0.5
            }


        });

        drawingManager.setMap(GMapService.gmap);

        var bounds;
        var start;
        var end;
        var confirmation;

        google.maps.event.addListener(drawingManager, 'overlaycomplete', function (e) {
            bounds = e.overlay.getBounds();
            start = bounds.getNorthEast();
            end = bounds.getSouthWest();

            var i = fingerPrintsMap.length;

            while (i--) {
                if (fingerPrintsMap[i].getPosition().lat() <= start.lat() && fingerPrintsMap[i].getPosition().lng() <= start.lng() && fingerPrintsMap[i].getPosition().lat() >= end.lat() && fingerPrintsMap[i].getPosition().lng() >= end.lng()) {

                    confirmation = confirm("Confirm:\nAre you sure you want to delete the selected fingerprints?");
                    break;
                }
            }
            i = heatmapFingerprints.length;
            while (i--) {
                if (heatmapFingerprints[i].getPosition().lat() <= start.lat() && heatmapFingerprints[i].getPosition().lng() <= start.lng() && heatmapFingerprints[i].getPosition().lat() >= end.lat() && heatmapFingerprints[i].getPosition().lng() >= end.lng()) {

                    confirmation = confirm("Confirm:\nAre you sure you want to delete the selected fingerprints?");
                    break;
                }
            }

            if (confirmation === undefined) {
                alert("You have to select an area with Fingerprints to delete. ");
                e.overlay.setMap(null);
                drawingManager.setMap(null);
                $scope.deleteButtonWarning = false;
                _DELETE_FINGERPRINTS_IS_ON = false;
                $scope.deleteFingerPrintsMode = false;
                document.getElementById("delete-mode").classList.remove('draggable-border-green');
                return;
            }

            if (confirmation) {

                var b = $scope.anyService.getBuilding();

                var f = $scope.anyService.getFloorNumber();

                var reqObj = $scope.creds;

                if (!$scope.owner_id) {
                    _err("Could not identify user. Please refresh and sign in again.");
                    return;
                }

                reqObj.owner_id = $scope.owner_id;

                if (!b || !b.buid) {
                    _err("No building selected");
                    return;
                }

                reqObj.buid = b.buid;

                reqObj.floor = f;

                reqObj.lat1 = start.lat() + "";

                reqObj.lon1 = start.lng() + "";

                reqObj.lat2 = end.lat() + "";

                reqObj.lon2 = end.lng() + "";

                var promise;

                if ($scope.fingerPrintsTimeMode) {
                    reqObj.timestampX = userTimeData[0] + "";

                    reqObj.timestampY = userTimeData[1] + "";
                    promise = $scope.anyAPI.deleteFingerprintsByTime(reqObj);
                } else {
                    promise = $scope.anyAPI.deleteFingerprints(reqObj);
                }

                var data = [];

                if (!_HEATMAP_F_IS_ON && !_HEATMAP_RSS_IS_ON)
                    _suc("The fingerprints are scheduled to be deleted.");
                else if (_HEATMAP_F_IS_ON && !_HEATMAP_RSS_IS_ON)
                    _suc("The fingerprints are scheduled to be deleted. " +
                        "A new radiomap for fingerprints will be regenerated shortly after.");
                else if (!_HEATMAP_F_IS_ON && _HEATMAP_RSS_IS_ON)
                    _suc("The fingerprints are scheduled to be deleted. " +
                        "A new radiomap for Wi-Fi coverage will be regenerated shortly after.");
                else if (_HEATMAP_F_IS_ON && _HEATMAP_RSS_IS_ON)
                    _suc("The fingerprints are scheduled to be deleted. " +
                        "New radiomaps for fingerprints and Wi-Fi coverage will be regenerated shortly after.");

                promise.then(
                    function (resp) {
                        // on success
                        data = resp.data.radioPoints;

                        console.log("fingerPrints deleted ");

                        // delete the fingerPrints from the loaded FingerPrints
                        if (data.length > 0) {
                            var i = fingerPrintsMap.length;
                            while (i--) {

                                if (fingerPrintsMap[i].getPosition().lat() <= start.lat() && fingerPrintsMap[i].getPosition().lng() <= start.lng() && fingerPrintsMap[i].getPosition().lat() >= end.lat() && fingerPrintsMap[i].getPosition().lng() >= end.lng()) {

                                    // delete the fingerPrints from the loaded FingerPrints
                                    fingerPrintsMap[i].setMap(null);
                                }

                            }

                            if (_HEATMAP_F_IS_ON) {
                                heatmap.setMap(null);
                                var heatMapData = [];
                                i = heatmapFingerprints.length;
                                while (i--) {
                                    if (heatmapFingerprints[i] !== null) {
                                        if (heatmapFingerprints[i].getPosition().lat() > start.lat() || heatmapFingerprints[i].getPosition().lng() > start.lng() || heatmapFingerprints[i].getPosition().lat() < end.lat() || heatmapFingerprints[i].getPosition().lng() < end.lng()) {
                                            heatMapData.push(
                                                {location: heatmapFingerprints[i].getPosition(), weight: 1}
                                            );
                                        } else {
                                            heatmapFingerprints[i] = null;
                                        }
                                    }
                                }
                                heatmap = new google.maps.visualization.HeatmapLayer({
                                    data: heatMapData
                                });
                                heatmap.setMap($scope.gmapService.gmap);
                            }
                            if (_HEATMAP_RSS_IS_ON) {
                                i = heatMap.length;

                                while (i--) {
                                    if (heatMap[i].location.lat() <= start.lat() && heatMap[i].location.lng() <= start.lng() && heatMap[i].location.lat() >= end.lat() && heatMap[i].location.lng() >= end.lng()) {
                                        heatMap[i].rectangle.setMap(null);
                                    }
                                }

                            }

                            if (_HEATMAP_Localization) { //lsolea01
                                i = heatMap_Location.length;

                                while (i--) {
                                    if (heatMap_Location[i].location.lat() <= start.lat() && heatMap_Location[i].location.lng() <= start.lng() && heatMap_Location[i].location.lat() >= end.lat() && heatMap_Location[i].location.lng() >= end.lng()) {
                                        heatMap_Location[i].setMap(null);
                                    }
                                }

                            }
                        }

                        _suc("Successfully deleted " + data.length + " fingerPrints.");
                    },
                    function (resp) {
                        // on error
                        var data = resp.data
                        _err("Something went wrong. It's likely that everything related to the fingerPrints is deleted but please refresh to make sure or try again.");
                        document.getElementById("delete-mode").classList.remove('draggable-border-green');
                    }
                );

            }

            e.overlay.setMap(null);
            drawingManager.setMap(null);
            $scope.deleteButtonWarning = false;
            _DELETE_FINGERPRINTS_IS_ON = false;
            $scope.deleteFingerPrintsMode = false;
            document.getElementById("delete-mode").classList.remove('draggable-border-green');

        });

    };

    $scope.getColorBarTextFor = function (color) {

        return !getColorClicked(color.charAt(0)) ? "click to hide " + color + " ones" : "click to show " + color + " ones";

    };

    function setColorClicked(color, value) {

        if (color === 'g') {
            colorBarGreenClicked = value;
            if (value) {
                document.getElementById("greenSquares").classList.add('faded');
            } else {
                document.getElementById("greenSquares").classList.remove('faded');
            }

        } else if (color === 'y') {
            colorBarYellowClicked = value;
            if (value) {
                document.getElementById("yellowSquares").classList.add('faded');
            } else {
                document.getElementById("yellowSquares").classList.remove('faded');
            }

        } else if (color === 'o') {
            colorBarOrangeClicked = value;
            if (value) {
                document.getElementById("orangeSquares").classList.add('faded');
            } else {
                document.getElementById("orangeSquares").classList.remove('faded');
            }

        } else if (color === 'p') {
            colorBarPurpleClicked = value;
            if (value) {
                document.getElementById("purpleSquares").classList.add('faded');
            } else {
                document.getElementById("purpleSquares").classList.remove('faded');
            }

        } else {
            colorBarRedClicked = value;
            if (value) {
                document.getElementById("redSquares").classList.add('faded');
            } else {
                document.getElementById("redSquares").classList.remove('faded');
            }

        }


    };

    function getColorClicked(color) {

        if (color === 'g')
            return colorBarGreenClicked;
        else if (color === 'y')
            return colorBarYellowClicked;
        else if (color === 'o')
            return colorBarOrangeClicked;
        else if (color === 'p')
            return colorBarPurpleClicked;
        else
            return colorBarRedClicked;
    };

    $scope.hideRSSExcept = function (color) {

        if (color === 'g' && !$scope.radioHeatmapRSSHasGreen) {
            _err("Wi-Fi coverage map has no green squares");
            return;
        }

        if (color === 'y' && !$scope.radioHeatmapRSSHasYellow) {
            _err("Wi-Fi coverage map has no yellow squares");
            return;
        }

        if (color === 'o' && !$scope.radioHeatmapRSSHasOrange) {
            _err("Wi-Fi coverage map has no orange squares");
            return;
        }

        if (color === 'p' && !$scope.radioHeatmapRSSHasPurple) {
            _err("Wi-Fi coverage map has no purple squares");
            return;
        }

        if (color === 'r' && !$scope.radioHeatmapRSSHasRed) {
            _err("Wi-Fi coverage map has no red squares");
            return;
        }
        var i = heatMap.length;
        while (i--) {
            if (getColorClicked(color)) {
                if (heatMap[i].id === color) {
                    heatMap[i].rectangle.setMap($scope.gmapService.gmap);
                    heatMap[i].clicked = true;
                }
            } else {
                if (heatMap[i].id === color) {
                    heatMap[i].rectangle.setMap(null);
                    heatMap[i].clicked = false;
                }

            }
        }
        setColorClicked(color, !getColorClicked(color));

    };

    $scope.showRadioHeatmapRSS = function () {

        var jsonReq;

        var promise;

        _NOW_ZOOM = GMapService.gmap.getZoom();

        if (($scope.radioHeatmapRSSTimeMode || $scope.fingerPrintsTimeMode) && userTimeData.length > 0) {
            jsonReq = {
                "buid": $scope.anyService.getBuildingId(),
                "floor": $scope.anyService.getFloorNumber(),
                "timestampX": userTimeData[0],
                "timestampY": userTimeData[1]
            };

            jsonReq.username = $scope.creds.username;
            jsonReq.password = $scope.creds.password;

            if (_NOW_ZOOM > MIN_ZOOM_FOR_HEATMAPS && _NOW_ZOOM < MAX_ZOOM_FOR_HEATMAPS) {
                levelOfZoom = 2;
                promise = $scope.anyAPI.getRadioHeatmapRSSByTime_2(jsonReq);

            } else if (_NOW_ZOOM > MIN_ZOOM_FOR_HEATMAPS) {
                levelOfZoom = 3;
                promise = $scope.anyAPI.getRadioHeatmapRSSByTime_3(jsonReq);


            } else {
                levelOfZoom = 1;
                promise = $scope.anyAPI.getRadioHeatmapRSSByTime_1(jsonReq);
            }


        } else {

            jsonReq = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber()};

            jsonReq.username = $scope.creds.username;
            jsonReq.password = $scope.creds.password;


            if (_NOW_ZOOM > MIN_ZOOM_FOR_HEATMAPS && _NOW_ZOOM < MAX_ZOOM_FOR_HEATMAPS) {
                levelOfZoom = 2;
                promise = $scope.anyAPI.getRadioHeatmapRSS_2(jsonReq);
            } else if (_NOW_ZOOM > MIN_ZOOM_FOR_HEATMAPS) {
                levelOfZoom = 3;

                promise = $scope.anyAPI.getRadioHeatmapRSS_3(jsonReq);

            } else {
                levelOfZoom = 1;
                promise = $scope.anyAPI.getRadioHeatmapRSS_1(jsonReq);
            }
        }

        if (promise !== undefined) {
            promise.then(
                function (resp) {
                    // on success
                    var data = resp.data;

                    var heatMapData = [];

                    var i = resp.data.radioPoints.length;

                    if (i <= 0) {
                        _err("This floor seems not to be WiFi mapped. Download the Anyplace app from the Google Play store to map the floor.");
                        if (!$scope.radioHeatmapRSSTimeMode) {
                            document.getElementById("radioHeatmapRSS-mode").classList.remove('draggable-border-green');
                            $scope.radioHeatmapRSSMode = false;
                            if (typeof(Storage) !== "undefined" && localStorage && !$scope.fingerPrintsTimeMode) {
                                localStorage.setItem('radioHeatmapRSSMode', 'NO');
                            }
                        } else {
                            _err("No fingerprints at this period.\n Please choose another one.");
                        }
                        return;
                    }

                    var j = 0;

                    while (i--) {

                        var rp = resp.data.radioPoints[i];
                        var rss = JSON.parse(rp.w); //count,average,total
                        //set weight based on RSSI

                        var w = parseFloat(rss.average);

                        if (w <= -30 && w >= -60) {

                            heatMapData.push(
                                {location: new google.maps.LatLng(rp.x, rp.y), weight: w, color: '#4ed419', id: 'g'}
                            );

                            $scope.radioHeatmapRSSHasGreen = true;

                        } else if (w < -60 && w >= -70) {

                            heatMapData.push(
                                {location: new google.maps.LatLng(rp.x, rp.y), weight: w, color: '#ffff00', id: 'y'}
                            );

                            $scope.radioHeatmapRSSHasYellow = true;

                        } else if (w < -70 && w >= -90) {

                            heatMapData.push(
                                {location: new google.maps.LatLng(rp.x, rp.y), weight: w, color: '#ffa500', id: 'o'}
                            );

                            $scope.radioHeatmapRSSHasOrange = true;

                        } else if (w < -90 && w >= -100) {

                            heatMapData.push(
                                {location: new google.maps.LatLng(rp.x, rp.y), weight: w, color: '#bd06bd', id: 'p'}
                            );

                            $scope.radioHeatmapRSSHasPurple = true;

                        } else {
                            heatMapData.push(
                                {location: new google.maps.LatLng(rp.x, rp.y), weight: w, color: '#ff0000', id: 'r'}
                            );

                            $scope.radioHeatmapRSSHasRed = true;

                        }

                        //calculate bounds
                        var center = heatMapData[j].location;
                        var size;
                        if (levelOfZoom == 3) {
                            size = new google.maps.Size(0.75, 0.75);
                        } else if (levelOfZoom == 2) {
                            size = new google.maps.Size(2, 2);
                        } else {
                            size = new google.maps.Size(5, 5);
                        }
                        var n = google.maps.geometry.spherical.computeOffset(center, size.height, 0).lat(),
                            s = google.maps.geometry.spherical.computeOffset(center, size.height, 180).lat(),
                            e = google.maps.geometry.spherical.computeOffset(center, size.width, 90).lng(),
                            w = google.maps.geometry.spherical.computeOffset(center, size.width, 270).lng();

                        var rectangle = new google.maps.Rectangle({
                            strokeColor: heatMapData[j].color,
                            strokeOpacity: 1,
                            strokeWeight: 1,
                            fillColor: heatMapData[j].color,
                            fillOpacity: 0.5,
                            bounds: new google.maps.LatLngBounds(
                                new google.maps.LatLng(s, w),
                                new google.maps.LatLng(n, e))
                        });
                        if (getColorClicked(heatMapData[j].id)) {
                            rectangle.setMap(null);
                        } else {
                            rectangle.setMap($scope.gmapService.gmap);
                        }

                        heatMap.push(
                            {rectangle: rectangle, location: center, id: heatMapData[j].id, clicked: false}
                        );
                        j++;

                        resp.data.radioPoints.splice(i, 1);
                    }
                    _HEATMAP_RSS_IS_ON = true;
                    $cookieStore.put('RSSClicked', 'YES');

                },
                function (resp) {
                    // on error
                    var data = resp.data;
                    _err('Something went wrong while fetching radio heatmap.');
                    if (!$scope.radioHeatmapRSSTimeMode) {
                        $scope.radioHeatmapRSSMode = false;
                        if (typeof(Storage) !== "undefined" && localStorage && !$scope.fingerPrintsTimeMode) {
                            localStorage.setItem('radioHeatmapRSSMode', 'NO');
                        }
                        document.getElementById("radioHeatmapRSS-mode").classList.remove('draggable-border-green');
                    }

                }
            );
        }
    }


    $scope.getAPsIds = function (jsonInfo) {

        var jsonReq = {};
        jsonReq.ids = jsonInfo;

        var promise = $scope.anyAPI.getAPsIds(jsonReq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data.accessPoints;

                var i = data.length;

                if (i <= 0) {
                    _err("Access Points seems to not have ids");
                    return;
                }

                var dataIDs = new Set();

                while (i--) {
                    APmap[i].mun = data[i];
                    dataIDs.add(data[i]);

                }

                dataIDs.forEach(function (element) {
                    if (element !== "N/A") {
                        $scope.example8data.push({id: element, label: element});
                        $scope.example8model.push({id: element, label: element});
                    }
                });


            },
            function (resp) {
                // on error
                var data = resp.data;
                _err('Something went wrong while fetching the ids of access points.');
            }
        );


    };


    $scope.showAPs = function () {
        //request for access points
        var jsonReq = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber()};

        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;

        var i;

        var promise = $scope.anyAPI.getAPs(jsonReq);
        promise.then(
            function (resp) {
                // on success

                i = resp.data.accessPoints.length;

                if (i <= 0) {
                    _err("This floor seems not to be Access Point mapped. Download the Anyplace app from the Google Play store to map the floor.");
                    $scope.APsMode = false;
                    document.getElementById("APs-mode").classList.remove('draggable-border-green');
                    return;
                }


                //algorithm to find the location of each AP
                var values = resp.data.accessPoints;

                i = values.length;


                //create AccessPoint "map"
                var _ACCESS_POINT_IMAGE = 'build/images/access-point-icon.svg';

                var imgType = _ACCESS_POINT_IMAGE;

                var size = new google.maps.Size(21, 32);

                i = values.length;
                var c = 0;
                var x;
                var y;
                var jsonInfo = [];
                while (i--) {
                    //check for limit
                    if (c == MAX) {
                        _err('Access Points have exceeded the maximun limit of 1000');
                        break;
                    }
                    if (values[i].den != 0) {
                        x = values[i].x / values[i].den;
                        y = values[i].y / values[i].den;
                    } else {
                        x = values[i].x;
                        y = values[i].y;

                    }
                    $scope.example9data.push({id: values[i].AP, label: values[i].AP});
                    $scope.example9model.push({id: values[i].AP, label: values[i].AP});

                    //alert("x: "+x+" y: "+y);

                    var accessPoint = new google.maps.Marker({
                        id: values[i].AP,
                        position: new google.maps.LatLng(x, y),
                        map: GMapService.gmap,
                        icon: {
                            url: imgType,
                            scaledSize: size
                        }
                    });

                    APmap.push(
                        accessPoint
                    );

                    jsonInfo.push(accessPoint.id);

                    var infowindow = new google.maps.InfoWindow();
                    if (!infowindow.getMap()) {
                        APmap[c].addListener('click', function () {

                            if (this.mun !== "N/A") {
                                infowindow.setContent(this.id + "<br><center>-</center><br>" + this.mun);
                            } else {
                                infowindow.setContent(this.id);
                            }

                            infowindow.open(this.gmap, this);
                        });
                    }


                    c++;
                }
                $scope.getAPsIds(jsonInfo);
            },
            function (resp) {
                // on error
                var data = resp.data;
                $scope.APsMode = false;
                _err('Something went wrong while fetching Access Points.');
                document.getElementById("APs-mode").classList.remove('draggable-border-green');
            }
        );

    }

    $scope.getFingerPrintsTime = function () {


        var jsonReq = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber()};

        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;

        var promise = $scope.anyAPI.getFingerprintsTime(jsonReq);

        promise.then(
            function (resp) {
                // on success
                var data = resp.data.radioPoints;

                var i = data.length;

                if (i <= 0) {
                    _err("This floor seems not to be FingerPrint mapped. Download the Anyplace app from the Google Play store to map the floor.");
                    return;
                }

                plotGraphs(data);

                initializeTimeFunction();
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err('Something went wrong while fetching fingerPrints timestamp.');
            }
        );


    };


    $scope.showFingerPrints = function () {

        var jsonReq;
        var promise;

        if (($scope.fingerPrintsTimeMode || $scope.radioHeatmapRSSTimeMode) && userTimeData.length > 0) {
            jsonReq = {
                "buid": $scope.anyService.getBuildingId(),
                "floor": $scope.anyService.getFloorNumber(),
                "timestampX": userTimeData[0],
                "timestampY": userTimeData[1]
            };

            jsonReq.username = $scope.creds.username;
            jsonReq.password = $scope.creds.password;

            if (_NOW_ZOOM !== _MAX_ZOOM_LEVEL) {


                promise = $scope.anyAPI.getRadioHeatmapRSSByTime(jsonReq);
            } else {

                var layerID = 'my_custom_layer';
                var layer = new google.maps.ImageMapType({
                    name: layerID,
                    getTileUrl: function (coord, zoom) {
                        if (_NOW_ZOOM !== _MAX_ZOOM_LEVEL || !$scope.fingerPrintsTimeMode)
                            return null;
                        var jsonReqTiles = {
                            "buid": $scope.anyService.getBuildingId(),
                            "floor": $scope.anyService.getFloorNumber(),
                            "timestampX": userTimeData[0],
                            "timestampY": userTimeData[1]
                        };

                        jsonReqTiles.username = $scope.creds.username;
                        jsonReqTiles.password = $scope.creds.password;
                        jsonReqTiles.x = coord.x;
                        jsonReqTiles.y = coord.y;
                        jsonReqTiles.z = zoom;
                        var tilePromise = $scope.anyAPI.getRadioHeatmapRSSByTime_Tiles(jsonReqTiles);

                        tilePromise.then(
                            function (resp) {

                                // on success
                                var data = resp.data;

                                var fingerPrintsData = [];

                                var i = resp.data.radioPoints.length;


                                while (i--) {
                                    var rp = resp.data.radioPoints[i];

                                    fingerPrintsData.push(
                                        {location: new google.maps.LatLng(rp.x, rp.y)}
                                    );
                                    resp.data.radioPoints.splice(i, 1);
                                }

                                //create fringerPrint "map"
                                var _FINGERPRINT_IMAGE = 'build/images/fingerPrint-icon.png';

                                var imgType = _FINGERPRINT_IMAGE;

                                var size = new google.maps.Size(21, 32);

                                i = fingerPrintsData.length;


                                while (i--) {
                                    if (_NOW_ZOOM === _MAX_ZOOM_LEVEL) {
                                        var fingerPrint = new google.maps.Marker({
                                            position: fingerPrintsData[i].location,
                                            map: GMapService.gmap,
                                            icon: new google.maps.MarkerImage(
                                                imgType,
                                                null, /* size is determined at runtime */
                                                null, /* origin is 0,0 */
                                                null, /* anchor is bottom center of the scaled image */
                                                size
                                            )
                                        });
                                        fingerPrintsMap.push(
                                            fingerPrint
                                        );
                                    }

                                }
                                _FINGERPRINTS_IS_ON = true;


                            },
                            function (resp) {
                                // on error
                                var data = resp.data;
                                _err('Something went wrong while fetching fingerPrints.');
                                if (!$scope.fingerPrintsMode) {
                                    document.getElementById("fingerPrints-mode").classList.remove('draggable-border-green');
                                    $scope.fingerPrintsMode = false;
                                    if (typeof(Storage) !== "undefined" && localStorage) {
                                        localStorage.setItem('fingerprintsMode', 'NO');
                                    }
                                }
                            }
                        );

                        return null;
                    },
                    tileSize: new google.maps.Size(256, 256),
                    minZoom: 1,
                    maxZoom: 22
                });

                GMapService.gmap.mapTypes.set(layerID, layer);
                GMapService.gmap.setMapTypeId(layerID);
            }

        } else {

            jsonReq = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber()};

            jsonReq.username = $scope.creds.username;
            jsonReq.password = $scope.creds.password;


            if (_NOW_ZOOM !== _MAX_ZOOM_LEVEL) {
                promise = $scope.anyAPI.getRadioHeatmapRSS_3(jsonReq);
            } else {

                var layerID = 'my_custom_layer1';
                var layer = new google.maps.ImageMapType({
                    name: layerID,
                    getTileUrl: function (coord, zoom) {
                        if (_NOW_ZOOM !== _MAX_ZOOM_LEVEL || !$scope.fingerPrintsMode)
                            return null;
                        var jsonReqTiles = {
                            "buid": $scope.anyService.getBuildingId(),
                            "floor": $scope.anyService.getFloorNumber()
                        };

                        jsonReqTiles.username = $scope.creds.username;
                        jsonReqTiles.password = $scope.creds.password;
                        jsonReqTiles.x = coord.x;
                        jsonReqTiles.y = coord.y;
                        jsonReqTiles.z = zoom;
                        var tilePromise = $scope.anyAPI.getRadioHeatmapRSS_3_Tiles(jsonReqTiles);

                        tilePromise.then(
                            function (resp) {

                                // on success
                                var data = resp.data;

                                var fingerPrintsData = [];

                                var i = resp.data.radioPoints.length;


                                while (i--) {
                                    var rp = resp.data.radioPoints[i];

                                    fingerPrintsData.push(
                                        {location: new google.maps.LatLng(rp.x, rp.y)}
                                    );
                                    resp.data.radioPoints.splice(i, 1);
                                }

                                //create fringerPrint "map"
                                var _FINGERPRINT_IMAGE = 'build/images/fingerPrint-icon.png';

                                var imgType = _FINGERPRINT_IMAGE;

                                var size = new google.maps.Size(21, 32);

                                i = fingerPrintsData.length;


                                while (i--) {
                                    if (_NOW_ZOOM === _MAX_ZOOM_LEVEL) {
                                        var fingerPrint = new google.maps.Marker({
                                            position: fingerPrintsData[i].location,
                                            map: GMapService.gmap,
                                            icon: new google.maps.MarkerImage(
                                                imgType,
                                                null, /* size is determined at runtime */
                                                null, /* origin is 0,0 */
                                                null, /* anchor is bottom center of the scaled image */
                                                size
                                            )
                                        });
                                        fingerPrintsMap.push(
                                            fingerPrint
                                        );
                                    }

                                }
                                _FINGERPRINTS_IS_ON = true;


                            },
                            function (resp) {
                                // on error
                                var data = resp.data;
                                _err('Something went wrong while fetching fingerPrints.');
                                if (!$scope.fingerPrintsMode) {
                                    document.getElementById("fingerPrints-mode").classList.remove('draggable-border-green');
                                    $scope.fingerPrintsMode = false;
                                    if (typeof(Storage) !== "undefined" && localStorage) {
                                        localStorage.setItem('fingerprintsMode', 'NO');
                                    }
                                }
                            }
                        );

                        return null;
                    },
                    tileSize: new google.maps.Size(256, 256),
                    minZoom: 1,
                    maxZoom: 22
                });

                GMapService.gmap.mapTypes.set(layerID, layer);
                GMapService.gmap.setMapTypeId(layerID);

            }

        }

        if (promise !== undefined) {
            promise.then(
                function (resp) {

                    // on success
                    var data = resp.data;

                    var fingerPrintsData = [];

                    var i = resp.data.radioPoints.length;

                    if (i <= 0) {
                        if (!$scope.fingerPrintsTimeMode) {
                            _err("This floor seems not to be FingerPrint mapped. Download the Anyplace app from the Google Play store to map the floor.");
                            document.getElementById("fingerPrints-mode").classList.remove('draggable-border-green');
                            $scope.fingerPrintsMode = false;
                            if (typeof(Storage) !== "undefined" && localStorage) {
                                localStorage.setItem('fingerprintsMode', 'NO');
                            }

                        } else {
                            _err("No fingerprints at this period.\n Please choose another one.");
                        }
                        return;
                    }
                    if (_NOW_ZOOM == _MAX_ZOOM_LEVEL) {
                        while (i--) {
                            var rp = resp.data.radioPoints[i];

                            fingerPrintsData.push(
                                {location: new google.maps.LatLng(rp.x, rp.y)}
                            );
                            resp.data.radioPoints.splice(i, 1);
                        }

                        //create fringerPrint "map"
                        var _FINGERPRINT_IMAGE = 'build/images/fingerPrint-icon.png';

                        var imgType = _FINGERPRINT_IMAGE;

                        var size = new google.maps.Size(21, 32);

                        i = fingerPrintsData.length;


                        while (i--) {

                            var fingerPrint = new google.maps.Marker({
                                position: fingerPrintsData[i].location,
                                map: GMapService.gmap,
                                icon: new google.maps.MarkerImage(
                                    imgType,
                                    null, /* size is determined at runtime */
                                    null, /* origin is 0,0 */
                                    null, /* anchor is bottom center of the scaled image */
                                    size
                                )
                            });
                            fingerPrintsMap.push(
                                fingerPrint
                            );

                        }
                        _FINGERPRINTS_IS_ON = true;
                    } else {


                        var heatMapData = [];
                        var c = 0;


                        while (i--) {
                            var rp = resp.data.radioPoints[i];
                            var rss = JSON.parse(rp.w); //count,average,total
                            var we = 100;


                            var removeCoords = [
                                {lat: 35.14421, lng: 33.41123},
                                {lat: 35.14408, lng: 33.41134},
                                {lat: 35.14433, lng: 33.41167},
                                {lat: 35.14445, lng: 33.41157},
                                {lat: 35.14421, lng: 33.41123}
                            ];


                            //here Construct the polygon.
                            var polygon = new google.maps.Polygon({
                                paths: removeCoords,
                                strokeColor: '#FF0000',
                                strokeOpacity: 0.8,
                                strokeWeight: 2,
                                fillColor: '#FF0000',
                                fillOpacity: 0.35
                            });


                            var coordinate = new google.maps.LatLng(rp.x, rp.y);

                            //
                            // if (google.maps.geometry.poly.containsLocation(coordinate, polygon)) {
                            //     we = 1;
                            // }


                            heatMapData.push(
                                {location: new google.maps.LatLng(rp.x, rp.y), weight: we}
                            );
                            var fingerPrint = new google.maps.Marker({
                                position: heatMapData[c].location,
                            });
                            heatmapFingerprints.push(
                                fingerPrint
                            );


                            resp.data.radioPoints.splice(i, 1);
                            c++;
                        }


                        heatmap = new google.maps.visualization.HeatmapLayer({
                            data: heatMapData
                        });
                        heatmap.setMap($scope.gmapService.gmap);

                        _HEATMAP_F_IS_ON = true;

                    }


                },
                function (resp) {
                    // on error
                    var data = resp.data;
                    _err('Something went wrong while fetching fingerPrints.');
                    if (!$scope.fingerPrintsMode) {
                        document.getElementById("fingerPrints-mode").classList.remove('draggable-border-green');
                        $scope.fingerPrintsMode = false;
                        if (typeof(Storage) !== "undefined" && localStorage) {
                            localStorage.setItem('fingerprintsMode', 'NO');
                        }
                    }
                }
            );
        }
    };

    $scope.showLocalizationAccHeatmap = function () {
        var jsonReq = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber()};

        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;

        var promise = $scope.anyAPI.getHeatmapAcces(jsonReq);


        //zoom
        _NOW_ZOOM = GMapService.gmap.getZoom();


        if (_NOW_ZOOM > MIN_ZOOM_FOR_HEATMAPS && _NOW_ZOOM < MAX_ZOOM_FOR_HEATMAPS) {
            levelOfZoom = 2;


        }
        else if (_NOW_ZOOM > MIN_ZOOM_FOR_HEATMAPS) {
            levelOfZoom = 3;


        }
        else {
            levelOfZoom = 1;

        }


        promise.then(
            function (resp) {

                var values = resp.data.crlb;

                var k = values.length;

                var data = resp.data.geojson.coordinates;

                var i = data.length;


                if (i <= 0) {
                    _err("This floor seems not to be FingerPrint mapped. Download the Anyplace app from the Google Play store to map the floor.");
                    document.getElementById("localizationAccurancy-mode").classList.remove('draggable-border-green');
                    $scope.localizationAccMode = false;
                    if (typeof(Storage) !== "undefined" && localStorage) {
                        localStorage.setItem('localizationAccMode', 'NO');
                    }
                    return;
                }

                var heatMapData = [];


                var j = 0;


                while (i--) {
                    var rp = data[i];


                    var color = '#ffffff';
                    if (isNaN(values[i]) || values[i] < 0) {
                        color = '#000000';

                    }
                    else if (values[i] > 0 && values[i] < 5) {
                        color = '#33cc33';

                    }
                    else if (values[i] > 5 && values[i] < 10) {
                        color = '#99ff33';

                    }

                    else if (values[i] > 10 && values[i] < 15) {
                        color = '#fdff76';


                    }
                    else if (values[i] > 15 && values[i] < 30) {
                        color = '#ffd716';

                    }
                    else if (values[i] > 30 && values[i] < 60) {
                        color = '#ff8a1b';

                    }
                    else {
                        color = '#ff391e';


                    }



                    //cs bounding
                    // var removeCoords = [
                    //     {lat: 35.14465, lng: 33.41081},
                    //     {lat: 35.14489, lng: 33.41115},
                    //     {lat: 35.14432, lng: 33.41168},
                    //     {lat: 35.14410, lng: 33.41136},
                    //     {lat: 35.14465, lng: 33.41081}
                    // ];



                    //mall bounding

                    // var removeCoords = [
                    //     {lat: 35.13052, lng: 33.37040},
                    //     {lat: 35.13020, lng: 33.37246},
                    //     {lat: 35.12936, lng: 33.37228},
                    //     {lat: 35.12969, lng: 33.37020},
                    //     {lat: 35.13052, lng: 33.37040}
                    // ];
                    //
                    //
                    //
                    // var polygon = new google.maps.Polygon({
                    //     paths: removeCoords,
                    //     strokeColor: '#FF0000',
                    //     strokeOpacity: 0.8,
                    //     strokeWeight: 2,
                    //     fillColor: '#FF0000',
                    //     fillOpacity: 0.35
                    // });
                    //
                    //
                    // var coordinate = new google.maps.LatLng(rp[0], rp[1]);
                    //
                    var ra=1.5;
                    // if (!google.maps.geometry.poly.containsLocation(coordinate, polygon)) {
                    //     ra= 0;
                    // }

                    //here
                    var circle = new google.maps.Circle({
                        //  path: google.maps.SymbolPath.CIRCLE,
                        strokeWeight: 0.5,
                        strokeColor: '#ffffff',
                        fillColor: color,
                        fillOpacity: 0.5,
                        map: $scope.gmapService.gmap,
                        center: {lat: rp[0], lng: rp[1]},
                        radius: ra
                    });

                    heatMap_Location.push(circle);

                    data.splice(i, 1);


                    j++;

                }


                document.getElementById("localizationAccurancy-mode").classList.add('draggable-border-green');
                $scope.localizationAccMode = true;
                if (typeof(Storage) !== "undefined" && localStorage) {
                    localStorage.setItem('localizationAccMode', 'NO');
                }
                else {
                    localStorage.setItem('localizationAccMode', 'YES');
                }


                _HEATMAP_Localization = true;
                $scope.radioHeatmapLocalization = true;

            }, function (resp) {
                // on error
                var data = resp.data;
                _err('Something went wrong while fetching fingerPrints.');
                document.getElementById("localizationAccurancy-mode").classList.remove('draggable-border-green');
                $scope.localizationAccMode = false;
                if (typeof(Storage) !== "undefined" && localStorage) {
                    localStorage.setItem('localizationAccMode', 'NO');
                }
            }
        );


    };

    $scope.showConnections = function () {

        for (var key in connectionsMap) {
            if (connectionsMap.hasOwnProperty(key)) {
                var con = connectionsMap[key];
                if (con && con.polyLine) {
                    con.polyLine.setMap(GMapService.gmap);
                }
            }
        }

        _CONNECTIONS_IS_ON = true;
        $scope._CONNECTIONS_IS_ON = true;
        if (typeof(Storage) !== "undefined" && localStorage) {
            localStorage.setItem('connectionsMode', 'YES');
        }
        $scope.anyService.setAllConnection(connectionsMap);

    };

    //zoom handler for clustering

    GMapService.gmap.addListener('zoom_changed', function () {
        _NOW_ZOOM = GMapService.gmap.getZoom();

        if (_NOW_ZOOM !== _MAX_ZOOM_LEVEL) {

            var i = fingerPrintsMap.length;

            //hide fingerPrints
            while (i--) {
                fingerPrintsMap[i].setMap(null);
                fingerPrintsMap[i] = null;
            }
            fingerPrintsMap = [];
        }

        if (_HEATMAP_RSS_IS_ON) {
            if ((_PREV_ZOOM == MIN_ZOOM_FOR_HEATMAPS && _NOW_ZOOM > _PREV_ZOOM) || (_PREV_ZOOM > MIN_ZOOM_FOR_HEATMAPS && _PREV_ZOOM < MAX_ZOOM_FOR_HEATMAPS && (_NOW_ZOOM <= MIN_ZOOM_FOR_HEATMAPS || _NOW_ZOOM >= MAX_ZOOM_FOR_HEATMAPS)) || (_PREV_ZOOM == MAX_ZOOM_FOR_HEATMAPS && _NOW_ZOOM < _PREV_ZOOM)) {
                var i = heatMap.length;
                while (i--) {
                    heatMap[i].rectangle.setMap(null);
                    heatMap[i] = null;
                }
                heatMap = [];

                $scope.showRadioHeatmapRSS();
            }
        }

        if (_HEATMAP_Localization) {
            // zoom in
            if ((_PREV_ZOOM == MIN_ZOOM_FOR_HEATMAPS && _NOW_ZOOM > _PREV_ZOOM) ||

                (_PREV_ZOOM > MIN_ZOOM_FOR_HEATMAPS && _PREV_ZOOM < MAX_ZOOM_FOR_HEATMAPS &&
                    (_NOW_ZOOM <= MIN_ZOOM_FOR_HEATMAPS || _NOW_ZOOM >= MAX_ZOOM_FOR_HEATMAPS)) ||

                (_PREV_ZOOM == MAX_ZOOM_FOR_HEATMAPS && _NOW_ZOOM < _PREV_ZOOM)) {
                var i = heatMap_Location.length;
                while (i--) {
                    heatMap_Location[i].setMap(null);
                    heatMap_Location[i] = null;
                }
                heatMap_Location = [];

                $scope.showLocalizationAccHeatmap();
            }
        }
        if ((_FINGERPRINTS_IS_ON || (heatmap && heatmap.getMap())) && !changedfloor) {
            if (_NOW_ZOOM == _MAX_ZOOM_LEVEL || _PREV_ZOOM == _MAX_ZOOM_LEVEL) {
                var i = fingerPrintsMap.length;
                while (i--) {
                    fingerPrintsMap[i].setMap(null);
                    fingerPrintsMap[i] = null;
                }
                fingerPrintsMap = [];

                if (heatmap && heatmap.getMap()) {
                    heatmap.setMap(null);
                    var i = heatmapFingerprints.length;
                    while (i--) {
                        heatmapFingerprints[i] = null;
                    }
                    heatmapFingerprints = [];
                    _HEATMAP_F_IS_ON = false;
                }

                $scope.showFingerPrints();
            }
        }
        _PREV_ZOOM = _NOW_ZOOM;
    });


    $scope.selectFilterForAPs = function () {

        var option = $scope.selected;
        switch (option) {
            case '0': {
                $scope.filterByMAC = true;
                $scope.filterByMAN = false;
                break;
            }
            default: {
                $scope.filterByMAC = false;
                $scope.filterByMAN = true;
                break;
            }

        }

    };
    $scope.multiuserevents = {

        onItemDeselect: function (item) {
            var i = APmap.length;
            while (i--) {
                if (APmap[i].id == item.id) {
                    APmap[i].setVisible(false);
                    break;
                }
            }
        },
        onItemSelect: function (item) {
            var i = APmap.length;
            while (i--) {
                if (APmap[i].id == item.id) {
                    APmap[i].setVisible(true);
                    break;
                }
            }
        },
        onDeselectAll: function () {
            var i = APmap.length;
            while (i--) {
                APmap[i].setVisible(false);
            }
        }

    };

    $scope.multiuserevents1 = {

        onItemDeselect: function (item) {
            var i = APmap.length;
            while (i--) {
                if (APmap[i].mun == item.id) {
                    APmap[i].setVisible(false);
                    break;
                }
            }
        },
        onItemSelect: function (item) {
            var i = APmap.length;
            while (i--) {
                if (APmap[i].mun == item.id) {
                    APmap[i].setVisible(true);
                    break;
                }
            }
        },
        onDeselectAll: function () {
            var i = APmap.length;
            while (i--) {
                APmap[i].setVisible(false);
            }
        }

    };

    function plotGraphs(timestamps) {
        // Various formatters.
        var formatNumber = d3.format(",d");

        // A little coercion, since the CSV is untyped.
        timestamps.forEach(function (d, i) {
            d.index = i;
            d.date = parseDate(d.date);

        });

        // Create the crossfilter for the relevant dimensions and groups.
        var timestamp = crossfilter(timestamps),
            all = timestamp.groupAll(),
            date = timestamp.dimension(function (d) {
                return d.date;
            }),
            dates = date.group(d3.time.day).reduceSum(function (d) {
                return d.count;
            });

        var dateToString = timestamps[timestamps.length - 1].date.toString();
        var endDate = new Date(dateToString);
        endDate.setDate(endDate.getDate() + 15);

        var charts = [

            barChart()
                .dimension(date)
                .group(dates)
                .round(d3.time.day.round)
                .x(d3.time.scale()
                    .domain([timestamps[0].date, endDate])
                    .rangeRound([0, 10 * 100])) //90
                .filter([timestamps[0].date, endDate])

        ];

        // Given our array of charts, which we assume are in the same order as the
        // .chart elements in the DOM, bind the charts to the DOM and render them.
        // We also listen to the chart's brush events to update the display.
        var chart = d3.selectAll(".chart")
            .data(charts)
            .each(function (chart) {
                chart.on("brush", renderAll).on("brushend", renderAll);
            });


        // Render the total.
        d3.selectAll("#total")
            .text(formatNumber(timestamp.size()));

        renderAll();

        // Renders the specified chart or list.
        function render(method) {
            d3.select(this).call(method);
        }

        // Whenever the brush moves, re-rendering everything.
        function renderAll() {
            chart.each(render);
            d3.select("#active").text(formatNumber(all.value()));
        }

        //parse to date
        function parseDate(d) {

            return new Date((d / 1000) * 1000);

        }

        window.filter = function (filters) {
            filters.forEach(function (d, i) {
                charts[i].filter(d);
            });
            renderAll();
        };

        window.reset = function (i) {
            charts[i].filter(null);
            renderAll();
        };

        function barChart() {
            if (!barChart.id) barChart.id = 0;

            var margin = {top: 10, right: 10, bottom: 20, left: 10},
                x,
                y = d3.scale.linear().range([100, 0]),
                id = barChart.id++,
                axis = d3.svg.axis().orient("bottom"),
                brush = d3.svg.brush(),
                brushDirty,
                dimension,
                group,
                round;


            function chart(div) {
                var width = x.range()[1],
                    height = y.range()[0];

                y.domain([0, group.top(1)[0].value]);

                div.each(function () {
                    var div = d3.select(this),
                        g = div.select("g");

                    // Create the skeletal chart.
                    if (g.empty()) {
                        div.select(".title").append("a")
                            .attr("href", "javascript:reset(" + id + ")")
                            .attr("class", "reset")
                            .text("reset")
                            .style("display", "none");

                        g = div.append("svg")
                            .attr("width", width + margin.left + margin.right)
                            .attr("height", height + margin.top + margin.bottom)
                            .append("g")
                            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

                        g.append("clipPath")
                            .attr("id", "clip-" + id)
                            .append("rect")
                            .attr("width", width)
                            .attr("height", height);

                        g.selectAll(".bar")
                            .data(["background", "foreground"])
                            .enter().append("path")
                            .attr("class", function (d) {
                                return d + " bar";
                            })
                            .datum(group.all());

                        g.selectAll(".foreground.bar")
                            .attr("clip-path", "url(#clip-" + id + ")");

                        g.append("g")
                            .attr("class", "axis")
                            .attr("transform", "translate(0," + height + ")")
                            .call(axis);

                        // Initialize the brush component with pretty resize handles.
                        var gBrush = g.append("g").attr("class", "brush").call(brush);
                        gBrush.selectAll("rect").attr("height", height);
                        gBrush.selectAll(".resize").append("path").attr("d", resizePath);
                    }

                    // Only redraw the brush if set externally.
                    if (brushDirty) {
                        brushDirty = false;
                        g.selectAll(".brush").call(brush);
                        div.select(".title a").style("display", brush.empty() ? "none" : null);
                        if (brush.empty()) {
                            g.selectAll("#clip-" + id + " rect")
                                .attr("x", 0)
                                .attr("width", width);
                        } else {
                            var extent = brush.extent();
                            g.selectAll("#clip-" + id + " rect")
                                .attr("x", x(extent[0]))
                                .attr("width", x(extent[1]) - x(extent[0]));
                        }
                    }

                    g.selectAll(".bar").attr("d", barPath);

                });

                function barPath(groups) {
                    var path = [],
                        i = -1,
                        n = groups.length,
                        d;
                    while (++i < n) {
                        d = groups[i];
                        path.push("M", x(d.key), ",", height, "V", y(d.value), "h9V", height);
                    }
                    return path.join("");
                }

                function resizePath(d) {
                    var e = +(d == "e"),
                        x = e ? 1 : -1,
                        y = height / 3;
                    return "M" + (.5 * x) + "," + y
                        + "A6,6 0 0 " + e + " " + (6.5 * x) + "," + (y + 6)
                        + "V" + (2 * y - 6)
                        + "A6,6 0 0 " + e + " " + (.5 * x) + "," + (2 * y)
                        + "Z"
                        + "M" + (2.5 * x) + "," + (y + 8)
                        + "V" + (2 * y - 8)
                        + "M" + (4.5 * x) + "," + (y + 8)
                        + "V" + (2 * y - 8);
                }
            }


            brush.on("brushstart.chart", function () {
                var div = d3.select(this.parentNode.parentNode.parentNode);
                div.select(".title a").style("display", null);
            });

            brush.on("brush.chart", function () {
                var g = d3.select(this.parentNode),
                    extent = brush.extent();
                if (round) g.select(".brush")
                    .call(brush.on('brushend', bindSelect))
                    .selectAll(".resize")
                    .style("display", null);
                g.select("#clip-" + id + " rect")
                    .attr("x", x(extent[0]))
                    .attr("width", x(extent[1]) - x(extent[0]));
                dimension.filterRange(extent);


                //handler for user's selection
                function bindSelect() {

                    initializeTimeFunction();

                    extent = brush.extent(); //data of selection

                    userTimeData = [];

                    extent.forEach(function (element) {
                        var t = String(element.getTime() / 1000 * 1000);
                        userTimeData.push(t);
                    });

                    if ($scope.fingerPrintsMode) {
                        clearFingerPrints();

                        $scope.showFingerPrints();

                        document.getElementById("fingerPrints-mode").classList.add('draggable-border-green');

                    }

                    if ($scope.radioHeatmapRSSMode) {
                        clearRadioHeatmap();

                        $scope.showRadioHeatmapRSS();
                        $scope.radioHeatmapRSSMode = true;
                        if (typeof(Storage) !== "undefined" && localStorage) {
                            localStorage.setItem('radioHeatmapRSSMode', 'YES');
                        }
                        $scope.anyService.radioHeatmapRSSMode = true;
                        document.getElementById("radioHeatmapRSS-mode").classList.add('draggable-border-green');

                    }

                    if ($scope.radioHeatmapLocalization) {
                        clearLocalization();

                        $scope.showRadioHeatmap();
                        $scope.radioHeatmapLocalization = true;
                        if (typeof(Storage) !== "undefined" && localStorage) {
                            localStorage.setItem('radioHeatmapLocalization', 'YES');
                        }
                        $scope.anyService.radioHeatmapLocalization = true;
                        document.getElementById("radioHeatmapRSS-mode").classList.add('draggable-border-green');

                    }

                }
            });

            brush.on("brushend.chart", function () {
                if (brush.empty()) {
                    var div = d3.select(this.parentNode.parentNode.parentNode);
                    div.select(".title a").style("display", "none");
                    div.select("#clip-" + id + " rect").attr("x", null).attr("width", "100%");
                    dimension.filterAll();
                }
            });

            chart.margin = function (_) {
                if (!arguments.length) return margin;
                margin = _;
                return chart;
            };

            chart.x = function (_) {
                if (!arguments.length) return x;
                x = _;
                axis.scale(x);
                brush.x(x);
                return chart;
            };

            chart.y = function (_) {
                if (!arguments.length) return y;
                y = _;
                return chart;
            };

            chart.dimension = function (_) {
                if (!arguments.length) return dimension;
                dimension = _;
                return chart;
            };

            chart.filter = function (_) {
                if (_) {
                    brush.extent(_);
                    dimension.filterRange(_);
                } else {
                    brush.clear();
                    dimension.filterAll();
                }
                brushDirty = true;
                return chart;
            };

            chart.group = function (_) {
                if (!arguments.length) return group;
                group = _;
                return chart;
            };

            chart.round = function (_) {
                if (!arguments.length) return round;
                round = _;
                return chart;
            };

            return d3.rebind(chart, brush, "on");
        }

    }

    //set localstorage


}]);

/**
 *
 The MIT License (MIT)

 Copyright (c) 2015, Kyriakos Georgiou, Marileni Angelidou, Data Management Systems Laboratory (DMSu
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



var changedfloor = false;

app.controller('FloorController', ['$scope', 'AnyplaceService', 'GMapService', 'AnyplaceAPIService', function ($scope, AnyplaceService, GMapService, AnyplaceAPIService) {
    $scope.anyService = AnyplaceService;
    $scope.anyAPI = AnyplaceAPIService;
    $scope.gmapService = GMapService;
    //$scope.controlBarService = ControlBarController;
    $scope.xFloors = [];

    $scope.myFloors = {};
    $scope.myFloorId = 0;

    $scope.newFloorNumber = 0;
    var heatmap;
    var shipselect=0;

    $scope.crudTabSelected = 1;
    $scope.setCrudTabSelected = function (n) {
        $scope.crudTabSelected = n;
    };
    $scope.isCrudTabSelected = function (n) {

        return $scope.crudTabSelected === n;
    };


    $scope.data = {
        floor_plan_coords: {},
        floor_plan_base64_data: {},
        floor_plan_groundOverlay: null
    };

    $scope.$on("loggedOff", function (event, mass) {
        _clearFloors();
    });

    var _clearFloors = function () {
        $scope.removeFloorPlan();
        $scope.xFloors = [];
        $scope.myFloorId = 0;
        $scope.myFloors = {};
    };

    var _latLngFromPoi = function (p) {
        if (p && p.coordinates_lat && p.coordinates_lon) {
            return {lat: parseFloat(p.coordinates_lat), lng: parseFloat(p.coordinates_lon)}
        }
        return undefined;
    };

    $scope.$watch('anyService.selectedBuilding', function (newVal, oldVal) {
        if (newVal) {
            changedfloor = false;

            $scope.fetchAllFloorsForBuilding(newVal);
        }
    });

    /**
     $scope.$watch('anyService.selectedPoi', function (newVal, oldVal) {
        if (newVal && _latLngFromPoi(newVal)) {
            $scope.showRadioHeatmapPoi();
        }
    });
     */
    $scope.$watch('newFloorNumber', function (newVal, oldVal) {
        //if (_floorNoExists(newVal)) {
        //    _setNextFloor();
        //}
    });

    var _err = function (msg) {
        $scope.anyService.addAlert('danger', msg);
    };

    var _suc = function (msg) {
        $scope.anyService.addAlert('success', msg);
    };

    var _latLngFromBuilding = function (b) {
        if (b && b.coordinates_lat && b.coordinates_lon) {
            return {
                lat: parseFloat(b.coordinates_lat),
                lng: parseFloat(b.coordinates_lon)
            }
        }
        return undefined;
    };

    $scope.$watch('anyService.selectedFloor', function (newVal, oldVal) {
        if (newVal !== undefined && newVal !== null && !_.isEqual(newVal, oldVal)) {
            $scope.fetchFloorPlanOverlay(newVal);
            GMapService.gmap.panTo(_latLngFromBuilding($scope.anyService.selectedBuilding));
            GMapService.gmap.setZoom(19);

            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem("lastFloor", newVal.floor_number);
            }

            changedfloor = false;
        }

    });

    $scope.fetchAllFloorsForBuilding = function (b) {
        // TODO: check for b.buid
        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = b.buid;

        var promise = AnyplaceAPIService.allBuildingFloors(jsonReq);
        promise.then(
            function (resp) {

                $scope.xFloors = resp.data.floors;

                $scope.xFloors = $scope.xFloors.sort(function (a, b) {
                    return parseInt(a.floor_number) - parseInt(b.floor_number)
                });


                $scope.anyService.availableFloors = [];
                $scope.anyService.availableFloors = $scope.xFloors;

                // give priority to floor in url parameter - if exists
                if ($scope.urlFloor) {
                    for (var k = 0; k < $scope.xFloors.length; k++) {
                        if ($scope.urlFloor == $scope.xFloors[k].floor_number) {
                            $scope.anyService.selectedFloor = $scope.xFloors[k];
                            return;
                        }
                    }
                }

                // Set default selected
                if (typeof(Storage) !== "undefined" && localStorage && !LPUtils.isNullOrUndefined(localStorage.getItem('lastBuilding')) && !LPUtils.isNullOrUndefined(localStorage.getItem('lastFloor'))) {
                    for (var i = 0; i < $scope.xFloors.length; i++) {
                        if (String($scope.xFloors[i].floor_number) === String(localStorage.getItem('lastFloor'))) {
                            $scope.anyService.selectedFloor = $scope.xFloors[i];
                            return;
                        }
                    }
                }

                // Set default the first floor if selected floor
                if ($scope.xFloors && $scope.xFloors.length > 0) {
                    $scope.anyService.selectedFloor = $scope.xFloors[0];
                } else {
                    $scope.anyService.selectedFloor = undefined;
                }

                _setNextFloor();

//                _suc("Successfully fetched all floors.");

            },
            function (resp) {
                console.log(resp.data.message);
                _err("Something went wrong while fetching all floors");
            }
        );

    };

    var _setNextFloor = function () {
        var max = -1;
        for (var i = 0; i < $scope.xFloors.length; i++) {
            if (parseInt($scope.xFloors[i].floor_number) >= max) {
                max = parseInt($scope.xFloors[i].floor_number);
            }
        }
        $scope.newFloorNumber = max + 1;
    };

    var _isValidFloorNumber = function (fl) {
        if (fl === null || fl == undefined) {
            return false;
        }

        if (fl.floor_number === null || fl.floor_number === undefined) {
            return false;
        }

        return true;
    };

    $scope.fetchFloorPlanOverlay = function () {

        if (!_isValidFloorNumber(this.anyService.selectedFloor)) {
            // TODO: alert
            console.log('something is wrong with the floor');
            return;
        }

        var floor_number = this.anyService.selectedFloor.floor_number;
        var buid = this.anyService.selectedBuilding.buid;

        var promise = AnyplaceAPIService.downloadFloorPlan(this.anyService.jsonReq, buid, floor_number);
        promise.then(
            function (resp) {

                $scope.data.floor_plan_file = null;
                $scope.data.floor_plan = null;
                if ($scope.data.floor_plan_groundOverlay != null) {
                    $scope.data.floor_plan_groundOverlay.setMap(null);
                    $scope.data.floor_plan_groundOverlay = null;
                }

                // on success
                var data = resp.data;

                // load the correct coordinates from the selected floor
                var fl = $scope.anyService.selectedFloor;
                var imageBounds = new google.maps.LatLngBounds(
                    new google.maps.LatLng(fl.bottom_left_lat, fl.bottom_left_lng),
                    new google.maps.LatLng(fl.top_right_lat, fl.top_right_lng));

                $scope.data.floor_plan_groundOverlay = new USGSOverlay(imageBounds, "data:image/png;base64," + data, GMapService.gmap);

                // TODO: alert success
            },
            function (resp) {
                // on error
                console.log('error downloading floor plan');
                // TODO: alert failure
            }
        );
    };

    var canvasOverlay = null;
    $scope.isCanvasOverlayActive = false;

    var floorPlanInputElement = $('#input-floor-plan');

    floorPlanInputElement.change(function handleImage(e) {
        var reader = new FileReader();
        reader.onload = function (event) {
            var imgObj = new Image();
            imgObj.src = event.target.result;
            imgObj.onload = function () {
                canvasOverlay = new CanvasOverlay(imgObj, GMapService.gmap);
                $scope.$apply($scope.isCanvasOverlayActive = true);

                // hide previous floorplan
                if ($scope.data.floor_plan_groundOverlay && $scope.data.floor_plan_groundOverlay.getMap()) {
                    $scope.data.floor_plan_groundOverlay.setMap(null);
                }

            }

        };
        reader.readAsDataURL(e.target.files[0]);

        this.disabled = true;
    });

    $scope.setFloorPlan = function () {
        if (!canvasOverlay) {
            return;
        }

        if (GMapService.gmap.getZoom() < 20) {
            _err("You have provided zoom level " + GMapService.gmap.getZoom() + ". You have to zoom at least to level 20 to upload the floorplan.");
            return;
        }

        if (AnyplaceService.getBuildingId() === null || AnyplaceService.getBuildingId() === undefined) {
            console.log('building is undefined');
            _err("Something went wrong. It seems like there is no building selected");
        }

        var newFl = {
            is_published: 'true',
            buid: String(AnyplaceService.getBuildingId()),
            floor_name: String($scope.newFloorNumber),
            description: String($scope.newFloorNumber),
            floor_number: String($scope.newFloorNumber)
        };

        $scope.myFloors[$scope.myFloorId] = newFl;
        $scope.myFloorId++;

        // create the proper image inside the canvas
        canvasOverlay.drawBoundingCanvas();

        // create the ground overlay and destroy the canvasOverlay object
        // and also set the floor_plan_coords in $scope.data
        var bl = canvasOverlay.bottom_left_coords;
        var tr = canvasOverlay.top_right_coords;
        $scope.data.floor_plan_coords.bottom_left_lat = bl.lat();
        $scope.data.floor_plan_coords.bottom_left_lng = bl.lng();
        $scope.data.floor_plan_coords.top_right_lat = tr.lat();
        $scope.data.floor_plan_coords.top_right_lng = tr.lng();
        $scope.data.floor_plan_coords.zoom = GMapService.gmap.getZoom() + "";
        var data = canvasOverlay.getCanvas().toDataURL("image/png"); // defaults to png
        $scope.data.floor_plan_base64_data = data;
        var imageBounds = new google.maps.LatLngBounds(
            new google.maps.LatLng(bl.lat(), bl.lng()),
            new google.maps.LatLng(tr.lat(), tr.lng()));
        $scope.data.floor_plan_groundOverlay = new USGSOverlay(imageBounds, data, GMapService.gmap);

        canvasOverlay.setMap(null); // remove the canvas overlay since the groundoverlay is placed
        $('#input-floor-plan').prop('disabled', false);
        $scope.isCanvasOverlayActive = false;

        if (_floorNoExists($scope.newFloorNumber)) {
            for (var i = 0; i < $scope.xFloors.length; i++) {
                var f = $scope.xFloors[i];
                if (!LPUtils.isNullOrUndefined(f)) {
                    if (f.floor_number === String($scope.newFloorNumber)) {
                        $scope.uploadFloorPlanBase64($scope.anyService.selectedBuilding, f, $scope.data);
                        break;
                    }
                }
            }
        } else {
            $scope.addFloorObject(newFl, $scope.anyService.selectedBuilding, $scope.data);
        }

    };

    var _checkFloorFormat = function (bobj) {
        if (bobj === null) {
            bobj = {}
        } else {
            bobj = JSON.parse(JSON.stringify(bobj))
        }
        if (LPUtils.isNullOrUndefined(bobj.buid)) {
            bobj.buid = ""
        }
        if (LPUtils.isNullOrUndefined(bobj.floor_name)) {
            bobj.floor_name = ""
        }
        if (LPUtils.isNullOrUndefined(bobj.floor_number)) {
            bobj.floor_number = ""
        }
        if (LPUtils.isNullOrUndefined(bobj.description)) {
            bobj.description = ""
        }
        if (LPUtils.isNullOrUndefined(bobj.is_published)) {
            bobj.is_published = 'true'
        }
        return bobj;
    };

    $scope.addFloorObject = function (flJson, selectedBuilding, flData) {
        var obj = _checkFloorFormat(flJson);

        obj.owner_id = $scope.owner_id;

        if (!obj.owner_id) {
            _err("Could not authorize user. Please refresh.");
            return;
        }

        // make the request at AnyplaceAPI
        var promise = $scope.anyAPI.addFloor(obj);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;
                // insert the newly created building inside the loadedBuildings
                $scope.xFloors.push(obj);

                $scope.anyService.selectedFloor = $scope.xFloors[$scope.xFloors.length - 1];

                _suc("Successfully added new floor");

                $scope.uploadFloorPlanBase64(selectedBuilding, obj, flData);

            },
            function (resp) {
                var data = resp.data;
                console.log(data.message);
                _err("Something went wrong while adding a new floor.");
            }
        );

    };

    $scope.removeFloorPlan = function () {
        $scope.data.floor_plan_file = null;
        $scope.data.floor_plan = null;

        if (canvasOverlay) {
            canvasOverlay.setMap(null);
        }


        if ($scope.data.floor_plan_groundOverlay) {
            $scope.data.floor_plan_groundOverlay.setMap(null);
        }

        var x = $('#input-floor-plan');
        x.replaceWith(x = x.clone(true));

        x.prop('disabled', false);

        $scope.isCanvasOverlayActive = false;
    };

    $scope.deleteFloor = function () {

        var bobj = $scope.anyService.getFloor();

        if (LPUtils.isNullOrUndefined(bobj) || LPUtils.isStringBlankNullUndefined(bobj.floor_number) || LPUtils.isStringBlankNullUndefined(bobj.buid)) {
            _err("No floor seems to be selected.");
            return;
        }

        bobj.username = $scope.creds.username;
        bobj.password = $scope.creds.password;
        bobj.owner_id = $scope.owner_id;

        console.log(bobj);

        // make the request at AnyplaceAPI
        var promise = $scope.anyAPI.deleteFloor(bobj);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                // delete the building from the loadedBuildings
                var lf = $scope.xFloors;
                var sz = lf.length;

                for (var i = 0; i < sz; i++) {
                    if (lf[i].floor_number == bobj.floor_number) {
                        lf.splice(i, 1);
                        break;
                    }
                }

                if ($scope.data.floor_plan_groundOverlay != null) {
                    $scope.data.floor_plan_groundOverlay.setMap(null);
                    $scope.data.floor_plan_groundOverlay = null;
                }

                if ($scope.xFloors && $scope.xFloors.length > 0) {
                    $scope.anyService.selectedFloor = $scope.xFloors[0];
                } else {
                    $scope.anyService.selectedFloor = undefined;
                }

                _suc("Successfully deleted floor.");
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err("Something went wrong while deleting the floor.");
            }
        );
    };


    var _floorNoExists = function (n) {
        for (var i = 0; i < $scope.xFloors.length; i++) {
            var f = $scope.xFloors[i];

            if (!LPUtils.isNullOrUndefined(f)) {
                if (f.floor_number === String(n)) {
                    return true;
                }
            }
        }
        return false;
    };

    var _cloneCoords = function (obj) {
        if (LPUtils.isNullOrUndefined(obj)) {
            return {}
        }
        var n = JSON.parse(JSON.stringify(obj));
        return n;
    };

    $scope.uploadFloorPlanBase64 = function (sb, sf, flData) {
        if (LPUtils.isNullOrUndefined(canvasOverlay)) {
            return;
        }

        var bobj = _cloneCoords(flData.floor_plan_coords);

        if (LPUtils.isNullOrUndefined(bobj) || LPUtils.isStringBlankNullUndefined(bobj.bottom_left_lat)
            || LPUtils.isStringBlankNullUndefined(bobj.bottom_left_lng)
            || LPUtils.isStringBlankNullUndefined(bobj.top_right_lat)
            || LPUtils.isStringBlankNullUndefined(bobj.top_right_lng)) {

            console.log('error with floor coords');
            _err("Something went wrong. It seems like no valid coordinates have been set up for this floor plan.");
            return;
        }

        bobj.bottom_left_lat = String(bobj.bottom_left_lat);
        bobj.bottom_left_lng = String(bobj.bottom_left_lng);
        bobj.top_right_lat = String(bobj.top_right_lat);
        bobj.top_right_lng = String(bobj.top_right_lng);

        sf.bottom_left_lat = bobj.bottom_left_lat;
        sf.bottom_left_lng = bobj.bottom_left_lng;
        sf.top_right_lat = bobj.top_right_lat;
        sf.top_right_lng = bobj.top_right_lng;

        if (!LPUtils.isNullOrUndefined(sb)) {
            if (!LPUtils.isNullOrUndefined(sb.buid)) {
                bobj.buid = sb.buid;
            } else {
                _err("Something went wrong with the selected building's id.");
                return;
            }
        } else {
            // no building selected
            _err("Something went wrong. It seems like there is no building selected.");
            return;
        }

        if (!LPUtils.isNullOrUndefined(sf)) {
            if (!LPUtils.isNullOrUndefined(sf.floor_number)) {
                bobj.floor_number = sf.floor_number;
            } else {
                _err("Something went wrong. It seems there is no floor number associated with the selected floor.");
                return;
            }
        } else {
            // no floor selected
            _err("Something went wrong. It seems there is no floor selected.");
            return;
        }

        bobj.owner_id = $scope.owner_id;

        var json_req = JSON.stringify(bobj);

        if (LPUtils.isNullOrUndefined(flData.floor_plan_base64_data)
            || LPUtils.isStringEmptyNullUndefined(flData.floor_plan_base64_data)) {
            console.log('no floor plan file');
            return;
        }

        // make the request at AnyplaceAPI
        var promise = $scope.anyAPI.uploadFloorPlan64(json_req, $scope.data.floor_plan_base64_data);

        promise.then(
            function (resp) {
                // on success
                var data = resp.data;
                _suc("Successfully uploaded new floor plan.");
            },
            function (resp) {
                // on error
                var data = resp.data;
                //TODO: alert error
                _suc("Successfully uploaded new floor plan.");
            });

    };

    $scope.toggleRadioHeatmap = function () {
        if (heatmap && heatmap.getMap()) {
            heatmap.setMap(null);
            return;
        }

        $scope.showRadioHeatmap();
    };


    $scope.showRadioHeatmapPoi = function () {
        var jsonReq = {
            "buid": $scope.anyService.getBuildingId(),
            "floor": $scope.anyService.getFloorNumber(),
            "coordinates_lat": $scope.anyService.selectedPoi.coordinates_lat,
            "coordinates_lon": $scope.anyService.selectedPoi.coordinates_lon,
            "range": "1"
        };

        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;

        var promise = $scope.anyAPI.getRadioHeatmapPoi(jsonReq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                var heatMapData = [];

                var i = resp.data.radioPoints.length;

                if (i <= 0) {
                    _err("This floor seems not to be WiFi mapped. Download the Anyplace app from the Google Play store to map the floor.");
                    return;
                }

                while (i--) {
                    var rp = resp.data.radioPoints[i];
                    heatMapData.push(
                        {location: new google.maps.LatLng(rp.x, rp.y), weight: 1}
                    );
                    resp.data.radioPoints.splice(i, 1);
                }

                if (heatmap && heatmap.getMap()) {
                    heatmap.setMap(null);
                }

                heatmap = new google.maps.visualization.HeatmapLayer({
                    data: heatMapData
                });
                heatmap.setMap($scope.gmapService.gmap);
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err('Something went wrong while fetching radio heatmap.');
            }
        );
    }

    $scope.showRadioHeatmap = function () {
        var jsonReq = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber()};

        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;

        var promise = $scope.anyAPI.getRadioHeatmap(jsonReq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                var heatMapData = [];

                var i = resp.data.radioPoints.length;

                if (i <= 0) {
                    _err("This floor seems not to be WiFi mapped. Download the Anyplace app from the Google Play store to map the floor.");
                    return;
                }

                while (i--) {
                    var rp = resp.data.radioPoints[i];
                    heatMapData.push(
                        {location: new google.maps.LatLng(rp.x, rp.y), weight: 1}
                    );
                    resp.data.radioPoints.splice(i, 1);
                }

                if (heatmap && heatmap.getMap()) {
                    heatmap.setMap(null);
                }

                heatmap = new google.maps.visualization.HeatmapLayer({
                    data: heatMapData
                });
                heatmap.setMap($scope.gmapService.gmap);
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err('Something went wrong while fetching radio heatmap.');
            }
        );
    }

}
]);
/**
 *
 The MIT License (MIT)

 Copyright (c) 2015, Kyriakos Georgiou, Marileni Angelidou, Data Management Systems Laboratory (DMSL)
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

app.controller('PoiController', ['$scope', '$compile', 'GMapService', 'AnyplaceService', 'AnyplaceAPIService', function ($scope, $compile, GMapService, AnyplaceService, AnyplaceAPIService) {
    _CONNECTIONS_IS_ON = true;
    _POIS_IS_ON = true;
    $scope.anyService = AnyplaceService;
    $scope.anyAPI = AnyplaceAPIService;

    $scope.myMarkers = {};
    $scope.myMarkerId = 0;

    $scope.myPaths = {};
    $scope.myPathId = 0;

    $scope.myPois = [];
    $scope.myPoisHashT = {};

    $scope.myConnectionsHashT = {};

    $scope.edgeMode = false;
    $scope.connectPois = {
        prev: undefined,
        next: undefined
    };


    $scope.crudTabSelected = 1;
    $scope.setCrudTabSelected = function (n) {
        $scope.crudTabSelected = n;
    };
    $scope.isCrudTabSelected = function (n) {
        return $scope.crudTabSelected === n;
    };


    $scope.orderByName = function (value) {
        return value.name;
    };

    $scope.$on("loggedOff", function (event, mass) {
        $scope.clearConnectionsOnMap();
        $scope.clearPoisOnMap();
        $scope.myPaths = {};
        $scope.myPathId = 0;
        $scope.myMarkers = {};
        $scope.myMarkerId = 0;
        $scope.myPois = [];
        $scope.myPoisHashT = {};
        $scope.myConnectionsHashT = {};
        $scope.anyService.setAllPois($scope.myPoisHashT);
        $scope.anyService.setAllConnection($scope.myConnectionsHashT);
        $scope.edgeMode = false;
        $scope.connectPois = {
            prev: undefined,
            next: undefined
        };
    });

    $scope.$watch('anyService.selectedBuilding', function (newVal, oldVal) {
        //if (newVal && newVal.buid && newVal.poistypeid) {
        //$scope.fetchAllPoisTypes(newVal.poistypeid);
        //}
        //else {
        $scope.poisTypes = [
            "Disabled Toilets",
            "Elevator",
            "Entrance",
            "Fire Extinguisher",
            "First Aid/AED",
            "Kitchen",
            "Office",
            "Ramp",
            "Room",
            "Security/Guard",
            "Stair",
            "Toilets",
            "Other"
        ];

        $scope.poicategories = [{
            poicat: "Disabled Toilets",
            poicatPlaceholder: "name",
            id: "type1",
            disenable: "false"
        }, {
            poicat: "Elevator",
            poicatPlaceholder: "name",
            id: "type2",
            disenable: "true"
        }, {
            poicat: "Entrance",
            poicatPlaceholder: "name",
            id: "type3",
            disenable: "true"
        }, {
            poicat: "Fire Extinguisher",
            poicatPlaceholder: "name",
            id: "type4",
            disenable: "false"
        }, {
            poicat: "First Aid/AED",
            poicatPlaceholder: "name",
            id: "type5",
            disenable: "false"
        }, {
            poicat: "Kitchen",
            poicatPlaceholder: "name",
            id: "type6",
            disenable: "false"
        }, {
            poicat: "Office",
            poicatPlaceholder: "name",
            id: "type7",
            disenable: "false"
        }, {
            poicat: "Ramp",
            poicatPlaceholder: "name",
            id: "type8",
            disenable: "false"
        }, {
            poicat: "Room",
            poicatPlaceholder: "name",
            id: "type9",
            disenable: "false"
        }, {
            poicat: "Security/Guard",
            poicatPlaceholder: "name",
            id: "type10",
            disenable: "false"
        }, {
            poicat: "Stair",
            poicatPlaceholder: "name",
            id: "type11",
            disenable: "true"
        }, {
            poicat: "Toilets",
            poicatPlaceholder: "name",
            id: "type12",
            disenable: "false"
        }, {
            poicat: "Other",
            poicatPlaceholder: "name",
            id: "type13",
            disenable: "false"
        }
        ];
        //}
    });

    $scope.$watch('anyService.selectedFloor', function (newVal, oldVal) {
        if (newVal !== undefined && newVal !== null && !_.isEqual(newVal, oldVal)) {
            $scope.fetchAllPoisForFloor(newVal);
        } else {
            $scope.anyService.selectedPoi = undefined;

        }
    });

    $scope.$watch('anyService.selectedPoi', function (newVal, oldVal) {
        if (newVal && _latLngFromPoi(newVal)) {
            if (newVal) {
                GMapService.gmap.panTo(_latLngFromPoi(newVal));
                //GMapService.gmap.setZoom(19);
                if (newVal.puid) {
                    var marker = $scope.myPoisHashT[newVal.puid].marker;
                    if (marker && marker.infowindow && marker.tpl2) {
                        marker.infowindow.setContent(marker.tpl2[0]);
                        marker.infowindow.open(GMapService.gmap, marker);
                    }

                    if (typeof(Storage) !== "undefined" && localStorage) {
                        localStorage.setItem("lastPoi", newVal.puid);
                    }
                }
            }
        }
    });

    $scope.add = function () {
        if ($scope.poicategories[$scope.poicategories.length - 1].poicat != "") {
            $scope.poicategories.push({
                poicat: "",
                poicatPlaceholder: "name",
                disenable: "false"
            });
        }
        else {
            _err("Complete the last input to continue!");
        }
    };

    $scope.deletetype = function (id) {

        for (var i = 0; i < $scope.poicategories.length; i++) {
            if ($scope.poicategories[i].id == id) {
                $scope.poicategories[i].id = "";
                $("#".id).remove();
                break;
            }
        }
    };

    $scope.addcategory = function () {

        var name_element = document.getElementById("poistype");
        var name = "\"poistype\":\"" + name_element.value + "\"";

        function S4() {
            return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
        }

        var guid = (S4() + S4() + "-" + S4() + "-4" + S4().substr(0, 3) + "-" + S4() + "-" + S4() + S4() + S4()).toLowerCase();
        var d = new Date();


        var poistypeid = "poistypeid_" + guid + "_" + d.getTime();
        poistypeid = "\"poistypeid\":\"" + poistypeid + "\"";

        var sz = $scope.poicategories.length;

        if (sz == 0) {
            _err("No categories added.");
            return;
        }

        var types = "\"types\":[";
        for (var i = sz - 1; i > 0; i--) {
            if ($scope.poicategories[i].poicat != "") {
                types = types + "\"" + $scope.poicategories[i].poicat + "\",";
            }
        }
        types = types + "\"" + $scope.poicategories[0].poicat + "\"]";

        var jreq = "{" + name + "," + poistypeid + "," + types + ",\"owner_id\":\"" + $scope.owner_id + "\",\"access_token\":\"" + $scope.gAuth.access_token + "\"}";

        var promise = $scope.anyAPI.addCategory(jreq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;
                _suc("Successfully added category.");
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err("Something went wrong while adding the category. " + data.message);
            }
        );

    };


    $scope.fetchAllPoisTypes = function (poistypeid) {

        //TODO: validation

        var jsonReq = $scope.anyService.jsonReq;

        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;
        jsonReq.owner_id = $scope.owner_id;
        jsonReq.access_token = $scope.gAuth.access_token;
        jsonReq.poistypeid = poistypeid;

        if (!jsonReq.owner_id) {
            _err("Could nor authorize user. Please refresh.");
            return;
        }
        var promise = $scope.anyAPI.retrievePoisTypes(jsonReq);
        promise.then(
            function (resp) {
                var data = resp.data;

                var poistypes = data.poistypes;

                var sz = poistypes.length;
                for (var i = sz - 1; i >= 0; i--) {
                    if (poistypes[i].poistypeid == poistypeid) {
                        var types = poistypes[i].types;
                        break;
                    }
                }

                var sz = types.length;
                for (var i = sz - 1; i >= 0; i--) {
                    $scope.poisTypes[i] = types[i];
                }
            },
            function (resp) {
                var data = resp.data;
                _err("Something went wrong while fetching POIs types");
            }
        );
    };


    $scope.onInfoWindowKeyDown = function (e) {
        // esc key
        if (e.keyCode == 27) {
            if ($scope.anyService.selectedPoi) {
                var p = $scope.anyService.selectedPoi;
                if ($scope.myPoisHashT[p.puid] && $scope.myPoisHashT[p.puid].marker && $scope.myPoisHashT[p.puid].marker.infowindow) {
                    $scope.myPoisHashT[p.puid].marker.infowindow.setMap(null);
                    $scope.anyService.setAllPois($scope.myPoisHashT);
                }
            }
        }
    };

    $scope.clearPoisOnMap = function () {

        for (var i = 0; i < $scope.myPois.length; i++) {
            var p = $scope.myPois[i];
            if (p && $scope.myPoisHashT[p.puid] && $scope.myPoisHashT[p.puid].marker) {
                $scope.myPoisHashT[p.puid].marker.setMap(null);
                delete $scope.myPoisHashT[p.puid];
            }
        }
        $scope.myPoisHashT = {};
        $scope.anyService.setAllPois($scope.myPoisHashT);
    };

    $scope.clearConnectionsOnMap = function () {
        if ($scope.myConnectionsHashT) {

            for (var con in $scope.myConnectionsHashT) {
                if (con && $scope.myConnectionsHashT.hasOwnProperty(con) && $scope.myConnectionsHashT[con] && $scope.myConnectionsHashT[con].polyLine) {
                    $scope.myConnectionsHashT[con].polyLine.setMap(null);
                    delete $scope.myConnectionsHashT[con];
                }
            }
            $scope.myConnectionsHashT = {};
            $scope.anyService.setAllConnection($scope.myConnectionsHashT);
        }

    };

    $scope.fetchConnections = function () {

        var tobj = $scope.anyService.jsonReq;
        var buid, floor_number;

        // we must check that a floor and a building has been selected
        var sb = $scope.anyService.getBuilding();
        if (!LPUtils.isNullOrUndefined(sb)) {
            if (!LPUtils.isNullOrUndefined(sb.buid)) {
                buid = $scope.anyService.getBuildingId();
            } else {
                _err("No valid building has been selected!");
                return;
            }
        } else {
            // no building selected
            _err("No building has been selected!");
            return;
        }

        var sf = $scope.anyService.getFloor();
        if (!LPUtils.isNullOrUndefined(sf)) {
            if (!LPUtils.isNullOrUndefined(sf.floor_number)) {
                floor_number = $scope.anyService.getFloorNumber()
            } else {
                _err("No valid floor has been selected!");
                return;
            }
        } else {
            _err("No floor has been selected!");
            return;
        }

        tobj.buid = buid;
        tobj.floor_number = floor_number;

        if (LPUtils.isNullOrUndefined($scope.myPois) || LPUtils.isNullOrUndefined($scope.myPoisHashT)) {
            _err("Please load the POIs of this floor first.");
        }

        if ($scope.myPois.length == 0) {
            // _err("This floor is empty.");
            return;
        }

        var json_req = tobj;

        // we must query the db to get all the pois for the floor
        var promise = $scope.anyAPI.retrieveConnectionsByBuildingFloor(json_req);
        promise.then(
            function (resp) {
                var data = resp.data;

                $scope.clearConnectionsOnMap();
                //var connections = JSON.parse( data.connections );
                var connections = data.connections;

                var hasht = {};
                var sz = connections.length;
                for (var i = 0; i < sz; i++) {
                    // insert the pois inside the hashtable
                    hasht[connections[i].cuid] = connections[i];
                }
                $scope.myConnectionsHashT = hasht;
                $scope.anyService.setAllConnection($scope.myConnectionsHashT);
                // draw the markers
                // $scope.data.MainController.clearConnectionsOnMap();

                $scope.drawConnectionsOnMap();

                //_suc("Connections were loaded successfully.");
            },
            function (resp) {
                var data = resp.data;
                _err("Something went wrong while loading the POI connections");
            }
        );

    };

    $scope.drawConnectionsOnMap = function () {

        for (var cuid in $scope.myConnectionsHashT) {
            if ($scope.myConnectionsHashT.hasOwnProperty(cuid)) {
                var conn = $scope.myConnectionsHashT[cuid];

                if (!conn || !conn.pois_a || !conn.pois_b || !$scope.myPoisHashT[conn.pois_a] || !$scope.myPoisHashT[conn.pois_b] || !$scope.myPoisHashT[conn.pois_a].model || !$scope.myPoisHashT[conn.pois_b].model) {
                    continue;
                }

                var flightPlanCoordinates = [
                    new google.maps.LatLng($scope.myPoisHashT[conn.pois_a].model.coordinates_lat, $scope.myPoisHashT[conn.pois_a].model.coordinates_lon),
                    new google.maps.LatLng($scope.myPoisHashT[conn.pois_b].model.coordinates_lat, $scope.myPoisHashT[conn.pois_b].model.coordinates_lon)
                ];

                var flightPath = new google.maps.Polyline({
                    path: flightPlanCoordinates,
                    strokeColor: "#0000FF",
                    strokeOpacity: 0.5,
                    strokeWeight: 4
                });
                if (_CONNECTIONS_IS_ON)
                    flightPath.setMap(GMapService.gmap);
                flightPath.model = conn;

                $scope.myConnectionsHashT[cuid].polyLine = flightPath;
                $scope.anyService.setAllConnection($scope.myConnectionsHashT);


                google.maps.event.addListener(flightPath, 'click', function () {
                    $scope.$apply(_deleteConnection(this));
                    $scope.anyService.setAllConnection($scope.myConnectionsHashT);
                });

            }
        }
    };

    var _POI_CONNECTOR_IMG = 'build/images/edge-connector-icon.png';
    var _POI_EXISTING_IMG = 'build/images/any-poi-icon.png';
    var _POI_NEW_IMG = 'build/images/poi-icon.png';

    var _latLngFromPoi = function (p) {
        if (p && p.coordinates_lat && p.coordinates_lon) {
            return {lat: parseFloat(p.coordinates_lat), lng: parseFloat(p.coordinates_lon)}
        }
        return undefined;
    };

    var _err = function (msg) {
        $scope.anyService.addAlert('danger', msg);
    };

    var _suc = function (msg) {
        $scope.anyService.addAlert('success', msg);
    };

    var _warn = function (msg) {
        $scope.anyService.addAlert('warning', msg);
    };

    var _isPoiNearFloor = function (coords) {
        var D = 0.001;

        var pLat = coords.lat();
        var pLng = coords.lng();


        var floor = $scope.anyService.getFloor();

        if (LPUtils.isNullOrUndefined(floor)) {
            _err("No selected floor found.");
            return false;
        }

        var topRightLat = floor.top_right_lat;
        var topRightLng = floor.top_right_lng;


        var bottomLeftLat = floor.bottom_left_lat;
        var bottomLeftLng = floor.bottom_left_lng;

        if (!topRightLat || !topRightLng || !bottomLeftLat || !bottomLeftLng) {
            _err("Floor coordinates not found.");
            return false;
        }

        return pLat <= parseFloat(topRightLat) + D && pLng <= parseFloat(topRightLng) + D && pLat >= parseFloat(bottomLeftLat) - D && pLng >= parseFloat(bottomLeftLng) - D;
    };

    $scope.drawPoisOnMap = function () {

        var infowindow = new google.maps.InfoWindow({
            content: '-',
            maxWidth: 500
        });

        var localPuid = undefined;
        var localPoiIndex = -1;
        if (typeof(Storage) !== "undefined" && localStorage
            && !LPUtils.isNullOrUndefined(localStorage.getItem('lastBuilding'))
            && !LPUtils.isNullOrUndefined(localStorage.getItem('lastFloor'))
            && !LPUtils.isNullOrUndefined(localStorage.getItem('lastPoi'))) {

            localPuid = localStorage.getItem('lastPoi');
        }


        for (var i = 0; i < $scope.myPois.length; i++) {
            $scope.myPois[i].pois_type2 = $scope.myPois[i].pois_type;
            var p = $scope.myPois[i];

            if (localPuid && p.puid === localPuid) {
                localPoiIndex = i;
            }

            var marker;
            var htmlContent = '-';
            p.pois_type2 = p.pois_type;
            if (p.pois_type == "None") {
                marker = new google.maps.Marker({
                    position: _latLngFromPoi(p),
                    draggable: true,
                    icon: new google.maps.MarkerImage(
                        _POI_CONNECTOR_IMG,
                        null, /* size is determined at runtime */
                        null, /* origin is 0,0 */
                        null, /* anchor is bottom center of the scaled image */
                        new google.maps.Size(21, 21)
                    )
                });
                if (_POIS_IS_ON)
                    marker.setMap(GMapService.gmap);

                htmlContent = '<div class="infowindow-scroll-fix" style="text-align: center; width:170px">'
                    + '<fieldset class="form-group" style="display: inline-block; width: 73%;">'
                    + '<button type="submit" class="btn btn-success add-any-button" ng-click="updatePoi(\'' + p.puid + '\')"><span class="glyphicon glyphicon-pencil"></span> Update'
                    + '</button>'
                    + '</fieldset>'
                    + '<fieldset class="form-group" style="display: inline-block;width: 23%;">'
                    + '<button type="submit" class="btn btn-danger add-any-button" style="margin-left:2px" ng-click="deletePoi(\'' + p.puid + '\')"><span class="glyphicon glyphicon-remove"></span>'
                    + '</button>'
                    + '</fieldset>'
                    + '</div>';
            } else {
                var imgType = _POI_EXISTING_IMG;
                var size = new google.maps.Size(21, 32);

//                if (p.pois_type === "Entrance") {
//                    imgType = "images/door.png";
//                    size = new google.maps.Size(32, 32)
//                } else if (p.pois_type === "Toilets") {
//                    imgType = "images/toilets.png";
//                    size = new google.maps.Size(32, 32)
//                } else if (p.pois_type === "Ramp") {
//                    imgType = "images/wheel_chair_accessible.png";
//                    size = new google.maps.Size(32, 32)
//                }

                marker = new google.maps.Marker({
                    position: _latLngFromPoi(p),
                    draggable: true,
                    icon: new google.maps.MarkerImage(
                        imgType,
                        null, /* size is determined at runtime */
                        null, /* origin is 0,0 */
                        null, /* anchor is bottom center of the scaled image */
                        size
                    )
                });

                if (_POIS_IS_ON)
                    marker.setMap(GMapService.gmap);

                htmlContent = '<div class="infowindow-scroll-fix" ng-keydown="onInfoWindowKeyDown($event)">'
                    + '<form name="poiForm">'
                    + '<fieldset class="form-group">'
                    + '<textarea ng-model="myPois[' + i + '].name" id="poi-name" type="text" class="form-control" placeholder="poi name" tabindex="1" autofocus></textarea>'
                    + '</fieldset>'
                    + '<fieldset class="form-group">'
                    + '<textarea ng-model="myPois[' + i + '].description" id="poi-description" type="text" class="form-control" placeholder="poi description" tabindex="5"></textarea>'
                    + '</fieldset>'
                    + '<fieldset class="form-group">'
                    + '<select ng-model="myPois[' + i + '].pois_type" class="form-control" ng-options="type for type in poisTypes" title="POI Types" tabindex="2">'
                    + '<option value="">Select POI Type</option>'
                    + '</select>'
                    + '</fieldset class="form-group">'
                    + '<fieldset class="form-group">Or enter your own type:'
                    + '<input ng-model="myPois[' + i + '].pois_type2" id="poi-pois_type2" type="text" class="form-control" placeholder="POI Type" tabindex="2">'
                    + '</fieldset>'
                    + '<fieldset class="form-group">'
                    + '<input ng-model="myPois[' + i + '].is_building_entrance" id="poi-entrance" type="checkbox" tabindex="4"><span> is building entrance?</span>'
                    + '</fieldset>'
                    + '<div style="text-align: center;">'
                    + '<fieldset class="form-group" style="display: inline-block; width: 75%;">'
                    + '<button type="submit" class="btn btn-success add-any-button" ng-click="updatePoi(\'' + p.puid + '\')" tabindex="3"><span class="glyphicon glyphicon-pencil"></span> Update'
                    + '</button>'
                    + '</fieldset>'
                    + '<fieldset class="form-group" style="display: inline-block;width: 23%;">'
                    + '<button type="submit" class="btn btn-danger add-any-button" style="margin-left:2px" ng-click="deletePoi(\'' + p.puid + '\')" tabindex="6"><span class="glyphicon glyphicon-remove"></span>'
                    + '</button>'
                    + '</fieldset>'
                    + '</div>'
                    + '</form>'
                    + '</div>';
            }

            var tpl = $compile(htmlContent)($scope);

            marker.tpl2 = tpl;
            marker.model = p;

            marker.infowindow = infowindow;

            $scope.myPoisHashT[p.puid].marker = marker;
            $scope.anyService.setAllPois($scope.myPoisHashT);

            google.maps.event.addListener(marker, 'click', function () {

                if ($scope.edgeMode) {
                    if ($scope.connectPois.prev) {

                        if ($scope.connectPois.prev == this) {
                            $scope.connectPois.prev = undefined;

                            if (this.model.pois_type === "None") {
                                this.setIcon(_getConnectorIconNormal());
                            } else {
                                this.setIcon(_getNormalPoiIconNormal());
                            }
                            return;
                        }

                        // No longer the beginning of an edge, make smaller.
                        if ($scope.connectPois.prev.model.pois_type === "None") {
                            $scope.connectPois.prev.setIcon(_getConnectorIconNormal());
                        } else {
                            $scope.connectPois.prev.setIcon(_getNormalPoiIconNormal());
                        }

                        var flightPath = new google.maps.Polyline({
                            path: [this.position, $scope.connectPois.prev.position],
                            geodesic: true,
                            strokeColor: '#FF0000',
                            strokeOpacity: 0.5,
                            strokeWeight: 4
                        });

                        flightPath.setMap(GMapService.gmap);

                        // Construct the request
                        var poiA = $scope.connectPois.prev.model;
                        var poiB = this.model;

                        if (!poiA || !poiB || !poiA.puid || !poiB.puid || !poiA.buid || !poiB.buid || LPUtils.isNullOrUndefined(poiA.floor_number) || LPUtils.isNullOrUndefined(poiB.floor_number)) {
                            _err("One or both of the POIs attempted to be connected seem to be be malformed. Please refresh.");
                            return;
                        }

                        var jsonReq = {

                            username: $scope.creds.username,
                            password: $scope.creds.password,
                            owner_id: $scope.owner_id,

                            pois_a: poiA.puid,
                            floor_a: poiA.floor_number,
                            buid_a: poiA.buid,

                            // insert the connection buid ( needed by the Dijkstra algorithm at the moment )
                            buid: poiA.buid,

                            pois_b: poiB.puid,
                            floor_b: poiB.floor_number,
                            buid_b: poiB.buid,

                            is_published: 'true',
                            edge_type: 'hallway'
                        };

                        flightPath.model = jsonReq;

                        // make the request at AnyplaceAPI

                        var promise = $scope.anyAPI.addConnection(jsonReq);
                        promise.then(
                            function (resp) {
                                var data = resp.data;
                                var cuid = data.cuid;

                                flightPath.model.cuid = cuid;
                                var cloneModel = flightPath.model;
                                cloneModel.polyLine = flightPath;
                                $scope.myConnectionsHashT[cuid] = cloneModel;
                                $scope.anyService.setAllConnection($scope.myConnectionsHashT);

                                flightPath.setOptions({
                                    strokeColor: '#0000FF',
                                    strokeOpacity: 0.5
                                });

                                google.maps.event.addListener(flightPath, 'click', function () {
                                    $scope.$apply(_deleteConnection(this));
                                });

                            },
                            function (resp) {
                                var data = resp.data;
                                _err("Something went wrong while attempting to connect the two POIs.");
                                flightPath.setMap(null);
                            }
                        );

                        $scope.connectPois.prev = undefined;

                    } else {

                        $scope.connectPois.prev = this;

                        // Increase the size of markers to indicate as the start of an edge
                        if (this.model.pois_type === "None") {
                            this.setIcon(_getConnectorIconBigger());
                        } else {
                            this.setIcon(_getNormalPoiIconBigger());
                        }
                    }

                } else {
                    infowindow.setContent(this.tpl2[0]);
                    infowindow.open(GMapService.gmap, this);
                    var self = this;
                    $scope.$apply(function () {
                        if (self.model && self.model.puid) {
                            $scope.anyService.selectedPoi = self.model;
                        }
                    })
                }
            });

            google.maps.event.addListener(marker, "dragend", function (event) {
                if (this.model && this.model.puid) {
                    $scope.updatePoiPosition(this);
                }
            });
        }

        if (localPoiIndex >= 0) {
            $scope.anyService.selectedPoi = $scope.myPois[localPoiIndex];
        }
        $scope.fetchConnections();
    };

    var _getConnectorIconBigger = function () {
        return new google.maps.MarkerImage(
            _POI_CONNECTOR_IMG,
            null, /* size is determined at runtime */
            null, /* origin is 0,0 */
            null, /* anchor is bottom center of the scaled image */
            new google.maps.Size(41, 41)
        )
    };

    var _getConnectorIconNormal = function () {
        return new google.maps.MarkerImage(
            _POI_CONNECTOR_IMG,
            null, /* size is determined at runtime */
            null, /* origin is 0,0 */
            null, /* anchor is bottom center of the scaled image */
            new google.maps.Size(21, 21)
        )
    };

    var _getNormalPoiIconBigger = function () {
        return new google.maps.MarkerImage(
            _POI_EXISTING_IMG,
            null, /* size is determined at runtime */
            null, /* origin is 0,0 */
            null, /* anchor is bottom center of the scaled image */
            new google.maps.Size(31, 48)
        )
    };

    var _getNormalPoiIconNormal = function () {
        return new google.maps.MarkerImage(
            _POI_EXISTING_IMG,
            null, /* size is determined at runtime */
            null, /* origin is 0,0 */
            null, /* anchor is bottom center of the scaled image */
            new google.maps.Size(21, 32)
        )
    };

    var _checkConnectionFormat = function (bobj) {
        if (bobj === null) {
            bobj = {}
        }
        if (LPUtils.isNullOrUndefined(bobj.buid)) {
            bobj.buid = ""
        }
        if (LPUtils.isNullOrUndefined(bobj.is_published)) {
            bobj.is_published = false
        }
        if (LPUtils.isNullOrUndefined(bobj.edge_type)) {
            bobj.edge_type = ""
        }
        if (LPUtils.isNullOrUndefined(bobj.pois_a)) {
            bobj.pois_a = ""
        }
        if (LPUtils.isNullOrUndefined(bobj.pois_b)) {
            bobj.pois_b = ""
        }
        if (LPUtils.isNullOrUndefined(bobj.floor_a)) {
            bobj.floor_a = ""
        }
        if (LPUtils.isNullOrUndefined(bobj.floor_b)) {
            bobj.floor_b = ""
        }
        if (LPUtils.isNullOrUndefined(bobj.buid_a)) {
            bobj.buid_a = ""
        }
        if (LPUtils.isNullOrUndefined(bobj.buid_b)) {
            bobj.buid_b = ""
        }
        return bobj;
    };

    var _deleteConnection = function (fp) {

        if (!$scope.edgeMode) {
            _err("Enable \"Edge Mode\" so you can delete connections by clicking on them.");
            return;
        }

        if (!fp || !fp.model || !fp.model.cuid) {
            _err("No valid connection selected.");
            return;
        }

        var conn = fp.model;
        var cuid = fp.model.cuid;

        if (!$scope.myConnectionsHashT[cuid]) {
            _err("The connection attempted to delete does not exist in the system.");
            return;
        }

        // there is a connection selected and loaded so use it for update
        var obj = _checkConnectionFormat(conn);
        obj.username = $scope.creds.username;
        obj.password = $scope.creds.password;
        obj.owner_id = $scope.owner_id;

        if (LPUtils.isNullOrUndefined(obj.pois_a) || LPUtils.isStringBlankNullUndefined(obj.pois_a)) {
            _err("No valid connection has been selected. Missing POI A.");
            return;
        } else if (LPUtils.isNullOrUndefined(obj.pois_b) || LPUtils.isStringBlankNullUndefined(obj.pois_b)) {
            _err("No valid connection has been selected. Missing POI B.");
            return;
        }

        // visually remove it immediately, on failure we restore it.
        fp.setMap(null);

        // TODO: Figure proper data structures for two way data hold.
        var temp = fp.model.polyLine;
        delete fp.model.polyLine;

        // make the request at AnyplaceAPI
        var promise = $scope.anyAPI.deleteConnection(obj);

        promise.then(
            function (resp) {
                var data = resp.data;

                delete $scope.myConnectionsHashT[cuid];
                $scope.anyService.setAllConnection($scope.myConnectionsHashT);
            },
            function (resp) {
                var data = resp.data;
                fp.setMap(GMapService.gmap);
                fp.model.polyLine = temp;
                _err("Something went wrong. Connection could not be deleted.");
            }
        );

    };

    $scope.fetchAllPoisForFloor = function (fl) {

        //TODO: validation
        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = AnyplaceService.getBuildingId();
        jsonReq.floor_number = AnyplaceService.getFloorNumber();

        var promise = AnyplaceAPIService.retrievePoisByBuildingFloor(jsonReq);
        promise.then(
            function (resp) {
                var data = resp.data;

                $scope.clearPoisOnMap();

                $scope.myPois = data.pois;

                var sz = $scope.myPois.length;
                for (var i = sz - 1; i >= 0; i--) {
                    // insert the pois inside the hashtable
                    // TODO clone (?)
                    var puid = $scope.myPois[i].puid;

                    if ($scope.myPois[i].is_building_entrance == 'true') {
                        $scope.myPois[i].is_building_entrance = true;
                    } else {
                        $scope.myPois[i].is_building_entrance = false;
                    }

                    $scope.myPois[i].pois_type2 = $scope.myPois[i].pois_type;

                    $scope.myPoisHashT[puid] = {};
                    $scope.myPoisHashT[puid].model = $scope.myPois[i];
                    $scope.anyService.setAllPois($scope.myPoisHashT);
                }

                // draw the markers
                $scope.drawPoisOnMap();

                //_suc("Successfully fetched all POIs.");
            },
            function (resp) {
                var data = resp.data;
                _err("Something went wrong while fetching POIs");
            }
        );
    };

    var overlay = new google.maps.OverlayView();
    overlay.draw = function () {
    };
    overlay.setMap(GMapService.gmap);

    $scope.addPoi = function (id) {
        if ($scope.myMarkers[id] && $scope.myMarkers[id].marker) {

            if ($scope.myMarkers[id].model.pois_type2.localeCompare("") != 0) {
                $scope.myMarkers[id].model.pois_type = $scope.myMarkers[id].model.pois_type2;
            }

            var poi = $scope.myMarkers[id].model;

            // set owner id
            poi.owner_id = $scope.owner_id;

            poi.coordinates_lat = String($scope.myMarkers[id].marker.position.lat());
            poi.coordinates_lon = String($scope.myMarkers[id].marker.position.lng());

            poi.is_building_entrance = String(poi.is_building_entrance);

            if (poi.coordinates_lat === undefined || poi.coordinates_lat === null) {
                _err("POI has invalid latitude format");
                return;
            }

            if (poi.coordinates_lon === undefined || poi.coordinates_lon === null) {
                _err("POI has invalid longitude format");
                return;
            }

            if (poi.owner_id && poi.name && poi.buid && poi.pois_type
                && !LPUtils.isNullOrUndefined(poi.is_building_entrance)
                && !LPUtils.isNullOrUndefined(poi.is_published)
                && !LPUtils.isNullOrUndefined(poi.is_door)
                && !LPUtils.isNullOrUndefined(poi.floor_name)
                && !LPUtils.isNullOrUndefined(poi.floor_number)) {

                if (poi.is_building_entrance == 'true') {
                    poi.is_building_entrance = 'true';
                } else {
                    poi.is_building_entrance = 'false';
                }

                var json_req = poi;

                // make the request at AnyplaceAPI
                var promise = $scope.anyAPI.addPois(json_req);
                promise.then(
                    function (resp) {
                        var data = resp.data;

                        poi.puid = data.puid;

                        if (poi.is_building_entrance == 'true') {
                            poi.is_building_entrance = true;
                        } else {
                            poi.is_building_entrance = false;
                        }

                        // insert the newly created building inside the loadedBuildings
                        $scope.myPois.push(poi);
                        $scope.anyService.selectedPoi = $scope.myPois[$scope.myPois.length - 1];

                        // update the hashtable

                        $scope.myPoisHashT[poi.puid] = {
                            model: poi,
                            marker: $scope.myMarkers[id].marker
                        };
                        $scope.anyService.setAllPois($scope.myPoisHashT);

                        $scope.myMarkers[id].model = poi;

                        if ($scope.myMarkers[id].infowindow) {
                            $scope.myMarkers[id].infowindow.close();
                            $scope.myMarkers[id].infowindow.setContent($scope.myMarkers[id].marker.tpl2[0]);
                        }

                        $scope.myMarkers[id].marker.model = poi;

                        if ($scope.myMarkers[id].marker.model.pois_type == "None") {
                            $scope.myMarkers[id].marker.setIcon(_getConnectorIconNormal());
                        } else {
                            $scope.myMarkers[id].marker.setIcon(_getNormalPoiIconNormal());
                        }

                        google.maps.event.clearListeners($scope.myMarkers[id].marker, 'click');

                        var infowindow = $scope.myMarkers[id].infowindow;

                        google.maps.event.addListener($scope.myMarkers[id].marker, 'click', function () {
                            if ($scope.edgeMode) {
                                if ($scope.connectPois.prev) {

                                    if ($scope.connectPois.prev == this) {
                                        $scope.connectPois.prev = undefined;

                                        if (this.model.pois_type === "None") {
                                            this.setIcon(_getConnectorIconNormal());
                                        } else {
                                            this.setIcon(_getNormalPoiIconNormal());
                                        }
                                        return;
                                    }

                                    // No longer the beginning of an edge, make smaller.
                                    if ($scope.connectPois.prev.model.pois_type === "None") {
                                        $scope.connectPois.prev.setIcon(_getConnectorIconNormal());
                                    } else {
                                        $scope.connectPois.prev.setIcon(_getNormalPoiIconNormal());
                                    }

                                    var flightPath = new google.maps.Polyline({
                                        path: [this.position, $scope.connectPois.prev.position],
                                        geodesic: true,
                                        strokeColor: '#FF0000',
                                        strokeOpacity: 0.5,
                                        strokeWeight: 4
                                    });

                                    flightPath.setMap(GMapService.gmap);

                                    // Construct the request
                                    var poiA = $scope.connectPois.prev.model;
                                    var poiB = this.model;

                                    if (!poiA || !poiB || !poiA.puid || !poiB.puid || !poiA.buid || !poiB.buid || LPUtils.isNullOrUndefined(poiA.floor_number) || LPUtils.isNullOrUndefined(poiB.floor_number)) {
                                        _err("One or both of the POIs attempted to be connected seem to be be malformed. Please refresh.");
                                        return;
                                    }

                                    var jsonReq = {

                                        username: $scope.creds.username,
                                        password: $scope.creds.password,
                                        owner_id: $scope.owner_id,

                                        pois_a: poiA.puid,
                                        floor_a: poiA.floor_number,
                                        buid_a: poiA.buid,

                                        // insert the connection buid ( needed by the Dijkstra algorithm at the moment )
                                        buid: poiA.buid,

                                        pois_b: poiB.puid,
                                        floor_b: poiB.floor_number,
                                        buid_b: poiB.buid,

                                        is_published: 'true',
                                        edge_type: 'hallway'
                                    };

                                    flightPath.model = jsonReq;

                                    // make the request at AnyplaceAPI

                                    var promise = $scope.anyAPI.addConnection(jsonReq);
                                    promise.then(
                                        function (resp) {
                                            var data = resp.data;
                                            var cuid = data.cuid;

                                            flightPath.model.cuid = cuid;
                                            var cloneModel = flightPath.model;
                                            cloneModel.polyLine = flightPath;
                                            $scope.myConnectionsHashT[cuid] = cloneModel;
                                            $scope.anyService.setAllConnection($scope.myConnectionsHashT);

                                            flightPath.setOptions({
                                                strokeColor: '#0000FF',
                                                strokeOpacity: 0.5
                                            });

                                            google.maps.event.addListener(flightPath, 'click', function () {
                                                $scope.$apply(_deleteConnection(this));
                                            });

                                        },
                                        function (resp) {
                                            var data = resp.data;
                                            _err("Something went wrong while attempting to connect the two POIs.");
                                            flightPath.setMap(null);
                                        }
                                    );

                                    $scope.connectPois.prev = undefined;
                                } else {
                                    $scope.connectPois.prev = this;

                                    // Increase the size of markers to indicate as the start of an edge
                                    if (this.model.pois_type === "None") {
                                        this.setIcon(_getConnectorIconBigger());
                                    } else {
                                        this.setIcon(_getNormalPoiIconBigger());
                                    }
                                }
                            } else {
                                infowindow.setContent(this.tpl2[0]);
                                infowindow.open(GMapService.gmap, this);
                                var self = this;
                                $scope.$apply(function () {
                                    if (self.model && self.model.puid) {
                                        $scope.anyService.selectedPoi = self.model;
                                    }
                                })
                            }
                        });

                        google.maps.event.addListener($scope.myMarkers[id].marker, "dragend", function (event) {
                            if (this.model && this.model.puid) {
                                $scope.updatePoiPosition(this);
                            }
                        });

                        // Too spammy.
                        //_suc("Successfully added new POI.");

                    },
                    function (resp) {
                        var data = resp.data;
                        _err("Something went wrong while adding the new POI.");
                    }
                );
            } else {
                _err("Cannot add new POI. Some required fields are missing.");
            }
        }
    };

    $scope.deletePoi = function (id) {

        var bobj = $scope.myPoisHashT[id];

        if (!bobj || !bobj.model || !bobj.model.puid) {
            if ($scope.myPoisHashT && $scope.myMarkers && $scope.myMarkers[id] && $scope.myMarkers[id].model && $scope.myPoisHashT[$scope.myMarkers[id].model.puid]) {

                bobj = $scope.myPoisHashT[$scope.myMarkers[id].model.puid];

                if (!bobj || !bobj.model || !bobj.model.puid) {
                    _err("No valid POI selected to be deleted.");
                    return;
                }

            } else {
                _err("No valid POI selected to be deleted.");
                return;
            }
        }

        // check that the pois is not connected to any connection and abort if it
        var cf;
        var lf = $scope.myConnectionsHashT;
        for (var cuid in lf) {
            if (lf.hasOwnProperty(cuid)) {
                cf = lf[cuid];
                if (cf.pois_a == bobj.model.puid || cf.pois_b == bobj.model.puid) {
                    // abort deletion since there are connections to the pois
                    _err("Please delete all the connections attached to this POI first.");
                    return;
                }
            }
        }

        var obj = bobj.model;
        obj.username = $scope.creds.username;
        obj.password = $scope.creds.password;

        obj.owner_id = $scope.owner_id;

        var delPuid = obj.puid;
        // make the request at AnyplaceAPI

        if (obj.is_building_entrance === true || obj.is_building_entrance === 'true') {
            obj.is_building_entrance = "true";
        } else {
            obj.is_building_entrance = "false";
        }

        var promise = $scope.anyAPI.deletePois(obj);

        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                if (obj.is_building_entrance === "true") {
                    obj.is_building_entrance = true;
                } else {
                    obj.is_building_entrance = false;
                }

                // delete the building from the loadedBuildings
                var lp = $scope.myPois;
                var sz = lp.length;
                for (var i = 0; i < sz; i++) {
                    if (lp[i].puid == delPuid) {
                        lp.splice(i, 1); // delete the pois from the loadPois list

                        if ($scope.myPoisHashT[delPuid] && $scope.myPoisHashT[delPuid].marker) {
                            $scope.myPoisHashT[delPuid].marker.setMap(null);
                        }
                        delete $scope.myPoisHashT[delPuid]; // delete the POIS from the hashtable
                        $scope.anyService.setAllPois($scope.myPoisHashT);
                        break;
                    }
                }
                _suc("Successfully deleted POI.");
            },
            function (resp) {
                // on error
                var data = resp.data;

                var lp = $scope.myPois;
                var sz = lp.length;
                for (var i = 0; i < sz; i++) {
                    if (lp[i].puid == delPuid) {
                        lp.splice(i, 1); // delete the pois from the loadPois list

                        if ($scope.myPoisHashT[delPuid] && $scope.myPoisHashT[delPuid].marker) {
                            $scope.myPoisHashT[delPuid].marker.setMap(null);
                        }
                        delete $scope.myPoisHashT[delPuid]; // delete the POIS from the hashtable
                        $scope.anyService.setAllPois($scope.myPoisHashT);
                        break;
                    }
                }

                _err("Something went wrong while deleting POI.");
            });

    };

    $scope.updatePoi = function (id) {
        var bobj = $scope.myPoisHashT[id];

        if (!bobj || !bobj.model || !bobj.model.puid) {
            if ($scope.myPoisHashT && $scope.myMarkers && $scope.myMarkers[id] && $scope.myMarkers[id].model && $scope.myPoisHashT[$scope.myMarkers[id].model.puid]) {

                bobj = $scope.myPoisHashT[$scope.myMarkers[id].model.puid];

                if (!bobj || !bobj.model || !bobj.model.puid) {
                    _err("No valid POI selected to be updated.");
                    return;
                }

            } else {
                _err("No valid POI selected to be updated.");
                return;
            }
        }

        if (bobj.model.pois_type2.localeCompare("") != 0) {
            bobj.model.pois_type = bobj.model.pois_type2;
        }

        var obj = bobj.model;
        obj.username = $scope.creds.username;
        obj.password = $scope.creds.password;
        obj.owner_id = $scope.owner_id;

        var marker = bobj.marker;
        if (marker) {
            var latLng = marker.position;
            if (latLng && latLng.lat() && latLng.lng()) {
                obj.coordinates_lat = String(latLng.lat());
                obj.coordinates_lon = String(latLng.lng());
            }
        }

        if (obj.is_building_entrance) {
            obj.is_building_entrance = 'true';
        } else {
            obj.is_building_entrance = 'false';
        }

        // make the request at AnyplaceAPI
        var promise = $scope.anyAPI.updatePois(obj);
        promise.then(
            function (resp) {
                // success
                var data = resp.data;

                if (marker) {
                    marker.infowindow.setMap(null);
                }

                if (obj.is_building_entrance == 'true') {
                    obj.is_building_entrance = true;
                } else {
                    obj.is_building_entrance = false;
                }

                for (var c in $scope.myConnectionsHashT) {
                    if ($scope.myConnectionsHashT.hasOwnProperty(c)) {
                        if ($scope.myConnectionsHashT[c].pois_a == obj.puid
                            || $scope.myConnectionsHashT[c].pois_b == obj.puid) {

                            var pa = $scope.myConnectionsHashT[c].pois_a;
                            var pb = $scope.myConnectionsHashT[c].pois_b;

                            var flightPlanCoordinates = [
                                new google.maps.LatLng($scope.myPoisHashT[pa].model.coordinates_lat, $scope.myPoisHashT[pa].model.coordinates_lon),
                                new google.maps.LatLng($scope.myPoisHashT[pb].model.coordinates_lat, $scope.myPoisHashT[pb].model.coordinates_lon)
                            ];

                            $scope.myConnectionsHashT[c].polyLine.setPath(flightPlanCoordinates);
                            $scope.anyService.setAllConnection($scope.myConnectionsHashT);
                            $scope.anyService.setAllPois($scope.myPoisHashT);
                        }
                    }
                }

                //_suc("Successfully updated POI.");
            },
            function (resp) {
                // error
                var data = resp.data;
                _err("Something went wrong while updating POI. Please refresh and try again.");
            }
        );
    };

    $scope.updatePoiPosition = function (marker) {

        var obj;

        if (marker && marker.model) {
            obj = marker.model;
        }

        obj.username = $scope.creds.username;
        obj.password = $scope.creds.password;
        obj.owner_id = $scope.owner_id;

        var latLng = marker.position;
        if (latLng && latLng.lat() && latLng.lng()) {
            obj.coordinates_lat = String(latLng.lat());
            obj.coordinates_lon = String(latLng.lng());
        }

        if (obj.is_building_entrance) {
            obj.is_building_entrance = 'true';
        } else {
            obj.is_building_entrance = 'false';
        }

        // make the request at AnyplaceAPI
        var promise = $scope.anyAPI.updatePois(obj);
        promise.then(
            function (resp) {
                // success
                var data = resp.data;

                if (obj.is_building_entrance == 'true' || obj.is_building_entrance === true) {
                    obj.is_building_entrance = true;
                } else {
                    obj.is_building_entrance = false;
                }

                for (var c in $scope.myConnectionsHashT) {
                    if ($scope.myConnectionsHashT.hasOwnProperty(c)) {
                        if ($scope.myConnectionsHashT[c].pois_a == marker.model.puid
                            || $scope.myConnectionsHashT[c].pois_b == marker.model.puid) {

                            var pa = $scope.myConnectionsHashT[c].pois_a;
                            var pb = $scope.myConnectionsHashT[c].pois_b;

                            var flightPlanCoordinates = [
                                new google.maps.LatLng($scope.myPoisHashT[pa].model.coordinates_lat, $scope.myPoisHashT[pa].model.coordinates_lon),
                                new google.maps.LatLng($scope.myPoisHashT[pb].model.coordinates_lat, $scope.myPoisHashT[pb].model.coordinates_lon)
                            ];

                            $scope.myConnectionsHashT[c].polyLine.setPath(flightPlanCoordinates);
                            $scope.anyService.setAllConnection($scope.myConnectionsHashT);
                            $scope.anyService.setAllPois($scope.myPoisHashT);
                        }
                    }
                }

            },
            function (resp) {
                // error
                var data = resp.data;
                _err("Something went wrong while moving POI.");
            }
        );
    };

    $scope.removeMarker = function (id) {
        if ($scope.myMarkers[id]) {
            $scope.myMarkers[id].marker.setMap(null);
            delete $scope.myMarkers[id];
        } else {
            _err("It seems that the marker to be deleted does not exist.");
        }
    };

    $scope.placeMarker = function (location, iconImage, size, type) {

        var marker = new google.maps.Marker({
            position: location,
            map: GMapService.gmap,
            icon: new google.maps.MarkerImage(
                iconImage,
                null, /* size is determined at runtime */
                null, /* origin is 0,0 */
                null, /* anchor is bottom center of the scaled image */
                size
            ),
            draggable: true
        });

        if (AnyplaceService.getBuildingId() === null || AnyplaceService.getBuildingId() === undefined) {
            _err("It seems there is no building selected. Please refresh.");
            return;
        }

        var infowindow = new google.maps.InfoWindow({
            content: '-',
            maxWidth: 500
        });

        $scope.$apply(function () {
            marker.myId = $scope.myMarkerId;
            $scope.myMarkers[marker.myId] = {};
            $scope.myMarkers[marker.myId].model = {
                buid: AnyplaceService.getBuildingId(),
                floor_number: AnyplaceService.getFloorNumber(),
                floor_name: AnyplaceService.getFloorName(),
                is_door: "false",
                is_building_entrance: "false",
                is_published: "true",
                description: type === 'connector' ? "Connector" : "",
                name: type === 'connector' ? "Connector" : "",
                pois_type: type === 'connector' ? "None" : "",
                pois_type2: type === 'connector' ? "None" : "",
                url: "-",
                image: "url_to_pois_image"
            };
            $scope.myMarkers[marker.myId].marker = marker;
            $scope.myMarkers[marker.myId].model.pois_type2 = $scope.myMarkers[marker.myId].model.pois_type;
            $scope.myMarkerId++;
        });

        var htmlContent = '<div class="infowindow-scroll-fix" ng-keydown="onInfoWindowKeyDown($event)">'
            + '<form name="poiForm">'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.name" id="poi-name" type="text" class="form-control" placeholder="poi name" tabindex="1" autofocus/>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<textarea ng-model="myMarkers[' + marker.myId + '].model.description" id="poi-description" type="text" class="form-control" placeholder="poi description" tabindex="5"></textarea>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<select ng-model="myMarkers[' + marker.myId + '].model.pois_type" class="form-control" ng-options="type for type in poisTypes" title="POI Types" tabindex="2">'
            + '<option value="">Select POI Type</option>'
            + '</select>'
            + '</fieldset class="form-group">'
            + '<fieldset class="form-group">Or ender your one type name:'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.pois_type2" id="poi-pois_type2" type="text" class="form-control" placeholder="POI Type" tabindex="2">'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.is_building_entrance" id="poi-entrance" type="checkbox" tabindex="4"><span> is building entrance?</span>'
            + '</fieldset>'
            + '<div style="text-align: center;">'
            + '<fieldset class="form-group" style="display: inline-block; width: 75%;">'
            + '<button type="submit" class="btn btn-success add-any-button" ng-click="addPoi(' + marker.myId + ')" tabindex="3"><span class="glyphicon glyphicon-plus"></span> Add'
            + '</button>'
            + '</fieldset>'
            + '<fieldset class="form-group" style="display: inline-block;width: 23%;">'
            + '<button type="submit" class="btn btn-danger add-any-button" style="margin-left:2px" ng-click="deleteTempPoi(' + marker.myId + ')" tabindex="6"><span class="glyphicon glyphicon-remove"></span>'
            + '</button>'
            + '</fieldset>'
            + '</div>'
            + '</form>'
            + '</div>';

        //var htmlContent2 = '<div class="infowindow-scroll-fix">'
        //    + '<h5 style="margin: 0">Name:</h5>'
        //    + '<span>{{myMarkers[' + marker.myId + '].model.name}}</span>'
        //    + '<h5 style="margin: 8px 0 0 0">Description:</h5>'
        //    + '<span>{{myMarkers[' + marker.myId + '].model.description}}</span>'
        //    + '<h5 style="margin: 8px 0 0 0">Type:</h5>'
        //    + '<span>{{myMarkers[' + marker.myId + '].model.pois_type}}</span>'
        //    + '</div>';
        var htmlContent2 = '<div class="infowindow-scroll-fix" ng-keydown="onInfoWindowKeyDown($event)">'
            + '<form name="poiForm">'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.name" id="poi-name" type="text" class="form-control" placeholder="poi name" tabindex="1" autofocus/>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<textarea ng-model="myMarkers[' + marker.myId + '].model.description" id="poi-description" type="text" class="form-control" placeholder="poi description" tabindex="5"></textarea>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<select ng-model="myMarkers[' + marker.myId + '].model.pois_type" class="form-control" ng-options="type for type in poisTypes" title="POI Types" tabindex="2">'
            + '<option value="">Select POI Type</option>'
            + '</select>'
            + '</fieldset class="form-group">'
            + '<fieldset class="form-group">Or ender your one type name:'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.pois_type" id="poi-pois_type2" type="text" class="form-control" placeholder="POI Type" tabindex="2">'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.is_building_entrance" id="poi-entrance" type="checkbox" tabindex="4"><span> is building entrance?</span>'
            + '</fieldset>'
            + '<div style="text-align: center;">'
            + '<fieldset class="form-group" style="display: inline-block; width: 75%;">'
            + '<button type="submit" class="btn btn-success add-any-button" ng-click="updatePoi(' + marker.myId + ')" tabindex="3"><span class="glyphicon glyphicon-pencil"></span> Update'
            + '</button>'
            + '</fieldset>'
            + '<fieldset class="form-group" style="display: inline-block;width: 23%;">'
            + '<button type="submit" class="btn btn-danger add-any-button" style="margin-left:2px" ng-click="deletePoi(' + marker.myId + ')" tabindex="6"><span class="glyphicon glyphicon-remove"></span>'
            + '</button>'
            + '</fieldset>'
            + '</div>'
            + '</form>'
            + '</div>';

        var htmlConnector = '<div class="infowindow-scroll-fix" style="text-align: center; width:170px">'
            + '<div style="margin-bottom: 5px">POI Connector</div>'
            + '<fieldset class="form-group" style="display: inline-block; width: 73%;">'
            + '<button type="submit" class="btn btn-success add-any-button" ng-click="addPoi(' + marker.myId + ')"><span class="glyphicon glyphicon-plus"></span> Add'
            + '</button>'
            + '</fieldset>'
            + '<fieldset class="form-group" style="display: inline-block;width: 23%;">'
            + '<button type="submit" class="btn btn-danger add-any-button" style="margin-left:2px" ng-click="deleteTempPoi(' + marker.myId + ')"><span class="glyphicon glyphicon-remove"></span>'
            + '</button>'
            + '</fieldset>'
            + '</div>';

        var htmlConnector2 = '<div class="infowindow-scroll-fix" style="text-align: center; width:170px">'
            + '<div style="margin-bottom: 5px">POI Connector</div>'
            + '<fieldset class="form-group">'
            + '<button type="submit" class="btn btn-danger add-any-button" style="margin-left:2px" ng-click="deletePoi(' + marker.myId + ')"><span class="glyphicon glyphicon-remove"></span> Remove'
            + '</button>'
            + '</fieldset>'
            + '</div>';

        if (type == 'poi') {
            var tpl = $compile(htmlContent)($scope);
            marker.tpl2 = $compile(htmlContent2)($scope);
            infowindow.setContent(tpl[0]);
            infowindow.open(GMapService.gmap, marker);
        } else if (type == 'connector') {
            var tplConn = $compile(htmlConnector)($scope);
            marker.tpl2 = $compile(htmlConnector2)($scope);
            infowindow.setContent(tplConn[0]);
            //infowindow.open(GMapService.gmap, marker);

            $scope.addPoi(marker.myId);
        }

        $scope.$apply(
            $scope.myMarkers[marker.myId].infowindow = infowindow
        );

        google.maps.event.addListener(marker, 'click', function () {
            if ($scope.edgeMode) {
                $scope.$apply(_warn("Only submitted objects can be connected together."));
            }
            infowindow.open(GMapService.gmap, marker);
        });


    };

    $scope.deleteTempPoi = function (i) {
        if (!$scope.myMarkers || !$scope.myMarkers[i].marker) {
            _err("No valid POI marker to delete found.");
            return;
        }
        $scope.myMarkers[i].marker.setMap(null);
    };

    $scope.toggleEdgeMode = function () {
        $scope.edgeMode = !$scope.edgeMode;
        if (!$scope.edgeMode) {
            $scope.connectPois.prev = undefined;
        }
    };

    $("#draggable-poi").draggable({
        helper: 'clone',
        stop: function (e) {
            var point = new google.maps.Point(e.pageX, e.pageY);
            var ll = overlay.getProjection().fromContainerPixelToLatLng(point);
            if (!_isPoiNearFloor(ll)) {
                $scope.$apply(_warn("The marker was placed too far away from the selected building."));
                return;
            }
            $scope.placeMarker(ll, _POI_NEW_IMG, new google.maps.Size(21, 32), 'poi');
        }
    });

    $("#draggable-connector").draggable({
        helper: 'clone',
        stop: function (e) {
            var point = new google.maps.Point(e.pageX, e.pageY);
            var ll = overlay.getProjection().fromContainerPixelToLatLng(point);
            if (!_isPoiNearFloor(ll)) {
                $scope.$apply(_warn("The marker was placed too far away from the selected building."));
                return;
            }
            $scope.placeMarker(ll, _POI_CONNECTOR_IMG, new google.maps.Size(21, 21), 'connector');
        }
    });

}
])
;
/**
 *
 The MIT License (MIT)

 Copyright (c) 2015, Marileni Angelidou, Data Management Systems Laboratory (DMSL)
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


app.controller('SelectFloorController', ['$scope', 'AnyplaceService', 'GMapService', 'AnyplaceAPIService', function ($scope, AnyplaceService, GMapService, AnyplaceAPIService) {
    $scope.anyService = AnyplaceService;
    $scope.anyAPI = AnyplaceAPIService;
    $scope.gmapService = GMapService;
    $scope.xFloors = [];

    $scope.$watch('anyService.availableFloors', function (newVal, oldVal) {
        if (newVal) {
            $scope.xFloors=[];
            $scope.xFloors=$scope.anyService.availableFloors;
        }
    });

    var _err = function (msg) {
        $scope.anyService.addAlert('danger', msg);
    };

    $scope.orderByFloorNo = function (floor) {
        if (!floor || LPUtils.isNullOrUndefined(floor.floor_number)) {
            return 0;
        }
        return parseInt(floor.floor_number);
    };



    $scope.floorUp = function () {
        //here new
        changedfloor = true;
        var next;
        for (var i = 0; i < $scope.xFloors.length; i++) {
            next = i + 1;
            if ($scope.xFloors[i].floor_number === $scope.anyService.selectedFloor.floor_number) {

                if (next < $scope.xFloors.length) {
                    $scope.anyService.selectedFloor = $scope.xFloors[next];
                    return;
                } else {
                    //_warn("There is no other floor above.");
                    return;
                }
            }
        }

        _err("Floor not found.");
    };

    $scope.floorDown = function () {

        changedfloor = true;
        var prev;
        for (var i = 0; i < $scope.xFloors.length; i++) {
            prev = i - 1;
            if ($scope.xFloors[i].floor_number === $scope.anyService.selectedFloor.floor_number) {

                if (prev >= 0) {
                    $scope.anyService.selectedFloor = $scope.xFloors[prev];
                    return;
                } else {
                    //_warn("There is no other floor below.");
                    return;
                }
            }
        }

        _err("Floor not found.");
    };


}]);
/**
 *
 The MIT License (MIT)

 Copyright (c) 2015, Marileni Angelidou, Loukas Solea , Data Management Systems Laboratory (DMSL)
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



var heatMap = [];


var heatMap_Location = [];


var APmap = [];
var heatmap;
var heatmapFingerprints = [];
var userTimeData = [];
var fingerPrintsMap = [];
var heatmapAcc;
var connectionsMap = {};
var POIsMap = {};
var drawingManager;
var _HEATMAP_RSS_IS_ON = false;
var _HEATMAP_Localization = false; //lsolea01
var _APs_IS_ON = false;
var _FINGERPRINTS_IS_ON = false;
var _DELETE_FINGERPRINTS_IS_ON = false;
var _HEATMAP_F_IS_ON = false;
var _CONNECTIONS_IS_ON = false;
var _POIS_IS_ON = false;
var changedfloor = false;
var colorBarGreenClicked = false;
var colorBarYellowClicked = false;
var colorBarOrangeClicked = false;
var colorBarPurpleClicked = false;
var colorBarRedClicked = false;
var levelOfZoom = 1;


app.controller('BuildingController', ['$cookieStore', '$scope', '$compile', 'GMapService', 'AnyplaceService', 'AnyplaceAPIService', function ($cookieStore, $scope, $compile, GMapService, AnyplaceService, AnyplaceAPIService) {

    $scope.myMarkers = {};
    $scope.myMarkerId = 0;
    $scope.laship=33.0000;
    $scope.loship=33.0000;
    $scope.moveShip=0;
    $scope.ShipInterval;
    $scope.gmapService = GMapService;
    $scope.anyService = AnyplaceService;
    $scope.anyAPI = AnyplaceAPIService;
    $scope.example9model = [];
    $scope.example9data = [];
    $scope.example9settings = {enableSearch: true, scrollable: true};

    $scope.example9modeledit = [];
    $scope.example9dataedit = [];
    $scope.example9settingsedit = {enableSearch: true, scrollable: true};

    $scope.myBuildings = [];
    $scope.myShips = [];



    $scope.myBuildingsHashT = {};
    $scope.myCampus = [];
    $scope.old_campus = [];

    $scope.crudTabSelected = 1;

    $scope.fileToUpload = "";
    $scope.logfile = "";

    $scope.poisTypes = {};
    $scope.catTypes = {};

    $scope.pageLoad = false;

    $scope.crudTabSelected = 1;


//     // Replace this with your URL.
//     var BUILDING_TILE_URL = AnyplaceAPI.FULL_SERVER + '/floortiles/{building}/{floor}/{z}/z{z}x{x}y{y}.png';
//
// // Name the layer anything you like.
//     var layerID = 'building_layer';
//
// // Create a new ImageMapType layer. static_tiles/19/z19x310801y207411.png
//     var maptiler = new google.maps.ImageMapType({
//         name: layerID,
//         getTileUrl: function (coord, zoom) {
//             var buid = $scope.anyService.getBuildingId();
//
//             var floor = $scope.anyService.getFloorNumber();
//             var url = BUILDING_TILE_URL
//                 .replace('{building}', buid)
//                 .replace('{floor}', floor)
//                 .replace('{z}', zoom)
//                 .replace('{x}', coord.x)
//                 .replace('{y}', coord.y)
//                 .replace('{z}', zoom);
//             return url;
//         },
//         tileSize: new google.maps.Size(256, 256),
//         isPng: true
//     });
//
//     // Register the new layer, then activate it.
//     $scope.gmapService.gmap.overlayMapTypes.insertAt(0, maptiler);

    $scope.setCrudTabSelected = function (n) {

        $scope.crudTabSelected = n;
        if (!$scope.anyService.getBuilding()) {
            _err("No building is selected.");
            return;
        }

        var b = $scope.myBuildingsHashT[$scope.anyService.getBuildingId()];



        if (!b) {
            return;
        }

        var m = b.marker;
        if (!m) {
            return;
        }
        // edit building
        if (n === 2) {
            m.setDraggable(true);
        } else {
            m.setDraggable(false);
        }

    };
    $scope.isCrudTabSelected = function (n) {
        return $scope.crudTabSelected === n;
    };

    $scope.$on("loggedIn", function (event, mass) {
        //_suc('Successfully logged in.');
        $scope.fetchAllBuildings();
        $scope.fetchAllCampus();
        //$scope.fetchAllPoisTypes();
    });

    $scope.$on("loggedOff", function (event, mass) {
        _clearBuildingMarkersAndModels();
        $scope.myMarkers = {};
        $scope.myMarkerId = 0;
        $scope.myBuildings = [];
        $scope.myBuildingsHashT = {};
    });

    /**
     * @return {string}
     */
    function S4() {
        return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
    }

    var guid = (S4() + S4() + "-" + S4() + "-4" + S4().substr(0, 3) + "-" + S4() + "-" + S4() + S4() + S4()).toLowerCase();
    var d = new Date();
    document.getElementById("CampusID").value = "cuid_" + guid + "_" + d.getTime();

    var logoPlanInputElement = $('#input-logo');

    logoPlanInputElement.change(function handleImage(e) {
        var reader = new FileReader();
        reader.onload = function (event) {
            var imgObj = new Image();
            imgObj.src = event.target.result;
        };
        reader.readAsDataURL(e.target.files[0]);
    });

    $scope.setLogoPlan = function (cuid) {

        var newFl = {
            is_published: 'true',
            cuid: cuid,
            logo: String($scope.newFloorNumber),
            description: String($scope.newFloorNumber),
            floor_number: String($scope.newFloorNumber)
        };

        $scope.myFloors[$scope.myFloorId] = newFl;
        $scope.myFloorId++;

        // create the proper image inside the canvas
        canvasOverlay.drawBoundingCanvas();

        // create the ground overlay and destroy the canvasOverlay object
        // and also set the floor_plan_coords in $scope.data
        var bl = canvasOverlay.bottom_left_coords;
        var tr = canvasOverlay.top_right_coords;
        $scope.data.floor_plan_coords.bottom_left_lat = bl.lat();
        $scope.data.floor_plan_coords.bottom_left_lng = bl.lng();
        $scope.data.floor_plan_coords.top_right_lat = tr.lat();
        $scope.data.floor_plan_coords.top_right_lng = tr.lng();
        $scope.data.floor_plan_coords.zoom = GMapService.gmap.getZoom() + "";
        var data = canvasOverlay.getCanvas().toDataURL("image/png"); // defaults to png
        $scope.data.floor_plan_base64_data = data;
        var imageBounds = new google.maps.LatLngBounds(
            new google.maps.LatLng(bl.lat(), bl.lng()),
            new google.maps.LatLng(tr.lat(), tr.lng()));
        $scope.data.floor_plan_groundOverlay = new USGSOverlay(imageBounds, data, GMapService.gmap);

        canvasOverlay.setMap(null); // remove the canvas overlay since the groundoverlay is placed
        $('#input-floor-plan').prop('disabled', false);
        $scope.isCanvasOverlayActive = false;

        if (_floorNoExists($scope.newFloorNumber)) {
            for (var i = 0; i < $scope.xFloors.length; i++) {
                var f = $scope.xFloors[i];
                if (!LPUtils.isNullOrUndefined(f)) {
                    if (f.floor_number === String($scope.newFloorNumber)) {
                        $scope.uploadFloorPlanBase64($scope.anyService.selectedBuilding, f, $scope.data);
                        break;
                    }
                }
            }
        } else {
            $scope.addFloorObject(newFl, $scope.anyService.selectedBuilding, $scope.data);
        }

    };

    $scope.$watch('anyService.selectedBuilding', function (newVal, oldVal) {
        if (newVal && newVal.coordinates_lat && newVal.coordinates_lon) {
            // Pan map to selected building
            $scope.gmapService.gmap.panTo(_latLngFromBuilding(newVal));
            $scope.gmapService.gmap.setZoom(19);
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem("lastBuilding", newVal.buid);
            }
        }
        //if (newVal && newVal.buid && newVal.poistypeid) {
        // $scope.fetchAllPoisTypes(newVal.poistypeid);
        // }
        // else {
        $scope.poisTypes = [
            "Disabled Toilets",
            "Elevator",
            "Entrance",
            "Fire Extinguisher",
            "First Aid/AED",
            "Kitchen",
            "Office",
            "Ramp",
            "Room",
            "Security/Guard",
            "Stair",
            "Toilets",
            "Other"
        ];
        //}
    });

    $scope.$watch('anyService.selectedCampus', function (newVal, oldVal) {
        if (newVal && newVal.cuid) {
            // Pan map to selected building
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem("lastCampus", newVal.cuid);
            }
        }

    });


    var _clearBuildingMarkersAndModels = function () {
        for (var b in $scope.myBuildingsHashT) {
            if ($scope.myBuildingsHashT.hasOwnProperty(b)) {
                $scope.myBuildingsHashT[b].marker.setMap(null);
                delete $scope.myBuildingsHashT[b];
            }
        }
    };

    var _err = function (msg) {
        $scope.anyService.addAlert('danger', msg);
    };

    var _suc = function (msg) {
        $scope.anyService.addAlert('success', msg);
    };

    var _latLngFromBuilding = function (b) {
        if (b && b.coordinates_lat && b.coordinates_lon) {
            return {
                lat: parseFloat(b.coordinates_lat),
                lng: parseFloat(b.coordinates_lon)
            }
        }
        return undefined;
    };
    $scope.fetchAllPoisTypes = function () {

        var jsonReq = {};

        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;
        jsonReq.access_token = $scope.gAuth.access_token;

        jsonReq.owner_id = $scope.owner_id;

        if (!jsonReq.owner_id) {
            _err("Could nor authorize user. Please refresh.1");
            return;
        }
        var promise = $scope.anyAPI.retrievePoisTypes(jsonReq);
        promise.then(
            function (resp) {
                var data = resp.data;
                $scope.catTypes = data.poistypes;
            },
            function (resp) {
                var data = resp.data;
                _err("Something went wrong while fetching POIs types");
            }
        );
    };


    $scope.fetchAllBuildings = function () {
        var jsonReq = {};
        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;
        jsonReq.owner_id = $scope.owner_id;

        if (!jsonReq.owner_id) {
            _err("Could nor authorize user. Please refresh.");
            return;
        }

        var promise = $scope.anyAPI.allBuildings(jsonReq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                $scope.myBuildings = [];
                $scope.myShips = [];

                //var bs = JSON.parse( data.buildings );
                for(var i in data.buildings){
                    var bid=data.buildings[i].buid;
                    var getship=bid.split("_",1);

                    if(getship=="ship") {
                        $scope.myShips.push(data.buildings[i]);
                    }
                    else{
                        $scope.myBuildings.push(data.buildings[i]);
                    }

                }



                var infowindow = new google.maps.InfoWindow({
                    content: '-',
                    maxWidth: 500
                });

                var localStoredBuildingIndex = -1;
                var localStoredBuildingId = undefined;
                if (typeof(Storage) !== "undefined" && localStorage && localStorage.getItem('lastBuilding')) {
                    localStoredBuildingId = localStorage.getItem('lastBuilding');
                }


                //show all buildings and ships on map
                for (var i = 0; i < data.buildings.length; i++) {

                    var b = data.buildings[i];

                    $scope.example9data[i] = {id: b.buid, label: b.name};
                    $scope.example9dataedit[i] = {id: b.buid, label: b.name};

                    if (localStoredBuildingId && localStoredBuildingId === b.buid) {
                        localStoredBuildingIndex = i;
                    }

                    if (b.is_published === 'true' || b.is_published == true) {
                        b.is_published = true;
                    } else {
                        b.is_published = false;
                    }
                    var bid=b.buid;
                    var getship=bid.split("_",1);

                    if(getship=="ship") {
                        var marker = new google.maps.Marker({
                            position: _latLngFromBuilding(b),
                            map: GMapService.gmap,
                            icon: new google.maps.MarkerImage(
                                'build/images/ship_icon.png',
                                null, /* size is determined at runtime */
                                null, /* origin is 0,0 */
                                null, /* anchor is bottom center of the scaled image */
                                new google.maps.Size(54, 54)),
                            draggable: false
                        });


                        var htmlContent = '<div class="infowindow-scroll-fix">'
                            + '<h5>Ship:</h5>'
                            + '<span>' + b.name + '</span>'
                            + '<h5>Description:</h5>'
                            + '<textarea class="infowindow-text-area"  rows="3" readonly>' + b.description + '</textarea>'
                            + '</div>';




                    }
                    else {
                        var marker = new google.maps.Marker({
                            position: _latLngFromBuilding(b),
                            map: GMapService.gmap,
                            icon: new google.maps.MarkerImage(
                                'build/images/building-icon.png',
                                null, /* size is determined at runtime */
                                null, /* origin is 0,0 */
                                null, /* anchor is bottom center of the scaled image */
                                new google.maps.Size(54, 54)),
                            draggable: false
                        });



                        var htmlContent = '<div class="infowindow-scroll-fix">'
                            + '<h5>Building:</h5>'
                            + '<span>' + b.name + '</span>'
                            + '<h5>Description:</h5>'
                            + '<textarea class="infowindow-text-area"  rows="3" readonly>' + b.description + '</textarea>'
                            + '</div>';

                    }



                    marker.infoContent = htmlContent;
                    marker.building = b;

                    $scope.myBuildingsHashT[b.buid] = {
                        marker: marker,
                        model: b
                    };

                    google.maps.event.addListener(marker, 'click', function () {
                        infowindow.setContent(this.infoContent);
                        infowindow.open(GMapService.gmap, this);
                        var self = this;
                        // $scope.$apply(function () {
                        $scope.anyService.selectedBuilding = self.building;
                        var bid=self.building.buid;
                        var getship=bid.split("_",1);


                        if(getship=="ship") {
                            if($scope.moveShip==0){
                                $scope.moveShip=1;
                                console.log("stop");
                            }
                            else {
                                $scope.moveShip=0;
                            }

                            //ship here


                            function myFunction(){
                                var b = $scope.myBuildingsHashT[bid];

                                var m = b.marker;
                                $scope.laship=parseFloat($scope.laship) + 0.0001;
                                $scope.loship=parseFloat($scope.loship) + 0.0001;
                                var myLatLng = {lat: parseFloat($scope.laship), lng: parseFloat($scope.loship)};

                                m.setPosition(myLatLng);

                                console.log("its work");



                            }

                            if ($scope.moveShip==0) {
                                clearInterval($scope.ShipInterval);
                                var model=$scope.myBuildingsHashT[bid];
                                console.log($scope.myBuildingsHashT[bid]);
                                console.log(model.model.coordinates_lat);

                                var myLatLng = {lat: parseFloat(model.model.coordinates_lat), lng: parseFloat(model.model.coordinates_lon)};
                                console.log(myLatLng);
                                model.marker.setPosition(myLatLng);
                                // console.log(b);
                            }
                            else{
                                myFunction();

                                $scope.ShipInterval=setInterval(function(){
                                    myFunction()}, 10000);

                            }


                            // Open a new connection, using the GET request on the URL endpoint
                            // request.open('GET', 'ship.json', true);
                            // request.send(null);
                            // request.onreadystatechange = function(){

                            //     //check if the status is 200(means everything is okay)
                            //     if (this.status === 200&& this.readyState==4) {
                            //         //return server response as an object with JSON.parse
                            //
                            //
                            //          json=JSON.parse(request.responseText);
                            //         console.log(json[0].LAT);



                            // var json=JSON.parse(request.responseText);
                            //
                            // console.log(request.responseText);
                            //  var myLatLng = {lat: parseFloat(json[0].LAT), lng:parseFloat( json[0].LON)};






                            // console.log(JSON.parse(this.request));
                            // }
                            // }

                            // Send request











                            // }

                            // for (var b in $scope.myBuildingsHashT) {
                            //         $scope.myBuildingsHashT[b].marker.setMap();
                            //
                            // }

                        }


                        // });



                    });

                    console.log("dfgg ");
                }

                // using the latest building form localStorage
                if (localStoredBuildingIndex >= 0) {
                    $scope.anyService.selectedBuilding = $scope.myBuildings[localStoredBuildingIndex];
                } else if ($scope.myBuildings[0]) {
                    $scope.anyService.selectedBuilding = $scope.myBuildings[0];
                }

                // _suc('Successfully fetched buildings.');
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err('Something went wrong while fetching buildings.');
            }
        );
    };

    $scope.addNewBuilding = function (id) {

        if ($scope.myMarkers[id] && $scope.myMarkers[id].marker) {

            var building = $scope.myMarkers[id].model;

            // set owner id
            building.owner_id = $scope.owner_id;

            if (!building.owner_id) {
                _err("Could not authorize user. Please refresh.");
                return;
            }


            building.coordinates_lat = String($scope.myMarkers[id].marker.position.lat());
            building.coordinates_lon = String($scope.myMarkers[id].marker.position.lng());

            if (building.coordinates_lat === undefined || building.coordinates_lat === null) {
                _err("Invalid building latitude.");
                return;
            }

            if (building.coordinates_lon === undefined || building.coordinates_lon === null) {
                _err("Invalid building longitude.");
                return;
            }

            if (building.is_published === true) {
                building.is_published = "true";
            } else {
                building.is_published = "false";
            }

            if (!building.description) {
                building.description = "-";
            }

            if (building.owner_id && building.name && building.description && building.is_published && building.url && building.address) {
                var promise ;
                var shipboolean;
                if (shipselect==0) {
                    promise = $scope.anyAPI.addBuilding(building);
                    shipboolean=1;
                }
                else{
                    promise = $scope.anyAPI.addShip(building)
                    shipboolean=0;
                }
                promise.then(
                    function (resp) {
                        // on success
                        var data = resp.data;
                        console.log("new buid: " + data.buid);
                        building.buid = data.buid;

                        if (building.is_published === 'true' || building.is_published == true) {
                            building.is_published = true;
                        } else {
                            building.is_published = false;
                        }

                        // insert the newly created building inside the loadedBuildings
                        if(shipselect==0) {
                            $scope.myBuildings.push(building);
                            $scope.anyService.selectedBuilding = $scope.myBuildings[$scope.myBuildings.length - 1];

                        }
                        else{
                            $scope.myShips.push(building);
                            // $scope.anyService.selectedBuilding = $scope.myBuildings[$scope.myBuildings.length - 1];


                        }

                        $scope.myMarkers[id].marker.setDraggable(false);

                        $scope.myBuildingsHashT[building.buid] = {
                            marker: $scope.myMarkers[id].marker,
                            model: building
                        };

                        if ($scope.myMarkers[id].infowindow) {
                            $scope.myMarkers[id].infowindow.setContent($scope.myMarkers[id].marker.tpl2[0]);
                            $scope.myMarkers[id].infowindow.close();
                        }
                        if(shipboolean){

                            _suc("Building added successfully.");
                        }
                        else {
                            _suc("Ship added successfully.");
                        }
                    },
                    function (resp) {
                        // on error
                        var data = resp.data;
                        _err("Something went wrong while adding the building. " + data.message);
                    }
                );


            } else {
                _err("Some required fields are missing.");
            }
        }
    };

    $scope.deleteBuilding = function () {

        var b = $scope.anyService.getBuilding();

        var reqObj = $scope.creds;

        if (!$scope.owner_id) {
            _err("Could not identify user. Please refresh and sign in again.");
            return;
        }

        reqObj.owner_id = $scope.owner_id;

        console.log( reqObj.owner_id);

        if (!b || !b.buid) {
            _err("No building selected for deletion.");
            return;
        }

        reqObj.buid = b.buid;

        var promise = $scope.anyAPI.deleteBuilding(reqObj);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                console.log("building deleted: ", b);

                // delete the building from the loadedBuildings
                $scope.myBuildingsHashT[b.buid].marker.setMap(null);
                delete $scope.myBuildingsHashT[b.buid];

                var bs = $scope.myBuildings;
                var sz = bs.length;
                for (var i = 0; i < sz; i++) {
                    if (bs[i].buid == b.buid) {
                        bs.splice(i, 1);
                        break;
                    }
                }

                // update the selected building
                if ($scope.myBuildings && $scope.myBuildings.length > 0) {
                    $scope.anyService.selectedBuilding = $scope.myBuildings[0];
                }

                $scope.setCrudTabSelected(1);

                _suc("Successfully deleted building.");
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err("Something went wrong. It's likely that everything related to the building is deleted but please refresh to make sure or try again.");
            }
        );

    };


    //lsolea01

    $scope.deleteRadiomaps = function () {

        var jsonReq = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber()};

        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;

        var promise = $scope.anyAPI.deleteRadiomaps(jsonReq);

        promise.then(
            function (resp) {

                _suc("Successfully Delete floor radiomap")


            },

            function (resp) {
                // on error
                var data = resp.data;
                _err("Something went wrong.The file is not exist ");
            }
        );


    };


    $scope.updateBuilding = function () {
        var b = $scope.anyService.getBuilding();

        if (LPUtils.isNullOrUndefined(b) || LPUtils.isNullOrUndefined(b.buid)) {
            _err("No building selected found.");
            return;
        }

        var reqObj = {};

        // from controlBarController
        reqObj = $scope.creds;
        if (!$scope.owner_id) {
            _err("Could not identify user. Please refresh and sign in again.");
            return;
        }

        reqObj.owner_id = $scope.owner_id;

        reqObj.buid = b.buid;

        if (b.description) {
            reqObj.description = b.description;
        }

        if (b.name) {
            reqObj.name = b.name;
        }

        if (b.is_published === true || b.is_published == "true") {
            reqObj.is_published = "true";
        } else {
            reqObj.is_published = "false";
        }

        if (b.bucode) {
            reqObj.bucode = b.bucode;
        }

        var marker = $scope.myBuildingsHashT[b.buid].marker;
        if (marker) {
            var latLng = marker.position;
            if (latLng && latLng.lat() && latLng.lng()) {
                reqObj.coordinates_lat = String(latLng.lat());
                reqObj.coordinates_lon = String(latLng.lng());
            }
        }

        var promise = $scope.anyAPI.updateBuilding(reqObj);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                if (b.is_published === 'true' || b.is_published == true) {
                    b.is_published = true;
                } else {
                    b.is_published = false;
                }

                _suc("Successfully updated building.")
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err("Something went wrong while updating building. " + data.message);
            }
        );

    };

    $scope.fetchAllCampus = function () {
        var jsonReq = {};
        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;
        jsonReq.owner_id = $scope.owner_id;
        $scope.myCampus = [];
        if (!jsonReq.owner_id) {
            _err("Could nor authorize user. Please refresh.");
            return;
        }

        var promise = $scope.anyAPI.allCampus(jsonReq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;
                //var bs = JSON.parse( data.buildings );
                $scope.myCampus = data.buildingsets;
                var localStoredCampusIndex = -1;
                var localStoredCampusId = undefined;
                if (typeof(Storage) !== "undefined" && localStorage && localStorage.getItem('lastCampus')) {
                    localStoredCampusId = localStorage.getItem('lastCampus');
                }

                for (var i = 0; i < $scope.myCampus.length; i++) {

                    var b = $scope.myCampus[i];

                    if (localStoredCampusId && localStoredCampusId === b.cuid) {
                        localStoredCampusIndex = i;
                    }
                }

                // using the latest building set form localStorage
                if (localStoredCampusIndex >= 0) {
                    $scope.anyService.selectedCampus = $scope.myCampus[localStoredCampusIndex];
                } else if ($scope.myCampus[0]) {
                    $scope.anyService.selectedCampus = $scope.myCampus[0];
                }
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err('Something went wrong while fetching buildings.');
            }
        );
    };

    $scope.updateCampus = function () {
        var b = $scope.anyService.getCampus();

        if (LPUtils.isNullOrUndefined(b) || LPUtils.isNullOrUndefined(b.cuid)) {
            _err("No campus selected found.");
            return;
        }

        var reqObj = {};

        // from controlBarController
        reqObj = $scope.creds;
        if (!$scope.owner_id) {
            _err("Could not identify user. Please refresh and sign in again.");
            return;
        }

        reqObj.owner_id = $scope.owner_id;

        reqObj.cuid = b.cuid;

        if (b.description) {
            reqObj.description = b.description;
        }

        if (b.name) {
            reqObj.name = b.name;
        }

        if (b.newcuid) {
            reqObj.newcuid = b.newcuid;
        }

        var sz = $scope.example9modeledit.length;

        if (sz == 0) {
            _err("No buildings selected.");
            return;
        }
        var buids = "\"buids\":[";
        for (var i = sz - 1; i > 0; i--) {
            buids = buids + "\"" + $scope.example9modeledit[i].id + "\",";
        }
        buids = buids + "\"" + $scope.example9modeledit[0].id + "\"]";

        reqObj.greeklish = document.getElementById("Greeklish-OnOffedit").checked;

        reqObj.buids = buids;


        var promise = $scope.anyAPI.updateCampus(reqObj);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;
                document.getElementById("CampusIDeditnew").value = "";
                _suc("Successfully updated campus.")
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err("Something went wrong while updating campus. " + data.message);
            }
        );

    };

    $scope.deleteCampus = function () {

        var b = $scope.anyService.getCampus();

        var reqObj = $scope.creds;

        if (!$scope.owner_id) {
            _err("Could not identify user. Please refresh and sign in again.");
            return;
        }

        reqObj.owner_id = $scope.owner_id;

        if (!b || !b.cuid) {
            _err("No Campus selected for deletion.");
            return;
        }

        reqObj.cuid = b.cuid;

        var promise = $scope.anyAPI.deleteCampus(reqObj);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                console.log("campus deleted: ", b);


                var bs = $scope.myCampus;
                var sz = bs.length;
                for (var i = 0; i < sz; i++) {
                    if (bs[i].cuid == b.cuid) {
                        bs.splice(i, 1);
                        break;
                    }
                }

                // update the selected building
                if ($scope.myCampus && $scope.myCampus.length > 0) {
                    $scope.anyService.selectedCampus = $scope.myCampus[0];
                }
                else if ($scope.myCampus.length == 0) {
                    $scope.anyService.selectedCampus = undefined;
                }

                $scope.setCrudTabSelected(1);

                _suc("Successfully deleted campus.");
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err("Something went wrong. It's likely that everything related to the campus is deleted but please refresh to make sure or try again.");
            }
        );

    };

    $scope.addCampus = function () {

        var name_element = document.getElementById("CampusName");
        var name = "\"name\":\"" + name_element.value + "\"";

        if (document.getElementById("CampusDescription").value.localeCompare("") == 0) {
            document.getElementById("CampusDescription").value = "-";
        }

        var des = document.getElementById("CampusDescription");
        var des = "\"description\":\"" + des.value + "\"";

        var mycuid = document.getElementById("CampusID");
        var mycuid = "\"cuid\":\"" + mycuid.value + "\"";

        var greeklish = document.getElementById("Greeklish-OnOff").checked;
        greeklish = "\"greeklish\":\"" + greeklish + "\"";
        var sz = $scope.example9model.length;

        if (sz == 0) {
            _err("No buildings selected.");
            return;
        }
        var buids = "\"buids\":[";
        for (var i = sz - 1; i > 0; i--) {
            buids = buids + "\"" + $scope.example9model[i].id + "\",";
        }
        buids = buids + "\"" + $scope.example9model[0].id + "\"]";

        var jreq = "{" + greeklish + "," + buids + "," + mycuid + "," + des + "," + name + ",\"owner_id\":\"" + $scope.owner_id + "\",\"access_token\":\"" + $scope.gAuth.access_token + "\"}";
        //alert(document.getElementById("Greeklish-OnOff").checked);
        var promise = $scope.anyAPI.addBuildingSet(jreq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;
                var new_campus = {};
                new_campus.name = document.getElementById("CampusName").value;
                new_campus.buids = buids.buids;
                new_campus.description = document.getElementById("CampusDescription").value;
                new_campus.cuid = data.cuid;
                $scope.myCampus.push(new_campus);
                $scope.anyService.selectedCampus = $scope.myCampus[$scope.myCampus.length - 1];
                _suc("Successfully added campus.");

                function S4() {
                    return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
                }

                var guid = (S4() + S4() + "-" + S4() + "-4" + S4().substr(0, 3) + "-" + S4() + "-" + S4() + S4() + S4()).toLowerCase();
                var d = new Date();
                document.getElementById("CampusID").value = "cuid_" + guid + "_" + d.getTime();
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err("Something went wrong while adding the building. " + data.message);
            }
        );

    };

    var overlay = new google.maps.OverlayView();
    overlay.draw = function () {
    };
    overlay.setMap(GMapService.gmap);

    $("#draggable-building").draggable({
        helper: 'clone',
        stop: function (e) {
            var point = new google.maps.Point(e.pageX, e.pageY);
            var ll = overlay.getProjection().fromContainerPixelToLatLng(point);
            $scope.placeMarker(ll);
            shipselect=0;
        }
    });

// ship drag
    $("#draggable-ship").draggable({

        helper: 'clone',
        stop: function (e) {
            var point = new google.maps.Point(e.pageX, e.pageY);
            var ll = overlay.getProjection().fromContainerPixelToLatLng(point);
            $scope.placeMarkership(ll);
            shipselect=1;
        }
    });

    $scope.placeMarkership = function (location) {

        var prevMarker = $scope.myMarkers[$scope.myMarkerId - 1];

        if (prevMarker && prevMarker.marker && prevMarker.marker.getMap() && prevMarker.marker.getDraggable()) {
            // TODO: alert for already pending building.
            console.log('there is a building pending, please add 1 at a time');
            return;
        }

        var marker = new google.maps.Marker({
            position: location,
            map: GMapService.gmap,
            icon: 'build/images/ship_icon.png',
            draggable: true
        });

        var infowindow = new google.maps.InfoWindow({
            content: '-',
            maxWidth: 500
        });

        $scope.$apply(function () {
            marker.myId = $scope.myMarkerId;
            $scope.myMarkers[marker.myId] = {};
            $scope.myMarkers[marker.myId].model = {
                description: "",
                name: undefined,
                is_published: true,
                address: "-",
                url: "-",
                bucode: ""
            };
            $scope.myMarkers[marker.myId].marker = marker;
            $scope.myMarkers[marker.myId].infowindow = infowindow;
            $scope.myMarkerId++;
        });

        var htmlContent = '<form name="buildingForm" class="infowindow-scroll-fix">'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.bucode" id="building-code" type="text" class="form-control" placeholder="Building Code (Optional)"/>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.name" id="building-name" type="text" class="form-control" placeholder="Building Name *"/>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<textarea ng-model="myMarkers[' + marker.myId + '].model.description" id="building-description" type="text" class="form-control" placeholder="Building Description (Optional)"></textarea>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.is_published" id="building-published" type="checkbox"><span> Make building public to view.</span>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '</fieldset class="form-group">'
            + '<div style="text-align: center;">'
            + '<fieldset class="form-group" style="display: inline-block; width: 75%;">'
            + '<button type="submit" class="btn btn-success add-any-button" ng-click="addNewBuilding(' + marker.myId + ')">'
            + '<span class="glyphicon glyphicon-plus"></span> Add'
            + '</button>'
            + '</fieldset>'
            + '<fieldset class="form-group" style="display: inline-block;width: 23%;">'
            + '<button class="btn btn-danger add-any-button" style="margin-left:2px" ng-click="deleteTempBuilding(' + marker.myId + ')"><span class="glyphicon glyphicon-remove"></span>'
            + '</button>'
            + '</fieldset>'
            + '</div>'
            + '</form>';

        var htmlContent2 = '<div class="infowindow-scroll-fix">'
            + '<h5 style="margin: 0">Building:</h5>'
            + '<span>{{myMarkers[' + marker.myId + '].model.name}}</span>'
            + '<h5 style="margin: 8px 0 0 0">Description:</h5>'
            + '<span>{{myMarkers[' + marker.myId + '].model.description}}</span>'
            + '</div>';

        var tpl = $compile(htmlContent)($scope);
        marker.tpl2 = $compile(htmlContent2)($scope);

        infowindow.setContent(tpl[0]);
        infowindow.open(GMapService.gmap, marker);

        google.maps.event.addListener(marker, 'click', function () {
            if (!infowindow.getMap()) {
                infowindow.open(GMapService.gmap, marker);
            }
        });
    };


    $scope.placeMarker = function (location) {

        var prevMarker = $scope.myMarkers[$scope.myMarkerId - 1];

        if (prevMarker && prevMarker.marker && prevMarker.marker.getMap() && prevMarker.marker.getDraggable()) {
            // TODO: alert for already pending building.
            console.log('there is a building pending, please add 1 at a time');
            return;
        }

        var marker = new google.maps.Marker({
            position: location,
            map: GMapService.gmap,
            icon: 'build/images/building-icon.png',
            draggable: true
        });

        var infowindow = new google.maps.InfoWindow({
            content: '-',
            maxWidth: 500
        });

        $scope.$apply(function () {
            marker.myId = $scope.myMarkerId;
            $scope.myMarkers[marker.myId] = {};
            $scope.myMarkers[marker.myId].model = {
                description: "",
                name: undefined,
                is_published: true,
                address: "-",
                url: "-",
                bucode: ""
            };
            $scope.myMarkers[marker.myId].marker = marker;
            $scope.myMarkers[marker.myId].infowindow = infowindow;
            $scope.myMarkerId++;
        });

        var htmlContent = '<form name="buildingForm" class="infowindow-scroll-fix">'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.bucode" id="building-code" type="text" class="form-control" placeholder="Building Code (Optional)"/>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.name" id="building-name" type="text" class="form-control" placeholder="Building Name *"/>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<textarea ng-model="myMarkers[' + marker.myId + '].model.description" id="building-description" type="text" class="form-control" placeholder="Building Description (Optional)"></textarea>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.is_published" id="building-published" type="checkbox"><span> Make building public to view.</span>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '</fieldset class="form-group">'
            + '<div style="text-align: center;">'
            + '<fieldset class="form-group" style="display: inline-block; width: 75%;">'
            + '<button type="submit" class="btn btn-success add-any-button" ng-click="addNewBuilding(' + marker.myId + ')">'
            + '<span class="glyphicon glyphicon-plus"></span> Add'
            + '</button>'
            + '</fieldset>'
            + '<fieldset class="form-group" style="display: inline-block;width: 23%;">'
            + '<button class="btn btn-danger add-any-button" style="margin-left:2px" ng-click="deleteTempBuilding(' + marker.myId + ')"><span class="glyphicon glyphicon-remove"></span>'
            + '</button>'
            + '</fieldset>'
            + '</div>'
            + '</form>';

        var htmlContent2 = '<div class="infowindow-scroll-fix">'
            + '<h5 style="margin: 0">Building:</h5>'
            + '<span>{{myMarkers[' + marker.myId + '].model.name}}</span>'
            + '<h5 style="margin: 8px 0 0 0">Description:</h5>'
            + '<span>{{myMarkers[' + marker.myId + '].model.description}}</span>'
            + '</div>';

        var tpl = $compile(htmlContent)($scope);
        marker.tpl2 = $compile(htmlContent2)($scope);

        infowindow.setContent(tpl[0]);
        infowindow.open(GMapService.gmap, marker);

        google.maps.event.addListener(marker, 'click', function () {
            if (!infowindow.getMap()) {
                infowindow.open(GMapService.gmap, marker);
            }
        });
    };

    $scope.deleteTempBuilding = function (id) {
        var myMarker = $scope.myMarkers[id];
        if (myMarker && myMarker.marker) {
            myMarker.marker.setMap(null);
        }
    };

    /**
     * building {
     *  name:
     *  description:
     *  coordinates_lat:
     *  coordinates_lon:
     *  floors: [
     *      0: {
     *          pois: [
     *              name:
     *              description:
     *              pois_type:
     *              is_building_entrance:
     *              coordinates_lat:
     *              coordinates_lon:
     *          ]
     *      }
     *  ];
     * }
     */
    $scope.exportBuildingToJson = function () {
        var result = {
            building: {
                floors: []
            }
        };

        var building = $scope.anyService.selectedBuilding;
        if (LPUtils.isNullOrUndefined(building)) {
            _err('No building selected');
            return;
        }
        result.building.buid = building.buid;
        result.building.name = building.name;
        result.building.description = building.description;
        result.building.coordinates_lat = building.coordinates_lat;
        result.building.coordinates_lon = building.coordinates_lon;

        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = building.buid;

        var count = 0;

        var promise = AnyplaceAPIService.allBuildingFloors(jsonReq);
        promise.then(
            function (resp) {
                var floors = resp.data.floors;

                var resFloors = [];

                if (floors) {
                    for (var i = 0; i < floors.length; i++) {

                        (function (jreq) {
                            var promise = AnyplaceAPIService.retrievePoisByBuildingFloor(jreq);
                            promise.then(
                                function (resp) {
                                    var data = resp.data;

                                    var poisArray = data.pois;

                                    if (poisArray) {

                                        var flPois = [];

                                        var fNo = poisArray[0].floor_number;

                                        for (var j = 0; j < poisArray.length; j++) {
                                            var sPoi = poisArray[j];

                                            if (sPoi.pois_type == "None") {
                                                continue;
                                            }
                                            if (sPoi.overwrite) {
                                                var tmp = {
                                                    name: sPoi.name,
                                                    description: sPoi.description,
                                                    puid: sPoi.puid,
                                                    pois_type: sPoi.pois_type,
                                                    coordinates_lat: sPoi.coordinates_lat,
                                                    coordinates_lon: sPoi.coordinates_lon,
                                                    overwrite: sPoi.overwrite
                                                };
                                            }
                                            else {
                                                var tmp = {
                                                    name: sPoi.name,
                                                    description: sPoi.description,
                                                    puid: sPoi.puid,
                                                    pois_type: sPoi.pois_type,
                                                    coordinates_lat: sPoi.coordinates_lat,
                                                    coordinates_lon: sPoi.coordinates_lon,
                                                    overwrite: "false"
                                                };
                                            }
                                            flPois.push(tmp);
                                        }

                                        resFloors.push(
                                            {
                                                floor_number: fNo,
                                                pois: flPois
                                            }
                                        );

                                        count++;
                                        if (count === floors.length) {
                                            result.building.floors = resFloors;
                                            _suc('Successfully exported ' + building.name + ' to JSON.');
                                            var blob = new Blob([JSON.stringify(result, null, 4)], {type: "text/plain;charset=utf-8"});
                                            saveAs(blob, building.name.toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
                                        }

                                    }
                                },
                                function (resp) {
                                    var data = resp.data;
                                    console.log(data.message);
                                });
                        }({
                            buid: building.buid,
                            floor_number: floors[i].floor_number
                        }));
                    }
                }
            },
            function (resp) {
                // TODO: alert failure
                console.log(resp.data.message);
            }
        );
    };

    /**
     * campus {
   "buids": [
   ],
   "description": "",
   "name": "",
   "owner_id": "",
   "access_token": "",
   "cuid": ""
    }
     * }
     */
    $scope.exportCampusToJson = function () {
        var result = {
            campus: {
                buids: []
            }
        };

        var campus = $scope.anyService.selectedCampus;
        if (LPUtils.isNullOrUndefined(campus)) {
            _err('No campus selected');
            return;
        }

        result.campus.name = campus.name;
        result.campus.description = campus.description;
        result.campus.buids = campus.buids;

        _suc('Successfully exported ' + campus.name + ' to JSON.');

        var blob = new Blob([JSON.stringify(result, null, 4)], {type: "text/plain;charset=utf-8"});
        saveAs(blob, campus.name.toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
    };

    function readSingleFile(e) {
        var file = e.target.files[0];
        if (!file) {
            return;
        }
        var reader = new FileReader();
        reader.onload = function (e) {
            var contents = e.target.result;
            $scope.fileToUpload = contents;
        };
        reader.readAsText(file);
    }

    document.getElementById('file-input')
        .addEventListener('change', readSingleFile, false);

    $scope.anyService.downloadlogfile = false;

    $scope.importBuildingFromJson = function () {

        $scope.anyService.progress = 0;
        var i, j, count = 0, countok = 0;
        if ($scope.fileToUpload == "") {
            _err("Something went wrong no file selected");
        }
        var obj = JSON.parse($scope.fileToUpload);
        for (i = 0; i < obj.building.floors.length; i++) {
            for (j = 0; j < obj.building.floors[i].pois.length; j++) {
                if (obj.building.floors[i].pois[j].overwrite == "true") {
                    count++;
                }
            }
        }
        if (count == 0) {
            _err("Something went wrong no pois to update");
        }

        for (i = 0; i < obj.building.floors.length; i++) {
            for (j = 0; j < obj.building.floors[i].pois.length; j++) {
                if (obj.building.floors[i].pois[j].overwrite == "true") {
                    countok++;
                    $scope.updatePoifromFile(obj.building.floors[i].pois[j].puid,
                        obj.building.floors[i].pois[j].coordinates_lat,
                        obj.building.floors[i].pois[j].coordinates_lon,
                        obj.building.floors[i].pois[j].name,
                        obj.building.floors[i].pois[j].description,
                        obj.building.floors[i].pois[j].pois_type,
                        obj.building.floors[i].pois[j].overwrite,
                        obj.building.buid, i, j, count, countok,
                        obj.building.name
                    );
                }
            }
        }
    }

    $scope.allpois = {};
    $scope.pois = {};

    $scope.importBuildingFromExcel = function (oEvent) {
        $scope.anyService.progress = 0;
        // Get The File From The Input
        var oFile = document.getElementById('my_file_input').files[0];
        $scope.fileToUpload = oFile.name;
        // Create A File Reader HTML5
        var reader = new FileReader();
        // Ready The Event For When A File Gets Selected
        reader.onload = function (e) {
            var data = e.target.result;
            var cfb = XLS.CFB.read(data, {type: 'binary'});
            var wb = XLS.parse_xlscfb(cfb);
            // Loop Over Each Sheet
            wb.SheetNames.forEach(function (sheetName) {
                // Obtain The Current Row As CSV
                var oJS = XLS.utils.sheet_to_row_object_array(wb.Sheets[sheetName]);
                var last_buid = "";
                $scope.uploadloop(0, oJS, last_buid);
            });
        };

        // Tell JS To Start Reading The File.. You could delay this if desired
        reader.readAsBinaryString(oFile);
    }

    $scope.uploadloop = function (potition, oJS, last_buid) {

        for (var i = potition; i < oJS.length; i++) {
            if (last_buid != oJS[i].buid && oJS[i].buid != undefined) {
                $scope.pois = {};
                last_buid = oJS[i].buid;
                $scope.fetchAllPoisForBuilding(oJS[i].buid, oJS, i, last_buid);
                return;
            }
            var description = "";

            if (oJS[i].des3 != undefined && oJS[i].des3 != "" && oJS[i].des3 != null) {
                if (oJS[i].des4 != undefined && oJS[i].des4 != "" && oJS[i].des4 != null) {
                    description = description + oJS[i].des3 + " " + oJS[i].des4;
                }
                else {
                    description = description + oJS[i].des3;
                }
            }
            else {
                if (oJS[i].des4 != undefined && oJS[i].des4 != "" && oJS[i].des4 != null) {
                    description = description + oJS[i].des4;
                }
            }

            if (oJS[i].des1 != undefined && oJS[i].des1 != "" && oJS[i].des1 != null) {
                if (description != "") {
                    description = description + "\n" + oJS[i].des1;
                }
                else {
                    description = description + oJS[i].des1;
                }
            }

            if (oJS[i].des2 != undefined && oJS[i].des2 != "" && oJS[i].des2 != null) {
                if (description != "") {
                    description = description + "\n" + oJS[i].des2;
                }
                else {
                    description = description + oJS[i].des2;
                }
            }

            $scope.anyService.progress = (i / (oJS.length - 1)) * 100;
            if ((oJS.length - 1) == 0) $scope.anyService.progress = 100;

            if ($scope.pois[oJS[i].name]) {
                $scope.updatePoifromExcel($scope.pois[oJS[i].name].puid,
                    $scope.pois[oJS[i].name].coordinates_lat,
                    $scope.pois[oJS[i].name].coordinates_lon,
                    $scope.pois[oJS[i].name].is_building_entrance,
                    oJS[i].name,
                    description,
                    $scope.pois[oJS[i].name].pois_type,
                    $scope.pois[oJS[i].name].overwrite,
                    $scope.pois[oJS[i].name].buid,
                    ""
                );
            }
            else {
                $scope.updatePoifromExcel("",
                    "",
                    "",
                    "",
                    oJS[i].name,
                    description,
                    "",
                    "",
                    "",
                    ""
                );
            }

        }
    };

    $scope.fetchAllPoisForBuilding = function (building, oJS, position, last_buid) {
        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = building;

        var promise = AnyplaceAPIService.retrievePoisByBuilding(jsonReq);
        promise.then(
            function (resp) {
                var data = resp.data;

                $scope.myPois = data.pois;

                var sz = $scope.myPois.length;
                for (var i = sz - 1; i >= 0; i--) {
                    var name = $scope.myPois[i].name;
                    $scope.pois[name] = $scope.myPois[i];
                }
                $scope.uploadloop(position, oJS, last_buid);
            },
            function (resp) {
                var data = resp.data;
                _err("Something went wrong while fetching POIs");
            }
        );
    };
    $scope.logfile = {
        pois: []
    };
    $scope.updatePoifromExcel = function (id, lat, lng, building_entrance, nm, des, ptype, ovwrite, bid, buildingname) {

        var obj = {};

        obj.username = $scope.creds.username;
        obj.password = $scope.creds.password;
        obj.owner_id = $scope.owner_id;

        obj.coordinates_lat = lat;
        obj.coordinates_lon = lng;

        obj.is_building_entrance = building_entrance;
        obj.name = nm;
        obj.puid = id;
        obj.description = des;
        obj.pois_type = ptype;
        obj.overwrite = "false";
        obj.buid = bid;
        // make the request at AnyplaceAPI
        var promise = $scope.anyAPI.updatePois(obj);
        promise.then(
            function (resp) {
                var data = resp.data;
                if ($scope.anyService.progress == 100) {

                    if ($scope.anyService.downloadlogfile) {
                        $scope.anyService.downloadlogfile = false;
                        _suc("Successfully updated POIs.A log file will be downloaded");
                        var blob = new Blob([JSON.stringify($scope.logfile, null, 4)], {type: "text/plain;charset=utf-8"});
                        saveAs(blob, ($scope.fileToUpload + "-log_file").toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                        }, 2500);
                    }
                    else {
                        _suc("Successfully updated POIs.");
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                            location.reload();
                        }, 2500);
                    }
                }
            },
            function (resp) {
                var data = resp.data;
                $scope.logfile.pois.push({
                    "name": nm,
                    "puid": id,
                    "description": des,
                    "status": "Something went wrong while updating POI."
                });
                $scope.anyService.downloadlogfile = true;
                if ($scope.anyService.progress == 100) {
                    $scope.anyService.downloadlogfile = false;
                    _suc("Successfully updated POIs.A log file will be downloaded");
                    var blob = new Blob([JSON.stringify($scope.logfile, null, 4)], {type: "text/plain;charset=utf-8"});
                    saveAs(blob, ($scope.fileToUpload + "-log_file").toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
                    $scope.logfile.pois = [];

                    setTimeout(function () {
                        $scope.anyService.progress = undefined;
                    }, 2500);
                }
            }
        );
    };


    $scope.updatePoifromFile = function (id, lat, lng, nm, des, ptype, ovwrite, bid, i, j, count, countok, buildingname) {

        var obj = {};

        obj.username = $scope.creds.username;
        obj.password = $scope.creds.password;
        obj.owner_id = $scope.owner_id;

        obj.coordinates_lat = lat;
        obj.coordinates_lon = lng;

        obj.name = nm;
        obj.puid = id;
        obj.description = des;
        obj.pois_type = ptype;
        obj.overwrite = "false";
        obj.buid = bid;
        // make the request at AnyplaceAPI
        var promise = $scope.anyAPI.updatePois(obj);
        promise.then(
            function (resp) {
                var data = resp.data;
                $scope.anyService.progress = countok / count * 100;
                if ($scope.anyService.progress == 100) {
                    if ($scope.anyService.downloadlogfile) {
                        _suc("Successfully updated POIs.A log file will be downloaded");
                        var blob = new Blob([JSON.stringify($scope.logfile, null, 4)], {type: "text/plain;charset=utf-8"});
                        saveAs(blob, (buildingname + "-log_file").toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                        }, 2500);
                    }
                    else {
                        _suc("Successfully updated POIs.");
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                            location.reload();
                        }, 2500);
                    }
                }
            },
            function (resp) {
                var data = resp.data;

                $scope.logfile.pois.push({
                    "name": nm,
                    "puid": id,
                    "description": des,
                    "status": "Something went wrong while updating POI."
                });

                $scope.anyService.downloadlogfile = true;
                $scope.anyService.progress = countok / count * 100;
                if ($scope.anyService.progress == 100) {

                    if ($scope.anyService.downloadlogfile) {
                        _suc("Successfully updated POIs.A log file will be downloaded");
                        var blob = new Blob([JSON.stringify($scope.logfile, null, 4)], {type: "text/plain;charset=utf-8"});
                        saveAs(blob, (buildingname + "-log_file").toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
                        $scope.logfile.pois = [];
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                        }, 2500);
                    }
                    else {
                        _suc("Successfully updated POIs.");
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                            location.reload();
                        }, 2500);
                    }
                }
            }
        );
    };

    $scope.Poisresult = {
        building: {
            floors: []
        }
    };

    $scope.Connectionsresult = {
        building: {}
    };

    $scope.zip = new JSZip();
    $scope.DownloadBackup = function () {

        var b = $scope.anyService.selectedBuilding;
        var xFloors = [];
        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = b.buid;
        $scope.anyService.progress = 0;
        var promise = AnyplaceAPIService.allBuildingFloors(jsonReq);
        promise.then(
            function (resp) {
                xFloors = resp.data.floors;
                var floor = 0;
                var floor_number = "";
                for (var i = 0; i < xFloors.length; i++) {
                    if (i == 0) {
                        floor_number = xFloors[i].floor_number;
                    }
                    else {
                        floor_number = floor_number + " " + xFloors[i].floor_number;
                    }
                }
                $scope.anyService.progress = 10;
                var buid = b.buid;
                var jsonReq2 = AnyplaceService.jsonReq;
                var promise2 = AnyplaceAPIService.downloadFloorPlanAll(jsonReq2, buid, floor_number);
                promise2.then(
                    function (resp) {
                        // on success
                        var data = resp.data;
                        var img = $scope.zip.folder("floor_plans");
                        for (var si = 0; si < data.all_floors.length; si++) {
                            if (data.all_floors[si] != "") {
                                img.file(xFloors[si].floor_number + ".png", data.all_floors[si], {base64: true});
                            }
                        }
                        $scope.anyService.progress = 25;
                        var jsonReq3 = AnyplaceService.jsonReq;
                        jsonReq3.buid = buid;
                        jsonReq3.floor = floor_number;
                        var promise3 = AnyplaceAPIService.getRadioByBuildingFloorAll(jsonReq3);
                        promise3.then(
                            function (resp) {
                                var data2 = resp.data;
                                var logs = $scope.zip.folder("radiomaps");
                                if (data2.rss_log_files) {
                                    var urls = "";
                                    for (var si2 = 0; si2 < data2.rss_log_files.length; si2++) {
                                        logs.file(xFloors[si2].floor_number + "-radiomap.txt", data2.rss_log_files[si2]);
                                    }
                                }
                                $scope.anyService.progress = 70;
                                $scope.exportPoisBuildingToJson();
                            },
                            function (resp) {

                            }
                        );
                    },
                    function (resp) {
                    }
                );
            },
            function (resp) {
                console.log(resp.data.message);
                _err("Something went wrong while fetching all floors");
            }
        );


    }

    $scope.exportPoisBuildingToJson = function () {

        var building = $scope.anyService.selectedBuilding;
        if (LPUtils.isNullOrUndefined(building)) {
            _err('No building selected');
            return;
        }
        $scope.Poisresult.building.buid = building.buid;
        $scope.Poisresult.building.name = building.name;
        $scope.Poisresult.building.description = building.description;
        $scope.Poisresult.building.coordinates_lat = building.coordinates_lat;
        $scope.Poisresult.building.coordinates_lon = building.coordinates_lon;

        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = building.buid;

        var count = 0;

        var promise = AnyplaceAPIService.allBuildingFloors(jsonReq);
        promise.then(
            function (resp) {
                var floors = resp.data.floors;

                var resFloors = [];
                $scope.anyService.progress = 80;
                if (floors) {
                    for (var i = 0; i < floors.length; i++) {

                        (function (jreq) {
                            var promise = AnyplaceAPIService.retrievePoisByBuildingFloor(jreq);
                            promise.then(
                                function (resp) {
                                    var data = resp.data;

                                    var poisArray = data.pois;

                                    if (poisArray) {

                                        var flPois = [];

                                        if (poisArray[0] != undefined) {
                                            var fNo = poisArray[0].floor_number;

                                            for (var j = 0; j < poisArray.length; j++) {
                                                var sPoi = poisArray[j];

                                                if (sPoi.pois_type == "None") {
                                                    continue;
                                                }
                                                if (sPoi.overwrite) {
                                                    var tmp = {
                                                        name: sPoi.name,
                                                        description: sPoi.description,
                                                        puid: sPoi.puid,
                                                        pois_type: sPoi.pois_type,
                                                        coordinates_lat: sPoi.coordinates_lat,
                                                        coordinates_lon: sPoi.coordinates_lon,
                                                        overwrite: sPoi.overwrite
                                                    };
                                                }
                                                else {
                                                    var tmp = {
                                                        name: sPoi.name,
                                                        description: sPoi.description,
                                                        puid: sPoi.puid,
                                                        pois_type: sPoi.pois_type,
                                                        coordinates_lat: sPoi.coordinates_lat,
                                                        coordinates_lon: sPoi.coordinates_lon,
                                                        overwrite: "false"
                                                    };
                                                }

                                                if (sPoi.is_building_entrance == 'true') {
                                                    tmp.is_building_entrance = 'true';
                                                }
                                                else {
                                                    tmp.is_building_entrance = 'false';
                                                }

                                                flPois.push(tmp);
                                            }

                                            resFloors.push(
                                                {
                                                    floor_number: fNo,
                                                    pois: flPois
                                                }
                                            );
                                        }
                                        count++;
                                        if (count === floors.length) {
                                            $scope.Poisresult.building.floors = resFloors;
                                            $scope.zip.file("allpois.json", JSON.stringify($scope.Poisresult, null, 4));
                                            $scope.anyService.progress = 90;
                                            $scope.exportConnectionBuildingToJson();
                                        }

                                    }
                                },
                                function (resp) {
                                    var data = resp.data;
                                    console.log(data.message);
                                });
                        }({
                            buid: building.buid,
                            floor_number: floors[i].floor_number
                        }));
                    }
                }
            },
            function (resp) {
                // TODO: alert failure
                console.log(resp.data.message);
            }
        );
    };

    $scope.exportConnectionBuildingToJson = function () {

        var building = $scope.anyService.selectedBuilding;
        if (LPUtils.isNullOrUndefined(building)) {
            _err('No building selected');
            return;
        }
        $scope.Connectionsresult.building.buid = building.buid;
        $scope.Connectionsresult.building.name = building.name;
        $scope.Connectionsresult.building.description = building.description;
        $scope.Connectionsresult.building.coordinates_lat = building.coordinates_lat;
        $scope.Connectionsresult.building.coordinates_lon = building.coordinates_lon;

        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = building.buid;

        var count = 0;

        var promise = AnyplaceAPIService.allBuildingFloors(jsonReq);
        promise.then(
            function (resp) {
                var floors = resp.data.floors;

                var resFloors = [];

                if (floors) {
                    for (var i = 0; i < floors.length; i++) {

                        (function (jreq) {
                            var promise = AnyplaceAPIService.retrieveConnectionsByBuildingFloor(jreq);
                            promise.then(
                                function (resp) {
                                    $scope.anyService.progress = 100;
                                    var data = resp.data;

                                    var connArray = data.connections;

                                    if (connArray) {

                                        var flConnections = [];

                                        if (connArray[0] != undefined) {

                                            var fNo = connArray[0].floor_a;

                                            for (var j = 0; j < connArray.length; j++) {
                                                var sConnection = connArray[j];

                                                var tmp = {
                                                    name: sConnection.name,
                                                    description: sConnection.description,
                                                    cuid: sConnection.puid,
                                                    weight: sConnection.weight,
                                                    pois_a: sConnection.pois_a,
                                                    pois_b: sConnection.pois_b,
                                                    floor_a: sConnection.floor_a,
                                                    floor_b: sConnection.floor_b,
                                                    is_published: sConnection.is_published,
                                                    buid_a: sConnection.buid_a,
                                                    buid_b: sConnection.buid_b
                                                };

                                                flConnections.push(tmp);
                                            }

                                            resFloors.push(
                                                {
                                                    floor_number: fNo,
                                                    connections: flConnections
                                                }
                                            );
                                        }
                                        count++;

                                        if (count === floors.length) {

                                            $scope.Connectionsresult.building.floors = resFloors;
                                            $scope.zip.file("allconnections.json", JSON.stringify($scope.Connectionsresult, null, 4));

                                            $scope.zip.generateAsync({type: "blob"})
                                                .then(function (content) {
                                                    // see FileSaver.js
                                                    saveAs(content, building.buid + ".zip");
                                                });
                                            $scope.anyService.progress = undefined;
                                        }
                                    }
                                },
                                function (resp) {
                                    var data = resp.data;
                                    console.log(data.message);
                                });
                        }({
                            buid: building.buid,
                            floor_number: floors[i].floor_number
                        }));
                    }
                }
            },
            function (resp) {
                console.log(resp.data.message);
            }
        );
    };

    //set cookies

    $('#dismiss').on('click', function () {
        // set expire date.
        if (typeof(Storage) !== "undefined" && localStorage) {
            localStorage.setItem('dismissClicked', 'YES');
        }

    });

    if (localStorage.getItem('dismissClicked') !== 'YES') {
        function showWelcomeMessage() {
            $('#myModal_Welcome').modal('show');
        }

        window.onload = showWelcomeMessage;
    }

}]);
