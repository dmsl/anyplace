package cy.ac.ucy.cs.anyplace.smas.utils.IMU

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.*
import java.util.ArrayList
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class MapManager {
    private var allMarkers: MutableList<Marker> = ArrayList()

    companion object{
        private const val earthRadius = 6371000
        private const val stepSize = 0.37 //previously 0.74
    }

    fun clearMarkers(){
        if(allMarkers.isNotEmpty()){
            for(marker in allMarkers){
                marker.remove()
            }
            allMarkers.clear()
        }
    }

    fun addMarker(location: LatLng, mMap: GoogleMap): Marker? {
        mMap.animateCamera(CameraUpdateFactory.newLatLng(location))
        val marker = mMap.addMarker(MarkerOptions().position(location).icon(
            BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)))

        if (marker != null) {
            allMarkers.add(marker)
            return marker
        }
        return null
    }

    //Finds the closest point on the route with the use of Map Matching
    fun findClosestPoint(point: LatLng, polylinesArray: ArrayList<Polyline>): LatLng{
        val match = MapMatching()
        return match.find_point(point, polylinesArray)
    }

    //Adds the closest point to the current oldPoint on the map
    fun addClosestPoint(azimuth: Double, oldPoint: LatLng, newPoint: LatLng, mMap: GoogleMap) {
        clearMarkers()

        //Add the point taken from the IMU on the map
        mMap.animateCamera(CameraUpdateFactory.newLatLng(oldPoint))
        val previousMarker = mMap.addMarker(
            MarkerOptions().position(oldPoint).title("IMU position").icon(
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
            )
        )

//        if (azimuth <= 180)
//            previousMarker?.rotation = azimuth.toFloat() + 180
//        else previousMarker?.rotation = azimuth.toFloat() - 180

        if (previousMarker != null)
            allMarkers.add(previousMarker)

        mMap.animateCamera(CameraUpdateFactory.newLatLng(newPoint))
        val currentMarker = mMap.addMarker(
            MarkerOptions().position(newPoint).title("Closest Position").icon(
                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE)
            )
        )

//        if (azimuth <= 180)
//            currentMarker?.rotation = azimuth.toFloat() + 180
//        else currentMarker?.rotation = azimuth.toFloat() - 180

        if (currentMarker != null)
            allMarkers.add(currentMarker)
    }

    //Calculates the new position with the use of Dead Reckoning
    fun findNewPosition(oldPosition: LatLng, azimuth: Double): LatLng{
        val azimuthRad = Math.toRadians(azimuth)

        val Lat = Math.toRadians(oldPosition.latitude)
        val Lng = Math.toRadians(oldPosition.longitude)

        val newLat: Double = asin(sin(Lat) * cos(stepSize / earthRadius) + cos(Lat) * sin(stepSize / earthRadius) * cos(azimuthRad))
        val newLng: Double = Lng + atan2(sin(azimuthRad) * sin(stepSize / earthRadius) * cos(Lat), cos(stepSize / earthRadius) - sin(Lat) * sin(newLat))

        val newLatDegrees = Math.toDegrees(newLat)
        val newLngDegrees = Math.toDegrees(newLng)

        return LatLng(newLatDegrees,newLngDegrees)
    }
}