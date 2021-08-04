/*
 * Anyplace: A free and open Indoor Navigation Service with superb accuracy!
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

import scala.collection.mutable.ListBuffer

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.json.Writes._

object GeoPoint {

	val charMap = "0123456789bcdefghjkmnpqrstuvwxyz"
	val MAX_LAT: Double = 90.0
	val MIN_LAT: Double = -90.0

	val MAX_LONG: Double = 180.0
	val MIN_LONG: Double = -180.0

	def getGeoBoundingBox(geoPoint: GeoPoint, meterDistance: Double): Array[GeoPoint] = {
		getGeoBoundingBox(geoPoint.dlat, geoPoint.dlon, meterDistance)
	}

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

	def getGeoBoundingBoxByRange(geoPoint1: GeoPoint, geoPoint2: GeoPoint): Array[GeoPoint] = {
		getGeoBoundingBoxByRange(geoPoint1.dlat, geoPoint1.dlon, geoPoint2.dlat, geoPoint2.dlon)
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

	def fromGeoHash(hash: String): GeoPoint = {
		require(hash.length > 0)

		var latHigh = MAX_LAT
		var latLow = MIN_LAT
		var lngHigh = MAX_LONG
		var lngLow = MIN_LONG


		var isEven = true
		for (c <- hash) yield {
			val char_mapIndex = charMap.indexOf(c)
			for (j <- 0 to 4) yield {
				(isEven, ((char_mapIndex << j) & 0x0010) != 0) match {
					case (true, true) => lngLow += (lngHigh - lngLow) / 2.0;
					case (true, false) => lngHigh -= (lngHigh - lngLow) / 2.0;
					case (false, true) => latLow += (latHigh - latLow) / 2.0;
					case (false, false) => latHigh -= (latHigh - latLow) / 2.0;
				}
				isEven = !isEven
			}
		}

		val latitude = latHigh - ((latHigh - latLow) / 2.0)
		val longitude = lngHigh - ((lngHigh - lngLow) / 2.0)
		new GeoPoint(latitude, longitude)
	}

	def boundedGeopoints(bottom: GeoPoint, top: GeoPoint, latPrec: Double = .000085, lonPrec: Double = .00017): List[GeoPoint] = {
		val latdiffs = List.tabulate(((top.dlat - bottom.dlat) / latPrec).toInt + 1)(n => bottom.dlat + latPrec * n.toDouble)
		val londiffs = List.tabulate(((top.dlon - bottom.dlon) / lonPrec).toInt + 1)(n => bottom.dlon + lonPrec * n.toDouble)
		latdiffs.flatMap(
			lat => londiffs.map(lon => new GeoPoint(lat, lon))
		)
	}

	def apply(latitude: String, longitude: String): GeoPoint = new GeoPoint(latitude, longitude)

	def unapply(arg: GeoPoint): Option[(String, String)] = Option[(String, String)](arg.lat, arg.lon)
}


class GeoPoint {

	var lat: String = _
	var lon: String = _
	var dlat: Double = 0.0
	var dlon: Double = 0.0

	def this(lat: Double, lon: Double) = {
		this()
		this.lat = java.lang.Double.toString(lat)
		this.lon = java.lang.Double.toString(lon)
		this.dlat = lat
		this.dlon = lon
	}

	def this(lat: String, lon: String) = {
		this()
		try {
			this.dlat = java.lang.Double.parseDouble(lat)
			this.dlon = java.lang.Double.parseDouble(lon)
		} catch {
			case _: Exception =>
				this.dlat = 0.0
				this.dlon = 0.0
		}
		this.lat = lat
		this.lon = lon
	}


	/**
	 * Source code for geohash adopted and adapted from
	 * https://github.com/Solliet/geohash_sisiphus/
	 */
	def asGeohash(prec: Integer): String = {
		require(prec >= 1 && prec <= 12)

		val hash = ListBuffer[Char]()
		val precision: Integer = prec * 5
		var latHigh = GeoPoint.MAX_LAT
		var latLow = GeoPoint.MIN_LAT
		var lngHigh = GeoPoint.MAX_LONG
		var lngLow = GeoPoint.MIN_LONG

		var mid: Double = 0.0
		var isEven = true
		var hashChar = 0;
		for (i <- 1 to precision) yield {
			val coord = if (isEven) dlon else dlat;
			mid = (if (isEven) lngLow + lngHigh else latLow + latHigh) / 2.0
			hashChar = hashChar << 1
			(isEven, coord > mid) match {
				case (true, true) =>
					lngLow = mid
					hashChar |= 0x01
				case (true, false) =>
					lngHigh = mid;
				case (false, true) =>
					latLow = mid
					hashChar |= 0x01
				case (false, false) =>
					latHigh = mid
			}

			if ((i % 5) == 0) {
				hash += GeoPoint.charMap(hashChar)
				hashChar = 0
			}
			isEven = !isEven
		}
		hash.mkString("")
	}

	override def equals(that: Any): Boolean = that match {
		case that: GeoPoint => this.dlat == that.dlat && this.dlon == that.dlon
		case _ => false
	}

	override def toString: String = s"lat[$lat] lon[$lon]"

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

	def getBoundingBox(distance_in_meters: Double): Array[GeoPoint] = GeoPoint.getGeoBoundingBox(this.dlat, this.dlon, distance_in_meters)



}
