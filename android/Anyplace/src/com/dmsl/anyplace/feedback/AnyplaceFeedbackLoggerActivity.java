package com.dmsl.anyplace.feedback;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.dmsl.anyplace.floor.Algo1Radiomap;

import com.dmsl.anyplace.sensors.SensorsStepCounter;
import com.dmsl.anyplace.tracker.AnyplaceTracker;
import com.dmsl.anyplace.tracker.TrackerLogicPlusIMU;
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
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.ClusterManager.OnClusterItemClickListener;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;
import com.dmsl.airplace.algorithms.RadioMap;
import com.dmsl.anyplace.AnyplaceAPI;
import com.dmsl.anyplace.R;
import com.dmsl.anyplace.AnyplaceAboutActivity;
import com.dmsl.anyplace.SelectBuildingActivity;
import com.dmsl.anyplace.cache.AnyplaceCache;
import com.dmsl.anyplace.cache.BackgroundFetchListener;
import com.dmsl.anyplace.googlemap.AnyPlaceMapTileProvider;
import com.dmsl.anyplace.googlemap.MyBuildingsRenderer;
import com.dmsl.anyplace.feedback.FeedbackPrefs.Action;
import com.dmsl.anyplace.nav.AnyPlaceSeachingHelper;
import com.dmsl.anyplace.nav.AnyUserData;
import com.dmsl.anyplace.nav.BuildingModel;
import com.dmsl.anyplace.nav.FloorModel;
import com.dmsl.anyplace.nav.AnyPlaceSeachingHelper.SearchTypes;
import com.dmsl.anyplace.sensors.MovementDetector;
import com.dmsl.anyplace.sensors.SensorsMain;
import com.dmsl.anyplace.tasks.DeleteFolderBackgroundTask;
import com.dmsl.anyplace.tasks.DownloadRadioMapTaskBuid;
import com.dmsl.anyplace.tasks.DownloadRadioMapTaskBuid.DownloadRadioMapListener;
import com.dmsl.anyplace.tasks.FetchFloorPlanTask;
import com.dmsl.anyplace.tasks.FetchNearBuildingsTask;
import com.dmsl.anyplace.tasks.FetchBuildingsTask.FetchBuildingsTaskListener;
import com.dmsl.anyplace.tasks.FetchFloorsByBuidTask.FetchFloorsByBuidTaskListener;
import com.dmsl.anyplace.utils.AndroidUtils;
import com.dmsl.anyplace.utils.AnyplaceUtils;
import com.dmsl.anyplace.utils.GeoPoint;
import com.dmsl.anyplace.utils.NetworkUtils;
import com.dmsl.anyplace.wifi.SimpleWifiManager;
import com.dmsl.anyplace.wifi.WifiReceiver;


import android.Manifest;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.ProgressDialog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.json.JSONException;
import org.json.JSONObject;


