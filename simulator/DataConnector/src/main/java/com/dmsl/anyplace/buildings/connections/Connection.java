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
package com.dmsl.anyplace.buildings.connections;

import javax.annotation.Generated;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

@Generated("org.jsonschema2pojo")
public class Connection {

	@SerializedName("cuid")
	@Expose
	private String cuid;
	@SerializedName("edge_type")
	@Expose
	private String edgeType;
	@SerializedName("weight")
	@Expose
	private String weight;
	@SerializedName("buid")
	@Expose
	private String buid;
	@SerializedName("pois_b")
	@Expose
	private String poisB;
	@SerializedName("floor_a")
	@Expose
	private String floorA;
	@SerializedName("floor_b")
	@Expose
	private String floorB;
	@SerializedName("pois_a")
	@Expose
	private String poisA;
	@SerializedName("is_published")
	@Expose
	private String isPublished;
	@SerializedName("buid_b")
	@Expose
	private String buidB;
	@SerializedName("buid_a")
	@Expose
	private String buidA;

	/**
	 * 
	 * @return The cuid
	 */
	public String getCuid() {
		return cuid;
	}

	/**
	 * 
	 * @param cuid
	 *            The cuid
	 */
	public void setCuid(String cuid) {
		this.cuid = cuid;
	}

	/**
	 * 
	 * @return The edgeType
	 */
	public String getEdgeType() {
		return edgeType;
	}

	/**
	 * 
	 * @param edgeType
	 *            The edge_type
	 */
	public void setEdgeType(String edgeType) {
		this.edgeType = edgeType;
	}

	/**
	 * 
	 * @return The weight
	 */
	public String getWeight() {
		return weight;
	}

	/**
	 * 
	 * @param weight
	 *            The weight
	 */
	public void setWeight(String weight) {
		this.weight = weight;
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
	 * @return The poisB
	 */
	public String getPoisB() {
		return poisB;
	}

	/**
	 * 
	 * @param poisB
	 *            The pois_b
	 */
	public void setPoisB(String poisB) {
		this.poisB = poisB;
	}

	/**
	 * 
	 * @return The floorA
	 */
	public String getFloorA() {
		return floorA;
	}

	/**
	 * 
	 * @param floorA
	 *            The floor_a
	 */
	public void setFloorA(String floorA) {
		this.floorA = floorA;
	}

	/**
	 * 
	 * @return The floorB
	 */
	public String getFloorB() {
		return floorB;
	}

	/**
	 * 
	 * @param floorB
	 *            The floor_b
	 */
	public void setFloorB(String floorB) {
		this.floorB = floorB;
	}

	/**
	 * 
	 * @return The poisA
	 */
	public String getPoisA() {
		return poisA;
	}

	/**
	 * 
	 * @param poisA
	 *            The pois_a
	 */
	public void setPoisA(String poisA) {
		this.poisA = poisA;
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
	 * @return The buidB
	 */
	public String getBuidB() {
		return buidB;
	}

	/**
	 * 
	 * @param buidB
	 *            The buid_b
	 */
	public void setBuidB(String buidB) {
		this.buidB = buidB;
	}

	/**
	 * 
	 * @return The buidA
	 */
	public String getBuidA() {
		return buidA;
	}

	/**
	 * 
	 * @param buidA
	 *            The buid_a
	 */
	public void setBuidA(String buidA) {
		this.buidA = buidA;
	}

}
