/*
 * Anyplace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Paschalis Mpeis, Timotheos Constambeys, Lambros Petrou
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
package cy.ac.ucy.cs.anyplace.logger.legacy

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import com.google.android.gms.maps.GoogleMap.OnMapClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.GoogleMap
import com.google.maps.android.clustering.ClusterManager
import cy.ac.ucy.cs.anyplace.lib.android.nav.BuildingModel
import cy.ac.ucy.cs.anyplace.lib.android.nav.AnyPlaceSeachingHelper.SearchTypes
import cy.ac.ucy.cs.anyplace.lib.android.sensors.wifi.SimpleWifiManager
import cy.ac.ucy.cs.anyplace.lib.android.sensors.wifi.WifiReceiver
import android.app.ProgressDialog
import android.content.SharedPreferences
import cy.ac.ucy.cs.anyplace.lib.android.sensors.SensorsMain
import cy.ac.ucy.cs.anyplace.lib.android.sensors.MovementDetector
import cy.ac.ucy.cs.anyplace.lib.android.nav.FloorModel
import com.google.maps.android.heatmaps.HeatmapTileProvider
import cy.ac.ucy.cs.anyplace.lib.android.logger.LoggerWiFi
import cy.ac.ucy.cs.anyplace.lib.android.cache.ObjectCache
import cy.ac.ucy.cs.anyplace.lib.android.tasks.FetchBuildingsTask.FetchBuildingsTaskListener
import com.google.android.gms.maps.CameraUpdateFactory
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceDebug
import android.content.pm.PackageManager
import android.content.DialogInterface
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.GoogleMap.CancelableCallback
import cy.ac.ucy.cs.anyplace.lib.android.nav.AnyPlaceSeachingHelper
import com.google.android.gms.maps.GoogleMap.OnMarkerDragListener
import android.content.IntentSender.SendIntentException
import cy.ac.ucy.cs.anyplace.lib.android.nav.AnyUserData
import cy.ac.ucy.cs.anyplace.lib.android.googlemap.MyBuildingsRenderer
import android.content.Intent
import android.app.AlertDialog
import android.content.Context
import cy.ac.ucy.cs.anyplace.lib.android.tasks.FetchFloorsByBuidTask.FetchFloorsByBuidTaskListener
import cy.ac.ucy.cs.anyplace.lib.android.googlemap.MapTileProvider
import cy.ac.ucy.cs.anyplace.lib.android.cache.BackgroundFetchListener
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.graphics.Color
import android.location.*
import cy.ac.ucy.cs.anyplace.lib.android.sensors.SensorsMain.IOrientationListener
import cy.ac.ucy.cs.anyplace.lib.android.sensors.MovementDetector.MovementListener
import android.os.*
import android.preference.PreferenceManager  // UPDATE
import cy.ac.ucy.cs.anyplace.lib.android.LOG
import cy.ac.ucy.cs.anyplace.lib.android.tasks.UploadRSSLogTask.UploadRSSLogTaskListener
import android.text.Html
import android.util.Log
import android.view.*
import android.widget.*
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*
import cy.ac.ucy.cs.anyplace.lib.android.DBG
import cy.ac.ucy.cs.anyplace.lib.android.consts.MSG
import cy.ac.ucy.cs.anyplace.lib.android.sensors.thermal.FlirUtils
import cy.ac.ucy.cs.anyplace.lib.android.tasks.*
import cy.ac.ucy.cs.anyplace.lib.android.utils.*
import cy.ac.ucy.cs.anyplace.lib.android.utils.FileUtils
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.OLDNetworkUtils
import cy.ac.ucy.cs.anyplace.logger.LoggerApp
import cy.ac.ucy.cs.anyplace.logger.R
import cy.ac.ucy.cs.anyplace.logger.databinding.ActivityLoggerOldBinding
import java.io.File
import java.lang.Exception
import java.lang.RuntimeException
import java.lang.StringBuilder
import java.util.*


import kotlinx.coroutines.launch

// TODO: REMOVE logic from here! just the lifecycle management. How?
// CHECK: LoggerActivityBase class (to put in lib) search:activities in android libraries?
class LoggerActivityOLD : AppCompatActivity(), OnSharedPreferenceChangeListener,
        // UPDATE: https://android-developers.googleblog.com/2017/11/moving-past-googleapiclient_21.html
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        // LocationListener,
        OnMapClickListener, OnMapReadyCallback {

  private val TAG = "ap-logger"
  private lateinit var app: LoggerApp
  private lateinit var VB: ActivityLoggerOldBinding

  private val LOCATION_CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000
  private val PLAY_SERVICES_RESOLUTION_REQUEST = 9001
  private val PREFS_LOGGER_RESULT = 1114
  private val SELECT_PLACE_ACTIVITY_RESULT = 1112
  val mInitialZoomLevel = 18.0f

  // Google API
  // private val mLocationListener: LocationListener = this
  // TODO:PM Make most nullable vars lateinit
  // Location API
  // private LocationClient mLocationClient; // CLR ?
  // Define an object that holds accuracy and frequency parameters
  // private var mLocationRequest: LocationRequest? = null
  private lateinit var mLocationRequest: LocationRequest
  private lateinit var gmap: GoogleMap  // TODO: maps need a refresh..
  private var marker: Marker? =null
  // XXX curLocation and mLastLocation?!?
  private var curLocation: LatLng? = null
  // private var mLastLocation: Location? = null  // XXX should NOT use this

  private lateinit var fusedLocationClient: FusedLocationProviderClient
  // private lateinit var locationRequest: LocationRequest
  private lateinit var locationCallback: LocationCallback
  // private lateinit var mFusedLocationClient: FusedLocationProviderClient // CLR
  // private var mFusedLocationClient: FusedLocationProviderClient? = null // CLR

  // Load Building and Marker
  private lateinit var clusterManager: ClusterManager<BuildingModel>
  private lateinit var downloadRadioMapTaskBuid: DownloadRadioMapTaskBuid
  private var searchType: SearchTypes = SearchTypes.UNKNOWN
  private var gpsMarker: Marker? = null
  private var bearing = 0f


  // Load Building and Marker
  private lateinit var wifiManager: SimpleWifiManager
  private lateinit var wifiReceiver: WifiReceiver
  private var mSamplingProgressDialog: ProgressDialog? = null

  private var folder_path: String? = null   // Path to store rss file

  // Filename to store rss records
  private var filename_rss: String? = null

  private lateinit var preferences: SharedPreferences

  // Positioning
  private lateinit var positioning: SensorsMain
  private lateinit var movementDetector: MovementDetector
  private var raw_heading = 0.0f
  private var walking = false
  private var upInProgress = false
  private val upInProgressLock = Any()  // CHECK:XXX PM Kotlin Any?
  private var userIsNearby = false
  private var buildingCurrent: BuildingModel? = null
  private var mCurrentFloor: FloorModel? = null
  private var mProvider: HeatmapTileProvider? = null

  // Logger Service
  private var mCurrentSamplesTaken = 0
  private var mIsSamplingActive = false
  private var logger: LoggerWiFi? = null
  // private lateinit var app: AnyplaceApp
  private var builds: List<BuildingModel>? = null

  val TODO__= false // XXX

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    LOG.D(TAG, "onCreate")
    app = application as LoggerApp

    // ViewBinding
    VB = ActivityLoggerOldBinding.inflate(layoutInflater)
    setContentView(VB.root)

    // init map
    val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
    mapFragment!!.getMapAsync(this) // onMapReady

    PermUtils.checkLoggerPermissionsAndSettings(this) // TODO proper permissions..

    lifecycleScope.launch {
      FlirUtils.initialize(app)
    }

    VB.btnRecord.setOnClickListener { btnRecordingInfo() }

    VB.btnFloorUp.setOnClickListener(View.OnClickListener {
      if (buildingCurrent == null) {
        Toast.makeText(baseContext, "Open a building first!", Toast.LENGTH_SHORT).show()
        return@OnClickListener
      }
      if (mIsSamplingActive) {
        Toast.makeText(baseContext, "Invalid during logging.", Toast.LENGTH_LONG).show()
        return@OnClickListener
      }

      // Move one floor up
      val index = buildingCurrent!!.selectedFloorIndex
      if (buildingCurrent!!.checkIndex(index + 1)) {
        bypassSelectBuildingActivity(buildingCurrent!!, buildingCurrent!!.loadedFloors[index + 1])
      }
    })

    VB.btnFloorDown.setOnClickListener(View.OnClickListener {
      if (buildingCurrent == null) {
        Toast.makeText(baseContext, "Load a map before tracking can be used!", Toast.LENGTH_SHORT).show()
        return@OnClickListener
      }
      if (mIsSamplingActive) {
        Toast.makeText(baseContext, "Invalid during logging.", Toast.LENGTH_LONG).show()
        return@OnClickListener
      }

      // Move one floor down
      val index = buildingCurrent!!.selectedFloorIndex
      if (buildingCurrent!!.checkIndex(index - 1)) {
        bypassSelectBuildingActivity(buildingCurrent!!, buildingCurrent!!.loadedFloors[index - 1])
      }
    })

    // get settings
    // TODO:PM preferences specific to logger-wifi, to logger-cv, and generic to anyplace
    PreferenceManager.setDefaultValues(this, getString(R.string.preferences_file), MODE_PRIVATE,
            cy.ac.ucy.cs.anyplace.lib.R.xml.preferences_logger, true)
    preferences = getSharedPreferences(getString(R.string.preferences_file), MODE_PRIVATE)
    preferences.registerOnSharedPreferenceChangeListener(this)
    onSharedPreferenceChanged(preferences, "walk_bar")
    val folderBrowser = preferences.getString("folder_browser", null)
    if (folderBrowser == null) {
      val f = File(Environment.getExternalStorageDirectory().toString() +
              File.separator + resources.getString(R.string.app_name))
      f.mkdirs()
      if (f.mkdirs() || f.isDirectory) {
        val path = f.absolutePath
        val editor = preferences.edit()
        editor.putString("folder_browser", path)
        editor.apply() // CLR:PM this was synchronous -> editor.commit()
      }
    } else {
      val f = File(folderBrowser)
      f.mkdirs()
    }

    // WiFi manager to manage scans
    wifiManager = SimpleWifiManager.getInstance(applicationContext)
    // Create new receiver to get broadcasts
    wifiReceiver = SimpleWifiReceiver()
    wifiManager.registerScan(wifiReceiver)
    wifiManager.startScan(preferences.getString("samples_interval", "1000"))
    positioning = SensorsMain(this)
    movementDetector = MovementDetector()
    positioning.addListener(movementDetector)
    positioning.addListener(OrientationListener())
    movementDetector.addStepListener(WalkingListener())
    val mSamplingAnyplaceLoggerReceiver = AnyplaceLoggerReceiver()
    logger = LoggerWiFi(mSamplingAnyplaceLoggerReceiver)
    // setUpMapIfNeeded()
  }


  public override fun onPause() {
    LOG.D3(TAG, "onPause")
    super.onPause()

    stopLocationUpdates()

    if (!mIsSamplingActive) {
      positioning.pause()
    }
  }


  public override fun onResume() {
    LOG.D3(TAG, "onResume")
    super.onResume()
    // setUpMapIfNeeded()
    if (!mIsSamplingActive) {
      positioning.resume()
    }
  }

  override fun onDestroy() {
    super.onDestroy()
    if(TODO__) {
      wifiManager.unregisterScan(wifiReceiver)
    }
  }

  // TODO make a preferences class
  override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
    if (key == "walk_bar") {
      val sensitivity = sharedPreferences.getInt("walk_bar", 26)
      val max = resources.getString(R.string.walk_bar_max).toInt()
      MovementDetector.setSensitivity((max - sensitivity).toFloat())
    } else if (key == "samples_interval") {
      wifiManager.startScan(sharedPreferences.getString("samples_interval", "1000"))
    }
  }

  ///////////////////////
  ///////////////////////
  ///////////////////////
  ///////////////////////
  ///////////////////////



  @SuppressLint("MissingPermission")
  private fun getLastKnownLocation() {
    LOG.D(TAG, "getLastKnownLocation")
    fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
              if (location != null) {
                // mLastLocation = location
                updateLocation(location)
              } else {
                LOG.D2("Location: LastKnown: <null>")
              }
            }
  }

  private fun getLocationUpdates() {
    LOG.I("getLocationUpdates()")
    // TODO use static values for this!!!!
    fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    // mLocationRequest = LocationRequest.create()
    // mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    // TODO LOCATION REQUEST
    mLocationRequest = LocationRequest()
    // locationRequest.interval = 50000
    // locationRequest.fastestInterval = 50000
    mLocationRequest.interval = 2000
    mLocationRequest.fastestInterval = 1000  // fasted update interval
    mLocationRequest.smallestDisplacement = 170f //170 m = 0.1 mile
    mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY //according to your app

    locationCallback = object : LocationCallback() {
      override fun onLocationResult(locationResult: LocationResult?) {
        LOG.D2("onLocationResult()")
        if(locationResult==null) {
          LOG.D2("Location: Update: <null>")
          return
        }
        if (locationResult.locations.isNotEmpty()) {
          val location = locationResult.lastLocation
          // mLastLocation = location // XXX either set this here OK... make it asynchronous..

          val prettyLoc = LocationUtils.prettyLocation(location, applicationContext)
          LOG.D2("Location: Update: $prettyLoc")
          updateLocation(location)
        } else {
          LOG.D2("Location: Update: <empty>")
        }
      }
    }
  }

  // Start location updates
  @SuppressLint("MissingPermission")
  private fun startLocationUpdates() {
    LOG.I("startLocationUpdates()")
    fusedLocationClient.requestLocationUpdates(
            mLocationRequest,
            locationCallback,
            Looper.getMainLooper())
  }

  // Stop location updates
  private fun stopLocationUpdates() {
    LOG.I("stopLocationUpdates()")
    // mLocationCallbackConnected XXX remove also this?
    fusedLocationClient.removeLocationUpdates(mLocationCallback)
  }


  /*
   * GOOGLE MAP FUNCTIONS
   */
  /**
   * Sets up the map if it is possible to do so (i.e., the Google Play
   * services APK is correctly installed) and the map has not already been
   * instantiated.. This will ensure that we only ever call  once when [.mMap] is not null.
   *
   *
   * If it isn't installed [SupportMapFragment] (and [MapView][com.google.android.gms.maps.MapView]) will show a prompt for the user to install/update the Google Play services APK on
   * their device.
   *
   *
   * A user can return to this FragmentActivity after following the prompt and correctly installing/updating/enabling the Google Play services. Since the FragmentActivity may not have been
   * completely destroyed during this process (it is likely that it would only be stopped or paused), [.onCreate] may not be called again so we should call this method in
   * [.onResume] to guarantee that it will be called.
   */
  private fun setUpMapIfNeeded() {
    // Do a null check to confirm that we have not already instantiated the
    // map.
    // if (gmap != null) {
    //   return
    // }

  }

  override fun onMapReady(googleMap: GoogleMap) {
    LOG.D2(TAG, "onMapReady")
    gmap = googleMap
    gmap.mapType = GoogleMap.MAP_TYPE_NORMAL

    getLocationUpdates()
    getLastKnownLocation()
    startLocationUpdates()

    clusterManager = ClusterManager<BuildingModel>(this, gmap)
    initListeners()

    // setup the trackme button overlaid in the map
    VB.btnTrackme.setOnClickListener {
      LOG.D("Track me clicked!")
      if (gpsMarker != null) {  // XXX FIX-FLOW
        LOG.D("gpsMarker: not null")
        val mAnyplaceCache = ObjectCache.getInstance(app)
        mAnyplaceCache.loadWorldBuildings(this, object : FetchBuildingsTaskListener {
          override fun onSuccess(result: String, buildings: List<BuildingModel>) {
            LOG.E(TAG, "loadWorldBuildings: onSuccess")
            LOG.E(TAG, "onSuccess: finding nearest buildings")
            val nearest = FetchNearBuildingsTask()
            nearest.run(buildings.iterator(),
                    gpsMarker!!.position.latitude, gpsMarker!!.position.longitude,
                    100)

            LOG.E(TAG, "nearby buildigns: ${nearest.buildings.size}")
            if (nearest.buildings.size > 0) {
              LOG.E(TAG, "chosen: ${nearest.buildings[0].name}")
              bypassSelectBuildingActivity(nearest.buildings[0])
            } else {
              // LOG.E(TAG, "animating camera: no nearest building found!")
              gmap.animateCamera(CameraUpdateFactory.newLatLngZoom(gpsMarker!!.position, mInitialZoomLevel))
            }
          }

          override fun onErrorOrCancel(result: String) {
            Toast.makeText(baseContext, "Error localizing", Toast.LENGTH_SHORT).show()
          }
        }, false)
      } else {
        LOG.W(TAG, "GPS Marker is null! skip loading buildings")
      }
    }

  }

  // CHECK?!?
  // CHECK: OLD APP: who calls this?
  var mLocationCallbackInitial: LocationCallback = object : LocationCallback() {
    override fun onLocationResult(locationResult: LocationResult) {
      val locationList = locationResult.locations
      if (locationList.size > 0) {
        //The last location in the list is the newest
        val location = locationList[locationList.size - 1]
        if (AnyplaceDebug.DEBUG_LOCATION) {
          Log.i(TAG, "Location: " + location.latitude + " " + location.longitude)
        }
        // mLastLocation = location
        if (gpsMarker != null) {
          // draw the location of the new position
          gpsMarker!!.remove()
        }
        val marker = MarkerOptions()
        marker.position(LatLng(locationResult.lastLocation.latitude, locationResult.lastLocation.longitude))
        marker.title("User").snippet("Estimated Position")
        marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_icon))
        marker.rotation(raw_heading - bearing)
        gpsMarker = gmap.addMarker(marker)

        //move map camera
        // mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
        gmap.animateCamera(CameraUpdateFactory.newLatLngZoom(gpsMarker!!.getPosition(), mInitialZoomLevel))
      }
      fusedLocationClient.removeLocationUpdates(this)
    }
  }

  // CHECK MERGE?
  var mLocationCallback: LocationCallback = object : LocationCallback() {
    override fun onLocationResult(locationResult: LocationResult) {
      val locationList = locationResult.locations
      if (locationList.size > 0) {
        //The last location in the list is the newest
        val location = locationList[locationList.size - 1]
        if (AnyplaceDebug.DEBUG_LOCATION) {
          Log.i(TAG, "Location: " + location.latitude + " " + location.longitude)
        }
        // mLastLocation = location
        if (gpsMarker != null) {
          // draw the location of the new position
          gpsMarker!!.remove()
        }
        val marker = MarkerOptions()
        marker.position(LatLng(locationResult.lastLocation.latitude, locationResult.lastLocation.longitude))
        marker.title("User").snippet("Estimated Position")
        marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_icon))
        marker.rotation(raw_heading - bearing)
        gpsMarker = gmap.addMarker(marker)

        // move map camera
        // mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
        // mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(gpsMarker.getPosition(), mInitialZoomLevel));
        val nearest = FetchNearBuildingsTask()
        if (builds != null) {
          nearest.run(builds!!.iterator(), gpsMarker!!.position.latitude, gpsMarker!!.position.longitude, 100)
          if (nearest.buildings.size > 0) {
            bypassSelectBuildingActivity(nearest.buildings[0])
          }
        }
      }
    }
  }

  // CHECK MERGE CLR?!
  var mLocationCallbackConnected: LocationCallback = object : LocationCallback() {
    override fun onLocationResult(locationResult: LocationResult) {
      val locationList = locationResult.locations
      if (locationList.size > 0) {
        //The last location in the list is the newest
        val location = locationList[locationList.size - 1]
        if (AnyplaceDebug.DEBUG_LOCATION) {
          Log.i(TAG, "Location: " + location.latitude + " " + location.longitude)
        }
        // mLastLocation = location
        if (gpsMarker != null) {
          // draw the location of the new position
          gpsMarker!!.remove()
        }
        val marker = MarkerOptions()
        marker.position(LatLng(locationResult.lastLocation.latitude, locationResult.lastLocation.longitude))
        marker.title("User").snippet("Estimated Position")
        marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_icon))
        marker.rotation(raw_heading - bearing)
        gpsMarker = gmap.addMarker(marker)
        // Log.d(TAG, "Should have a marker");

        //move map camera
        // mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 11));
        gmap.animateCamera(CameraUpdateFactory.newLatLngZoom(gpsMarker!!.position, mInitialZoomLevel))
      }
      // fusedLocationClient.removeLocationUpdates(this)  XXX
      // checkLocationPermission() // TODO
      // fusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
    }
  }

  private fun initCamera() {
    if (gpsMarker != null) {
      return
    }
    // checkLocationPermission() // XXX
    /* TODO XXX
    fusedLocationClient
            .getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, null)
            .addOnCompleteListener { task ->
              val gps = task.result
              mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(gps.latitude, gps.longitude), mInitialZoomLevel), object : CancelableCallback {
                override fun onFinish() {
                  handleBuildingsOnMap()
                }

                override fun onCancel() {
                  handleBuildingsOnMap()
                }
              })
            }.addOnFailureListener { e ->
              Toast.makeText(applicationContext, "Failed to get location. Please check if location is enabled", Toast.LENGTH_SHORT).show()
              Log.d(TAG, "FusedLocation: Failed: " +  e.message)
            }
     */
  }

  @SuppressLint("PotentialBehaviorOverride")
  private fun initListeners() {
    LOG.D2("loadBuildingsOnMap: initListeners")
    loadBuildingsOnMap() // load buildings anyway?

    gmap.setOnCameraIdleListener {
      val position = gmap.cameraPosition
      if (searchType == SearchTypes.UNKNOWN) {
        LOG.D2("Search type was null. initializing..")
        searchType = AnyPlaceSeachingHelper.getSearchType(position.zoom)
      }
      LOG.E("initListeners: searchType")
      if (searchType != AnyPlaceSeachingHelper.getSearchType(position.zoom)) {
        searchType = AnyPlaceSeachingHelper.getSearchType(position.zoom)
        if (searchType == SearchTypes.INDOOR_MODE) {
          LOG.E("initListeners: searchType: indoor")
          VB.btnTrackme.visibility = View.INVISIBLE
          VB.btnRecord.visibility = View.VISIBLE
          if (gpsMarker != null) {
            // draw the location of the new position
            gpsMarker!!.remove()
          }
        } else if (searchType == SearchTypes.OUTDOOR_MODE) {
          LOG.E("initListeners: searchType: outdoor")
          VB.btnTrackme.visibility = View.VISIBLE
          VB.btnRecord.visibility = View.INVISIBLE
          // checkLocationPermission() // XXX
          // fusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallbackConnected, Looper.myLooper())
          // mMap.setMyLocationEnabled(true);
        }
      }

      bearing = position.bearing
      // mClusterManager.onCameraChange(position) // XXX:PM CHECK:PM
      gmap.setOnCameraIdleListener(clusterManager)
      gmap.setOnMarkerClickListener(clusterManager)  // warn: PotentialBehaviourOverride
    }
    gmap.setOnMapClickListener(this)
    gmap.setOnMarkerDragListener(object : OnMarkerDragListener {
      override fun onMarkerDragStart(arg0: Marker) {}

      override fun onMarkerDragEnd(arg0: Marker) {
        LOG.D2("onMarkerDragEnd: ${arg0.title}")
        val dragPosition = arg0.position
        if (mIsSamplingActive) {
          saveRecordingToLine(dragPosition)
        }
        curLocation = dragPosition
      }

      override fun onMarkerDrag(arg0: Marker) {}
    })
    gmap.setOnMarkerClickListener(clusterManager)
    clusterManager.setOnClusterItemClickListener { b ->
      b?.let { bypassSelectBuildingActivity(it) }
      // Prevent Popup dialog
      true
    }
  }

  // private fun checkReady(): Boolean {
  //   if (gmap == null) {
  //     Toast.makeText(this, "Map not ready!", Toast.LENGTH_SHORT).show()
  //     return false
  //   }
  //   return true
  // }

  /******************************************************************************************************************
   * LOCATION API FUNCTIONS
   */
  private fun checkPlayServices(): Boolean  { // TODO MOVE SEPARATE FILE
    // Check that Google Play services is available
    val resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)
    // If Google Play services is available
    return if (ConnectionResult.SUCCESS == resultCode) {
      // In debug mode, log the status
      Log.d("Location Updates", "Google Play services is available.")
      // Continue
      true
    } else {
      // Google Play services was not available for some reason
      if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
        GooglePlayServicesUtil.getErrorDialog(
                resultCode,
                this,
                PLAY_SERVICES_RESOLUTION_REQUEST
        ).show()
      } else {
        Log.i("AnyplaceNavigator", "This device is not supported.")
        finish()
      }
      false
    }
  }

  override fun onConnectionFailed(connectionResult: ConnectionResult) {
    LOG.E(TAG, "GooglePlayServices: Connection failed")
    // Google Play services can resolve some errors it detects.
    // If the error has a resolution, try sending an Intent to
    // start a Google Play services activity that can resolve
    // error.
    if (connectionResult.hasResolution()) {
      try {
        // Start an Activity that tries to resolve the error
        connectionResult.startResolutionForResult(this, LOCATION_CONNECTION_FAILURE_RESOLUTION_REQUEST)
        // Thrown if Google Play services canceled the original
        // PendingIntent
      } catch (e: SendIntentException) {
        // Log the error
        e.printStackTrace()
      }
    } else {
      // If no resolution is available, display a dialog to the
      // user with the error.
      GooglePlayServicesUtil.getErrorDialog(connectionResult.errorCode, this, 0).show()
    }
  }

  override fun onConnected(arg0: Bundle?) {
    LOG.E("onConnected(): runs!!! WHEN? FROM WHERE?")
    // TODO fix this?!
    mLocationRequest = LocationRequest.create()
    mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    mLocationRequest.interval = 1000 // Update location every second
    // checkLocationPermission()
    // CHECK: all places
    // fusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())

    // No map is loaded
    if (checkPlayServices()) {  // CHECK
      initCamera()
      // CHECK:PM Is this a BUG? it is not initialized even in legacy app
      searchType = AnyPlaceSeachingHelper.getSearchType(gmap.cameraPosition.zoom)
      // CLR:PM ?
      // if (type == SearchTypes.INDOOR_MODE) {
      // } else if (type == SearchTypes.OUTDOOR_MODE) {
      // }
    }
  }

  override fun onConnectionSuspended(i: Int) {}


  // CLR: Deprecate
  // override fun onLocationChanged(location: Location) {
  //   LOG.I(TAG, "onLocationChanged")
  //   // if (location != null) { // CLR:PM condition always true
  //   val gps: GeoPoint = if (AnyplaceDebug.DEBUG_WIFI) { AnyUserData.fakeGPS()}
  //   else {
  //     LOG.D("onLocationChanged: using actual gps location")
  //     GeoPoint(location.latitude, location.longitude)
  //     // checkLocationPermission();
  //   }
  //   updateLocation(gps)
  //   // } // CLR:PM
  // }

  private fun updateLocation(loc: Location) {
    val gps: GeoPoint
    if (AnyplaceDebug.DEBUG_WIFI) {
      gps = AnyUserData.fakeGPS()
    } else {
      gps = GeoPoint(loc)
    }

    val prettyLoc = LocationUtils.prettyLocation(loc, this)
    LOG.D2("Location: LastKnown: $prettyLoc")

    if (gpsMarker != null) {
      // draw the location of the new position
      gpsMarker!!.remove()
    }
    val marker = MarkerOptions()
    marker.position(LatLng(gps.dlat, gps.dlon))
    marker.title("User").snippet("Estimated Position")
    marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.user_location))
    marker.rotation(raw_heading - bearing)
    gpsMarker = gmap.addMarker(marker)
  }

  private fun loadBuildingsOnMap() {
    LOG.D2("loadBuildingsOnMap")
    val mAnyplaceCache = ObjectCache.getInstance(app)
    mAnyplaceCache.loadWorldBuildings(this@LoggerActivityOLD, object : FetchBuildingsTaskListener {
      override fun onSuccess(result: String, buildings: List<BuildingModel>) {
        val collection: MutableList<BuildingModel> = ArrayList(buildings)
        LOG.D2("loadBuildingsOnMap: clearing cluster/gmap")
        mapClear()
        if (buildingCurrent != null) collection.remove(buildingCurrent!!)
        clusterManager.addItems(collection)
        clusterManager.cluster()
        // HACK. This dumps all the cached icons & recreates everything.
        clusterManager.renderer = MyBuildingsRenderer(this@LoggerActivityOLD, gmap, clusterManager)
      }

      override fun onErrorOrCancel(result: String) {}
    },  false)
  }

  /** Called when we want to clear the map overlays  */
  private fun mapClear() {
    LOG.D2("mapClear")
    // if (!checkReady()) { return }
    clusterManager.clearItems()
    gmap.clear()
  }


  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
    LOG.D(TAG, "onRequestPermissionsResult:")
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)

    val status =  if (grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED) "granted" else "denied"
    var permType = "unknown"

    when (requestCode) {
      PermUtils.REQ_LOC_FINE_GRAIN -> permType = "location/fine"
      PermUtils.REQ_LOC_BG-> permType = "location/bg"
    }

    val msg = "Permission $status ($permType)"
    Toast.makeText(this@LoggerActivityOLD, msg, Toast.LENGTH_SHORT).show();
  }


  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    LOG.I(TAG, "onActivityResult")
    super.onActivityResult(requestCode, resultCode, data)
    when (requestCode) {
      SELECT_PLACE_ACTIVITY_RESULT -> if (resultCode == RESULT_OK) {
        if (data == null) {
          return
        }
        val fpf = data.getStringExtra("floor_plan_path")
        if (fpf == null) {
          Toast.makeText(baseContext, MSG.WARN_SELECT_BUILDING_DOOR, Toast.LENGTH_SHORT).show()
          return
        }
        try {
          val b = ObjectCache.getInstance(app).spinnerBuildings[data.getIntExtra("bmodel", 0)]
          val f = b.loadedFloors[data.getIntExtra("fmodel", 0)]
          bypassSelectBuildingActivity(b, f)
        } catch (e: Exception) {
          Toast.makeText(baseContext, MSG.WARN_SELECT_BUILDING_DOOR, Toast.LENGTH_SHORT).show()
          LOG.E(e)
        }
      } else if (resultCode == RESULT_CANCELED) {
        // CANCELLED
        if (data == null) { return }
        val msg = data.getSerializableExtra("message") as String
        if (msg != null && msg.isNotEmpty()) {
          Toast.makeText(baseContext, msg, Toast.LENGTH_LONG).show()
        }
      }
      PREFS_LOGGER_RESULT -> if (resultCode == RESULT_OK) {
        val result = data!!.getSerializableExtra("action") as LoggerPrefs.Action
        when (result) {
          LoggerPrefs.Action.REFRESH_BUILDING -> {
            LOG.D(TAG, "Refreshing buildings..")
            if (buildingCurrent == null) {
              Toast.makeText(baseContext, "Load a map before performing this action!", Toast.LENGTH_SHORT).show()
            } else if (VB.progressBar.visibility == View.VISIBLE) {
              Toast.makeText(baseContext, "Building Loading in progress. Please Wait!", Toast.LENGTH_SHORT).show()
            } else {
              try {
                // clear_floorplans
                // val floorsRoot = File(AnyplaceUtils!.getFloorPlansRootFolder(this), buildingCurrent!!.buid)
                val floorsRoot = File(app.fileCache.floorplansFolder(), buildingCurrent!!.buid)
                // clear radiomaps
                // val radiomapsRoot = AnyplaceUtils!.getRadioMapsRootFolder(this)
                val radiomapsRoot = app.fileCache.radiomapsFolder();
                val radiomaps = radiomapsRoot.list { dir, filename -> if (filename.startsWith(buildingCurrent!!.buid)) true else false }
                var i = 0
                while (i < radiomaps!!.size) {
                  radiomaps[i] = radiomapsRoot.absolutePath + File.separator + radiomaps[i]
                  i++
                }

                val task = DeleteFolderBackgroundTask(
                        // kotlin SAM
                        {bypassSelectBuildingActivity(buildingCurrent!!, buildingCurrent!!.selectedFloor)},
                        this, true)
                task.setFiles(floorsRoot)
                task.setFiles(radiomaps)
                task.execute()
              } catch (e: Exception) {
                LOG.E("Refresh building Error: " + e.message)
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
              }
            }
          }
        }
        // break CHECK:PM XXX
      }
    }
  }

  private fun bypassSelectBuildingActivity(building: BuildingModel?) {
    if (building != null) {
      if (mIsSamplingActive) {
        val msg = "SamplingActive: invalid during logging."
        LOG.W("SamplingActive: invalid during logging.")
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show()
        return
      }

      // Load Building
      building.loadFloors(this, object : FetchFloorsByBuidTaskListener {
        override fun onSuccess(result: String?, floors: List<FloorModel>?) {
          LOG.E("loadFloors: onSuccess")
          val mAnyplaceCache = ObjectCache.getInstance(app)
          val list = ArrayList<BuildingModel>(1)
          list.add(building)
          mAnyplaceCache.selectedBuildingIndex = 0
          mAnyplaceCache.setSpinnerBuildings(app, list)
          var floor: FloorModel?
          if (building.getFloorFromNumber("0").also { floor = it } == null) {
            floor = building.selectedFloor
          }
          bypassSelectBuildingActivity(building, floor)
        }

        override fun onErrorOrCancel(result: String?) {
          LOG.E(TAG, "loadFloors: errorOrCancel")
          Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
        }
      }, forceReload = false, showDialog = true)
    }
  }

  private fun bypassSelectBuildingActivity(b: BuildingModel, f: FloorModel?) {
    val fetchFloorPlanTask = FetchFloorPlanTask(this, b.buid, f!!.floor_number)
    fetchFloorPlanTask.setCallbackInterface(object : FetchFloorPlanTask.Callback {
      private val dialog: ProgressDialog? = null
      override fun onSuccess(result: String?, floor_plan_file: File?) {
        LOG.D(TAG, "bypassSelectBuildingActivity: onSuccess(FetchFloorPlanTask)")
        dialog?.dismiss()
        selectPlaceActivityResult(b, f)
      }

      override fun onErrorOrCancel(result: String?) {
        dialog?.dismiss()
        Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
      }

      override fun onPrepareLongExecute() { // CLR
        LOG.E("onPrepareLongExecute: wanted to add large progress bar")
        VB.progressBar.visibility = View.VISIBLE
        // CHECK who disabled this?
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
        // val layout = findViewById<RelativeLayout>(R.id.loggerView)
        // CHECK THIS:
        // val progressBar = ProgressBar(this@LoggerActivity, null, android.R.attr.progressBarStyleLarge)
        // val params = RelativeLayout.LayoutParams(100, 100)
        // params.addRule(RelativeLayout.CENTER_IN_PARENT)
        // layout.addView(progressBar, params)
      }
    })
    fetchFloorPlanTask.execute()
  }

  private fun loadMapBasicLayer(b: BuildingModel, f: FloorModel?) {
    LOG.E(TAG, "loadMapBasicLayer")
    // remove the previous GroundOverlay or TileOverlay
    mapClear()
    // load the floorplan
    // add the Tile Provider that uses our Building tiles over
    // Google Maps
    val mTileOverlay = gmap.addTileOverlay(
            TileOverlayOptions().tileProvider(MapTileProvider(app, b.buid, f!!.floor_number)).zIndex(0f))
  }

  private fun selectPlaceActivityResult(b: BuildingModel, f: FloorModel?) {
    // set the newly selected floor
    b.setSelectedFloor(f!!.floor_number)
    buildingCurrent = b
    mCurrentFloor = f
    curLocation = null
    userIsNearby = false
    VB.textFloor.text = f.floor_name
    loadMapBasicLayer(b, f)
    gmap.animateCamera(CameraUpdateFactory.newLatLngZoom(b.position, 19.0f), object : CancelableCallback {
      override fun onFinish() {
        LOG.D2("loadBuildingsOnMap: selectPlaceActivityResult")
        loadBuildingsOnMap()
      }

      override fun onCancel() {}
    })

    class CallbackDownloadRadiomap : DownloadRadioMapTaskBuid.Callback, PreviousRunningTask {
      var progressBarEnabled = false  // XXX Progressbar here should be given AS argument
      var disableSuccess = false
      override fun onSuccess(result: String?) {
        if (disableSuccess) {
          onErrorOrCancel("")
          return
        }
        val root: File
        try {
          // root = AnyplaceUtils1.getRadioMapFolder(this@LoggerActivityOLD, buildingCurrent!!.buid, mCurrentFloor!!.floor_number)
          root = app.fileCache.radiomapsFolder(buildingCurrent!!.buid, mCurrentFloor!!.floor_number)
          LOG.D(TAG, "callback: DownloadRadioMapTaskBuid: root: $root")
          // val file = File(root, AnyplaceUtils.getRadioMapFileName(mCurrentFloor!!.floor_number))
          val file = File(root, app.fileCache.radiomapFilename(mCurrentFloor!!.floor_number))
          LOG.D(TAG, "callback: DownloadRadioMapTaskBuid: file: $root")
          // LOG.D3(TAG, "inside the Callback class before heatmaptask") // CLR?
          HeatmapTask(this@LoggerActivityOLD, mProvider!!, gmap).execute(file)
        } catch (e: Exception) {
        }
        if (AnyplaceDebug.PLAY_STORE) {
          val mAnyplaceCache = ObjectCache.getInstance(app)
          mAnyplaceCache.fetchAllFloorsRadiomapsRun(this@LoggerActivityOLD, object : BackgroundFetchListener {
            override fun onSuccess(result: String) {
              hideProgressBar()
              if (AnyplaceDebug.DEBUG_MESSAGES) {
                VB.btnTrackme.setBackgroundColor(Color.YELLOW)
              }
            }

            override fun onProgressUpdate(progress_current: Int, progress_total: Int) {
              VB.progressBar.progress = (progress_current.toFloat() / progress_total * VB.progressBar.max).toInt()
            }

            override fun onErrorOrCancel(result: String, error: BackgroundFetchListener.ErrorType) {
              // Do not hide progress bar if previous task is running
              // ErrorType.SINGLE_INSTANCE
              // Do not hide progress bar because a new task will be created
              // ErrorType.CANCELLED
              if (error == BackgroundFetchListener.ErrorType.EXCEPTION) hideProgressBar()
            }

            override fun onPrepareLongExecute() {
              showProgressBar()
            }
          }, buildingCurrent)
        }
      }

      override fun onErrorOrCancel(result: String?) {
        if (DBG.CALLBACK) {
          Log.d(TAG, "Callback onErrorOrCancel with $result")
        }
        if (progressBarEnabled) {
          hideProgressBar()
        }
      }

      override fun onPrepareLongExecute() {
        progressBarEnabled = true
        showProgressBar()
        // Set a smaller percentage than fetchAllFloorsRadiomapsOfBUID
        VB.progressBar.progress = (1.0f / (b.loadedFloors.size * 2) * VB.progressBar!!.max).toInt()
      }

      override fun disableSuccess() {
        disableSuccess = true
      }
    }
    // CHECH for CLR?
    // if (downloadRadioMapTaskBuid != null) {
    //   (downloadRadioMapTaskBuid!!.callbackInterface as PreviousRunningTask).disableSuccess()
    // }

    // b.latitudeString, b.longitudeString : INFO:PM removed those as the task did not utilize them
    downloadRadioMapTaskBuid = DownloadRadioMapTaskBuid(this@LoggerActivityOLD,
            CallbackDownloadRadiomap(),
            b.buid, f.floor_number, false)
    // TODO:PM KT coroutine
    // Execute task parallel with others and multiple instances of
    // itself
    downloadRadioMapTaskBuid.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    showHelp("Help", "<b>1.</b> Select your floor (using arrows on the right).<br>" +
            "<b>2.</b> Click on the map (to identify your location).")
  }

  override fun onConfigurationChanged(newConfig: Configuration) {
    // TODO:PM properly implement this
    // prevent orientation change when auto-rotate is enabled on Android OS
    super.onConfigurationChanged(newConfig)
    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
  }

  // MENUS
  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    val inflater = menuInflater
    inflater.inflate(cy.ac.ucy.cs.anyplace.lib.R.menu.logger, menu)
    return true
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {
    return super.onPrepareOptionsMenu(menu)
  }

  @SuppressLint("MissingPermission")
  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.main_menu_upload_rsslog -> {
        uploadRSSLog()
        return true
      }
      R.id.main_menu_loadmap -> {

        // start the activity where the user can select the building
        if (mIsSamplingActive) {
          Toast.makeText(this, "Invalid during logging.", Toast.LENGTH_LONG).show()
          return true
        }

        // checkLocationPermission();
        // Location currentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        // we must set listener to the get the first location from the API
        // it will trigger the onLocationChanged below when a new location
        // is found or notify the user
        // checkLocationPermission()
        fusedLocationClient.lastLocation.addOnCompleteListener { task ->
          val currentLocation = task.result
          // XXX WHAT IS THIS?!?! and WHY here?!?!
          LOG.E(TAG, "XXX: onLocationChanged(currentLocation)")
          // onLocationChanged(currentLocation)
          val placeIntent = Intent(this@LoggerActivityOLD, SelectBuildingActivityOLD::class.java)
          val b = Bundle()
          if (currentLocation != null) {
            b.putString("coordinates_lat", currentLocation.latitude.toString())
            b.putString("coordinates_lon", currentLocation.longitude.toString())
          }
          if (buildingCurrent == null) {
            b.putSerializable("mode", SelectBuildingActivityOLD.Mode.NEAREST)
          }
          placeIntent.putExtras(b)
          startActivityForResult(placeIntent, SELECT_PLACE_ACTIVITY_RESULT)
        }.addOnFailureListener { Toast.makeText(baseContext, "No location available at the moment.", Toast.LENGTH_LONG).show() }
        return true
      }
      R.id.main_menu_clear_logging -> {
        if (buildingCurrent == null) Toast.makeText(baseContext, "Load a map before tracking can be used!", Toast.LENGTH_SHORT).show() else {
          loadMapBasicLayer(buildingCurrent!!, mCurrentFloor)
          loadBuildingsOnMap()
          if (curLocation != null) updateMarker(curLocation!!)
        }
        return true
      }
      R.id.logger_menu_preferences -> {
        val i = Intent(this, LoggerPrefs::class.java)
        startActivityForResult(i, PREFS_LOGGER_RESULT)
        return true
      }
      R.id.main_menu_preferences -> {
        val i = Intent(this, AnyplacePrefs::class.java)
        // startActivityForResult(i, PREFERENCES_ACTIVITY_RESULT)
        startActivity(i)
        return true
      }
      R.id.main_menu_about -> {
        startActivity(Intent(this@LoggerActivityOLD, AnyplaceAboutActivity::class.java))
        return true
      }
    }
    return false
  }

  private fun updateMarker(latlng: LatLng) {
    if (marker != null) {
      marker!!.remove()
    }
    LOG.E(TAG, "updateMarker")
    marker = gmap.addMarker(MarkerOptions().position(latlng).draggable(true).icon(
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))
    curLocation = latlng
  }

  // update the info view
  private fun updateInfoView() {
    val sb = StringBuilder()
    sb.append("Lat[ ")
    if (curLocation != null) sb.append(curLocation!!.latitude)
    sb.append(" ]")
    sb.append("\nLon[ ")
    if (curLocation != null) sb.append(curLocation!!.longitude)
    sb.append(" ]")
    sb.append("\nHeading[ ")
    sb.append(String.format("%.2f", raw_heading))
    sb.append(" ]")
    sb.append("  Status[ ")
    sb.append(String.format("%8s", if (walking) "Walking" else "Standing"))
    sb.append(" ]")
    sb.append("  Samples[ ")
    sb.append(mCurrentSamplesTaken)
    sb.append(" ]")
    VB.trackingInfoData.text = sb.toString()
  }

  /*
   * Gets called whenever there is a change in sensors in positioning
   *
   * @see com.lambrospetrou.anyplace.tracker.Positioning.PositioningListener#
   * onNewPosition()
   */
  private inner class OrientationListener : IOrientationListener {
    override fun onNewOrientation(values: FloatArray) {
      raw_heading = values[0]
      updateInfoView()
    }
  }

  private inner class WalkingListener : MovementListener {
    override fun onWalking() {
      walking = true
      updateInfoView()
    }

    override fun onStanding() {
      walking = false
      updateInfoView()
    }
  }

  //
  // The receiver of the result after processing a WiFi ScanResult previously
  // by WiFiReceiver
  //
  inner class AnyplaceLoggerReceiver : LoggerWiFi.Callback {
    fun dist(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
      var lat1 = lat1
      var lat2 = lat2
      val dLat: Double
      val dLon: Double
      val R = 6371 // Km
      dLat = (lat2 - lat1) * Math.PI / 180
      dLon = (lon2 - lon1) * Math.PI / 180
      lat1 = lat1 * Math.PI / 180
      lat2 = lat2 * Math.PI / 180
      val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2)
      val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
      return R * c
    }

    fun dist(latlng1: LatLng, latlng2: LatLng): Double {
      val lat1 = latlng1.latitude
      val lon1 = latlng1.longitude
      val lat2 = latlng2.latitude
      val lon2 = latlng2.longitude
      return dist(lat1, lon1, lat2, lon2)
    }

    private fun draw(latlng: LatLng, sum: Int) {
      val options = CircleOptions()
      options.center(latlng)
      options.radius(0.5 + sum * 0.05)
      options.fillColor(Color.BLUE)
      options.strokeWidth(3f)
      // Display above floor image
      options.zIndex(2f)
      gmap.addCircle(options)
    }

    override fun onFinish(logger: LoggerWiFi, function: LoggerWiFi.Function) {
      if (function == LoggerWiFi.Function.ADD) {
        runOnUiThread { updateInfoView() }
      } else if (function == LoggerWiFi.Function.SAVE) {
        val exceptionOccured = logger.exceptionOccured
        val msg = logger.msg
        val mSamples = logger.mSamples
        runOnUiThread(Runnable {
          if (exceptionOccured) {
            Toast.makeText(this@LoggerActivityOLD, msg, Toast.LENGTH_LONG).show()
            return@Runnable
          } else {
            if (!(mSamples == null || mSamples.size == 0)) {
              var prevSample = mSamples[0]
              var sum = 0
              for (i in 1 until mSamples.size) {
                val records = mSamples[i]
                // double d = dist(prevSample.get(0).lat,
                // prevSample.get(0).lng,
                // records.get(0).lat, records.get(0).lng);
                if (records[0].walking) {
                  val latlng = LatLng(prevSample[0].lat, prevSample[0].lng)
                  draw(latlng, sum)
                  prevSample = records
                } else {
                  if (sum < 10) sum += 1
                }
              }
              val latlng = LatLng(prevSample[0].lat, prevSample[0].lng)
              draw(latlng, sum)
            }
            Toast.makeText(this@LoggerActivityOLD, mSamples!!.size.toString() + " Samples Recorded Successfully!", Toast.LENGTH_LONG).show()
          }
          mCurrentSamplesTaken -= mSamples.size
          if (mSamplingProgressDialog != null) {
            LOG.E(TAG, "will enable record btn")
            mSamplingProgressDialog!!.dismiss()
            mSamplingProgressDialog = null
            enableRecordButton()
            showHelp("Help", "When you are done logging, click \"Menu\" -> \"Upload\"")
          }
        })
      }
    }
  }

  //
  // The WifiReceiver is responsible to Receive Access Points results
  //
  private inner class SimpleWifiReceiver : WifiReceiver() {
    override fun onReceive(c: Context, intent: Intent) {
      try {
        if (intent == null || c == null || intent.action == null) return
        val wifiList = wifiManager.scanResults
        VB.scanResults.text = "AP : " + wifiList.size

        // If we are not in an active sampling session we have to skip
        // this intent
        if (!mIsSamplingActive) return
        if (wifiList.size > 0) {
          mCurrentSamplesTaken++
          logger!!.add(wifiList, curLocation!!.latitude.toString() + "," + curLocation!!.longitude, raw_heading, walking)
        }
      } catch (e: RuntimeException) {
        Toast.makeText(c, "RuntimeException [" + e.message + "]", Toast.LENGTH_SHORT).show()
        return
      }
    }
  }

  //
  // Method used to print pop up message to user
  //
  protected fun toastPrint(textMSG: String?, duration: Int) {
    Toast.makeText(this, textMSG, duration).show()
  }

  //
  // Record button pressed. Get samples number from preferences
  //
  private fun btnRecordingInfo() {
    if (mIsSamplingActive) {
      mIsSamplingActive = false
      mSamplingProgressDialog = ProgressDialog(this@LoggerActivityOLD)
      mSamplingProgressDialog!!.setProgressStyle(ProgressDialog.STYLE_SPINNER)
      mSamplingProgressDialog!!.setMessage("Saving...")
      mSamplingProgressDialog!!.setCancelable(false)
      mSamplingProgressDialog!!.show()
      saveRecordingToLine(curLocation)
    } else {
      startRecordingInfo()
    }
  }

  @SuppressLint("MissingPermission")
  private fun startRecordingInfo() {

    // avoid recording when no floor has been selected
    if (mCurrentFloor == null || !mCurrentFloor!!.isFloorValid) {
      Toast.makeText(baseContext, "Load map before recording...", Toast.LENGTH_SHORT).show()
      return
    }

    // avoid recording when no floor has been selected
    if (curLocation == null) {
      Toast.makeText(baseContext, "Click a position before recording...", Toast.LENGTH_SHORT).show()
      return
    }
    val hasGPS = packageManager.hasSystemFeature(PackageManager.FEATURE_LOCATION_GPS)
    if (hasGPS) {
      if (!userIsNearby) {
        val manager = getSystemService(LOCATION_SERVICE) as LocationManager
        val statusOfGPS = manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (statusOfGPS == false) {
          Toast.makeText(this, "Please enable GPS", Toast.LENGTH_LONG).show()
          return
        }
        val gps: GeoPoint
        if (AnyplaceDebug.DEBUG_WIFI) {
          gps = AnyUserData.fakeGPS()
        } else {
          // checkLocationPermission();
          // Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
          try {
            // checkLocationPermission()
            fusedLocationClient.lastLocation.addOnCompleteListener { task ->
              try {
                val location = task.result
                val gps = location?.let { GeoPoint(it.latitude, location.longitude) }
                if (GeoPoint.getDistanceBetweenPoints(buildingCurrent!!.lon, buildingCurrent!!.lat, gps!!.dlon, gps.dlat, "") > 200) {
                  val msg = "Logging is only allowed when your are within vicinity with the building."
                  Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                  LOG.D(msg)
                  userIsNearby = false
                } else {
                  userIsNearby = true
                }
              } catch (e: Exception) {
                e.message?.let { LOG.D(TAG, it) }
              }
            }
            if (!userIsNearby) {
              return
            }
          } catch (e: Exception) {
            e.message?.let { LOG.D(TAG, it) }
          }
        }
        userIsNearby = true
      }
    }
    // WRITE_EXTERNAL_STORAGE
    // folder_path = (String) preferences.getString("folder_browser", "n/a");
    folder_path = FileUtils.getExternalDir(this) // TODO: from APCache
    LOG.I(2, "EXT dir: $folder_path")
    if (folder_path == "n/a" || folder_path == "") {
      toastPrint("Folder path not specified\nGo to Menu::Preferences::Storing Settings::Folder", Toast.LENGTH_LONG)
      return
    } else if (!File(folder_path).canWrite()) {
      toastPrint("Folder path is not writable\nGo to Menu::Preferences::Storing Settings::Folder", Toast.LENGTH_LONG)
      return
    }
    filename_rss = preferences.getString("filename_log", "n/a")
    if (filename_rss == "n/a" || filename_rss == "") {
      toastPrint("Filename of RSS log not specified\nGo to Menu::Preferences::Storing Settings::Filename", Toast.LENGTH_LONG)
      return
    }
    LOG.E(TAG, "DISABLING RECORD..")
    disableRecordButton()
    // start the TASK
    mIsSamplingActive = true
  }

  private fun saveRecordingToLine(latlng: LatLng?) {
    logger!!.save(latlng!!.latitude.toString() + "," + latlng.longitude, folder_path, filename_rss, mCurrentFloor!!.floor_number, buildingCurrent!!.buid)
  }

  // ****************************************************************
  // Listener that handles clicks on map
  // ****************************************************************
  override fun onMapClick(latlng: LatLng) {
    if (mIsSamplingActive) {
      saveRecordingToLine(latlng)
    }
    updateMarker(latlng)
    updateInfoView()
    if (!mIsSamplingActive) {
      showHelp("Help", "<b>1.</b> Please click \"START\"<br><b>2.</b> Then walk around the building in staight lines.<br><b>3.</b> Re-identify your location on the map every time you turn.")
    }
  }

  // ***************************************************************************************
  // UPLOAD RSS TASK
  // ***************************************************************************************
  private fun uploadRSSLog() {
    synchronized(upInProgressLock) {
      if (!upInProgress) {
        if (!OLDNetworkUtils.isOnline(this@LoggerActivityOLD)) {
          Toast.makeText(applicationContext, "No Internet Connection", Toast.LENGTH_SHORT).show()
          return
        }
        val file_path = preferences!!.getString("folder_browser", "") + File.separator + preferences!!.getString("filename_log", "")
        startUploadTask(file_path)
      } else {
        Toast.makeText(applicationContext, "Already uploading rss log...", Toast.LENGTH_SHORT).show()
      }
    }
  }

  private fun startUploadTask(file_path: String) {
    upInProgress = true
    UploadRSSLogTask(object : UploadRSSLogTaskListener {
      override fun onSuccess(result: String) {
        upInProgress = false
        val file = File(file_path)
        file.delete()
        val builder = AlertDialog.Builder(this@LoggerActivityOLD)
        if (buildingCurrent == null) builder.setMessage("Thank you for improving the location quality of Anyplace")
        else builder.setMessage("Thank you for improving the location quality for building " + buildingCurrent!!.name)
        builder.setCancelable(false).setPositiveButton("OK") { dialog, id ->
          // do things
        }
        val alert = builder.create()
        alert.show()
      }

      override fun onErrorOrCancel(result: String) {
        upInProgress = false
        Toast.makeText(applicationContext, result, Toast.LENGTH_LONG).show()
      }
    }, this, file_path, preferences.getString("username", ""),
            preferences.getString("password", "")).execute()
  }

  private fun showProgressBar() {
    VB.progressBar.visibility = View.VISIBLE
  }

  private fun hideProgressBar() {
    VB.progressBar.visibility = View.GONE
  }

  // *****************************************************************************
  // HELPERS
  // *****************************************************************************
  private fun enableRecordButton() {
    LOG.E(TAG, "enableRecordButton")
    VB.btnRecord.text = "Start WiFi Recording"
    VB.btnRecord.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.presence_invisible, 0, 0, 0)
  }

  private fun disableRecordButton() {
    LOG.E(TAG, "disableRecordButton")
    VB.btnRecord.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.presence_online, 0, 0, 0)
    VB.btnRecord.text = "Stop WiFi Recording"
  }


  private fun showHelp(title: String, message: String) {
    val adb = AlertDialog.Builder(this)
    val adbInflater = LayoutInflater.from(this)
    val eulaLayout = adbInflater.inflate(cy.ac.ucy.cs.anyplace.lib.R.layout.info_window_help, null)
    val dontShowAgain = eulaLayout.findViewById<View>(R.id.skip) as CheckBox
    adb.setView(eulaLayout)
    adb.setTitle(Html.fromHtml(title))
    adb.setMessage(Html.fromHtml(message))
    adb.setPositiveButton("Ok", DialogInterface.OnClickListener { dialog, which ->
      val editor = preferences.edit()
      editor.putBoolean("skipHelpMessage", dontShowAgain.isChecked)
      editor.apply() // CLR: editor.commit()
      return@OnClickListener
    })
    val skipMessage = preferences.getBoolean("skipHelpMessage", false)
    if (!skipMessage) adb.show()
  }
}


/// TODO CHECK THIS
/*
      mGoogleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()
 */