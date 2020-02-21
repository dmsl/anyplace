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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import static com.sun.deploy.registration.InstallCommands.STATUS_OK;

public class Anyplace {

	private String host;
	private String path;
	private String port;
	private String cache;

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
		Map<String, String> params = new HashMap<String, String>();
		params.put("access_token", access_token);
		params.put("pois", pois);

		String response = client.doPost(params, getHost(), getPath());
		return response;
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
	 * @return
	 */
	public String navigationXY(String access_token, String pois_to, String buid, String floor, String coordinates_lat,
			String coordinates_lon) throws JSONException {

		RestClient client = new RestClient();
		setPath("/anyplace/navigation/route_xy");
		Map<String, String> params = new HashMap<String, String>();
		params.put("access_token", access_token);
		params.put("pois_to", pois_to);
		params.put("buid", buid);
		params.put("floor_number", floor);
		params.put("coordinates_lat", coordinates_lat);
		params.put("coordinates_lon", coordinates_lon);

		String response = client.doPost(params, getHost(), getPath());

		JSONObject obj = new JSONObject(response);
		int statusCode = obj.getInt("status_code");

		if (statusCode == 200) {
		    // CHECK CA
		}
		else {
			System.out.println("Bad response");
		}

		return response;

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
	public String navigationPoiToPoi(String access_token, String pois_to, String pois_from) throws JSONException {

		RestClient client = new RestClient();
		setPath("/anyplace/navigation/route");
		Map<String, String> params = new HashMap<String, String>();
		params.put("access_token", access_token);
		params.put("pois_from", pois_from);
		params.put("pois_to", pois_to);

		String response = client.doPost(params, getHost(), getPath());

		JSONObject obj = new JSONObject(response);
		int statusCode = obj.getInt("status_code");

		if (statusCode == 200) {

		}

		else {
			System.out.println("Bad response");
		}
		return response;
	}

	/**
	 * Get all annotated buildings
	 * 
	 * @return The response JSON as a String
	 */
	public String buildingAll() {
		RestClient client = new RestClient();
		setPath("/anyplace/mapping/building/all");

		return client.doPost(null, getHost(), getPath());
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
		Map<String, String> params = new HashMap<String, String>();
		params.put("cuid", cuid);

		String response = client.doPost(params, getHost(), getPath());
		return response;
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
		Map<String, String> params = new HashMap<String, String>();
		params.put("bucode", bucode);

		return client.doPost(params, getHost(), getPath());
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
		Map<String, String> params = new HashMap<String, String>();
		params.put("access_token", access_token);
		params.put("coordinates_lat", coordinates_lat);
		params.put("coordinates_lon", coordinates_lon);

		String response = client.doPost(params, getHost(), getPath());
		return response;
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
		Map<String, String> params = new HashMap<String, String>();
		params.put("buid", buid);

		String response = client.doPost(params, getHost(), getPath());

		return response;
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
		Map<String, String> params = new HashMap<String, String>();
		params.put("buid", buid);

		String response = client.doPost(params, getHost(), getPath());

		// TODO CA: must return json object by default (IN ALL CASES)
        // must also have AllBuildingPOI wrappers that will return objects

		return response;
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
		Map<String, String> params = new HashMap<String, String>();
		params.put("buid", buid);
		params.put("floor_number", floor);

		String response = client.doPost(params, getHost(), getPath());

		return response;
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
		Map<String, String> params = new HashMap<String, String>();
		params.put("buid", buid);
		params.put("floor_number", floor);

		String response = client.doPost(params, getHost(), getPath());

		return response;
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
		Map<String, String> params = new HashMap<String, String>();
		params.put("buid", buid);
		params.put("floor", floor);
		setPath("/anyplace/mapping/radio/heatmap_building_floor");

		String response = client.doPost(params, getHost(), getPath());

		return response;

	}

	/**
	 * Download the floor plan in base64 png format. It also stores the file localy
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
		Map<String, String> params = new HashMap<String, String>();
		params.put("access_token", access_token);
		params.put("buid", buid);
		params.put("floor", floor);

		String response = client.doPost(params, getHost(), getPath());

		String filename = cache + buid + "/" + floor + "/" + "floorplan.png";

		try {
			File outputfile = new File(filename);
			ImageIO.write(decodeToImage(response), "png", outputfile);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return response;
	}

	/**
	 * Download the floor plan tiles in a zip file
	 * 
	 * @param access_token The access token(api key)
	 * @param buid         The building ID
	 * @param floor        The floor number
	 * @return JSON String containing the floor tile zip download url
	 */
	public String floortiles(String access_token, String buid, String floor) throws JSONException {

		RestClient client = new RestClient();
		setPath("/anyplace/floortiles/" + buid + "/" + floor);
		Map<String, String> params = new HashMap<String, String>();
		params.put("access_token", access_token);
		params.put("buid", buid);
		params.put("floor", floor);

		String response = client.doPost(params, getHost(), getPath());

		JSONObject obj = new JSONObject(response);
		int statusCode = obj.getInt("status_code");

		if (statusCode == 200) {
			String tiles_archive = obj.getString("tiles_archive");
			byte[] zip = client.getFileWithGet(getHost(), tiles_archive);
			String filename = "res/" + buid + "/" + floor + "/" + "floorPlanTiles.zip";

			try {
				FileOutputStream outputStream = new FileOutputStream(filename);

				outputStream.write(zip);
				outputStream.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			System.out.println("Bad response");
		}

		return response;
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
		Map<String, String> params = new HashMap<String, String>();
		params.put("access_token", access_token);
		params.put("coordinates_lat", coordinates_lat);
		params.put("coordinates_lon", coordinates_lon);
		params.put("floor_number", floor);
		params.put("mode", "foo");

		String response = client.doPost(params, getHost(), getPath());

		return response;

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
		Map<String, String> params = new HashMap<String, String>();
		params.put("buid", buid);
		params.put("floor_number", floor);
		params.put("coordinates_lat", coordinates_lat);
		params.put("coordinates_lon", coordinates_lon);
		params.put("range", range);

		String response = client.doPost(params, getHost(), getPath());

		return response;
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
	public String radioByBuildingFloor(String access_token, String buid, String floor) throws JSONException {

		RestClient client = new RestClient();
		setPath("/anyplace/position/radio_by_building_floor");
		Map<String, String> params = new HashMap<String, String>();
		params.put("access_token", access_token);
		params.put("buid", buid);
		params.put("floor", floor);

		String response = client.doPost(params, getHost(), getPath());

		JSONObject obj = new JSONObject(response);
		int statusCode = obj.getInt("status_code");

		if (statusCode == 200) {
			String map_url_parameters = obj.getString("map_url_parameters");
			byte[] parameters = client.getFileWithPost(getHost(), map_url_parameters);
			String map_url_mean = obj.getString("map_url_mean");
			byte[] mean = client.getFileWithPost(getHost(), map_url_mean);
			String map_url_weights = obj.getString("map_url_weights");
			byte[] weights = client.getFileWithPost(getHost(), map_url_weights);

			String temp = cache + buid + "/" + floor;
			File dir = new File(temp);
			dir.mkdirs();

			String indoor_radiomap_parameters = cache + buid + "/" + floor + "/indoor_radiomap_parameters.txt";
			String indoor_radiomap_mean = cache + buid + "/" + floor + "/indoor_radiomap_mean.txt";
			String indoor_radiomap_weights = cache + buid + "/" + floor + "/indoor_radiomap_weights.txt";

			File f1 = new File(indoor_radiomap_mean);
			File f2 = new File(indoor_radiomap_parameters);
			File f3 = new File(indoor_radiomap_weights);

			try {
				f1.createNewFile();
				f2.createNewFile();
				f3.createNewFile();

			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
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
				e.printStackTrace();
			}

		}

		else {
			System.out.println("Bad response");
		}

		return response;

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
	public String estimatePosition(String buid, String floor, String aps[], String algorithm) throws JSONException {
		RestClient client = new RestClient();
		setPath("/anyplace/position/estimate_position");
		Map<String, String> params = new HashMap<String, String>();
		params.put("buid", buid);
		params.put("floor", floor);
		String addb = "\\\"bssid\\\"";
		String addr = "\\\"rss\\\"";
		String addq = "\\\"";
		String ap = "[";
		for (int i = 0; i < aps.length; i++) {

			JSONObject obj = new JSONObject(aps[i]);
			String bssid = obj.getString("bssid");
			int rss = obj.getInt("rss");
			ap += "{" + addb + ":" + addq + bssid + addq + "," + addr + ":" + rss + "},";

		}
		ap = ap.substring(0, ap.length() - 1) + "]";

		params.put("APs", ap);
		params.put("algorithm_choice", algorithm);

		String response = client.doPost(params, getHost(), getPath());

		return response;
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
	public String estimatePositionOffline(String buid, String floor, String aps[], String algorithm) throws JSONException {
	    // CA: TODO handle exceptions (remove throws)

		ArrayList<LogRecord> list = new ArrayList<LogRecord>();

		for (int i = 0; i < aps.length; i++) {

			JSONObject obj = new JSONObject(aps[i]);
			String bssid = obj.getString("bssid");
			int rss = obj.getInt("rss");
			list.add(new LogRecord(bssid, rss));
		}

		int al = Integer.parseInt(algorithm);
		File file = new File(cache + buid + "/" + floor + "/indoor_radiomap_mean.txt");
		RadioMap radio = null;
		try {
			radio = new RadioMap(file);
        } catch (IOException e1) {
            JsonHelper.printError(e1);
		} catch (Exception e) {
		    // CA: TODO make this class
		    JsonHelper.printError(e);
			// TODO Auto-generated catch block
//			e.printStackTrace();
			JSONObject r = new JSONObject();
			r.put("status", STATUS_ERR);
            r.put("msg", e.getCause());
            return r.toString();
		}

		String response = Algorithms.ProcessingAlgorithms(list, radio, al);
        JSONObject r = new JSONObject();
        r.put("status", STATUS_OK);

        return response;
	}

    public Location EstimatePositionOffline(String buid, String floor, String aps[], String algorithm) {
	    JSONObject res = estimatePositionOffline(buid, floor, aps, algorithm);
	    Location l = new Location();
	    l.lat = res.get("lat");
        l.lon = res.get("lon");
        return l;
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

}
