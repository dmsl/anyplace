/*
* AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Author(s): 
* http://stackoverflow.com/questions/14123243/google-maps-android-api-v2-interactive-infowindow-like-in-original-android-go
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
* this software and associated documentation files (the “Software”), to deal in the
* Software without restriction, including without limitation the rights to use, copy,
* modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
* and to permit persons to whom the Software is furnished to do so, subject to the
* following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
* DEALINGS IN THE SOFTWARE.
*
*/

package com.circlegate.tt.cg.an.lib.map;

import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;

import com.google.android.gms.maps.model.Marker;

public abstract class OnInfoWindowElemTouchListener implements OnTouchListener {
	private final View view;
	private final Drawable bgDrawableNormal;
	private final Drawable bgDrawablePressed;
	private final Handler handler = new Handler();

	private Marker marker;
	private boolean pressed = false;

	public OnInfoWindowElemTouchListener(View view, Drawable bgDrawableNormal, Drawable bgDrawablePressed) {
		this.view = view;
		this.bgDrawableNormal = bgDrawableNormal;
		this.bgDrawablePressed = bgDrawablePressed;
	}

	public void setMarker(Marker marker) {
		this.marker = marker;
	}

	@Override
	public boolean onTouch(View vv, MotionEvent event) {
		if (0 <= event.getX() && event.getX() <= view.getWidth() && 0 <= event.getY() && event.getY() <= view.getHeight()) {
			switch (event.getActionMasked()) {
			case MotionEvent.ACTION_DOWN:
				startPress();
				break;

			// We need to delay releasing of the view a little so it shows the
			// pressed state on the screen
			case MotionEvent.ACTION_UP:
				handler.postDelayed(confirmClickRunnable, 150);
				break;

			case MotionEvent.ACTION_CANCEL:
				endPress();
				break;
			default:
				break;
			}
		} else {
			// If the touch goes outside of the view's area
			// (like when moving finger out of the pressed button)
			// just release the press
			endPress();
		}
		return false;
	}

	private void startPress() {
		if (!pressed) {
			pressed = true;
			handler.removeCallbacks(confirmClickRunnable);
			view.setBackgroundDrawable(bgDrawablePressed);
			if (marker != null)
				marker.showInfoWindow();
		}
	}

	private boolean endPress() {
		if (pressed) {
			this.pressed = false;
			handler.removeCallbacks(confirmClickRunnable);
			view.setBackgroundDrawable(bgDrawableNormal);
			if (marker != null)
				marker.showInfoWindow();
			return true;
		} else
			return false;
	}

	private final Runnable confirmClickRunnable = new Runnable() {
		public void run() {
			if (endPress()) {
				onClickConfirmed(view, marker);
			}
		}
	};

	/**
	 * This is called after a successful click
	 */
	protected abstract void onClickConfirmed(View v, Marker marker);
}