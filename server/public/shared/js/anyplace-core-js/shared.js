/*
 * Anyplace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Paschalis Mpeis
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
 * --------------------------------------------------------------------------------
 *
 * Shared javascript resources among different services:
 *  - anyplace_architect
 *  - anyplace_viewer
 *  - anyplace_viewer_campus
 *
 */

var IMG_ACCESS_POINT_ARCHITECT = 'build/images/wireless-router-icon-bg.png';
var IMG_BUILDING_ARCHITECT = 'build/images/building-icon.png';
var IMG_VESSEL_ARCHITECT = 'build/images/vessel-icon.png';
// PM: For some reason different dimensions are used for viewer
var IMG_BUILDING_VIEWER = 'build/images/building-icon-viewer.png';
var IMG_FINGERPRINT_RED_SPOT= 'build/images/red_dot.png';

var DEFAULT_AUTOHIDE=5000

// Activate tooltips
$('document').ready(function(){
    // modal focus fix
    $('#myModal_Welcome').on('shown.bs.modal', function () {
        $('#myModal_Welcome').trigger('focus')
    });

    // Activate tooltips
    if ($("[rel=tooltip]").length) {
        $("[rel=tooltip]").tooltip();
    }
});

function __addAlert(scope, level, msg) {
  // INFO new lines are not displayed.
  // See more here: https://stackoverflow.com/a/14963641/776345
  // msg = msg.replace(/(?:\r\n|\r|\n)/g, '\n');
  scope.anyService.addAlert(level, msg);
}

var _err  = function (scope, msg) { __addAlert(scope, 'danger',  msg); }
var _suc  = function (scope, msg) { __addAlert(scope, 'success', msg); }
var _info = function (scope, msg) { __addAlert(scope, "info",    msg); }
var _warn = function (scope, msg) { __addAlert(scope, 'warning', msg); }

var _warn_autohide = function (scope, msg) { _warn_autohide_timeout(scope, msg, DEFAULT_AUTOHIDE); }
var _info_autohide = function (scope, msg) { _info_autohide_timeout(scope, msg, DEFAULT_AUTOHIDE); }
var _suc_autohide = function (scope, msg) { _suc_autohide_timeout(scope, msg, DEFAULT_AUTOHIDE); }

var _warn_autohide_timeout = function (scope, msg, timeout) {
    _msg_autohide(scope, msg, 'warning', timeout)
};

var _info_autohide_timeout = function (scope, msg, timeout) {
    _msg_autohide(scope, msg, 'info', timeout)
}

var _suc_autohide_timeout = function (scope, msg, timeout) {
    _msg_autohide(scope, msg, 'success', timeout)
}

var _msg_autohide = function (scope, msg, msgType, timeout) {
    __addAlert(scope, msgType, msg)
    window.setTimeout(function() {
        $(".alert-"+msgType).fadeTo(500, 0).slideUp(500, function(){
            $(this).remove();
        });
    }, timeout);
}


function _ShowAlert(scope, func, response, defaultMsg, showDefaultMessage) {
  var data = response.data;
  var msg = defaultMsg;
  if (data != null && data["message"] != null) {
    if(showDefaultMessage) {
      msg += ": " + data["message"];
    } else {
      msg = data["message"];
    }
  }
  func(scope, msg);
}

/**
 * Show Error message from server's response.
 *
 * @param scope
 * @param response
 * @param defaultMsg
 * @param showDefaultMessage
 */
function ShowError(scope, response, defaultMsg, showDefaultMessage) {
  showDefaultMessage = showDefaultMessage || false;
  _ShowAlert(scope, _err, response, defaultMsg, showDefaultMessage)
}

/**
 *  Show Warning message from server's response that autohides.
 * @param scope
 * @param response
 * @param defaultMsg
 * @param showDefaultMessage
 * @constructor
 */
function ShowWarningAutohide(scope, response, defaultMsg, showDefaultMessage) {
  showDefaultMessage = showDefaultMessage || false;
  _ShowAlert(scope, _warn_autohide, response, defaultMsg, showDefaultMessage)
}

