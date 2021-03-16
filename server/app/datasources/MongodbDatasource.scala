package datasources

import java.io.{FileOutputStream, IOException}
import java.util

import com.couchbase.client.java.document.json.JsonObject
import datasources.MongodbDatasource.mdb
import db_models.Poi
import floor_module.IAlgo
import org.mongodb.scala._
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Aggregates
import org.mongodb.scala.model.Aggregates.project
import org.mongodb.scala.model.Filters.{and, equal}
import play.Play
import play.api.libs.json.{JsObject, JsValue, Json}
import play.twirl.api.TemplateMagic.javaCollectionToScala
import utils.{GeoPoint, LPLogger}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object MongodbDatasource {
  private var sInstance: MongodbDatasource = null
  private var mdb: MongoDatabase = null
  var _SCHEMA:Int = 0

  def getMDB: MongoDatabase = mdb

  def getStaticInstance: MongodbDatasource = {
    val conf =  Play.application().configuration()
    val username = conf.getString("mongodb.app.username")
    val password = conf.getString("mongodb.app.password")
    val hostname = conf.getString("mongodb.hostname")
    val port = conf.getString("mongodb.port")
    val database = conf.getString("mongodb.database")
    sInstance = createInstance(hostname, database, username, password, port)
    sInstance
  }

  def createInstance( hostname: String, database: String, username: String, password: String,port: String): MongodbDatasource = {
    val uri: String = "mongodb://" + username + ":" + password + "@" + hostname + ":" + port
    val mongoClient: MongoClient = MongoClient(uri)
    // TODO check if database anyplace exists
    mdb = mongoClient.getDatabase(database)
    val collections = mdb.listCollectionNames()
    val awaited = Await.result(collections.toFuture, Duration.Inf)
    val res = awaited.toList
    LPLogger.info("MongoDB: Connected to: " + hostname + ":" + port)
    LPLogger.debug("Collections = " + res)
//    if (res.size != 7)
//      return null
//    for (colName <- mdb.listCollectionNames()) {
//      if (colName != "buildings" && colName != "campuses" && colName != "edges" && colName != "fingerprintsWifi"
//        && colName != "floorplans" && colName != "pois" && colName != "users") {
//          notFound = true
//        LPLogger.info(colName + " is problematic")
//      }
//    }
    new MongodbDatasource()
  }

}

// TODO getAllAccounts()

class MongodbDatasource() extends IDatasource {

  private def connect(): Boolean = {
    //    LPLogger.info("Mongodb: connecting to: " + mHostname + ":" + mPort + " bucket[" +
    //      mBucket + "]")
    false
  }

  override def getAllPoisTypesByOwner(owner_id: String): java.util.List[JsonObject] = ???

  override def poisByBuildingIDAsJson(buid: String): java.util.List[JsonObject] = ???

  override def poisByBuildingAsJson2(cuid: String, letters: String): java.util.List[JsonObject] = ???

  override def poisByBuildingAsJson2GR(cuid: String, letters: String): java.util.List[JsonObject] = ???

  override def poisByBuildingAsJson3(buid: String, letters: String): java.util.List[JsonObject] = ???

  override def init(): Boolean = ???

  def addJsonDocument(key: String, expiry: Int, document: String): Boolean = ???

  override def addJsonDocument(col: String, document: String, key: String): Boolean = {
    val collection = mdb.getCollection(col)
    val addJson = collection.insertOne(Document.apply(document))
    val awaited = Await.result(addJson.toFuture, Duration.Inf)
    val res = awaited.toString
    true
  }

  override def replaceJsonDocument(key: String, expiry: Int, document: String): Boolean = ???

