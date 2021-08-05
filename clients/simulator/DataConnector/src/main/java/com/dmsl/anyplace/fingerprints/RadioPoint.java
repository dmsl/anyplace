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
package com.dmsl.anyplace.fingerprints;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class RadioPoint {

	@SerializedName("x")
	@Expose
	private String x;
	@SerializedName("y")
	@Expose
	private String y;
	@SerializedName("w")
	@Expose
	private String w;

	/**
	 * 
	 * @return The x
	 */
	public String getX() {
		return x;
	}

	/**
	 * 
	 * @param x
	 *            The x
	 */
	public void setX(String x) {
		this.x = x;
	}

	/**
	 * 
	 * @return The y
	 */
	public String getY() {
		return y;
	}

	/**
	 * 
	 * @param y
	 *            The y
	 */
	public void setY(String y) {
		this.y = y;
	}

	/**
	 * 
	 * @return The w
	 */
	public String getW() {
		return w;
	}

	/**
	 * 
	 * @param w
	 *            The w
	 */
	public void setW(String w) {
		this.w = w;
	}

}
