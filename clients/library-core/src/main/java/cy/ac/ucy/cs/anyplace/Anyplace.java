/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Christakis Achilleos, Constandinos Demetriou, Marcos Antonios Charalambous
 *
 * Co-supervisor: Paschalis Mpeis
 * 
 * Supervisor: Demetrios Zeinalipour-Yazti
 *
 * URL: http://anyplace.cs.ucy.ac.cy
 * Contact: anyplace@cs.ucy.ac.cy
 *
 * Copyright (c) 2019, Data Management Systems Lab (DMSL), University of Cyprus.
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

package cy.ac.ucy.cs.anyplace;

import org.json.JSONException;
import org.json.JSONObject;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

public class Anyplace {

	public static final int STATUS_OK = 0;
	public static final int STATUS_ERR = 1;
	private String host;
	private String path;
	private String port;
	private String cache;
	private String token = null;

	public Anyplace(Preferences p){
		setCache(p.getCache());
		setHost(p.getHost());
		setPort(p.getPort());
		setToken(p.getApi_key());
	}
	public Anyplace(String host, String port, String cache) {
		setCache(cache);
		setHost(host);
		setPort(port);
	}

	private static BufferedImage decodeToImage(String imageString) {

		BufferedImage image = null;
		byte[] imageByte;
		try {
			Base64.Decoder decoder = Base64.getDecoder();
			imageByte = decoder.decode(imageString);
			ByteArrayInputStream bis = new ByteArrayInputStream(imageByte);
			image = ImageIO.read(bis);
			bis.close();
		} catch (Exception e) {
			e.printStackTrace();

		}
		return image;
	}

	/**
	 * Gets the details of a specific Point Of Interest
	 * 
	 * @param access_token The users access token (api key)
	 * @param pois         The POI that is being specified
	 * @return The response JSON as a String
	 */
	public String poiDetails(String access_token, String pois) {
		RestClient client = new RestClient();
		setPath("/anyplace/navigation/pois/id");
		Map<String, String> params = new HashMap<>();
		params.put("access_token", access_token);
		params.put("pois", pois);

		return JsonHelper.jsonResponse(STATUS_OK, client.doPost(params, getHost(), getPath()) );
	}

	public String poiDetails(String pois){
		if(token.isEmpty() || token == null){
			return JsonHelper.jsonResponse(STATUS_ERR,"{}");
		}
		return poiDetails(token, pois);

	}

	/**
	 * Navigation instructions given from a given location to a POI
	 * 
	 * @param access_token   The users access token (api key)
	 * @param pois_to        The target POI
	 * @param buid           The building ID
	 * @param floor          The floor number
	 * @param coordinates_lat The latitude of position
	 * @param coordinates_lon The longitude of position
	 * @return A JSON in String form
	 */
	public String navigationXY(String access_token, String pois_to, String buid, String floor, String coordinates_lat,
			String coordinates_lon)  {

		RestClient client = new RestClient();
		setPath("/anyplace/navigation/route_xy");
		Map<String, String> params = new HashMap<>();
		params.put("access_token", access_token);
		params.put("pois_to", pois_to);
		params.put("buid", buid);
		params.put("floor_number", floor);
		params.put("coordinates_lat", coordinates_lat);
		params.put("coordinates_lon", coordinates_lon);

		String response = client.doPost(params, getHost(), getPath());

		JSONObject obj;
		int statusCode;
		try {
			obj = new JSONObject(response);
			statusCode = obj.getInt("status_code");
			if (statusCode != 200) {
				return JsonHelper.printError(new Exception(), "navigationXY");
			}
		} catch (JSONException e) {
			return JsonHelper.printError(e, "navigationXY");
		}

		return JsonHelper.jsonResponse(STATUS_OK, response);

	}

