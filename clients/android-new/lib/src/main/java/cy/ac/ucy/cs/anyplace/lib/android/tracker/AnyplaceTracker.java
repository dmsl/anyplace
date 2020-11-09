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

package cy.ac.ucy.cs.anyplace.lib.android.tracker;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import com.google.android.gms.maps.model.LatLng;
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceDebug;
import cy.ac.ucy.cs.anyplace.lib.android.sensors.SensorsMain;
import cy.ac.ucy.cs.anyplace.lib.android.wifi.SimpleWifiManager;
import cy.ac.ucy.cs.anyplace.lib.android.wifi.WifiReceiver;
import cy.ac.ucy.cs.anyplace.lib.LogRecord;
//import cy.ac.ucy.cs.anyplace.lib.android.utils.LogRecord;
import cy.ac.ucy.cs.anyplace.lib.RadioMap;

/**
 * The main tracker component of Anyplace Navigator. Detects changes in WiFi
 * Scan results and Heading using the Positioning object
 * 
 * Additionally it is the object that runs the algorithm that calculates the
 * position according to the RadioMap file provided. It notifies you for each
 * position calculated or if there is an error while calculating it.
 *
 */
public class AnyplaceTracker {

	// Wifi
	public interface WifiResultsAnyplaceTrackerListener {
		public void onNewWifiResults(int aps);
	}

	private List<WifiResultsAnyplaceTrackerListener> wrlisteners = new ArrayList<WifiResultsAnyplaceTrackerListener>(1);

	public void addListener(WifiResultsAnyplaceTrackerListener list) {
		wrlisteners.add(list);
	}

	public void removeListener(WifiResultsAnyplaceTrackerListener list) {
		wrlisteners.remove(list);
	}

	private void triggerWiFiResultsListeners(int aps) {
		for (WifiResultsAnyplaceTrackerListener l : wrlisteners) {
			l.onNewWifiResults(aps);
		}
	}

	// Location
	public interface TrackedLocAnyplaceTrackerListener {
		public void onNewLocation(LatLng pos);
	}

	private List<TrackedLocAnyplaceTrackerListener> tllisteners = new ArrayList<TrackedLocAnyplaceTrackerListener>(1);

	public void addListener(TrackedLocAnyplaceTrackerListener list) {
		tllisteners.add(list);
	}

	public void removeListener(TrackedLocAnyplaceTrackerListener list) {
		tllisteners.remove(list);
	}

	private void triggerTrackedLocListeners(LatLng pos) {
		for (TrackedLocAnyplaceTrackerListener l : tllisteners) {
			l.onNewLocation(pos);
		}
	}

	// Error
	public interface ErrorAnyplaceTrackerListener {
		public void onTrackerError(String msg);
	}

	private List<ErrorAnyplaceTrackerListener> errorlisteners = new ArrayList<ErrorAnyplaceTrackerListener>(1);

	public void addListener(ErrorAnyplaceTrackerListener list) {
		errorlisteners.add(list);
	}

	public void removeListener(ErrorAnyplaceTrackerListener list) {
		errorlisteners.remove(list);
	}

	private void triggerErrorListeners(String msg) {
		for (ErrorAnyplaceTrackerListener l : errorlisteners) {
			l.onTrackerError(msg);
		}
	}

	// Flag to show if there is an ongoing progress
	private Boolean inProgress = false;
	private ExecutorService executorService;
	private Future future;
	private boolean trackMe;
	private boolean trackResume;
	// The latest scan list of APs and heading
	private ArrayList<LogRecord> latestScanList = new ArrayList<LogRecord>();
	private float RAWheading;

	// WiFi manager
	private SimpleWifiManager wifi;
	// WiFi Receiver
	private WifiReceiver receiverWifi;
	private SensorsMain positioning;

	// Algorithm
	private String radiomap_file;
	private byte algoChoice;
	private RadioMap rm;
	// private com.cy.wifi.algorithms.Algorithms algo; //RBF

	// flags
	boolean isWifiOn = false;

	public AnyplaceTracker(SensorsMain positioning, Context ctx) {
		this.positioning = positioning;
		// WiFi manager to manage scans
		wifi = SimpleWifiManager.getInstance(ctx);
		// Create new receiver to get broadcasts
		receiverWifi = new SimpleWifiReceiver();
		trackMe = false;
		trackResume = false;

		executorService = Executors.newSingleThreadExecutor();
	}

	synchronized private boolean setProgress() {
		if (inProgress == true) {
			return false;
		}
		inProgress = true;
		return true;
	}

	synchronized private boolean unsetProgress() {
		inProgress = false;
		return true;
	}

	private boolean waitFindMe() {
		if (future != null && future.isDone() == false) {
			try {
				future.get();
			} catch (InterruptedException e) {
				return false;
			} catch (ExecutionException e) {
				return false;
			}
		}

		return true;
	}

	// Turn On Tracker
	public boolean trackOn() {

		if (!waitFindMe()) {
			triggerErrorListeners("Cannot start Tracker.");
			return false;
		}

		// Check that radiomap file is readable
		File file = new File(radiomap_file);

		if (!file.exists()) {
			triggerErrorListeners("Please download the required radiomap file.");
			return false;
		} else if (!file.canRead()) {
			triggerErrorListeners("Radiomap file is not readable.");
			return false;
		}

		resumeTracking();

		// RBF
		/*
		 * if (algoChoice == 0) { rm = null; algo = new
		 * com.cy.wifi.algorithms.Algorithms(this.radiomap_file); } else { algo
		 * = null;
		 */

		try {
			rm = new RadioMap(new File(radiomap_file));
		} catch (Exception e) {
			triggerErrorListeners("Error while reading radio map.\nDownload new Radio Map and try again");
			return false;
		}

		// }

		trackMe = true;
		return true;
	}

