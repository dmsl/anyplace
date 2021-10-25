/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Nikolas Neofytou, Constantinos Costa, Kyriakos Georgiou, Lambros Petrou
 *
 * Co-Supervisor: Paschalis Mpeis
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
import java.util.HashMap

import modules.floor.IAlgo
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.JsValue
import utils.GeoPoint

@Singleton
class ProxyDataSource @Inject() (conf: Configuration) extends IDatasource {
  private var mongoDB: MongodbDatasource = _
  private var activeDB: IDatasource = _

  initMongodb()
  setActiveDatabase(this.mongoDB)

  private var sInstance: ProxyDataSource = _

  def getInstance(): ProxyDataSource = {
    if (sInstance == null) {
      sInstance = new ProxyDataSource(conf)
    }
    sInstance
  }

  def db: IDatasource = getInstance()

  private def initMongodb(): Unit = {
    MongodbDatasource.initialize(conf)
    this.mongoDB = MongodbDatasource.instance
  }

  private def setActiveDatabase(ds: IDatasource): Unit = { this.activeDB = ds }

  override def addJson(col: String,  json: JsValue): Boolean = {
    checkHasActiveDB()
    activeDB.addJson(col, json)
  }

  override def replaceJsonDocument(col: String, key: String, value: String, document: String): Boolean = {
    checkHasActiveDB()
    activeDB.replaceJsonDocument(col, key, value, document)
  }

  def deleteFromKey(col: String, key: String, value:String): Boolean = {
    checkHasActiveDB()
    activeDB.deleteFromKey(col, key, value)
  }

  def floorHasFingerprints(buid: String, floor: String): Boolean = {
    checkHasActiveDB()
    activeDB.floorHasFingerprints(buid, floor)
  }

  override def getFromKey(collection:String, key: String, value: String):JsValue = {
    checkHasActiveDB()
    activeDB.getFromKey(collection, key, value)
  }

  override def deleteRadiosInBox(): Boolean = {
    checkHasActiveDB()
    activeDB.deleteRadiosInBox()
  }

  override def getFromKeyAsJson(collection: String,key: String, value: String): JsValue = {
    checkHasActiveDB()
    activeDB.getFromKeyAsJson(collection, key, value)
  }

  override def fingerprintExists(collection: String,buid: String, floor: String, x: String, y:String, heading:String): Boolean = {
    checkHasActiveDB()
    activeDB.fingerprintExists(collection, buid, floor, x, y, heading)
  }

  override def buildingFromKeyAsJson(key: String): JsValue = {
    checkHasActiveDB()
    activeDB.buildingFromKeyAsJson(key)
  }

  override def poiFromKeyAsJson(collection: String, key: String, value: String): JsValue = getFromKeyAsJson(collection, key, value)

  override def poisByBuildingFloorAsJson(buid: String, floor_number: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.poisByBuildingFloorAsJson(buid, floor_number)
  }

  override def poisByBuildingFloorAsMap(buid: String, floor_number: String): java.util.List[HashMap[String, String]] = {
    checkHasActiveDB()
    activeDB.poisByBuildingFloorAsMap(buid, floor_number)
  }

  override def poisByBuildingAsJson(buid: String): java.util.List[JsValue] = {
    checkHasActiveDB()
    activeDB.poisByBuildingAsJson(buid)
  }

  override def poiByBuidFloorPuid(buid: String, floor_number: String, puid: String): Boolean = {
    checkHasActiveDB()
    activeDB.poiByBuidFloorPuid(buid, floor_number, puid)
  }

  override def poisByBuildingAsMap(buid: String): java.util.List[HashMap[String, String]] = {
    checkHasActiveDB()
    activeDB.poisByBuildingAsMap(buid)
  }

  override def floorsByBuildingAsJson(buid: String): java.util.List[JsValue] = {
    checkHasActiveDB()
    activeDB.floorsByBuildingAsJson(buid)
  }

  override def connectionsByBuildingAsJson(buid: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.connectionsByBuildingAsJson(buid)
  }

