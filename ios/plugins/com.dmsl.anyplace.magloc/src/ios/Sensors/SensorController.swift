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
import CoreMotion
import CoreLocation

@objc protocol SensorControllerDelegate: class {
    optional func handleUpdate(updateError error: NSError!, magneticData data: MagneticData!)
    optional func handleUpdate(updateError error: NSError!, accelerationData data: AccelerationData!)
    optional func handleUpdate(updateError error: NSError!, rotationData data: RotationData!)
    optional func handleUpdate(updateError error: NSError!, attitudeData data: AttitudeData!)
    optional func handleUpdate(updateError error: NSError!, altitudeData data: AltitudeData!)
    optional func handleUpdate(updateError error: NSError!, pedometerData data: PedometerData!)
}

enum UpdateSource: Int {
    case Raw, Calibrated
}

enum Accuracy {
    case Raw, Uncalibrated, Low, Medium, High
}

enum Sensor: Int, Hashable {
    case Magnetometer, Accelerometer, Gyro, Altimeter, Pedometer
    
    var hashValue: Int {
        return rawValue
    }
    
    static var all: [Sensor] {
        return [Magnetometer, Accelerometer, Gyro, Altimeter, Pedometer]
    }
}

enum Source: Int, Hashable {
    case MagneticField, MagneticFieldCalibrated, Acceleration, AccelerationCalibrated, Rotation, RotationCalibrated, Attitude, Altitude, Pedometer
    
    var hashValue: Int {
        return rawValue
    }
    
    static var all: [Source] {
        return [MagneticField, MagneticFieldCalibrated, Acceleration, AccelerationCalibrated, Rotation, RotationCalibrated, Attitude, Altitude, Pedometer]
    }
}

struct SensorControllerOptions: OptionSetType {
    let rawValue: Int
    init(rawValue: Int) { self.rawValue = rawValue }
    
    private static let Calibrated = SensorControllerOptions(rawValue: 1)
    static let ReferenceMagneticNorth = SensorControllerOptions(rawValue: 1 << 2)
    static let ReferenceTrueNorth = SensorControllerOptions(rawValue: 1 << 3)
    static let MagneticField = SensorControllerOptions(rawValue: 1 << 4)
    static let MagneticFieldCalibrated = SensorControllerOptions(rawValue: 1 << 5 + 1)
    static let Acceleration = SensorControllerOptions(rawValue: 1 << 6)
    static let AccelerationCalibrated = SensorControllerOptions(rawValue: 1 << 7 + 1)
    static let Rotation = SensorControllerOptions(rawValue: 1 << 8)
    static let RotationCalibrated = SensorControllerOptions(rawValue: 1 << 9 + 1)
    static let Attitude = SensorControllerOptions(rawValue: 1 << 10 + 1)
    static let Altitude = SensorControllerOptions(rawValue: 1 << 11)
    static let Pedometer = SensorControllerOptions(rawValue: 1 << 12)
    static let Motion: SensorControllerOptions = [Acceleration, Rotation]
    static let MotionCalibrated: SensorControllerOptions = [AccelerationCalibrated, RotationCalibrated]
    static let Orientation: SensorControllerOptions = [MagneticField, Acceleration, Rotation]
    static let OrientationCalibrated: SensorControllerOptions = [Attitude]
    static let OrientationCalibratedReferenceMagneticNorth: SensorControllerOptions = [ReferenceMagneticNorth, OrientationCalibrated]
    static let OrientationCalibratedReferenceTrueNorth: SensorControllerOptions = [ReferenceTrueNorth, OrientationCalibrated]
    static let Navigation: SensorControllerOptions = [Orientation,  Pedometer]
    static let NavigationCalibrated: SensorControllerOptions = [OrientationCalibrated,  Pedometer]
    static let NavigationCalibratedReferenceMagneticNorth: SensorControllerOptions = [OrientationCalibratedReferenceMagneticNorth,  Pedometer]
    static let NavigationCalibratedReferenceTrueNorth: SensorControllerOptions = [OrientationCalibratedReferenceTrueNorth,  Pedometer]
}

enum SensorControllerError: ErrorType {
    case RawUnavailable, CalibratedUnavailable, NoDelegates, ConfigurationConflict, OperationUnavailableWhileActive, NeedLocationServicesEnabledWhenInUse
}

typealias Rotation = Vector3D
typealias Acceleration = Vector3D
typealias MagneticField = Vector3D

@objc class SensorData: NSObject {
    var timestamp: Double
    let accuracy: Accuracy
    init(timestamp: Double, accuracy: Accuracy ) {
        self.timestamp = timestamp
        self.accuracy = accuracy
    }
}

