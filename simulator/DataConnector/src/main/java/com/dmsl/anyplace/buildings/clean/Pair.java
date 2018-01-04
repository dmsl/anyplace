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

public class Pair implements Comparable<Pair> {

	private int from;
	private int to;
	private double weight;
	private int sigLevel;

	// Maybe floor is useful in future
	private String floor;

	public Pair(int from, int to, String floor) {
		this(from, to, floor, 1);
	}

	public Pair(int from, int to, String floor, double w) {
		this.to = to;
		this.floor = floor;
		this.from = from;
		weight = w;
	}

	public Pair(int from, int to) {
		this(from, to, "");
	}

	@Override
	public String toString() {
		return from + "->" + to + "(" + weight + "," + sigLevel + ")";
	}

	public int getTo() {
		return to;
	}

	public double getWeight() {
		return weight;
	}

	@Override
	public int compareTo(Pair o) {
		if (this.weight < o.weight)
			return 1;
		if (this.weight > o.weight)
			return -1;
		return 0;
	}

	public void setWeight(double weight) {
		this.weight = weight;

	}

	public int getFrom() {
		return from;
	}

	/**
	 * @return the sigLevel
	 */
	public int getSigLevel() {
		return sigLevel;
	}

	/**
	 * @param sigLevel
	 *            the sigLevel to set
	 */
	public void setSigLevel(int sigLevel) {
		this.sigLevel = sigLevel;
	}
}
