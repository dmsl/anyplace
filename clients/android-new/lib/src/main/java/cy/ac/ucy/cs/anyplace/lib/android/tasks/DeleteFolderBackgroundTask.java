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

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

public class DeleteFolderBackgroundTask extends AsyncTask<Void, Void, Void> {

	public interface DeleteFolderBackgroundTaskListener {
		void onSuccess();
	}

	private DeleteFolderBackgroundTaskListener mListener = null;
	private Boolean showDialog = true;
	private ProgressDialog dialog;
	private Context ctx;
	private File[] mParams;
	private String[] mParams2;

	public DeleteFolderBackgroundTask(Context ctx) {
		this.ctx = ctx;
	}

	public DeleteFolderBackgroundTask(DeleteFolderBackgroundTaskListener deleteFolderBackgroundTaskListener, Context ctx, Boolean showDialog) {
		this.mListener = deleteFolderBackgroundTaskListener;
		this.ctx = ctx;
		this.showDialog = showDialog;
	}

	public void setFiles(File... params) {
		this.mParams = params;
	}

	public void setFiles(String[] params) {
		this.mParams2 = params;
	}

	@Override
	protected void onPreExecute() {
		if (showDialog) {
			dialog = new ProgressDialog(ctx);
			dialog.setCancelable(false);
			dialog.setIndeterminate(true);
			dialog.setTitle("Deleting");
			dialog.setMessage("Please be patient...");
			dialog.show();
		}
	}

	@Override
	protected Void doInBackground(Void... params) {
		if (mParams != null) {
			for (File f : mParams) {
				deleteDirRecursively(f);
			}
		}

		if (mParams2 != null) {
			for (String f : mParams2) {
				deleteDirRecursively(new File(f));
			}
		}
		return null;
	}

	@Override
	protected void onPostExecute(Void v) {
		if (showDialog)
			dialog.dismiss();

		if (mListener != null)
			mListener.onSuccess();

	}

	private void deleteDirRecursively(File f) {
		if (f.exists()) {
			for (File t : f.listFiles()) {
				if (t.isDirectory()) {
					deleteDirRecursively(t);
				}
				t.delete(); // delete each radio map
			}
		}
	}
}
