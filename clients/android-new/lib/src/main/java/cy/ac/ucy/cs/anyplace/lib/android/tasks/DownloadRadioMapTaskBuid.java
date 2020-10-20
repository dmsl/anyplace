/*
* Anyplace: A free and open Indoor Navigation Service with superb accuracy!
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

import java.io.File;
import java.io.FileWriter;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;


import cy.ac.ucy.cs.anyplace.lib.Anyplace;
import cy.ac.ucy.cs.anyplace.lib.android.utils.AnyplaceUtils;
import cy.ac.ucy.cs.anyplace.lib.android.utils.NetworkUtils;

/**
 * The main task that downloads a radio map for the specified area.
 * TODO - we should check more thoroughly the concurrency issues
 * Time consuming real time calculation of radiomaps
 */
public class DownloadRadioMapTaskBuid extends AsyncTask<Void, Void, String> {

	public interface DownloadRadioMapListener {

		void onPrepareLongExecute();

		void onErrorOrCancel(String result);

		void onSuccess(String result);
	}

	// Allow only one download task (real time creation of radiomap)
	private static volatile Boolean downInProgress = false;

	private DownloadRadioMapListener mListener;
	private Context ctx;
	private String json_req;
	private String mBuildID;
	private String mFloor_number;
	private Boolean mForceDonwload;

	private boolean success = false;

	// Sync/Run PreExecute Listener on UI Thread
	final Object syncListener = new Object();
	boolean run = false;

	public DownloadRadioMapTaskBuid(DownloadRadioMapListener mListener, Context ctx, String lat, String lon, String buid, String floor_number, boolean forceDonwload) {
		this(mListener, ctx, buid, floor_number, forceDonwload);
	}

	public DownloadRadioMapTaskBuid(DownloadRadioMapListener mListener, Context ctx, String buid, String floor_number, boolean forceDonwload) {

		try {
			this.mListener = mListener;
			this.ctx = ctx;

			JSONObject j = new JSONObject();

			j.put("username", "username");
			j.put("password", "pass");

			// add the building and floor in order to get only the necessary
			// radio map
			j.put("buid", buid);
			j.put("floor", floor_number);

			this.json_req = j.toString();
			this.mBuildID = buid;
			this.mFloor_number = floor_number;
			this.mForceDonwload = forceDonwload;

		} catch (JSONException e) {

		}

	}

	public DownloadRadioMapListener getCallbackInterface() {
		return this.mListener;
	}

	@Override
	protected String doInBackground(Void... params) {

		boolean releaseLock = false;
		try {

			if (json_req == null)
				return "Error creating the request!";
			// check sdcard state
			File root;
			try {
				root = AnyplaceUtils.getRadioMapFolder(ctx, mBuildID, mFloor_number);
			} catch (Exception e) {
				return e.getMessage();
			}

			File okfile = new File(root, "ok.txt");
			if (!mForceDonwload && okfile.exists()) {
				success = true;
				return "Successfully read radio map from cache!";
			}

			// Allow only one download of the radiomap
			synchronized (downInProgress) {
				if (downInProgress == false) {
					downInProgress = true;
					releaseLock = true;
				} else {
					return "Already downloading radio map. Please wait...";
				}
			}

			runPreExecuteOnUI();
			okfile.delete();

			// receive only the radio map for the current floor 0 timeout overrides default timeout
			// String response = NetworkUtils.downloadHttpClientJsonPost(AnyplaceAPI.getRadioDownloadBuid(ctx), json_req, 0);

          //TODO: USE SHARED PREFERENCES
          Anyplace client = new Anyplace("ap-dev.cs.ucy.ac.cy", "443", "");

          String access_token = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjhjNThlMTM4NjE0YmQ1ODc0MjE3MmJkNTA4MGQxOTdkMmIyZGQyZjMiLCJ0eXAiOiJKV1QifQ";
          String response = client.radiomapMeanByBuildingFloor( access_token, mBuildID, mFloor_number);
			JSONObject json = new JSONObject(response);

			if (json.getString("status").equalsIgnoreCase("error")) {
				return "Error Message: " + json.getString("message");
			}

			// String means = json.getString("map_url_mean");

			// // create the credentials JSON in order to send and download the radio map
			// JSONObject json_credentials = new JSONObject();
			// json_credentials.put("username", "username");
			// json_credentials.put("password", "pass");
			// String cred_str = json_credentials.toString();
            //
			// String ms = NetworkUtils.downloadHttpClientJsonPost(means, cred_str);


			String ms = response;

			// check if the files downloaded correctly
			// if (ms.contains("error")) {
			// 	json = new JSONObject(response);
			// 	return "Error Message: " + json.getString("message");
			// }

			// rename the radiomap according to the floor
			// parameters and weights not used any more (RPF Algorithm Removed)



			String filename_radiomap_download = AnyplaceUtils.getRadioMapFileName(mFloor_number);
			String mean_fname = filename_radiomap_download;



			// String rbf_weights_fname = mean_fname.replace(".txt", "-rbf-weights.txt");
			// String parameters_fname = mean_fname.replace(".txt", "-parameters.txt");


          FileWriter out;

			out = new FileWriter(new File(root, mean_fname));
			out.write(ms);
			out.close();

			out = new FileWriter(okfile);
			out.write("ok;version:0;");
			out.close();

			waitPreExecute();
			success = true;
			return "Successfully saved radio maps!";

		} catch (Exception e) {
			return "Error downloading radio maps [ " + e.getMessage() + " ]";
		} finally {
			if (releaseLock)
				downInProgress = false;
		}

	}

	@Override
	protected void onPostExecute(String result) {
		if (success) {
			if (mListener != null)
				mListener.onSuccess(result);
		} else {
			// there was an error during the process
			if (mListener != null)
				mListener.onErrorOrCancel(result);
		}
	}

	private void runPreExecuteOnUI() {
		// Get a handler that can be used to post to the main thread
		Handler mainHandler = new Handler(ctx.getMainLooper());

		Runnable myRunnable = new Runnable() {

			@Override
			public void run() {
				try {
					if (mListener != null)
						mListener.onPrepareLongExecute();
				} finally {
					synchronized (syncListener) {
						run = true;
						syncListener.notifyAll();
					}
				}
			}
		};

		mainHandler.post(myRunnable);
	}

	private void waitPreExecute() throws InterruptedException {
		synchronized (syncListener) {
			while (run == false) {
				syncListener.wait();
			}
		}
	}

	@Override
	protected void onCancelled(String result) {
		if (mListener != null)
			mListener.onErrorOrCancel(result);
	}

	@Override
	protected void onCancelled() {
		if (mListener != null)
			mListener.onErrorOrCancel("Downloading RadioMap was cancelled!");
	}

}// end of radiomap download task

