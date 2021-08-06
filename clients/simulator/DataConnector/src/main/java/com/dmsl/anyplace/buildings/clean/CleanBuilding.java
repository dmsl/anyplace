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
package com.dmsl.anyplace.buildings.clean;

import com.dmsl.anyplace.buildings.connections.Connection;
import com.dmsl.anyplace.buildings.connections.Connections;
import com.dmsl.anyplace.DataConnector;
import com.dmsl.anyplace.buildings.pois.Poi;
import com.dmsl.anyplace.buildings.pois.Pois;
import com.dmsl.anyplace.fingerprints.FingerPrints;
import com.google.gson.Gson;

import java.io.*;
import java.util.*;

public class CleanBuilding {

	private Pois pois;
	private Connections connections;
	private String buid;
	private String name;
	private String lat, lon;

	private CleanPoi[] vertices;

	public double PR[];
	private double oldPR[];
	private FingerPrints fps[];

	public double dampingParameter = 0.85;

	public CleanBuilding(String buid, String name, String lat, String lon,
			Pois pois, Connections connections) {
		this.pois = pois;
		this.connections = connections;
		this.buid = buid;
		this.lat = lat;
		this.lon = lon;
		this.name = name;

		// This is the stair on the first floor
		// this.pois.remove("poi_0dfd09bd-44c6-41b3-b067-191d697723ee");

		vertices = new CleanPoi[this.pois.getPois().size()];

		loadVertices();
		loadEdges();

		calculateSignificanceLevel();
		calculatePR(true);
	}

	public void calculateSignificanceLevel() {
		for (int u = 0; u < vertices.length; u++) {

			for (Pair p : vertices[u].getNeighbours()) {
				int v = p.getTo();
				ArrayList<Integer> visited = new ArrayList<>();
				visited.add(u);
				int sigLevel = getSignficanceLevel(v, visited);
				p.setSigLevel(sigLevel);
				// System.out.println(u + "->" + v + ":" + sigLevel);
			}
		}
	}

	public void calculatePR(boolean normalized) {
		PR = new double[vertices.length];
		oldPR = new double[vertices.length];
		for (int i = 0; i < PR.length; i++) {
			PR[i] = (double) 1;

		}

		// System.out.println(calculatePRdist());
		while (calculatePRdist() > 10e-16) {

			pagerank(dampingParameter);

		}

		normalizePageRank(normalized);

		// for (int i = 0; i < vertices.length; i++)
		// System.out.println(vertices[i].getPagerank());

		assignWeights();
	}

	public void normalizePageRank(boolean flag) {
		if (flag) {
			double max = 0;
			for (int i = 0; i < vertices.length; i++)
				if (PR[i] > max)
					max = PR[i];
			for (int i = 0; i < vertices.length; i++)
				vertices[i].setPagerank(PR[i] / max);
			for (int i = 0; i < vertices.length; i++)
				PR[i] = vertices[i].getPagerank();
		} else {
			for (int i = 0; i < vertices.length; i++)
				vertices[i].setPagerank(PR[i]);
			for (int i = 0; i < vertices.length; i++)
				PR[i] = vertices[i].getPagerank();
		}

	}

	public CleanBuilding(String buid, boolean test) throws Exception {
		this.buid = buid;
		File file;
		if (test)
			file = new File(DataConnector.GRAPH_TEST_PATH + buid + "/" + buid + "."
					+ DataConnector.FILE_FORMAT);
		else
			file = new File(DataConnector.GRAPH_PATH + buid + "/" + buid + "."
					+ DataConnector.FILE_FORMAT);

		parseFile(file);

		calculateSignificanceLevel();
		calculatePR(true);

	}

	public void parseFpFile() throws Exception {
		File fpFile = new File(DataConnector.FINGERPRINTS_PATH + buid + "/" + buid + "."
				+ DataConnector.FILE_FORMAT);
		parseFPFile(fpFile);

		for (int i = 0; i < vertices.length; i++) {
			String str = null;
			if (fps[i] != null)
				str = fps[i].getFprints();

			vertices[i].setFp(str);
		}
	}

	private void parseFPFile(File file) throws Exception {
		BufferedReader bf = new BufferedReader(new FileReader(file));
		String line;
		// Read the first line
		line = bf.readLine();

		// Get number of fingerprints
		String toks[] = line.split(" ");
		int size = Integer.parseInt(toks[1]);

		fps = new FingerPrints[size];

		Gson gson = new Gson();
		// Read the fingerprints
		for (int i = 0; i < size; i++) {
			line = bf.readLine();
			if (line == null)
				throw new Exception("File fingerprints is not valid");

			toks = line.split("\\|");

			int id = Integer.parseInt(toks[0].trim());
			String jsonString = toks[1].trim();

			if (jsonString.equals("null")) {
				fps[id] = null;
				continue;
			} else {
				fps[id] = gson.fromJson(jsonString, FingerPrints.class);
			}

		}

		bf.close();
	}

