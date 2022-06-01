package cy.ac.ucy.cs.anyplace.smas.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.location.Location
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.search.modules.RoutePOI
import com.example.search.utils.RVAdapter
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.*
import com.google.android.material.button.MaterialButton
import cy.ac.ucy.cs.anyplace.lib.android.data.models.helpers.FloorHelper
import cy.ac.ucy.cs.anyplace.lib.android.utils.LOG
import cy.ac.ucy.cs.anyplace.lib.android.extensions.*
import cy.ac.ucy.cs.anyplace.lib.android.ui.cv.CvMapActivity
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.CvMapViewModel
import cy.ac.ucy.cs.anyplace.lib.android.viewmodels.DetectorViewModel
import cy.ac.ucy.cs.anyplace.lib.models.Coord
import cy.ac.ucy.cs.anyplace.lib.models.POI
import cy.ac.ucy.cs.anyplace.lib.models.UserCoordinates
import cy.ac.ucy.cs.anyplace.smas.R
import cy.ac.ucy.cs.anyplace.smas.ui.settings.dialogs.MainSmasSettingsDialog
import cy.ac.ucy.cs.anyplace.smas.utils.SmasAssetReader
import cy.ac.ucy.cs.anyplace.smas.viewmodel.SmasMainViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import java.lang.Float.MAX_VALUE

/**
 *
 * This will be merged later with another class (by me).
 * But for now keep everything separate here..
 *
 */
@AndroidEntryPoint
class SearchActivity : CvMapActivity(), OnMapReadyCallback, GoogleMap.OnMarkerClickListener {
  // PROVIDE TO BASE CLASS [CameraActivity]:
  // PROVIDE TO BASE CLASS [CameraActivity]:
  override val layout_activity: Int get() = R.layout.activity_search
  override val id_bottomsheet: Int get() = R.id.bottom_sheet_layout
  override val id_gesture_layout: Int get() = R.id.gesture_layout
  override val id_gmap: Int get() = R.id.mapView

  private val id_search_tool: Int get() = R.id.search_tool
  private val id_results_list: Int get() = R.id.results_list

  //The list with all the POIS on the ship
  private var listOfPois: List<POI>? = null
  //The list of the POIS' names that matched the user input
  private var matchedPois: ArrayList<POI> = arrayListOf()
  //The route from the current position to the selected POI
  private var route: List<RoutePOI>? = null
  //The coordinates of the user - current position
  private lateinit var userCoordinates: UserCoordinates
  //A list of the polylines drawn for the route
  private val routePolylines = ArrayList<Polyline>()

  private lateinit var rvAdapter: RVAdapter
  protected val assetSmas by lazy { SmasAssetReader(applicationContext) }

  @Suppress("UNCHECKED_CAST")
  override val view_model_class: Class<DetectorViewModel> =
          SmasMainViewModel::class.java as Class<DetectorViewModel>

  // VIEW MODELS
  /** extends [CvMapViewModel] */
  private lateinit var VM: SmasMainViewModel

  override fun postCreate() {
    super.postCreate()
    VM = _vm as SmasMainViewModel

    setupCollectors()
  }

  override fun onResume() {
    super.onResume()
    LOG.D(TAG, "onResume")
  }

  override fun setupButtonsAndUi() {
    super.setupButtonsAndUi()
    LOG.D2()
    setupButtonSettings()
  }

  private fun setupButtonSettings() {
    val btnSettings: MaterialButton = findViewById(R.id.button_settings)
    btnSettings.setOnClickListener {
      MainSmasSettingsDialog.SHOW(supportFragmentManager, MainSmasSettingsDialog.FROM_MAIN)
    }
  }

  private fun setupCollectors() {
    LOG.D(TAG_METHOD)
    collectLoadedFloors()
  }

  /**
   * Observes when the initial floor will be loaded, and runs a method
   */
  private fun collectLoadedFloors() {
    lifecycleScope.launch(Main) {
      VM.floor.collect { floor ->
        if (floor == null) return@collect
        VM.floorH = FloorHelper(floor, VM.spaceH)
        LOG.W(TAG, "FLOOR NOW IS: ${VM.floorH!!.prettyFloorName()}")
      }
    }
  }

  private fun setupSearchBar() {
    initRecyclerView()
    setSearchViewListeners()
  }

  //FUNCTIONS RELATED TO THE MAP
  override fun onMapReadyCallback() {
     //val userCoordinates = getUserCoordinates()
    //only for testing..
    userCoordinates = UserCoordinates("vessel_2a2cf77c-91e0-41e2-971b-e80f5570d616_1635154314048", 1, 57.695007, 11.911662)
    setupSearchBar()
  }

