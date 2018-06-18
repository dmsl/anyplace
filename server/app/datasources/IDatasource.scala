/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Constantinos Costa, Kyriakos Georgiou, Lambros Petrou
 *
 * Supervisor: Demetrios Zeinalipour-Yazti
 *
 * URL: https://anyplace.cs.ucy.ac.cy
 * Contact: anyplace@cs.ucy.ac.cy
 *
 * Copyright (c) 2016, Data Management Systems Lab (DMSL), University of Cyprus.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 */
package datasources

import floor_module.IAlgo
import utils.GeoPoint
import java.io.FileOutputStream
import java.util.HashMap
import java.util.List

import com.couchbase.client.java.document.json.{JsonObject}


trait IDatasource {
  def getAllPoisTypesByOwner(owner_id: String): List[JsonObject]

  def poisByBuildingIDAsJson(buid: String): List[JsonObject]

  def poisByBuildingAsJson2(cuid: String, letters: String): List[JsonObject]

  def poisByBuildingAsJson2GR(cuid: String, letters: String): List[JsonObject]

  def poisByBuildingAsJson3(buid: String, letters: String): List[JsonObject]


  def init(): Boolean

  def addJsonDocument(key: String, expiry: Int, document: String): Boolean

  def replaceJsonDocument(key: String, expiry: Int, document: String): Boolean

  def deleteFromKey(key: String): Boolean

  def getFromKey(key: String): AnyRef

  def getFromKeyAsJson(key: String): JsonObject

  def buildingFromKeyAsJson(key: String): JsonObject

  def poiFromKeyAsJson(key: String): JsonObject

  def poisByBuildingFloorAsJson(buid: String, floor_number: String): List[JsonObject]

  def poisByBuildingFloorAsMap(buid: String, floor_number: String): List[HashMap[String, String]]

  def poisByBuildingAsJson(buid: String): List[JsonObject]

  def poisByBuildingAsMap(buid: String): List[HashMap[String, String]]

  def floorsByBuildingAsJson(buid: String): List[JsonObject]

  def connectionsByBuildingAsJson(buid: String): List[JsonObject]

  def connectionsByBuildingAsMap(buid: String): List[HashMap[String, String]]

  def connectionsByBuildingFloorAsJson(buid: String, floor_number: String): List[JsonObject]

  def connectionsByBuildingAllFloorsAsJson(buid: String): List[JsonObject]

  def deleteAllByBuilding(buid: String): List[String]

  def deleteAllByFloor(buid: String, floor_number: String): List[String]

  def deleteAllByConnection(cuid: String): List[String]

  def deleteAllByPoi(puid: String): List[String]

  def getRadioHeatmap(): List[JsonObject]

  def getRadioHeatmapByBuildingFloor(buid: String, floor: String): List[JsonObject]

  def getRadioHeatmapByBuildingFloorAverage(buid: String, floor: String): List[JsonObject]

  def getRadioHeatmapByBuildingFloorAverage1(buid: String, floor: String): List[JsonObject]

  def getRadioHeatmapByBuildingFloorAverage2(buid: String, floor: String): List[JsonObject]

  def getRadioHeatmapByBuildingFloorAverage3(buid: String, floor: String): List[JsonObject]

  def getRadioHeatmapByBuildingFloorTimestamp(buid: String, floor: String, timestampX: String, timestampY: String): List[JsonObject]

  def getRadioHeatmapByBuildingFloorTimestampAverage1(buid: String, floor: String, timestampX: String, timestampY: String): List[JsonObject]

  def getRadioHeatmapByBuildingFloorTimestampAverage2(buid: String, floor: String, timestampX: String, timestampY: String): List[JsonObject]

  def getAPsByBuildingFloor(buid: String, floor: String): List[JsonObject]

  def deleteAllByXsYs(buid: String,floor: String,x: String,y: String): List[String]

  def getFingerPrintsBBox(buid: String, floor: String,lat1: String, lon1: String, lat2: String, lon2: String): List[JsonObject]

  def getFingerPrintsTimestampBBox(buid: String, floor: String, lat1: String, lon1: String, lat2: String, lon2: String, timestampX: String, timestampY: String): List[JsonObject]

  def getFingerPrintsTime(buid: String, floor: String): List[JsonObject]

  def getRadioHeatmapByBuildingFloor2(lat: String, lon: String, buid: String, floor: String, range: Int): List[JsonObject]

  def getRadioHeatmapBBox(lat: String, lon: String, buid: String, floor: String, range: Int): List[JsonObject]

  def getRadioHeatmapBBox2(lat: String, lon: String, buid: String, floor: String, range: Int): List[JsonObject]

  def getAllBuildings(): List[JsonObject]

  def getAllBuildingsByOwner(oid: String): List[JsonObject]

  def getAllBuildingsByBucode(bucode: String): List[JsonObject]

  def getBuildingByAlias(alias: String): JsonObject

  def getAllBuildingsNearMe(oid: String,lat: Double, lng: Double): List[JsonObject]

  def dumpRssLogEntriesSpatial(outFile: FileOutputStream, bbox: Array[GeoPoint], floor_number: String): Long

  def dumpRssLogEntriesByBuildingFloor(outFile: FileOutputStream, buid: String, floor_number: String): Long

  def dumpRssLogEntriesByBuildingACCESFloor(outFile: FileOutputStream, buid: String, floor_number: String): Long

  def getAllAccounts(): List[JsonObject]

  def predictFloor(algo: IAlgo, bbox: Array[GeoPoint], strongestMACs: Array[String]): Boolean

  def deleteRadiosInBox(): Boolean

  def magneticPathsByBuildingFloorAsJson(buid: String, floor_number: String): List[JsonObject]

  def magneticPathsByBuildingAsJson(buid: String): List[JsonObject]

  def magneticMilestonesByBuildingFloorAsJson(buid: String, floor_number: String): List[JsonObject]

  def BuildingSetsCuids(cuid: String): Boolean

  def getBuildingSet(cuid: String): List[JsonObject]

  def getAllBuildingsetsByOwner(owner_id: String) : List[JsonObject]

  def deleteNotValidDocuments(): Boolean

  }