  override def connectionsByBuildingAsMap(buid: String): java.util.List[HashMap[String, String]] = {
    checkHasActiveDB()
    activeDB.connectionsByBuildingAsMap(buid)
  }

  override def connectionsByBuildingFloorAsJson(buid: String, floor_number: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.connectionsByBuildingFloorAsJson(buid, floor_number)
  }

  override def deleteAllByBuilding(buid: String):Boolean = {
    checkHasActiveDB()
    activeDB.deleteAllByBuilding(buid)
  }

  override def deleteAllByFloor(buid: String, floor_number: String): Boolean = {
    checkHasActiveDB()
    activeDB.deleteAllByFloor(buid, floor_number)
  }

  override def deleteAllByConnection(cuid: String): java.util.List[String] = {
    checkHasActiveDB()
    activeDB.deleteAllByConnection(cuid)
  }

  override def deleteAllByPoi(puid: String): java.util.List[String] = {
    checkHasActiveDB()
    activeDB.deleteAllByPoi(puid)
  }

  override def getRadioHeatmapByBuildingFloor(buid: String, floor: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.getRadioHeatmapByBuildingFloor(buid, floor)
  }

  override def getRadioHeatmapByBuildingFloorAverage1(buid: String, floor: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.getRadioHeatmapByBuildingFloorAverage1(buid, floor)
  }

  override def getRadioHeatmapByBuildingFloorAverage2(buid: String, floor: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.getRadioHeatmapByBuildingFloorAverage2(buid, floor)
  }

  override def getRadioHeatmapByBuildingFloorAverage3(buid: String, floor: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.getRadioHeatmapByBuildingFloorAverage3(buid, floor)
  }

  override def getRadioHeatmapByFloorTimestamp(buid: String, floor: String, timestampX: String, timestampY: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.getRadioHeatmapByFloorTimestamp(buid, floor, timestampX, timestampY)
  }

  override def getRadioHeatmapByBuildingFloorTimestampAverage1(buid: String, floor: String, timestampX: String, timestampY: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.getRadioHeatmapByBuildingFloorTimestampAverage1(buid, floor, timestampX, timestampY)
  }

  override def getRadioHeatmapByBuildingFloorTimestampAverage2(buid: String, floor: String, timestampX: String, timestampY: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.getRadioHeatmapByBuildingFloorTimestampAverage2(buid, floor, timestampX, timestampY)
  }

  override def getAPsByBuildingFloor(buid: String, floor: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.getAPsByBuildingFloor(buid, floor)
  }

  override def getCachedAPsByBuildingFloor(buid: String, floor: String): JsValue = {
    checkHasActiveDB()
    activeDB.getCachedAPsByBuildingFloor(buid, floor)
  }

  override def getFingerPrintsBBox(buid: String, floor: String,lat1: String, lon1: String, lat2: String, lon2: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.getFingerPrintsBBox(buid,floor,lat1,lon1,lat2,lon2)
  }
  override def getFingerPrintsTimestampBBox(buid: String, floor: String, lat1: String, lon1: String, lat2: String, lon2: String, timestampX: String, timestampY: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.getFingerPrintsTimestampBBox(buid: String, floor: String, lat1: String, lon1: String, lat2: String, lon2: String, timestampX: String, timestampY: String)
  }

  override def getFingerprintsByTime(buid: String, floor: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.getFingerprintsByTime(buid,floor)
  }

  override def getAllBuildings(): List[JsValue] = {
    checkHasActiveDB()
    activeDB.getAllBuildings()
  }

  override def getSpaceAccessible(oid: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.getSpaceAccessible(oid)
  }

  override def getAllSpaceOwned(owner_id: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.getAllSpaceOwned(owner_id)
  }

  override def getAllBuildingsByBucode(bucode: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.getAllBuildingsByBucode(bucode)
  }

  override def getAllBuildingsNearMe(lat: Double, lng: Double, range: Int, owner_id: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.getAllBuildingsNearMe(lat, lng, range, owner_id)
  }

