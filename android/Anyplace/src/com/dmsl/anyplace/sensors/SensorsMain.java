/*
* AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Author(s): Timotheos Constambeys, Lambros Petrou
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

package com.dmsl.anyplace.sensors;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

/**
 * The Positioning class handles the sensor data reception from sensor fusion
 * and the pedometer step detection. It uses the re-implemented dead-reckoning
 * object and map matching to determine the current position of the user on the
 * map.
 * 
 * @author Jens Nitzschke, Beyonav, Lambros Petrou (UCY)
 */
public class SensorsMain implements SensorEventListener {
	private static final String TAG = "Positioning";

	// Orientation
	public interface IOrientationListener {
		public void onNewOrientation(float[] values);
	}

	private List<IOrientationListener> olisteners = new ArrayList<IOrientationListener>(2);

	public void addListener(IOrientationListener list) {
		if (!olisteners.contains(list))
			olisteners.add(list);
	}

	public void removeListener(IOrientationListener list) {
		olisteners.remove(list);
	}

	private void triggerOrientationListeners(float[] values) {
		for (IOrientationListener l : olisteners) {
			l.onNewOrientation(values);
		}
	}

	// Acceleration
	public interface IAccelerometerListener {
		public void onNewAccelerometer(float[] values);
	}

	private List<IAccelerometerListener> alisteners = new ArrayList<IAccelerometerListener>(2);

	public void addListener(IAccelerometerListener list) {
		alisteners.add(list);
	}

	public void removeListener(IAccelerometerListener list) {
		alisteners.remove(list);
	}

	private void triggerAccelerometerListeners(float[] values) {
		for (IAccelerometerListener l : alisteners) {
			l.onNewAccelerometer(values);
		}
	}

	private SensorManager mSensorManager = null;

	private boolean isPositioningOn = false;
	// for handling rotation sensor
	private float rotationMatrix[] = new float[9];
	private float rotationAngle = (float) (180.0 / Math.PI);
	private int mDataReady;
	private UserData mUserData;

	/**
	 * Constructor
	 * 
	 * @param engine
	 *            the engine object this positioning object belongs to
	 * @param context
	 *            the activity it belongs too.
	 */
	public SensorsMain(Context context) {
		mUserData = new UserData();

		// get the sensor manager
		mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
	}

	/**
	 * Pause the position processing
	 */
	public void pause() {
		if (isPositioningOn) {
			Log.i(TAG, "pause");
			isPositioningOn = false;
			mSensorManager.unregisterListener(this);
		}
	}

	/**
	 * Resume the position processing
	 */
	public void resume() {
		if (!isPositioningOn) {
			Log.i(TAG, "resume");
			isPositioningOn = true;
			registerSensors();
		}
	}
	
	// register sensors
	private void registerSensors() {
		// get orientation sensor
		Sensor orientation = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

		// check if the device has a default orientation sensor
		if (orientation == null) {
			Log.e(TAG, "No default orientation sensor. Giving up sensor registration");
			return;
		}

		// check if sensor fusion is installed
		if (orientation.getName() != "CWGD 9-axis Orientation Sensor") {
			Log.i(TAG, "Before loop, orientation is: " + orientation.getName());
			List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ORIENTATION);
			for (Sensor s : sensors) {
				// Log.i(TAG, "Orientation sensor: " + s.getName());
				if (s.getName().equals("CWGD 9-axis Orientation Sensor")) {
					orientation = s;
					break;
				}
			}
		}

		if (orientation != null) {
			mSensorManager.registerListener(this, orientation, SensorManager.SENSOR_DELAY_FASTEST);
			Log.i(TAG, "Orientation sensor: " + orientation.getName());
		}

		/*Sensor gyroscope = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
		if (gyroscope != null) {
			mSensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
			Log.i(TAG, "Gyroscope: " + gyroscope.getName());
		}*/

		Sensor acc = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		if (acc != null) {
			mSensorManager.registerListener(this, acc, SensorManager.SENSOR_DELAY_FASTEST);
			Log.i(TAG, "Accelerometer: " + acc.getName());
		}

