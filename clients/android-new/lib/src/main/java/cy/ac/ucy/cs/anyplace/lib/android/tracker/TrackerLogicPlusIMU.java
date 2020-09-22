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

package cy.ac.ucy.cs.anyplace.lib.android.tracker;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

import cy.ac.ucy.cs.anyplace.lib.android.sensors.MovementDetector;
import cy.ac.ucy.cs.anyplace.lib.android.sensors.SensorsMain;
import cy.ac.ucy.cs.anyplace.lib.android.sensors.SensorsStepCounter;
import cy.ac.ucy.cs.anyplace.lib.android.tracker.IMU.TrackedLocAnyplaceIMUListener;
import com.google.android.gms.maps.model.LatLng;

//On Walking => IMU  + Reset After T by Tracker + Kalman Filter
//On Standing  => Median of Tracker Locations
public class TrackerLogicPlusIMU extends AnyplaceTracker {

	private static final int IMU_RESET_TIME = 20000;

	private List<TrackedLocAnyplaceTrackerListener> tllisteners = new ArrayList<TrackedLocAnyplaceTrackerListener>();

	public void addListener(TrackedLocAnyplaceTrackerListener list) {
		tllisteners.add(list);
	}

	public void removeListener(TrackedLocAnyplaceTrackerListener list) {
		tllisteners.remove(list);
	}

	private void triggerTrackedLocListeners(LatLng pos) {
		for (TrackedLocAnyplaceTrackerListener l : tllisteners) {
			l.onNewLocation(pos);
		}
	}

	// AnyplaceTracker
	private boolean reset = false;
	private KalmanFilter kalmanFilter;
	private RunningMedian runningMedian;
	// </IMU
	private IMU imu;
	private Long lastIMUresetTimestamp;
	private LatLng resetIMUPoint;
	private boolean resetIMU = false;
	// />
	private boolean walkingTracker_old = false;
	private boolean walking = false;

	public TrackerLogicPlusIMU(MovementDetector movementDetector, SensorsMain sensorsMain, SensorsStepCounter sensorsStep, Context ctx) {
		super(sensorsMain, ctx);
		
		// Add listeners from MOVEMENT DETECTOR, TRACKER
		movementDetector.addStepListener(new WalkingListener());
		super.addListener(new TrackerListenerInit(sensorsMain, sensorsStep));
	}

	private class WalkingListener implements MovementDetector.MovementListener {

		@Override
		public void onWalking() {
			walking = true;
		}

		@Override
		public void onStanding() {
			// TODO Auto-generated method stub
			walking = false;
		}

	}

	private class TrackerListenerInit implements AnyplaceTracker.TrackedLocAnyplaceTrackerListener {

		SensorsMain sensorsMain;
		SensorsStepCounter sensorsStep;

		TrackerListenerInit(SensorsMain sensorsMain, SensorsStepCounter sensorsStep) {
			this.sensorsMain = sensorsMain;
			this.sensorsStep = sensorsStep;
		}

		@Override
		public void onNewLocation(LatLng pos) {
			// Call this method only for Initialisation
			TrackerLogicPlusIMU.super.removeListener(this);
			TrackerLogicPlusIMU.super.addListener(new TrackerListener());

			kalmanFilter = new KalmanFilter(pos.latitude, pos.longitude);
			runningMedian = new RunningMedian(pos.latitude, pos.longitude);
			imu = new IMU(sensorsMain, sensorsStep, pos.latitude, pos.longitude);
			lastIMUresetTimestamp = System.currentTimeMillis();

			resetIMU = walking;
			resetIMUPoint = pos;
			triggerTrackedLocListeners(pos);

			imu.addListener(new IMUListener());

		}

	}

	// Callback from Tracker
	private class TrackerListener implements AnyplaceTracker.TrackedLocAnyplaceTrackerListener {

		// RUNS IN BACKGROUND THREAD
		@Override
		public void onNewLocation(LatLng pos) {
			if (reset) {
				reset = false;
				kalmanFilter.reset(pos.latitude, pos.longitude);
				runningMedian.reset(pos.latitude, pos.longitude);
				imu.reset(pos.latitude, pos.longitude);
			}

			
			if (walking) { // Walking
				// Reset Kalman filter if Standing Update Triggered
				if (walkingTracker_old != true) {
					walkingTracker_old = true;
					kalmanFilter.reset(pos.latitude, pos.longitude);
				} else {
					pos = kalmanFilter.update(pos.latitude, pos.longitude);
				}

				long timestamp = System.currentTimeMillis();
				if (timestamp - lastIMUresetTimestamp > IMU_RESET_TIME) {
					imu.reset(pos.latitude, pos.longitude);
					lastIMUresetTimestamp = timestamp;
				}

			} else {// Standing
				if (walkingTracker_old != false) {
					walkingTracker_old = false;
					runningMedian.reset(pos.latitude, pos.longitude);
					resetIMUPoint = pos;
				} else {
					resetIMUPoint = runningMedian.update(pos.latitude, pos.longitude);
				}
				resetIMU = true;
				triggerTrackedLocListeners(resetIMUPoint);
			}

		}

	}

	// Callback from IMU
	private class IMUListener implements TrackedLocAnyplaceIMUListener {

		// Runs on UI THREAD
		@Override
		public void onNewLocation(LatLng pos) {

			if (walking) {
				if (resetIMU == true) {
					// User was standing still and now moves
					resetIMU = false;
					imu.reset(resetIMUPoint.latitude, resetIMUPoint.longitude);
					lastIMUresetTimestamp = System.currentTimeMillis();
				} else {
					triggerTrackedLocListeners(pos);
				}
			}
		}

	}

	@Override
	public void trackOff() {
		// Wait before Change listener because a separate thread may change
		// it!!!!
		super.trackOff();
		reset = true;
	}
	
	@Override
	public void setAlgorithm(String name) {
		super.setAlgorithm(name);
		reset = true;
	}

	public void reset() {
		reset = true;
	}
	
}
