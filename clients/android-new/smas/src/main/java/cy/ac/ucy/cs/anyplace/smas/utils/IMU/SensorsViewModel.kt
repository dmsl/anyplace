package cy.ac.ucy.cs.anyplace.smas.utils.IMU

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import cy.ac.ucy.cs.anyplace.smas.utils.IMU.sensorsCollector2.collector.DataEvent
import cy.ac.ucy.cs.anyplace.smas.utils.IMU.sensorsCollector2.model.TYPE_SENSOR_EVENT
import cy.ac.ucy.cs.anyplace.smas.utils.IMU.sensorsCollector2.step.IStepDetector
import cy.ac.ucy.cs.anyplace.smas.utils.IMU.sensorsCollector2.step.StaticAccMagnitudeStepDetector

/**
 * This class is used to register the listeners for the sensors and
 * manage the changes on their values.
 */
class SensorsViewModel(application: Application): AndroidViewModel(application), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    // This stepDetector uses an open source project that utilizes the accelerometer for detecting steps
    var stepDetector: IStepDetector = StaticAccMagnitudeStepDetector()

    var stepsDetected = MutableLiveData<Int>(0)
    var stepsDetectedSensorsCollector2 = MutableLiveData<Int>(0)
    var azimuthRotationVector = MutableLiveData<Double>(0.0)

    companion object{
        val TAG = "mSensors"
    }

    fun registerListeners(){
        sensorManager = getApplication<Application>().getSystemService(Context.SENSOR_SERVICE)
                as SensorManager

        //Register step detector sensor listener
        val stepDetectorSensor : Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        if(stepDetectorSensor == null){
            Toast.makeText(getApplication(),
                "No step detector sensor detected in this device", Toast.LENGTH_SHORT).show()
        }else{
            sensorManager.registerListener(this,stepDetectorSensor,SensorManager.SENSOR_DELAY_FASTEST)
        }

        //Register rotation vector sensor listener
        val rotationSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
        if (rotationSensor == null){
            Toast.makeText(getApplication(),
                "No rotation vector sensor detected in this device", Toast.LENGTH_SHORT).show()
        }else{
            sensorManager.registerListener(this,rotationSensor,SensorManager.SENSOR_DELAY_FASTEST)
        }

        //Register accelerometer sensor listener
        val accelerometerSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometerSensor == null){
            Toast.makeText(getApplication(),
                "No accelerometer sensor detected in this device", Toast.LENGTH_SHORT).show()
        }else{
            sensorManager.registerListener(this,accelerometerSensor,SensorManager.SENSOR_DELAY_FASTEST)
        }
    }

    fun unregisterListener(){
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {

        //Steps detected with accelerometer (using sensorscollector2 project)
        if (event == null) return
        stepDetector.updateWithDataEvent(DataEvent(TYPE_SENSOR_EVENT, event))
        if (stepDetector.isStepDetected()){
            Log.d(TAG, "Step detected from accelerometer")
            stepsDetectedSensorsCollector2.postValue(stepsDetectedSensorsCollector2.value?.plus(1)?:0)
        }

        //Step detector sensor event
        if (event?.sensor?.type == Sensor.TYPE_STEP_DETECTOR){
            Log.d(TAG, "Step detected from step detector sensor");
            stepsDetected.postValue(stepsDetected.value?.plus(1) ?: 0)
        }

        //Azimuth from the rotation vector
        if (event?.sensor?.type == Sensor.TYPE_ROTATION_VECTOR){
            val vector: FloatArray = event.values
            val targetMatrix = FloatArray(16)
            var orientation = FloatArray(3)

            //Converts the vector into a rotation matrix
            SensorManager.getRotationMatrixFromVector(targetMatrix, vector)
            //Converts the rotation matrix into a set of radian values
            SensorManager.getOrientation(targetMatrix, orientation)
            var azimuth = Math.toDegrees(orientation[0].toDouble())
            if (azimuth < 0)
                azimuth += 360
            azimuthRotationVector.postValue(azimuth)
        }

    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}