  override def replaceJsonDocument(col: String, key: String, document: String): Boolean = {
    val collection = mdb.getCollection(col)
    val query = BsonDocument("buid" -> key)
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

  override def deleteFromKey(col: String, key: String): Boolean = {

    false
  }

  override def getFromKey(key: String) = ???

  override def getFromKey(sCollection: String, key: String): Document = {
    val collection = mdb.getCollection(sCollection)
    val buildingLookUp = collection.find(equal("buid", key)).first()
    val awaited = Await.result(buildingLookUp.toFuture, Duration.Inf)
    val res = awaited.asInstanceOf[Document]
    res
  }

  override def getFromKeyAsJson(key: String) = ???

  override def getFromKeyAsJson(collection: String,key: String): JsValue = {
    if (key == null || key.trim().isEmpty) {
      throw new IllegalArgumentException("No null or empty string allowed as key!")
    }
    val db_res = getFromKey(collection, key)
    if (db_res == null) {
      return null
    }
    try {
      convertJson(db_res)
    } catch {
      case e: IOException => {
        LPLogger.error("CouchbaseDatasource::getFromKeyAsJson():: Could not convert document from Couchbase into JSON!")
        null
      }
    }
  }

  override def buildingFromKeyAsJson(key: String): JsValue = {
    LPLogger.D1("buildingFromKeyAsJson::"+key)
    var building = getFromKeyAsJson("buildings", key)
    if (building == null) {
      return null
    }
    building = building.as[JsObject] - "_id" - "_schema"
    val floors = new java.util.ArrayList[JsValue]()
    for (f <- floorsByBuildingAsJson(key)) {
      floors.add(f)
    }

    // ignore pois with pois_type = "None"
    val pois = new java.util.ArrayList[JsValue]()
    for (p <- poisByBuildingAsJson(key)) {
      if (!(p\"pois_type").toString.equalsIgnoreCase(Poi.POIS_TYPE_NONE))
        pois.add(p)
    }
    building.as[JsObject] + ("floors" -> Json.toJson(floors.toList)) +
      ("pois" -> Json.toJson(pois.toList))

  }

  override def poiFromKeyAsJson(key: String): JsValue = ???

  override def poisByBuildingFloorAsJson(buid: String, floor_number: String): java.util.List[JsonObject] = ???

  override def poisByBuildingFloorAsMap(buid: String, floor_number: String): java.util.List[java.util.HashMap[String, String]] = ???

  override def poisByBuildingAsJson(buid: String): java.util.List[JsValue] = {
    val collection = mdb.getCollection("pois")
    val poisLookUp = collection.find(equal("buid", buid))
    val awaited = Await.result(poisLookUp.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val pois = new java.util.ArrayList[JsValue]()
    for (floor <- listJson) {
      try {
        pois.add(floor.as[JsObject] - "owner_id" - "geometry" - "_id")
      } catch {
        case e: IOException =>
      }
    }
    pois
  }

  override def poisByBuildingAsMap(buid: String): java.util.List[java.util.HashMap[String, String]] = ???

  override def floorsByBuildingAsJson(buid: String): java.util.List[JsValue] = {
    val collection = mdb.getCollection("floorplans")
    val floorLookUp = collection.find(equal("buid", buid))
    val awaited = Await.result(floorLookUp.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val floors = new java.util.ArrayList[JsValue]()
    for (floor <- listJson) {
      try {
        floors.add(floor.as[JsObject] - "owner_id" - "_id")
      } catch {
        case e: IOException =>
      }
    }
    floors
  }

  override def connectionsByBuildingAsJson(buid: String): java.util.List[JsonObject] = ???

  override def connectionsByBuildingAsMap(buid: String): java.util.List[java.util.HashMap[String, String]] = ???

  override def connectionsByBuildingFloorAsJson(buid: String, floor_number: String): java.util.List[JsonObject] = ???

  override def connectionsByBuildingAllFloorsAsJson(buid: String): java.util.List[JsonObject] = ???

  override def deleteAllByBuilding(buid: String) {
    val query = BsonDocument("buid" -> buid)
    val collections = List("buildings", "floorplans", "edges", "pois", "fingerprints")
    for (col <- collections) {
      val collection = mdb.getCollection(col)
      val objects = collection.findOneAndDelete(query)
      val awaited = Await.result(objects.toFuture, Duration.Inf)
      val res = awaited
      LPLogger.debug("from collection: " + col + " deleted: " + res)
    }
  }

  override def deleteAllByFloor(buid: String, floor_number: String): java.util.List[String] = ???

  override def deleteAllByConnection(cuid: String): java.util.List[String] = ???

  override def deleteAllByPoi(puid: String): java.util.List[String] = ???

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
      val temp:JsValue = x
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

  override def getAPsByBuildingFloor(buid: String, floor: String): java.util.List[JsonObject] = ???

  override def deleteAllByXsYs(buid: String, floor: String, x: String, y: String): java.util.List[String] = ???

  override def getFingerPrintsBBox(buid: String, floor: String, lat1: String, lon1: String, lat2: String, lon2: String): java.util.List[JsonObject] = ???

  override def getFingerPrintsTimestampBBox(buid: String, floor: String, lat1: String, lon1: String, lat2: String, lon2: String, timestampX: String, timestampY: String): java.util.List[JsonObject] = ???

  override def getFingerPrintsTime(buid: String, floor: String): java.util.List[JsonObject] = ???

  override def getRadioHeatmapByBuildingFloor2(lat: String, lon: String, buid: String, floor: String, range: Int): java.util.List[JsonObject] = ???

  override def getRadioHeatmapBBox(lat: String, lon: String, buid: String, floor: String, range: Int): java.util.List[JsonObject] = ???

  override def getRadioHeatmapBBox2(lat: String, lon: String, buid: String, floor: String, range: Int): java.util.List[JsonObject] = ???


  override def getAllBuildings(): List[JsValue] = {
    LPLogger.debug("mongodb getAllBuildings")
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
      newJson = x.as[JsObject] - "geometry" - "owner_id" - "co_owners" - "_id"
      newList.add(newJson)
    }
    newList.toList
  }

  override def getAllBuildingsByOwner(oid: String): List[JsValue] = {
    val collection = mdb.getCollection("buildings")
    val buildingLookUp = collection.find(equal("owner_id", oid))
    val awaited = Await.result(buildingLookUp.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val buildings = new java.util.ArrayList[JsValue]()
    for (building <- listJson) {
      try {
        buildings.add(building.as[JsObject] - "co_owners" - "geometry" - "owner_id" - "_id" - "_schema")
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
        buildings.add(building.as[JsObject] - "co_owners" - "geometry" - "owner_id" - "_id")
      } catch {
        case e: IOException =>
      }
    }
    buildings.toList
  }

  override def getBuildingByAlias(alias: String): JsonObject = ???

  override def getAllBuildingsNearMe(oid: String, lat: Double, lng: Double): java.util.List[JsonObject] = {
    val bbox = GeoPoint.getGeoBoundingBox(lat, lng, 50)

    val collection = mdb.getCollection("buildings")
    val buildingLookUp = collection.find(and(equal("coordinates_lat", lat),equal("coordinates_lon", lng)))
    val awaited = Await.result(buildingLookUp.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val buildings = new java.util.ArrayList[JsValue]()
    for (building <- listJson) {
      try {
        buildings.add(building.as[JsObject] - "co_owners" - "geometry" - "owner_id" - "_id")
      } catch {
        case e: IOException =>
      }
    }
    buildings.toList
    null
  }

  override def dumpRssLogEntriesSpatial(outFile: FileOutputStream, bbox: Array[GeoPoint], floor_number: String): Long = ???

  override def dumpRssLogEntriesByBuildingFloor(outFile: FileOutputStream, buid: String, floor_number: String): Long = ???

  override def dumpRssLogEntriesByBuildingACCESFloor(outFile: FileOutputStream, buid: String, floor_number: String): Long = ???

  override def getAllAccounts(): List[JsValue] = {
    LPLogger.debug("mongodb getAllAccounts: ")
    val collection = mdb.getCollection("users")
    val users = collection.find()
    val awaited = Await.result(users.toFuture, Duration.Inf)
    val res = awaited.toList
    LPLogger.debug(s"Res on complete Length:${res.length}")
    convertJson(res)
  }

  override def predictFloor(algo: IAlgo, bbox: Array[GeoPoint], strongestMACs: Array[String]): Boolean = ???

  override def deleteRadiosInBox(): Boolean = ???

  override def magneticPathsByBuildingFloorAsJson(buid: String, floor_number: String): java.util.List[JsonObject] = ???

  override def magneticPathsByBuildingAsJson(buid: String): java.util.List[JsonObject] = ???

  override def magneticMilestonesByBuildingFloorAsJson(buid: String, floor_number: String): java.util.List[JsonObject] = ???

  override def BuildingSetsCuids(cuid: String): Boolean = ???

  override def getBuildingSet(cuid: String): java.util.List[JsonObject] = ???

  override def getAllBuildingsetsByOwner(owner_id: String): java.util.List[JsonObject] = ???

  override def deleteNotValidDocuments(): Boolean = ???

  def convertJson(list: List[Document]): List[JsValue] = {
    val jsList = ListBuffer[JsValue]()
    for (doc <- list) {
      jsList.append(convertJson(doc))
    }
    jsList.toList
  }

  def convertJson(doc: Document) = Json.parse(doc.toJson())
}

