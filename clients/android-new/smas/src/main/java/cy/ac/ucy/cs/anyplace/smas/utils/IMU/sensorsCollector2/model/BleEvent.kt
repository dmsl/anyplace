package cy.ac.ucy.cs.anyplace.smas.utils.IMU.sensorsCollector2.model

import android.bluetooth.le.ScanResult

data class BleEvent(val scanResult: ScanResult, val major: Int, val minor: Int)

val TYPE_BLE_EVENT = 0x1
val TYPE_SENSOR_EVENT = 0x2
val TYPE_GENERATED_EVENT = 0x3