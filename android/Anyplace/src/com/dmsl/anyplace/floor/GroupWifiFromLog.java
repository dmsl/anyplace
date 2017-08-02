/*
* AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
* 
* Author(s): Timotheos Constambeys
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

package com.dmsl.anyplace.floor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

public abstract class GroupWifiFromLog {

	public GroupWifiFromLog() {

	}

	protected abstract void process(String maxMac, ArrayList<String> values);

	protected abstract String getFloor();

	private void calculateMaxWifi(ArrayList<String> values) {
		if (values.size() > 0) {
			String maxMac = "";
			int maxRss = Integer.MIN_VALUE;

			for (int i = 0; i < values.size(); i++) {
				String value = values.get(i);
				String[] segs = value.split(" ");
				try {
					String mac = segs[4];
					int rss = Integer.parseInt(segs[5]);

					if (rss > maxRss) {
						maxRss = rss;
						maxMac = mac;
					}
				} catch (NumberFormatException ex) {
					if (values.remove(value))
						i--;

				}
			}

			process(maxMac, values);

		}
	}

	private void readFloorAlgoFromFile(InputStream in) throws IOException {
		String line;
		BufferedReader bf = null;
		try {
			bf = new BufferedReader(new InputStreamReader(in));

			ArrayList<String> values = new ArrayList<String>(10);

			while ((line = bf.readLine()) != null) {
				if (line.startsWith("# Timestamp")) {
					calculateMaxWifi(values);
					values.clear();
				} else {
					values.add(line);
				}
			}

			calculateMaxWifi(values);

		} finally {
			try {
				if (bf != null)
					bf.close();
			} catch (IOException e) {

			}
		}
	}

	public void run(InputStream in) throws IOException {
		readFloorAlgoFromFile(in);
	}
}
