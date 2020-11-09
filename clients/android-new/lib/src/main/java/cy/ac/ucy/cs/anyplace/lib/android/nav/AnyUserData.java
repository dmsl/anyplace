/*
 * Anyplace: A free and open Indoor Navigation Service with superb accuracy!
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

package cy.ac.ucy.cs.anyplace.lib.android.nav;

import java.util.ArrayList;
import java.util.List;

import android.location.Location;
import android.util.Log;


import cy.ac.ucy.cs.anyplace.lib.android.utils.LogRecord;
import cy.ac.ucy.cs.anyplace.lib.android.utils.GeoPoint;
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceDebug;

public class AnyUserData {
  private static final String TAG = AnyUserData.class.getSimpleName();
	// the selected Floor displayed on map & used when requesting the radio map
	// = floor_number in FloorModel
	private FloorModel selectedFloor = null;
	// the selected building whose floor is displayed on the map
	private BuildingModel selectedBuilding = null;
	// building used for last navigation
	private BuildingModel navBuilding = null;
	// holds all the POIs for the Navigation route
	private List<PoisNav> navPois = null;
	// the position based on ip address
	private GeoPoint positionIP = null;
	// last position estimated by Wifi Tracker
	private GeoPoint positionCoordsWifi = null;
	// last position requested from Google Location API
	private Location positionGPS = null;

	// <BUILDING /FLOOR>
	public String getSelectedFloorNumber() {
		return selectedFloor != null ? selectedFloor.floor_number : null;
	}

	public String getSelectedBuildingId() {
		return selectedBuilding != null ? selectedBuilding.buid : null;
	}

	public BuildingModel getSelectedBuilding() {
		return selectedBuilding;
	}

	public void setSelectedFloor(FloorModel f) {
		selectedFloor = f;
	}

	public void setSelectedBuilding(BuildingModel b) {
		resetPosition();
		selectedBuilding = b;
		selectedFloor = null;
	}

	public boolean isFloorSelected() {
		return selectedFloor != null && selectedFloor.floor_number != null && !selectedFloor.floor_number.equalsIgnoreCase("") && !selectedFloor.floor_number.equalsIgnoreCase("-");
	}

	// <NAVIGATION>
	public boolean isNavBuildingSelected() {
		if (navBuilding == null || selectedBuilding == null)
			return false;
		if (navBuilding.buid != null && selectedBuilding.buid != null) {
			return navBuilding.buid.equalsIgnoreCase(selectedBuilding.buid);
		}
		return false;
	}

	public BuildingModel getNavBuilding() {
		return navBuilding;
	}

	public List<PoisNav> getNavPois() {
		return navPois;
	}

	public void setNavBuilding(BuildingModel b) {
		navBuilding = b;
	}

	public void setNavPois(List<PoisNav> p) {
		navPois = p;
	}

	public void clearNav() {
		if (navPois != null) {
			navPois.clear();
		}
		navBuilding = null;
		navPois = null;
	}

	// <POSITIONING>
	public void setPositionWifi(double lat, double lng) {
		positionCoordsWifi = new GeoPoint(lat, lng);
	}

	public void setLocationGPS(Location loc) {
		positionGPS = loc;
	}

	public void setLocationIP(GeoPoint loc) {
		positionIP = loc;
	}

	public GeoPoint getLatestUserPosition() {
		GeoPoint result = null;

		if (!AnyplaceDebug.LOCK_TO_GPS && positionCoordsWifi != null) {
			result = positionCoordsWifi;
		} else {
			result = getLocationGPSorIP();
		}

		return result;
	}

	public GeoPoint getPositionWifi() {
		return positionCoordsWifi;
	}

	public GeoPoint getLocationGPSorIP() {



		GeoPoint result = null;

		if (positionGPS != null) {
			if (AnyplaceDebug.DEBUG_WIFI) {
				result = fakeGPS();
			} else {
				result = new GeoPoint(positionGPS.getLatitude(), positionGPS.getLongitude());
			}
		} else {
		  Log.d(TAG,"positionGPS is null");
			result = positionIP;
		}
		// if (result == null){
		//   result = fakeGPS();
		//   Log.d(TAG, "The gps location is null since there isnt a gps registered");
        // }
		return result;
	}

	private void resetPosition() {
		positionCoordsWifi = null;
	}

	// </POSITIONING>
	public static GeoPoint fakeGPS() {
		String lat, lon;

		// lat = "35.145245"; // SFC04 - Mall, New Campus, UCY, Nicosia
		// lon = "33.409967";

		// lat = "35.144679"; // SFC02
		// lon = "33.408902";

		// lat = "35.144455"; // FST02
		// lon = "33.410430";

		// lat = "35.14583813397"; // SFC03
		// lon = "33.40938393026";

		// lat = "35.145644"; // SFC01
		// lon = "33.408996";

		// lat = "35.146166"; // Sports Center
		// lon = "33.413210";

		// lat = "35.144977"; // CFT01 New Campus
		// lon = "33.410648";

		// lat = "35.145379"; // FEB01-02
		// lon = "33.411559";

		// lat = "35.145215"; // SFC05
		// lon = "33.408359";

		// lat = "35.145747"; // Anastasios G. Leventis --CHECK
		// lon = "33.410309";

		// lat = "35.145608"; // CTF02
		// lon = "33.412053";

		lat = "35.144521"; // FST01
		lon = "33.411182";

		// lat = "35.154907"; // Filoxenia Conference Center
		// lon = "33.380032";

		// lat = "35.148958"; // KIOS Research Center
		// lon = "33.386422";
		
		return new GeoPoint(lat, lon);
	}

	public static class FakeResults {
		public ArrayList<LogRecord> records;
		public int heading;
	}

	public static FakeResults fakeScan() {
		return fakeFST01FL2();
	}

	private static FakeResults fakeFST01FLB1() {
		FakeResults r = new FakeResults();
		ArrayList<LogRecord> latestScanList = new ArrayList<LogRecord>();

		// 1406741536045 35.14467106162172 33.41082897037268 92.0 Floor B1
		latestScanList.add(new LogRecord("24:b6:57:b4:f6:20", -90));
		latestScanList.add(new LogRecord("00:0b:fd:f3:ab:56", -95));
		latestScanList.add(new LogRecord("00:0b:fd:4a:71:94", -94));
		latestScanList.add(new LogRecord("00:0b:fd:4a:71:ab", -90));
		latestScanList.add(new LogRecord("00:0e:84:4b:0b:ec", -96));
		r.records = latestScanList;
		r.heading = 92;
		return r;
	}

	private static FakeResults fakeFST01FL2() {
		FakeResults r = new FakeResults();
		ArrayList<LogRecord> latestScanList = new ArrayList<LogRecord>();

		// [35.144137278765335,33.411331214010715,"1406475896542"] Floor 2
		latestScanList.add(new LogRecord("00:0b:fd:4a:71:ab", -85));
		latestScanList.add(new LogRecord("00:0b:fd:4a:71:ce", -58));
		latestScanList.add(new LogRecord("00:0b:fd:4a:71:d6", -85));
		latestScanList.add(new LogRecord("00:0e:84:4b:0b:86", -88));
		latestScanList.add(new LogRecord("00:0e:84:4b:0b:aa", -88));
		latestScanList.add(new LogRecord("00:0e:84:4b:0b:e8", -88));
		latestScanList.add(new LogRecord("00:0e:84:4b:0b:ec", -70));
		latestScanList.add(new LogRecord("00:0e:84:4b:0b:f6", -88));
		latestScanList.add(new LogRecord("04:a1:51:a3:13:25", -79));
		latestScanList.add(new LogRecord("24:b6:57:7b:e8:a0", -85));
		latestScanList.add(new LogRecord("24:b6:57:7b:e9:60", -85));
		latestScanList.add(new LogRecord("d4:d7:48:b6:3b:80", -82));
		latestScanList.add(new LogRecord("d4:d7:48:b8:2c:70", -85));
		latestScanList.add(new LogRecord("d4:d7:48:b8:2f:a0", -85));
		r.records = latestScanList;
		r.heading = 151;
		return r;
	}

	private static FakeResults fakeFEB0102FLB1() {
		FakeResults r = new FakeResults();
		ArrayList<LogRecord> latestScanList = new ArrayList<LogRecord>();

		// 35.145360834385826, 33.41167017817497, 180

		latestScanList.add(new LogRecord("00:3a:98:2a:62:20", -85));
		latestScanList.add(new LogRecord("00:3a:98:2a:62:21", -91));
		latestScanList.add(new LogRecord("00:3a:98:2a:62:22", -88));
		latestScanList.add(new LogRecord("00:3a:98:2a:59:61", -85));
		latestScanList.add(new LogRecord("00:3a:98:2a:59:62", -82));
		latestScanList.add(new LogRecord("00:3a:98:2a:5a:80", -91));
		latestScanList.add(new LogRecord("00:3a:98:2a:5a:81", -91));
		latestScanList.add(new LogRecord("00:3a:98:2a:5a:82", -91));
		latestScanList.add(new LogRecord("00:3a:98:2a:68:81", -91));
		latestScanList.add(new LogRecord("00:3a:98:2a:68:80", -91));
		latestScanList.add(new LogRecord("00:3a:98:2a:5a:60", -70));
		latestScanList.add(new LogRecord("00:3a:98:2a:5a:61", -76));
		latestScanList.add(new LogRecord("00:3a:98:2a:5a:62", -70));
		latestScanList.add(new LogRecord("00:3a:98:2a:68:82", -91));
		latestScanList.add(new LogRecord("00:3a:98:2a:66:61", -67));
		latestScanList.add(new LogRecord("00:3a:98:2a:66:60", -73));
		latestScanList.add(new LogRecord("00:3a:98:2a:66:62", -67));
		latestScanList.add(new LogRecord("00:3a:98:2a:6b:e2", -91));
		latestScanList.add(new LogRecord("00:3a:98:2a:63:c2", -91));

		r.records = latestScanList;
		r.heading = 180;
		return r;
	}
}
