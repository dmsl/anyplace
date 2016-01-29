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
    // Now there won't be a hashbang within URLs for browsers that support HTML5 history
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
