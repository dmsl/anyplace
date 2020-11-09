/*
 * Anyplace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 * 
 * Author(s): Timotheos Constambeys
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

package cy.ac.ucy.cs.anyplace.lib.android.floor;

import java.util.*;
import java.util.concurrent.*;







import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.Handler;

import cy.ac.ucy.cs.anyplace.lib.android.wifi.SimpleWifiManager;
import cy.ac.ucy.cs.anyplace.lib.android.wifi.WifiReceiver;
import cy.ac.ucy.cs.anyplace.lib.android.AnyplaceDebug;
import cy.ac.ucy.cs.anyplace.lib.LogRecord;

public abstract class FloorSelector {

	private final long PERIODIC_EVENT_TIMEOUT = 15000;

	// Floor Tracking
	public interface FloorAnyplaceFloorListener {
		public void onNewFloor(String floor);
	}

	private List<FloorAnyplaceFloorListener> flisteners = new ArrayList<FloorAnyplaceFloorListener>();

	public void addListener(FloorAnyplaceFloorListener list) {
		flisteners.add(list);
	}

	public void removeListener(FloorAnyplaceFloorListener list) {
		flisteners.remove(list);
	}

	/**
	 * Running on UI Thread
	 */
	private void triggerFloorListeners(String f) {
		for (FloorAnyplaceFloorListener l : flisteners) {
			l.onNewFloor(f);
		}
	}

	// Error
	public interface ErrorAnyplaceFloorListener {
		public void onFloorError(Exception ex);
	}

	private List<ErrorAnyplaceFloorListener> errorlisteners = new ArrayList<ErrorAnyplaceFloorListener>();

	public void addListener(ErrorAnyplaceFloorListener list) {
		errorlisteners.add(list);
	}

	public void removeListener(ErrorAnyplaceFloorListener list) {
		errorlisteners.remove(list);
	}

	/**
	 * Running on UI Thread
	 */
	private void triggerErrorListeners(Exception ex) {
		for (ErrorAnyplaceFloorListener l : errorlisteners) {
			l.onFloorError(ex);
		}
	}

	private Double dlat = null;
	private Double dlong = null;

	/** Timer to perform Timer Task */
	private Timer timer;
	private Boolean timerElapsed = true;
	private Boolean trackMe = false;
	private boolean trackResume = false;

	Context context;
	ExecutorService executorService;
	CalculateFloorTask task;
	Future future;

	// WiFi manager
	private SimpleWifiManager wifi;
	// WiFi Receiver
	private WifiReceiver receiverWifi;

	FloorSelector(final Context myContext) {
		context = myContext;
		TimerTask WifiTask = new TimerTask() {

			@Override
			public void run() {
				timerElapsed = true;
			}
		};

		timer = new Timer();
		timer.schedule(WifiTask, 0, PERIODIC_EVENT_TIMEOUT);

		executorService = Executors.newSingleThreadExecutor();

		// WiFi manager to manage scans
		wifi = SimpleWifiManager.getInstance(context);
		// Create new receiver to get broadcasts
		receiverWifi = new SimpleWifiReceiver();
		wifi.registerScan(receiverWifi);
	}

	protected abstract String calculateFloor(Args args) throws Exception;

	private class CalculateFloorTask implements Runnable {

		volatile boolean aboard = false;
		ArrayList<LogRecord> latestScanList;

		CalculateFloorTask(ArrayList<LogRecord> latestScanList) {
			this.latestScanList = latestScanList;
		}

		public void run() {
			try {
				// Find the first and second strongest Access Point
				LogRecord firstMac = null;
				LogRecord secondMac = null;
				if (latestScanList.size() > 0) {
					firstMac = latestScanList.get(0);
				} else
					return;

				for (int i = 1; i < latestScanList.size(); ++i) {
					LogRecord current = latestScanList.get(i);
					if (firstMac.getRss() <= current.getRss()) {
						secondMac = firstMac;
						firstMac = current;
					} else if (secondMac == null || secondMac.getRss() < current.getRss()) {
						secondMac = current;
					}
				}

				Args args = new Args();
				args.firstMac = firstMac;
				args.secondMac = secondMac;
				args.dlat = dlat;
				args.dlong = dlong;
				args.latestScanList = latestScanList;

				final String floor = calculateFloor(args);

				if (floor != null && floor.equals("")) {
					throw new NonCriticalError();
				} else if (floor != null) {
					// Get a handler that can be used to post to the main thread
					Handler mainHandler = new Handler(context.getMainLooper());

					Runnable myRunnable = new Runnable() {

						@Override
						public void run() {
							if (!aboard) {
								triggerFloorListeners(floor);
							}
						}
					};

					mainHandler.post(myRunnable);
				}

			} catch (final Exception ex) {
				// Get a handler that can be used to post to the main thread
				Handler mainHandler = new Handler(context.getMainLooper());

				Runnable myRunnable = new Runnable() {

					@Override
					public void run() {
						if (!aboard)
							triggerErrorListeners(ex);

					}
				};

				mainHandler.post(myRunnable);
			}
		}
	}

	private synchronized void calculateFloor(ArrayList<LogRecord> latestScanList) {
		timerElapsed = false;

		if (future != null && future.isDone() == false)
			return;

		task = new CalculateFloorTask(latestScanList);
		future = executorService.submit(task);
	}

	/*
	 * The WifiReceiver is responsible to Receive Access Points results
	 */
	class SimpleWifiReceiver extends WifiReceiver {

		public void onReceive(Context c, Intent intent) {
			if (intent == null || c == null || intent.getAction() == null)
				return;

			String action = intent.getAction();

			if (!action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
				return;

			if (timerElapsed == false || trackMe == false || trackResume == false)
				return;

			// BroadcastReceiver.onReceive always run in the UI thread
			List<ScanResult> wifiList = wifi.getScanResults();
			// The latest scan list of APs
			final ArrayList<LogRecord> latestScanList = new ArrayList<LogRecord>();

			LogRecord lr = null;

			// If we receive results, add them to latest scan list
			if (wifiList != null && !wifiList.isEmpty()) {
				for (int i = 0; i < wifiList.size(); i++) {
					lr = new LogRecord(wifiList.get(i).BSSID, wifiList.get(i).level);
					latestScanList.add(lr);
				}
			}

			if (AnyplaceDebug.DEBUG_WIFI) {
				//calculateFloor(AnyUserData.fakeScan().records);
			} else {
				calculateFloor(latestScanList);
			}
		}
	}

	/**
	 * Pause creation of new tasks
	 */
	public void pauseTracking() {
		trackResume = false;
	}

	/**
	 * Resume creation of new tasks
	 */
	public void resumeTracking() {
		trackResume = true;
	}

	/**
	 * Run from a UI Thread
	 * Stops new tasks
	 * Aboard current task callback
	 */
	public void Stop() {
		trackMe = false;

		if (task != null)
			task.aboard = true;
	}

	public void RunNow() {
		if (dlat != null && dlong != null) {
			timerElapsed = true;
		}
	}

	/**
	 * Use all wifi records
	 */
	public void Start() {
		Start(0, 0);
	}

	/**
	 * restart with previous settings
	 */
	public void ReStart() {
		Start(dlat, dlong);
	}

	/**
	 * Get records x meters from lat,long
	 * 
	 * @param dlat
	 * @param dlong
	 */
	public void Start(double dlat, double dlong) {
		if (!AnyplaceDebug.FLOOR_SELECTOR) {
			return;
		}

		Stop();
		this.dlat = dlat;
		this.dlong = dlong;
		trackMe = true;

		List<ScanResult> wifiList = wifi.getScanResults();
		// The latest scan list of APs
		final ArrayList<LogRecord> latestScanList = new ArrayList<LogRecord>();

		LogRecord lr = null;

		// If we receive results, add them to latest scan list
		if (wifiList != null && !wifiList.isEmpty()) {
			for (int i = 0; i < wifiList.size(); i++) {
				lr = new LogRecord(wifiList.get(i).BSSID, wifiList.get(i).level);
				latestScanList.add(lr);
			}

			if (AnyplaceDebug.DEBUG_WIFI) {
				//calculateFloor(AnyUserData.fakeScan().records);
			} else {
				calculateFloor(latestScanList);
			}

		}
	}

	public void Start(String dlat, String dlong) {
		Start(Double.parseDouble(dlat), Double.parseDouble(dlong));
	}

	public void Destoy() {
		Stop();
		timer.cancel();
		timer.purge();
		wifi.unregisterScan(receiverWifi);
		executorService.shutdown();
	}

	// Network Error or 0 records return in server
	public static class NonCriticalError extends Exception {
	};

	static class Args {
		ArrayList<LogRecord> latestScanList;
		LogRecord firstMac;
		LogRecord secondMac;
		double dlat;
		double dlong;
	}

}
