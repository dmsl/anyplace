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
import java.util.Iterator;
import java.util.Random;
import java.util.Stack;


public class SimulatedAnnealingBlocks extends AlgorithmBlocks {

	int startNode;
	Random rnd;
	int maxIterations;

	public SimulatedAnnealingBlocks(CleanPoi[] vertices, int nBlocks, Random rnd, float probLost, int maxIterations) {
		super(5, vertices, nBlocks, probLost);
		this.rnd = rnd;
		this.maxIterations = maxIterations;
	}

//	public static void main(String args[]) throws Exception {
//		CleanBuilding building = new CleanBuilding(Main.UCY_BUILDING_ID + "_0.85", false);
//		DatasetCreator dataset = new DatasetCreator(Main.UCY_BUILDING_ID + "_0.85", 1456737260395L, true);
//		AlgorithmBlocks algo = new SimulatedAnnealingBlocks(building.getVertices(), 2, dataset.random, 0.0f, 100);
//		System.out.println(algo.run(206));
//	}

	public ArrayList<Integer> run(int u) {
		float T = 1.0f;
		float coolingRate = 0.99f;
		ArrayList<Integer> res = new ArrayList<>();
		// Signal is lost
		float prob = rnd.nextFloat();
		if (prob < probLost)
			return new ArrayList<>();

		int count = 0;
		this.startNode = u;
		ArrayList<Node> visited = new ArrayList<>();
		Node s = new Node(u, 1);
		visited.add(s);

		Node treeOld = getRandomSpanningTree(s, visited, 5);
		int i = 0;
		for (i = 0; i < maxIterations; i++) {
		
			visited = new ArrayList<>();

	
			Node treeNew = clone(treeOld);
			visited.addAll(treeNodesToList(treeNew));

			Node randNodeToRM;
			int countStacked;
			Node randNodeToADD;
			ArrayList<Node> listToAdd;
			ArrayList<Node> removedNodes;

			countStacked = 0;
			ArrayList<Node> list = treeNodesToList(treeNew);
			do {
				randNodeToRM = list.get((int) (rnd.nextFloat() * list.size()));
			} while (randNodeToRM.u == u);
			removedNodes = removeNodes(treeNew, randNodeToRM);

			//System.out.println("Random Node:" + randNodeToRM);
			//System.out.println("Removed Nodes: " + removedNodes);
			// printTree(treeNew,0);

			// Add new nodes

			listToAdd = treeNodesToList(treeNew);
			visited.removeAll(removedNodes);// Set unvisited

			countStacked = 0;
			do {
				Node randNodeToADDtemp = listToAdd.get((int) (rnd.nextFloat() * listToAdd.size()));
				randNodeToADD = new Node(randNodeToADDtemp.u, randNodeToADDtemp.cost);

				countStacked++;
				if (countStacked == 20) {
					
					break;
				}

			} while (getPossibleNodes(visited, randNodeToADD).size() < removedNodes.size());

			if (countStacked == 20) {
				int c = 0;
				while (c < removedNodes.size()) {
					listToAdd = treeNodesToList(treeNew);
	
					Node randNodeToADDtemp = listToAdd.get((int) (rnd.nextFloat() * listToAdd.size()));
					randNodeToADD = new Node(randNodeToADDtemp.u, randNodeToADDtemp.cost);

					ArrayList<Node> children = getPossibleNodes(visited, randNodeToADD);
					//System.out.println(randNodeToADD);
					if (children.size() == 0)
						continue;
					Node r = children.get((int) (rnd.nextFloat() * children.size()));
					addNodeToTree(treeNew, randNodeToADD, r);
					visited.add(r);
					c++;
					//System.out.println("stacked2");
				}

			} else {
				//System.out.println("Finding random spanning Tree");
				Node subTreeToAdd = getRandomSpanningTree(randNodeToADD, visited, removedNodes.size());
				//System.out.println("Tree to add:");
				//printTree(subTreeToAdd, 0);
				mergeTrees(treeNew, subTreeToAdd);
			}

			//System.out.println("OldTree:");
			//printTree(treeOld, 0);
			//System.out.println("New:");
			//printTree(treeNew, 0);
			updateCost(treeNew, 1);
			double oldCost = getCost(treeOld);
			double newCost = getCost(treeNew);
//			System.out.println("Old Cost:" + oldCost);
//			System.out.println("New Cost:" + newCost);

			float p = rnd.nextFloat();
			//System.out.println(Math.exp(-(oldCost- newCost ) / T));
			if (newCost > oldCost || p < Math.exp(-(oldCost- newCost ) / T)) {
				treeOld = treeNew;
//				System.out.println("New solution");
			}
//			else
//				System.out.println("Old solution");
//			System.out.println("---------------------");
			T *= coolingRate;
		}
		res = convertToInteger(treeNodesToList(treeOld));
		//System.out.println("Iterations:" + i);
		res.remove(new Integer(u));
		return res;
	}

	private void addNodeToTree(Node tree, Node p, Node n) {

		if (tree == null)
			return;
		if (tree.equals(p)) {
			tree.addChildren(n);
			return;
		}
		for (Node x : tree.children) {
			if (x.equals(p)) {
				x.addChildren(n);
				return;
			}
			addNodeToTree(x, p, n);
		}

	}

