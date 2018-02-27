/**
 *
 The MIT License (MIT)

 Copyright (c) 2015, Marileni Angelidou, Data Management Systems Laboratory (DMSL)
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


app.controller('SelectFloorController', ['$scope', 'AnyplaceService', 'GMapService', 'AnyplaceAPIService', function ($scope, AnyplaceService, GMapService, AnyplaceAPIService) {
    $scope.anyService = AnyplaceService;
    $scope.anyAPI = AnyplaceAPIService;
    $scope.gmapService = GMapService;
    $scope.xFloors = [];

    $scope.$watch('anyService.availableFloors', function (newVal, oldVal) {
        if (newVal) {
            $scope.xFloors=[];
            $scope.xFloors=$scope.anyService.availableFloors;
            $scope.anyService.selectedFloor=$scope.xFloors[0];
        }
    });

    var _err = function (msg) {
        $scope.anyService.addAlert('danger', msg);
    };

    $scope.orderByFloorNo = function (floor) {
        if (!floor || LPUtils.isNullOrUndefined(floor.floor_number)) {
            return 0;
        }
        return parseInt(floor.floor_number);
    };


    // new marileni 4/1

    $scope.floorUp = function () {
        //here new
        changedfloor = true;
        var next;
        for (var i = 0; i < $scope.xFloors.length; i++) {
            next = i + 1;
            if ($scope.xFloors[i].floor_number === $scope.anyService.selectedFloor.floor_number) {

                if (next < $scope.xFloors.length) {
                    $scope.anyService.selectedFloor = $scope.xFloors[next];
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

        changedfloor = true;
        var prev;
        for (var i = 0; i < $scope.xFloors.length; i++) {
            prev = i - 1;
            if ($scope.xFloors[i].floor_number === $scope.anyService.selectedFloor.floor_number) {

                if (prev >= 0) {
                    $scope.anyService.selectedFloor = $scope.xFloors[prev];
                    return;
                } else {
                    //_warn("There is no other floor below.");
                    return;
                }
            }
        }

        _err("Floor not found.");
    };

    // end new marileni

}]);