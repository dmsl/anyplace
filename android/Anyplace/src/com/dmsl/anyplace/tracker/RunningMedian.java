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

package com.dmsl.anyplace.tracker;

import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.maps.model.LatLng;

public class RunningMedian {

	private static final int LENGTH = 10;
	private List<Double> lats;
	private List<Double> lonts;
	private List<Double> lats_history;
	private List<Double> lonts_history;

	RunningMedian(double lat0, double lot0) {
		lats = new ArrayList<Double>(LENGTH / 2);
		lonts = new ArrayList<Double>(LENGTH / 2);
		lats_history = new ArrayList<Double>(LENGTH / 2);
		lonts_history = new ArrayList<Double>(LENGTH / 2);
		reset(lat0, lot0);
	}

	public void reset(double lat0, double lot0) {
		lats.clear();
		lonts.clear();
		lats_history.clear();
		lonts_history.clear();
		Double xd = new Double(lat0);
		Double yd = new Double(lot0);
		lats.add(xd);
		lonts.add(yd);
		lats_history.add(xd);
		lonts_history.add(yd);
	}

	private void insertLat(double lat, int i) {
		Double xd = new Double(lat);
		lats.add(i, xd);
		lats_history.add(0, xd);

		if (lats.size() > LENGTH) {
			lats.remove(lats_history.remove(lats_history.size() - 1));
		}
	}

	private void insertLont(double lot, int i) {
		Double yd = new Double(lot);
		lonts.add(i, yd);
		lonts_history.add(0, yd);

		if (lonts.size() > LENGTH) {
			lonts.remove(lonts_history.remove(lonts_history.size() - 1));
		}
	}

	public LatLng update(double lat, double lot) {

		boolean waitX = true;
		boolean waitY = true;
		int i;
		final int size = lats.size();
		
		for (i = 0; i < size && (waitX || waitY); i++) {
			if (waitX && lat < lats.get(i)) {
				insertLat(lat, i);
				waitX = false;
			}

			if (waitY && lot < lonts.get(i)) {
				insertLont(lot, i);
				waitY = false;
			}
		}

		if (waitX) {
			insertLat(lat, i);
			waitX = false;
		}

		if (waitY) {
			insertLont(lot, i);
			waitY = false;
		}

		return new LatLng(lats.get(lats.size() / 2), lonts.get(lonts.size() / 2));
	}
}
