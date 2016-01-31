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

app.controller('PoiController', ['$scope', '$compile', 'GMapService', 'AnyplaceService', 'AnyplaceAPIService', function ($scope, $compile, GMapService, AnyplaceService, AnyplaceAPIService) {

    var _POI_CONNECTOR_IMG = 'build/images/edge-connector-icon.png';
    var _POI_EXISTING_IMG = 'build/images/any-poi-icon-blue.png';
    var _POI_NEW_IMG = 'build/images/poi-icon.png';

    var _MARKERS_IMG_RAW_SIZE = new google.maps.Size(62, 93);
    var _MARKERS_SIZE_NORMAL = new google.maps.Size(21, 32);
    var _MARKERS_SIZE_BIG = new google.maps.Size(31, 48);

    var HIDE_POIS_ZOOM_LEVEL = 17;

    $scope.anyService = AnyplaceService;
    $scope.anyAPI = AnyplaceAPIService;
    $scope.gmapService = GMapService;

    $scope.myPois = [];
    $scope.myPoisHashT = {};

    $scope.myEntrances = [];

    $scope.showPoiDescription = false;
    $scope.poiShareUrl = {
        puid: undefined,
        url: '...'
    };

    $scope.poiRouteState = {
        form: undefined
    };

    $scope.togglePoiDescription = function () {
        $scope.showPoiDescription = !$scope.showPoiDescription;
    };

    var poiClosestToUserPos = undefined;

    var poiRoutePolyline = {};
    var auxPoiRoutePolyline = undefined;
    var userToPoiPolyline = undefined;

    var arePoisLoaded = false;
    var prevSelectedPoi = undefined;

    google.maps.event.addListener($scope.gmapService.gmap, 'zoom_changed', function () {
        var zoom = this.getZoom();
        // iterate over markers and call setVisible
        if (zoom <= HIDE_POIS_ZOOM_LEVEL) {
            _hidePoisOnMap();
        } else {
            _makeVisiblePoisOnMap();
        }
    });

    var _closeActiveInfoWindow = function () {
        if ($scope.anyService.selectedPoi) {
            var poi = $scope.anyService.selectedPoi;
            if (poi.puid && $scope.myPoisHashT[poi.puid] && $scope.myPoisHashT[poi.puid].marker && $scope.myPoisHashT[poi.puid].marker.infowindow) {
                $scope.myPoisHashT[poi.puid].marker.infowindow.setMap(null);
            }
        }
    };

    // hide the info window on click anywhere on the map..
    google.maps.event.addListener($scope.gmapService.gmap, 'click', function () {
        _closeActiveInfoWindow();
    });

    $scope.$watch('anyService.selectedBuilding', function (newVal, oldVal) {
        if (newVal && newVal.buid) {
            arePoisLoaded = false;
            $scope.removePoisOnMap();
            $scope.myEntrances = [];
            $scope.myPoisHashT = {};
            $scope.fetchAllPoisForBuilding(newVal);
            $scope.clearNavPolylines();
        } else {
            $scope.anyService.selectedPoi = undefined;
            $scope.removePoisOnMap();
            $scope.clearNavPolylines();
        }
    });

    $scope.$watch('anyService.selectedFloor', function (newVal, oldVal) {
        if (newVal !== undefined && newVal !== null && arePoisLoaded) {
            if (newVal.floor_number) {
                $scope.showPoisOnlyForFloor(newVal.floor_number);
            }
            _displayPolylineForFloor(newVal, oldVal);
        } else {
            $scope.anyService.selectedPoi = undefined;
        }
    });

    $scope.$watch('anyService.selectedPoi', function (newVal, oldVal) {
        if (newVal && newVal.puid && _latLngFromPoi(newVal)) {

            $scope.poiShareUrl.puid = undefined;
            $scope.poiShareUrl.url = '...';

            if ($scope.anyService.getFloorNumber() != newVal.floor_number) {
                $scope.anyService.setSelectedFloorByNum(newVal.floor_number);
            }

            GMapService.gmap.panTo(_latLngFromPoi(newVal));
            GMapService.gmap.setZoom(20);

            // make marker bigger and open infowindow
            if (newVal.puid && $scope.myPoisHashT[newVal.puid] && $scope.myPoisHashT[newVal.puid].marker) {
                var m = $scope.myPoisHashT[newVal.puid].marker;

                m.setVisible(true);
                m.setIcon(_getBiggerPoiIcon(newVal));

                if (m.infowindow) {
                    m.infowindow.setContent(m.tpl2);
                    m.infowindow.open(GMapService.gmap, m);
                }
            }

            // make previous selected POI's marker smaller
            if (prevSelectedPoi && prevSelectedPoi.puid && $scope.myPoisHashT[prevSelectedPoi.puid] && $scope.myPoisHashT[prevSelectedPoi.puid].marker) {
                $scope.myPoisHashT[prevSelectedPoi.puid].marker.setIcon(_getNormalPoiIconNormal(prevSelectedPoi));
                if (GMapService.gmap.getZoom() <= HIDE_POIS_ZOOM_LEVEL) {
                    $scope.myPoisHashT[prevSelectedPoi.puid].marker.setVisible(false);
                }
            }

            try {
                if (typeof(Storage) !== "undefined" && localStorage)
                    localStorage.setItem("lastPoi", newVal.puid);
            } catch (e) {
            }

            prevSelectedPoi = newVal;

        }
    });

    var _err = function (msg) {
        $scope.anyService.addAlert('danger', msg);
    };

    var _suc = function (msg) {
        $scope.anyService.addAlert('success', msg);
    };

    var _warn = function (msg) {
        $scope.anyService.addAlert('warning', msg);
    };

    var _latLngFromPoi = function (p) {
        if (p && p.coordinates_lat && p.coordinates_lon) {
            return {lat: parseFloat(p.coordinates_lat), lng: parseFloat(p.coordinates_lon)}
        }
        return undefined;
    };

    $scope.orderByName = function (value) {
        return value.name;
    };

    $scope.orderByFloor = function (value) {
        return parseInt(value.floor_number) + value.name;
    };

    var _getImageIconForPoi = function (poi) {
        var img = 'build/images/any-poi-icon-blue.png';

        if (poi.is_building_entrance) {
            img = 'build/images/poi_icon_entrance-green.png';
        } else if (poi.pois_type == "Stair") {
            img = 'build/images/poi_icon_stairs-orange.png';
        } else if (poi.pois_type == "Elevator") {
            img = 'build/images/poi_icon_elevator-purple.png';
        }

        return img;
    };

    var _getBiggerPoiIcon = function (poi) {
        var img = _getImageIconForPoi(poi).replace(/\-[a-z]+\.png/gi, "-red.png");

        var s = _MARKERS_SIZE_BIG;
        if ($scope.isFirefox)
            s = _MARKERS_IMG_RAW_SIZE;


        return {
            url: img,
            size: s,
            scaledSize: _MARKERS_SIZE_BIG
        }
    };

    var _getNormalPoiIconNormal = function (poi) {

        var s = _MARKERS_SIZE_NORMAL;
        if ($scope.isFirefox)
            s = _MARKERS_IMG_RAW_SIZE;

        return {
            url: _getImageIconForPoi(poi),
            size: s,
            scaledSize: _MARKERS_SIZE_NORMAL
        }
    };

    var _clearPoiRoutePolyline = function () {
        for (var fkey in poiRoutePolyline) {
            if (poiRoutePolyline.hasOwnProperty(fkey)) {
                if (poiRoutePolyline[fkey].polyline) {
                    poiRoutePolyline[fkey].polyline.setMap(null);
                }
            }
        }
        poiRoutePolyline = {};
    };

    var _displayPolylineForFloor = function (newFl, oldFl) {

        if (poiRoutePolyline) {
            if (newFl && newFl.floor_number) {
                var nf = newFl.floor_number;
                if (poiRoutePolyline[nf] && poiRoutePolyline[nf].polyline) {
                    poiRoutePolyline[nf].polyline.setMap($scope.gmapService.gmap);
                }
            }

            if (oldFl && oldFl.floor_number) {
                var of = oldFl.floor_number;
                if (poiRoutePolyline[of] && poiRoutePolyline[of].polyline) {
                    poiRoutePolyline[of].polyline.setMap(null);
                }
            }
        }
    };

    $scope.unselectBuilding = function () {
        $scope.anyService.selectedBuilding = undefined;

        // TODO: remove form local storage
        try {
            if (typeof(Storage) !== "undefined" && localStorage)
                localStorage.removeItem("lastBuilding");
                localStorage.removeItem("lastFloor");
                localStorage.removeItem("lastPoi");
        } catch (e) {
        }
        
    };

    $scope.clearNavPolylines = function () {

        _clearPoiRoutePolyline();

        if (auxPoiRoutePolyline)
            auxPoiRoutePolyline.setMap(null);

        if (GMapService.isRouteShown())
            GMapService.clearRoute();

        if (userToPoiPolyline)
            userToPoiPolyline.setMap(null);

        // hide the user's location marker in case the navigation happened for one time only
        // (the continuous localization was turned off)
        if (!$scope.getIsUserLocVisible()) {
            $scope.hideUserLocation();
        }
    };

    $scope.navRoutesShown = function () {
        for (var fkey in poiRoutePolyline) {
            if (poiRoutePolyline.hasOwnProperty(fkey)) {
                if (poiRoutePolyline[fkey].polyline && poiRoutePolyline[fkey].polyline.getMap()) {
                    return true;
                }
            }
        }

        if (auxPoiRoutePolyline && auxPoiRoutePolyline.getMap())
            return true;

        if (GMapService.isRouteShown())
            return true;

        return false;
    };

    $scope.zoomInPoi = function () {
        $scope.gmapService.gmap.panTo(_latLngFromPoi($scope.anyService.selectedPoi));
        $scope.gmapService.gmap.setZoom(20);
    };

    $scope.fetchAllPoisForBuilding = function (building) {

        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = building.buid;

        var promise = AnyplaceAPIService.retrievePoisByBuilding(jsonReq);
        promise.then(
            function (resp) {
                var data = resp.data;

                $scope.myPois = data.pois;

                var sz = $scope.myPois.length;
                for (var i = sz - 1; i >= 0; i--) {
                    var puid = $scope.myPois[i].puid;

                    if ($scope.myPois[i].is_building_entrance == 'true') {
                        $scope.myPois[i].is_building_entrance = true;
                        // add poi to entrances array for faster processing later
                        $scope.myEntrances.push($scope.myPois[i]);
                    } else {
                        $scope.myPois[i].is_building_entrance = false;
                    }

                    // insert the pois inside the hashtable
                    $scope.myPoisHashT[puid] = {};
                    $scope.myPoisHashT[puid].model = $scope.myPois[i];
                }

                arePoisLoaded = true;

                $scope.drawPoisOnMapForBuilding();
                $scope.showPoisOnlyForFloor($scope.anyService.getFloorNumber());
            },
            function (resp) {
                var data = resp.data;
                _err("Something went wrong while fetching POIs");
            }
        );
    };

    $scope.retrieveRouteFromPoiToPoi = function (from, to) {

        if (!from || !from.puid) {
            _err("Source POI is corrupted.");
            return;
        }

        if (!to || !to.puid) {
            _err("Source POI is corrupted.");
            return;
        }

        var jsonReq = $scope.creds;

        jsonReq.pois_from = from.puid;
        jsonReq.pois_to = to.puid;

        var promise = AnyplaceAPIService.retrieveRouteFromPoiToPoi(jsonReq);
        promise.then(
            function (resp) {
                var data = resp.data;

                var listPois = data.pois;

                _clearPoiRoutePolyline();

                for (var i = 0; i < listPois.length; i++) {
                    if (listPois[i].lat && listPois[i].lon && listPois[i].floor_number) {
                        var fl = listPois[i].floor_number;

                        if (!poiRoutePolyline.hasOwnProperty(fl)) {
                            poiRoutePolyline[fl] = {
                                flightPlanCoordinates: []
                            };
                        }

                        poiRoutePolyline[fl].flightPlanCoordinates.push(new google.maps.LatLng(
                            parseFloat(listPois[i].lat),
                            parseFloat(listPois[i].lon)
                        ));
                    }
                }

                for (var fkey in poiRoutePolyline) {
                    if (poiRoutePolyline.hasOwnProperty(fkey)) {

                        poiRoutePolyline[fkey].polyline = new google.maps.Polyline({
                            path: poiRoutePolyline[fkey].flightPlanCoordinates,
                            geodesic: true,
                            strokeColor: '#FF0000',
                            strokeOpacity: 0.75,
                            strokeWeight: 6
                        });

                        if ($scope.anyService.selectedFloor.floor_number == fkey)
                            poiRoutePolyline[fkey].polyline.setMap($scope.gmapService.gmap);
                    }
                }

                // user is in the building
                if (poiClosestToUserPos) {

                    var lineSymbol = {
                        path: 'M 0,-1 0,1',
                        strokeOpacity: 0.75,
                        scale: 4
                    };

                    userToPoiPolyline = new google.maps.Polyline({
                        path: [
                            new google.maps.LatLng(parseFloat(from.coordinates_lat), parseFloat(from.coordinates_lon)),
                            new google.maps.LatLng($scope.userPosition.lat, $scope.userPosition.lng)
                        ],
                        geodesic: true,
                        strokeColor: '#FF0000',
                        strokeOpacity: 0,
                        strokeWeight: 6,
                        icons: [{
                            icon: lineSymbol,
                            offset: '0',
                            repeat: '20px'
                        }]
                    });

                    userToPoiPolyline.setMap($scope.gmapService.gmap);

                    poiClosestToUserPos = undefined;
                }
            },
            function (resp) {
                var data = resp.data;
                _err("Something went wrong while fetching route.");
            }
        );

    };

    var _euclideanDistance = function (x1, y1, x2, y2) {
        return Math.sqrt((x2 - x1) * (x2 - x1) + (y2 - y1) * (y2 - y1));
    };

    /**
     *
     * @param a The last point of google navigation
     * @param b The target POI inside the building
     * @returns The entrance POI
     * @private
     */
    var _findClosestEntranceFrom2Points = function (a, b) {

        var xa = a.lat();
        var ya = a.lng();

        var xb = parseFloat(b.coordinates_lat);
        var yb = parseFloat(b.coordinates_lon);

        var entr = $scope.myEntrances;

        // If there are building entrance we will use those as entrance
        // point to our target destination
        if (!entr || !entr.length)
            entr = $scope.myPois;


        var minD = Number.MAX_VALUE;
        var minPoi = undefined;

        for (var i = 0; i < entr.length; i++) {
            if (entr[i].puid == b.puid)
                continue;

            var x = parseFloat(entr[i].coordinates_lat);
            var y = parseFloat(entr[i].coordinates_lon);

            var d;
            if (entr && entr.length > 0)
                d = _euclideanDistance(xa, ya, x, y) + _euclideanDistance(xb, yb, x, y);
            else
                d = _euclideanDistance(xa, ya, x, y);

            if (d < minD) {
                minD = d;
                minPoi = entr[i];
            }
        }

        return minPoi;
    };

    var _findClosestEntranceFrom1Point = function (b) {
        var xb = parseFloat(b.coordinates_lat);
        var yb = parseFloat(b.coordinates_lon);

        var entr = $scope.myEntrances;

        // If there are building entrance we will use those as entrance
        // point to our target destination
        if (!entr || !entr.length)
            entr = $scope.myPois;

        var minD = Number.MAX_VALUE;
        var minPoi = undefined;

        for (var i = 0; i < entr.length; i++) {
            if (entr[i].puid == b.puid) {
                // the poi is a door itself
                return b;
            }

            var x = parseFloat(entr[i].coordinates_lat);
            var y = parseFloat(entr[i].coordinates_lon);

            var d;
            if (entr && entr.length > 0)
                d = _euclideanDistance(xb, yb, x, y);

            if (d < minD) {
                minD = d;
                minPoi = entr[i];
            }
        }

        return minPoi;
    };

    $scope.getPoiShareUrl = function (puid) {

        if (puid === $scope.poiShareUrl.puid) {
            $scope.poiShareUrl.puid = undefined;
            return;
        }

        $scope.poiShareUrl.puid = puid;

        if (!$scope.anyService.selectedPoi || $scope.anyService.selectedPoi.puid != puid) {
            $scope.anyService.selectedPoi = $scope.myPoisHashT[puid].model;
        }

        var viewerUrl = $scope.anyService.getViewerUrl();

        $scope.poiShareUrl.embed = '<iframe width="100%" height="500" frameborder="0" scrolling="yes" marginheight="0" marginwidth="0" src="' + viewerUrl + '"></iframe>';

        var json_req = {
            longUrl: viewerUrl
        };

        var promise = $scope.anyAPI.googleUrlShortener(json_req);
        promise.then(
            function (resp) {
                $scope.poiShareUrl.url = resp.data.id;
                //prompt("Copy & Share:", resp.data.id);
            },
            function (resp) {
                $scope.poiShareUrl.url = viewerUrl;
                //prompt("Copy & Share:", viewerUrl);
            }
        );
    };

    $scope.startNavFromPoi = function () {
        $scope.poiRouteState.from = $scope.anyService.selectedPoi;
        _suc("Now you can click on another POI to draw the indoor path between the 2 points.");
    };

    $scope.getHtml5GeoLocation = function (callback, errcallback) {
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(callback, errcallback);
        } else {
            _err("The Geolocation feature is not supported by this browser.");
        }
    };

    $scope.navigateFromUserToPoiAux = function (position) {
        if (!position) {
            _err('Invalid position detected by HTML5.');
            return;
        }

        var lat = position.lat;
        var lng = position.lng;

        if (!$scope.userPosition) {
            $scope.userPosition = {lat: lat, lng: lng};
        }

        var latLngPos = new google.maps.LatLng(lat, lng);

        var targetPoi = $scope.anyService.selectedPoi;

        if (!targetPoi) {
            _err('No target poi found.');
            return;
        }

        if (_isPointInSelectedFloor(lat, lng)) {
            var minD = Number.MAX_VALUE;
            var minP = undefined;
            var ht = $scope.myPoisHashT;
            var d;
            for (var k in ht) {
                if (ht.hasOwnProperty(k)) {
                    var p = ht[k].model;
                    if (!p || p.floor_number != targetPoi.floor_number)
                        continue;
                    if ((d = _euclideanDistance(
                            parseFloat(p.coordinates_lat), parseFloat(p.coordinates_lon),
                            lat, lng)
                        ) <= minD) {
                        minD = d;
                        minP = p;
                    }
                }
            }
            poiClosestToUserPos = minP;

            $scope.retrieveRouteFromPoiToPoi(minP, targetPoi);

            if (!$scope.getIsUserLocVisible()) {
                $scope.displayMyLocMarker({lat: lat, lng: lng});
            }

            return;
        } else {
            poiClosestToUserPos = undefined;
        }

        var targetLatLng = new google.maps.LatLng(
            parseFloat(targetPoi.coordinates_lat),
            parseFloat(targetPoi.coordinates_lon));

        var targetEntrance = _findClosestEntranceFrom1Point(targetPoi);

        if (targetEntrance) {
            targetLatLng = new google.maps.LatLng(
                parseFloat(targetEntrance.coordinates_lat),
                parseFloat(targetEntrance.coordinates_lon));
        }

        $scope.gmapService.calcRoute(latLngPos, targetLatLng, function (lastPoint) {
            if (lastPoint) {
                var trgtPoi = $scope.anyService.selectedPoi;

                if (targetEntrance) {
                    var ep = targetEntrance;

                    var flightPath = [
                        lastPoint,
                        new google.maps.LatLng(parseFloat(ep.coordinates_lat), parseFloat(ep.coordinates_lon))
                    ];

                    if (auxPoiRoutePolyline)
                        auxPoiRoutePolyline.setMap(null);

                    var lineSymbol = {
                        path: 'M 0,-1 0,1',
                        strokeOpacity: 0.75,
                        scale: 4
                    };

                    auxPoiRoutePolyline = new google.maps.Polyline({
                        path: flightPath,
                        geodesic: true,
                        strokeColor: '#73B9FF',
                        strokeOpacity: 0,
                        strokeWeight: 6,
                        icons: [{
                            icon: lineSymbol,
                            offset: '0',
                            repeat: '20px'
                        }]
                    });

                    auxPoiRoutePolyline.setMap($scope.gmapService.gmap);

                    if (ep.puid != trgtPoi.puid)
                        $scope.retrieveRouteFromPoiToPoi(ep, trgtPoi);
                }

            }
        });
    };

    $scope.navigateFromUserToPoi = function (puid) {

        if (!$scope.anyService.selectedPoi || $scope.anyService.selectedPoi.puid != puid) {
            $scope.anyService.selectedPoi = $scope.myPoisHashT[puid].model;
        }

        if ($scope.getIsUserLocVisible() && $scope.userPosition) {
            $scope.navigateFromUserToPoiAux($scope.userPosition);
        } else {
            $scope.getHtml5GeoLocation(
                function (position) {
                    var lat = position.coords.latitude;
                    var lng = position.coords.longitude;
                    $scope.navigateFromUserToPoiAux({lat: lat, lng: lng});
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
                }
            );
        }
    };

    /**
     * Drawing, Showing, Removing, Hiding POIs
     */

    $scope.drawPoisOnMapForBuilding = function () {

        var infowindow = new google.maps.InfoWindow({
            content: '-',
            maxWidth: 500
        });

        var localPuid = undefined;
        var localPoiIndex = -1;

        try {
            if (typeof(Storage) !== "undefined" && localStorage
                && !LPUtils.isNullOrUndefined(localStorage.getItem('lastBuilding'))
                && !LPUtils.isNullOrUndefined(localStorage.getItem('lastFloor'))
                && !LPUtils.isNullOrUndefined(localStorage.getItem('lastPoi'))) {

                localPuid = localStorage.getItem('lastPoi');
            }
        } catch (e) {

        }

        var entranceFound = false;
        var entrancePoi = undefined;

        var urlPuidIndex = -1;

        for (var i = 0; i < $scope.myPois.length; i++) {

            var p = $scope.myPois[i];

            if (localPuid && p.puid === localPuid) {
                localPoiIndex = i;
            }

            if ($scope.urlPuid && $scope.urlPuid == p.puid) {
                urlPuidIndex = i;
            }

            var marker;
            var htmlContent = '-';

            if (p.pois_type == "None") {
                continue;
            }

            var imgType = _getImageIconForPoi(p);

            var size = _MARKERS_SIZE_NORMAL;

            if ($scope.isFirefox)
                size = new google.maps.Size(62, 93);

            var scaledSize = _MARKERS_SIZE_NORMAL;

            marker = new google.maps.Marker({
                position: _latLngFromPoi(p),
                map: GMapService.gmap,
                draggable: false,
                icon: {
                    url: imgType,
                    size: size,
                    scaledSize: scaledSize
                },
                visible: false
            });

            htmlContent = '<div class="iw infowindow-scroll-fix">'
            + '<div class="wordwrap" style="text-align: center">'
            + '<span ng-show="navRoutesShown()" id="info-window-zoomin" ng-click="zoomInPoi()"><img src="build/images/html5_location_icon.png"></span>'
            + '<span class="iw-poi-name">' + p.name + '</span></div>'
            + '<div class="wordwrap iw-poi-description" ng-show="showPoiDescription">' + p.description + '</div>'
            + '<div style="text-align: center">'
            + '<div class="poi-action-btn"><button class="btn btn-info" ng-click="togglePoiDescription()"><i class="fa fa-info-circle"></i></i></button></div>'
            + '<div class="poi-action-btn"><button class="btn btn-primary" ng-click="startNavFromPoi()"><i style="font-size: 12px;" class="fa fa-flag"></i></button></div>'
            + '<div class="poi-action-btn"><button class="btn btn-success" ng-click="navigateFromUserToPoi(\'' + p.puid + '\')"><i class="fa fa-location-arrow"></i></button></div>'
            + '<div class="poi-action-btn"><button class="btn btn-warning" ng-click="getPoiShareUrl(\'' + p.puid + '\')"><i class="fa fa-share-alt"></i></button></div>'
                //+ '<span id="info-window-nav-from-poi" ng-click="startNavFromPoi()"><img src="build/images/start-poi-nav.png"></span>'
                //+ '<div ng-show="navRoutesShown()" class="poi-action-btn"><button class="btn btn-primary" ng-click="zoomInPoi()"><i class="fa fa-crosshairs"></i></button></div>'
            + '</div>'
            + '<div ng-show="poiShareUrl.puid" style="margin-top: 2px">'
            + '<div>Share URL:</div>'
            + '<input class="form-control" value="{{poiShareUrl.url}}"/>'
            + '<div>Embed:</div>'
            + '<input class="form-control" value="{{poiShareUrl.embed}}"/>'
            + '</div>'
            + '</div>';

            var compiledTpl = $compile(htmlContent)($scope);

            marker.tpl2 = compiledTpl[0];
            marker.model = p;

            marker.infowindow = infowindow;

            $scope.myPoisHashT[p.puid].marker = marker;

            google.maps.event.addListener(marker, 'click', function () {
                var self = this;
                $scope.$apply(function () {
                    if (self.model && self.model.puid) {

                        if ($scope.poiRouteState.from) {
                            var fpoi = $scope.poiRouteState.from;

                            $scope.retrieveRouteFromPoiToPoi(fpoi, self.model);

                            if (fpoi && fpoi.puid && $scope.myPoisHashT[fpoi.puid] && $scope.myPoisHashT[fpoi.puid].marker) {
                                self.infowindow.setContent($scope.myPoisHashT[fpoi.puid].marker.tpl2);
                                self.infowindow.open(GMapService.gmap, $scope.myPoisHashT[fpoi.puid].marker);
                            }

                            $scope.poiRouteState.from = undefined;
                            return;
                        } else {
                            $scope.poiRouteState.from = undefined;
                            if ($scope.anyService.selectedPoi && $scope.anyService.selectedPoi.puid != self.model.puid && $scope.navRoutesShown())
                                $scope.clearNavPolylines();
                        }

                        if ($scope.anyService.selectedPoi && $scope.anyService.selectedPoi.puid == self.model.puid) {
                            self.infowindow.setContent(self.tpl2);
                            self.infowindow.open(GMapService.gmap, self);
                        }
                        $scope.anyService.selectedPoi = self.model;
                    }
                })
            });

            if (entranceFound && p.is_building_entrance) {
                entrancePoi = p;
                entranceFound = true;
            }
        }

        if (urlPuidIndex >= 0) {
            $scope.anyService.selectedPoi = $scope.myPois[urlPuidIndex];
        } else if (localPoiIndex >= 0) {
            $scope.anyService.selectedPoi = $scope.myPois[localPoiIndex];
        } else if (entranceFound && entrancePoi) {
            $scope.anyService.selectedPoi = entrancePoi;
        }
    };

    $scope.showPoisOnlyForFloor = function (floor_num) {
        for (var i = 0; i < $scope.myPois.length; i++) {
            var p = $scope.myPois[i];

            if (p && $scope.myPoisHashT[p.puid] && $scope.myPoisHashT[p.puid].marker) {
                var mrkr = $scope.myPoisHashT[p.puid].marker;

                if (p.floor_number != floor_num) {
                    mrkr.setVisible(false);
                    if (mrkr.infowindow && mrkr.infowindow.getMap()) {
                        mrkr.infowindow.setMap(null);
                    }
                } else {
                    mrkr.setVisible(true);
                }
            }
        }
    };

    $scope.removePoisOnMap = function () {
        for (var i = 0; i < $scope.myPois.length; i++) {
            var p = $scope.myPois[i];
            if (p && $scope.myPoisHashT[p.puid] && $scope.myPoisHashT[p.puid].marker) {
                $scope.myPoisHashT[p.puid].marker.setMap(null);
                delete $scope.myPoisHashT[p.puid];
            }
        }
    };

    var _hidePoisOnMap = function () {
        for (var i = 0; i < $scope.myPois.length; i++) {
            var p = $scope.myPois[i];

            if ($scope.anyService.selectedPoi && p.puid == $scope.anyService.selectedPoi.puid)
                continue;

            if (p && $scope.myPoisHashT[p.puid] && $scope.myPoisHashT[p.puid].marker) {
                var mrkr = $scope.myPoisHashT[p.puid].marker;
                mrkr.setVisible(false);
            }
        }
    };

    var _makeVisiblePoisOnMap = function () {
        for (var i = 0; i < $scope.myPois.length; i++) {
            var p = $scope.myPois[i];
            if (p.floor_number != $scope.anyService.getFloorNumber())
                continue;
            if (p && $scope.myPoisHashT[p.puid] && $scope.myPoisHashT[p.puid].marker) {
                var mrkr = $scope.myPoisHashT[p.puid].marker;
                mrkr.setVisible(true);
            }
        }
    };

    var _isPointInSelectedFloor = function (x, y) {
        var f = $scope.anyService.selectedFloor;
        if (!f)
            return false;

        var bllat = parseFloat(f.bottom_left_lat);
        var bllng = parseFloat(f.bottom_left_lng);

        var trlat = parseFloat(f.top_right_lat);
        var trlng = parseFloat(f.top_right_lng);

        var radius = _euclideanDistance(bllat, bllng, trlat, trlng) / 2;

        var centre_x = (bllat + trlat) / 2;
        var centre_y = (bllng + trlng) / 2;

        var resd = _euclideanDistance(centre_x, centre_y, x, y);
        return (resd <= radius)
    }
}
]);