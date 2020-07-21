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

package com.dmsl.anyplace;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.Cursor;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.text.TextPaint;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.actionbarsherlock.widget.SearchView;
import com.circlegate.tt.cg.an.lib.map.MapWrapperLayout;
import com.circlegate.tt.cg.an.lib.map.OnInfoWindowElemTouchListener;
import com.dmsl.anyplace.AnyplacePrefs.Action;
import com.dmsl.anyplace.cache.AnyplaceCache;
import com.dmsl.anyplace.cache.BackgroundFetchListener;
import com.dmsl.anyplace.floor.Algo1Radiomap;
import com.dmsl.anyplace.floor.Algo1Server;
import com.dmsl.anyplace.floor.FloorSelector;
import com.dmsl.anyplace.floor.FloorSelector.ErrorAnyplaceFloorListener;
import com.dmsl.anyplace.floor.FloorSelector.FloorAnyplaceFloorListener;
import com.dmsl.anyplace.floor.FloorSelector.NonCriticalError;
import com.dmsl.anyplace.googlemap.AnyPlaceMapTileProvider;
import com.dmsl.anyplace.googlemap.MyBuildingsRenderer;
import com.dmsl.anyplace.googlemap.VisiblePois;
import com.dmsl.anyplace.logger.AnyplaceLoggerActivity;
import com.dmsl.anyplace.nav.*;
import com.dmsl.anyplace.nav.AnyPlaceSeachingHelper.HTMLCursorAdapter;
import com.dmsl.anyplace.nav.AnyPlaceSeachingHelper.SearchTypes;
import com.dmsl.anyplace.nav.BuildingModel.FetchBuildingTaskListener;
import com.dmsl.anyplace.sensors.MovementDetector;
import com.dmsl.anyplace.sensors.SensorsMain;
import com.dmsl.anyplace.sensors.SensorsStepCounter;
import com.dmsl.anyplace.tasks.*;
import com.dmsl.anyplace.tasks.FetchBuildingsTask.FetchBuildingsTaskListener;
import com.dmsl.anyplace.tasks.FetchFloorsByBuidTask.FetchFloorsByBuidTaskListener;
import com.dmsl.anyplace.tasks.FetchPoisByBuidTask.FetchPoisListener;
import com.dmsl.anyplace.tracker.*;
import com.dmsl.anyplace.utils.*;
import com.dmsl.anyplace.wifi.SimpleWifiManager;
import com.flurry.android.FlurryAgent;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.*;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.ClusterManager.OnClusterClickListener;
import com.google.maps.android.clustering.ClusterManager.OnClusterItemClickListener;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UnifiedNavigationActivity extends SherlockFragmentActivity implements AnyplaceTracker.TrackedLocAnyplaceTrackerListener, AnyplaceTracker.WifiResultsAnyplaceTrackerListener, AnyplaceTracker.ErrorAnyplaceTrackerListener, GooglePlayServicesClient.ConnectionCallbacks, GooglePlayServicesClient.OnConnectionFailedListener, LocationListener, FloorAnyplaceFloorListener, ErrorAnyplaceFloorListener, OnSharedPreferenceChangeListener {

    private static final double csLat = 35.144569;
    private static final double csLon = 33.411107;
    private static final float mInitialZoomLevel = 19.0f;
    public static final String SHARED_PREFS_ANYPLACE = "Anyplace_Preferences";

    // Define a request code to send to Google Play services This code is
    // returned in Activity.onActivityResult
    private final static int LOCATION_CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9001;
    private final static int SELECT_PLACE_ACTIVITY_RESULT = 1112;
    private final static int SEARCH_POI_ACTIVITY_RESULT = 1113;
    private final static int PREFERENCES_ACTIVITY_RESULT = 1114;

    // Location API
    private LocationClient mLocationClient;
    // Define an object that holds accuracy and frequency parameters
    private LocationRequest mLocationRequest;

    // UI Elements
    private ProgressBar progressBar;
    private ImageButton btnFloorUp;
    private ImageButton btnFloorDown;
    private TextView textFloor;
    private TextView detectedAPs;
    private ImageButton btnTrackme;
    private TextView textDebug;

    private SearchTypes searchType = SearchTypes.OUTDOOR_MODE;
    private SearchView searchView;
    private AnyplaceSuggestionsTask mSuggestionsTask;

    // <Tasks>
    private DownloadRadioMapTaskBuid downloadRadioMapTaskBuid;
    private boolean floorChangeRequestDialog = false;
    // </Tasks>
    private boolean mAutomaticGPSBuildingSelection;

    /**
     * Note that this may be null if the Google Play services APK is not available.
     */
    private GoogleMap mMap;
    private boolean cameraUpdate = false;
    private float bearing;

    // Navigation
    private AnyUserData userData = null;
    // holds the lines for the navigation route on map
    private Polyline pathLineInside = null;
    private PolylineOptions pathLineOutdoorOptions = null;
    private Polyline pathLineOutdoor = null;

    private AnyplaceCache mAnyplaceCache = null;
    // holds the PoisModels and Markers on map
    private VisiblePois visiblePois = null;
    private ClusterManager<BuildingModel> mClusterManager;

    // AnyplaceTracker
    private SensorsMain sensorsMain; // acceleration and orientation
    private MovementDetector movementDetector; // walking vs standing
    private SensorsStepCounter sensorsStepCounter; // step counter

    private TrackerLogicPlusIMU lpTracker;
    private Algo1Radiomap floorSelector;
    private String lastFloor;

    private boolean isTrackingErrorBackground;
    private Marker userMarker = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_unifiednav);

        detectedAPs = (TextView) findViewById(R.id.detectedAPs);
        textFloor = (TextView) findViewById(R.id.textFloor);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        textDebug = (TextView) findViewById(R.id.textDebug);
        if (AnyplaceAPI.DEBUG_MESSAGES)
            textDebug.setVisibility(View.VISIBLE);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);

        userData = new AnyUserData();

        SimpleWifiManager.getInstance().startScan();
        sensorsMain = new SensorsMain(getApplicationContext());
        movementDetector = new MovementDetector();
        sensorsMain.addListener(movementDetector);
        sensorsStepCounter = new SensorsStepCounter(getApplicationContext(), sensorsMain);
        lpTracker = new TrackerLogicPlusIMU(movementDetector, sensorsMain, sensorsStepCounter);
        // lpTracker = new TrackerLogic(sensorsMain);
        floorSelector = new Algo1Radiomap(getApplicationContext());

        mAnyplaceCache = AnyplaceCache.getInstance(this);
        visiblePois = new VisiblePois();

        setUpMapIfNeeded();

        // setup the trackme button overlaid in the map
        btnTrackme = (ImageButton) findViewById(R.id.btnTrackme);
        btnTrackme.setImageResource(R.drawable.dark_device_access_location_off);
        isTrackingErrorBackground = true;
        btnTrackme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                final GeoPoint gpsLoc = userData.getLocationGPSorIP();
                if (gpsLoc != null) {
                    AnyplaceCache mAnyplaceCache = AnyplaceCache.getInstance(UnifiedNavigationActivity.this);
                    mAnyplaceCache.loadWorldBuildings(new FetchBuildingsTaskListener() {

                        @Override
                        public void onSuccess(String result, List<BuildingModel> buildings) {
                            final FetchNearBuildingsTask nearest = new FetchNearBuildingsTask();
                            nearest.run(buildings.iterator(), gpsLoc.lat, gpsLoc.lng, 200);

                            if (nearest.buildings.size() > 0 && (userData.getSelectedBuildingId() == null || !userData.getSelectedBuildingId().equals(nearest.buildings.get(0).buid))) {
                                floorSelector.Stop();
                                final FloorSelector floorSelectorAlgo1 = new Algo1Server(getApplicationContext());
                                final ProgressDialog floorSelectorDialog = new ProgressDialog(UnifiedNavigationActivity.this);

                                floorSelectorDialog.setIndeterminate(true);
                                floorSelectorDialog.setTitle("Detecting floor");
                                floorSelectorDialog.setMessage("Please be patient...");
                                floorSelectorDialog.setCancelable(true);
                                floorSelectorDialog.setCanceledOnTouchOutside(false);
                                floorSelectorDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        floorSelectorAlgo1.Destoy();
                                        bypassSelectBuildingActivity(nearest.buildings.get(0), "0", false);
                                    }
                                });

                                class Callback implements ErrorAnyplaceFloorListener, FloorAnyplaceFloorListener {

                                    @Override
                                    public void onNewFloor(String floor) {
                                        floorSelectorAlgo1.Destoy();
                                        if (floorSelectorDialog.isShowing()) {
                                            floorSelectorDialog.dismiss();
                                            bypassSelectBuildingActivity(nearest.buildings.get(0), floor, false);
                                        }
                                    }

                                    @Override
                                    public void onFloorError(Exception ex) {
                                        floorSelectorAlgo1.Destoy();
                                        if (floorSelectorDialog.isShowing()) {
                                            floorSelectorDialog.dismiss();
                                            bypassSelectBuildingActivity(nearest.buildings.get(0), "0", false);
                                        }
                                    }

                                }
                                Callback callback = new Callback();
                                floorSelectorAlgo1.addListener((FloorAnyplaceFloorListener) callback);
                                floorSelectorAlgo1.addListener((ErrorAnyplaceFloorListener) callback);

                                // Show Dialog
                                floorSelectorDialog.show();
                                floorSelectorAlgo1.Start(gpsLoc.lat, gpsLoc.lng);
                            } else {
                                focusUserLocation();

                                // Clear cancel request
                                lastFloor = null;
                                floorSelector.RunNow();
                                lpTracker.reset();
                            }
                        }

                        @Override
                        public void onErrorOrCancel(String result) {

                        }

                    }, UnifiedNavigationActivity.this, false);
                } else {
                    focusUserLocation();

                    // Clear cancel request
                    lastFloor = null;
                    floorSelector.RunNow();
                    lpTracker.reset();
                }

            }
        });

        btnFloorUp = (ImageButton) findViewById(R.id.btnFloorUp);
        btnFloorUp.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (!userData.isFloorSelected()) {
                    Toast.makeText(getBaseContext(), "Load a map before tracking can be used!", Toast.LENGTH_SHORT).show();
                    return;
                }

                BuildingModel b = userData.getSelectedBuilding();
                if (b == null) {
                    return;
                }

                if (userData.isNavBuildingSelected()) {
                    // Move to start/destination poi's floor
                    String floor_number;
                    List<PoisNav> puids = userData.getNavPois();
                    // Check start and destination floor number
                    if (!puids.get(puids.size() - 1).floor_number.equals(puids.get(0).floor_number)) {
                        if (userData.getSelectedFloorNumber().equals(puids.get(puids.size() - 1).floor_number)) {
                            floor_number = puids.get(0).floor_number;
                        } else {
                            floor_number = puids.get(puids.size() - 1).floor_number;
                        }

                        FloorModel floor = b.getFloorFromNumber(floor_number);
                        if (floor != null) {
                            bypassSelectBuildingActivity(b, floor);
                            return;
                        }
                    }
                }

                // Move one floor up
                int index = b.getSelectedFloorIndex();

                if (b.checkIndex(index + 1)) {
                    bypassSelectBuildingActivity(b, b.getFloors().get(index + 1));
                }

            }
        });

        btnFloorDown = (ImageButton) findViewById(R.id.btnFloorDown);
        btnFloorDown.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!userData.isFloorSelected()) {
                    Toast.makeText(getBaseContext(), "Load a map before tracking can be used!", Toast.LENGTH_SHORT).show();
                    return;
                }

                BuildingModel b = userData.getSelectedBuilding();
                if (b == null) {
                    return;
                }

                if (userData.isNavBuildingSelected()) {
                    // Move to start/destination poi's floor
                    String floor_number;
                    List<PoisNav> puids = userData.getNavPois();
                    // Check start and destination floor number
                    if (!puids.get(puids.size() - 1).floor_number.equals(puids.get(0).floor_number)) {
                        if (userData.getSelectedFloorNumber().equals(puids.get(puids.size() - 1).floor_number)) {
                            floor_number = puids.get(0).floor_number;
                        } else {
                            floor_number = puids.get(puids.size() - 1).floor_number;
                        }

                        FloorModel floor = b.getFloorFromNumber(floor_number);
                        if (floor != null) {
                            bypassSelectBuildingActivity(b, floor);
                            return;
                        }
                    }
                }

                // Move one floor down
                int index = b.getSelectedFloorIndex();

                if (b.checkIndex(index - 1)) {
                    bypassSelectBuildingActivity(b, b.getFloors().get(index - 1));
                }
            }

        });

		/*
         * Create a new location client, using the enclosing class to handle callbacks.
		 */
        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create();
        // Use high accuracy
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        // Set the update interval to 2 seconds
        mLocationRequest.setInterval(2000);
        // Set the fastest update interval to 1 second
        mLocationRequest.setFastestInterval(1000);
        mLocationClient = new LocationClient(this, this, this);
        // declare that this is the first time this Activity launched so make
        // the automatic building selection
        mAutomaticGPSBuildingSelection = true;

        // get/set settings
        PreferenceManager.setDefaultValues(this, SHARED_PREFS_ANYPLACE, MODE_PRIVATE, R.xml.preferences_anyplace, true);
        SharedPreferences preferences = getSharedPreferences(SHARED_PREFS_ANYPLACE, MODE_PRIVATE);
        preferences.registerOnSharedPreferenceChangeListener(this);
        lpTracker.setAlgorithm(preferences.getString("TrackingAlgorithm", "WKNN"));

        // handle the search intent
        handleIntent(getIntent());
    }

    private void focusUserLocation() {
        if (userMarker != null) {
            if (AnyPlaceSeachingHelper.getSearchType(mMap.getCameraPosition().zoom) == SearchTypes.OUTDOOR_MODE) {
                cameraUpdate = true;
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userMarker.getPosition(), mInitialZoomLevel), new CancelableCallback() {

                    @Override
                    public void onFinish() {
                        cameraUpdate = false;
                    }

                    @Override
                    public void onCancel() {
                        cameraUpdate = false;
                    }
                });
            } else {

                cameraUpdate = true;
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userMarker.getPosition(), mMap.getCameraPosition().zoom), new CancelableCallback() {

                    @Override
                    public void onFinish() {
                        cameraUpdate = false;
                    }

                    @Override
                    public void onCancel() {
                        cameraUpdate = false;
                    }
                });

            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        mLocationClient.connect();

        // Flurry Analytics
        if (AnyplaceAPI.FLURRY_ENABLE) {
            FlurryAgent.onStartSession(this, AnyplaceAPI.FLURRY_APIKEY);
        }

        Runnable checkGPS = new Runnable() {
            @Override
            public void run() {
                LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                boolean statusOfGPS = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
                if (statusOfGPS == false) {
                    AndroidUtils.showGPSSettings(UnifiedNavigationActivity.this);
                }
            }
        };

        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        boolean isWifiOn = wifi.isWifiEnabled();
        boolean isOnline = NetworkUtils.isOnline(UnifiedNavigationActivity.this);

        if (!isOnline) {
            AndroidUtils.showWifiSettings(this,"No Internet Connection", null, checkGPS);
        }
        else if (!isWifiOn){
            AndroidUtils.showWifiSettings(this,"WiFi is disabled", null, checkGPS);
        }
        else {
            checkGPS.run();
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        addTrackerListeners();
        // check the Play Services
        checkPlayServices();
        sensorsMain.resume();
        sensorsStepCounter.resume();
        lpTracker.resumeTracking();
        floorSelector.resumeTracking();
    }

    @Override
    protected void onPause() {
        super.onPause();
        lpTracker.pauseTracking();
        floorSelector.pauseTracking();
        sensorsMain.pause();
        sensorsStepCounter.pause();
        removeTrackerListeners();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Disconnecting the client invalidates it.
        mLocationClient.disconnect();

        // Flurry Analytics
        if (AnyplaceAPI.FLURRY_ENABLE) {
            FlurryAgent.onEndSession(this);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.unified_options_menu, menu);

        // ****************************************** Search View
        // ***************************************************************** /
        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setQueryHint("Search outdoor");
        searchView.setAddStatesFromChildren(true);

        // set query change listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextChange(final String newText) {
                // return false; // false since we do not handle this call

                if (newText == null || newText.trim().length() < 1) {
                    if (mSuggestionsTask != null && !mSuggestionsTask.isCancelled()) {
                        mSuggestionsTask.cancel(true);
                    }
                    searchView.setSuggestionsAdapter(null);
                    return true;
                }

                if (mSuggestionsTask != null) {
                    mSuggestionsTask.cancel(true);
                }

                if (searchType == SearchTypes.INDOOR_MODE) {
                    if (!userData.isFloorSelected()) {
                        List<IPoisClass> places = new ArrayList<IPoisClass>(1);
                        PoisModel pm = new PoisModel();
                        pm.name = "Load a building first ...";
                        places.add(pm);
                        Cursor cursor = AnyPlaceSeachingHelper.prepareSearchViewCursor(places);
                        showSearchResult(cursor);
                        return true;
                    }
                }

                GeoPoint gp = userData.getLatestUserPosition();

                mSuggestionsTask = new AnyplaceSuggestionsTask(new AnyplaceSuggestionsTask.AnyplaceSuggestionsListener() {
                    @Override
                    public void onSuccess(String result, List<? extends IPoisClass> pois) {
                        showSearchResult(AnyPlaceSeachingHelper.prepareSearchViewCursor(pois, newText));
                    }

                    @Override
                    public void onErrorOrCancel(String result) {
                        Log.d("AnyplaceSuggestions", result);
                    }

                    @Override
                    public void onUpdateStatus(String string, Cursor cursor) {
                        showSearchResult(cursor);
                    }

                }, UnifiedNavigationActivity.this, searchType, (gp == null) ? new GeoPoint(csLat, csLon) : gp, newText);
                mSuggestionsTask.execute(null, null);

                // we return true to avoid caling the provider set in the xml
                return true;
            }

            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }
        });
        searchView.setSubmitButtonEnabled(true);
        searchView.setQueryRefinementEnabled(false);

        // ****************************************** Select building
        // ***************************************************************** /
        // Select building and floor to start navigating and positioning
        final SubMenu subMenuPlace = menu.addSubMenu("Select Building");
        final MenuItem sPlace = subMenuPlace.getItem();
        sPlace.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        sPlace.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                // start the activity where the user can select the FROM and TO
                // pois he wants to navigate
                GeoPoint gp = userData.getLatestUserPosition();
                loadSelectBuildingActivity(gp, false);
                return true;
            }
        });

        // ********************************** CLEAR NAVIGATION
        // *********************************************** /
        final SubMenu subMenuResetNav = menu.addSubMenu("Clear Navigation");
        final MenuItem ResetNav = subMenuResetNav.getItem();
        ResetNav.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        ResetNav.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                clearNavigationData();
                return true;
            }
        });

        // ***********************Load Ayplace Logger
        // ************************************** /
        final SubMenu subMenuLoadLogger = menu.addSubMenu("Show Logger");
        final MenuItem LoadLogger = subMenuLoadLogger.getItem();
        LoadLogger.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        LoadLogger.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent intent = new Intent(getApplicationContext(), AnyplaceLoggerActivity.class);
                startActivity(intent);
                return true;
            }
        });

        // ****************************************** preferences
        // ********************************************** /
        final SubMenu subMenuPreferences = menu.addSubMenu("Preferences");
        final MenuItem prefsMenu = subMenuPreferences.getItem();
        prefsMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        prefsMenu.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                Intent i = new Intent(UnifiedNavigationActivity.this, AnyplacePrefs.class);
                startActivityForResult(i, PREFERENCES_ACTIVITY_RESULT);
                return true;
            }
        });

        // ****************************************** about
        // ********************************************** /
        final SubMenu subMenuAbout = menu.addSubMenu("About");
        final MenuItem about = subMenuAbout.getItem();
        about.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        about.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                startActivity(new Intent(UnifiedNavigationActivity.this, AnyplaceAboutActivity.class));
                return true;
            }
        });

        // ****************************************** exit
        // ********************************************** /
        final SubMenu subMenuExit = menu.addSubMenu("Exit");
        final MenuItem Exit = subMenuExit.getItem();
        Exit.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        Exit.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                finish();
                return true;
            }
        });

        /***************************************** END OF MAIN MENU ***************************************************************/

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        /*
         * case android.R.id.home: // finish(); break;
		 */
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSearchResult(Cursor cursor) {
        // bind the text data from the results to the
        // custom
        // layout
        String[] from = {SearchManager.SUGGEST_COLUMN_TEXT_1
                // ,SearchManager.SUGGEST_COLUMN_TEXT_2
        };
        int[] to = {android.R.id.text1
                // ,android.R.id.text2
        };
        // add the cursor of the results to the search view
        // SimpleCursorAdapter adapter = new SimpleCursorAdapter(UnifiedNavigationActivity.this, R.layout.queried_pois_item_1_searchbox, cursor, from, to, 0);
        // searchView.setSuggestionsAdapter(adapter);
        // adapter.notifyDataSetChanged();

        AnyPlaceSeachingHelper.HTMLCursorAdapter adapter = new HTMLCursorAdapter(UnifiedNavigationActivity.this, R.layout.queried_pois_item_1_searchbox, cursor, from, to);
        searchView.setSuggestionsAdapter(adapter);
        adapter.notifyDataSetChanged();
    }


    // </ GOOGLE MAP FUNCTIONS

    // Called from onCreate or onResume
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the
        // map.
        if (mMap != null) {
            return;
        }
        // Try to obtain the map from the SupportMapFragment.
        mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getMap();
        mClusterManager = new ClusterManager<BuildingModel>(this, mMap);

        // Check if we were successful in obtaining the map.
        if (mMap != null) {

            // http://stackoverflow.com/questions/14123243/google-maps-android-api-v2-interactive-infowindow-like-in-original-android-go
            final MapWrapperLayout mapWrapperLayout = (MapWrapperLayout) findViewById(R.id.map_relative_layout);

            // MapWrapperLayout initialization
            // 39 - default marker height
            // 20 - offset between the default InfoWindow bottom edge and
            // it's content bottom edge
            mapWrapperLayout.init(mMap, getPixelsFromDp(this, 39 + 20));

            final ViewGroup infoWindow;
            final TextView infoTitle;
            final TextView infoSnippet;
            final Button infoButton1;
            final OnInfoWindowElemTouchListener infoButtonListener1;
            Button infoButton2;
            final OnInfoWindowElemTouchListener infoButtonListener2;

            // We want to reuse the info window for all the markers,
            // so let's create only one class member instance
            infoWindow = (ViewGroup) getLayoutInflater().inflate(R.layout.info_window, null);
            infoTitle = (TextView) infoWindow.findViewById(R.id.title);
            infoSnippet = (TextView) infoWindow.findViewById(R.id.snippet);
            infoButton1 = (Button) infoWindow.findViewById(R.id.button1);
            infoButton2 = (Button) infoWindow.findViewById(R.id.button2);

            // Setting custom OnTouchListener which deals with the pressed
            // state
            // so it shows up
            infoButtonListener1 = new OnInfoWindowElemTouchListener(infoButton1, getResources().getDrawable(R.drawable.button_unsel), getResources().getDrawable(R.drawable.button_sel)) {
                @Override
                protected void onClickConfirmed(View v, Marker marker) {

                    PoisModel poi = visiblePois.getPoisModelFromMarker(marker);
                    if (poi != null) {
                        // start the navigation using the clicked marker as
                        // destination
                        startNavigationTask(poi.puid);
                    }

                }
            };
            infoButton1.setOnTouchListener(infoButtonListener1);

            // Setting custom OnTouchListener which deals with the pressed
            // state
            // so it shows up
            infoButtonListener2 = new OnInfoWindowElemTouchListener(infoButton2, getResources().getDrawable(R.drawable.button_unsel), getResources().getDrawable(R.drawable.button_sel)) {
                @Override
                protected void onClickConfirmed(View v, Marker marker) {

                    PoisModel poi = visiblePois.getPoisModelFromMarker(marker);
                    if (poi != null) {
                        if (poi.description.equals("") || poi.description.equals("-")) {
                            // start the navigation using the clicked marker
                            // as destination
                            popup_msg("No description available.", poi.name);
                        } else {
                            popup_msg(poi.description, poi.name);
                        }

                    }

                }
            };
            infoButton2.setOnTouchListener(infoButtonListener2);

            mMap.setInfoWindowAdapter(new InfoWindowAdapter() {
                @Override
                public View getInfoWindow(Marker marker) {
                    return null;
                }

                @Override
                public View getInfoContents(Marker marker) {
                    // Setting up the infoWindow with current's marker info
                    infoTitle.setText(marker.getTitle());
                    infoSnippet.setText(marker.getSnippet());
                    infoButtonListener1.setMarker(marker);
                    infoButtonListener2.setMarker(marker);

                    // We must call this to set the current marker and
                    // infoWindow references
                    // to the MapWrapperLayout
                    mapWrapperLayout.setMarkerWithInfoWindow(marker, infoWindow);
                    return infoWindow;
                }
            });
            setUpMap();
        }

    }

    public static int getPixelsFromDp(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dp * scale + 0.5f);
    }

    // Called from setUpMapIfNeeded
    private void setUpMap() {
        initMap();
        // initCamera();
        initListeners();
    }

    // Called from setUpMap
    private void initMap() {
        // Sets the map type to be NORMAL - ROAD mode
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        // mMap.setMyLocationEnabled(true); //displays a button to navigate to
        // the current user's position
        // mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(this,
        // mPoiMarkersBundle));
        mMap.setBuildingsEnabled(false);
    }

    // Called from onConnecetd
    private void initCamera() {
        // Only for the first time
        if (userMarker != null) {
            return;
        }

        Location gps = mLocationClient.getLastLocation();
        if (gps != null) {
            cameraUpdate = true;
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(gps.getLatitude(), gps.getLongitude()), mInitialZoomLevel), new CancelableCallback() {

                @Override
                public void onFinish() {
                    cameraUpdate = false;
                    handleBuildingsOnMap(false);
                }

                @Override
                public void onCancel() {
                    cameraUpdate = false;
                    handleBuildingsOnMap(false);
                }
            });
        } else {
            AsyncTask<Void, Integer, Void> task = new AsyncTask<Void, Integer, Void>() {

                GeoPoint location;

                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        location = AndroidUtils.getIPLocation();
                    } catch (Exception e) {

                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {

                    if (location != null && userMarker == null) {
                        userData.setLocationIP(location);
                        updateLocation();
                        cameraUpdate = true;
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.dlat, location.dlon), mInitialZoomLevel), new CancelableCallback() {

                            @Override
                            public void onFinish() {
                                cameraUpdate = false;
                                handleBuildingsOnMap(false);
                            }

                            @Override
                            public void onCancel() {
                                cameraUpdate = false;
                                handleBuildingsOnMap(false);
                            }
                        });
                    } else {
                        handleBuildingsOnMap(false);
                    }

                }

            };

            int currentapiVersion = android.os.Build.VERSION.SDK_INT;
            if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            } else {
                task.execute();
            }
        }
    }

    // Called from setUpMap
    private void initListeners() {
        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition position) {

                // change search box message and clear pois
                if (searchType != AnyPlaceSeachingHelper.getSearchType(position.zoom)) {
                    searchType = AnyPlaceSeachingHelper.getSearchType(position.zoom);
                    if (searchType == SearchTypes.INDOOR_MODE) {
                        searchView.setQueryHint("Search indoor");
                        visiblePois.showAll();
                        if (pathLineInside != null)
                            pathLineInside.setVisible(true);
                    } else if (searchType == SearchTypes.OUTDOOR_MODE) {
                        searchView.setQueryHint("Search outdoor");
                        visiblePois.hideAll();
                        if (pathLineInside != null)
                            pathLineInside.setVisible(false);
                    }
                }

                bearing = position.bearing;
                mClusterManager.onCameraChange(position);
            }
        });

        mMap.setOnMarkerClickListener(new OnMarkerClickListener() {

            @Override
            public boolean onMarkerClick(Marker marker) {

                // mClusterManager returns true if is a cluster item
                if (!mClusterManager.onMarkerClick(marker)) {

                    PoisModel poi = visiblePois.getPoisModelFromMarker(marker);
                    if (poi != null) {
                        return false;
                    } else {
                        // Prevent Popup dialog
                        return true;
                    }
                } else {
                    // Prevent Popup dialog
                    return true;
                }
            }
        });

        mClusterManager.setOnClusterClickListener(new OnClusterClickListener<BuildingModel>() {

            @Override
            public boolean onClusterClick(Cluster<BuildingModel> cluster) {
                // Prevent Popup dialog
                return true;
            }
        });

        mClusterManager.setOnClusterItemClickListener(new OnClusterItemClickListener<BuildingModel>() {

            @Override
            public boolean onClusterItemClick(final BuildingModel b) {
                if (b != null) {

                    bypassSelectBuildingActivity(b, "0", false);
                }
                // Prevent Popup dialog
                return true;
            }
        });

    }

    // /> GOOGLE MAP FUNCTIONS


    // Select Building Activity based on gps location
    private void loadSelectBuildingActivity(GeoPoint loc, boolean invisibleSelection) {

        Intent placeIntent = new Intent(UnifiedNavigationActivity.this, SelectBuildingActivity.class);
        Bundle b = new Bundle();

        if (loc != null) {
            b.putString("coordinates_lat", String.valueOf(loc.dlat));
            b.putString("coordinates_lon", String.valueOf(loc.dlon));
        }
        b.putSerializable("mode", invisibleSelection ? SelectBuildingActivity.Mode.INVISIBLE : SelectBuildingActivity.Mode.NONE);
        placeIntent.putExtras(b);

        // start the activity where the user can select the building he is in
        startActivityForResult(placeIntent, SELECT_PLACE_ACTIVITY_RESULT);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case LOCATION_CONNECTION_FAILURE_RESOLUTION_REQUEST:
                // If the result code is Activity.RESULT_OK, try to connect again
                switch (resultCode) {
                    case Activity.RESULT_OK:
                        // Try the request again
                        // TODO - check google developers documentation again
                        // and implement it correctly
                }

                break;
            case SEARCH_POI_ACTIVITY_RESULT:
                if (resultCode == Activity.RESULT_OK) {
                    // search activity finished OK
                    if (data == null)
                        return;
                    // PoisModel poi_to = (PoisModel)
                    // data.getSerializableExtra("pmodel");
                    // startNavigationTask(poi_to);

                    IPoisClass place = (IPoisClass) data.getSerializableExtra("ianyplace");
                    handleSearchPlaceSelection(place);

                } else if (resultCode == Activity.RESULT_CANCELED) {
                    // CANCELLED
                    if (data == null)
                        return;
                    String msg = (String) data.getSerializableExtra("message");
                    if (msg != null)
                        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                }
                break;
            case SELECT_PLACE_ACTIVITY_RESULT:
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null)
                        return;

                    String fpf = data.getStringExtra("floor_plan_path");
                    if (fpf == null) {
                        Toast.makeText(getBaseContext(), "You haven't selected both building and floor...!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        BuildingModel b = mAnyplaceCache.getSpinnerBuildings().get(data.getIntExtra("bmodel", 0));
                        FloorModel f = b.getFloors().get(data.getIntExtra("fmodel", 0));
                        selectPlaceActivityResult(b, f);
                    } catch (Exception ex) {
                        Toast.makeText(getBaseContext(), "You haven't selected both building and floor...!", Toast.LENGTH_SHORT).show();
                    }

                } else if (resultCode == Activity.RESULT_CANCELED) {
                    // CANCELLED
                    if (data == null)
                        return;
                    String msg = (String) data.getSerializableExtra("message");
                    if (msg != null)
                        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
                }
                break;
            case PREFERENCES_ACTIVITY_RESULT:
                if (resultCode == RESULT_OK) {
                    AnyplacePrefs.Action result = (Action) data.getSerializableExtra("action");

                    switch (result) {
                        case REFRESH_BUILDING:

                            if (!userData.isFloorSelected()) {
                                Toast.makeText(getBaseContext(), "Load a map before performing this action!", Toast.LENGTH_SHORT).show();
                                break;
                            }

                            if (progressBar.getVisibility() == View.VISIBLE) {
                                Toast.makeText(getBaseContext(), "Building Loading in progress. Please Wait!", Toast.LENGTH_SHORT).show();
                                break;
                            }

                            try {

                                final BuildingModel b = userData.getSelectedBuilding();
                                // clear_floorplans
                                File floorsRoot = new File(AnyplaceUtils.getFloorPlansRootFolder(this), b.buid);
                                // clear radiomaps
                                File radiomapsRoot = AnyplaceUtils.getRadioMapsRootFolder(this);
                                final String[] radiomaps = radiomapsRoot.list(new FilenameFilter() {

                                    @Override
                                    public boolean accept(File dir, String filename) {
                                        if (filename.startsWith(b.buid))
                                            return true;
                                        else
                                            return false;
                                    }
                                });
                                for (int i = 0; i < radiomaps.length; i++) {
                                    radiomaps[i] = radiomapsRoot.getAbsolutePath() + File.separator + radiomaps[i];
                                }

                                floorSelector.Stop();
                                disableAnyplaceTracker();
                                DeleteFolderBackgroundTask task = new DeleteFolderBackgroundTask(new DeleteFolderBackgroundTask.DeleteFolderBackgroundTaskListener() {

                                    @Override
                                    public void onSuccess() {

                                        // clear any markers that might have already
                                        // been added to the map
                                        visiblePois.clearAll();
                                        // clear and resets the cached POIS inside
                                        // AnyplaceCache
                                        mAnyplaceCache.setPois(new HashMap<String, PoisModel>(), "");
                                        mAnyplaceCache.fetchAllFloorsRadiomapReset();

                                        bypassSelectBuildingActivity(b, b.getSelectedFloor());

                                    }
                                }, UnifiedNavigationActivity.this, true);
                                task.setFiles(floorsRoot);
                                task.setFiles(radiomaps);
                                task.execute();
                            } catch (Exception e) {
                                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                            break;
                        case REFRESH_MAP:
                            handleBuildingsOnMap(true);
                            break;
                    }
                }
                break;
        }
    }

    private void bypassSelectBuildingActivity(final BuildingModel b, final String floor_number, final Boolean force) {
        // Load Building
        b.loadFloors(new FetchFloorsByBuidTaskListener() {

            @Override
            public void onSuccess(String result, List<FloorModel> floors) {

                // Force loading of floor_number
                FloorModel floor;
                if ((floor = b.getFloorFromNumber(floor_number)) != null || !force) {
                    if (floor == null) {
                        floor = b.getSelectedFloor();
                    }

                    ArrayList<BuildingModel> list = new ArrayList<BuildingModel>(1);
                    list.add(b);
                    // Set building for Select Dialog
                    mAnyplaceCache.setSelectedBuildingIndex(0);
                    mAnyplaceCache.setSpinnerBuildings(list);

                    bypassSelectBuildingActivity(b, floor);

                } else {
                    Toast.makeText(getBaseContext(), "Building's Floor Not Found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onErrorOrCancel(String result) {
                Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();

            }
        }, UnifiedNavigationActivity.this, false, true);
    }

    private void bypassSelectBuildingActivity(final BuildingModel b, final String floor_number, final Boolean force, final PoisModel poi) {
        // Load Building
        b.loadFloors(new FetchFloorsByBuidTaskListener() {

            @Override
            public void onSuccess(String result, List<FloorModel> floors) {

                // Force loading of floor_number
                FloorModel floor;
                if ((floor = b.getFloorFromNumber(floor_number)) != null || !force) {
                    if (floor == null) {
                        floor = b.getSelectedFloor();
                    }

                    ArrayList<BuildingModel> list = new ArrayList<BuildingModel>(1);
                    list.add(b);
                    // Set building for Select Dialog
                    mAnyplaceCache.setSelectedBuildingIndex(0);
                    mAnyplaceCache.setSpinnerBuildings(list);

                    bypassSelectBuildingActivity(b, floor, poi);

                } else {
                    Toast.makeText(getBaseContext(), "Building's Floor Not Found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onErrorOrCancel(String result) {
                Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();

            }
        }, UnifiedNavigationActivity.this, false, true);
    }

    private void bypassSelectBuildingActivity(final BuildingModel b, final FloorModel f) {

        final FetchFloorPlanTask fetchFloorPlanTask = new FetchFloorPlanTask(UnifiedNavigationActivity.this, b.buid, f.floor_number);
        fetchFloorPlanTask.setCallbackInterface(new FetchFloorPlanTask.FetchFloorPlanTaskListener() {

            private ProgressDialog dialog;

            @Override
            public void onSuccess(String result, File floor_plan_file) {
                if (dialog != null)
                    dialog.dismiss();
                selectPlaceActivityResult(b, f);
            }

            @Override
            public void onErrorOrCancel(String result) {
                if (dialog != null)
                    dialog.dismiss();
                Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPrepareLongExecute() {
                dialog = new ProgressDialog(UnifiedNavigationActivity.this);
                dialog.setIndeterminate(true);
                dialog.setTitle("Downloading floor plan");
                dialog.setMessage("Please be patient...");
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(false);
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        fetchFloorPlanTask.cancel(true);
                    }
                });
                dialog.show();
            }
        });
        fetchFloorPlanTask.execute();
    }

    private void bypassSelectBuildingActivity(final BuildingModel b, final FloorModel f, final PoisModel pm) {

        final FetchFloorPlanTask fetchFloorPlanTask = new FetchFloorPlanTask(UnifiedNavigationActivity.this, b.buid, f.floor_number);
        fetchFloorPlanTask.setCallbackInterface(new FetchFloorPlanTask.FetchFloorPlanTaskListener() {

            private ProgressDialog dialog;

            @Override
            public void onSuccess(String result, File floor_plan_file) {
                if (dialog != null)
                    dialog.dismiss();
                selectPlaceActivityResult(b, f, pm);
            }

            @Override
            public void onErrorOrCancel(String result) {
                if (dialog != null)
                    dialog.dismiss();
                Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPrepareLongExecute() {
                dialog = new ProgressDialog(UnifiedNavigationActivity.this);
                dialog.setIndeterminate(true);
                dialog.setTitle("Downloading floor plan");
                dialog.setMessage("Please be patient...");
                dialog.setCancelable(true);
                dialog.setCanceledOnTouchOutside(false);
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        fetchFloorPlanTask.cancel(true);
                    }
                });
                dialog.show();
            }
        });
        fetchFloorPlanTask.execute();
    }

    private void selectPlaceActivityResult(final BuildingModel b, final FloorModel f, final PoisModel pm) {

        selectPlaceActivityResult_HELP(b, f);

        fetchPoisByBuidToCache(b.buid, new FetchPoisListener() {

            @Override
            public void onSuccess(String result, Map<String, PoisModel> poisMap) {

                // This should never return null
                if (poisMap.get(pm.puid) == null) {
                    poisMap.put(pm.puid, pm);
                }

                handlePoisOnMap(poisMap.values());
                startNavigationTask(pm.puid);
                selectPlaceActivityResult_HELP2(b, f);
            }

            @Override
            public void onErrorOrCancel(String result) {

                Collection<PoisModel> l = mAnyplaceCache.getPois();
                l.add(pm);
                handlePoisOnMap(l);
                startNavigationTask(pm.puid);

                selectPlaceActivityResult_HELP2(b, f);
            }
        });
    }

    private void selectPlaceActivityResult(final BuildingModel b, final FloorModel f) {

        selectPlaceActivityResult_HELP(b, f);

        fetchPoisByBuidToCache(b.buid, new FetchPoisListener() {

            @Override
            public void onSuccess(String result, Map<String, PoisModel> poisMap) {
                handlePoisOnMap(poisMap.values());
                loadIndoorOutdoorPath();
                selectPlaceActivityResult_HELP2(b, f);
            }

            @Override
            public void onErrorOrCancel(String result) {
                loadIndoorOutdoorPath();
                selectPlaceActivityResult_HELP2(b, f);
            }
        });

    }

    // Help tasks
    private void selectPlaceActivityResult_HELP(final BuildingModel b, final FloorModel f) {
        mAutomaticGPSBuildingSelection = false;
        floorSelector.Stop();
        disableAnyplaceTracker();

        // set the newly selected floor
        b.setSelectedFloor(f.floor_number);
        userData.setSelectedBuilding(b);
        userData.setSelectedFloor(f);
        textFloor.setText(f.floor_name);

        // clean the map in case there are overlays
        mMap.clear();

        // add the Tile Provider that uses our Building tiles over Google Maps
        TileOverlay mTileOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(new AnyPlaceMapTileProvider(getBaseContext(), b.buid, f.floor_number)));
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(b.getPosition(), 19.0f), new CancelableCallback() {

            @Override
            public void onFinish() {
                cameraUpdate = false;
                handleBuildingsOnMap(false);
                updateLocation();
            }

            @Override
            public void onCancel() {
                cameraUpdate = false;
            }
        });

        // we must now change the radio map file since we changed floor RADIO MAP initialization
        try {
            File root = AnyplaceUtils.getRadioMapFoler(this, b.buid, userData.getSelectedFloorNumber());
            lpTracker.setRadiomapFile(new File(root, AnyplaceUtils.getRadioMapFileName(userData.getSelectedFloorNumber())).getAbsolutePath());
        } catch (Exception e) {
            // exception thrown by GetRootFolder when sdcard is not writable
            Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // Download RADIOMAP
    private void selectPlaceActivityResult_HELP2(final BuildingModel b, final FloorModel f) {

        String trackedPositionLat = userData.getSelectedBuilding().getLatitudeString();
        String trackedPositionLon = userData.getSelectedBuilding().getLongitudeString();

        // first we should disable the tracker if it's working
        disableAnyplaceTracker();

        class Callback implements DownloadRadioMapTaskBuid.DownloadRadioMapListener, PreviousRunningTask {

            boolean progressBarEnabled = false;
            boolean disableSuccess = false;

            @Override
            public void onSuccess(String result) {
                if (disableSuccess) {
                    onErrorOrCancel("");
                    return;
                }
                // start the tracker
                enableAnyplaceTracker();

                // Download All Building Floors and Radiomaps
                if (AnyplaceAPI.PLAY_STORE) {

                    mAnyplaceCache.fetchAllFloorsRadiomapsRun(new BackgroundFetchListener() {

                        @Override
                        public void onSuccess(String result) {
                            hideProgressBar();
                            if (AnyplaceAPI.DEBUG_MESSAGES) {
                                btnTrackme.setBackgroundColor(Color.YELLOW);
                            }
                            floorSelector.updateFiles(b.buid);
                            floorSelector.Start(b.getLatitudeString(), b.getLongitudeString());
                        }

                        @Override
                        public void onProgressUpdate(int progress_current, int progress_total) {
                            progressBar.setProgress((int) ((float) progress_current / progress_total * progressBar.getMax()));
                        }

                        @Override
                        public void onErrorOrCancel(String result, ErrorType error) {
                            // Do not hide progress bar if previous task is running
                            // ErrorType.SINGLE_INSTANCE
                            // Do not hide progress bar because a new task will be created
                            // ErrorType.CANCELLED
                            if (error == ErrorType.EXCEPTION)
                                hideProgressBar();
                        }

                        @Override
                        public void onPrepareLongExecute() {
                            showProgressBar();
                        }

                    }, b);
                }
            }

            @Override
            public void onErrorOrCancel(String result) {
                if (progressBarEnabled) {
                    hideProgressBar();
                }
            }

            @Override
            public void onPrepareLongExecute() {
                progressBarEnabled = true;
                showProgressBar();
                // Set a smaller percentage than fetchAllFloorsRadiomapsOfBUID
                progressBar.setProgress((int) (1.0f / (userData.getSelectedBuilding().getFloors().size() * 2) * progressBar.getMax()));
            }

            @Override
            public void disableSuccess() {
                disableSuccess = true;
            }

        }

        if (downloadRadioMapTaskBuid != null) {
            ((PreviousRunningTask) downloadRadioMapTaskBuid.getCallbackInterface()).disableSuccess();
        }

        downloadRadioMapTaskBuid = new DownloadRadioMapTaskBuid(new Callback(), this, trackedPositionLat, trackedPositionLon, userData.getSelectedBuildingId(), userData.getSelectedFloorNumber(), false);

        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            // Execute task parallel with others and multiple instances of
            // itself
            downloadRadioMapTaskBuid.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            downloadRadioMapTaskBuid.execute();
        }
    }


    // </ Play Services Functions
    private boolean checkPlayServices() {
        // Check that Google Play services is available
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d("Location Updates", "Google Play services is available.");
            // Continue
            return true;
        } else {
            // Google Play services was not available for some reason

            // GooglePlayServicesUtil.getErrorDialog(resultCode, this,
            // 0).show();
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this, PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i("AnyplaceNavigator", "This device is not supported.");
                finish();
            }
            return false;
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // TODO - CHECK HOW THIS WORKS
        Log.d("Google Play Services", "Connection failed");
        // Google Play services can resolve some errors it detects.
        // If the error has a resolution, try sending an Intent to
        // start a Google Play services activity that can resolve
        // error.
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(this, LOCATION_CONNECTION_FAILURE_RESOLUTION_REQUEST);
                // Thrown if Google Play services canceled the original
                // PendingIntent
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            // If no resolution is available, display a dialog to the
            // user with the error.
            // showErrorDialog(connectionResult.getErrorCode());
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
        }
    }

    @Override
    public void onConnected(Bundle dataBundle) {
        // Called after onResume by system
        // Log.d("Google Play services", "Connected");
        if (checkPlayServices()) {
            initCamera();
            // Get Wifi + GPS Fused Location
            Location currentLocation = mLocationClient.getLastLocation();
            // we must set listener to the get the first location from the API
            // it will trigger the onLocationChanged below when a new location
            // is found or notify the user
            mLocationClient.requestLocationUpdates(mLocationRequest, this);
            if (currentLocation != null) {
                onLocationChanged(currentLocation);
            } else {
                if (mAutomaticGPSBuildingSelection)
                    Toast.makeText(getBaseContext(), "No location available at the moment.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onDisconnected() {
        Log.d("Google Play services", "Disconnected. Please re-connect!");
    }
    // /> Play Services Functions


    // </ NAVIGATION FUNCTIONS
    private void startNavigationTask(String id) {

        if (!NetworkUtils.isOnline(this)) {
            Toast.makeText(this, "No connection available!", Toast.LENGTH_SHORT).show();
            return;
        }

        // show the info window for the destination marker
        Marker marker = visiblePois.getMarkerFromPoisModel(id);
        if (marker != null) {
            marker.showInfoWindow();
        }

        final BuildingModel b = userData.getSelectedBuilding();
        final String currentFloor = userData.getSelectedFloorNumber();

        class Status {
            Boolean task1 = false;
            Boolean task2 = false;
        }
        final Status status = new Status();

        final ProgressDialog dialog;
        dialog = new ProgressDialog(this);
        dialog.setIndeterminate(true);
        dialog.setTitle("Plotting navigation");
        dialog.setMessage("Please be patient...");
        dialog.setCancelable(true);
        dialog.setCanceledOnTouchOutside(false);

        PoisModel _entrance = null;
        GeoPoint pos = userData.getPositionWifi();
        if (pos == null) {
            // Find The nearest building entrance from the destination poi
            PoisModel _entranceGlobal = null;
            PoisModel _entrance0 = null;
            PoisModel _entranceCurrentFloor = null;
            double min = Double.MAX_VALUE;

            PoisModel dest = mAnyplaceCache.getPoisMap().get(id);
            for (PoisModel pm : mAnyplaceCache.getPoisMap().values()) {
                if (pm.is_building_entrance) {

                    if (pm.floor_number.equalsIgnoreCase(currentFloor)) {
                        double distance = Math.abs(pm.lat() - dest.lat()) + Math.abs(pm.lng() - dest.lng());
                        if (min > distance) {
                            _entranceCurrentFloor = pm;
                            min = distance;
                        }
                    } else if (pm.floor_number.equalsIgnoreCase("0")) {
                        _entrance0 = pm;
                    } else {
                        _entranceGlobal = pm;
                    }
                }
            }

            if (_entranceCurrentFloor != null) {
                _entrance = _entranceCurrentFloor;
            } else if (_entrance0 != null) {
                _entrance = _entrance0;
            } else if (_entranceGlobal != null) {
                _entrance = _entranceGlobal;
            } else {
                Toast.makeText(this, "No entrance found!", Toast.LENGTH_SHORT).show();
                return;
            }
        }


        // Does not run if entrance==null or is near the building
        final AsyncTask<Void, Void, String> async1f = new NavOutdoorTask(new NavOutdoorTask.NavDirectionsListener() {

            @Override
            public void onNavDirectionsSuccess(String result, List<LatLng> points) {
                onNavDirectionsFinished();

                if (!points.isEmpty()) {
                    // points.add(new LatLng(entrancef.dlat, entrancef.dlon));
                    pathLineOutdoorOptions = new PolylineOptions().addAll(points).width(10).color(Color.RED).zIndex(100.0f);
                    pathLineOutdoor = mMap.addPolyline(pathLineOutdoorOptions);
                }
            }

            @Override
            public void onNavDirectionsErrorOrCancel(String result) {
                onNavDirectionsFinished();
                // display the error cause
                Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onNavDirectionsAbort() {
                onNavDirectionsFinished();
            }

            public void onNavDirectionsFinished() {
                status.task1 = true;
                if (status.task1 && status.task2)
                    dialog.dismiss();
                else {
                    // First task executed calls this
                    clearNavigationData();
                }
            }

        }, userData.getLocationGPSorIP(), (_entrance != null) ? new GeoPoint(_entrance.lat(), _entrance.lng()) : null);

        // start the navigation task
        final AsyncTask<Void, Void, String> async2f = new NavIndoorTask(new NavIndoorTask.NavRouteListener() {
            @Override
            public void onNavRouteSuccess(String result, List<PoisNav> points) {
                onNavDirectiosFinished();

                // set the navigation building and new points
                userData.setNavBuilding(b);
                userData.setNavPois(points);

                // handle drawing of the points
                handleIndoorPath(points);
            }

            @Override
            public void onNavRouteErrorOrCancel(String result) {
                onNavDirectiosFinished();
                // display the error cause
                Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
            }

            public void onNavDirectiosFinished() {
                status.task2 = true;
                if (status.task1 && status.task2)
                    dialog.dismiss();
                else {
                    // First task executed calls this
                    clearNavigationData();
                }
            }

        }, this, id, (pos == null) ? new GeoPoint(_entrance.lat(), _entrance.lng()) : pos, (pos == null) ? _entrance.floor_number : currentFloor);

        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                async1f.cancel(true);
                async2f.cancel(true);
            }
        });
        dialog.show();
        async1f.execute();
        async2f.execute();
    }

    private void removeNavOverlays() {
        if (pathLineInside != null) {
            pathLineInside.remove();
        }
        if (pathLineOutdoor != null) {
            pathLineOutdoor.remove();
        }
        visiblePois.clearFromMarker();
        visiblePois.clearToMarker();
    }

    private void clearNavigationData() {
        if (userData != null) {
            userData.clearNav();
        }
        removeNavOverlays();
        btnFloorUp.setVisibility(View.VISIBLE);
        btnFloorDown.setVisibility(View.VISIBLE);
    }

    // Loads the navigation route if any exists for the current floor selected
    // Multi Floor Route ex. DMSL --> Zeina
    private void loadIndoorOutdoorPath() {
        if (userData.isNavBuildingSelected()) {
            removeNavOverlays();
            handleIndoorPath(userData.getNavPois());

            if (pathLineOutdoorOptions != null) {
                pathLineOutdoor = mMap.addPolyline(pathLineOutdoorOptions);
            }
        } else {
            btnFloorUp.setVisibility(View.VISIBLE);
            btnFloorDown.setVisibility(View.VISIBLE);
        }
    }

    // draws the navigation route for the loaded floor
    private void handleIndoorPath(List<PoisNav> puids) {
        List<LatLng> p = new ArrayList<LatLng>();
        String selectedFloor = userData.getSelectedFloorNumber();
        for (PoisNav pt : puids) {
            // draw only the route for this floor
            if (pt.floor_number.equalsIgnoreCase(selectedFloor)) {
                p.add(new LatLng(Double.parseDouble(pt.lat), Double.parseDouble(pt.lon)));
            }
        }
        pathLineInside = mMap.addPolyline(new PolylineOptions().addAll(p).width(10).color(Color.RED).zIndex(100.0f));

        if (!puids.isEmpty()) {
            // add markers for starting and ending position
            // starting point
            PoisNav nrpFrom = puids.get(0);
            if (nrpFrom.floor_number.equalsIgnoreCase(selectedFloor))
                visiblePois.setFromMarker(mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(nrpFrom.lat), Double.parseDouble(nrpFrom.lon))).title("Starting Position").icon(BitmapDescriptorFactory.fromResource(R.drawable.map_flag_green2_48))));
            // destination point
            PoisNav nrpTo = puids.get(puids.size() - 1);
            if (nrpTo.floor_number.equalsIgnoreCase(selectedFloor))
                visiblePois.setToMarker(mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(nrpTo.lat), Double.parseDouble(nrpTo.lon))).title("Final Destination").icon(BitmapDescriptorFactory.fromResource(R.drawable.map_flag_pink2_48))));

            // adjust floor buttons
            if (nrpTo.floor_number.equals(nrpFrom.floor_number)) {
                btnFloorUp.setVisibility(View.VISIBLE);
                btnFloorDown.setVisibility(View.VISIBLE);
            } else {
                // Go to Navigation Destination
                if (Integer.parseInt(nrpTo.floor_number) > Integer.parseInt(selectedFloor)) {
                    btnFloorDown.setVisibility(View.INVISIBLE);
                    btnFloorUp.setVisibility(View.VISIBLE);
                } else if (Integer.parseInt(nrpTo.floor_number) < Integer.parseInt(selectedFloor)) {
                    btnFloorUp.setVisibility(View.INVISIBLE);
                    btnFloorDown.setVisibility(View.VISIBLE);
                } else { // if Navigation Destination Floor Go to Navigation
                    // Start
                    if (Integer.parseInt(nrpFrom.floor_number) > Integer.parseInt(selectedFloor)) {
                        btnFloorDown.setVisibility(View.INVISIBLE);
                        btnFloorUp.setVisibility(View.VISIBLE);
                    } else {
                        btnFloorUp.setVisibility(View.INVISIBLE);
                        btnFloorDown.setVisibility(View.VISIBLE);
                    }
                }
            }

        }
    }

    private void handleBuildingsOnMap(boolean forceReload) {
        AnyplaceCache mAnyplaceCache = AnyplaceCache.getInstance(UnifiedNavigationActivity.this);
        mAnyplaceCache.loadWorldBuildings(new FetchBuildingsTaskListener() {

            @Override
            public void onSuccess(String result, List<BuildingModel> buildings) {
                List<BuildingModel> collection = new ArrayList<BuildingModel>(buildings);
                mClusterManager.clearItems();
                BuildingModel buid = userData.getSelectedBuilding();
                if (buid != null)
                    collection.remove(buid);
                mClusterManager.addItems(collection);
                mClusterManager.cluster();
                // HACK. This dumps all the cached icons & recreates everything.
                mClusterManager.setRenderer(new MyBuildingsRenderer(UnifiedNavigationActivity.this, mMap, mClusterManager));
            }

            @Override
            public void onErrorOrCancel(String result) {

            }

        }, this, forceReload);
    }
    // /> NAVIGATION FUNCTIONS


    // </POIS
    private void handlePoisOnMap(Collection<PoisModel> collection) {

        visiblePois.clearAll();
        String currentFloor = userData.getSelectedFloorNumber();

        // Display part of Description Text Only
        // Make an approximation of available space based on map size
        final int fragmentWidth = (int) (findViewById(R.id.map).getWidth() * 2);
        ViewGroup infoWindow = (ViewGroup) getLayoutInflater().inflate(R.layout.info_window, null);
        TextView infoSnippet = (TextView) infoWindow.findViewById(R.id.snippet);
        TextPaint paint = infoSnippet.getPaint();

        for (PoisModel pm : collection) {
            if (pm.floor_number.equalsIgnoreCase(currentFloor)) {
                String snippet = AndroidUtils.fillTextBox(paint, fragmentWidth, pm.description);
                Marker m = mMap.addMarker(new MarkerOptions().position(new LatLng(Double.parseDouble(pm.lat), Double.parseDouble(pm.lng))).title(pm.name).snippet(snippet).icon(BitmapDescriptorFactory.fromResource(R.drawable.pin8)));
                visiblePois.addMarkerAndPoi(m, pm);
            }
        }
    }

    private void fetchPoisByBuidToCache(final String buid, final FetchPoisByBuidTask.FetchPoisListener l) {
        // Check for cahced pois
        if (mAnyplaceCache.checkPoisBUID(buid)) {
            l.onSuccess("Pois read from cache", mAnyplaceCache.getPoisMap());
        } else {
            FetchPoisByBuidTask fetchPoisByBuidFloorTask = new FetchPoisByBuidTask(new FetchPoisByBuidTask.FetchPoisListener() {
                @Override
                public void onSuccess(String result, Map<String, PoisModel> poisMap) {
                    mAnyplaceCache.setPois(poisMap, buid);
                    l.onSuccess(result, poisMap);
                }

                @Override
                public void onErrorOrCancel(String result) {
                    // clear any markers that might have already been added to
                    // the map
                    visiblePois.clearAll();
                    // clear and resets the cached POIS inside AnyplaceCache
                    mAnyplaceCache.setPois(new HashMap<String, PoisModel>(), "");
                    l.onErrorOrCancel(result);
                }
            }, this, buid);

            fetchPoisByBuidFloorTask.execute();
        }
    }
    // />POIS


    // </ Activity Listeners
    @Override
    public void onNewWifiResults(int aps) {
        // Log.d( "Anyplace Tracker", "wifi res: " + list.size() );
        detectedAPs.setText("AP: " + aps);
    }

    //Play Services location listener
    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            userData.setLocationGPS(location);
            updateLocation();

            if (mAutomaticGPSBuildingSelection) {
                mAutomaticGPSBuildingSelection = false;
                loadSelectBuildingActivity(userData.getLatestUserPosition(), true);
            }

        }
    }

    @Override
    public void onNewLocation(final LatLng pos) {
        userData.setPositionWifi(pos.latitude, pos.longitude);
        this.runOnUiThread(new Runnable() {

            @Override
            public void run() {
                if (isTrackingErrorBackground) {
                    isTrackingErrorBackground = false;
                    btnTrackme.setImageResource(R.drawable.dark_device_access_location_searching);
                }

                // update the wifi location of the user
                updateLocation();
            }
        });

    }

    @Override
    public void onTrackerError(final String msg) {
        if (!isTrackingErrorBackground)
            this.runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    if (!isTrackingErrorBackground) {
                        btnTrackme.setImageResource(R.drawable.dark_device_access_location_off);
                        isTrackingErrorBackground = true;
                    }
                }
            });

    }

    @Override
    public void onFloorError(final Exception ex) {
        if (ex instanceof NonCriticalError)
            return;

        floorSelector.Stop();
        // TODO Auto-generated method stub
        Log.e("Floor Selector", ex.toString());
        Toast.makeText(getBaseContext(), "Floor Selector ecountered an error", Toast.LENGTH_SHORT).show();

    }

    // Change Floor Request on current Building
    @Override
    public void onNewFloor(final String floorNumber) {

        if (floorChangeRequestDialog)
            return;

        final BuildingModel b = userData.getSelectedBuilding();

        if (b == null) {
            Log.e("Unified Activity", "onNewFloor b=null");
            return;
        }

        // Check if the floor is the loaded floor
        if (b.getSelectedFloor().floor_number.equals(floorNumber)) {
            lastFloor = null;
            return;
        }

        // User clicked Cancel
        if (lastFloor != null && lastFloor.equals(floorNumber)) {
            return;
        }

        lastFloor = floorNumber;

        final FloorModel f = b.getFloorFromNumber(floorNumber);
        if (f != null) {

            AlertDialog.Builder alertDialog = new AlertDialog.Builder(UnifiedNavigationActivity.this);
            alertDialog.setTitle("Floor Change Detected");
            alertDialog.setMessage("Floor Number: " + floorNumber + ". Do you want to proceed?");
            alertDialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    floorChangeRequestDialog = false;
                    bypassSelectBuildingActivity(b, f);
                }
            });
            alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
                    floorChangeRequestDialog = false;
                }
            });
            alertDialog.show();
            floorChangeRequestDialog = true;
        }
    }

    private void updateLocation() {

        GeoPoint location = userData.getLatestUserPosition();
        if (location != null) {
            // draw the location of the new position
            if (userMarker != null) {
                userMarker.remove();
            }
            MarkerOptions marker = new MarkerOptions();
            marker.position(new LatLng(location.dlat, location.dlon));
            marker.title("User").snippet("Estimated Position");
            marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon21));
            marker.rotation(sensorsMain.getRAWHeading() - bearing);
            userMarker = this.mMap.addMarker(marker);
        }
    }

    // /> Activity Listeners


    // </ HELPER FUNCTIONS
    private void enableAnyplaceTracker() {
        // Do not change file wile enabling tracker
        if (lpTracker.trackOn()) {
            btnTrackme.setImageResource(R.drawable.dark_device_access_location_searching);
            isTrackingErrorBackground = false;
        }

    }

    private void disableAnyplaceTracker() {
        lpTracker.trackOff();
        btnTrackme.setImageResource(R.drawable.dark_device_access_location_off);
        isTrackingErrorBackground = true;
    }

    private void addTrackerListeners() {

        // sensorsMain.addListener((SensorsMain.IOrientationListener) this);
        lpTracker.addListener((AnyplaceTracker.WifiResultsAnyplaceTrackerListener) this);
        lpTracker.addListener((AnyplaceTracker.TrackedLocAnyplaceTrackerListener) this);
        lpTracker.addListener((AnyplaceTracker.ErrorAnyplaceTrackerListener) this);
        floorSelector.addListener((FloorSelector.FloorAnyplaceFloorListener) this);
        floorSelector.addListener((FloorSelector.ErrorAnyplaceFloorListener) this);
    }

    private void removeTrackerListeners() {

        // sensorsMain.removeListener((SensorsMain.IOrientationListener) this);
        lpTracker.removeListener((AnyplaceTracker.WifiResultsAnyplaceTrackerListener) this);
        lpTracker.removeListener((AnyplaceTracker.TrackedLocAnyplaceTrackerListener) this);
        lpTracker.removeListener((AnyplaceTracker.ErrorAnyplaceTrackerListener) this);
        floorSelector.removeListener((FloorSelector.FloorAnyplaceFloorListener) this);
        floorSelector.removeListener((FloorSelector.ErrorAnyplaceFloorListener) this);
    }

    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }
    // /> HELPER FUNCTIONS


    // </ SEARCHING FUNCTIONS
    @Override
    protected void onNewIntent(Intent intent) {
        handleIntent(intent);
    }

    // Search Button or URL handle
    private void handleIntent(Intent intent) {

        String action = intent.getAction();
        if (Intent.ACTION_SEARCH.equals(action)) {

            // check what type of search we need
            SearchTypes searchType = AnyPlaceSeachingHelper.getSearchType(mMap.getCameraPosition().zoom);
            String query = intent.getStringExtra(SearchManager.QUERY);
            GeoPoint gp = userData.getLatestUserPosition();

            // manually launch the real search activity
            Intent searchIntent = new Intent(UnifiedNavigationActivity.this, SearchPOIActivity.class);
            // add query to the Intent Extras
            searchIntent.setAction(action);
            searchIntent.putExtra("searchType", searchType);
            searchIntent.putExtra("query", query);
            searchIntent.putExtra("lat", (gp == null) ? csLat : gp.dlat);
            searchIntent.putExtra("lng", (gp == null) ? csLon : gp.dlon);
            startActivityForResult(searchIntent, SEARCH_POI_ACTIVITY_RESULT);

        } else if (Intent.ACTION_VIEW.equals(action)) {
            String data = intent.getDataString();

            if (data != null && data.startsWith("http")) {
                final Uri uri = intent.getData();
                if (uri != null) {
                    String path = uri.getPath();

                    if (path != null && path.equals("/getnavigation")) {
                        String poid = uri.getQueryParameter("poid");
                        if (poid == null || poid.equals("")) {
                            // Share building
                            // http://anyplace.rayzit.com/getnavigation?buid=username_1373876832005&floor=0
                            String buid = uri.getQueryParameter("buid");
                            if (buid == null || buid.equals("")) {
                                Toast.makeText(getBaseContext(), "Buid parameter expected", Toast.LENGTH_SHORT).show();
                            } else {
                                mAutomaticGPSBuildingSelection = false;
                                mAnyplaceCache.loadBuilding(buid, new FetchBuildingTaskListener() {

                                    @Override
                                    public void onSuccess(String result, final BuildingModel b) {

                                        bypassSelectBuildingActivity(b, uri.getQueryParameter("floor"), true);

                                    }

                                    @Override
                                    public void onErrorOrCancel(String result) {
                                        Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();

                                    }
                                }, UnifiedNavigationActivity.this);
                            }
                        } else {
                            // Share POI
                            // http://anyplace.rayzit.com/getnavigation?poid=username_username_1373876832005_0_35.14424091022549_33.41139659285545_1382635428093
                            mAutomaticGPSBuildingSelection = false;
                            new FetchPoiByPuidTask(new FetchPoiByPuidTask.FetchPoiListener() {

                                @Override
                                public void onSuccess(String result, final PoisModel poi) {

                                    if (userData.getSelectedBuildingId() != null && userData.getSelectedBuildingId().equals(poi.buid)) {
                                        // Building is Loaded
                                        startNavigationTask(poi.puid);
                                    } else {
                                        // Load Building
                                        mAnyplaceCache.loadBuilding(poi.buid, new FetchBuildingTaskListener() {

                                            @Override
                                            public void onSuccess(String result, final BuildingModel b) {

                                                bypassSelectBuildingActivity(b, poi.floor_number, true, poi);

                                            }

                                            @Override
                                            public void onErrorOrCancel(String result) {
                                                Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();

                                            }
                                        }, UnifiedNavigationActivity.this);
                                    }
                                }

                                @Override
                                public void onErrorOrCancel(String result) {
                                    Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
                                }
                            }, this, poid).execute();
                        }
                    }
                }
            } else {

                // Search TextBox results only

                // PoisModel or Place Class
                IPoisClass place_selected = AnyPlaceSeachingHelper.getClassfromJson(data);

                if (place_selected.id() != null) {
                    // hide the search view when a navigation route is drawn
                    if (searchView != null) {
                        searchView.setIconified(true);
                        searchView.clearFocus();
                    }
                    handleSearchPlaceSelection(place_selected);
                }

            }

        }
    } // end of handle intent

    // handle the selected place from the TextBox or search activity
    // either Anyplace POI or a Google Place
    private void handleSearchPlaceSelection(final IPoisClass place) {
        if (place == null)
            return;
        switch (place.type()) {
            case AnyPlacePOI:
                startNavigationTask(place.id());
                break;
            case GooglePlace:

                mAnyplaceCache.loadWorldBuildings(new FetchBuildingsTaskListener() {

                    @Override
                    public void onSuccess(String result, List<BuildingModel> allBuildings) {
                        FetchNearBuildingsTask nearBuildings = new FetchNearBuildingsTask();
                        nearBuildings.run(allBuildings.iterator(), place.lat(), place.lng(), 200);

                        if (nearBuildings.buildings.size() > 0) {
                            final BuildingModel b = nearBuildings.buildings.get(0);

                            bypassSelectBuildingActivity(b, "0", false);
                        } else {
                            showGooglePoi(place);
                        }
                    }

                    @Override
                    public void onErrorOrCancel(String result) {
                        showGooglePoi(place);
                    }
                }, UnifiedNavigationActivity.this, false);

                break;
        }
    }

    private void showGooglePoi(IPoisClass place) {
        cameraUpdate = true;
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(place.lat(), place.lng()), mInitialZoomLevel), new CancelableCallback() {

            @Override
            public void onFinish() {
                cameraUpdate = false;
            }

            @Override
            public void onCancel() {
                cameraUpdate = false;
            }
        });
        // add the marker for this Google Place
        Marker mGooglePlaceMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(place.lat(), place.lng())).icon(BitmapDescriptorFactory.fromResource(R.drawable.pin2)));
        visiblePois.setGooglePlaceMarker(mGooglePlaceMarker, place);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

        if (key.equals("TrackingAlgorithm")) {
            lpTracker.setAlgorithm(sharedPreferences.getString("TrackingAlgorithm", "WKNN"));
        }
    }

    private void popup_msg(String msg, String title) {

        AlertDialog.Builder alert_box = new AlertDialog.Builder(this);
        alert_box.setTitle(title);
        alert_box.setMessage(msg);

        alert_box.setNeutralButton("Hide", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog alert = alert_box.create();
        alert.show();
    }

    // Define a DialogFragment that displays the error dialog
    public static class ErrorDialogFragment extends DialogFragment {
        // Global field to contain the error dialog
        private Dialog mDialog;

        // Default constructor. Sets the dialog field to null
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        // Set the dialog to display
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        // Return a Dialog to the DialogFragment.
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_BACK:
                finish();
                return true;
            case KeyEvent.KEYCODE_FOCUS:
            case KeyEvent.KEYCODE_CAMERA:
            case KeyEvent.KEYCODE_SEARCH:
                // Handle these events so they don't launch the Camera app
                return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    interface PreviousRunningTask {
        void disableSuccess();
    }
}
