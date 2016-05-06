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

typealias MagneticData = SensorController.MagneticData
typealias AttitudeData = SensorController.AttitudeData

@objc(APMagloc)
class APMagloc: CDVPlugin/*, CLLocationManagerDelegate*/,SensorControllerDelegate {
    
    /*func locationManager(manager: CLLocationManager, didChangeAuthorizationStatus status: CLAuthorizationStatus) {
     print("NavigatorController: didChangeAuthorizationStatus: \(status.rawValue)")
     }*/
    
    private static let MIN_SENSOR_UPDATE_INTERVAL: Double = 0.5
    private static let DEFAULT_SENSOR_UPDATE_INTERVAL: Double = 0.5
    private static let DEFAULT_SENSOR_IGNORE_UPDATE_INTERVAL_RATIO: Double = 5.0
    
    private(set) var updateInterval: Double! = nil
    private(set) var ignoreUpdateIntervalRatio: Double! = nil
    private var ignoreInterval: Double! = nil
    
    private func setUpdateInterval(T: Double) {
        assert(T >= APMagloc.MIN_SENSOR_UPDATE_INTERVAL)
        updateInterval = T < APMagloc.MIN_SENSOR_UPDATE_INTERVAL ? APMagloc.MIN_SENSOR_UPDATE_INTERVAL : T
        ignoreInterval = updateInterval*ignoreUpdateIntervalRatio
        sensorController.setUpdateInterval(updateInterval)
    }
    
    private func setIgnoreUpdateIntervalRatio(r: Double) {
        assert(r >= 1.0)
        ignoreUpdateIntervalRatio = r < 1.0 ? 1.0 : r
        ignoreInterval = updateInterval*ignoreUpdateIntervalRatio
    }
    
    private var magneticData: MagneticData! = nil
    private var attitudeData: AttitudeData! = nil
    
    let DUMMY_FLOOR = 0
    private var floor: Int! = nil
    
    private enum DictKey: String {
        case STATUS = "status",
        LOC = "location",
        LAT = "lat",
        LNG = "lng",
        ACC = "acc",
        FLD = "field",
        ATT = "orientation",
        TMS = "timestamp",
        W = "w",
        X = "x",
        Y = "y",
        Z = "z"
    }
    
    private enum Status: String {
        case NOT_PREPARED = "0",
        INACTIVE = "1",
        LOGGING = "2",
        LOCALIZING = "3"
    }
    private enum Error: Int32 {
        case UNEXPECTED = 0,
        NOT_PREPARED,
        IS_ACTIVE,
        NOT_ACTIVE,
        SENSOR_DESYNC
    }
    private enum Accuracy: String {
        case LOW = "0",
        MEDIUM = "1",
        HIGH = "2"
    }
    
    private(set) var prepared: Bool = false
    private var status: Status = .INACTIVE
    private var active: Bool { get { return status.rawValue >= Status.LOGGING.rawValue } }
    
    
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
    
    //    private var lltoxy:  ((LatLng) -> Point)! = nil
    private var xytoll: ((Point) -> (LatLng))! = nil
    
    private var callbackId: String! = nil
    
