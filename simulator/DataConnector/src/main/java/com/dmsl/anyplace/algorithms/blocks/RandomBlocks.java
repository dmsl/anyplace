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

import java.util.ArrayList;
import java.util.Random;


public class RandomBlocks extends AlgorithmBlocks {

	int startNode;
	Random rnd;

	public RandomBlocks(CleanPoi[] vertices, int nBlocks, Random rnd, float probLost) {
		super(0, vertices, nBlocks, probLost);
		this.rnd = rnd;

	}

	public ArrayList<Integer> run(int u) {

		int count = 0;
		this.startNode = u;

		ArrayList<Integer> localVisited = new ArrayList<>();

		// random
		Random r = new Random();
		int low = 0;
		int high = vertices.length;
		
		while(count!=nBlocks){
			int result = r.nextInt(high-low) + low;
			if(visited.contains(result)&&result!=u)
				continue;
			else{
				visited.add(result);
				localVisited.add(result);
				count++;
			}
		}
		
		return localVisited;
	}
	

}
