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

import com.dmsl.anyplace.buildings.clean.CleanPoi;
import com.google.gson.JsonObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;


public class RadioMapSplit {
	private String filename;

	private ArrayList<String> fps;
	private String firstRow;
	private String secondRow;
	private String floor;
	private int range;
	private double perRowLat;
	private double perColLon;
	private GridFingerprints[][] fpsGrid;
	private CleanPoi[] vertices;
	boolean[][] assigned;
	private GridFingerprints[] pois;

	private double minLat;
	private double minLon;

	public RadioMapSplit(CleanPoi[] vertices) {
		this.vertices = vertices;
		this.pois = new GridFingerprints[vertices.length];
	}

	public void radioMapSplit(String filename, String floor, int range) {
		this.perColLon = 0;
		this.firstRow = null;
		this.secondRow = null;
		this.perRowLat = 0;
		this.filename = filename;
		this.range = range;
		this.floor = floor;
		this.fps = new ArrayList<>();
		assigned = new boolean[range][range];
		fpsGrid = new GridFingerprints[range][range];
		if (parseFile(this.filename)) {
			findMinLatLon();
			assignFingerPrintsToGrid();
			addFingerPrintsToPois();
			System.out.println("Assigned fingerprints to "+assignedFingerPrintsToEmptyPois()+" empty pois!");
		} else {
			System.err.println("Cannot parse file");
		}

		System.out.println("Finish!");
	}

	public int assignedFingerPrintsToEmptyPois() {
		int count = 0;
		for (int i = 0; i < vertices.length; i++) {
			double minDistance = Double.MAX_VALUE;
			int row = -1, col = -1;
			CleanPoi p = vertices[i];
			if (!p.getFloor().equals(floor) || pois[i] != null)
				continue;
			double plat = Double.parseDouble(p.getLat());
			double plon = Double.parseDouble(p.getLon());

			for (int k = 0; k < range; k++) {
				for (int j = 0; j < range; j++) {
					if (fpsGrid[k][j] == null)
						continue;

					double euDistance = distance(plat, plon, fpsGrid[k][j].lat, fpsGrid[k][j].lon);
					if (euDistance < minDistance) {
						minDistance = euDistance;
						row = k;
						col = j;
					}

				}
			}
			
			if (row == -1 || col==-1) {
				System.err.println("Cannot add fingerprints to poi!");
			}
			if (pois[i] == null) {
				pois[i] = new GridFingerprints(firstRow, secondRow);
			}
			pois[i].addAllFps(fpsGrid[row][col].getFps());
			count++;
		}
		return count;
	}

	void printGrid() {
		int count = 0;
		for (int i = 0; i < range; i++) {
			for (int j = 0; j < range; j++) {
				if (!assigned[i][j])
					// if (fpsGrid[i][j] == null)
					System.out.print("|-");
				else {
					System.out.print("|1");
					count++;
				}
			}
			System.out.println();
		}
		System.out.println(count);
	}

	public int countTotalFps() {
		int count = 0;
		for (GridFingerprints g : pois) {
			if (g != null && g.getFps() != null)
				count += g.getFps().size();
		}
		return count;
	}

	public int countEmptyPois() {
		int c = 0;
		for (int i = 0; i < pois.length; i++)
			if (pois[i] == null)
				c++;
		return c;
	}

	public CleanPoi[] loadNewVertices() {
		for (int i = 0; i < pois.length; i++) {
			if (pois[i] == null)
				continue;

			String jsonString = getFpString(i);
			vertices[i].setFp(jsonString);
		}

		return vertices;
	}

	private String getFpString(int x) {

		GridFingerprints fp = pois[x];

		StringBuilder builder = new StringBuilder();
		builder.append(fp.firstRow);
		builder.append("\n");
		builder.append(fp.secondRow);
		builder.append("\n");
		for (String str : fp.getFps()) {
			builder.append(str);
			builder.append("\n");
		}

		JsonObject obj = new JsonObject();
		obj.addProperty("id", x);
		obj.addProperty("fprints", builder.toString());

		return obj.toString();
	}

	private void addFingerPrintsToPois() {

		for (int i = 0; i < vertices.length; i++) {
			CleanPoi p = vertices[i];
			// if poi is not at the same floor
			if (!p.getFloor().equals(floor))
				continue;
			double plat = Double.parseDouble(p.getLat());
			double plon = Double.parseDouble(p.getLon());

			GridLocation l = getGridLocation(plat, plon);
			if (fpsGrid[l.row][l.col] != null) {
				assigned[l.row][l.col] = true;

				int position = p.getId();
				if (pois[position] == null) {
					pois[position] = new GridFingerprints(firstRow, secondRow);
				}
				pois[position].addAllFps(fpsGrid[l.row][l.col].getFps());
			}
		}

		for (int i = 0; i < range; i++) {
			for (int j = 0; j < range; j++) {
				if (fpsGrid[i][j] == null || assigned[i][j])
					continue;
				addToPoi(fpsGrid[i][j]);
			}
		}
	}