	/**
	 * Navigation instructions given from a given location to a POI
	 *
	 * @param pois_to        The target POI
	 * @param buid           The building ID
	 * @param floor          The floor number
	 * @param coordinates_lat The latitude of position
	 * @param coordinates_lon The longitude of position
	 * @return A JSON in String form
	 */
	public String navigationXY(String pois_to, String buid, String floor, String coordinates_lat, String coordinates_lon){

		return navigationXY(token, pois_to, buid, floor, coordinates_lat, coordinates_lon);
	}

	/**
	 * Navigation instructions between 2 POIs.
	 * 
	 * @param access_token The users access token (api key)
	 * @param pois_to      The target POI
	 * @param pois_from    The starting POI
	 * @return The navigation path in the form of a JSON string as sent by the
	 *         server
	 */
	public String navigationPoiToPoi(String access_token, String pois_to, String pois_from)  {

		RestClient client = new RestClient();
		setPath("/anyplace/navigation/route");
		Map<String, String> params = new HashMap<>();
		params.put("access_token", access_token);
		params.put("pois_from", pois_from);
		params.put("pois_to", pois_to);

		String response = client.doPost(params, getHost(), getPath());

		JSONObject obj ;
		int statusCode ;
		try {
			obj = new JSONObject(response);
			statusCode = obj.getInt("status_code");
		} catch (JSONException e) {
			return JsonHelper.printError(e, "navigationPoiToPoi");
		}

		if (statusCode != 200) {
			return JsonHelper.printError(new Exception(), "navigationPoiToPoi");
		}

		return JsonHelper.jsonResponse(STATUS_OK, response);
	}

	/**
	 * Navigation instructions between 2 POIs.
	 *
	 * @param pois_to      The target POI
	 * @param pois_from    The starting POI
	 * @return The navigation path in the form of a JSON string as sent by the
	 *         server
	 */
	public String navigationPoiToPoi(String pois_to, String pois_from) {
		return navigationPoiToPoi(token, pois_to, pois_from);
	}

		/**
         * Get all annotated buildings
         *
         * @return The response JSON as a String
         */
	public String buildingAll() {
		RestClient client = new RestClient();
		setPath("/anyplace/mapping/building/all");

		return JsonHelper.jsonResponse(STATUS_OK,client.doPost(null, getHost(), getPath()));
	}

	/**
	 * Get all buildings for a campus
	 * 
	 * @param cuid The campus ID
	 * @return JSON String response
	 */
	public String buildingsByCampus(String cuid) {

		RestClient client = new RestClient();
		setPath("/anyplace/mapping/campus/all_cucode");
		Map<String, String> params = new HashMap<>();
		params.put("cuid", cuid);

		return JsonHelper.jsonResponse(STATUS_OK, client.doPost(params, getHost(), getPath()));
	}

	/**
	 * Get all buildings with the same code
	 * 
	 * @param bucode The building code
	 * @return JSON String response
	 */
	public String buildingsByBuildingCode(String bucode) {
		RestClient client = new RestClient();
		setPath("/anyplace/mapping/building/all_bucode");
		Map<String, String> params = new HashMap<>();
		params.put("bucode", bucode);
		return JsonHelper.jsonResponse(STATUS_OK, client.doPost(params, getHost(), getPath()));
	}

	/**
	 * Get all nearby buildings - 50 meter radius
	 * 
	 * @param access_token    The users access token (api key)
	 * @param coordinates_lat The latitude
	 * @param coordinates_lon The longitude
	 * @return Gives the JSON String response of the server
	 */
	public String nearbyBuildings(String access_token, String coordinates_lat, String coordinates_lon) {

		RestClient client = new RestClient();
		setPath("/anyplace/mapping/building/coordinates");
		Map<String, String> params = new HashMap<>();
		params.put("access_token", access_token);
		params.put("coordinates_lat", coordinates_lat);
		params.put("coordinates_lon", coordinates_lon);


		return JsonHelper.jsonResponse(STATUS_OK, client.doPost(params, getHost(), getPath()));
	}

