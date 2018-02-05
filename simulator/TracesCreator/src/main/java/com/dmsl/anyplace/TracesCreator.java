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


import com.dmsl.anyplace.algorithms.srcdest.AlgorithmSrcDest;
import com.dmsl.anyplace.algorithms.srcdest.BFSSrcDest;
import com.dmsl.anyplace.buildings.clean.CleanBuilding;

import java.io.*;
import java.util.ArrayList;
import java.util.Random;

public class TracesCreator {

	static int MAX_FACULTY_PATHS = 100;
	static int MAX_STUDENT_PATHS = 100;
	static int MAX_CLEANINGLADY_PATHS = 100;
	static boolean SAME_FLOOR = true;

	float source[];

	float probabilities[][];

	private String buid;
	private Destination destinations;
	private ArrayList<ArrayList<Integer>> dataset;

	// Room, Indoor, None, Stair, Outdoor, Toilets, First Aid/AED, Disabled
	// Toilets, Office, Elevator
	public static void main(String[] args) {
		try {
			new TracesCreator(DataConnector.BUILDING_ID, 0, MAX_FACULTY_PATHS, SAME_FLOOR);
			new TracesCreator(DataConnector.BUILDING_ID, 1, MAX_STUDENT_PATHS, SAME_FLOOR);
			new TracesCreator(DataConnector.BUILDING_ID, 2, MAX_CLEANINGLADY_PATHS, SAME_FLOOR);
		} catch (Exception e) {

			e.printStackTrace();
		}
	}

	private void setData(int algo) {
		// faculty
		if (algo == 0) {
			// room, entrance, toilets, office, outdoor(garden)
			float src[] = { 0.2f, 0.4f, 0.08f, 0.3f, 0.02f };

			// room, entrance, toilets, office, outdoor(garden)
			float prob[][] = { { 0.1f, 0.35f, 0.1f, 0.1f, 0.05f }, // room
					{ 0.35f, 0.1f, 0.1f, 0.4f, 0.05f }, // entrance
					{ 0.2f, 0.2f, 0.0f, 0.55f, 0.05f }, // toilets
					{ 0.25f, 0.4f, 0.2f, 0.1f, 0.05f }, // office
					{ 0.2f, 0.1f, 0.2f, 0.5f, 0.0f } }; // outdoor(garden)

			this.source = src;
			this.probabilities = prob;
		} else if (algo == 1) { // students
			// room, entrance, toilets, office, outdoor(garden)
			float src[] = { 0.35f, 0.4f, 0.1f, 0.1f, 0.05f };

			// room, entrance, toilets, office, outdoor(garden)
			float prob[][] = { { 0.4f, 0.35f, 0.1f, 0.07f, 0.08f }, // room
					{ 0.55f, 0.2f, 0.1f, 0.07f, 0.08f }, // entrance
					{ 0.45f, 0.4f, 0.0f, 0.07f, 0.08f }, // toilets
					{ 0.4f, 0.25f, 0.2f, 0.07f, 0.08f }, // office
					{ 0.45f, 0.3f, 0.18f, 0.07f, 0.0f } }; // outdoor(garden)

			this.source = src;
			this.probabilities = prob;
		} else {// cleaning lady
			// room, entrance, toilets, office, outdoor(garden)
			float src[] = { 0.35f, 0.1f, 0.35f, 0.1f, 0.1f };

			// room, entrance, toilets, office, outdoor(garden)
			float prob[][] = { 
					{ 0.35f, 0.1f, 0.35f, 0.1f, 0.1f }, // room
					{ 0.4f, 0.1f, 0.4f, 0.05f, 0.05f }, // entrance
					{ 0.4f, 0.05f, 0.4f, 0.05f, 0.1f }, // toilets
					{ 0.35f, 0.1f, 0.35f, 0.1f, 0.1f }, // office
					{ 0.45f, 0.05f, 0.45f, 0.05f, 0.0f } }; // outdoor(garden)
			
			this.source = src;
			this.probabilities = prob;
		}

	}

