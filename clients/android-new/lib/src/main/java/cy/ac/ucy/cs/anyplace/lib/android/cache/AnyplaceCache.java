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

package cy.ac.ucy.cs.anyplace.lib.android.cache;

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

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;


/*
import com.dmsl.anyplace.AnyplaceAPI;
import com.dmsl.anyplace.MyApplication;

import com.dmsl.anyplace.nav.BuildingModel.FetchBuildingTaskListener;
import com.dmsl.anyplace.tasks.FetchBuildingsTask;
import com.dmsl.anyplace.tasks.FetchBuildingsTask.FetchBuildingsTaskListener;
import com.dmsl.anyplace.utils.NetworkUtils;
*/
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceAPI;
import cy.ac.ucy.cs.anyplace.lib.android.LOG;
import cy.ac.ucy.cs.anyplace.lib.android.nav.BuildingModel;
import cy.ac.ucy.cs.anyplace.lib.android.nav.PoisModel;
import cy.ac.ucy.cs.anyplace.lib.android.tasks.FetchBuildingsTask;
import cy.ac.ucy.cs.anyplace.lib.android.utils.NetworkUtils;


/**
 * This class should provide the last fetched Buildings, Floors and POIs. At the
 * moment it's just static data structures but they should be implemented as a
 * local database and being retrieved as a ContentProvider.
 * 
 *
 */
@SuppressWarnings("serial")
public class AnyplaceCache implements Serializable {
  private static final String TAG = AnyplaceCache.class.getSimpleName();

  private static AnyplaceCache mInstance = null;
  private final Context ctx;


  public static AnyplaceCache getInstance(Context ctx) {
        // Log.e(TAG, "AnyplaceCache getting dir. -> " + ctx.getCacheDir().toString());

    // if(ActivityCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE)
    //         == PackageManager.PERMISSION_GRANTED){
    //   LOG.e("Unable to write to external storage due to no permissions. WRITE_EXTERNAL_STORAGE");
    //
    //
    // }
    // else if(true){
    //
    // }
    // if(ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
    //         != PackageManager.PERMISSION_GRANTED){
    //   LOG.e("No access to fine location");
    //
    // }
    //
    // File AnyplaceAppDir = new File(Environment.getExternalStorageDirectory()
    //         +File.separator+"AnyplaceCache");
    //
    // if(!AnyplaceAppDir.exists() && !AnyplaceAppDir.isDirectory())
    // {
    //   // create empty directory
    //   if (AnyplaceAppDir.mkdirs())
    //   {
    //     Log.i("CreateDir","AnyplaceCache dir created");
    //   }
    //   else
    //   {
    //     Log.e("CreateDir","Unable to create AnyplaceCache dir!");
    //   }
    // }
    // else
    // {
    //   Log.i("CreateDir","AnyplaceCache dir already exists");
    // }
    //
    //
    //
    //
    //     LOG.i(AnyplaceAppDir.getAbsolutePath());

		if (mInstance == null) {
			synchronized (ctx) {
				if (mInstance == null) {
					mInstance = getObject(ctx, ctx.getCacheDir());
					// mInstance = getObject(ctx, AnyplaceAppDir);
					LOG.i("cache dir : " + (mInstance == null ? "NULL" :  "resolved"));
					//TODO: Create an internal private directory and replace cache
				}
				if (mInstance == null) {
					mInstance = new AnyplaceCache(ctx);
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
	private List<BuildingModel> mSpinnerBuildings = new ArrayList<>(0);
	private List<BuildingModel> mWorldBuildings = new ArrayList<>(0);

	// last fetched pois
	private Map<String, PoisModel> mLoadedPoisMap;
	private String poisBUID;


	private AnyplaceCache(Context ctx) {
		// last fetched Buildings
		this.mSpinnerBuildings = new ArrayList<BuildingModel>();
		// last fetched pois
		this.mLoadedPoisMap = new HashMap<String, PoisModel>();
		this.ctx = ctx;

	}

	// </All Buildings
	public List<BuildingModel> loadWorldBuildings(final FetchBuildingsTask.FetchBuildingsTaskListener fetchBuildingsTaskListener, final Context ctx, Boolean forceReload) {
		if ((forceReload && NetworkUtils.isOnline(ctx)) || mWorldBuildings.isEmpty()) {
			new FetchBuildingsTask(new FetchBuildingsTask.FetchBuildingsTaskListener() {

				@Override
				public void onSuccess(String result, List<BuildingModel> buildings) {
					mWorldBuildings = buildings;
					AnyplaceCache.saveInstance(ctx);
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

	public void loadBuilding(final String buid, final BuildingModel.FetchBuildingTaskListener l, Context ctx) {

		loadWorldBuildings(new FetchBuildingsTask.FetchBuildingsTaskListener() {

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

  /**
   * @param ctx Context
   * @param mLoadedBuildings Loaded buildings
   */
	public void setSpinnerBuildings(Context ctx, List<BuildingModel> mLoadedBuildings) {
		this.mSpinnerBuildings = mLoadedBuildings;
		AnyplaceCache.saveInstance(ctx);
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

	public void setPois(Context ctx,Map<String, PoisModel> lpID, String poisBUID) {
		this.mLoadedPoisMap = lpID;
		this.poisBUID = poisBUID;
		AnyplaceCache.saveInstance(ctx);
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
			bf = new BackgroundFetch(ctx, l, build);
			bf.run();
		} else if (!bf.build.buid.equals(build.buid)) {
			// Navigated to another building
			bf.cancel();
			l.onPrepareLongExecute();
			bf = new BackgroundFetch(ctx, l, build);
			bf.run();
		} else if (bf.status == BackgroundFetchListener.Status.SUCCESS) {
			// Previously finished for the current building
			l.onSuccess("Already Downloaded");
		} else if (bf.status == BackgroundFetchListener.Status.STOPPED) {
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

	public BackgroundFetchListener.Status fetchAllFloorsRadiomapStatus() {
		return bf.status;
	}

	// />Fetch all Floor and Radiomaps of the current building

	// </SAVE CACHE
	public static boolean saveObject(Context ctx, File cacheDir, AnyplaceCache obj) {



      if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE)
              != PackageManager.PERMISSION_GRANTED ){

        LOG.e("Need write permissions to save object");

      }
      else{
        Log.d(TAG,"We have write permissions");
      }

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
            LOG.i(2, "AnyplaceCache: saveObject :" + e.getMessage());
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
      if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE)
              != PackageManager.PERMISSION_GRANTED ){

        LOG.e(TAG,"Need read permissions to read object");

      }

      else{
        LOG.i(TAG,"We have read permissions");
      }


		AnyplaceCache simpleClass = null;
		FileInputStream fis = null;
		ObjectInputStream is = null;

		try {
			fis = new FileInputStream(suspend_f);
			is = new ObjectInputStream(fis);
			simpleClass = (AnyplaceCache) is.readObject();
		} catch (Exception e) {
          LOG.i(2, "AnyplaceCache: getObject :" + e.getMessage());
			if (AnyplaceAPI.DEBUG_MESSAGES)
				Toast.makeText(ctx, "AnyplaceCache: getObject :" + e.getMessage(), Toast.LENGTH_LONG).show();
		} finally {
			try {
				if (fis != null)
					fis.close();
				if (is != null)
					is.close();
			} catch (Exception e) {
              LOG.i(2, "AnyplaceCache : getObject finally:" + e.getMessage());
			}
		}

		return simpleClass;
	}
}
