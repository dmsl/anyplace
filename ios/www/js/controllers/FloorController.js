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

app.controller('FloorController', ['$scope', '$compile', 'AnyplaceService', 'GMapService', 'AnyplaceAPIService', function ($scope, $compile, AnyplaceService, GMapService, AnyplaceAPIService) {
    $scope.anyService = AnyplaceService;
    $scope.anyAPI = AnyplaceAPIService;

    $scope.xFloors = [];

    $scope.data = {
        floor_plan_coords: {},
        floor_plan_base64_data: {},
        floor_plan_groundOverlay: null
    };

    var _clearFloors = function () {
        $scope.removeFloorPlan();
        $scope.xFloors = [];
        $scope.myFloorId = 0;
        $scope.myFloors = {};
    };

    $scope.$watch('anyService.selectedBuilding', function (newVal, oldVal) {
        if (newVal && newVal.buid) {
            $scope.fetchAllFloorsForBuilding(newVal);
        } else {
            $scope.removeFloorPlanOverlay();
        }
    });

    $scope.$watch('anyService.selectedFloor', function (newVal, oldVal) {
        if (newVal !== undefined && newVal !== null) {
            $scope.fetchFloorPlanOverlay(newVal);
            GMapService.gmap.panTo(_latLngFromBuilding($scope.anyService.selectedBuilding));
            GMapService.gmap.setZoom(20);

            try {
                if (typeof(Storage) !== "undefined" && localStorage) {
                    localStorage.setItem("lastFloor", newVal.floor_number);
                }
            } catch (e) {

            }
        }
    });

    $scope.removeFloorPlanOverlay = function () {
        if ($scope.data.floor_plan_groundOverlay) {
            $scope.data.floor_plan_groundOverlay.setMap(null);
        }
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

    var _err = function (msg) {
        $scope.anyService.addAlert('danger', msg);
    };

    var _suc = function (msg) {
        $scope.anyService.addAlert('success', msg);
    };

    var _warn = function (msg) {
        $scope.anyService.addAlert('warning', msg);
    };

    $scope.orderByFloorNo = function (floor) {
        if (!floor || LPUtils.isNullOrUndefined(floor.floor_number)) {
            return 0;
        }
        return parseInt(floor.floor_number);
    };

    $scope.fetchAllFloorsForBuilding = function (b) {
        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = b.buid;

        var promise = AnyplaceAPIService.allBuildingFloors(jsonReq);
        promise.then(
            function (resp) {
                $scope.xFloors = resp.data.floors;

                $scope.xFloors = $scope.xFloors.sort(function (a, b) {
                    return parseInt(a.floor_number) - parseInt(b.floor_number)
                });

                $scope.anyService.availableFloors = {};
                for (var ind = 0; ind < $scope.xFloors.length; ind++) {
                    var fl = $scope.xFloors[ind];
                    $scope.anyService.availableFloors[fl.floor_number] = fl;
                }

                // give priority to floor in url parameter - if exists
                if ($scope.urlFloor) {
                    for (var k = 0; k < $scope.xFloors.length; k++) {
                        if ($scope.urlFloor == $scope.xFloors[k].floor_number) {
                            $scope.anyService.selectedFloor = $scope.xFloors[k];
                            return;
                        }
                    }
                }

                // give 2nd priority to floor found in localStorage
                try {
                    if (typeof(Storage) !== "undefined" && localStorage && !LPUtils.isNullOrUndefined(localStorage.getItem('lastBuilding')) && !LPUtils.isNullOrUndefined(localStorage.getItem('lastFloor'))) {
                        for (var i = 0; i < $scope.xFloors.length; i++) {
                            if (String($scope.xFloors[i].floor_number) === String(localStorage.getItem('lastFloor'))) {
                                $scope.anyService.selectedFloor = $scope.xFloors[i];
                                return;
                            }
                        }
                    }
                } catch (e) {

                }

                // try loading floor 0
                for (var j = 0; j < $scope.xFloors.length; j++) {
                    if ($scope.xFloors[j].floor_number == "0") {
                        $scope.anyService.selectedFloor = $scope.xFloors[j];
                        return;
                    }
                }

                // last resort, default floor
                if ($scope.xFloors[0]) {
                    $scope.anyService.selectedFloor = $scope.xFloors[0];
                }
            },
            function (resp) {
                console.log(resp.data.message);
                _err("Something went wrong while fetching all floors");
            }
        );
    };

    $scope.fetchFloorPlanOverlay = function () {
        var floor_number = $scope.anyService.selectedFloor.floor_number;
        var buid = $scope.anyService.selectedBuilding.buid;

        var promise = AnyplaceAPIService.downloadFloorPlan(this.anyService.jsonReq, buid, floor_number);
        promise.then(
            function (resp) {

                // in case the building was switched too fast, don't load the old building's
                // floorplan 
                if (buid == $scope.anyService.selectedBuilding.buid && floor_number == $scope.anyService.selectedFloor.floor_number) {

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
                }
            },
            function (resp) {
                // on error
                // TODO: alert failure
            }
        );
    };

    var canvasOverlay = null;
    $scope.isCanvasOverlayActive = false;

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

        $scope.addFloorObject(newFl, $scope.anyService.selectedBuilding, $scope.data);

    };

    $scope.removeFloorPlan = function () {
        $scope.data.floor_plan_file = null;
        $scope.data.floor_plan = null;

        if (canvasOverlay) {
            canvasOverlay.setMap(null);
        }

        var x = $('#input-floor-plan');
        x.replaceWith(x = x.clone(true));

        x.prop('disabled', false);

        $scope.isCanvasOverlayActive = false;
    };

    $scope.floorUp = function () {
        for (var i = 0; i < $scope.xFloors.length; i++) {
            if ($scope.xFloors[i].floor_number == $scope.anyService.selectedFloor.floor_number) {
                if (i + 1 < $scope.xFloors.length) {
                    $scope.anyService.selectedFloor = $scope.xFloors[i + 1];
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
        for (var i = 0; i < $scope.xFloors.length; i++) {
            if ($scope.xFloors[i].floor_number == $scope.anyService.selectedFloor.floor_number) {
                if (i - 1 >= 0) {
                    $scope.anyService.selectedFloor = $scope.xFloors[i - 1];
                    return;
                } else {
                    //_warn("There is no other floor below.");
                    return;
                }
            }
        }

        _err("Floor not found.");
    };

    // pass scope to floor controls in the html
    var floorControls = $('#floor-controls');
    floorControls.replaceWith($compile(floorControls.html())($scope));
}
])
;