	public TracesCreator(String buid, int algo, int max, boolean sameFloor) throws Exception {
		this.buid = buid;
		setData(algo);

		File path = new File(DataConnector.TRACES_PATH);
		if (!path.exists()) {
			if(path.mkdirs())
				System.out.println("[Info] Directory: " + DataConnector.TRACES_PATH + " created");
		}
		String filename = "";
		if (algo == 0){
			if (sameFloor)
				filename = DataConnector.TRACES_PATH + buid + "_samefloor" + ".faculty";
			else
				filename = DataConnector.TRACES_PATH + buid + "_differentfloor" + ".faculty";
		}else if(algo==1) {
			if (sameFloor)
				filename = DataConnector.TRACES_PATH + buid + "_samefloor" + ".students";
			else
				filename = DataConnector.TRACES_PATH + buid + "_differentfloor" + ".students";
		}else{
			if (sameFloor)
				filename = DataConnector.TRACES_PATH + buid + "_samefloor" + ".cleaninglady";
			else
				filename = DataConnector.TRACES_PATH + buid + "_differentfloor" + ".cleaninglady";
		}
			

		File file = new File(filename);
		if (file.exists())
			file.delete();

		File readFile = new File(DataConnector.GRAPH_PATH +buid+"/"+ buid + ".typeDescription");
		if (!readFile.exists()) {
			System.out.println("Type Description file does not exist!");
			return;
		}

		this.parseDestinations(readFile);
		this.createDatasets(max, sameFloor);
		this.writeToFile(dataset, filename);

	}

	private void writeToFile(ArrayList<ArrayList<Integer>> dataset2, String filename) throws Exception {
		File file = new File(filename);

		if (file.isDirectory()) {
			System.err.println("[Error]: This is a directory");
			return;
		}
		if (file.exists()) {
			System.out.println("[Info]: File: " + filename + " would be overwritten");
		}

		BufferedWriter bf = new BufferedWriter(new FileWriter(file));

		for (ArrayList<Integer> list : dataset) {
			bf.write(printList(list));
			bf.write("\n");
		}

		bf.close();
		System.out.println("[Info]: File: " + filename + " successfuly saved");

	}

	private String printList(ArrayList<Integer> list) {
		String str = "";
		int count = 0;
		for (Integer integer : list) {
			str += integer;
			if (count == list.size() - 1)
				break;
			str += ",";
			count++;
		}
		return str;
	}

	private void createDatasets(int max, boolean sameFloor) throws Exception {
		this.dataset = new ArrayList<>();

		// sum sources
		for (int i = 1; i < source.length; i++) {
			source[i] += source[i - 1];
		}

		// sum probabilities
		for (int i = 0; i < probabilities.length; i++) {
			for (int j = 1; j < probabilities[0].length; j++) {
				probabilities[i][j] += probabilities[i][j - 1];
			}
		}

		CleanBuilding building = new CleanBuilding(DataConnector.BUILDING_ID, false);
		AlgorithmSrcDest algo = new BFSSrcDest(1, building.getVertices());

		for (int i = 0; i < max; i++) {
			int index = getSourceIndex();
			int start = getStartPoint(index);

			int destIndex = getDestinationIndex(index);
			int stop = getDestinationPoint(start, destIndex);

			if (sameFloor) {
				String startfloor = building.getVertices()[start].getFloor();
				String stopfloor = building.getVertices()[stop].getFloor();
				while (!startfloor.equals(stopfloor)) {
					destIndex = getDestinationIndex(index);
					stop = getDestinationPoint(start, destIndex);

					stopfloor = building.getVertices()[stop].getFloor();
				}
			}

			ArrayList<Integer> result = algo.run(start, stop);
			if (result.size() == 1) {
				System.err.println(start + " " + stop);
			}
			dataset.add(result);
		}

	}

	private int getDestinationIndex(int index) {
		Random ran = new Random();
		float prob = ran.nextFloat();
		for (int i = 0; i < probabilities[index].length; i++) {
			if (probabilities[index][i] <= prob)
				return i;
		}
		return 0;
	}

	private int getSourceIndex() {
		Random ran = new Random();
		float prob = ran.nextFloat();
		for (int i = 0; i < source.length; i++) {
			if (source[i] <= prob)
				return i;
		}
		return 0;
	}

	private int getStartPoint(int index) {
		Random ran = new Random();
		if (index == 0) { // room
			ArrayList<Integer> dest = destinations.getRoom();
			return dest.get(ran.nextInt(dest.size()));
		} else if (index == 1) { // indoor
			ArrayList<Integer> dest = destinations.getIndoor();
			return dest.get(ran.nextInt(dest.size()));
		} else if (index == 2) { // toilet
			ArrayList<Integer> dest = destinations.getToilets();
			return dest.get(ran.nextInt(dest.size()));
		} else if (index == 3) { // office
			ArrayList<Integer> dest = destinations.getOffice();
			return dest.get(ran.nextInt(dest.size()));
		} else { // outdoor
			ArrayList<Integer> dest = destinations.getOutdoor();
			return dest.get(ran.nextInt(dest.size()));
		}
	}

