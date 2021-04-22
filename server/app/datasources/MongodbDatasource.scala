/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Nikolas Neofytou
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

import java.io.{FileOutputStream, IOException, PrintWriter}
import java.util

import com.couchbase.client.java.document.json.JsonObject
import datasources.MongodbDatasource.{__geometry, admins, convertJson, mdb}
import db_models.RadioMapRaw.unrollFingerprint
import db_models.{Connection, Poi, RadioMapRaw}
import floor_module.IAlgo
import org.mongodb.scala._
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Aggregates
import org.mongodb.scala.model.Aggregates.project
import org.mongodb.scala.model.Filters._
import play.Play
import play.api.libs.json.{JsNumber, JsObject, JsValue, Json}
import play.twirl.api.TemplateMagic.javaCollectionToScala
import utils.JsonUtils.cleanupMongoJson
import utils.{GeoPoint, JsonUtils, LPLogger}

import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.text.Document.break

object MongodbDatasource {
  private var sInstance: MongodbDatasource = null
  private var mdb: MongoDatabase = null
  private var admins: List[String] = List[String]()
  val __SCHEMA: Int = 0
  val __USERS = "users"
  val __geometry = "geometry"

  def getMDB: MongoDatabase = mdb

  def getStaticInstance: MongodbDatasource = {
    val conf = Play.application().configuration()
    val username = conf.getString("mongodb.app.username")
    val password = conf.getString("mongodb.app.password")
    val hostname = conf.getString("mongodb.hostname")
    val port = conf.getString("mongodb.port")
    val database = conf.getString("mongodb.database")
    sInstance = createInstance(hostname, database, username, password, port)
    sInstance
  }

  def createInstance(hostname: String, database: String, username: String, password: String, port: String): MongodbDatasource = {
    val uri: String = "mongodb://" + username + ":" + password + "@" + hostname + ":" + port
    val mongoClient: MongoClient = MongoClient(uri)
    // TODO check if database anyplace exists
    mdb = mongoClient.getDatabase(database)
    val collections = mdb.listCollectionNames()
    val awaited = Await.result(collections.toFuture, Duration.Inf)
    val res = awaited.toList
    LPLogger.info("MongoDB: Connected to: " + hostname + ":" + port)
    LPLogger.debug("Collections = " + res)
    admins = loadAdmins()
    new MongodbDatasource()
  }

  /**
   * Cache admins on MongoDB initialization.
   *
   * @return a list with admins.
   */
  def loadAdmins(): List[String] = {
    val collection = mdb.getCollection(__USERS)
    val query = BsonDocument("type" -> "admin")
    val adm = collection.find(query)
    val awaited = Await.result(adm.toFuture, Duration.Inf)
    val res = awaited.toList
    val ret = new util.ArrayList[String]
    for (admin <- res) {
      val temp = Json.parse(admin.toJson())
      ret.add((temp \ "owner_id").as[String])
    }
    ret.toList
  }

  def convertJson(list: List[Document]): List[JsValue] = {
    val jsList = ListBuffer[JsValue]()
    for (doc <- list) {
      if (doc != null)
        jsList.append(convertJson(doc))
    }
    jsList.toList
  }

  def convertJson(doc: Document) = cleanupMongoJson(Json.parse(doc.toJson()))
}

// TODO getAllAccounts()

class MongodbDatasource() extends IDatasource {

  private def connect(): Boolean = {
    //    LPLogger.info("Mongodb: connecting to: " + mHostname + ":" + mPort + " bucket[" +
    //      mBucket + "]")
    false
  }

