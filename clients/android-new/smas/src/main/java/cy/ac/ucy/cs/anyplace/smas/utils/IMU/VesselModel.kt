package cy.ac.ucy.cs.anyplace.smas.utils.IMU

import android.content.Context
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import cy.ac.ucy.cs.anyplace.smas.utils.SmasAssetReader

class VesselModel(val context: Context) {

    protected val assetReader by lazy { SmasAssetReader(context) }
    private var poisLatLng: MutableMap<String,LatLng> = HashMap()

    fun getSpaceLatLng(): LatLng?{
        val vessel = assetReader.getSpace()
        return if (vessel != null)
            LatLng(vessel.coordinatesLat.toDouble(), vessel.coordinatesLon.toDouble())
        else null
    }

    fun storePois(): MutableCollection<LatLng>?{

        val connectors = assetReader.getPois()

        if (connectors?.pois != null) {
            connectors.pois.forEach {
                poisLatLng[it.puid] =
                    LatLng(it.coordinatesLat.toDouble(), it.coordinatesLon.toDouble())
            }
            return poisLatLng.values
        }
        return null
    }

    fun createPolylines(): ArrayList<PolylineOptions>?{

        val polylineOptions = ArrayList<PolylineOptions>()
        val vertices = assetReader.getConnections()

        if (vertices?.connections != null){
            vertices.connections.forEach {
                val poi_a = it.poisA
                val poi_b = it.poisB
                val poi_a_latlng = poisLatLng[poi_a]
                val poi_b_latlng = poisLatLng[poi_b]

                polylineOptions.add(PolylineOptions().add(poi_a_latlng).add(poi_b_latlng))
            }
            return polylineOptions
        }
        return null
    }

}