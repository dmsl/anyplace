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
import com.dmsl.anyplace.buildings.clean.CleanPoi;
import com.dmsl.anyplace.buildings.clean.Pair;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Stack;

public class UniformSearchSrcDest extends AlgorithmSrcDest {

	class Node implements Comparable<Node> {
		Node parent;
		int u;
		double cost;

		public Node(Node parent, int u, double cost) {
			this.parent = parent;
			this.u = u;
			this.cost = cost;
		}

		@Override
		public String toString() {
			return u + "";
		}

		public boolean equals(Object obj) {
			Node o = (Node) (obj);
			return this.u == o.u;
		}

		@Override
		public int compareTo(Node o) {
			if (this.cost < o.cost)
				return 1;
			if (this.cost > o.cost)
				return -1;
			return 0;
		}
	}

	public UniformSearchSrcDest(int id, CleanPoi[] vertices) {
		super(id, vertices);

	}

	public PriorityQueue<Node> getPossibleNodes(Node n) {
		PriorityQueue<Node> res = new PriorityQueue<>();
		for (Pair p : vertices[n.u].getNeighboursToList())
			res.add(new Node(n, p.getTo(), p.getWeight() * n.cost));

		return res;
	}

	@Override
	public ArrayList<Integer> run(int u, int v) {
		PriorityQueue<Node> statesToSearch = new PriorityQueue<Node>();
		ArrayList<Node> visited = new ArrayList<>();
		Node n = new Node(null, u, 1);
		statesToSearch.add(n);
		visited.add(n);

		while (!statesToSearch.isEmpty()) {
			Node current = statesToSearch.remove();
			if (current.u == v) {
				n = current;
				break;
			}
			for (Node ot : getPossibleNodes(current)) {
				if (!visited.contains(ot)) {
					statesToSearch.add(ot);
					visited.add(ot);
				}
			}
		}
		// Reverse the path
		Stack<Node> stack = new Stack<>();
		while (n != null) {
			stack.push(n);
			n = n.parent;
		}
		ArrayList<Integer> res = new ArrayList<>();
		while (!stack.isEmpty()) {
			res.add(stack.pop().u);
		}

		return res;
	}

	public static void main(String args[]) throws Exception {
		CleanBuilding building = new CleanBuilding(DataConnector.BUILDING_ID, false);
		DatasetCreator dataset = new DatasetCreator(DataConnector.BUILDING_ID, 1456737260395L,true);
		AlgorithmSrcDest algo = new UniformSearchSrcDest(2, building.getVertices());
		System.out.println(algo.run(241, 251));
	}

}