	private void parseFile(File file) throws Exception {
		BufferedReader bf = new BufferedReader(new FileReader(file));
		String line = "";

		// Read the first line
		line = bf.readLine();

		// Get name
		String toks[] = line.split("\"");
		this.name = toks[1];

		// Get number of vertices
		toks = line.split(" ");
		int numVertices = Integer.parseInt(toks[1]);
		vertices = new CleanPoi[numVertices];

		// Read the vertices
		for (int i = 0; i < numVertices; i++) {
			line = bf.readLine();
			if (line == null)
				throw new Exception("File is not valid");

			toks = line.split("\"");

			int id = Integer.parseInt(toks[0].trim());
			String name = toks[1];

			toks = toks[2].split(" ");

			String lat = toks[1];
			String lon = toks[2];
			String floor = toks[3];
			double PR = Double.parseDouble(toks[4]);
			String pid = toks[5];

			String fp = null;
			if (fps != null && fps[id] != null)
				fp = fps[id].getFprints();

			CleanPoi p = new CleanPoi(pid, name, floor, lat, lon, "", "", id,
					fp);

			p.setPagerank(PR);
			vertices[id] = p;
		}

		line = bf.readLine();

		int numEdges = Integer.parseInt(line.split(" ")[1]);
		for (int i = 0; i < numEdges; i++) {
			line = bf.readLine();
			if (line == null)
				throw new Exception("File is not valid");

			toks = line.split(" ");
			int ida = Integer.parseInt(toks[0]);
			int idb = Integer.parseInt(toks[1]);
			double weight = Double.parseDouble(toks[2]);
			Pair p1 = new Pair(ida, idb);
			vertices[ida].addPair(p1);
			p1.setWeight(weight);

		}
		bf.close();

	}

	/**
	 * Store all the pois in a vertices array
	 */
	public void loadVertices() {
		int i = 0;
		for (Poi p : pois.getPois()) {
			String pid = p.getPuid();
			String floor = p.getFloorNumber();
			String lat = p.getCoordinatesLat();
			String lon = p.getCoordinatesLon();
			String name = p.getName();
			String description = p.getDescription();

			CleanPoi s = new CleanPoi(pid, name, floor, lat, lon,
					p.getPoisType(), description, i, null);
			vertices[i++] = s;
		}
	}

	public void loadEdges() {
		// Temporal solution with O(1) search to find the index
		HashMap<String, Integer> map = new HashMap<>();
		for (int i = 0; i < vertices.length; i++) {
			if(map.containsKey(vertices[i].getPid()))
				System.err.println("ree");
			map.put(vertices[i].getPid(), i);
		}
		System.out.println("EdgesSize:" + map.size());

		// Get each connection and add it to the neighbor list of the
		// appropriate vertex
		for (Connection conn : connections.getConnections()) {

			String keyA = conn.getPoisA();
			String keyB = conn.getPoisB();

			if (keyA.equals(keyB))
				continue;


			String floorA = conn.getFloorA();
			String floorB = conn.getFloorB();

			Integer indA = map.get(keyA);
			Integer indB = map.get(keyB);
			
			if (indA == null || indB == null){
				continue;
			}

			Pair p1 = new Pair(indA, indB, floorB);
			Pair p2 = new Pair(indB, indA, floorA);

			if (!vertices[indA].contains(p1))
				vertices[indA].addPair(p1);
			if (!vertices[indB].contains(p2))
				vertices[indB].addPair(p2);
		}
		
	}

	@Override
	public String toString() {

		return this.name;
	}

	public CleanPoi[] getVertices() {
		return vertices;
	}

	/**
	 * Get all the vertices from the given floor
	 * 
	 * @param floor
	 * @return
	 */
	public ArrayList<CleanPoi> getVerticesByFloor(String floor) {
		ArrayList<CleanPoi> v = new ArrayList<>();
		for (int i = 0; i < vertices.length; i++) {

			if (vertices[i].getFloor().equals(floor))
				v.add(vertices[i]);

		}
		return v;
	}

	public String getBuid() {

		return buid;
	}

	public int countIsolated() {
		int count = 0;
		for (CleanPoi p : vertices) {
			if (p.getNeighbours().isEmpty())
				count++;
		}
		return count;
	}

