/*
* AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
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

package com.dmsl.anyplace.logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import android.net.wifi.ScanResult;
import android.util.Log;

public class LoggerWiFi {

	public enum Function {
		ADD, SAVE
	}

	public interface Callback {
		public void onFinish(LoggerWiFi logger, Function function);
	}

	public ArrayList<ArrayList<LogRecordMap>> mSamples = new ArrayList<ArrayList<LogRecordMap>>();
	public boolean exceptionOccured = false;
	public String msg;
	public static boolean sample_ssid = false;
	private String unknownSsid = "HIDDEN";
	private Callback callback;
	ExecutorService executorService;

	public LoggerWiFi(Callback callback) {
		this.callback = callback;
		executorService = Executors.newSingleThreadExecutor();
	}

	public void add(final List<ScanResult> wifiList, final String curLocation, final Float raw_heading, final Boolean isWalking) {

		executorService.execute(new Runnable() {
			public void run() {
				if (wifiList == null) {
					return;
				}
				// format: >lat,lng<
				if (curLocation == null || raw_heading == null) {
					return;
				}

				String[] latlng = curLocation.split(",");
				double lat = Double.valueOf(latlng[0]);
				double lng = Double.valueOf(latlng[1]);

				ArrayList<LogRecordMap> records = new ArrayList<LogRecordMap>();
				long timestamp = System.currentTimeMillis();

				for (int i = 0; i < wifiList.size(); i++) {
					Log.d("wifi_capabilities: ", wifiList.get(i).capabilities + "\nfrequency: " + Integer.toString(wifiList.get(i).frequency)
							+ "\nchannel_width: " + Integer.toString(wifiList.get(i).channelWidth)
							+ "\ncenter_freq0: " + Integer.toString(wifiList.get(i).centerFreq0) + "\ncenter_freq1: " + Integer.toString(wifiList.get(i).centerFreq1));
					//todo add a condition to use ssid or not
					LogRecordMap lr;
					String objectid = android.os.Build.MANUFACTURER  + android.os.Build.MODEL;
					if(LoggerWiFi.sample_ssid){
						String ssid = wifiList.get(i).SSID;
						if(ssid == null || wifiList.get(i).SSID.isEmpty()){
							lr = new LogRecordMap(timestamp, lat, lng, raw_heading, isWalking, wifiList.get(i).BSSID, wifiList.get(i).level, unknownSsid, objectid, wifiList.get(i).frequency, wifiList.get(i).capabilities, wifiList.get(i).channelWidth);
						} else {
							lr = new LogRecordMap(timestamp, lat, lng, raw_heading, isWalking, wifiList.get(i).BSSID, wifiList.get(i).level, ssid, objectid, wifiList.get(i).frequency, wifiList.get(i).capabilities, wifiList.get(i).channelWidth);
						}

					} else {
						lr = new LogRecordMap(timestamp, lat, lng, raw_heading, isWalking, wifiList.get(i).BSSID, wifiList.get(i).level);
					}

					Log.d("LoggerWifi", lr.toString());
					records.add(lr);
				}
				mSamples.add(records);

				callback.onFinish(LoggerWiFi.this, Function.ADD);
			}
		});

	}

	public void save(final String endLocation, final String folder_path, final String filename_rss, final String currentFloor, final String currentBuilding) {

		executorService.execute(new Runnable() {
			public void run() {
				String[] latlng = endLocation.split(",");
				double lat = Double.valueOf(latlng[0]);
				double lng = Double.valueOf(latlng[1]);

				distributeLineSamples(lat, lng);
				write_to_log(folder_path, filename_rss, currentFloor, currentBuilding);

				callback.onFinish(LoggerWiFi.this, Function.SAVE);

				mSamples = new ArrayList<ArrayList<LogRecordMap>>();
			}
		});
	}

	/**
	 * it distributes the samples on the line's points.
	 * 
	 * @param lat
	 * @param lng
	 *            Ending point of the line
	 */

	private void distributeLineSamples(double lat, double lng) {

		/* No samples recorded, nothing to write */
		if (mSamples.isEmpty())
			return;

		LogRecordMap start = mSamples.get(0).get(0);

		/* Calculate walked line's length */
		double latdiff = lat - start.lat;
		double lngdiff = lng - start.lng;

		LogRecordMap end = mSamples.get(mSamples.size() - 1).get(0);
		// Get timestamp of the last walking sample
		// Invalid Location record
		for (int i = mSamples.size() - 1; i >= 0; i--) {
			if (mSamples.get(i).get(0).walking == true) {
				end = mSamples.get(i).get(0);
				break;
			}
		}

		long timediff = end.ts - start.ts;

		double velocityLat = latdiff / timediff;
		double velocityLng = lngdiff / timediff;

		if (velocityLat == 0 && velocityLng == 0)
			return;

		for (int i = 1; i < mSamples.size(); i++) {

			/* Sample was taken while standing still */
			if (mSamples.get(i).get(0).walking == false) {

				double newlat = mSamples.get(i - 1).get(0).lat;
				double newlng = mSamples.get(i - 1).get(0).lng;

				/* We need to adjust lat and lon of records */
				for (LogRecordMap lr : mSamples.get(i)) {
					lr.lat = newlat;
					lr.lng = newlng;
				}

			} else {

				/* We need to adjust lat and lon of records */
				for (LogRecordMap lr : mSamples.get(i)) {
					lr.lat = start.lat + velocityLat * (lr.ts - start.ts);
					lr.lng = start.lng + velocityLng * (lr.ts - start.ts);
				}

			}
		}

	}

	/**
	 * Writes AP scan records to log file specified by the user
	 * */
	private void write_to_log(String folder_path, String filename_rss, String currentFloor, String currentBuilding) {

		String header;
		if (!LoggerWiFi.sample_ssid){
			header = "# Timestamp, X, Y, HEADING, MAC Address of AP, RSS, Floor, BUID\n";
		} else {
			header = "# Timestamp, X, Y, HEADING, MAC Address of AP, RSS, SSID, ObjectID, Frequency, Capabilities, Floor, BUID\n";
		}

		LogRecordMap writeLR;

		try {

			File root = new File(folder_path);

			if (root.canWrite()) {

				FileOutputStream fos = new FileOutputStream(new File(root, filename_rss), true);

				for (ArrayList<LogRecordMap> writeRecords : mSamples) {
					fos.write(header.getBytes());
					for (int j = 0; j < writeRecords.size(); ++j) {
						writeLR = writeRecords.get(j);

						String log_record = writeLR.toString() + " " + String.valueOf(currentFloor) + " " + String.valueOf(currentBuilding);
						if (writeLR.getSsid() != null) {
							log_record = log_record + " " + String.valueOf(writeLR.getSsid()).trim().replace(' ', '_') +
									" " + String.valueOf(writeLR.getObjectid()).trim().replace(' ', '_') +
									" " + String.valueOf(writeLR.getFrequency()).trim() +
									" " + String.valueOf(writeLR.getChannelWidth()).trim() +
									" " + String.valueOf(writeLR.getCapabilities()).trim().replace(' ', '_');
						}
						Log.d("LoggerWifi log_record ", log_record);
						fos.write((log_record+ "\n").getBytes());
					}
				}

				fos.close();
			}
			exceptionOccured = false;
		} catch (ClassCastException cce) {
			exceptionOccured = true;
			msg = "Error: " + cce.getMessage();
		} catch (NumberFormatException nfe) {
			exceptionOccured = true;
			msg = "Error: " + nfe.getMessage();
		} catch (FileNotFoundException fnfe) {
			exceptionOccured = true;
			msg = "Error: " + fnfe.getMessage();
		} catch (IOException ioe) {
			exceptionOccured = true;
			msg = "Error: " + ioe.getMessage();
		}

	}

	/**
	 * Invokes {@code shutdown} when this class is no longer referenced and it
	 * has no threads.
	 */
	protected void finalize() {
		executorService.shutdown();
	}
}
