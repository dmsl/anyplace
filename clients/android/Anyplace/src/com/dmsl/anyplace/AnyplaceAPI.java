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

package com.dmsl.anyplace;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class AnyplaceAPI {

	public final static String FLURRY_APIKEY = "73R5667PJ6MRB5ZB6D3P";
	public final static Boolean FLURRY_ENABLE = false;
	public final static Boolean FLOOR_SELECTOR = true;

	// Lock Location to GPS
	public final static Boolean LOCK_TO_GPS = false;
	// Show Debug Messages
	public final static Boolean DEBUG_MESSAGES = false;
	// Wifi and GPS Data
	public final static Boolean DEBUG_WIFI = false;
	// API URLS
	public final static Boolean DEBUG_URL = false;

	// Load All Building's Floors and Radiomaps
	public final static Boolean PLAY_STORE = true;

	// private static String server ="http://thinklambros.in.cs.ucy.ac.cy:9000";
	// private static String server ="http://anyplace.in.cs.ucy.ac.cy";
	// private static String server = "https://anyplace.rayzit.com";

	private static URL getServerIPAddress() throws MalformedURLException {
		if (!DEBUG_URL) {
			Context c = MyApplication.getAppContext();
			SharedPreferences preferences = c.getSharedPreferences(UnifiedNavigationActivity.SHARED_PREFS_ANYPLACE, c.MODE_PRIVATE);
			return new URL(preferences.getString("server_ip_address", c.getString(R.string.default_server_ip_address)));
		} else {
			return new URL("http://192.168.1.2:9000");
		}
	}

	private final static String PREDICT_FLOOR_ALGO1 = "/anyplace/position/predictFloorAlgo1";
	private final static String PREDICT_FLOOR_ALGO2 = "/anyplace/position/predictFloorAlgo2";

	private final static String RADIO_DOWNLOAD_XY = "/anyplace/position/radio_download_floor";
	private final static String RADIO_DOWNLOAD_BUID = "/anyplace/position/radio_by_building_floor";
	private final static String RADIO_UPLOAD_URL_API = "/anyplace/position/radio_upload";

	private final static String NAV_ROUTE_URL_API = "/anyplace/navigation/route";
	private final static String NAV_ROUTE_XY_URL_API = "/anyplace/navigation/route_xy";

	private final static String FLOOR_PLAN_DOWNLOAD = "/anyplace/floorplans";
	private final static String FLOOR_TILES_ZIP_DOWNLOAD = "/anyplace/floortiles/zip";

	public static String predictFloorAlgo1() throws MalformedURLException {
		return new URL(getServerIPAddress(), PREDICT_FLOOR_ALGO1).toString();
	}

	public static String predictFloorAlgo2() throws MalformedURLException {
		return new URL(getServerIPAddress(), PREDICT_FLOOR_ALGO2).toString();
	}

	public static String getRadioDownloadBuid() throws MalformedURLException {
		return new URL(getServerIPAddress() , RADIO_DOWNLOAD_BUID).toString();
	}

	public static String getRadioDownloadXY() throws MalformedURLException {
		return new URL(getServerIPAddress() , RADIO_DOWNLOAD_XY).toString();
	}

	public static String getRadioUploadUrl() throws MalformedURLException {
		return new URL(getServerIPAddress() , RADIO_UPLOAD_URL_API).toString();
	}

	private static String getNavRouteUrl() throws MalformedURLException {
		return new URL(getServerIPAddress() , NAV_ROUTE_URL_API).toString();
	}

	public static String getNavRouteXYUrl() throws MalformedURLException {
		return new URL(getServerIPAddress() , NAV_ROUTE_XY_URL_API).toString();
	}

	// --------------Select Building Activity--------------------------

	public static String getFetchBuildingsUrl() throws MalformedURLException {
		return new URL(getServerIPAddress() , "/anyplace/mapping/building/all").toString();
	}

	public static String getFetchBuildingsByBuidUrl() throws MalformedURLException {
		return new URL(getServerIPAddress() , "/anyplace/navigation/building/id").toString();
	}

	public static String getFetchFloorsByBuidUrl() throws MalformedURLException {
		return new URL(getServerIPAddress() , "/anyplace/mapping/floor/all").toString();
	}

	public static String getServeFloorTilesZipUrl(String buid, String floor_number) throws MalformedURLException {
		return new URL(getServerIPAddress() , FLOOR_TILES_ZIP_DOWNLOAD + File.separatorChar + buid + File.separatorChar + floor_number).toString();
	}

	// -------------- Near coordinates ----------------------------------

	private static String getFetchBuildingsCoordinatesUrl() throws MalformedURLException {
		return new URL(getServerIPAddress() , "/anyplace/mapping/building/coordinates").toString();
	}

	private static String getServeFloorPlanUrl(String buid, String floor_number) throws MalformedURLException {
		return new URL(getServerIPAddress() , FLOOR_PLAN_DOWNLOAD + File.separatorChar + buid + File.separatorChar + floor_number).toString();
	}

	// ----------------------------------------------------------------

	// --------------POIS Api--------------------------

	public static String getFetchPoisByBuidUrl() throws MalformedURLException {
		return new URL(getServerIPAddress() , "/anyplace/mapping/pois/all_building").toString();
	}

	public static String getFetchPoisByBuidFloorUrl() throws MalformedURLException {
		return new URL(getServerIPAddress() , "/anyplace/mapping/pois/all_floor").toString();
	}

	public static String getFetchPoisByPuidUrl() throws MalformedURLException {
		return new URL(getServerIPAddress() , "/anyplace/navigation/pois/id").toString();
	}

	// ------------------------------------------------

}
