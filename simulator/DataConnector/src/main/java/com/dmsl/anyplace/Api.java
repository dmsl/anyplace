/**
 * Anyplace Simulator:  A trace-driven evaluation and visualization of IoT Data Prefetching in Indoor Navigation SOAs
 *
 * Author(s): Zacharias Georgiou, Panagiotis Irakleous

 * Supervisor: Demetrios Zeinalipour-Yazti
 *
 * Copyright (c) 2017 Data Management Systems Laboratory, University of Cyprus
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/
 */
package com.dmsl.anyplace;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import javax.net.ssl.HttpsURLConnection;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import com.dmsl.anyplace.buildings.AllBuildings;
import com.dmsl.anyplace.buildings.connections.Connections;
import com.dmsl.anyplace.buildings.floors.Floors;
import com.dmsl.anyplace.buildings.pois.Pois;
import com.dmsl.anyplace.fingerprints.FingerPrints;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class Api {
	
	
	static void doTrustToCertificates(){
		// Create a trust manager that does not validate certificate chains like the  
		TrustManager[] trustAllCerts = new TrustManager[] {
		    new X509TrustManager() { 
		        public java.security.cert.X509Certificate[] getAcceptedIssuers() { 
		            return null; 
		        } 
		        public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) { 
		            //No need to implement.  
		        } 
		        public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) { 
		            //No need to implement.  
		        } 
		    } 
		}; 
		// Install the all-trusting trust manager 
		try { 
		    SSLContext sc = SSLContext.getInstance("SSL");
		    sc.init(null, trustAllCerts, new java.security.SecureRandom()); 
		    HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory()); 
		} catch (Exception e) {
		    System.err.println(e);
		} 
	}

	/**
	 * Check if the inputstream uses GZIP or NOT
	 * 
	 * @throws IOException
	 */
	private static InputStream decompressStream(InputStream input)
			throws IOException {
		// we need a pushbackstream to look ahead
		PushbackInputStream pb = new PushbackInputStream(input, 2);
		byte[] signature = new byte[2];
		pb.read(signature); // read the signature
		pb.unread(signature); // push back the signature to the stream
		// check if matches standard gzip magic number
		if (signature[0] == (byte) 0x1f && signature[1] == (byte) 0x8b)
			return new GZIPInputStream(pb);
		else
			return pb;
	}

	/**
	 * @return AllBuildings
	 * @throws Exception
	 */
	public static AllBuildings getAllBuildings(boolean devMode) throws Exception {
		doTrustToCertificates();
		String url;
		if(devMode)
			 url = "https://dev.anyplace.rayzit.com/anyplace/mapping/building/all";
		else
			url = "https://anyplace.rayzit.com/anyplace/mapping/building/all";
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");

		String postJsonString = "{ \"access_token\" : \"api_tester\" }";

		// Send post request
		con.setDoOutput(true);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				con.getOutputStream(), "UTF-8"));
		out.write(postJsonString);
		out.close();

		int responseCode = con.getResponseCode();
		String responseMessage = con.getResponseMessage();
		// System.out.println("Response Code : " + responseCode + "\nMessage: "
		// + responseMessage);

		if (responseCode == HttpsURLConnection.HTTP_OK) {
			InputStream stream = decompressStream(con.getInputStream());
			Reader decoder = new InputStreamReader(stream, "UTF-8");
			AllBuildings all = new Gson().fromJson(decoder, AllBuildings.class);
			stream.close();
			decoder.close();
			return all;
		} else {
			throw new Exception("{ \nResponse Code : " + responseCode
					+ "\nMessage: " + responseMessage + "\n}");
		}
	}

	/**
	 * @param buid
	 * @return Floors
	 * @throws Exception
	 */
	public static Floors getBuildingFloor(boolean devMode,String buid) throws Exception {
		doTrustToCertificates();
		String url;
		if(devMode)
			url = "https://dev.anyplace.rayzit.com/anyplace/mapping/floor/all";
		else
			url = "https://anyplace.rayzit.com/anyplace/mapping/floor/all";

		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");

		String postJsonString = "{ \"access_token\" : \"api_tester\", \"buid\" : \""
				+ buid + "\"}";

		// Send post request
		con.setDoOutput(true);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				con.getOutputStream(), "UTF-8"));
		out.write(postJsonString);
		out.close();

		int responseCode = con.getResponseCode();
		String responseMessage = con.getResponseMessage();
		// System.out.println("Response Code : " + responseCode + "\nMessage: "
		// + responseMessage);

		if (responseCode == HttpsURLConnection.HTTP_OK) {
			InputStream stream = decompressStream(con.getInputStream());
			Reader decoder = new InputStreamReader(stream, "UTF-8");
			Floors floors = new Gson().fromJson(decoder, Floors.class);
			stream.close();
			decoder.close();
			return floors;
		} else {
			throw new Exception("{ \nResponse Code : " + responseCode
					+ "\nMessage: " + responseMessage + "\n}");
		}
	}

	/**
	 * @param buid
	 * @return Pois
	 * @throws Exception
	 */
	public static Pois getPOIsByBuilding(boolean devMode,String buid) throws Exception {
		doTrustToCertificates();
		String url;
		if(devMode)
			url = "https://dev.anyplace.rayzit.com/anyplace/mapping/pois/all_pois_nconnectors";
		else
			url = "https://anyplace.rayzit.com/anyplace/mapping/pois/all_pois_nconnectors";
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");

		String postJsonString = "{ \"access_token\" : \"api_tester\", \"buid\" : \""
				+ buid + "\"}";

		// Send post request
		con.setDoOutput(true);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				con.getOutputStream(), "UTF-8"));
		out.write(postJsonString);
		out.close();

		int responseCode = con.getResponseCode();
		String responseMessage = con.getResponseMessage();
		// System.out.println("Response Code : " + responseCode + "\nMessage: "
		// + responseMessage);

		if (responseCode == HttpsURLConnection.HTTP_OK) {
			InputStream stream = decompressStream(con.getInputStream());
			Reader decoder = new InputStreamReader(stream, "UTF-8");
			Pois floors = new Gson().fromJson(decoder, Pois.class);
			stream.close();
			decoder.close();
			return floors;
		} else {
			throw new Exception("{ \nResponse Code : " + responseCode
					+ "\nMessage: " + responseMessage + "\n}");
		}
	}

	/**
	 * @param buid
	 * @param floor
	 * @return
	 * @throws Exception
	 */
	public static Connections getBuildingConnections(String buid, String floor)
			throws Exception {
		doTrustToCertificates();
		String url = "https://dev.anyplace.rayzit.com/anyplace/mapping/connection/all_floor";
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");

		String postJsonString = "{ \"access_token\" : \"api_tester\", \"buid\" : \""
				+ buid + "\", \"floor_number\" : \"" + floor + "\"}";

		// Send post request
		con.setDoOutput(true);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				con.getOutputStream(), "UTF-8"));
		out.write(postJsonString);
		out.close();

		int responseCode = con.getResponseCode();
		String responseMessage = con.getResponseMessage();
		// System.out.println("Response Code : " + responseCode + "\nMessage: "
		// + responseMessage);

		if (responseCode == HttpsURLConnection.HTTP_OK) {
			InputStream stream = decompressStream(con.getInputStream());
			Reader decoder = new InputStreamReader(stream, "UTF-8");
			Connections connections = new Gson().fromJson(decoder,
					Connections.class);
			stream.close();
			decoder.close();
			return connections;
		} else {
			throw new Exception("{ \nResponse Code : " + responseCode
					+ "\nMessage: " + responseMessage + "\n}");
		}
	}

	/**
	 * @param devMode
	 * @param buid
	 * @return
	 * @throws Exception
	 */
	public static Connections getBuildingConnections(boolean devMode,String buid)
			throws Exception {
		doTrustToCertificates();
		String url;
		if(devMode)
			url = "https://dev.anyplace.rayzit.com/anyplace/mapping/connection/all_floors";
		else
			url = "https://anyplace.rayzit.com/anyplace/mapping/connection/all_floors";
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("Content-Type", "application/json");

		String postJsonString = "{ \"access_token\" : \"api_tester\", \"buid\" : \""
				+ buid + "\"}";

		// Send post request
		con.setDoOutput(true);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				con.getOutputStream(), "UTF-8"));
		out.write(postJsonString);
		out.close();

		int responseCode = con.getResponseCode();
		String responseMessage = con.getResponseMessage();
		// System.out.println("Response Code : " + responseCode + "\nMessage: "
		// + responseMessage);

		if (responseCode == HttpsURLConnection.HTTP_OK) {
			InputStream stream = decompressStream(con.getInputStream());
			Reader decoder = new InputStreamReader(stream, "UTF-8");
			Connections connections = new Gson().fromJson(decoder,
					Connections.class);
			stream.close();
			decoder.close();
			return connections;
		} else {
			throw new Exception("{ \nResponse Code : " + responseCode
					+ "\nMessage: " + responseMessage + "\n}");
		}
	}

	public static FingerPrints getFingerPrintsByLatLon(int pid, String buid,
			String lat, String lon, String floor, int range) throws Exception {
		doTrustToCertificates();
		String url = "https://anyplace.rayzit.com/anyplace/position/radio_by_building_floor_bbox";
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("Accept", "application/json");
		con.setRequestProperty("Content-Type", "application/json");

		JsonObject json = new JsonObject();
		json.addProperty("buid", buid);
		json.addProperty("coordinates_lat", lat);
		json.addProperty("coordinates_lon", lon);
		json.addProperty("floor", floor);
		json.addProperty("range", String.valueOf(range));
		String jsonStr = json.toString();

		// Send post request
		con.setDoOutput(true);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				con.getOutputStream(), "UTF-8"));
		// out.write(postJsonString);
		out.write(jsonStr);
		out.close();

		int responseCode = con.getResponseCode();
		String responseMessage = con.getResponseMessage();
		System.out.println("Response Code : " + responseCode + "\nMessage: "
				+ responseMessage);
		// String str = receiveResponse(con);
		if (responseCode == HttpsURLConnection.HTTP_OK) {
			InputStream stream = decompressStream(con.getInputStream());
			Reader decoder = new InputStreamReader(stream, "UTF-8");

			String mean = new Gson().fromJson(decoder, JsonObject.class)
					.get("map_url_mean").getAsString();
			String res = getMean(mean, buid, floor, false);
			return new FingerPrints(pid, res);
		} else {
			System.err.println(jsonStr);
			throw new Exception("{ \nResponse Code : " + responseCode
					+ "\nMessage: " + responseMessage + "\n}");
		}
	}

	public static FingerPrints getRadiomapFloor(String buid, String floor)
			throws Exception {
		doTrustToCertificates();
		String url = "https://anyplace.rayzit.com/anyplace/position/radio_by_building_floor";
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("Accept", "application/json");
		con.setRequestProperty("Content-Type", "application/json");

		JsonObject json = new JsonObject();
		json.addProperty("buid", buid);
		json.addProperty("usernam", "username");
		json.addProperty("password", "pass");
		json.addProperty("floor", floor);
		String jsonStr = json.toString();

		// Send post request
		con.setDoOutput(true);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				con.getOutputStream(), "UTF-8"));
		// out.write(postJsonString);
		out.write(jsonStr);
		out.close();

		int responseCode = con.getResponseCode();
		String responseMessage = con.getResponseMessage();
		System.out.println("Response Code : " + responseCode + "\nMessage: "
				+ responseMessage);
		// String str = receiveResponse(con);
		if (responseCode == HttpsURLConnection.HTTP_OK) {
			InputStream stream = decompressStream(con.getInputStream());
			Reader decoder = new InputStreamReader(stream, "UTF-8");

			String mean = new Gson().fromJson(decoder, JsonObject.class)
					.get("map_url_mean").getAsString();
			getMean(mean, buid, floor, true);
			return null;
		} else {
			throw new Exception("{ \nResponse Code : " + responseCode
					+ "\nMessage: " + responseMessage + "\n}");
		}
	}

	private static String getMean(String url, String buid, String floor,
			boolean writeToFile) throws Exception {
		doTrustToCertificates();
		
		url = url.replace("80", "443");
		URL obj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) obj.openConnection();

		// add reuqest header
		con.setRequestMethod("POST");
		con.setRequestProperty("Accept", "application/json");
		con.setRequestProperty("Content-Type", "application/json");

		JsonObject json = new JsonObject();
		json.addProperty("usernam", "username");
		json.addProperty("password", "pass");
		String jsonStr = json.toString();

		// Send post request
		con.setDoOutput(true);
		BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
				con.getOutputStream(), "UTF-8"));
		// out.write(postJsonString);
		out.write(jsonStr);
		out.close();

		int responseCode = con.getResponseCode();
		String responseMessage = con.getResponseMessage();
		System.out.println("Response Code : " + responseCode + "\nMessage: "
				+ responseMessage);
		// String str = receiveResponse(con);

		File path = new File(DataConnector.FINGERPRINTS_PATH + buid + "/");
		if (!path.exists()) {
			if(path.mkdirs())
			System.out.println("[Info] Directory: " + DataConnector.FINGERPRINTS_PATH
					+ buid + "/" + " created");

		}

		if (responseCode == HttpsURLConnection.HTTP_OK) {
			String result = receiveResponse(con);
			if (writeToFile) {
				FileWriter fw = new FileWriter(new File(DataConnector.FINGERPRINTS_PATH
						+ buid + "/" + floor + ".fing"));
				fw.write(result);
				System.out.println("File " + DataConnector.FINGERPRINTS_PATH + buid
						+ "/" + floor + ".fing created");
			}
			return result;
		} else {
			throw new Exception("{ \nResponse Code : " + responseCode
					+ "\nMessage: " + responseMessage + "\n}");
		}
	}

	public static String receiveResponse(HttpURLConnection conn)
			throws IOException {
		conn.setConnectTimeout(10000);
		conn.setReadTimeout(10000);
		// retrieve the response from server
		InputStream is = null;
		try {
			is = conn.getInputStream();
			int ch;
			StringBuffer sb = new StringBuffer();
			while ((ch = is.read()) != -1) {
				sb.append((char) ch);
			}
			return sb.toString();
		} catch (IOException e) {
			throw e;
		} finally {
			if (is != null) {
				is.close();
			}
		}
	}
}