class MagneticData: SensorData {
    let field: MagneticField
    init(field: MagneticField, timestamp: Double, accuracy: Accuracy ) {
        self.field = field
        super.init(timestamp: timestamp, accuracy: accuracy)
    }
}

class AccelerationData: SensorData {
    let acceleration: Acceleration
    init(acceleration: Acceleration, timestamp: Double, accuracy: Accuracy ) {
        self.acceleration = acceleration
        super.init(timestamp: timestamp, accuracy: accuracy)
    }
}

class RotationData: SensorData {
    let rotation: Rotation
    init(rotation: Rotation, timestamp: Double, accuracy: Accuracy ) {
        self.rotation = rotation
        super.init(timestamp: timestamp, accuracy: accuracy)
    }
}

class AttitudeData: SensorData {
    let attitude: CMAttitude
    init(attitude: CMAttitude, timestamp: Double, accuracy: Accuracy ) {
        self.attitude = attitude
        super.init(timestamp: timestamp, accuracy: accuracy)
    }
}

class AltitudeData: SensorData {
    let altitude: Double
    init(altitude: Double, timestamp: Double, accuracy: Accuracy ) {
        self.altitude = altitude
        super.init(timestamp: timestamp, accuracy: accuracy)
    }
}

class PedometerData: SensorData {
    let steps: UInt
    let distance: Double
    init(steps: UInt, distance: Double, timestamp: Double, accuracy: Accuracy ) {
        self.steps = steps
        self.distance = distance
        super.init(timestamp: timestamp, accuracy: accuracy)
    }
}

@objc class SensorController: NSObject, StepCounterDelegate, CLLocationManagerDelegate {
    
    func locationManager(manager: CLLocationManager, didUpdateToLocation newLocation: CLLocation, fromLocation oldLocation: CLLocation) {
        print("didUpdateLocation \(newLocation)")
    }
    
    private var motionManager: CMMotionManager!
    private var locationManager: CLLocationManager
    private var altimeter: CMAltimeter!
    private var softwarePedometer: SoftwarePedometer!
    private var hardwarePedometer: HardwarePedometer!
    
    private var delegates = [SensorControllerDelegate]()
    
    func attachDelegate(delegate: SensorControllerDelegate) {
        let new = !delegates.contains() { $0 === delegate }
        if new {
            delegates.append(delegate)
        }
    }
    
    func detachDelegate(delegate: SensorControllerDelegate) {
        var ind: Int? = nil
        for i in (0...delegates.count) {
            if delegates[i] === delegate {
                ind = i
                break
            }
        }
        if ind != nil {
            delegates.removeAtIndex(ind!)
        }
    }
    
    func detachDelegates() {
        delegates.removeAll(keepCapacity: true)
    }

    static private let defaultUpdateInterval = 0.1
    
    private var updateIntervals: [Source: Double] = [Source: Double]()
    
    func setUpdateInterval(dt: Double) {
        for source in Source.all {
            setUpdateInterval(dt, source: source)
        }
    }
    func setUpdateInterval(dt: Double, source: Source) {
        updateIntervals[source] = max(0.0, dt)
    }
    func getUpdateInterval(source: Source) -> Double {
        return updateIntervals[source]!
    }
    
    private var startTimestamps = [Source: Double]()
    private var lastTimestamps = [Source: Double] ()
    
    func getStartTimestamp(source: Source) -> Double! {
        return startTimestamps[source]
    }
    
    func available(source: Source) -> Bool {
        switch source {
        case .MagneticField: return magnetometerAvailable
        case .Acceleration: return accelerometerAvailable
        case .Rotation: return gyroAvailable
        case .MagneticFieldCalibrated: fallthrough
        case .AccelerationCalibrated: fallthrough
        case .RotationCalibrated: fallthrough
        case .Attitude: return deviceMotionAvailable
        case .Altitude: return altimeterAvailable
        case .Pedometer: return pedometerAvailable
        }
    }
    
    private var deviceMotionAvailable: Bool { return motionManager.deviceMotionAvailable }
    private var magnetometerAvailable: Bool { return motionManager.magnetometerAvailable }
    private var accelerometerAvailable: Bool { return motionManager.accelerometerAvailable }
    private var gyroAvailable: Bool { return motionManager.gyroAvailable }
    private var altimeterAvailable: Bool { return CMAltimeter.isRelativeAltitudeAvailable() }
    private var pedometerAvailable: Bool { return HardwarePedometer.isAvailable || accelerometerAvailable }
    