    private func sendErrorCode(error error: Error, callbackId: String, keepCallback: Bool = false){
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAsInt: error.rawValue)
        pluginResult.setKeepCallbackAsBool(keepCallback)
        commandDelegate!.sendPluginResult(pluginResult, callbackId: callbackId)
    }
    
    private func sendResult(payload payload: [String: AnyObject], callbackId: String, keepCallback: Bool = false){
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAsDictionary: payload)
        pluginResult.setKeepCallbackAsBool(keepCallback)
        commandDelegate!.sendPluginResult(pluginResult, callbackId: callbackId)
    }
    
    //In order to release callbackId from start() and log() along with stop()
    //need to send dummy result that does not trigger callbacks
    //Solution from here: http://stackoverflow.com/questions/36580098/custom-cordova-plugin-release-a-kept-callback-without-calling-it
    //However, also found threads on error connected with CDVCommandStatus_NO_RESULT (both callbacks were called) for cordova 4.1, 4.2
    //Here: https://github.com/don/cordova-plugin-ble-central/issues/32
    private func sendDummy(callbackId callbackId: String, keepCallback: Bool = false) {
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_NO_RESULT)
        pluginResult.setKeepCallbackAsBool(keepCallback)
        commandDelegate!.sendPluginResult(pluginResult, callbackId: callbackId)
    }
    
    
    private var sensorController: SensorController! = nil
    
    override func pluginInitialize() {
        updateInterval = APMagloc.DEFAULT_SENSOR_UPDATE_INTERVAL
        ignoreUpdateIntervalRatio = APMagloc.DEFAULT_SENSOR_IGNORE_UPDATE_INTERVAL_RATIO
        ignoreInterval = APMagloc.DEFAULT_SENSOR_UPDATE_INTERVAL*APMagloc.DEFAULT_SENSOR_IGNORE_UPDATE_INTERVAL_RATIO
        
        sensorController = try! SensorController(options: [.NavigationCalibratedReferenceTrueNorth, .MagneticFieldCalibrated])
        sensorController.attachDelegate(self)
        sensorController.setUpdateInterval(self.updateInterval)
    }
    
    func prepare(command: CDVInvokedUrlCommand) {
        let argv = command.arguments
        assert(argv.count > 4)
        
        let bl = LatLng(lat: argv[0] as! Double, lng: argv[1] as! Double)
        let tr = LatLng(lat: argv[2] as! Double, lng: argv[3] as! Double)
        
        assert ( (bl.lng >= 179 && tr.lng <= -179) ||  (bl.lng <= tr.lng) )
        assert(  (bl.lat <= -79 && tr.lat <= -79) || (bl.lat >= 79 && tr.lat >= 79) || (bl.lat <= tr.lat) )
        let br = LatLng(lat: bl.lat, lng: tr.lng)
        let tl = LatLng(lat: tr.lat, lng: bl.lng)
        
        let width = ( LatLng.dist(tl, tr) + LatLng.dist(bl, br) ) / 2
        let height = ( LatLng.dist(bl, tl) + LatLng.dist(br, tr) ) / 2
        
        let lltoxy = { (ll: LatLng) -> (Point) in
            let x = LatLng.dist( LatLng(lat: ll.lat, lng: bl.lng), ll )
            let y = LatLng.dist( LatLng(lat: bl.lat, lng: ll.lng), ll )
            return Point(x: x, y: y)
        }
        self.xytoll = {
            (p: Point) -> (LatLng) in
            let dx = p.x
            let dy = p.y
            let d = sqrt(pow(dx, 2) + pow(dy, 2))
            let x = atan2(p.x, p.y)
            let bearing = (x > 0 ? x : (2*M_PI + x)) * 180 / M_PI
            return LatLng(ll: bl, d: d, bearing: bearing)
        }
        struct CMilestone {
            var p: CPoint
            var l: CUnsignedInt
            var o: COrientation
            var f: CField
            init(m: ApiClientDataTypes.Milestone, lltoxy: (LatLng) -> (Point)) {
                let p = lltoxy(m.pos)
                self.p = CPoint(x: CDouble(p.x), y: CDouble(p.y))
                self.l = CUnsignedInt(m.lineId)
                self.o = COrientation(w: m.orientation.w,
                                      v: CVector(
                                        x: CDouble(m.orientation.x),
                                        y: CDouble(m.orientation.y),
                                        z: CDouble(m.orientation.z)))
                self.f = CField(x: CDouble(m.field.x), y: CDouble(m.field.y), z: CDouble(m.field.z))
            }
        }
        
        var cms = [CMilestone]()
        for i in 4...argv.count {
            let m = ApiClientDataTypes.Milestone(JSON: StringToJSON(argv[i] as! NSString) as! [String: AnyObject])
            cms.append(CMilestone(m: m, lltoxy: lltoxy))
        }
        mcl_init()
        
        //Set dummy floor
        self.floor = self.DUMMY_FLOOR
        
        //Add map
        let addr = UnsafeBufferPointer(start: &cms, count: cms.count).baseAddress
        mcl_add_map(addr, strideof(ApiClientDataTypes.Milestone.self)*cms.count, CDouble(width), CDouble(height), CInt(self.floor))
        
        prepared = true
        sendResult(payload: [DictKey.STATUS.rawValue : Status.INACTIVE.rawValue], callbackId: command.callbackId, keepCallback: false)
    }
    
    func log(command: CDVInvokedUrlCommand) {
        if active {
            sendErrorCode( error: Error.IS_ACTIVE, callbackId: command.callbackId )
        } else {
            self.callbackId = command.callbackId
            let argv = command.arguments
            assert(argv.count != 0)
            
            setUpdateInterval(argv[0] as! Double)
            status = .LOGGING
            magneticData = nil
            attitudeData = nil
            try! sensorController.changeOptions([.OrientationCalibratedReferenceTrueNorth, .MagneticFieldCalibrated])
            try! sensorController.start()
        }
    }
    
    func start(command: CDVInvokedUrlCommand) {
        if !prepared {
            sendErrorCode(error: Error.NOT_PREPARED, callbackId: command.callbackId)
        } else if active {
            sendErrorCode(error: Error.IS_ACTIVE, callbackId: command.callbackId)
        } else {
            self.callbackId = command.callbackId
            let argv = command.arguments
            assert(argv.count == 1 || argv.count == 3)
            let fraction = argv[0] as! Double
            assert(fraction <= 1.0 && fraction >= 0)
            
            status = .LOCALIZING
            magneticData = nil
            attitudeData = nil
            
            if argv.count == 3 {
                mcl_start(fraction, argv[1] as! Double, argv[2] as! Double)
            } else {
                mcl_start(fraction, 0.05, 0.4)
            }
            try! sensorController.changeOptions([.NavigationCalibratedReferenceTrueNorth, .MagneticFieldCalibrated])
            try! sensorController.start()
        }
    }
    
    func stop(command: CDVInvokedUrlCommand) {
        sendDummy(callbackId: command.callbackId)
        return
        if sensorController.active {
            sensorController.stop()
            status = .INACTIVE
            sendResult(payload: [DictKey.STATUS.rawValue : Status.INACTIVE.rawValue], callbackId: command.callbackId, keepCallback: false)
            //Need to free both this callbackId and self.callbackId
            sendDummy(callbackId: self.callbackId, keepCallback: false)
            self.callbackId = nil
        } else {
            sendErrorCode(error: Error.NOT_ACTIVE, callbackId: command.callbackId)
        }
        sendDummy(callbackId: command.callbackId)
    }
    
    func reset(command: CDVInvokedUrlCommand) {
        if active {
            sendErrorCode(error: Error.IS_ACTIVE, callbackId: command.callbackId)
        } else {
            prepared = false
            sendResult(payload: [DictKey.STATUS.rawValue : Status.NOT_PREPARED.rawValue], callbackId: command.callbackId)
        }
    }
    
    
    func handleUpdate(updateError error: NSError!, magneticData data: MagneticData!) {
        assert(error == nil && data != nil)
        if data.accuracy != .Uncalibrated {
            //        if data.accuracy.rawValue >= .MEDIUM.rawValue {
            self.magneticData = data
            self.notifyDelegates()
        }
    }
    
    func handleUpdate(updateError error: NSError!, attitudeData data: AttitudeData!) {
        assert(error == nil && data != nil)
        if data.accuracy != .Uncalibrated {
            //        if data.accuracy.rawValue >= .MEDIUM.rawValue {
            self.attitudeData = data
            self.notifyDelegates()
        }
    }
    
    
    private func location(dm dm: MagneticData, da: AttitudeData) -> (LatLng, Accuracy) {
        
        return (LatLng(lat: 0.0, lng: 0.0), Accuracy.HIGH)
        
        let addr = UnsafeMutablePointer<CPoint>.alloc(1)
        addr.initialize(CPoint(x: 0.0, y: 0.0))
        mcl_most_probable_position(addr, strideof(CPoint.self), CBool(true), CInt(floor))
        let p = addr.memory
        addr.destroy(); addr.dealloc(1)
        //Dummy accuracy
        let acc = Accuracy.MEDIUM
        //Coordinates
        let coords = self.xytoll(Point(x: p.x, y: p.y))
        return (coords, acc)
    }
    
    
    
    private func notifyDelegates() {
        if let dm = self.magneticData, da = self.attitudeData {
            print("dm: \(dm.timestamp) da: \(da.timestamp) update: \(self.updateInterval) ignore: \(self.ignoreInterval)")
            if abs(dm.timestamp - da.timestamp) < self.ignoreInterval {
                let tms = (dm.timestamp + da.timestamp) / 2
                if status == .LOCALIZING {
                    let (latlng, acc) = location(dm: dm, da: da)
                    if acc.rawValue >= Accuracy.MEDIUM.rawValue {
                        let dict: [String: AnyObject] = [
                            DictKey.STATUS.rawValue : Status.LOCALIZING.rawValue,
                            DictKey.TMS.rawValue : tms,
                            DictKey.LOC.rawValue : [
                                DictKey.LAT.rawValue : latlng.lat,
                                DictKey.LNG.rawValue : latlng.lng,
                                DictKey.ACC.rawValue : acc.rawValue
                            ]
                        ]
                        sendResult(payload: dict, callbackId: self.callbackId, keepCallback: true)
                        self.magneticData = nil
                        self.attitudeData = nil
                    }
                } else if status == .LOGGING {
                    let dict: [String: AnyObject] = [
                        DictKey.STATUS.rawValue : Status.LOGGING.rawValue,
                        DictKey.TMS.rawValue : tms,
                        DictKey.FLD.rawValue : [
                            DictKey.X.rawValue : dm.field.x,
                            DictKey.Y.rawValue : dm.field.y,
                            DictKey.Z.rawValue : dm.field.z
                        ],
                        DictKey.ATT.rawValue : [
                            DictKey.W.rawValue : da.attitude.quaternion.w,
                            DictKey.X.rawValue : da.attitude.quaternion.x,
                            DictKey.Y.rawValue : da.attitude.quaternion.y,
                            DictKey.Z.rawValue : da.attitude.quaternion.z
                        ]
                    ]
                    sendResult(payload: dict, callbackId: self.callbackId, keepCallback: true)
                    self.magneticData = nil
                    self.attitudeData = nil
                }
            } else {
                print("APMagloc: magnetic and attitude data arrival times are desynchronized")
                sendErrorCode(error: Error.SENSOR_DESYNC, callbackId: self.callbackId, keepCallback: true)
            }
        }
    }
    
    func test(command: CDVInvokedUrlCommand) {
        //let v = CLLocationManager.locationServicesEnabled()
        //let v = true
        let v: Int = command.arguments[0] as! Int
        //let v = command.arguments[0] as! Int
        let v1: Int32 = mcl_test_ret(CInt(v))
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_OK, messageAsInt: v1)
        commandDelegate!.sendPluginResult(pluginResult, callbackId: command.callbackId)
    }
    
}

