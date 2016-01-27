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

package com.dmsl.anyplace.tasks;

import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;

import com.dmsl.anyplace.AnyplaceAPI;
import com.dmsl.anyplace.nav.PoisModel;
import com.dmsl.anyplace.utils.NetworkUtils;

/**
 * Returns the POIs according to a given Building and Floor
 *
 */
public class FetchPoisByBuidTask extends AsyncTask<Void, Void, String> {

	public interface FetchPoisListener {
		void onErrorOrCancel(String result);

		void onSuccess(String result, Map<String, PoisModel> poisMap);
	}

	private FetchPoisListener mListener;
	private Context mCtx;

	private Map<String, PoisModel> poisMap = new HashMap<String, PoisModel>();;

	private String buid;
	private String floor_number = null;

	private ProgressDialog dialog;
	private boolean success = false;

	public FetchPoisByBuidTask(FetchPoisListener l, Context ctx, String buid, String floor_number) {
		this.mCtx = ctx;
		this.mListener = l;
		this.buid = buid;
		this.floor_number = floor_number;
	}

	public FetchPoisByBuidTask(FetchPoisListener l, Context ctx, String buid) {
		this.mCtx = ctx;
		this.mListener = l;
		this.buid = buid;
	}

	@Override
	protected void onPreExecute() {
		dialog = new ProgressDialog(mCtx);
		dialog.setIndeterminate(true);
		dialog.setTitle("Fetching POIs");
		dialog.setMessage("Please be patient...");
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				FetchPoisByBuidTask.this.cancel(true);
			}
		});
		dialog.show();
	}

	@Override
	protected String doInBackground(Void... params) {
		if (!NetworkUtils.isOnline(mCtx)) {
			return "No connection available!";
		}

		try {
			JSONObject j = new JSONObject();
			try {
				j.put("username", "username");
				j.put("password", "pass");

				j.put("buid", this.buid);
				j.put("floor_number", this.floor_number);
			} catch (JSONException e) {
				return "Error Message: Could not create the request for the POIs!";
			}

			String response;
			if (floor_number != null) {
				// fetch the pois of this floor
				response = NetworkUtils.downloadHttpClientJsonPostGzip(AnyplaceAPI.getFetchPoisByBuidFloorUrl(), j.toString());
			} else {
				// fetch the pois for the whole building
				response = NetworkUtils.downloadHttpClientJsonPostGzip(AnyplaceAPI.getFetchPoisByBuidUrl(), j.toString());
			}

			JSONObject json_all = new JSONObject(response);
			if (json_all.has("status") && json_all.getString("status").equalsIgnoreCase("error")) {
				return "Error Message: " + json_all.getString("message");
			}
			// process the buildings received
			JSONArray buids_json = new JSONArray(json_all.getString("pois"));

			for (int i = 0, sz = buids_json.length(); i < sz; i++) {

				JSONObject json = (JSONObject) buids_json.get(i);

				// skip POIS without meaning
				if (json.getString("pois_type").equals("None"))
					continue;

				PoisModel poi = new PoisModel();
				poi.lat = json.getString("coordinates_lat");
				poi.lng = json.getString("coordinates_lon");
				poi.buid = json.getString("buid");
				poi.floor_name = json.getString("floor_name");
				poi.floor_number = json.getString("floor_number");
				poi.description = json.getString("description");
				poi.name = json.getString("name");
				poi.pois_type = json.getString("pois_type");
				poi.puid = json.getString("puid");
				if (json.has("is_building_entrance")) {
					poi.is_building_entrance = json.getBoolean("is_building_entrance");
				}

				poisMap.put(poi.puid, poi); // add the POI to the hashmap
			}

			success = true;
			return "Successfully fetched Points of Interest";

		} catch (ConnectTimeoutException e) {
			return "Connecting to Anyplace service is taking too long!";
		} catch (SocketTimeoutException e) {
			return "Communication with the server is taking too long!";
		} catch (JSONException e) {
			return "Not valid response from the server! Contact the admin.";
		} catch (Exception e) {
			return "Error fetching Points of Interest. Exception[ " + e.getMessage() + " ]";
		}
	}

	@Override
	protected void onPostExecute(String result) {
		dialog.dismiss();

		if (success) {
			mListener.onSuccess(result, poisMap);
		} else {
			mListener.onErrorOrCancel(result);

		}
	}

	@Override
	protected void onCancelled(String result) {
		dialog.dismiss();
		mListener.onErrorOrCancel(result);
	}

	@Override
	protected void onCancelled() {
		dialog.dismiss();
		mListener.onErrorOrCancel("Fetching POIs was cancelled!");
	}

} // end of fetch POIS by building and floor
