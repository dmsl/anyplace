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

import com.dmsl.anyplace.buildings.clean.CleanBuilding;
import com.dmsl.anyplace.buildings.clean.CleanPoi;
import com.dmsl.anyplace.buildings.clean.Pair;

import java.util.*;


public class ME2 extends AlgorithmBlocks {

	// csucy
	static double leftlat = 35.14404722486025;
	static double leftlon = 33.41077988429299;
	static double rightlat = 35.14492259219234;
	static double rightlon = 33.41176947996616;
	// end

	// mall
//	static double leftlat = 35.1294167436441;
//	static double leftlon = 33.3702045785156;
//	static double rightlat = 35.13055078307128;
//	static double rightlon = 33.372469402843116;
	// end

	// hotel
//	static double leftlat = 40.441281160896274;
//	static double leftlon = -80.00696810480697;
//	static double rightlat = 40.442182121722986;
//	static double rightlon = -80.00595469223975;
	// end
	
	
	static double MAX_DIST = distance(leftlat, leftlon, rightlat, rightlon);
	static double MAX_DIST_H3 = -1; // TODO change this

	int startNode;
	final static int ID = 2;
	Random rnd;
	static float a1 = 0.0f;
	static float a2 = 0.0f;
	static float a3 = 1.0f;

	// This has to be set
	private int destNode;
	private double lat2;
	private double lon2;

	/**
	 * Call this only one time
	 * 
	 * @param building
	 */
	public void createGraph(CleanBuilding building) {
		List<DijsktraAlgorithm.Vertex> vertices = new ArrayList<>();
		List<DijsktraAlgorithm.Edge> edges = new ArrayList<>();
		CleanPoi[] pois = building.getVertices();
		int ids = 0;
		for (CleanPoi poi : pois)
			vertices.add(new DijsktraAlgorithm.Vertex(poi.getId()));

		for (CleanPoi poi : pois) {
			int from = poi.getId();

			double lat1 = Double.parseDouble(poi.getLat());
			double lon1 = Double.parseDouble(poi.getLon());

			for (Pair p : poi.getNeighbours()) {
				int to = p.getTo();
				double lat2 = Double.parseDouble(pois[to].getLat());
				double lon2 = Double.parseDouble(pois[to].getLon());
				double dist = distance(lat1, lon1, lat2, lon2);
				edges.add(new DijsktraAlgorithm.Edge(ids++, vertices.get(from), vertices.get(to), dist));
			}
		}

		DijsktraAlgorithm.Graph graph = new DijsktraAlgorithm.Graph(vertices, edges);
//		System.out.println(vertices);
		dijkstra = new DijsktraAlgorithm(graph);

		double maxDist = Double.MIN_VALUE;
		for (DijsktraAlgorithm.Vertex v : vertices) {
			dijkstra.execute(v);
			for (DijsktraAlgorithm.Vertex l : vertices) {
				if (v.equals(l))
					continue;
				double dist = dijkstra.getMinDistance(l);
				if (maxDist < dist)
					maxDist = dist;

			}
		}
		MAX_DIST_H3 = maxDist;
//		System.out.println(MAX_DIST_H3);

	}

	public ME2(CleanPoi[] vertices, int nBlocks, Random rnd, float probLost, CleanBuilding building) {
		super(ID, vertices, nBlocks, probLost);
		this.rnd = rnd;
		createGraph(building);
	}

	static DijsktraAlgorithm dijkstra;

	public ArrayList<Integer> run(int u) {
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

		double h3 = dijkstra.execute(dijkstra.nodes.get(u), dijkstra.nodes.get(destNode));
		//System.out.println(h3);
		//System.out.println("Shortest:" + dijkstra.getPath(dijkstra.nodes.get(destNode)));

		Node n = new Node(null, u, 0, 1, dist, h3);
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

		if(res.size()!=15)
			System.err.println("!15");
		return res;
	}

