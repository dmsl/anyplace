/*
* AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Author(s): 
* https://github.com/bagilevi/android-pedometer/blob/master/src/name/bagi/levente/pedometer/StepDetector.java
* Timotheos Constambeys
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

public class SoftwareStepCounter {

	private final float mYOffset = 480 * 0.5f;
	private final float mLimit = (float) 1.97;

	private float mLastValues;
	private float mLastDirections;
	private float mLastExtremes[] = new float[2];
	private float mLastDiff;
	private int mLastMatch = -1;

	public int steps = 0;

	public boolean checkIfStep(float[] values) {

		boolean step = false;
		float vSum = values[0] + values[1] + values[2];

		float v = mYOffset - vSum * 4 / 3;

		float direction = (v > mLastValues ? 1 : (v < mLastValues ? -1 : 0));
		if (direction == -mLastDirections) {
			// Direction changed
			int extType = (direction > 0 ? 0 : 1); // minimum or maximum?
			mLastExtremes[extType] = mLastValues;
			float diff = Math.abs(mLastExtremes[extType] - mLastExtremes[1 - extType]);

			if (diff > mLimit) {

				boolean isAlmostAsLargeAsPrevious = diff > (mLastDiff * 2 / 3);
				boolean isPreviousLargeEnough = mLastDiff > (diff / 3);
				boolean isNotContra = (mLastMatch != 1 - extType);

				if (isAlmostAsLargeAsPrevious && isPreviousLargeEnough && isNotContra) {
					steps++;
					step = true;
					mLastMatch = extType;
					// }
				} else {
					// Log.i(TAG, "no step");
					mLastMatch = -1;
				}
			}
			mLastDiff = diff;
		}
		mLastDirections = direction;
		mLastValues = v;

		return step;
	}
}
