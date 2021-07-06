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
app.controller('ControlBarController', ['$scope', '$rootScope', 'AnyplaceService', 'GMapService', 'AnyplaceAPIService', function ($scope, $rootScope, AnyplaceService, GMapService, AnyplaceAPIService) {

    $scope.anyService = AnyplaceService;
    $scope.gmapService = GMapService;

    $scope.isAuthenticated = false;

    $scope.signInType = "google";

    $scope.gAuth = {};
    $scope.person = undefined;

    $scope.creds = {
        fullName: undefined,
        username: undefined,
        password: undefined
    };

    $scope.user = {
        name: undefined,
        email: undefined,
        username: undefined,
        password: undefined
    }

    $scope.owner_id = undefined;
    $scope.displayName = undefined;
    $scope.userType = undefined;

    var self = this; //to be able to reference to it in a callback, you could use $scope instead


    $scope.setAuthenticated = function (bool) {
        $scope.isAuthenticated = bool;
    };

    $scope.showFullControls = true;

    $scope.toggleFullControls = function () {
        $scope.showFullControls = !$scope.showFullControls;
    };

    // not called
    var apiClientLoaded = function () {
        gapi.client.plus.people.get({userId: 'me'}).execute(handleEmailResponse);
    };


    var handleEmailResponse = function (resp) {
        console.log("handleEmailResponse ?");
        $scope.personLookUp(resp);
    };

    $scope.showGoogleID = function () {
        if (!$scope.person) {
            return;
        }
        AnyplaceService.addAlert('success', 'Your Google ID is: ' + $scope.person.id);
    };

    $scope.showGoogleAuth = function () {
        if (!$scope.gAuth) {
            return;
        }
        AnyplaceService.addAlert('success', 'access_token: ' + $scope.gAuth.access_token);
    };

    $scope.onSignIn = function (googleUser) {

        if ($scope.getCookie("username") === "") {
            $scope.setCookie("username", "true", 365);
            location.reload();
        }

        //location.reload();
        $scope.setAuthenticated(true);

        $scope.gAuth = gapi.auth2.getAuthInstance().currentUser.get().getAuthResponse();

        $scope.gAuth.access_token = $scope.gAuth.id_token;

        app.access_token = $scope.gAuth.id_token;

        $scope.personLookUp(googleUser);
    };


    $scope.onSignInFailure = function () {
        console.log('Sign-in state: Error');
    };

    window.onSignIn = $scope.onSignIn;
    window.onSignInFailure = $scope.onSignInFailure;

    $scope.personLookUp = function (resp) {
        // BUG: resp.getBasicProfile is not a function
        $scope.person = resp.getBasicProfile(); // BUG
        $scope.person.image = $scope.person.getImageUrl(); // BUG
        $scope.person.id = $scope.person.getId(); // BUG
        $scope.person.displayName = $scope.person.getName();
        // compose user id
        $scope.owner_id = $scope.person.id + '_' + $scope.signInType;
        $scope.displayName = $scope.person.displayName;

        if ($scope.person && $scope.person.id) {
            $scope.$broadcast('loggedIn', []);
        }

        var promise = AnyplaceAPIService.signGoogleAccount({
            name: $scope.person.displayName,
            external: "google"
        });

        promise.then(
            function (resp) {
                // console.log(resp)
                $scope.userType = resp.data.type;
            },
            function (resp) {
                console.log("error: personLookUp")
                console.log(resp)
            }
        );
    };

    $scope.signOut = function () {
        $scope.setCookie("username", "", 365);
        var auth2 = gapi.auth2.getAuthInstance();
        auth2.signOut().then(function () {
            console.log('User signed out.');
        });
        $scope.isAuthenticated = false;

        $scope.$broadcast('loggedOff', []);
        $scope.gAuth = {};
        $scope.owner_id = undefined;
        $scope.person = undefined;

        clearFingerprintCoverage();
        clearFingerprintHeatmap();
    };

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

    $scope.getCookie = function (cname) {
        var name = cname + "=";
        var ca = document.cookie.split(';');
        for (var i = 0; i < ca.length; i++) {
            var c = ca[i];
            while (c.charAt(0) == ' ') {
                c = c.substring(1);
            }
            if (c.indexOf(name) == 0) {
                return c.substring(name.length, c.length);
            }
        }
        return "";
    };

    $scope.setCookie = function (cname, cvalue, exdays) {
        var d = new Date();
        d.setTime(d.getTime() + (exdays * 24 * 60 * 60 * 1000));
        var expires = "expires=" + d.toUTCString();
        document.cookie = cname + "=" + cvalue + "; " + expires;
    };

    $scope.tab = 1;

    $scope.setTab = function (num) {
        $scope.tab = num;
    };

    $scope.isTabSet = function (num) {
        return $scope.tab === num;
    };

    $scope.isUserAdmin = function () {
        if ($scope.userType == null) {
            return false;
        } else if ($scope.userType == undefined) {
            return false;
        } else if ($scope.userType == "admin") {
            return true;
        }
    };
    // CLR:NN REVIEW:PM
    // $scope.tab = 1;
    //
    // $scope.setTab = function (num) {
    //     $scope.tab = num;
    // };
    //
    // $scope.isTabSet = function (num) {
    //     return $scope.tab === num;
    // };

    var _err = function (msg) {
        $scope.anyService.addAlert('danger', msg);
    };

    var myLocMarker = undefined;

    $scope.userPosition = undefined;

    var pannedToUserPosOnce = false;

    $scope.isUserLocVisible = false;

    $scope.getIsUserLocVisible = function () {
        return $scope.isUserLocVisible;
    };

    $scope.panToUserLocation = function () {
        if (!$scope.userPosition)
            return;

        GMapService.gmap.panTo($scope.userPosition);
        GMapService.gmap.setZoom(20);
    };

    $scope.displayMyLocMarker = function (posLatlng) {
        if (myLocMarker && myLocMarker.getMap()) {
            myLocMarker.setPosition(posLatlng);
            return;
        }
    };

    $scope.displayMyLocMarker = function (posLatlng) {
        if (myLocMarker && myLocMarker.getMap()) {
            myLocMarker.setPosition(posLatlng);
            return;
        }
        var s = new google.maps.Size(20, 20);
        if ($scope.isFirefox)
            s = new google.maps.Size(48, 48);

        myLocMarker = new google.maps.Marker({
            position: posLatlng,
            map: GMapService.gmap,
            draggable: false,
            icon: {
                url: 'build/images/location-radius-centre.png',
                origin: new google.maps.Point(0, 0), /* origin is 0,0 */
                anchor: new google.maps.Point(10, 10), /* anchor is bottom center of the scaled image */
                size: s,
                scaledSize: new google.maps.Size(20, 20)
            }
        });
    };

    $scope.hideUserLocation = function () {
        if (myLocMarker)
            myLocMarker.setMap(null);
        //if (accuracyRadius)
        //    accuracyRadius.setMap(null);

        $scope.isUserLocVisible = false;
    };


    $scope.showUserLocation = function () {

        if ($scope.getIsUserLocVisible()) {
            $scope.hideUserLocation();

            if (navigator.geolocation)
                navigator.geolocation.clearWatch(watchPosNum);

            return;
        }

        if (navigator.geolocation) {
            watchPosNum = navigator.geolocation.watchPosition(
                function (position) {

                    var posLatlng = {lat: position.coords.latitude, lng: position.coords.longitude};
                    //var radius = position.coords.accuracy;

                    $scope.userPosition = posLatlng;

                    $scope.displayMyLocMarker(posLatlng);

                    var infowindow = new google.maps.InfoWindow({
                        content: 'Your current location.',
                        maxWidth: 500
                    });

                    google.maps.event.addListener(myLocMarker, 'click', function () {
                        var self = this;
                        $scope.$apply(function () {
                            infowindow.open(GMapService.gmap, self);
                        })
                    });

                    if (!$scope.isUserLocVisible) {
                        $scope.$apply(function () {
                            $scope.isUserLocVisible = true;
                        });
                    }

                    if (!pannedToUserPosOnce) {
                        GMapService.gmap.panTo(posLatlng);
                        GMapService.gmap.setZoom(19);
                        pannedToUserPosOnce = true;
                    }
                },
                function (err) {
                    $scope.$apply(function () {
                      HandleGeolocationError(err.code);
                    });
                });
        } else {
            _err($scope, ERR_GEOLOC_NOT_SUPPORTED);
        }
    };

    $scope.copyApiKey = function () {
        var copyTextarea = document.querySelector('#auth-api-key');
        copyTextarea.focus();
        copyTextarea.select();
        document.execCommand("copy");
        _info($scope, "API key copied");
    }

    $scope.centerViewToSelectedItem = function () {
        if ($scope.anyService.selectedBuilding == null || $scope.anyService.selectedBuilding == undefined) {
            _err($scope, "You have to select a building first");
            return;
        }
        var position = {};
        if ($scope.anyService.selectedPoi) {
            var p = $scope.anyService.selectedPoi;
            position = {lat: parseFloat(p.coordinates_lat), lng: parseFloat(p.coordinates_lon)};
        } else if ($scope.anyService.selectedBuilding) {
            var b = $scope.anyService.selectedBuilding;
            position = {lat: parseFloat(b.coordinates_lat), lng: parseFloat(b.coordinates_lon)};
        } else {
            _err($scope, "No building is selected.");
            return;
        }

        $scope.gmapService.gmap.panTo(position);
        $scope.gmapService.gmap.setZoom(20);
    }

}]);