function HandleGeolocationError(scope, errorCode) {
  if (errorCode === 1) {
    _warn_autohide(scope, ERR_GEOLOC_DEVICE_SETTINGS)
  } else if (errorCode === 2) {
      _warn_autohide(scope, ERR_GEOLOC_NET_OR_SATELLITES)
  } else if (errorCode === 3) {
      _warn_autohide(scope, ERR_GEOLOC_TIMEOUT)
  } else {
      _warn_autohide(scope, ERR_GEOLOC_UNKNOWN);
  }
}

function selectAllInputText(element) {
  element.setSelectionRange(0, element.value.length)
}

function getMapsIconBuildingViewer(scope, latLong) {
    // console.log("getMapsIconBuildingViewer")
    var s = new google.maps.Size(27.5, 40);
    if (scope.isFirefox)
        s = new google.maps.Size(55, 80);

    return new google.maps.Marker({
        position: latLong,
        icon: {
            url: IMG_BUILDING_VIEWER,
            size: s,
            scaledSize: new google.maps.Size(27.5, 40)
        },
        draggable: false
    });
}

// for items that existed on the map
function getMapsIconBuildingArchitect(gmap, latLong) {
    return new google.maps.Marker({
        position: latLong,
        map: gmap,
        icon: new google.maps.MarkerImage(
            IMG_BUILDING_ARCHITECT,
            null, /* size is determined at runtime */
            null, /* origin is 0,0 */
            null, /* anchor is bottom center of the scaled image */
            new google.maps.Size(54, 54)),
        draggable: false
    });
}

// for items that existed on the map
function getMapsIconVesselArchitect(gmap, latLong) {
    return new google.maps.Marker({
        position: latLong,
        map: gmap,
        icon: new google.maps.MarkerImage(
            IMG_VESSEL_ARCHITECT,
            null, /* size is determined at runtime */
            null, /* origin is 0,0 */
            null, /* anchor is bottom center of the scaled image */
            new google.maps.Size(54, 54)),
        draggable: false
    });
}

function getMapsIconFingerprint(gmaps, fingerPrintsData) {
    var size = new google.maps.Size(25, 25);
    return new google.maps.Marker({
        position: fingerPrintsData.location,
        map: gmaps,
        icon: new google.maps.MarkerImage(
            IMG_FINGERPRINT_RED_SPOT,
            null, /* size is determined at runtime */
            null, /* origin is 0,0 */
            null, /* anchor is bottom center of the scaled image */
            size
        )
    });
}

function clearFingerprintCoverage() {
    var check = 0;
    if (heatMap[check] !== undefined && heatMap[check] !== null) {

        var i = heatMap.length;
        while (i--) {
            heatMap[i].rectangle.setMap(null);
            heatMap[i] = null;
        }
        heatMap = [];
        document.getElementById("radioHeatmapRSS-mode").classList.remove('quickaction-selected');
        _HEATMAP_FINGERPRINT_COVERAGE = false;
        setColorClicked('g', false);
        setColorClicked('y', false);
        setColorClicked('o', false);
        setColorClicked('p', false);
        setColorClicked('r', false);
        $scope.radioHeatmapRSSMode = false;
        if (typeof (Storage) !== "undefined" && localStorage) {
            localStorage.setItem('radioHeatmapRSSMode', 'NO');
        }
        $scope.anyService.radioHeatmapRSSMode = false;
        $scope.radioHeatmapRSSHasGreen = false;
        $scope.radioHeatmapRSSHasYellow = false;
        $scope.radioHeatmapRSSHasOrange = false;
        $scope.radioHeatmapRSSHasPurple = false;
        $scope.radioHeatmapRSSHasRed = false;
        $cookieStore.put('RSSClicked', 'NO');

    }
}

function clearFingerprintHeatmap() {
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
        document.getElementById("fingerPrints-mode").classList.remove('quickaction-selected');
    }

    if (heatmap && heatmap.getMap()) { //hide fingerPrints heatmap
        heatmap.setMap(null);
        _FINGERPRINTS_IS_ON = false;
        document.getElementById("fingerPrints-mode").classList.remove('quickaction-selected');
        _HEATMAP_F_IS_ON = false;
        var i = heatmapFingerprints.length;
        while (i--) {
            heatmapFingerprints[i] = null;
        }
        heatmapFingerprints = [];
    }
}

function isNullOrEmpty(value){
    return (value == null || value == undefined || value == "" || value == "-");
}

