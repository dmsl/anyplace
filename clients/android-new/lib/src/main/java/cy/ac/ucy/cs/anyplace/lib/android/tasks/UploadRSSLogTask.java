/*
* Anyplace: A free and open Indoor Navigation Service with superb accuracy!
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

package cy.ac.ucy.cs.anyplace.lib.android.tasks;

import java.io.File;

import org.apache.http.ParseException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;


import cy.ac.ucy.cs.anyplace.lib.Anyplace;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import static android.content.Context.MODE_PRIVATE;


public class UploadRSSLogTask extends AsyncTask<Void, Integer, String> {
  private final static String RADIO_UPLOAD_URL_API = "/anyplace/position/radio_upload";
    private static String TAG = UploadRSSLogTask.class.getSimpleName();
	public interface UploadRSSLogTaskListener {
		void onErrorOrCancel(String result);

		void onSuccess(String result);
	}

	private UploadRSSLogTaskListener mListener;

	private String username, password;
	private String file;

	private Context context;
	private ProgressDialog dialog;
	private int currentProgress = 0;
	private HttpPost httppost;

	private boolean exceptionOccured = false;

	public UploadRSSLogTask(UploadRSSLogTaskListener l, Context ctx, String file, String username, String password){

		this.context = ctx;
		this.mListener = l;
		this.file = file;
		this.username = username;
		this.password = password;
	}

	@Override
	protected void onPreExecute() {
		dialog = new ProgressDialog(context);
		dialog.setMax(100);
		dialog.setMessage("Uploading file ...");
		dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		dialog.setCancelable(true);
		dialog.setCanceledOnTouchOutside(false);
		dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				httppost.abort();
			}
		});
		dialog.show();
	}

	@Override
	protected String doInBackground(Void... params) {
		try {
			 JSONObject j;

			// j.put("username", username);
			// j.put("password", password);
			// String json = j.toString();
			
			File rsslog = new File(this.file);
			if (rsslog.exists() == false) {
				exceptionOccured = true;
				return "File not found";
			}
			Log.d("radio upload", rsslog.toString());
			String response;
            // Log.e(TAG, "TODO: replace the api calls into the library");
			HttpClient httpclient = new DefaultHttpClient();

			//TODO: Place into the Anyplace core lib and use shared prefs
          SharedPreferences pref = context.getSharedPreferences("LoggerPreferences", MODE_PRIVATE);

          String host = pref.getString("server_ip_address", "ap.cs.ucy.ac.cy");


          String port = pref.getString("server_port", "443");
          String token = pref.getString("access_token", " ");


          //Anyplace client = new Anyplace("ap.cs.ucy.ac.cy", "443", "");
          Anyplace client = new Anyplace(host, port, context.getCacheDir().getAbsolutePath());




			// httppost = new HttpPost(host + RADIO_UPLOAD_URL_API);

			// MultipartEntity entity = new MultipartEntity();
            //
			// entity.addPart("radiomap", new FileBody(rsslog));
			// entity.addPart("json", new StringBody(json));
            //
			// ProgressCallback progressCallback = new ProgressCallback() {
            //
			// 	@Override
			// 	public void progress(float progress) {
			// 		if (currentProgress != (int) (progress)) {
			// 			currentProgress = (int) progress;
			// 			publishProgress(currentProgress);
			// 		}
			// 	}
			// };
            //
            //
			// httppost.setEntity(new ProgressHttpEntityWrapper(entity, progressCallback));
			// HttpResponse httpresponse = httpclient.execute(httppost);
			// HttpEntity resEntity = httpresponse.getEntity();

			// response = EntityUtils.toString(resEntity);

          response = client.uploadRssLog(token,file);

          //TODO fix uploadRSS

			Log.d("radio upload", "response: " + response);


			j = new JSONObject(response);
			if (j.getString("status").equalsIgnoreCase("1")) {
				exceptionOccured = true;
				return "Error: " + j.getString("message");
			}

		} catch (JSONException e) {
			exceptionOccured = true;
			Log.d("upload rss log", e.getMessage());
			return "Cannot upload RSS log. JSONException occurred[ " + e.getMessage() + " ]";
		} catch (ParseException e) {
          exceptionOccured = true;
          Log.d("upload rss log", e.getMessage());
          return "Cannot upload RSS log. ParseException occurred[ " + e.getMessage() + " ]";
        }
		// } catch (IOException e) {
		// 	exceptionOccured = true;
		// 	Log.d("upload rss log", e.getMessage());
        //
		// 	if (httppost != null && httppost.isAborted()) {
		// 		return "Uploading cancelled!";
		// 	} else {
		// 		return "Cannot upload RSS log. IOException occurred[ " + e.getMessage() + " ]";
		// 	}
        //
		// }
		return "Successfully uploaded RSS log!";
	}

	@Override
	protected void onProgressUpdate(Integer... values) {
		dialog.setProgress(values[0]);
	}

	@Override
	protected void onPostExecute(String result) {
		dialog.dismiss();

		if (exceptionOccured) {
			// call the error listener
			mListener.onErrorOrCancel(result);
		} else {
			// call the success listener
			mListener.onSuccess(result);
		}
	}

	@Override
	protected void onCancelled(String result) {
		mListener.onErrorOrCancel(result);
	}

	@Override
	protected void onCancelled() { // just for < API 11
		onCancelled("Uploading cancelled!");
	}

}