	/**
	 * Get all nearby buildings - 50 meter radius
	 *
	 * @param coordinates_lat The latitude
	 * @param coordinates_lon The longitude
	 * @return Gives the JSON String response of the server
	 */
	public String nearbyBuildings(String coordinates_lat, String coordinates_lon){
		return nearbyBuildings(token, coordinates_lat, coordinates_lon);
	}

	/**
	 * Get all floors of a building
	 * 
	 * @param buid Building ID
	 * @return A JSON String containing all the floors of the building
	 */
	public String allBuildingFloors(String buid) {

		RestClient client = new RestClient();
		setPath("/anyplace/mapping/floor/all");
		Map<String, String> params = new HashMap<>();
		params.put("buid", buid);


		return JsonHelper.jsonResponse(STATUS_OK,client.doPost(params, getHost(), getPath()));
	}

	/**
	 * Get all POIs inside of a building
	 * 
	 * @param buid The building ID
	 * @return JSON string with all the POIs in the building
	 */
	public String allBuildingPOIs(String buid) {

		RestClient client = new RestClient();
		setPath("/anyplace/mapping/pois/all_building");
		Map<String, String> params = new HashMap<>();
		params.put("buid", buid);
		return JsonHelper.jsonResponse(STATUS_OK, client.doPost(params, getHost(), getPath()));
	}

	/**
	 * Get all POIs inside of a floor of a building
	 * 
	 * @param buid  The building ID
	 * @param floor The floor number
	 * @return JSON String with all the POIs of a floor
	 */
	public String allBuildingFloorPOIs(String buid, String floor) {

		RestClient client = new RestClient();
		setPath("/anyplace/mapping/pois/all_floor");
		Map<String, String> params = new HashMap<>();
		params.put("buid", buid);
		params.put("floor_number", floor);



		return JsonHelper.jsonResponse(STATUS_OK, client.doPost(params, getHost(), getPath()));
	}

	/**
	 * Get all connections between POIs inside of a floor of a building
	 * 
	 * @param buid  The building ID
	 * @param floor The floor number
	 * @return JSON String with all the connections in a floor
	 */
	public String connectionsByFloor(String buid, String floor) {

		RestClient client = new RestClient();
		setPath("/anyplace/mapping/connection/all_floor");
		Map<String, String> params = new HashMap<>();
		params.put("buid", buid);
		params.put("floor_number", floor);


		return JsonHelper.jsonResponse(STATUS_OK, client.doPost(params, getHost(), getPath()));
	}

	/**
	 * Get all positions with their respective Wi-Fi radio measurements.
	 * 
	 * @param buid  The building ID
	 * @param floor The floor number
	 * @return JSON String with the wifi intensities on a floor
	 */
	public String radioheatMapBuildingFloor(String buid, String floor) {

		RestClient client = new RestClient();
		Map<String, String> params = new HashMap<>();
		params.put("buid", buid);
		params.put("floor", floor);
		setPath("/anyplace/mapping/radio/heatmap_building_floor");

		return JsonHelper.jsonResponse(STATUS_OK, client.doPost(params, getHost(), getPath()));



	}

	/**
	 * Download the floor plan in base64 png format. It also stores the file locally
	 * as a png file
	 * 
	 * @param access_token The access token(api key)
	 * @param buid         The building ID
	 * @param floor        The floor number
	 * @return JSON String containing the floor plan in a base64 format
	 */
	public String floorplans64(String access_token, String buid, String floor) {

		RestClient client = new RestClient();
		setPath("/anyplace/floorplans64/" + buid + "/" + floor);
		Map<String, String> params = new HashMap<>();
		params.put("access_token", access_token);
		params.put("buid", buid);
		params.put("floor", floor);

		String response = client.doPost(params, getHost(), getPath());

		String filename = cache + buid + "/" + floor + "/" + "floorplan.png";

		try {
			File outputfile = new File(filename);
			ImageIO.write(decodeToImage(response), "png", outputfile);
		} catch (Exception e) {
			return JsonHelper.printError(e, "floorplan64");
		}

		return JsonHelper.jsonResponse(STATUS_OK, response);
	}

