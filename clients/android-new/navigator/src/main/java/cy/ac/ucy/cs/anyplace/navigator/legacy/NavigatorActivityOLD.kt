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
package cy.ac.ucy.cs.anyplace.navigator

import android.Manifest
import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.IntentSender.SendIntentException
import android.content.SharedPreferences
import android.content.SharedPreferences.OnSharedPreferenceChangeListener
import android.content.pm.PackageManager
import android.database.Cursor
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.preference.PreferenceManager
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.DialogFragment
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GooglePlayServicesUtil
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.CancelableCallback
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.maps.android.clustering.ClusterManager
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceDebug
import cy.ac.ucy.cs.anyplace.lib.android.cache.deprecated.BackgroundFetchListener
import cy.ac.ucy.cs.anyplace.lib.android.cache.ObjectCache
import cy.ac.ucy.cs.anyplace.lib.android.circlegate.MapWrapperLayout
import cy.ac.ucy.cs.anyplace.lib.android.circlegate.OnInfoWindowElemTouchListener
import cy.ac.ucy.cs.anyplace.lib.android.consts.MSG.WARN_NO_NETWORK
import cy.ac.ucy.cs.anyplace.lib.android.floor.Algo1Radiomap
import cy.ac.ucy.cs.anyplace.lib.android.floor.Algo1Server
import cy.ac.ucy.cs.anyplace.lib.android.floor.FloorSelector
import cy.ac.ucy.cs.anyplace.lib.android.floor.FloorSelector.*
import cy.ac.ucy.cs.anyplace.lib.android.maps.legacy.MapTileProvider
import cy.ac.ucy.cs.anyplace.lib.android.maps.legacy.MyBuildingsRenderer
import cy.ac.ucy.cs.anyplace.lib.android.maps.legacy.VisiblePois
import cy.ac.ucy.cs.anyplace.lib.android.nav.*
import cy.ac.ucy.cs.anyplace.lib.android.nav.AnyPlaceSeachingHelper.HTMLCursorAdapter
import cy.ac.ucy.cs.anyplace.lib.android.nav.AnyPlaceSeachingHelper.SearchTypes
import cy.ac.ucy.cs.anyplace.lib.android.sensors.MovementDetector
import cy.ac.ucy.cs.anyplace.lib.android.sensors.SensorsMain
import cy.ac.ucy.cs.anyplace.lib.android.sensors.SensorsStepCounter
import cy.ac.ucy.cs.anyplace.lib.android.tasks.*
import cy.ac.ucy.cs.anyplace.lib.android.tasks.AnyplaceSuggestionsTask.AnyplaceSuggestionsListener
import cy.ac.ucy.cs.anyplace.lib.android.tasks.DownloadRadioMapTaskBuid.Callback
import cy.ac.ucy.cs.anyplace.lib.android.tasks.FetchBuildingsTask.FetchBuildingsTaskListener
import cy.ac.ucy.cs.anyplace.lib.android.tasks.FetchFloorsByBuidTask.FetchFloorsByBuidTaskListener
import cy.ac.ucy.cs.anyplace.lib.android.tasks.FetchPoiByPuidTask.FetchPoiListener
import cy.ac.ucy.cs.anyplace.lib.android.tasks.NavIndoorTask.NavRouteListener
import cy.ac.ucy.cs.anyplace.lib.android.tasks.NavOutdoorTask.NavDirectionsListener
import cy.ac.ucy.cs.anyplace.lib.android.tracker.AnyplaceTracker.*
import cy.ac.ucy.cs.anyplace.lib.android.tracker.TrackerLogicPlusIMU
import cy.ac.ucy.cs.anyplace.lib.android.utils.AndroidUtils
// import cy.ac.ucy.cs.anyplace.lib.android.utils.AnyplaceUtils
import cy.ac.ucy.cs.anyplace.lib.android.utils.GeoPoint
import cy.ac.ucy.cs.anyplace.lib.android.utils.network.OLDNetworkUtils
import cy.ac.ucy.cs.anyplace.lib.android.sensors.wifi.SimpleWifiManager
import cy.ac.ucy.cs.anyplace.navigator.databinding.ActivityNavigatorOldBinding
import java.io.File
import java.util.*

//import com.flurry.android.FlurryAgent; CLR:PM
//import com.google.android.gms.common.GooglePlayServicesClient;
//import com.google.android.gms.location.LocationClient;

