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

@objc(APMagloc)
class APMagloc: CDVPlugin, CLLocationManagerDelegate, SensorControllerDelegate {
    
    private let SENSOR_UPDATE_INTERVAL: Double = 0.5
    private let SENSOR_IGNORE_INTERVAL: Double = 10*SENSOR_UPDATE_INTERVAL
    
    private var magneticData: MagneticData! = nil
    private var attitudeData: AttitudeData! = nil
    
    private var prepared: Bool = false
    private var active: Bool = false
    
    let DUMMY_FLOOR = 0
    private var floor: Int! = nil
    
    private var width: Double! = nil
    private var height: Double! = nil
    
    private enum APMaglocError: Int32 {
        case UNEXPECTED_ERROR = 0
    }
    
    private enum APMaglocAccuracy: Int32 {
        case LOW = 0,
        MEDIUM = 1,
        HIGH = 2
    }

    private func sendErrorCode(command: CDVInvokedUrlCommand, error: APMaglocError){
        let pluginResult = CDVPluginResult(status: CDVCommandStatus_ERROR, messageAsInt: error.rawValue)
        commandDelegate!.sendPluginResult(pluginResult, callbackId:command.callbackId)
    }
    
    private let sensorController: SensorController!
    
    init?() {
        sensorController = try? SensorController(options: [.NavigationCalibratedReferenceTrueNorth])
        if sensorController == nil {
            return nil
        }
        sensorController.attachDelegate(self)
        sensorController.setUpdateInterval(SENSOR_UPDATE_INTERVAL)
    }
    
    func prepare(command: CDVInvokedUrlCommand) {
        let argv = command.arguments
        let bl: LatLng = String(argv[0])
        let tr: LatLng = String(argv[1])
        
        assert ( (bl.lng >= 179 && tr.lng <= -179) ||  (bl.lng <= tr.lng) )
        assert(  (bl.lat <= -79 && tr.lat <= -79) || (bl.lat >= 79 && tr.lat >= 79) || (bl.lat <= tr.lat) )
        let br = LatLng(lat: bl.lat, lng: tr.lng)
        let tl = LatLng(lat: tr.lat, lng: bl.lng)
        
        let width = self.width = ( LatLng.dist(tl, tr) + LatLng.dist(bl, br) ) / 2
        let height = self.height = ( LatLng.dist(bl, tl) + LatLng.dist(br, tr) ) / 2

        
        let lltoxy = { (ll: LatLng) -> (Double, Double) in
            let x = LatLng.dist( LatLng(lat: ll.lat, lng: bl.lng), ll )
            let y = LatLng.dist( LatLng(lat: bl.lat, lng: ll.lng), ll )
            return (x, y)
        }
        
        struct CMilestone {
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
            var p: CPoint
            var l: CUint
            var o: COrientation
            var f: CField
            init(m: Milestone) {
                let (x, y) = lltoxy(m.pos)
                self.p = CPoint(x: CDouble(x), y: CDouble(y))
                self.l = CUint(m.lineId)
                self.o = COrientation(w: m.orientation.w,
                    v: CVector(
                        x: CDouble(m.orientation.x),
                        y: CDouble(m.orientation.y),
                        z: CDouble(m.orientation.z)))
                self.f = CField(x: CDouble(m.field.x), y: CDouble(m.field.y), z: CDouble(m.field.z))
            }
        }
        
        var cms: [CMilestone]
        for i in 2...argv.count {
            m = Milestone(StringToJSON(string: argv[i] as! NSString))
            cms.append(CMilestone(m: m))
        }
        
        
        mcl_init()
        
        //Set dummy floor
        floor = DUMMY_FLOOR
        
        //Add map
        
        
        
        prepared = true
    }
    
    func start(command: CDVInvokedUrlCommand) {
        if !prepared {
            //throw error
        } else {
            active = true
            magneticData = nil
            attitudeData = nil
            try! sensorController.start()
        }
    }
    
    func stop(command: CDVInvokedUrlCommand) {
        if sensorController.active {
            sensorController.stop()
            active = false
        }
    }
    
    func reset(command: CDVInvokedUrlCommand) {
        prepared = false
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
        struct CPoint {
            var x: CDouble
            var y: CDouble
        }
        var p = CPoint(x: 0.0, y: 0.0)
        let addr = UnsafeMutablePointer(start: &p).baseAddress
        mcl_most_probable_position(addr, strideof(CPoint), CBool(true), CInt(floor))
        //Dummy accuracy
        let acc = APMaglocAccuracy.MEDIUM
        //Dummy LatLng
        let coords = LatLng(lat: 0.0, lng: 0.0)
        return (coords, acc)
    }
    
    
    
    private func notifyDelegates() {
        if let dm = self.magneticData, da = self.attitudeData {
            if abs(dm.timestamp - da.timestamp) < SENSOR_IGNORE_INTERVAL {
                let (latlng, acc) = location(dm: dm, da: da)
                if acc >= APMaglocAccuracy.MEDIUM {
                    //callback
                }
            } else {
                print("APMagloc: magnetic and attitude data arrival times are desynchronized")
            }
        }
    }
    
    
    
    
    
    
    
    
    
    
}

