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

import com.dmsl.anyplace.buildings.AllBuildings;
import com.dmsl.anyplace.buildings.Building;
import com.dmsl.anyplace.buildings.clean.CleanBuilding;
import com.dmsl.anyplace.buildings.clean.CleanPoi;
import com.dmsl.anyplace.buildings.connections.Connections;
import com.dmsl.anyplace.buildings.floors.Floor;
import com.dmsl.anyplace.buildings.floors.Floors;
import com.dmsl.anyplace.buildings.pois.Pois;
import com.dmsl.anyplace.fingerprints.FingerPrints;
import com.google.gson.Gson;

import java.io.*;

public class DataConnector {

	public static String BUILDING_ID = "username_1373876832005"; // cs ucy
//	public static final String BUILDING_ID="building_d1036173-bbfb-46bd-85dd-f76f05e07308_1424273600734"; // MALL OF CYPRUS
//	public static final String BUILDING_ID = "building_ac9bc473-1bf7-4bea-a884-2b5f1de1672a_1428609552379"; // hotel

    /**
     * All paths
     */
    public static final String DATA_PATH = "./data/";
	public static final String DATASETS_PATH = DATA_PATH + "datasets/";
	public static final String TRACES_PATH = DATA_PATH + "traces/";
	public static final String GRAPH_PATH = DATA_PATH + "graphs/";
	public static final String GRAPH_TEST_PATH = DATA_PATH + "testGraphs/";
	public static final String SIMULATION_PATH = DATA_PATH + "simulations/";
	public static final String FINGERPRINTS_PATH = DATA_PATH + "fingerprints/";
	public static final String FILE_FORMAT = "pajek";

	private AllBuildings buildings;
	private Floors floors;

	//Selected building
	private CleanBuilding selectedBuilding;

    public static void main(String[] args) throws IOException {
        if(args.length!=1){
			System.out.println("Using default BUILDING_ID:"+ BUILDING_ID);
		}else{
        	BUILDING_ID = args[0];
			System.out.println("Using BUILDING_ID:"+ BUILDING_ID);

		}
        DataConnector main = new DataConnector();
        main.selectedBuilding.writeToFile();
        main.selectedBuilding.writeToFileTypesDescriptions();
    }

	public DataConnector() {
		loadBuildings(false);
	}

	public void loadBuildings(boolean devMode) {
		try {
			Connections connections;
			this.buildings = Api.getAllBuildings(devMode);
			for (Building build : this.buildings.getBuildings()) {

				// Get the building
				if (build.getBuid().equals(BUILDING_ID)) {
					System.out.println("Found BUILDING: "+BUILDING_ID);

					connections = (Api.getBuildingConnections(devMode,build.getBuid()));
					this.floors = Api.getBuildingFloor(devMode,build.getBuid());
					Pois pois = Api.getPOIsByBuilding(devMode,build.getBuid());

					this.selectedBuilding = new CleanBuilding(build.getBuid(), build.getName(), build.getCoordinatesLat(),
							build.getCoordinatesLon(), pois, connections);
					break;
				}
			}
			
			for (Floor f : floors.getFloors()) {
				Api.getRadiomapFloor(selectedBuilding.getBuid(), f.getFloorNumber());
			}
			System.out.println("Reading fingerprints");

		} catch (Exception e) {
			e.printStackTrace();
			System.out.println(e.getMessage());
		}
	}

	private FingerPrints[] parseFile(File file) throws Exception {
		BufferedReader bf = new BufferedReader(new FileReader(file));
		String line = "";

		// Read the first line
		line = bf.readLine();

		// Get number of fingerprints
		String toks[] = line.split(" ");
		int size = Integer.parseInt(toks[1]);

		FingerPrints[] fp = new FingerPrints[size];

		Gson gson = new Gson();
		// Read the fingerprints
		for (int i = 0; i < size; i++) {
			line = bf.readLine();
			if (line == null)
				throw new Exception("File is not valid");

			toks = line.split("\\|");

			int id = Integer.parseInt(toks[0].trim());
			String jsonString = toks[1].trim();

			FingerPrints f = gson.fromJson(jsonString, FingerPrints.class);

			fp[id] = f;
		}

		bf.close();

		return fp;
	}

	public boolean writeFingerPrintsToFile(String filename, FingerPrints[] fp) throws IOException {

		File path = new File(DataConnector.FINGERPRINTS_PATH);
		if (!path.exists()) {
			if(path.mkdirs())
                System.out.println("[Info] Directory: " + DataConnector.FINGERPRINTS_PATH + " created");

		}
		File file = new File(DataConnector.FINGERPRINTS_PATH + filename + "." + DataConnector.FILE_FORMAT);
		if (file.isDirectory()) {
			System.err.println("[Error]: This is a directory");
			return false;
		}
		if (file.exists()) {
			System.out.println("[Info]: File: " + filename + " would be overwritten");
		}

		BufferedWriter bf = new BufferedWriter(new FileWriter(file));
		bf.write("*Fingerprints " + fp.length + "\n");

		int count = 0;
		for (FingerPrints p : fp) {
			Gson gson = new Gson();
			String jsonString = gson.toJson(p, FingerPrints.class);
			bf.write(String.valueOf(count) + "|" + jsonString + "\n");
			count++;
		}

		bf.close();
		System.out.println("[Info]: File: " + filename + " successfully saved");

		return true;
	}

	public void printGraph() {
		CleanPoi[] pois = selectedBuilding.getVertices();
		int i = 0;
		for (CleanPoi p : pois) {
			System.out.println(i + "->" + p.getNeighbours() + " " + p.getName());
			i++;
		}
	}


}
