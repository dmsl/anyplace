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

import com.couchbase.client.java.document.json.JsonObject
import floor_module.IAlgo
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.libs.json.JsValue
import utils.GeoPoint

@Singleton
class ProxyDataSource @Inject() (conf: Configuration) extends IDatasource {

  private var mCouchbase: CouchbaseDatasource = _
  private var mongoDB: MongodbDatasource = _

  private var mActiveDatabase: IDatasource = _

  initCouchbase()
  setActiveDatabase(this.mCouchbase)

  initMongodb()
  //setActiveDatabase(this.mongoDB) // TODO: nneof once all done

  private var sInstance: ProxyDataSource = _

  def getInstance(): ProxyDataSource = {
    if (sInstance == null) {
      sInstance = new ProxyDataSource(conf)
    }
    sInstance
  }

  def getIDatasource: IDatasource = getInstance()

  private def initCouchbase(): Unit = {
    this.mCouchbase = CouchbaseDatasource.initialize(conf)
  }

  private def initMongodb(): Unit = {
    this.mongoDB = MongodbDatasource.initialize(conf)
  }

  private def setActiveDatabase(ds: IDatasource): Unit = {
    this.mActiveDatabase = ds
  }

  override def init(): Boolean = true

  override def addJsonDocument(key: String, expiry: Int, document: String): Boolean = {
    _checkActiveDatasource()
    mActiveDatabase.addJsonDocument(key, expiry, document)
  }

  override def addJsonDocument(col: String, document: String): Boolean = {
    _checkActiveDatasource()
    mongoDB.addJsonDocument(col, document)
  }

  override def replaceJsonDocument(key: String, expiry: Int, document: String): Boolean = {
    _checkActiveDatasource()
    mActiveDatabase.replaceJsonDocument(key, expiry, document)
  }

  override def replaceJsonDocument(col: String, key: String, value: String, document: String): Boolean = {
    _checkActiveDatasource()
    mongoDB.replaceJsonDocument(col, key, value, document)
  }

  def deleteFromKey(col: String, key: String, value:String): Boolean = {
    _checkActiveDatasource()
    mongoDB.deleteFromKey(col, key, value)
  }

  override def deleteFromKey(key: String): Boolean = {
    _checkActiveDatasource()
    mActiveDatabase.deleteFromKey(key)
  }

  override def getFromKey(collection:String, key: String, value: String):JsValue = {
    _checkActiveDatasource()
    mongoDB.getFromKey(collection, key, value)
  }

  //override def getFromKey(key: String): AnyRef = {
  //  _checkActiveDatasource()
  //  mActiveDatabase.getFromKey(key)
  //}

  override def deleteRadiosInBox(): Boolean = {
    _checkActiveDatasource()
    mActiveDatabase.deleteRadiosInBox()
  }

  override def getFromKeyAsJson(collection: String,key: String, value: String): JsValue = {
    _checkActiveDatasource()
    mongoDB.getFromKeyAsJson(collection, key, value)
  }

  override def fingerprintExists(collection: String,buid: String, floor: String, x: String, y:String, heading:String): Boolean = {
    _checkActiveDatasource()
    mongoDB.fingerprintExists(collection, buid, floor, x, y, heading)
  }

  //override def getFromKeyAsJson(key: String): JsValue = {
  //  _checkActiveDatasource()
  //  mActiveDatabase.getFromKeyAsJson(key)
  //}

  override def buildingFromKeyAsJson(key: String): JsValue = {
    _checkActiveDatasource()
    mongoDB.buildingFromKeyAsJson(key)
  }

  override def poiFromKeyAsJson(collection: String, key: String, value: String): JsValue = getFromKeyAsJson(collection, key, value)

  override def poisByBuildingFloorAsJson(buid: String, floor_number: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.poisByBuildingFloorAsJson(buid, floor_number)
  }

  override def poisByBuildingFloorAsMap(buid: String, floor_number: String): java.util.List[HashMap[String, String]] = {
    _checkActiveDatasource()
    mongoDB.poisByBuildingFloorAsMap(buid, floor_number)
  }

