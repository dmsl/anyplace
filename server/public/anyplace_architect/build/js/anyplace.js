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

var app = angular.module('anyArchitect', ['ui.bootstrap', 'ui.select', 'ngSanitize']);

app.service('GMapService', function () {

    this.gmap = {};
//    this.searchBox = {};

    var self = this;

    // Initialize Google Maps
    var mapOptions = {
        center: {lat: 35.14448545801575, lng: 33.41121554374695},
        zoom: 8,
        panControl: true,
        zoomControl: true,
        mapTypeControl: true,
        mapTypeControlOptions: {
            position:google.maps.ControlPosition.RIGHT_BOTTOM
        },
        scaleControl: true,
        streetViewControl: false,
        overviewMapControl: true
    };
    self.gmap = new google.maps.Map(document.getElementById('map-canvas'),
        mapOptions);

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

    // Add click listener on map to add a marker on click
    //google.maps.event.addListener(self.gmap, 'click', function (event) {
    //
    //    if (!self.mainMarker) {
    //        self.mainMarker = new google.maps.Marker({
    //            position: event.latLng,
    //            map: self.gmap,
    //            title: "Helper marker at (" + event.latLng.lat() + ", " + event.latLng.lng() + ")"
    //        });
    //    }
    //
    //    self.mainMarker.setPosition(event.latLng);
    //    self.mainMarker.setTitle("Helper marker at (" + event.latLng.lat() + ", " + event.latLng.lng() + ")");
    //});

});

