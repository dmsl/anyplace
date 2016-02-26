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

@available(iOS 7, *)
class HardwarePedometer {
    private var sensor: AnyObject! = nil
    
    private var queue: NSOperationQueue! = nil
    
    var isActive: Bool = false
    
    private static let STEP_LENGTH_M: Double = 0.75;
    
    var delegate: StepCounterDelegate! = nil
    
    private static var isStepCountingAvailable: Bool {
        if #available(iOS 8, *) {
            return CMPedometer.isStepCountingAvailable()
        } else if #available(iOS 7, *) {
            return CMStepCounter.isStepCountingAvailable()
        } else {
            return false
        }
    }
    
    static var isAvailable: Bool { return HardwarePedometer.isStepCountingAvailable }
    
    init?() {
        if !HardwarePedometer.isAvailable {
            return nil
        } else {
            if #available(iOS 8, *) {
                sensor = CMPedometer()
            } else if #available(iOS 7, *) {
                sensor = CMStepCounter()
            }
        }
    }
    
    
    func startPedometerUpdates(queue: NSOperationQueue) {
        assert(delegate != nil)
        startTime = nil
        
        self.queue = queue
        
        if #available(iOS 8, *) {
            (sensor as! CMPedometer).startPedometerUpdatesFromDate(NSDate(), withHandler: handleUpdate)
        } else if #available(iOS 7, *)  {
            (sensor as! CMStepCounter).startStepCountingUpdatesToQueue(self.queue, updateOn: 1, withHandler: handleUpdate)
        }
        
        isActive = true
    }
    
    func stopPedometerUpdates() {
        if #available(iOS 8, *) {
            (sensor as! CMPedometer).stopPedometerUpdates()
        } else if #available(iOS 7, *)  {
            (sensor as! CMStepCounter).stopStepCountingUpdates()
        }
        
        isActive = false
    }
    
    private var startTime: Double? = nil
    @available(iOS 8, *)
    private func handleUpdate(data: CMPedometerData?, error: NSError?) {
        var stepData: StepData! = nil
        if data != nil {
            if startTime == nil {
                startTime = data!.startDate.timeIntervalSince1970
            }
            let steps: UInt = UInt(data!.numberOfSteps)
            let dist: Double
            if data!.distance != nil {
                dist = Double(data!.distance!)
            } else {
                dist = Double(steps)*HardwarePedometer.STEP_LENGTH_M
            }
            
            stepData = StepData(steps: steps, distance: dist, timestamp: data!.startDate.timeIntervalSince1970)
        }
        if NSOperationQueue.currentQueue() != nil && NSOperationQueue.currentQueue() != queue {
            queue.addOperationWithBlock({ [weak self] () -> Void in
                self?.delegate?.handleUpdate(stepData, error: error)
            })
        } else {
            delegate?.handleUpdate(stepData, error: error)
        }
    }
    
    @available(iOS 7, *)
    private func handleUpdate(steps: Int, date: NSDate, error: NSError?) {
        if startTime == nil {
            startTime = date.timeIntervalSince1970
        }
        let dist = Double(steps)*HardwarePedometer.STEP_LENGTH_M
            
        let stepData = StepData(steps: UInt(steps), distance: dist, timestamp: date.timeIntervalSince1970)
        delegate?.handleUpdate(stepData, error: error)
    }
}