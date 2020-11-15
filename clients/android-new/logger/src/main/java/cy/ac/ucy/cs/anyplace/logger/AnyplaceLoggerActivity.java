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

package cy.ac.ucy.cs.anyplace.logger;


import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MapStyleOptions;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.ClusterManager.OnClusterItemClickListener;
import com.google.maps.android.heatmaps.HeatmapTileProvider;
import com.google.maps.android.heatmaps.WeightedLatLng;

import cy.ac.ucy.cs.anyplace.lib.RadioMap;
import cy.ac.ucy.cs.anyplace.lib.android.logger.LogRecordMap;
import cy.ac.ucy.cs.anyplace.lib.android.nav.AnyPlaceSeachingHelper;
import cy.ac.ucy.cs.anyplace.logger.LoggerPrefs.Action;
import cy.ac.ucy.cs.anyplace.lib.android.logger.LoggerWiFi;
import cy.ac.ucy.cs.anyplace.lib.android.logger.LoggerWiFi.Function;
import cy.ac.ucy.cs.anyplace.lib.android.tasks.UploadRSSLogTask;
import cy.ac.ucy.cs.anyplace.lib.android.tasks.FetchBuildingsTask.FetchBuildingsTaskListener;
import cy.ac.ucy.cs.anyplace.lib.android.wifi.SimpleWifiManager;
import cy.ac.ucy.cs.anyplace.lib.android.wifi.WifiReceiver;
import cy.ac.ucy.cs.anyplace.lib.android.sensors.MovementDetector;
import cy.ac.ucy.cs.anyplace.lib.android.sensors.SensorsMain;
import cy.ac.ucy.cs.anyplace.lib.android.nav.BuildingModel;
import cy.ac.ucy.cs.anyplace.lib.android.nav.FloorModel;
import cy.ac.ucy.cs.anyplace.lib.android.nav.AnyUserData;
import cy.ac.ucy.cs.anyplace.lib.android.nav.AnyPlaceSeachingHelper.SearchTypes;
import cy.ac.ucy.cs.anyplace.lib.android.cache.AnyplaceCache;
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceDebug;
import cy.ac.ucy.cs.anyplace.lib.android.cache.BackgroundFetchListener;
import cy.ac.ucy.cs.anyplace.lib.android.googlemap.MapTileProvider;
import cy.ac.ucy.cs.anyplace.lib.android.googlemap.MyBuildingsRenderer;
import cy.ac.ucy.cs.anyplace.lib.android.utils.NetworkUtils;
import cy.ac.ucy.cs.anyplace.lib.android.utils.GeoPoint;
import cy.ac.ucy.cs.anyplace.lib.android.utils.AnyplaceUtils;
import cy.ac.ucy.cs.anyplace.lib.android.utils.AndroidUtils;
import cy.ac.ucy.cs.anyplace.lib.android.tasks.FetchFloorsByBuidTask.FetchFloorsByBuidTaskListener;
import cy.ac.ucy.cs.anyplace.lib.android.tasks.DeleteFolderBackgroundTask;
import cy.ac.ucy.cs.anyplace.lib.android.tasks.DownloadRadioMapTaskBuid;
import cy.ac.ucy.cs.anyplace.lib.android.tasks.FetchFloorPlanTask;
import cy.ac.ucy.cs.anyplace.lib.android.tasks.FetchNearBuildingsTask;
import cy.ac.ucy.cs.anyplace.lib.android.tasks.DownloadRadioMapTaskBuid.DownloadRadioMapListener;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;

import java.io.File;
import java.io.FilenameFilter;
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
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.telecom.Call;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Anyplace Logger Activity. The main interface for the Logger functionality
 *
 */
