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
import com.dmsl.anyplace.buildings.clean.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;


public class BFSSrcDest extends AlgorithmSrcDest {

	public BFSSrcDest(int id, CleanPoi[] vertices) {
		super(id, vertices);
	}
	
	class Node{
		Node parent;
		int data;
		
		public Node(Node node, int u){
			this.parent = node;
			this.data =u;
		}
	}

	@Override
	public ArrayList<Integer> run(int u, int v) {
		Queue<Node> queue = new LinkedList<Node>();
		ArrayList<Integer> visited = new ArrayList<>();
		Node n = new Node(null, u);
		queue.add(n);
		visited.add(n.data);

		while (!queue.isEmpty()) {
			Node current = queue.remove();
			if (current.data == v) {
				n = current;
				break;
			}
			for (Pair p : vertices[current.data].getNeighboursToList()) {
				if (!visited.contains(p.getTo())) {
					queue.add(new Node(current,p.getTo()));
					visited.add(p.getTo());
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
			res.add(stack.pop().data);
		}

		return res;
	}
}
