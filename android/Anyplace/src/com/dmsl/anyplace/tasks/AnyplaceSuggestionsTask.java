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

package com.dmsl.anyplace.tasks;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.http.conn.ConnectTimeoutException;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import com.dmsl.anyplace.cache.AnyplaceCache;
import com.dmsl.anyplace.googleapi.GooglePlaces;
import com.dmsl.anyplace.googleapi.PlacesList;
import com.dmsl.anyplace.nav.IAnyPlace;
import com.dmsl.anyplace.nav.PoisModel;
import com.dmsl.anyplace.nav.AnyPlaceSeachingHelper.SearchTypes;
import com.dmsl.anyplace.provider.AnyplacePOIProvider;
import com.dmsl.anyplace.utils.GeoPoint;

/**
 * The task that provides the Suggestions according to the zoom level and the
 * position.
 */
public class AnyplaceSuggestionsTask extends AsyncTask<Void, Void, String> {

	public interface AnyplaceSuggestionsListener {
		void onErrorOrCancel(String result);

		void onSuccess(String result, Cursor cursor);
	}

	private AnyplaceSuggestionsListener mListener;
	private Context ctx;
	private final Object sync = new Object();
	private boolean run = false;

	private boolean exceptionOccured = false;
	private SearchTypes searchType;
	private GeoPoint position;
	private String query;
	private Cursor cursor;

	private AnyplaceCache mAnyplaceCache = null;

	public AnyplaceSuggestionsTask(AnyplaceSuggestionsListener l, Context ctx, SearchTypes searchType, GeoPoint position, String query) {
		this.mListener = l;
		this.searchType = searchType;
		this.position = position;
		this.query = query;
		this.ctx = ctx;
		mAnyplaceCache = AnyplaceCache.getInstance(ctx);
	}

	public List<IAnyPlace> queryStaticAnyPlacePOI(String query) throws IOException {
		Collection<PoisModel> pois = mAnyplaceCache.getPois();
		List<IAnyPlace> ianyplace = new ArrayList<IAnyPlace>();
		for (PoisModel pm : pois) {
			if (PoisModel.matchQueryPoi(query, pm.name)) {
				ianyplace.add(pm);
			}
		}
		return ianyplace;
	}

	@Override
	protected String doInBackground(Void... params) {
		try {
			// get the search suggestions
			if (searchType == SearchTypes.INDOOR_MODE) {
				// if we are at a zoom level higher than 19 then we use the
				// AnyPlace POI
				// the buildings for search suggestions
				// cursor = AnyplacePOIProvider.queryStatic(query,
				// AnyplacePOIProvider.POI_ANYPLACE, null);

				// sleep for a while to avoid execution in case another task
				// started
				// and check afterwards if you are cancelled
				Thread.sleep(150);
				if (isCancelled()) {
					return "Cancelled!";
				}

				// use the 2-step method to get out quickly if this task is
				// cancelled
				List<IAnyPlace> places = queryStaticAnyPlacePOI(query);
				if (isCancelled()) {
					return "Cancelled!";
				}
				cursor = AnyplacePOIProvider.prepareAnyPlacePOIsCursor(places);

			} else if (searchType == SearchTypes.OUTDOOR_MODE) {
				// at a lower zoom level we use the Google Places API for search
				// in order to allow the user to search more coarsely for
				// locations

				// sleep for a while to avoid execution in case another task
				// started
				// and check afterwards if you are cancelled
				Thread.sleep(500);
				if (isCancelled()) {
					return "Cancelled!";
				}

				// Get a handler that can be used to post to the main thread
				Handler mainHandler = new Handler(ctx.getMainLooper());

				Runnable myRunnable = new Runnable() {

					@Override
					public void run() {
						try {
							List<IAnyPlace> places = new ArrayList<IAnyPlace>(1);
							PoisModel pm = new PoisModel();
							pm.name = "Searching through Google …";
							places.add(pm);
							cursor = AnyplacePOIProvider.prepareAnyPlacePOIsCursor(places);
							mListener.onSuccess("Dummy Result", cursor);
						} finally {
							synchronized (sync) {
								run = true;
								sync.notifyAll();
							}
						}
					}
				};

				mainHandler.post(myRunnable);

				// cursor = AnyplacePOIProvider.queryStatic(query,
				// AnyplacePOIProvider.POI_GOOGLE_PLACES, position);
				PlacesList places = GooglePlaces.queryStaticGoogle(query, position);
				if (isCancelled())
					return "Cancelled!";
				// cache the results
				mAnyplaceCache.setGooglePlaces(places);
				// create the cursor for the results
				cursor = AnyplacePOIProvider.prepareGooglePlacesCursor(places);

				synchronized (sync) {
					while (run == false) {
						sync.wait();
					}
				}

			}

			if (isCancelled()) {
				return "Cancelled!";
			}

			return "Success!";

		} catch (ConnectTimeoutException e) {
			exceptionOccured = true;
			return "Connecting to the server is taking too long!";
		} catch (SocketTimeoutException e) {
			exceptionOccured = true;
			return "Communication with the server is taking too long!";
		} catch (IOException e) {
			exceptionOccured = true;
			return "Communication error[ " + e.getMessage() + " ]";
		} catch (InterruptedException e) {
			exceptionOccured = true;
			return "Suggestions task interrupted!";
		}

	}

	@Override
	protected void onProgressUpdate(Void... values) {
		// TODO Auto-generated method stub
		super.onProgressUpdate(values);
	}

	@Override
	protected void onPostExecute(String result) {
		if (exceptionOccured) {
			// call the error listener
			mListener.onErrorOrCancel(result);
		} else {

			// call the success listener
			mListener.onSuccess(result, cursor);

		}
	}

	@Override
	protected void onCancelled(String result) {
		// mListener.onErrorOrCancel(result);
	}

	@Override
	protected void onCancelled() { // just for < API 11
		// mListener.onErrorOrCancel("Anyplace Suggestions task cancelled!");
	}
}// end of suggestions task