	/**
	 * Download the floor plan in base64 png format. It also stores the file locally
	 * as a png file
	 *
	 * @param buid         The building ID
	 * @param floor        The floor number
	 * @return JSON String containing the floor plan in a base64 format
	 */
	public String floorplans64( String buid, String floor) {
		return floorplans64(token, buid, floor);
	}

	/**
	 * Download the floor plan tiles in a zip file
	 * 
	 * @param access_token The access token(api key)
	 * @param buid         The building ID
	 * @param floor        The floor number
	 * @return JSON String containing the floor tile zip download url
	 */
	public String floortiles(String access_token, String buid, String floor) {

		RestClient client = new RestClient();
		setPath("/anyplace/floortiles/" + buid + "/" + floor);
		Map<String, String> params = new HashMap<>();
		params.put("access_token", access_token);
		params.put("buid", buid);
		params.put("floor", floor);

		String response = client.doPost(params, getHost(), getPath());

		JSONObject obj;
		int statusCode;
		try {
			obj = new JSONObject(response);
			statusCode = obj.getInt("status_code");
		} catch (JSONException e) {
			return JsonHelper.printError(e, "floortiles");
		}

		if (statusCode == 200) {
			String tiles_archive;
			try {
				tiles_archive = obj.getString("tiles_archive");
			} catch (JSONException e) {
				return JsonHelper.printError(e, "floortiles");
			}
			byte[] zip = client.getFileWithGet(getHost(), tiles_archive);
			String filename = "res/" + buid + "/" + floor + "/" + "floorPlanTiles.zip";

			try {
				FileOutputStream outputStream = new FileOutputStream(filename);

				outputStream.write(zip);
				outputStream.close();
			} catch (IOException e) {
				return JsonHelper.printError(e, "floortiles");
			}

		} else {
			System.out.println("Bad response");
		}

		return JsonHelper.jsonResponse(STATUS_OK, response);
	}

	/**
	 * Download the floor plan tiles in a zip file
	 *
	 * @param buid         The building ID
	 * @param floor        The floor number
	 * @return JSON String containing the floor tile zip download url
	 */
	public String floortiles( String buid, String floor){
		return floortiles(token, buid, floor);
	}

	/**
	 * Radio map using all entries near the location
	 * 
	 * @param access_token    The access token(api key)
	 * @param coordinates_lat Latitude
	 * @param coordinates_lon Longitude
	 * @param floor           The floor number
	 * @return JSON String with all the radio measurements of a floor
	 */
	public String radioByCoordinatesFloor(String access_token, String coordinates_lat, String coordinates_lon,
			String floor) {

		RestClient client = new RestClient();
		setPath("/anyplace/position/radio_download_floor");
		Map<String, String> params = new HashMap<>();
		params.put("access_token", access_token);
		params.put("coordinates_lat", coordinates_lat);
		params.put("coordinates_lon", coordinates_lon);
		params.put("floor_number", floor);
		params.put("mode", "foo");
		return JsonHelper.jsonResponse(STATUS_OK, client.doPost(params, getHost(), getPath()));

	}

	/**
	 * Radio map using all entries near the location
	 *
	 * @param coordinates_lat Latitude
	 * @param coordinates_lon Longitude
	 * @param floor           The floor number
	 * @return JSON String with all the radio measurements of a floor
	 */
	public String radioByCoordinatesFloor(String coordinates_lat, String coordinates_lon,
										  String floor){
		return radioByCoordinatesFloor(token, coordinates_lat, coordinates_lon, floor);

	}

	/**
	 * Get a radio map in a floor with a range
	 * 
	 * @param buid            The building ID
	 * @param floor           The floor number
	 * @param coordinates_lat The Latitude
	 * @param coordinates_lon The Longitude
	 * @param range           The desired range
	 * @return JSON String with the radiomap measurements of the floor
	 */
	public String radioByBuildingFloorRange(String buid, String floor, String coordinates_lat, String coordinates_lon,
			String range) {

		RestClient client = new RestClient();
		setPath("/anyplace/position/radio_by_floor_bbox");
		Map<String, String> params = new HashMap<>();
		params.put("buid", buid);
		params.put("floor_number", floor);
		params.put("coordinates_lat", coordinates_lat);
		params.put("coordinates_lon", coordinates_lon);
		params.put("range", range);

		return JsonHelper.jsonResponse(STATUS_OK, client.doPost(params, getHost(), getPath()));
	}

