/*
* AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Author(s): Timotheos Constambeys
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

package cy.ac.ucy.cs.anyplace.lib.android.sensors;

import java.util.ArrayList;
import java.util.List;


import cy.ac.ucy.cs.anyplace.lib.android.sensors.SensorsMain.IAccelerometerListener;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class SensorsStepCounter implements SensorEventListener, IAccelerometerListener {

	private static final String TAG = "Step Counter";
	private static final float SOFTWARE_LENGTH_KM = 0.00030f;
	private static final float HARDWARE_LENGTH_KM = 0.00055f;
	
	private final boolean forceSoftwareSensor = false;
	
	// Orientation
	public interface IStepListener {
		public void onNewStep(float value);
	}

	private List<IStepListener> stepListeners = new ArrayList<IStepListener>(1);

	public void addListener(IStepListener list) {
		if (!stepListeners.contains(list))
			stepListeners.add(list);
	}

	public void removeListener(IStepListener list) {
		stepListeners.remove(list);
	}

	private void triggerStepListeners(float value) {
		for (IStepListener l : stepListeners) {
			l.onNewStep(value);
		}
	}

	private SoftwareStepCounter softwareSensor = null;
	private SensorsMain sensorsMain;
	private SensorManager mSensorManager = null;
	private boolean isPositioningOn = false;
	private UserData mUserData;

	/**
	 * Constructor
	 * 
	 * @param sensorsMain
	 *            the engine object this positioning object belongs to
	 * @param context
	 *            the activity it belongs too.
	 */
	public SensorsStepCounter(Context context, SensorsMain sensorsMain) {
		mUserData = new UserData();

		// get the sensor manager
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);

		PackageManager myPackageManager = context.getPackageManager();

		if (forceSoftwareSensor || !myPackageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_STEP_COUNTER)) {
			softwareSensor = new SoftwareStepCounter();
			this.sensorsMain = sensorsMain;
		}

	}

	public float getStepLength()
	{
		if (softwareSensor == null) {
			return HARDWARE_LENGTH_KM;
		} else {
			return SOFTWARE_LENGTH_KM;
		}
	}
	
	/**
	 * Pause the position processing
	 */
	public void pause() {
		if (isPositioningOn) {
			Log.i(TAG, "pause");
			isPositioningOn = false;
			if (softwareSensor == null) {
				mSensorManager.unregisterListener(this);
			} else {
				sensorsMain.removeListener((IAccelerometerListener) this);
			}
		}
	}

	/**
	 * Resume the position processing
	 */
	public void resume() {
		if (!isPositioningOn) {
			Log.i(TAG, "resume");
			isPositioningOn = true;

			if (softwareSensor == null) {
				registerSensors();
			} else {
				sensorsMain.addListener((IAccelerometerListener) this);
			}
		}
	}

	// register sensors
	private void registerSensors() {

		Sensor countSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
		if (countSensor != null) {
			mSensorManager.registerListener(this, countSensor, SensorManager.SENSOR_DELAY_UI);
		}
	}

	/**
	 * @see SensorEventListener#onSensorChanged(SensorEvent)
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		// Log.i(TAG, "onSensorChanged: " + event.sensor.getType());

		switch (event.sensor.getType()) {

		case Sensor.TYPE_STEP_COUNTER:
			mUserData.step = event.values[0];
			triggerStepListeners(mUserData.step);
			break;

		}

	}

	@Override
	public void onNewAccelerometer(float[] values) {
		if (softwareSensor.checkIfStep(values)) {
			mUserData.step = softwareSensor.steps;
			triggerStepListeners(mUserData.step);
		}
	}

	/**
	 * @see SensorEventListener#onAccuracyChanged(Sensor,
	 *      int)
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	private static class UserData {

		public float step;

		public UserData() {
			step = 0;
		}

		/**
		 * Clone this data set
		 * 
		 * @return a new object with the same user data
		 */
		public UserData clone() {
			UserData data = new UserData();
			data.step = step;
			return data;
		}
	}

}