function getPrettyVersion(version) {
   var s = version.version;
   if(version.variant != null && version.variant !== "") {
       s +="-"+version.variant;
   }
   return s;
}

var regex_ck_lat = /^(-?[1-8]?\d(?:\.\d{1,18})?|90(?:\.0{1,18})?)$/;
var regex_ck_lon = /^(-?(?:1[0-7]|[1-9])?\d(?:\.\d{1,18})?|180(?:\.0{1,18})?)$/;

/**
 * accepts coordinates in google maps format:
 * @param str e.g., '57.693431580156464, 11.913933480401484'
 *
 * @returns {{}|null}
 */
function get_coordinates(str){
    LOG.D4("get_coordinates: " + str)
    var spaces = str.split(" ");
    if (spaces.length === 2) {
        var lat = spaces[0].slice(0, -1); // remove last comma
        var lon = spaces[1]
        LOG.D3("lat: " + lat)
        LOG.D3("lon: " + lon)
        if (check_lat_lon(lat, lon)) {
            return {
                lat: parseFloat(spaces[0]),
                lng: parseFloat(spaces[1])
            }
        }
    }
    return null;
}

function check_lat_lon(lat, lon){
    var validLat = regex_ck_lat.test(lat);
    var validLon = regex_ck_lon.test(lon);
    if(validLat && validLon) {
        return true;
    } else {
        return false;
    }
}


// Haversine formula: https://www.geodatasource.com/developers/javascript
/**
 * //:::  This routine calculates the distance between two points (given the     :::
 //:::  latitude/longitude of those points). It is being used to calculate     :::
 //:::  the distance between two locations using GeoDataSource (TM) prodducts  :::
 //:::                                                                         :::
 //:::  Definitions:                                                           :::
 //:::    South latitudes are negative, east longitudes are positive           :::
 //:::                                                                         :::
 //:::  Passed to function:                                                    :::
 //:::    lat1, lon1 = Latitude and Longitude of point 1 (in decimal degrees)  :::
 //:::    lat2, lon2 = Latitude and Longitude of point 2 (in decimal degrees)  :::
 * @param lat1
 * @param lon1
 * @param lat2
 * @param lon2
 * @returns {number}
 */
function calculate_distance(lat1, lon1, lat2, lon2) {
    if ((lat1 == lat2) && (lon1 == lon2)) { return 0; }
    else {
        var radlat1 = Math.PI * lat1/180;
        var radlat2 = Math.PI * lat2/180;
        var theta = lon1-lon2;
        var radtheta = Math.PI * theta/180;
        var dist = Math.sin(radlat1) * Math.sin(radlat2) + Math.cos(radlat1) * Math.cos(radlat2) * Math.cos(radtheta);
        if (dist > 1) {
            dist = 1;
        }
        dist = Math.acos(dist);
        dist = dist * 180/Math.PI;
        dist = dist * 60 * 1.1515;

        // convert to kilometers (from miles)
        dist = dist * 1.609344
        // convert to meters
        dist = dist*1000;
        return dist;
    }
}

function round(num, decimal_points) {
    if (decimal_points === 0) return num;
    var tmp = Math.pow(10, decimal_points);
    return Math.round(num*tmp)/tmp;
}

var LOG = {};
LOG.level = 2;
LOG.DBG1 = function() { return 1 <= LOG.level; }
LOG.DBG2 = function() { return 2 <= LOG.level; }
LOG.DBG3 = function() { return 3 <= LOG.level; }
LOG.DBG4 = function() { return 4 <= LOG.level; }
LOG.DBG5 = function() { return 5 <= LOG.level; }

LOG.W = function(msg) { console.log("WARN: " + msg);}
LOG.E = function(msg) { console.log("ERR: " + msg);}
LOG.D = function(msg) { console.log(msg);}
LOG.F = function(msg) { alert(msg); window.stop(); }
LOG.D1 = function(msg) { if (LOG.DBG1()) console.log(msg);}
LOG.D2 = function(msg) { if (LOG.DBG2()) console.log(msg);}
LOG.D3 = function(msg) { if (LOG.DBG3()) console.log(msg);}
LOG.D4 = function(msg) { if (LOG.DBG4()) console.log(msg);}
LOG.D5 = function(msg) { if (LOG.DBG5()) console.log(msg);}

