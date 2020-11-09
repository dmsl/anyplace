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

package cy.ac.ucy.cs.anyplace.lib.android.cache;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import android.content.Context;
import android.os.AsyncTask;


import cy.ac.ucy.cs.anyplace.lib.android.nav.BuildingModel;
import cy.ac.ucy.cs.anyplace.lib.android.cache.BackgroundFetchListener.Status;
import cy.ac.ucy.cs.anyplace.lib.android.cache.BackgroundFetchListener.ErrorType;
import cy.ac.ucy.cs.anyplace.lib.android.nav.FloorModel;

import cy.ac.ucy.cs.anyplace.lib.android.tasks.FetchFloorsByBuidTask.FetchFloorsByBuidTaskListener;
import cy.ac.ucy.cs.anyplace.lib.android.tasks.FetchFloorPlanTask;
import cy.ac.ucy.cs.anyplace.lib.android.tasks.DownloadRadioMapTaskBuid;


@SuppressWarnings("serial")
class BackgroundFetch implements Serializable, Runnable {

	private BackgroundFetchListener l;
	public BuildingModel build = null;


	public Status status = Status.RUNNING;
	private ErrorType error = ErrorType.EXCEPTION;

	private int progress_total = 0;
	private int progress_current = 0;

	private Context ctx;

	private AsyncTask<Void, Void, String> currentTask;

	BackgroundFetch(Context ctx, BackgroundFetchListener l, BuildingModel build) {
		this.l = l;
		this.build = build;
		this.ctx = ctx;
	}

	@Override
	public void run() {
		fetchFloors();
	}
	
	// Fetch Building Floors Details
	private void fetchFloors() {
		if (build.isFloorsLoaded() == false) {
			build.loadFloors(new FetchFloorsByBuidTaskListener() {

				@Override
				public void onSuccess(String result, List<FloorModel> floors) {
					progress_total = build.getFloors().size() * 2;
					fetchAllFloorPlans(0);
				}

				@Override
				public void onErrorOrCancel(String result) {
					status = Status.STOPPED;
					l.onErrorOrCancel(result, error);

				}
			}, ctx, false, false);

		} else {
			progress_total = build.getFloors().size() * 2;
			fetchAllFloorPlans(0);
		}
	}

	// Fetch Floor Maps
	private void fetchAllFloorPlans(final int index) {

		if (build.isFloorsLoaded()) {
			if (index < build.getFloors().size()) {
				FloorModel f = build.getFloors().get(index);

				currentTask = new FetchFloorPlanTask(ctx, build.buid, f.floor_number);
				((FetchFloorPlanTask) currentTask).setCallbackInterface(new FetchFloorPlanTask.FetchFloorPlanTaskListener() {

					@Override
					public void onSuccess(String result, File floor_plan_file) {
						l.onProgressUpdate(++progress_current, progress_total);
						fetchAllFloorPlans(index + 1);
					}

					@Override
					public void onErrorOrCancel(String result) {
						status = Status.STOPPED;
						l.onErrorOrCancel(result, error);
					}

					@Override
					public void onPrepareLongExecute() {

					}
				});
				currentTask.execute();

			} else {
				fetchAllRadioMaps(0);
			}
		} else {
			status = Status.STOPPED;
			l.onErrorOrCancel("Fetch Floor Plans Error", error);
		}

	}

	// fetch All Radio Maps except from current floor(floor_number)
	private void fetchAllRadioMaps(final int index) {

		if (build.getFloors() != null) {
			if (index < build.getFloors().size()) {
				FloorModel f = build.getFloors().get(index);

				AsyncTask<Void, Void, String> task = new DownloadRadioMapTaskBuid(new DownloadRadioMapTaskBuid.DownloadRadioMapListener() {

					@Override
					public void onSuccess(String result) {
						l.onProgressUpdate(progress_current++, progress_total);
						fetchAllRadioMaps(index + 1);
					}

					@Override
					public void onErrorOrCancel(String result) {
						status = Status.STOPPED;
						l.onErrorOrCancel(result, BackgroundFetchListener.ErrorType.EXCEPTION);
					}

					@Override
					public void onPrepareLongExecute() {

					}

				}, ctx, build.getLatitudeString(), build.getLongitudeString(), build.buid, f.floor_number, false);

				int currentapiVersion = android.os.Build.VERSION.SDK_INT;
				if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
					// Execute task parallel with others
					currentTask = task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
				} else {
					currentTask = task.execute();
				}

			} else {
				status = Status.SUCCESS;
				l.onSuccess("Finished loading building");
			}
		} else {
			l.onErrorOrCancel("Fetch Floor Plans Error", error);
		}
	}

	public void cancel() {
		error = BackgroundFetchListener.ErrorType.CANCELLED;
		if (currentTask != null) {
			currentTask.cancel(true);
		}
	}

	
}
