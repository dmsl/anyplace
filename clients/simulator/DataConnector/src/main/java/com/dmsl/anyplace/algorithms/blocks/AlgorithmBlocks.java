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


public abstract class AlgorithmBlocks {

	protected CleanPoi vertices[];
	protected int nBlocks;
	protected ArrayList<Integer> visited;
	protected int ID;
	protected float probLost;
	protected boolean connectivityLost;
	
	public AlgorithmBlocks(int id, CleanPoi vertices[], int nBlocks,float probLost) {
		this.probLost=probLost;
		this.vertices = vertices;
		this.nBlocks = nBlocks;
		ID = id;
		visited = new ArrayList<>();
		connectivityLost = false;
	}
	
	public void setProbLost(float p){
		this.probLost = p;
	}
	
	public float getProbabilityLost(){
		return probLost;
	}

	public void clean() {
		visited = new ArrayList<>();
	}

	public abstract ArrayList<Integer> run(int u);

	/**
	 * 
	 * @author zgeorg03
	 *
	 */
	public class Stats {
		int hit;
		int miss;
		ArrayList<Integer> nodes;
		ArrayList<StatsItem> items;

		public Stats(int hit, int miss) {
			this.hit = hit;
			this.miss = miss;
		}

		public Stats(int hit, int miss, ArrayList<Integer> nodes,ArrayList<StatsItem> items) {
			this(hit,miss);
			this.nodes = nodes;
			this.items = items;
		}

		@Override
		public String toString() {
			return hit + "\t" + miss;
		}
		
		public ArrayList<Integer> getNodes(){
			return nodes;
		}
	}
	
	public class StatsItem{
		double error;
		int fpsSize;
		double time;
		
		public double getError() {
			return error;
		}
		
		public int getFpsSize() {
			return fpsSize;
		}
		
		public double getTime() {
			return time;
		}
		
		public void setError(double error) {
			this.error = error;
		}
		
		public void setFpsSize(int fpsSize) {
			this.fpsSize = fpsSize;
		}
		
		public void setTime(double time) {
			this.time = time;
		}
	}
}
