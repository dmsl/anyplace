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

import com.dmsl.anyplace.buildings.clean.CleanBuilding;
import com.dmsl.anyplace.buildings.clean.CleanPoi;
import com.dmsl.anyplace.buildings.clean.Pair;

import java.util.ArrayList;


public class TestSignificanceLevel {

	public static void main(String args[]) throws Exception {
		CleanBuilding building = new CleanBuilding("test_significanceLevel", true);

		System.out.println(getSignficanceLevel(1, building));

	}

	public static int getSignficanceLevel(Integer u, CleanBuilding building) {
		ArrayList<SigNode<CleanPoi>> vertices = new ArrayList<SigNode<CleanPoi>>();
		ArrayList<Integer> visited = new ArrayList<>();

		for (CleanPoi p : building.getVertices())
			vertices.add(new SigNode<CleanPoi>(p, 0));
		// visited.add(u);
		return significanceRecursevily(u, vertices, visited);

	}

	public static int significanceRecursevily(Integer u, ArrayList<SigNode<CleanPoi>> vertices,
			ArrayList<Integer> visited) {

		if (visited.contains(u))
			return vertices.get(u).significanceLevel;

		visited.add(u);
		// Get significance of neighbours
		for (Pair p : vertices.get(u).data.getNeighbours()) {
			int v = p.getTo();
			if (!visited.contains(v))
				vertices.get(u).significanceLevel += significanceRecursevily(v, vertices, visited) + 1;
		}

		return vertices.get(u).significanceLevel;
	}

}

class SigNode<E> {
	E data;
	int significanceLevel;

	@Override
	public String toString() {
		return data.toString() + ":" + significanceLevel;
	}

	public SigNode(E data, int significanceLevel) {
		this.data = data;
		this.significanceLevel = significanceLevel;
	}
}
