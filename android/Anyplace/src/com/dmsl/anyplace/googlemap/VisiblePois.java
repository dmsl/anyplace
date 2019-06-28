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

package com.dmsl.anyplace.googlemap;

import com.dmsl.anyplace.nav.IPoisClass;
import com.dmsl.anyplace.nav.PoisModel;
import com.google.android.gms.maps.model.Marker;

public class VisiblePois extends VisibleObject<PoisModel> {

	private Marker mFromMarker = null;
	private Marker mToMarker = null;

	public VisiblePois() {
	}

	@Override
	public void hideAll() {

		super.hideAll();

		if (mFromMarker != null)
			mFromMarker.setVisible(false);
		if (mToMarker != null)
			mToMarker.setVisible(false);
	}

	@Override
	public void showAll() {

		super.showAll();

		if (mFromMarker != null)
			mFromMarker.setVisible(true);
		if (mToMarker != null)
			mToMarker.setVisible(true);
	}

	@Override
	public void clearAll() {
		super.clearAll();

		clearFromMarker();
		clearToMarker();
	}

	public Marker getMarkerFromPoisModel(String id) {
		for (Marker m : mMarkersToPoi.keySet()) {
			if (mMarkersToPoi.get(m).puid.equalsIgnoreCase(id)) {
				return m;
			}
		}
		return null;
	}

	public boolean isFromMarker(Marker other) {
		return mFromMarker != null ? mFromMarker.equals(other) : false;
	}

	public boolean isToMarker(Marker other) {
		return mToMarker != null ? mToMarker.equals(other) : false;
	}

	// <From/To Marker>
	public void setFromMarker(Marker m) {
		mFromMarker = m;
	}

	public Marker getFromMarker() {
		return mFromMarker;
	}

	public void setToMarker(Marker m) {
		mToMarker = m;
	}

	public Marker getToMarker() {
		return mToMarker;
	}

	public void clearFromMarker() {
		if (mFromMarker != null) {
			mFromMarker.remove();
			mFromMarker = null;
		}
	}

	public void clearToMarker() {
		if (mToMarker != null) {
			mToMarker.remove();
			mToMarker = null;
		}
	}

	// </From/To Marker>
}