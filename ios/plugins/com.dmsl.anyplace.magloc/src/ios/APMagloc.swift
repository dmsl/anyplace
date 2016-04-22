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

import Foundation
import CoreLocation

@objc(APMagloc)
class APMagloc: CDVPlugin/*, CLLocationManagerDelegate*//*SensorControllerDelegate*/ {
   
    /*func locationManager(manager: CLLocationManager, didChangeAuthorizationStatus status: CLAuthorizationStatus) {
        print("NavigatorController: didChangeAuthorizationStatus: \(status.rawValue)")
    }*/

    
    /*private let SENSOR_UPDATE_INTERVAL: Double = 0.5
    private let SENSOR_IGNORE_INTERVAL: Double = 10*SENSOR_UPDATE_INTERVAL
    
    private var magneticData: MagneticData! = nil
    private var attitudeData: AttitudeData! = nil
    
    private(set) var prepared: Bool = false
    private(set) var active: Bool = false
    
    let DUMMY_FLOOR = 0
    private var floor: Int! = nil
    
    private var width: Double! = nil
    private var height: Double! = nil
    
    private enum APMaglocDictKey: String {
        case STATUS = "status",
        LOC = "location",
        LAT = "lat",
        LNG = "lng",
        ACC = "acc"
    }
    
    private enum APMaglocStatus: Int32 {
        case ACTIVE = 0,
        INACTIVE
    }
    private enum APMaglocError: Int32 {
        case UNEXPECTED = 0,
        NOT_PREPARED,
        IS_ACTIVE,
        SENSOR_DESYNC
    }
    
    private enum APMaglocAccuracy: Int32 {
        case LOW = 0,
        MEDIUM,
        HIGH
    }

    struct CPoint {
        var x, y: CDouble
    }
    struct CVector {
        var x, y, z: CDouble
    }
    struct COrientation {
        var w: CDouble
        var v: CVector
    }
    typealias CField = CVector
    
    private var lltoxy:  ((LatLng) -> Point)! = nil
    private var xytoll: ((Point) -> LatLng)! = nil
    
    private var locationCallbackId: String! = nil
    
    private func sendErrorCode(callbackId: String, error: APMaglocError, keepCallback: Bool = false){
        var pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAsInt: error.rawValue)
        pluginResult.setKeepCallbackAsBool(keepCallback)
        commandDelegate!.sendPluginResult(pluginResult, callbackId: callbackId)
    }
    
    private func sendResult(payload: Dictionary<String, Any>, keepCallback: Bool = false){
        var pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAsDictionary: payload)
        pluginResult.setKeepCallbackAsBool(keepCallback)
        commandDelegate!.sendPluginResult(pluginResult, callbackId: self.locationCallbackId)
    }

    
    private let sensorController: SensorController!
    
    
    override func pluginInitialize() {
        /*sensorController = try? SensorController(options: [.NavigationCalibratedReferenceTrueNorth])
        if sensorController == nil {
            return nil
        }
        sensorController.attachDelegate(self)
        sensorController.setUpdateInterval(SENSOR_UPDATE_INTERVAL)*/

    }
    
    func prepare(command: CDVInvokedUrlCommand) {
        let argv = command.arguments
        assert(argv.count > 4)
        
        let bl = LatLng(lat: argv[0], lng: argv[1])
        let tr = LatLng(lat: argv[2], lng: argv[3])
        
        assert ( (bl.lng >= 179 && tr.lng <= -179) ||  (bl.lng <= tr.lng) )
        assert(  (bl.lat <= -79 && tr.lat <= -79) || (bl.lat >= 79 && tr.lat >= 79) || (bl.lat <= tr.lat) )
        let br = LatLng(lat: bl.lat, lng: tr.lng)
        let tl = LatLng(lat: tr.lat, lng: bl.lng)
        
        let width = self.width = ( LatLng.dist(tl, tr) + LatLng.dist(bl, br) ) / 2
        let height = self.height = ( LatLng.dist(bl, tl) + LatLng.dist(br, tr) ) / 2

        
        self.lltoxy = { (ll: LatLng) -> Point in
            let x = LatLng.dist( LatLng(lat: ll.lat, lng: bl.lng), ll )
            let y = LatLng.dist( LatLng(lat: bl.lat, lng: ll.lng), ll )
            return Point(x: x, y: y)
        }
        self.xytoll = {
            (p: Point) -> LatLng in
            let dx = p.x
            let dy = p.y
            let d = sqrt(pow(dx, 2) + pow(dy, 2))
            let x = atan2(p.x, p.y)
            let bearing = (x > 0 ? x : (2*M_PI + x)) * 180 / M_PI
            return LatLng(ll: bl, d: d, bearing: bearing)
        }
        
        struct CMilestone {
            var p: CPoint
            var l: CUInt
            var o: COrientation
            var f: CField
            init(m: Milestone) {
                let p = lltoxy(m.pos)
                self.p = CPoint(x: CDouble(p.x), y: CDouble(p.y))
                self.l = CUInt(m.lineId)
                self.o = COrientation(w: m.orientation.w,
                    v: CVector(
                        x: CDouble(m.orientation.x),
                        y: CDouble(m.orientation.y),
                        z: CDouble(m.orientation.z)))
                self.f = CField(x: CDouble(m.field.x), y: CDouble(m.field.y), z: CDouble(m.field.z))
            }
        }
        
        var cms: [CMilestone]
        for i in 4...argv.count {
            m = Milestone(StringToJSON(string: argv[i] as! NSString))
            cms.append(CMilestone(m: m))
        }
        mcl_init()
        
        //Set dummy floor
        floor = DUMMY_FLOOR
        
        //Add map
        let addr = UnsafeBufferPointer(start: &cms, count: cms.count).baseAddress
        mcl_add_map(addr, strideof(Milestone)*cms.count, CDouble(width), CDouble(height), CInt(floor))
        
        
        prepared = true
    }
    
    func start(command: CDVInvokedUrlCommand) {
        if !prepared {
            sendErrorCode(callbackId: command.callbackId, error: APMaglocError.NOT_PREPARED)
        } else if active {
            sendErrorCode(callbackId: command.callbackId, error: APMaglocError.IS_ACTIVE)
        } else {
            self.locationCallbackId = command.callbackId
            let argv = command.arguments
            assert(argv.count != 0)
            let fraction = argv[0]
            assert(fraction <= 1.0 && fraction >= 0)
            
            active = true
            magneticData = nil
            attitudeData = nil
            
            if argv.count == 3 {
                mcl_start(fraction, argv[1], argv[2])
            } else {
                mcl_start(fraction)
            }
            try! sensorController.start()
        }
    }
    
    func stop(command: CDVInvokedUrlCommand) {
        if sensorController.active {
            sensorController.stop()
            active = false
            
            sendResult(payload: [APMaglocDictKey.STATUS.rawValue : APMaglocStatus.INACTIVE.rawValue], keepCallback: false)
        }
    }
    
    func reset(command: CDVInvokedUrlCommand) {
        if active {
            sendErrorCode(callbackId: command.callbackId, error: APMaglocError.IS_ACTIVE)
        } else {
            prepared = false
        }
    }
    
    
    func handleUpdate(updateError error: NSError!, magneticData data: MagneticData!) {
        assert(error == nil && data != nil)
        if data.accuracy != Accuracy.Uncalibrated {
            self.magneticData = data
            self.notifyDelegates()
        }
    }
    
    func handleUpdate(updateError error: NSError!, attitudeData data: AttitudeData!) {
        assert(error == nil && data != nil)
        if data.accuracy != Accuracy.Uncalibrated {
            self.attitudeData = data
            self.notifyDelegates()
        }
    }
    
    
    
    private func location(dm: MagneticData, da: AttitudeData) -> (LatLng, APMaglocAccuracy) {
        
        return (LatLng(lat: 0.0, lng: 0.0), APMaglocAccuracy.HIGH)
        
        var p = CPoint(x: 0.0, y: 0.0)
        let addr = UnsafeMutablePointer(start: &p).baseAddress
        mcl_most_probable_position(addr, strideof(CPoint), CBool(true), CInt(floor))
        //Dummy accuracy
        let acc = APMaglocAccuracy.MEDIUM
        //Coordinates
        let coords = self.xytoll(p)
        return (coords, acc)
    }
    
    
    
    private func notifyDelegates() {
        if let dm = self.magneticData, da = self.attitudeData {
            if abs(dm.timestamp - da.timestamp) < SENSOR_IGNORE_INTERVAL {
                let (latlng, acc) = location(dm: dm, da: da)
                if acc >= APMaglocAccuracy.MEDIUM {
                    let dict = [
                        APMaglocDictKey.STATUS.rawValue : APMaglocStatus.ACTIVE.rawValue,
                        APMaglocDictKey.LOC.rawValue : [
                            APMaglocDictKey.LAT.rawValue : latlng.lat,
                            APMaglocDictKey.LNG.rawValue : latlng.lng,
                            APMaglocDictKey.ACC.rawValue : acc.rawValue
                        ]
                    ]
                    sendResult(payload: dict, keepCallback: true)
                }
            } else {
                print("APMagloc: magnetic and attitude data arrival times are desynchronized")
                sendErrorCode(callbackId: self.locationCallbackId, error: APMaglocError.SENSOR_DESYNC, keepCallback: true)
            }
        }
    }
    
     */
    func test(command: CDVInvokedUrlCommand) {
        //let v = CLLocationManager.locationServicesEnabled()
        //let v = true
        let v: Int = command.arguments[0] as! Int
        //let v = command.arguments[0] as! Int
        let v1: Int32 = mcl_test_ret(CInt(v))
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAsInt: v1)
        commandDelegate!.sendPluginResult(pluginResult, callbackId: command.callbackId)
    }
    
    /*
    */
    
    
    
    
    
}

