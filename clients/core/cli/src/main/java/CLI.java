/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Marcos Antonios Charalambous, Constandinos Demetriou, Christakis Achilleos
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

import cy.ac.ucy.cs.anyplace.core.Anyplace;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Scanner;

public class CLI {

	public final static String parametersFilename = ".anyplace";
	public static String access_token, host, port, cache;

	// CA: USE apache tool for arguments (FUTURE)
    // -estimatePosition --debug -buid=XXX

	/**
	 * This function initializes the parameters.
	 * 
	 * @param line A line from the file
	 */
	public static void initialize_parameters(String line) {
		String word[] = line.split(" ");
		if (word[0].equals("access_token")) {
			access_token = new String(word[1]);
		} else if (word[0].equals("host")) {
			host = new String(word[1]);
		} else if (word[0].equals("port")) {
			port = new String(word[1]);
		} else if (word[0].equals("cache")) {
			cache = new String(word[1]);
		}
	}

	/**
	 * This function reads the parameters file.
	 */
	public static void read_paramenters() {
		try {
		    // TODO CA: check that this work on Windows!
            String home = System.getProperty("user.home");
            File parametersFile = new File(home + "/" + parametersFilename);
			Scanner sc = new Scanner(parametersFile);
			while (sc.hasNext()) {
				initialize_parameters(sc.nextLine());
			}
			sc.close();
		} catch (FileNotFoundException e) {
			System.out.println("Error: File " + parametersFilename + " not found!");
			System.out.println("Make sure that file " + parametersFilename + " is in the correct format!");
			System.out.println("The format of the file is:");
			System.out.println("host ap-dev.cs.ucy.ac.cy");
			System.out.println("port 43");
			System.out.println("cache res/");
			System.out.println("access_token <api_key> " +
                "[The API key has be generated based on your google account through the anyplace architect]");
			// TODO CA: when this error is shown, prompt, with Y or N to initialize that file with defaults
            // if yes, then read parameters again
			System.exit(-1);
		}
	}