  override def dumpRssLogEntriesSpatial(outFile: FileOutputStream, bbox: Array[GeoPoint], floor_number: String): Long = {
    checkHasActiveDB()
    activeDB.dumpRssLogEntriesSpatial(outFile, bbox, floor_number)
  }

  override def dumpRssLogEntriesWithCoordinates (floor_number: String, lat: Double, lon: Double): String = {
    checkHasActiveDB()
    activeDB.dumpRssLogEntriesWithCoordinates(floor_number: String, lat, lon)
  }

  override def dumpRssLogEntriesByBuildingFloor(outFile: FileOutputStream, buid: String, floor_number: String): Long = {
    checkHasActiveDB()
    activeDB.dumpRssLogEntriesByBuildingFloor(outFile, buid, floor_number)
  }

  override def getAllAccounts(): List[JsValue] = {
    checkHasActiveDB()
    activeDB.getAllAccounts()
  }

  def checkHasActiveDB(): Unit = {
    if (this.activeDB == null) { throw new DatasourceException("No active Datasource.") }
  }

  override def predictFloor(algo: IAlgo, bbox: Array[GeoPoint], strongestMACs: Array[String]): Boolean = {
    checkHasActiveDB()
    activeDB.predictFloor(algo, bbox, strongestMACs)
  }

  override def poisByBuildingIDAsJson(buid: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.poisByBuildingIDAsJson(buid)
  }

  override def poisByBuildingAsJson2(cuid: String, letters: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.poisByBuildingAsJson2(cuid, letters)
  }

  override def poisByBuildingAsJson2GR(cuid: String, letters: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.poisByBuildingAsJson2GR(cuid, letters)
  }

  override def poisByBuildingAsJson3(buid: String, letters: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.poisByBuildingAsJson3(buid,letters)
  }

  override def connectionsByBuildingAllFloorsAsJson(buid: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.connectionsByBuildingAllFloorsAsJson(buid)
  }

  override def BuildingSetsCuids(cuid: String): Boolean = {
    checkHasActiveDB()
    activeDB.BuildingSetsCuids(cuid)
  }

  override def getBuildingSet(cuid: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.getBuildingSet(cuid)
  }

  override def getAllBuildingsetsByOwner(owner_id: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.getAllBuildingsetsByOwner(owner_id)
  }

  override def generateHeatmaps(): Boolean = {
    checkHasActiveDB()
    activeDB.generateHeatmaps()
  }

  override def deleteCachedDocuments(buid: String, floor_number: String): Boolean = {
    checkHasActiveDB()
    activeDB.deleteCachedDocuments(buid, floor_number)
  }

  override def deleteFingerprint(fingerprint: JsValue): Boolean = {
    checkHasActiveDB()
    activeDB.deleteFingerprint(fingerprint)
  }

  override def cacheHeatmapByTime(col: String, buid: String, floor: String, level: Int) = {
    checkHasActiveDB()
    activeDB.cacheHeatmapByTime(col, buid, floor, level)
  }

  override def login(collection: String, username: String, password: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.login(collection, username, password)
  }

  /**
   * Given an access_token it returns the user account.
   * Used only for local accounts.
   */
  override def getUserFromAccessToken(accessToken: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.getUserFromAccessToken(accessToken)
  }

  override def getUserFromOwnerId(accessToken: String): List[JsValue] = {
    checkHasActiveDB()
    activeDB.getUserFromOwnerId(accessToken)
  }

  override def register(collection: String, name: String, email: String, username: String, password: String,
                        external: String, accType: String): JsValue = {
    checkHasActiveDB()
    activeDB.register(collection, name, email, username, password, external, accType)
  }

  override def isAdmin(): Boolean = {
    checkHasActiveDB()
    activeDB.isAdmin()
  }

  override def deleteAllByXsYs(buid: String,floor: String,x: String,y: String): java.util.List[String] = {
    checkHasActiveDB()
    activeDB.deleteAllByXsYs(buid,floor,x,y)
  }

}