	private ArrayList<Integer> convertToInteger(ArrayList<Node> treeNodesToList) {
		ArrayList<Integer> res = new ArrayList<>();
		for (Node s : treeNodesToList)
			res.add(s.u);
		return res;
	}

	private void updateCost(Node x, double weight) {
		if (x == null)
			return;
		x.cost = weight;
		double w = 0;

		for (Node s : x.children) {
			for (Pair p : vertices[x.u].getNeighboursToList()) {
				if (p.getTo() == s.u) {
					w = p.getWeight();
					break;
				}

			}
			updateCost(s, x.cost * w);
		}

	}

	private void mergeTrees(Node treeNew, Node subTreeToAdd) {

		Stack<Node> stack = new Stack<>();
		stack.push(treeNew);

		while (!stack.isEmpty()) {
			Node x = stack.pop();
			if (x.equals(subTreeToAdd)) {
				for (Node p : subTreeToAdd.children)
					x.addChildren(p);
				break;
			}

			for (Node v : x.children)

				stack.push(v);
		}

	}

	private ArrayList<Node> getPossibleNodes(ArrayList<Node> visited, Node n) {
		ArrayList<Node> res = new ArrayList<>();

		for (Pair p : vertices[n.u].getNeighboursToList()) {
			Node x = new Node(p.getTo(), p.getWeight() * n.cost);
			if (!visited.contains(x))
				res.add(x);
		}

		return res;
	}

	private ArrayList<Node> removeNodes(Node treeNew, Node randomNode) {

		ArrayList<Node> res = new ArrayList<>();
		Stack<Node> stack = new Stack<>();
		stack.push(treeNew);

		while (!stack.isEmpty()) {
			Node x = stack.pop();

			Iterator<Node> neigh = x.children.iterator();
			while (neigh.hasNext()) {
				Node v = neigh.next();

				if (v.equals(randomNode)) {
					getChildrens(res, v);
					// res.add(v);
					neigh.remove();
					break;
				}
				stack.push(v);
			}

		}

		return res;
	}

	public void printTree(Node s, int indent) {
		for (int i = 0; i < indent; i++)
			System.out.print(" ");

		System.out.println(s.u);
		for (Node p : s.children)
			printTree(p, indent + 1);
	}

	public ArrayList<Node> treeNodesToList(Node s) {
		ArrayList<Node> res = new ArrayList<>();
		Stack<Node> stack = new Stack<>();
		stack.push(s);

		while (!stack.isEmpty()) {
			Node x = stack.pop();
			// if (x.children.size() != 0)
			res.add(x);
			for (Node v : x.children)

				stack.add(v);
		}
		return res;
	}

	public ArrayList<Node> treeLeafsToList(Node s) {
		ArrayList<Node> res = new ArrayList<>();
		Stack<Node> stack = new Stack<>();
		stack.push(s);

		while (!stack.isEmpty()) {
			Node x = stack.pop();

			boolean hasChildren = false;
			for (Node v : x.children) {
				stack.add(v);
				hasChildren = true;
			}
			if (!hasChildren)
				res.add(x);
		}
		return res;
	}

	public int countChildrens(Node s) {
		if (s == null)
			return 0;

		int count = 0;
		for (Node p : s.children) {
			count += countChildrens(p) + 1;
		}
		return count;
	}

	public int getChildrens(ArrayList<Node> children, Node s) {

		if (s == null)
			return 0;

		int count = 0;
		children.add(s);
		for (Node p : s.children) {
			count += getChildrens(children, p) + 1;
		}
		return count;
	}

	public Node clone(Node x) {

		Node p = new Node(x.u, x.cost);
		for (Node o : x.children) {
			p.addChildren(clone(o));
		}
		return p;
	}

	public double getCost(Node x) {

		double cost = x.cost;
		for (Node o : x.children) {
			cost += getCost(o);
		}
		return cost;
	}

	public Node getRandomSpanningTree(Node s, ArrayList<Node> visited, int k) {
		// ArrayList<Node> res = new ArrayList<>();
		int counter = 0;
		ArrayList<Node> list = new ArrayList<>();
		list.add(s);

		while (!list.isEmpty()) {
			int rr = (int) (rnd.nextFloat() * list.size());
			Node x = list.remove(rr);

			for (Node n : getPossibleNodes(x)) {
				if (counter == k)
					return s;
				if (!visited.contains(n)) {
					visited.add(n);
					list.add(n);
					x.addChildren(n);
					counter++;
				}

			}
		}

		return s;
	}

	public ArrayList<Node> getPossibleNodes(Node n) {
		ArrayList<Node> res = new ArrayList<>();
		for (Pair p : vertices[n.u].getNeighboursToList())
			res.add(new Node(p.getTo(), p.getWeight() * n.cost));

		return res;
	}

	private class Node implements Comparable<Node> {

		int u;
		double cost;
		ArrayList<Node> children = new ArrayList<>();

		public Node(int u, double cost) {

			this.u = u;
			this.cost = cost;
		}

		public void addChildren(Node c) {
			children.add(c);
		}

		@Override
		public String toString() {
			/*
			 * StringBuilder sb = new StringBuilder(); Stack<Node> s = new
			 * Stack<>(); s.push(this); while (!s.isEmpty()) { Node x = s.pop();
			 * sb.append(x.u + " "); for (Node v : x.children) s.push(v); }
			 */
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

}