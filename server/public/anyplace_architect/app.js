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
        tile.src = "https://tile.openstreetmap.org/" + zoom + "/" + x + "/" + coord.y + ".png";
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

    var mapTypeId = "roadmap";
    if (typeof(Storage) !== "undefined" && localStorage) {
        if (localStorage.getItem('mapTypeId'))
            mapTypeId = localStorage.getItem('mapTypeId');
        else
            localStorage.setItem("mapTypeId", "roadmap");
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
        
        //Bugfix: Loading of maps fail when zoomed to MAX level with fingerprints enabled.
        //Issue happens due to setting of custom maptype Id
        if (self.gmap.getMapTypeId() === 'my_custom_layer1' && self.gmap.zoom < 22) {
            GMapService.gmap.setMapTypeId(localStorage.getItem("previousMapTypeId"))                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                ;
        } 
        else if (self.gmap.getMapTypeId() !== 'my_custom_layer1' && self.gmap.zoom === 22){
            localStorage.setItem("previousMapTypeId",self.gmap.getMapTypeId());
        }

        var showStreetViewControl = self.gmap.getMapTypeId() === 'roadmap' || self.gmap.getMapTypeId() === 'satellite';
        localStorage.setItem("mapTypeId",self.gmap.getMapTypeId());
        customMapAttribution(self.gmap);
        self.gmap.setOptions({
            streetViewControl: showStreetViewControl
        });
    });

    function customMapAttribution(map) {
        var id = "custom-maps-attribution";
        var attributionElm = document.getElementById(id);
        if (attributionElm === undefined || attributionElm === null) {
            attributionElm = document.createElement('div');
            attributionElm.id = id;
            map.controls[google.maps.ControlPosition.BOTTOM_RIGHT].push(attributionElm);
        }
        if (self.gmap.getMapTypeId() === "OSM")
            attributionElm.innerHTML = '<a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors';
        if (self.gmap.getMapTypeId() === "roadmap")
            attributionElm.innerHTML = '';
        if (self.gmap.getMapTypeId() === "satellite")
            attributionElm.innerHTML = '';
        if (self.gmap.getMapTypeId() === "CartoLight")
            attributionElm.innerHTML = '<a href="http://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors, © <a href="https://carto.com/attribution">CARTO</a>';
    }

    //Define OSM map type pointing at the OpenStreetMap tile server
    self.gmap.mapTypes.set("OSM", new OSMMapType(new google.maps.Size(256, 256)));
    //Define Carto Dark map type pointing at the OpenStreetMap tile server
    // self.gmap.mapTypes.set("CartoDark", new CartoDarkMapType(new google.maps.Size(256, 256)));
    //Define Carto Light map type pointing at the OpenStreetMap tile server
    self.gmap.mapTypes.set("CartoLight", new CartoLightMapType(new google.maps.Size(256, 256)));
    // Now attach the coordinate map type to the map's registry.
    //self.gmap.mapTypes.set('coordinate', new CoordMapType(new google.maps.Size(256, 256)));
    customMapAttribution(self.gmap);

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
