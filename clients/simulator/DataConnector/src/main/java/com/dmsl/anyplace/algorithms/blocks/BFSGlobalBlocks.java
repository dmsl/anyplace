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
import com.dmsl.anyplace.buildings.clean.CleanBuilding;
import com.dmsl.anyplace.buildings.clean.CleanPoi;
import com.dmsl.anyplace.buildings.clean.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;


public class BFSGlobalBlocks extends AlgorithmBlocks {

	int startNode;
	Random rnd;

	// Used by global BFS
	Queue<Integer> blocks = new LinkedList<>();

	public static void main(String[] args) throws Exception {
		ArrayList<Integer> l = new ArrayList<Integer>();
		l.add(11);
		l.add(24);
		l.add(278);
		l.add(281);
		l.add(191);
		l.add(297);
		l.add(308);
		l.add(325);
		l.add(344);
		l.add(340);

		// load building
		CleanBuilding building = new CleanBuilding(DataConnector.BUILDING_ID, false);
		// building.parseFpFile();

		BFSBlocks b = new BFSBlocks(building.getVertices(), 3, new Random(), 0.25f);
		for (Integer integer : l) {
			b.run(integer);
		}

	}

	public BFSGlobalBlocks(CleanPoi[] vertices, int nBlocks, Random rnd, float probLost) {
		super(1, vertices, nBlocks, probLost);
		this.rnd = rnd;
	}

	public void clean() {
		visited = new ArrayList<>();
		blocks = new LinkedList<>();
	}
	public ArrayList<Integer> run(int u) {

		int count = 0;
		this.startNode = u;

		ArrayList<Integer> localVisited = new ArrayList<>();

		blocks.add(u);
		// visited.add(u);
		while (!blocks.isEmpty()) {

			Integer s = blocks.remove();
			if (s != u)
				localVisited.add(s);

			if (!visited.contains(s) && s != u) {
				visited.add(s);
				// localVisited.add(s);

				if (++count == nBlocks) {
					return localVisited;
				}
			}
			// Shuffle to get random neighbours
			List<Pair> neigh = vertices[s].getNeighboursToList();
			Collections.shuffle(neigh, rnd);

			for (Pair p : neigh) {
				Integer v = p.getTo();

				// If its not contained in the local blocks queue
				if (!localVisited.contains(v)) {
					blocks.add(v);
				}

			}

		}
		return localVisited;
	}

}