	/**
	 * Radiomap using all the entries of an entire floor of a building. The
	 * measurements are also stored locally for use by estimatePositionOffline
	 * 
	 * @param access_token The access token(api key)
	 * @param buid         The building ID
	 * @param floor        The floor number
	 * @return JSON String with the radio measurement of the floor
	 */
	public String radioByBuildingFloor(String access_token, String buid, String floor){

		RestClient client = new RestClient();
		setPath("/anyplace/position/radio_by_building_floor");
		Map<String, String> params = new HashMap<>();
		params.put("access_token", access_token);
		params.put("buid", buid);
		params.put("floor", floor);

		String response = client.doPost(params, getHost(), getPath());

		JSONObject obj;
		int statusCode;
		try {
			obj = new JSONObject(response);
			statusCode = obj.getInt("status_code");
		} catch (JSONException e) {
			return JsonHelper.printError(e, "radioByBuildingFloor");
		}

		if (statusCode == 200) {
			String map_url_parameters;
			try {
				map_url_parameters = obj.getString("map_url_parameters");
			} catch (JSONException e) {
				return JsonHelper.printError(e, "radioByBuildingFloor");
			}
			byte[] parameters = client.getFileWithPost(getHost(), map_url_parameters);
			String map_url_mean ;
			try {
				map_url_mean = obj.getString("map_url_mean");
			} catch (JSONException e) {
				return JsonHelper.printError(e, "radioByBuildingFloor");
			}
			byte[] mean = client.getFileWithPost(getHost(), map_url_mean);
			String map_url_weights ;
			try {
				map_url_weights = obj.getString("map_url_weights");
			} catch (JSONException e) {
				return JsonHelper.printError(e, "radioByBuildingFloor");
			}
			byte[] weights = client.getFileWithPost(getHost(), map_url_weights);

			String temp = cache + buid + "/" + floor;
			File dir = new File(temp);
			boolean t = dir.mkdirs();
			if (!t){
				return JsonHelper.printError(new AnyplaceException("Couldn't create directory.", new Exception()), "radioByBuildingFloor");
			}
			String indoor_radiomap_parameters = cache + buid + "/" + floor + "/indoor_radiomap_parameters.txt";
			String indoor_radiomap_mean = cache + buid + "/" + floor + "/indoor_radiomap_mean.txt";
			String indoor_radiomap_weights = cache + buid + "/" + floor + "/indoor_radiomap_weights.txt";

			File f1 = new File(indoor_radiomap_mean);
			File f2 = new File(indoor_radiomap_parameters);
			File f3 = new File(indoor_radiomap_weights);

			try {
				boolean t1 =f1.createNewFile();
				boolean t2 =f2.createNewFile();
				boolean t3 =f3.createNewFile();

				if ( !t1 || !t2 || !t3){
					return JsonHelper.printError(new AnyplaceException("Couldn't create file.", new Exception()), "radioByBuildingFloor");
				}
			} catch (IOException e1) {
				return JsonHelper.printError(e1, "radioByBuildingFloor");
			}

			try {
				FileOutputStream outputStream = new FileOutputStream(indoor_radiomap_parameters);
				outputStream.write(parameters);
				outputStream.close();

				outputStream = new FileOutputStream(indoor_radiomap_mean);
				outputStream.write(mean);
				outputStream.close();

				outputStream = new FileOutputStream(indoor_radiomap_weights);
				outputStream.write(weights);
				outputStream.close();
			} catch (IOException e) {
				return JsonHelper.printError(e, "radioByBuildingFloor");
			}

		}

		else {
			System.out.println("Bad response");
		}

		return JsonHelper.jsonResponse(STATUS_OK, response);

	}

