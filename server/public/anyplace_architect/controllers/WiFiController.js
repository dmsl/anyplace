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


var heatMap = [];
var APmap = [];
var heatmap;
var fingerPrintsMap = [];
var connectionsMap = {};
var POIsMap = {};
var drawingManager;
var _HEATMAP_RSS_IS_ON = false;
var _APs_IS_ON = false;
var _FINGERPRINTS_IS_ON = false;
var _DELETE_FINGERPRINTS_IS_ON = false;
var _HEATMAP_F_IS_ON = false;
var _CONNECTIONS_IS_ON = false;
var _POIS_IS_ON = false;
var changedfloor = false;

app.controller('WiFiController', ['$scope', 'AnyplaceService', 'GMapService', 'AnyplaceAPIService', function ($scope, AnyplaceService, GMapService, AnyplaceAPIService) {
    $scope.anyService = AnyplaceService;
    $scope.anyAPI = AnyplaceAPIService;
    $scope.gmapService = GMapService;
    $scope.example9model = [];
    $scope.example9data = [];
    $scope.example9settings = {enableSearch: true, scrollable: true};
    $scope.deleteButtonWarning = false;
    $scope.radioHeatmapRSSMode = false;
    $scope.fingerPrintsMode = false;
    $scope.APsMode = false;

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


    $scope.$watch('anyService.selectedBuilding', function (newVal, oldVal) {
        if (newVal) {
            if (_HEATMAP_RSS_IS_ON) {

                var i = heatMap.length;
                while (i--) {
                    heatMap[i].setMap(null);
                    heatMap[i] = null;
                }
                heatMap = [];

                $scope.showRadioHeatmapRSS();

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
                APmap = [];
                $scope.example9data = [];
                $scope.example9model = [];
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
            }

            if (heatmap && heatmap.getMap()) {
                //hide fingerPrints heatmap
                heatmap.setMap(null);

                $scope.showFingerPrints();
            }

            var check = 0;
            if (!_CONNECTIONS_IS_ON) {
                connectionsMap = $scope.anyService.getAllConnections();
                var key = Object.keys(connectionsMap);
                if (connectionsMap[key[check]] !== undefined) {
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
                    heatMap[i].setMap(null);
                    heatMap[i] = null;
                }
                heatMap = [];

                $scope.showRadioHeatmapRSS();


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
                APmap = [];
                $scope.example9data = [];
                $scope.example9model = [];
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
            }

            if (heatmap && heatmap.getMap()) {
                //hide fingerPrints heatmap
                heatmap.setMap(null);

                $scope.showFingerPrints();
            }

            var check = 0;
            if (!_CONNECTIONS_IS_ON) {
                connectionsMap = $scope.anyService.getAllConnections();
                var key = Object.keys(connectionsMap);
                if (connectionsMap[key[check]] !== undefined) {
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


    $scope.heatmapRSSisON = function () {
        return _HEATMAP_RSS_IS_ON;
    };

    $scope.toggleRadioHeatmapRSS = function () {

        var check = 0;
        if (heatMap[check] !== undefined && heatMap[check] !== null) {

            var i = heatMap.length;
            while (i--) {
                heatMap[i].setMap(null);
                heatMap[i] = null;
            }
            heatMap = [];
            document.getElementById("radioHeatmapRSS-mode").classList.remove('draggable-border-green');
            _HEATMAP_RSS_IS_ON = false;
            $scope.radioHeatmapRSSMode = false;
            return;
        }

        document.getElementById("radioHeatmapRSS-mode").classList.add('draggable-border-green');
        $scope.radioHeatmapRSSMode = true;

        $scope.showRadioHeatmapRSS();
        return;
    };

    $scope.APsisON = function () {
        return _APs_IS_ON;
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
            APmap = [];
            $scope.example9data = [];
            $scope.example9model = [];
            _APs_IS_ON = false;
            document.getElementById("APs-mode").classList.remove('draggable-border-green');
            $scope.APsMode = false;
            return;

        }
        _APs_IS_ON = true;

        $scope.APsMode = true;

        document.getElementById("APs-mode").classList.add('draggable-border-green');

        $scope.showAPs();

    };

    $scope.toggleFingerPrints = function () {
        $scope.fingerPrintsMode = !$scope.fingerPrintsMode;

        if ($scope.fingerPrintsMode) {
            document.getElementById("fingerPrints-mode").classList.add('draggable-border-green');
        } else {
            document.getElementById("fingerPrints-mode").classList.remove('draggable-border-green');
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
            $scope.fingerPrintsMode = false;
            return;

        }

        if (heatmap && heatmap.getMap()) {
            //hide fingerPrints heatmap

            heatmap.setMap(null);
            _FINGERPRINTS_IS_ON = false;
            document.getElementById("fingerPrints-mode").classList.remove('draggable-border-green');
            $scope.fingerPrintsMode = false;
            _HEATMAP_F_IS_ON = false;
            return;
        }

        document.getElementById("fingerPrints-mode").classList.add('draggable-border-green');
        $scope.fingerPrintsMode = true;

        $scope.showFingerPrints();
    };

    $scope.toggleLocalizationAccurancy = function () {
        $scope.localizationAccurancyMode = !$scope.localizationAccurancyMode;
        //IN PROGRESS

    };

    $scope.togglePOIs = function () {

        POIsMap = $scope.anyService.getAllPois();
        var key = Object.keys(POIsMap);
        var check = 0;
        if (!POIsMap.hasOwnProperty(key[check])) {
            _err("No POIs yet.")
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

        if (connectionsMap[key[check]].polyLine.getMap() !== null && connectionsMap[key[check]].polyLine.getMap() !== undefined) {

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
            return;
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


    $scope.getLocalizationAccurancyButtonText = function () {
        //IN PROGRESS

    };


    $scope.getPOIsButtonText = function () {

        POIsMap = $scope.anyService.getAllPois();
        var key = Object.keys(POIsMap);
        var check = 0;
        if (POIsMap.hasOwnProperty(key[check])) {
            if (POIsMap[key[check]].marker.getMap() !== null && POIsMap[key[check]].marker.getMap() !== undefined ) {
                document.getElementById("POIs-mode").classList.add('draggable-border-green');
                $scope.POIsMode = true;
                return "Hide POIs";
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
            if (connectionsMap[key[check]].polyLine.getMap() !== null && connectionsMap[key[check]].polyLine.getMap() !== undefined) {
                document.getElementById("connections-mode").classList.add('draggable-border-green');
                $scope.connectionsMode = true;
                return "Hide Edges";
            }
        }
        document.getElementById("connections-mode").classList.remove('draggable-border-green');
        $scope.connectionsMode = false;
        return "Show Edges";
        //return (connectionsMap!==undefined && connectionsMap!==null)  ? "Hide Edges" : "Show Edges";
    };

    $('#HMs_1').unbind().click(function (e) {
        e.stopPropagation();
        $('#HMs').click();
        $('#HMsButton').click();
    });

    $('#APs_1').unbind().click(function () {
        $('#HMs').click();
        $('#APsButton').click();
    });

    $('#FPs_1').unbind().click(function () {
        $('#FPs').click();
        $('#FPsButton').click();
    });
    $('#DL_1').unbind().click(function () {
        $('#deleteButton').click();
    });

    $('#LA_1').unbind().click(function () {
        $('#LAs').click();
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
        return $scope.radioHeatmapRSSMode ? "Heatmap is online" : "Heatmap is offline";

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


    $scope.getLocalizationAccurancyModeText = function () {
        return $scope.localizationAccurancyMode ? "Localization is online" : "Localization is offline";

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

        if (!_FINGERPRINTS_IS_ON) {
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
            confirmation = confirm("Confirm:\nAre you sure you want to delete those Fingerprints?");
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

                reqObj.lat1 = start.lat();

                reqObj.lon1 = start.lng();

                reqObj.lat2 = end.lat();

                reqObj.lon2 = end.lng();

                var data = [];

                var promise = $scope.anyAPI.deleteFingerprints(reqObj);


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
                            i = fingerPrintsMap.length;
                            while (i--) {

                                if (fingerPrintsMap[i].getPosition().lat() <= start.lat() && fingerPrintsMap[i].getPosition().lng() <= start.lng() && fingerPrintsMap[i].getPosition().lat() >= end.lat() && fingerPrintsMap[i].getPosition().lng() >= end.lng()) {

                                    // delete the fingerPrints from the loaded FingerPrints
                                    fingerPrintsMap[i].setMap(null);
                                }

                            }
                            //HERE
                            if (_HEATMAP_F_IS_ON) {
                                heatmap.setMap(null);
                                $scope.showFingerPrints();
                            }
                            if (_HEATMAP_RSS_IS_ON) {
                                var i = heatMap.length;
                                while (i--) {
                                    heatMap[i].setMap(null);
                                    heatMap[i] = null;
                                }
                                heatMap = [];
                                $scope.showRadioHeatmapRSS();
                            }
                        }

                        _suc("Successfully deleted " + data.length + " fingerPrints.");
                    },
                    function (resp) {
                        // on error
                        //var data = resp.data;
                        _err("Something went wrong. It's likely that everything related to the fingerPrints is deleted but please refresh to make sure or try again.");
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

    $scope.showRadioHeatmapRSS = function () {

        var jsonReq = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber()};

        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;

        var promise;
        _NOW_ZOOM = GMapService.gmap.getZoom();
        if (_NOW_ZOOM > MIN_ZOOM_FOR_HEATMAPS && _NOW_ZOOM < MAX_ZOOM_FOR_HEATMAPS)
            promise = $scope.anyAPI.getRadioHeatmapRSS_2(jsonReq);
        else if (_NOW_ZOOM > MIN_ZOOM_FOR_HEATMAPS)
            promise = $scope.anyAPI.getRadioHeatmapRSS_3(jsonReq);
        else
            promise = $scope.anyAPI.getRadioHeatmapRSS_1(jsonReq);

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
                var j = 0;
                while (i--) {

                    var rp = resp.data.radioPoints[i];
                    var rss = JSON.parse(rp.w); //count,average,total
                    //set weight based on RSSI

                    var w = parseInt(rss.average);

                    if (w <= -30 && w >= -35) {

                        heatMapData.push(
                            {location: new google.maps.LatLng(rp.x, rp.y), weight: w, color: '#4ed419'}
                        );

                    } else if (w <= -36 && w >= -45) {

                        heatMapData.push(
                            {location: new google.maps.LatLng(rp.x, rp.y), weight: w, color: '#27670e'}
                        );

                    } else if (w <= -46 && w >= -55) {

                        heatMapData.push(
                            {location: new google.maps.LatLng(rp.x, rp.y), weight: w, color: '#ffff00'}
                        );

                    } else if (w <= -56 && w >= -65) {

                        heatMapData.push(
                            {location: new google.maps.LatLng(rp.x, rp.y), weight: w, color: '#ffa500'}
                        );

                    } else if (w <= -66 && w >= -75) {

                        heatMapData.push(
                            {location: new google.maps.LatLng(rp.x, rp.y), weight: w, color: '#00b8ff'}
                        );

                    } else if (w <= -76 && w >= -85) {

                        heatMapData.push(
                            {location: new google.maps.LatLng(rp.x, rp.y), weight: w, color: '#0000ff'}
                        );

                    } else if (w <= -86 && w >= -95) {

                        heatMapData.push(
                            {location: new google.maps.LatLng(rp.x, rp.y), weight: w, color: '#800080'}
                        );

                    } else if (w <= -96 && w >= -105) {

                        heatMapData.push(
                            {location: new google.maps.LatLng(rp.x, rp.y), weight: w, color: '#e36e83'}
                        );

                    } else {

                        heatMapData.push(
                            {location: new google.maps.LatLng(rp.x, rp.y), weight: w, color: '#ff0000'}
                        );

                    }

                    //calculate bounds
                    var center = heatMapData[j].location;
                    var size = new google.maps.Size(2, 2);
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
                        map: $scope.gmapService.gmap,
                        bounds: new google.maps.LatLngBounds(
                            new google.maps.LatLng(s, w),
                            new google.maps.LatLng(n, e))
                    });

                    heatMap.push(
                        rectangle
                    );
                    j++;

                    resp.data.radioPoints.splice(i, 1);
                }
                _HEATMAP_RSS_IS_ON = true;

            },
            function (resp) {
                // on error
                var data = resp.data;
                _err('Something went wrong while fetching radio heatmap.');
            }
        );
    }

    $scope.showAPs = function () {
        //request for access points
        var jsonReq = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber()};

        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;

        var APsData = [];

        var i;

        var promise = $scope.anyAPI.getAPs(jsonReq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                i = resp.data.accessPoints.length;

                if (i <= 0) {
                    _err("This floor seems not to be Access Point mapped. Download the Anyplace app from the Google Play store to map the floor.");
                    return;
                }

                while (i--) {
                    var rp = resp.data.accessPoints[i];
                    var rss = rp.RSS;
                    APsData.push(
                        {AP: rp.AP, x: rp.x, y: rp.y, RSS: rss.average}
                    );

                    resp.data.accessPoints.splice(i, 1);
                }

                //algorithm to find the location of each AP
                var values = [];

                var j;

                i = APsData.length;

                while (i--) {
                    j = 0;

                    while (true) {

                        if (values[j] === undefined || values[j] === null) {
                            values.push(
                                {
                                    AP: APsData[i].AP,
                                    numx: APsData[i].RSS * APsData[i].x,
                                    numy: APsData[i].RSS * APsData[i].y,
                                    den: APsData[i].RSS
                                }
                            );

                            $scope.example9data[j] = {id: APsData[i].AP, label: APsData[i].AP};
                            $scope.example9model[j] = {id: APsData[i].AP, label: APsData[i].AP};

                            break;
                        } else if (values[j].AP === APsData[i].AP) {
                            values[j].numx += parseFloat((APsData[i].RSS * APsData[i].x));
                            values[j].numy += parseFloat((APsData[i].RSS * APsData[i].y));
                            values[j].den += parseFloat(APsData[i].RSS);
                            break;
                        }
                        j++;
                    }

                }

                //create AccessPoint "map"
                var _ACCESS_POINT_IMAGE = 'build/images/access-point-icon.svg';

                var imgType = _ACCESS_POINT_IMAGE;

                var size = new google.maps.Size(36, 48);

                i = values.length;
                var c = 0;
                while (i--) {
                    //check for limit
                    if (c == MAX) {
                        _err('Access Points have exceeded the maximun limit of 1000');
                        break;
                    }
                    var x = values[i].numx / values[i].den;
                    var y = values[i].numy / values[i].den;
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

                    var infowindow=new google.maps.InfoWindow();
                    if(!infowindow.getMap()) {
                        APmap[c].addListener('click', function () {
                            /*var infowindow = new google.maps.InfoWindow({
                               content: "MAC: " + this.id
                           });*/
                            infowindow.setContent("MAC: " + this.id);
                            infowindow.open(this.gmap, this);
                        });
                    }


                    c++;
                }
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err('Something went wrong while fetching Access Points.');
            }
        );

    }

    $scope.fingerPrintsisON = function () {
        return _FINGERPRINTS_IS_ON;
    };

    $scope.showFingerPrints = function () {

        var jsonReq = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber()};

        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;

        var promise = $scope.anyAPI.getRadioHeatmapRSS_3(jsonReq);

        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                var fingerPrintsData = [];

                var i = resp.data.radioPoints.length;

                if (i <= 0) {
                    _err("This floor seems not to be FingerPrint mapped. Download the Anyplace app from the Google Play store to map the floor.");
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

                    var c = 0;

                    while (i--) {
                        //check for limit
                        if (c == MAX) {
                            _err('FingerPrints have exceeded the maximun limit of 1000');
                            break;
                        }

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

                        c++;
                    }
                } else {

                    var heatMapData = [];
                    while (i--) {
                        var rp = resp.data.radioPoints[i];
                        var rss = JSON.parse(rp.w); //count,average,total
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

                    _HEATMAP_F_IS_ON = true;

                }
                _FINGERPRINTS_IS_ON = true;


            },
            function (resp) {
                // on error
                var data = resp.data;
                _err('Something went wrong while fetching fingerPrints.');
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
        $scope.anyService.setAllConnection(connectionsMap);

    };

    //zoom handler for clustering

    GMapService.gmap.addListener('zoom_changed', function () {
        _NOW_ZOOM = GMapService.gmap.getZoom();


        if (_HEATMAP_RSS_IS_ON) {
            if ((_PREV_ZOOM == MIN_ZOOM_FOR_HEATMAPS && _NOW_ZOOM > _PREV_ZOOM) || (_PREV_ZOOM > MIN_ZOOM_FOR_HEATMAPS && _PREV_ZOOM < MAX_ZOOM_FOR_HEATMAPS && (_NOW_ZOOM <= MIN_ZOOM_FOR_HEATMAPS || _NOW_ZOOM >= MAX_ZOOM_FOR_HEATMAPS)) || (_PREV_ZOOM == MAX_ZOOM_FOR_HEATMAPS && _NOW_ZOOM < _PREV_ZOOM)) {
                var i = heatMap.length;
                while (i--) {
                    heatMap[i].setMap(null);
                    heatMap[i] = null;
                }
                heatMap = [];

                $scope.showRadioHeatmapRSS();
            }
        }
        if (_FINGERPRINTS_IS_ON && !changedfloor) {
            if (_NOW_ZOOM == _MAX_ZOOM_LEVEL || _PREV_ZOOM == _MAX_ZOOM_LEVEL) {
                var i = fingerPrintsMap.length;
                while (i--) {
                    fingerPrintsMap[i].setMap(null);
                    fingerPrintsMap[i] = null;
                }
                fingerPrintsMap = [];

                if (heatmap && heatmap.getMap()) {
                    heatmap.setMap(null);
                }

                $scope.showFingerPrints();
            }
        }
        _PREV_ZOOM = _NOW_ZOOM;
    });


    $scope.multiuserevents = {

        onItemDeselect: function (item) {
            i = APmap.length;
            while (i--) {
                if (APmap[i].id == item.id) {
                    APmap[i].setVisible(false);
                    break;
                }
            }

        },
        onItemSelect: function (item) {
            i = APmap.length;

            while (i--) {
                if (APmap[i].id == item.id) {
                    APmap[i].setVisible(true);
                    break;
                }
            }

        },
        onDeselectAll: function () {
            i = APmap.length;
            while (i--) {
                APmap[i].setVisible(false);


            }
        }

    };

}]);