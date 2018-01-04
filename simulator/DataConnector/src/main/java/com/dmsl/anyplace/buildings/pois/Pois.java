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
package com.dmsl.anyplace.buildings.pois;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Generated;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class Pois {

	@SerializedName("pois")
	@Expose
	private List<Poi> pois = new ArrayList<Poi>();

	/**
	 * 
	 * @return The pois
	 */
	public List<Poi> getPois() {
		return pois;
	}

	/**
	 * 
	 * @param pois
	 *            The pois
	 */
	public void setPois(List<Poi> pois) {
		this.pois = pois;
	}

	public void remove(String string) {
		Iterator<Poi> it = pois.iterator();
		while (it.hasNext()) {
			Poi s = it.next();
			if (s.getPuid().equals(string))
				it.remove();
		}

	}

}