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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;

public class CleanPoi implements Comparable<CleanPoi> {
	public static float R = 6371000;
	private String pid;
	private String floor;
	private String lat, lon;
	private String name;
	private PriorityQueue<Pair> neighbours;
	private double pagerank;
	private int id;
	private double x;
	private double y;
	private String type;
	private String description;
	private String fp;

	public CleanPoi(String pid, String name, String floor, String lat, String lon, String type, String description,
			int id, String fp) {
		this.pid = pid;
		this.setFloor(floor);
		this.setLat(lat);
		this.setLon(lon);
		this.name = name;
		this.setId(id);
		this.setType(type);
		this.setDescription(description);
		this.fp = fp;
		neighbours = new PriorityQueue<>(4);
	}

	public List<Pair> getNeighboursToList() {
		List<Pair> data = new LinkedList<>();
	
		data.addAll(neighbours);
		Collections.sort(data);
		return data;
	}

	public void setX(double x) {
		this.x = x;
	}

	public void setY(double y) {
		this.y = y;
	}

	public double getX() {
		return x;
	}

	public double getY() {
		return y;
	}

	public String getPid() {
		return pid;
	}
	
	public void setFp(String fp) {
		this.fp = fp;
	}
	
	public String getFp() {
		return fp;
	}

	@Override
	public String toString() {

		return pid;
	}

	public void addPair(Pair p) {
		
		neighbours.add(p);

	}

	public PriorityQueue<Pair> getNeighbours() {

		return neighbours;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public boolean contains(Pair p) {
		for (Pair s : neighbours) {
			if (p.getTo() == s.getTo())
				return true;
		}
		return false;

	}

	public String getLat() {
		return lat;
	}

	public void setLat(String lat) {
		this.lat = lat;
	}

	public String getLon() {
		return lon;
	}

	public void setLon(String lon) {
		this.lon = lon;
	}

	public String getFloor() {
		return floor;
	}

	public void setFloor(String floor) {
		this.floor = floor;
	}

	@Override
	public int compareTo(CleanPoi o) {
		if (pagerank > o.pagerank)
			return -1;
		if (pagerank < o.pagerank)
			return 1;
		return 0;
	}

	public double getPagerank() {
		return pagerank;
	}

	public void setPagerank(double pagerank) {
		this.pagerank = pagerank;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getPID() {
		return this.pid;
	}

	/**
	 * Check if the probability to go to ancestors sums up to 1
	 * 
	 * @return
	 */
	public boolean isProbCorrect() {
		float sum = 0;
		for (Pair p : neighbours)
			sum += p.getWeight();
		return (Math.abs(sum - 1) < 1e-5) ? true : false;
	}

	public void setNeighbours(PriorityQueue<Pair> corr) {
		this.neighbours = corr;

	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description
	 *            the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type
	 *            the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
}
