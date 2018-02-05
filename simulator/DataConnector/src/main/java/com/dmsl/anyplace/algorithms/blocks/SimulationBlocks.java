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
package com.dmsl.anyplace.algorithms.blocks;

import com.dmsl.anyplace.DataConnector;
import com.dmsl.anyplace.airplace.Location;
import com.dmsl.anyplace.airplace.LogRecord;
import com.dmsl.anyplace.airplace.RadioMap;
import com.dmsl.anyplace.algorithms.DatasetCreator;
import com.dmsl.anyplace.algorithms.GetResultsFiles;
import com.dmsl.anyplace.buildings.clean.CleanBuilding;
import com.dmsl.anyplace.buildings.clean.CleanPoi;
import com.google.gson.Gson;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class SimulationBlocks {
	public static final String FILE_FORMAT = "sim";
	private int nBlocks;
	private DatasetCreator dataset;
	private AlgorithmBlocks algorithm;
	private CleanBuilding building;
	private String savedFile;

	private ArrayList<AlgorithmBlocks.Stats> results;
	private ArrayList<String> lookAhead;
	private double minDistance = 0.2; // in meters
	private int locAlgo = 1; // 1 KNN, 2 WKNN, 3 MMSE, 4 WMMSE

	public SimulationBlocks(int nBlocks, CleanBuilding building,
			DatasetCreator dataset, AlgorithmBlocks algorithm,
			ArrayList<String> lookAhead) {
		this.setDataset(dataset);
		this.setAlgorithm(algorithm);
		this.building = building;
		this.nBlocks = nBlocks;
		this.lookAhead = lookAhead;
	}

	/**
	 * Run all the dataset
	 * 
	 * @return
	 */
	public ArrayList<AlgorithmBlocks.Stats> runSimulation() {

		ArrayList<AlgorithmBlocks.Stats> list = new ArrayList<>();
		for (int i = 0; i < dataset.size; i++) {
			list.add(run(i));
		}
		this.results = list;
		return list;
	}

	/**
	 * 
	 * @param i
	 */
	public AlgorithmBlocks.Stats run(int i) {
		int hit = 0;
		int miss = 0;
		List<Integer> path = dataset.getDataset().get(i);
		TreeSet<Integer> set = new TreeSet<>();

		Location cloc = new Location("", "");
		Location rloc = new Location("", "");

		if (algorithm.ID == 2) {
			((ME1) algorithm).setDestNode(path.get(path.size() - 1));
		}

		ArrayList<AlgorithmBlocks.StatsItem> items = new ArrayList<AlgorithmBlocks.StatsItem>();
		for (int step = 0; step < path.size(); step++) {
			long startTime = System.nanoTime();
			int hop = path.get(step);
			boolean hasConnection = false;
			AlgorithmBlocks.StatsItem item = algorithm.new StatsItem();

			ArrayList<Integer> p = new ArrayList<Integer>();

			// always have connection on first step
			if (step == 0) {
				p = algorithm.run(hop);
				hasConnection = true;
			} else {
				double dis = minAPDistance(building.getVertices()[hop]);
				if (dis <= minDistance) {
					hasConnection = true;
					p = algorithm.run(hop);
				}
			}
			ArrayList<Integer> as = new ArrayList<Integer>();
			// do not download fps at the final step
			if (step != path.size() - 1) {
				
				for (Integer integer : p) {
					if(!set.contains(integer))
						as.add(integer);
				}
				set.addAll(p);
			}

			// check for connectivity
			if (!hasConnection) {

				// connectivity is lost
				cloc = new Location("", "");
				rloc = new Location("", "");
				ArrayList<LogRecord> wifi = null;
				try {
					wifi = findListeningFP(hop);
				} catch (Exception ex) {
					System.err.println("Cannot get listening fps!");
					return null;
				}

				// localize with downloaded fps
				RadioMap setRM = getDownloadedPoisRadioMap(set);
				cloc = RadioMap.localize(wifi, setRM, locAlgo); // use 1 for knn
				if (cloc == null)
					System.err.println("null");
				long stopTime = System.nanoTime();

				// localize with full rm
				RadioMap realRM;
				try {
					File f = new File(DataConnector.FINGERPRINTS_PATH
							+ building.getBuid() + "/" + "all.pajek");
					realRM = new RadioMap(f);
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("Cannot parse fps file!");
					return null;
				}
				rloc = RadioMap.localize(wifi, realRM, locAlgo); // use 1 for
																	// knn

				double error = euclideanDistance(cloc, rloc);

				item.error = error;
				item.fpsSize = 0;
				long elapsedTime = stopTime - startTime;
				// nano seconds to seconds
				item.time = (double) elapsedTime / 1000000000.0;

			} else {
				System.err.println(hop);
				long stopTime = System.nanoTime();
				long elapsedTime = stopTime - startTime;
				// nano seconds to seconds
				item.time = (double) elapsedTime / 1000000000.0;

				
				// connection exist
				TreeSet<Integer> ts = new TreeSet<Integer>();
				ts.addAll(as);
				RadioMap setRM = getDownloadedPoisRadioMap(ts);
				item.error = 0;
				item.fpsSize = setRM.getNumOfFPS();
				item.time = 0;
			}

			items.add(item);
		}
		miss = set.size() - hit;
		algorithm.clean();
		return algorithm.new Stats(hit, miss, new ArrayList<Integer>(set),
				items);
	}

	private double minAPDistance(CleanPoi cleanPoi) {
		double min = Double.MAX_VALUE;
		for (String str : lookAhead) {
			String temp[] = str.split(",");
			double eu = euclideanDistance(new Location(cleanPoi.getLat(),
					cleanPoi.getLon()), new Location(temp[0], temp[1]));
			if (eu < min)
				min = eu;
		}
		return min;
	}

	public static double calculateDistance(double[] array1, double[] array2) {
		double Sum = 0.0;
		for (int i = 0; i < array1.length; i++) {
			Sum = Sum + Math.pow((array1[i] - array2[i]), 2.0);
		}
		return Math.sqrt(Sum);
	}

	private double euclideanDistance(Location loc1, Location loc2) {
		double lat1 = Double.parseDouble(loc1.getLat());
		double lon1 = Double.parseDouble(loc1.getLon());
		double lat2 = Double.parseDouble(loc2.getLat());
		double lon2 = Double.parseDouble(loc2.getLon());
		final int R = 6371; // Radius of the earth

		Double latDistance = Math.toRadians(lat2 - lat1);
		Double lonDistance = Math.toRadians(lon2 - lon1);
		Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
				+ Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2)
				* Math.sin(lonDistance / 2);
		Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double distance = R * c * 1000; // convert to meters

		distance = Math.pow(distance, 2);

		return Math.sqrt(distance);

	}

	private RadioMap getDownloadedPoisRadioMap(TreeSet<Integer> set) {
		RadioMap rm = new RadioMap();
		for (int x : set) {
			rm.ConstructRadioMap(building.getVertices()[x].getFp());
		}
		return rm;
	}

	private ArrayList<LogRecord> findListeningFP(int step) throws IOException {
		CleanPoi poi = building.getVertices()[step];
		return new RadioMap().getListeningAccessPoints(poi.getFloor(),
				building.getBuid(), poi.getLat(), poi.getLon());
	}

	public void writeToFile() throws IOException {
		File path = new File(DataConnector.SIMULATION_PATH + building.getBuid() + "/");
		if (!path.exists()) {
			path.mkdir();
			System.out.println("[Info] Directory: " + DataConnector.SIMULATION_PATH
					+ building.getBuid() + "/" + " created");
		}
		System.out.println(dataset);
		String filename = dataset + "_" + algorithm.ID + "_" + nBlocks + "_"
				+ algorithm.probLost + "." + FILE_FORMAT;

		// TODO this is only for localization algorithms
//		 filename = dataset + "_" + algorithm.ID + "_" + nBlocks+"1" + "_"
//		 + algorithm.probLost + "." + FILE_FORMAT;

		// TODO this is only for weights
		// filename = dataset + "_" + algorithm.ID + "_" + nBlocks + "_"+ME2.a1
		// + "_" +ME2.a2+ "_"+ME2.a3+"_"
		// + algorithm.probLost + "." + FILE_FORMAT;

		// if (algorithm.ID == 2) {
		// float a1 = ((ME2) algorithm).a1;
		// float a2 = ((ME2) algorithm).a2;
		// float a3 = ME2.a3;
		// filename = dataset + "_" + algorithm.ID + "_" + nBlocks + "_"
		// + algorithm.probLost + "_" + a1 + "_" + a2 + "."
		// + FILE_FORMAT;
		//
		// // TODO this is only for localization algorithms
		// filename = dataset + "_" + algorithm.ID + "_" + nBlocks + "_10"+
		// "_"
		// + algorithm.probLost + "_" + a1 + "_" + a2 + "."
		// + FILE_FORMAT;
		//
		// //TODO this is only for weights
		// filename = dataset + "_" + algorithm.ID + "_" + nBlocks +"_" + a1 +
		// "_" + a2 + "_" + a3
		// + "_" + algorithm.probLost + "_" + a1 + "_" + a2 + "_" + a3+ "."
		// + FILE_FORMAT;
		// }

		File file = new File(DataConnector.SIMULATION_PATH + building.getBuid() + "/"
				+ filename);
		if (file.exists()) {
			System.out.println("[Info]: File: " + filename
					+ " would be overwritten");
		}

		savedFile = file.getPath();

		BufferedWriter bf = new BufferedWriter(new FileWriter(file));
		bf.write("# Alorithm:	" + algorithm.ID + "\n");
		bf.write("# Building:	" + building.getBuid() + "\n");
		bf.write("# Dataset: " + DataConnector.DATASETS_PATH + building.getBuid() + "/"
				+ dataset + "\n");
		bf.write("# Max_block_number: " + this.nBlocks + "\n");
		bf.write("# Probability_Signal_Lost: " + this.algorithm.probLost + "\n");

		bf.write("# Min_depth Max_depth: " + this.dataset.MIN_DEPTH + " "
				+ dataset.MAX_DEPTH + "\n");
		// bf.write("# Path Number\tTotal Hits\tTotal Misses\tDownloaded nodes\n");
		bf.write("# Path Number\tTotal Hits\tTotal Misses\tStats\n");
		int i = 0;
		for (AlgorithmBlocks.Stats stats : results) {
			bf.write(i + "\t");
			bf.write(stats.toString() + "\t");

			bf.write("*");
			bf.write(new Gson().toJson(stats.items));
			bf.write("*\n");
			i++;
		}
		bf.close();
		System.out.println("[Info]: File: " + filename + " successfuly saved");
	}

	public void writeNodesToFile() throws IOException {
		File path = new File(DataConnector.SIMULATION_PATH + building.getBuid() + "/");
		if (!path.exists()) {
			path.mkdir();
			System.out.println("[Info] Directory: " + DataConnector.SIMULATION_PATH
					+ building.getBuid() + "/" + " created");
		}
		System.out.println(dataset);
		String filename = dataset + "_" + algorithm.ID + "_" + nBlocks + "_"
				+ algorithm.probLost + ".nodes";

		// TODO this is only for localization algorithms
//		 filename = dataset + "_" + algorithm.ID + "_" + nBlocks + "_1"+ "_"
//		 + algorithm.probLost + ".nodes";

		// TODO this is only for weights
		// filename = dataset + "_" + algorithm.ID + "_" + nBlocks + "_"+ME2.a1
		// + "_"+ME2.a2+ "_"+ME2.a3+ "_"
		// + algorithm.probLost + ".nodes";

		// if (algorithm.ID == 2) {
		// float a1 = ((ME2) algorithm).a1;
		// float a2 = ((ME2) algorithm).a2;
		// float a3 = ((ME2) algorithm).a3;
		// filename = dataset + "_" + algorithm.ID + "_" + nBlocks + "_"
		// + algorithm.probLost + "_" + a1 + "_" + a2 + ".nodes";
		//
		// // TODO this is only for localization algorithms
		// filename = dataset + "_" + algorithm.ID + "_" + nBlocks+"_10" +
		// "_"
		// + algorithm.probLost + "_" + a1 + "_" + a2 + ".nodes";
		//
		// // TODO this is only for weights
		// filename = dataset + "_" + algorithm.ID + "_" + nBlocks + "_"+ME2.a1+
		// "_"+ME2.a2+ "_"+ME2.a3
		// + "_" + algorithm.probLost + "_" + a1 + "_" + a2 + "_" + a3+
		// ".nodes";
		// }

		File file = new File(DataConnector.SIMULATION_PATH + building.getBuid() + "/"
				+ filename);
		if (file.exists()) {
			System.out.println("[Info]: File: " + filename
					+ " would be overwritten");
		}

		BufferedWriter bf = new BufferedWriter(new FileWriter(file));
		bf.write("# Alorithm:	" + algorithm.ID + "\n");
		bf.write("# Building:	" + building.getBuid() + "\n");
		bf.write("# Dataset: " + DataConnector.DATASETS_PATH + building.getBuid() + "/"
				+ dataset + "\n");
		bf.write("# Max_block_number: " + this.nBlocks + "\n");
		bf.write("# Probability_Signal_Lost: " + this.algorithm.probLost + "\n");

		bf.write("# Min_depth Max_depth: " + this.dataset.MIN_DEPTH + " "
				+ dataset.MAX_DEPTH + "\n");
		// bf.write("# Path Number\tTotal Hits\tTotal Misses\tDownloaded nodes\n");
		bf.write("# Path Number\tTotal Hits\tTotal Misses\tStats\n");
		int i = 0;
		for (AlgorithmBlocks.Stats stats : results) {
			bf.write(i + "\t");
			bf.write(stats.toString() + "\t");

			bf.write("[");
			for (int j = 0; j < stats.getNodes().size(); j++) {
				if (j != 0)
					bf.write(",");
				bf.write(stats.getNodes().get(j).toString());
			}
			bf.write("]\n");
			i++;
		}
		bf.close();
		System.out.println("[Info]: File: " + filename + " successfuly saved");
	}

	public void outResults() throws Exception {
		new GetResultsFiles().run(savedFile);
	}

	public ArrayList<List<Integer>> parseFile() throws Exception {
		ArrayList<List<Integer>> res = new ArrayList<>();
		File path = new File(DataConnector.SIMULATION_PATH + building.getBuid() + "/");
		if (!path.exists()) {
			path.mkdir();
			System.out.println("[Info] Directory: " + DataConnector.SIMULATION_PATH
					+ building.getBuid() + "/" + " created");
		}
		String filename = dataset + "_" + algorithm.ID + "_" + nBlocks + "_"
				+ algorithm.probLost + ".nodes";

		// TODO this is only for localization algorithms
//		 filename = dataset + "_" + algorithm.ID + "_" + nBlocks + "_1"+ "_"
//		 + algorithm.probLost + ".nodes";

		// TODO this is only for weights
		// filename = dataset + "_" + algorithm.ID + "_" + nBlocks + "_"+ME2.a1
		// + "_"+ME2.a2+ "_"+ME2.a3+ "_"
		// + algorithm.probLost + ".nodes";

		// if (algorithm.ID == 2) {
		// float a1 = ME2.a1;
		// float a2 = ME2.a2;
		// float a3 = ME2.a3;
		// filename = dataset + "_" + algorithm.ID + "_" + nBlocks + "_"
		// + algorithm.probLost + "_" + a1 + "_" + a2 + ".nodes";
		//
		// // TODO this is only for localization algorithms
		// filename = dataset + "_" + algorithm.ID + "_" + nBlocks+ "_10" +
		// "_"
		// + algorithm.probLost + "_" + a1 + "_" + a2
		// + ".nodes";
		//
		// // TODO this is only for weights
		// filename = dataset + "_" + algorithm.ID + "_" + nBlocks +"_"+ ME2.a1+
		// "_"+ME2.a2+ "_"+ME2.a3
		// + "_" + algorithm.probLost + "_" + a1 + "_" + a2 + "_" + a3 +
		// ".nodes";
		// }

		File file = new File(DataConnector.SIMULATION_PATH + building.getBuid() + "/"
				+ filename);

		if (!file.exists()) {
			System.out.println(filename);
			throw new Exception("File doesn't exist");
		}
		BufferedReader bf = new BufferedReader(new FileReader(file));
		String line = "";
		int N;
		while ((line = bf.readLine()) != null) {

			if (line.startsWith("#"))
				continue;
			String toks[] = line.split("[ \t]");
			int pathNum = Integer.parseInt(toks[0]);
			int hits = Integer.parseInt(toks[1]);
			int misses = Integer.parseInt(toks[2]);
			String s[] = toks[3].split(",");
			List<Integer> list = new ArrayList<>();
			list.add(Integer.parseInt(s[0].replace("[", "")));
			for (int i = 1; i < s.length - 1; i++) {
				list.add(Integer.parseInt(s[i]));
			}
			list.add(Integer.parseInt(s[s.length - 1].replace("]", "")));
			res.add(list);
		}
		bf.close();
		return res;

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
	public AlgorithmBlocks getAlgorithm() {
		return algorithm;
	}

	/**
	 * @param algorithm
	 *            the algorithm to set
	 */
	public void setAlgorithm(AlgorithmBlocks algorithm) {
		this.algorithm = algorithm;
	}

	/**
	 * @return the nBlocks
	 */
	public int getnBlocks() {
		return nBlocks;
	}

	/**
	 * @param nBlocks
	 *            the nBlocks to set
	 */
	public void setnBlocks(int nBlocks) {
		this.nBlocks = nBlocks;
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
	public ArrayList<AlgorithmBlocks.Stats> getResults() {
		if (results == null)
			runSimulation();
		return results;
	}

	public ArrayList<AlgorithmBlocks.Stats> runCSSimulation() {

		ArrayList<AlgorithmBlocks.Stats> list = new ArrayList<>();
		for (int i = 0; i < dataset.size; i++) {
			list.add(runCS(i));
		}
		this.results = list;
		return list;
	}

	private AlgorithmBlocks.Stats runCS(int i) {
		int hit = 0;
		int miss = 0;
		List<Integer> path = dataset.getDataset().get(i);
		TreeSet<Integer> set = new TreeSet<>();

		Location rloc = new Location("", "");

		ArrayList<AlgorithmBlocks.StatsItem> items = new ArrayList<AlgorithmBlocks.StatsItem>();
		for (int step = 0; step < path.size(); step++) {
			long startTime = System.nanoTime();
			int hop = path.get(step);
			AlgorithmBlocks.StatsItem item = algorithm.new StatsItem();

			rloc = new Location("", "");
			ArrayList<LogRecord> wifi = null;
			try {
				wifi = findListeningFP(hop);
			} catch (Exception ex) {
				System.err.println("Cannot get listening fps!");
				return null;
			}

			// localize with full rm
			RadioMap realRM;
			try {
				File f = new File(DataConnector.FINGERPRINTS_PATH + building.getBuid()
						+ "/" + "all.pajek");
				realRM = new RadioMap(f);
			} catch (Exception e) {
				e.printStackTrace();
				System.err.println("Cannot parse fps file!");
				return null;
			}
			rloc = RadioMap.localize(wifi, realRM, locAlgo); // use 2 for wknn

			long stopTime = System.nanoTime();

			item.error = 0;
			// hange this
			if (step == 0)
				item.fpsSize = realRM.getNumOfFPS(); // total fps of radio map
			else
				item.fpsSize = 0;
			long elapsedTime = stopTime - startTime;
			// nano seconds to seconds
			item.time = (double) elapsedTime / 1000000000.0;

			items.add(item);
		}
		miss = set.size() - hit;
		algorithm.clean();
		return algorithm.new Stats(hit, miss, new ArrayList<Integer>(set),
				items);
	}

	public ArrayList<AlgorithmBlocks.Stats> runSSSimulation() {

		ArrayList<AlgorithmBlocks.Stats> list = new ArrayList<>();
		for (int i = 0; i < dataset.size; i++) {
			list.add(runSS(i));
		}
		this.results = list;
		return list;
	}

	private AlgorithmBlocks.Stats runSS(int i) {
		int hit = 0;
		int miss = 0;
		List<Integer> path = dataset.getDataset().get(i);
		TreeSet<Integer> set = new TreeSet<>();

		Location cloc = new Location("", "");
		Location rloc = new Location("", "");

		ArrayList<AlgorithmBlocks.StatsItem> items = new ArrayList<AlgorithmBlocks.StatsItem>();
		for (int step = 0; step < path.size(); step++) {
			boolean hasConnection = false;
			long startTime = System.nanoTime();
			int hop = path.get(step);
			AlgorithmBlocks.StatsItem item = algorithm.new StatsItem();

			// always have connection on first step
			if (step == 0) {
				hasConnection = true;
			} else {
				double dis = minAPDistance(building.getVertices()[hop]);
				if (dis <= minDistance) {
					hasConnection = true;
				}
			}

			// check for connectivity
			if (!hasConnection) {
				rloc = new Location("", "");

				long stopTime = System.nanoTime();

				// localize with full rm
				RadioMap realRM;
				try {
					// realRM = new RadioMap(new File(Main.FINGERPRINTS_PATH +
					// Main.UCY_BUILDING_ID+"_"+building.getVertices()[hop].getFloor()+".fing_new"));
					File f = new File(DataConnector.FINGERPRINTS_PATH
							+ building.getBuid() + "/" + "all.pajek");
					realRM = new RadioMap(f);
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("Cannot parse fps file!");
					return null;
				}

				ArrayList<LogRecord> wifi = null;
				try {
					wifi = findListeningFP(hop);
				} catch (Exception ex) {
					System.err.println("Cannot get listening fps!");
					return null;
				}

				rloc = RadioMap.localize(wifi, realRM, locAlgo); // use 2 for
																	// wknn

				long elapsedTime = stopTime - startTime;

				double error = euclideanDistance(cloc, rloc);

				item.error = error;
				item.fpsSize = 0;
				// nano seconds to seconds
				item.time = (double) elapsedTime / 1000000000.0;
			} else {
				// connection exist
				// connectivity is lost
				cloc = new Location("", "");
				rloc = new Location("", "");
				ArrayList<LogRecord> wifi = null;
				try {
					wifi = findListeningFP(hop);
				} catch (Exception ex) {
					System.err.println("Cannot get listening fps!");
					return null;
				}

				RadioMap realRM;
				try {
					File f = new File(DataConnector.FINGERPRINTS_PATH
							+ building.getBuid() + "/" + "all.pajek");
					realRM = new RadioMap(f);
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println("Cannot parse fps file!");
					return null;
				}
				cloc = RadioMap.localize(wifi, realRM, locAlgo); // use 2 for
																	// wknn
				if (cloc == null)
					System.err.println("null");
				long stopTime = System.nanoTime();

				item.error = 0;
				item.fpsSize = 0;
				long elapsedTime = stopTime - startTime;
				// nano seconds to seconds
				item.time = 0;
				// (double) elapsedTime / 1000000000.0;
			}

			items.add(item);
		}
		miss = set.size() - hit;
		algorithm.clean();
		return algorithm.new Stats(hit, miss, new ArrayList<Integer>(set),
				items);
	}

}