	private void addToPoi(GridFingerprints gfp) {

		double minDistance = Double.MAX_VALUE;
		int position = -1;

		for (int i = 0; i < vertices.length; i++) {
			CleanPoi p = vertices[i];
			// if poi is not at the same floor
			if (!p.getFloor().equals(floor))
				continue;
			double plat = Double.parseDouble(p.getLat());
			double plon = Double.parseDouble(p.getLon());

			double euDistance = distance(plat, plon, gfp.lat, gfp.lon);
			if (euDistance < minDistance) {
				minDistance = euDistance;
				position = i;
			}

		}

		if (position == -1) {
			System.err.println("Cannot add fingerprints to poi!");
		}
		if (pois[position] == null) {
			pois[position] = new GridFingerprints(firstRow, secondRow);
		}
		pois[position].addAllFps(gfp.getFps());

	}

	private double distance(double lat1, double lon1, double lat2, double lon2) {

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

	public CleanPoi[] getVertices() {
		return vertices;
	}

	private void assignFingerPrintsToGrid() {
		for (String string : fps) {
			String tmpFp = string;
			if (tmpFp.trim().equals(""))
				continue;
			tmpFp = tmpFp.replace(", ", " ");
			String[] temp = tmpFp.split(" ");

			double lat = Double.parseDouble(temp[0]);
			double lon = Double.parseDouble(temp[1]);

			GridLocation gLoc = getGridLocation(lat, lon);
			if (fpsGrid[gLoc.row][gLoc.col] == null) {
				fpsGrid[gLoc.row][gLoc.col] = new GridFingerprints(firstRow, secondRow, lat, lon);
			}
			fpsGrid[gLoc.row][gLoc.col].addFps(string);
		}

	}

	private GridLocation getGridLocation(double lat, double lon) {
		int row = 0, col = 0;
		boolean foundRow = false, foundCol = false;

		for (int i = range - 1; i >= 0; i--) {
			if (!foundRow) {
				double maxLat = minLat + (perRowLat * (range - i));
				if (lat < maxLat) {
					row = i;
					foundRow = true;
				}
			}

			if (!foundCol) {

				for (int j = 0; j < range; j++) {
					double maxLon = minLon + (perColLon * (j + 1));
					if (lon < maxLon) {
						col = j;
						foundCol = true;
						break;
					}
				}
			}
			if (foundRow && foundCol)
				break;
		}

		if (!foundRow)
			row = 0;
		if (!foundCol)
			col = range - 1;

		return new GridLocation(row, col);
	}

	private void findMinLatLon() {
		double minLat = Double.MAX_VALUE;
		double maxLat = Double.MIN_VALUE;

		double minLon = Double.MAX_VALUE;
		double maxLon = Double.MIN_VALUE;

		for (String string : fps) {
			if (string.trim().equals(""))
				continue;
			string = string.replace(", ", " ");
			String[] temp = string.split(" ");

			double lat = Double.parseDouble(temp[0]);
			double lon = Double.parseDouble(temp[1]);

			if (lat > maxLat)
				maxLat = lat;
			if (lat < minLat)
				minLat = lat;
			if (lon > maxLon)
				maxLon = lon;
			if (lon < minLon)
				minLon = lon;
		}

		this.minLat = minLat;
		this.minLon = minLon;

		double lonDiff = maxLon - minLon;
		double latDiff = maxLat - minLat;

		perRowLat = latDiff / range;
		perColLon = lonDiff / range;
	}

	private boolean parseFile(String filename) {
		File file = new File(filename);

		try {
			String[] temp = null;
			String line = null;
			BufferedReader reader = new BufferedReader(new FileReader(file));

			// read 2 first lines
			line = reader.readLine();
			firstRow = line;
			temp = line.split(" ");
			if (!temp[1].equals("NaN")) {
				reader.close();
				return false;
			}

			line = reader.readLine();
			secondRow = line;
			// 2nd line must exist
			if (line == null) {
				reader.close();
				return false;
			}

			while ((line = reader.readLine()) != null) {
				fps.add(line);
			}
			reader.close();
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return false;
	}

	private class GridLocation {
		int row;
		int col;

		public GridLocation(int row, int col) {
			this.row = row;
			this.col = col;
		}
	}

	private class GridFingerprints {
		private ArrayList<String> fps;
		private String firstRow;
		private String secondRow;
		private double lat;
		private double lon;

		public GridFingerprints(String fRow, String sRow, double lat, double lon) {
			this(fRow, sRow);
			this.lat = lat;
			this.lon = lon;
		}

		public GridFingerprints(String fRow, String sRow) {
			fps = new ArrayList<>();
			this.secondRow = sRow;
			this.firstRow = fRow;
		}

		public void addFps(String fin) {
			fps.add(fin);
		}

		public String getFirstRow() {
			return firstRow;
		}

		public String getSecondRow() {
			return secondRow;
		}

		public ArrayList<String> getFps() {
			return fps;
		}

		public void addAllFps(ArrayList<String> list) {
			fps.addAll(list);
		}
	}
}
