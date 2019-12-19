/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Constantinos Costa, Kyriakos Georgiou, Lambros Petrou
 *
 * Supervisor: Demetrios Zeinalipour-Yazti
 *
 * URL: https://anyplace.cs.ucy.ac.cy
 * Contact: anyplace@cs.ucy.ac.cy
 *
 * Copyright (c) 2016, Data Management Systems Lab (DMSL), University of Cyprus.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 */
package utils

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Writes._

object GeoPoint {

    def getGeoBoundingBox(latitude: Double, longitude: Double, distance_in_meters: Double): Array[GeoPoint] = {
        val EARTH_RADIUS = 6378.137
        val lat = Math.toRadians(latitude)
        val lon = Math.toRadians(longitude)
        val R = EARTH_RADIUS
        val parallelR = R * Math.cos(lat)
        val distKM = distance_in_meters / 1000.0
        val lat_min = Math.toDegrees(lat - distKM / R)
        val lat_max = Math.toDegrees(lat + distKM / R)
        val lon_min = Math.toDegrees(lon - distKM / parallelR)
        val lon_max = Math.toDegrees(lon + distKM / parallelR)
        val bbox = Array(new GeoPoint(lat_min, lon_min), new GeoPoint(lat_max, lon_max))
        bbox
    }

	implicit val geoPointReads = (
		(__ \ "latitude").read[String] and
			(__ \ "longitude").read[String]
		)(GeoPoint.apply _)

	implicit val geoPointWrites = (
		(__ \ "latitude").write[String] and
			(__ \ "longitude").write[String]
		)(unlift(GeoPoint.unapply))

	def getGeoBoundingBoxByRange(latitude1: Double, longitude1: Double, latitude2: Double, longitude2: Double): Array[GeoPoint] = {
        val lat1 = Math.toRadians(latitude1)
        val lon1 = Math.toRadians(longitude1)
        val lat2 = Math.toRadians(latitude2)
        val lon2 = Math.toRadians(longitude2)
        val lat_min = Math.toDegrees(lat2)
        val lat_max = Math.toDegrees(lat1)
        val lon_min = Math.toDegrees(lon2)
        val lon_max = Math.toDegrees(lon1)
        val bbox = Array(new GeoPoint(lat_min, lon_min), new GeoPoint(lat_max, lon_max))
        bbox
    }


    def getDistanceBetweenPoints(lon1: Double,
                                 lat1: Double,
                                 lon2: Double,
                                 lat2: Double,
                                 unit: String): Double = {
        val theta = lon1 - lon2
        var dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) +
          Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.cos(Math.toRadians(theta))
        dist = Math.acos(dist)
        dist = Math.toDegrees(dist)
        dist = dist * 60 * 1.1515
        if (unit == "K") {
            dist = dist * 1.609344
        } else if (unit == "M") {
            dist = dist * 0.8684
        }
        (dist)
    }
}

class GeoPoint {

    var lat: String = _

    var lon: String = _

    var dlat: Double = 0.0

    var dlon: Double = 0.0

    def this(lat: Double, lon: Double) {
        this()
        this.lat = java.lang.Double.toString(lat)
        this.lon = java.lang.Double.toString(lon)
        this.dlat = lat
        this.dlon = lon
    }

    def this(lat: String, lon: String) {
        this()
        try {
            this.dlat = java.lang.Double.parseDouble(lat)
            this.dlon = java.lang.Double.parseDouble(lon)
        } catch {
            case e: Exception => {
                this.dlat = 0.0
                this.dlon = 0.0
            }
        }
        this.lat = lat
        this.lon = lon
    }

    override def toString(): String = "lat[" + lat + "] lon[" + lon + "]"

    def getNewPointFromDistanceBearing(distance: Double, bearing: Double): GeoPoint = {
        val R = 6378.14
        val bearRad = Math.toRadians(bearing)
        val distKM = distance / 1000
        val lat1 = Math.toRadians(java.lang.Double.parseDouble(lat))
        val lon1 = Math.toRadians(java.lang.Double.parseDouble(lon))
        var lat2 = Math.asin(Math.sin(lat1) * Math.cos(distKM / R) +
          Math.cos(lat1) * Math.sin(distKM / R) * Math.cos(bearRad))
        var lon2 = lon1 +
          Math.atan2(Math.sin(bearRad) * Math.sin(distKM / R) * Math.cos(lat1), Math.cos(distKM / R) - Math.sin(lat1) * Math.sin(lat2))
        lat2 = Math.toDegrees(lat2)
        lon2 = Math.toDegrees(lon2)
        new GeoPoint(lat2, lon2)
    }
}
