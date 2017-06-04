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

package com.dmsl.anyplace.wifi;

import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;

import com.dmsl.anyplace.MyApplication;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class SimpleWifiManager {

	private final static Long DEFAULT_INTERVAL = 2000L;
	private final static Object sync =  new Object();
	private static SimpleWifiManager mInstance = null;

	/**
	 * Creates a new instance
	 * 
	 */
	public static SimpleWifiManager getInstance() {
		
		if (mInstance == null) {
			synchronized (sync) {
				if (mInstance == null) {
					mInstance = new SimpleWifiManager(MyApplication.getAppContext());
				}
			}
		}
		return mInstance;
	}

	/** WiFi manager used to scan and get scan results */
	private final WifiManager mainWifi;

	/**
	 * Intent with the SCAN_RESULTS_AVAILABLE_ACTION action will be broadcast to
	 * asynchronously announce that the scan is complete and results are
	 * available.
	 */
	private final IntentFilter wifiFilter;

	/** Timer to perform new scheduled scans */
	private final Timer timer;

	/** Task to perform a scan */
	private TimerTask WifiTask;

	/** Application context */
	private final Context mContext;

	/** If Scanning, true or false */
	private Boolean isScanning;

	/**
	 * Creates a new instance
	 * 
	 * @param context
	 *            Application context
	 */
	private SimpleWifiManager(Context context) {
		mContext = context;
		isScanning = Boolean.valueOf(false);
		mainWifi = (WifiManager) mContext.getSystemService(Context.WIFI_SERVICE);
		wifiFilter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		timer = new Timer();
	}

	/**
	 * @return if the WiFi manager performs a scan
	 * */
	public Boolean getIsScanning() {
		return isScanning;
	}

	/**
	 * @return the results of the current scan
	 * */
	public List<ScanResult> getScanResults() {
		return mainWifi.getScanResults();
	}

	/**
	 * Starts the Access Points Scanning
	 * 
	 * @param interval
	 *            Interval used to perform a new scan
	 * */
	public void startScan(Long interval) {
		synchronized (isScanning) {
			isScanning = true;
		}

		//enableWifi();

		if (WifiTask != null) {
			WifiTask.cancel();
			WifiTask = null;
		}

		if (timer != null) {
			timer.purge();
		}

		WifiTask = new TimerTask() {
			@Override
			public void run() {
				mainWifi.startScan();
			}
		};

		timer.schedule(WifiTask, 0, interval);

	}

	/**
	 * Starts the Access Points Scanning
	 * 
	 * @param samples_interval
	 *            Interval used to perform a new scan
	 * */
	public void startScan(String samples_interval) {
		long interval = DEFAULT_INTERVAL;
		try {
			interval = Long.parseLong(samples_interval);
		} catch (NumberFormatException ex) {

		}

		startScan(interval);
	}
	
	/**
	 * Starts the Access Points Scanning
	 *
	 * */
	public void startScan() {
		startScan(DEFAULT_INTERVAL);
	}

	/**
	 * Stop the Access Points Scanning
	 *  */
	public void stopScan() {

		synchronized (isScanning) {
			isScanning = false;
		}

		if (WifiTask != null) {
			WifiTask.cancel();
			WifiTask = null;
		}

		if (timer != null) {
			timer.purge();
		}
	}

	// Call startScan
	public void registerScan(WifiReceiver receiverWifi) {

		mContext.registerReceiver(receiverWifi, wifiFilter);
	}

	public void unregisterScan(WifiReceiver receiverWifi) {

		mContext.unregisterReceiver(receiverWifi);
	}

	/**
	 * Enables WiFi
	 * */
	private void enableWifi() {
		if (!mainWifi.isWifiEnabled())
			if (mainWifi.getWifiState() != WifiManager.WIFI_STATE_ENABLING)
				mainWifi.setWifiEnabled(true);
	}

	/**
	 * Disables WiFi
	 * */
	private void disableWifi() {
		if (mainWifi.isWifiEnabled())
			if (mainWifi.getWifiState() != WifiManager.WIFI_STATE_DISABLING)
				mainWifi.setWifiEnabled(false);
	}

}