	/**
	 * Radiomap using all the entries of an entire floor of a building. The
	 * measurements are also stored locally for use by estimatePositionOffline
	 *
	 * @param buid         The building ID
	 * @param floor        The floor number
	 * @return JSON String with the radio measurement of the floor
	 */
	public String radioByBuildingFloor( String buid, String floor) {

		return radioByBuildingFloor(token, buid, floor);
	}

	/**
	 * Get an estimation on the user's position based on the APs.
	 * 
	 * @param buid      The building ID
	 * @param floor     The floor number
	 * @param aps       A table of bssid and rss fingerprints in the form of a JSON
	 * @param algorithm The number of the desired algorithm
	 * @return JSON String containing the lat and lon
	 */
	public String estimatePosition(String buid, String floor, String[] aps, String algorithm)  {
		RestClient client = new RestClient();
		setPath("/anyplace/position/estimate_position");
		Map<String, String> params = new HashMap<>();
		params.put("buid", buid);
		params.put("floor", floor);
		String addb = "\\\"bssid\\\"";
		String addr = "\\\"rss\\\"";
		String addq = "\\\"";
		StringBuilder apBuilder = new StringBuilder("[");
		for (String s : aps) {


			JSONObject obj;
			String bssid;
			int rss;
			try {
				obj = new JSONObject(s);
				bssid = obj.getString("bssid");
				rss = obj.getInt("rss");
			} catch (JSONException e) {
				return JsonHelper.printError(e, "estimatePosition");
			}

			apBuilder.append("{").append(addb).append(":").append(addq).append(bssid).append(addq).append(",").append(addr).append(":").append(rss).append("},");

		}
		String ap = apBuilder.toString();
		ap = ap.substring(0, ap.length() - 1) + "]";

		params.put("APs", ap);
		params.put("algorithm_choice", algorithm);



		return JsonHelper.jsonResponse(STATUS_OK, client.doPost(params, getHost(), getPath()));
	}

	/**
	 * Get an estimation on the user's position based on the APs while offline.
	 * Needs the radiomap to be stored locally.
	 * 
	 * @param buid      The building ID
	 * @param floor     The floor number
	 * @param aps       A table of bssid and rss fingerprints in the form of a JSON
	 * @param algorithm The number of the desired algorithm
	 * @return String with the lat and lon
	 */
	public String estimatePositionOffline(String buid, String floor, String[] aps, String algorithm)   {

		ArrayList<LogRecord> list = new ArrayList<>();


		for (String ap : aps) {

			JSONObject obj;
			String bssid;
			int rss;
			try {
				obj = new JSONObject(ap);
				bssid = obj.getString("bssid");
				rss = obj.getInt("rss");
			} catch (JSONException e) {
				return JsonHelper.printError(e, "estimatePositionOffline");
			}

			list.add(new LogRecord(bssid, rss));
		}

		int al = Integer.parseInt(algorithm);
		File file = new File(cache + buid + "/" + floor + "/indoor_radiomap_mean.txt");
		RadioMap radio;
		try {
			radio = new RadioMap(file);

		} catch (Exception e) {

		     return JsonHelper.printError(e, "estimatePositionOffline");


		}


		String response = Algorithms.ProcessingAlgorithms(list, radio, al);
		System.out.println(response);
		String[] coords = new String[0];
		if (response != null) {
			coords = response.split(" ");
		}
		JSONObject r = new JSONObject();
		try {
			r.put("status", STATUS_OK);
			r.put("lon", coords[0]);
			r.put("lat", coords[1]);
		} catch (JSONException e) {
			return JsonHelper.printError(e, "estimatePositionOffline");
		}

		return r.toString();

	}


	public String getHost() {
		return host;
	}

	public String getCache() {
		return cache;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getPort() {
		return port;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public void setCache(String cache) {
		this.cache = cache;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}
}
