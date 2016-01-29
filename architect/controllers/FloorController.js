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