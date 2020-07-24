/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Lambros Petrou, Timotheos Constambeys
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

package com.dmsl.anyplace.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Environment;
import android.provider.Settings;
import android.text.TextPaint;

public class AndroidUtils {

	public static void showWifiSettings(final Activity activity, final String title, final Runnable yes, final Runnable no) {
		// check for internet connection
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
		alertDialog.setTitle(title);
		alertDialog.setMessage("Would you like to change settings ?");
		// Setting Positive "Yes" Button
		alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				activity.startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
				if (yes != null)
					yes.run();
			}
		});
		// Setting Negative "NO" Button
		alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				if (no != null)
					no.run();
			}
		});
		// Showing Alert Message
		alertDialog.show();
	}

	public static void showGPSSettings(final Activity activity) {
		// check for internet connection
		AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
		alertDialog.setTitle("GPS adapter disabled");
		alertDialog.setMessage("GPS is not enabled. Please enable GPS");
		// Setting Positive "Yes" Button
		alertDialog.setPositiveButton("Settings", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				activity.startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
			}
		});
		// Setting Negative "NO" Button
		alertDialog.setNegativeButton("NO", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
			}
		});
		// Showing Alert Message
		alertDialog.show();
	}

	public static boolean checkExternalStorageState() {
		boolean mExternalStorageAvailable = false;
		boolean mExternalStorageWriteable = false;
		String state = Environment.getExternalStorageState();

		if (Environment.MEDIA_MOUNTED.equals(state)) {
			// We can read and write the media
			mExternalStorageAvailable = mExternalStorageWriteable = true;
		} else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
			// We can only read the media
			mExternalStorageAvailable = true;
			mExternalStorageWriteable = false;
		} else {
			// Something else is wrong. It may be one of many other states, but
			// all we need
			// to know is we can neither read nor write
			mExternalStorageAvailable = mExternalStorageWriteable = false;
		}

		if (!mExternalStorageWriteable || !mExternalStorageAvailable) {
			// we cannot download the floor plan on the sdcard
			return false;
		}
		return true;
	}

	public static void unzip(String strZipFile) {

		try {
			/*
			 * STEP 1 : Create directory with the name of the zip file
			 * 
			 * For e.g. if we are going to extract c:/demo.zip create c:/demo directory where we can extract all the zip entries
			 */
			File fSourceZip = new File(strZipFile);
			String zipPath = strZipFile.substring(0, strZipFile.length() - 4);
			File temp = new File(zipPath);
			temp.mkdir();
			System.out.println(zipPath + " created");

			/*
			 * STEP 2 : Extract entries while creating required sub-directories
			 */
			ZipFile zipFile = new ZipFile(fSourceZip);
			Enumeration<? extends ZipEntry> e = zipFile.entries();

			while (e.hasMoreElements()) {
				ZipEntry entry = (ZipEntry) e.nextElement();
				File destinationFilePath = new File(zipPath, entry.getName());

				// create directories if required.
				destinationFilePath.getParentFile().mkdirs();

				// if the entry is directory, leave it. Otherwise extract it.
				if (entry.isDirectory()) {
					continue;
				} else {
					// System.out.println("Extracting " + destinationFilePath);

					/*
					 * Get the InputStream for current entry of the zip file using
					 * 
					 * InputStream getInputStream(Entry entry) method.
					 */
					BufferedInputStream bis = new BufferedInputStream(zipFile.getInputStream(entry));

					int b;
					byte buffer[] = new byte[1024];

					/*
					 * read the current entry from the zip file, extract it and write the extracted file.
					 */
					FileOutputStream fos = new FileOutputStream(destinationFilePath);
					BufferedOutputStream bos = new BufferedOutputStream(fos, 1024);

					while ((b = bis.read(buffer, 0, 1024)) != -1) {
						bos.write(buffer, 0, b);
					}

					// flush the output stream and close it.
					bos.flush();
					bos.close();

					// close the input stream.
					bis.close();
				}

			}
			zipFile.close();
		} catch (IOException ioe) {
			System.out.println("IOError :" + ioe);
		}

	}

	public static String fillTextBox(TextPaint paint, int fragmentWidth, String source) {
		StringBuilder sb = new StringBuilder();
		final int length = source.length();
		// Display whole words only
		int lastWhiteSpace = 0;

		for (int index = 0; paint.measureText(sb.toString()) < fragmentWidth && index < length; index++) {
			char c = source.charAt(index);
			if (Character.isWhitespace(c))
				lastWhiteSpace = index;
			sb.append(c);
		}

		if (sb.length() != length) {
			// Delete last word part
			sb.delete(lastWhiteSpace, sb.length());
			sb.append("...");
		}

		return sb.toString();

	}

	public static String fillTextBox(TextPaint paint, int fragmentWidth, String source, int start) {
		StringBuilder sb = new StringBuilder();
		final int length = source.length();
		int indexLeft = start;
		int indexRight = start + 1;
		int lastWhiteSpaceL = 0;
		int lastWhiteSpaceR = 0;
		while (paint.measureText(sb.toString()) < fragmentWidth && (indexLeft >= 0 || indexRight < length)) {
			if (indexLeft >= 0) {
				char c = source.charAt(indexLeft);
				if (Character.isWhitespace(c))
					lastWhiteSpaceL = indexLeft;
				sb.insert(0, c);
				indexLeft--;
			}

			if (indexRight < length) {
				char c = source.charAt(indexRight);
				if (Character.isWhitespace(c))
					lastWhiteSpaceR = indexRight;
				sb.append(c);
				indexRight++;
			}
		}

		if (indexLeft >= 0) {
			// Delete first word part
			sb.delete(0, lastWhiteSpaceL - indexLeft);
			sb.insert(0, "...");
			indexLeft = lastWhiteSpaceL - 3; // Set new index left
		}

		if (indexRight < length) {
			// Delete last word part
			sb.delete(lastWhiteSpaceR - (indexLeft + 1), sb.length());
			sb.append("...");
		}

		return sb.toString();
	}

	public static GeoPoint getIPLocation() throws Exception {
		// http://ip-api.com/docs/api:json
		String response = NetworkUtils.downloadUrlAsStringHttp("http://ip-api.com/json");

		JSONObject json = new JSONObject(response);

		if (json.getString("status").equalsIgnoreCase("error")) {
			throw new Exception(json.getString("message"));
		}

		String lat = json.getString("lat");
		String lon = json.getString("lon");

		return new GeoPoint(lat, lon);

	}
}
