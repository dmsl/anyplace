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

import java.io.File;

public class AnyplaceAPI {

	public final static String FLURRY_APIKEY = "insert_flurry_key";
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
	private static String server = "https://anyplace.rayzit.com";
	private static String serverTesting;

	static {
		if (!DEBUG_URL) {
			serverTesting = server;
		} else {
			serverTesting = "http://192.168.1.2:9000";
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

	public static String predictFloorAlgo1() {
		return server + PREDICT_FLOOR_ALGO1;
	}

	public static String predictFloorAlgo2() {
		return server + PREDICT_FLOOR_ALGO2;
	}

	public static String getRadioDownloadBuid() {
		return server + RADIO_DOWNLOAD_BUID;
	}

	public static String getRadioDownloadXY() {
		return server + RADIO_DOWNLOAD_XY;
	}

	public static String getRadioUploadUrl() {
		return serverTesting + RADIO_UPLOAD_URL_API;
	}

	private static String getNavRouteUrl() {
		return server + NAV_ROUTE_URL_API;
	}

	public static String getNavRouteXYUrl() {
		return server + NAV_ROUTE_XY_URL_API;
	}

	// --------------Select Building Activity--------------------------
	public static String getFetchBuildingsUrl() {
		return server + "/anyplace/mapping/building/all";
	}

	public static String getFetchBuildingsByBuidUrl() {
		return server + "/anyplace/navigation/building/id";
	}

	public static String getFetchFloorsByBuidUrl() {
		return server + "/anyplace/mapping/floor/all";
	}

	public static String getServeFloorTilesZipUrl(String buid, String floor_number) {
		return server + FLOOR_TILES_ZIP_DOWNLOAD + File.separatorChar + buid + File.separatorChar + floor_number;
	}

	// Near coordinates
	private static String getFetchBuildingsCoordinatesUrl() {
		return server + "/anyplace/mapping/building/coordinates";
	}

	private static String getServeFloorPlanUrl(String buid, String floor_number) {
		return server + FLOOR_PLAN_DOWNLOAD + File.separatorChar + buid + File.separatorChar + floor_number;
	}

	// ----------------------------------------------------------------

	// --------------POIS Api--------------------------
	public static String getFetchPoisByBuidUrl() {
		return server + "/anyplace/mapping/pois/all_building";
	}

	public static String getFetchPoisByBuidFloorUrl() {
		return server + "/anyplace/mapping/pois/all_floor";
	}

	public static String getFetchPoisByPuidUrl() {
		return server + "/anyplace/navigation/pois/id";
	}
	// ------------------------------------------------

}