public class AnyplaceLoggerActivity extends AppCompatActivity implements
        OnSharedPreferenceChangeListener, GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener, OnMapClickListener,
        OnMapReadyCallback {
  private static final String TAG = "AnyplaceLoggerActivity";


  // Define a request code to send to Google Play services This code is
  // returned in Activity.onActivityResult
  private final static int LOCATION_CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;
  private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9001;
  private final static int PREFERENCES_ACTIVITY_RESULT = 1114;
  private static final int SELECT_PLACE_ACTIVITY_RESULT = 1112;
  private final int REQUEST_PERMISSION_LOCATION = 1;

  private static final float mInitialZoomLevel = 18.0f;

  //Google API


  private LocationListener mLocationListener = this;
  // Location API
  //private LocationClient mLocationClient;
  // Define an object that holds accuracy and frequency parameters
  private LocationRequest mLocationRequest;

  private GoogleMap mMap;
  private Marker marker;
  private LatLng curLocation = null;
  private Location mLastLocation;

  private FusedLocationProviderClient mFusedLocationClient;


  // <Load Building and Marker>
  private ClusterManager<BuildingModel> mClusterManager;
  private DownloadRadioMapTaskBuid downloadRadioMapTaskBuid;
  private SearchTypes searchType = null;
  private Marker gpsMarker = null;
  private float bearing;
  private ImageButton btnTrackme;
  // </Load Building and Marker>

  // UI Elements
  ProgressBar progressBar;

  // WiFi manager
  private SimpleWifiManager wifi;

  // WiFi Receiver
  private WifiReceiver receiverWifi;

  // TextView showing the current floor
  private TextView textFloor;

  // TextView showing the current scan results
  private TextView scanResults;

  // ProgressDialog
  private ProgressDialog mSamplingProgressDialog;

  // Path to store rss file
  private String folder_path;

  // Filename to store rss records
  private String filename_rss;

  // Button that records access points
  private Button btnRecord;

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
  // Logger Service
  private int mCurrentSamplesTaken = 0;
  private boolean mIsSamplingActive = false;
  private LoggerWiFi logger;
  private AnyplaceApp app;

  private List<BuildingModel> builds;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    //app = (AnyplaceApp) getApplication();
    setContentView(R.layout.activity_logger);


    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);


    textFloor = (TextView) findViewById(R.id.textFloor);
    progressBar = (ProgressBar) findViewById(R.id.progressBar);
    btnRecord = (Button) findViewById(R.id.recordBtn);
    btnRecord.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        btnRecordingInfo();
      }
    });

    // setup the trackme button overlaid in the map
    btnTrackme = (ImageButton) findViewById(R.id.btnTrackme);





    btnTrackme.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {


        if (gpsMarker != null) {
          Log.d(TAG, " gpsMarker is not null");
          AnyplaceCache mAnyplaceCache = AnyplaceCache.getInstance(AnyplaceLoggerActivity.this);
          mAnyplaceCache.loadWorldBuildings(new FetchBuildingsTaskListener() {

            @Override
            public void onSuccess(String result, List<BuildingModel> buildings) {
              FetchNearBuildingsTask nearest = new FetchNearBuildingsTask();
              nearest.run(buildings.iterator(), gpsMarker.getPosition().latitude, gpsMarker.getPosition().longitude, 100);

              if (nearest.buildings.size() > 0) {
                bypassSelectBuildingActivity(nearest.buildings.get(0));
              } else {
                // mMap.getCameraPosition().zoom
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(gpsMarker.getPosition(), mInitialZoomLevel));
              }
            }

            @Override
            public void onErrorOrCancel(String result) {
              Toast.makeText(getBaseContext(), "Error localizing", Toast.LENGTH_SHORT).show();
            }

          }, AnyplaceLoggerActivity.this, false);
        }


        else{
          if(AnyplaceDebug.DEBUG_MESSAGES){
            Log.d(TAG, " gpsMarker is null");
          }


          AnyplaceCache mAnyplaceCache = AnyplaceCache.getInstance(AnyplaceLoggerActivity.this);
          //TODO: in MapUtils
          mAnyplaceCache.loadWorldBuildings(new FetchBuildingsTaskListener() {

            @Override
            public void onSuccess(String result, List<BuildingModel> buildings) {
              builds = buildings;
              checkLocationPermission();
              // mFusedLocationClient.requestLocationUpdates(mLocationRequest,mLocationCallbackInitial,Looper.myLooper());

              mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
                @Override
                public void onComplete(@NonNull Task<Location> task) {
                  Location loc = task.getResult();
                  LatLng coord = new LatLng(loc.getLatitude(),loc.getLongitude());
                  FetchNearBuildingsTask nearest = new FetchNearBuildingsTask();
                  nearest.run(builds.iterator(), coord.latitude, coord.longitude, 100);

                  if (nearest.buildings.size() > 0) {
                    bypassSelectBuildingActivity(nearest.buildings.get(0));
                  } else {
                    // mMap.getCameraPosition().zoom
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(coord, mInitialZoomLevel));
                  }
                }
              });



            }

            @Override
            public void onErrorOrCancel(String result) {
              Toast.makeText(getBaseContext(), "Error localizing", Toast.LENGTH_SHORT).show();
            }

          }, AnyplaceLoggerActivity.this, false);

        }

      }
    });

    ImageButton btnFloorUp = (ImageButton) findViewById(R.id.btnFloorUp);
    btnFloorUp.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {

        if (mCurrentBuilding == null) {
          Toast.makeText(getBaseContext(), "Load a map before tracking can be used!", Toast.LENGTH_SHORT).show();
          return;
        }

        if (mIsSamplingActive) {
          Toast.makeText(getBaseContext(), "Invalid during logging.", Toast.LENGTH_LONG).show();
          return;
        }

        // Move one floor up
        int index = mCurrentBuilding.getSelectedFloorIndex();

        if (mCurrentBuilding.checkIndex(index + 1)) {
          bypassSelectBuildingActivity(mCurrentBuilding, mCurrentBuilding.getFloors().get(index + 1));
        }

      }
    });

    ImageButton btnFloorDown = (ImageButton) findViewById(R.id.btnFloorDown);
    btnFloorDown.setOnClickListener(new OnClickListener() {

      @Override
      public void onClick(View v) {
        if (mCurrentBuilding == null) {
          Toast.makeText(getBaseContext(), "Load a map before tracking can be used!", Toast.LENGTH_SHORT).show();
          return;
        }

        if (mIsSamplingActive) {
          Toast.makeText(getBaseContext(), "Invalid during logging.", Toast.LENGTH_LONG).show();
          return;
        }

        // Move one floor down
        int index = mCurrentBuilding.getSelectedFloorIndex();

        if (mCurrentBuilding.checkIndex(index - 1)) {
          bypassSelectBuildingActivity(mCurrentBuilding, mCurrentBuilding.getFloors().get(index - 1));
        }
      }

    });

    scanResults = (TextView) findViewById(R.id.detectedAPs);
    mTrackingInfoView = (TextView) findViewById(R.id.trackingInfoData);

    /*
     * Create a new location client, using the enclosing class to handle
     * callbacks.
     */
    // Create the LocationRequest object
    mLocationRequest = LocationRequest.create();
    // Use high accuracy
    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    // Set the update interval to 2 seconds
    mLocationRequest.setInterval(2000);
    // Set the fastest update interval to 1 second
    mLocationRequest.setFastestInterval(1000);



    // get settings
    PreferenceManager.setDefaultValues(this, getString(R.string.preferences_file), MODE_PRIVATE,
            cy.ac.ucy.cs.anyplace.lib.R.xml.preferences_logger, true);
    preferences = getSharedPreferences(getString(R.string.preferences_file), MODE_PRIVATE);
    preferences.registerOnSharedPreferenceChangeListener(this);
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

    // WiFi manager to manage scans
    wifi = SimpleWifiManager.getInstance(getApplicationContext());
    // Create new receiver to get broadcasts
    receiverWifi = new SimpleWifiReceiver();
    wifi.registerScan(receiverWifi);
    wifi.startScan(preferences.getString("samples_interval", "1000"));

    positioning = new SensorsMain(this);
    movementDetector = new MovementDetector();
    positioning.addListener(movementDetector);
    positioning.addListener(new OrientationListener());
    movementDetector.addStepListener(new WalkingListener());

    AnyplaceLoggerReceiver mSamplingAnyplaceLoggerReceiver = new AnyplaceLoggerReceiver();
    logger = new LoggerWiFi(mSamplingAnyplaceLoggerReceiver);

    setUpMapIfNeeded();
  }
  public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
  private void checkLocationPermission() {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

      // Should we show an explanation?
      if (ActivityCompat.shouldShowRequestPermissionRationale(this,
              Manifest.permission.ACCESS_FINE_LOCATION)) {

        // Show an explanation to the user *asynchronously* -- don't block
        // this thread waiting for the user's response! After the user
        // sees the explanation, try again to request the permission.
        new AlertDialog.Builder(this)
                .setTitle("Location Permission Needed")
                .setMessage("This app needs the Location permission, please accept to use location functionality")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialogInterface, int i) {
                    //Prompt the user once explanation has been shown
                    ActivityCompat.requestPermissions(AnyplaceLoggerActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                            MY_PERMISSIONS_REQUEST_LOCATION );
                  }
                })
                .create()
                .show();


      } else {
        // No explanation needed, we can request the permission.
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                MY_PERMISSIONS_REQUEST_LOCATION );
      }
    }


    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

      // Should we show an explanation?
      if (ActivityCompat.shouldShowRequestPermissionRationale(this,
              Manifest.permission.ACCESS_COARSE_LOCATION)) {

        // Show an explanation to the user *asynchronously* -- don't block
        // this thread waiting for the user's response! After the user
        // sees the explanation, try again to request the permission.
        new AlertDialog.Builder(this)
                .setTitle("Location Permission Needed")
                .setMessage("This app needs the Location permission, please accept to use location functionality")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                  @Override
                  public void onClick(DialogInterface dialogInterface, int i) {
                    //Prompt the user once explanation has been shown
                    ActivityCompat.requestPermissions(AnyplaceLoggerActivity.this,
                            new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                            MY_PERMISSIONS_REQUEST_LOCATION );
                  }
                })
                .create()
                .show();


      } else {
        // No explanation needed, we can request the permission.
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_LOCATION );
      }
    }

  }

  /*
   * GOOGLE MAP FUNCTIONS
   */

  /**
   * Sets up the map if it is possible to do so (i.e., the Google Play
   * services APK is correctly installed) and the map has not already been
   * instantiated.. This will ensure that we only ever call  once when {@link #mMap} is not null.
   * <p>
   * If it isn't installed {@link SupportMapFragment} (and {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to install/update the Google Play services APK on
   * their device.
   * <p>
   * A user can return to this FragmentActivity after following the prompt and correctly installing/updating/enabling the Google Play services. Since the FragmentActivity may not have been
   * completely destroyed during this process (it is likely that it would only be stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this method in
   * {@link #onResume()} to guarantee that it will be called.
   */
  private void setUpMapIfNeeded() {
    // Do a null check to confirm that we have not already instantiated the
    // map.
    if (mMap != null) {
      return;
    }
    SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
            .findFragmentById(R.id.map);

    mapFragment.getMapAsync(this);


  }


  @Override
  public void onMapReady(GoogleMap googleMap) {
    mMap = googleMap;
    mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

    mClusterManager = new ClusterManager<>(this, mMap);
    initListeners();


  }

  LocationCallback mLocationCallbackInitial = new LocationCallback() {
    @Override
    public void onLocationResult(LocationResult locationResult) {
      List<Location> locationList = locationResult.getLocations();
      if (locationList.size() > 0) {
        //The last location in the list is the newest
        Location location = locationList.get(locationList.size() - 1);
        if (AnyplaceDebug.DEBUG_LOCATION){
          Log.i(TAG, "Location: " + location.getLatitude() + " " + location.getLongitude());
        }

        mLastLocation = location;


        if (gpsMarker != null) {
          // draw the location of the new position
          gpsMarker.remove();

        }
        MarkerOptions marker = new MarkerOptions();
        marker.position(new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude()));
        marker.title("User").snippet("Estimated Position");
        marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon21));

        marker.rotation(raw_heading - bearing);
        gpsMarker = mMap.addMarker(marker);

        //move map camera
        // mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(gpsMarker.getPosition(), mInitialZoomLevel));


      }

      mFusedLocationClient.removeLocationUpdates(mLocationCallbackInitial);
    }
  };


  LocationCallback mLocationCallback = new LocationCallback() {
    @Override
    public void onLocationResult(LocationResult locationResult) {
      List<Location> locationList = locationResult.getLocations();
      if (locationList.size() > 0) {
        //The last location in the list is the newest
        Location location = locationList.get(locationList.size() - 1);
        if (AnyplaceDebug.DEBUG_LOCATION){
          Log.i(TAG, "Location: " + location.getLatitude() + " " + location.getLongitude());
        }

        mLastLocation = location;


        if (gpsMarker != null) {
          // draw the location of the new position
          gpsMarker.remove();

        }
        MarkerOptions marker = new MarkerOptions();
        marker.position(new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude()));
        marker.title("User").snippet("Estimated Position");
        marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon21));

        marker.rotation(raw_heading - bearing);
        gpsMarker = mMap.addMarker(marker);

        //move map camera
        // mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));

        // mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(gpsMarker.getPosition(), mInitialZoomLevel));

        FetchNearBuildingsTask nearest = new FetchNearBuildingsTask();
        if (builds != null){
          nearest.run(builds.iterator(), gpsMarker.getPosition().latitude, gpsMarker.getPosition().longitude, 100);
          if (nearest.buildings.size() > 0) {
            bypassSelectBuildingActivity(nearest.buildings.get(0));
          }
        }
      }
    }
  };

  LocationCallback mLocationCallbackConnected = new LocationCallback() {
    @Override
    public void onLocationResult(LocationResult locationResult) {
      List<Location> locationList = locationResult.getLocations();
      if (locationList.size() > 0) {
        //The last location in the list is the newest
        Location location = locationList.get(locationList.size() - 1);
        if (AnyplaceDebug.DEBUG_LOCATION){
          Log.i(TAG, "Location: " + location.getLatitude() + " " + location.getLongitude());
        }

        mLastLocation = location;


        if (gpsMarker != null) {
          // draw the location of the new position
          gpsMarker.remove();

        }
        MarkerOptions marker = new MarkerOptions();
        marker.position(new LatLng(locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude()));
        marker.title("User").snippet("Estimated Position");
        marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon21));

        marker.rotation(raw_heading - bearing);
        gpsMarker = mMap.addMarker(marker);
        // Log.d(TAG, "Should have a marker");

        //move map camera
        // mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(gpsMarker.getPosition(), mInitialZoomLevel));


      }
        mFusedLocationClient.removeLocationUpdates(mLocationCallbackConnected);
        checkLocationPermission();
        mFusedLocationClient.requestLocationUpdates(mLocationRequest,mLocationCallback,Looper.myLooper());

    }
  };



  private void initCamera() {

    if (gpsMarker != null) {
      return;
    }

    checkLocationPermission();
    mFusedLocationClient
            .getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
            .addOnCompleteListener(new OnCompleteListener<Location>() {
              @Override
              public void onComplete(@NonNull Task<Location> task) {
                Location gps = task.getResult();
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
              }
            }).addOnFailureListener(new OnFailureListener() {
      @Override
      public void onFailure(@NonNull Exception e) {
        Toast.makeText(getApplicationContext(), "Failed to get location. Please check if location is enabled", Toast.LENGTH_SHORT).show();
        Log.d(TAG, e.getMessage());
      }
    });




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


            if (gpsMarker != null) {
              // draw the location of the new position
              gpsMarker.remove();
            }

          } else if (searchType == SearchTypes.OUTDOOR_MODE) {
            btnTrackme.setVisibility(View.VISIBLE);
            btnRecord.setVisibility(View.INVISIBLE);



            checkLocationPermission();
            mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallbackConnected, Looper.myLooper());
            handleBuildingsOnMap();

            // mMap.setMyLocationEnabled(true);

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

        if (mIsSamplingActive) {
          saveRecordingToLine(dragPosition);
        }

        curLocation = dragPosition;

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

  private boolean checkReady() {
    if (mMap == null) {
      Toast.makeText(this, "Map not ready!", Toast.LENGTH_SHORT).show();
      return false;
    }
    return true;
  }

  /******************************************************************************************************************
   * LOCATION API FUNCTIONS
   */
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
      GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), this, 0).show();
    }

  }

  @Override
  public void onConnected(Bundle arg0) {


    mLocationRequest = LocationRequest.create();
    mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    mLocationRequest.setInterval(1000); // Update location every second
    checkLocationPermission();
    mFusedLocationClient.requestLocationUpdates(mLocationRequest,mLocationCallback,Looper.myLooper());



    // No map is loaded
    if (checkPlayServices()) {
      initCamera();
      SearchTypes type = AnyPlaceSeachingHelper.getSearchType(mMap.getCameraPosition().zoom);
      if (type == SearchTypes.INDOOR_MODE) {

      } else if (type == SearchTypes.OUTDOOR_MODE) {
      }
    }
  }

  @Override
  public void onConnectionSuspended(int i) {

  }

  @Override
  public void onLocationChanged(final Location location) {

    if (location != null) {
      GeoPoint gps;
      if (AnyplaceDebug.DEBUG_WIFI) {
        gps = AnyUserData.fakeGPS();
      } else {
        gps = new GeoPoint(location.getLatitude(), location.getLongitude());
        // checkLocationPermission();
      }

      updateLocation(gps);
    }
  }

  private void updateLocation(GeoPoint gps) {
    if (gpsMarker != null) {
      // draw the location of the new position
      gpsMarker.remove();
    }
    MarkerOptions marker = new MarkerOptions();
    marker.position(new LatLng(gps.dlat, gps.dlon));
    marker.title("User").snippet("Estimated Position");
    marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.icon21));
    marker.rotation(raw_heading - bearing);
    gpsMarker = mMap.addMarker(marker);

  }


  private void handleBuildingsOnMap() {

    AnyplaceCache mAnyplaceCache = AnyplaceCache.getInstance(AnyplaceLoggerActivity.this);
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
        mClusterManager.setRenderer(new MyBuildingsRenderer(AnyplaceLoggerActivity.this, mMap, mClusterManager));
      }

      @Override
      public void onErrorOrCancel(String result) {

      }

    }, this, false);
  }


  /** Called when we want to clear the map overlays */
  private void clearMap() {
    if (!checkReady()) {
      return;
    }
    mMap.clear();
  }

  @Override
  public void onPause() {
    Log.i(TAG, "onPause");
    super.onPause();

    if (!mIsSamplingActive) {
      positioning.pause();
    }
  }

  @Override
  public void onResume() {
    Log.i(TAG, "onResume");
    super.onResume();
    setUpMapIfNeeded();

    if (!mIsSamplingActive) {
      positioning.resume();
    }
  }

  @Override
  protected void onStart() {
    super.onStart();


  }

  @Override
  protected void onStop() {
    super.onStop();

    // Disconnecting the client invalidates it.

  }

  @Override
  protected void onDestroy() {
    super.onDestroy();

    wifi.unregisterScan(receiverWifi);
  }

  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
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
          Action result = (Action) data.getSerializableExtra("action");

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

      if (mIsSamplingActive) {
        Toast.makeText(getBaseContext(), "Invalid during logging.", Toast.LENGTH_LONG).show();
        return;
      }

      // Load Building
      b.loadFloors(new FetchFloorsByBuidTaskListener() {

        @Override
        public void onSuccess(String result, List<FloorModel> floors) {

          AnyplaceCache mAnyplaceCache = AnyplaceCache.getInstance(AnyplaceLoggerActivity.this);
          ArrayList<BuildingModel> list = new ArrayList<BuildingModel>(1);
          list.add(b);
          mAnyplaceCache.setSelectedBuildingIndex(0);
          mAnyplaceCache.setSpinnerBuildings(getApplicationContext(), list);

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
      }, AnyplaceLoggerActivity.this, false, true);
    }
  }

  private void bypassSelectBuildingActivity(final BuildingModel b, final FloorModel f) {

    final FetchFloorPlanTask fetchFloorPlanTask = new FetchFloorPlanTask(getApplicationContext(), b.buid, f.floor_number);

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
        // dialog = new ProgressDialog(getApplicationContext());
        // dialog.setIndeterminate(true);
        // dialog.setTitle("Downloading floor plan");
        // dialog.setMessage("Please be patient...");
        // dialog.setCancelable(true);
        // dialog.setCanceledOnTouchOutside(false);
        // dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
        //   @Override
        //   public void onCancel(DialogInterface dialog) {
        //     fetchFloorPlanTask.cancel(true);
        //   }
        // });
        // dialog.show();

        RelativeLayout layout = findViewById(R.id.loggerView);

        progressBar = new ProgressBar(AnyplaceLoggerActivity.this, null, android.R.attr.progressBarStyleLarge);
        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(100, 100);
        params.addRule(RelativeLayout.CENTER_IN_PARENT);
        layout.addView(progressBar, params);

        progressBar.setVisibility(View.VISIBLE);




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
    TileOverlay mTileOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(new MapTileProvider(getBaseContext(), b.buid, f.floor_number)).zIndex(0));
  }

  private void selectPlaceActivityResult(final BuildingModel b, FloorModel f) {



    // set the newly selected floor
    b.setSelectedFloor(f.floor_number);
    mCurrentBuilding = b;
    mCurrentFloor = f;
    curLocation = null;
    userIsNearby = false;
    textFloor.setText(f.floor_name);

    loadMapBasicLayer(b, f);
    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(b.getPosition(), 19.0f), new CancelableCallback() {

      @Override
      public void onFinish() {
        handleBuildingsOnMap();
      }

      @Override
      public void onCancel() {
      }
    });

    class Callback implements DownloadRadioMapListener, PreviousRunningTask {
      boolean progressBarEnabled = false;
      boolean disableSuccess = false;
      static final boolean DEBUG_CALLBACK = false;




      @Override
      public void onSuccess(String result) {

        if (disableSuccess) {
          onErrorOrCancel("");
          return;
        }

        File root;
        try {
          root = AnyplaceUtils.getRadioMapFolder(AnyplaceLoggerActivity.this, mCurrentBuilding.buid, mCurrentFloor.floor_number);
          File f = new File(root, AnyplaceUtils.getRadioMapFileName(mCurrentFloor.floor_number));
          if(DEBUG_CALLBACK){
            Log.d(TAG, "inside the Callback class before heatmaptask");
          }

          new HeatmapTask().execute(f);
        } catch (Exception e) {
        }

        if (AnyplaceDebug.PLAY_STORE) {

          AnyplaceCache mAnyplaceCache = AnyplaceCache.getInstance(AnyplaceLoggerActivity.this);
          mAnyplaceCache.fetchAllFloorsRadiomapsRun(new BackgroundFetchListener() {

            @Override
            public void onSuccess(String result) {
              hideProgressBar();
              if (AnyplaceDebug.DEBUG_MESSAGES) {
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
        if (DEBUG_CALLBACK){
          Log.d(TAG, "Callback onErrorOrCancel with " + result);
        }
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
      ((PreviousRunningTask) downloadRadioMapTaskBuid.getCallbackInterface()).disableSuccess();
    }


    downloadRadioMapTaskBuid = new DownloadRadioMapTaskBuid(new Callback(), this, b.getLatitudeString(), b.getLongitudeString(), b.buid, f.floor_number, false);

    int currentapiVersion = android.os.Build.VERSION.SDK_INT;
    if (currentapiVersion >= android.os.Build.VERSION_CODES.HONEYCOMB) {
      // Execute task parallel with others and multiple instances of
      // itself
      downloadRadioMapTaskBuid.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    } else {
      downloadRadioMapTaskBuid.execute();
    }
    showHelp("Help", "<b>1.</b> Select your floor (using arrows on the right).<br><b>2.</b> Click on the map (to identify your location).");
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
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(cy.ac.ucy.cs.anyplace.lib.R.menu.logger, menu);
    return true;
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.main_menu_upload_rsslog: {
        uploadRSSLog();
        return true;
      }
      case R.id.main_menu_loadmap: {
        // start the activity where the user can select the building
        if (mIsSamplingActive) {
          Toast.makeText(this, "Invalid during logging.", Toast.LENGTH_LONG).show();
          return true;
        }

          // checkLocationPermission();
          // Location currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
          // we must set listener to the get the first location from the API
          // it will trigger the onLocationChanged below when a new location
          // is found or notify the user
          checkLocationPermission();


          mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
            @Override
            public void onComplete(@NonNull Task<Location> task) {
              final Location currentLocation = task.getResult();
              onLocationChanged(currentLocation);

              Intent placeIntent = new Intent(AnyplaceLoggerActivity.this, SelectBuildingActivity.class);
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

            }
          }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
              Toast.makeText(getBaseContext(), "No location available at the moment.", Toast.LENGTH_LONG).show();
            }
          });


        return true;
      }
      case R.id.main_menu_clear_logging: {
        if (mCurrentBuilding == null)
          Toast.makeText(getBaseContext(), "Load a map before tracking can be used!", Toast.LENGTH_SHORT).show();
        else {
          loadMapBasicLayer(mCurrentBuilding, mCurrentFloor);
          handleBuildingsOnMap();

          if (curLocation != null)
            updateMarker(curLocation);
        }
        return true;
      }

      // Launch preferences
      case R.id.logger_menu_preferences: {
        Intent i = new Intent(this, LoggerPrefs.class);
        startActivityForResult(i, PREFERENCES_ACTIVITY_RESULT);
        return true;
      }
      case R.id.main_menu_preferences: {
        Intent i = new Intent(this, SettingsActivity.class);
        // startActivityForResult(i, PREFERENCES_ACTIVITY_RESULT);
        startActivity(i);
        return true;
      }
      case R.id.main_menu_about: {
        startActivity(new Intent(AnyplaceLoggerActivity.this, AnyplaceAboutActivity.class));
        return true;
      }

      // case R.id.main_menu_exit: {
      //   this.finish();
      //   System.gc();
      // }
    }
    return false;
  }

  private void updateMarker(LatLng latlng) {
    if (this.marker != null) {
      this.marker.remove();
    }
    this.marker = this.mMap.addMarker(new MarkerOptions().position(latlng).draggable(true).icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)));
    curLocation = latlng;
  }

  // update the info view
  private void updateInfoView() {

    StringBuilder sb = new StringBuilder();
    sb.append("Lat[ ");
    if (curLocation != null)
      sb.append(curLocation.latitude);
    sb.append(" ]");
    sb.append("\nLon[ ");
    if (curLocation != null)
      sb.append(curLocation.longitude);
    sb.append(" ]");
    sb.append("\nHeading[ ");
    sb.append(String.format("%.2f", raw_heading));
    sb.append(" ]");
    sb.append("  Status[ ");
    sb.append(String.format("%8s", walking ? "Walking" : "Standing"));
    sb.append(" ]");
    sb.append("  Samples[ ");
    sb.append(mCurrentSamplesTaken);
    sb.append(" ]");
    mTrackingInfoView.setText(sb.toString());

  }



  /*
   * Gets called whenever there is a change in sensors in positioning
   *
   * @see com.lambrospetrou.anyplace.tracker.Positioning.PositioningListener#
   * onNewPosition()
   */

  private class OrientationListener implements SensorsMain.IOrientationListener {
    @Override
    public void onNewOrientation(float[] values) {
      raw_heading = values[0];
      updateInfoView();
    }
  }

  private class WalkingListener implements MovementDetector.MovementListener {

    @Override
    public void onWalking() {
      walking = true;
      updateInfoView();
    }

    @Override
    public void onStanding() {
      walking = false;
      updateInfoView();
    }

  }

  //
  // The receiver of the result after processing a WiFi ScanResult previously
  // by WiFiReceiver
  //
  public class AnyplaceLoggerReceiver implements LoggerWiFi.Callback {

    public double dist(double lat1, double lon1, double lat2, double lon2) {
      double dLat;
      double dLon;

      int R = 6371; // Km
      dLat = (lat2 - lat1) * Math.PI / 180;
      dLon = (lon2 - lon1) * Math.PI / 180;
      lat1 = lat1 * Math.PI / 180;
      lat2 = lat2 * Math.PI / 180;

      double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
      double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
      double d = R * c;

      return d;

    }

    public double dist(LatLng latlng1, LatLng latlng2) {
      double lat1 = latlng1.latitude;
      double lon1 = latlng1.longitude;
      double lat2 = latlng2.latitude;
      double lon2 = latlng2.longitude;

      return dist(lat1, lon1, lat2, lon2);
    }

    private void draw(LatLng latlng, int sum) {
      CircleOptions options = new CircleOptions();
      options.center(latlng);
      options.radius(0.5 + sum * 0.05);
      options.fillColor(Color.BLUE);
      options.strokeWidth(3);
      // Display above floor image
      options.zIndex(2);
      mMap.addCircle(options);
    }

    @Override
    public void onFinish(LoggerWiFi logger, Function function) {
      if (function == Function.ADD) {
        runOnUiThread(new Runnable() {
          public void run() {
            updateInfoView();
          }
        });
      } else if (function == Function.SAVE) {

        final boolean exceptionOccured = logger.exceptionOccured;
        final String msg = logger.msg;
        final ArrayList<ArrayList<LogRecordMap>> mSamples = logger.mSamples;

        runOnUiThread(new Runnable() {
          @Override
          public void run() {

            if (exceptionOccured) {
              Toast.makeText(AnyplaceLoggerActivity.this, msg, Toast.LENGTH_LONG).show();
              return;
            } else {
              if (!(mSamples == null || mSamples.size() == 0)) {
                ArrayList<LogRecordMap> prevSample = mSamples.get(0);
                int sum = 0;

                for (int i = 1; i < mSamples.size(); i++) {
                  ArrayList<LogRecordMap> records = mSamples.get(i);
                  // double d = dist(prevSample.get(0).lat,
                  // prevSample.get(0).lng,
                  // records.get(0).lat, records.get(0).lng);

                  if (records.get(0).walking) {
                    LatLng latlng = new LatLng(prevSample.get(0).lat, prevSample.get(0).lng);
                    draw(latlng, sum);
                    prevSample = records;
                  } else {
                    if (sum < 10)
                      sum += 1;
                  }
                }

                LatLng latlng = new LatLng(prevSample.get(0).lat, prevSample.get(0).lng);
                draw(latlng, sum);
              }

              Toast.makeText(AnyplaceLoggerActivity.this, mSamples.size() + " Samples Recorded Successfully!", Toast.LENGTH_LONG).show();
            }

            mCurrentSamplesTaken -= mSamples.size();
            if (mSamplingProgressDialog != null) {
              mSamplingProgressDialog.dismiss();
              mSamplingProgressDialog = null;
              enableRecordButton();
              showHelp("Help", "When you are done logging, click \"Menu\" -> \"Upload\"");
            }

          }
        });

      }

    }
  }

  //
  // The WifiReceiver is responsible to Receive Access Points results
  //
  private class SimpleWifiReceiver extends WifiReceiver {

    @Override
    public void onReceive(Context c, Intent intent) {


      try {
        if (intent == null || c == null || intent.getAction() == null)
          return;

        List<ScanResult> wifiList = wifi.getScanResults();
        scanResults.setText("AP : " + wifiList.size());

        // If we are not in an active sampling session we have to skip
        // this intent
        if (!mIsSamplingActive)
          return;

        if (wifiList.size() > 0) {
          mCurrentSamplesTaken++;

          logger.add(wifiList, curLocation.latitude + "," + curLocation.longitude, raw_heading, walking);
        }
      } catch (RuntimeException e) {
        Toast.makeText(c, "RuntimeException [" + e.getMessage() + "]", Toast.LENGTH_SHORT).show();
        return;
      }
    }
  }

  //
  // Method used to print pop up message to user
  //
  protected void toastPrint(String textMSG, int duration) {
    Toast.makeText(this, textMSG, duration).show();
  }

  //
  // Record button pressed. Get samples number from preferences
  //
  private void btnRecordingInfo() {

    if (mIsSamplingActive) {
      mIsSamplingActive = false;
      mSamplingProgressDialog = new ProgressDialog(AnyplaceLoggerActivity.this);
      mSamplingProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
      mSamplingProgressDialog.setMessage("Saving...");
      mSamplingProgressDialog.setCancelable(false);
      mSamplingProgressDialog.show();

      saveRecordingToLine(curLocation);

    } else {
      startRecordingInfo();
    }
  }




  private void startRecordingInfo() {

    // avoid recording when no floor has been selected
    if (mCurrentFloor == null || !mCurrentFloor.isFloorValid()) {
      Toast.makeText(getBaseContext(), "Load map before recording...", Toast.LENGTH_SHORT).show();
      return;
    }

    // avoid recording when no floor has been selected
    if (curLocation == null) {
      Toast.makeText(getBaseContext(), "Click a position before recording...", Toast.LENGTH_SHORT).show();
      return;
    }

    boolean hasGPS = getPackageManager().hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS);
    if (hasGPS) {
      if (!userIsNearby) {
        LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        boolean statusOfGPS = manager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if (statusOfGPS == false) {
          Toast.makeText(this, "Please enable GPS", Toast.LENGTH_LONG).show();
          return;
        }

        final GeoPoint gps;
        if (AnyplaceDebug.DEBUG_WIFI) {
          gps = AnyUserData.fakeGPS();

        } else {
          // checkLocationPermission();
          // Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);

          try {
            checkLocationPermission();
            mFusedLocationClient.getLastLocation().addOnCompleteListener(new OnCompleteListener<Location>() {
              @Override
              public void onComplete(@NonNull Task<Location> task) {
                try {
                  Location location = task.getResult();
                  GeoPoint gps = new GeoPoint(location.getLatitude(), location.getLongitude());
                  if (GeoPoint.getDistanceBetweenPoints(mCurrentBuilding.longitude, mCurrentBuilding.latitude, gps.dlon, gps.dlat, "") > 200) {
                    Toast.makeText(getBaseContext(), "You are only allowed to use the logger for a building you are currently at or physically nearby.", Toast.LENGTH_SHORT).show();
                    userIsNearby = false;
                  }else{
                    userIsNearby = true;
                  }


                }
                catch(Exception e){
                  Log.d(TAG, e.getMessage());
                }
              }
              });

            if (!userIsNearby){
              return;
            }
           }
           catch (Exception e){
              Log.d(TAG, e.getMessage());
            }
          }

        userIsNearby = true;


      }
    }

		folder_path = (String) preferences.getString("folder_browser", "n/a");
		if (folder_path.equals("n/a") || folder_path.equals("")) {
			toastPrint("Folder path not specified\nGo to Menu::Preferences::Storing Settings::Folder", Toast.LENGTH_LONG);
			return;

		} else if ((!(new File(folder_path).canWrite()))) {
			toastPrint("Folder path is not writable\nGo to Menu::Preferences::Storing Settings::Folder", Toast.LENGTH_LONG);
			return;
		}

		filename_rss = (String) preferences.getString("filename_log", "n/a");
		if (filename_rss.equals("n/a") || filename_rss.equals("")) {
			toastPrint("Filename of RSS log not specified\nGo to Menu::Preferences::Storing Settings::Filename", Toast.LENGTH_LONG);
			return;
		}

		disableRecordButton();
		// start the TASK
		mIsSamplingActive = true;
	}

	private void saveRecordingToLine(LatLng latlng) {

		logger.save(latlng.latitude + "," + latlng.longitude, folder_path, filename_rss, mCurrentFloor.floor_number, mCurrentBuilding.buid);

	}

	// ****************************************************************
	// Listener that handles clicks on map
	// ****************************************************************

	@Override
	public void onMapClick(LatLng latlng) {

		if (mIsSamplingActive) {
			saveRecordingToLine(latlng);
		}

		updateMarker(latlng);
		updateInfoView();

		if (!mIsSamplingActive) {
			showHelp("Help", "<b>1.</b> Please click \"START\"<br><b>2.</b> Then walk around the building in staight lines.<br><b>3.</b> Re-identify your location on the map every time you turn.");
		}

	}

	// ***************************************************************************************
	// UPLOAD RSS TASK
	// ***************************************************************************************

	private void uploadRSSLog() {
		synchronized (upInProgressLock) {
			if (!upInProgress) {

				if (!NetworkUtils.isOnline(AnyplaceLoggerActivity.this)) {
					Toast.makeText(getApplicationContext(), "No Internet Connection", Toast.LENGTH_SHORT).show();
					return;
				}

				String file_path = preferences.getString("folder_browser", "") + File.separator + preferences.getString("filename_log", "");
				startUploadTask(file_path);

			} else {
				Toast.makeText(getApplicationContext(), "Already uploading rss log...", Toast.LENGTH_SHORT).show();
			}
		}

	}

	private void startUploadTask(final String file_path) {
		upInProgress = true;

		new UploadRSSLogTask(new UploadRSSLogTask.UploadRSSLogTaskListener() {
			@Override
			public void onSuccess(String result) {
				upInProgress = false;
				File file = new File(file_path);
				file.delete();

				AlertDialog.Builder builder = new AlertDialog.Builder(AnyplaceLoggerActivity.this);
				if (mCurrentBuilding == null)
					builder.setMessage("Thank you for improving the location quality of Anyplace");
				else
					builder.setMessage("Thank you for improving the location quality for building " + mCurrentBuilding.name);

				builder.setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						// do things
					}
				});
				AlertDialog alert = builder.create();
				alert.show();

			}

			@Override
			public void onErrorOrCancel(String result) {
				upInProgress = false;
				Toast.makeText(getApplicationContext(), result, Toast.LENGTH_LONG).show();
			}
		}, this, file_path, preferences.getString("username", ""), preferences.getString("password", "")).execute();
	}

	private void showProgressBar() {
		progressBar.setVisibility(View.VISIBLE);
	}

	private void hideProgressBar() {
		progressBar.setVisibility(View.GONE);
	}

	// *****************************************************************************
	// HELPERS
	// *****************************************************************************

	private void enableRecordButton() {
		btnRecord.setText("Start WiFi Recording");
		btnRecord.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.presence_invisible, 0, 0, 0);
	}

	private void disableRecordButton() {
		btnRecord.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.presence_online, 0, 0, 0);
		btnRecord.setText("Stop WiFi Recording");
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		// TODO Auto-generated method stub

		if (key.equals("walk_bar")) {
			int sensitivity = sharedPreferences.getInt("walk_bar", 26);
			int max = Integer.parseInt(getResources().getString(R.string.walk_bar_max));
			MovementDetector.setSensitivity(max - sensitivity);
		} else if (key.equals("samples_interval")) {
			wifi.startScan(sharedPreferences.getString("samples_interval", "1000"));
		}

	}

	private void showHelp(String title, String message) {
		AlertDialog.Builder adb = new AlertDialog.Builder(this);
		LayoutInflater adbInflater = LayoutInflater.from(this);
		View eulaLayout = adbInflater.inflate(cy.ac.ucy.cs.anyplace.lib.R.layout.info_window_help, null);
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

	private class HeatmapTask extends AsyncTask<File, Integer, Collection<WeightedLatLng>> {
        private static final boolean DEBUG = false;

		public HeatmapTask() {

		}

		@Override
		protected Collection<WeightedLatLng> doInBackground(File... params) {
		  if (DEBUG) {
            Log.d(TAG, "HeatmapTask doInBackground");
            if (params[0] == null) {
              Log.d(TAG, "HeatmapTask params is null");
            } else {
              Log.d(TAG, "HeatmapTask params is not null and is : " + params[0].getAbsolutePath());
            }
          }
          Collection<WeightedLatLng>  res = MapTileProvider.readRadioMapLocations(params[0]);
		  if (DEBUG && res == null){
            Log.e(TAG, "HeatmapTask doInBackground has a null result");
          }

          return res;

		}

		@Override
		protected void onPostExecute(Collection<WeightedLatLng> result) {
			// Check if need to instantiate (avoid setData etc
			// twice)
			if (mProvider == null) {
			  if(result  == null){
			    Log.d(TAG, "No radiomap for selected building");
			    Toast.makeText(getApplicationContext(), "This building has no radiomap", Toast.LENGTH_SHORT).show();
			    return;
              }
				mProvider = new HeatmapTileProvider.Builder().weightedData(result).build();
			} else {
				mProvider.setWeightedData(result);
			}
            if (DEBUG){
              Log.d(TAG, "Setting heatmap, " + result.size());
            }


			TileOverlay mHeapOverlay = mMap.addTileOverlay(new TileOverlayOptions().tileProvider(mProvider).zIndex(1));
		}

	}

	interface PreviousRunningTask {
		void disableSuccess();
	}
}