public class AnyplaceFeedbackLoggerActivity extends SherlockFragmentActivity implements OnSharedPreferenceChangeListener, GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener, LocationListener, OnMapClickListener, AnyplaceTracker.TrackedLocAnyplaceTrackerListener,
        AnyplaceTracker.ErrorAnyplaceTrackerListener, AnyplaceTracker.WifiResultsAnyplaceTrackerListener {
    private static final String TAG = "AnyplaceFeedbackLogger";

    private final String url = AnyplaceAPI.getFeedbackEndpoint();

    public static final String SHARED_PREFS_LOGGER = "feedback_preferences";
    private static final int PERMISSION_STORAGE_WRITE = 100;
    private final static int LOCATION_CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9001;
    private final static int PREFERENCES_ACTIVITY_RESULT = 1114;
    private static final int SELECT_PLACE_ACTIVITY_RESULT = 1112;
    private static final float mInitialZoomLevel = 18.0f;
    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;
    private GoogleMap mMap;
    private Marker mMarker = null;
    private LatLng currLocation = null;
    private String unknownSSID = "UnknownSSID";
    // <Load Building and Marker>
    private ClusterManager<BuildingModel> mClusterManager;
    private DownloadRadioMapTaskBuid downloadRadioMapTaskBuid;
    private SearchTypes searchType = null;
    private Marker gpsMarker = null;
    private Marker wifiMarker = null;
    private Location gpsLocation = null;
    private float bearing;
    //Button That fixes the GPS Co-ordinates if not fixed
    private ImageButton btnTrackme;
    // Button that records access points
    private Button btnRecord;

    ProgressBar progressBar;

    // WiFi manager
    private SimpleWifiManager wifi;

    // WiFi Receiver
    private WifiReceiver receiverWifi;

    // TextView showing the current floor
    private TextView textFloor;

    // TextView showing the current scan results
    private TextView scanResults;


    //UserData
    private AnyUserData userData = null;

    private boolean isTrackingErrorBackground;


    // the textview that displays the current position and heading
    private TextView mTrackingInfoView = null;

    private SharedPreferences preferences;

    // Positioning
    private SensorsMain positioning;
    private MovementDetector movementDetector;
    private float raw_heading = 0.0f;
    private boolean walking = false;

    private boolean upInProgress = false;
    private Object upInProgressLock = new Object();

    private boolean userIsNearby = false;
    private BuildingModel mCurrentBuilding = null;
    private FloorModel mCurrentFloor = null;
    private HeatmapTileProvider mProvider;

    // AnyplaceTracker
    private SensorsMain sensorsMain; // acceleration and orientation
    //    private MovementDetector movementDetector; // walking vs standing
    private SensorsStepCounter sensorsStepCounter; // step counter

    private TrackerLogicPlusIMU lpTracker;
    private Algo1Radiomap floorSelector;
    private String lastFloor;
    private String dvid;
    private boolean mAutomaticGPSBuildingSelection;

    public AnyplaceFeedbackLoggerActivity() throws MalformedURLException {
    }
//    private boolean isTrackingErrorBackground;


    private void resetUserMarker() {
        this.mMarker.remove();
        this.mMarker = null;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
//        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
//        dvid = wifiManager.getConnectionInfo().getMacAddress();
        dvid = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;

        mAutomaticGPSBuildingSelection = false;
        userData = new AnyUserData();
        isTrackingErrorBackground = true;
        wifi = SimpleWifiManager.getInstance();
        // Create new receiver to get broadcasts
        // Create new receiver to get broadcasts
        receiverWifi = new SimpleWifiReceiver();
        wifi.registerScan(receiverWifi);
//        wifi.startScan(preferences.getString("samples_interval", "1000"));
        wifi.startScan();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.feedback_logger);
        textFloor = (TextView) findViewById(R.id.textFloor);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        btnRecord = (Button) findViewById(R.id.recordBtn);
        movementDetector = new MovementDetector();
        sensorsMain = new SensorsMain(getApplicationContext());
        sensorsMain.addListener(movementDetector);
        sensorsStepCounter = new SensorsStepCounter(getApplicationContext(), sensorsMain);
        lpTracker = new TrackerLogicPlusIMU(movementDetector, sensorsMain, sensorsStepCounter);
        btnRecord.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ArrayList<JSONObject> wifiObject = new ArrayList<JSONObject>();
                Log.d(TAG, "Record Button clicked");
                final JSONObject postData = new JSONObject();
                JSONObject gpsLocJson = new JSONObject();
                JSONObject wifiLocJson = new JSONObject();
                JSONObject userLocJson = new JSONObject();
                if (mCurrentFloor == null || !mCurrentFloor.isFloorValid()) {
                    Toast.makeText(getBaseContext(), "Load map before recording...", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (mMarker == null) {
                    Toast.makeText(getBaseContext(), "Set User Marker", Toast.LENGTH_SHORT).show();
                    return;
                }
                List<ScanResult> wifiList = wifi.getScanResults();
                for (int i = 0; i < wifiList.size(); i++) {
                    ScanResult scanResult = wifiList.get(i);
                    String ssid = scanResult.SSID;
                    JSONObject temp = new JSONObject();
                    try {
                        if (ssid == null || scanResult.SSID.isEmpty()) {
                            temp.put("ssid", unknownSSID);
                        } else {
                            temp.put("ssid", ssid);
                        }
                        temp.put("bssid", scanResult.BSSID);
                        temp.put("level", scanResult.level);
                    } catch (JSONException e) {
                        resetUserMarker();
                        Toast.makeText(getBaseContext(), "Json Error", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    wifiObject.add(temp);
                }


                try {
                    postData.put("buid", mCurrentBuilding.buid);
                    postData.put("floor", mCurrentFloor.floor_number);
                    postData.put("dvid", dvid);
                    postData.put("raw_radio", wifiObject);

                    gpsLocJson.put("lat", Double.toString(gpsMarker.getPosition().latitude));
                    gpsLocJson.put("lon", Double.toString(gpsMarker.getPosition().longitude));

                    if (gpsLocation != null) {
                        gpsLocJson.put("acc", Float.toString(gpsLocation.getAccuracy()));
                    } else {
                        gpsLocJson.put("acc", "0.0");
                    }

                    postData.put("gps", gpsLocJson.toString());

                    wifiLocJson.put("lat", Double.toString(wifiMarker.getPosition().latitude));
                    wifiLocJson.put("lon", Double.toString(wifiMarker.getPosition().longitude));
                    postData.put("wifi", wifiLocJson.toString());

                    userLocJson.put("lat", Double.toString(mMarker.getPosition().latitude));
                    userLocJson.put("lon", Double.toString(mMarker.getPosition().longitude));
                    postData.put("usr", userLocJson.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                    Toast.makeText(getBaseContext(), "Json Error", Toast.LENGTH_SHORT).show();
                    resetUserMarker();
                    return;
                }
                Log.d(TAG, postData.toString());

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        StringEntity se = null;
                        try {
                            se = new StringEntity(postData.toString());
                            se.setContentType(new BasicHeader(HTTP.CONTENT_TYPE, "application/json"));
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                        HttpResponse response = null;
                        HttpParams httpParameters = new BasicHttpParams();
                        HttpConnectionParams.setConnectionTimeout(httpParameters, 20000); // 20seconds
                        HttpConnectionParams.setSoTimeout(httpParameters, 20000); // 20 seconds
                        HttpClient httpClient = new DefaultHttpClient(httpParameters);
                        HttpPost httpPost = new HttpPost(url);
                        Log.d(TAG, url);
                        httpPost.setEntity(se);
                        httpPost.setParams(httpParameters);
                        HttpContext localContext = new BasicHttpContext();


                        try {
                            response = httpClient.execute(httpPost, localContext);
                        } catch (IOException e) {
                            Looper.prepare();
                            Log.e(TAG, "Error In Request");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getBaseContext(), "Error in Request", Toast.LENGTH_SHORT).show();
                                }
                            });
                            e.printStackTrace();
                            return;
                        }
                        StatusLine statusLine = response.getStatusLine();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getBaseContext(), "Data is Logged", Toast.LENGTH_SHORT).show();
                            }
                        });
                        Log.d(TAG, statusLine.toString());
                    }
                }).start();
                resetUserMarker();
            }
        });

        btnTrackme = (ImageButton) findViewById(R.id.btnTrackme);

        btnTrackme.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                if (gpsMarker != null) {
                    AnyplaceCache mAnyPlaceCache = AnyplaceCache.getInstance(AnyplaceFeedbackLoggerActivity.this);
                    mAnyPlaceCache.loadWorldBuildings(new FetchBuildingsTaskListener() {
                        @Override
                        public void onErrorOrCancel(String result) {
                            Log.d(TAG, result);
                        }

                        @Override
                        public void onSuccess(String result, List<BuildingModel> buildings) {
                            FetchNearBuildingsTask nearest = new FetchNearBuildingsTask();
                            nearest.run(buildings.iterator(), gpsMarker.getPosition().latitude, gpsMarker.getPosition().longitude, 100);
                            if (nearest.buildings.size() > 0) {
                                //todo add loading building
                                bypassSelectBuildingActivity(nearest.buildings.get(0));
                                Log.d(TAG, "found buildings");
                            } else {
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(gpsMarker.getPosition(), mInitialZoomLevel));
                            }
                        }
                    }, AnyplaceFeedbackLoggerActivity.this, false);

                }
            }
        });

        ImageButton btnFloorUp = (ImageButton) findViewById(R.id.btnFloorUp);
        btnFloorUp.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                if (mCurrentBuilding == null) {
                    Toast.makeText(getBaseContext(), "Load a map before tracking can be used!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Move one floor up
                int index = mCurrentBuilding.getSelectedFloorIndex();

                if (mCurrentBuilding.checkIndex(index + 1)) {
                    bypassSelectBuildingActivity(mCurrentBuilding, mCurrentBuilding.getFloors().get(index + 1));
                }
                lpTracker.reset();

            }
        });

        ImageButton btnFloorDown = (ImageButton) findViewById(R.id.btnFloorDown);
        btnFloorDown.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (mCurrentBuilding == null) {
                    Toast.makeText(getBaseContext(), "Load a map before tracking can be used!", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Move one floor down
                int index = mCurrentBuilding.getSelectedFloorIndex();

                if (mCurrentBuilding.checkIndex(index - 1)) {
                    bypassSelectBuildingActivity(mCurrentBuilding, mCurrentBuilding.getFloors().get(index - 1));
                }
                lpTracker.reset();
            }

        });

        scanResults = (TextView) findViewById(R.id.detectedAPs);
        mTrackingInfoView = (TextView) findViewById(R.id.trackingInfoData);

        //set location from GPS
        mLocationRequest = LocationRequest.create();
