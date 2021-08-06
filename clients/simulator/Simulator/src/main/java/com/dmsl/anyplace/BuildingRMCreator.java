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

import java.io.*;
import java.util.ArrayList;
import java.util.TreeSet;


public class BuildingRMCreator {
	private String[] floors;
	private ArrayList<ArrayList<String>> floorMaps;
	private ArrayList<ArrayList<String>> completedFloorMap;
	private String firstLine;
	private TreeSet<String> macAddresses;
	private ArrayList<String> rmRSS;
	private String buid;

	public BuildingRMCreator(String[] fl, String buid) {
		this.floors = fl;
		this.floorMaps = new ArrayList<>();
		this.completedFloorMap = new ArrayList<>();
		this.macAddresses = new TreeSet<>();
		this.rmRSS = new ArrayList<>();
		this.buid = buid;

		for (String string : fl) {
			if (!parseFile(string)) {
				System.out.println("Cannot parse file");
				break;
			}
		}

		// create radio map
		createRM();

		try {
			// write all building fps
			if (!writeToFile())
				System.out.println("Failed to write file!");
			
			// replace each floor fingerprints with the new ones
			// this will contain all fps in each floor
			for (int i=0;i<fl.length;i++) {
				if (!writeFloorToFile(fl[i]+"_new",completedFloorMap.get(i)))
					System.out.println("Failed to write file: "+fl[i]+"_new");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private boolean writeFloorToFile(String filename, ArrayList<String>rss) throws IOException {
		StringBuilder builder = new StringBuilder();
		builder.append(firstLine + "\n");
		builder.append("# X, Y, HEADING");
		for (String str : macAddresses) {
			builder.append(", " + str);
		}
		for (String str : rss) {
			builder.append("\n" + str);
		}

		File path = new File(DataConnector.FINGERPRINTS_PATH);
		if (!path.exists()) {
			path.mkdir();
			System.out.println("[Info] Directory: " + DataConnector.FINGERPRINTS_PATH + " created");

		}
		File file = new File(filename);
		if (file.isDirectory()) {
			System.err.println("[Error]: This is a directory");
			return false;
		}
		if (file.exists()) {
			System.out.println("[Info]: File: " + filename + " would be overwritten");
		}

		BufferedWriter bf = new BufferedWriter(new FileWriter(file));
		bf.write(builder.toString());
		bf.close();

		System.out.println("[Info]: File: " + filename + " successfuly saved");
		return true;
	}

	private boolean writeToFile() throws IOException {
		StringBuilder builder = new StringBuilder();
		builder.append(firstLine + "\n");
		builder.append("# X, Y, HEADING");
		for (String str : macAddresses) {
			builder.append(", " + str);
		}
		for (String str : rmRSS) {
			builder.append("\n" + str);
		}

		File path = new File(DataConnector.FINGERPRINTS_PATH+buid+"/");
		if (!path.exists()) {
			path.mkdir();
			System.out.println("[Info] Directory: " + DataConnector.FINGERPRINTS_PATH+buid+"/" + " created");

		}
		File file = new File(DataConnector.FINGERPRINTS_PATH + buid+"/" + "all" + "." + DataConnector.FILE_FORMAT);
		if (file.isDirectory()) {
			System.err.println("[Error]: This is a directory");
			return false;
		}
		if (file.exists()) {
			System.out.println("[Info]: File: " + buid+"/" + "all" + " would be overwritten");
		}

		BufferedWriter bf = new BufferedWriter(new FileWriter(file));
		bf.write(builder.toString());
		bf.close();

		System.out.println("[Info]: File: " + buid+"/" + "all" + " successfuly saved");
		return true;
	}

	private void createRM() {
		for (ArrayList<String> floor : floorMaps) {
			String x = null, y = null, heading = null;
			String[] rss = new String[macAddresses.size()];
			ArrayList<String> cMac = new ArrayList<>();
			ArrayList<String> cfloor = new ArrayList<>();
			final int startOfRSS = 4;
			String[] temp;

			for (int i = 0; i < floor.size(); i++) {
				String str = floor.get(i);

				if (i == 0) {
					str = str.replace(", ", " ");
					temp = str.split(" ");

					// Must have more than 4 fields
					if (temp.length < startOfRSS) {
						break;
					}

					// Store all Mac Addresses Heading Added
					for (int j = startOfRSS; j < temp.length; ++j)
						cMac.add(temp[j]);

					continue;
				}

				if (str.trim().equals(""))
					continue;

				str = str.replace(", ", " ");
				temp = str.split(" ");

				if (temp.length < startOfRSS)
					break;

				x = temp[0];
				y = temp[1];
				heading = temp[2];

				for (int j = startOfRSS - 1; j < temp.length; ++j) {
					if (macAddresses.contains(cMac.get(j - (startOfRSS - 1)))) {
						int position = macAddresses.headSet(cMac.get(j - (startOfRSS - 1))).size();
						rss[position] = temp[j];
					} else {
						System.err.println("Something wrong");
					}
				}

				for (int j = 0; j < rss.length; j++) {
					if (rss[j] == null || rss[j].isEmpty()) {
						rss[j] = "-110";
					}
				}

				StringBuilder builder = new StringBuilder();
				builder.append(x + ", " + y + ", " + heading);
				for (String str1 : rss) {
					builder.append(", " + str1);
				}
				String fp = builder.toString();
				rmRSS.add(fp);
				cfloor.add(fp);
			}
			completedFloorMap.add(cfloor);
		}
	}

	private boolean parseFile(String filename) {
		ArrayList<String> fp = new ArrayList<>();

		try {
			File file = new File(filename);

			String[] temp = null;
			String line = null;
			BufferedReader reader = new BufferedReader(new FileReader(file));

			// read 2 first lines
			line = reader.readLine();
			firstLine = line;
			temp = line.split(" ");
			if (!temp[1].equals("NaN")) {
				reader.close();
				return false;
			}

			line = reader.readLine();
			fp.add(line);
			// 2nd line must exist
			if (line == null) {
				reader.close();
				return false;
			}

			line = line.replace(", ", " ");
			temp = line.split(" ");

			final int startOfRSS = 4;

			// Must have more than 4 fields
			if (temp.length < startOfRSS) {
				reader.close();
				return false;
			}

			// Store all Mac Addresses Heading Added
			for (int i = startOfRSS; i < temp.length; ++i)
				this.macAddresses.add(temp[i]);

			while ((line = reader.readLine()) != null) {
				if(line.equals("-"))
					line = "-110"; // remove this if fps file is ok
				fp.add(line);
			}
			reader.close();

			this.floorMaps.add(fp);

			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
}
