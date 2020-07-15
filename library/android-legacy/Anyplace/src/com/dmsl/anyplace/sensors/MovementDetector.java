/*
* AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Author(s):
* https://github.com/bagilevi/android-pedometer/blob/master/src/name/bagi/levente/pedometer/StepDetector.java
* Kyriakos Georgiou
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

import android.hardware.SensorManager;

/**
 *         Step detector, if no steps are being detected, the user is standing.
 *         If steps are being detected, the user is moving.
 * 
 */
public class MovementDetector implements SensorsMain.IAccelerometerListener {

	/*
	 * Determines how fast the transition between walking to standing happens,
	 * give a chance for a long step to take place
	 */
	private static final int STANDING_DELAY = 50;

	private static float sensitivity = 4.5f;

	public interface MovementListener {
		public void onWalking();

		public void onStanding();
	}

	private ArrayList<MovementListener> mStepListeners = new ArrayList<MovementListener>();

	public void addStepListener(MovementListener ml) {
		mStepListeners.add(ml);
	}
	
	private enum MovementState {
		WALKING("Walking"), STANDING("Standing");
		
		private String name;
        private MovementState(String name) {
            this.name = name;
        }
       
        @Override
        public String toString(){
            return name;
        } 
	};

	private MovementState currentState = MovementState.STANDING;

	private float mLastValues[] = new float[3 * 2];
	private float mScale[] = new float[2];
	private float mYOffset;

	private float mLastDirections[] = new float[3 * 2];
	private float mLastExtremes[][] = { new float[3 * 2], new float[3 * 2] };
	private float mLastDiff[] = new float[3 * 2];
	private int mLastMatch = -1;

	private int noStep = 0;
	
	public MovementDetector() {
		int h = 480;
		mYOffset = h * 0.5f;
		mScale[0] = -(h * 0.5f * (1.0f / (SensorManager.STANDARD_GRAVITY * 2)));
		mScale[1] = -(h * 0.5f * (1.0f / (SensorManager.MAGNETIC_FIELD_EARTH_MAX)));
	}
	
	public MovementState getCurrentState(){
		return currentState;
	}
	
	// walking sensitivity updated in logger activity
	public static void setSensitivity(float s) {
		// Suggested sensitivity values, more sensitive as values go down
		// 1.97 2.96 4.44 6.66 10.00 15.00 22.50 33.75 50.62
		sensitivity = s;
	}

	@Override
	public void onNewAccelerometer(float[] values) {

		float vSum = 0;
		/* Sum x, y, z axis values */
		for (int i = 0; i < 3; i++) {
			final float v = mYOffset + values[i] * mScale[1];
			vSum += v;
		}
		int k = 0;
		float v = vSum / 3;

		float direction = (v > mLastValues[k] ? 1 : (v < mLastValues[k] ? -1
				: 0));
		if (direction == -mLastDirections[k]) {
			/* Direction changed minimum or maximum? */
			int extType = (direction > 0 ? 0 : 1);
			mLastExtremes[extType][k] = mLastValues[k];
			float diff = Math.abs(mLastExtremes[extType][k]
					- mLastExtremes[1 - extType][k]);

			/* Passed the threshold sensitivity? */
			if (diff > sensitivity) {

				boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff[k] * 2 / 3);
				boolean isPreviousLargeEnough = mLastDiff[k] > (diff / 3);
				boolean isNotContra = (mLastMatch != 1 - extType);

				if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough
						&& isNotContra) {
					// Log.i(TAG, "step");
					noStep = 0;
					if (currentState != MovementState.WALKING) {
						currentState = MovementState.WALKING;
						for (MovementListener stepListener : mStepListeners) {
							stepListener.onWalking();
						}
					}
					mLastMatch = extType;
				} else {
					mLastMatch = -1;
				}
			} else {
				noStep++;
				// Log.i(TAG, "standing");
				if (currentState != MovementState.STANDING
						&& noStep > STANDING_DELAY) {
					currentState = MovementState.STANDING;
					for (MovementListener stepListener : mStepListeners) {
						stepListener.onStanding();
					}
				}
			}
			mLastDiff[k] = diff;
		}
		mLastDirections[k] = direction;
		mLastValues[k] = v;
	}


}