	/**
	 * The main entry to our library. Inside the main function you can find the
	 * implementation of the command line interface.
	 * 
	 * @param args
	 */
	public static void main(String[] args) throws JSONException {
		/*
		 TODO CA the following is a test case! make it one, with some desc!
		 * String buid = "username_1373876832005"; String pois_to =
		 * "poi_064f4a01-07bd-45fa-9579-63fa197d3d90"; String coordinates_la =
		 * "35.14414934169342"; String coordinates_lo = "33.41130472719669"; String
		 * floor = "0"; String pois_from = "poi_88a34fd5-75bd-4601-81dc-fe5aef69bd3c";
		 */

		String response;

		read_paramenters();
		if (access_token == null || host == null || port == null || cache == null || access_token.isEmpty()
				|| host.isEmpty() || port.isEmpty() || cache.isEmpty()) {
			System.out.println("Make sure that file " + parametersFilename + " is in the correct format!");
			System.out.println("The format of the file is:");
			System.out.println("host ap-dev.cs.ucy.ac.cy");
			System.out.println("port 43");
			System.out.println("cache res/");
			System.out.println(
					"access_token <api_key> [The API key has be generated based on your google account through the anyplace architect]");
			System.exit(0);
		}

		if (args.length == 0) {/* Menu of options is shown if no arguments are given. */
			System.out.println("Please use one of the available options available options:");

			System.out.println("Navigation");
			System.out.println("----------");
			System.out.println("-poiDetails: Get Point of Interest details.");
			System.out.println("-navigationXY: Get Navigation instructions from a given	location to a POI.");
			System.out.println("-navPoiToPoi: Get Navigation instructions between 2 POIs");

			System.out.println("\nMapping(Public)");
			System.out.println("---------------");
			System.out.println("-buildingAll: Get all annotated buildings.");
			System.out.println("-buildingsByCampus: Get all buildings for a campus.");
			System.out.println("-buildingsByBuildingCode: Get all buildings with the same code.");
			System.out.println("-nearbyBuildings: Get annotated buildings near you (50 meters radius).");
			System.out.println("-allBuildingFloors: Get all floors of a building.");
			System.out.println("-allBuildingPOIs: Get all POIs inside a building.");
			System.out.println("-allBuildingFloorPOIs: Get all POIs inside a floor.");
			System.out.println("-connectionsByFloor: Get all POI connections inside a floor.");
			System.out.println("-heatmapByBuildingFloor: Get all positions with their respective Wi-Fi radio measurements.");

			System.out.println("\nBlueprints");
			System.out.println("----------");
			System.out.println("-floor64: Downloads the floor plan in a base64 png format (w/o prefix).");
			System.out.println("-floortiles: Fetches the floor plan tiles zip link.");

			System.out.println("\nPosition");
			System.out.println("--------");
			System.out.println(
					"-radioByCoordinatesFloor: Radiomap using all the entries near the coordinate parameters.");
			System.out.println("-radioBuidFloor: Radiomap based on building and floor.");
			System.out.println("-radioBuidFloorRange: Radiomap by limiting the range.");
			System.out.println("-estimatePosition: Estimate the location of the user.");
			System.out
					.println("-estimatePosOffline: Estimate the location of the user offline. Needs the radiomap file");

		} else {
			Anyplace client = new Anyplace(host, port, cache);

			if (args[0].equals("-poiDetails")) {
				if (args.length != 2) {
					System.out.println("Usage: -poiDetails <pois>");
					System.exit(0);
				}
				String pois = args[1];
				response = client.poiDetails(access_token, pois);
				System.out.println(response + "\n");
			} else if (args[0].equals("-navigationXY")) {
				if (args.length != 6) {
					System.out
							.println("Usage: -navigationXY <buid> <floor> <pois_to> <coordinates_la> <coordinates_lo>");
					System.exit(0);
				}
				String buid = args[1];
				String floor = args[2];
				String pois_to = args[3];
				String coordinates_la = args[4];
				String coordinates_lo = args[5];
				response = client.navigationXY(access_token, pois_to, buid, floor, coordinates_la, coordinates_lo);
				System.out.println(response + "\n"); /* .substring(0, 100) */
			} else if (args[0].equals("-navPoiToPoi")) {
				if (args.length != 3) {
					System.out.println("Usage: -navPoiToPoi <pois_from> <pois_to>");
					System.exit(0);
				}
				String pois_from = args[1];
				String pois_to = args[2];
				response = client.navigationPoiToPoi(access_token, pois_to, pois_from);
				System.out.println(response + "\n");
			} else if (args[0].equals("-buildingAll")) {
				response = client.buildingAll();
				System.out.println(response + "\n");
			} else if (args[0].equals("-buildingsByCampus")) {
				if (args.length != 2) {
					System.out.println("Usage: -buildingsByCampus <cuid>");
					System.exit(0);
				}
				String cuid = args[1];
				response = client.buildingsByCampus(cuid);
				System.out.println(response + "\n");
			} else if (args[0].equals("-buildingsByBuildingCode")) {
				if (args.length != 2) {
					System.out.println("Usage: -buildingsByBuildingCode <bucode>");
					System.exit(0);
				}
				String bucode = args[1];
				response = client.buildingsByBuildingCode(bucode);
				System.out.println(response + "\n");
			} else if (args[0].equals("-nearbyBuildings")) {
				if (args.length != 3) {
					System.out.println("Usage: -nearbyBuildings <coordinates_lat> <coordinates_lon>");
					System.exit(0);
				}
				String coordinates_lat = args[1];
				String coordinates_lon = args[2];
				response = client.nearbyBuildings(access_token, coordinates_lat, coordinates_lon);
				System.out.println(response + "\n");
			} else if (args[0].equals("-allBuildingFloors")) {
				if (args.length != 2) {
					System.out.println("Usage: -allBuildingFloors <buid>");
					System.exit(0);
				}
				String buid = args[1];
				response = client.allBuildingFloors(buid);
				System.out.println(response + "\n");
			} else if (args[0].equals("-allBuildingPOIs")) {
				if (args.length != 2) {
					System.out.println("Usage: -allBuildingPOIs <buid>");
					System.exit(0);
				}
				String buid = args[1];
				response = client.allBuildingPOIs(buid);
				System.out.println(response + "\n");
			} else if (args[0].equals("-allBuildingFloorPOIs")) {
				if (args.length != 3) {
					System.out.println("Usage: -allBuildingPOIs <buid> <floor>");
					System.exit(0);
				}
				String buid = args[1];
				String floor = args[2];
				response = client.allBuildingFloorPOIs(buid, floor);
				System.out.println(response + "\n");
			} else if (args[0].equals("-connectionsByFloor")) {
				if (args.length != 3) {
					System.out.println("Usage: -connectionsByFloor <buid> <floor>");
					System.exit(0);
				}
				String buid = args[1];
				String floor = args[2];
				response = client.connectionsByFloor(buid, floor);
				System.out.println(response + "\n");
			} else if (args[0].equals("-heatmapBuidFloor")) {
				if (args.length != 3) {
					System.out.println("Usage: -heatmapBuidFloor <buid> <floor>");
					System.exit(0);
				}
				String buid = args[1];
				String floor = args[2];
				response = client.radioheatMapBuildingFloor(buid, floor);
				System.out.println(response + "\n");
			} else if (args[0].equals("-floor64")) {
				if (args.length != 3) {
					System.out.println("Usage: -floor64 <buid> <floor>");
					System.exit(0);
				}
				String buid = args[1];
				String floor = args[2];
				response = client.floorplans64(access_token, buid, floor);
				System.out.println(response.substring(0, 100) + "\n");
			} else if (args[0].equals("-floortiles")) {
				if (args.length != 3) {
					System.out.println("Usage: -floortiles <buid> <floor>");
					System.exit(0);
				}
				String buid = args[1];
				String floor = args[2];
				response = client.floortiles(access_token, buid, floor);
				System.out.println(response + "\n"); /* .substring(0, 100) */
			} else if (args[0].equals("-radioByCoordinatesFloor")) {
				if (args.length != 4) {
					System.out.println("Usage: -radioByCoordinatesFloor <coordinates_lat> <coordinates_lon> <floor>");
					System.exit(0);
				}
				String coordinates_lat = args[1];
				String coordinates_lon = args[2];
				String floor = args[3];
				response = client.radioByCoordinatesFloor(access_token, coordinates_lat, coordinates_lon, floor);
				System.out.println(response + "\n"); /* .substring(0, 100) */
			} else if (args[0].equals("-radioBuidFloor")) {
				if (args.length != 3) {
					System.out.println("Usage: -radioBuidFloor <buid> <floor>");
					System.exit(0);
				}
				String buid = args[1];
				String floor = args[2];
				response = client.radioByBuildingFloor(access_token, buid, floor);
				System.out.println(response + "\n"); /* .substring(0, 100) */
			} else if (args[0].equals("-radioBuidFloorRange")) {
				if (args.length != 3) {
					System.out.println(
							"Usage: -radioBuidFloorRange <buid> <foor> <coordinates_lat> <coordinates_lon> <range>");
					System.exit(0);
				}
				String buid = args[1];
				String floor = args[2];
				String coordinates_lat = args[3];
				String coordinates_lon = args[4];
				String range = args[5];
				response = client.radioByBuildingFloorRange(buid, floor, coordinates_lat, coordinates_lon, range);
				System.out.println(response + "\n"); /* .substring(0, 100) */
			} else if (args[0].equals("-estimatePosition")) {
				if (args.length != 5) {
					System.out.println("Usage: -estimatePosition <operating_system> <buid> <floor> <algorithm>");
					System.exit(0);
				}
				String operating_system = args[1];
				String buid = args[2];
				String floor = args[3];
				String algorithm = args[4];

				String cmd[] = new String[3];
				if (operating_system.equals("linux")) {
					cmd[0] = "/bin/sh";
					cmd[1] = "-c";
					cmd[2] = "sudo iwlist wlo1 scan | awk  '/Address/ {print $5}; /level/ {print $3}' |  cut -d\"=\" -f2 ";
				} else if (operating_system.equals("mac")) {
					cmd[0] = "/bin/sh";
					cmd[1] = "-c";
					cmd[2] = "/System/Library/PrivateFrameworks/Apple80211.framework/Versions/A/Resources/airport -s | grep ':' | tr -s ' ' | cut -d' ' -f3 -f4| tr ' ' '\n'";

				} else {
					System.out.println("Only linux and mac are the available operating systems");
					System.exit(0);
				}

				String aps[] = new String[200];
				Process p;
				String s, temp;
				int counter = 0;
				try {
					p = Runtime.getRuntime().exec(cmd);

					BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
					while ((s = br.readLine()) != null && counter <= 20) {
						temp = "{\"bssid\":\"";
						temp += s;
						temp += "\",\"rss\":";
						s = br.readLine();
						temp += s;
						temp += "}";
						temp = temp.toLowerCase();
						aps[counter++] = temp;
					}
					p.destroy();
					br.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				aps = Arrays.copyOf(aps, counter);
				for (int j = 0; j < counter; j++) {
					System.out.println(aps[j]);
				}
				response = client.estimatePosition(buid, floor, aps, algorithm);
				System.out.println(response + "\n"); /* .substring(0, 100) */
			} else if (args[0].equals("-estimatePosOffline")) {
				if (args.length != 5) {
					System.out.println("Usage: -estimatePosOffline <operating_system> <buid> <floor> <algorithm>");
					System.exit(0);
				}
				String operating_system = args[1];
				String buid = args[2];
				String floor = args[3];
				String algorithm = args[4];

				String cmd[] = new String[3];
				if (operating_system.equals("linux")) {
					cmd[0] = "/bin/sh";
					cmd[1] = "-c";
					cmd[2] = "sudo iwlist wlo1 scan | awk  '/Address/ {print $5}; /level/ {print $3}' |  cut -d\"=\" -f2 ";
				} else if (operating_system.equals("mac")) {
					cmd[0] = "/bin/sh";
					cmd[1] = "-c";
					cmd[2] = "/System/Library/PrivateFrameworks/Apple80211.framework/Versions/A/Resources/airport -s | grep ':' | tr -s ' ' | cut -d' ' -f3 -f4| tr ' ' '\n'";
				} else {
					System.out.println("Only linux and mac are the available operating systems");
					System.exit(0);
				}

				String aps[] = new String[200];
				Process p;
				String s, temp;
				int counter = 0;
				try {
					p = Runtime.getRuntime().exec(cmd);

					BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
					while ((s = br.readLine()) != null && counter <= 20) {
						temp = "{\"bssid\":\"";
						temp += s;
						temp += "\",\"rss\":";
						s = br.readLine();
						temp += s;
						temp += "}";
						temp = temp.toLowerCase();
						aps[counter++] = temp;
					}
					p.destroy();
					br.close();
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				aps = Arrays.copyOf(aps, counter);

				response = client.estimatePositionOffline(buid, floor, aps, algorithm);

				//Location location = client.EstimatePositionOffline(..);

				System.out.println(response + "\n"); /* .substring(0, 100) */
			} else {
				System.out.println("Unknown argument: " + args[0]);
			}

		}
	}
}
