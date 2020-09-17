/*
* Anyplace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Authors: C. Laoudias, G.Larkou, G. Constantinou, M. Constantinides, S. Nicolaou
* 
* Supervisor: Demetrios Zeinalipour-Yazti
*
* URL: http://anyplace.cs.ucy.ac.cy
* Contact: anyplace@cs.ucy.ac.cy
*
* Copyright (c) 2015, Data Management Systems Lab (DMSL), University of Cyprus.
* All rights reserved.
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of
* this software and associated documentation files (the "Software"), to deal in the
* Software without restriction, including without limitation the rights to use, copy,
* modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
* and to permit persons to whom the Software is furnished to do so, subject to the
* following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
* DEALINGS IN THE SOFTWARE.
*
*/

package cy.ac.ucy.cs.anyplace.lib;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class RadioMap {
	private String NaN = "-110";
	private File RadiomapMean_File = null;
	private ArrayList<String> MacAdressList = null;
	private HashMap<String, ArrayList<String>> LocationRSS_HashMap = null;
	private ArrayList<String> OrderList = null;

	public RadioMap(File inFile) throws Exception {
		MacAdressList = new ArrayList<String>();
		LocationRSS_HashMap = new HashMap<String, ArrayList<String>>();
		OrderList = new ArrayList<String>();

		if (!ConstructRadioMap(inFile)) {
			throw new Exception("Inavlid Radiomap File");
		}
	}

	/**
	 * Getter of MAC Address list in file order
	 * 
	 * @return the list of MAC Addresses
	 */
	public ArrayList<String> getMacAdressList() {
		return MacAdressList;
	}

	/**
	 * Getter of HashMap Location-RSS Values list in no particular order
	 * 
	 * @return the HashMap Location-RSS Values
	 */
	public HashMap<String, ArrayList<String>> getLocationRSS_HashMap() {
		return LocationRSS_HashMap;
	}

	/**
	 * Getter of Location list in file order
	 * 
	 * @return the Location list
	 */
	public ArrayList<String> getOrderList() {
		return OrderList;
	}

	/**
	 * Getter of radio map mean filename
	 * 
	 * @return the filename of radiomap mean used
	 */
	public File getRadiomapMean_File() {
		return this.RadiomapMean_File;
	}

	public String getNaN() {
		return NaN;
	}

	/**
	 * Construct a radio map
	 * 
	 * @param inFile the radio map file to read
	 * 
	 * @return true if radio map constructed successfully, otherwise false
	 */
	private boolean ConstructRadioMap(File inFile) {

		if (!inFile.exists() || !inFile.canRead()) {
			return false;
		}

		this.RadiomapMean_File = inFile;
		this.OrderList.clear();
		this.MacAdressList.clear();
		this.LocationRSS_HashMap.clear();

		ArrayList<String> RSS_Values = null;
		BufferedReader reader = null;
		String line = null;
		String[] temp = null;
		String key = null;

		try {

			reader = new BufferedReader(new FileReader(inFile));

			// Read the first line # NaN -110
			line = reader.readLine();
			temp = line.split(" ");
			if (!temp[1].equals("NaN"))
				return false;
			NaN = temp[2];
			line = reader.readLine();

			// Must exists
			if (line == null)
				return false;

			line = line.replace(", ", " ");
			temp = line.split(" ");

			final int startOfRSS = 4;

			// Must have more than 4 fields
			if (temp.length < startOfRSS)
				return false;

			// Store all Mac Addresses Heading Added
			for (int i = startOfRSS; i < temp.length; ++i)
				this.MacAdressList.add(temp[i]);

			while ((line = reader.readLine()) != null) {

				if (line.trim().equals(""))
					continue;

				line = line.replace(", ", " ");
				temp = line.split(" ");

				if (temp.length < startOfRSS)
					return false;

				key = temp[0] + " " + temp[1];

				RSS_Values = new ArrayList<String>();

				for (int i = startOfRSS - 1; i < temp.length; ++i)
					RSS_Values.add(temp[i]);

				// Equal number of MAC address and RSS Values
				if (this.MacAdressList.size() != RSS_Values.size())
					return false;

				this.LocationRSS_HashMap.put(key, RSS_Values);

				this.OrderList.add(key);
			}

		} catch (Exception ex) {
			return false;
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {

				}
		}
		return true;
	}

	public String toString() {
		String str = "MAC Adresses: ";
		ArrayList<String> temp;
		for (int i = 0; i < MacAdressList.size(); ++i)
			str += MacAdressList.get(i) + " ";

		str += "\nLocations\n";
		for (String location : LocationRSS_HashMap.keySet()) {
			str += location + " ";
			temp = LocationRSS_HashMap.get(location);
			for (int i = 0; i < temp.size(); ++i)
				str += temp.get(i) + " ";
			str += "\n";
		}

		return str;
	}
}