  private fun getUserCoordinates(): UserCoordinates? {
    var userCoord: UserCoordinates? = null
    if (VM.location.value.coord != null) {
      userCoord = UserCoordinates(VM.spaceH.obj.id,
              VM.floorH?.obj!!.floorNumber.toInt(),
              VM.location.value.coord!!.lat,
              VM.location.value.coord!!.lon)
      return userCoord
    }

    return null
  }

  //Distance between two geographical coordinates
  fun findDistance(source: Coord, destination: Coord): Float {
    val srcLoc = Location("srcLoc")
    srcLoc.latitude = source.lat
    srcLoc.longitude = source.lon
    val destLoc = Location("destLoc")
    destLoc.latitude = destination.lat
    destLoc.longitude = destination.lon
    return srcLoc.distanceTo(destLoc)
  }

  //Finds the closest POI to the user's location
  private fun findClosestPoi(userCoordinates: UserCoordinates): POI? {
    var minDistance = MAX_VALUE
    var closestPoi: POI? = null

    //The pois here should only be the pois of the same building and floor (nw) as the user's
    val pois = getPois()
    if (pois != null) {
      for (poi in pois) {
        val latLonUser = Coord(userCoordinates.lat, userCoordinates.lon)
        val latLonPoi = Coord(poi.coordinatesLat.toDouble(), poi.coordinatesLon.toDouble())
        val dist = findDistance(latLonUser, latLonPoi)
        if (dist < minDistance) {
          minDistance = dist
          closestPoi = poi
        }
      }
    }
    return closestPoi
  }

  //When the user clicks on a marker, a route to that marker is drawn on the map
  override fun onMarkerClick(p0: Marker): Boolean {

    if (userCoordinates != null) {
      val latLonUser = LatLng(userCoordinates.lat, userCoordinates.lon)
      var closestPoiLatLon = LatLng(0.0, 0.0)
      var closestPoi = findClosestPoi(userCoordinates)

      if (closestPoi != null) {
        Log.d("closestPOI", "${closestPoi.coordinatesLat} ${closestPoi.coordinatesLon}")
        closestPoiLatLon = LatLng(closestPoi.coordinatesLat.toDouble(), closestPoi.coordinatesLon.toDouble())
      }

      //the route should start from closestPoi and end to the poi-target...
      route = getRoute()
      Log.d("route", route.toString())

      //the floor changes
      lifecycleScope.launch(Main) {
        VM.floor.collect { floor ->
          if (floor == null) return@collect
          VM.floorH = FloorHelper(floor, VM.spaceH)

          //clear the already drawn polylines from the map
          clearPolylines()

          //draw the path from the user to the closest POI
          if (closestPoi != null && closestPoi.floorNumber == VM.floorH!!.obj.floorNumber) {
            val line = wMap.obj.addPolyline(PolylineOptions().add(latLonUser).add(closestPoiLatLon).color(Color.RED))
            routePolylines.add(line)
          }

          //draw the route from the closest POI to the POI-destination
          route?.let {
            it.forEachIndexed { index, poi ->
              val poiCurr =
                      LatLng(poi.lat.toDouble(), poi.lon.toDouble())
              if (index != it.size - 1 && poi.floor_number == VM.floorH!!.obj.floorNumber) {
                val poiNext = LatLng(it[index + 1].lat.toDouble(), it[index + 1].lon.toDouble())
                val line = wMap.obj.addPolyline(PolylineOptions().add(poiCurr).add(poiNext).color(Color.RED))
                routePolylines.add(line)
              }
            }
          }
        }
      }

    } else {
      Toast.makeText(app, "Localization problem", Toast.LENGTH_SHORT)
      LOG.E("Localization problem")
    }
    return false;
  }

  private fun clearPolylines() {
    routePolylines.forEach {
      it.remove()
    }
    routePolylines.clear()
  }

  private fun switchFloors(poi: POI) {
    //move floors up and down according to the POI-destination and its floor
    val currFloor = VM.floorH!!.obj.floorNumber
    val floorDiff = currFloor.toInt() - poi.floorNumber.toInt()
    if (floorDiff < 0) {
      while (VM.floorH!!.obj.floorNumber.toInt() != poi.floorNumber.toInt())
        VM.floorGoUp()
    } else if (floorDiff > 0) {
      while (VM.floorH!!.obj.floorNumber.toInt() != poi.floorNumber.toInt())
        VM.floorGoDown()
    }
  }

