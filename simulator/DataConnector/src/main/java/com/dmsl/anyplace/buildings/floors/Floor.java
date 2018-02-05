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
package com.dmsl.anyplace.buildings.floors;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class Floor {

	@SerializedName("floor_number")
	@Expose
	private String floorNumber;
	@SerializedName("floor_name")
	@Expose
	private String floorName;
	@SerializedName("description")
	@Expose
	private String description;
	@SerializedName("buid")
	@Expose
	private String buid;
	@SerializedName("fuid")
	@Expose
	private String fuid;
	@SerializedName("is_published")
	@Expose
	private String isPublished;
	@SerializedName("bottom_left_lat")
	@Expose
	private String bottomLeftLat;
	@SerializedName("bottom_left_lng")
	@Expose
	private String bottomLeftLng;
	@SerializedName("top_right_lat")
	@Expose
	private String topRightLat;
	@SerializedName("top_right_lng")
	@Expose
	private String topRightLng;

	/**
	 * 
	 * @return The floorNumber
	 */
	public String getFloorNumber() {
		return floorNumber;
	}

	/**
	 * 
	 * @param floorNumber
	 *            The floor_number
	 */
	public void setFloorNumber(String floorNumber) {
		this.floorNumber = floorNumber;
	}

	/**
	 * 
	 * @return The floorName
	 */
	public String getFloorName() {
		return floorName;
	}

	/**
	 * 
	 * @param floorName
	 *            The floor_name
	 */
	public void setFloorName(String floorName) {
		this.floorName = floorName;
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
	 * @return The fuid
	 */
	public String getFuid() {
		return fuid;
	}

	/**
	 * 
	 * @param fuid
	 *            The fuid
	 */
	public void setFuid(String fuid) {
		this.fuid = fuid;
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
	 * @return The bottomLeftLat
	 */
	public String getBottomLeftLat() {
		return bottomLeftLat;
	}

	/**
	 * 
	 * @param bottomLeftLat
	 *            The bottom_left_lat
	 */
	public void setBottomLeftLat(String bottomLeftLat) {
		this.bottomLeftLat = bottomLeftLat;
	}

	/**
	 * 
	 * @return The bottomLeftLng
	 */
	public String getBottomLeftLng() {
		return bottomLeftLng;
	}

	/**
	 * 
	 * @param bottomLeftLng
	 *            The bottom_left_lng
	 */
	public void setBottomLeftLng(String bottomLeftLng) {
		this.bottomLeftLng = bottomLeftLng;
	}

	/**
	 * 
	 * @return The topRightLat
	 */
	public String getTopRightLat() {
		return topRightLat;
	}

	/**
	 * 
	 * @param topRightLat
	 *            The top_right_lat
	 */
	public void setTopRightLat(String topRightLat) {
		this.topRightLat = topRightLat;
	}

	/**
	 * 
	 * @return The topRightLng
	 */
	public String getTopRightLng() {
		return topRightLng;
	}

	/**
	 * 
	 * @param topRightLng
	 *            The top_right_lng
	 */
	public void setTopRightLng(String topRightLng) {
		this.topRightLng = topRightLng;
	}

}