app.factory('AnyplaceService', function () {

    var anyService = {};

    anyService.selectedBuilding = undefined;
    anyService.selectedFloor = undefined;
    anyService.selectedPoi = undefined;

    anyService.alerts = [];

    anyService.jsonReq = {
        username: 'username',
        password: 'password'
    };

    anyService.getBuilding = function () {
        return this.selectedBuilding;
    };

    anyService.getBuildingId = function () {
        if(!this.selectedBuilding) {
            return undefined;
        }
        return this.selectedBuilding.buid;
    };

    anyService.getBuildingName = function() {
        if(!this.selectedBuilding) {
            return 'N/A';
        }
        return this.selectedBuilding.name;
    };

    anyService.getFloor = function () {
        return this.selectedFloor;
    };

    anyService.getFloorNumber = function () {
        if(!this.selectedFloor) {
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
        this.alerts[0] = ({msg: msg, type: type});
    };

    anyService.closeAlert = function (index) {
        this.alerts.splice(index, 1);
    };

    anyService.getBuildingViewerUrl = function() {
        if(!this.selectedBuilding || !this.selectedBuilding.buid) {
            return "N/A";
        }
        return "http://anyplace.cs.ucy.ac.cy/viewer/?buid=" + this.selectedBuilding.buid;
    };

    anyService.clearAllData = function() {
        anyService.selectedPoi = undefined;
        anyService.selectedFloor = undefined;
        anyService.selectedBuilding = undefined;
    };

    return anyService;
});

app.factory('Alerter', function() {
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

app.factory('myInterceptor', [function() {
    var requestInterceptor = {
        request: function(config) {

            if (config.url.indexOf(AnyplaceAPI.FULL_SERVER) != 0) {
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

app.config(['$httpProvider', function($httpProvider) {
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

//AnyplaceAPI.SERVER = "http://127.0.0.1"
//AnyplaceAPI.PORT = "9000";
//AnyplaceAPI.FULL_SERVER = AnyplaceAPI.SERVER + ":" + AnyplaceAPI.PORT;
//AnyplaceAPI.FULL_SERVER = "http://127.0.0.1:9000/anyplace";
AnyplaceAPI.FULL_SERVER = "http://anyplace.rayzit.com/anyplace";

/**
 * MAPPING API
 */
AnyplaceAPI.Mapping = {};

AnyplaceAPI.Mapping.RADIO_HEATMAP = "/mapping/radio/heatmap_building_floor";
AnyplaceAPI.Mapping.RADIO_HEATMAP_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.RADIO_HEATMAP;

AnyplaceAPI.Mapping.BUILDING_ADD = "/mapping/building/add";
AnyplaceAPI.Mapping.BUILDING_ADD_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.BUILDING_ADD;
AnyplaceAPI.Mapping.BUILDING_UPDATE = "/mapping/building/update";
AnyplaceAPI.Mapping.BUILDING_UPDATE_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.BUILDING_UPDATE;
AnyplaceAPI.Mapping.BUILDING_DELETE = "/mapping/building/delete";
AnyplaceAPI.Mapping.BUILDING_DELETE_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.BUILDING_DELETE;
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
AnyplaceAPI.Mapping.FLOOR_PLAN_UPLOAD = "/mapping/floor/upload"
AnyplaceAPI.Mapping.FLOOR_PLAN_UPLOAD_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.FLOOR_PLAN_UPLOAD;
AnyplaceAPI.Mapping.FLOOR_PLAN_DOWNLOAD = "/floorplans64/"
AnyplaceAPI.Mapping.FLOOR_PLAN_DOWNLOAD_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.FLOOR_PLAN_DOWNLOAD;

AnyplaceAPI.Mapping.POIS_ADD = "/mapping/pois/add";
AnyplaceAPI.Mapping.POIS_ADD_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.POIS_ADD;
AnyplaceAPI.Mapping.POIS_UPDATE = "/mapping/pois/update";
AnyplaceAPI.Mapping.POIS_UPDATE_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.POIS_UPDATE;
AnyplaceAPI.Mapping.POIS_DELETE = "/mapping/pois/delete";
AnyplaceAPI.Mapping.POIS_DELETE_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.POIS_DELETE;
AnyplaceAPI.Mapping.POIS_ALL_FLOOR = "/mapping/pois/all_floor";
AnyplaceAPI.Mapping.POIS_ALL_FLOOR_URL = AnyplaceAPI.FULL_SERVER + AnyplaceAPI.Mapping.POIS_ALL_FLOOR;

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
        }).
            success(function (data, status) {
                return data;
            }).
            error(function (data, status) {
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
        }).
            success(function (data, status) {
                return data;
            }).
            error(function (data, status) {
                return data;
            });
    };

    apiService.updateBuilding = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.BUILDING_UPDATE_URL,
            data: json_req
        }).
            success(function (data, status) {
                return data;
            }).
            error(function (data, status) {
                return data;
            });

    };

    apiService.deleteBuilding = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.BUILDING_DELETE_URL,
            data: json_req
        }).
            success(function (data, status) {
                return data;
            }).
            error(function (data, status) {
                return data;
            });

    };

    apiService.allBuildings = function (json_req) {
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.BUILDING_ALL_URL,
            data: json_req
        }).
            success(function (data, status) {
                return data;
            }).
            error(function (data, status) {
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
        }).
            success(function (data, status) {
                return data;
            }).
            error(function (data, status) {
                return data;
            });
    };

    apiService.updateFloor = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.FLOOR_UPDATE_URL,
            data: json_req
        }).
            success(function (data, status) {
                return data;
            }).
            error(function (data, status) {
                return data;
            });
    };

    apiService.deleteFloor = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.FLOOR_DELETE_URL,
            data: json_req
        }).
            success(function (data, status) {
                return data;
            }).
            error(function (data, status) {
                return data;
            });

    };

    apiService.allBuildingFloors = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.FLOOR_ALL_URL,
            data: json_req
        }).
            success(function (data, status) {
                return data;
            }).
            error(function (data, status) {
                return data;
            });

    };


    apiService.uploadFloorPlan = function (json_req, file) {
        alert("make the request: " + json_req);
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
        }).
            success(function (data, status) {
                return data;
            }).
            error(function (data, status) {
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
        }).
            success(function (data, status) {
                return data;
            }).
            error(function (data, status) {
                return data;
            });
    };

    apiService.downloadFloorPlan = function (json_req, buid, floor_number) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.FLOOR_PLAN_DOWNLOAD_URL + buid + "/" + floor_number,
            data: json_req
        }).
            success(function (data, status) {
                return data;
            }).
            error(function (data, status) {
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
        }).
            success(function (data, status) {
                return data;
            }).
            error(function (data, status) {
                return data;
            });
    };

    apiService.updatePois = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.POIS_UPDATE_URL,
            data: json_req
        }).
            success(function (data, status) {
                return data;
            }).
            error(function (data, status) {
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
        }).
            success(function (data, status) {
                //deferred.resolve(data);
                return data;
            }).
            error(function (data, status) {
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
        }).
            success(function (data, status) {
                return data;
            }).
            error(function (data, status) {
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
        }).
            success(function (data, status) {
                return data;
            }).
            error(function (data, status) {
                return data;
            });
    };

    apiService.updateConnection = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.CONNECTION_UPDATE_URL,
            data: json_req
        }).
            success(function (data, status) {
                return data;
            }).
            error(function (data, status) {
                return data;
            });
    };

    apiService.deleteConnection = function (json_req) {
        //alert( "make the request: " + json_req );
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.CONNECTION_DELETE_URL,
            data: json_req
        }).
            success(function (data, status) {
                return data;
            }).
            error(function (data, status) {
                return data;
            });
    };

    apiService.retrieveConnectionsByBuildingFloor = function (json_req) {
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.CONNECTION_ALL_FLOOR_URL,
            data: json_req
        }).
            success(function (data, status) {
                return data;
            }).
            error(function (data, status) {
                return data;
            });
    };

    apiService.signAccount = function (json_req) {
        return $http({
            method: "POST",
            url: AnyplaceAPI.Mapping.SIGN_URL,
            data: json_req
        }).
            success(function (data, status) {
                return data;
            }).
            error(function (data, status) {
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

        //patch: calculate the corect mouse offset for a more natural feel
        ndx = dx * _cos + dy * _sin;
        ndy = dy * _cos - dx * _sin;
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
        var c = c1 = c2 = 0;

        while ( i < utftext.length ) {

            c = utftext.charCodeAt(i);

            if (c < 128) {
                string += String.fromCharCode(c);
                i++;
            }
            else if((c > 191) && (c < 224)) {
                c2 = utftext.charCodeAt(i+1);
                string += String.fromCharCode(((c & 31) << 6) | (c2 & 63));
                i += 2;
            }
            else {
                c2 = utftext.charCodeAt(i+1);
                c3 = utftext.charCodeAt(i+2);
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

/*
 Building Schema:
 {
 address: "-"
 buid: "building_02e4ebd2-413e-4b27-b3bf-6f7e2ca3640f_1397488807470"
 coordinates_lat: "52.52816874290241"
 coordinates_lon: "13.457137495279312"
 description: "IPSN 2014 Venue"
 is_published: "true"
 name: "Andel's Hotel Berlin, Berlin, Germany"
 url: "-"
 }
 */

app.controller('BuildingController', ['$scope', '$compile', 'GMapService', 'AnyplaceService', 'AnyplaceAPIService', function ($scope, $compile, GMapService, AnyplaceService, AnyplaceAPIService) {

    $scope.myMarkers = {};
    $scope.myMarkerId = 0;

    $scope.gmapService = GMapService;
    $scope.anyService = AnyplaceService;
    $scope.anyAPI = AnyplaceAPIService;

    $scope.myBuildings = [];

    $scope.myBuildingsHashT = {};

    $scope.crudTabSelected = 1;
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
    });

    $scope.$on("loggedOff", function (event, mass) {
        _clearBuildingMarkersAndModels();
        $scope.myMarkers = {};
        $scope.myMarkerId = 0;
        $scope.myBuildings = [];
        $scope.myBuildingsHashT = {};
    });

    $scope.$watch('anyService.selectedBuilding', function (newVal, oldVal) {
        if (newVal && newVal.coordinates_lat && newVal.coordinates_lon) {
            // Pan map to selected building
            $scope.gmapService.gmap.panTo(_latLngFromBuilding(newVal));
            $scope.gmapService.gmap.setZoom(19);

            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem("lastBuilding", newVal.buid);
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
                //var bs = JSON.parse( data.buildings );
                $scope.myBuildings = data.buildings;

                var infowindow = new google.maps.InfoWindow({
                    content: '-',
                    maxWidth: 500
                });

                var localStoredBuildingIndex = -1;
                var localStoredBuildingId = undefined;
                if (typeof(Storage) !== "undefined" && localStorage && localStorage.getItem('lastBuilding')) {
                    localStoredBuildingId = localStorage.getItem('lastBuilding');
                }

                for (var i = 0; i < $scope.myBuildings.length; i++) {

                    var b = $scope.myBuildings[i];

                    if (localStoredBuildingId && localStoredBuildingId === b.buid) {
                        localStoredBuildingIndex = i;
                    }

                    if (b.is_published === 'true' || b.is_published == true) {
                        b.is_published = true;
                    } else {
                        b.is_published = false;
                    }

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
                        + '<h5 style="margin: 0">Building:</h5>'
                        + '<span>' + b.name + '</span>'
                        + '<h5 style="margin: 8px 0 0 0">Description:</h5>'
                        + '<span>' + b.description + '</span>'
                        + '</div>';

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
                        $scope.$apply(function () {
                            $scope.anyService.selectedBuilding = self.building;
                        });
                    });
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

                var promise = $scope.anyAPI.addBuilding(building);

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
                        $scope.myBuildings.push(building);

                        $scope.anyService.selectedBuilding = $scope.myBuildings[$scope.myBuildings.length - 1];

                        $scope.myMarkers[id].marker.setDraggable(false);

                        $scope.myBuildingsHashT[building.buid] = {
                            marker: $scope.myMarkers[id].marker,
                            model: building
                        };

                        if ($scope.myMarkers[id].infowindow) {
                            $scope.myMarkers[id].infowindow.setContent($scope.myMarkers[id].marker.tpl2[0]);
                            $scope.myMarkers[id].infowindow.close();
                        }

                        _suc("Building added successfully.");

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
        }
    });

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

                                            var tmp = {
                                                name: sPoi.name,
                                                description: sPoi.description,
                                                pois_type: sPoi.pois_type,
                                                coordinates_lat: sPoi.coordinates_lat,
                                                coordinates_lon: sPoi.coordinates_lon
                                            };

                                            if (sPoi.is_building_entrance) {
                                                tmp.is_building_entrance = 'true';
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

app.controller('ControlBarController', ['$scope', '$rootScope', 'AnyplaceService', 'AnyplaceAPIService', function ($scope, $rootScope, AnyplaceService, AnyplaceAPIService) {

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

    $scope.setAuthenticated = function (bool) {
        $scope.isAuthenticated = bool;
    };

    $scope.showFullControls = true;

    $scope.toggleFullControls = function() {
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

    $scope.signinCallback = function (authResult) {
        if (authResult['status']['signed_in']) {
            // Update the app to reflect a signed in user
            // Hide the sign-in button now that the user is authorized, for example:
            // document.getElementById('signinButton').setAttribute('style', 'display: none');
            $scope.setAuthenticated(true);
            $scope.gAuth = authResult;

            app.access_token = authResult.access_token;

            gapi.client.load('plus', 'v1', apiClientLoaded);

        } else {
            // Update the app to reflect a signed out user
            // Possible error values:
            //   "user_signed_out" - User is signed-out
            //   "access_denied" - User denied access to your app
            //   "immediate_failed" - Could not automatically log in the user
            console.log('Sign-in state: ' + authResult['error']);
        }

    };

    $scope.personLookUp = function (resp) {
        $scope.person = resp;

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
        gapi.auth.signOut();
        $scope.isAuthenticated = false;

        $scope.$broadcast('loggedOff', []);
        $scope.gAuth = {};
        $scope.owner_id = undefined;
        $scope.person = undefined;
    };

    $scope.tab = 1;

    $scope.setTab = function (num) {
        $scope.tab = num;
    };

    $scope.isTabSet = function (num) {
        return $scope.tab === num;
    };


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

/*
 Floor Schema:
 {
 bottom_left_lat: "35.164948104577476"
 bottom_left_lng: "129.133842587471"
 buid: "building_2f25420e-3cb1-4bc1-9996-3939e5530d30_1414014035379"
 description: "First Floor"
 floor_name: "First Floor"
 floor_number: "1"
 fuid: "building_2f25420e-3cb1-4bc1-9996-3939e5530d30_1414014035379_1"
 is_published: "true"
 top_right_lat: "35.1664873646166"
 top_right_lng: "129.13621366024017"
 }
 */
app.controller('FloorController', ['$scope', 'AnyplaceService', 'GMapService', 'AnyplaceAPIService', function ($scope, AnyplaceService, GMapService, AnyplaceAPIService) {
    $scope.anyService = AnyplaceService;
    $scope.anyAPI = AnyplaceAPIService;
    $scope.gmapService = GMapService;

    $scope.xFloors = [];

    $scope.myFloors = {};
    $scope.myFloorId = 0;

    $scope.newFloorNumber = 0;

    var heatmap;

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

    $scope.$watch('anyService.selectedBuilding', function (newVal, oldVal) {
        if (newVal) {
            if (heatmap)
                heatmap.setMap(null);
            $scope.fetchAllFloorsForBuilding(newVal);
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
        if (newVal !== undefined && newVal !== null) {
            $scope.fetchFloorPlanOverlay(newVal);
            GMapService.gmap.panTo(_latLngFromBuilding($scope.anyService.selectedBuilding));
            GMapService.gmap.setZoom(19);

            if (heatmap)
                heatmap.setMap(null);

            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem("lastFloor", newVal.floor_number);
            }
        }
    });

    $scope.orderByFloorNo = function (floor) {
        if (!floor || LPUtils.isNullOrUndefined(floor.floor_number)) {
            return 0;
        }
        return parseInt(floor.floor_number);
    };

    $scope.fetchAllFloorsForBuilding = function (b) {
        // TODO: check for b.buid
        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = b.buid;

        var promise = AnyplaceAPIService.allBuildingFloors(jsonReq);
        promise.then(
            function (resp) {
                $scope.xFloors = resp.data.floors;
                // Set default selected
                var found = false;
                if (typeof(Storage) !== "undefined" && localStorage && !LPUtils.isNullOrUndefined(localStorage.getItem('lastBuilding')) && !LPUtils.isNullOrUndefined(localStorage.getItem('lastFloor'))) {
                    for (var i = 0; i < $scope.xFloors.length; i++) {
                        if (String($scope.xFloors[i].floor_number) === String(localStorage.getItem('lastFloor'))) {
                            $scope.anyService.selectedFloor = $scope.xFloors[i];
                            found = true;
                            break;
                        }
                    }
                }

                if (!found && $scope.xFloors[0]) {
                    $scope.anyService.selectedFloor = $scope.xFloors[0];
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
            $scope.data.floor_plan_groundOverlay.setMap($scope.gmapService.gmap);
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

    }

    $scope.toggleRadioHeatmap = function () {
        if (heatmap && heatmap.getMap()) {
            heatmap.setMap(null);
            return;
        }

        $scope.showRadioHeatmap();
    };

    $scope.getHeatMapButtonText = function () {
        return heatmap && heatmap.getMap() ? "Hide WiFi Map" : "Show WiFi Map";
    };

    $scope.showRadioHeatmap = function () {
        var jsonReq = {};
        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;
        jsonReq.buid = $scope.anyService.getBuildingId();
        jsonReq.floor = $scope.anyService.getFloorNumber();

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

/*
 POI Schema:
 {
 buid: "building_2f25420e-3cb1-4bc1-9996-3939e5530d30_1414014035379"
 coordinates_lat: "35.1660861073284"
 coordinates_lon: "129.13564771413803"
 description: "4B Hall"
 floor_name: "First Floor"
 floor_number: "1"
 geometry: {
 coordinates: [35.1660861073284, 129.13564771413803],
 type: "Point"
 }
 coordinates: [35.1660861073284, 129.13564771413803]
 0: 35.1660861073284
 1: 129.13564771413803
 type: "Point"
 image: "url_to_pois_image"
 is_building_entrance: "false"
 is_door: "false"
 is_published: "true"
 name: "4B Hall"
 pois_type: "Room"
 puid: "poi_057ce88f-01c4-4cf7-b39e-992f73b6b68d"
 url: ""
 }

 Connection Schema:
 {
 buid: "building_2f25420e-3cb1-4bc1-9996-3939e5530d30_1414014035379"
 buid_a: "building_2f25420e-3cb1-4bc1-9996-3939e5530d30_1414014035379"
 buid_b: "building_2f25420e-3cb1-4bc1-9996-3939e5530d30_1414014035379"
 cuid: "conn_poi_0f8916c8-b57d-44a3-a568-6042cbc173b7_poi_1de0950f-9ef3-4f01-b06e-f24fb42eb9dd"
 edge_type: "hallway"
 floor_a: "1"
 floor_b: "1"
 is_published: "true"
 pois_a: "poi_0f8916c8-b57d-44a3-a568-6042cbc173b7"
 pois_b: "poi_1de0950f-9ef3-4f01-b06e-f24fb42eb9dd"
 weight: "0.0197223019334019"
 }

 */
app.controller('PoiController', ['$scope', '$compile', 'GMapService', 'AnyplaceService', 'AnyplaceAPIService', function ($scope, $compile, GMapService, AnyplaceService, AnyplaceAPIService) {

    $scope.anyService = AnyplaceService;
    $scope.anyAPI = AnyplaceAPIService;

    /*
     myMarkers {
     1: {
     model: {
     name:
     description:
     type:
     isEntrance:
     floor:
     },
     marker: { }
     }
     */
    $scope.myMarkers = {};
    $scope.myMarkerId = 0;

    $scope.myPaths = {};
    $scope.myPathId = 0;

    $scope.myPois = [];
    $scope.myPoisHashT = {};

    $scope.myConnectionsHashT = {};

    $scope.poisTypes = ["Disabled Toilets", "Elevator", "Entrance", "Fire Extinguisher", "First Aid/AED", "Kitchen", "Office", "Ramp", "Room", "Security/Guard", "Stair", "Toilets", "Other"];

    $scope.edgeMode = false;
    $scope.connectPois = {
        prev: undefined,
        next: undefined
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
        $scope.edgeMode = false;
        $scope.connectPois = {
            prev: undefined,
            next: undefined
        };
    });

    $scope.$watch('anyService.selectedFloor', function (newVal, oldVal) {
        if (newVal !== undefined && newVal !== null) {
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

    $scope.onInfoWindowKeyDown = function (e) {
        // esc key
        if (e.keyCode == 27) {
            if ($scope.anyService.selectedPoi) {
                var p = $scope.anyService.selectedPoi;
                if ($scope.myPoisHashT[p.puid] && $scope.myPoisHashT[p.puid].marker && $scope.myPoisHashT[p.puid].marker.infowindow) {
                    $scope.myPoisHashT[p.puid].marker.infowindow.setMap(null);
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
    };

    $scope.clearConnectionsOnMap = function () {
        if ($scope.myConnectionsHashT) {
            for (var con in $scope.myConnectionsHashT) {
                if (con && $scope.myConnectionsHashT.hasOwnProperty(con) && $scope.myConnectionsHashT[con] && $scope.myConnectionsHashT[con].polyLine) {
                    $scope.myConnectionsHashT[con].polyLine.setMap(null);
                }
            }
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

                //var connections = JSON.parse( data.connections );
                var connections = data.connections;

                var hasht = {};
                var sz = connections.length;
                for (var i = 0; i < sz; i++) {
                    // insert the pois inside the hashtable
                    hasht[connections[i].cuid] = connections[i];
                }
                $scope.myConnectionsHashT = hasht;

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

                if (!conn || !conn.pois_a || !conn.pois_b ||
                    !$scope.myPoisHashT[conn.pois_a] ||
                    !$scope.myPoisHashT[conn.pois_b] ||
                    !$scope.myPoisHashT[conn.pois_a].model ||
                    !$scope.myPoisHashT[conn.pois_b].model) {
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

                flightPath.setMap(GMapService.gmap);
                flightPath.model = conn;

                $scope.myConnectionsHashT[cuid].polyLine = flightPath;

                google.maps.event.addListener(flightPath, 'click', function () {
                    $scope.$apply(_deleteConnection(this));
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

            var p = $scope.myPois[i];

            if (localPuid && p.puid === localPuid) {
                localPoiIndex = i;
            }

            var marker;
            var htmlContent = '-';

            if (p.pois_type == "None") {
                marker = new google.maps.Marker({
                    position: _latLngFromPoi(p),
                    map: GMapService.gmap,
                    draggable: true,
                    icon: new google.maps.MarkerImage(
                        _POI_CONNECTOR_IMG,
                        null, /* size is determined at runtime */
                        null, /* origin is 0,0 */
                        null, /* anchor is bottom center of the scaled image */
                        new google.maps.Size(21, 21)
                    )
                });

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
                    map: GMapService.gmap,
                    draggable: true,
                    icon: new google.maps.MarkerImage(
                        imgType,
                        null, /* size is determined at runtime */
                        null, /* origin is 0,0 */
                        null, /* anchor is bottom center of the scaled image */
                        size
                    )
                });

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
                $scope.clearConnectionsOnMap();

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

                    $scope.myPoisHashT[puid] = {};
                    $scope.myPoisHashT[puid].model = $scope.myPois[i];
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
                url: "-",
                image: "url_to_pois_image"
            };
            $scope.myMarkers[marker.myId].marker = marker;
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