  //FUNCTIONS RELATED TO RECYCLER VIEW AND SEARCH VIEW
  private fun initRecyclerView() {
    listOfPois = getPois()

    if (listOfPois != null) {
      rvAdapter = RVAdapter(listOfPois!!) { poi ->
        val destLatLng =
                LatLng(poi.coordinatesLat.toDouble(), poi.coordinatesLon.toDouble())
        wMap.obj.moveCamera(CameraUpdateFactory.newLatLngZoom(destLatLng, 24f))

        switchFloors(poi)

        //prepare the markers for the user-location and POI-destination
        val destMarkerOpt = MarkerOptions().position(destLatLng).icon(bitmapDescriptorFromVector(applicationContext, R.drawable.locator))
        var destMarker: Marker? = null
        val userMarkerOpt = MarkerOptions().position(LatLng(userCoordinates.lat, userCoordinates.lon)).icon(bitmapDescriptorFromVector(applicationContext, R.drawable.ic_baseline_person_pin_circle_24))
        var userMarker: Marker? = null

        //when the floor changes
        lifecycleScope.launch(Main) {
          VM.floor.collect { floor ->
            if (floor == null) return@collect
            VM.floorH = FloorHelper(floor, VM.spaceH)

            //add the user marker on the correct floor
            if (userCoordinates.level == VM.floorH!!.obj.floorNumber.toInt())
              userMarker = wMap.obj.addMarker(userMarkerOpt)
            else userMarker?.remove()

            //add the POI-destination marker on the correct floor
            if (VM.floorH!!.obj.floorNumber == poi.floorNumber)
              destMarker = wMap.obj.addMarker(destMarkerOpt)
            else destMarker?.remove()
          }
        }

        wMap.obj.setOnMarkerClickListener(this)

        val searchView = findViewById<SearchView>(id_search_tool)
        searchView.setQuery(null, false)
        searchView.isIconified = true

      }
              .also {
                findViewById<RecyclerView>(id_results_list).adapter = it
                findViewById<RecyclerView>(id_results_list).adapter!!.notifyDataSetChanged()
              }
    }
  }

  private fun setSearchViewListeners() {
    val searchView = findViewById<SearchView>(id_search_tool)
    val recyclerView = findViewById<RecyclerView>(id_results_list)

    //listener for the change of the user input
    searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
      override fun onQueryTextSubmit(query: String?): Boolean {
        if (matchedPois.isNotEmpty() && query != null) {
          search(query)
        } else {
          Toast.makeText(applicationContext, "No match found", Toast.LENGTH_SHORT).show()
        }
        return false
      }

      override fun onQueryTextChange(newText: String?): Boolean {
        if (newText != null)
          search(newText)
        return false
      }
    })

    searchView.setOnSearchClickListener {
      recyclerView.fadeIn()
      searchView.background = ContextCompat.getDrawable(applicationContext, R.drawable.bg_white_rounded)
    }

    searchView.setOnCloseListener {
      findViewById<RecyclerView>(id_results_list).visibility = View.GONE
      searchView.background = ContextCompat.getDrawable(applicationContext, R.drawable.bg_blue_rounded)
      false
    }
  }

  private fun updateRecyclerView() {
    findViewById<RecyclerView>(id_results_list).apply {
      rvAdapter.pois = matchedPois
      rvAdapter.notifyDataSetChanged()
    }
  }

  //auto-completion
  private fun search(text: String) {
    matchedPois = arrayListOf()

    listOfPois?.forEach { poi ->
      if (poi.name.contains(text, true)) {
        matchedPois.add(poi)
      }
      updateRecyclerView()
    }
  }

  //START OF DATA READ FROM ASSET READER
  //the pois that can be searched
  fun getPois(): List<POI>? = assetSmas.getPois()?.pois

  //the route from a position to a poi
  fun getRoute(): List<RoutePOI>? = assetSmas.getRoute()?.pois

  //UTILS
  //From https://stackoverflow.com/questions/42365658/custom-marker-in-google-maps-in-android-with-vector-asset-icon#comment122784538_42365658 - Leonid Ustenko
  private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
    return ContextCompat.getDrawable(context, vectorResId)?.run {
      setBounds(0, 0, intrinsicWidth, intrinsicHeight)
      val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
      draw(Canvas(bitmap))
      BitmapDescriptorFactory.fromBitmap(bitmap)
    }
  }
}