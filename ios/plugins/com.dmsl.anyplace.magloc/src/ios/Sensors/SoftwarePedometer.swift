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


typealias AccelerationData = SensorController.AccelerationData

class SoftwarePedometer: SensorControllerDelegate {

    enum Error: ErrorType {
        case InvalidState
    }
    
    var delegate: StepCounterDelegate! = nil
    
    static let BEST_UPDATE_INTERVAL: Double = 0.0
    private static let STEP_LENGTH_M: Double = 0.75;
    private static let EARTH_GRAVITY_SI = 9.81
    
    private var active: Bool = false
    var isActive: Bool { return active }
    
    func begin() {
        lastValues = 0.0
        lastDirections = 0.0
        lastExtremes = [0.0, 0.0]
        lastDiff = 0.0
        lastMatch = -1
        steps = 0
        
        active = true
    }
    
    func end() {
        active = false
    }
    
    private let yOffset: Double = 480*0.5
    private let limit: Double = 1.97
    
    private var lastValues: Double = 0.0
    private var lastDirections: Double = 0.0
    private var lastExtremes: [Double] = [0.0, 0.0]
    private var lastDiff: Double = 0.0
    private var lastMatch: Int = -1
    
    private var steps: Int = 0
    
    private func checkIfStep(acceleration: Acceleration) -> Bool {
        let acc_si = acceleration*SoftwarePedometer.EARTH_GRAVITY_SI
        var step = false
        let vSum = acc_si.x + acc_si.y + acc_si.z
        let v = yOffset - vSum * 4.0 / 3.0
        let direction: Double = ( v > lastValues ? 1 : ( v < lastValues ? -1 : 0 ) )
        if direction == -lastDirections {
            //Direction changed
            let extType: Int = ( direction > 0 ? 0 : 1 ) //Minimum or maximum?
            lastExtremes[extType] = lastValues
            let diff = abs( lastExtremes[extType] - lastExtremes[1 - extType] )
            if (diff > limit) {
                let isAlmostAsLargeAsPrevious: Bool = diff > ( lastDiff * 2 / 3 )
                let isPreviousLargeEnough: Bool = lastDiff > ( diff / 3 )
                let isNotContra: Bool = ( lastMatch != 1 - extType )
                if isAlmostAsLargeAsPrevious && isPreviousLargeEnough && isNotContra {
                    steps += 1
                    step = true
                    lastMatch = extType
                } else {
                    lastMatch = -1
                }
            }
            lastDiff = diff
        }
        lastDirections = direction
        lastValues = v
        return step
    }
    
    func processData(error: NSError?, accelerationData data: AccelerationData?) throws {
//        print("processData error = \(error) data = \(data)")
        if !active {
            throw Error.InvalidState
        }
        
        var stepData: StepData! = nil
        if data != nil {
            if checkIfStep(data!.acceleration) {
                stepData = StepData(steps: UInt(steps), distance: Double(steps)*SoftwarePedometer.STEP_LENGTH_M, timestamp: data!.timestamp)
            } else {
                return
            }
        }
        delegate.handleUpdate(stepData, error: error)
    }
    
}