class NavigatorActivityOLD : AppCompatActivity(),
        TrackedLocAnyplaceTrackerListener,
        WifiResultsAnyplaceTrackerListener,
        ErrorAnyplaceTrackerListener,
        // LocationListener,
        // FloorAnyplaceFloorListener,
        // ErrorAnyplaceFloorListener,
        // OnSharedPreferenceChangeListener,
        // GoogleApiClient.ConnectionCallbacks,
        // GoogleApiClient.OnConnectionFailedListener,
        OnMapReadyCallback {
  // private val mLocationListener: LocationListener = this
  // private val raw_heading = 0.0f

  private var mLastLocation: Location? = null
  private var builds: List<BuildingModel>? = null
  private var mFusedLocationClient: FusedLocationProviderClient? = null
  private val REQUEST_PERMISSION_LOCATION = 1

  // Location API
  //private LocationClient mLocationClient;
  // Define an object that holds accuracy and frequency parameters
  private var mLocationRequest: LocationRequest? = null

  private lateinit var _b: ActivityNavigatorOldBinding
  // UI Elements TODO:PM relace with binding?
  private lateinit var progressBar: ProgressBar
  private lateinit var btnFloorUp: ImageButton
  private lateinit var btnFloorDown: ImageButton
  private lateinit var textFloor: TextView
  private lateinit var detectedAPs: TextView
  private lateinit var btnTrackme: ImageButton
  private lateinit var textDebug: TextView
  private lateinit var searchView: SearchView
  private var mSuggestionsTask: AnyplaceSuggestionsTask? = null
  private var searchType = SearchTypes.OUTDOOR_MODE

  // <Tasks>
  private var downloadRadioMapTaskBuid: DownloadRadioMapTaskBuid? = null
  private var floorChangeRequestDialog = false
  private var mAutomaticGPSBuildingSelection = false

  /**
   * Note that this may be null if the Google Play services APK is not available.
   */
  private var mMap: GoogleMap? = null
  private var cameraUpdate = false
  private var bearing = 0f

  // Navigation
  private var userData: AnyUserData? = null

  // holds the lines for the navigation route on map
  private var pathLineInside: Polyline? = null
  private var pathLineOutdoorOptions: PolylineOptions? = null
  private var pathLineOutdoor: Polyline? = null
  private var mAnyplaceCache: ObjectCache? = null

  // holds the PoisModels and Markers on map
  private var visiblePois: VisiblePois? = null
  private var mClusterManager: ClusterManager<BuildingModel>? = null

  // AnyplaceTracker
  private var sensorsMain // acceleration and orientation
          : SensorsMain? = null
  private var movementDetector // walking vs standing
          : MovementDetector? = null
  private var sensorsStepCounter // step counter
          : SensorsStepCounter? = null
  private var lpTracker: TrackerLogicPlusIMU? = null
  private var floorSelector: Algo1Radiomap? = null
  private var lastFloor: String? = null
  private var isTrackingErrorBackground = false
  private var userMarker: Marker? = null

  private lateinit var app: NavigatorApp

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    app = application as NavigatorApp

    _b = ActivityNavigatorOldBinding.inflate(layoutInflater)
    setContentView(_b.root)
    // setContentView(R.layout.activity_unifiednav)

    mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    detectedAPs = findViewById<View>(R.id.detectedAPs) as TextView
    textFloor = findViewById<View>(R.id.textFloor) as TextView
    progressBar = findViewById<View>(R.id.progressBar) as ProgressBar
    textDebug = findViewById<View>(R.id.textDebug) as TextView
    if (AnyplaceDebug.DEBUG_MESSAGES) textDebug!!.visibility = View.VISIBLE
    val actionBar = supportActionBar
    actionBar?.setHomeButtonEnabled(true)
    userData = AnyUserData()
    SimpleWifiManager.getInstance(applicationContext).startScan()
    sensorsMain = SensorsMain(applicationContext)
    movementDetector = MovementDetector()
    sensorsMain!!.addListener(movementDetector)
    sensorsStepCounter = SensorsStepCounter(applicationContext, sensorsMain)
    lpTracker = TrackerLogicPlusIMU(movementDetector, sensorsMain, sensorsStepCounter, applicationContext)
    // lpTracker = new TrackerLogic(sensorsMain);
    // floorSelector = Algo1Radiomap(applicationContext)
    // mAnyplaceCache = ObjectCache.getInstance(app)
    visiblePois = VisiblePois()
    setUpMapIfNeeded()

    // setup the trackme button overlaid in the map
    btnTrackme = findViewById<View>(R.id.btnTrackme) as ImageButton
    btnTrackme!!.setImageResource(R.drawable.dark_device_access_location_off)
    isTrackingErrorBackground = true
    btnTrackme!!.setOnClickListener {
      // final GeoPoint gpsLoc = userData.getLocationGPSorIP();
      ///----------------------
      // checkLocationPermission()
      // mFusedLocationClient.getCurrentLocation(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY, null)
      //         .addOnCompleteListener { task ->
      //           val location = task.result
      //           val gpsLoc = GeoPoint(location)
      //           val mAnyplaceCache = ObjectCache.getInstance(app)
      //           mAnyplaceCache.loadWorldBuildings(this@NavigatorActivityOLD,
      //                   object : FetchBuildingsTaskListener {
      //                     override fun onSuccess(result: String, buildings: List<BuildingModel>) {
      //                       val nearest = FetchNearBuildingsTask()
      //                       nearest.run(buildings.iterator(), gpsLoc.lat, gpsLoc.lng, 200)
      //                       if (nearest.buildings.size > 0 && (userData!!.selectedBuildingId == null || userData!!.selectedBuildingId != nearest.buildings[0].buid)) {
      //                         floorSelector!!.Stop()
      //                         val floorSelectorAlgo1: FloorSelector = Algo1Server(applicationContext)
      //                         val floorSelectorDialog = ProgressDialog(this@NavigatorActivityOLD)
      //                         floorSelectorDialog.isIndeterminate = true
      //                         floorSelectorDialog.setTitle("Detecting floor")
      //                         floorSelectorDialog.setMessage("Please be patient...")
      //                         floorSelectorDialog.setCancelable(true)
      //                         floorSelectorDialog.setCanceledOnTouchOutside(false)
      //                         floorSelectorDialog.setOnCancelListener {
      //                           floorSelectorAlgo1.Destoy()
      //                           bypassSelectBuildingActivity(nearest.buildings[0], "0", false)
      //                         }
      //                         class Callback : ErrorAnyplaceFloorListener, FloorAnyplaceFloorListener {
      //                           override fun onNewFloor(floor: String) {
      //                             floorSelectorAlgo1.Destoy()
      //                             if (floorSelectorDialog.isShowing) {
      //                               floorSelectorDialog.dismiss()
      //                               bypassSelectBuildingActivity(nearest.buildings[0], floor, false)
      //                             }
      //                           }
      //
      //                           override fun onFloorError(ex: Exception) {
      //                             floorSelectorAlgo1.Destoy()
      //                             if (floorSelectorDialog.isShowing) {
      //                               floorSelectorDialog.dismiss()
      //                               bypassSelectBuildingActivity(nearest.buildings[0], "0", false)
      //                             }
      //                           }
      //                         }
      //
      //                         val callback = Callback()
      //                         floorSelectorAlgo1.addListener(callback as FloorAnyplaceFloorListener)
      //                         floorSelectorAlgo1.addListener(callback as ErrorAnyplaceFloorListener)
      //
      //                         // Show Dialog
      //                         floorSelectorDialog.show()
      //                         floorSelectorAlgo1.Start(gpsLoc.lat, gpsLoc.lng)
      //                       } else {
      //                         Log.d(TAG, "No nearby buildings or buid missmatch")
      //                         // focusUserLocation();
      //                         checkLocationPermission()
      //                         // mFusedLocationClient.requestLocationUpdates(mLocationRequest,mLocationCallback, Looper.myLooper());
      //                         mFusedLocationClient.getLastLocation().addOnCompleteListener { task ->
      //                           val loc = task.result
      //                           addMarker(loc)
      //                           cameraUpdate = true
      //                           mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(userMarker!!.position, mInitialZoomLevel), object : CancelableCallback {
      //                             override fun onFinish() {
      //                               cameraUpdate = false
      //                             }
      //
      //                             override fun onCancel() {
      //                               cameraUpdate = false
      //                             }
      //                           })
      //                         }
      //                         // Clear cancel request
      //                         lastFloor = null
      //                         floorSelector!!.RunNow()
      //                         lpTracker!!.reset()
      //                       }
      //                     }
      //
      //                     override fun onErrorOrCancel(result: String) {}
      //                   }, false)
      //         }.addOnFailureListener {
      //           lastFloor = null
      //           floorSelector!!.RunNow()
      //           lpTracker!!.reset()
      //         }
    }
    btnFloorUp = findViewById<View>(R.id.btnFloorUp) as ImageButton
    btnFloorUp!!.setOnClickListener(View.OnClickListener {
      if (!userData!!.isFloorSelected) {
        Toast.makeText(baseContext, "Load a map before tracking can be used!", Toast.LENGTH_SHORT).show()
        return@OnClickListener
      }
      val b = userData!!.selectedBuilding ?: return@OnClickListener
      if (userData!!.isNavBuildingSelected) {
        // Move to start/destination poi's floor
        val floor_number: String
        val puids = userData!!.navPois
        // Check start and destination floor number
        if (puids[puids.size - 1].floor_number != puids[0].floor_number) {
          floor_number = if (userData!!.selectedFloorNumber == puids[puids.size - 1].floor_number) {
            puids[0].floor_number
          } else {
            puids[puids.size - 1].floor_number
          }
          val floor = b.getFloorFromNumber(floor_number)
          if (floor != null) {
            // bypassSelectBuildingActivity(b, floor)
            return@OnClickListener
          }
        }
      }

      // Move one floor up
      val index = b.selectedFloorIndex
      if (b.checkIndex(index + 1)) {
        // bypassSelectBuildingActivity(b, b.loadedFloors[index + 1])
      }
    })
    btnFloorDown = findViewById<View>(R.id.btnFloorDown) as ImageButton
    btnFloorDown!!.setOnClickListener(View.OnClickListener {
      if (!userData!!.isFloorSelected) {
        Toast.makeText(baseContext, "Load a map before tracking can be used!", Toast.LENGTH_SHORT).show()
        return@OnClickListener
      }
      val b = userData!!.selectedBuilding ?: return@OnClickListener
      if (userData!!.isNavBuildingSelected) {
        // Move to start/destination poi's floor
        val floor_number: String
        val puids = userData!!.navPois
        // Check start and destination floor number
        if (puids[puids.size - 1].floor_number != puids[0].floor_number) {
          floor_number = if (userData!!.selectedFloorNumber == puids[puids.size - 1].floor_number) {
            puids[0].floor_number
          } else {
            puids[puids.size - 1].floor_number
          }
          val floor = b.getFloorFromNumber(floor_number)
          if (floor != null) {
            // bypassSelectBuildingActivity(b, floor)
            return@OnClickListener
          }
        }
      }

      // Move one floor down
      val index = b.selectedFloorIndex
      if (b.checkIndex(index - 1)) {
        // bypassSelectBuildingActivity(b, b.loadedFloors[index - 1])
      }
    })

    /*
     * Create a new location client, using the enclosing class to handle callbacks.
     */
    // Create the LocationRequest object
    // mLocationRequest = LocationRequest.create()
    // // Use high accuracy
    // mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
    // // Set the update interval to 2 seconds
    // mLocationRequest.setInterval(2000)
    // // Set the fastest update interval to 1 second
    // mLocationRequest.setFastestInterval(1000)
    // //mLocationClient = new LocationClient(this, this, this);


    // declare that this is the first time this Activity launched so make
    // the automatic building selection
    mAutomaticGPSBuildingSelection = true

    // get/set settings
    PreferenceManager.setDefaultValues(this, SHARED_PREFS_ANYPLACE, MODE_PRIVATE, R.xml.preferences_anyplace, true)
    val preferences = getSharedPreferences(SHARED_PREFS_ANYPLACE, MODE_PRIVATE)
    // preferences.registerOnSharedPreferenceChangeListener(this)
    lpTracker!!.setAlgorithm(preferences.getString("TrackingAlgorithm", "WKNN"))

    // handle the search intent
    // handleIntent(intent)
  }

  private fun focusUserLocation() {
    if (userMarker != null) {
      if (AnyPlaceSeachingHelper.getSearchType(mMap!!.cameraPosition.zoom) == SearchTypes.OUTDOOR_MODE) {
        cameraUpdate = true
        mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(userMarker!!.position, mInitialZoomLevel), object : CancelableCallback {
          override fun onFinish() {
            cameraUpdate = false
          }

          override fun onCancel() {
            cameraUpdate = false
          }
        })
      } else {
        cameraUpdate = true
        mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(userMarker!!.position, mMap!!.cameraPosition.zoom), object : CancelableCallback {
          override fun onFinish() {
            cameraUpdate = false
          }

          override fun onCancel() {
            cameraUpdate = false
          }
        })
      }
    } else {
      if (AnyplaceDebug.DEBUG_MESSAGES) {
        Log.d(TAG, "No user marker, in focusUserLocation()")
      }
    }
  }

  override fun onStart() {
    super.onStart()
    val checkGPS = Runnable {
      val manager = getSystemService(LOCATION_SERVICE) as LocationManager
      val statusOfGPS = manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
      if (statusOfGPS == false) {
        AndroidUtils.showGPSSettings(this@NavigatorActivityOLD)
      }
    }
    val wifi = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
    val isWifiOn = wifi.isWifiEnabled
    val isOnline = OLDNetworkUtils.isOnline(this@NavigatorActivityOLD)
    if (!isOnline) {
      AndroidUtils.showWifiSettings(this, "No Internet Connection", null, checkGPS)
    } else if (!isWifiOn) {
      AndroidUtils.showWifiSettings(this, "WiFi is disabled", null, checkGPS)
    } else {
      checkGPS.run()
    }
  }

  override fun onResume() {
    super.onResume()
    setUpMapIfNeeded()
    //TODO CHECK IF MMAP IS USED AND MOVE TO ONMAPREADY
    addTrackerListeners()
    // check the Play Services
    checkPlayServices()
    sensorsMain!!.resume()
    sensorsStepCounter!!.resume()
    lpTracker!!.resumeTracking()
    floorSelector!!.resumeTracking()
  }

  override fun onPause() {
    super.onPause()
    lpTracker!!.pauseTracking()
    floorSelector!!.pauseTracking()
    sensorsMain!!.pause()
    sensorsStepCounter!!.pause()
    removeTrackerListeners()
  }

  override fun onStop() {
    super.onStop()
  }

  override fun onRestart() {
    super.onRestart()
  }

  override fun onDestroy() {
    super.onDestroy()
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    val inflater = menuInflater
    inflater.inflate(R.menu.unified_options_menu, menu)

    // ****************************************** Search View
    // ***************************************************************** /
    // Associate searchable configuration with the SearchView
    val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
    searchView = menu.findItem(R.id.search).actionView as SearchView
    searchView!!.setSearchableInfo(searchManager.getSearchableInfo(componentName))
    searchView!!.queryHint = "Search outdoor"
    searchView!!.setAddStatesFromChildren(true)

    // set query change listener
    searchView!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
      override fun onQueryTextChange(newText: String): Boolean {
        // return false; // false since we do not handle this call
        if (newText == null || newText.trim { it <= ' ' }.length < 1) {
          if (mSuggestionsTask != null && !mSuggestionsTask!!.isCancelled) {
            mSuggestionsTask!!.cancel(true)
          }
          searchView!!.suggestionsAdapter = null
          return true
        }
        if (mSuggestionsTask != null) {
          mSuggestionsTask!!.cancel(true)
        }
        if (searchType == SearchTypes.INDOOR_MODE) {
          if (!userData!!.isFloorSelected) {
            val places: MutableList<IPoisClass> = ArrayList(1)
            val pm = PoisModel()
            pm.name = "Load a building first ..."
            places.add(pm)
            val cursor = AnyPlaceSeachingHelper.prepareSearchViewCursor(places)
            showSearchResult(cursor)
            return true
          }
        }
        val gp = userData!!.latestUserPosition
        val key = getString(R.string.maps_api_key) // CHECK does it work with the secure.properties?
        // mSuggestionsTask = AnyplaceSuggestionsTask(
        //         app,
        //         object : AnyplaceSuggestionsListener {
        //           override fun onSuccess(result: String, pois: List<IPoisClass>) {
        //             showSearchResult(AnyPlaceSeachingHelper.prepareSearchViewCursor(pois, newText))
        //           }
        //
        //           override fun onErrorOrCancel(result: String) {
        //             Log.d("AnyplaceSuggestions", result)
        //           }
        //
        //           override fun onUpdateStatus(string: String, cursor: Cursor) {
        //             showSearchResult(cursor)
        //           }
        //         }, searchType, gp ?: GeoPoint(csLat, csLon), newText, key)
        mSuggestionsTask!!.execute(null, null)

        // we return true to avoid caling the provider set in the xml
        return true
      }

      override fun onQueryTextSubmit(query: String): Boolean {
        return false
      }
    })
    searchView!!.isSubmitButtonEnabled = true
    searchView!!.isQueryRefinementEnabled = false


    // ****************************************** Select building
    // ***************************************************************** /
    // Select building and floor to start navigating and positioning
    val subMenuPlace = menu.addSubMenu("Select Building")
    val sPlace = subMenuPlace.item
    sPlace.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
    sPlace.setOnMenuItemClickListener { // start the activity where the user can select the FROM and TO
      // pois he wants to navigate
      val gp = userData!!.latestUserPosition
      loadSelectBuildingActivity(gp, false)
      true
    }

    // ********************************** CLEAR NAVIGATION
    // *********************************************** /
    val subMenuResetNav = menu.addSubMenu("Clear Navigation")
    val ResetNav = subMenuResetNav.item
    ResetNav.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
    ResetNav.setOnMenuItemClickListener {
      clearNavigationData()
      true
    }

    // ****************************************** preferences
    // ********************************************** /
    val subMenuPreferences = menu.addSubMenu("Preferences")
    val prefsMenu = subMenuPreferences.item
    prefsMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
    prefsMenu.setOnMenuItemClickListener {
      val i = Intent(this@NavigatorActivityOLD, AnyplacePrefs::class.java)
      startActivityForResult(i, PREFERENCES_ACTIVITY_RESULT)
      true
    }

    // ****************************************** about
    // ********************************************** /
    val subMenuAbout = menu.addSubMenu("About")
    val about = subMenuAbout.item
    about.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
    about.setOnMenuItemClickListener {
      startActivity(Intent(this@NavigatorActivityOLD, AnyplaceAboutActivity::class.java))
      true
    }

    // ****************************************** exit
    // ********************************************** /
    val subMenuExit = menu.addSubMenu("Exit")
    val Exit = subMenuExit.item
    Exit.setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER)
    Exit.setOnMenuItemClickListener {
      finish()
      true
    }
    /***************************************** END OF MAIN MENU  */
    return super.onCreateOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      else -> {
      }
    }
    return super.onOptionsItemSelected(item)
  }

  private fun showSearchResult(cursor: Cursor) {
    // bind the text data from the results to the
    // custom
    // layout
    val from = arrayOf(SearchManager.SUGGEST_COLUMN_TEXT_1 // ,SearchManager.SUGGEST_COLUMN_TEXT_2
    )
    val to = intArrayOf(android.R.id.text1 // ,android.R.id.text2
    )
    val adapter = HTMLCursorAdapter(
            this@NavigatorActivityOLD,
            R.layout.queried_pois_item_1_searchbox, cursor, from, to)
    searchView!!.suggestionsAdapter = adapter
    adapter.notifyDataSetChanged()
  }

  // </ GOOGLE MAP FUNCTIONS
  // Called from onCreate or onResume
  private fun setUpMapIfNeeded() {
    // Do a null check to confirm that we have not already instantiated the
    // map.
    if (mMap != null) {
      return
    }
    val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
    mapFragment!!.getMapAsync(this)
  }

  // Called from setUpMapIfNeeded
  private fun setUpMap() {
    initMap()
    initCamera()
    initListeners()
  }

  // Called from setUpMap
  private fun initMap() {
    // Sets the map type to be NORMAL - ROAD mode
    mMap!!.mapType = GoogleMap.MAP_TYPE_NORMAL
    mMap!!.isBuildingsEnabled = false
  }

  private fun checkLocationPermission() {
    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) {

      // Should we show an explanation?
      if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                      Manifest.permission.ACCESS_FINE_LOCATION)) {

        // Show an explanation to the user *asynchronously* -- don't block
        // this thread waiting for the user's response! After the user
        // sees the explanation, try again to request the permission.
        AlertDialog.Builder(this)
                .setTitle("Location Permission Needed")
                .setMessage("This app needs the Location permission, please accept to use location functionality")
                .setPositiveButton("OK") { dialogInterface, i -> //Prompt the user once explanation has been shown
                  ActivityCompat.requestPermissions(this@NavigatorActivityOLD, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                          MY_PERMISSIONS_REQUEST_LOCATION)
                }
                .create()
                .show()
      } else {
        // No explanation needed, we can request the permission.
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION)
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
        AlertDialog.Builder(this)
                .setTitle("Location Permission Needed")
                .setMessage("This app needs the Location permission, please accept to use location functionality")
                .setPositiveButton("OK") { dialogInterface, i -> //Prompt the user once explanation has been shown
                  ActivityCompat.requestPermissions(this@NavigatorActivityOLD, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                          MY_PERMISSIONS_REQUEST_LOCATION)
                }
                .create()
                .show()
      } else {
        // No explanation needed, we can request the permission.
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION),
                MY_PERMISSIONS_REQUEST_LOCATION)
      }
    }
  }

  // Called from onConnecetd
  private fun initCamera() {
    // Only for the first time
    if (userMarker != null) {
      return
    }
    checkLocationPermission()
    val source = CancellationTokenSource()
    val token = source.token
    mFusedLocationClient!!.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY, token)
            .addOnCompleteListener { task ->
              val gps = task.result
              if (gps == null) {
                if (AnyplaceDebug.DEBUG_MESSAGES) {
                  Log.d(TAG, "Location returned is null")
                }
                // return;
              }
              cameraUpdate = true
              addMarker(gps)
              mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(gps!!.latitude, gps.longitude), mInitialZoomLevel), object : CancelableCallback {
                override fun onFinish() {
                  cameraUpdate = false
                  handleBuildingsOnMap(false)
                }

                override fun onCancel() {
                  cameraUpdate = false
                  handleBuildingsOnMap(false)
                }
              })
            }.addOnFailureListener { e ->
              Toast.makeText(applicationContext, "Failed to get location. Please check if location is enabled", Toast.LENGTH_SHORT).show()
              Log.d(TAG, e.message!!)
            }

    // mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
  }

  private fun addMarker(location: Location?) {
    if (userMarker != null) userMarker!!.remove()
    val marker = MarkerOptions()
    marker.position(LatLng(location!!.latitude, location.longitude))
    marker.title("User").snippet("Estimated Position")
    marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_icon))
    marker.rotation(sensorsMain!!.rawHeading - bearing)
    userMarker = mMap!!.addMarker(marker)
  }

  // Called from setUpMap
  private fun initListeners() {
    mMap!!.setOnCameraChangeListener { position ->
      // change search box message and clear pois
      if (searchType != AnyPlaceSeachingHelper.getSearchType(position.zoom)) {
        searchType = AnyPlaceSeachingHelper.getSearchType(position.zoom)
        if (searchType == SearchTypes.INDOOR_MODE) {
          searchView!!.queryHint = "Search indoor"
          visiblePois!!.showAll()
          if (pathLineInside != null) pathLineInside!!.isVisible = true
        } else if (searchType == SearchTypes.OUTDOOR_MODE) {
          searchView!!.queryHint = "Search outdoor"
          visiblePois!!.hideAll()
          if (pathLineInside != null) pathLineInside!!.isVisible = false
        }
      }
      bearing = position.bearing

      // CHECK:PM UPDATE MAPS CLUSTER API CLR if ok
      // mClusterManager.onCameraChange(position);  // This method is removed!
      mMap!!.setOnCameraIdleListener(mClusterManager)
      mMap!!.setOnMarkerClickListener(mClusterManager) // warn: PotentialBehaviourOverride

      // mMap.setOnCameraIdleListener(); // CHECK old notes?
      // mClusterManager.onc
      // something like below has to be used
      // mAlgorithm = new ScreenBasedAlgorithmAdapter<>(new PreCachingAlgorithmDecorator<>(
      //         new NonHierarchicalDistanceBasedAlgorithm<ClusterItem>()));
      // mAlgorithm.onCameraChange(position);
    }


    // TODO: UPDATE MAPS CLUSTER API:
    // TODO MAPS MIGRATION GUIDE: https://github.com/googlemaps/android-maps-utils#migration-guide
    mMap!!.setOnMarkerClickListener { marker ->
      // mClusterManager returns true if is a cluster item
      if (!mClusterManager!!.onMarkerClick(marker)) {
        val poi = visiblePois!!.getPoisModelFromMarker(marker)
        // Prevent Popup dialog
        poi == null
        // Prevent Popup dialog
      } else {
        true
      }
    }
    mClusterManager!!.setOnClusterClickListener { // Prevent Popup dialog
      true
    }
    mClusterManager!!.setOnClusterItemClickListener { b ->
      if (b != null) {
        bypassSelectBuildingActivity(b, "0", false)
      }
      // Prevent Popup dialog
      true
    }
  }

  // /> GOOGLE MAP FUNCTIONS
  // Select Building Activity based on gps location
  private fun loadSelectBuildingActivity(loc: GeoPoint?, invisibleSelection: Boolean) {
    val placeIntent = Intent(this@NavigatorActivityOLD, SelectBuildingActivity::class.java)
    val b = Bundle()
    if (loc != null) {
      b.putString("coordinates_lat", loc.dlat.toString())
      b.putString("coordinates_lon", loc.dlon.toString())
    }
    b.putSerializable("mode", if (invisibleSelection) SelectBuildingActivity.Mode.INVISIBLE else SelectBuildingActivity.Mode.NONE)
    placeIntent.putExtras(b)

    // start the activity where the user can select the building he is in
    startActivityForResult(placeIntent, SELECT_PLACE_ACTIVITY_RESULT)
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    when (requestCode) {
      LOCATION_CONNECTION_FAILURE_RESOLUTION_REQUEST -> when (resultCode) {
        RESULT_OK -> {
        }
      }
      SEARCH_POI_ACTIVITY_RESULT -> if (resultCode == RESULT_OK) {
        // search activity finished OK
        if (data == null) return
        val place = data.getSerializableExtra("ianyplace") as IPoisClass?
        // handleSearchPlaceSelection(place)
      } else if (resultCode == RESULT_CANCELED) {
        // CANCELLED
        if (data == null) return
        val msg = data.getSerializableExtra("message") as String?
        if (msg != null) Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
      }
      SELECT_PLACE_ACTIVITY_RESULT -> if (resultCode == RESULT_OK) {
        if (data == null) return
        val fpf = data.getStringExtra("floor_plan_path")
        if (fpf == null) {
          Toast.makeText(baseContext, "You haven't selected both building and floor...!", Toast.LENGTH_SHORT).show()
          return
        }
        try {
          val b = mAnyplaceCache!!.spinnerBuildings[data.getIntExtra("bmodel", 0)]
          val f = b.loadedFloors[data.getIntExtra("fmodel", 0)]
          // selectPlaceActivityResult(b, f)
        } catch (ex: Exception) {
          Toast.makeText(baseContext, "You haven't selected both building and floor...!", Toast.LENGTH_SHORT).show()
        }
      } else if (resultCode == RESULT_CANCELED) {
        // CANCELLED
        if (data == null) return
        val msg = data.getSerializableExtra("message") as String?
        if (msg != null) Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
      }
      PREFERENCES_ACTIVITY_RESULT -> if (resultCode == RESULT_OK) {
        val result = data!!.getSerializableExtra("action") as AnyplacePrefs.Action?
        // when (result) {
        //   AnyplacePrefs.Action.REFRESH_BUILDING -> {
        //     if (!userData!!.isFloorSelected) {
        //       Toast.makeText(baseContext, "Load a map before performing this action!", Toast.LENGTH_SHORT).show()
        //       break
        //     }
        //     if (progressBar!!.visibility == View.VISIBLE) {
        //       Toast.makeText(baseContext, "Building Loading in progress. Please Wait!", Toast.LENGTH_SHORT).show()
        //       break
        //     }
        //     try {
        //       val b = userData!!.selectedBuilding
        //       // clear_floorplans
        //       val floorsRoot = File(AnyplaceUtils.getFloorPlansRootFolder(this), b.buid)
        //       // clear radiomaps
        //       val radiomapsRoot = AnyplaceUtils.getRadioMapsRootFolder(this)
        //       val radiomaps = radiomapsRoot.list { dir, filename -> if (filename.startsWith(b.buid)) true else false }
        //       var i = 0
        //       while (i < radiomaps.size) {
        //         radiomaps[i] = radiomapsRoot.absolutePath + File.separator + radiomaps[i]
        //         i++
        //       }
        //       floorSelector!!.Stop()
        //       disableAnyplaceTracker()
        //       val task = DeleteFolderBackgroundTask({ // clear any markers that might have already
        //         // been added to the map
        //         visiblePois!!.clearAll()
        //         // clear and resets the cached POIS inside
        //         // AnyplaceCache
        //         mAnyplaceCache!!.setPois(app, HashMap(), "")
        //         mAnyplaceCache!!.fetchAllFloorsRadiomapReset()
        //         bypassSelectBuildingActivity(b, b.selectedFloor)
        //       }, this@NavigatorActivityOLD, true)
        //       task.setFiles(floorsRoot)
        //       task.setFiles(radiomaps)
        //       task.execute()
        //     } catch (e: Exception) {
        //       Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT).show()
        //     }
        //   }
        //   AnyplacePrefs.Action.REFRESH_MAP -> handleBuildingsOnMap(true)
        // }
      }
    }
  }

  private fun bypassSelectBuildingActivity(b: BuildingModel?, floor_number: String?, force: Boolean) {
    // Load Building
    b!!.loadFloors(this, object : FetchFloorsByBuidTaskListener {
      override fun onSuccess(result: String?, floors: List<FloorModel>?) {
        // Force loading of floor_number
        var floor: FloorModel?
        if (b.getFloorFromNumber(floor_number!!).also { floor = it } != null || !force) {
          if (floor == null) {
            floor = b.selectedFloor
          }
          val list = ArrayList<BuildingModel?>(1)
          list.add(b)
          // Set building for Select Dialog
          // mAnyplaceCache!!.selectedBuildingIndex = 0
          // mAnyplaceCache!!.setSpinnerBuildings(app, list)
          // bypassSelectBuildingActivity(b, floor)
        } else {
          Toast.makeText(baseContext, "Building's Floor Not Found", Toast.LENGTH_SHORT).show()
        }
      }

      override fun onErrorOrCancel(result: String?) {
        Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
      }
    }, false, true)
  }

  // TODO:PM MERGE:PM with
  private fun bypassSelectBuildingActivity(building: BuildingModel?, floor_number: String,
                                           force: Boolean, poi: PoisModel) {
    // Load Building
    building!!.loadFloors(this, object : FetchFloorsByBuidTaskListener {
      override fun onSuccess(result: String?, floors: List<FloorModel>?) {

        // Force loading of floor_number
        var floor: FloorModel?
        if (building.getFloorFromNumber(floor_number).also { floor = it } != null || !force) {
          if (floor == null) {
            floor = building.selectedFloor
          }
          val list = ArrayList<BuildingModel?>(1)
          list.add(building)
          // Set building for Select Dialog
          // mAnyplaceCache!!.selectedBuildingIndex = 0
          // mAnyplaceCache!!.setSpinnerBuildings(app, list)
          // bypassSelectBuildingActivity(building, floor, poi)
        } else {
          Toast.makeText(baseContext, "Building's Floor Not Found", Toast.LENGTH_SHORT).show()
        }
      }

      override fun onErrorOrCancel(result: String?) {
        Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
      }
    }, false, true)
  }

  // private fun bypassSelectBuildingActivity(b: BuildingModel?, f: FloorModel?) {
  //   val fetchFloorPlanTask = FetchFloorPlanTask(this@NavigatorActivityOLD, b!!.buid, f!!.floor_number)
  //   fetchFloorPlanTask.setCallbackInterface(object : FetchFloorPlanTask.Callback {
  //     private var dialog: ProgressDialog? = null
  //     override fun onSuccess(result: String, floor_plan_file: File) {
  //       if (dialog != null) dialog!!.dismiss()
  //       selectPlaceActivityResult(b, f)
  //     }
  //
  //     override fun onErrorOrCancel(result: String) {
  //       if (dialog != null) dialog!!.dismiss()
  //       Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
  //     }
  //
  //     override fun onPrepareLongExecute() {
  //       dialog = ProgressDialog(this@NavigatorActivityOLD)
  //       dialog!!.isIndeterminate = true
  //       dialog!!.setTitle("Downloading floor plan")
  //       dialog!!.setMessage("Please be patient...")
  //       dialog!!.setCancelable(true)
  //       dialog!!.setCanceledOnTouchOutside(false)
  //       dialog!!.setOnCancelListener { fetchFloorPlanTask.cancel(true) }
  //       dialog!!.show()
  //     }
  //   })
  //   fetchFloorPlanTask.execute()
  // }
  //
  // private fun bypassSelectBuildingActivity(b: BuildingModel?, f: FloorModel?, pm: PoisModel) {
  //   val fetchFloorPlanTask = FetchFloorPlanTask(this@NavigatorActivityOLD, b!!.buid, f!!.floor_number)
  //   fetchFloorPlanTask.setCallbackInterface(object : FetchFloorPlanTask.Callback {
  //     private var dialog: ProgressDialog? = null
  //     override fun onSuccess(result: String, floor_plan_file: File) {
  //       if (dialog != null) dialog!!.dismiss()
  //       selectPlaceActivityResult(b, f, pm)
  //     }
  //
  //     override fun onErrorOrCancel(result: String) {
  //       if (dialog != null) dialog!!.dismiss()
  //       Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
  //     }
  //
  //     override fun onPrepareLongExecute() {
  //       dialog = ProgressDialog(this@NavigatorActivityOLD)
  //       dialog!!.isIndeterminate = true
  //       dialog!!.setTitle("Downloading floor plan")
  //       dialog!!.setMessage("Please be patient...")
  //       dialog!!.setCancelable(true)
  //       dialog!!.setCanceledOnTouchOutside(false)
  //       dialog!!.setOnCancelListener { fetchFloorPlanTask.cancel(true) }
  //       dialog!!.show()
  //     }
  //   })
  //   fetchFloorPlanTask.execute()
  // }
  //
  // private fun selectPlaceActivityResult(b: BuildingModel?, f: FloorModel?, pm: PoisModel) {
  //   selectPlaceActivityResult_HELP(b, f)
  //   fetchPoisByBuidToCache(b!!.buid, object : FetchPoisByBuidTask.Callback {
  //     fun onSuccess(result: String?, poisMap: MutableMap<String?, PoisModel>) {
  //       // This should never return null
  //       if (poisMap[pm.puid] == null) {
  //         poisMap[pm.puid] = pm
  //       }
  //       handlePoisOnMap(poisMap.values as Collection<PoisModel>)
  //       startNavigationTask(pm.puid)
  //       selectPlaceActivityResult_HELP2(b, f)
  //     }
  //
  //     override fun onErrorOrCancel(result: String) {
  //       val l = mAnyplaceCache!!.pois
  //       l.add(pm)
  //       handlePoisOnMap(l)
  //       startNavigationTask(pm.puid)
  //       selectPlaceActivityResult_HELP2(b, f)
  //     }
  //   })
  // }
  //
  // private fun selectPlaceActivityResult(b: BuildingModel?, f: FloorModel?) {
  //   selectPlaceActivityResult_HELP(b, f)
  //   fetchPoisByBuidToCache(b!!.buid, object : FetchPoisByBuidTask.Callback {
  //     fun onSuccess(result: String?, poisMap: Map<String?, PoisModel>) {
  //       handlePoisOnMap(poisMap.values)
  //       loadIndoorOutdoorPath()
  //       selectPlaceActivityResult_HELP2(b, f)
  //     }
  //
  //     override fun onErrorOrCancel(result: String) {
  //       loadIndoorOutdoorPath()
  //       selectPlaceActivityResult_HELP2(b, f)
  //     }
  //   })
  // }
  //
  // // Help tasks
  // private fun selectPlaceActivityResult_HELP(b: BuildingModel?, f: FloorModel?) {
  //   mAutomaticGPSBuildingSelection = false
  //   floorSelector!!.Stop()
  //   disableAnyplaceTracker()
  //
  //   // set the newly selected floor
  //   b!!.setSelectedFloor(f!!.floor_number)
  //   userData!!.selectedBuilding = b
  //   userData!!.setSelectedFloor(f)
  //   textFloor!!.text = f.floor_name
  //   mMap!!.clear() // clean the map in case there are overlays
  //
  //   // add the Tile Provider that uses our Building tiles over Google Maps
  //   val mTileOverlay = mMap!!.addTileOverlay(TileOverlayOptions().tileProvider(MapTileProvider(baseContext, b.buid, f.floor_number)))
  //   mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(b.position, 19.0f), object : CancelableCallback {
  //     override fun onFinish() {
  //       cameraUpdate = false
  //       handleBuildingsOnMap(false)
  //       updateLocation()
  //     }
  //
  //     override fun onCancel() {
  //       cameraUpdate = false
  //     }
  //   })
  //
  //   // we must now change the radio map file since we changed floor RADIO MAP initialization
  //   try {
  //     val root = AnyplaceUtils.getRadioMapFolder(this, b.buid, userData!!.selectedFloorNumber)
  //     lpTracker!!.setRadiomapFile(File(root, AnyplaceUtils.getRadioMapFileName(userData!!.selectedFloorNumber)).absolutePath)
  //   } catch (e: Exception) {
  //     // exception thrown by GetRootFolder when sdcard is not writable
  //     Toast.makeText(baseContext, e.message, Toast.LENGTH_SHORT).show()
  //   }
  // }
  //
  // // Download RADIOMAP
  // private fun selectPlaceActivityResult_HELP2(b: BuildingModel?, f: FloorModel?) {
  //   val trackedPositionLat = userData!!.selectedBuilding.latitudeString
  //   val trackedPositionLon = userData!!.selectedBuilding.longitudeString
  //
  //   // first we should disable the tracker if it's working
  //   disableAnyplaceTracker()
  //   class Callback : DownloadRadioMapTaskBuid.Callback, PreviousRunningTask {
  //     var progressBarEnabled = false
  //     var disableSuccess = false
  //     override fun onSuccess(result: String) {
  //       if (disableSuccess) {
  //         onErrorOrCancel("")
  //         return
  //       }
  //       // start the tracker
  //       // enableAnyplaceTracker(); CHECK ?!?!
  //
  //       // Download All Building Floors and Radiomaps
  //       if (AnyplaceDebug.PLAY_STORE) {
  //         mAnyplaceCache!!.fetchAllFloorsRadiomapsRun(this@NavigatorActivityOLD, object :
  //           BackgroundFetchListener {
  //           override fun onSuccess(result: String) {
  //             hideProgressBar()
  //             if (AnyplaceDebug.DEBUG_MESSAGES) {
  //               btnTrackme!!.setBackgroundColor(Color.YELLOW)
  //             }
  //             floorSelector!!.updateFiles(b!!.buid)
  //             floorSelector!!.Start(b.latitudeString, b.longitudeString)
  //           }
  //
  //           override fun onProgressUpdate(progress_current: Int, progress_total: Int) {
  //             progressBar!!.progress = (progress_current.toFloat() / progress_total * progressBar!!.max).toInt()
  //           }
  //
  //           override fun onErrorOrCancel(result: String, error: BackgroundFetchListener.ErrorType) {
  //             // Do not hide progress bar if previous task is running
  //             // ErrorType.SINGLE_INSTANCE
  //             // Do not hide progress bar because a new task will be created
  //             // ErrorType.CANCELLED
  //             if (error == BackgroundFetchListener.ErrorType.EXCEPTION) hideProgressBar()
  //           }
  //
  //           override fun onPrepareLongExecute() {
  //             showProgressBar()
  //           }
  //         }, b)
  //       }
  //     }
  //
  //     override fun onErrorOrCancel(result: String) {
  //       if (progressBarEnabled) {
  //         hideProgressBar()
  //       }
  //     }
  //
  //     override fun onPrepareLongExecute() {
  //       progressBarEnabled = true
  //       showProgressBar()
  //       // Set a smaller percentage than fetchAllFloorsRadiomapsOfBUID
  //       progressBar!!.progress = (1.0f / (userData!!.selectedBuilding.loadedFloors.size * 2) * progressBar!!.max).toInt()
  //     }
  //
  //     override fun disableSuccess() {
  //       disableSuccess = true
  //     }
  //   }
  //   if (downloadRadioMapTaskBuid != null) {
  //     (downloadRadioMapTaskBuid!!.callbackInterface as PreviousRunningTask).disableSuccess()
  //   }
  //   downloadRadioMapTaskBuid = DownloadRadioMapTaskBuid(Callback(), this, trackedPositionLat, trackedPositionLon, userData!!.selectedBuildingId, userData!!.selectedFloorNumber, false)
  //   val currentapiVersion = Build.VERSION.SDK_INT
  //   if (currentapiVersion >= Build.VERSION_CODES.HONEYCOMB) {
  //     // Execute task parallel with others and multiple instances of
  //     // itself
  //     downloadRadioMapTaskBuid!!.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
  //   } else {
  //     downloadRadioMapTaskBuid!!.execute()
  //   }
  // }
  //
  // var mLocationCallback: LocationCallback = object : LocationCallback() {
  //   override fun onLocationResult(locationResult: LocationResult) {
  //     val locationList = locationResult.locations
  //     if (locationList.size > 0) {
  //       //The last location in the list is the newest
  //       val location = locationList[locationList.size - 1]
  //       if (AnyplaceDebug.DEBUG_LOCATION) {
  //         D(TAG, "Location: " + location.latitude + " " + location.longitude)
  //       }
  //       mLastLocation = location
  //       // draw the location of the new position
  //       if (userMarker != null) {
  //         userMarker!!.remove()
  //       }
  //       val marker = MarkerOptions()
  //       marker.position(LatLng(locationResult.lastLocation.latitude, locationResult.lastLocation.longitude))
  //       marker.title("User").snippet("Estimated Position")
  //       marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_icon))
  //       marker.rotation(sensorsMain!!.rawHeading - bearing)
  //       userMarker = mMap!!.addMarker(marker)
  //       // CHECK:PM
  //       //mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userMarker.getPosition(), mInitialZoomLevel));
  //     }
  //   }
  // }
  // var mLocationCallbackInitial: LocationCallback = object : LocationCallback() {
  //   override fun onLocationResult(locationResult: LocationResult) {
  //     val locationList = locationResult.locations
  //     if (locationList.size > 0) {
  //       //The last location in the list is the newest
  //       val location = locationList[locationList.size - 1]
  //       if (AnyplaceDebug.DEBUG_LOCATION) {
  //         Log.i(TAG, "Location: " + location.latitude + " " + location.longitude)
  //       }
  //       mLastLocation = location
  //       // draw the location of the new position
  //       if (userMarker != null) {
  //         userMarker!!.remove()
  //       }
  //       val marker = MarkerOptions()
  //       marker.position(LatLng(locationResult.lastLocation.latitude, locationResult.lastLocation.longitude))
  //       marker.title("User").snippet("Estimated Position")
  //       marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_icon))
  //       marker.rotation(sensorsMain!!.rawHeading - bearing)
  //       userMarker = mMap!!.addMarker(marker)
  //
  //       // TODO:PM coroutine
  //       mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(userMarker.getPosition(), mInitialZoomLevel))
  //     }
  //   }
  // }

  // </ Play Services Functions
  private fun checkPlayServices(): Boolean {
    // Check that Google Play services is available
    val resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this)
    // If Google Play services is available
    return if (ConnectionResult.SUCCESS == resultCode) {
      // In debug mode, log the status
      // D3(
      //         TAG,
      //         "LocationUpdates: Google Play services is available."
      // )
      // Continue
      true
    } else {
      // Google Play services not available for some reason
      if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
        GooglePlayServicesUtil.getErrorDialog(
                resultCode,
                this,
                PLAY_SERVICES_RESOLUTION_REQUEST
        ).show()
      } else {
        // E(TAG, "This device is not supported.")
        Toast.makeText(
                this@NavigatorActivityOLD, "Google play services are required!",
                Toast.LENGTH_SHORT
        ).show()
        finish()
      }
      false
    }
  }

  // override fun onConnectionFailed(connectionResult: ConnectionResult) {
  //   Log.d(TAG, "Google Play Services: Connection failed")
  //   // Google Play services can resolve some errors it detects.
  //   // If the error has a resolution, try sending an Intent to
  //   // start a Google Play services activity that can resolve
  //   // error.
  //   if (connectionResult.hasResolution()) {
  //     try {
  //       // Start an Activity that tries to resolve the error
  //       connectionResult.startResolutionForResult(this, LOCATION_CONNECTION_FAILURE_RESOLUTION_REQUEST)
  //       // Thrown if Google Play services canceled the original
  //       // PendingIntent
  //     } catch (e: SendIntentException) {
  //       // Log the error
  //       e.printStackTrace()
  //     }
  //   } else {
  //     // If no resolution is available, display a dialog to the
  //     // user with the error.
  //     GooglePlayServicesUtil.getErrorDialog(connectionResult.errorCode, this, 0).show()
  //   }
  // }

  override fun onRequestPermissionsResult(
          requestCode: Int,
          permissions: Array<String>,
          grantResults: IntArray) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    when (requestCode) {
      REQUEST_PERMISSION_LOCATION -> if (grantResults.size > 0
              && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(this@NavigatorActivityOLD, "Permission Granted!", Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(this@NavigatorActivityOLD, "Permission Denied!", Toast.LENGTH_SHORT).show()
      }
    }
  }

  // @SuppressLint("MissingPermission")
  // override fun onConnected(dataBundle: Bundle?) {
  //   // I(2, "We are connected") // Called after onResume by system
  //   // if (checkPlayServices()) {
  //   //   I("checking Play services")
  //   //   initCamera()
  //   //   // Get Wifi + GPS Fused Location
  //   //   checkLocationPermission()
  //   //   mFusedLocationClient!!.lastLocation.addOnCompleteListener { task ->
  //   //     val location = task.result
  //   //     onLocationChanged(location)
  //   //   }.addOnFailureListener { Toast.makeText(baseContext, "No location available at the moment.", Toast.LENGTH_LONG).show() }
  //   //   // checkLocationPermission()
  //   //   // mFusedLocationClient!!.requestLocationUpdates(mLocationRequest, mLocationCallbackInitial, Looper.myLooper())
  //   // }
  // }

  // override fun onConnectionSuspended(i: Int) {}

  // </ NAVIGATION FUNCTIONS
  private fun startNavigationTask(id: String) {
    if (!OLDNetworkUtils.isOnline(this)) {
      Toast.makeText(this, WARN_NO_NETWORK, Toast.LENGTH_SHORT).show()
      return
    }

    // show the info window for the destination marker
    val marker = visiblePois!!.getMarkerFromPoisModel(id)
    marker?.showInfoWindow()
    val b = userData!!.selectedBuilding
    val currentFloor = userData!!.selectedFloorNumber

    class Status {
      var task1 = false
      var task2 = false
    }

    val status = Status()
    val dialog: ProgressDialog
    dialog = ProgressDialog(this)
    dialog.isIndeterminate = true
    dialog.setTitle("Plotting navigation")
    dialog.setMessage("Please be patient...")
    dialog.setCancelable(true)
    dialog.setCanceledOnTouchOutside(false)
    var _entrance: PoisModel? = null
    val pos = userData!!.positionWifi
    if (pos == null) {
      // Find The nearest building entrance from the destination poi
      var _entranceGlobal: PoisModel? = null
      var _entrance0: PoisModel? = null
      var _entranceCurrentFloor: PoisModel? = null
      var min = Double.MAX_VALUE
      val dest = mAnyplaceCache!!.poisMap[id]
      for (pm in mAnyplaceCache!!.poisMap.values) {
        if (pm.is_building_entrance) {
          if (pm.floor_number.equals(currentFloor, ignoreCase = true)) {
            val distance = Math.abs(pm.lat() - dest!!.lat()) + Math.abs(pm.lng() - dest.lng())
            if (min > distance) {
              _entranceCurrentFloor = pm
              min = distance
            }
          } else if (pm.floor_number.equals("0", ignoreCase = true)) {
            _entrance0 = pm
          } else {
            _entranceGlobal = pm
          }
        }
      }
      _entrance = _entranceCurrentFloor
              ?: (_entrance0
                      ?: if (_entranceGlobal != null) {
                        _entranceGlobal
                      } else {
                        Toast.makeText(this, "No entrance found!", Toast.LENGTH_SHORT).show()
                        return
                      })
    }

    // Does not run if entrance==null or is near the building
    val async1f: AsyncTask<Void, Void, String> = NavOutdoorTask(object : NavDirectionsListener {
      override fun onNavDirectionsSuccess(result: String, points: List<LatLng>) {
        onNavDirectionsFinished()
        if (!points.isEmpty()) {
          // points.add(new LatLng(entrancef.dlat, entrancef.dlon));
          pathLineOutdoorOptions = PolylineOptions().addAll(points).width(10f).color(Color.RED).zIndex(100.0f)
          pathLineOutdoor = mMap!!.addPolyline(pathLineOutdoorOptions)
        }
      }

      override fun onNavDirectionsErrorOrCancel(result: String) {
        onNavDirectionsFinished()
        // display the error cause
        Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
      }

      override fun onNavDirectionsAbort() {
        onNavDirectionsFinished()
      }

      fun onNavDirectionsFinished() {
        status.task1 = true
        if (status.task1 && status.task2) dialog.dismiss() else {
          clearNavigationData() // First task executed calls this
        }
      }
    }, userData!!.locationGPSorIP, if (_entrance != null) GeoPoint(_entrance.lat(), _entrance.lng()) else null)

    // start the navigation task
    val async2f: AsyncTask<Void, Void, String> = NavIndoorTask(object : NavRouteListener {
      override fun onNavRouteSuccess(result: String, points: List<PoisNav>) {
        onNavDirectionsFinished()
        // set the navigation building and new points
        userData!!.navBuilding = b
        userData!!.navPois = points
        handleIndoorPath(points) // handle drawing of the points
      }

      override fun onNavRouteErrorOrCancel(result: String) {
        onNavDirectionsFinished()
        // display the error cause
        Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
      }

      fun onNavDirectionsFinished() {
        status.task2 = true
        if (status.task1 && status.task2) dialog.dismiss() else {
          // First task executed calls this
          clearNavigationData()
        }
      }
    }, this, id, pos
            ?: GeoPoint(_entrance!!.lat(), _entrance.lng()), if (pos == null) _entrance!!.floor_number else currentFloor, b.buid)
    dialog.setOnCancelListener {
      async1f.cancel(true)
      async2f.cancel(true)
    }
    dialog.show()
    async1f.execute()
    async2f.execute()
  }

  private fun removeNavOverlays() {
    if (pathLineInside != null) {
      pathLineInside!!.remove()
    }
    if (pathLineOutdoor != null) {
      pathLineOutdoor!!.remove()
    }
    visiblePois!!.clearFromMarker()
    visiblePois!!.clearToMarker()
  }

  private fun clearNavigationData() {
    if (userData != null) {
      userData!!.clearNav()
    }
    removeNavOverlays()
    btnFloorUp!!.visibility = View.VISIBLE
    btnFloorDown!!.visibility = View.VISIBLE
  }

  // Loads the navigation route if any exists for the current floor selected
  // Multi Floor Route ex. DMSL --> Zeina
  private fun loadIndoorOutdoorPath() {
    if (userData!!.isNavBuildingSelected) {
      removeNavOverlays()
      handleIndoorPath(userData!!.navPois)
      if (pathLineOutdoorOptions != null) {
        pathLineOutdoor = mMap!!.addPolyline(pathLineOutdoorOptions)
      }
    } else {
      btnFloorUp!!.visibility = View.VISIBLE
      btnFloorDown!!.visibility = View.VISIBLE
    }
  }

  // draws the navigation route for the loaded floor
  private fun handleIndoorPath(puids: List<PoisNav>) {
    val p: MutableList<LatLng> = ArrayList()
    val selectedFloor = userData!!.selectedFloorNumber
    for (pt in puids) {
      // draw only the route for this floor
      if (pt.floor_number.equals(selectedFloor, ignoreCase = true)) {
        p.add(LatLng(pt.lat.toDouble(), pt.lon.toDouble()))
      }
    }
    pathLineInside = mMap!!.addPolyline(PolylineOptions().addAll(p).width(10f).color(Color.RED).zIndex(100.0f))
    if (!puids.isEmpty()) {
      // add markers for starting and ending position
      // starting point
      val nrpFrom = puids[0]
      if (nrpFrom.floor_number.equals(selectedFloor, ignoreCase = true)) visiblePois!!.fromMarker = mMap!!.addMarker(MarkerOptions().position(
              LatLng(nrpFrom.lat.toDouble(), nrpFrom.lon.toDouble())).title("Starting Position").icon(
              BitmapDescriptorFactory.fromResource(R.drawable.map_flag_green2_48)))
      // destination point
      val nrpTo = puids[puids.size - 1]
      if (nrpTo.floor_number.equals(selectedFloor, ignoreCase = true)) visiblePois!!.toMarker = mMap!!.addMarker(MarkerOptions().position(
              LatLng(nrpTo.lat.toDouble(), nrpTo.lon.toDouble())).title("Final Destination").icon(
              BitmapDescriptorFactory.fromResource(R.drawable.map_flag_pink2_48)))

      // adjust floor buttons
      if (nrpTo.floor_number == nrpFrom.floor_number) {
        btnFloorUp!!.visibility = View.VISIBLE
        btnFloorDown!!.visibility = View.VISIBLE
      } else {
        // Go to Navigation Destination
        if (nrpTo.floor_number.toInt() > selectedFloor.toInt()) {
          btnFloorDown!!.visibility = View.INVISIBLE
          btnFloorUp!!.visibility = View.VISIBLE
        } else if (nrpTo.floor_number.toInt() < selectedFloor.toInt()) {
          btnFloorUp!!.visibility = View.INVISIBLE
          btnFloorDown!!.visibility = View.VISIBLE
        } else { // if Navigation Destination Floor Go to Navigation
          // Start
          if (nrpFrom.floor_number.toInt() > selectedFloor.toInt()) {
            btnFloorDown!!.visibility = View.INVISIBLE
            btnFloorUp!!.visibility = View.VISIBLE
          } else {
            btnFloorUp!!.visibility = View.INVISIBLE
            btnFloorDown!!.visibility = View.VISIBLE
          }
        }
      }
    }
  }

  //TODO: move to android lib.
  private fun handleBuildingsOnMap(forceReload: Boolean) {
    // val mAnyplaceCache = ObjectCache.getInstance(app)
    mAnyplaceCache?.loadWorldBuildings(this, object : FetchBuildingsTaskListener {
      override fun onSuccess(result: String, buildings: List<BuildingModel>) {
        val collection: MutableList<BuildingModel> = ArrayList(buildings)
        builds = buildings
        mClusterManager!!.clearItems()
        val buid = userData!!.selectedBuilding
        if (buid != null) collection.remove(buid)
        mClusterManager!!.addItems(collection)
        mClusterManager!!.cluster()
        // HACK. This dumps all the cached icons & recreates everything.
        mClusterManager!!.renderer = MyBuildingsRenderer(this@NavigatorActivityOLD, mMap, mClusterManager)
      }

      override fun onErrorOrCancel(result: String) {}
    }, forceReload)
  }

  // /> NAVIGATION FUNCTIONS
  // </POIS
  private fun handlePoisOnMap(collection: Collection<PoisModel>) {
    visiblePois!!.clearAll()
    val currentFloor = userData!!.selectedFloorNumber

    // Display part of Description Text Only
    // Make an approximation of available space based on map size
    val fragmentWidth = findViewById<View>(R.id.map).width * 2
    val infoWindow = layoutInflater.inflate(R.layout.info_window, null) as ViewGroup
    val infoSnippet = infoWindow.findViewById<TextView>(R.id.snippet)
    val paint = infoSnippet.paint
    val bmapDesc = BitmapDescriptorFactory.fromResource(R.drawable.pin_poi)
    for (pm in collection) {
      if (pm.floor_number.equals(currentFloor, ignoreCase = true)) {
        val snippet = AndroidUtils.fillTextBox(paint, fragmentWidth, pm.description)
        val m = mMap!!.addMarker(MarkerOptions().position(
                LatLng(pm.lat.toDouble(), pm.lng.toDouble())).title(pm.name).snippet(snippet).icon(bmapDesc))
        visiblePois!!.addMarkerAndPoi(m, pm)
      }
    }
  }

  private fun fetchPoisByBuidToCache(buid: String, l: FetchPoisByBuidTask.Callback) {
    // Check for cahced pois
    if (mAnyplaceCache!!.checkPoisBUID(buid)) {
      l.onSuccess("Pois read from cache", mAnyplaceCache!!.poisMap)
    } else {
      val fetchPoisByBuidFloorTask = FetchPoisByBuidTask(
              this,
              object : FetchPoisByBuidTask.Callback {
                override fun onSuccess(result: String, poisMap: Map<String, PoisModel>) {
                  // mAnyplaceCache!!.setPois(app, poisMap, buid)
                  l.onSuccess(result, poisMap)
                }

                override fun onErrorOrCancel(result: String) {
                  // clear any markers that might have already been added to
                  // the map
                  visiblePois!!.clearAll()
                  // clear and resets the cached POIS inside AnyplaceCache
                  // mAnyplaceCache!!.setPois(app, HashMap(), "")
                  l.onErrorOrCancel(result)
                }
              }, buid)
      fetchPoisByBuidFloorTask.execute()
    }
  }

  // />POIS
  // </ Activity Listeners
  override fun onNewWifiResults(aps: Int) {
    detectedAPs!!.text = "AP: $aps"
  }

  //Play Services location listener
  // override fun onLocationChanged(location: Location) {
  //   if (location != null) {
  //     userData!!.setLocationGPS(location)
  //     // updateLocation()
  //     if (mAutomaticGPSBuildingSelection) {
  //       mAutomaticGPSBuildingSelection = false
  //       checkLocationPermission()
  //       mFusedLocationClient!!.lastLocation.addOnCompleteListener { task ->
  //         val location = task.result
  //         val point = GeoPoint(location.latitude, location.longitude)
  //         loadSelectBuildingActivity(point, true)
  //       }
  //     }
  //   }
  // }

  override fun onNewLocation(pos: LatLng) {
    // userData!!.setPositionWifi(pos.latitude, pos.longitude)
    // runOnUiThread {
    //   if (isTrackingErrorBackground) {
    //     isTrackingErrorBackground = false
    //     btnTrackme!!.setImageResource(R.drawable.dark_device_access_location_searching)
    //   }
    //   updateLocation() // update the wifi location of the user
    // }
  }

  override fun onTrackerError(msg: String) {
    // if (!isTrackingErrorBackground) runOnUiThread {
    //   if (!isTrackingErrorBackground) {
    //     btnTrackme!!.setImageResource(R.drawable.dark_device_access_location_off)
    //     isTrackingErrorBackground = true
    //   }
    // }
  }

  // override fun onFloorError(ex: Exception) {
  //   if (ex is NonCriticalError) return
  //   floorSelector!!.Stop()
  //   // TODO Auto-generated method stub
  //   Log.e("Floor Selector", ex.toString())
  //   Toast.makeText(baseContext, "Floor Selector ecountered an error", Toast.LENGTH_SHORT).show()
  // }

  // Change Floor Request on current Building
  // override fun onNewFloor(floorNumber: String) {
  //   if (floorChangeRequestDialog) return
  //   val b = userData!!.selectedBuilding
  //   if (b == null) {
  //     Log.e("Unified Activity", "onNewFloor b=null")
  //     return
  //   }
  //
  //   // Check if the floor is the loaded floor
  //   if (b.selectedFloor!!.floor_number == floorNumber) {
  //     lastFloor = null
  //     return
  //   }
  //
  //   // User clicked Cancel
  //   if (lastFloor != null && lastFloor == floorNumber) {
  //     return
  //   }
  //   lastFloor = floorNumber
  //   val f = b.getFloorFromNumber(floorNumber)
  //   if (f != null) {
  //     val alertDialog = AlertDialog.Builder(this@NavigatorActivityOLD)
  //     alertDialog.setTitle("Floor Change Detected")
  //     alertDialog.setMessage("Floor Number: $floorNumber. Do you want to proceed?")
  //     alertDialog.setPositiveButton("OK") { dialog, which ->
  //       dialog.dismiss()
  //       floorChangeRequestDialog = false
  //       // bypassSelectBuildingActivity(b, f)
  //     }
  //     alertDialog.setNegativeButton("Cancel") { dialog, which ->
  //       dialog.cancel()
  //       floorChangeRequestDialog = false
  //     }
  //     alertDialog.show()
  //     floorChangeRequestDialog = true
  //   }
  // }

  // private fun updateLocation() {
  //   // GeoPoint location = userData.getLatestUserPosition();
  //   try {
  //     checkLocationPermission()
  //     mFusedLocationClient!!.lastLocation.addOnCompleteListener { task ->
  //       try {
  //         val loc = task.result
  //         val location = GeoPoint(loc.latitude, loc.longitude)
  //         if (location != null) {
  //           // draw the location of the new position
  //           if (userMarker != null) {
  //             userMarker!!.remove()
  //           }
  //           val marker = MarkerOptions()
  //           marker.position(LatLng(location.dlat, location.dlon))
  //           marker.title("User").snippet("Estimated Position")
  //           marker.icon(BitmapDescriptorFactory.fromResource(R.drawable.marker_icon))
  //           marker.rotation(sensorsMain!!.rawHeading - bearing)
  //           userMarker = mMap!!.addMarker(marker)
  //         }
  //       } catch (e: Exception) {
  //         Log.d(TAG, e.message!!)
  //       }
  //     }
  //   } catch (e: Exception) {
  //     Log.d(TAG, e.message!!)
  //   }
  // }

  // </ HELPER FUNCTIONS
  private fun enableAnyplaceTracker() {
    // Do not change file wile enabling tracker
    if (lpTracker!!.trackOn()) {
      btnTrackme!!.setImageResource(R.drawable.dark_device_access_location_searching)
      isTrackingErrorBackground = false
    }
  }

  private fun disableAnyplaceTracker() {
    lpTracker!!.trackOff()
    btnTrackme!!.setImageResource(R.drawable.dark_device_access_location_off)
    isTrackingErrorBackground = true
  }

  private fun addTrackerListeners() {
    // sensorsMain.addListener((SensorsMain.IOrientationListener) this);
    lpTracker!!.addListener(this as WifiResultsAnyplaceTrackerListener)
    lpTracker!!.addListener(this as TrackedLocAnyplaceTrackerListener)
    lpTracker!!.addListener(this as ErrorAnyplaceTrackerListener)
    floorSelector!!.addListener(this as FloorAnyplaceFloorListener)
    floorSelector!!.addListener(this as ErrorAnyplaceFloorListener)
  }

  private fun removeTrackerListeners() {
    // sensorsMain.removeListener((SensorsMain.IOrientationListener) this);
    lpTracker!!.removeListener(this as WifiResultsAnyplaceTrackerListener)
    lpTracker!!.removeListener(this as TrackedLocAnyplaceTrackerListener)
    lpTracker!!.removeListener(this as ErrorAnyplaceTrackerListener)
    floorSelector!!.removeListener(this as FloorAnyplaceFloorListener)
    floorSelector!!.removeListener(this as ErrorAnyplaceFloorListener)
  }

  private fun showProgressBar() {
    progressBar!!.visibility = View.VISIBLE
  }

  private fun hideProgressBar() {
    progressBar!!.visibility = View.GONE
  }

  // /> HELPER FUNCTIONS
  // </ SEARCHING FUNCTIONS
  // override fun onNewIntent(intent: Intent) {
  //   super.onNewIntent(intent)
  //   handleIntent(intent)
  // }

  // Search Button or URL handle
  // private fun handleIntent(intent: Intent) {
  //   val action = intent.action
  //   if (Intent.ACTION_SEARCH == action) {
  //     // check what type of search we need
  //     val searchType = AnyPlaceSeachingHelper.getSearchType(mMap!!.cameraPosition.zoom)
  //     val query = intent.getStringExtra(SearchManager.QUERY)
  //     val gp = userData!!.latestUserPosition
  //
  //     // manually launch the real search activity
  //     val searchIntent = Intent(this@NavigatorActivityOLD, SearchPOIActivity::class.java)
  //     // add query to the Intent Extras
  //     searchIntent.action = action
  //     searchIntent.putExtra("searchType", searchType)
  //     searchIntent.putExtra("query", query)
  //     searchIntent.putExtra("lat", gp?.dlat ?: csLat)
  //     searchIntent.putExtra("lng", gp?.dlon ?: csLon)
  //     startActivityForResult(searchIntent, SEARCH_POI_ACTIVITY_RESULT)
  //   } else if (Intent.ACTION_VIEW == action) {
  //     val data = intent.dataString
  //     // if (data != null && data.startsWith("http")) {
  //     //   val uri = intent.data
  //     //   if (uri != null) {
  //     //     val path = uri.path
  //     //     if (path != null && path == "/getnavigation") {
  //     //       val poid = uri.getQueryParameter("poid")
  //     //       if (poid == null || poid == "") {
  //     //         // Share building
  //     //         val buid = uri.getQueryParameter("buid")
  //     //         if (buid == null || buid == "") {
  //     //           Toast.makeText(baseContext, "Buid parameter expected", Toast.LENGTH_SHORT).show()
  //     //         } else {
  //     //           mAutomaticGPSBuildingSelection = false
  //     //           mAnyplaceCache!!.loadBuilding(this, buid, object : Callback {
  //     //             override fun onSuccess(result: String?, b: BuildingModel?) {
  //     //               bypassSelectBuildingActivity(b, uri.getQueryParameter("floor"), true)
  //     //             }
  //     //
  //     //             override fun onErrorOrCancel(result: String?) {
  //     //               Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
  //     //             }
  //     //           })
  //     //         }
  //     //       } else {
  //     //         // Share POI
  //     //         mAutomaticGPSBuildingSelection = false
  //     //         val pref = getSharedPreferences("Anyplace_Preferences", MODE_PRIVATE)
  //     //         val access_token = pref.getString("access_token", "")
  //     //         FetchPoiByPuidTask(object : FetchPoiListener {
  //     //           override fun onSuccess(result: String, poi: PoisModel) {
  //     //             if (userData!!.selectedBuildingId != null && userData!!.selectedBuildingId == poi.buid) {
  //     //               // Building is Loaded
  //     //               startNavigationTask(poi.puid)
  //     //             } else {
  //     //               // Load Building
  //     //               mAnyplaceCache!!.loadBuilding(this@NavigatorActivityOLD, poi.buid, object : Callback {
  //     //                 override fun onSuccess(result: String?, b: BuildingModel?) {
  //     //                   bypassSelectBuildingActivity(b, poi.floor_number, true, poi)
  //     //                 }
  //     //
  //     //                 override fun onErrorOrCancel(result: String?) {
  //     //                   Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
  //     //                 }
  //     //               })
  //     //             }
  //     //           }
  //     //
  //     //           override fun onErrorOrCancel(result: String) {
  //     //             Toast.makeText(baseContext, result, Toast.LENGTH_SHORT).show()
  //     //           }
  //     //         }, app, poid, access_token).execute()
  //     //       }
  //     //     }
  //     //   }
  //     // } else {
  //     //
  //     //   // Search TextBox results only
  //     //   // PoisModel or Place Class
  //     //   val place_selected = AnyPlaceSeachingHelper.getClassfromJson(data)
  //     //   if (place_selected.id() != null) {
  //     //     // hide the search view when a navigation route is drawn
  //     //     if (searchView != null) {
  //     //       searchView!!.isIconified = true
  //     //       searchView!!.clearFocus()
  //     //     }
  //     //     handleSearchPlaceSelection(place_selected)
  //     //   }
  //     // }
  //   }
  // } // end of handle intent
  // handle the selected place from the TextBox or search activity
  // either Anyplace POI or a Google Place
  // private fun handleSearchPlaceSelection(place: IPoisClass?) {
  //   if (place == null) return
  //   when (place.type()) {
  //     IPoisClass.Type.AnyplacePOI -> startNavigationTask(place.id())
  //     IPoisClass.Type.GooglePlace -> mAnyplaceCache!!.loadWorldBuildings(this, object : FetchBuildingsTaskListener {
  //       override fun onSuccess(result: String, allBuildings: List<BuildingModel>) {
  //         val nearBuildings = FetchNearBuildingsTask()
  //         nearBuildings.run(allBuildings.iterator(), place.lat(), place.lng(), 200)
  //         if (nearBuildings.buildings.size > 0) {
  //           val b = nearBuildings.buildings[0]
  //           bypassSelectBuildingActivity(b, "0", false)
  //         } else {
  //           showGooglePoi(place)
  //         }
  //       }
  //
  //       override fun onErrorOrCancel(result: String) {
  //         showGooglePoi(place)
  //       }
  //     }, false)
  //   }
  // }

  // private fun showGooglePoi(place: IPoisClass) {
  //   cameraUpdate = true
  //
  //   // TODO coroutines..
  //   mMap!!.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(place.lat(), place.lng()), mInitialZoomLevel), object : CancelableCallback {
  //     override fun onFinish() {
  //       cameraUpdate = false
  //     }
  //
  //     override fun onCancel() {
  //       cameraUpdate = false
  //     }
  //   })
  //   // add the marker for this Google Place
  //   val mGooglePlaceMarker = mMap!!.addMarker(
  //           MarkerOptions().position(
  //                   LatLng(place.lat(), place.lng())).icon(BitmapDescriptorFactory.fromResource(
  //                   R.drawable.pin_poi)))
  //   visiblePois!!.setGooglePlaceMarker(mGooglePlaceMarker, place)
  // }

  // override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
  //   if (key == "TrackingAlgorithm") {
  //     lpTracker!!.setAlgorithm(sharedPreferences.getString("TrackingAlgorithm", "WKNN"))
  //   }
  // }

  private fun popup_msg(msg: String, title: String) {
    val alert_box = AlertDialog.Builder(this)
    alert_box.setTitle(title)
    alert_box.setMessage(msg)
    alert_box.setNeutralButton("Hide") { dialog, which -> dialog.cancel() }
    val alert = alert_box.create()
    alert.show()
  }

  override fun onMapReady(googleMap: GoogleMap) {
    mMap = googleMap

    //mClusterManager = new ClusterManager<>(this, mMap);
    mClusterManager = ClusterManager(this, mMap)

    // Check if we were successful in obtaining the map.
    if (mMap != null) {

      // http://stackoverflow.com/questions/14123243/google-maps-android-api-v2-interactive-infowindow-like-in-original-android-go
      val mapWrapperLayout = findViewById<View>(R.id.map_relative_layout) as MapWrapperLayout

      // MapWrapperLayout initialization
      // 39 - default marker height
      // 20 - offset between the default InfoWindow bottom edge and
      // it's content bottom edge
      mapWrapperLayout.init(mMap, getPixelsFromDp(this, (39 + 20).toFloat()))
      val infoWindow: ViewGroup
      val infoTitle: TextView
      val infoSnippet: TextView
      val infoButton1: Button
      val infoButtonListener1: OnInfoWindowElemTouchListener
      val infoButton2: Button
      val infoButtonListener2: OnInfoWindowElemTouchListener

      // We want to reuse the info window for all the markers,
      // so let's create only one class member instance
      infoWindow = layoutInflater.inflate(R.layout.info_window, null) as ViewGroup
      infoTitle = infoWindow.findViewById(R.id.title)
      infoSnippet = infoWindow.findViewById(R.id.snippet)
      infoButton1 = infoWindow.findViewById(R.id.button1)
      infoButton2 = infoWindow.findViewById(R.id.button2)

      // Setting custom OnTouchListener which deals with the pressed
      // state
      // so it shows up
      infoButtonListener1 = object : OnInfoWindowElemTouchListener(infoButton1,
              resources.getDrawable(R.drawable.button_unsel),
              resources.getDrawable(R.drawable.button_sel)) {
        override fun onClickConfirmed(v: View, marker: Marker) {
          val poi = visiblePois!!.getPoisModelFromMarker(marker)
          if (poi != null) {
            // start the navigation using the clicked marker as
            // destination
            startNavigationTask(poi.puid)
          }
        }
      }
      infoButton1.setOnTouchListener(infoButtonListener1)

      // Setting custom OnTouchListener which deals with the pressed
      // state
      // so it shows up
      infoButtonListener2 = object : OnInfoWindowElemTouchListener(infoButton2,
              resources.getDrawable(R.drawable.button_unsel),
              resources.getDrawable(R.drawable.button_sel)) {
        override fun onClickConfirmed(v: View, marker: Marker) {
          val poi = visiblePois!!.getPoisModelFromMarker(marker)
          if (poi != null) {
            if (poi.description == "" || poi.description == "-") {
              // start the navigation using the clicked marker
              // as destination
              popup_msg("No description available.", poi.name)
            } else {
              popup_msg(poi.description, poi.name)
            }
          }
        }
      }
      infoButton2.setOnTouchListener(infoButtonListener2)
      // mMap!!.setInfoWindowAdapter(object : InfoWindowAdapter {
      //   override fun getInfoWindow(marker: Marker): View {
      //     return null
      //   }
      //
      //   override fun getInfoContents(marker: Marker): View {
      //     // Setting up the infoWindow with current's marker info
      //     infoTitle.text = marker.title
      //     infoSnippet.text = marker.snippet
      //     infoButtonListener1.setMarker(marker)
      //     infoButtonListener2.setMarker(marker)
      //
      //     // We must call this to set the current marker and
      //     // infoWindow references
      //     // to the MapWrapperLayout
      //     mapWrapperLayout.setMarkerWithInfoWindow(marker, infoWindow)
      //     return infoWindow
      //   }
      // })
      setUpMap()
    }

    // initMap();
    // // initCamera();
    // initListeners();
  }

  // Define a DialogFragment that displays the error dialog
  // class ErrorDialogFragment     // Default constructor. Sets the dialog field to null
  //   : DialogFragment() {
  //   // Global field to contain the error dialog
  //   private var mDialog: Dialog = null
  //
  //   // Set the dialog to display
  //   fun setDialog(dialog: Dialog) {
  //     mDialog = dialog
  //   }
  //
  //   // Return a Dialog to the DialogFragment.
  //   override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
  //     return mDialog
  //   }
  // }

  override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    when (keyCode) {
      KeyEvent.KEYCODE_BACK -> {
        finish()
        return true
      }
      KeyEvent.KEYCODE_FOCUS, KeyEvent.KEYCODE_CAMERA, KeyEvent.KEYCODE_SEARCH ->         // Handle these events so they don't launch the Camera app
        return true
    }
    return super.onKeyDown(keyCode, event)
  }

  companion object {
    private const val TAG = "ap_nav"
    private const val csLat = 35.144569
    private const val csLon = 33.411107
    private const val mInitialZoomLevel = 19.0f
    const val SHARED_PREFS_ANYPLACE = "Anyplace_Preferences"

    // Define a request code to send to Google Play services This code is
    // returned in Activity.onActivityResult
    private const val LOCATION_CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000
    private const val PLAY_SERVICES_RESOLUTION_REQUEST = 9001
    private const val SELECT_PLACE_ACTIVITY_RESULT = 1112
    private const val SEARCH_POI_ACTIVITY_RESULT = 1113
    private const val PREFERENCES_ACTIVITY_RESULT = 1114
    fun getPixelsFromDp(context: Context, dp: Float): Int {
      val scale = context.resources.displayMetrics.density
      return (dp * scale + 0.5f).toInt()
    }

    const val MY_PERMISSIONS_REQUEST_LOCATION = 99
  }
}