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

import com.dmsl.anyplace.buildings.clean.CleanBuilding;
import com.dmsl.anyplace.buildings.clean.CleanPoi;
import com.dmsl.anyplace.fingerprints.FingerPrints;
import com.google.gson.Gson;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class ExperimentsMain {

	public static void main(String[] args) throws Exception {
		// load building
		CleanBuilding building = new CleanBuilding(DataConnector.BUILDING_ID, false);
		
		// construct building radiomap and new floors radiomaps
		String[] floors = new String[] {
				DataConnector.FINGERPRINTS_PATH + DataConnector.BUILDING_ID + "/" + "-1.fing",
				DataConnector.FINGERPRINTS_PATH + DataConnector.BUILDING_ID + "/" + "0.fing",
				DataConnector.FINGERPRINTS_PATH + DataConnector.BUILDING_ID + "/" + "1.fing",
				DataConnector.FINGERPRINTS_PATH + DataConnector.BUILDING_ID + "/" + "2.fing"
				};

		// run the creator
		new BuildingRMCreator(floors, building.getBuid());

		int range = 100;

		// split all floors
		RadioMapSplit rms = new RadioMapSplit(building.getVertices());
		rms.radioMapSplit(DataConnector.FINGERPRINTS_PATH + DataConnector.BUILDING_ID + "/"
				+ "-1.fing_new", "-1", range);
		rms.radioMapSplit(DataConnector.FINGERPRINTS_PATH + DataConnector.BUILDING_ID + "/"
				+ "0.fing_new", "0", range);
		rms.radioMapSplit(DataConnector.FINGERPRINTS_PATH + DataConnector.BUILDING_ID + "/"
				+ "1.fing_new", "1", range);
		rms.radioMapSplit(DataConnector.FINGERPRINTS_PATH + DataConnector.BUILDING_ID + "/"
				+ "2.fing_new", "2", range);

		// for testing purpose
		System.out.println("Total pois: " + building.getVertices().length);
		System.out.println("Total fps: " + rms.countTotalFps());
		System.out.println("Total empty pois: " + rms.countEmptyPois());

		// get all vertices and save finger prints
		building.setVertices(rms.loadNewVertices());

		// get fps
		FingerPrints[] fPrints = new FingerPrints[building.getVertices().length];
		for (CleanPoi p : building.getVertices()) {
			fPrints[p.getId()] = new Gson().fromJson(p.getFp(),
					FingerPrints.class);
		}

		// save fingerprints
		writeFingerPrintsToFile(building.getBuid(), fPrints);

		// load fps from file
		building.parseFpFile();

		// number of blocks
		int N = 15;
		float lostProb = 30;

//		DatasetCreator dataset = new DatasetCreator(Main.UCY_BUILDING_ID,
//				1456737260395L, false);
//
//		// now run
//		AlgorithmBlocks algo = new SSAlgorithmBlocks(building.getVertices(), N,
//				dataset.random, lostProb);
//		
//		CreateLookAhead lookAhead = new CreateLookAhead(Main.FINGERPRINTS_PATH+Main.UCY_BUILDING_ID+"/all.pajek", 30);
//
//		SimulationBlocks sim = new SimulationBlocks(N, building, dataset, algo,lookAhead.getLookAhead());
//		//sim.runSimulation();
//		//sim.runSSSimulation();
//		sim.writeNodesToFile();
//		sim.writeToFile();
//		sim.outResults();

	}

	public static boolean writeFingerPrintsToFile(String filename,
			FingerPrints[] fp) throws IOException {
//		String[] r = filename.split("_");
//		String bid = r[0] + "_" + r[1];
		String bid = filename;

		File path = new File(DataConnector.FINGERPRINTS_PATH + bid + "/");
		if (!path.exists()) {
			path.mkdir();
			System.out.println("[Info] Directory: " + DataConnector.FINGERPRINTS_PATH
					+ bid + "/" + " created");

		}
		File file = new File(DataConnector.FINGERPRINTS_PATH + bid + "/" + filename
				+ "." + DataConnector.FILE_FORMAT);
		if (file.isDirectory()) {
			System.err.println("[Error]: This is a directory");
			return false;
		}
		if (file.exists()) {
			System.out.println("[Info]: File: " + filename
					+ " would be overwritten");
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
		System.out.println("[Info]: File: " + filename + " successfuly saved");

		return true;
	}
}