    private var locationServicesEnabled: Bool { return CLLocationManager.locationServicesEnabled() && CLLocationManager.authorizationStatus() != CLAuthorizationStatus.AuthorizedWhenInUse || CLLocationManager.authorizationStatus() != CLAuthorizationStatus.AuthorizedAlways }
    
    var referenceMagneticNorthAvailable: Bool { return CMMotionManager.availableAttitudeReferenceFrames().contains(CMAttitudeReferenceFrame.XMagneticNorthZVertical) }
    var referenceTrueNorthAvailable: Bool { return CMMotionManager.availableAttitudeReferenceFrames().contains(CMAttitudeReferenceFrame.XTrueNorthZVertical) && locationServicesEnabled }
    
    
    var active: Bool { return magnetometerActive || accelerometerActive || gyroActive || deviceMotionActive || altimeterActive || pedometerActive }
    
    private var magnetometerActive: Bool = false
    private var accelerometerActive: Bool = false
    private var gyroActive: Bool = false
    private var deviceMotionActive: Bool = false
    private var altimeterActive: Bool = false
    private var pedometerActive: Bool = false
    
    private let initialOptions: SensorControllerOptions
    private var options: SensorControllerOptions
    
    func changeOptions(options: SensorControllerOptions) throws {
        if active {
            throw SensorControllerError.OperationUnavailableWhileActive
        }
        if !initialOptions.contains(options) {
            throw SensorControllerError.ConfigurationConflict
        }
        self.options = options
    }
    
    
    init?(options: SensorControllerOptions) throws {
        self.initialOptions = options
        self.options = options
        self.motionManager = CMMotionManager()
        self.altimeter = CMAltimeter()
        self.locationManager = CLLocationManager()
        super.init()
        locationManager.delegate = self
        
        setUpdateInterval(SensorController.defaultUpdateInterval)
        
        if options.contains(.ReferenceMagneticNorth) {
            if !referenceMagneticNorthAvailable {
                print("SensorController: init: Reference magnetic north NOT available")
                return nil
            } else {
                print("SensorController: init: Reference magnetic north available")
            }
        }
        
        if options.contains(.ReferenceTrueNorth) {
            if !referenceTrueNorthAvailable {
                print("SensorController: init: Reference true north NOT available")
                if !locationServicesEnabled {
                    throw SensorControllerError.NeedLocationServicesEnabledWhenInUse
                }
                return nil
            } else {
                print("SensorController: init: Reference true north available")
//                if CLLocationManager.authorizationStatus() == CLAuthorizationStatus.NotDetermined {
//                    locationManager.requestAlwaysAuthorization()
//                }
            }
            
        }
        
        if options.contains(.ReferenceTrueNorth) && !CLLocationManager.locationServicesEnabled()  {
            print("SensorController: init: reference to true north is impossible without location service")
            return nil
        }
        
        if options.contains(.Calibrated) {
            if !motionManager.deviceMotionAvailable {
print("SensorController: init: Calibration is NOT available")
                return nil
            } else {
                print("SensorController: init: Calibration is available")
            }
        }
        
        if options.contains(.MagneticField) {
            if !available(Source.MagneticField){
                print("SensorController: init: Magnetic NOT available")
                return nil
            } else {
                print("SensorController: init: Magnetic available")
            }
}
        if options.contains(.Acceleration) {
            if !available(Source.Acceleration) {
                print("SensorController: init: Acceleration NOT available")
                return nil
            } else {
                print("SensorController: init: Acceleration available")
            }
}
        if options.contains(.Rotation) {
            if !available(Source.Rotation) {
                print("SensorController: init: Rotation NOT available")
                return nil
            } else {
                print("SensorController: init: Rotation available")
            }
        }
        
        if options.contains(.Altitude) {
            if !available(Source.Altitude) {
                print("SensorController: init: Altimeter NOT available")
                return nil
            } else {
                print("SensorController: init: Altimeter available")
                self.altimeter = CMAltimeter()
            }
        }
        
        if options.contains(.Attitude) {
            if !available(Source.Attitude) {
                print("SensorController: init: Attitude NOT available")
                return nil
            } else {
                print("SensorController: init: Attitude available")
            }
        }
        
        if options.contains(.Pedometer) {
            if !available(Source.Pedometer) {
                print("SensorController: init: Pedometer NOT available")
                return nil
            } else {
                if !HardwarePedometer.isAvailable {
                    softwarePedometer = SoftwarePedometer()
                    assert( softwarePedometer != nil )
                    softwarePedometer.delegate = self
                    print("SensorController: init: Hardware pedometer NOT available, but Software pedometer available")
                } else {
                    hardwarePedometer = HardwarePedometer()
                    assert( hardwarePedometer != nil )
                    hardwarePedometer.delegate = self
                    print("SensorController: init: Hardware pedometer available")
                }
            }
        }
     
        
    }
    
