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

package com.dmsl.anyplace.tasks;

import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;

import com.dmsl.anyplace.AnyplaceAPI;
import com.dmsl.anyplace.nav.FloorModel;
import com.dmsl.anyplace.utils.NetworkUtils;

public class FetchFloorsByBuidTask extends AsyncTask<Void, Void, String> {

	public interface FetchFloorsByBuidTaskListener {
		void onErrorOrCancel(String result);

		void onSuccess(String result, List<FloorModel> floors);
	}

	private FetchFloorsByBuidTaskListener mListener;
	private Context ctx;

	private List<FloorModel> floors = new ArrayList<FloorModel>();

	private String buid;

	private boolean success = false;

	private ProgressDialog dialog;
	private Boolean showDialog = true;

	public FetchFloorsByBuidTask(FetchFloorsByBuidTaskListener fetchFloorsByBuidTaskListener, Context ctx, String buid) {
		this.mListener = fetchFloorsByBuidTaskListener;
		this.ctx = ctx;
		this.buid = buid;
	}

	public FetchFloorsByBuidTask(FetchFloorsByBuidTaskListener fetchFloorsByBuidTaskListener, Context ctx, String buid, Boolean showDialog) {
		this.mListener = fetchFloorsByBuidTaskListener;
		this.ctx = ctx;
		this.buid = buid;
		this.showDialog = showDialog;
	}

	@Override
	protected void onPreExecute() {
		if (showDialog) {
			dialog = new ProgressDialog(ctx);
			dialog.setIndeterminate(true);
			dialog.setTitle("Fetching floors");
			dialog.setMessage("Please be patient...");
			dialog.setCancelable(true);
			dialog.setCanceledOnTouchOutside(false);
			dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
				@Override
				public void onCancel(DialogInterface dialog) {
					// finishJob();
					FetchFloorsByBuidTask.this.cancel(true);
				}
			});
			dialog.show();
		}

	}

	@Override
	protected String doInBackground(Void... params) {

		if (!NetworkUtils.isOnline(ctx)) {
			return "No connection available!";
		}

		try {
			JSONObject j = new JSONObject();
			try {
				j.put("username", "username");
				j.put("password", "pass");
				j.put("buid", this.buid);
			} catch (JSONException e) {
				return "Error requesting the floors for the buildings!";
			}

			String response = NetworkUtils.downloadHttpClientJsonPostGzip(AnyplaceAPI.getFetchFloorsByBuidUrl(), j.toString());
			JSONObject json = new JSONObject(response);

			if (json.has("status") && json.getString("status").equalsIgnoreCase("error")) {
				return "Error Message: " + json.getString("message");
			}

			// process the buildings received
			FloorModel b;
			JSONArray buids_json = new JSONArray(json.getString("floors"));

			if (buids_json.length() == 0) {
				return "Error: 0 Floors found";
			}

			for (int i = 0, sz = buids_json.length(); i < sz; i++) {
				JSONObject cp = (JSONObject) buids_json.get(i);
				b = new FloorModel();
				b.buid = cp.getString("buid");
				b.floor_name = cp.getString("floor_name");
				b.floor_number = cp.getString("floor_number");
				b.description = cp.getString("description");

				// use optString() because these values might not exist of a
				// floor plan has not been set for this floor
				b.bottom_left_lat = cp.optString("bottom_left_lat");
				b.bottom_left_lng = cp.optString("bottom_left_lng");
				b.top_right_lat = cp.optString("top_right_lat");
				b.top_right_lng = cp.optString("top_right_lng");

				floors.add(b);
			}

			Collections.sort(floors);

			success = true;
			return "Successfully fetched floors";

		} catch (ConnectTimeoutException e) {
			return "Cannot connect to Anyplace service!";
		} catch (SocketTimeoutException e) {
			return "Communication with the server is taking too long!";
		} catch (UnknownHostException e) {
			return "No connection available!";
		} catch (Exception e) {
			// Log.d("fetching floors task", e.getMessage());
			return "Error fetching floors. [ " + e.getMessage() + " ]";
		}
	}

	@Override
	protected void onPostExecute(String result) {
		// removes the progress dialog
		if (showDialog)
			dialog.dismiss();

		if (success) {
			mListener.onSuccess(result, floors);
		} else {
			// there was an error during the process
			mListener.onErrorOrCancel(result);
		}

	}

	@Override
	protected void onCancelled(String result) {
		if (showDialog)
			dialog.dismiss();
		mListener.onErrorOrCancel("Floor fetching cancelled...");
	}

	@Override
	protected void onCancelled() { // just for < API 11
		if (showDialog)
			dialog.dismiss();
		mListener.onErrorOrCancel("Floor fetching cancelled...");
	}

}
