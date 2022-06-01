/*Author: Ioannis Ioannides*/

package cy.ac.ucy.cs.anyplace.smas.utils.IMU

import android.location.Location
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Polyline
import java.lang.Float.POSITIVE_INFINITY
import java.lang.Math.*

class MapMatching {

    fun toRad(deg: Double) :Double{
        return deg * PI / 180
    }

    fun toDeg(rad: Double) :Double{
        return rad * 180 / PI
    }

    fun splitPathIntoPoints(source: LatLng, destination: LatLng): List<LatLng> {
        var distance: Float = findDistance(source, destination)
        val splitPoints: MutableList<LatLng> = ArrayList()
        splitPoints.add(source)
        splitPoints.add(destination)
        while (distance > 1) {
            val polypathSize = splitPoints.size
            val tempPoints: MutableList<LatLng> = ArrayList()
            tempPoints.addAll(splitPoints)
            var injectionIndex = 1
            for (i in 0 until polypathSize - 1) {
                val a1 = tempPoints[i]
                val a2 = tempPoints[i + 1]
                splitPoints.add(injectionIndex, findMidPoint(a1, a2))
                injectionIndex += 2
            }
            distance = findDistance(splitPoints[0], splitPoints[1])
        }
        Log.d("TAG", splitPoints.size.toString())
        return splitPoints
    }

    fun findMidPoint(source: LatLng, destination: LatLng): LatLng {
        val x1: Double = toRad(source.latitude)
        val y1: Double = toRad(source.longitude)
        val x2: Double = toRad(destination.latitude)
        val y2: Double = toRad(destination.longitude)
        val Bx = cos(x2) * cos(y2 - y1)
        val By = cos(x2) * sin(y2 - y1)
        val x3: Double = toDeg(atan2(sin(x1) + sin(x2), sqrt((cos(x1) + Bx) * (cos(x1) + Bx) + By * By)))
        var y3 = y1 + atan2(By, cos(x1) + Bx)
        y3 = toDeg((y3 + 540) % 360 - 180)
        return LatLng(x3, y3)
    }

    fun findDistance(source: LatLng, destination: LatLng): Float {
        val srcLoc = Location("srcLoc")
        srcLoc.latitude = source.latitude
        srcLoc.longitude = source.longitude
        val destLoc = Location("destLoc")
        destLoc.latitude = destination.latitude
        destLoc.longitude = destination.longitude
        return srcLoc.distanceTo(destLoc)
    }

    fun snapToArray(currentLocation: LatLng, line: List<LatLng>) :LatLng{
        var snapped = line[0]
        var dist = findDistance(currentLocation, snapped)
        for(i in 1 until line.size-1){
            val newdist = findDistance(currentLocation, line[i])
            if(newdist<dist) {
                dist = newdist
                snapped = line[i]
            }
            else{
                break
            }
        }
        return snapped
    }

    /* This splits each polyline into 4 equal lines and later finds the best match in the best line */
    fun find_point(point: LatLng, array: ArrayList<Polyline>) : LatLng{
        var prev = LatLng(0.0, 0.0)
        var next = LatLng(0.0, 0.0)
        var dist = POSITIVE_INFINITY

        var prev2 = LatLng(0.0, 0.0)
        var next2 = LatLng(0.0, 0.0)

        for(i in 0 until array.size) {
//            val tmp = array[i].points // cannot access through any thread other than the main one (array[i] is accesible, array[i].points is not)

            var temp_dist = findDistance(array[i].points.first(), point)
            var mid = findMidPoint(array[i].points.first(), array[i].points.last())
            var quart1 = findMidPoint(array[i].points.first(), mid)
            var quart2 = findMidPoint(mid, array[i].points.last())
            if(temp_dist<=dist){
                if(temp_dist<dist){
                    dist = temp_dist
                    prev = array[i].points.first()
                    next = quart1
                }else{ /////// temp_dist==dist
                    prev2 = array[i].points.first()
                    next2 = quart1
                }
            }
            temp_dist = findDistance(array[i].points.last(), point)
            if(temp_dist<=dist) {
                if (temp_dist < dist) {
                    dist = temp_dist
                    prev = quart2
                    next = array[i].points.last()
                }else{
                    prev2 = quart2
                    next2 = array[i].points.last()
                }
            }
            temp_dist = findDistance(mid, point)
            if(temp_dist<=dist) {
                if (temp_dist < dist) {
                    dist = temp_dist
                    prev = quart1
                    next = quart2
                }else{
                    prev2 = quart1
                    next2 = quart2
                }
            }

            temp_dist = findDistance(quart1, point)
            if(temp_dist<=dist) {
                if (temp_dist < dist) {
                    dist = temp_dist
                    prev = array[i].points.first()
                    next = mid
                }else{
                    prev2 = array[i].points.first()
                    next2 = mid
                }
            }

            temp_dist = findDistance(quart2, point)
            if(temp_dist<=dist) {
                if (temp_dist < dist) {
                    dist = temp_dist
                    prev = mid
                    next = array[i].points.last()
                }else{
                    prev2 = mid
                    next2 = array[i].points.last()
                }
            }
        }

        var new_point: LatLng
        if(prev==prev2 && next==next2){
            val splitPoints = splitPathIntoPoints(prev, next)
            new_point = snapToArray(point, splitPoints)
        }else{
            var splitPoints = splitPathIntoPoints(prev, next)
            val new_point1 = snapToArray(point, splitPoints)
            splitPoints = splitPathIntoPoints(prev2, next2)
            val new_point2 = snapToArray(point, splitPoints)
            val dist1 = findDistance(point, new_point1)
            val dist2 = findDistance(point, new_point2)
            if(dist1>dist2)
                new_point = new_point2
            else
                new_point = new_point1

        }

//        googleMap.addMarker(MarkerOptions().position(prev).title("prev").icon(
//                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))
//        googleMap.addMarker(MarkerOptions().position(next).title("next").icon(
//                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))
//        googleMap.addMarker(MarkerOptions().position(prev2).title("prev2").icon(
//                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))
//        googleMap.addMarker(MarkerOptions().position(next2).title("next2").icon(
//                BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)))

        return new_point
    }

}