	private void DFSRecursive(Integer u, Stack<Integer> path,
			ArrayList<List<Integer>> paths) {

		visited.add(u);
		path.push(u);

		CleanPoi p = vertices[u];

		for (Pair v : p.getNeighbours()) {
			if (!visited.contains(v.getTo()))
				DFSRecursive(v.getTo(), path, paths);
		}
		boolean leafNode = isLeafNode(p, this.visited);
		if (leafNode)
			paths.add(path);
		path.pop();
		visited.remove(u);

	}

	/** Used for dfs and bfs */
	ArrayList<Integer> visited = new ArrayList<Integer>();

	/**
	 * Acyclic Breadth first search
	 * 
	 * @param u
	 * @param depth
	 * @return
	 */
	public ArrayList<List<Integer>> BFS(int u, int depth) {
		ArrayList<List<Integer>> paths = new ArrayList<>();

		LinkedList<ArrayList<Integer>> queue = new LinkedList<>();
		ArrayList<Integer> path = new ArrayList<>();
		ArrayList<Integer> cDepth = new ArrayList<>();

		path.add(u);
		queue.push(path);
		visited.add(u);
		cDepth.add(0);
		while (!queue.isEmpty()) {
			ArrayList<Integer> temp = queue.remove();
			CleanPoi poi = vertices[temp.get(temp.size() - 1)];
			int currentDepth = cDepth.remove(0);
			if (currentDepth == depth) {
				paths.add(temp);
				continue;
			}

			for (Pair p : poi.getNeighbours()) {
				if (temp.contains(p.getTo()))
					continue;
				// visited.add(p.getTo());
				ArrayList<Integer> c = new ArrayList<>(temp);
				c.add(p.getTo());
				queue.add(c);
				cDepth.add(currentDepth + 1);
			}

		}
		visited.clear();
		return paths;
	}
	
	

	/**
	 * Depth First search
	 * 
	 * @param u
	 * @return
	 */
	public ArrayList<List<Integer>> DFS(int u) {
		ArrayList<List<Integer>> paths = new ArrayList<>();
		Stack<Integer> path = new Stack<>();
		DFSRecursive(u, path, paths);
		visited.clear();
		return paths;
	}

	/**
	 * Check if the nodes has no neighbors
	 * 
	 * @param node
	 * @param visited
	 * @return
	 */
	private boolean isLeafNode(CleanPoi node, ArrayList<Integer> visited) {
		int count = 0;
		for (Pair p : node.getNeighbours()) {
			Integer v = p.getTo();
			if (!visited.contains(v))
				count++;

		}
		return (count == 0) ? true : false;
	}

	public boolean writeToFile(String filename) throws IOException {

		File path = new File(DataConnector.GRAPH_PATH + buid + "/");
		if (!path.exists()) {
			if(path.mkdirs())
				System.out.println("[Info] Directory: " + DataConnector.GRAPH_PATH
					+ " created");

		}
		File file = new File(DataConnector.GRAPH_PATH + buid + "/" + filename + "." + DataConnector.FILE_FORMAT);
		if (file.isDirectory()) {
			System.err.println("[Error]: This is a directory");
			return false;
		}
		if (file.exists()) {
			System.out.println("[Info]: File: " + filename
					+ " will be overwritten");
		}

		BufferedWriter bf = new BufferedWriter(new FileWriter(file));
		bf.write("*Vertices " + vertices.length + " \"" + this.name + "\" "
				+ this.buid + "\n");

		int count = 0;
		for (CleanPoi p : vertices) {
			bf.write(p.getId() + " \"" + p.getName() + "\" " + p.getLat() + " "
					+ p.getLon() + " " + p.getFloor() + " " + p.getPagerank()
					+ " " + p.getPID() + "\n");
			count += p.getNeighbours().size();
		}
		bf.write("*Arcs " + count + "\n");
		for (int i = 0; i < vertices.length; i++) {
			CleanPoi p = vertices[i];
			PriorityQueue<Pair> neigh = p.getNeighbours();
			for (Pair v : neigh) {
				bf.write(i + " " + v.getTo() + " " + v.getWeight() + "\n");
			}

		}
		bf.close();
		System.out.println("[Info]: File: " + filename + " successfuly saved");

		return true;
	}

