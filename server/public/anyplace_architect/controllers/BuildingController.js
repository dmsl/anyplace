/**
 *
 The MIT License (MIT)

 Copyright (c) 2015, Kyriakos Georgiou, Marileni Angelidou,Loukas Solea, Data Management Systems Laboratory (DMSL)
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



app.controller('BuildingController', ['$cookieStore', '$scope', '$compile', 'GMapService', 'AnyplaceService', 'AnyplaceAPIService', function ($cookieStore, $scope, $compile, GMapService, AnyplaceService, AnyplaceAPIService) {

    $scope.myMarkers = {};
    $scope.myMarkerId = 0;

    $scope.gmapService = GMapService;
    $scope.anyService = AnyplaceService;
    $scope.anyAPI = AnyplaceAPIService;
    $scope.example9model = [];
    $scope.example9data = [];
    $scope.example9settings = {enableSearch: true, scrollable: true};

    $scope.example9modeledit = [];
    $scope.example9dataedit = [];
    $scope.example9settingsedit = {enableSearch: true, scrollable: true};

    $scope.myBuildings = [];

    $scope.myBuildingsHashT = {};
    $scope.myCampus = [];
    $scope.old_campus = [];

    $scope.crudTabSelected = 1;

    $scope.fileToUpload = "";
    $scope.logfile = "";

    $scope.poisTypes = {};
    $scope.catTypes = {};

    $scope.pageLoad = false;

    $scope.crudTabSelected = 1;


    $scope.fetchVersion = function () {
        var jsonReq = {};
        var promise = $scope.anyAPI.version(jsonReq);
        promise.then(
            function (resp) { // on success
                var data = resp.data;
                // console.log("VERSION:: " + data);
                var element = document.getElementById("anyplace-version");
                element.textContent = "v"+data;
            },
            function (resp) { console.log("Failed to get version: " + resp.data); }
        );
    };
    $scope.fetchVersion();


//     // Replace this with your URL.
//     var BUILDING_TILE_URL = AnyplaceAPI.FULL_SERVER + '/floortiles/{building}/{floor}/{z}/z{z}x{x}y{y}.png';
//
// // Name the layer anything you like.
//     var layerID = 'building_layer';
//
// // Create a new ImageMapType layer. static_tiles/19/z19x310801y207411.png
//     var maptiler = new google.maps.ImageMapType({
//         name: layerID,
//         getTileUrl: function (coord, zoom) {
//             var buid = $scope.anyService.getBuildingId();
//
//             var floor = $scope.anyService.getFloorNumber();
//             var url = BUILDING_TILE_URL
//                 .replace('{building}', buid)
//                 .replace('{floor}', floor)
//                 .replace('{z}', zoom)
//                 .replace('{x}', coord.x)
//                 .replace('{y}', coord.y)
//                 .replace('{z}', zoom);
//             return url;
//         },
//         tileSize: new google.maps.Size(256, 256),
//         isPng: true
//     });
//
//     // Register the new layer, then activate it.
//     $scope.gmapService.gmap.overlayMapTypes.insertAt(0, maptiler);

    $scope.setCrudTabSelected = function (n) {
        $scope.crudTabSelected = n;
        if (!$scope.anyService.getBuilding()) {
            _err($scope, "No building selected.");
            return;
        }

        var b = $scope.myBuildingsHashT[$scope.anyService.getBuildingId()];
        if (!b) {
            return;
        }

        var m = b.marker;
        if (!m) {
            return;
        }
        // edit building
        if (n === 2) {
            m.setDraggable(true);
        } else {
            m.setDraggable(false);
        }

    };
    $scope.isCrudTabSelected = function (n) {
        return $scope.crudTabSelected === n;
    };

    $scope.$on("loggedIn", function (event, mass) {
        //_suc('Successfully logged in.');
        $scope.fetchAllBuildings();
        $scope.fetchAllCampus();
    });

    $scope.$on("loggedOff", function (event, mass) {
        _clearBuildingMarkersAndModels();
        $scope.myMarkers = {};
        $scope.myMarkerId = 0;
        $scope.myBuildings = [];
        $scope.myBuildingsHashT = {};
    });

    /**
     * @return {string}
     */
    function S4() {
        return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
    }

    var guid = (S4() + S4() + "-" + S4() + "-4" + S4().substr(0, 3) + "-" + S4() + "-" + S4() + S4() + S4()).toLowerCase();
    var d = new Date();
    document.getElementById("CampusID").value = "cuid_" + guid + "_" + d.getTime();

    var logoPlanInputElement = $('#input-logo');

    logoPlanInputElement.change(function handleImage(e) {
        var reader = new FileReader();
        reader.onload = function (event) {
            var imgObj = new Image();
            imgObj.src = event.target.result;
        };
        reader.readAsDataURL(e.target.files[0]);
    });

    $scope.setLogoPlan = function (cuid) {

        var newFl = {
            is_published: 'true',
            cuid: cuid,
            logo: String($scope.newFloorNumber),
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
        $scope.data.floor_plan_coords.zoom = GMapService.gmap.getZoom() + "";
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

    $scope.$watch('anyService.selectedBuilding', function (newVal, oldVal) {
        if (newVal && newVal.coordinates_lat && newVal.coordinates_lon) {
            // Pan map to selected building
            $scope.gmapService.gmap.panTo(_latLngFromBuilding(newVal));
            $scope.gmapService.gmap.setZoom(19);
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem("lastBuilding", newVal.buid);
            }
        }
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
    });

    $scope.$watch('anyService.selectedCampus', function (newVal, oldVal) {
        if (newVal && newVal.cuid) {
            // Pan map to selected building
            if (typeof(Storage) !== "undefined" && localStorage) {
                localStorage.setItem("lastCampus", newVal.cuid);
            }
        }

    });


    var _clearBuildingMarkersAndModels = function () {
        for (var b in $scope.myBuildingsHashT) {
            if ($scope.myBuildingsHashT.hasOwnProperty(b)) {
                $scope.myBuildingsHashT[b].marker.setMap(null);
                delete $scope.myBuildingsHashT[b];
            }
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

    $scope.fetchAllBuildings = function () {
        var jsonReq = {};
        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;
        jsonReq.owner_id = $scope.owner_id;

        if (!jsonReq.owner_id) {
            _err($scope, ERR_USER_AUTH);
            return;
        }

        var promise = $scope.anyAPI.allBuildings(jsonReq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;
                //var bs = JSON.parse( data.buildings );
                $scope.myBuildings = data.buildings;

                var infowindow = new google.maps.InfoWindow({
                    content: '-',
                    maxWidth: 500
                });

                var localStoredBuildingIndex = -1;
                var localStoredBuildingId = undefined;
                if (typeof(Storage) !== "undefined" && localStorage && localStorage.getItem('lastBuilding')) {
                    localStoredBuildingId = localStorage.getItem('lastBuilding');
                }

                for (var i = 0; i < $scope.myBuildings.length; i++) {

                    var b = $scope.myBuildings[i];

                    $scope.example9data[i] = {id: b.buid, label: b.name};
                    $scope.example9dataedit[i] = {id: b.buid, label: b.name};

                    if (localStoredBuildingId && localStoredBuildingId === b.buid) {
                        localStoredBuildingIndex = i;
                    }

                    if (b.is_published === 'true' || b.is_published == true) {
                        b.is_published = true;
                    } else {
                        b.is_published = false;
                    }

                    var marker = getMapsIconBuildingArchitect(GMapService.gmap, _latLngFromBuilding(b))


                    var htmlContent = '<div class="infowindow-scroll-fix">'
                        + '<h5>Building:</h5>'
                        + '<span>' + b.name + '</span>'
                        + '<h5>Description:</h5>'
                        + '<textarea class="infowindow-text-area"  rows="3" readonly>' + b.description + '</textarea>'
                        + '</div>';

                    marker.infoContent = htmlContent;
                    marker.building = b;

                    $scope.myBuildingsHashT[b.buid] = {
                        marker: marker,
                        model: b
                    };

                    google.maps.event.addListener(marker, 'click', function () {
                        infowindow.setContent(this.infoContent);
                        infowindow.open(GMapService.gmap, this);
                        var self = this;
                        $scope.$apply(function () {
                            $scope.anyService.selectedBuilding = self.building;
                        });
                    });
                }
                console.log("Loaded " + $scope.myBuildings.length + " buildings!")

                // using the latest building form localStorage
                if (localStoredBuildingIndex >= 0) {
                    $scope.anyService.selectedBuilding = $scope.myBuildings[localStoredBuildingIndex];
                } else if ($scope.myBuildings[0]) {
                    $scope.anyService.selectedBuilding = $scope.myBuildings[0];
                }

                // _suc('Successfully fetched buildings.');
            },
          function (resp) {
            ShowError($scope, resp, ERR_FETCH_BUILDINGS);
          }
        );
    };

    $scope.addNewBuilding = function (id) {

        if ($scope.myMarkers[id] && $scope.myMarkers[id].marker) {

            var building = $scope.myMarkers[id].model;

            // set owner id
            building.owner_id = $scope.owner_id;

            if (!building.owner_id) {
                _err($scope, ERR_USER_AUTH);
                return;
            }


            building.coordinates_lat = String($scope.myMarkers[id].marker.position.lat());
            building.coordinates_lon = String($scope.myMarkers[id].marker.position.lng());

            if (building.coordinates_lat === undefined || building.coordinates_lat === null) {
                _err($scope, "Invalid building latitude.");
                return;
            }

            if (building.coordinates_lon === undefined || building.coordinates_lon === null) {
                _err($scope, "Invalid building longitude.");
                return;
            }

            if (building.is_published === true) {
                building.is_published = "true";
            } else {
                building.is_published = "false";
            }

            if (!building.description) {
                building.description = "-";
            }

            if (building.owner_id && building.name && building.description && building.is_published && building.url && building.address) {

                var promise = $scope.anyAPI.addBuilding(building);

                promise.then(
                    function (resp) {
                        // on success
                        var data = resp.data;
                        console.log("new buid: " + data.buid);
                        building.buid = data.buid;

                        if (building.is_published === 'true' || building.is_published == true) {
                            building.is_published = true;
                        } else {
                            building.is_published = false;
                        }

                        // insert the newly created building inside the loadedBuildings
                        $scope.myBuildings.push(building);

                        $scope.anyService.selectedBuilding = $scope.myBuildings[$scope.myBuildings.length - 1];

                        $scope.myMarkers[id].marker.setDraggable(false);

                        $scope.myBuildingsHashT[building.buid] = {
                            marker: $scope.myMarkers[id].marker,
                            model: building
                        };

                        if ($scope.myMarkers[id].infowindow) {
                            $scope.myMarkers[id].infowindow.setContent($scope.myMarkers[id].marker.tpl2[0]);
                            $scope.myMarkers[id].infowindow.close();
                        }

                        _suc($scope, "Building added successfully.");

                    },
                    function (resp) {
                      ShowError($scope, resp, "Something went wrong while adding the building.", true);
                    }
                );
            } else {
                _err($scope, "Some required fields are missing.");
            }
        }
    };

    $scope.deleteBuilding = function () {

        var b = $scope.anyService.getBuilding();

        var reqObj = $scope.creds;

        if (!$scope.owner_id) {
            _err($scope, "Could not identify user. Please refresh and sign in again.");
            return;
        }

        reqObj.owner_id = $scope.owner_id;

        if (!b || !b.buid) {
            _err($scope, "No building selected for deletion.");
            return;
        }

        reqObj.buid = b.buid;

        var promise = $scope.anyAPI.deleteBuilding(reqObj);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                console.log("building deleted: ", b);

                // delete the building from the loadedBuildings
                $scope.myBuildingsHashT[b.buid].marker.setMap(null);
                delete $scope.myBuildingsHashT[b.buid];

                var bs = $scope.myBuildings;
                var sz = bs.length;
                for (var i = 0; i < sz; i++) {
                    if (bs[i].buid == b.buid) {
                        bs.splice(i, 1);
                        break;
                    }
                }

                // update the selected building
                if ($scope.myBuildings && $scope.myBuildings.length > 0) {
                    $scope.anyService.selectedBuilding = $scope.myBuildings[0];
                }

                $scope.setCrudTabSelected(1);

                _suc($scope, "Successfully deleted building.");
            },
            function (resp) {
              ShowError($scope, resp,
                "Something went wrong." +"" +
                "It's likely that everything related to the building is deleted " +
                "but please refresh to make sure or try again.", true)
            }
        );

    };


    // REVIEWLS what does this actually delete?
    $scope.deleteRadiomaps = function () {

        var jsonReq = {"buid": $scope.anyService.getBuildingId(), "floor": $scope.anyService.getFloorNumber()};
        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;
        var promise = $scope.anyAPI.deleteRadiomaps(jsonReq);

        promise.then(
            function (resp) {
                _suc($scope, "Deleted radiomap floor")
            },

            function (resp) {
              ShowError($scope, resp, "deleteRadiomaps: file does not exist.", true);
            }
        );
    };

    $scope.updateBuilding = function () {
        var b = $scope.anyService.getBuilding();

        if (LPUtils.isNullOrUndefined(b) || LPUtils.isNullOrUndefined(b.buid)) {
            _err($scope, "No selected building found.");
            return;
        }

        var reqObj = {};

        // from controlBarController
        reqObj = $scope.creds;
        if (!$scope.owner_id) {
            _err($scope, ERR_USER_AUTH);
            return;
        }

        reqObj.owner_id = $scope.owner_id;

        reqObj.buid = b.buid;

        reqObj.description = b.description;
        if (isNullOrEmpty(b.description)) {
            reqObj.description = "";
        }

        if (b.name) {
            reqObj.name = b.name;
        }

        if (b.is_published === true || b.is_published == "true") {
            reqObj.is_published = "true";
        } else {
            reqObj.is_published = "false";
        }


        reqObj.bucode = b.bucode;
        if (isNullOrEmpty(b.bucode)) {
            reqObj.bucode = "";
        }

        var marker = $scope.myBuildingsHashT[b.buid].marker;
        if (marker) {
            var latLng = marker.position;
            if (latLng && latLng.lat() && latLng.lng()) {
                reqObj.coordinates_lat = String(latLng.lat());
                reqObj.coordinates_lon = String(latLng.lng());
            }
        }

        var promise = $scope.anyAPI.updateBuilding(reqObj);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                if (b.is_published === 'true' || b.is_published == true) {
                    b.is_published = true;
                } else {
                    b.is_published = false;
                }

                _suc($scope, "Successfully updated building.")
            },
            function (resp) {
              ShowError($scope, resp, "Something went wrong while updating building.", true);
            }
        );

    };

    $scope.fetchAllCampus = function () {
        var jsonReq = {};
        jsonReq.username = $scope.creds.username;
        jsonReq.password = $scope.creds.password;
        jsonReq.owner_id = $scope.owner_id;
        $scope.myCampus = [];
        if (!jsonReq.owner_id) {
            _err($scope, ERR_USER_AUTH);
            return;
        }

        var promise = $scope.anyAPI.allCampus(jsonReq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;
                //var bs = JSON.parse( data.buildings );
                $scope.myCampus = data.buildingsets;
                var localStoredCampusIndex = -1;
                var localStoredCampusId = undefined;
                if (typeof(Storage) !== "undefined" && localStorage && localStorage.getItem('lastCampus')) {
                    localStoredCampusId = localStorage.getItem('lastCampus');
                }

                for (var i = 0; i < $scope.myCampus.length; i++) {

                    var b = $scope.myCampus[i];

                    if (localStoredCampusId && localStoredCampusId === b.cuid) {
                        localStoredCampusIndex = i;
                    }
                }

                // using the latest building set form localStorage
                if (localStoredCampusIndex >= 0) {
                    $scope.anyService.selectedCampus = $scope.myCampus[localStoredCampusIndex];
                } else if ($scope.myCampus[0]) {
                    $scope.anyService.selectedCampus = $scope.myCampus[0];
                }
            },
            function (resp) {
              ShowError($scope, resp, ERR_FETCH_BUILDINGS);
            }
        );
    };

    $scope.updateCampus = function () {
        var b = $scope.anyService.getCampus();

        if (LPUtils.isNullOrUndefined(b) || LPUtils.isNullOrUndefined(b.cuid)) {
            _err($scope, "No selected campus found.");
            return;
        }

        var reqObj = {};

        // from controlBarController
        reqObj = $scope.creds;
        if (!$scope.owner_id) {
            _err($scope, ERR_USER_AUTH);
            return;
        }

        reqObj.owner_id = $scope.owner_id;

        reqObj.cuid = b.cuid;

        reqObj.description = b.description;
        if (isNullOrEmpty(b.description)) {
            reqObj.description = "";
        }

        reqObj.name = b.name;
        if (isNullOrEmpty(b.name)) {
            reqObj.name = "";
        }

        if (b.newcuid) {
            reqObj.newcuid = b.newcuid;
        }

        var sz = $scope.example9modeledit.length;

        if (sz == 0) {
            _err($scope, "No buildings selected.");
            return;
        }
        var buids = "[";
        for (var i = sz - 1; i > 0; i--) {
            buids = buids + "\"" + $scope.example9modeledit[i].id + "\",";
        }
        buids = buids + "\"" + $scope.example9modeledit[0].id + "\"]";

        reqObj.greeklish = document.getElementById("Greeklish-OnOffedit").checked;

        reqObj.buids = buids;


        var promise = $scope.anyAPI.updateCampus(reqObj);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;
                document.getElementById("CampusIDeditnew").value = "";
                _suc($scope, "Successfully updated campus.")
            },
            function (resp) {
              ShowError($scope, resp, "Something went wrong while updating campus.", true);
            }
        );

    };

    $scope.deleteCampus = function () {

        var b = $scope.anyService.getCampus();

        var reqObj = $scope.creds;

        if (!$scope.owner_id) {
            _err($scope, ERR_USER_AUTH);
            return;
        }

        reqObj.owner_id = $scope.owner_id;

        if (!b || !b.cuid) {
            _err($scope, "No Campus selected for deletion.");
            return;
        }

        reqObj.cuid = b.cuid;

        var promise = $scope.anyAPI.deleteCampus(reqObj);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;

                console.log("campus deleted: ", b);


                var bs = $scope.myCampus;
                var sz = bs.length;
                for (var i = 0; i < sz; i++) {
                    if (bs[i].cuid == b.cuid) {
                        bs.splice(i, 1);
                        break;
                    }
                }

                // update the selected building
                if ($scope.myCampus && $scope.myCampus.length > 0) {
                    $scope.anyService.selectedCampus = $scope.myCampus[0];
                }
                else if ($scope.myCampus.length == 0) {
                    $scope.anyService.selectedCampus = undefined;
                }

                $scope.setCrudTabSelected(1);

                _suc($scope, "Successfully deleted campus.");
            },
            function (resp) {
              ShowError($scope, resp,
                "Something went wrong. It's likely that everything related to " +
                "the campus is deleted but please refresh to make sure or try again.", true)
            }
        );

    };

    $scope.addCampus = function () {

        var name_element = document.getElementById("CampusName");
        var name = "\"name\":\"" + name_element.value + "\"";

        if (document.getElementById("CampusDescription").value.localeCompare("") == 0) {
            document.getElementById("CampusDescription").value = "-";
        }

        var des = document.getElementById("CampusDescription");
        var des = "\"description\":\"" + des.value + "\"";

        var mycuid = document.getElementById("CampusID");
        var mycuid = "\"cuid\":\"" + mycuid.value + "\"";

        var greeklish = document.getElementById("Greeklish-OnOff").checked;
        greeklish = "\"greeklish\":\"" + greeklish + "\"";
        var sz = $scope.example9model.length;

        if (sz == 0) {
            _err($scope, "No buildings selected.");
            return;
        }
        var buids = "\"buids\":[";
        for (var i = sz - 1; i > 0; i--) {
            buids = buids + "\"" + $scope.example9model[i].id + "\",";
        }
        buids = buids + "\"" + $scope.example9model[0].id + "\"]";

        var jreq = "{" + greeklish + "," + buids + "," + mycuid + "," + des + "," + name + ",\"owner_id\":\"" + $scope.owner_id + "\",\"access_token\":\"" + $scope.gAuth.access_token + "\"}";
        //alert(document.getElementById("Greeklish-OnOff").checked);
        var promise = $scope.anyAPI.addBuildingSet(jreq);
        promise.then(
            function (resp) {
                // on success
                var data = resp.data;
                var new_campus = {};
                new_campus.name = document.getElementById("CampusName").value;
                new_campus.buids = buids.buids;
                new_campus.description = document.getElementById("CampusDescription").value;
                new_campus.cuid = data.cuid;
                $scope.myCampus.push(new_campus);
                $scope.anyService.selectedCampus = $scope.myCampus[$scope.myCampus.length - 1];
                _suc($scope, "Successfully added campus.");

                function S4() {
                    return (((1 + Math.random()) * 0x10000) | 0).toString(16).substring(1);
                }

                var guid = (S4() + S4() + "-" + S4() + "-4" + S4().substr(0, 3) + "-" + S4() + "-" + S4() + S4() + S4()).toLowerCase();
                var d = new Date();
                document.getElementById("CampusID").value = "cuid_" + guid + "_" + d.getTime();
            },
            function (resp) {
              ShowError($scope, resp, "Something went wrong while adding the building.", true);
            }
        );

    };

    var overlay = new google.maps.OverlayView();
    overlay.draw = function () {
    };
    overlay.setMap(GMapService.gmap);

    $("#draggable-building").draggable({
        helper: 'clone',
        stop: function (e) {
            var point = new google.maps.Point(e.pageX, e.pageY);
            var ll = overlay.getProjection().fromContainerPixelToLatLng(point);
            $scope.placeMarker(ll);
        }
    });

    $scope.placeMarker = function (location) {

        var prevMarker = $scope.myMarkers[$scope.myMarkerId - 1];

        if (prevMarker && prevMarker.marker && prevMarker.marker.getMap() && prevMarker.marker.getDraggable()) {
            // TODO: alert for already pending building.
            console.log('there is a building pending, please add 1 at a time');
            return;
        }

        var marker = new google.maps.Marker({
            position: location,
            map: GMapService.gmap,
            icon: IMG_BUILDING_ARCHITECT,
            draggable: true
        });

        var infowindow = new google.maps.InfoWindow({
            content: '-',
            maxWidth: 500
        });

        $scope.$apply(function () {
            marker.myId = $scope.myMarkerId;
            $scope.myMarkers[marker.myId] = {};
            $scope.myMarkers[marker.myId].model = {
                description: "",
                name: undefined,
                is_published: true,
                address: "-",
                url: "-",
                bucode: ""
            };
            $scope.myMarkers[marker.myId].marker = marker;
            $scope.myMarkers[marker.myId].infowindow = infowindow;
            $scope.myMarkerId++;
        });

        var htmlContent = '<form name="buildingForm" class="infowindow-scroll-fix">'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.bucode" id="building-code" type="text" class="form-control" placeholder="Building Code (Optional)"/>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.name" id="building-name" type="text" class="form-control" placeholder="Building Name *"/>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<textarea ng-model="myMarkers[' + marker.myId + '].model.description" id="building-description" type="text" class="form-control" placeholder="Building Description (Optional)"></textarea>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '<input ng-model="myMarkers[' + marker.myId + '].model.is_published" id="building-published" type="checkbox"><span> Make building public to view.</span>'
            + '</fieldset>'
            + '<fieldset class="form-group">'
            + '</fieldset class="form-group">'
            + '<div style="text-align: center;">'
            + '<fieldset class="form-group" style="display: inline-block; width: 75%;">'
            + '<button type="submit" class="btn btn-success add-any-button" ng-click="addNewBuilding(' + marker.myId + ')">'
            + '<span class="glyphicon glyphicon-plus"></span> Add'
            + '</button>'
            + '</fieldset>'
            + '<fieldset class="form-group" style="display: inline-block;width: 23%;">'
            + '<button class="btn btn-danger add-any-button" style="margin-left:2px" ng-click="deleteTempBuilding(' + marker.myId + ')"><span class="glyphicon glyphicon-remove"></span>'
            + '</button>'
            + '</fieldset>'
            + '</div>'
            + '</form>';

        var htmlContent2 = '<div class="infowindow-scroll-fix">'
            + '<h5 style="margin: 0">Building:</h5>'
            + '<span>{{myMarkers[' + marker.myId + '].model.name}}</span>'
            + '<h5 style="margin: 8px 0 0 0">Description:</h5>'
            + '<span>{{myMarkers[' + marker.myId + '].model.description}}</span>'
            + '</div>';

        var tpl = $compile(htmlContent)($scope);
        marker.tpl2 = $compile(htmlContent2)($scope);

        infowindow.setContent(tpl[0]);
        infowindow.open(GMapService.gmap, marker);

        google.maps.event.addListener(marker, 'click', function () {
            if (!infowindow.getMap()) {
                infowindow.open(GMapService.gmap, marker);
            }
        });
    };

    $scope.deleteTempBuilding = function (id) {
        var myMarker = $scope.myMarkers[id];
        if (myMarker && myMarker.marker) {
            myMarker.marker.setMap(null);
        }
    };

    /**
     * building {
     *  name:
     *  description:
     *  coordinates_lat:
     *  coordinates_lon:
     *  floors: [
     *      0: {
     *          pois: [
     *              name:
     *              description:
     *              pois_type:
     *              is_building_entrance:
     *              coordinates_lat:
     *              coordinates_lon:
     *          ]
     *      }
     *  ];
     * }
     */
    $scope.exportBuildingToJson = function () {
        var result = {
            building: {
                floors: []
            }
        };

        var building = $scope.anyService.selectedBuilding;
        if (LPUtils.isNullOrUndefined(building)) {
            _err($scope, 'No building selected');
            return;
        }
        result.building.buid = building.buid;
        result.building.name = building.name;
        result.building.description = building.description;
        result.building.coordinates_lat = building.coordinates_lat;
        result.building.coordinates_lon = building.coordinates_lon;

        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = building.buid;

        var count = 0;

        var promise = AnyplaceAPIService.allBuildingFloors(jsonReq);
        promise.then(
            function (resp) {
                var floors = resp.data.floors;

                var resFloors = [];

                if (floors) {
                    for (var i = 0; i < floors.length; i++) {

                        (function (jreq) {
                            var promise = AnyplaceAPIService.retrievePoisByBuildingFloor(jreq);
                            promise.then(
                                function (resp) {
                                    var data = resp.data;

                                    var poisArray = data.pois;

                                    if (poisArray) {

                                        var flPois = [];

                                        var fNo = poisArray[0].floor_number;

                                        for (var j = 0; j < poisArray.length; j++) {
                                            var sPoi = poisArray[j];

                                            if (sPoi.pois_type == "None") {
                                                continue;
                                            }
                                            if (sPoi.overwrite) {
                                                var tmp = {
                                                    name: sPoi.name,
                                                    description: sPoi.description,
                                                    puid: sPoi.puid,
                                                    pois_type: sPoi.pois_type,
                                                    coordinates_lat: sPoi.coordinates_lat,
                                                    coordinates_lon: sPoi.coordinates_lon,
                                                    overwrite: sPoi.overwrite
                                                };
                                            }
                                            else {
                                                var tmp = {
                                                    name: sPoi.name,
                                                    description: sPoi.description,
                                                    puid: sPoi.puid,
                                                    pois_type: sPoi.pois_type,
                                                    coordinates_lat: sPoi.coordinates_lat,
                                                    coordinates_lon: sPoi.coordinates_lon,
                                                    overwrite: "false"
                                                };
                                            }
                                            flPois.push(tmp);
                                        }

                                        resFloors.push(
                                            {
                                                floor_number: fNo,
                                                pois: flPois
                                            }
                                        );

                                        count++;
                                        if (count === floors.length) {
                                            result.building.floors = resFloors;
                                            _suc($scope, 'Successfully exported ' + building.name + ' to JSON.');
                                            var blob = new Blob([JSON.stringify(result, null, 4)], {type: "text/plain;charset=utf-8"});
                                            saveAs(blob, building.name.toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
                                        }

                                    }
                                },
                                function (resp) {
                                    var data = resp.data;
                                    console.log(data.message);
                                });
                        }({
                            buid: building.buid,
                            floor_number: floors[i].floor_number
                        }));
                    }
                }
            },
            function (resp) {
                // TODO: alert failure
                console.log(resp.data.message);
            }
        );
    };

    /**
     * campus {
   "buids": [
   ],
   "description": "",
   "name": "",
   "owner_id": "",
   "access_token": "",
   "cuid": ""
    }
     * }
     */
    $scope.exportCampusToJson = function () {
        var result = {
            campus: {
                buids: []
            }
        };

        var campus = $scope.anyService.selectedCampus;
        if (LPUtils.isNullOrUndefined(campus)) {
            _err('No campus selected');
            return;
        }

        result.campus.name = campus.name;
        result.campus.description = campus.description;
        result.campus.buids = campus.buids;

        _suc($scope, 'Successfully exported ' + campus.name + ' to JSON.');

        var blob = new Blob([JSON.stringify(result, null, 4)], {type: "text/plain;charset=utf-8"});
        saveAs(blob, campus.name.toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
    };

    function readSingleFile(e) {
        var file = e.target.files[0];
        if (!file) {
            return;
        }
        var reader = new FileReader();
        reader.onload = function (e) {
            var contents = e.target.result;
            $scope.fileToUpload = contents;
        };
        reader.readAsText(file);
    }

    document.getElementById('file-input')
        .addEventListener('change', readSingleFile, false);

    $scope.anyService.downloadlogfile = false;

    $scope.importBuildingFromJson = function () {

        $scope.anyService.progress = 0;
        var i, j, count = 0, countok = 0;
        if ($scope.fileToUpload == "") {
            _err($scope, "Something went wrong no file selected");
        }
        var obj = JSON.parse($scope.fileToUpload);
        for (i = 0; i < obj.building.floors.length; i++) {
            for (j = 0; j < obj.building.floors[i].pois.length; j++) {
                if (obj.building.floors[i].pois[j].overwrite == "true") {
                    count++;
                }
            }
        }
        if (count == 0) {
            _err($scope, "Something went wrong no pois to update");
        }

        for (i = 0; i < obj.building.floors.length; i++) {
            for (j = 0; j < obj.building.floors[i].pois.length; j++) {
                if (obj.building.floors[i].pois[j].overwrite == "true") {
                    countok++;
                    $scope.updatePoifromFile(obj.building.floors[i].pois[j].puid,
                        obj.building.floors[i].pois[j].coordinates_lat,
                        obj.building.floors[i].pois[j].coordinates_lon,
                        obj.building.floors[i].pois[j].name,
                        obj.building.floors[i].pois[j].description,
                        obj.building.floors[i].pois[j].pois_type,
                        obj.building.floors[i].pois[j].overwrite,
                        obj.building.buid, i, j, count, countok,
                        obj.building.name
                    );
                }
            }
        }
    }

    $scope.allpois = {};
    $scope.pois = {};

    $scope.importBuildingFromExcel = function (oEvent) {
        $scope.anyService.progress = 0;
        // Get The File From The Input
        var oFile = document.getElementById('my_file_input').files[0];
        $scope.fileToUpload = oFile.name;
        // Create A File Reader HTML5
        var reader = new FileReader();
        // Ready The Event For When A File Gets Selected
        reader.onload = function (e) {
            var data = e.target.result;
            var cfb = XLS.CFB.read(data, {type: 'binary'});
            var wb = XLS.parse_xlscfb(cfb);
            // Loop Over Each Sheet
            wb.SheetNames.forEach(function (sheetName) {
                // Obtain The Current Row As CSV
                var oJS = XLS.utils.sheet_to_row_object_array(wb.Sheets[sheetName]);
                var last_buid = "";
                $scope.uploadloop(0, oJS, last_buid);
            });
        };

        // Tell JS To Start Reading The File.. You could delay this if desired
        reader.readAsBinaryString(oFile);
    }

    $scope.uploadloop = function (potition, oJS, last_buid) {

        for (var i = potition; i < oJS.length; i++) {
            if (last_buid != oJS[i].buid && oJS[i].buid != undefined) {
                $scope.pois = {};
                last_buid = oJS[i].buid;
                $scope.fetchAllPoisForBuilding(oJS[i].buid, oJS, i, last_buid);
                return;
            }
            var description = "";

            if (oJS[i].des3 != undefined && oJS[i].des3 != "" && oJS[i].des3 != null) {
                if (oJS[i].des4 != undefined && oJS[i].des4 != "" && oJS[i].des4 != null) {
                    description = description + oJS[i].des3 + " " + oJS[i].des4;
                }
                else {
                    description = description + oJS[i].des3;
                }
            }
            else {
                if (oJS[i].des4 != undefined && oJS[i].des4 != "" && oJS[i].des4 != null) {
                    description = description + oJS[i].des4;
                }
            }

            if (oJS[i].des1 != undefined && oJS[i].des1 != "" && oJS[i].des1 != null) {
                if (description != "") {
                    description = description + "\n" + oJS[i].des1;
                }
                else {
                    description = description + oJS[i].des1;
                }
            }

            if (oJS[i].des2 != undefined && oJS[i].des2 != "" && oJS[i].des2 != null) {
                if (description != "") {
                    description = description + "\n" + oJS[i].des2;
                }
                else {
                    description = description + oJS[i].des2;
                }
            }

            $scope.anyService.progress = (i / (oJS.length - 1)) * 100;
            if ((oJS.length - 1) == 0) $scope.anyService.progress = 100;

            if ($scope.pois[oJS[i].name]) {
                $scope.updatePoifromExcel($scope.pois[oJS[i].name].puid,
                    $scope.pois[oJS[i].name].coordinates_lat,
                    $scope.pois[oJS[i].name].coordinates_lon,
                    $scope.pois[oJS[i].name].is_building_entrance,
                    oJS[i].name,
                    description,
                    $scope.pois[oJS[i].name].pois_type,
                    $scope.pois[oJS[i].name].overwrite,
                    $scope.pois[oJS[i].name].buid,
                    ""
                );
            }
            else {
                $scope.updatePoifromExcel("",
                    "",
                    "",
                    "",
                    oJS[i].name,
                    description,
                    "",
                    "",
                    "",
                    ""
                );
            }

        }
    };

    $scope.fetchAllPoisForBuilding = function (building, oJS, position, last_buid) {
        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = building;

        var promise = AnyplaceAPIService.retrievePoisByBuilding(jsonReq);
        promise.then(
            function (resp) {
                var data = resp.data;

                $scope.myPois = data.pois;

                var sz = $scope.myPois.length;
                for (var i = sz - 1; i >= 0; i--) {
                    var name = $scope.myPois[i].name;
                    $scope.pois[name] = $scope.myPois[i];
                }
                $scope.uploadloop(position, oJS, last_buid);
            },
            function (resp) {
              ShowError($scope, resp, "Something went wrong while fetching POIs", true);
            }
        );
    };
    $scope.logfile = {
        pois: []
    };
    $scope.updatePoifromExcel = function (id, lat, lng, building_entrance, nm, des, ptype, ovwrite, bid, buildingname) {

        var obj = {};

        obj.username = $scope.creds.username;
        obj.password = $scope.creds.password;
        obj.owner_id = $scope.owner_id;

        obj.coordinates_lat = lat;
        obj.coordinates_lon = lng;

        obj.is_building_entrance = building_entrance;
        obj.name = nm;
        obj.puid = id;
        obj.description = des;
        obj.pois_type = ptype;
        obj.overwrite = "false";
        obj.buid = bid;
        // make the request at AnyplaceAPI
        var promise = $scope.anyAPI.updatePois(obj);
        promise.then(
            function (resp) {
                var data = resp.data;
                if ($scope.anyService.progress == 100) {

                    if ($scope.anyService.downloadlogfile) {
                        $scope.anyService.downloadlogfile = false;
                        _suc($scope, "Successfully updated POIs.A log file will be downloaded");
                        var blob = new Blob([JSON.stringify($scope.logfile, null, 4)], {type: "text/plain;charset=utf-8"});
                        saveAs(blob, ($scope.fileToUpload + "-log_file").toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                        }, 2500);
                    }
                    else {
                        _suc($scope, "Successfully updated POIs.");
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                            location.reload();
                        }, 2500);
                    }
                }
            },
            function (resp) {
                var data = resp.data;
                $scope.logfile.pois.push({
                    "name": nm,
                    "puid": id,
                    "description": des,
                    "status": "Something went wrong while updating POI."
                });
                $scope.anyService.downloadlogfile = true;
                if ($scope.anyService.progress == 100) {
                    $scope.anyService.downloadlogfile = false;
                    _suc($scope, "Successfully updated POIs.A log file will be downloaded");
                    var blob = new Blob([JSON.stringify($scope.logfile, null, 4)], {type: "text/plain;charset=utf-8"});
                    saveAs(blob, ($scope.fileToUpload + "-log_file").toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
                    $scope.logfile.pois = [];

                    setTimeout(function () {
                        $scope.anyService.progress = undefined;
                    }, 2500);
                }
            }
        );
    };


    $scope.updatePoifromFile = function (id, lat, lng, nm, des, ptype, ovwrite, bid, i, j, count, countok, buildingname) {

        var obj = {};

        obj.username = $scope.creds.username;
        obj.password = $scope.creds.password;
        obj.owner_id = $scope.owner_id;

        obj.coordinates_lat = lat;
        obj.coordinates_lon = lng;

        obj.name = nm;
        obj.puid = id;
        obj.description = des;
        obj.pois_type = ptype;
        obj.overwrite = "false";
        obj.buid = bid;
        // make the request at AnyplaceAPI
        var promise = $scope.anyAPI.updatePois(obj);
        promise.then(
            function (resp) {
                var data = resp.data;
                $scope.anyService.progress = countok / count * 100;
                if ($scope.anyService.progress == 100) {
                    if ($scope.anyService.downloadlogfile) {
                        _suc($scope, "Successfully updated POIs.A log file will be downloaded");
                        var blob = new Blob([JSON.stringify($scope.logfile, null, 4)], {type: "text/plain;charset=utf-8"});
                        saveAs(blob, (buildingname + "-log_file").toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                        }, 2500);
                    }
                    else {
                        _suc($scope, "Successfully updated POIs.");
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                            location.reload();
                        }, 2500);
                    }
                }
            },
            function (resp) {
                var data = resp.data;

                $scope.logfile.pois.push({
                    "name": nm,
                    "puid": id,
                    "description": des,
                    "status": "Something went wrong while updating POI."
                });

                $scope.anyService.downloadlogfile = true;
                $scope.anyService.progress = countok / count * 100;
                if ($scope.anyService.progress == 100) {

                    if ($scope.anyService.downloadlogfile) {
                        _suc($scope, "Successfully updated POIs.A log file will be downloaded");
                        var blob = new Blob([JSON.stringify($scope.logfile, null, 4)], {type: "text/plain;charset=utf-8"});
                        saveAs(blob, (buildingname + "-log_file").toLowerCase().replace(/[^a-zA-Z0-9]+/g, "-") + ".txt");
                        $scope.logfile.pois = [];
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                        }, 2500);
                    }
                    else {
                        _suc($scope, "Successfully updated POIs.");
                        setTimeout(function () {
                            $scope.anyService.progress = undefined;
                            location.reload();
                        }, 2500);
                    }
                }
            }
        );
    };

    $scope.Poisresult = {
        building: {
            floors: []
        }
    };

    $scope.Connectionsresult = {
        building: {}
    };

    $scope.zip = new JSZip();
    $scope.DownloadBackup = function () {

        var b = $scope.anyService.selectedBuilding;
        var xFloors = [];
        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = b.buid;
        $scope.anyService.progress = 0;
        var promise = AnyplaceAPIService.allBuildingFloors(jsonReq);
        promise.then(
            function (resp) {
                xFloors = resp.data.floors;
                var floor = 0;
                var floor_number = "";
                for (var i = 0; i < xFloors.length; i++) {
                    if (i == 0) {
                        floor_number = xFloors[i].floor_number;
                    }
                    else {
                        floor_number = floor_number + " " + xFloors[i].floor_number;
                    }
                }
                $scope.anyService.progress = 10;
                var buid = b.buid;
                var jsonReq2 = AnyplaceService.jsonReq;
                var promise2 = AnyplaceAPIService.downloadFloorPlanAll(jsonReq2, buid, floor_number);
                promise2.then(
                    function (resp) {
                        // on success
                        var data = resp.data;
                        var img = $scope.zip.folder("floor_plans");
                        for (var si = 0; si < data.all_floors.length; si++) {
                            if (data.all_floors[si] != "") {
                                img.file(xFloors[si].floor_number + ".png", data.all_floors[si], {base64: true});
                            }
                        }
                        $scope.anyService.progress = 25;
                        var jsonReq3 = AnyplaceService.jsonReq;
                        jsonReq3.buid = buid;
                        jsonReq3.floor = floor_number;
                        var promise3 = AnyplaceAPIService.getRadioByBuildingFloorAll(jsonReq3);
                        promise3.then(
                            function (resp) {
                                var data2 = resp.data;
                                var logs = $scope.zip.folder("radiomaps");
                                if (data2.rss_log_files) {
                                    var urls = "";
                                    for (var si2 = 0; si2 < data2.rss_log_files.length; si2++) {
                                        logs.file(xFloors[si2].floor_number + "-radiomap.txt", data2.rss_log_files[si2]);
                                    }
                                }
                                $scope.anyService.progress = 70;
                                $scope.exportPoisBuildingToJson();
                            },
                            function (resp) {

                            }
                        );
                    },
                    function (resp) {
                    }
                );
            },
            function (resp) {
              ShowError($scope, resp, ERR_FETCH_ALL_FLOORS, true);
            }
        );
    }

    $scope.exportPoisBuildingToJson = function () {

        var building = $scope.anyService.selectedBuilding;
        if (LPUtils.isNullOrUndefined(building)) {
            _err($scope, 'No building selected');
            return;
        }
        $scope.Poisresult.building.buid = building.buid;
        $scope.Poisresult.building.name = building.name;
        $scope.Poisresult.building.description = building.description;
        $scope.Poisresult.building.coordinates_lat = building.coordinates_lat;
        $scope.Poisresult.building.coordinates_lon = building.coordinates_lon;

        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = building.buid;

        var count = 0;

        var promise = AnyplaceAPIService.allBuildingFloors(jsonReq);
        promise.then(
            function (resp) {
                var floors = resp.data.floors;

                var resFloors = [];
                $scope.anyService.progress = 80;
                if (floors) {
                    for (var i = 0; i < floors.length; i++) {

                        (function (jreq) {
                            var promise = AnyplaceAPIService.retrievePoisByBuildingFloor(jreq);
                            promise.then(
                                function (resp) {
                                    var data = resp.data;

                                    var poisArray = data.pois;

                                    if (poisArray) {

                                        var flPois = [];

                                        if (poisArray[0] != undefined) {
                                            var fNo = poisArray[0].floor_number;

                                            for (var j = 0; j < poisArray.length; j++) {
                                                var sPoi = poisArray[j];

                                                if (sPoi.pois_type == "None") {
                                                    continue;
                                                }
                                                if (sPoi.overwrite) {
                                                    var tmp = {
                                                        name: sPoi.name,
                                                        description: sPoi.description,
                                                        puid: sPoi.puid,
                                                        pois_type: sPoi.pois_type,
                                                        coordinates_lat: sPoi.coordinates_lat,
                                                        coordinates_lon: sPoi.coordinates_lon,
                                                        overwrite: sPoi.overwrite
                                                    };
                                                }
                                                else {
                                                    var tmp = {
                                                        name: sPoi.name,
                                                        description: sPoi.description,
                                                        puid: sPoi.puid,
                                                        pois_type: sPoi.pois_type,
                                                        coordinates_lat: sPoi.coordinates_lat,
                                                        coordinates_lon: sPoi.coordinates_lon,
                                                        overwrite: "false"
                                                    };
                                                }

                                                if (sPoi.is_building_entrance == 'true') {
                                                    tmp.is_building_entrance = 'true';
                                                }
                                                else {
                                                    tmp.is_building_entrance = 'false';
                                                }

                                                flPois.push(tmp);
                                            }

                                            resFloors.push(
                                                {
                                                    floor_number: fNo,
                                                    pois: flPois
                                                }
                                            );
                                        }
                                        count++;
                                        if (count === floors.length) {
                                            $scope.Poisresult.building.floors = resFloors;
                                            $scope.zip.file("allpois.json", JSON.stringify($scope.Poisresult, null, 4));
                                            $scope.anyService.progress = 90;
                                            $scope.exportConnectionBuildingToJson();
                                        }

                                    }
                                },
                                function (resp) {
                                    var data = resp.data;
                                    console.log(data.message);
                                });
                        }({
                            buid: building.buid,
                            floor_number: floors[i].floor_number
                        }));
                    }
                }
            },
            function (resp) {
                // TODO: alert failure
                console.log(resp.data.message);
            }
        );
    };

    $scope.exportConnectionBuildingToJson = function () {

        var building = $scope.anyService.selectedBuilding;
        if (LPUtils.isNullOrUndefined(building)) {
            _err('No building selected');
            return;
        }
        $scope.Connectionsresult.building.buid = building.buid;
        $scope.Connectionsresult.building.name = building.name;
        $scope.Connectionsresult.building.description = building.description;
        $scope.Connectionsresult.building.coordinates_lat = building.coordinates_lat;
        $scope.Connectionsresult.building.coordinates_lon = building.coordinates_lon;

        var jsonReq = AnyplaceService.jsonReq;
        jsonReq.buid = building.buid;

        var count = 0;

        var promise = AnyplaceAPIService.allBuildingFloors(jsonReq);
        promise.then(
            function (resp) {
                var floors = resp.data.floors;

                var resFloors = [];

                if (floors) {
                    for (var i = 0; i < floors.length; i++) {

                        (function (jreq) {
                            var promise = AnyplaceAPIService.retrieveConnectionsByBuildingFloor(jreq);
                            promise.then(
                                function (resp) {
                                    $scope.anyService.progress = 100;
                                    var data = resp.data;

                                    var connArray = data.connections;

                                    if (connArray) {

                                        var flConnections = [];

                                        if (connArray[0] != undefined) {

                                            var fNo = connArray[0].floor_a;

                                            for (var j = 0; j < connArray.length; j++) {
                                                var sConnection = connArray[j];

                                                var tmp = {
                                                    name: sConnection.name,
                                                    description: sConnection.description,
                                                    cuid: sConnection.puid,
                                                    weight: sConnection.weight,
                                                    pois_a: sConnection.pois_a,
                                                    pois_b: sConnection.pois_b,
                                                    floor_a: sConnection.floor_a,
                                                    floor_b: sConnection.floor_b,
                                                    is_published: sConnection.is_published,
                                                    buid_a: sConnection.buid_a,
                                                    buid_b: sConnection.buid_b
                                                };

                                                flConnections.push(tmp);
                                            }

                                            resFloors.push(
                                                {
                                                    floor_number: fNo,
                                                    connections: flConnections
                                                }
                                            );
                                        }
                                        count++;

                                        if (count === floors.length) {

                                            $scope.Connectionsresult.building.floors = resFloors;
                                            $scope.zip.file("allconnections.json", JSON.stringify($scope.Connectionsresult, null, 4));

                                            $scope.zip.generateAsync({type: "blob"})
                                                .then(function (content) {
                                                    // see FileSaver.js
                                                    saveAs(content, building.buid + ".zip");
                                                });
                                            $scope.anyService.progress = undefined;
                                        }
                                    }
                                },
                                function (resp) {
                                    var data = resp.data;
                                    console.log(data.message);
                                });
                        }({
                            buid: building.buid,
                            floor_number: floors[i].floor_number
                        }));
                    }
                }
            },
            function (resp) {
                console.log(resp.data.message);
            }
        );
    };

    //set cookies

    $('#dismiss').on('click', function () {
        // set expire date.
        if (typeof(Storage) !== "undefined" && localStorage) {
            localStorage.setItem('dismissClicked', 'YES');
        }

    });

    if (localStorage.getItem('dismissClicked') !== 'YES') {
        function showWelcomeMessage() {
            $('#myModal_Welcome').modal('show');
        }

        window.onload = showWelcomeMessage;
    }

}]);
