/**
 *
 The MIT License (MIT)

 Copyright (c) 2015, Marileni Angelidou , Data Management Systems Laboratory (DMSL)
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
var heatmapFingerprints=[];
var userTimeData=[];
var fingerPrintsMap = [];
var heatmapAcc;
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
var colorBarGreenClicked=false;
var colorBarYellowClicked=false;
var colorBarOrangeClicked=false;
var colorBarPurpleClicked=false;
var colorBarRedClicked=false;
var levelOfZoom=1;


app.controller('WiFiController', ['$cookieStore','$scope', 'AnyplaceService', 'GMapService', 'AnyplaceAPIService', function ($cookieStore,$scope, AnyplaceService, GMapService, AnyplaceAPIService) {
    $scope.anyService = AnyplaceService;
    $scope.anyAPI = AnyplaceAPIService;
    $scope.gmapService = GMapService;
    $scope.example9model = [];
    $scope.example9data = [];
    $scope.example9settings = {enableSearch: true, scrollable: true};
    $scope.example8model = [];
    $scope.example8data = [];
    $scope.deleteButtonWarning = false;
    $scope.radioHeatmapRSSMode = false;
    $scope.radioHeatmapRSSTimeMode = false;
    $scope.fingerPrintsMode = false;
    $scope.fingerPrintsTimeMode = false;
    $scope.APsMode = false;
    $scope.filterByMAC = false;
    $scope.filterByMAN = false;
    $scope.radioHeatmapRSSHasGreen = false;
    $scope.radioHeatmapRSSHasYellow = false;
    $scope.radioHeatmapRSSHasOrange = false;
    $scope.radioHeatmapRSSHasPurple = false;
    $scope.radioHeatmapRSSHasRed = false;
    $scope.localizationAccMode = false;
    $scope.selected = "Filters:";
    $scope.initializeTime=false;
    $scope.initializeFingerPrints=false;
    $scope.initializeRadioHeatmapRSS=false;
    $scope.initializeAPs=false;
    $scope.initializeConnections=false;
    $scope.initializePOIs=false;
    $scope.initializeAcces=false;


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
    function clearRadioHeatmap(){
        var check = 0;
        if (heatMap[check] !== undefined && heatMap[check] !== null) {

            var i = heatMap.length;
            while (i--) {
                heatMap[i].rectangle.setMap(null);
                heatMap[i] = null;
            }
            heatMap = [];
            document.getElementById("radioHeatmapRSS-mode").classList.remove('draggable-border-green');
            _HEATMAP_RSS_IS_ON = false;
            setColorClicked('g',false);
            setColorClicked('y',false);
            setColorClicked('o',false);
            setColorClicked('p',false);
            setColorClicked('r',false);
            $scope.radioHeatmapRSSMode = false;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('radioHeatmapRSSMode', 'NO');
            }
            $scope.anyService.radioHeatmapRSSMode=false;
            $scope.radioHeatmapRSSHasGreen=false;
            $scope.radioHeatmapRSSHasYellow=false;
            $scope.radioHeatmapRSSHasOrange=false;
            $scope.radioHeatmapRSSHasPurple=false;
            $scope.radioHeatmapRSSHasRed=false;
            $cookieStore.put('RSSClicked', 'NO');

        }
    }
    
    function clearFingerPrints(){

        // if ($scope.fingerPrintsMode) {
        //     document.getElementById("fingerPrints-mode").classList.add('draggable-border-green');
        // } else {
        //     document.getElementById("fingerPrints-mode").classList.remove('draggable-border-green');
        // }

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


        }

        if (heatmap && heatmap.getMap()) {
            //hide fingerPrints heatmap

            heatmap.setMap(null);
             _FINGERPRINTS_IS_ON = false;
             document.getElementById("fingerPrints-mode").classList.remove('draggable-border-green');
             _HEATMAP_F_IS_ON = false;
            var i=heatmapFingerprints.length;
            while(i--){
                heatmapFingerprints[i]=null;
            }
            heatmapFingerprints=[];

        }

        // document.getElementById("fingerPrints-mode").classList.add('draggable-border-green');

    }

    function initializeTimeFunction(){
        if($scope.fingerPrintsMode) {

            $scope.fingerPrintsTimeMode = true;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('fingerPrintsTimeMode', 'YES');
            }
            $scope.anyService.fingerPrintsTimeMode=true;
            $scope.fingerPrintsMode = true;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('fingerprintsMode', 'YES');
            }
            // document.getElementById("fingerPrints-time-mode").classList.add('draggable-border-green');
        }

        if($scope.radioHeatmapRSSMode){
            $scope.radioHeatmapRSSTimeMode = true;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('fingerPrintsTimeMode', 'YES');
            }
            $scope.radioHeatmapRSSMode = true;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('radioHeatmapRSSMode', 'YES');
            }
            $scope.anyService.radioHeatmapRSSTimeMode=true;
            $scope.anyService.radioHeatmapRSSMode=true;
            // document.getElementById("radioHeatmapRSS-time-mode").classList.add('draggable-border-green');
        }
        document.getElementById("fingerPrints-time-mode").classList.add('draggable-border-green');

    }


    $scope.$watch('anyService.selectedBuilding', function (newVal, oldVal) {
        if (newVal) {


            if (localStorage.getItem('fingerprintsMode') !== undefined) {

                if (localStorage.getItem('fingerprintsMode') === 'YES') {
                    $scope.initializeFingerPrints=true;

                }
            }

            if (localStorage.getItem('radioHeatmapRSSMode') !== undefined) {

                if (localStorage.getItem('radioHeatmapRSSMode') === 'YES') {
                    $scope.initializeRadioHeatmapRSS=true;

                }
            }

            if (localStorage.getItem('APsMode') !== undefined) {

                if (localStorage.getItem('APsMode') === 'YES') {
                    $scope.initializeAPs=true;

                }
            }

            if (localStorage.getItem('localizationAccMode') !== undefined) {

                if (localStorage.getItem('localizationAccMode') === 'YES') {
                    $scope.initializeAcces=true;

                }
            }

            if (localStorage.getItem('connectionsMode') !== undefined) {

                if (localStorage.getItem('connectionsMode') === 'NO') {
                    $scope.initializeConnections=true;

                }
            }

            if (localStorage.getItem('POIsMode') !== undefined) {

                if (localStorage.getItem('POIsMode') === 'NO') {
                    $scope.initializePOIs=true;
                }
            }

            if (localStorage.getItem('fingerPrintsTimeMode') !== undefined) {

                if (localStorage.getItem('fingerPrintsTimeMode') === 'YES') {
                    $scope.initializeTime=true;

                }
            }

            function initializeFingerPrints(){

                $('#wifiTab').click();
                $('#FPs').click();
                $('#FPsButton').click();
            }
            function initializeRadioHeatmapRSS(){
                $('#wifiTab').click();
                $('#HMs').click();
                $('#HMsButton').click();
            }
            function initializeAPs(){
                $('#wifiTab').click();
                $('#HMs').click();
                $('#APsButton').click();
            }
            function initializeAcces(){

                $('#wifiTab').click();
                $('#LAs').click();
                $('#LAButton').click();
            }
            function initializeConnections(){
                $('#FPs').click();
                $('#connectionsButton').click();
            }
            function initializePOIs(){
                $('#FPs').click();
                $('#POIsButton').click();
            }
            function initializeTime(){

                $('#wifiTab').click();
                $('#FPs').click();
                $('#FPsTimeButton').click();
            }

            window.onload = function(){
                if($scope.initializeFingerPrints){
                    initializeFingerPrints();
                }
                if($scope.initializeRadioHeatmapRSS){
                    initializeRadioHeatmapRSS();
                }
                if($scope.initializeAPs){
                    initializeAPs();
                }
                if($scope.initializeAcces){
                    initializeAcces();
                }
                if($scope.initializeConnections){
                    initializeConnections();
                }
                if($scope.initializePOIs){
                    initializePOIs();
                }
                if($scope.initializeTime){
                    initializeTime();
                }
            }

            if (_HEATMAP_RSS_IS_ON) {

                var i = heatMap.length;
                while (i--) {
                    heatMap[i].rectangle.setMap(null);
                    heatMap[i] = null;
                }
                heatMap = [];

                $scope.showRadioHeatmapRSS();

                if( $scope.radioHeatmapRSSTimeMode ){
                    d3.selectAll("svg > *").remove();
                    $( "svg" ).remove();
                    $scope.getFingerPrintsTime();
                }

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

                i = $scope.example8data.length;
                while(i--){
                    $scope.example8data[i] = null;
                    $scope.example8model[i] = null;
                }
                APmap = [];
                $scope.example9data = [];
                $scope.example9model = [];
                $scope.example8data = [];
                $scope.example8model = [];
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

                if($scope.fingerPrintsTimeMode && !$scope.radioHeatmapRSSTimeMode){
                    d3.selectAll("svg > *").remove();
                    $( "svg" ).remove();
                    $scope.getFingerPrintsTime();
                }


            }


            if (heatmap && heatmap.getMap()) {
                //hide fingerPrints heatmap
                heatmap.setMap(null);
                var i=heatmapFingerprints.length;
                while(i--){
                    heatmapFingerprints[i]=null;
                }
                heatmapFingerprints=[];
                _HEATMAP_F_IS_ON=false;

                $scope.showFingerPrints();

                if($scope.fingerPrintsTimeMode && !$scope.radioHeatmapRSSTimeMode) {

                    d3.selectAll("svg > *").remove();
                    $( "svg" ).remove();
                    $scope.getFingerPrintsTime();

                }
            }

            if (heatmapAcc && heatmapAcc.getMap()) {
                //hide acces heatmap

                heatmapAcc.setMap(null);
                $scope.showLocalizationAccHeatmap();

            }

            var check = 0;
            if (!_CONNECTIONS_IS_ON) {
                connectionsMap = $scope.anyService.getAllConnections();
                var key = Object.keys(connectionsMap);
                if (connectionsMap[key[check]] !== undefined) {
                    if(connectionsMap[key[check]].polyLine !== undefined) {
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
                    heatMap[i].rectangle.setMap(null);
                    heatMap[i] = null;
                }
                heatMap = [];

                $scope.showRadioHeatmapRSS();

                if( $scope.radioHeatmapRSSTimeMode ){
                    d3.selectAll("svg > *").remove();
                    $( "svg" ).remove();
                    $scope.getFingerPrintsTime();
                }

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
                i = $scope.example8data.length;
                while(i--){
                    $scope.example8data[i] = null;
                    $scope.example8model[i] = null;
                }

                APmap = [];
                $scope.example9data = [];
                $scope.example9model = [];
                $scope.example8data = [];
                $scope.example8model = [];
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
                if($scope.fingerPrintsTimeMode && !$scope.radioHeatmapRSSTimeMode) {

                    d3.selectAll("svg > *").remove();
                    $( "svg" ).remove();
                    $scope.getFingerPrintsTime();

                }
            }


            if (heatmap && heatmap.getMap()) {
                //hide fingerPrints heatmap
                heatmap.setMap(null);
                var i=heatmapFingerprints.length;
                while(i--){
                    heatmapFingerprints[i]=null;
                }
                heatmapFingerprints=[];
                _HEATMAP_F_IS_ON=false;

                $scope.showFingerPrints();
                if($scope.fingerPrintsTimeMode && !$scope.radioHeatmapRSSTimeMode) {

                    d3.selectAll("svg > *").remove();
                    $( "svg" ).remove();
                    $scope.getFingerPrintsTime();

                }
            }

            if (heatmapAcc && heatmapAcc.getMap()) {
                //hide acces heatmap

                heatmapAcc.setMap(null);
                $scope.showLocalizationAccHeatmap();

            }

            var check = 0;
            if (!_CONNECTIONS_IS_ON ) {
                connectionsMap = $scope.anyService.getAllConnections();
                var key = Object.keys(connectionsMap);
                if (connectionsMap[key[check]] !== undefined) {
                    if(connectionsMap[key[check]].polyLine !== undefined) {
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

    $scope.toggleRadioHeatmapRSS = function () {

        var check = 0;
        if ((heatMap[check] !== undefined && heatMap[check] !== null) || $scope.radioHeatmapRSSTimeMode) {

            var i = heatMap.length;
            while (i--) {
                heatMap[i].rectangle.setMap(null);
                heatMap[i] = null;
            }
            heatMap = [];
            document.getElementById("radioHeatmapRSS-mode").classList.remove('draggable-border-green');
            //document.getElementById("radioHeatmapRSS-time-mode").classList.remove('draggable-border-green');
            _HEATMAP_RSS_IS_ON = false;
            setColorClicked('g',false);
            setColorClicked('y',false);
            setColorClicked('o',false);
            setColorClicked('p',false);
            setColorClicked('r',false);
            $scope.radioHeatmapRSSMode = false;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('radioHeatmapRSSMode', 'NO');
            }
            $scope.anyService.radioHeatmapRSSMode=false;
            $scope.radioHeatmapRSSTimeMode = false;
            if (typeof(Storage) !== "undefined" && localStorage && !$scope.fingerPrintsTimeMode) {
                localStorage.setItem('fingerPrintsTimeMode', 'NO');
            }
            $scope.anyService.radioHeatmapRSSTimeMode=false;
            $scope.radioHeatmapRSSHasGreen=false;
            $scope.radioHeatmapRSSHasYellow=false;
            $scope.radioHeatmapRSSHasOrange=false;
            $scope.radioHeatmapRSSHasPurple=false;
            $scope.radioHeatmapRSSHasRed=false;
            if(!$scope.fingerPrintsMode){
                document.getElementById("fingerPrints-time-mode").classList.remove('draggable-border-green');
            }
            $cookieStore.put('RSSClicked', 'NO');
            return;
        }

        if($scope.fingerPrintsTimeMode){
            $scope.radioHeatmapRSSTimeMode=true;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('fingerPrintsTimeMode', 'YES');
            }
            $scope.anyService.radioHeatmapRSSTimeMode=true;
        }

        document.getElementById("radioHeatmapRSS-mode").classList.add('draggable-border-green');
        $scope.radioHeatmapRSSMode = true;
        if (typeof(Storage) !== "undefined" && localStorage) {
            localStorage.setItem('radioHeatmapRSSMode', 'YES');
        }
        $scope.anyService.radioHeatmapRSSMode=true;

        $scope.showRadioHeatmapRSS();
        return;
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
            i = $scope.example8data.length;
            while(i--){
                $scope.example8data[i] = null;
                $scope.example8model[i] = null;
            }

            APmap = [];
            $scope.example9data = [];
            $scope.example9model = [];
            $scope.example8data = [];
            $scope.example8model = [];
            _APs_IS_ON = false;
            $scope.filterByMAC=false;
            $scope.filterByMAN=false;
            document.getElementById("APs-mode").classList.remove('draggable-border-green');
            $scope.APsMode = false;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('APsMode', 'NO');
            }
            return;

        }
        _APs_IS_ON = true;

        $scope.APsMode = true;

        if (typeof(Storage) !== "undefined" && localStorage) {
            localStorage.setItem('APsMode', 'YES');
        }

        document.getElementById("APs-mode").classList.add('draggable-border-green');

        $scope.showAPs();

    };
    $scope.toggleFingerPrints = function () {
        $scope.fingerPrintsMode = !$scope.fingerPrintsMode;

        if ($scope.fingerPrintsMode) {
            document.getElementById("fingerPrints-mode").classList.add('draggable-border-green');
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('fingerprintsMode', 'YES');
            }
        } else {
            document.getElementById("fingerPrints-mode").classList.remove('draggable-border-green');
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('fingerprintsMode', 'NO');
            }
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
            //document.getElementById("fingerPrints-time-mode").classList.remove('draggable-border-green');
            $scope.fingerPrintsMode = false;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('fingerprintsMode', 'NO');
            }
            $scope.fingerPrintsTimeMode=false;
            if (typeof(Storage) !== "undefined" && localStorage && !$scope.radioHeatmapRSSTimeMode) {
                localStorage.setItem('fingerPrintsTimeMode', 'NO');
            }
            $scope.anyService.fingerPrintsTimeMode=false;
            if(!$scope.radioHeatmapRSSMode){
                document.getElementById("fingerPrints-time-mode").classList.remove('draggable-border-green');
            }
            return;

        }

        if (heatmap && heatmap.getMap()) {
            //hide fingerPrints heatmap

            heatmap.setMap(null);
            _FINGERPRINTS_IS_ON = false;
            document.getElementById("fingerPrints-mode").classList.remove('draggable-border-green');
            //document.getElementById("fingerPrints-time-mode").classList.remove('draggable-border-green');
            $scope.fingerPrintsMode = false;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('fingerprintsMode', 'NO');
            }
            $scope.fingerPrintsTimeMode=false;
            if (typeof(Storage) !== "undefined" && localStorage && !$scope.radioHeatmapRSSTimeMode) {
                localStorage.setItem('fingerPrintsTimeMode', 'NO');
            }
            $scope.anyService.fingerPrintsTimeMode=false;
            _HEATMAP_F_IS_ON = false;
            if(!$scope.radioHeatmapRSSMode){
                document.getElementById("fingerPrints-time-mode").classList.remove('draggable-border-green');
            }
            var i=heatmapFingerprints.length;
            while(i--){
                heatmapFingerprints[i]=null;
            }
            heatmapFingerprints=[];
            return;
        }

        document.getElementById("fingerPrints-mode").classList.add('draggable-border-green');
        $scope.fingerPrintsMode = true;
        if (typeof(Storage) !== "undefined" && localStorage) {
            localStorage.setItem('fingerprintsMode', 'YES');
        }
        if($scope.radioHeatmapRSSTimeMode){
            $scope.fingerPrintsTimeMode=true;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('fingerPrintsTimeMode', 'YES');
            }
            $scope.anyService.fingerPrintsTimeMode=true;
        }

        $scope.showFingerPrints();
    };


    $scope.toggleLocalizationAccurancy = function () {
        $scope.localizationAccMode = !$scope.localizationAccMode;

        if (heatmapAcc && heatmapAcc.getMap()) {
            //hide fingerPrints heatmap

            heatmapAcc.setMap(null);
            document.getElementById("localizationAccurancy-mode").classList.remove('draggable-border-green');

            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('localizationAccMode', 'NO');
            }
            return;
        }

        document.getElementById("localizationAccurancy-mode").classList.add('draggable-border-green');

        if (typeof(Storage) !== "undefined" && localStorage) {
            localStorage.setItem('localizationAccMode', 'YES');
        }
        $scope.showLocalizationAccHeatmap();

    };


    $scope.toggleFingerPrintsTime = function () {
        if($scope.fingerPrintsMode){
            $scope.fingerPrintsTimeMode=!$scope.fingerPrintsTimeMode;
            if($scope.fingerPrintsTimeMode){
                if (typeof(Storage) !== "undefined" && localStorage) {
                    localStorage.setItem('fingerPrintsTimeMode', 'YES');
                }
            }else{
                if (typeof(Storage) !== "undefined" && localStorage && !$scope.radioHeatmapRSSTimeMode) {
                    localStorage.setItem('fingerPrintsTimeMode', 'NO');
                }
            }
            $scope.anyService.fingerPrintsTimeMode=!$scope.anyService.fingerPrintsTimeMode;
        }

        if($scope.radioHeatmapRSSMode){
            $scope.radioHeatmapRSSTimeMode=!$scope.radioHeatmapRSSTimeMode;
            if($scope.radioHeatmapRSSTimeMode){
                if (typeof(Storage) !== "undefined" && localStorage) {
                    localStorage.setItem('fingerPrintsTimeMode', 'YES');
                }
            }else{
                if (typeof(Storage) !== "undefined" && localStorage && !$scope.fingerPrintsTimeMode) {
                    localStorage.setItem('fingerPrintsTimeMode', 'NO');
                }
            }
            $scope.anyService.radioHeatmapRSSTimeMode=!$scope.anyService.radioHeatmapRSSTimeMode;
        }

        if(!$scope.fingerPrintsTimeMode && $scope.fingerPrintsMode){
            clearFingerPrints();
            $scope.showFingerPrints();
            document.getElementById("fingerPrints-mode").classList.add('draggable-border-green');
            document.getElementById("fingerPrints-time-mode").classList.remove('draggable-border-green');
        }

        if(!$scope.radioHeatmapRSSTimeMode && $scope.radioHeatmapRSSMode){
            clearRadioHeatmap();
            $scope.showRadioHeatmapRSS();
            $scope.radioHeatmapRSSMode=true;
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('radioHeatmapRSSMode', 'YES');
            }
            $scope.anyService.radioHeatmapRSSMode=true;
            document.getElementById("radioHeatmapRSS-mode").classList.add('draggable-border-green');
            document.getElementById("fingerPrints-time-mode").classList.remove('draggable-border-green');
        }

        if($scope.radioHeatmapRSSTimeMode || $scope.fingerPrintsTimeMode) {
            $scope.getFingerPrintsTime();

        }

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
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem('POIsMode', 'NO');
            }
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
        if (typeof(Storage) !== "undefined" && localStorage) {
            localStorage.setItem('POIsMode', 'YES');
        }
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

        if (connectionsMap[key[check]].polyLine !== undefined) {

            if (connectionsMap[key[check]].polyLine.getMap() !== undefined) {
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
                    _CONNECTIONS_IS_ON = false;
                    if (typeof(Storage) !== "undefined" && localStorage) {
                        localStorage.setItem('connectionsMode', 'NO');
                    }
                    return;
                }
            }
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

    $scope.getFingerPrintTimeButtonText = function () {

        return $scope.fingerPrintsTimeMode ? "Hide FingerPrints By Time" : "Show FingerPrints By Time";
    };

    $scope.getHeatMapTimeButtonText = function () {

        return $scope.radioHeatmapRSSTimeMode ? "Hide WiFi Map By Time" : "Show WiFi Map By Time";
    };

    $scope.getLocalizationAccurancyText = function(){
        return $scope.localizationAccMode ? "Hide Acces Map" : "Show Acces Map";
    }


    $scope.getPOIsButtonText = function () {

        POIsMap = $scope.anyService.getAllPois();
        var key = Object.keys(POIsMap);
        var check = 0;
        if (POIsMap.hasOwnProperty(key[check])) {
            if(POIsMap[key[check]].marker.getMap() !== undefined) {
                if (POIsMap[key[check]].marker.getMap() !== null) {
                    document.getElementById("POIs-mode").classList.add('draggable-border-green');
                    $scope.POIsMode = true;
                    return "Hide POIs";
                }
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
            if(connectionsMap[key[check]].polyLine !== undefined) {
                if (connectionsMap[key[check]].polyLine.getMap() !== undefined) {
                    if (connectionsMap[key[check]].polyLine.getMap() !== null) {
                        document.getElementById("connections-mode").classList.add('draggable-border-green');
                        $scope.connectionsMode = true;
                        return "Hide Edges";
                    }
                }
            }
        }
        document.getElementById("connections-mode").classList.remove('draggable-border-green');
        $scope.connectionsMode = false;
        return "Show Edges";
        //return (connectionsMap!==undefined && connectionsMap!==null)  ? "Hide Edges" : "Show Edges";
    };

    $('#HMs_1').unbind().click(function () {
        $('#wifiTab').click();
        $('#HMs').click();
        $('#HMsButton').click();
    });

    $('#APs_1').unbind().click(function () {
        $('#wifiTab').click();
        $('#HMs').click();
        $('#APsButton').click();
    });

    $('#FPs_1').unbind().click(function () {
        $('#wifiTab').click();
        $('#FPs').click();
        $('#FPsButton').click();
    });

    $('#DL_1').unbind().click(function () {
        $('#wifiTab').click();
        $('#FPs').click();
        $('#deleteButton').click();
    });

    $('#FPs_2').unbind().click(function () {
        $('#wifiTab').click();
        $('#FPs').click();
        $('#FPsTimeButton').click();

    });

    $('#LA_1').unbind().click(function () {
        $('#wifiTab').click();
        $('#LAs').click();
        $('#LAButton').click();

        //_err("Not available yet. Please check in the next release.");
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
        return $scope.radioHeatmapRSSMode ? "WiFi Map is online" : "WiFi Map is offline";

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

    $scope.getFingerPrintsTimeModeText = function () {
        return $scope.fingerPrintsTimeMode || $scope.radioHeatmapRSSTimeMode  ? "ON" : "OFF";

    };

    $scope.getLocalizationAccurancyModeText = function () {
        return $scope.localizationAccMode ? "Localization is online" : "Localization is offline";

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

        if (!_FINGERPRINTS_IS_ON && (!heatmap || !heatmap.getMap())) {
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

            var i = fingerPrintsMap.length;

            while (i--) {
                if (fingerPrintsMap[i].getPosition().lat() <= start.lat() && fingerPrintsMap[i].getPosition().lng() <= start.lng() && fingerPrintsMap[i].getPosition().lat() >= end.lat() && fingerPrintsMap[i].getPosition().lng() >= end.lng()) {

                    confirmation = confirm("Confirm:\nAre you sure you want to delete the selected fingerprints?");
                    break;
                }
            }
            i = heatmapFingerprints.length;
            while (i--) {
                if (heatmapFingerprints[i].getPosition().lat() <= start.lat() && heatmapFingerprints[i].getPosition().lng() <= start.lng() && heatmapFingerprints[i].getPosition().lat() >= end.lat() && heatmapFingerprints[i].getPosition().lng() >= end.lng()) {

                    confirmation = confirm("Confirm:\nAre you sure you want to delete the selected fingerprints?");
                    break;
                }
            }

            if(confirmation===undefined){
                alert("You have to select an area with Fingerprints to delete. ");
                e.overlay.setMap(null);
                drawingManager.setMap(null);
                $scope.deleteButtonWarning = false;
                _DELETE_FINGERPRINTS_IS_ON = false;
                $scope.deleteFingerPrintsMode = false;
                document.getElementById("delete-mode").classList.remove('draggable-border-green');
                return;
            }

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

                reqObj.lat1 = start.lat()+"";

                reqObj.lon1 = start.lng()+"";

                reqObj.lat2 = end.lat()+"";

                reqObj.lon2 = end.lng()+"";

                var promise;

                if($scope.fingerPrintsTimeMode){
                    reqObj.timestampX = userTimeData[0]+"";

                    reqObj.timestampY = userTimeData[1]+"";
                    promise= $scope.anyAPI.deleteFingerprintsByTime(reqObj);
                }else{
                    promise = $scope.anyAPI.deleteFingerprints(reqObj);
                }

                var data = [];

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

                            if (_HEATMAP_F_IS_ON) {
                                heatmap.setMap(null);
                                var heatMapData=[];
                                i = heatmapFingerprints.length;
                                while (i--) {
                                    if(heatmapFingerprints[i]!==null) {
                                        if (heatmapFingerprints[i].getPosition().lat() > start.lat() || heatmapFingerprints[i].getPosition().lng() > start.lng() || heatmapFingerprints[i].getPosition().lat() < end.lat() || heatmapFingerprints[i].getPosition().lng() < end.lng()) {
                                            heatMapData.push(
                                                {location: heatmapFingerprints[i].getPosition(), weight: 1}
                                            );
                                        } else {
                                            heatmapFingerprints[i] = null;
                                        }
                                    }
                                }
                                heatmap = new google.maps.visualization.HeatmapLayer({
                                    data: heatMapData
                                });
                                heatmap.setMap($scope.gmapService.gmap);
                            }
                            if (_HEATMAP_RSS_IS_ON) {
                                var i = heatMap.length;

                                while (i--) {
                                    if (heatMap[i].location.lat() <= start.lat() && heatMap[i].location.lng() <= start.lng() && heatMap[i].location.lat() >= end.lat() && heatMap[i].location.lng() >= end.lng()) {
                                        heatMap[i].rectangle.setMap(null);
                                    }
                                }

                            }
                        }

                        _suc("Successfully deleted " + data.length + " fingerPrints.");
                    },
                    function (resp) {
                        // on error
                        var data = resp.data
                        _err("Something went wrong. It's likely that everything related to the fingerPrints is deleted but please refresh to make sure or try again.");
                        document.getElementById("delete-mode").classList.remove('draggable-border-green');
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

    $scope.getColorBarTextFor = function (color) {

        return !getColorClicked(color.charAt(0)) ? "click to hide "+color+" ones" : "click to show "+color+" ones";

    };

    function setColorClicked(color,value){

        if(color==='g') {
            colorBarGreenClicked = value;
            if(value){
                document.getElementById("greenSquares").classList.add('faded');
            }else{
                document.getElementById("greenSquares").classList.remove('faded');
            }

        }else if(color==='y') {
            colorBarYellowClicked = value;
            if(value){
                document.getElementById("yellowSquares").classList.add('faded');
            }else{
                document.getElementById("yellowSquares").classList.remove('faded');
            }

        }else if (color==='o') {
            colorBarOrangeClicked = value;
            if(value){
                document.getElementById("orangeSquares").classList.add('faded');
            }else{
                document.getElementById("orangeSquares").classList.remove('faded');
            }

        }else if (color==='p') {
            colorBarPurpleClicked = value;
            if(value){
                document.getElementById("purpleSquares").classList.add('faded');
            }else{
                document.getElementById("purpleSquares").classList.remove('faded');
            }

        }else {
            colorBarRedClicked = value;
            if(value){
                document.getElementById("redSquares").classList.add('faded');
            }else{
                document.getElementById("redSquares").classList.remove('faded');
            }

        }


    };

    function getColorClicked(color){

        if(color==='g')
            return colorBarGreenClicked;
        else if(color==='y')
            return colorBarYellowClicked;
        else if (color==='o')
            return colorBarOrangeClicked;
        else if (color==='p')
            return colorBarPurpleClicked;
        else
            return colorBarRedClicked;
    };

    $scope.hideRSSExcept=function(color){

        if( color==='g' && !$scope.radioHeatmapRSSHasGreen){
            _err("Wi-Fi coverage map has no green squares");
            return;
        }

        if( color==='y' && !$scope.radioHeatmapRSSHasYellow){
            _err("Wi-Fi coverage map has no yellow squares");
            return;
        }

        if( color==='o' && !$scope.radioHeatmapRSSHasOrange){
            _err("Wi-Fi coverage map has no orange squares");
            return;
        }

        if( color==='p' && !$scope.radioHeatmapRSSHasPurple){
            _err("Wi-Fi coverage map has no purple squares");
            return;
        }

        if( color==='r' && !$scope.radioHeatmapRSSHasRed){
            _err("Wi-Fi coverage map has no red squares");
            return;
        }
        var i=heatMap.length;
        while(i--) {
            if (getColorClicked(color)){
                if (heatMap[i].id === color) {
                    heatMap[i].rectangle.setMap($scope.gmapService.gmap);
                    heatMap[i].clicked = true;
                }
            }else{
                if (heatMap[i].id === color) {
                    heatMap[i].rectangle.setMap(null);
                    heatMap[i].clicked = false;
                }

            }
        }
        setColorClicked(color,!getColorClicked(color));

    };

    $scope.showRadioHeatmapRSS = function () {

        var jsonReq;

        var promise;

        _NOW_ZOOM = GMapService.gmap.getZoom();

        if(($scope.radioHeatmapRSSTimeMode || $scope.fingerPrintsTimeMode) && userTimeData.length>0){
            jsonReq = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber(),"timestampX":userTimeData[0],"timestampY":userTimeData[1]};

            jsonReq.username = $scope.creds.username;
            jsonReq.password = $scope.creds.password;

            if (_NOW_ZOOM > MIN_ZOOM_FOR_HEATMAPS && _NOW_ZOOM < MAX_ZOOM_FOR_HEATMAPS) {
                levelOfZoom=2;
                promise = $scope.anyAPI.getRadioHeatmapRSSByTime_2(jsonReq);

            }else if (_NOW_ZOOM > MIN_ZOOM_FOR_HEATMAPS) {
                levelOfZoom=3;
                promise = $scope.anyAPI.getRadioHeatmapRSSByTime_3(jsonReq);


            }else {
                levelOfZoom=1;
                promise = $scope.anyAPI.getRadioHeatmapRSSByTime_1(jsonReq);
            }


        }else {

            jsonReq = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber()};

            jsonReq.username = $scope.creds.username;
            jsonReq.password = $scope.creds.password;


            if (_NOW_ZOOM > MIN_ZOOM_FOR_HEATMAPS && _NOW_ZOOM < MAX_ZOOM_FOR_HEATMAPS) {
                levelOfZoom=2;
                promise = $scope.anyAPI.getRadioHeatmapRSS_2(jsonReq);
            }else if (_NOW_ZOOM > MIN_ZOOM_FOR_HEATMAPS) {
                levelOfZoom=3;

                promise = $scope.anyAPI.getRadioHeatmapRSS_3(jsonReq);

            }else {
                levelOfZoom=1;
                promise = $scope.anyAPI.getRadioHeatmapRSS_1(jsonReq);
            }
        }

        if(promise!==undefined) {
            promise.then(
                function (resp) {
                    // on success
                    var data = resp.data;

                    var heatMapData = [];

                    var i = resp.data.radioPoints.length;

                    if (i <= 0) {
                        _err("This floor seems not to be WiFi mapped. Download the Anyplace app from the Google Play store to map the floor.");
                        if (!$scope.radioHeatmapRSSTimeMode) {
                            document.getElementById("radioHeatmapRSS-mode").classList.remove('draggable-border-green');
                            $scope.radioHeatmapRSSMode = false;
                            if (typeof(Storage) !== "undefined" && localStorage && !$scope.fingerPrintsTimeMode) {
                                localStorage.setItem('radioHeatmapRSSMode', 'NO');
                            }
                        } else {
                            _err("No fingerprints at this period.\n Please choose another one.");
                        }
                        return;
                    }

                    var j = 0;

                    while (i--) {

                        var rp = resp.data.radioPoints[i];
                        var rss = JSON.parse(rp.w); //count,average,total
                        //set weight based on RSSI

                        var w = parseFloat(rss.average);

                        if (w <= -30 && w >= -60) {

                            heatMapData.push(
                                {location: new google.maps.LatLng(rp.x, rp.y), weight: w, color: '#4ed419', id: 'g'}
                            );

                            $scope.radioHeatmapRSSHasGreen = true;

                        } else if (w < -60 && w >= -70) {

                            heatMapData.push(
                                {location: new google.maps.LatLng(rp.x, rp.y), weight: w, color: '#ffff00', id: 'y'}
                            );

                            $scope.radioHeatmapRSSHasYellow = true;

                        } else if (w < -70 && w >= -90) {

                            heatMapData.push(
                                {location: new google.maps.LatLng(rp.x, rp.y), weight: w, color: '#ffa500', id: 'o'}
                            );

                            $scope.radioHeatmapRSSHasOrange = true;

                        } else if (w < -90 && w >= -100) {

                            heatMapData.push(
                                {location: new google.maps.LatLng(rp.x, rp.y), weight: w, color: '#bd06bd', id: 'p'}
                            );

                            $scope.radioHeatmapRSSHasPurple = true;

                        } else {
                            heatMapData.push(
                                {location: new google.maps.LatLng(rp.x, rp.y), weight: w, color: '#ff0000', id: 'r'}
                            );

                            $scope.radioHeatmapRSSHasRed = true;

                        }

                        //calculate bounds
                        var center = heatMapData[j].location;
                        var size;
                        if (levelOfZoom == 3) {
                            size = new google.maps.Size(0.75, 0.75);
                        } else if (levelOfZoom == 2) {
                            size = new google.maps.Size(2, 2);
                        } else {
                            size = new google.maps.Size(5, 5);
                        }
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
                            bounds: new google.maps.LatLngBounds(
                                new google.maps.LatLng(s, w),
                                new google.maps.LatLng(n, e))
                        });
                        if (getColorClicked(heatMapData[j].id)) {
                            rectangle.setMap(null);
                        } else {
                            rectangle.setMap($scope.gmapService.gmap);
                        }

                        heatMap.push(
                            {rectangle: rectangle, location: center, id: heatMapData[j].id, clicked: false}
                        );
                        j++;

                        resp.data.radioPoints.splice(i, 1);
                    }
                    _HEATMAP_RSS_IS_ON = true;
                    $cookieStore.put('RSSClicked', 'YES');

                },
                function (resp) {
                    // on error
                    var data = resp.data;
                    _err('Something went wrong while fetching radio heatmap.');
                    if (!$scope.radioHeatmapRSSTimeMode) {
                        $scope.radioHeatmapRSSMode = false;
                        if (typeof(Storage) !== "undefined" && localStorage && !$scope.fingerPrintsTimeMode) {
                            localStorage.setItem('radioHeatmapRSSMode', 'NO');
                        }
                        document.getElementById("radioHeatmapRSS-mode").classList.remove('draggable-border-green');
                    }

                }
            );
        }
    }


    $scope.getAPsIds=function(jsonInfo){

        var jsonReq={};
        jsonReq.ids=jsonInfo;

        var promise= $scope.anyAPI.getAPsIds(jsonReq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data.accessPoints;

                var i = data.length;

                if (i <= 0) {
                    _err("Access Points seems to not have ids");
                    return;
                }

                var dataIDs=new Set();

                while(i--){
                    APmap[i].mun=data[i];
                    dataIDs.add(data[i]);

                }

                dataIDs.forEach(function (element) {
                    if(element!=="N/A"){
                    $scope.example8data.push({id: element, label: element});
                    $scope.example8model.push({id: element, label: element});
                }
            });


            },
            function (resp) {
                // on error
                var data = resp.data;
                _err('Something went wrong while fetching the ids of access points.');
            }
        );


    };


    $scope.showAPs = function () {
        //request for access points
        var jsonReq = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber()};

        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;

        var i;

        var promise = $scope.anyAPI.getAPs(jsonReq);
        promise.then(
            function (resp) {
                // on success

                i = resp.data.accessPoints.length;

                if (i <= 0) {
                    _err("This floor seems not to be Access Point mapped. Download the Anyplace app from the Google Play store to map the floor.");
                    $scope.APsMode=false;
                    document.getElementById("APs-mode").classList.remove('draggable-border-green');
                    return;
                }


                //algorithm to find the location of each AP
                var values = resp.data.accessPoints;

                i = values.length;


                //create AccessPoint "map"
                var _ACCESS_POINT_IMAGE = 'build/images/access-point-icon.svg';

                var imgType = _ACCESS_POINT_IMAGE;

                var size = new google.maps.Size(21, 32);

                i = values.length;
                var c = 0;
                var x;
                var y;
                var jsonInfo=[];
                while (i--) {
                    //check for limit
                    if (c == MAX) {
                        _err('Access Points have exceeded the maximun limit of 1000');
                        break;
                    }
                    if( values[i].den!=0) {
                        x = values[i].x / values[i].den;
                        y = values[i].y / values[i].den;
                    }else{
                        x = values[i].x;
                        y = values[i].y;

                    }
                    $scope.example9data.push({id: values[i].AP, label: values[i].AP});
                    $scope.example9model.push({id: values[i].AP, label: values[i].AP});

                    //alert("x: "+x+" y: "+y);

                    var accessPoint = new google.maps.Marker({
                        id: values[i].AP,
                        position: new google.maps.LatLng(x, y),
                        map: GMapService.gmap,
                        zIndex: 9999,
                        icon: new google.maps.MarkerImage(
                            imgType,
                            null, /* size is determined at runtime */
                            null, /* origin is 0,0 */
                            null, /* anchor is bottom center of the scaled image */
                            size
                        )
                    });

                    APmap.push(
                        accessPoint
                    );

                    jsonInfo.push(accessPoint.id);

                    var infowindow=new google.maps.InfoWindow();
                    if(!infowindow.getMap()) {
                        APmap[c].addListener('click', function () {

                            if(this.mun !== "N/A") {
                                infowindow.setContent(this.id + "<br><center>-</center><br>" + this.mun);
                            }else{
                                infowindow.setContent(this.id);
                            }

                            infowindow.open(this.gmap, this);
                        });
                    }


                    c++;
                }
                $scope.getAPsIds(jsonInfo);
            },
            function (resp) {
                // on error
                var data = resp.data;
                $scope.APsMode=false;
                _err('Something went wrong while fetching Access Points.');
                document.getElementById("APs-mode").classList.remove('draggable-border-green');
            }
        );

    }

    $scope.getFingerPrintsTime = function (){


        var jsonReq = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber()};

        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;

        var promise=$scope.anyAPI.getFingerprintsTime(jsonReq);

        promise.then(
            function (resp) {
                // on success
                var data = resp.data.radioPoints;

                var i = data.length;

                if (i <= 0) {
                    _err("This floor seems not to be FingerPrint mapped. Download the Anyplace app from the Google Play store to map the floor.");
                    return;
                }

                plotGraphs(data);

                initializeTimeFunction();
            },
            function (resp) {
                // on error
                var data = resp.data;
                _err('Something went wrong while fetching fingerPrints timestamp.');
            }
        );



    };



    $scope.showFingerPrints = function () {

        var jsonReq;
        var promise;

        if(($scope.fingerPrintsTimeMode || $scope.radioHeatmapRSSTimeMode) && userTimeData.length>0){
            jsonReq = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber(),"timestampX":userTimeData[0],"timestampY":userTimeData[1]};

            jsonReq.username = $scope.creds.username;
            jsonReq.password = $scope.creds.password;

            if(_NOW_ZOOM !== _MAX_ZOOM_LEVEL) {


                promise = $scope.anyAPI.getRadioHeatmapRSSByTime(jsonReq);
            }else{

                var layerID = 'my_custom_layer';
                var layer = new google.maps.ImageMapType({
                    name: layerID,
                    getTileUrl: function(coord, zoom) {
                        if(_NOW_ZOOM !== _MAX_ZOOM_LEVEL || !$scope.fingerPrintsTimeMode)
                            return null;
                        var jsonReqTiles = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber(),"timestampX":userTimeData[0],"timestampY":userTimeData[1]};

                        jsonReqTiles.username = $scope.creds.username;
                        jsonReqTiles.password = $scope.creds.password;
                            jsonReqTiles.x = coord.x;
                            jsonReqTiles.y = coord.y;
                            jsonReqTiles.z = zoom;
                            var tilePromise = $scope.anyAPI.getRadioHeatmapRSSByTime_Tiles(jsonReqTiles);

                            tilePromise.then(
                                function (resp) {

                                        // on success
                                        var data = resp.data;

                                        var fingerPrintsData = [];

                                        var i = resp.data.radioPoints.length;


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


                                        while (i--) {
                                            if(_NOW_ZOOM === _MAX_ZOOM_LEVEL) {
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
                                            }

                                        }
                                        _FINGERPRINTS_IS_ON = true;



                                },
                                function (resp) {
                                    // on error
                                    var data = resp.data;
                                    _err('Something went wrong while fetching fingerPrints.');
                                    if (!$scope.fingerPrintsMode) {
                                        document.getElementById("fingerPrints-mode").classList.remove('draggable-border-green');
                                        $scope.fingerPrintsMode = false;
                                        if (typeof(Storage) !== "undefined" && localStorage) {
                                            localStorage.setItem('fingerprintsMode', 'NO');
                                        }
                                    }
                                }
                            );

                        return null;
                    },
                    tileSize: new google.maps.Size(256, 256),
                    minZoom: 1,
                    maxZoom: 22
                });

                GMapService.gmap.mapTypes.set(layerID, layer);
                GMapService.gmap.setMapTypeId(layerID);
            }

        }else {

            jsonReq = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber()};

            jsonReq.username = $scope.creds.username;
            jsonReq.password = $scope.creds.password;


            if(_NOW_ZOOM !== _MAX_ZOOM_LEVEL) {
                promise = $scope.anyAPI.getRadioHeatmapRSS_3(jsonReq);
            }else{

                var layerID = 'my_custom_layer1';
                var layer = new google.maps.ImageMapType({
                    name: layerID,
                    getTileUrl: function(coord, zoom) {
                        if(_NOW_ZOOM !== _MAX_ZOOM_LEVEL || !$scope.fingerPrintsMode)
                            return null;
                        var jsonReqTiles = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber()};

                        jsonReqTiles.username = $scope.creds.username;
                        jsonReqTiles.password = $scope.creds.password;
                        jsonReqTiles.x = coord.x;
                        jsonReqTiles.y = coord.y;
                        jsonReqTiles.z = zoom;
                        var tilePromise = $scope.anyAPI.getRadioHeatmapRSS_3_Tiles(jsonReqTiles);

                        tilePromise.then(
                            function (resp) {

                                // on success
                                var data = resp.data;

                                var fingerPrintsData = [];

                                var i = resp.data.radioPoints.length;


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


                                while (i--) {
                                    if(_NOW_ZOOM === _MAX_ZOOM_LEVEL) {
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
                                    }

                                }
                                _FINGERPRINTS_IS_ON = true;



                            },
                            function (resp) {
                                // on error
                                var data = resp.data;
                                _err('Something went wrong while fetching fingerPrints.');
                                if (!$scope.fingerPrintsMode) {
                                    document.getElementById("fingerPrints-mode").classList.remove('draggable-border-green');
                                    $scope.fingerPrintsMode = false;
                                    if (typeof(Storage) !== "undefined" && localStorage) {
                                        localStorage.setItem('fingerprintsMode', 'NO');
                                    }
                                }
                            }
                        );
                        var url = null;
                        if ( GMapService.gmap.getMapTypeId() === 'CartoLight') {
                            url = "https://cartodb-basemaps-a.global.ssl.fastly.net/light_all/{z}/{x}/{y}.png";

                            url = url.replace('{x}', coord.x)
                                .replace('{y}', coord.y)
                                .replace('{z}', zoom);
                        } else if (GMapService.gmap.getMapTypeId() === 'roadmap' ||  GMapService.gmap.getMapTypeId() === 'satellite') {
                            //TODO: Add GOOGLE TILES URL with api key
                        }
                        return null;
                    },
                    tileSize: new google.maps.Size(256, 256),
                    minZoom: 1,
                    maxZoom: 22
                });
                localStorage.setItem("previousMapTypeId",GMapService.gmap.getMapTypeId());
                GMapService.gmap.mapTypes.set(layerID, layer);
                GMapService.gmap.setMapTypeId(layerID);

            }

        }

    if(promise!==undefined) {
        promise.then(
            function (resp) {

            // on success
            var data = resp.data;

            var fingerPrintsData = [];

            var i = resp.data.radioPoints.length;

            if (i <= 0) {
                if (!$scope.fingerPrintsTimeMode) {
                    _err("This floor seems not to be FingerPrint mapped. Download the Anyplace app from the Google Play store to map the floor.");
                    document.getElementById("fingerPrints-mode").classList.remove('draggable-border-green');
                    $scope.fingerPrintsMode = false;
                    if (typeof(Storage) !== "undefined" && localStorage) {
                        localStorage.setItem('fingerprintsMode', 'NO');
                    }

                } else {
                    _err("No fingerprints at this period.\n Please choose another one.");
                }
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


                while (i--) {

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

                }
                _FINGERPRINTS_IS_ON = true;
            } else {


                var heatMapData = [];
                var c = 0;
                while (i--) {
                    var rp = resp.data.radioPoints[i];
                    var rss = JSON.parse(rp.w); //count,average,total
                    heatMapData.push(
                        {location: new google.maps.LatLng(rp.x, rp.y), weight: 1}
                    );
                    var fingerPrint = new google.maps.Marker({
                        position: heatMapData[c].location,
                    });
                    heatmapFingerprints.push(
                        fingerPrint
                    );
                    resp.data.radioPoints.splice(i, 1);
                    c++;
                }


                heatmap = new google.maps.visualization.HeatmapLayer({
                    data: heatMapData
                });
                heatmap.setMap($scope.gmapService.gmap);

                _HEATMAP_F_IS_ON = true;

            }


        },
            function (resp) {
            // on error
            var data = resp.data;
            _err('Something went wrong while fetching fingerPrints.');
            if (!$scope.fingerPrintsMode) {
                document.getElementById("fingerPrints-mode").classList.remove('draggable-border-green');
                $scope.fingerPrintsMode = false;
                if (typeof(Storage) !== "undefined" && localStorage) {
                    localStorage.setItem('fingerprintsMode', 'NO');
                }
            }
        }
    );
}
    };

    $scope.showLocalizationAccHeatmap = function (){
        var jsonReq = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber()};

        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;

        var promise = $scope.anyAPI.getHeatmapAcces(jsonReq);

        promise.then(
            function (resp) {

                var values=  resp.data.crlb;
                var data = resp.data.geojson.coordinates;

                var i = data.length;

                if (i <= 0) {
                    _err("This floor seems not to be FingerPrint mapped. Download the Anyplace app from the Google Play store to map the floor.");
                    document.getElementById("localizationAccurancy-mode").classList.remove('draggable-border-green');
                    $scope.localizationAccMode=false;
                    if (typeof(Storage) !== "undefined" && localStorage) {
                        localStorage.setItem('localizationAccMode', 'NO');
                    }
                    return;
                }

                var heatMapData = [];

                fltr = function(v) { return !isNaN(v) && v != Number.POSITIVE_INFINITY };
                var crlb_clamp = 5.0*4;
                //console.log('crlbs: ', values);
                values = values.map(function(v) { return fltr(v) ? Math.min(v, crlb_clamp) : crlb_clamp});
                //console.log('crlbs clamp: ', values);
                var crlb_max = crlb_clamp;
                //console.log("crlb_max: ", crlb_max);
                var weights = values.map(function(v) { return 1-Math.log(1.0 + v / crlb_max) });
                //console.log('weights: ', weights);


                while (i--) {
                    var rp = data[i];
                    heatMapData.push(
                        {
                            location: new google.maps.LatLng(rp[0], rp[1]),
                            // weight: values[i]
                            weight: weights[i]
                        }
                    );


                    data.splice(i, 1);
                }

                heatmapAcc = new google.maps.visualization.HeatmapLayer({
                    data: heatMapData
                });

                var gradient = [
                    'rgba(0, 255, 255, 0)',
                    'rgba(0, 255, 255, 1)',
                    'rgba(0, 191, 255, 1)',
                    'rgba(0, 127, 255, 1)',
                    'rgba(0, 63, 255, 1)',
                    'rgba(0, 0, 255, 1)',
                    'rgba(0, 0, 223, 1)',
                    'rgba(0, 0, 191, 1)',
                    'rgba(0, 0, 159, 1)',
                    'rgba(0, 0, 127, 1)',
                    'rgba(63, 0, 91, 1)',
                    'rgba(127, 0, 63, 1)',
                    'rgba(191, 0, 31, 1)',
                    'rgba(255, 0, 0, 1)'
                ];



                heatmapAcc.set('gradient', gradient);

                heatmapAcc.set('radius', 20);

                heatmapAcc.setMap($scope.gmapService.gmap);


            }, function (resp) {
                // on error
                var data = resp.data;
                _err('Something went wrong while fetching fingerPrints.');
                document.getElementById("localizationAccurancy-mode").classList.remove('draggable-border-green');
                $scope.localizationAccMode=false;
                if (typeof(Storage) !== "undefined" && localStorage) {
                    localStorage.setItem('localizationAccMode', 'NO');
                }
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
        if (typeof(Storage) !== "undefined" && localStorage) {
            localStorage.setItem('connectionsMode', 'YES');
        }
        $scope.anyService.setAllConnection(connectionsMap);

    };

    //zoom handler for clustering

    GMapService.gmap.addListener('zoom_changed', function () {
        _NOW_ZOOM = GMapService.gmap.getZoom();

        if(_NOW_ZOOM!==_MAX_ZOOM_LEVEL){

            var i = fingerPrintsMap.length;

            //hide fingerPrints
            while (i--) {
                fingerPrintsMap[i].setMap(null);
                fingerPrintsMap[i] = null;
            }
            fingerPrintsMap = [];
            if (GMapService.gmap.getMapTypeId() === 'my_custom_layer1' ) {
                GMapService.gmap.setMapTypeId(localStorage.getItem("previousMapTypeId"))                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                ;
            } 
        }

        if (_HEATMAP_RSS_IS_ON) {
            if ((_PREV_ZOOM == MIN_ZOOM_FOR_HEATMAPS && _NOW_ZOOM > _PREV_ZOOM) || (_PREV_ZOOM > MIN_ZOOM_FOR_HEATMAPS && _PREV_ZOOM < MAX_ZOOM_FOR_HEATMAPS && (_NOW_ZOOM <= MIN_ZOOM_FOR_HEATMAPS || _NOW_ZOOM >= MAX_ZOOM_FOR_HEATMAPS)) || (_PREV_ZOOM == MAX_ZOOM_FOR_HEATMAPS && _NOW_ZOOM < _PREV_ZOOM)) {
                var i = heatMap.length;
                while (i--) {
                    heatMap[i].rectangle.setMap(null);
                    heatMap[i] = null;
                }
                heatMap = [];

                $scope.showRadioHeatmapRSS();
            }
        }
        if ((_FINGERPRINTS_IS_ON || (heatmap && heatmap.getMap())) && !changedfloor) {
            if (_NOW_ZOOM == _MAX_ZOOM_LEVEL || _PREV_ZOOM == _MAX_ZOOM_LEVEL) {
                var i = fingerPrintsMap.length;
                while (i--) {
                    fingerPrintsMap[i].setMap(null);
                    fingerPrintsMap[i] = null;
                }
                fingerPrintsMap = [];

                if (heatmap && heatmap.getMap()) {
                    heatmap.setMap(null);
                    var i=heatmapFingerprints.length;
                    while(i--){
                        heatmapFingerprints[i]=null;
                    }
                    heatmapFingerprints=[];
                    _HEATMAP_F_IS_ON=false;
                }

                $scope.showFingerPrints();
            }
        }
        _PREV_ZOOM = _NOW_ZOOM;
    });


    $scope.selectFilterForAPs=function() {

        var option = $scope.selected;
        switch (option) {
            case '0': {
                $scope.filterByMAC = true;
                $scope.filterByMAN = false;
                break;
            }
            default: {
                $scope.filterByMAC = false;
                $scope.filterByMAN = true;
                break;
            }

         }

    };
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

    $scope.multiuserevents1 = {

        onItemDeselect: function (item) {
            i = APmap.length;
            while (i--) {
                if (APmap[i].mun == item.id) {
                    APmap[i].setVisible(false);
                    break;
                }
            }

        },
        onItemSelect: function (item) {
            i = APmap.length;

            while (i--) {
                if (APmap[i].mun == item.id) {
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

    function plotGraphs(timestamps) {
        // Various formatters.
        var formatNumber = d3.format(",d");

        // A little coercion, since the CSV is untyped.
        timestamps.forEach(function (d, i) {
            d.index = i;
            d.date = parseDate(d.date);

        });

        // Create the crossfilter for the relevant dimensions and groups.
        var timestamp = crossfilter(timestamps),
            all = timestamp.groupAll(),
            date = timestamp.dimension(function (d) {
                return d.date;
            }),
            dates = date.group(d3.time.day).reduceSum(function (d) {
                return d.count;
            });

        var dateToString=timestamps[timestamps.length - 1].date.toString();
        var endDate= new Date(dateToString);
         endDate.setDate(endDate.getDate() + 15);

        var charts = [

            barChart()
                .dimension(date)
                .group(dates)
                .round(d3.time.day.round)
                .x(d3.time.scale()
                    .domain([timestamps[0].date, endDate])
                    .rangeRound([0, 10 * 100])) //90
                .filter([timestamps[0].date, endDate])

        ];

        // Given our array of charts, which we assume are in the same order as the
        // .chart elements in the DOM, bind the charts to the DOM and render them.
        // We also listen to the chart's brush events to update the display.
        var chart = d3.selectAll(".chart")
            .data(charts)
            .each(function (chart) {
                chart.on("brush", renderAll).on("brushend", renderAll);
            });


        // Render the total.
        d3.selectAll("#total")
            .text(formatNumber(timestamp.size()));

        renderAll();

        // Renders the specified chart or list.
        function render(method) {
            d3.select(this).call(method);
        }

        // Whenever the brush moves, re-rendering everything.
        function renderAll() {
            chart.each(render);
            d3.select("#active").text(formatNumber(all.value()));
        }

        //parse to date
        function parseDate(d) {

            return new Date((d / 1000) * 1000);

        }

        window.filter = function (filters) {
            filters.forEach(function (d, i) {
                charts[i].filter(d);
            });
            renderAll();
        };

        window.reset = function (i) {
            charts[i].filter(null);
            renderAll();
        };

        function barChart() {
            if (!barChart.id) barChart.id = 0;

            var margin = {top: 10, right: 10, bottom: 20, left: 10},
                x,
                y = d3.scale.linear().range([100, 0]),
                id = barChart.id++,
                axis = d3.svg.axis().orient("bottom"),
                brush = d3.svg.brush(),
                brushDirty,
                dimension,
                group,
                round;


            function chart(div) {
                var width = x.range()[1],
                    height = y.range()[0];

                y.domain([0, group.top(1)[0].value]);

                div.each(function () {
                    var div = d3.select(this),
                        g = div.select("g");

                    // Create the skeletal chart.
                    if (g.empty()) {
                        div.select(".title").append("a")
                            .attr("href", "javascript:reset(" + id + ")")
                            .attr("class", "reset")
                            .text("reset")
                            .style("display", "none");

                        g = div.append("svg")
                            .attr("width", width + margin.left + margin.right)
                            .attr("height", height + margin.top + margin.bottom)
                            .append("g")
                            .attr("transform", "translate(" + margin.left + "," + margin.top + ")");

                        g.append("clipPath")
                            .attr("id", "clip-" + id)
                            .append("rect")
                            .attr("width", width)
                            .attr("height", height);

                        g.selectAll(".bar")
                            .data(["background", "foreground"])
                            .enter().append("path")
                            .attr("class", function (d) {
                                return d + " bar";
                            })
                            .datum(group.all());

                        g.selectAll(".foreground.bar")
                            .attr("clip-path", "url(#clip-" + id + ")");

                        g.append("g")
                            .attr("class", "axis")
                            .attr("transform", "translate(0," + height + ")")
                            .call(axis);

                        // Initialize the brush component with pretty resize handles.
                        var gBrush = g.append("g").attr("class", "brush").call(brush);
                        gBrush.selectAll("rect").attr("height", height);
                        gBrush.selectAll(".resize").append("path").attr("d", resizePath);
                    }

                    // Only redraw the brush if set externally.
                    if (brushDirty) {
                        brushDirty = false;
                        g.selectAll(".brush").call(brush);
                        div.select(".title a").style("display", brush.empty() ? "none" : null);
                        if (brush.empty()) {
                            g.selectAll("#clip-" + id + " rect")
                                .attr("x", 0)
                                .attr("width", width);
                        } else {
                            var extent = brush.extent();
                            g.selectAll("#clip-" + id + " rect")
                                .attr("x", x(extent[0]))
                                .attr("width", x(extent[1]) - x(extent[0]));
                        }
                    }

                    g.selectAll(".bar").attr("d", barPath);

                });

                function barPath(groups) {
                    var path = [],
                        i = -1,
                        n = groups.length,
                        d;
                    while (++i < n) {
                        d = groups[i];
                        path.push("M", x(d.key), ",", height, "V", y(d.value), "h9V", height);
                    }
                    return path.join("");
                }

                function resizePath(d) {
                    var e = +(d == "e"),
                        x = e ? 1 : -1,
                        y = height / 3;
                    return "M" + (.5 * x) + "," + y
                        + "A6,6 0 0 " + e + " " + (6.5 * x) + "," + (y + 6)
                        + "V" + (2 * y - 6)
                        + "A6,6 0 0 " + e + " " + (.5 * x) + "," + (2 * y)
                        + "Z"
                        + "M" + (2.5 * x) + "," + (y + 8)
                        + "V" + (2 * y - 8)
                        + "M" + (4.5 * x) + "," + (y + 8)
                        + "V" + (2 * y - 8);
                }
            }


            brush.on("brushstart.chart", function () {
                var div = d3.select(this.parentNode.parentNode.parentNode);
                div.select(".title a").style("display", null);
            });

            brush.on("brush.chart", function () {
                var g = d3.select(this.parentNode),
                    extent = brush.extent();
                if (round) g.select(".brush")
                    .call(brush.on('brushend',bindSelect))
                    .selectAll(".resize")
                    .style("display", null);
                g.select("#clip-" + id + " rect")
                    .attr("x", x(extent[0]))
                    .attr("width", x(extent[1]) - x(extent[0]));
                dimension.filterRange(extent);


                //handler for user's selection
                function bindSelect(){

                    initializeTimeFunction();

                    extent = brush.extent(); //data of selection

                    userTimeData=[];

                    extent.forEach(function (element) {
                        var t = String(element.getTime() / 1000 * 1000);
                        userTimeData.push(t);
                    });

                    if($scope.fingerPrintsMode) {
                        clearFingerPrints();

                        $scope.showFingerPrints();

                        document.getElementById("fingerPrints-mode").classList.add('draggable-border-green');

                    }

                    if($scope.radioHeatmapRSSMode) {
                        clearRadioHeatmap();

                        $scope.showRadioHeatmapRSS();
                        $scope.radioHeatmapRSSMode = true;
                        if (typeof(Storage) !== "undefined" && localStorage) {
                            localStorage.setItem('radioHeatmapRSSMode', 'YES');
                        }
                        $scope.anyService.radioHeatmapRSSMode = true;
                        document.getElementById("radioHeatmapRSS-mode").classList.add('draggable-border-green');

                    }

                }
            });

            brush.on("brushend.chart", function () {
                if (brush.empty()) {
                    var div = d3.select(this.parentNode.parentNode.parentNode);
                    div.select(".title a").style("display", "none");
                    div.select("#clip-" + id + " rect").attr("x", null).attr("width", "100%");
                    dimension.filterAll();
                }
            });

            chart.margin = function (_) {
                if (!arguments.length) return margin;
                margin = _;
                return chart;
            };

            chart.x = function (_) {
                if (!arguments.length) return x;
                x = _;
                axis.scale(x);
                brush.x(x);
                return chart;
            };

            chart.y = function (_) {
                if (!arguments.length) return y;
                y = _;
                return chart;
            };

            chart.dimension = function (_) {
                if (!arguments.length) return dimension;
                dimension = _;
                return chart;
            };

            chart.filter = function (_) {
                if (_) {
                    brush.extent(_);
                    dimension.filterRange(_);
                } else {
                    brush.clear();
                    dimension.filterAll();
                }
                brushDirty = true;
                return chart;
            };

            chart.group = function (_) {
                if (!arguments.length) return group;
                group = _;
                return chart;
            };

            chart.round = function (_) {
                if (!arguments.length) return round;
                round = _;
                return chart;
            };

            return d3.rebind(chart, brush, "on");
        }

    }

    //set localstorage







}]);