	private int getDestinationPoint(int start, int index) {
		Random ran = new Random();
		int destination = start;
		while (start == destination) {
			if (index == 0) { // room
				ArrayList<Integer> dest = destinations.getRoom();
				destination = dest.get(ran.nextInt(dest.size()));
			} else if (index == 1) { // indoor
				ArrayList<Integer> dest = destinations.getIndoor();
				destination = dest.get(ran.nextInt(dest.size()));
			} else if (index == 2) { // toilet
				ArrayList<Integer> dest = destinations.getToilets();
				destination = dest.get(ran.nextInt(dest.size()));
			} else if (index == 3) { // office
				ArrayList<Integer> dest = destinations.getOffice();
				destination = dest.get(ran.nextInt(dest.size()));
			} else { // outdoor
				ArrayList<Integer> dest = destinations.getOutdoor();
				destination = dest.get(ran.nextInt(dest.size()));
			}
		}

		return destination;
	}

	private void parseDestinations(File file) throws Exception {
		destinations = new Destination();
		BufferedReader bf = new BufferedReader(new FileReader(file));
		String line = "";
		int c = 0;
		while ((line = bf.readLine()) != null) {
			if (c == 0) {
				c++;
				continue;
			}
			String toks[] = line.split("\t");
			switch (toks[1]) {
			case "Room":
				destinations.addRoom(Integer.parseInt(toks[0]));
				break;
			case "Indoor":
				destinations.addIndoor(Integer.parseInt(toks[0]));
				break;
			case "None":
				destinations.addNone(Integer.parseInt(toks[0]));
				break;
			case "Stair":
				destinations.addStair(Integer.parseInt(toks[0]));
				break;
			case "Outdoor":
				destinations.addOutdoor(Integer.parseInt(toks[0]));
				break;
			case "Toilets":
				destinations.addToilets(Integer.parseInt(toks[0]));
				break;
			case "First Aid/AED":
				destinations.addFirstAid(Integer.parseInt(toks[0]));
				break;
			case "Disabled Toilets":
				destinations.addDisabledWC(Integer.parseInt(toks[0]));
				break;
			case "Office":
				destinations.addOffice(Integer.parseInt(toks[0]));
				break;
			case "Elevator":
				destinations.addElevator(Integer.parseInt(toks[0]));
				break;
			// case "Entrance":
			// destinations.addEntrance(Integer.parseInt(toks[0]));
			// break;
			default:
				System.err.println(line);
			}
		}
		bf.close();
	}

	class Destination {
		public ArrayList<Integer> room;
		public ArrayList<Integer> indoor;
		public ArrayList<Integer> none;
		public ArrayList<Integer> stair;
		public ArrayList<Integer> outdoor;
		public ArrayList<Integer> toilets;
		public ArrayList<Integer> firstAid;
		public ArrayList<Integer> disabledWC;
		public ArrayList<Integer> office;
		public ArrayList<Integer> elevator;
		public ArrayList<Integer> entrance;

		public Destination() {
			this.room = new ArrayList<>();
			this.indoor = new ArrayList<>();
			this.none = new ArrayList<>();
			this.stair = new ArrayList<>();
			this.outdoor = new ArrayList<>();
			this.toilets = new ArrayList<>();
			this.firstAid = new ArrayList<>();
			this.disabledWC = new ArrayList<>();
			this.office = new ArrayList<>();
			this.elevator = new ArrayList<>();
			this.entrance = new ArrayList<>();
		}

		public void addEntrance(int entrance) {
			this.entrance.add(entrance);
		}

		public void addElevator(int elevator) {
			this.elevator.add(elevator);
		}

		public void addOffice(int office) {
			this.office.add(office);
		}

		public void addDisabledWC(int wc) {
			this.disabledWC.add(wc);
		}

		public void addFirstAid(int aid) {
			this.firstAid.add(aid);
		}

		public void addToilets(int toilet) {
			this.toilets.add(toilet);
		}

		public void addOutdoor(int outdoor) {
			this.outdoor.add(outdoor);
		}

		public void addStair(int stair) {
			this.stair.add(stair);
		}

		public void addNone(int none) {
			this.none.add(none);
		}

		public void addIndoor(int indoor) {
			this.indoor.add(indoor);
		}

		public void addRoom(int room) {
			this.room.add(room);
		}

		public ArrayList<Integer> getDisabledWC() {
			return disabledWC;
		}

		public ArrayList<Integer> getElevator() {
			return elevator;
		}

		public ArrayList<Integer> getFirstAid() {
			return firstAid;
		}

		public ArrayList<Integer> getIndoor() {
			return indoor;
		}

		public ArrayList<Integer> getNone() {
			return none;
		}

		public ArrayList<Integer> getOffice() {
			return office;
		}

		public ArrayList<Integer> getOutdoor() {
			return outdoor;
		}

		public ArrayList<Integer> getRoom() {
			return room;
		}

		public ArrayList<Integer> getStair() {
			return stair;
		}

		public ArrayList<Integer> getToilets() {
			return toilets;
		}

		public ArrayList<Integer> getEntrance() {
			return entrance;
		}
	}
}
