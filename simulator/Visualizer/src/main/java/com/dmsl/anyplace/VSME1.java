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
package com.dmsl.anyplace;

import com.dmsl.anyplace.algorithms.blocks.AlgorithmBlocks;
import com.dmsl.anyplace.buildings.clean.CleanPoi;
import com.dmsl.anyplace.buildings.clean.Pair;

import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.Stack;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.JOptionPane;


public class VSME1 extends AlgorithmBlocks {
	static double leftlat = 35.14404722486025;
	static double leftlon = 33.41077988429299;
	static double rightlat = 35.14492259219234;
	static double rightlon = 33.41176947996616;
	static double MAX_DIST = distance(leftlat, leftlon, rightlat, rightlon);

	Random rnd;
	float a1;
	float a2;

	// This has to be set
	private int destNode;
	private double lat2;
	private double lon2;

	public VSME1(CleanPoi[] vertices, int nBlocks, Random rnd, float a1, float a2, int startNode, int destNode) {
		super(2, vertices, nBlocks, 0f);
		this.rnd = rnd;
		this.a1 = a1;
		this.a2 = a2;
		this.startNode = startNode;
		lat2 = Double.parseDouble(vertices[destNode].getLat());
		lon2 = Double.parseDouble(vertices[destNode].getLon());
		double lat1 = Double.parseDouble(vertices[startNode].getLat());
		double lon1 = Double.parseDouble(vertices[startNode].getLon());
		double dist = distance(lat1, lon1, lat2, lon2);
		currentNode = new Node(null, startNode, 0, 1, dist);
		blocksNode.add(currentNode);
		localVisited.add(currentNode);
		possibleNodes = getPossibleNodes(currentNode, currentNode.n + 1);
	}

	int startNode;
	int count;

	PriorityQueue<Node> blocksNode = new PriorityQueue<Node>();
	ArrayList<Node> localVisited = new ArrayList<>();
	ArrayList<Node> toReturn = new ArrayList<>();
	PriorityQueue<Node> possibleNodes;
	Node currentNode;

	public String getCurrentState(int u) {
		StringBuilder sb = new StringBuilder();

		sb.append("Current Node: " + u + "\n");
		sb.append("Nodes Downloaded:" + toReturn.size() + "\n");
		int i = 1;
		for (Node n : toReturn) {
			sb.append(String.format("%3d)", (i++)) + n + "\n");
		}

		return sb.toString();
	}

	public boolean nextStep() {

		while (true) {
			currentNode = blocksNode.remove();
			if (currentNode == null) {
				JOptionPane.showMessageDialog(null, "No more nodes availabe");
				return false;
			}
			if (startNode != currentNode.u && !visited.contains(currentNode.u)) {

				visited.add(currentNode.u);
				toReturn.add(currentNode);
				if (++count == nBlocks) {
					return false;
				}
			}
			for (Node ot : getPossibleNodes(currentNode, currentNode.n + 1)) {

				if (!localVisited.contains(ot)) {
					blocksNode.add(ot);
					localVisited.add(ot);
				}
			}
		}

	}

	public void reset(int u) {
		blocksNode = new PriorityQueue<Node>();
		localVisited = new ArrayList<>();
		count = 0;
		this.startNode = u;
		lat2 = Double.parseDouble(vertices[destNode].getLat());
		lon2 = Double.parseDouble(vertices[destNode].getLon());
		double lat1 = Double.parseDouble(vertices[startNode].getLat());
		double lon1 = Double.parseDouble(vertices[startNode].getLon());
		double dist = distance(lat1, lon1, lat2, lon2);
		currentNode = new Node(null, startNode, 0, 1, dist);
		blocksNode.add(currentNode);
		localVisited.add(currentNode);
		possibleNodes = getPossibleNodes(currentNode, currentNode.n + 1);

	}

	public ArrayList<Integer> run(int u) {
		// Signal is lost

		lat2 = Double.parseDouble(vertices[destNode].getLat());
		lon2 = Double.parseDouble(vertices[destNode].getLon());

		int count = 0;
		this.startNode = u;

		PriorityQueue<Node> blocksNode = new PriorityQueue<Node>();
		ArrayList<Node> localVisited = new ArrayList<>();
		ArrayList<Node> toReturn = new ArrayList<>();
		double lat1 = Double.parseDouble(vertices[u].getLat());
		double lon1 = Double.parseDouble(vertices[u].getLon());
		double dist = distance(lat1, lon1, lat2, lon2);
		Node n = new Node(null, u, 0, 1, dist);
		blocksNode.add(n);
		localVisited.add(n);

		while (!blocksNode.isEmpty()) {
			Node current = blocksNode.remove();
			// System.out.println("Current Node: \n\t" + current);
			if (u != current.u && !visited.contains(current.u)) {
				visited.add(current.u);
				toReturn.add(current);
				if (++count == nBlocks) {
					break;
				}
			}

			// System.out.println("Possible Nodes:");
			for (Node ot : getPossibleNodes(current, current.n + 1)) {
				// System.out.println("\t" + ot);
				if (!localVisited.contains(ot)) {
					blocksNode.add(ot);
					localVisited.add(ot);
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
		for (Node node : toReturn) {
			res.add(node.u);
		}

		return res;
	}

	public boolean isContainedInReturned(int s) {
		for (Node n : toReturn) {
			if (n.u == s)
				return true;
		}
		return false;
	}

	public PriorityQueue<Node> getPossibleNodes(Node n, int step) {
		PriorityQueue<Node> res = new PriorityQueue<>();
		for (Pair p : vertices[n.u].getNeighboursToList()) {
			double lat1 = Double.parseDouble(vertices[p.getTo()].getLat());
			double lon1 = Double.parseDouble(vertices[p.getTo()].getLon());
			double dist = distance(lat1, lon1, lat2, lon2);
			res.add(new Node(n, p.getTo(), p.getWeight() + n.h1, step, dist));

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

	class Node implements Comparable<Node> {
		Node parent;
		int u;
		double h1;
		double h2;
		int n; // N is used for normalization

		public Node(Node parent, int u, double h1, int n, double h2) {
			this.parent = parent;
			this.u = u;
			this.h1 = h1;
			this.h2 = h2;
			this.n = n;

		}

		@Override
		public String toString() {
			double normH1 = a1 * (1 - h1 / n);
			double normH2 = a2 * h2 / MAX_DIST;

			String s0 = String.format("ID: %3d", u);
			String s1 = String.format("Ha: %.3f", normH1);
			String s2 = String.format("Hb: %.3f", normH2);
			String s3 = String.format("SUM: %.3f", (normH2 + normH1));
			return Stream.of(s0, s1, s2, s3).collect(Collectors.joining("\t", "{", "}"));

		}

		public boolean equals(Object obj) {
			Node o = (Node) (obj);
			return this.u == o.u;
		}

		@Override
		public int compareTo(Node o) {
			double normH1 = a1 * (1 - h1 / n);
			double normH2 = a2 * h2 / MAX_DIST;
			double otherNormH1 = a1 * (1 - o.h1 / n);
			double otherNormH2 = a2 * o.h2 / MAX_DIST;
			// If I have less => better
			if ((normH1 + normH2) < (otherNormH1 + otherNormH2))
				return -1;
			if ((normH1 + normH2) > (otherNormH1 + otherNormH2))
				return 1;
			return 0;
		}
	}

}