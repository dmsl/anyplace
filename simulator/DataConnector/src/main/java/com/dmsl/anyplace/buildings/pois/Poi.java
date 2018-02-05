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

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class Poi {

	@SerializedName("is_building_entrance")
	@Expose
	private String isBuildingEntrance;
	@SerializedName("floor_number")
	@Expose
	private String floorNumber;
	@SerializedName("pois_type")
	@Expose
	private String poisType;
	@SerializedName("buid")
	@Expose
	private String buid;
	@SerializedName("image")
	@Expose
	private String image;
	@SerializedName("coordinates_lon")
	@Expose
	private String coordinatesLon;
	@SerializedName("url")
	@Expose
	private String url;
	@SerializedName("coordinates_lat")
	@Expose
	private String coordinatesLat;
	@SerializedName("floor_name")
	@Expose
	private String floorName;
	@SerializedName("description")
	@Expose
	private String description;
	@SerializedName("name")
	@Expose
	private String name;
	@SerializedName("is_door")
	@Expose
	private String isDoor;
	@SerializedName("is_published")
	@Expose
	private String isPublished;
	@SerializedName("puid")
	@Expose
	private String puid;

	/**
	 * 
	 * @return The isBuildingEntrance
	 */
	public String getIsBuildingEntrance() {
		return isBuildingEntrance;
	}

	/**
	 * 
	 * @param isBuildingEntrance
	 *            The is_building_entrance
	 */
	public void setIsBuildingEntrance(String isBuildingEntrance) {
		this.isBuildingEntrance = isBuildingEntrance;
	}

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
	 * @return The poisType
	 */
	public String getPoisType() {
		return poisType;
	}

	/**
	 * 
	 * @param poisType
	 *            The pois_type
	 */
	public void setPoisType(String poisType) {
		this.poisType = poisType;
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
	 * @return The image
	 */
	public String getImage() {
		return image;
	}

	/**
	 * 
	 * @param image
	 *            The image
	 */
	public void setImage(String image) {
		this.image = image;
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
	 * @return The isDoor
	 */
	public String getIsDoor() {
		return isDoor;
	}

	/**
	 * 
	 * @param isDoor
	 *            The is_door
	 */
	public void setIsDoor(String isDoor) {
		this.isDoor = isDoor;
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
	 * @return The puid
	 */
	public String getPuid() {
		return puid;
	}

	/**
	 * 
	 * @param puid
	 *            The puid
	 */
	public void setPuid(String puid) {
		this.puid = puid;
	}

}