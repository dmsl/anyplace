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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;

import org.apache.http.conn.ConnectTimeoutException;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Handler;


import cy.ac.ucy.cs.anyplace.lib.Anyplace;
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceAPI;
import cy.ac.ucy.cs.anyplace.lib.android.utils.NetworkUtils;
import cy.ac.ucy.cs.anyplace.lib.android.utils.AndroidUtils;


public class FetchFloorPlanTask extends AsyncTask<Void, Void, String> {

	private final static Object sync = new Object();

	public interface FetchFloorPlanTaskListener {
		void onPrepareLongExecute();

		void onErrorOrCancel(String result);

		void onSuccess(String result, File floor_plan_file);
	}

	private FetchFloorPlanTaskListener mListener;
	private Context ctx;
	private String buid;
	private String floor_number;

	private File floor_plan_file;

	private boolean success = false;

	// Sync/Run PreExecute Listener on UI Thread
	final Object syncListener = new Object();
	boolean run = false;

	public FetchFloorPlanTask(Context ctx, String buid, String floor_number) {
		this.ctx = ctx;
		this.buid = buid;
		this.floor_number = floor_number;
	}

	public void setCallbackInterface(FetchFloorPlanTaskListener fetchFloorPlanTaskListener) {
		this.mListener = fetchFloorPlanTaskListener;
	}

	public void copy(File src, File dst) throws IOException {
		InputStream in = null;
		OutputStream out = null;
		try {
			in = new FileInputStream(src);
			out = new FileOutputStream(dst);
			// Transfer bytes from in to out
			byte[] buf = new byte[1024];
			int len;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		} finally {
			if (in != null)
				in.close();
			if (out != null)
				out.close();
		}
	}

	@Override
	protected String doInBackground(Void... params) {
		OutputStream output = null;
		InputStream is = null;
		File tempFile = null;
		try {

			// check sdcard state
			if (!AndroidUtils.checkExternalStorageState()) {
				// we cannot download the floor plan on the sdcard
				return "Error: It seems that we cannot write on your sdcard!";
			}

			File sdcard_root = ctx.getExternalFilesDir(null);
			if (sdcard_root == null) {
				return "Error: It seems we cannot save the floorplan on sdcard!";
			}
			File root = new File(sdcard_root, "floor_plans" + File.separatorChar + buid + File.separatorChar + floor_number);
			root.mkdirs();
			File dest_path = new File(root, "tiles_archive.zip");

			File okfile = new File(root, "ok.txt");

			// check if the file already exists and if yes return immediately
			if (dest_path.exists() && dest_path.canRead() && dest_path.isFile() && okfile.exists()) {
				floor_plan_file = dest_path;
				success = true;
				return "Successfully read floor plan from cache!";
			}

			runPreExecuteOnUI();
			okfile.delete();

			// // prepare the json object request
			// JSONObject j = new JSONObject();
            //
			// j.put("username", "username");
			// j.put("password", "pass");

			//is = NetworkUtils.downloadHttpClientJsonPostStream(AnyplaceAPI.getServeFloorTilesZipUrl(buid, floor_number, ctx), j.toString());

			tempFile = new File(ctx.getCacheDir(), "FloorPlan" + Integer.toString((int) (Math.random() * 100)));
			if (tempFile.exists())
				throw new Exception("Temp File already in use");


          //TODO preferences
          Anyplace client = new Anyplace("ap-dev.cs.ucy.ac.cy", "443", "");

          String access_token = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjhjNThlMTM4NjE0YmQ1ODc0MjE3MmJkNTA4MGQxOTdkMmIyZGQyZjMiLCJ0eXAiOiJKV1QifQ";
          String response = client.floorplans( access_token, buid, floor_number, tempFile);



			// Critical Block - Added for safety
			synchronized (sync) {
				copy(tempFile, dest_path);
				// unzip the tiles_archive
				AndroidUtils.unzip(dest_path.getAbsolutePath());

				FileWriter out = new FileWriter(okfile);
				out.write("ok;version:0;");
				out.close();
			}

			floor_plan_file = dest_path;
			waitPreExecute();
			success = true;
			return "Successfully fetched floor plan";

		} catch (ConnectTimeoutException e) {
			return "Cannot connect to Anyplace service!";
		} catch (SocketTimeoutException e) {
			return "Communication with the server is taking too long!";
		} catch (JSONException e) {
			return "JSONException: " + e.getMessage();
		} catch (Exception e) {
			return "Error fetching floor plan. [ " + e.getMessage() + " ]";
		} finally {
			if (is != null)
				try {
					is.close();
				} catch (IOException e) {
				}
			if (output != null)
				try {
					output.close();
				} catch (IOException e) {
				}
			if (tempFile != null) {
				tempFile.delete();
			}

		}

	}

	@Override
	protected void onPostExecute(String result) {
		if (success) {
			mListener.onSuccess(result, floor_plan_file);
		} else {
			// there was an error during the process
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
		mListener.onErrorOrCancel("Floor plan loading cancelled...");
	}

	@Override
	protected void onCancelled() { // just for < API 11
		mListener.onErrorOrCancel("Floor plan loading cancelled...");
	}
}// end of floor plan downloader
