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
package com.dmsl.anyplace.buildings;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import javax.annotation.Generated;

@Generated("org.jsonschema2pojo")
public class Building {

	@SerializedName("coordinates_lat")
	@Expose
	private String coordinatesLat;
	@SerializedName("address")
	@Expose
	private String address;
	@SerializedName("description")
	@Expose
	private String description;
	@SerializedName("name")
	@Expose
	private String name;
	@SerializedName("buid")
	@Expose
	private String buid;
	@SerializedName("coordinates_lon")
	@Expose
	private String coordinatesLon;
	@SerializedName("is_published")
	@Expose
	private String isPublished;
	@SerializedName("url")
	@Expose
	private String url;
	@SerializedName("bucode")
	@Expose
	private String bucode;

	/**
	 * 
	 * @return The coordinatesLat
	 */
	public String getCoordinatesLat() {
		return coordinatesLat;
	}

	/**
	 * 
	 * @param coordinatesLat
	 *            The coordinates_lat
	 */
	public void setCoordinatesLat(String coordinatesLat) {
		this.coordinatesLat = coordinatesLat;
	}

	/**
	 * 
	 * @return The address
	 */
	public String getAddress() {
		return address;
	}

	/**
	 * 
	 * @param address
	 *            The address
	 */
	public void setAddress(String address) {
		this.address = address;
	}

	/**
	 * 
	 * @return The description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * 
	 * @param description
	 *            The description
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * 
	 * @return The name
	 */
	public String getName() {
		return name;
	}

	/**
	 * 
	 * @param name
	 *            The name
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * 
	 * @return The buid
	 */
	public String getBuid() {
		return buid;
	}

	/**
	 * 
	 * @param buid
	 *            The buid
	 */
	public void setBuid(String buid) {
		this.buid = buid;
	}

	/**
	 * 
	 * @return The coordinatesLon
	 */
	public String getCoordinatesLon() {
		return coordinatesLon;
	}

	/**
	 * 
	 * @param coordinatesLon
	 *            The coordinates_lon
	 */
	public void setCoordinatesLon(String coordinatesLon) {
		this.coordinatesLon = coordinatesLon;
	}

	/**
	 * 
	 * @return The isPublished
	 */
	public String getIsPublished() {
		return isPublished;
	}

	/**
	 * 
	 * @param isPublished
	 *            The is_published
	 */
	public void setIsPublished(String isPublished) {
		this.isPublished = isPublished;
	}

	/**
	 * 
	 * @return The url
	 */
	public String getUrl() {
		return url;
	}

	/**
	 * 
	 * @param url
	 *            The url
	 */
	public void setUrl(String url) {
		this.url = url;
	}

	/**
	 * 
	 * @return The bucode
	 */
	public String getBucode() {
		return bucode;
	}

	/**
	 * 
	 * @param bucode
	 *            The bucode
	 */
	public void setBucode(String bucode) {
		this.bucode = bucode;
	}

}