//        mLocationClient.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000, 10, new MyLocationListener());
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(2000);
        mLocationRequest.setFastestInterval(1000);
        mLocationClient = new LocationClient(this, this, this);


        PreferenceManager.setDefaultValues(this, SHARED_PREFS_LOGGER, MODE_PRIVATE, R.xml.preferences_feedback, true);
        preferences = getSharedPreferences(SHARED_PREFS_LOGGER, MODE_PRIVATE);
        preferences.registerOnSharedPreferenceChangeListener(this);
        lpTracker.setAlgorithm(preferences.getString("TrackingAlgorithm", "WKNN"));
        onSharedPreferenceChanged(preferences, "walk_bar");

        String folder_browser = preferences.getString("folder_browser", null);
        if (folder_browser == null) {
            File f = new File(Environment.getExternalStorageDirectory() + File.separator + getResources().getString(R.string.app_name));
            f.mkdirs();
            if (f.mkdirs() || f.isDirectory()) {
                String path = f.getAbsolutePath();
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString("folder_browser", path);
                editor.commit();
            }
        } else {
            File f = new File(folder_browser);
            f.mkdirs();
        }


        requestForPermissions();
    }

    @TargetApi(Build.VERSION_CODES.M)
    private void requestForPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String permissionString = Manifest.permission.WRITE_EXTERNAL_STORAGE;

            if (this.checkSelfPermission(permissionString)
                    != PackageManager.PERMISSION_GRANTED) {
                this.requestPermissions(
                        new String[]{permissionString},
                        PERMISSION_STORAGE_WRITE);
            } else {
                setUpMapIfNeeded();
            }
        } else {
            setUpMapIfNeeded();
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals("TrackingAlgorithm")) {
            lpTracker.setAlgorithm(sharedPreferences.getString("TrackingAlgorithm", "WKNN"));
        }

    }

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

    private void updateLocation(GeoPoint gps) {
        if (gpsMarker != null) {
            // draw the location of the new position
            gpsMarker.remove();
        }
        Log.d(TAG, "GPS Location " + gps.toString());
        MarkerOptions marker = new MarkerOptions();
        marker.position(new LatLng(gps.dlat, gps.dlon));
        marker.title("User").snippet("Estimated Position");
        marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon21));
        marker.rotation(raw_heading - bearing);
        gpsMarker = this.mMap.addMarker(marker);
