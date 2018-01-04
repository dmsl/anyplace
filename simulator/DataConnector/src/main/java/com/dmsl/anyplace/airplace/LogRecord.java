/**
 * Anyplace Simulator:  A trace-driven evaluation and visualization of IoT Data Prefetching in Indoor Navigation SOAs
 *
 * Author(s): Zacharias Georgiou, Panagiotis Irakleous
 *
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
package com.dmsl.anyplace.airplace;

public class LogRecord {

	private String bssid;
	private int rss;

	public LogRecord(String bssid, int rss) {
		super();
		this.bssid = bssid;
		this.rss = rss;
	}

	public String getBssid() {
		return bssid;
	}

	public int getRss() {
		return rss;
	}
	
	public String toString() {
		String str = String.valueOf(bssid) + " " + String.valueOf(rss) + "\n";
		return str;
	}

}