	public PriorityQueue<Node> getPossibleNodes(Node n, int step) {
		PriorityQueue<Node> res = new PriorityQueue<>();
		for (Pair p : vertices[n.u].getNeighboursToList()) {
			double lat1 = Double.parseDouble(vertices[p.getTo()].getLat());
			double lon1 = Double.parseDouble(vertices[p.getTo()].getLon());
			double dist = distance(lat1, lon1, lat2, lon2);
			double h3 = dijkstra.execute(dijkstra.nodes.get(n.u), dijkstra.nodes.get(destNode));

			res.add(new Node(n, p.getTo(), p.getWeight() + n.h1, step, dist, h3));

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

	/**
	 * Dijkstra Implementation
	 * 
	 * @author zgeorg03
	 *
	 */
	private static class DijsktraAlgorithm {

		static class Graph {
			private final List<Vertex> vertices;
			private final List<Edge> edges;

			public Graph(List<Vertex> vertexes, List<Edge> edges) {
				this.vertices = vertexes;
				this.edges = edges;
			}

			public List<Vertex> getVertexes() {
				return vertices;
			}

			public List<Edge> getEdges() {
				return edges;
			}
		}

		static class Vertex implements Comparable<Vertex> {
			final public int id;

			public Vertex(int id) {
				this.id = id;
			}

			@Override
			public String toString() {

				return id + "";
			}

			@Override
			public boolean equals(Object obj) {
				if (obj instanceof Vertex) {
					return ((Vertex) obj).id == id;
				}
				return super.equals(obj);
			}

			@Override
			public int compareTo(Vertex o) {

				return (id == o.id) ? 0 : -1;
			}
		}

		static class Edge {
			private final int id;
			private final Vertex source;
			private final Vertex destination;
			private final double weight;

			public Edge(int id, Vertex source, Vertex destination, double weight) {
				this.id = id;
				this.source = source;
				this.destination = destination;
				this.weight = weight;
			}

			public int getId() {
				return id;
			}

			public Vertex getDestination() {
				return destination;
			}

			public Vertex getSource() {
				return source;
			}

			public double getWeight() {
				return weight;
			}

			@Override
			public String toString() {
				return source + " -> " + destination;
			}
		}

		private final List<Vertex> nodes;
		private final List<Edge> edges;
		private Set<Vertex> settledNodes;
		private Set<Vertex> unSettledNodes;
		private Map<Vertex, Vertex> predecessors;
		private Map<Vertex, Double> distance;

		public DijsktraAlgorithm(Graph graph) {
			this.nodes = graph.vertices;
			this.edges = graph.edges;
		}

		public void execute(Vertex source) {

			settledNodes = new HashSet<Vertex>();
			unSettledNodes = new HashSet<Vertex>();
			distance = new HashMap<Vertex, Double>();
			predecessors = new HashMap<Vertex, Vertex>();
			distance.put(source, 0.0);
			unSettledNodes.add(source);
			while (unSettledNodes.size() > 0) {
				Vertex node = getMinimum(unSettledNodes);
				settledNodes.add(node);
				unSettledNodes.remove(node);
				findMinimalDistances(node);
			}

		}

		public Double getMinDistance(Vertex dest) {
			Double d = distance.get(dest);
			if (d == null)
				return Double.MIN_VALUE;
			return d;
		}

		public Double execute(Vertex source, Vertex dest) {

			settledNodes = new HashSet<Vertex>();
			unSettledNodes = new HashSet<Vertex>();
			distance = new HashMap<Vertex, Double>();
			predecessors = new HashMap<Vertex, Vertex>();
			distance.put(source, 0.0);
			unSettledNodes.add(source);
			while (unSettledNodes.size() > 0) {
				Vertex node = getMinimum(unSettledNodes);
				settledNodes.add(node);
				unSettledNodes.remove(node);
				findMinimalDistances(node);
			}
			Double d = distance.get(dest);
			if (d == null)
				return Double.MIN_VALUE;
			return d;
		}

		/*
		 * This method returns the path from the source to the selected target
		 * and NULL if no path exists
		 */
		public LinkedList<Vertex> getPath(Vertex target) {
			LinkedList<Vertex> path = new LinkedList<Vertex>();
			Vertex step = target;
			// check if a path exists
			if (predecessors.get(step) == null) {
				return null;
			}
			path.add(step);
			while (predecessors.get(step) != null) {
				step = predecessors.get(step);
				path.add(step);
			}
			// Put it into the correct order
			Collections.reverse(path);
			return path;
		}

		private void findMinimalDistances(Vertex node) {
			List<Vertex> adjacentNodes = getNeighbors(node);
			for (Vertex target : adjacentNodes) {
				if (getShortestDistance(target) > getShortestDistance(node) + getDistance(node, target)) {
					distance.put(target, getShortestDistance(node) + getDistance(node, target));
					predecessors.put(target, node);
					unSettledNodes.add(target);
				}
			}

		}

		private List<Vertex> getNeighbors(Vertex node) {
			List<Vertex> neighbors = new ArrayList<Vertex>();
			for (Edge edge : edges) {
				if (edge.getSource().equals(node) && !isSettled(edge.getDestination())) {
					neighbors.add(edge.getDestination());
				}
			}
			return neighbors;
		}

		private boolean isSettled(Vertex vertex) {
			return settledNodes.contains(vertex);
		}

		private double getDistance(Vertex node, Vertex target) {
			for (Edge edge : edges) {
				if (edge.getSource().equals(node) && edge.getDestination().equals(target)) {
					return edge.getWeight();
				}
			}
			throw new RuntimeException("Should not happen");
		}

		private Vertex getMinimum(Set<Vertex> vertexes) {
			Vertex minimum = null;
			for (Vertex vertex : vertexes) {
				if (minimum == null) {
					minimum = vertex;
				} else {
					if (getShortestDistance(vertex) < getShortestDistance(minimum)) {
						minimum = vertex;
					}
				}
			}
			return minimum;
		}

		private double getShortestDistance(Vertex destination) {
			Double d = distance.get(destination);
			if (d == null) {
				return Double.MAX_VALUE;
			} else {
				return d;
			}
		}

	}

	private class Node implements Comparable<Node> {
		Node parent;
		int u;
		double h1;
		double h2;
		double h3;
		int n; // N is used for normalization

		public Node(Node parent, int u, double h1, int n, double h2, double h3) {
			this.parent = parent;
			this.u = u;
			this.h1 = h1;
			this.h2 = h2;
			this.h3 = h3;
			this.n = n;

		}

		@Override
		public String toString() {
			double normH1 = a1 * (1 - h1 / n);
			double normH2 = a2 * h2 / MAX_DIST;
			String s1 = String.format("%.2f", normH1);
			String s2 = String.format("%.2f", normH2);
			String s3 = String.format("%.2f", (normH2 + normH1));
			return u + "(\t" + s1 + "\t" + s2 + "\t" + s3 + "\t" + n + ")";
		}

		public boolean equals(Object obj) {
			Node o = (Node) (obj);
			return this.u == o.u;
		}

		@Override
		public int compareTo(Node o) {
			double normH1 = a1 * (1 - h1 / n);
			double normH2 = a2 * h2 / MAX_DIST;
			double normH3 = a3 * h3 / MAX_DIST_H3;

			double otherNormH1 = a1 * (1 - o.h1 / n);
			double otherNormH2 = a2 * o.h2 / MAX_DIST;
			double otherNormH3 = a3 * o.h3 / MAX_DIST_H3;

			double s1 = normH1 + normH2 + normH3;
			double s2 = otherNormH1 + otherNormH2 + otherNormH3;
			// If I have less => better
			if (s1 < s2)
				return -1;
			if (s1 > s2)
				return 1;
			return 0;
		}
	}

}