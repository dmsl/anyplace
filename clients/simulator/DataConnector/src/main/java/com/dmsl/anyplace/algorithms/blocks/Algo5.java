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

import com.dmsl.anyplace.buildings.clean.CleanPoi;
import com.dmsl.anyplace.buildings.clean.Pair;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;


public class Algo5 extends AlgorithmBlocks {

	int startNode;
	Random rnd;

	public Algo5(CleanPoi[] vertices, int nBlocks, Random rnd, float probLost) {
		super(5, vertices, nBlocks, probLost);
		this.rnd = rnd;
	}

	public ArrayList<Integer> run(int u) {
		connectivityLost = false;
		// Signal is lost
		float prob = rnd.nextFloat();
		if (prob > probLost){
			connectivityLost = true;
			return new ArrayList<>();
		}
			
		int count = 0;
		this.startNode = u;

		Queue<Integer> blocks = new LinkedList<>();
		ArrayList<Integer> localVisited = new ArrayList<>();

		blocks.add(u);
		visited.add(u);
		while (!blocks.isEmpty()) {

			Integer s = blocks.remove();
			localVisited.add(s);

			if (!visited.contains(s)&& u!=s) {
				visited.add(s);

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
