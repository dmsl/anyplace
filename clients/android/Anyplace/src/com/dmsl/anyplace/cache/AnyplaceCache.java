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

package com.dmsl.anyplace.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.widget.Toast;

import com.dmsl.anyplace.AnyplaceAPI;
import com.dmsl.anyplace.MyApplication;
import com.dmsl.anyplace.cache.BackgroundFetchListener.Status;
import com.dmsl.anyplace.nav.BuildingModel;
import com.dmsl.anyplace.nav.PoisModel;
import com.dmsl.anyplace.nav.BuildingModel.FetchBuildingTaskListener;
import com.dmsl.anyplace.tasks.FetchBuildingsTask;
import com.dmsl.anyplace.tasks.FetchBuildingsTask.FetchBuildingsTaskListener;
import com.dmsl.anyplace.utils.NetworkUtils;

/**
 * This class should provide the last fetched Buildings, Floors and POIs. At the
 * moment it's just static data structures but they should be implemented as a
 * local database and being retrieved as a ContentProvider.
 * 
 *
 */
@SuppressWarnings("serial")
public class AnyplaceCache implements Serializable {

	private static AnyplaceCache mInstance = null;

	public static AnyplaceCache getInstance(Context ctx) {
		if (mInstance == null) {
			synchronized (MyApplication.getAppContext()) {
				if (mInstance == null) {
					mInstance = getObject(ctx, ctx.getCacheDir());
				}
				if (mInstance == null) {
					mInstance = new AnyplaceCache();
				}
			}
		}
		return mInstance;
	}

	public static void saveInstance(Context ctx) {
		saveObject(ctx, ctx.getCacheDir(), getInstance(ctx));
	}

	private transient BackgroundFetch bf = null;

	private int selectedBuilding = 0;
	// last fetched Buildings
	private List<BuildingModel> mSpinnerBuildings = new ArrayList<BuildingModel>(0);
	public List<BuildingModel> mWorldBuildings = new ArrayList<BuildingModel>(0);

	// last fetched pois
	private Map<String, PoisModel> mLoadedPoisMap;
	private String poisBUID;

	private AnyplaceCache() {
		// last fetched Buildings
		this.mSpinnerBuildings = new ArrayList<BuildingModel>();
		// last fetched pois
		this.mLoadedPoisMap = new HashMap<String, PoisModel>();
	}

	// </All Buildings
	public List<BuildingModel> loadWorldBuildings(final FetchBuildingsTaskListener fetchBuildingsTaskListener, Context ctx, Boolean forceReload) {
		if ((forceReload && NetworkUtils.isOnline(ctx)) || mWorldBuildings.isEmpty()) {
			new FetchBuildingsTask(new FetchBuildingsTask.FetchBuildingsTaskListener() {

				@Override
				public void onSuccess(String result, List<BuildingModel> buildings) {
					mWorldBuildings = buildings;
					AnyplaceCache.saveInstance(MyApplication.getAppContext());
					fetchBuildingsTaskListener.onSuccess(result, buildings);
				}

				@Override
				public void onErrorOrCancel(String result) {
					fetchBuildingsTaskListener.onErrorOrCancel(result);
				}

			}, ctx).execute();
		} else {
			fetchBuildingsTaskListener.onSuccess("Successfully read from cache", mWorldBuildings);
		}
		return mWorldBuildings;
	}

	public void loadBuilding(final String buid, final FetchBuildingTaskListener l, Context ctx) {

		loadWorldBuildings(new FetchBuildingsTaskListener() {

			@Override
			public void onSuccess(String result, List<BuildingModel> buildings) {
				BuildingModel fcb = null;
				for (BuildingModel b : buildings) {
					if (b.buid.equals(buid)) {
						fcb = b;
						break;
					}
				}

				if (fcb != null) {
					l.onSuccess("Success", fcb);
				} else {
					l.onErrorOrCancel("Building not found");
				}
			}

			@Override
			public void onErrorOrCancel(String result) {
				l.onErrorOrCancel(result);
			}
		}, ctx, false);
	}

	// </Buildings Spinner in Select Building Activity
	public List<BuildingModel> getSpinnerBuildings() {

		return mSpinnerBuildings;
	}

