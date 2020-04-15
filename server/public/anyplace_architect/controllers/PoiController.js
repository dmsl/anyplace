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
    _POIS_IS_ON=true;
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
        },{
            poicat: "Elevator",
            poicatPlaceholder: "name",
            id: "type2",
            disenable: "true"
        },{
            poicat: "Entrance",
            poicatPlaceholder: "name",
            id: "type3",
            disenable: "true"
        },{
            poicat: "Fire Extinguisher",
            poicatPlaceholder: "name",
            id: "type4",
            disenable: "false"
        },{
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

        for (var i=0; i<$scope.poicategories.length; i++){
            if ($scope.poicategories[i].id==id){
                $scope.poicategories[i].id="";
                $( "#".id ).remove();
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
        $scope.myPoisHashT={};
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
            $scope.myConnectionsHashT={};
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
                if(_CONNECTIONS_IS_ON)
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
                    if(_POIS_IS_ON)
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

                    if(_POIS_IS_ON)
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

                    $scope.myPois[i].pois_type2=$scope.myPois[i].pois_type;

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

            if ($scope.myMarkers[id].model.pois_type2.localeCompare("")!=0){
                $scope.myMarkers[id].model.pois_type=$scope.myMarkers[id].model.pois_type2;
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

        if (bobj.model.pois_type2.localeCompare("")!=0){
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
            $scope.myMarkers[marker.myId].model.pois_type2=$scope.myMarkers[marker.myId].model.pois_type;
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