	// Turn off Tracker
	public void trackOff() {

		waitFindMe();

		trackMe = false;

		pauseTracking();

		rm = null;
	}

	// used in Activity Pause
	public void pauseTracking() {
		if (isWifiOn) {
			isWifiOn = false;
			wifi.unregisterScan(receiverWifi);
		}

		trackResume = false;

	}

	// Used on Activity Resume
	public void resumeTracking() {
		if (!isWifiOn) {
			isWifiOn = true;
			wifi.registerScan(receiverWifi);
		}

		trackResume = true;
	}

	public boolean isTrackingOn() {
		// return trackMe.get();
		return trackMe;
	}

	// Called after select place
	public void setRadiomapFile(String radiomap_file) {
		this.radiomap_file = radiomap_file;
	}

	public void setAlgorithm(String name) {

		byte algoValue = 1;

		// if (name.equals("RBF")) {
		// algoValue = 0;
		// } else
		if (name.equals("KNN")) {
			algoValue = 1;
		} else if (name.equals("WKNN")) {
			algoValue = 2;
		} else if (name.equals("MAP")) {
			algoValue = 3;
		} else if (name.equals("MMSE")) {
			algoValue = 4;
		}

		if (algoValue == 0) {
			// Close and Restart Tarcker
			if (trackMe == true && algoChoice != 0) {
				trackOff();
				algoChoice = algoValue;
				trackOn();
			} else {
				algoChoice = algoValue;
			}
		} else {
			// Close and Restart Tarcker
			if (trackMe == true && algoChoice == 0) {
				trackOff();
				algoChoice = algoValue;
				trackOn();
			} else {
				algoChoice = algoValue;
			}
		}

	}

	/**
	 * Starts the appropriate positioning algorithm
	 * */
	private boolean findMe() {
		if (!setProgress()) {
			return false;
		}
		try {
			// long startTime = System.currentTimeMillis();
			if (latestScanList.isEmpty()) {
				triggerErrorListeners("No Access Point Received.\nWait for a scan first and try again.");
				return false;
			}

			// if (algoChoice == 0) {
			// if (!calculateRBFPosition()) {
			// triggerErrorListeners("Can't find location. Check that radio map file refers to the same area.");
			// return false;
			// }
			// } else {
			String calculatedLocation = cy.ac.ucy.cs.anyplace.lib.Algorithms.ProcessingAlgorithms(latestScanList, rm, algoChoice);

			if (calculatedLocation == null) {
				triggerErrorListeners("Can't find location. Check that radio map file refers to the same area.");
			} else {
				String[] temp = calculatedLocation.split(" ");
				LatLng trackedPosition = new LatLng(Double.parseDouble(temp[0]), Double.parseDouble(temp[1]));			
				triggerTrackedLocListeners(trackedPosition);
			}

			// }

			// System.err.println("Elapsed Time: " + (System.currentTimeMillis()
			// - startTime));
			return true;

		} catch (Exception ex) {
			triggerErrorListeners("Tracker Exception" + ex.getMessage());
			return false;
		} finally {
			unsetProgress();
		}

	}

	/*
	 * private boolean calculateRBFPosition() {
	 * 
	 * int degrees = 360; int num_orientations = 4; int range = degrees /
	 * num_orientations; float deviation = range / 2;
	 * 
	 * int heading = (int) (((Math.round(RAWheading + deviation) % degrees) /
	 * range) % num_orientations) * range; boolean result =
	 * algo.ProcessingAlgorithm(latestScanList, heading);
	 * 
	 * if (!result) { return false; }
	 * 
	 * float x, y; // double r; x = algo.getX(); y = algo.getY(); // r =
	 * algo.getR();
	 * 
	 * if (!Float.isNaN(x) && !Float.isInfinite(x) && !Float.isNaN(y) &&
	 * !Float.isInfinite(y)) { LatLng trackedPosition = new LatLng(x, y);
	 * 
	 * // Log.d("algorithm", "tracked at: " + trackedPosition);
	 * triggerTrackedLocListeners(trackedPosition); } return true; }
	 */

	/**
	 * The WifiReceiver is responsible to Receive Access Points results
	 * */
	private class SimpleWifiReceiver extends WifiReceiver {

		public void onReceive(Context c, Intent intent) {

			try {
				if (intent == null || c == null || intent.getAction() == null)
					return;

				String action = intent.getAction();

				if (!action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
					return;

				// BroadcastReceiver.onReceive always run in the UI thread
				List<ScanResult> wifiList = wifi.getScanResults();
				triggerWiFiResultsListeners(wifiList.size());

				if (trackMe && trackResume) {

					if (future != null && future.isDone() == false)
						return;

					if (AnyplaceDebug.DEBUG_WIFI) {
                        //
						// FakeResults r = AnyUserData.fakeScan();
						// latestScanList =  r.records;
						// RAWheading = r.heading;
					} else {
						RAWheading = positioning.getRAWHeading();
						latestScanList.clear();

						LogRecord lr = null;

						// If we receive results, add them to latest scan list
						if (wifiList != null && !wifiList.isEmpty()) {
							for (int i = 0; i < wifiList.size(); i++) {
								lr = new LogRecord(wifiList.get(i).BSSID, wifiList.get(i).level);
								latestScanList.add(lr);
							}
						}
					}

					future = executorService.submit(new Runnable() {

						@Override
						public void run() {
							findMe();
						}
					});

				}

			} catch (RuntimeException e) {
				return;
			}

		}

	}

	public void Destoy() {
		trackOff();
		executorService.shutdown();
	}
}
