package cy.ac.ucy.cs.anyplace.smas.utils.IMU.sensorsCollector2.step

import cy.ac.ucy.cs.anyplace.smas.utils.IMU.sensorsCollector2.collector.DataEvent

interface IStepDetector {
    fun updateWithDataEvent(event : DataEvent): DataEvent?
    fun isStepDetected() : Boolean
    fun lastDetectedTimestamp() : Long
}