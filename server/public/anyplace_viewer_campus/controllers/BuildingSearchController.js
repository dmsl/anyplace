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
app.controller('BuildingSearchController', ['$scope', '$compile', 'GMapService', 'AnyplaceService', 'AnyplaceAPIService','$timeout', '$q', '$log', function BuildingSearchController ($scope, $compile, GMapService, AnyplaceService, AnyplaceAPIService,$timeout, $q, $log) {

    $scope.gmapService = GMapService;
    $scope.anyService = AnyplaceService;
    $scope.anyAPI = AnyplaceAPIService;

    $scope.creds = {
        username: 'username',
        password: 'password'
    };


    $scope.myBuildings = [];

    $scope.myBuildingsnames = {};
    $scope.data = [];
    $scope.datasz = 0;
    $scope.myallPoisHashT = {};

    $scope.myallEntrances = [];

    $scope.mylastquery = "";
    $scope.myallPois = [];
    $scope.buid = "";

    var self = this;
    self.querySearch = querySearch;

    function querySearch (query) {
        if (query == ""){
            return ;
        }
        if (query == $scope.mylastquery){
            return $scope.myallPois;
        }
        if (!$scope.userPosition) {
            // _info("Enabling the location service will improve your search results.");
            $scope.showUserLocation();
        }

        $scope.anyService.selectedSearchPoi = query;
        setTimeout(
            function(){
                if (query==$scope.anyService.selectedSearchPoi ){
                    $scope.fetchAllPoi(query, $scope.urlCampus);
                }
            },1000);
        $scope.mylastquery = query;
        return $scope.myallPois;
    }

    $scope.fetchAllPoi = function (letters , cuid) {

        var jsonReq = { "access-control-allow-origin": "",    "content-encoding": "gzip",    "access-control-allow-credentials": "true",    "content-length": "17516",    "content-type": "application/json" , "buid":$scope.buid , "cuid":$scope.urlCampus, "letters":letters, "greeklish":$scope.greeklish };
        var promise = AnyplaceAPIService.retrieveALLPois(jsonReq);
        promise.then(
            function (resp) {
                var data = resp.data;
                $scope.myallPois = data.pois;

                var sz = $scope.myallPois.length;

                for (var i = sz - 1; i >= 0; i--) {
                    $scope.myallPois[i].buname=$scope.myBuildingsnames[$scope.myallPois[i].buid];
                }

            },
            function (resp) {
                var data = resp.data;
                if (letters=="")
                    _err("Something went wrong while fetching POIs");
            }
        );
    };


    $scope.$watch('anyService.selectedBuilding', function (newVal, oldVal) {
        if (newVal && newVal.buid) {
            $scope.mylastquery="";
        }
    });

    $scope.fetchAllBuildings = function () {
        var jsonReq = { "access-control-allow-origin": "",    "content-encoding": "gzip",    "access-control-allow-credentials": "true",    "content-length": "17516",    "content-type": "application/json" , "cuid":$scope.urlCampus};
        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;

        var promise = $scope.anyAPI.allBuildings(jsonReq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;
                //var bs = JSON.parse( data.buildings );
                $scope.myBuildings = data.buildings;
                $scope.greeklish = data.greeklish;

                for (var i = 0; i < $scope.myBuildings.length; i++) {

                    var b = $scope.myBuildings[i];

                    $scope.myBuildingsnames[b.buid] = b.name;
                }

            },
            function (resp) {
                // on error
                var data = resp.data;
                _err('Something went wrong while fetching buildings.');
            }
        );
    };

    $scope.fetchAllBuildings();

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

    var _info = function (msg) {
        $scope.anyService.addAlert('info', msg);
        window.setTimeout(function() {
            $(".alert-info").fadeTo(500, 0).slideUp(500, function(){
                $(this).remove();
            });
        }, 4000);
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
