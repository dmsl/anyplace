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

package com.dmsl.anyplace.googlemap;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import com.dmsl.anyplace.nav.PoisModel;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.Marker;
import com.dmsl.anyplace.R;

public class CustomInfoWindowAdapter implements InfoWindowAdapter {
	private View view;
	private VisiblePois mPoiMarkersBundle;
	private TextView textViewName;
	private TextView textViewInfo;

	public CustomInfoWindowAdapter(Activity activity,
			VisiblePois poiMarkersBundle) {
		view = activity.getLayoutInflater().inflate(R.layout.info_window_poi, null);
		textViewName = ((TextView) view.findViewById(R.id.name));
		textViewInfo = ((TextView) view.findViewById(R.id.info));
		mPoiMarkersBundle = poiMarkersBundle;

	}

	@Override
	public View getInfoContents(Marker marker) {
		PoisModel poi = mPoiMarkersBundle.getPoisModelFromMarker(marker);
		if (poi != null) {
			// load the information about the marker selected
			textViewName.setText(poi.name);
			textViewInfo.setText("Click to navigate here!");
		} else {

			if (mPoiMarkersBundle.isFromMarker(marker)) { // green flag
				// load the information for the starting marker
				textViewName.setText("Navigation Start");
				textViewInfo.setText("-");
			} else if (mPoiMarkersBundle.isToMarker(marker)) { // red flag
				// load the information for the destination marker
				textViewName.setText("Navigation Destination");
				textViewInfo.setText("-");
			} else if (mPoiMarkersBundle.isGooglePlaceMarker(marker)) { // google
																		// poi
																		// selected
				// load the information for the click marker
				// at the moment there shouldn't be any
				textViewName.setText(mPoiMarkersBundle.getGooglePlace().name());
				textViewInfo.setText(mPoiMarkersBundle.getGooglePlace().description());
			} else { // Current User Location
				textViewName.setText(marker.getTitle());
				textViewInfo.setText(marker.getSnippet());
			}
		}
		return view;
	}

	@Override
	public View getInfoWindow(final Marker marker) {
		return null;
	}
} // end of InfoWindowAdapter
