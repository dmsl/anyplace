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

package cy.ac.ucy.cs.anyplace.lib.android;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.File;

public class AnyplaceDebug {

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

    public static final boolean DEBUG_LOCATION = false ;


	private static String getServerIPAddress(Context ctx){


		if (!DEBUG_URL) {
			Context c = ctx;
			SharedPreferences preferences = c.getSharedPreferences("Anyplace_Preferences", c.MODE_PRIVATE);
			return preferences.getString("server_ip_address", "ap.cs.ucy.ac.cy" ).trim();
		} else {
			return "http://192.168.1.2:9000";
		}
	}
}