//        updateLocation();

    }

    private void updateLocation() {

        GeoPoint location = userData.getLatestUserPosition();
//        GeoPoint location = userData.getPositionWifi();
//        Log.d(TAG, "location  " + Double.toString(location.dlat) + " " +  Double.toString(location.dlon));
        if (location != null) {
            // draw the location of the new position
            if (wifiMarker != null) {
                wifiMarker.remove();
            }
            Log.d(TAG, "WiFi Location " + location.toString());
            MarkerOptions marker = new MarkerOptions();
            marker.position(new LatLng(location.dlat, location.dlon));
            marker.title("User").snippet("Estimated Position");
            marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.wifi_icon));
            marker.rotation(raw_heading - bearing);
            wifiMarker = this.mMap.addMarker(marker);
        }
    }

    private void handleBuildingsOnMap() {

        AnyplaceCache mAnyplaceCache = AnyplaceCache.getInstance(AnyplaceFeedbackLoggerActivity.this);
        mAnyplaceCache.loadWorldBuildings(new FetchBuildingsTaskListener() {

            @Override
            public void onSuccess(String result, List<BuildingModel> buildings) {
                List<BuildingModel> collection = new ArrayList<BuildingModel>(buildings);
                mClusterManager.clearItems();
                if (mCurrentBuilding != null)
                    collection.remove(mCurrentBuilding);
                mClusterManager.addItems(collection);
                mClusterManager.cluster();
                // HACK. This dumps all the cached icons & recreates everything.
                mClusterManager.setRenderer(new MyBuildingsRenderer(AnyplaceFeedbackLoggerActivity.this, mMap, mClusterManager));

//                updateMarker(gpsMarker.getPosition());

            }

            @Override
            public void onErrorOrCancel(String result) {

            }

        }, this, false);
    }

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
            initMap();
            // initCamera();
            initListeners();
        }

    }

    private void initListeners() {

        mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition position) {
                // change search box message and clear pois
                if (searchType != AnyPlaceSeachingHelper.getSearchType(position.zoom)) {
                    searchType = AnyPlaceSeachingHelper.getSearchType(position.zoom);
                    if (searchType == SearchTypes.INDOOR_MODE) {
                        btnTrackme.setVisibility(View.INVISIBLE);
                        btnRecord.setVisibility(View.VISIBLE);
                        mLocationClient.removeLocationUpdates(AnyplaceFeedbackLoggerActivity.this);
//                        if (gpsMarker != null) {
//                            // draw the location of the new position
//                            gpsMarker.remove();
//                        }
//                        wifiMarker

                    } else if (searchType == SearchTypes.OUTDOOR_MODE) {
                        btnTrackme.setVisibility(View.VISIBLE);
                        btnRecord.setVisibility(View.INVISIBLE);
                        mLocationClient.requestLocationUpdates(mLocationRequest, AnyplaceFeedbackLoggerActivity.this);

                    }
                }

                bearing = position.bearing;
                mClusterManager.onCameraChange(position);
            }
        });

        mMap.setOnMapClickListener(this);

        mMap.setOnMarkerDragListener(new OnMarkerDragListener() {

            @Override
            public void onMarkerDragStart(Marker arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onMarkerDragEnd(Marker arg0) {
                // TODO Auto-generated method stub
                LatLng dragPosition = arg0.getPosition();

//                if (mIsSamplingActive) {
//                    saveRecordingToLine(dragPosition);
//                }

                currLocation = dragPosition;

            }

            @Override
            public void onMarkerDrag(Marker arg0) {
                // TODO Auto-generated method stub

            }
        });

        mMap.setOnMarkerClickListener(mClusterManager);

        mClusterManager.setOnClusterItemClickListener(new OnClusterItemClickListener<BuildingModel>() {

            @Override
            public boolean onClusterItemClick(final BuildingModel b) {
                if (b != null) {
                    bypassSelectBuildingActivity(b);
                }
                // Prevent Popup dialog
                return true;
            }
        });
    }

    private void initMap() {
        // Sets the map type to be NORMAL - ROAD mode
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        // mMap.setMyLocationEnabled(true); //displays a button to navigate to
        // the current user's position
    }

    private void initCamera() {
        // Only for the first time
        if (gpsMarker != null) {
            return;
        }

        Location gps = mLocationClient.getLastLocation();
        if (gps != null) {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(gps.getLatitude(), gps.getLongitude()), mInitialZoomLevel), new CancelableCallback() {

                @Override
                public void onFinish() {
                    handleBuildingsOnMap();
                }

                @Override
                public void onCancel() {
                    handleBuildingsOnMap();
                }
            });
        } else {
            AsyncTask<Void, Integer, Void> task = new AsyncTask<Void, Integer, Void>() {

                GeoPoint location;

                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        location = AndroidUtils.getIPLocation();
//                        location = userData.getLocationGPSorIP();
                    } catch (Exception e) {

                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {

                    if (location != null && gpsMarker == null) {
                        userData.setLocationIP(location);
                        updateLocation(location);
                        updateLocation();
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.dlat, location.dlon), mInitialZoomLevel), new CancelableCallback() {

                            @Override
                            public void onFinish() {
                                handleBuildingsOnMap();
                            }

                            @Override
                            public void onCancel() {
                                handleBuildingsOnMap();
                            }
                        });
                    } else {
                        handleBuildingsOnMap();
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

    private void loadSelectBuildingActivity(GeoPoint loc, boolean invisibleSelection) {

        Intent placeIntent = new Intent(AnyplaceFeedbackLoggerActivity.this, SelectBuildingActivity.class);
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

    @Override
    public void onConnected(Bundle bundle) {
        if (checkPlayServices()) {
            initCamera();
            SearchTypes type = AnyPlaceSeachingHelper.getSearchType(mMap.getCameraPosition().zoom);
//            if (type == SearchTypes.INDOOR_MODE) {
//                mLocationClient.removeLocationUpdates(AnyplaceFeedbackLoggerActivity.this);
//            } else if (type == SearchTypes.OUTDOOR_MODE) {
//                mLocationClient.requestLocationUpdates(mLocationRequest, AnyplaceFeedbackLoggerActivity.this);
//            }
            mLocationClient.requestLocationUpdates(mLocationRequest, AnyplaceFeedbackLoggerActivity.this);
            Location currentLocation = mLocationClient.getLastLocation();
            // we must set listener to the get the first location from the API
            // it will trigger the onLocationChanged below when a new location
            // is found or notify the user
            mLocationClient.requestLocationUpdates(mLocationRequest, this);
            if (currentLocation != null) {
                    onLocationChanged(currentLocation);
            }
        }

    }

    @Override
    public void onDisconnected() {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Play Services connection failed");
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(this, LOCATION_CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0);
        }
    }

    @Override
    public void onPause() {
        Log.i(TAG, "onPause");
        super.onPause();
        lpTracker.pauseTracking();
        sensorsMain.pause();
        sensorsStepCounter.pause();
        removeTrackerListeners();
//        if (!mIsSamplingActive) {
//            positioning.pause();
//        }
    }

    @Override
    public void onResume() {
        Log.i(TAG, "onResume");
        super.onResume();
        setUpMapIfNeeded();
        addTrackerListeners();
        sensorsMain.resume();
        sensorsStepCounter.resume();
        lpTracker.resumeTracking();

//        if (!mIsSamplingActive) {
//            positioning.resume();
//        }
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
                    AndroidUtils.showGPSSettings(AnyplaceFeedbackLoggerActivity.this);
                }
            }
        };
        WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        boolean isWifiOn = wifi.isWifiEnabled();
        boolean isOnline = NetworkUtils.isOnline(AnyplaceFeedbackLoggerActivity.this);
        if (!isOnline) {
            AndroidUtils.showWifiSettings(this, "No Internet Connection", null, checkGPS);
        } else if (!isWifiOn) {
            AndroidUtils.showWifiSettings(this, "WiFi is disabled", null, checkGPS);
        } else {
            checkGPS.run();
        }
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
    protected void onDestroy() {
        super.onDestroy();

        wifi.unregisterScan(receiverWifi);
    }


    @Override
    public void onLocationChanged(Location location) {
        if (location != null) {
            Log.d(TAG, "OnLocationChanged Called");
            GeoPoint gps;
            userData.setLocationGPS(location);
            this.gpsLocation = location;
            if (AnyplaceAPI.DEBUG_WIFI) {
                gps = AnyUserData.fakeGPS();
            } else {
                Log.d("GPS Accuracy", Float.toString(location.getAccuracy()));
                gps = new GeoPoint(location.getLatitude(), location.getLongitude());
            }
            if (mAutomaticGPSBuildingSelection) {
                mAutomaticGPSBuildingSelection = false;
                loadSelectBuildingActivity(userData.getLatestUserPosition(), true);
            }
            updateLocation(gps);
            updateLocation();
        }
    }

    @Override
    public void onMapClick(LatLng latLng) {
        updateMarker(latLng);
//        updateInfoView();

    }


    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case SELECT_PLACE_ACTIVITY_RESULT:
                if (resultCode == RESULT_OK) {
                    if (data == null) {
                        return;
                    }

                    String fpf = data.getStringExtra("floor_plan_path");
                    if (fpf == null) {
                        Toast.makeText(getBaseContext(), "You haven't selected both building and floor...!", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    try {
                        BuildingModel b = AnyplaceCache.getInstance(this).getSpinnerBuildings().get(data.getIntExtra("bmodel", 0));
                        FloorModel f = b.getFloors().get(data.getIntExtra("fmodel", 0));

                        bypassSelectBuildingActivity(b, f);
//                        selectPlaceActivityResult(b, f);
                    } catch (Exception ex) {
                        Toast.makeText(getBaseContext(), "You haven't selected both building and floor...!", Toast.LENGTH_SHORT).show();
                    }
                } else if (resultCode == RESULT_CANCELED) {
                    // CANCELLED
                    if (data == null) {
                        return;
                    }
                    String msg = (String) data.getSerializableExtra("message");
                    if (msg != null) {
                        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_LONG).show();
                    }
                }
                break;
            case PREFERENCES_ACTIVITY_RESULT:
                if (resultCode == RESULT_OK) {
                    FeedbackPrefs.Action result = (Action) data.getSerializableExtra("action");

                    switch (result) {
                        case REFRESH_BUILDING:

                            if (mCurrentBuilding == null) {
                                Toast.makeText(getBaseContext(), "Load a map before performing this action!", Toast.LENGTH_SHORT).show();
                                break;
                            }

                            if (progressBar.getVisibility() == View.VISIBLE) {
                                Toast.makeText(getBaseContext(), "Building Loading in progress. Please Wait!", Toast.LENGTH_SHORT).show();
                                break;
                            }

                            try {

                                // clear_floorplans
                                File floorsRoot = new File(AnyplaceUtils.getFloorPlansRootFolder(this), mCurrentBuilding.buid);
                                // clear radiomaps
                                File radiomapsRoot = AnyplaceUtils.getRadioMapsRootFolder(this);
                                final String[] radiomaps = radiomapsRoot.list(new FilenameFilter() {

                                    @Override
                                    public boolean accept(File dir, String filename) {
                                        if (filename.startsWith(mCurrentBuilding.buid))
                                            return true;
                                        else
                                            return false;
                                    }
                                });
                                for (int i = 0; i < radiomaps.length; i++) {
                                    radiomaps[i] = radiomapsRoot.getAbsolutePath() + File.separator + radiomaps[i];
                                }

                                DeleteFolderBackgroundTask task = new DeleteFolderBackgroundTask(new DeleteFolderBackgroundTask.DeleteFolderBackgroundTaskListener() {

                                    @Override
                                    public void onSuccess() {
                                        bypassSelectBuildingActivity(mCurrentBuilding, mCurrentBuilding.getSelectedFloor());
                                    }
                                }, this, true);
                                task.setFiles(floorsRoot);
                                task.setFiles(radiomaps);
                                task.execute();
                            } catch (Exception e) {
                                Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                            }
                    }
                    break;
                }
                break;
        }
    }

    private void bypassSelectBuildingActivity(final BuildingModel b) {

        if (b != null) {

//            if (mIsSamplingActive) {
//                Toast.makeText(getBaseContext(), "Invalid during logging.", Toast.LENGTH_LONG).show();
//                return;
//            }

            // Load Building
            b.loadFloors(new FetchFloorsByBuidTaskListener() {

                @Override
                public void onSuccess(String result, List<FloorModel> floors) {

                    AnyplaceCache mAnyplaceCache = AnyplaceCache.getInstance(AnyplaceFeedbackLoggerActivity.this);
                    ArrayList<BuildingModel> list = new ArrayList<BuildingModel>(1);
                    list.add(b);
                    mAnyplaceCache.setSelectedBuildingIndex(0);
                    mAnyplaceCache.setSpinnerBuildings(list);

                    FloorModel floor;
                    if ((floor = b.getFloorFromNumber("0")) == null) {
                        floor = b.getSelectedFloor();
                    }

                    bypassSelectBuildingActivity(b, floor);
                }

                @Override
                public void onErrorOrCancel(String result) {
                    Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();

                }
            }, AnyplaceFeedbackLoggerActivity.this, false, true);
        }
    }

    private void bypassSelectBuildingActivity(final BuildingModel b, final FloorModel f) {
        Log.d(TAG, "bypassSelectBuildingActivity called");
        final FetchFloorPlanTask fetchFloorPlanTask = new FetchFloorPlanTask(this, b.buid, f.floor_number);
        selectPlaceActivityResult(b, f);

        fetchFloorPlanTask.setCallbackInterface(new FetchFloorPlanTask.FetchFloorPlanTaskListener() {

            private ProgressDialog dialog;

            @Override
            public void onSuccess(String result, File floor_plan_file) {
                if (dialog != null)
                    dialog.dismiss();
            }

            @Override
            public void onErrorOrCancel(String result) {
                if (dialog != null)
                    dialog.dismiss();
                Toast.makeText(getBaseContext(), result, Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onPrepareLongExecute() {
                dialog = new ProgressDialog(AnyplaceFeedbackLoggerActivity.this);
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


    private void loadMapBasicLayer(BuildingModel b, FloorModel f) {
        // remove the previous GroundOverlay or TileOverlay
        mMap.clear();
        // load the floorplan
        // add the Tile Provider that uses our Building tiles over
        // Google Maps
        TileOverlay mTileOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(new AnyPlaceMapTileProvider(getBaseContext(), b.buid, f.floor_number)).zIndex(0));
    }


    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        // prevent orientation change when auto-rotate is enabled on Android OS
        super.onConfigurationChanged(newConfig);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }

    // MENUS
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.main_menu_feedback, menu);

        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.feed_back_main_menu_loadmap: {
                // start the activity where the user can select the building
//                if (mIsSamplingActive) {
//                    Toast.makeText(this, "Invalid during logging.", Toast.LENGTH_LONG).show();
//                    return true;
//                }

                Location currentLocation = mLocationClient.getLastLocation();

                Intent placeIntent = new Intent(this, SelectBuildingActivity.class);
                Bundle b = new Bundle();
                if (currentLocation != null) {
                    b.putString("coordinates_lat", String.valueOf(currentLocation.getLatitude()));
                    b.putString("coordinates_lon", String.valueOf(currentLocation.getLongitude()));
                }

                if (mCurrentBuilding == null) {
                    b.putSerializable("mode", SelectBuildingActivity.Mode.NEAREST);
                }

                placeIntent.putExtras(b);
                startActivityForResult(placeIntent, SELECT_PLACE_ACTIVITY_RESULT);
                return true;
            }
            case R.id.feed_back_main_menu_clear_logging: {
                if (mCurrentBuilding == null)
                    Toast.makeText(getBaseContext(), "Load a map before tracking can be used!", Toast.LENGTH_SHORT).show();
                else {
                    loadMapBasicLayer(mCurrentBuilding, mCurrentFloor);
                    handleBuildingsOnMap();

                    if (currLocation != null)
                        updateMarker(currLocation);
                }
                return true;
            }

            // Launch preferences
            case R.id.feed_back_main_menu_preferences: {
                Intent i = new Intent(this, FeedbackPrefs.class);
                startActivityForResult(i, PREFERENCES_ACTIVITY_RESULT);
                return true;
            }
            case R.id.feed_back_main_menu_about: {
                startActivity(new Intent(AnyplaceFeedbackLoggerActivity.this, AnyplaceAboutActivity.class));
                return true;
            }

            case R.id.feed_back_main_menu_exit: {
                this.finish();
                System.gc();
            }
        }
        return false;
    }

    private void updateMarker(LatLng latlng) {
        if (this.mMarker != null) {
            this.mMarker.remove();
        }
        this.mMarker = this.mMap.addMarker(new MarkerOptions().position(latlng).draggable(true).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
        currLocation = latlng;
    }

    private void selectPlaceActivityResult(final BuildingModel b, FloorModel f) {
        Log.d(TAG,"selectPlaceActivityresult");
        // set the newly selected floor
        b.setSelectedFloor(f.floor_number);
        mCurrentBuilding = b;
        mCurrentFloor = f;
        currLocation = null;
        userIsNearby = false;
        textFloor.setText(f.floor_name);
        disableAnyplaceTracker();
        userData.setSelectedBuilding(b);
        userData.setSelectedFloor(f);
        loadMapBasicLayer(b, f);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(b.getPosition(), 19.0f), new CancelableCallback() {

            @Override
            public void onFinish() {
                handleBuildingsOnMap();
                GeoPoint location = userData.getLocationGPSorIP();
                updateLocation(location);
                updateLocation();
                enableAnyplaceTracker();
                Log.d(TAG,"Tracker Enabled at selectPlaceActivityResult on finish");
            }

            @Override
            public void onCancel() {
            }
        });

        class Callback implements DownloadRadioMapListener, PreviousRunningTask {
            boolean progressBarEnabled = false;
            boolean disableSuccess = false;

            @Override
            public void onSuccess(String result) {

                if (disableSuccess) {
                    Log.d(TAG, "callback Disable success");
                    onErrorOrCancel("");
                    return;
                }
                enableAnyplaceTracker();
                Log.d(TAG, "tracker enabled at selectPlaceActivityResult callback");

                File root;
                try {
                    root = AnyplaceUtils.getRadioMapFoler(AnyplaceFeedbackLoggerActivity.this, mCurrentBuilding.buid, mCurrentFloor.floor_number);
                    File f = new File(root, AnyplaceUtils.getRadioMapFileName(mCurrentFloor.floor_number));

//                    new HeatmapTask().execute(f);
                } catch (Exception e) {
                    Log.d(TAG,"Radio Map Not Downloaded");
                }

                if (AnyplaceAPI.PLAY_STORE) {

                    AnyplaceCache mAnyplaceCache = AnyplaceCache.getInstance(AnyplaceFeedbackLoggerActivity.this);
                    mAnyplaceCache.fetchAllFloorsRadiomapsRun(new BackgroundFetchListener() {

                        @Override
                        public void onSuccess(String result) {
                            hideProgressBar();
                            if (AnyplaceAPI.DEBUG_MESSAGES) {
                                btnTrackme.setBackgroundColor(Color.YELLOW);
                            }
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

                    }, mCurrentBuilding);
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
                progressBar.setProgress((int) (1.0f / (b.getFloors().size() * 2) * progressBar.getMax()));
            }

            @Override
            public void disableSuccess() {
                disableSuccess = true;
            }
        }

        if (downloadRadioMapTaskBuid != null) {
            Log.d(TAG, "downloadRadioMapTaskBuid is not null");
            ((PreviousRunningTask) downloadRadioMapTaskBuid.getCallbackInterface()).disableSuccess();
        }

        downloadRadioMapTaskBuid = new DownloadRadioMapTaskBuid(new Callback(), this, b.getLatitudeString(), b.getLongitudeString(), b.buid, f.floor_number, false);

        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            // Execute task parallel with others and multiple instances of
            // itself
            downloadRadioMapTaskBuid.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            Log.d(TAG, "radio Map downloaded");
        } else {
            downloadRadioMapTaskBuid.execute();
        }

        try {
            File root = AnyplaceUtils.getRadioMapFoler(this, b.buid, userData.getSelectedFloorNumber());
            lpTracker.setRadiomapFile(new File(root, AnyplaceUtils.getRadioMapFileName(userData.getSelectedFloorNumber())).getAbsolutePath());
            Log.d(TAG,"radio map set for the tracker : " + new File(root, AnyplaceUtils.getRadioMapFileName(userData.getSelectedFloorNumber())).getAbsolutePath());
        } catch (Exception e) {
            // exception thrown by GetRootFolder when sdcard is not writable
            Log.d(TAG, "Unable to set radio map for the tracker");
            Toast.makeText(getBaseContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
        }
        //showHelp("Help", "<b>1.</b> Select your floor (using arrows on the right).<br><b>2.</b> Click on the map (to identify your location).");
    }

    private void enableAnyplaceTracker() {
        // Do not change file wile enabling tracker
        Log.d(TAG, "trackeron");
        if (lpTracker.trackOn()) {
            btnTrackme.setImageResource(R.drawable.dark_device_access_location_searching);
            isTrackingErrorBackground = false;
        }

    }

    private void disableAnyplaceTracker() {
        Log.d(TAG, "trackeroff");
        lpTracker.trackOff();
        btnTrackme.setImageResource(R.drawable.dark_device_access_location_off);
        isTrackingErrorBackground = true;
    }

    private void addTrackerListeners() {
        Log.d(TAG, "Added Tracker Listeners");

        // sensorsMain.addListener((SensorsMain.IOrientationListener) this);
        lpTracker.addListener((AnyplaceTracker.WifiResultsAnyplaceTrackerListener) this);
        lpTracker.addListener((AnyplaceTracker.TrackedLocAnyplaceTrackerListener) this);
        lpTracker.addListener((AnyplaceTracker.ErrorAnyplaceTrackerListener) this);
//        floorSelector.addListener((FloorSelector.FloorAnyplaceFloorListener) this);
//        floorSelector.addListener((FloorSelector.ErrorAnyplaceFloorListener) this);
    }

    private void removeTrackerListeners() {
        Log.d(TAG, "Removed Tracker Listeners");
        // sensorsMain.removeListener((SensorsMain.IOrientationListener) this);
        lpTracker.removeListener((AnyplaceTracker.WifiResultsAnyplaceTrackerListener) this);
        lpTracker.removeListener((AnyplaceTracker.TrackedLocAnyplaceTrackerListener) this);
        lpTracker.removeListener((AnyplaceTracker.ErrorAnyplaceTrackerListener) this);
//        floorSelector.removeListener((FloorSelector.FloorAnyplaceFloorListener) this);
//        floorSelector.removeListener((FloorSelector.ErrorAnyplaceFloorListener) this);
    }




    private void showHelp(String title, String message) {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        LayoutInflater adbInflater = LayoutInflater.from(this);
        View eulaLayout = adbInflater.inflate(R.layout.info_window_help, null);
        final CheckBox dontShowAgain = (CheckBox) eulaLayout.findViewById(R.id.skip);
        adb.setView(eulaLayout);
        adb.setTitle(Html.fromHtml(title));
        adb.setMessage(Html.fromHtml(message));
        adb.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean("skipHelpMessage", dontShowAgain.isChecked());
                editor.commit();
                return;
            }
        });

        Boolean skipMessage = preferences.getBoolean("skipHelpMessage", false);
        if (!skipMessage)
            adb.show();
    }
    private void showProgressBar() {
        progressBar.setVisibility(View.VISIBLE);
    }

    private void hideProgressBar() {
        progressBar.setVisibility(View.GONE);
    }



    @Override
    public void onNewLocation(LatLng pos) {
        Log.d(TAG,"OnNewLocation Called");
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
    public void onTrackerError(String msg) {
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
    public void onNewWifiResults(int aps) {
         Log.d(TAG, "wifi res: " + Integer.toString(aps) );
    }

    private class OrientationListener implements SensorsMain.IOrientationListener {
        @Override
        public void onNewOrientation(float[] values) {
            raw_heading = values[0];
//            updateInfoView();
        }
    }

    private class WalkingListener implements MovementDetector.MovementListener {

        @Override
        public void onWalking() {
            walking = true;
//            updateInfoView();
        }

        @Override
        public void onStanding() {
            walking = false;
//            updateInfoView();
        }

    }


    //
    // The WifiReceiver is responsible to Receive Access Points results
    //
    private class SimpleWifiReceiver extends WifiReceiver {

        @Override
        public void onReceive(Context c, Intent intent) {

            // Log.d("SimpleWiFi Receiver", "wifi received");

            try {
                if (intent == null || c == null || intent.getAction() == null)
                    return;

                List<ScanResult> wifiList = wifi.getScanResults();
                scanResults.setText("AP : " + wifiList.size());
                // If we are not in an active sampling session we have to skip
                // this intent

            } catch (RuntimeException e) {
                Toast.makeText(c, "RuntimeException [" + e.getMessage() + "]", Toast.LENGTH_SHORT).show();
                return;
            }
        }
    }

    interface PreviousRunningTask {
        void disableSuccess();
    }
}