    private var queues = [Source: NSOperationQueue]()
    func setOperationQueue(queue: NSOperationQueue) {
        for source in Source.all {
            queues[source] = queue
        }
    }
    func setOperationQueue(source: Source, queue: NSOperationQueue) {
        queues[source] = queue
    }
    
    func start() throws {
        try start(self.options)
//        print("intervals \(updateIntervals)")
    }
    
    private func start(options: SensorControllerOptions) throws {
        if delegates.isEmpty {
            throw SensorControllerError.NoDelegates
        }
        startTimestamps.removeAll()
        let calibrated = options.contains(.Calibrated)
        let magnetic = options.contains(.MagneticField)
        let acceleration = options.contains(.Acceleration)
        let rotation = options.contains(.Rotation)
        let attitude = options.contains(.Attitude)
        let altitude = options.contains(.Altitude)
        let pedometer = options.contains(.Pedometer)
        
        if calibrated || attitude /*actually, is not needed as .Attitude includes .Calibrated in options*/ {
            try! startDeviceMotion()
        }
        if magnetic {
            try! startMagnetometer()
        }
        if acceleration {
            try! startAccelerometer()
        }
        if rotation {
            try! startGyro()
        }
        if altitude {
            try! startAltimeter()
        }
        if pedometer {
            try! startPedometer()
        }
    }
    

    private func startMagnetometer() throws {
        print("SensorController: startMagnetometer")
        if !options.contains(.MagneticField) {
            throw SensorControllerError.ConfigurationConflict
        }
        if magnetometerActive {
            print("SensorController: startMagnetometer: already active")
            return
        }
        
        let queue = queues[.MagneticField] ?? NSOperationQueue.currentQueue() ?? NSOperationQueue.mainQueue()
        let dt = updateIntervals[.MagneticField]!
        
        motionManager.magnetometerUpdateInterval = NSTimeInterval(dt)
        motionManager.startMagnetometerUpdatesToQueue(queue, withHandler: handleUpdate)
    }
    
    private func startDeviceMotion() throws {
        print("SensorController: startDeviceMotion")
        if !options.contains(.Calibrated) {
            throw SensorControllerError.ConfigurationConflict
        }
        if deviceMotionActive {
            print("SensorController: startDeviceMotion: already active")
            return
        }
        let frame = options.contains(.ReferenceMagneticNorth) ? CMAttitudeReferenceFrame.XMagneticNorthZVertical : (options.contains(.ReferenceTrueNorth) ? CMAttitudeReferenceFrame.XTrueNorthZVertical :  CMAttitudeReferenceFrame.XArbitraryCorrectedZVertical)
        
        print("frame = true north \(frame == CMAttitudeReferenceFrame.XTrueNorthZVertical)")
//        print("\(queues)")
        var queue: NSOperationQueue! = queues[.MagneticFieldCalibrated] ?? queues[.AccelerationCalibrated] ?? queues[.RotationCalibrated] ?? queues[.Attitude]
        queue = queue ?? NSOperationQueue.currentQueue() ?? NSOperationQueue.mainQueue()
//        print("\(queue)")
        
        var dt: Double = Double.infinity
        if options.contains(.MagneticFieldCalibrated) {
            dt = min(dt, updateIntervals[.MagneticFieldCalibrated]!)
        }
        if options.contains(.AccelerationCalibrated) {
            dt = min(dt, updateIntervals[.AccelerationCalibrated]!)
        }
        if options.contains(.RotationCalibrated) {
            dt = min(dt, updateIntervals[.RotationCalibrated]!)
        }
        if options.contains(.Attitude) {
            dt = min(dt, updateIntervals[.Attitude]!)
        }
        if options.contains(.Pedometer) && softwarePedometer != nil {
            dt = min(dt, SoftwarePedometer.BEST_UPDATE_INTERVAL)
        }
        dt = max(dt, 0.0)
        
        
        //NOT sure that it is needed, however detected situation when updates did not come without 
        //starting location updates
//        if frame == CMAttitudeReferenceFrame.XTrueNorthZVertical {
////            if CLLocationManager.significantLocationChangeMonitoringAvailable() {
////                print("SensorController: sartDeviceMotion: startMonitoringSignificantLocationChanges")
////                locationManager.startMonitoringSignificantLocationChanges()
////            } else {
//                print("SensorController: startDeviceMotion: startUpdatingLocation")
//                locationManager.pausesLocationUpdatesAutomatically = true
//                locationManager.startUpdatingLocation()
////            }
//        }
        
        
        motionManager.deviceMotionUpdateInterval = NSTimeInterval(dt)
        motionManager.showsDeviceMovementDisplay = true
        motionManager.startDeviceMotionUpdatesUsingReferenceFrame(frame, toQueue: queue, withHandler: handleUpdate)
        deviceMotionActive = true
    }
    
