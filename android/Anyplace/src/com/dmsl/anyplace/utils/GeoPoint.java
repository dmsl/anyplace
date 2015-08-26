/*
* AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Author(s): Lambros Petrou
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

package com.dmsl.anyplace.utils;

public class GeoPoint {

	public String lat;
	public String lng;
	public double dlat;
	public double dlon;

	public GeoPoint() {

	}

	public GeoPoint(double lat, double lon) {
		this.lat = Double.toString(lat);
		this.lng = Double.toString(lon);
		this.dlat = lat;
		this.dlon = lon;
	}

	public GeoPoint(String lat, String lon) {
		try {
			this.dlat = Double.parseDouble(lat);
			this.dlon = Double.parseDouble(lon);
		} catch (Exception e) {
			this.dlat = 0.0;
			this.dlon = 0.0;
		}
		this.lat = lat;
		this.lng = lon;
	}

	public String toString() {
		return "lat[" + lat + "] lon[" + lng + "]";
	}

	public static GeoPoint[] getGeoBoundingBox(double latitude, double longitude, double distance_in_meters) {
		double EARTH_RADIUS = 6378.137; // meters

		double lat = Math.toRadians(latitude);
		double lon = Math.toRadians(longitude);
		double R = EARTH_RADIUS;
		double parallelR = R * Math.cos(lat);
		double distKM = distance_in_meters / 1000.0;

		double lat_min = Math.toDegrees(lat - distKM / R);
		double lat_max = Math.toDegrees(lat + distKM / R);
		double lon_min = Math.toDegrees(lon - distKM / parallelR);
		double lon_max = Math.toDegrees(lon + distKM / parallelR);

		GeoPoint bbox[] = new GeoPoint[] { new GeoPoint(lat_min, lon_min), new GeoPoint(lat_max, lon_max) };

		return bbox;
	}

	/**
	 *
	 * @param distance
	 *            in meters
	 * @param bearing
	 *            in degrees
	 * @return
	 */
	public GeoPoint getNewPointFromDistanceBearing(double distance, double bearing) {

		double R = 6378.14; // Radius of the Earth
		double bearRad = Math.toRadians(bearing);
		double distKM = distance / 1000;

		double lat1 = Math.toRadians(Double.parseDouble(lat)); // Current lat
																// point
																// converted to
																// radians
		double lon1 = Math.toRadians(Double.parseDouble(lng)); // Current long
																// point
																// converted to
																// radians

		double lat2 = Math.asin(Math.sin(lat1) * Math.cos(distKM / R) + Math.cos(lat1) * Math.sin(distKM / R) * Math.cos(bearRad));

		double lon2 = lon1 + Math.atan2(Math.sin(bearRad) * Math.sin(distKM / R) * Math.cos(lat1), Math.cos(distKM / R) - Math.sin(lat1) * Math.sin(lat2));

		lat2 = Math.toDegrees(lat2);
		lon2 = Math.toDegrees(lon2);

		return new GeoPoint(lat2, lon2);
	}

	/**
	 * Returns the Euclidean distance between two LatLng points
	 * 
	 * @param lon1
	 * @param lat1
	 * @param lon2
	 * @param lat2
	 * @param unit
	 *            'K' for kilometers and 'M' for miles '' for meters
	 * @return
	 */
	public static double getDistanceBetweenPoints(double lon1, double lat1, double lon2, double lat2, String unit) {
		double theta = lon1 - lon2;
		double dist = Math.sin(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2)) + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(Math.toRadians(theta));
		dist = Math.acos(dist);
		dist = Math.toDegrees(dist);
		dist = dist * 60 * 1.1515;
		if (unit.equals("K")) {
			dist = dist * 1.609344;
		} else if (unit.equals("M")) {
			dist = dist * 0.8684;
		} else {
			dist = dist * 1609.344;
		}
		return (dist);
	}

}
