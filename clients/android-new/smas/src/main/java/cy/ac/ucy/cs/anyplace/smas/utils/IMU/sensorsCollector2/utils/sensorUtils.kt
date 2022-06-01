package cy.ac.ucy.cs.anyplace.smas.utils.IMU.sensorsCollector2.utils

import android.annotation.SuppressLint
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.util.*

fun registerSensors(
    sensorTypes: List<Int>,
    manager: SensorManager,
    listener: SensorEventListener,
    sensorDelay: Int = SensorManager.SENSOR_DELAY_FASTEST
) {
    for(sensorType in sensorTypes) {
        val sensor = manager.getDefaultSensor(sensorType)
        sensor?.also { it ->
            manager.registerListener(listener, it, sensorDelay)
        }
    }
}

@SuppressLint("SimpleDateFormat")
fun getTimeStringFromCurrentTimeMillis(currentTimeMillis : Long) : String {
    val format = java.text.SimpleDateFormat("yyyy-MM-dd-HH-mm-ss")
    return format.format(Date(currentTimeMillis))
}