    private func startAccelerometer() throws {
        print("SensorController: startAccelerometer")
        if !options.contains(.Acceleration) {
            throw SensorControllerError.ConfigurationConflict
        }
        if accelerometerActive {
            print("SensorController: startAccelerometer: already active")
            return
        }
        
        let queue: NSOperationQueue = queues[.Acceleration] ?? NSOperationQueue.currentQueue() ?? NSOperationQueue.mainQueue()
        
        var dt: Double = Double.infinity
        if softwarePedometer != nil {
            dt = min( dt, updateIntervals[.Pedometer]! )
        } else {
            dt = min( dt, updateIntervals[.Acceleration]! )
        }
        
        motionManager.accelerometerUpdateInterval = NSTimeInterval(dt)
        motionManager.startAccelerometerUpdatesToQueue(queue, withHandler: handleUpdate)
        accelerometerActive = true
    }
    
    private func startGyro() throws {
        print("SensorController: startGyro")
        if !options.contains(.Rotation) {
            throw SensorControllerError.ConfigurationConflict
        }
        if gyroActive {
            print("SensorController: startGyro: already active")
            return
        }
        
        let queue: NSOperationQueue = queues[.Rotation] ?? NSOperationQueue.currentQueue() ?? NSOperationQueue.mainQueue()
        let dt: Double = updateIntervals[.Rotation]!
        
        motionManager.gyroUpdateInterval = NSTimeInterval(dt)
        motionManager.startGyroUpdatesToQueue(queue, withHandler: handleUpdate)
        gyroActive = true
    }
    
    private func startAltimeter() throws {
        print("SensorController: startAltimeter")
        if !options.contains(.Altitude) {
            throw SensorControllerError.ConfigurationConflict
        }
        if altimeterActive {
            print("SensorController: startAltimeter: already active")
            return
        }
        
        let queue: NSOperationQueue = queues[.Altitude] ?? NSOperationQueue.currentQueue() ?? NSOperationQueue.mainQueue()
        
        altimeter.stopRelativeAltitudeUpdates()
        altimeter.startRelativeAltitudeUpdatesToQueue(queue, withHandler: handleUpdate)
        altimeterActive = true
    }
    
    private func startPedometer() throws {
        print("SensorController: startPedometer")
        if !options.contains(.Pedometer) {
            throw SensorControllerError.ConfigurationConflict
        }
        assert( softwarePedometer == nil || hardwarePedometer == nil )
        if pedometerActive {
            print("SensorController: startPedometer: already active")
            return
        }
        
        pedometerDistance = 0.0
        pedometerSteps = UInt(0)
        
        if softwarePedometer != nil {
            print("\(options.contains(.Calibrated))")
            if deviceMotionAvailable && !deviceMotionActive {
                try! startDeviceMotion()
            } else if !deviceMotionAvailable && !accelerometerActive {
                try! startAccelerometer()
            }
            softwarePedometer.begin()
        } else if hardwarePedometer != nil {
            let queue: NSOperationQueue = queues[.Pedometer] ?? NSOperationQueue.currentQueue() ?? NSOperationQueue.mainQueue()
            hardwarePedometer.startPedometerUpdates(queue)
        }
        pedometerActive = true
    }
    
    
    func stop() {
        stop(self.options)
    }
    
    private func stop(options: SensorControllerOptions) {
        print("SensorController: stop");
        if options.contains(.Calibrated) {
            stopDeviceMotion()
        }
        if options.contains(.MagneticField) {
            stopMagnetometer()
        }
        if options.contains(.Acceleration) {
            stopAccelerometer()
        }
        if options.contains(.Rotation) {
            stopGyro()
        }
        if options.contains(.Altitude) {
            stopAltimeter()
        }
        if options.contains(.Pedometer) {
            stopPedometer()
        }
    }
    
