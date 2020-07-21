/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Artem Nikitin
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
 * this software and associated documentation files (the "Software"), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 */

/*global cordova, module*/

/*
 * Here some magic happens. It should be possible to define plugin in the following manner:
 * function APMagloc() {};
 * APMagloc.prototype.prepare = function ...
 * APMagloc.DictKey = {...}
 * module.exports = new APMagloc()
 * I.e. we define constructor and add some static variables and member functions
 * However, plugin loads ("Device is ready") happens only if there is only 1 member function and no variables
 * I have no clue why it happens, so define in the way you can see below.
 */

var exec = require('cordova/exec')

module.exports = {

    DictKey : {
        STATUS : "status",
        LOC : "location",
        LAT : "lat",
        LNG : "lng",
        ACC : "acc",
        FLD : "field",
        ATT : "orientation",
        TMS : "timestamp",
        W : "w",
        X : "x",
        Y : "y",
        Z : "z"
    },

    Status : {
        0 : "NOT_PREPARED",
        1 : "INACTIVE",
        2 : "LOGGING",
        3 : "LOCALIZING"
    },

    Error : {
        0 : "UNEXPECTED",
        1 : "NOT_PREPARED",
        2 : "IS_ACTIVE",
        3 : "NOT_ACTIVE",
        4 : "SENSOR_DESYNC"
    },
    
    Accuracy : {
        0 : "LOW",
        1 : "MEDIUM",
        2 : "HIGH"
    },

    test : function (successCallback, errorCallback, val) {
        exec(successCallback, errorCallback, "AnyplaceMagLoc", "test", [val]);
    },
    
    /*prepare: function (successCallback, errorCallback, bl, tr, milestones) {
        if (!bl.hasOwnProperty('lat') || !bl.hasOwnProperty('lng') ||
            !tr.hasOwnProperty('lat') || !tr.hasOwnProperty('lat') ||
            typeof milestones === "undefined"){
            alert('APMagloc: prepare: wrong arguments!');
        }
        cordova.exec(successCallback, errorCallback, "AnyplaceMagLoc", "prepare", [bl.lat, bl.lng, tr.lat, tr.lng, tr, milestones]);
    }*/

    log : function (successCallback, errorCallback, updateInterval) {
        exec(successCallback, errorCallback, "AnyplaceMagLoc", "log", [updateInterval]);
    },

    /*start: function (successCallback, errorCallback, fraction, a_slow, a_fast) {
        if (typeof a_slow === "undefined" || typeof a_fast == "undefined")
            args = [fraction];
        else
            args = [fraction, a_slow, a_fast];
            
        cordova.exec(successCallback, errorCallback, "AnyplaceMagLoc", "start", args);
    }*/
    
    stop : function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, "AnyplaceMagLoc", "stop");
    },

    reset : function (successCallback, errorCallback) {
        exec(successCallback, errorCallback, "AnyplaceMagLoc", "reset");
    }

}

//module.exports = new APMagloc();



