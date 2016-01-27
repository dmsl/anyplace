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

public abstract class GroupWifiFromRadiomap {

	protected String floor;
	protected String NaN;
	
	public GroupWifiFromRadiomap() {

	}

	protected abstract void process(String maxMac, String[] macs, String line);

	protected abstract String getFloor();

	public void readFloorAlgoFromFile(InputStream in) throws IOException {
		BufferedReader bf = null;
		try {
			bf = new BufferedReader(new InputStreamReader(in));
			String line;

			line = bf.readLine();
			NaN = line.split(" ")[2];

			line = bf.readLine();
			String[] macs = line.split(", ");

			while ((line = bf.readLine()) != null) {

				String[] segs = line.split(", ");

				String maxMac = "";
				int maxRss = Integer.MIN_VALUE;

				for (int i = 3; i < segs.length; i++) {

					if (!segs[i].equals(NaN)) {

						int rss = Integer.parseInt(segs[i].split("\\.")[0]);

						if (rss > maxRss) {
							maxRss = rss;
							maxMac = macs[i];
						}
					}
				}
				
				process(maxMac, macs, line);
			}
		} finally {
			try {
				if (bf != null)
					bf.close();
			} catch (IOException e) {
			}
		}
	}

	protected void run(InputStream in,String floor) throws IOException {
		this.floor = floor;
		readFloorAlgoFromFile(in);
	}
}