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

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

public class CreateLookAhead {
	private String filename;
	private int max;
	private ArrayList<String> lookAhead;

//	public static void main(String[] args) throws Exception {
//		new CreateLookAhead(Main.FINGERPRINTS_PATH
//				+ Main.UCY_BUILDING_ID + "/" + "all.pajek", 90);
//		
//		// total fps 4878
//		// max 45 for 5%
//		// max 52 for 10%
//		// max 59 for 15%
//		// max 62 for 20%
//	}

	public CreateLookAhead(String filename,int max) {
		this.lookAhead = new ArrayList<String>();
		this.filename = filename;
		this.max = max;
		try {
			parseFile();
		} catch (Exception e) {
			System.out.println("Can not parse file!");
			e.printStackTrace();
		}
	}

	public void parseFile() throws Exception {
		String line = null;
		String[] temp = null;

		BufferedReader reader = new BufferedReader(new FileReader(filename));

		// read 2 first lines
		line = reader.readLine();
		temp = line.split(" ");
		if (!temp[1].equals("NaN")) {
			reader.close();
		}

		line = reader.readLine();

		line = line.replace(", ", " ");
		temp = line.split(" ");

		final int startOfRSS = 4;

		// Must have more than 4 fields
		if (temp.length < startOfRSS)
			return;
		
		int count = 0;
		while ((line = reader.readLine()) != null) {

			if (line.trim().equals(""))
				continue;
			line = line.replace(", ", " ");
			temp = line.split(" ");

			if (temp.length < startOfRSS) {
				return;
			}


			for (int i = startOfRSS - 1; i < temp.length; ++i){
				int val = (int) Math.abs(Math.round(Double.parseDouble(temp[i])));
				if(val<=max){
					lookAhead.add(temp[0]+","+temp[1]);
					break;
				}
			}
			count++;
		}
		
//		System.out.println("Total fps: "+count);
//		System.out.println("Lookahead size: "+lookAhead.size());
//		double perc = ((double)lookAhead.size()*100.0)/(double)count;
//		System.out.println("Percentage: "+perc);
	}
	
	public ArrayList<String> getLookAhead() {
		return lookAhead;
	}
}