  override def poisByBuildingAsJson(buid: String): java.util.List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.poisByBuildingAsJson(buid)
  }

  override def poiByBuidFloorPuid(buid: String, floor_number: String, puid: String): Boolean = {
    _checkActiveDatasource()
    mongoDB.poiByBuidFloorPuid(buid, floor_number, puid)
  }

  override def poisByBuildingAsMap(buid: String): java.util.List[HashMap[String, String]] = {
    _checkActiveDatasource()
    mongoDB.poisByBuildingAsMap(buid)
  }

  override def floorsByBuildingAsJson(buid: String): java.util.List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.floorsByBuildingAsJson(buid)
  }

  override def connectionsByBuildingAsJson(buid: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.connectionsByBuildingAsJson(buid)
  }

  override def connectionsByBuildingAsMap(buid: String): java.util.List[HashMap[String, String]] = {
    _checkActiveDatasource()
    mongoDB.connectionsByBuildingAsMap(buid)
  }

  override def connectionsByBuildingFloorAsJson(buid: String, floor_number: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.connectionsByBuildingFloorAsJson(buid, floor_number)
  }

  override def deleteAllByBuilding(buid: String):Boolean = {
    _checkActiveDatasource()
    mongoDB.deleteAllByBuilding(buid)
  }

  override def deleteAllByFloor(buid: String, floor_number: String): Boolean = {
    _checkActiveDatasource()
    mongoDB.deleteAllByFloor(buid, floor_number)
  }

  override def deleteAllByConnection(cuid: String): java.util.List[String] = {
    _checkActiveDatasource()
    mongoDB.deleteAllByConnection(cuid)
  }

  override def deleteAllByPoi(puid: String): java.util.List[String] = {
    _checkActiveDatasource()
    mongoDB.deleteAllByPoi(puid)
  }

  override def getRadioHeatmap(): java.util.List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getRadioHeatmap()
  }

  override def getRadioHeatmapByBuildingFloor(buid: String, floor: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.getRadioHeatmapByBuildingFloor(buid, floor)
  }

  override def getRadioHeatmapByBuildingFloorAverage1(buid: String, floor: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.getRadioHeatmapByBuildingFloorAverage1(buid, floor)
  }

  override def getRadioHeatmapByBuildingFloorAverage2(buid: String, floor: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.getRadioHeatmapByBuildingFloorAverage2(buid, floor)
  }

  override def getRadioHeatmapByBuildingFloorAverage3(buid: String, floor: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.getRadioHeatmapByBuildingFloorAverage3(buid, floor)
  }

  override def getRadioHeatmapByFloorTimestamp(buid: String, floor: String, timestampX: String, timestampY: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.getRadioHeatmapByFloorTimestamp(buid, floor, timestampX, timestampY)
  }

  override def getRadioHeatmapByBuildingFloorTimestampAverage1(buid: String, floor: String, timestampX: String, timestampY: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.getRadioHeatmapByBuildingFloorTimestampAverage1(buid, floor, timestampX, timestampY)
  }

  override def getRadioHeatmapByBuildingFloorTimestampAverage2(buid: String, floor: String, timestampX: String, timestampY: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.getRadioHeatmapByBuildingFloorTimestampAverage2(buid, floor, timestampX, timestampY)
  }

  override def getAPsByBuildingFloorcdb(buid: String, floor: String): util.List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.getAPsByBuildingFloorcdb(buid, floor)
  }

  override def getAPsByBuildingFloor(buid: String, floor: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.getAPsByBuildingFloor(buid, floor)
  }

  override def getCachedAPsByBuildingFloor(buid: String, floor: String): JsValue = {
    _checkActiveDatasource()
    mongoDB.getCachedAPsByBuildingFloor(buid, floor)
  }

  override def deleteAllByXsYs(buid: String,floor: String,x: String,y: String): java.util.List[String] = {
    _checkActiveDatasource()
    mActiveDatabase.deleteAllByXsYs(buid,floor,x,y)
  }

  override def getFingerPrintsBBox(buid: String, floor: String,lat1: String, lon1: String, lat2: String, lon2: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.getFingerPrintsBBox(buid,floor,lat1,lon1,lat2,lon2)
  }
  override def getFingerPrintsTimestampBBox(buid: String, floor: String, lat1: String, lon1: String, lat2: String, lon2: String, timestampX: String, timestampY: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.getFingerPrintsTimestampBBox(buid: String, floor: String, lat1: String, lon1: String, lat2: String, lon2: String, timestampX: String, timestampY: String)
  }

  override def getFingerprintsByTime(buid: String, floor: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.getFingerprintsByTime(buid,floor)
  }

  override def getAllBuildings(): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.getAllBuildings()
  }

  override def getAllBuildingsByOwner(oid: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.getAllBuildingsByOwner(oid)
  }

  override def getAllBuildingsByBucode(bucode: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.getAllBuildingsByBucode(bucode)
  }

  override def getBuildingByAlias(alias: String): JsonObject = {
    _checkActiveDatasource()
    mActiveDatabase.getBuildingByAlias(alias)
  }

  override def getAllBuildingsNearMe(lat: Double, lng: Double, range: Int, owner_id: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.getAllBuildingsNearMe(lat, lng, range, owner_id)
  }

  override def dumpRssLogEntriesSpatial(outFile: FileOutputStream, bbox: Array[GeoPoint], floor_number: String): Long = {
    _checkActiveDatasource()
    mongoDB.dumpRssLogEntriesSpatial(outFile, bbox, floor_number)
  }

  override def dumpRssLogEntriesWithCoordinates (floor_number: String, lat: Double, lon: Double): String = {
    _checkActiveDatasource()
    mongoDB.dumpRssLogEntriesWithCoordinates(floor_number: String, lat, lon)
  }

  override def dumpRssLogEntriesByBuildingACCESFloor(outFile: FileOutputStream, buid: String, floor_number: String): Long = {
    _checkActiveDatasource()
    mActiveDatabase.dumpRssLogEntriesByBuildingACCESFloor(outFile, buid, floor_number)
  }

  override def dumpRssLogEntriesByBuildingFloor(outFile: FileOutputStream, buid: String, floor_number: String): Long = {
    _checkActiveDatasource()
    mongoDB.dumpRssLogEntriesByBuildingFloor(outFile, buid, floor_number)
  }

  override def getAllAccounts(): List[JsValue] = {
    _checkActiveDatasource()
    //mActiveDatabase.getAllAccounts
    mongoDB.getAllAccounts()
  }

  def _checkActiveDatasource(): Unit = {
    if (this.mActiveDatabase == null) {
      throw new DatasourceException("No active Datasource exists!")
    }
  }

  override def predictFloor(algo: IAlgo, bbox: Array[GeoPoint], strongestMACs: Array[String]): Boolean = {
    _checkActiveDatasource()
    mongoDB.predictFloor(algo, bbox, strongestMACs)
  }

  override def magneticPathsByBuildingFloorAsJson(buid: String, floor_number: String): java.util.List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.magneticPathsByBuildingFloorAsJson(buid, floor_number)
  }

  override def magneticPathsByBuildingAsJson(buid: String): java.util.List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.magneticPathsByBuildingAsJson(buid)
  }

  override def magneticMilestonesByBuildingFloorAsJson(buid: String, floor_number: String): java.util.List[JsonObject] = {
    _checkActiveDatasource()
    mActiveDatabase.magneticMilestonesByBuildingFloorAsJson(buid, floor_number)
  }

  override def poisByBuildingIDAsJson(buid: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.poisByBuildingIDAsJson(buid)
  }

  override def poisByBuildingAsJson2(cuid: String, letters: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.poisByBuildingAsJson2(cuid, letters)
  }

  override def poisByBuildingAsJson2GR(cuid: String, letters: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.poisByBuildingAsJson2GR(cuid, letters)
  }

  override def poisByBuildingAsJson3(buid: String, letters: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.poisByBuildingAsJson3(buid,letters)
  }

  override def connectionsByBuildingAllFloorsAsJson(buid: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.connectionsByBuildingAllFloorsAsJson(buid)
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
    mongoDB.BuildingSetsCuids(cuid)
  }

  override def getBuildingSet(cuid: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.getBuildingSet(cuid)
  }

  override def getAllBuildingsetsByOwner(owner_id: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.getAllBuildingsetsByOwner(owner_id)
  }

  override def generateHeatmaps(): Boolean = {
    _checkActiveDatasource()
    mongoDB.generateHeatmaps()
  }

  override def deleteNotValidDocuments(): Boolean ={
    _checkActiveDatasource()
    mActiveDatabase.deleteNotValidDocuments()
  }

  override def deleteAffectedHeatmaps(buid: String, floor_number: String): Boolean = {
    _checkActiveDatasource()
    mongoDB.deleteAffectedHeatmaps(buid, floor_number)
  }

  override def deleteFingerprint(fingerprint: JsValue): Boolean = {
    _checkActiveDatasource()
    mongoDB.deleteFingerprint(fingerprint)
  }

  override def createTimestampHeatmap(col: String, buid: String, floor: String, level: Int) = {
    _checkActiveDatasource()
    mongoDB.createTimestampHeatmap(col, buid, floor, level)
  }

  override def login(collection: String, username: String, password: String): List[JsValue] = {
    _checkActiveDatasource()
    mongoDB.login(collection, username, password)
  }

  override def register(collection: String, name: String, email: String, username: String, password: String,
                        external: String, accType: String): JsValue = {
    _checkActiveDatasource()
    mongoDB.register(collection, name, email, username, password, external, accType)
  }

  override def isAdmin(col: String): Boolean = {
    _checkActiveDatasource()
    mongoDB.isAdmin(col)
  }
}
