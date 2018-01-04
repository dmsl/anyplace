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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

public class CheckFPFilesTest {

	static ArrayList<String> MacAdressList;

	public static void main(String[] args) throws IOException {
		MacAdressList = new ArrayList<>();

		String line = null;
		String[] temp = null;

		BufferedReader reader = new BufferedReader(
				new FileReader("data/fingerprints/username_1373876832005_all.pajek"));

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

		// Store all Mac Addresses Heading Added
		for (int i = startOfRSS; i < temp.length; ++i)
			MacAdressList.add(temp[i]);
		ArrayList<String> RSS_Values;
		while ((line = reader.readLine()) != null) {

			if (line.trim().equals(""))
				continue;

			line = line.replace(", ", " ");
			temp = line.split(" ");

			if (temp.length < startOfRSS) {
				return;
			}

			RSS_Values = new ArrayList<String>();

			for (int i = startOfRSS - 1; i < temp.length; ++i)
				RSS_Values.add(temp[i]);

			// Equal number of MAC address and RSS Values
			if (MacAdressList.size() != RSS_Values.size()) {
				System.out.println(MacAdressList.size());
				System.err.println("Not equal!");
				return;
			}

		}
		System.out.println("Same");
	}
}