	public void setSpinnerBuildings(List<BuildingModel> mLoadedBuildings) {
		this.mSpinnerBuildings = mLoadedBuildings;
		AnyplaceCache.saveInstance(MyApplication.getAppContext());
	}

	// Use nav/AnyUserData for the loaded building in Navigator
	public BuildingModel getSelectedBuilding() {
		BuildingModel b = null;
		try {
			b = mSpinnerBuildings.get(selectedBuilding);
		} catch (IndexOutOfBoundsException ex) {

		}

		return b;
	}

	public int getSelectedBuildingIndex() {
		if (!(selectedBuilding < mSpinnerBuildings.size()))
			selectedBuilding = 0;

		return selectedBuilding;
	}

	public void setSelectedBuildingIndex(int selectedBuildingIndex) {
		this.selectedBuilding = selectedBuildingIndex;
	}

	// />

	// /< POIS
	public Collection<PoisModel> getPois() {
		return this.mLoadedPoisMap.values();
	}

	public Map<String, PoisModel> getPoisMap() {
		return this.mLoadedPoisMap;
	}

	public void setPois(Map<String, PoisModel> lpID, String poisBUID) {
		this.mLoadedPoisMap = lpID;
		this.poisBUID = poisBUID;
		AnyplaceCache.saveInstance(MyApplication.getAppContext());
	}

	// Check the loaded pois if match the Building ID
	public boolean checkPoisBUID(String poisBUID) {
		if (this.poisBUID != null && this.poisBUID.equals(poisBUID))
			return true;
		else
			return false;
	}

	// />POIS

	public void fetchAllFloorsRadiomapsRun(BackgroundFetchListener l, final BuildingModel build) {

		if (bf == null) {
			l.onPrepareLongExecute();
			bf = new BackgroundFetch(l, build);
			bf.run();
		} else if (!bf.build.buid.equals(build.buid)) {
			// Navigated to another building
			bf.cancel();
			l.onPrepareLongExecute();
			bf = new BackgroundFetch(l, build);
			bf.run();
		} else if (bf.status == Status.SUCCESS) {
			// Previously finished for the current building
			l.onSuccess("Already Downloaded");
		} else if (bf.status == Status.STOPPED) {
			// Task Download Error Occurred
			l.onErrorOrCancel("Task Failed", BackgroundFetchListener.ErrorType.EXCEPTION);
		} else {
			l.onErrorOrCancel("Another instance is running", BackgroundFetchListener.ErrorType.SINGLE_INSTANCE);
		}
	}

	public void fetchAllFloorsRadiomapReset() {
		if (bf != null)
			bf = null;
	}

	public Status fetchAllFloorsRadiomapStatus() {
		return bf.status;
	}

	// />Fetch all Floor and Radiomaps of the current building

	// </SAVE CACHE
	public static boolean saveObject(Context ctx, File cacheDir, AnyplaceCache obj) {

		final File suspend_f = new File(cacheDir, "AnyplaceCache");

		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		boolean keep = true;

		try {
			fos = new FileOutputStream(suspend_f);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(obj);
		} catch (Exception e) {
			keep = false;
			if (AnyplaceAPI.DEBUG_MESSAGES)
				Toast.makeText(ctx, "AnyplaceCache: saveObject :" + e.getMessage(), Toast.LENGTH_LONG).show();
		} finally {
			try {
				if (oos != null)
					oos.close();
				if (fos != null)
					fos.close();
				if (keep == false)
					suspend_f.delete();
			} catch (Exception e) { /* do nothing */
			}
		}

		return keep;
	}

	public static AnyplaceCache getObject(Context ctx, File cacheDir) {
		final File suspend_f = new File(cacheDir, "AnyplaceCache");

		AnyplaceCache simpleClass = null;
		FileInputStream fis = null;
		ObjectInputStream is = null;

		try {
			fis = new FileInputStream(suspend_f);
			is = new ObjectInputStream(fis);
			simpleClass = (AnyplaceCache) is.readObject();
		} catch (Exception e) {
			if (AnyplaceAPI.DEBUG_MESSAGES)
				Toast.makeText(ctx, "AnyplaceCache: getObject :" + e.getMessage(), Toast.LENGTH_LONG).show();
		} finally {
			try {
				if (fis != null)
					fis.close();
				if (is != null)
					is.close();
			} catch (Exception e) {
			}
		}

		return simpleClass;
	}
}
