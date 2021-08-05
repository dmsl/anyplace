/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Kyriakos Georgiou
 *
 * Supervisor: Demetrios Zeinalipour-Yazti
 *
 * URL: http://anyplace.cs.ucy.ac.cy
 * Contact: anyplace@cs.ucy.ac.cy
 *
 * Copyright (c) 2015, Data Management Systems Lab (DMSL), University of Cyprus.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 */
app.controller('BuildingController', ['$scope', '$compile', 'GMapService', 'AnyplaceService', 'AnyplaceAPIService','$timeout', '$q', '$log', function BuildingController ($scope, $compile, GMapService, AnyplaceService, AnyplaceAPIService,$timeout, $q, $log) {

    $scope.gmapService = GMapService;
    $scope.anyService = AnyplaceService;
    $scope.anyAPI = AnyplaceAPIService;

    $scope.creds = {
        username: undefined,
        password: undefined
    };

    $scope.user = {
        name: undefined,
        email: undefined,
        username: undefined,
        password: undefined,
        owner_id: undefined,
        access_token: undefined
    }

    $scope.myBuildings = [];

    $scope.myBuildingsHashT = {};
    $scope.mylastquery = "";
    $scope.myallPois = [];
    $scope.myallPoisHashT = {};
    $scope.myallEntrances = [];

    $scope.fetchVersion = function () {
        var jsonReq = {};
        var promise = $scope.anyAPI.version(jsonReq);
        promise.then(
            function (resp) { // on success
                var data = resp.data;
                var prettyVersion=getPrettyVersion(data);
                LOG.D3("Anyplace Version: " + prettyVersion);
                var element = document.getElementById("anyplace-version");
                element.textContent = "v"+prettyVersion;
            },
            function (resp) { console.log("Failed to get version: " + resp.data); }
        );
    };
    $scope.fetchVersion();

    var markerCluster = new MarkerClusterer($scope.gmapService.gmap);

    var self = this;
    self.SearchBuilding = SearchBuilding;

    function SearchBuilding () {

        if (document.getElementById("myBuildingSelected").value == ""){
            return $scope.myBuildings;
        }

        if (document.getElementById("myBuildingSelected").value == $scope.mylastquery){
            return $scope.data;
        }

        $scope.data = [];
        var size = 0;
        for (var i = 0; i < $scope.myBuildings.length; i++) {
            var b = $scope.myBuildings[i];
            if (b.name.toLowerCase().indexOf(document.getElementById("myBuildingSelected").value.toLowerCase()) > -1){
                $scope.data[size]=b;
                size++;
            }
        }

        $scope.mylastquery = document.getElementById("myBuildingSelected").value;
        return $scope.data;
    }

    var _setBuildingMarkesVisibility = function (bool) {

        for (var buid in $scope.myBuildingsHashT) {
            if ($scope.myBuildingsHashT.hasOwnProperty(buid)) {
                $scope.myBuildingsHashT[buid].marker.setVisible(bool);
            }
        }
    };

    $scope.$watch('anyService.selectedBuilding', function (newVal, oldVal) {

        if (newVal && newVal.coordinates_lat && newVal.coordinates_lon) {
            // Hide the building's marker for less clutter
            if (!newVal.buid) {
                _err($scope, "Some information is missing from the building and it could not be loaded.");
                return;
            }


            // If you choose to hide all the building markers when a building is selected
            // _setBuildingMarkesVisibility(false);

            // Show last building's marker
            if (oldVal && oldVal.buid) {
                $scope.myBuildingsHashT[oldVal.buid].marker.setVisible(true);
            }

            // hide current buildings marker
            $scope.myBuildingsHashT[newVal.buid].marker.setVisible(false);

            // Pan map to selected building
            $scope.gmapService.gmap.panTo(_latLngFromBuilding(newVal));
            $scope.gmapService.gmap.setZoom(20);

            try {
                if (typeof(Storage) !== "undefined" && localStorage) {
                    localStorage.setItem("lastBuilding", newVal.buid);
                }
            } catch (e) {

            }
        } else {
            _setBuildingMarkesVisibility(true);
        }

        if (newVal && newVal.puid){
            for (var i = 0; i < $scope.myBuildings.length; i++) {
                if ($scope.myBuildings[i].buid == newVal.buid){
                    newVal.name = $scope.myBuildings[i].name;
                }
            }
        }
    });

    var _latLngFromBuilding = function (b) {
        if (b && b.coordinates_lat && b.coordinates_lon) {
            return {
                lat: parseFloat(b.coordinates_lat),
                lng: parseFloat(b.coordinates_lon)
            }
        }
        return undefined;
    };

    $scope.fetchBuilding = function (buid) {

        var jsonReq = { buid: buid };

        var promise = $scope.anyAPI.getOneBuilding(jsonReq);

        promise.then(
            function (resp) {
                var data = resp.data;
                var b = data.space;

                $scope.myBuildings.push(b);

                // var s = new google.maps.Size(55, 80);
                // if ($scope.isFirefox)
                //     s = new google.maps.Size(110, 160);
                // var marker = new google.maps.Marker({
                //     position: _latLngFromBuilding(b),
                //     icon: {
                //         url: 'build/images/building-icon.png',
                //         size: s,
                //         scaledSize: new google.maps.Size(55, 80)
                //     },
                //     draggable: false
                // });
                // markerCluster.addMarker(marker);
                var marker = getMapsIconBuildingViewer($scope, _latLngFromBuilding(b));
                markerCluster.addMarker(marker);

                var htmlContent = '<div class="infowindow-scroll-fix">'
                    + '<h5>Building:</h5>'
                    + '<span>' + b.name + '</span>'
                    + '<h5>Description:</h5>'
                    + '<textarea class="infowindow-text-area"  rows="3" readonly>' + b.description + '</textarea>'
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

                    setTimeout(function () {
                        infowindow.setMap(null);
                    }, 2000);

                    var self = this;
                    $scope.$apply(function () {
                        $scope.anyService.selectedBuilding = self.building;
                    });
                });

                $scope.anyService.selectedBuilding = b;
            },
            function (resp) {
                ShowError($scope, resp, "No matching building found", true);
            }
        )

    };

    $scope.getCampuses = function () {
        var jsonReq = { "access-control-allow-origin": "",    "content-encoding": "gzip",    "access-control-allow-credentials": "true",    "content-length": "17516",    "content-type": "application/json" , "cuid":$scope.urlCampus};
        var promise = $scope.anyAPI.allCucodeCampus(jsonReq);
        promise.then(
            function (resp) { // on success
                var data = resp.data;
                //var bs = JSON.parse( data.buildings );
                $scope.myBuildings = data.spaces;
                $scope.anyService.selectedCampus = data.name;
                var infowindow = new google.maps.InfoWindow({
                    content: '-',
                    maxWidth: 500
                });
                var localStoredBuildingIndex = -1;
                var localStoredBuildingId = undefined;
                try {
                    if (typeof(Storage) !== "undefined" && localStorage && localStorage.getItem('lastBuilding')) {
                        localStoredBuildingId = localStorage.getItem('lastBuilding');
                    }
                } catch (e) {
                }
                var loadBuidFromUrl = -1;
                for (var i = 0; i < $scope.myBuildings.length; i++) {
                    var b = $scope.myBuildings[i];
                    if (i==0){
                        $scope.gmapService.gmap.panTo(_latLngFromBuilding(b));
                        $scope.gmapService.gmap.setZoom(13);
                    }
                    if (localStoredBuildingId && localStoredBuildingId === b.buid) {
                        localStoredBuildingIndex = i;
                    }
                    if (b.is_published === 'true' || b.is_published == true) {
                        b.is_published = true;
                    } else {
                        b.is_published = false;
                    }
                    if ($scope.urlBuid && $scope.urlBuid == b.buid) {
                        loadBuidFromUrl = i;
                    }
                    // var s = new google.maps.Size(55, 80);
                    // if ($scope.isFirefox)
                    //     s = new google.maps.Size(110, 160);
                    // var marker = new google.maps.Marker({
                    //     position: _latLngFromBuilding(b),
                    //     icon: {
                    //         url: 'build/images/building-icon.png',
                    //         size: s,
                    //         scaledSize: new google.maps.Size(55, 80)
                    //     },
                    //     draggable: false,
                    //     title: b.name
                    // });
                    // markerCluster.addMarker(marker);
                    var marker = getMapsIconBuildingViewer($scope, _latLngFromBuilding(b));
                    markerCluster.addMarker(marker);

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

                        setTimeout(function () {
                            infowindow.setMap(null);
                        }, 2000);

                        var self = this;
                        $scope.$apply(function () {
                            $scope.anyService.selectedBuilding = self.building;
                        });
                    });
                }
                if (loadBuidFromUrl > -1) {
                    $scope.anyService.selectedBuilding = $scope.myBuildings[loadBuidFromUrl];
                } else if ($scope.urlBuid) {
                    $scope.fetchBuilding($scope.urlBuid);
                } else if (localStoredBuildingIndex >= 0) {
                    // using the latest building form localStorage
                    $scope.anyService.selectedBuilding = $scope.myBuildings[localStoredBuildingIndex];
                }
                $scope.anyService.BuildingsLoaded=false;
            },
            function (resp) {
              ShowError($scope, resp, ERR_FETCH_BUILDINGS);
            }
        );
    };
    $scope.getCampuses();

    var _clearBuildingMarkersAndModels = function () {
        for (var b in $scope.myBuildingsHashT) {
            if ($scope.myBuildingsHashT.hasOwnProperty(b)) {
                $scope.myBuildingsHashT[b].marker.setMap(null);
                delete $scope.myBuildingsHashT[b];
            }
        }
    };

    var _calcDistance = function (x1, y1, x2, y2) {
        return Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
    };

    $scope.orderByName = function (v) {
        return v.name;
    };

    $scope.orderByDistCentre = function (v) {
        if ($scope.anyService.selectedBuilding)
            return v.name;
        var c = $scope.gmapService.gmap.getCenter();
        return _calcDistance(parseFloat(v.coordinates_lat), parseFloat(v.coordinates_lon), c.lat(), c.lng());
    }

}]);
