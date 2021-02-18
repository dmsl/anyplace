package datasources

import java.io.FileOutputStream

import com.couchbase.client.java.document.json.JsonObject
import datasources.MongodbDatasource.mdb
import floor_module.IAlgo
import org.mongodb.scala._
import play.Play
import play.api.libs.json.{JsValue, Json}
import utils.{GeoPoint, LPLogger}

import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object MongodbDatasource {
  private var sInstance: MongodbDatasource = null
  private var mdb: MongoDatabase = null

  def getStaticInstance: MongodbDatasource = {
    val conf =  Play.application().configuration()
    val username = conf.getString("mongodb.app.username")
    val password = conf.getString("mongodb.app.password")
    val hostname = conf.getString("mongodb.hostname")
    val port = conf.getString("mongodb.port")
    val database = conf.getString("mongodb.database")
    LPLogger.info("Mongodb: connecting to: " + hostname + ":" + port)
    sInstance = createInstance(hostname, database, username, password, port)
    sInstance
  }

  def createInstance( hostname: String, database: String, username: String, password: String,port: String): MongodbDatasource = {
    val uri: String = "mongodb://" + username + ":" + password + "@" + hostname + ":" + port
    val mongoClient: MongoClient = MongoClient(uri)
    // TODO check if database anyplace exists
    mdb = mongoClient.getDatabase(database)
    // IF database not found
    // create it with collections
    // kill server
    var notFound = false
    for (colName <- mdb.listCollectionNames()) {
      if (colName != "buildings" && colName != "campuses" && colName != "edges" && colName != "fingerprintsWifi"
        && colName != "floorplans" && colName != "pois" && colName != "users") {
          notFound = true
        LPLogger.info(colName + " is problematic")
      }
    }
    if (notFound) {
      // create collections
      LPLogger.info("Not found: TODO collections")
    } else {
      LPLogger.info("Found all collections.")
    }
    new MongodbDatasource()
  }

}

// TODO getAllAccounts()

class MongodbDatasource() extends _IDatasource {

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

  override def addJsonDocument(key: String, expiry: Int, document: String): Boolean = ???

  override def replaceJsonDocument(key: String, expiry: Int, document: String): Boolean = ???

  override def deleteFromKey(key: String): Boolean = ???

  override def getFromKey(key: String): AnyRef = ???

  override def getFromKeyAsJson(key: String): JsonObject = ???

  override def buildingFromKeyAsJson(key: String): JsonObject = ???

  override def poiFromKeyAsJson(key: String): JsonObject = ???

  override def poisByBuildingFloorAsJson(buid: String, floor_number: String): java.util.List[JsonObject] = ???

  override def poisByBuildingFloorAsMap(buid: String, floor_number: String): java.util.List[java.util.HashMap[String, String]] = ???

  override def poisByBuildingAsJson(buid: String): java.util.List[JsonObject] = ???

  override def poisByBuildingAsMap(buid: String): java.util.List[java.util.HashMap[String, String]] = ???

  override def floorsByBuildingAsJson(buid: String): java.util.List[JsonObject] = ???

  override def connectionsByBuildingAsJson(buid: String): java.util.List[JsonObject] = ???

  override def connectionsByBuildingAsMap(buid: String): java.util.List[java.util.HashMap[String, String]] = ???

  override def connectionsByBuildingFloorAsJson(buid: String, floor_number: String): java.util.List[JsonObject] = ???

  override def connectionsByBuildingAllFloorsAsJson(buid: String): java.util.List[JsonObject] = ???

  override def deleteAllByBuilding(buid: String): java.util.List[String] = ???

  override def deleteAllByFloor(buid: String, floor_number: String): java.util.List[String] = ???

  override def deleteAllByConnection(cuid: String): java.util.List[String] = ???

  override def deleteAllByPoi(puid: String): java.util.List[String] = ???

  override def getRadioHeatmap(): java.util.List[JsonObject] = ???

  override def getRadioHeatmapByBuildingFloor(buid: String, floor: String): java.util.List[JsonObject] = ???

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

  override def getAllBuildings(): java.util.List[JsonObject] = ???

  override def getAllBuildingsByOwner(oid: String): java.util.List[JsonObject] = ???

  override def getAllBuildingsByBucode(bucode: String): java.util.List[JsonObject] = ???

  override def getBuildingByAlias(alias: String): JsonObject = ???

  override def getAllBuildingsNearMe(oid: String, lat: Double, lng: Double): java.util.List[JsonObject] = ???

  override def dumpRssLogEntriesSpatial(outFile: FileOutputStream, bbox: Array[GeoPoint], floor_number: String): Long = ???

  override def dumpRssLogEntriesByBuildingFloor(outFile: FileOutputStream, buid: String, floor_number: String): Long = ???

  override def dumpRssLogEntriesByBuildingACCESFloor(outFile: FileOutputStream, buid: String, floor_number: String): Long = ???

  override def getAllAccounts(): ListBuffer[JsValue] = {
    // TODO: Only for admin users
    LPLogger.debug("mongodb getAllAccounts: ")

    val collection = mdb.getCollection("users")

    //val resultado = NewMongo.SequenceCollection.findOneAndUpdate(query,inc("nextId",1))
    //resultado.subscribe(new Observer[Document] {
    //  override def onNext(result: Document): Unit ={}
    //  override def onError(e: Throwable): Unit ={}
    //  override def onComplete(): Unit = {}
    //})
    //val awaitedR = Await.result(resultado.toFuture, Duration.Inf).asInstanceOf[List[Document]](0)
    //
    //val ret = awaitedR.get("nextId").getOrElse(0).asInstanceOf[BsonDouble].intValue();

    val users = collection.find()
    Await.result(users.toFuture, Duration.Inf)
    var usersJsons: ListBuffer[JsValue] = ListBuffer[JsValue]()

    users.subscribe(new Observer[Document] {
      // add on json array
      override def onNext(result: Document): Unit = {
        println(result.toJson())
        usersJsons.append(Json.toJson(result.toJson()))
      }
      // throw error
      override def onError(e: Throwable): Unit = {

      }
      // complete message
      override def onComplete(): Unit = {
        println(usersJsons.length)
      }
    })


    usersJsons

    // TODO: See buildings_all and repeat
    // TODO: save output and total number of accounts, LPL.(resultssize)
    // TODO: Once getAllAccounts done, disable it
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
}