    private func stopDeviceMotion() {
        print("SensorController: stopDeviceMotion");

        //See comment in startDeviceMotion()
//        if options.contains(.ReferenceTrueNorth) {
//            if CLLocationManager.significantLocationChangeMonitoringAvailable() {
//                print("SensorController: stopDeviceMotion: stopMonitoringSignificantLocationChanges")
//                locationManager.stopMonitoringSignificantLocationChanges()
//            } else {
//                print("SensorController: stopDeviceMotion: stopUpdatingLocation")
//                locationManager.stopUpdatingLocation()
//            }
//        }
        
        motionManager.stopDeviceMotionUpdates();
        deviceMotionActive = false
    }
    
    
    
    private func stopMagnetometer() { print("SensorController: stopMagnetometer"); motionManager.stopMagnetometerUpdates(); magnetometerActive = false }
    private func stopAccelerometer() { print("SensorController: stopAccelerometer"); motionManager.stopAccelerometerUpdates(); accelerometerActive = false }
    private func stopGyro() { print("SensorController: stopGyro"); motionManager.stopGyroUpdates(); gyroActive = false }
    private func stopAltimeter() { print("SensorController: stopAltimeter"); altimeter.stopRelativeAltitudeUpdates(); altimeterActive = false }
    private func stopPedometer() {
        print("SensorController: stopPedometer");
        if let pedometer = softwarePedometer {
            pedometer.end()
            if !options.contains(.Acceleration) && accelerometerActive {
                stopAccelerometer()
            } else if !options.contains(.Calibrated) && deviceMotionActive {
                stopDeviceMotion()
            }
        } else if let pedometer = hardwarePedometer {
            pedometer.stopPedometerUpdates()
        }
        pedometerActive = false
    }
    
    private func handleUpdate(magnetometerData: CMMagnetometerData?, error: NSError?) {
        var magneticData: MagneticData? = nil
        if magnetometerData != nil {
            if startTimestamps[.MagneticField] == nil {
                startTimestamps[.MagneticField] = magnetometerData!.timestamp
                lastTimestamps[.MagneticField] = magnetometerData!.timestamp
            }
            
            let field = MagneticField(x: magnetometerData!.magneticField.x, y: magnetometerData!.magneticField.y, z: magnetometerData!.magneticField.z)
            magneticData = MagneticData(field: field, timestamp: magnetometerData!.timestamp, accuracy: .Raw)
        }
        
        if let timestamp = magnetometerData?.timestamp {
            if !isEarlyUpdate(timestamp, source: .MagneticField) {
                lastTimestamps[.MagneticField] = timestamp
                notifyDelegates(magneticData, error: error)
            } else {
                return
            }
        } else if error != nil {
            notifyDelegates(magneticData, error: error)
        }
        
        
    }
    
    private func handleUpdate(accelerometerData: CMAccelerometerData?, error: NSError?) {
        var accelerationData: AccelerationData? = nil
        if accelerometerData != nil {
            if startTimestamps[.Acceleration] == nil {
                startTimestamps[.Acceleration] = accelerometerData!.timestamp
                lastTimestamps[.Acceleration] = accelerometerData!.timestamp
            }
            
            let acceleration = Acceleration(x: accelerometerData!.acceleration.x, y: accelerometerData!.acceleration.y, z: accelerometerData!.acceleration.z)
            accelerationData = AccelerationData(acceleration: acceleration, timestamp: accelerometerData!.timestamp, accuracy: .Raw)
        }
        
        if options.contains(.Acceleration) {
            if let timestamp = accelerationData?.timestamp {
                if !isEarlyUpdate(timestamp, source: .Acceleration) {
                    lastTimestamps[.Acceleration] = timestamp
                    notifyDelegates(accelerationData, error: error)
                } else {
                    return
                }
            } else if error != nil {
                notifyDelegates(accelerationData, error: error)
            }
            
        }
        
        if softwarePedometer != nil && softwarePedometer.isActive {
            let queue = queues[.Pedometer] ?? NSOperationQueue.mainQueue()
            SensorController.addBlockToQueue( queue,  block: { [weak self] () -> Void in
                    try! self?.softwarePedometer.processData(error, accelerationData: accelerationData)
                })
        }
    }
    
    private func handleUpdate(gyroData: CMGyroData?, error: NSError?) {
        var rotationData: RotationData? = nil
        if gyroData != nil {
            if startTimestamps[.Rotation] == nil {
                startTimestamps[.Rotation] = rotationData!.timestamp
                lastTimestamps[.Rotation] = rotationData!.timestamp
            }
            
            let rotation = Rotation(x: gyroData!.rotationRate.x, y: gyroData!.rotationRate.y, z: gyroData!.rotationRate.z)
            rotationData = RotationData(rotation: rotation, timestamp: gyroData!.timestamp, accuracy: .Raw)
        }
        
        if let timestamp = gyroData?.timestamp {
            if timestamp - lastTimestamps[.Rotation]! < updateIntervals[.Rotation]! {
                return
            } else {
                lastTimestamps[.Rotation] = timestamp
                notifyDelegates(rotationData, error: error)
            }
        } else if error != nil {
            notifyDelegates(rotationData, error: error)
        }
    
        
    }
    