  override def poisByBuildingIDAsJson(buid: String): List[JsValue] = {
    val collection = mdb.getCollection("pois")
    val query = BsonDocument("buid" -> buid)
    val pois = collection.find(query)
    val awaited = Await.result(pois.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val poisArray = new java.util.ArrayList[JsValue]()
    for (poi <- listJson)
      poisArray.add(poi.as[JsObject] - "owner_id" - "geometry" - "_id" - "_schema")
    poisArray.toList
  }

  override def poisByBuildingAsJson2(cuid: String, letters: String): java.util.List[JsonObject] = ???

  override def poisByBuildingAsJson2GR(cuid: String, letters: String): java.util.List[JsonObject] = ???

  override def poisByBuildingAsJson3(buid: String, letters: String): java.util.List[JsonObject] = ???

  override def init(): Boolean = ???

  def addJsonDocument(key: String, expiry: Int, document: String): Boolean = ???

  override def addJsonDocument(col: String, document: String): Boolean = {
    val collection = mdb.getCollection(col)
    val addJson = collection.insertOne(Document.apply(document))
    val awaited = Await.result(addJson.toFuture, Duration.Inf)
    val res = awaited
    if (res.toString() == "The operation completed successfully")
      true
    else
      false
  }

  override def replaceJsonDocument(key: String, expiry: Int, document: String): Boolean = ???

  override def replaceJsonDocument(col: String, key: String, value: String, document: String): Boolean = {
    val collection = mdb.getCollection(col)
    val query = BsonDocument(key -> value)
    val update = BsonDocument(document)
    val replaceJson = collection.replaceOne(query, update)
    val awaited = Await.result(replaceJson.toFuture, Duration.Inf)
    val res = awaited
    if (res.getModifiedCount == 0)
      false
    else
      true
  }

  override def deleteFromKey(key: String): Boolean = ???

  override def deleteFromKey(col: String, key: String, value: String): Boolean = {
    val collection = mdb.getCollection(col)
    val query = BsonDocument(key -> value)
    val deleted = collection.deleteOne(query)
    val awaited = Await.result(deleted.toFuture, Duration.Inf)
    val res = awaited
    res.wasAcknowledged()
  }

  override def getFromKey(key: String) = ???

  override def getFromKey(col: String, key: String, value: String): JsValue = {
    val collection = mdb.getCollection(col)
    val buildingLookUp = collection.find(equal(key, value)).first()
    val awaited = Await.result(buildingLookUp.toFuture, Duration.Inf)
    val res = awaited.asInstanceOf[Document]
    if (res != null)
      convertJson(res)
    else
      null
  }

  override def getFromKeyAsJson(key: String) = ???

  override def getFromKeyAsJson(collection: String, key: String, value: String): JsValue = {
    if (key == null || key.trim().isEmpty) {
      throw new IllegalArgumentException("No null or empty string allowed as key!")
    }
    val db_res = getFromKey(collection, key, value)
    if (db_res == null) {
      return null
    }
    try {
      db_res
    } catch {
      case e: IOException => {
        LPLogger.error("CouchbaseDatasource::getFromKeyAsJson():: Could not convert document from Couchbase into JSON!")
        null
      }
    }
  }

  override def fingerprintExists(col: String, buid: String, floor: String, x: String, y: String, heading: String): Boolean = {
    val collection = mdb.getCollection(col)
    val query = BsonDocument("buid" -> buid, "floor" -> floor, "x" -> x, "y" -> y, "heading" -> heading)
    val fingerprintLookUp = collection.find(query)
    val awaited = Await.result(fingerprintLookUp.toFuture, Duration.Inf)
    val res = awaited.asInstanceOf[List[Document]]
    if (res.size > 0)
      return true
    return false
  }

  override def buildingFromKeyAsJson(value: String): JsValue = {
    var building = getFromKeyAsJson("buildings", "buid", value)
    if (building == null) {
      return null
    }
    building = building.as[JsObject] - "_id" - "_schema"
    val floors = new java.util.ArrayList[JsValue]()
    for (f <- floorsByBuildingAsJson(value)) {
      floors.add(f)
    }

    // ignore pois with pois_type = "None"
    val pois = new java.util.ArrayList[JsValue]()
    for (p <- poisByBuildingAsJson(value)) {
      if (!(p \ "pois_type").toString.equalsIgnoreCase(Poi.POIS_TYPE_NONE))
        pois.add(p)
    }
    building.as[JsObject] + ("floors" -> Json.toJson(floors.toList)) +
      ("pois" -> Json.toJson(pois.toList))

  }

  override def poiFromKeyAsJson(collection: String, key: String, value: String): JsValue = {
    getFromKeyAsJson(collection, key, value)
  }

  override def poisByBuildingFloorAsJson(buid: String, floor_number: String): List[JsValue] = {
    val collection = mdb.getCollection("pois")
    val query = BsonDocument("buid" -> buid, "floor_number" -> floor_number)
    val pois = collection.find(query)
    val awaited = Await.result(pois.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val poisArray = new java.util.ArrayList[JsValue]()
    for (poi <- listJson)
      poisArray.add(poi.as[JsObject] - "owner_id" - __geometry - "_id" - "_schema")
    poisArray.toList
  }

  override def poisByBuildingFloorAsMap(buid: String, floor_number: String): java.util.List[java.util.HashMap[String, String]] = {
    val pois = poisByBuildingFloorAsJson(buid, floor_number)
    val result = new java.util.ArrayList[java.util.HashMap[String, String]]()
    for (pois <- pois) {
      result.add(JsonUtils.getHashMapStrStr(pois))
    }
    result
  }

  override def poisByBuildingAsJson(buid: String): java.util.List[JsValue] = {
    val collection = mdb.getCollection("pois")
    val poisLookUp = collection.find(equal("buid", buid))
    val awaited = Await.result(poisLookUp.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val pois = new java.util.ArrayList[JsValue]()
    for (floor <- listJson) {
      try {
        pois.add(floor.as[JsObject] - "owner_id" - __geometry - "_id" - "_schema")
      } catch {
        case e: IOException =>
      }
    }
    pois
  }

  override def poiByBuidFloorPuid(buid: String, floor_number: String, puid: String): Boolean = {
    val collection = mdb.getCollection("pois")
    val query = BsonDocument("buid" -> buid, "floor_number" -> floor_number, "puid" -> puid)
    val poisLookUp = collection.find(query)
    val awaited = Await.result(poisLookUp.toFuture, Duration.Inf)
    val res = awaited.toList
    if (res.size > 0)
      return true
    false
  }

  override def poisByBuildingAsMap(buid: String): java.util.List[java.util.HashMap[String, String]] = {
    val pois = poisByBuildingAsJson(buid)
    val result = new java.util.ArrayList[java.util.HashMap[String, String]]()
    for (poi <- pois) {
      result.add(JsonUtils.getHashMapStrStr(poi))
    }
    result
  }

  override def floorsByBuildingAsJson(buid: String): java.util.List[JsValue] = {
    val collection = mdb.getCollection("floorplans")
    val floorLookUp = collection.find(equal("buid", buid))
    val awaited = Await.result(floorLookUp.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val floors = new java.util.ArrayList[JsValue]()
    for (floor <- listJson) {
      try {
        floors.add(floor.as[JsObject] - "owner_id" - "_id" - "_schema")
      } catch {
        case e: IOException =>
      }
    }
    floors
  }

  override def connectionsByBuildingAsJson(buid: String): List[JsValue] = {
    val collection = mdb.getCollection("edges")
    val query = BsonDocument("buid" -> buid)
    val edges = collection.find(query)
    val awaited = Await.result(edges.toFuture, Duration.Inf)
    val res = awaited.toList
    convertJson(res)
  }

  override def connectionsByBuildingAsMap(buid: String): java.util.List[java.util.HashMap[String, String]] = {

    val edges = connectionsByBuildingAsJson(buid)
    var hm: util.HashMap[String, String] = null
    val conns = new util.ArrayList[util.HashMap[String, String]]()
    for (edge <- edges) {
      hm = JsonUtils.getHashMapStrStr(edge)
      if (!hm.get("edge_type").equalsIgnoreCase(Connection.EDGE_TYPE_OUTDOOR))
        conns.add(hm)
    }
    conns
  }

  override def connectionsByBuildingFloorAsJson(buid: String, floor_number: String): List[JsValue] = {
    val collection = mdb.getCollection("edges")
    val query = BsonDocument("buid" -> buid, "floor_a" -> floor_number, "floor_b" -> floor_number)
    val edges = collection.find(query)
    val awaited = Await.result(edges.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val edgesArray = new java.util.ArrayList[JsValue]()
    for (poi <- listJson)
      edgesArray.add(poi.as[JsObject] - "owner_id" - "_id" - "_schema")
    edgesArray.toList
  }

  override def connectionsByBuildingAllFloorsAsJson(buid: String): List[JsValue] = {
    val collection = mdb.getCollection("edges")
    val query = BsonDocument("buid" -> buid)
    val edges = collection.find(query)
    val awaited = Await.result(edges.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val edgesArray = new java.util.ArrayList[JsValue]()
    for (poi <- listJson)
      edgesArray.add(poi.as[JsObject] - "owner_id" - "_id" - "_schema")
    edgesArray.toList
  }


  override def deleteAllByBuilding(buid: String): Boolean = {
    LPLogger.debug("Cascading building " + buid)
    var ret = true

    // deleting floors along with pois, edges
    val floors = floorsByBuildingAsJson(buid)
    for (floor <- floors) {
      if ((floor \ "floor_number").toOption.isDefined) {
        val tempBool = deleteAllByFloor(buid, (floor \ "floor_number").as[String])
        ret = ret && tempBool
      }
    }

    // deleting building
    var query = BsonDocument("buid" -> buid)
    var collection = mdb.getCollection("buildings")
    val objects = collection.deleteOne(query)
    val awaited = Await.result(objects.toFuture, Duration.Inf)
    val res = awaited
    val bool = res.wasAcknowledged()

    // deleting buid from campus buids[]
    query = BsonDocument("buids" -> buid)
    collection = mdb.getCollection("campuses")
    val campuses = collection.find(query)
    var await = Await.result(campuses.toFuture, Duration.Inf)
    var re = await.toList
    val camp = convertJson(re)
    LPLogger.debug("camp = " + camp)
    for (c <- camp) {
      val newBuids: util.ArrayList[String] = new util.ArrayList[String]()
      val buids = (c \ "buids").as[List[String]]
      for (buid2 <- buids) {
        if (buid2 != buid)
          newBuids.add(buid2)
      }
      //      LPLogger.debug("OldBuids = " + buids)
      //      LPLogger.debug("newBuids = " + newBuids)
      val newCamp: String = (c.as[JsObject] + ("buids" -> Json.toJson(newBuids.toList))).toString()
      ret = ret && replaceJsonDocument("campuses", "cuid", (c \ "cuid").as[String], newCamp)
    }

    // deleting fingerprints
    // requires testing
    query = BsonDocument("buid" -> buid)
    collection = mdb.getCollection("fingerprintsWifi")
    val deleted = collection.deleteMany(query)
    val delAwaited = Await.result(deleted.toFuture, Duration.Inf)
    val delRes = delAwaited
    val bool1 = delRes.wasAcknowledged()
    LPLogger.debug("fingerprints with buid " + buid + " " + delRes.toString)
    ret = ret && bool1

    (ret && bool)
  }

  override def deleteAllByFloor(buid: String, floor_number: String): Boolean = {
    LPLogger.debug("deleteAllByFloor::")
    LPLogger.debug("Cascade deletion: edges reaching this floor, edges of this floor," +
      " pois of this floor, floorplan(mongoDB), floorplan image(server)")
    var collection = mdb.getCollection("edges")

    // queryBuidA deletes edges that start from building buid (containing edges that have buid_b == buid_a)
    val queryBuidA = BsonDocument("buid_a" -> buid, "floor_a" -> floor_number)
    var deleted = collection.deleteMany(queryBuidA)
    var awaited = Await.result(deleted.toFuture, Duration.Inf)
    var res = awaited
    val bool1 = res.wasAcknowledged()
    LPLogger.debug("edges from buid_a: " + buid + " " + res.toString)

    // queryBuidB deletes edges that point to building buid
    val queryBuidB = BsonDocument("buid_b" -> buid, "floor_b" -> floor_number)
    deleted = collection.deleteMany(queryBuidB)
    awaited = Await.result(deleted.toFuture, Duration.Inf)
    res = awaited
    val bool2 = res.wasAcknowledged()
    LPLogger.debug("edges to buid_b: " + buid + " " + res.toString)

    // this query will delete the pois of the floor
    val queryFloor = BsonDocument("buid" -> buid, "floor_number" -> floor_number)
    collection = mdb.getCollection("pois")
    deleted = collection.deleteMany(queryFloor)
    awaited = Await.result(deleted.toFuture, Duration.Inf)
    res = awaited
    val bool3 = res.wasAcknowledged()
    LPLogger.debug("pois in building with buid: " + buid + " " + res.toString)

    // this query will delete the floor it self
    collection = mdb.getCollection("floorplans")
    deleted = collection.deleteMany(queryFloor)
    awaited = Await.result(deleted.toFuture, Duration.Inf)
    res = awaited
    val bool4 = res.wasAcknowledged()
    LPLogger.debug("floorplan with buid " + buid + " " + res.toString)
    bool1 && bool2 && bool3 && bool4
  }

  override def deleteAllByConnection(cuid: String): java.util.List[String] = {
    val all_items_failed = new util.ArrayList[String]()
    if (!this.deleteFromKey("edges", "cuid", cuid)) {
      all_items_failed.add(cuid)
    }
    all_items_failed
  }

  // ASK:PM
  override def deleteAllByPoi(puid: String): java.util.List[String] = {
    val all_items_failed = new util.ArrayList[String]()
    if (!this.deleteFromKey("edges", "pois_a", puid)) {
      all_items_failed.add(puid)
    }
    if (!this.deleteFromKey("edges", "pois_b", puid)) {
      all_items_failed.add(puid)
    }
    if (!this.deleteFromKey("pois", "puid", puid)) {
      all_items_failed.add(puid)
    }
    all_items_failed
  }

  override def getRadioHeatmap(): java.util.List[JsonObject] = ???

  override def getRadioHeatmapByBuildingFloor(buid: String, floor: String): List[JsValue] = {
    val collection = mdb.getCollection("fingerprintsWifi")
    val query = BsonDocument("buid" -> buid, "floor" -> floor)

    val radioPoints = collection.aggregate(Seq(
      Aggregates.filter(query),
      project(
        Document("x" -> "$x", "y" -> "$y", "measurements" -> "$measurements")
      )))

    val awaited = Await.result(radioPoints.toFuture, Duration.Inf)
    val res = awaited.toList
    LPLogger.debug(s"Res on complete Length:${res.length}")
    val listJson = convertJson(res)
    var points = new util.ArrayList[JsValue]
    for (x <- listJson) {
      val y = (x \ "measurements").as[List[List[String]]]
      val temp: JsValue = x
      points.add(temp.as[JsObject] - "_id")
    }
    points.toList
  }

  override def getRadioHeatmapByBuildingFloorAverage(buid: String, floor: String): java.util.List[JsonObject] = ???

  override def getRadioHeatmapByBuildingFloorAverage1(buid: String, floor: String): java.util.List[JsonObject] = ???

  override def getRadioHeatmapByBuildingFloorAverage2(buid: String, floor: String): java.util.List[JsonObject] = ???

  override def getRadioHeatmapByBuildingFloorAverage3(buid: String, floor: String): java.util.List[JsonObject] = ???

  override def getRadioHeatmapByBuildingFloorTimestamp(buid: String, floor: String, timestampX: String, timestampY: String): java.util.List[JsonObject] = ???

  override def getRadioHeatmapByBuildingFloorTimestampAverage1(buid: String, floor: String, timestampX: String, timestampY: String): java.util.List[JsonObject] = ???

  override def getRadioHeatmapByBuildingFloorTimestampAverage2(buid: String, floor: String, timestampX: String, timestampY: String): java.util.List[JsonObject] = ???

  //override def getAPsByBuildingFloor(buid: String, floor: String): util.List[JsonObject] = ???
  override def getAPsByBuildingFloor(buid: String, floor: String): List[JsValue] = {
    val collection = mdb.getCollection("fingerprintsWifi")
    val query = BsonDocument("buid" -> buid, "floor" -> floor)
    val fingerprints = collection.find(query)
    val awaited = Await.result(fingerprints.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val hm = new util.HashMap[JsValue, Array[Double]]()
    for (f <- listJson) {
      if ((f \ "buid").as[String] == buid && (f \ "floor").as[String] == floor) {
        val measurements = (f \ "measurements").as[List[List[String]]]
        for (measurement <- measurements) {
          val json = unrollFingerprint(f, measurement)
          var key = Json.obj("x" -> (json \ "x").as[String], "y" -> (json \ "y").as[String],
            "AP" -> (json \ "MAC").as[String])
          if (!hm.containsKey(key)) {
            // count / average / total
            val tArray = new Array[Double](3)
            tArray(0) = 1
            tArray(1) = (json \ "rss").as[String].toDouble
            tArray(2) = (json \ "rss").as[String].toDouble
            hm.put(key, tArray)
          } else {
            val tArray = hm.get(key)
            tArray(0) += 1
            tArray(1) = (tArray(1) / tArray(0))
            tArray(2) += hm.get(key)(2)
            hm.put(key, tArray)
          }
        }
      }
    }
    val points = new util.ArrayList[JsValue]()
    for (h <- hm.toMap.toList) {
      var tempJson: JsValue = h._1
      val rss = Json.obj("count" -> JsNumber(h._2(0)),
        "average" -> JsNumber(h._2(2)),
        "total" -> JsNumber(h._2(1)))
      tempJson = tempJson.as[JsObject] + ("RSS" -> rss)
      if ((rss \ "average").as[Double] >= -70)
        points.add(tempJson.as[JsObject] - "x" - "y")
    }
    points.toList
  }

  override def getCachedAPsByBuildingFloor(buid: String, floor: String): JsValue = {
    val collection = mdb.getCollection("accessPointsWifi")
    val query = BsonDocument("buid" -> buid, "floor" -> floor)
    val accessPointsLookup = collection.find(query)
    val awaited = Await.result(accessPointsLookup.toFuture, Duration.Inf)
    val res = awaited.toList
    if (res.size == 0)
      return null
    val json = convertJson(res(0))
    return json
  }

  override def deleteAllByXsYs(buid: String, floor: String, x: String, y: String): java.util.List[String] = ???

  override def getFingerPrintsBBox(buid: String, floor: String, lat1: String, lon1: String, lat2: String, lon2: String): List[JsValue] = {
    val collection = mdb.getCollection("fingerprintsWifi")
    val fingerprints = collection.find(and(
      geoWithinBox("geometry", lat1.toDouble, lon1.toDouble, lat2.toDouble, lon2.toDouble),
      equal("buid", buid),
      equal("floor", floor)))
    val awaited = Await.result(fingerprints.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val newList = new util.ArrayList[JsValue]()
    for (f <- listJson) {
      if ((f \ "buid").as[String] == buid && (f \ "floor").as[String] == floor) {
        newList.add(f)
      }
    }
    newList.toList
  }

  override def getFingerPrintsTimestampBBox(buid: String, floor: String, lat1: String, lon1: String, lat2: String,
                                            lon2: String, timestampX: String, timestampY: String): List[JsValue] = {
    val collection = mdb.getCollection("fingerprintsWifi")
    val fingerprints = collection.find(and(geoWithinBox("geometry", lat1.toDouble, lon1.toDouble,
      lat2.toDouble, lon2.toDouble), and(gt("timestamp", timestampX), lt("timestamp", timestampY))))
    val awaited = Await.result(fingerprints.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val newList = new util.ArrayList[JsValue]()
    for (f <- listJson) {
      if ((f \ "buid").as[String] == buid && (f \ "floor").as[String] == floor) {
        newList.add(f)
      }
    }
    newList.toList
  }

  override def getFingerPrintsTime(buid: String, floor: String): List[JsValue] = {
    val collection = mdb.getCollection("fingerprintsWifi")
    val fingerprints = collection.find(and(and(gt("timestamp", "000000000000000"),
      lt("timestamp", "999999999999999")), and(equal("buid", buid)), equal("floor", floor)))
    val awaited = Await.result(fingerprints.toFuture, Duration.Inf)
    val res = awaited.toList
    LPLogger.debug("res = " + res.size)
    val listJson = convertJson(res)
    val points = new util.ArrayList[JsValue]()
    for (f <- listJson) {
      points.add(Json.obj("date" -> (f \ "timestamp").as[String],
        "count" -> (f \ "measurements").as[List[List[String]]].size))
    }
    points.toList
  }

  override def getRadioHeatmapByBuildingFloor2(lat: String, lon: String, buid: String, floor: String, range: Int): java.util.List[JsonObject] = ???

  override def getRadioHeatmapBBox(lat: String, lon: String, buid: String, floor: String, range: Int): java.util.List[JsonObject] = ???

  override def getRadioHeatmapBBox2(lat: String, lon: String, buid: String, floor: String, range: Int): java.util.List[JsonObject] = ???


  override def getAllBuildings(): List[JsValue] = {
    val collection = mdb.getCollection("buildings")
    val query = BsonDocument("is_published" -> "true")
    val buildings = collection.find(query)
    val awaited = Await.result(buildings.toFuture, Duration.Inf)
    val res = awaited.toList
    LPLogger.debug(s"Res on complete Length:${res.length}")
    val listJson = convertJson(res)
    var newJson: JsValue = null
    val newList = new util.ArrayList[JsValue]()
    for (x <- listJson) {
      newJson = x.as[JsObject] - __geometry - "owner_id" - "co_owners" - "_id"
      newList.add(newJson)
    }
    newList.toList
  }


  override def getAllBuildingsByOwner(oid: String): List[JsValue] = {
    val collection = mdb.getCollection("buildings")
    var buildingLookUp = collection.find(or(equal("owner_id", oid), equal("co_owners", oid)))
    if (admins.contains(oid)) {
      buildingLookUp = collection.find()
    }
    val awaited = Await.result(buildingLookUp.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val buildings = new java.util.ArrayList[JsValue]()
    for (building <- listJson) {
      try {
        buildings.add(building.as[JsObject] - "co_owners" - __geometry - "owner_id" - "_id" - "_schema")
      } catch {
        case e: IOException =>
      }
    }
    buildings.toList
  }


  override def getAllBuildingsByBucode(bucode: String): List[JsValue] = {
    val collection = mdb.getCollection("buildings")
    val buildingLookUp = collection.find(equal("bucode", bucode))
    val awaited = Await.result(buildingLookUp.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val buildings = new java.util.ArrayList[JsValue]()
    for (building <- listJson) {
      try {
        buildings.add(building.as[JsObject] - "co_owners" - __geometry - "owner_id" - "_id" - "_schema")
      } catch {
        case e: IOException =>
      }
    }
    buildings.toList
  }

  override def getBuildingByAlias(alias: String): JsonObject = ???

  override def getAllBuildingsNearMe(lat: Double, lng: Double, range: Int, owner_id: String): List[JsValue] = {
    val bbox = GeoPoint.getGeoBoundingBox(lat, lng, range)
    val collection = mdb.getCollection("buildings")
    val buildingLookUp = collection.find(and(geoWithinBox("geometry", bbox(0).dlat, bbox(0).dlon, bbox(1).dlat,
      bbox(1).dlon),
      or(equal("is_published", "true"),
        and(equal("is_published", "false"), equal("owner_id", owner_id)))))
    val awaited = Await.result(buildingLookUp.toFuture, Duration.Inf)
    val res = awaited.toList
    LPLogger.debug("getAllBuildingsNearMe: fetched " + res.size + " building(s) within a range of: " + range)
    val listJson = convertJson(res)
    val buildings = new java.util.ArrayList[JsValue]()
    for (building <- listJson) {
      try {
        buildings.add(building.as[JsObject] - "co_owners" - __geometry - "owner_id" - "_id")
      } catch {
        case e: IOException =>
      }
    }
    buildings.toList
  }

  override def dumpRssLogEntriesSpatial(outFile: FileOutputStream, bbox: Array[GeoPoint], floor_number: String): Long = {
    val writer = new PrintWriter(outFile)
    val floorLimit = 100000
    val queryLimit = 5000
    var totalFetched = 0
    var floorFetched = 0
    val collection = mdb.getCollection("fingerprintsWifi")
    val fingerprints = collection.find(geoWithinBox("geometry", bbox(0).dlat, bbox(0).dlon, bbox(1).dlat,
      bbox(1).dlon)).limit(queryLimit)
    val awaited = Await.result(fingerprints.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    for (rss <- listJson) {
      if (floorFetched > floorLimit)
        break
      totalFetched += 1
      if ((rss \ "floor").as[String] == floor_number) {
        floorFetched += 1
        val measurements = (rss \ "measurements").as[List[List[String]]]
        for (measurement <- measurements) {
          writer.println(RadioMapRaw.toRawRadioMapRecord(unrollFingerprint(rss, measurement)))
        }
      }
    }
    LPLogger.info("total fetched: " + totalFetched)

    writer.flush()
    writer.close()

    floorFetched
  }

  override def dumpRssLogEntriesWithCoordinates(floor_number: String, lat: Double, lon: Double): String = {
    val collection = mdb.getCollection("floorplans")
    val query = BsonDocument("floor_number" -> floor_number)
    val floorLookUp = collection.find(query)
    val awaited = Await.result(floorLookUp.toFuture, Duration.Inf)
    val res = awaited.asInstanceOf[List[Document]]
    val floorplans = convertJson(res)
    var unique = 0
    var uniqBuid = ""
    for (floorplan <- floorplans) {
      val lat1 = (floorplan \ "bottom_left_lat")
      val lon1 = (floorplan \ "bottom_left_lng")
      val lat2 = (floorplan \ "top_right_lat")
      val lon2 = (floorplan \ "top_right_lng")
      if (lat1.toOption.isDefined && lon1.toOption.isDefined && lat2.toOption.isDefined && lon2.toOption.isDefined) {
        if (lat1.as[String].toDouble <= lat && lat <= lat2.as[String].toDouble &&
          lon1.as[String].toDouble <= lon && lon <= lon2.as[String].toDouble) {
          unique += 1
          uniqBuid = (floorplan \ "buid").as[String]
        }
      }
    }
    if (unique == 1)
      return uniqBuid
    return null
  }

  override def dumpRssLogEntriesByBuildingFloor(outFile: FileOutputStream, buid: String, floor_number: String): Long = {
    val writer = new PrintWriter(outFile)
    var totalFetched = 0

    val collection = mdb.getCollection("fingerprintsWifi")
    val query = BsonDocument("buid" -> buid, "floor" -> floor_number)
    val fingerprintLookUp = collection.find(query)
    val awaited = Await.result(fingerprintLookUp.toFuture, Duration.Inf)
    val res = awaited.asInstanceOf[List[Document]]
    val rssLog = convertJson(res)
    // splitting Measurements[MAC, rss] to buid, floor, .., MAC, rss, ... (old form)
    if (rssLog.size > 0) {
      for (rss <- rssLog) {
        val measurements = (rss \ "measurements").as[List[List[String]]]
        for (measurement <- measurements) {
          writer.println(RadioMapRaw.toRawRadioMapRecord(unrollFingerprint(rss, measurement)))
          totalFetched += 1
          if (totalFetched == 10000) {
            writer.flush()
            writer.close()
            return totalFetched
          }
        }
      }
    }
    writer.flush()
    writer.close()
    totalFetched
  }

  override def dumpRssLogEntriesByBuildingACCESFloor(outFile: FileOutputStream, buid: String, floor_number: String): Long = ???

  override def getAllAccounts(): List[JsValue] = {
    val collection = mdb.getCollection("users")
    val users = collection.find()
    val awaited = Await.result(users.toFuture, Duration.Inf)
    val res = awaited.toList
    LPLogger.debug(s"Res on complete Length:${res.length}")
    val usersList = convertJson(res)
    val ret = new util.ArrayList[JsValue]()
    for (user <- usersList) {
      ret.add(cleanupMongoJson(user))
    }
    ret.toList
  }

  override def predictFloor(algo: IAlgo, bbox: Array[GeoPoint], strongestMACs: Array[String]): Boolean = ???

  override def deleteRadiosInBox(): Boolean = ???

  override def magneticPathsByBuildingFloorAsJson(buid: String, floor_number: String): java.util.List[JsonObject] = ???

  override def magneticPathsByBuildingAsJson(buid: String): java.util.List[JsonObject] = ???

  override def magneticMilestonesByBuildingFloorAsJson(buid: String, floor_number: String): java.util.List[JsonObject] = ???

  override def BuildingSetsCuids(cuid: String): Boolean = {
    if (getBuildingSet(cuid).size > 1)
      return true
    false
  }

  override def getBuildingSet(cuid: String): List[JsValue] = {
    val collection = mdb.getCollection("campuses")
    val query: BsonDocument = BsonDocument("cuid" -> cuid)
    val campus = collection.find(query)
    val awaited = Await.result(campus.toFuture, Duration.Inf)
    val res = awaited.toList
    convertJson(res)
  }

  override def getAllBuildingsetsByOwner(owner_id: String): List[JsValue] = {
    val collection = mdb.getCollection("campuses")
    val query: BsonDocument = BsonDocument("owner_id" -> owner_id)
    val campus = collection.find(query)
    val awaited = Await.result(campus.toFuture, Duration.Inf)
    val res = awaited.toList
    convertJson(res)
  }

  override def deleteNotValidDocuments(): Boolean = ???
}

