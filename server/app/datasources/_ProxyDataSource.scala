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

import java.io.FileOutputStream
import java.util
import java.util.{HashMap, List}

import com.couchbase.client.java.document.json.JsonObject
import floor_module.IAlgo
import play.libs.Json
import utils.GeoPoint

object ProxyDataSource {

  private var sInstance: ProxyDataSource = _

  def getInstance(): ProxyDataSource = {
    if (sInstance == null) {
      sInstance = new ProxyDataSource()
    }
    sInstance
  }

  def getIDatasource(): IDatasource = getInstance()
}

class ProxyDataSource private() extends IDatasource {

  private var mCouchbase: CouchbaseDatasource = _
  private var mongoDB: MongodbDatasource = _

  private var mActiveDatabase: IDatasource = _

  initCouchbase()
  setActiveDatabase(this.mCouchbase)

  initMongodb()
  //setActiveDatabase(this.mongoDB) // TODO: nneof once all done

  private def initCouchbase() {
    this.mCouchbase = CouchbaseDatasource.getStaticInstance
  }

  private def initMongodb() {
    this.mongoDB = MongodbDatasource.getStaticInstance
  }

  private def setActiveDatabase(ds: IDatasource) {
    this.mActiveDatabase = ds
  }

  override def init(): Boolean = true

  override def addJsonDocument(key: String, expiry: Int, document: String): Boolean = {
    _checkActiveDatasource()
    mActiveDatabase.addJsonDocument(key, expiry, document)
  }

  override def replaceJsonDocument(key: String, expiry: Int, document: String): Boolean = {
    _checkActiveDatasource()
    mActiveDatabase.replaceJsonDocument(key, expiry, document)
  }

  override def deleteFromKey(key: String): Boolean = {
    _checkActiveDatasource()
    mActiveDatabase.deleteFromKey(key)
  }

  override def getFromKey(key: String): AnyRef = {
    _checkActiveDatasource()
    mActiveDatabase.getFromKey(key)
  }

  override def deleteRadiosInBox(): Boolean = {
    _checkActiveDatasource()
    mActiveDatabase.deleteRadiosInBox()
  }

  override def getFromKeyAsJson(key: String): JsonObject = {
    _checkActiveDatasource()
    mActiveDatabase.getFromKeyAsJson(key)
  }

  override def buildingFromKeyAsJson(key: String): JsonObject = {
    _checkActiveDatasource()
    mActiveDatabase.buildingFromKeyAsJson(key)
  }

  override def poiFromKeyAsJson(key: String): JsonObject = getFromKeyAsJson(key)

