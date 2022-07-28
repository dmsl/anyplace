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
package com.dmsl.anyplace.algorithms.srcdest;


import com.dmsl.anyplace.DataConnector;
import com.dmsl.anyplace.algorithms.DatasetCreator;
import com.dmsl.anyplace.buildings.clean.CleanBuilding;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class SimulationSrcDest {
	public static final String FILE_FORMAT = "sim";
	private DatasetCreator dataset;
	private AlgorithmSrcDest algorithm;
	private CleanBuilding building;

	private ArrayList<AlgorithmSrcDest.Stats> results;

	public static void main(String[] args) throws Exception {
		CleanBuilding building = new CleanBuilding(DataConnector.BUILDING_ID, false);
		DatasetCreator dataset = new DatasetCreator(DataConnector.BUILDING_ID, 1456737260395L,true);

		// BFS
		AlgorithmSrcDest algo = new BFSSrcDest(1, building.getVertices());
		SimulationSrcDest sim = new SimulationSrcDest(building, dataset, algo);
		sim.runSimulation();
		sim.writeToFile();

		// US
		algo = new UniformSearchSrcDest(2, building.getVertices());
		sim = new SimulationSrcDest(building, dataset, algo);
		sim.runSimulation();
		sim.writeToFile();
	}

	public SimulationSrcDest(CleanBuilding building, DatasetCreator dataset, AlgorithmSrcDest algorithm) {
		this.setDataset(dataset);
		this.setAlgorithm(algorithm);
		this.building = building;

	}

	/**
	 * Run all the dataset
	 * 
	 * @return
	 */
	public ArrayList<AlgorithmSrcDest.Stats> runSimulation() {

		ArrayList<AlgorithmSrcDest.Stats> list = new ArrayList<>();
		for (int i = 0; i < dataset.size; i++)
			list.add(run(i));
		this.results = list;
		return list;
	}

	/**
	 * 
	 * @param i
	 */
	public AlgorithmSrcDest.Stats run(int i) {
		int hit = 0;
		int miss = 0;
		List<Integer> path = dataset.getDataset().get(i);
		TreeSet<Integer> set = new TreeSet<>();

		ArrayList<Integer> p = algorithm.run(path.get(0), path.get(path.size() - 1));
		set.addAll(p);
		for (int step = 0; step < path.size() - 1; step++) {
			// Hit if the step exists
			if (set.contains(path.get(step))) {
				// System.out.print("\tHIT!\t");
				hit++;
			}
			// System.out.println(set);
		}
		miss = set.size() - hit;
		algorithm.clean();
		return algorithm.new Stats(hit, miss);
	}

	private void writeToFile() throws IOException {
		File path = new File(DataConnector.SIMULATION_PATH);
		if (!path.exists()) {
			path.mkdir();
			System.out.println("[Info] Directory: " + DataConnector.SIMULATION_PATH + " created");
		}
		String filename = dataset + "_" + algorithm.ID + "_Source_Destination." + FILE_FORMAT;
		File file = new File(DataConnector.SIMULATION_PATH + filename);
		if (file.exists()) {
			System.out.println("[Info]: File: " + filename + " would be overwritten");
		}
		BufferedWriter bf = new BufferedWriter(new FileWriter(file));
		bf.write("# Building:	" + building.getBuid() + "\n");
		bf.write("# Dataset: " + DataConnector.DATASETS_PATH + dataset + "\n");
		bf.write("# Min_depth Max_depth: " + this.dataset.MIN_DEPTH + " " + dataset.MAX_DEPTH + "\n");
		bf.write("# Path Number\tTotal Hits\tTotal Misses\n");
		int i = 0;
		for (AlgorithmSrcDest.Stats stats : results) {
			bf.write(i + "\t");
			bf.write(stats.toString() + "\n");
			i++;
		}
		bf.close();
		System.out.println("[Info]: File: " + filename + " successfuly saved");
	}

	/**
	 * @return the dataset
	 */
	public DatasetCreator getDataset() {
		return dataset;
	}

	/**
	 * @param dataset
	 *            the dataset to set
	 */
	public void setDataset(DatasetCreator dataset) {
		this.dataset = dataset;
	}

	/**
	 * @return the algorithm
	 */
	public AlgorithmSrcDest getAlgorithm() {
		return algorithm;
	}

	/**
	 * @param algorithm
	 *            the algorithm to set
	 */
	public void setAlgorithm(AlgorithmSrcDest algorithm) {
		this.algorithm = algorithm;
	}

	/**
	 * @return the building
	 */
	public CleanBuilding getBuilding() {
		return building;
	}

	/**
	 * @param building
	 *            the building to set
	 */
	public void setBuilding(CleanBuilding building) {
		this.building = building;
	}

	/**
	 * @return the results
	 */
	public ArrayList<AlgorithmSrcDest.Stats> getResults() {
		if (results == null)
			runSimulation();
		return results;
	}

}
