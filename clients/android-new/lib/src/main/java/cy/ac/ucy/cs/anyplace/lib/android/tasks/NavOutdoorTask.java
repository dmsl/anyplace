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

package cy.ac.ucy.cs.anyplace.lib.android.tasks;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;

import cy.ac.ucy.cs.anyplace.lib.android.googleapi.GMapV2Direction;
import cy.ac.ucy.cs.anyplace.lib.android.utils.GeoPoint;
import com.google.android.gms.maps.model.LatLng;

import android.os.AsyncTask;

public class NavOutdoorTask extends AsyncTask<Void, Void, String> {

	public interface NavDirectionsListener {
		void onNavDirectionsAbort();

		void onNavDirectionsErrorOrCancel(String result);

		void onNavDirectionsSuccess(String result, List<LatLng> points);
	}

	private enum Status {
		SUCCESS, ERROR, ABORT
	}

	private NavDirectionsListener mListener;

	private GeoPoint fromPosition;
	private GeoPoint toPosition;
	private ArrayList<LatLng> directionPoints;
	private Status status = Status.ERROR;

	public NavOutdoorTask(NavDirectionsListener l, GeoPoint fromPosition, GeoPoint pos) {
		this.mListener = l;
		this.fromPosition = fromPosition;
		this.toPosition = pos;
	}

	public void onPreExecute() {
	}

	@Override
	protected String doInBackground(Void... params) {

		try {
			if (fromPosition == null || toPosition == null) {
				status = Status.ABORT;
				return "Task Cancelled";
			}

			// Avoid Running if the user is in or near the building
			double distance = GeoPoint.getDistanceBetweenPoints(fromPosition.dlon, fromPosition.dlat, toPosition.dlon, toPosition.dlat, "");
			if (distance < 500) {
				status = Status.ABORT;
				return "Task Cancelled";
			}

			GMapV2Direction md = new GMapV2Direction();
			Document doc = md.getDocument(fromPosition.dlat, fromPosition.dlon, toPosition, GMapV2Direction.MODE_DRIVING);
			directionPoints = md.getDirection(doc);
			status = Status.SUCCESS;
			return "Successfully plotted navigation route!";
		} catch (Exception e) {
			return "Error plotting navigation route. Exception[ " + e.getMessage() + " ]";
		}
	}

	@Override
	protected void onPostExecute(String result) {

		switch (status) {
		case SUCCESS:
			// call the success listener
			mListener.onNavDirectionsSuccess(result, directionPoints);
			break;
		case ERROR:
			// call the error listener
			mListener.onNavDirectionsErrorOrCancel(result);
			break;
		case ABORT:
			mListener.onNavDirectionsAbort();
		}
	}
}