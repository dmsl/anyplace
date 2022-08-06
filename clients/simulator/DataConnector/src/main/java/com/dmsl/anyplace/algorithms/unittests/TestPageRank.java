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
package com.dmsl.anyplace.algorithms.unittests;

import com.dmsl.anyplace.DataConnector;
import com.dmsl.anyplace.buildings.clean.CleanBuilding;
import com.dmsl.anyplace.buildings.clean.CleanPoi;
import com.dmsl.anyplace.buildings.clean.Pair;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.PriorityQueue;

public class TestPageRank {

	CleanBuilding building;

	public TestPageRank(String filename) throws Exception {
		building = new CleanBuilding(filename, true);

		building.calculatePR(false);

	}

	public static void main(String[] args) throws Exception {
		String testName = "test_5";
		TestPageRank tp = new TestPageRank(testName);

	}

	public boolean writeToFile(String filename) throws IOException {

		File path = new File(DataConnector.GRAPH_PATH);
		if (!path.exists()) {
			if(path.mkdirs())
				System.out.println("[Info] Directory: " + DataConnector.GRAPH_PATH + " created");

		}
		File file = new File(DataConnector.GRAPH_PATH + filename + "." + DataConnector.FILE_FORMAT);
		if (file.isDirectory()) {
			System.err.println("[Error]: This is a directory");
			return false;
		}
		if (file.exists()) {
			System.out.println("[Info]: File: " + filename + " would be overwritten");
		}

		BufferedWriter bf = new BufferedWriter(new FileWriter(file));
		bf.write("*Vertices " + building.getVertices().length + " \"" + building.getName() + "\" " + building.getBuid()
				+ "\n");

		int count = 0;
		for (CleanPoi p : building.getVertices()) {
			bf.write(p.getId() + " \"" + p.getName() + "\" " + p.getLat() + " " + p.getLon() + " " + p.getFloor() + " "
					+ p.getPagerank() + " " + p.getPID() + "\n");
			count += p.getNeighbours().size();
		}
		bf.write("*Arcs " + count + "\n");
		for (int i = 0; i < building.getVertices().length; i++) {
			CleanPoi p = building.getVertices()[i];
			PriorityQueue<Pair> neigh = p.getNeighbours();
			for (Pair v : neigh) {
				bf.write(i + " " + v.getTo() + " " + v.getWeight() + "\n");
			}

		}
		bf.close();
		System.out.println("[Info]: File: " + filename + " successfuly saved");

		return true;
	}
}