    private func handleUpdate(altimeterData: CMAltitudeData?, error: NSError?) {
        var altitudeData: AltitudeData? = nil
        if altimeterData != nil {
            if startTimestamps[.Altitude] == nil {
                startTimestamps[.Altitude] = altimeterData!.timestamp
                lastTimestamps[.Altitude] = altimeterData!.timestamp
            }
            altitudeData = AltitudeData(altitude: Double(altimeterData!.relativeAltitude), timestamp: altimeterData!.timestamp, accuracy: .Raw)
        }
        
        if let timestamp = altitudeData?.timestamp {
            if timestamp - lastTimestamps[.Altitude]! < updateIntervals[.Altitude]! {
                return
            } else {
                lastTimestamps[.Altitude] = timestamp
                notifyDelegates(altitudeData, error: error)
            }
        } else if error != nil {
            notifyDelegates(altitudeData, error: error)
        }
        
    }
    
    private func handleUpdate(deviceMotionData: CMDeviceMotion?, error: NSError?) {
//        print("handleUpdate: deviceMotionData")
            var magneticData: MagneticData? = nil
            var accelerationData: AccelerationData? = nil
            var rotationData: RotationData? = nil
            var attitudeData: AttitudeData? = nil
        
//            print("deviceMotionActive \(motionManager.deviceMotionActive)")
        
            if deviceMotionData != nil {
                let accuracy: Accuracy
                
//                print("accuracy = \(deviceMotionData!.MagneticFieldField.accuracy.rawValue)")
                
                if deviceMotionData!.magneticField.accuracy.rawValue <  CMMagneticFieldCalibrationAccuracyLow.rawValue {
                    accuracy = Accuracy.Uncalibrated
                } else if deviceMotionData!.magneticField.accuracy.rawValue < CMMagneticFieldCalibrationAccuracyMedium.rawValue {
                    accuracy = Accuracy.Low
                } else if deviceMotionData!.magneticField.accuracy.rawValue < CMMagneticFieldCalibrationAccuracyHigh.rawValue {
                    accuracy = Accuracy.Medium
                } else {
                    accuracy = Accuracy.High
                }
                
                let field = MagneticField(x: deviceMotionData!.magneticField.field.x, y: deviceMotionData!.magneticField.field.y, z: deviceMotionData!.magneticField.field.z)
                magneticData = MagneticData(field: field, timestamp: deviceMotionData!.timestamp, accuracy: accuracy)
            
                let acceleration = Acceleration(x: deviceMotionData!.userAcceleration.x, y: deviceMotionData!.userAcceleration.y, z: deviceMotionData!.userAcceleration.z)
                accelerationData = AccelerationData(acceleration: acceleration, timestamp: deviceMotionData!.timestamp, accuracy: accuracy)
                
                let rotation = Rotation(x: deviceMotionData!.rotationRate.x, y: deviceMotionData!.rotationRate.y, z: deviceMotionData!.rotationRate.z)
                rotationData = RotationData(rotation: rotation, timestamp: deviceMotionData!.timestamp, accuracy: accuracy)
                
                attitudeData = AttitudeData(attitude: deviceMotionData!.attitude, timestamp: deviceMotionData!.timestamp, accuracy: accuracy)
                
                if startTimestamps[.Attitude] == nil {
                    startTimestamps[.Attitude] = deviceMotionData!.timestamp
                    lastTimestamps[.Attitude] = deviceMotionData!.timestamp
                }
                if startTimestamps[.MagneticFieldCalibrated] == nil {
                    startTimestamps[.MagneticFieldCalibrated] = deviceMotionData!.timestamp
                    lastTimestamps[.MagneticFieldCalibrated] = deviceMotionData!.timestamp
                }
                if startTimestamps[.AccelerationCalibrated] == nil {
                    startTimestamps[.AccelerationCalibrated] = deviceMotionData!.timestamp
                    lastTimestamps[.AccelerationCalibrated] = deviceMotionData!.timestamp
                }
                if startTimestamps[.RotationCalibrated] == nil {
                    startTimestamps[.RotationCalibrated] = deviceMotionData!.timestamp
                    lastTimestamps[.RotationCalibrated] = deviceMotionData!.timestamp
                }
        } else if error != nil {
            notifyDelegates(attitudeData, error: error)
            notifyDelegates(magneticData, error: error)
            notifyDelegates(accelerationData, error: error)
            notifyDelegates(rotationData, error: error)
            return
        }
        
        if options.contains([.Attitude]) {
            if let timestamp = deviceMotionData?.timestamp {
                if timestamp - lastTimestamps[.Attitude]! >= updateIntervals[.Attitude]! {
                    lastTimestamps[.Attitude] = timestamp
                    notifyDelegates(attitudeData, error: error)
                }
            }
        }
        if options.contains(.MagneticFieldCalibrated) {
            if let timestamp = deviceMotionData?.timestamp {
                if timestamp - lastTimestamps[.MagneticFieldCalibrated]! >= updateIntervals[.MagneticFieldCalibrated]! {
                    lastTimestamps[.MagneticFieldCalibrated] = timestamp
                    notifyDelegates(magneticData, error: error)
                }
            }
        }
        if options.contains(.AccelerationCalibrated) {
            if let timestamp = deviceMotionData?.timestamp {
                if timestamp - lastTimestamps[.AccelerationCalibrated]! >= updateIntervals[.AccelerationCalibrated]! {
                    lastTimestamps[.AccelerationCalibrated] = timestamp
                    notifyDelegates(accelerationData, error: error)
                }
            }
        }
        if options.contains(.RotationCalibrated) {
            if let timestamp = deviceMotionData?.timestamp {
                if timestamp - lastTimestamps[.RotationCalibrated]! >= updateIntervals[.RotationCalibrated]! {
                    lastTimestamps[.RotationCalibrated] = timestamp
                    notifyDelegates(rotationData, error: error)
                }
            }
            
        }
        if softwarePedometer != nil && softwarePedometer.isActive {
            let queue = queues[.Pedometer] ?? NSOperationQueue.mainQueue()
            SensorController.addBlockToQueue(queue, block: { [weak self] () -> Void in
                try! self?.softwarePedometer.processData(error, accelerationData: accelerationData)
            })
        }
        
    }
    