/**
 
 <?ignore
 <source-file src="src/ios/API/ApiClient.swift" target-dir="APMaglocPlugin/API"/>
 <source-file src="src/ios/APMagloc.swift" target-dir="APMaglocPlugin"/>
 <source-file src="src/ios/Architect/ArchitectController.swift" target-dir="APMaglocPlugin/Architect"/>
 <source-file src="src/ios/Architect/FloorMap.swift" target-dir="APMaglocPlugin/Architect"/>
 <source-file src="src/ios/Architect/FloorMapView.swift" target-dir="APMaglocPlugin/Architect"/>
 <source-file src="src/ios/Architect/LoggerController.swift" target-dir="APMaglocPlugin/Architect"/>
 <source-file src="src/ios/DataTypes.swift" target-dir="APMaglocPlugin"/>
 <source-file src="src/ios/FileSystem/fileSystem.swift" target-dir="APMaglocPlugin/FileSystem"/>
 <source-file src="src/ios/Geometry/Cartesian.swift" target-dir="APMaglocPlugin/Geometry"/>
 <source-file src="src/ios/Geometry/Spherical.swift" target-dir="APMaglocPlugin/Geometry"/>
 <source-file src="src/ios/Localization/DBSCAN/dbscan.cpp" target-dir="APMaglocPlugin/Localization/DBSCAN"/>
 <source-file src="src/ios/Localization/MCL/datatypes.cpp" target-dir="APMaglocPlugin/Localization/MCL"/>
 <source-file src="src/ios/Localization/MCL/Distributions.cpp" target-dir="APMaglocPlugin/Localization/MCL"/>
 <source-file src="src/ios/Localization/MCL/Localizer.cpp" target-dir="APMaglocPlugin/Localization/MCL"/>
 <source-file src="src/ios/Localization/MCL/Map.cpp" target-dir="APMaglocPlugin/Localization/MCL"/>
 <source-file src="src/ios/Localization/MCL Bridge/MagneticMCL.cpp" target-dir="APMaglocPlugin/Localization/MCL Bridge"/>
 <source-file src="src/ios/Navigator/NavigatorController.swift" target-dir="APMaglocPlugin/Navigator"/>
 <source-file src="src/ios/Sensors/HardwarePedometer.swift" target-dir="APMaglocPlugin/Sensors"/>
 <source-file src="src/ios/Sensors/SensorController.swift" target-dir="APMaglocPlugin/Sensors"/>
 <source-file src="src/ios/Sensors/SoftwarePedometer.swift" target-dir="APMaglocPlugin/Sensors"/>
 <source-file src="src/ios/Sensors/StepCounting.swift" target-dir="APMaglocPlugin/Sensors"/>
 <source-file src="src/ios/Utils/jsonConverter.swift" target-dir="APMaglocPlugin/Utils"/>
 <source-file src="src/ios/Utils/Reachability.swift" target-dir="APMaglocPlugin/Utils"/>
 <header-file src="src/ios/Central-Bridging-Header.h" target-dir="APMaglocPlugin"/>
 <header-file src="src/ios/Localization/DBSCAN/dbscan.h" target-dir="APMaglocPlugin/Localization/DBSCAN"/>
 <header-file src="src/ios/Localization/MCL/datatypes.hpp" target-dir="APMaglocPlugin/Localization/MCL"/>
 <header-file src="src/ios/Localization/MCL/Distributions.hpp" target-dir="APMaglocPlugin/Localization/MCL"/>
 <header-file src="src/ios/Localization/MCL/Localizer.hpp" target-dir="APMaglocPlugin/Localization/MCL"/>
 <header-file src="src/ios/Localization/MCL/Map.hpp" target-dir="APMaglocPlugin/Localization/MCL"/>
 <header-file src="src/ios/Localization/MCL Bridge/MagneticMCL.h" target-dir="APMaglocPlugin/Localization/MCL Bridge"/>
 <header-file src="src/ios/Localization/MCL Bridge/MCL-Bridging-Header.h" target-dir="APMaglocPlugin/Localization/MCL Bridge"/>
 <header-file src="src/ios/Localization/stdafx.hpp" target-dir="APMaglocPlugin/Localization"/>
 ?>

 */

