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

import com.dmsl.anyplace.algorithms.CreateLookAhead;
import com.dmsl.anyplace.algorithms.DatasetCreator;
import com.dmsl.anyplace.algorithms.blocks.*;
import com.dmsl.anyplace.buildings.clean.CleanBuilding;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;


public class ExperimentsRun {

	private String confPath;
	private String buid;
	private String dataset;
	private int algorithm;
	private int k;
	private int maxSignalStrength;
	private int probability;

	public ExperimentsRun(String path) throws Exception {
		this.confPath = path;
	}

	public static void main(String[] args) throws Exception {
		if (args.length == 0)
			System.out.println("Please give configuration file");

		ExperimentsRun run = new ExperimentsRun(args[0]);
		run.parseConfigFile();
		run.runExperiment();
	}

	private void parseConfigFile() throws Exception {
		String line = null;
		String[] temp = null;
		File file = new File(this.confPath);
		BufferedReader reader = new BufferedReader(new FileReader(file));

		while ((line = reader.readLine()) != null) {
			if (line.isEmpty() || line.startsWith("#"))
				continue;
			if (line.contains("buid")) {
				temp = line.split("=");
				this.buid = temp[1];
			} else if (line.contains("dataset")) {
				temp = line.split("=");
				this.dataset = temp[1];
			} else if (line.contains("algorithm")) {
				temp = line.split("=");
				this.algorithm = Integer.parseInt(temp[1]);
			} else if (line.contains("maxSignalStrength")) {
				temp = line.split("=");
				this.maxSignalStrength = Integer.parseInt(temp[1]);
			} else if (line.contains("k")) {
				temp = line.split("=");
				this.k = Integer.parseInt(temp[1]);
			}else if (line.contains("probability")) {
				temp = line.split("=");
				this.probability = Integer.parseInt(temp[1]);
			}

		}
		System.out.println("Buid: " + this.buid + " dataset: " + this.dataset
				+ " algorithm: " + this.algorithm + " maxSignalStrength: "
				+ this.maxSignalStrength + " k: " + this.k +" probability: "+this.probability);
		reader.close();
	}

	private void runExperiment() throws Exception {
		// load building
		CleanBuilding building = new CleanBuilding(this.buid, false);

		// load fps from file
		building.parseFpFile();

		DatasetCreator dataset = new DatasetCreator(this.dataset, this.buid,
				1456737260395L);

		CreateLookAhead lookahead = new CreateLookAhead(DataConnector.FINGERPRINTS_PATH
				+ this.buid + "/all.pajek", this.maxSignalStrength);

		// now run
		AlgorithmBlocks algo = null;

		if (algorithm == 0) {
			algo = new RandomBlocks(building.getVertices(), k, dataset.random,
					probability);
		} else if (algorithm == 1) {
			algo = new BFSGlobalBlocks(building.getVertices(), k, dataset.random,
					probability);
		} else if (algorithm == 2) {
			// me1
			algo = new ME1(building.getVertices(), k, dataset.random,
					probability);

		} else if (algorithm == 3) {
			// cs
			algo = new CSAlgorithmBlocks(building.getVertices(), k,
					dataset.random, probability);
		} else if (algorithm == 4) {
			// ss
			algo = new SSAlgorithmBlocks(building.getVertices(), k,
					dataset.random, probability);
		} else if (algorithm == 5) {
			algo = new SimulatedAnnealingBlocks(building.getVertices(), k,
					dataset.random, probability, 100);
		}else if (algorithm == 6){
			algo = new ME4a(building.getVertices(), k, dataset.random, probability, 3, 3,true,building);
			//System.out.println("ME3");
		}else if (algorithm == 7){
			algo = new ME3(building.getVertices(), k, dataset.random, probability, 3, 3,true);
		}

		SimulationBlocks sim = new SimulationBlocks(k, building, dataset, algo,
				lookahead.getLookAhead());
		if (algorithm == 3) { // csalgorithm
			sim.runCSSimulation();
		} else if (algorithm == 4) { // ssalgorithm
			sim.runSSSimulation();
		} else {
			sim.runSimulation();
		}         
		sim.writeToFile();
		sim.writeNodesToFile(); // create downloaded nodes file
		sim.outResults();
		
	} 

}