    private var pedometerDistance: Double = 0
    private var pedometerSteps: UInt = 0
    func handleUpdate(data: StepData?, error: NSError?) {
        var pedometerData: PedometerData? = nil
        if data != nil {
            if startTimestamps[.Pedometer] == nil {
                startTimestamps[.Pedometer] = data!.timestamp
            }
            let steps = data!.steps - pedometerSteps
            pedometerSteps = data!.steps
            
            let dist = data!.distance - pedometerDistance
            pedometerDistance = data!.distance
            
            pedometerData = PedometerData(steps: steps, distance: dist, timestamp: data!.timestamp, accuracy: .Raw)
        }
        notifyDelegates(pedometerData, error: error)
    }
    
    private func isEarlyUpdate(timestamp: Double, source: Source) -> Bool {
        if abs(timestamp - lastTimestamps[source]! - updateIntervals[source]!) < 1e-3 {
            return true
        } else {
            return false
        }
    }
    
    private func notifyDelegates(data: SensorData?, error: NSError?) {
        for delegate in delegates {
            if let magneticData = data as? MagneticData {
                delegate.handleUpdate?(updateError: error, magneticData: magneticData)
            } else if let accelerationData = data as? AccelerationData {
                delegate.handleUpdate?(updateError: error, accelerationData: accelerationData)
            } else if let rotationData = data as? RotationData {
                delegate.handleUpdate?(updateError: error, rotationData: rotationData)
            } else if let attitudeData = data as? AttitudeData {
                delegate.handleUpdate?(updateError: error, attitudeData: attitudeData)
            } else if let altitudeData = data as? AltitudeData {
                delegate.handleUpdate?(updateError: error, altitudeData: altitudeData)
            } else if let pedometerData = data as? PedometerData {
                delegate.handleUpdate?(updateError: error, pedometerData: pedometerData)
            }
            
        }
    }
    
    private static func isMainQueue() -> Bool { return NSThread.isMainThread() && NSOperationQueue.currentQueue() == NSOperationQueue.mainQueue() }
    
    private static func addBlockToQueue(queue: NSOperationQueue, block: () -> Void) {
        if ( !isMainQueue() && queue == NSOperationQueue.mainQueue() ) || ( queue != NSOperationQueue.currentQueue() )
        {
            queue.addOperationWithBlock(block)
        } else {
            block()
        }
        
        

        
    }
    
}