	public boolean writeToDot(String filename) throws IOException {

		File path = new File(DataConnector.GRAPH_PATH);
		if (!path.exists()) {
			if(path.mkdirs())
				System.out.println("[Info] Directory: " + DataConnector.GRAPH_PATH
					+ " created");

		}
		File file = new File(DataConnector.GRAPH_PATH + filename + ".dot");
		if (file.isDirectory()) {
			System.err.println("[Error]: This is a directory");
			return false;
		}
		if (file.exists()) {
			System.out.println("[Info]: File: " + filename
					+ " will be overwritten");
		}

		BufferedWriter bf = new BufferedWriter(new FileWriter(file));
		bf.write("digraph{\n");

		for (int i = 0; i < vertices.length; i++) {
			CleanPoi p = vertices[i];
			PriorityQueue<Pair> neigh = p.getNeighbours();
			for (Pair v : neigh) {
				bf.write(i + " -> " + v.getTo() + " [ label=" + v.getWeight()
						+ ",weight=" + v.getWeight() + ",z=" + p.getFloor()
						+ "];\n");
			}

		}
		bf.write("}\n");

		bf.close();
		System.out.println("[Info]: File: " + filename + " successfuly saved");

		return true;
	}

	public void writeToFile() throws IOException {
		writeToFile(this.getBuid());

	}

	public void writeToDot() throws IOException {
		writeToDot(this.getBuid());

	}

	public void writeToWindows(int floor) throws IOException {
		writeToWindows(this.getBuid(), floor);
	}

	public boolean writeToWindows(String filename, int floor)
			throws IOException {

		File file = new File(DataConnector.DATA_PATH + filename + "_" + dampingParameter
				+ "_" + floor + ".wtxt");
		if (file.isDirectory()) {
			System.err.println("[Error]: This is a directory");
			return false;
		}
		if (file.exists()) {
			System.out.println("[Info]: File: " + filename
					+ " would be overwritten");
		}

		BufferedWriter bf = new BufferedWriter(new FileWriter(file));

		for (CleanPoi p : vertices)
			if (Integer.parseInt(p.getFloor()) == floor)
				bf.write(p.getId() + " " + p.getLat() + " " + p.getLon() + " "
						+ p.getPagerank() + "\n");

		bf.close();
		System.out.println("[Info]: File: " + filename + " successfuly saved");

		return true;
	}

	public List<CleanPoi> getNodesByRank() {
		List<CleanPoi> ranked = new ArrayList<>();
		for (int i = 0; i < vertices.length; i++) {
			vertices[i].setPagerank(PR[i]);
			ranked.add(vertices[i]);
		}
		Collections.sort(ranked);
		return ranked;
	}

	/**
	 * PageRank algorithm
	 * 
	 * @param d
	 */
	public void pagerank(double d) {
		for (int i = 0; i < vertices.length; i++)
			oldPR[i] = PR[i];

		for (int u = 0; u < vertices.length; u++) {
			CleanPoi p = vertices[u];
			double part = (1 - d) / PR.length;
			double sum = 0;

			for (Pair pv : findNeighborsReferencingMe(u)) {

				int j = pv.getFrom();
				int sigLevel = pv.getSigLevel();

				double prv = oldPR[j] * sigLevel;
				int totalSigLevel = 0;
				for (Pair o : vertices[j].getNeighbours()) {
					totalSigLevel += o.getSigLevel();
				}
				sum += prv / (double) totalSigLevel;
			}

			PR[u] = part + d * sum;

		}

	}

	public ArrayList<Pair> findNeighborsReferencingMe(int u) {
		ArrayList<Pair> result = new ArrayList<>();
		for (int i = 0; i < vertices.length; i++) {
			if (i == u)
				continue;
			CleanPoi p = vertices[i];
			for (Pair pr : p.getNeighbours()) {
				if (pr.getTo() == u) {
					result.add(pr);
				}
			}

		}
		return result;
	}

	/**
	 * Assign weights to all the edges based on page rank of the vertices
	 */
	public void assignWeights() {
		for (int i = 0; i < vertices.length; i++) {
			CleanPoi p = vertices[i];
			PriorityQueue<Pair> old = p.getNeighbours();
			float sum = 0;
			for (Pair pp : old) {
				sum += vertices[pp.getTo()].getPagerank();
			}
			for (Pair pp : old) {

				pp.setWeight(vertices[pp.getTo()].getPagerank() / sum);
			}
			PriorityQueue<Pair> corr = new PriorityQueue<>();
			while (!old.isEmpty()) {
				corr.add(old.remove());
			}
			p.setNeighbours(corr);

		}
	}

	/**
	 * Calculate the euclidean distance between the two vectors PR(current) and
	 * PR(previous state)
	 * 
	 * @return
	 */
	public double calculatePRdist() {
		double sum = 0;
		for (int i = 0; i < vertices.length; i++) {
			sum += (PR[i] - oldPR[i]) * (PR[i] - oldPR[i]);

		}
		return Math.sqrt(sum);
	}

