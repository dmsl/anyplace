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

import com.dmsl.anyplace.sensors.SensorsMain;
import com.dmsl.anyplace.sensors.SensorsStepCounter;
import com.dmsl.anyplace.sensors.SensorsStepCounter.IStepListener;
import com.google.android.gms.maps.model.LatLng;

public class IMU {

	// Location
	public interface TrackedLocAnyplaceIMUListener {
		public void onNewLocation(LatLng pos);
	}

	private List<TrackedLocAnyplaceIMUListener> tllisteners = new ArrayList<TrackedLocAnyplaceIMUListener>(1);

	public void addListener(TrackedLocAnyplaceIMUListener list) {
		tllisteners.add(list);
	}

	public void removeListener(TrackedLocAnyplaceIMUListener list) {
		tllisteners.remove(list);
	}

	private void triggerTrackedLocListeners(LatLng pos) {
		for (TrackedLocAnyplaceIMUListener l : tllisteners) {
			l.onNewLocation(pos);
		}
	}

	private final float STEP_LENGTH_KM;
	private double currLat;
	private double currLong;
	private float prevSteps;

	private SensorsMain sensorsMain;

	public IMU(SensorsMain sensorsMain, SensorsStepCounter sensorsStep, double currLat, double currLong) {
		this.sensorsMain = sensorsMain;
		this.currLat = currLat;
		this.currLong = currLong;
		this.STEP_LENGTH_KM = sensorsStep.getStepLength();
		sensorsStep.addListener(new StepListenerInit(sensorsStep));
	}

	public void reset(double lat0, double lot0) {
		synchronized (this) {
			currLat = lat0;
			currLong = lot0;
		}
	}

	// Same as GeoPoint getNewPointFromDistanceBearing
	public static LatLng calculateNewPoint(double lat, double lot, double distanceKM, double angleDegrees) {

		final int R = 6371;

		double angleRad = Math.toRadians(angleDegrees);

		double lat1, lat2, lon1, lon2;

		lat1 = Math.toRadians(lat); // #Current lat point converted to radians
		lon1 = Math.toRadians(lot); // #Current long point converted to radians
		lat2 = Math.asin(Math.sin(lat1) * Math.cos(distanceKM / R) + Math.cos(lat1) * Math.sin(distanceKM / R) * Math.cos(angleRad));
		lon2 = lon1 + Math.atan2(Math.sin(angleRad) * Math.sin(distanceKM / R) * Math.cos(lat1), Math.cos(distanceKM / R) - Math.sin(lat1) * Math.sin(lat2));

		return new LatLng(Math.toDegrees(lat2), Math.toDegrees(lon2));

	}

	private class StepListenerInit implements IStepListener {

		SensorsStepCounter sensorsStep;

		StepListenerInit(SensorsStepCounter sensorsStep) {
			this.sensorsStep = sensorsStep;
		}

		@Override
		public void onNewStep(float value) {
			prevSteps = value;
			sensorsStep.removeListener(this);
			sensorsStep.addListener(new StepListener());
		}
	}

	private class StepListener implements IStepListener {

		@Override
		public void onNewStep(float value) {
			synchronized (IMU.this) {
				LatLng loc = calculateNewPoint(currLat, currLong, STEP_LENGTH_KM * (value - prevSteps), sensorsMain.getRAWHeading());
				prevSteps = value;
				currLat = loc.latitude;
				currLong = loc.longitude;
				//Don not move trigger Listeners Up 
				triggerTrackedLocListeners(loc);
			}
		}
	}
}
