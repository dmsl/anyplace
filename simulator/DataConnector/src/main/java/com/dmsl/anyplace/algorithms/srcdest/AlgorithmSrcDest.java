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

import com.dmsl.anyplace.buildings.clean.CleanPoi;

import java.util.ArrayList;


public abstract class AlgorithmSrcDest {

	protected CleanPoi vertices[];
	protected ArrayList<Integer> visited;
	protected int ID;

	public AlgorithmSrcDest(int id, CleanPoi vertices[]) {
		this.vertices = vertices;
		ID = id;
		visited = new ArrayList<>();
	}



	public void clean() {
		visited = new ArrayList<>();
	}

	public abstract ArrayList<Integer> run(int u, int v);

	/**
	 * 
	 * @author zgeorg03
	 *
	 */
	public class Stats {
		int hit;
		int miss;

		public Stats(int hit, int miss) {
			this.hit = hit;
			this.miss = miss;
		}

		@Override
		public String toString() {
		
			return hit + "\t" + miss;
		}
	}
}
