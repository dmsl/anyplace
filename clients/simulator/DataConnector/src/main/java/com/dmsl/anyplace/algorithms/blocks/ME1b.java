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
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Stack;


public class ME1b extends AlgorithmBlocks {
	static double leftlat = 35.14404722486025;
	static double leftlon = 33.41077988429299;
	static double rightlat = 35.14492259219234;
	static double rightlon = 33.41176947996616;
	static double MAX_DIST = distance(leftlat, leftlon, rightlat, rightlon);
	int startNode;

	Random rnd;
	float a1 = 0.5f;
	float a2 = 0.5f;

	// This has to be set
	private int destNode;
	private double lat2;
	private double lon2;

	public ME1b(CleanPoi[] vertices, int nBlocks, Random rnd, float probLost) {
		super(4, vertices, nBlocks, probLost);
		this.rnd = rnd;
	}

	public ArrayList<Integer> run(int u) {
		// Signal is lost

		lat2 = Double.parseDouble(vertices[destNode].getLat());
		lon2 = Double.parseDouble(vertices[destNode].getLon());

		int count = 0;
		this.startNode = u;

		PriorityQueue<ME1bNode> blocksNode = new PriorityQueue<ME1bNode>();
		ArrayList<ME1bNode> localVisited = new ArrayList<>();
		ArrayList<ME1bNode> toReturn = new ArrayList<>();
		double lat1 = Double.parseDouble(vertices[u].getLat());
		double lon1 = Double.parseDouble(vertices[u].getLon());
		double dist = distance(lat1, lon1, lat2, lon2);
		ME1bNode n = new ME1bNode(null, u, (dist / MAX_DIST) + (1 - 1));
		blocksNode.add(n);
		localVisited.add(n);

		while (!blocksNode.isEmpty()) {
			ME1bNode current = blocksNode.remove();
			// System.out.println("Current Node: \n\t" + current);
			if (u != current.u && !visited.contains(current.u)) {
				visited.add(current.u);
				toReturn.add(current);
				if (++count == nBlocks) {
					break;
				}
			}

			// System.out.println("Possible Nodes:");
			for (ME1bNode ot : getPossibleNodes(current)) {
				// System.out.println("\t" + ot);
				if (!localVisited.contains(ot)) {
					blocksNode.add(ot);
					localVisited.add(ot);
				}
			}
		}
		// Reverse the path
		Stack<ME1bNode> stack = new Stack<>();
		while (n != null) {
			stack.push(n);
			n = n.parent;
		}

		ArrayList<Integer> res = new ArrayList<>();
		for (ME1bNode node : toReturn) {
			res.add(node.u);
		}

		return res;
	}

	public PriorityQueue<ME1bNode> getPossibleNodes(ME1bNode n) {
		PriorityQueue<ME1bNode> res = new PriorityQueue<>();
		for (Pair p : vertices[n.u].getNeighboursToList()) {
			double lat1 = Double.parseDouble(vertices[p.getTo()].getLat());
			double lon1 = Double.parseDouble(vertices[p.getTo()].getLon());
			double dist = distance(lat1, lon1, lat2, lon2);
			res.add(new ME1bNode(n, p.getTo(), n.h + ((dist / MAX_DIST) + (1 - p.getWeight()))));

		}
		return res;
	}

	private static double distance(double lat1, double lon1, double lat2, double lon2) {

		final int R = 6371; // Radius of the earth

		Double latDistance = Math.toRadians(lat2 - lat1);
		Double lonDistance = Math.toRadians(lon2 - lon1);
		Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) + Math.cos(Math.toRadians(lat1))
				* Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
		Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double distance = R * c * 1000; // convert to meters

		distance = Math.pow(distance, 2);

		return Math.sqrt(distance);
	}

	/**
	 * @return the destNode
	 */
	public int getDestNode() {
		return destNode;
	}

	/**
	 * @param destNode
	 *            the destNode to set
	 */
	public void setDestNode(int destNode) {
		this.destNode = destNode;
	}

	private class ME1bNode implements Comparable<ME1bNode> {
		ME1bNode parent;
		int u;
		double h;

		// int n; // N is used for normalization

		public ME1bNode(ME1bNode parent, int u, double h) {
			this.parent = parent;
			this.u = u;
			this.h = h;

		}

		@Override
		public String toString() {

			return u + "(\t" + h + ")";
		}

		public boolean equals(Object obj) {
			ME1bNode o = (ME1bNode) (obj);
			return this.u == o.u;
		}

		@Override
		public int compareTo(ME1bNode o) {

			// If I have less => better
			if (h < o.h)
				return -1;
			if (h > o.h)
				return 1;
			return 0;
		}
	}

}