	/**
	 * Find the isolated nodes from the graph
	 * 
	 * @return
	 */
	public String findIsolatedNodes() {
		StringBuilder sb = new StringBuilder();
		for (CleanPoi p : vertices) {
			if (p.getNeighbours().isEmpty()) {
				sb.append(p.getId() + "\t" + p.getName() + "\t" + p.getPid()
						+ "\n");
			}

		}
		return sb.toString();
	}

	/**
	 * Get the Significance Level of the given node
	 * 
	 * @param u
	 * @return
	 */
	public int getSignficanceLevel(Integer u, ArrayList<Integer> visited) {
		ArrayList<SigNode<CleanPoi>> vertices = new ArrayList<SigNode<CleanPoi>>();

		for (CleanPoi p : this.getVertices())
			vertices.add(new SigNode<CleanPoi>(p, 0));
		// visited.add(u);
		return significanceRecursevily(u, vertices, visited) + 1;

	}

	/**
	 * Get recursively significance amount of the given node This
	 * 
	 * @param u
	 * @param vertices
	 * @param visited
	 * @return
	 */
	public int significanceRecursevily(Integer u,
			ArrayList<SigNode<CleanPoi>> vertices, ArrayList<Integer> visited) {

		if (visited.contains(u))
			return vertices.get(u).significanceLevel;

		visited.add(u);
		// Get significance of neighbours
		boolean flag = false;
		for (Pair p : vertices.get(u).data.getNeighbours()) {
			int v = p.getTo();
			if (vertices.get(u).data.getFloor().compareTo(
					vertices.get(v).data.getFloor()) != 0) {

				continue;
			}
			if (!visited.contains(v))
				vertices.get(u).significanceLevel += significanceRecursevily(v,
						vertices, visited) + 1;
		}

		return vertices.get(u).significanceLevel;
	}

	public Pois getPois() {
		return pois;
	}

	public String getName() {
		return name;
	}

	public void writeToBIOlayout() throws IOException {
		writeToBIOlayout(this.getBuid());
	}

	public boolean writeToBIOlayout(String filename) throws IOException {

		File path = new File(DataConnector.GRAPH_PATH);
		if (!path.exists()) {
			path.mkdir();
			System.out.println("[Info] Directory: " + DataConnector.GRAPH_PATH
					+ " created");

		}
		File file = new File(DataConnector.GRAPH_PATH + filename + ".biolayout");
		if (file.isDirectory()) {
			System.err.println("[Error]: This is a directory");
			return false;
		}
		if (file.exists()) {
			System.out.println("[Info]: File: " + filename
					+ " would be overwritten");
		}

		BufferedWriter bf = new BufferedWriter(new FileWriter(file));

		for (int i = 0; i < vertices.length; i++) {
			CleanPoi p = vertices[i];
			PriorityQueue<Pair> neigh = p.getNeighbours();
			for (Pair v : neigh) {
				bf.write(i + " " + v.getTo() + " " + v.getWeight() + "\n");
			}

		}
		bf.close();
		System.out.println("[Info]: File: " + filename + " successfuly saved");

		return true;
	}

	public void writeToFileTypesDescriptions() throws IOException {
		writeToFileTypesDescriptions(this.getBuid());

	}

	private boolean writeToFileTypesDescriptions(String filename)
			throws IOException {
		File path = new File(DataConnector.GRAPH_PATH + buid + "/");
		if (!path.exists()) {
			path.mkdir();
			System.out.println("[Info] Directory: " + DataConnector.GRAPH_PATH + buid
					+ "/" + " created");
		}
		File file = new File(DataConnector.GRAPH_PATH + buid + "/" + filename
				+ ".typeDescription");
		if (file.isDirectory()) {
			System.err.println("[Error]: This is a directory");
			return false;
		}
		if (file.exists()) {
			System.out.println("[Info]: File: " + filename
					+ " would be overwritten");
		}

		BufferedWriter bf = new BufferedWriter(new FileWriter(file));

		bf.write("NumberOfVertices:\t" + vertices.length + "\n");
		for (int i = 0; i < vertices.length; i++) {
			CleanPoi p = vertices[i];
			bf.write(i + "\t" + p.getType() + "\n");

		}
		bf.close();
		System.out.println("[Info]: File: " + filename + " successfuly saved");

		return true;
	}

	public void setVertices(CleanPoi[] loadNewVertices) {
		this.vertices = loadNewVertices;
	}
}

/**
 * Helper Class for Significance Level
 * 
 * @author zgeorg03
 *
 * @param <E>
 */
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
