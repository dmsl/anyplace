/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Costantinos Costa, Kyriakos Georgiou
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
    // 21, 32 old size
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


    $scope.mylastquery = "";
    $scope.myallPois = [];
    var self = this;
    self.querySearch = querySearch;

    function querySearch (query){
        if (query == ""){
            return $scope.myPois;
        }
        if (query == $scope.mylastquery){
            return $scope.myallPois;
        }
        $scope.anyService.selectedSearchPoi = query;
        setTimeout(
            function(){
                if (query==$scope.anyService.selectedSearchPoi ){
                    window.stop();
                    $scope.fetchAllPoi(query, $scope.anyService.selectedBuilding.buid);
                }
            },200);
        $scope.mylastquery = query;
        return
    }

    $scope.fetchAllPoi = function (letters , buid) {

        var jsonReq = { "access-control-allow-origin": "",    "content-encoding": "gzip",    "access-control-allow-credentials": "true",    "content-length": "17516",    "content-type": "application/json" , "buid":buid, "cuid":"", "letters":letters , "greeklish":$scope.greeklish};
        var promise = AnyplaceAPIService.retrieveALLPois(jsonReq);
        promise.then(
            function (resp) {
                var data = resp.data;
                $scope.myallPois = data.pois;
            },
            function (resp) {
                if (letters=="") {
                    ShowError($scope, resp, "Something went wrong while fetching POIs", true);
                }
            }
        );
    };



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

            if (newVal && newVal.puid && _latLngFromPoi(newVal)) {
                if (newVal.floor_number) {
                    $scope.showPoisOnlyForFloor(newVal.floor_number);
                }
                _displayPolylineForFloor(newVal, oldVal);
                $scope.showSelectedPoi(newVal, oldVal);
            }

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
            if (!arePoisLoaded){
                window.stop();
            }
            $scope.showSelectedPoi(newVal, oldVal);
        }
    });

    $scope.showSelectedPoi = function (newVal, oldVal) {

        $scope.poiShareUrl.puid = undefined;
        $scope.poiShareUrl.url = '...';

        if ($scope.anyService.getFloorNumber() != newVal.floor_number) {
            $scope.anyService.setSelectedFloorByNum(newVal.floor_number);
        }

        GMapService.gmap.panTo(_latLngFromPoi(newVal));
        GMapService.gmap.setZoom(20);



        // make previous selected POI's marker smaller
        if (prevSelectedPoi && prevSelectedPoi.puid && $scope.myPoisHashT[prevSelectedPoi.puid] && $scope.myPoisHashT[prevSelectedPoi.puid].marker) {
            $scope.myPoisHashT[prevSelectedPoi.puid].marker.setIcon(_getNormalPoiIconNormal(prevSelectedPoi));
            if (GMapService.gmap.getZoom() <= HIDE_POIS_ZOOM_LEVEL) {
                $scope.myPoisHashT[prevSelectedPoi.puid].marker.setVisible(false);
            }
        }

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

        try {
            if (typeof(Storage) !== "undefined" && localStorage)
                localStorage.setItem("lastPoi", newVal.puid);
        } catch (e) {
        }

        prevSelectedPoi = newVal;
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

        if (poi.is_building_entrance  && poi.is_building_entrance !== "false") {
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
        $scope.anyService.selectedFloor = undefined;
        $scope.anyService.selectedPoi = undefined;
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
                ShowError($scope, resp, "Something went wrong while fetching POIs", true);
            }
        );
    };

    $scope.retrieveRouteFromPoiToPoi = function (from, to) {

        if (!from || !from.puid) {
            _err($scope, "Source POI is corrupted.");
            return;
        }

        if (!to || !to.puid) {
            _err($scope, "Source POI is corrupted.");
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
                ShowError($scope, resp, "Something went wrong while fetching route.", true);
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

        document.getElementById("myText").select();

        if (puid === $scope.poiShareUrl.puid) {
            $scope.poiShareUrl.puid = undefined;
            return;
        }

        $scope.poiShareUrl.puid = puid;

        if (!$scope.anyService.selectedPoi || $scope.anyService.selectedPoi.puid != puid) {
            $scope.anyService.selectedPoi = $scope.myPoisHashT[puid].model;
        }
        var viewerUrl ="https://anyplace.cs.ucy.ac.cy/viewer/?cuid="+ $scope.urlCampus + $scope.anyService.getViewerUrl();

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
        _suc($scope, "Now you can click on another POI to draw the indoor path between the 2 points.");
    };

    $scope.getHtml5GeoLocation = function (callback, errcallback) {
        if (navigator.geolocation) {
            navigator.geolocation.getCurrentPosition(callback, errcallback);
        } else {
            _err($scope, "The Geolocation feature is not supported by this browser.");
        }
    };

    $scope.navigateFromUserToPoiAux = function (position) {
        if (!position) {
            _err($scope, "Invalid position detected by HTML5.");
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
            _err($scope, "No target poi found.");
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
                      HandleGeolocationError(err.code);
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
                + '<input class="form-control" id="myText" value="{{poiShareUrl.url}}" onClick="selectAllInputText(this)"/>'
                + /**
                 +'<ul class="rrssb-buttons">'
                 +'<li class="rrssb-facebook">'
                 +'<a href="https://www.facebook.com/sharer/sharer.php?u={{anyService.getCampusViewerUrl()}}" class="popup">'
                 +'<span class="rrssb-icon">'
                 +'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 29 29">'
                 +'<path d="M26.4 0H2.6C1.714 0 0 1.715 0 2.6v23.8c0 .884 1.715 2.6 2.6 2.6h12.393V17.988h-3.996v-3.98h3.997v-3.062c0-3.746 2.835-5.97 6.177-5.97 1.6 0 2.444.173 2.845.226v3.792H21.18c-1.817 0-2.156.9-2.156 2.168v2.847h5.045l-.66 3.978h-4.386V29H26.4c.884 0 2.6-1.716 2.6-2.6V2.6c0-.885-1.716-2.6-2.6-2.6z"/>'
                 +'</svg>'
                 +'</span>'
                 +'<span class="rrssb-text">facebook</span>'
                 +'</a>'
                 +'</li>'
                 +'<li class="rrssb-twitter">'
                 +'<a href="https://twitter.com/intent/tweet?text={{anyService.getCampusViewerUrlEncoded()}}"class="popup">'
                 +'<span class="rrssb-icon">'
                 +'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 28 28">'
                 +'<path d="M24.253 8.756C24.69 17.08 18.297 24.182 9.97 24.62a15.093 15.093 0 0 1-8.86-2.32c2.702.18 5.375-.648 7.507-2.32a5.417 5.417 0 0 1-4.49-3.64c.802.13 1.62.077 2.4-.154a5.416 5.416 0 0 1-4.412-5.11 5.43 5.43 0 0 0 2.168.387A5.416 5.416 0 0 1 2.89 4.498a15.09 15.09 0 0 0 10.913 5.573 5.185 5.185 0 0 1 3.434-6.48 5.18 5.18 0 0 1 5.546 1.682 9.076 9.076 0 0 0 3.33-1.317 5.038 5.038 0 0 1-2.4 2.942 9.068 9.068 0 0 0 3.02-.85 5.05 5.05 0 0 1-2.48 2.71z"/>'
                 +'</svg>'
                 +'</span>'
                 +'<span class="rrssb-text">twitter</span>'
                 +'</a>'
                 +'</li>'
                 +'<li class="rrssb-email">'
                 +'<a href="mailto:?&amp;body={{anyService.getCampusViewerUrlEncoded()}}">'
                 +'<span class="rrssb-icon">'
                 +'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 28 28">'
                 +'<path d="M20.11 26.147c-2.335 1.05-4.36 1.4-7.124 1.4C6.524 27.548.84 22.916.84 15.284.84 7.343 6.602.45 15.4.45c6.854 0 11.8 4.7 11.8 11.252 0 5.684-3.193 9.265-7.398 9.3-1.83 0-3.153-.934-3.347-2.997h-.077c-1.208 1.986-2.96 2.997-5.023 2.997-2.532 0-4.36-1.868-4.36-5.062 0-4.75 3.503-9.07 9.11-9.07 1.713 0 3.7.4 4.6.972l-1.17 7.203c-.387 2.298-.115 3.3 1 3.4 1.674 0 3.774-2.102 3.774-6.58 0-5.06-3.27-8.994-9.304-8.994C9.05 2.87 3.83 7.545 3.83 14.97c0 6.5 4.2 10.2 10 10.202 1.987 0 4.09-.43 5.647-1.245l.634 2.22zM16.647 10.1c-.31-.078-.7-.155-1.207-.155-2.572 0-4.596 2.53-4.596 5.53 0 1.5.7 2.4 1.9 2.4 1.44 0 2.96-1.83 3.31-4.088l.592-3.72z"/>'
                 +'</svg>'
                 +'</span>'
                 +'<span class="rrssb-text">email</span>'
                 +'</a>'
                 +'</li>'
                 +'<li class="rrssb-googleplus">'
                 +'<a href="https://plus.google.com/share?url={{anyService.getCampusViewerUrlEncoded()}}" class="popup">'
                 +'<span class="rrssb-icon">'
                 +'<svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24"><path d="M21 8.29h-1.95v2.6h-2.6v1.82h2.6v2.6H21v-2.6h2.6v-1.885H21V8.29zM7.614 10.306v2.925h3.9c-.26 1.69-1.755 2.925-3.9 2.925-2.34 0-4.29-2.016-4.29-4.354s1.885-4.353 4.29-4.353c1.104 0 2.014.326 2.794 1.105l2.08-2.08c-1.3-1.17-2.924-1.883-4.874-1.883C3.65 4.586.4 7.835.4 11.8s3.25 7.212 7.214 7.212c4.224 0 6.953-2.988 6.953-7.082 0-.52-.065-1.104-.13-1.624H7.614z"/>'
                 +'</svg>'
                 +'</span>'
                 +'<span class="rrssb-text">google+</span>'
                 +'</a>'
                 +'</li>'
                 +'</ul>'
                 */
                '<div>Embed:</div>'
                + '<input class="form-control" value="{{poiShareUrl.embed}}" onClick="selectAllInputText(this)"/>'
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
                            if ($scope.anyService.selectedGenPoi && $scope.anyService.selectedGenPoi.puid != self.model.puid && $scope.navRoutesShown())
                                $scope.clearNavPolylines();
                        }

                        if ($scope.anyService.selectedGenPoi && $scope.anyService.selectedGenPoi.puid == self.model.puid) {
                            self.infowindow.setContent(self.tpl2);
                            self.infowindow.open(GMapService.gmap, self);
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
        //else if ($scope.myPois && $scope.myPois.length > 0) {
        //    $scope.anyService.selectedPoi = $scope.myPois[0];
        //}

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
])
;