		/*Sensor mag = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
		if (mag != null) {
			mSensorManager.registerListener(this, mag, SensorManager.SENSOR_DELAY_GAME);
			Log.i(TAG, "Magnetic field: " + mag.getName());
		}*/

	}

	/**
	 * @see android.hardware.SensorEventListener#onSensorChanged(android.hardware.SensorEvent)
	 */
	@Override
	public void onSensorChanged(SensorEvent event) {
		// Log.i(TAG, "onSensorChanged: " + event.sensor.getType());

		switch (event.sensor.getType()) {
		case Sensor.TYPE_ROTATION_VECTOR: {
			SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);
			SensorManager.getOrientation(rotationMatrix, event.values);
			for (int i = 0; i < 3; i++) {
				event.values[i] *= rotationAngle;
			}
			// no break, fall through
		}

		case Sensor.TYPE_ORIENTATION:
			mUserData.ori[0] = event.values[0];
			mUserData.ori[1] = event.values[1];
			mUserData.ori[2] = event.values[2];
			mDataReady |= 1;
			triggerOrientationListeners(mUserData.ori);
			break;

		case Sensor.TYPE_GYROSCOPE:
			mUserData.gyr[0] = event.values[0];
			mUserData.gyr[1] = event.values[1];
			mUserData.gyr[2] = event.values[2];
			mDataReady |= 2;
			break;

		case Sensor.TYPE_ACCELEROMETER:
			mUserData.acc[0] = event.values[0];
			mUserData.acc[1] = event.values[1];
			mUserData.acc[2] = event.values[2];
			mDataReady |= 4;
			triggerAccelerometerListeners(mUserData.acc);
			break;

		case Sensor.TYPE_MAGNETIC_FIELD:
			mUserData.mag[0] = event.values[0];
			mUserData.mag[1] = event.values[1];
			mUserData.mag[2] = event.values[2];
			mDataReady |= 8;
			break;
		}

	}

	/**
	 * @see android.hardware.SensorEventListener#onAccuracyChanged(android.hardware.Sensor,
	 *      int)
	 */
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
	}

	/**
	 * Get Unfiltered heading
	 * 
	 * @return the current heading in degrees
	 */
	public float getRAWHeading() {
		return mUserData.ori[0];
	}

	/**
	 * Get MagnetismInformation
	 * 
	 * @return the current magnetism information
	 */

	public float[] getMagnetism() {
		float[] magInfo = { mUserData.mag[0], mUserData.mag[1], mUserData.mag[2] };
		return magInfo;
	}

	private static class UserData {

		/**
		 * Accelerometer sensor data
		 */
		public float[] acc = new float[3];

		/**
		 * Magnetic sensor data
		 */
		public float[] mag = new float[3];

		/**
		 * Gyro sensor data
		 */
		public float[] gyr = new float[3];

		/**
		 * Orientation sensor data
		 */
		public float[] ori = new float[3];

		/**
		 * Constructor
		 */
		public UserData() {
			ori[0] = 0;
			ori[1] = 0;
			ori[2] = 0;
			gyr[0] = 0;
			gyr[1] = 0;
			gyr[2] = 0;
			acc[0] = 0;
			acc[1] = 0;
			acc[2] = 0;
			mag[0] = 0;
			mag[1] = 0;
			mag[2] = 0;
		}

		/**
		 * Clone this data set
		 * 
		 * @return a new object with the same user data
		 */
		public UserData clone() {
			UserData data = new UserData();
			data.ori[0] = ori[0];
			data.ori[1] = ori[1];
			data.ori[2] = ori[2];
			data.gyr[0] = gyr[0];
			data.gyr[1] = gyr[1];
			data.gyr[2] = gyr[2];
			data.acc[0] = acc[0];
			data.acc[1] = acc[1];
			data.acc[2] = acc[2];
			data.mag[0] = mag[0];
			data.mag[1] = mag[1];
			data.mag[2] = mag[2];
			return data;
		}
	}
}
