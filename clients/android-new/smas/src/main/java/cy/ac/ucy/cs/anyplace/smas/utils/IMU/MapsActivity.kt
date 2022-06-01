package cy.ac.ucy.cs.anyplace.smas.utils.IMU

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import cy.ac.ucy.cs.anyplace.smas.R
import cy.ac.ucy.cs.anyplace.smas.databinding.ActivityMapsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MapsActivity : AppCompatActivity(), OnMapReadyCallback {


    private lateinit var binding: ActivityMapsBinding
    private lateinit var mMap: GoogleMap

    private lateinit var vessel: VesselModel
    private lateinit var sensorsVM: SensorsViewModel
    private val mapManager = MapManager()

    private var currentPosition: LatLng = LatLng(0.0,0.0)
    private val polylinesFloor1 = ArrayList<Polyline>()


    companion object{
        private const val ALL_PERMISSIONS = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val context = applicationContext
        vessel = VesselModel(context)
        sensorsVM = ViewModelProvider(this).get(SensorsViewModel::class.java)

        //Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        checkPermission()
    }

  //Register sensor listeners
    override fun onResume(){
        super.onResume()
        sensorsVM.registerListeners()
    }

  //Unregister the sensor listeners
    override fun onPause(){
        super.onPause()
        sensorsVM.unregisterListener()
    }

    //From https://stackoverflow.com/questions/42365658/custom-marker-in-google-maps-in-android-with-vector-asset-icon#comment122784538_42365658 - Leonid Ustenko
    private fun bitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor? {
        return ContextCompat.getDrawable(context, vectorResId)?.run {
            setBounds(0, 0, intrinsicWidth, intrinsicHeight)
            val bitmap = Bitmap.createBitmap(intrinsicWidth, intrinsicHeight, Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

    private fun createVessel(){
        val polylineOptions: ArrayList<PolylineOptions>?
        val pois: MutableCollection<LatLng>?

        val vesselPos = vessel.getSpaceLatLng()
        if (vesselPos != null) {
            val zoomLevel = 16.0f
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(vesselPos,zoomLevel))

            pois = vessel.storePois()
            pois?.forEach{
                mMap.addMarker(MarkerOptions().position(it).icon(bitmapDescriptorFromVector(applicationContext, R.drawable.poi)))
            }

            polylineOptions = vessel.createPolylines()
            polylineOptions?.forEach{
                val pol = mMap.addPolyline(it)
                polylinesFloor1.add(pol)
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        createVessel()

        //Users can specify their current position by long pressing at a point on the map
        var currentPositionMarker: Marker? = null
        mMap.setOnMapLongClickListener {
            currentPosition = it
            currentPositionMarker?.remove()
            currentPositionMarker = mapManager.addMarker(currentPosition, mMap)
        }
        startTracking()

    }

    private fun startTracking(){

        var stopObserver1 = false
        var stopObserver2 = false

        var azimuth: Double = 330.0
        sensorsVM.azimuthRotationVector.observe(this, Observer{
            Log.i("Azimuth",azimuth.toString())
            azimuth = it
            binding.navigation.rotation = azimuth.toFloat()
        })

        binding.btnMode1.setOnClickListener {
            stopObserver1 = false
            stopObserver2 = true
            mapManager.clearMarkers()

          //every time the value of stepsDetected changes the new position of the object is calculated
            sensorsVM.stepsDetected.observe(this, Observer{ stepCount->
                Log.d("btnmode1",stepCount.toString())
                if (!stopObserver1) {
                    if (stepCount != 0) {
                        val tmp = mapManager.findNewPosition(currentPosition, azimuth)
                        val closestPoint = mapManager.findClosestPoint(tmp, polylinesFloor1)
                        mapManager.addClosestPoint(azimuth,tmp, closestPoint, mMap)
                        currentPosition = tmp
                    } else {
                        val closestPoint =
                            mapManager.findClosestPoint(currentPosition, polylinesFloor1)
                        mapManager.addClosestPoint(azimuth,currentPosition, closestPoint, mMap)
                    }
                }
            })
        }

        binding.btnMode2.setOnClickListener {
            stopObserver1 = true
            stopObserver2 = false
            mapManager.clearMarkers()

            sensorsVM.stepsDetectedSensorsCollector2.observe(this, Observer{ stepCount->
                Log.d("btnmode2",stepCount.toString())
                if(!stopObserver2) {
                    if (stepCount != 0) {
                        val tmp = mapManager.findNewPosition(currentPosition, azimuth)
                        val closestPoint = mapManager.findClosestPoint(tmp, polylinesFloor1)
                        mapManager.addClosestPoint(azimuth,tmp, closestPoint, mMap)
                        currentPosition = tmp
                    } else {
                        val closestPoint =
                            mapManager.findClosestPoint(currentPosition, polylinesFloor1)
                        mapManager.addClosestPoint(azimuth,currentPosition, closestPoint, mMap)
                    }
                }
            })
        }
    }

    private fun checkPermission(){
        var permissions: MutableList<String> = ArrayList()

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED)
            permissions.add(Manifest.permission.ACTIVITY_RECOGNITION)

        if (permissions != null && !permissions.isEmpty())
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), ALL_PERMISSIONS)
    }
}