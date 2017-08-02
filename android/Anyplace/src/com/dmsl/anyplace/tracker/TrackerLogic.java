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

package com.dmsl.anyplace.tracker;

import java.util.ArrayList;
import java.util.List;

import com.dmsl.anyplace.sensors.MovementDetector;
import com.dmsl.anyplace.sensors.SensorsMain;
import com.google.android.gms.maps.model.LatLng;

//On Walking => Tracker + Kalman Filter
//On Standing  => Median of Tracker Locations
public class TrackerLogic extends AnyplaceTracker {

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
	private boolean walking_old = false;
	private boolean walking = false;

	public TrackerLogic(MovementDetector movementDetector, SensorsMain positioning) {
		super(positioning);
		movementDetector.addStepListener(new WalkingListener());
		super.addListener((AnyplaceTracker.TrackedLocAnyplaceTrackerListener) new TrackerListenerInit());
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

	private class TrackerListenerInit implements
			AnyplaceTracker.TrackedLocAnyplaceTrackerListener {

		@Override
		public void onNewLocation(LatLng pos) {
			// Call this method only for Initialisation
			TrackerLogic.super.removeListener(this);
			TrackerLogic.super.addListener((AnyplaceTracker.TrackedLocAnyplaceTrackerListener) new TrackerListener());

			kalmanFilter = new KalmanFilter(pos.latitude, pos.longitude);
			runningMedian = new RunningMedian(pos.latitude, pos.longitude);

			walking_old = walking;
			triggerTrackedLocListeners(pos);
		}

	}

	private class TrackerListener implements
			AnyplaceTracker.TrackedLocAnyplaceTrackerListener {

		@Override
		public void onNewLocation(LatLng pos) {
			LatLng result = pos;

			if (reset) {
				reset = false;
				kalmanFilter.reset(pos.latitude, pos.longitude);
				runningMedian.reset(pos.latitude, pos.longitude);
			}

			if (walking) {
				if (walking_old != true) {
					kalmanFilter.reset(pos.latitude, pos.longitude);
					walking_old = true;
				} else {
					result = kalmanFilter.update(pos.latitude, pos.longitude);
				}
			} else {
				if (walking_old != false) {
					runningMedian.reset(pos.latitude, pos.longitude);
					walking_old = false;
				} else {
					result = runningMedian.update(pos.latitude, pos.longitude);
				}
			}

			triggerTrackedLocListeners(result);
		}

	}

	@Override
	public void trackOff() {
		// Wait before Change listener because a separate thread may change
		// it!!!!
		super.trackOff();
		reset = true;
	}

	public void reset() {
		reset = true;
	}

}
