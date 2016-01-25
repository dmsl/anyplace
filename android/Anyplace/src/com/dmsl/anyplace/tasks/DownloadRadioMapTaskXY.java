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

package com.dmsl.anyplace.tasks;

import java.io.File;
import java.io.FileWriter;
import java.net.SocketTimeoutException;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;

import com.dmsl.anyplace.AnyplaceAPI;
import com.dmsl.anyplace.utils.AnyplaceUtils;
import com.dmsl.anyplace.utils.NetworkUtils;

/**
 * The main task that downloads a radio map for the specified area. TODO - we
 * should check more thoroughly the concurrency issues
 */
public class DownloadRadioMapTaskXY extends AsyncTask<Void, Void, String> {

	public interface DownloadRadioMapListener {

		void onErrorOrCancel(String result);

		void onSuccess(String result);
	}

	private DownloadRadioMapListener mListener;
	private Context ctx;
	private String json_req;
	private String mBuildID;
	private String mFloor_number;
	private Boolean mForceDonwload;

	private boolean success = false;

	public DownloadRadioMapTaskXY(DownloadRadioMapListener mListener, Context ctx, String lat, String lon, String build, String floor_number, boolean forceDonwload) {

		try {

			this.mListener = mListener;
			this.ctx = ctx;

			JSONObject j = new JSONObject();

			j.put("username", "username");
			j.put("password", "pass");

			j.put("coordinates_lat", lat);
			j.put("coordinates_lon", lon);

			// add the floor in order to get only the necessary radio map
			j.put("floor_number", floor_number);

			this.json_req = j.toString();
			this.mBuildID = build;
			this.mFloor_number = floor_number;
			this.mForceDonwload = forceDonwload;

		} catch (JSONException e) {

		}

	}

	@Override
	protected void onPreExecute() {
	}

	@Override
	protected String doInBackground(Void... params) {

		try {
			if (json_req == null)
				return "Error creating the request!";
			// check sdcard state
			File root;
			try {
				root = AnyplaceUtils.getRadioMapFoler(ctx, mBuildID, mFloor_number);
			} catch (Exception e) {
				return e.getMessage();
			}

			// rename the radiomap according to the floor
			String filename_radiomap_download = AnyplaceUtils.getRadioMapFileName(mFloor_number);
			String mean_fname = filename_radiomap_download;
			String rbf_weights_fname = mean_fname.replace(".txt", "-rbf-weights.txt");
			String parameters_fname = mean_fname.replace(".txt", "-parameters.txt");

			File okfile = new File(root, "ok.txt");
			if (!mForceDonwload && okfile.exists()) {
				success = true;
				return "Successfully read radio map from cache!";
			}

			okfile.delete();

			// changed in order to receive only the radio map for the
			// current floor
			String response = NetworkUtils.downloadHttpClientJsonPost(AnyplaceAPI.getRadioDownloadXY(), json_req);
			JSONObject json = new JSONObject(response);

			if (json.getString("status").equalsIgnoreCase("error")) {
				return "Error Message: " + json.getString("message");
			}

			String means = json.getString("map_url_mean");
			String parameters = json.getString("map_url_parameters");
			String weights = json.getString("map_url_weights");

			// create the credentials JSON in order to send and download the
			// radio map
			JSONObject json_credentials = new JSONObject();
			json_credentials.put("username", "username");
			json_credentials.put("password", "pass");
			String cred_str = json_credentials.toString();

			String ms = NetworkUtils.downloadHttpClientJsonPost(means, cred_str);
			String ps = NetworkUtils.downloadHttpClientJsonPost(parameters, cred_str);
			String ws = NetworkUtils.downloadHttpClientJsonPost(weights, cred_str);

			// check if the files downloaded correctly
			if (ms.contains("error") || ps.contains("error") || ws.contains("error")) {
				json = new JSONObject(response);
				return "Error Message: " + json.getString("message");
			}

			FileWriter out;

			out = new FileWriter(new File(root, rbf_weights_fname));
			out.write(ws);
			out.close();

			out = new FileWriter(new File(root, parameters_fname));
			out.write(ps);
			out.close();

			out = new FileWriter(new File(root, mean_fname));
			out.write(ms);
			out.close();

			out = new FileWriter(okfile);
			out.write("ok;version:0;");
			out.close();

			success = true;
			return "Successfully saved radio maps!";

		} catch (ConnectTimeoutException e) {
			return "Connecting to Anyplace service is taking too long!";
		} catch (SocketTimeoutException e) {
			return "Communication with the server is taking too long!";
		} catch (Exception e) {
			return "Error downloading radio maps [ " + e.getMessage() + " ]";
		}

	}

	@Override
	protected void onPostExecute(String result) {
		if (success) {
			mListener.onSuccess(result);
		} else {
			// there was an error during the process
			mListener.onErrorOrCancel(result);
		}
	}

	@Override
	protected void onCancelled(String result) {
		mListener.onErrorOrCancel(result);
	}

	@Override
	protected void onCancelled() {
		mListener.onErrorOrCancel("Downloading RadioMap was cancelled!");
	}

}// end of radiomap download task