  override def poisByBuildingFloorAsJson(buid: String, floor_number: String): List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.poisByBuildingFloorAsJson(buid, floor_number)
  }

  override def poisByBuildingFloorAsMap(buid: String, floor_number: String): List[HashMap[String, String]] = {
    _checkActiveDatasource()
    mActiveDatabase.poisByBuildingFloorAsMap(buid, floor_number)
  }

  override def poisByBuildingAsJson(buid: String): List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.poisByBuildingAsJson(buid)
  }

  override def poisByBuildingAsMap(buid: String): List[HashMap[String, String]] = {
    _checkActiveDatasource()
    mActiveDatabase.poisByBuildingAsMap(buid)
  }

  override def floorsByBuildingAsJson(buid: String): List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.floorsByBuildingAsJson(buid)
  }

  override def connectionsByBuildingAsJson(buid: String): List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.connectionsByBuildingAsJson(buid)
  }

  override def connectionsByBuildingAsMap(buid: String): List[HashMap[String, String]] = {
    _checkActiveDatasource()
    mActiveDatabase.connectionsByBuildingAsMap(buid)
  }

  override def connectionsByBuildingFloorAsJson(buid: String, floor_number: String): List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.connectionsByBuildingFloorAsJson(buid, floor_number)
  }

  override def deleteAllByBuilding(buid: String): List[String] = {
    _checkActiveDatasource()
    mActiveDatabase.deleteAllByBuilding(buid)
  }

  override def deleteAllByFloor(buid: String, floor_number: String): List[String] = {
    _checkActiveDatasource()
    mActiveDatabase.deleteAllByFloor(buid, floor_number)
  }

  override def deleteAllByConnection(cuid: String): List[String] = {
    _checkActiveDatasource()
    mActiveDatabase.deleteAllByConnection(cuid)
  }

  override def deleteAllByPoi(puid: String): List[String] = {
    _checkActiveDatasource()
    mActiveDatabase.deleteAllByPoi(puid)
  }

  override def getRadioHeatmap(): List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getRadioHeatmap
  }

  override def getRadioHeatmapByBuildingFloor(buid: String, floor: String): List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getRadioHeatmapByBuildingFloor(buid, floor)
  }

  override def getRadioHeatmapByBuildingFloorAverage(buid: String, floor: String): List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getRadioHeatmapByBuildingFloorAverage(buid, floor)
  }

   override def getRadioHeatmapByBuildingFloorAverage1(buid: String, floor: String): List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getRadioHeatmapByBuildingFloorAverage1(buid, floor)
  }

   override def getRadioHeatmapByBuildingFloorAverage2(buid: String, floor: String): List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getRadioHeatmapByBuildingFloorAverage2(buid, floor)
  }

   override def getRadioHeatmapByBuildingFloorAverage3(buid: String, floor: String): List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getRadioHeatmapByBuildingFloorAverage3(buid, floor)
  }

  override def getRadioHeatmapByBuildingFloorTimestamp(buid: String, floor: String, timestampX: String, timestampY: String): List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getRadioHeatmapByBuildingFloorTimestamp(buid, floor, timestampX, timestampY)
  }

  override def getRadioHeatmapByBuildingFloorTimestampAverage1(buid: String, floor: String, timestampX: String, timestampY: String): List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getRadioHeatmapByBuildingFloorTimestampAverage1(buid, floor, timestampX, timestampY)
  }

  override def getRadioHeatmapByBuildingFloorTimestampAverage2(buid: String, floor: String, timestampX: String, timestampY: String): List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getRadioHeatmapByBuildingFloorTimestampAverage2(buid, floor, timestampX, timestampY)
  }

  override def getAPsByBuildingFloor(buid: String, floor: String): List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getAPsByBuildingFloor(buid, floor)
  }


  override def deleteAllByXsYs(buid: String,floor: String,x: String,y: String): List[String] = {
    _checkActiveDatasource()
    mActiveDatabase.deleteAllByXsYs(buid,floor,x,y)
  }

    override def getFingerPrintsBBox(buid: String, floor: String,lat1: String, lon1: String, lat2: String, lon2: String): util.List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getFingerPrintsBBox(buid,floor,lat1,lon1,lat2,lon2)
  }
  override def getFingerPrintsTimestampBBox(buid: String, floor: String, lat1: String, lon1: String, lat2: String, lon2: String, timestampX: String, timestampY: String): util.List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getFingerPrintsTimestampBBox(buid: String, floor: String, lat1: String, lon1: String, lat2: String, lon2: String, timestampX: String, timestampY: String)
  }

  override def getFingerPrintsTime(buid: String, floor: String): util.List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getFingerPrintsTime(buid,floor)
  }


  override def getAllBuildings(): List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getAllBuildings
  }

  override def getAllBuildingsByOwner(oid: String): List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getAllBuildingsByOwner(oid)
  }

  override def getAllBuildingsByBucode(bucode: String): List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getAllBuildingsByBucode(bucode)
  }

  override def getBuildingByAlias(alias: String): JsonObject = {
    _checkActiveDatasource()
    mActiveDatabase.getBuildingByAlias(alias)
  }

  override def getAllBuildingsNearMe(ownerid:String,lat: Double, lng: Double): List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getAllBuildingsNearMe(ownerid,lat, lng)
  }

  override def dumpRssLogEntriesSpatial(outFile: FileOutputStream, bbox: Array[GeoPoint], floor_number: String): Long = {
    _checkActiveDatasource()
    mActiveDatabase.dumpRssLogEntriesSpatial(outFile, bbox, floor_number)
  }

  override def dumpRssLogEntriesByBuildingACCESFloor(outFile: FileOutputStream, buid: String, floor_number: String): Long = {
    _checkActiveDatasource()
    mActiveDatabase.dumpRssLogEntriesByBuildingACCESFloor(outFile, buid, floor_number)
  }

  override def dumpRssLogEntriesByBuildingFloor(outFile: FileOutputStream, buid: String, floor_number: String): Long = {
    _checkActiveDatasource()
    mActiveDatabase.dumpRssLogEntriesByBuildingFloor(outFile, buid, floor_number)
  }

  override def getAllAccounts(): List[Json] = {
    _checkActiveDatasource()
    //mActiveDatabase.getAllAccounts
    mongoDB.getAllAccounts()
  }

  def _checkActiveDatasource() {
    if (this.mActiveDatabase == null) {
      throw new DatasourceException("No active Datasource exists!")
    }
  }

  override def predictFloor(algo: IAlgo, bbox: Array[GeoPoint], strongestMACs: Array[String]): Boolean = {
    _checkActiveDatasource()
    mActiveDatabase.predictFloor(algo, bbox, strongestMACs)
  }

  override def magneticPathsByBuildingFloorAsJson(buid: String, floor_number: String): List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.magneticPathsByBuildingFloorAsJson(buid, floor_number)
  }

  override def magneticPathsByBuildingAsJson(buid: String): List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.magneticPathsByBuildingAsJson(buid)
  }

  override def magneticMilestonesByBuildingFloorAsJson(buid: String, floor_number: String): List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.magneticMilestonesByBuildingFloorAsJson(buid, floor_number)
  }

  override def getAllPoisTypesByOwner(owner_id: String): util.List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getAllPoisTypesByOwner(owner_id)
  }

  override def poisByBuildingIDAsJson(buid: String): util.List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.poisByBuildingIDAsJson(buid)
  }

  override def poisByBuildingAsJson2(cuid: String, letters: String): util.List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.poisByBuildingAsJson2(cuid, letters)
  }

  override def poisByBuildingAsJson2GR(cuid: String, letters: String): util.List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.poisByBuildingAsJson2GR(cuid, letters)
  }

  override def poisByBuildingAsJson3(buid: String, letters: String): util.List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.poisByBuildingAsJson3(buid,letters)
  }

  override def connectionsByBuildingAllFloorsAsJson(buid: String): util.List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.connectionsByBuildingAllFloorsAsJson(buid)
  }

  override def getRadioHeatmapByBuildingFloor2(lat: String, lon: String, buid: String, floor: String, range: Int): util.List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getRadioHeatmapByBuildingFloor2(lat,lon,buid,floor,range)
  }

  override def getRadioHeatmapBBox(lat: String, lon: String, buid: String, floor: String, range: Int): util.List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getRadioHeatmapBBox(lat,lon,buid,floor,range)
  }

  override def getRadioHeatmapBBox2(lat: String, lon: String, buid: String, floor: String, range: Int): util.List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getRadioHeatmapBBox2(lat,lon,buid,floor,range)
  }

  override def BuildingSetsCuids(cuid: String): Boolean = {
    _checkActiveDatasource()
    mActiveDatabase.BuildingSetsCuids(cuid)
  }

  override def getBuildingSet(cuid: String): util.List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getBuildingSet(cuid)
  }

  override def getAllBuildingsetsByOwner(owner_id: String): util.List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getAllBuildingsetsByOwner(owner_id)
  }

  override def deleteNotValidDocuments(): Boolean ={
    _checkActiveDatasource()
    mActiveDatabase.deleteNotValidDocuments()
  }

}
