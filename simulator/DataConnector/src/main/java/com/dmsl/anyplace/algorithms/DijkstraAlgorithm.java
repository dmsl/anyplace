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
package com.dmsl.anyplace.algorithms;

import java.util.*;

/**
 * Dijkstra Implementation
 * 
 * @author zgeorg03
 *
 */
public class DijkstraAlgorithm {

	public static class Graph {
		private final Map<Integer,Vertex> vertices;
		private final List<Edge> edges;

		public Graph(Map<Integer,Vertex> vertexes, List<Edge> edges) {
			this.vertices = vertexes;
			this.edges = edges;
		}

		public Map<Integer,Vertex> getVertexes() {
			return vertices;
		}

		public List<Edge> getEdges() {
			return edges;
		}
	}

	public static class Vertex implements Comparable<Vertex> {
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

	public static class Edge {
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

	public final Map<Integer,Vertex> nodes;
	final List<Edge> edges;
	private Set<Vertex> settledNodes;
	private Set<Vertex> unSettledNodes;
	private Map<Vertex, Vertex> predecessors;
	private Map<Vertex, Double> distance;

	public DijkstraAlgorithm(Graph graph) {
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
	 * This method returns the path from the source to the selected target and
	 * NULL if no path exists
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