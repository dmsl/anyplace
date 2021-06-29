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
import datasources.MongodbDatasource.{admins, convertJson, mdb}
import datasources.SCHEMA._
import db_models.RadioMapRaw.unrollFingerprint
import db_models.{Connection, Poi, RadioMapRaw}
import floor_module.IAlgo
import org.mongodb.scala._
import org.mongodb.scala.bson.BsonDocument
import org.mongodb.scala.model.Aggregates
import org.mongodb.scala.model.Aggregates.project
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.model.Sorts.{ascending, orderBy}
import play.Play
import play.api.libs.json.{JsNumber, JsObject, JsString, JsValue, Json}
import play.twirl.api.TemplateMagic.javaCollectionToScala
import utils.JsonUtils.cleanupMongoJson
import utils.{GeoJSONPoint, GeoPoint, JsonUtils, LPLogger}

import scala.collection.JavaConversions.mapAsScalaMap
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.text.Document.break
import scala.util.control.Breaks


object MongodbDatasource {
  val __SCHEMA: Int = 0
  private var sInstance: MongodbDatasource = null
  private var mdb: MongoDatabase = null
  private var admins: List[String] = List[String]()

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
    val collection = mdb.getCollection(SCHEMA.cUsers)
    val query = BsonDocument(SCHEMA.fType -> "admin")
    val adm = collection.find(query)
    val awaited = Await.result(adm.toFuture, Duration.Inf)
    val res = awaited.toList
    val ret = new util.ArrayList[String]
    for (admin <- res) {
      val temp = Json.parse(admin.toJson())
      ret.add((temp \ SCHEMA.fOwnerId).as[String])
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

  var wordsELOT = new java.util.ArrayList[java.util.ArrayList[String]]
  var allPoisSide = new java.util.HashMap[String, java.util.List[JsValue]]()
  var allPoisbycuid = new java.util.HashMap[String, java.util.List[JsValue]]()
  var lastletters = ""

  override def poisByBuildingIDAsJson(buid: String): List[JsValue] = {
    val collection = mdb.getCollection(SCHEMA.cPOIS)
    val query = BsonDocument(SCHEMA.fBuid -> buid)
    val pois = collection.find(query)
    val awaited = Await.result(pois.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val poisArray = new java.util.ArrayList[JsValue]()
    for (poi <- listJson)
      poisArray.add(poi.as[JsObject] - SCHEMA.fOwnerId - SCHEMA.fGeometry - SCHEMA.fId - SCHEMA.fSchema)
    poisArray.toList
  }

  override def poisByBuildingAsJson2(cuid: String, letters: String): List[JsValue] = {
    var pois: java.util.List[JsValue] = null
    val pois2 = new java.util.ArrayList[JsValue]
    pois = allPoisbycuid.get(cuid)
    if (pois == null) {
      val buids = getBuildingSet(cuid)
      val tempPois = new java.util.ArrayList[JsValue]
      for (buid <- (buids(0) \ SCHEMA.fBuids).as[List[String]]) {
        for (poi <- poisByBuildingAsJson(buid)) {
          if (poi != null) {
            tempPois.add(poi)
          }
        }
      }
      pois = tempPois
    }
    val words = letters.split(" ")
    var flag = false
    for (json <- pois) {
      flag = true
      for (w <- words if flag) {
        var name = ""
        var description = ""
        if ((json \ SCHEMA.fName).toOption.isDefined)
          name = (json \ SCHEMA.fName).as[String].toLowerCase
        if ((json \ SCHEMA.fDescription).toOption.isDefined)
          description = (json \ SCHEMA.fDescription).as[String].toLowerCase
        if (!(name.contains(w.toLowerCase) || description.contains(w.toLowerCase)))
          flag = false
      }
      if (flag) pois2.add(json)
    }
    pois2.toList
  }

  override def poisByBuildingAsJson2GR(cuid: String, letters: String): List[JsValue] = {
    var pois = allPoisbycuid.get(cuid)
    if (pois == null) {
      val buids = getBuildingSet(cuid)
      val tempPois = new java.util.ArrayList[JsValue]
      for (buid <- (buids(0) \ SCHEMA.fBuids).as[List[String]]) {
        for (poi <- poisByBuildingAsJson(buid)) {
          if (poi != null) {
            tempPois.add(poi)
          }
        }
      }
      pois = tempPois
    }
    val pois2 = new java.util.ArrayList[JsValue]
    val words = letters.split(" ")
    var flag = false
    var flag2 = false
    var flag3 = false

    if (letters.compareTo(lastletters) != 0) {
      lastletters = letters
      wordsELOT = new java.util.ArrayList[java.util.ArrayList[String]]
      var j = 0
      for (word <- words) {
        wordsELOT.add(greeklishTogreekList(word.toLowerCase))
      }
    }
    for (json <- pois) {
      flag = true
      flag2 = true
      flag3 = true
      var j = 0
      // create a Breaks object as follows
      val loop = new Breaks
      val ex_loop = new Breaks
      ex_loop.breakable {
        for (w <- words) {
          var name = ""
          var description = ""
          if ((json \ SCHEMA.fName).toOption.isDefined)
            name = (json \ SCHEMA.fName).as[String].toLowerCase
          if ((json \ SCHEMA.fDescription).toOption.isDefined)
            description = (json \ SCHEMA.fDescription).as[String].toLowerCase
          if (!(name.contains(w.toLowerCase) || description.contains(w.toLowerCase))) flag = false
          val greeklish = greeklishTogreek(w.toLowerCase)
          if (!(name.contains(greeklish) || description.contains(greeklish))) flag2 = false
          if (wordsELOT.size != 0) {
            var wordsELOT2 = new java.util.ArrayList[String]
            wordsELOT2 = wordsELOT.get({
              j += 1
              j - 1
            })
            if (wordsELOT2.size == 0) flag3 = false
            else {
              loop.breakable {
                for (greeklishELOT <- wordsELOT2) {
                  if (!(name.contains(greeklishELOT) || description.contains(greeklishELOT))) flag3 = false
                  else {
                    flag3 = true
                    loop.break()
                  }
                }
              }
            }
          }
          else flag3 = false
          if (!flag3 && !flag && !flag2) ex_loop.break()
          if (flag || flag2 || flag3) pois2.add(json)
        }
      }
    }

    pois2.toList
  }

  override def getBuildingSet(cuid: String): List[JsValue] = {
    val collection = mdb.getCollection(SCHEMA.cCampuses)
    val query: BsonDocument = BsonDocument(SCHEMA.fCampusCuid -> cuid)
    val campus = collection.find(query)
    val awaited = Await.result(campus.toFuture, Duration.Inf)
    val res = awaited.toList
    convertJson(res)
  }

  override def poisByBuildingAsJson3(buid: String, letters: String): List[JsValue] = {
    LPLogger.info("poisByBuildingAsJson3")
    var pois: java.util.List[JsValue] = null
    val pois2 = new java.util.ArrayList[JsValue]
    if (allPoisSide.get(buid) != null)
      pois = allPoisSide.get(buid)
    else
      pois = poisByBuildingAsJson(buid)

    val words = letters.split(" ")

    var flag = false
    var flag2 = false
    var flag3 = false

    if (letters.compareTo(lastletters) != 0) {
      lastletters = letters
      wordsELOT = new java.util.ArrayList[java.util.ArrayList[String]]
      var j = 0
      for (word <- words) {
        wordsELOT.add(greeklishTogreekList(word.toLowerCase))
      }
    }
    for (json <- pois) {
      flag = true
      flag2 = true
      flag3 = true
      var j = 0
      // create a Breaks object as follows
      val loop = new Breaks

      val ex_loop = new Breaks
      ex_loop.breakable {
        for (w <- words) {
          var name = ""
          var description = ""
          if ((json \ SCHEMA.fName).toOption.isDefined)
            name = (json \ SCHEMA.fName).as[String].toLowerCase
          if ((json \ SCHEMA.fDescription).toOption.isDefined)
            description = (json \ SCHEMA.fDescription).as[String].toLowerCase
          if (!(name.contains(w.toLowerCase) || description.contains(w.toLowerCase))) flag = false
          val greeklish = greeklishTogreek(w.toLowerCase)
          if (!(name.contains(greeklish) || description.contains(greeklish))) flag2 = false
          if (wordsELOT.size != 0) {
            var wordsELOT2 = new java.util.ArrayList[String]
            wordsELOT2 = wordsELOT.get({
              j += 1
              j - 1
            })
            if (wordsELOT2.size == 0) flag3 = false
            else {
              loop.breakable {
                for (greeklishELOT <- wordsELOT2) {
                  if (!(name.contains(greeklishELOT) || description.contains(greeklishELOT))) flag3 = false
                  else {
                    flag3 = true
                    loop.break()
                  }
                }
              }
            }
          }
          else flag3 = false
          if (!flag3 && !flag && !flag2) ex_loop.break()
          if (flag || flag2 || flag3) pois2.add(json)
        }
      }
    }

    pois2.toList
  }

  def greeklishTogreekList(greeklish: String) = {
    val words = new java.util.ArrayList[String]
    words.add("")
    val myChars = greeklish.toCharArray
    var i = 0
    for (i <- 0 until greeklish.length) {
      val size = words.size
      var j = 0
      for (j <- 0 until size) {
        var myword = ""
        myword = words.get(j)
        if (myChars(i) == 'a') words.add(myword + "α")
        else if (myChars(i) == 'b') {
          words.add(myword + "β")
          words.add(myword + "μπ")
        }
        else if (myChars(i) == 'c') {
          if (i < greeklish.length - 1) if (myChars(i + 1) == 'h') words.add(myword + "χ")
          words.add(myword + "γ")
        }
        else if (myChars(i) == 'd') {
          words.add(myword + "δ")
          words.add(myword + "ντ")
        }
        else if (myChars(i) == 'e') {
          words.add(myword + "ε")
          words.add(myword + "αι")
          words.add(myword + "ι")
          words.add(myword + "η")
        }
        else if (myChars(i) == 'f') words.add(myword + "φ")
        else if (myChars(i) == 'g') {
          words.add(myword + "γ")
          words.add(myword + "γγ")
          words.add(myword + "γκ")
        }
        else if (myChars(i) == 'h') {
          if (myword.length > 0 && myword.charAt(myword.length - 1) == 'θ') {
            words.add(myword)

          }
          else if (myword.length > 0 && myword.charAt(myword.length - 1) == 'χ') {
            words.add(myword)

          } else {
            words.add(myword + "χ")
            words.add(myword + "η")
          }
        }
        else if (myChars(i) == 'i') {
          words.add(myword + "ι")
          words.add(myword + "η")
          words.add(myword + "υ")
          words.add(myword + "οι")
          words.add(myword + "ει")
        }
        else if (myChars(i) == 'j') words.add(myword + "ξ")
        else if (myChars(i) == 'k') {
          if (i < greeklish.length - 1) if (myChars(i + 1) == 's') words.add(myword + "ξ")
          words.add(myword + "κ")
        }
        else if (myChars(i) == 'l') words.add(myword + "λ")
        else if (myChars(i) == 'm') words.add(myword + "μ")
        else if (myChars(i) == 'n') words.add(myword + "ν")
        else if (myChars(i) == 'o') {
          words.add(myword + "ο")
          words.add(myword + "ω")
        }
        else if (myChars(i) == 'p') {
          if (i < greeklish.length - 1) if (myChars(i + 1) == 's') words.add(myword + "ψ")
          words.add(myword + "π")
        }
        else if (myChars(i) == 'q') words.add(myword + ";")
        else if (myChars(i) == 'r') words.add(myword + "ρ")
        else if (myChars(i) == 's') {
          if (myword.length > 0 && myword.charAt(myword.length - 1) == 'ξ') {
            words.add(myword)

          } else if (myword.length > 0 && myword.charAt(myword.length - 1) == 'ψ') {
            words.add(myword)
          }
          else {
            words.add(myword + "σ")
            words.add(myword + "ς")
          }
        }
        else if (myChars(i) == 't') {
          if (i < greeklish.length - 1) if (myChars(i + 1) == 'h') words.add(myword + "θ")
          words.add(myword + "τ")
        }
        else if (myChars(i) == 'u') {
          words.add(myword + "υ")
          words.add(myword + "ου")
        }
        else if (myChars(i) == 'v') words.add(myword + "β")
        else if (myChars(i) == 'w') words.add(myword + "ω")
        else if (myChars(i) == 'x') {
          words.add(myword + "χ")
          words.add(myword + "ξ")
        }
        else if (myChars(i) == 'y') words.add(myword + "υ")
        else if (myChars(i) == 'z') words.add(myword + "ζ")

      }

      for (k <- 0 until size) {
        words.remove(0)
      }
    }
    words
  }

  def greeklishTogreek(greeklish: String) = {
    val myChars = greeklish.toCharArray
    var i = 0
    while ( {
      i < greeklish.length
    }) {
      myChars(i) match {
        case 'a' =>
          myChars(i) = 'α'

        case 'b' =>
          myChars(i) = 'β'

        case 'c' =>
          myChars(i) = 'ψ'

        case 'd' =>
          myChars(i) = 'δ'

        case 'e' =>
          myChars(i) = 'ε'

        case 'f' =>
          myChars(i) = 'φ'

        case 'g' =>
          myChars(i) = 'γ'

        case 'h' =>
          myChars(i) = 'η'

        case 'i' =>
          myChars(i) = 'ι'

        case 'j' =>
          myChars(i) = 'ξ'

        case 'k' =>
          myChars(i) = 'κ'

        case 'l' =>
          myChars(i) = 'λ'

        case 'm' =>
          myChars(i) = 'μ'

        case 'n' =>
          myChars(i) = 'ν'

        case 'o' =>
          myChars(i) = 'ο'

        case 'p' =>
          myChars(i) = 'π'

        case 'q' =>
          myChars(i) = ';'

        case 'r' =>
          myChars(i) = 'ρ'

        case 's' =>
          myChars(i) = 'σ'

        case 't' =>
          myChars(i) = 'τ'

        case 'u' =>
          myChars(i) = 'θ'

        case 'v' =>
          myChars(i) = 'ω'

        case 'w' =>
          myChars(i) = 'ς'

        case 'x' =>
          myChars(i) = 'χ'

        case 'y' =>
          myChars(i) = 'υ'

        case 'z' =>
          myChars(i) = 'ζ'

        case _ =>

      }

      {
        i += 1;
        i - 1
      }
    }
    String.valueOf(myChars)
  }

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

  override def deleteFromKey(key: String): Boolean = ???

  override def getFromKey(key: String) = ???

  override def getFromKeyAsJson(key: String) = ???

  override def fingerprintExists(col: String, buid: String, floor: String, x: String, y: String, heading: String): Boolean = {
    val collection = mdb.getCollection(col)
    val query = BsonDocument(SCHEMA.fBuid -> buid, SCHEMA.fFloor -> floor, SCHEMA.fX -> x, SCHEMA.fY -> y, SCHEMA.fHeading -> heading)
    val fingerprintLookUp = collection.find(query)
    val awaited = Await.result(fingerprintLookUp.toFuture, Duration.Inf)
    val res = awaited.asInstanceOf[List[Document]]
    if (res.size > 0)
      return true
    return false
  }

  override def buildingFromKeyAsJson(value: String): JsValue = {
    var building = getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, value)
    if (building == null) {
      return null
    }
    building = building.as[JsObject] - SCHEMA.fId - SCHEMA.fSchema
    val floors = new java.util.ArrayList[JsValue]()
    for (f <- floorsByBuildingAsJson(value)) {
      floors.add(f)
    }

    // ignore pois with pois_type = "None"
    val pois = new java.util.ArrayList[JsValue]()
    for (p <- poisByBuildingAsJson(value)) {
      if (!(p \ SCHEMA.fPoisType).toString.equalsIgnoreCase(Poi.POIS_TYPE_NONE))
        pois.add(p)
    }
    building.as[JsObject] + ("floors" -> Json.toJson(floors.toList)) +
      (SCHEMA.cPOIS -> Json.toJson(pois.toList))
  }

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

  override def poisByBuildingAsJson(buid: String): java.util.List[JsValue] = {
    val collection = mdb.getCollection(SCHEMA.cPOIS)
    val poisLookUp = collection.find(equal(SCHEMA.fBuid, buid))
    val awaited = Await.result(poisLookUp.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val pois = new java.util.ArrayList[JsValue]()
    for (floor <- listJson) {
      pois.add(floor.as[JsObject] - SCHEMA.fOwnerId - SCHEMA.fGeometry - SCHEMA.fId - SCHEMA.fSchema)
    }
    pois
  }

  override def floorsByBuildingAsJson(buid: String): java.util.List[JsValue] = {
    val collection = mdb.getCollection(SCHEMA.cFloorplans)
    val floorLookUp = collection.find(equal(SCHEMA.fBuid, buid))
    val awaited = Await.result(floorLookUp.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val floors = new java.util.ArrayList[JsValue]()
    for (floor <- listJson) {
      try {
        floors.add(floor.as[JsObject] - SCHEMA.fOwnerId - SCHEMA.fId - SCHEMA.fSchema)
      } catch {
        case e: IOException =>
      }
    }
    floors
  }

  override def poiFromKeyAsJson(collection: String, key: String, value: String): JsValue = {
    getFromKeyAsJson(collection, key, value)
  }

  override def poisByBuildingFloorAsMap(buid: String, floor_number: String): java.util.List[java.util.HashMap[String, String]] = {
    val pois = poisByBuildingFloorAsJson(buid, floor_number)
    val result = new java.util.ArrayList[java.util.HashMap[String, String]]()
    for (pois <- pois) {
      result.add(JsonUtils.getHashMapStrStr(pois))
    }
    result
  }

  override def poisByBuildingFloorAsJson(buid: String, floor_number: String): List[JsValue] = {
    val collection = mdb.getCollection(SCHEMA.cPOIS)
    val query = BsonDocument(SCHEMA.fBuid -> buid, SCHEMA.fFloorNumber -> floor_number)
    val pois = collection.find(query)
    val awaited = Await.result(pois.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val poisArray = new java.util.ArrayList[JsValue]()
    for (poi <- listJson)
      poisArray.add(poi.as[JsObject] - SCHEMA.fOwnerId - SCHEMA.fGeometry - SCHEMA.fId - SCHEMA.fSchema)
    poisArray.toList
  }

  override def poiByBuidFloorPuid(buid: String, floor_number: String, puid: String): Boolean = {
    val collection = mdb.getCollection(SCHEMA.cPOIS)
    val query = BsonDocument(SCHEMA.fBuid -> buid, SCHEMA.fFloorNumber -> floor_number, SCHEMA.fPuid -> puid)
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

  override def connectionsByBuildingAsMap(buid: String): java.util.List[java.util.HashMap[String, String]] = {

    val edges = connectionsByBuildingAsJson(buid)
    var hm: util.HashMap[String, String] = null
    val conns = new util.ArrayList[util.HashMap[String, String]]()
    for (edge <- edges) {
      hm = JsonUtils.getHashMapStrStr(edge)
      if (!hm.get(SCHEMA.fEdgeType).equalsIgnoreCase(Connection.EDGE_TYPE_OUTDOOR))
        conns.add(hm)
    }
    conns
  }

  override def connectionsByBuildingAsJson(buid: String): List[JsValue] = {
    val collection = mdb.getCollection(SCHEMA.cEdges)
    val query = BsonDocument(SCHEMA.fBuid -> buid)
    val edges = collection.find(query)
    val awaited = Await.result(edges.toFuture, Duration.Inf)
    val res = awaited.toList
    convertJson(res)
  }

  override def connectionsByBuildingFloorAsJson(buid: String, floor_number: String): List[JsValue] = {
    val collection = mdb.getCollection(SCHEMA.cEdges)
    val query = BsonDocument(SCHEMA.fBuid -> buid, SCHEMA.fFloorA -> floor_number, SCHEMA.fFloorB -> floor_number)
    val edges = collection.find(query)
    val awaited = Await.result(edges.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val edgesArray = new java.util.ArrayList[JsValue]()
    for (poi <- listJson)
      edgesArray.add(poi.as[JsObject] - SCHEMA.fOwnerId - SCHEMA.fId - SCHEMA.fSchema)
    edgesArray.toList
  }

  override def connectionsByBuildingAllFloorsAsJson(buid: String): List[JsValue] = {
    val collection = mdb.getCollection(SCHEMA.cEdges)
    val query = BsonDocument(SCHEMA.fBuid -> buid)
    val edges = collection.find(query)
    val awaited = Await.result(edges.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val edgesArray = new java.util.ArrayList[JsValue]()
    for (poi <- listJson)
      edgesArray.add(poi.as[JsObject] - SCHEMA.fOwnerId - SCHEMA.fId - SCHEMA.fSchema)
    edgesArray.toList
  }

  override def deleteAllByBuilding(buid: String): Boolean = {
    LPLogger.debug("Cascading building " + buid)
    var ret = true

    // deleting floors along with pois, edges
    val floors = floorsByBuildingAsJson(buid)
    for (floor <- floors) {
      if ((floor \ SCHEMA.fFloorNumber).toOption.isDefined) {
        val tempBool = deleteAllByFloor(buid, (floor \ SCHEMA.fFloorNumber).as[String])
        ret = ret && tempBool
      }
    }

    // deleting building
    var query = BsonDocument(SCHEMA.fBuid -> buid)
    var collection = mdb.getCollection(SCHEMA.cSpaces)
    val objects = collection.deleteOne(query)
    val awaited = Await.result(objects.toFuture, Duration.Inf)
    val res = awaited
    val bool = res.wasAcknowledged()

    // deleting buid from campus buids[]
    query = BsonDocument(SCHEMA.fBuids -> buid)
    collection = mdb.getCollection(SCHEMA.cCampuses)
    val campuses = collection.find(query)
    var await = Await.result(campuses.toFuture, Duration.Inf)
    var re = await.toList
    val camp = convertJson(re)
    for (c <- camp) {
      val newBuids: util.ArrayList[String] = new util.ArrayList[String]()
      val buids = (c \ SCHEMA.fBuids).as[List[String]]
      for (buid2 <- buids) {
        if (buid2 != buid)
          newBuids.add(buid2)
      }
      val newCamp: String = (c.as[JsObject] + (SCHEMA.fBuids -> Json.toJson(newBuids.toList))).toString()
      ret = ret && replaceJsonDocument(SCHEMA.cCampuses, SCHEMA.fCampusCuid, (c \ SCHEMA.fCampusCuid).as[String], newCamp)
    }

    // deleting fingerprints
    // requires testing
    query = BsonDocument(SCHEMA.fBuid -> buid)
    collection = mdb.getCollection(SCHEMA.cFingerprintsWifi)
    val deleted = collection.deleteMany(query)
    val delAwaited = Await.result(deleted.toFuture, Duration.Inf)
    val delRes = delAwaited
    val bool1 = delRes.wasAcknowledged()
    LPLogger.debug("fingerprints with buid " + buid + " " + delRes.toString)
    ret = ret && bool1

    (ret && bool)
  }

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

  override def deleteAllByFloor(buid: String, floor_number: String): Boolean = {
    LPLogger.debug("deleteAllByFloor::")
    LPLogger.debug("Cascade deletion: edges reaching this floor, edges of this floor," +
      " pois of this floor, floorplan(mongoDB), floorplan image(server)")
    var collection = mdb.getCollection(SCHEMA.cEdges)

    // queryBuidA deletes edges that start from building buid (containing edges that have buid_b == buid_a)
    val queryBuidA = BsonDocument(SCHEMA.fBuidA -> buid, SCHEMA.fFloorA -> floor_number)
    var deleted = collection.deleteMany(queryBuidA)
    var awaited = Await.result(deleted.toFuture, Duration.Inf)
    var res = awaited
    val bool1 = res.wasAcknowledged()
    LPLogger.debug("edges from buid_a: " + buid + " " + res.toString)

    // queryBuidB deletes edges that point to building buid
    val queryBuidB = BsonDocument(SCHEMA.fBuidB -> buid, SCHEMA.fFloorB -> floor_number)
    deleted = collection.deleteMany(queryBuidB)
    awaited = Await.result(deleted.toFuture, Duration.Inf)
    res = awaited
    val bool2 = res.wasAcknowledged()
    LPLogger.debug("edges to buid_b: " + buid + " " + res.toString)

    // this query will delete the pois of the floor
    val queryFloor = BsonDocument(SCHEMA.fBuid -> buid, SCHEMA.fFloorNumber -> floor_number)
    collection = mdb.getCollection(SCHEMA.cPOIS)
    deleted = collection.deleteMany(queryFloor)
    awaited = Await.result(deleted.toFuture, Duration.Inf)
    res = awaited
    val bool3 = res.wasAcknowledged()
    LPLogger.debug("pois in building with buid: " + buid + " " + res.toString)

    // this query will delete the floor it self
    collection = mdb.getCollection(SCHEMA.cFloorplans)
    deleted = collection.deleteMany(queryFloor)
    awaited = Await.result(deleted.toFuture, Duration.Inf)
    res = awaited
    val bool4 = res.wasAcknowledged()
    LPLogger.debug("floorplan with buid " + buid + " " + res.toString)
    bool1 && bool2 && bool3 && bool4
  }

  override def deleteAllByConnection(cuid: String): java.util.List[String] = {
    val all_items_failed = new util.ArrayList[String]()
    if (!this.deleteFromKey(SCHEMA.cEdges, SCHEMA.fConCuid, cuid)) {
      all_items_failed.add(cuid)
    }
    all_items_failed
  }

  // ASK:PM
  override def deleteAllByPoi(puid: String): java.util.List[String] = {
    val all_items_failed = new util.ArrayList[String]()
    if (!this.deleteFromKey(SCHEMA.cEdges, SCHEMA.fPoisA, puid)) {
      all_items_failed.add(puid)
    }
    if (!this.deleteFromKey(SCHEMA.cEdges, SCHEMA.fPoisB, puid)) {
      all_items_failed.add(puid)
    }
    if (!this.deleteFromKey(SCHEMA.cPOIS, SCHEMA.fPuid, puid)) {
      all_items_failed.add(puid)
    }
    all_items_failed
  }

  override def deleteFromKey(col: String, key: String, value: String): Boolean = {
    val collection = mdb.getCollection(col)
    val query = BsonDocument(key -> value)
    val deleted = collection.deleteOne(query)
    val awaited = Await.result(deleted.toFuture, Duration.Inf)
    val res = awaited
    res.wasAcknowledged()
  }

  override def getRadioHeatmap(): java.util.List[JsonObject] = ???

  override def getRadioHeatmapByBuildingFloor(buid: String, floor: String): List[JsValue] = {
    val collection = mdb.getCollection(SCHEMA.cHeatmapWifi3)
    val query = BsonDocument(SCHEMA.fBuid -> buid, SCHEMA.fFloor -> floor)
    val radioPoints = collection.aggregate(Seq(
      Aggregates.filter(query),
      project(
        Document(SCHEMA.fLocation -> "$location", "count" -> "$count")
      )))
    val awaited = Await.result(radioPoints.toFuture, Duration.Inf)
    val res = awaited.toList
    val heatmaps = new util.ArrayList[JsValue]()
    for (heatmap <- convertJson(res)) {
      val count = (heatmap \ "count").as[Int]
      heatmaps.add(Json.obj(SCHEMA.fX -> JsString((heatmap \ SCHEMA.fLocation \ SCHEMA.fCoordinates).as[List[Double]].head + ""),
        SCHEMA.fY -> JsString((heatmap \ SCHEMA.fLocation \ SCHEMA.fCoordinates).as[List[Double]].tail.head + ""),
        "w" -> JsString(count + "")))
    }
    return heatmaps.toList
  }

  override def getRadioHeatmapByBuildingFloorAverage1(buid: String, floor: String): List[JsValue] = {
    val collection = mdb.getCollection(SCHEMA.cHeatmapWifi1)
    val query = BsonDocument(SCHEMA.fBuid -> buid, SCHEMA.fFloor -> floor)
    val radioPoints = collection.aggregate(Seq(
      Aggregates.filter(query),
      project(
        Document(SCHEMA.fLocation -> "$location", "sum" -> "$sum", "count" -> "$count")
      )))
    val awaited = Await.result(radioPoints.toFuture, Duration.Inf)
    val res = awaited.toList

    var foundHeatmaps = convertJson(res)
    LPLogger.debug("size = " + foundHeatmaps.size)
    if (foundHeatmaps.size == 0) {
      foundHeatmaps = generateHeatmapsOnFly(SCHEMA.cHeatmapWifi1, buid, floor, query, 1, false)
      if (foundHeatmaps == null)
        return null
    }

    val heatmaps = new util.ArrayList[JsValue]()
    for (heatmap <- foundHeatmaps) {
      val count = (heatmap \ "count").as[Int]
      val sum = (heatmap \ "sum").as[Int]
      val average = sum.toDouble / count.toDouble
      heatmaps.add(Json.obj(SCHEMA.fX -> (heatmap \ SCHEMA.fLocation \ SCHEMA.fCoordinates).as[List[Double]].head,
        SCHEMA.fY -> (heatmap \ SCHEMA.fLocation \ SCHEMA.fCoordinates).as[List[Double]].tail.head,
        "w" -> JsString("{\"count\":" + count + ",\"average\":" + average + ",\"total\":" + sum + "}")))
    }
    return heatmaps.toList
  }

  override def getRadioHeatmapByBuildingFloorAverage2(buid: String, floor: String): List[JsValue] = {
    val collection = mdb.getCollection(SCHEMA.cHeatmapWifi2)
    val query = BsonDocument(SCHEMA.fBuid -> buid, SCHEMA.fFloor -> floor)
    val radioPoints = collection.aggregate(Seq(
      Aggregates.filter(query),
      project(
        Document(SCHEMA.fLocation -> "$location", "sum" -> "$sum", "count" -> "$count", "average" -> "$average")
      )))
    val awaited = Await.result(radioPoints.toFuture, Duration.Inf)
    val res = awaited.toList

    var foundHeatmaps = convertJson(res)
    if (foundHeatmaps.size == 0) {
      foundHeatmaps = generateHeatmapsOnFly(SCHEMA.cHeatmapWifi2, buid, floor, query, 2, false)
      if (foundHeatmaps == null)
        return null
    }

    val heatmaps = new util.ArrayList[JsValue]()
    for (heatmap <- foundHeatmaps) {
      val count = (heatmap \ "count").as[Int]
      val sum = (heatmap \ "sum").as[Int]
      val average = sum.toDouble / count.toDouble
      heatmaps.add(Json.obj(SCHEMA.fX -> (heatmap \ SCHEMA.fLocation \ SCHEMA.fCoordinates).as[List[Double]].head,
        SCHEMA.fY -> (heatmap \ SCHEMA.fLocation \ SCHEMA.fCoordinates).as[List[Double]].tail.head,
        "w" -> JsString("{\"count\":" + count + ",\"average\":" + average + ",\"total\":" + sum + "}")))
    }
    return heatmaps.toList
  }

  override def getRadioHeatmapByBuildingFloorAverage3(buid: String, floor: String): List[JsValue] = {
    val collection = mdb.getCollection(SCHEMA.cHeatmapWifi3)
    val query = BsonDocument(SCHEMA.fBuid -> buid, SCHEMA.fFloor -> floor)
    var radioPoints = collection.aggregate(Seq(
      Aggregates.filter(query),
      project(
        Document(SCHEMA.fLocation -> "$location", "sum" -> "$sum", "count" -> "$count", "average" -> "$average")
      )))
    val awaited = Await.result(radioPoints.toFuture, Duration.Inf)
    val res = awaited.toList
    var foundHeatmaps = convertJson(res)
    // in case there are 0 heatmaps try to generate them
    if (foundHeatmaps.size == 0) {
      foundHeatmaps = generateHeatmapsOnFly(SCHEMA.cHeatmapWifi3, buid, floor, query, 3, false)
      if (foundHeatmaps == null) return null
    }

    val heatmaps = new util.ArrayList[JsValue]()
    for (heatmap <- foundHeatmaps) {
      val count = (heatmap \ "count").as[Int]
      val sum = (heatmap \ "sum").as[Int]
      val average = sum.toDouble / count.toDouble
      // TODO: dont put a json in a string, e.g. and handle in js
      //heatmaps.add(Json.obj(SCHEMA.fLocation -> (heatmap \ SCHEMA.fLocation \ SCHEMA.fCoordinates).as[List[Double]],
      //  "count" -> count, "sum" -> sum, "average" -> average))
      heatmaps.add(Json.obj(SCHEMA.fX -> (heatmap \ SCHEMA.fLocation \ SCHEMA.fCoordinates).as[List[Double]].head,
        SCHEMA.fY -> (heatmap \ SCHEMA.fLocation \ SCHEMA.fCoordinates).as[List[Double]].tail.head,
        "w" -> JsString("{\"count\":" + count + ",\"average\":" + average + ",\"total\":" + sum + "}")))
    }
    return heatmaps.toList
  }

  override def getRadioHeatmapByFloorTimestamp(buid: String, floor: String, timestampX: String, timestampY: String): List[JsValue] = {
    val collection = mdb.getCollection(SCHEMA.cHeatmapWifiTimestamp3)

    // TODO: Get count from collection and building
    val radioPoints = collection.find(and(lt(SCHEMA.fTimestamp, timestampY), gt(SCHEMA.fTimestamp, timestampX),
      equal(SCHEMA.fBuid, buid), equal(SCHEMA.fFloor, floor)))
    val awaited = Await.result(radioPoints.toFuture, Duration.Inf)
    val res = awaited.toList
    val heatmaps = new util.ArrayList[JsValue]()
    for (heatmap <- convertJson(res)) {
      val count = (heatmap \ "count").as[Int]
      val sum = (heatmap \ "sum").as[Int]
      val average = sum.toDouble / count.toDouble
      heatmaps.add(Json.obj(SCHEMA.fX -> (heatmap \ SCHEMA.fLocation \ SCHEMA.fCoordinates).as[List[Double]].head,
        SCHEMA.fY -> (heatmap \ SCHEMA.fLocation \ SCHEMA.fCoordinates).as[List[Double]].tail.head,
        "w" -> JsString("{\"count\":" + count + ",\"average\":" + average + ",\"total\":" + sum + "}")))
    }
    return heatmaps.toList
  }

  override def getRadioHeatmapByBuildingFloorTimestampAverage1(buid: String, floor: String, timestampX: String, timestampY: String): List[JsValue] = {
    val collection = mdb.getCollection(SCHEMA.cHeatmapWifiTimestamp1)
    val radioPoints = collection.find(and(lt(SCHEMA.fTimestamp, timestampY), gt(SCHEMA.fTimestamp, timestampX),
      equal(SCHEMA.fBuid, buid), equal(SCHEMA.fFloor, floor)))
    val awaited = Await.result(radioPoints.toFuture, Duration.Inf)
    val res = awaited.toList
    val heatmaps = new util.ArrayList[JsValue]()
    for (heatmap <- convertJson(res)) {
      val count = (heatmap \ "count").as[Int]
      val sum = (heatmap \ "sum").as[Int]
      val average = sum.toDouble / count.toDouble
      heatmaps.add(Json.obj(SCHEMA.fX -> (heatmap \ SCHEMA.fLocation \ SCHEMA.fCoordinates).as[List[Double]].head,
        SCHEMA.fY -> (heatmap \ SCHEMA.fLocation \ SCHEMA.fCoordinates).as[List[Double]].tail.head,
        "w" -> JsString("{\"count\":" + count + ",\"average\":" + average + ",\"total\":" + sum + "}")))
    }
    return heatmaps.toList
  }

  override def getRadioHeatmapByBuildingFloorTimestampAverage2(buid: String, floor: String, timestampX: String, timestampY: String): List[JsValue] = {
    LPLogger.debug("getRadioHeatmapByBuildingFloorTimestampAverage2")
    val collection = mdb.getCollection(SCHEMA.cHeatmapWifiTimestamp2)
    val radioPoints = collection.find(and(lt(SCHEMA.fTimestamp, timestampY), gt(SCHEMA.fTimestamp, timestampX),
      equal(SCHEMA.fBuid, buid), equal(SCHEMA.fFloor, floor)))
    val awaited = Await.result(radioPoints.toFuture, Duration.Inf)
    val res = awaited.toList
    val heatmaps = new util.ArrayList[JsValue]()
    for (heatmap <- convertJson(res)) {
      val count = (heatmap \ "count").as[Int]
      val sum = (heatmap \ "sum").as[Int]
      val average = sum.toDouble / count.toDouble
      heatmaps.add(Json.obj(SCHEMA.fX -> (heatmap \ SCHEMA.fLocation \ SCHEMA.fCoordinates).as[List[Double]].head,
        SCHEMA.fY -> (heatmap \ SCHEMA.fLocation \ SCHEMA.fCoordinates).as[List[Double]].tail.head,
        "w" -> JsString("{\"count\":" + count + ",\"average\":" + average + ",\"total\":" + sum + "}")))
    }
    return heatmaps.toList
  }

  /**
   * On request if there are 0 heatmaps this method will try to generate them. It only works if there are fingerprints
   * for the specific building / floor. If successfully created, the heatmaps are fetched and returned.
   *
   * @param buid
   * @param floor
   * @param query
   * @return
   */
  def generateHeatmapsOnFly(col: String, buid: String, floor: String, query: BsonDocument, level: Int,
                            hasTimestamp: Boolean): List[JsValue] = {
    val collection = mdb.getCollection(col)
    if (onRequestCreateHeatmaps(buid, floor, level, hasTimestamp)) { // if success fetch
      LPLogger.D2("generateHeatmapsOnFly")
      var radioPoints: AggregateObservable[Document] = null
      radioPoints = collection.aggregate(Seq(
        Aggregates.filter(query),
        project(Document(SCHEMA.fLocation -> "$location", "sum" -> "$sum", "count" -> "$count", "average" -> "$average")
        )))
      val awaited = Await.result(radioPoints.toFuture, Duration.Inf)
      val res = awaited.toList
      return convertJson(res)
    }
    return null
  }

  /**
   * Creates heatmaps and stores them in a collection according to the zoom level.
   *
   * @param buid
   * @param floor
   * @param level
   * @param hasTimestamp
   * @return
   */
  def onRequestCreateHeatmaps(buid: String, floor: String, level: Int, hasTimestamp: Boolean): Boolean = {
    LPLogger.info("onRequestCreateHeatmaps")
    val collection = mdb.getCollection(SCHEMA.cFingerprintsWifi)
    val query: BsonDocument = BsonDocument(SCHEMA.fBuid -> buid, SCHEMA.fFloor -> floor)
    val fingerprintsLookup = collection.find(query)
    val awaited = Await.result(fingerprintsLookup.toFuture, Duration.Inf)
    val res = awaited.toList
    val fingerprints = convertJson(res)
    if (fingerprints.size == 0) {
      LPLogger.info("No fingerprints in the building.Can't generate heatmaps.")
      return false
    }

    for (fng <- fingerprints) {
      updateHeatmap(fng, level, hasTimestamp)
    }
    true
  }

  /**
   * TODO:NN write doc
   *
   * @param fingerprint
   * @param level
   * @param hasTimestamp
   * @return
   */
  def updateHeatmap(fingerprint: JsValue, level: Int, hasTimestamp: Boolean): Boolean = {
    var collectionName = "heatmapWifi" // concatenating rest of the Collection name
    if (hasTimestamp) collectionName = collectionName + "Timestamp"
    collectionName = collectionName + level
    val storedHeatmap = fetchStoredHeatmap(collectionName, fingerprint, level, hasTimestamp)
    if (storedHeatmap == null) {
      val heatmap = createHeatmap(fingerprint, level, hasTimestamp)
      ProxyDataSource.getIDatasource().addJsonDocument(collectionName, heatmap.toString())
    } else {
      val newSum = confirmNegativity((fingerprint \ "sum").as[Int]) + confirmNegativity((storedHeatmap \ "sum").as[Int])
      val newCount = (fingerprint \ "count").as[Int] + (storedHeatmap \ "count").as[Int]
      val newHeatmap: JsValue = storedHeatmap.as[JsObject] + ("sum" -> JsNumber(newSum)) + ("count" -> JsNumber(newCount))
      replaceStoredHeatmap(collectionName, newHeatmap, fingerprint, level, hasTimestamp)
    }
  }

  def createHeatmap(fingerprint: JsValue, level: Int, timestamp: Boolean): JsValue = {
    var heatmap: JsValue = fingerprint.as[JsObject] - SCHEMA.fId - SCHEMA.fStrongestWifi - SCHEMA.fHeading - SCHEMA.fX - SCHEMA.fY - SCHEMA.fGeometry -
      SCHEMA.fMeasurements
    if (!timestamp)
      heatmap = heatmap.as[JsObject] - SCHEMA.fTimestamp
    val location = trimCoordinates(fingerprint, level)
    if (location == null) return null
    heatmap = heatmap.as[JsObject] + (SCHEMA.fLocation -> Json.toJson(new GeoJSONPoint(location.get(0),
      location.get(1)).toGeoJSON()))
    return heatmap
  }

  def fetchStoredHeatmap(collection: String, fingerprint: JsValue, level: Int, timestamp: Boolean): JsValue = {
    val heatmap = mdb.getCollection(collection)
    val location = trimCoordinates(fingerprint, level)
    if (location == null) return null
    var query: BsonDocument = null
    if (!timestamp) {
      query = BsonDocument(SCHEMA.fBuid -> (fingerprint \ SCHEMA.fBuid).as[String], SCHEMA.fFloor -> (fingerprint \ SCHEMA.fFloor).as[String],
        "location.coordinates" -> location.toList)
    } else {
      query = BsonDocument(SCHEMA.fBuid -> (fingerprint \ SCHEMA.fBuid).as[String], SCHEMA.fFloor -> (fingerprint \ SCHEMA.fFloor).as[String],
        "location.coordinates" -> location.toList, SCHEMA.fTimestamp -> (fingerprint \ SCHEMA.fTimestamp).as[String])
    }
    val heatmapLookup = heatmap.find(query).first()
    val awaited = Await.result(heatmapLookup.toFuture, Duration.Inf)
    val res = awaited.asInstanceOf[Document]
    if (res != null)
      return convertJson(res)
    null
  }

  def replaceStoredHeatmap(collection: String, newHeatmap: JsValue, fingerprint: JsValue, level: Int, timestamp: Boolean): Boolean = {
    val heatmap = mdb.getCollection(collection)
    val location = trimCoordinates(fingerprint, level)
    if (location == null) return false
    var query: BsonDocument = null
    if (!timestamp) {
      query = BsonDocument(SCHEMA.fBuid -> (fingerprint \ SCHEMA.fBuid).as[String], SCHEMA.fFloor -> (fingerprint \ SCHEMA.fFloor).as[String],
        "location.coordinates" -> location.toList)
    } else {
      query = BsonDocument(SCHEMA.fBuid -> (fingerprint \ SCHEMA.fBuid).as[String], SCHEMA.fFloor -> (fingerprint \ SCHEMA.fFloor).as[String],
        "location.coordinates" -> location.toList, SCHEMA.fTimestamp -> (fingerprint \ SCHEMA.fTimestamp).as[String])
    }
    val update = BsonDocument(newHeatmap.toString())
    val heatmapReplace = heatmap.replaceOne(query, update)
    val awaited = Await.result(heatmapReplace.toFuture, Duration.Inf)
    val res = awaited
    if (res.getModifiedCount == 0)
      false
    else
      true
  }

  def trimCoordinates(fingerprint: JsValue, level: Int): util.ArrayList[Double] = {
    if ((fingerprint \ SCHEMA.fGeometry \ SCHEMA.fCoordinates).toOption.isEmpty) {
      LPLogger.debug("trimCoordinates: " + fingerprint + ".No field coordinates.")
      return null
    }
    val location = new util.ArrayList[Double]
    try {
      val lat = (fingerprint \ SCHEMA.fGeometry \ SCHEMA.fCoordinates).get(0).as[Double]
      val lon = (fingerprint \ SCHEMA.fGeometry \ SCHEMA.fCoordinates).get(1).as[Double]
      //LPLogger.D2("(LAT, LON) = " + lat + ", " + lon)
      val tokLat = lat.toString.split("\\.")
      val tokLon = lon.toString.split("\\.")
      if (level == 1) {
        location.add((tokLat(0) + "." + tokLat(1).substring(0, 5)).toDouble)
        location.add((tokLon(0) + "." + tokLon(1).substring(0, 5)).toDouble)
      } else if (level == 2) {
        location.add((tokLat(0) + "." + tokLat(1).substring(0, 6)).toDouble)
        location.add((tokLon(0) + "." + tokLon(1).substring(0, 6)).toDouble)
      } else if (level == 3) {
        location.add(lat)
        location.add(lon)
      }
      return location
    } catch {
      case e: Exception => LPLogger.error("trimCoordinates", "" ,e)
    }
    null
  }

  def confirmNegativity(number: Int): Int = {
    if (number > 0)
      return number * (-1)
    return number
  }

  override def getAPsByBuildingFloorcdb(buid: String, floor: String): util.List[JsonObject] = ???

  override def getAPsByBuildingFloor(buid: String, floor: String): List[JsValue] = {
    val collection = mdb.getCollection(SCHEMA.cFingerprintsWifi)
    val query = BsonDocument(SCHEMA.fBuid -> buid, SCHEMA.fFloor -> floor)
    val fingerprints = collection.find(query)
    val awaited = Await.result(fingerprints.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val hm = new util.HashMap[JsValue, Array[Double]]()
    for (f <- listJson) {
      val measurements = (f \ SCHEMA.fMeasurements).as[List[List[String]]]
      for (measurement <- measurements) {
        val json = unrollFingerprint(f, measurement)
        val key = Json.obj(SCHEMA.fX -> (json \ SCHEMA.fX).as[String], SCHEMA.fY -> (json \ SCHEMA.fY).as[String],
          "AP" -> (json \ SCHEMA.fMac).as[String])
        if (!hm.containsKey(key)) {
          // count / average / total
          val tArray = new Array[Double](3)
          tArray(0) = 1
          tArray(1) = (json \ SCHEMA.fRSS).as[String].toDouble
          tArray(2) = (json \ SCHEMA.fRSS).as[String].toDouble
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
    val points = new util.ArrayList[JsValue]()
    for (h <- hm.toMap.toList) {
      var tempJson: JsValue = h._1
      val rss = Json.obj("count" -> JsNumber(h._2(0)),
        "average" -> JsNumber(h._2(2)),
        "total" -> JsNumber(h._2(1)))
      tempJson = tempJson.as[JsObject] + ("RSS" -> rss)
      if ((rss \ "average").as[Double] >= -70)
        points.add(tempJson.as[JsObject])
    }
    points.toList
  }

  override def getCachedAPsByBuildingFloor(buid: String, floor: String): JsValue = {
    val collection = mdb.getCollection(SCHEMA.cAccessPointsWifi)
    val query = BsonDocument(SCHEMA.fBuid -> buid, SCHEMA.fFloor -> floor)
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
    val collection = mdb.getCollection(SCHEMA.cFingerprintsWifi)
    val fingerprints = collection.find(and(
      geoWithinBox(SCHEMA.fGeometry, lat1.toDouble, lon1.toDouble, lat2.toDouble, lon2.toDouble),
      equal(SCHEMA.fBuid, buid),
      equal(SCHEMA.fFloor, floor)))
    val awaited = Await.result(fingerprints.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val newList = new util.ArrayList[JsValue]()
    for (f <- listJson) {
      if ((f \ SCHEMA.fBuid).as[String] == buid && (f \ SCHEMA.fFloor).as[String] == floor) {
        newList.add(f)
      }
    }
    newList.toList
  }

  override def getFingerPrintsTimestampBBox(buid: String, floor: String, lat1: String, lon1: String, lat2: String,
                                            lon2: String, timestampX: String, timestampY: String): List[JsValue] = {
    val collection = mdb.getCollection(SCHEMA.cFingerprintsWifi)
    val fingerprints = collection.find(and(geoWithinBox(SCHEMA.fGeometry, lat1.toDouble, lon1.toDouble,
      lat2.toDouble, lon2.toDouble), and(gt(SCHEMA.fTimestamp, timestampX), lt(SCHEMA.fTimestamp, timestampY))))
    val awaited = Await.result(fingerprints.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val newList = new util.ArrayList[JsValue]()
    for (f <- listJson) {
      if ((f \ SCHEMA.fBuid).as[String] == buid && (f \ SCHEMA.fFloor).as[String] == floor) {
        newList.add(f)
      }
    }
    newList.toList
  }

  override def getFingerprintsByTime(buid: String, floor: String): List[JsValue] = {
    val collection = mdb.getCollection(SCHEMA.cFingerprintsWifi)
    val fingerprints = collection.find(and(
      and(gt(SCHEMA.fTimestamp, "0"), lt(SCHEMA.fTimestamp, "999999999999999")),
      and(equal(SCHEMA.fBuid, buid)), equal(SCHEMA.fFloor, floor))
    ).sort(orderBy(ascending(SCHEMA.fTimestamp)))
    val awaited = Await.result(fingerprints.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val points = new util.ArrayList[JsValue]()
    for (f <- listJson) {
      points.add(Json.obj("date" -> (f \ SCHEMA.fTimestamp).as[String],
        "count" -> (f \ "count").as[Int]))
    }
    points.toList
  }

  override def getRadioHeatmapByBuildingFloor2(lat: String, lon: String, buid: String, floor: String, range: Int): java.util.List[JsonObject] = ???

  override def getRadioHeatmapBBox(lat: String, lon: String, buid: String, floor: String, range: Int): java.util.List[JsonObject] = ???

  override def getRadioHeatmapBBox2(lat: String, lon: String, buid: String, floor: String, range: Int): java.util.List[JsonObject] = ???

  override def getAllBuildings(): List[JsValue] = {
    val collection = mdb.getCollection(SCHEMA.cSpaces)
    val query = BsonDocument(SCHEMA.fIsPublished -> "true")
    val buildings = collection.find(query)
    val awaited = Await.result(buildings.toFuture, Duration.Inf)
    val res = awaited.toList
    LPLogger.debug(s"Res on complete Length:${res.length}")
    val listJson = convertJson(res)
    var newJson: JsValue = null
    val newList = new util.ArrayList[JsValue]()
    for (x <- listJson) {
      newJson = x.as[JsObject] - SCHEMA.fGeometry - SCHEMA.fOwnerId - SCHEMA.fCoOwners - SCHEMA.fId
      newList.add(newJson)
    }
    newList.toList
  }

  override def getAllBuildingsByOwner(oid: String): List[JsValue] = {
    val collection = mdb.getCollection(SCHEMA.cSpaces)
    var buildingLookUp = collection.find(or(equal(SCHEMA.fOwnerId, oid),
      equal(SCHEMA.fCoOwners, oid)))
    if (admins.contains(oid)) {
      buildingLookUp = collection.find()
    }
    val awaited = Await.result(buildingLookUp.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val buildings = new java.util.ArrayList[JsValue]()
    for (building <- listJson) {
      buildings.add(building.as[JsObject] - SCHEMA.fCoOwners - SCHEMA.fGeometry
        - SCHEMA.fOwnerId - SCHEMA.fId - SCHEMA.fSchema)
    }
    buildings.toList
  }

  override def getAllBuildingsByBucode(bucode: String): List[JsValue] = {
    val collection = mdb.getCollection(SCHEMA.cSpaces)
    val buildingLookUp = collection.find(equal(SCHEMA.fBuCode, bucode))
    val awaited = Await.result(buildingLookUp.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    val buildings = new java.util.ArrayList[JsValue]()
    for (building <- listJson) {
      try {
        buildings.add(building.as[JsObject] - SCHEMA.fCoOwners - SCHEMA.fGeometry - SCHEMA.fOwnerId - SCHEMA.fId - SCHEMA.fSchema)
      } catch {
        case e: IOException =>
      }
    }
    buildings.toList
  }

  override def getBuildingByAlias(alias: String): JsonObject = ???

  override def getAllBuildingsNearMe(lat: Double, lng: Double, range: Int, owner_id: String): List[JsValue] = {
    val bbox = GeoPoint.getGeoBoundingBox(lat, lng, range)
    val collection = mdb.getCollection(SCHEMA.cSpaces)
    val buildingLookUp = collection.find(and(geoWithinBox(SCHEMA.fGeometry, bbox(0).dlat, bbox(0).dlon, bbox(1).dlat,
      bbox(1).dlon),
      or(equal(SCHEMA.fIsPublished, "true"),
        and(equal(SCHEMA.fIsPublished, "false"), equal(SCHEMA.fOwnerId, owner_id)))))
    val awaited = Await.result(buildingLookUp.toFuture, Duration.Inf)
    val res = awaited.toList
    LPLogger.debug("getAllBuildingsNearMe: fetched " + res.size + " building(s) within a range of: " + range)
    val listJson = convertJson(res)
    val buildings = new java.util.ArrayList[JsValue]()
    for (building <- listJson) {
      try {
        buildings.add(building.as[JsObject] - SCHEMA.fCoOwners - SCHEMA.fGeometry - SCHEMA.fOwnerId - SCHEMA.fId)
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
    val collection = mdb.getCollection(SCHEMA.cFingerprintsWifi)
    val fingerprints = collection.find(geoWithinBox(SCHEMA.fGeometry, bbox(0).dlat, bbox(0).dlon, bbox(1).dlat,
      bbox(1).dlon)).limit(queryLimit)
    val awaited = Await.result(fingerprints.toFuture, Duration.Inf)
    val res = awaited.toList
    val listJson = convertJson(res)
    for (rss <- listJson) {
      if (floorFetched > floorLimit)
        break
      totalFetched += 1
      if ((rss \ SCHEMA.fFloor).as[String] == floor_number) {
        floorFetched += 1
        val measurements = (rss \ SCHEMA.fMeasurements).as[List[List[String]]]
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
    val collection = mdb.getCollection(SCHEMA.cFloorplans)
    val query = BsonDocument(SCHEMA.fFloorNumber -> floor_number)
    val floorLookUp = collection.find(query)
    val awaited = Await.result(floorLookUp.toFuture, Duration.Inf)
    val res = awaited.asInstanceOf[List[Document]]
    val floorplans = convertJson(res)
    var unique = 0
    var uniqBuid = ""
    for (floorplan <- floorplans) {
      val lat1 = (floorplan \ fLatBottomLeft)
      val lon1 = (floorplan \ SCHEMA.fLonBottomLeft)
      val lat2 = (floorplan \ SCHEMA.fLatTopRight)
      val lon2 = (floorplan \ SCHEMA.fLonTopRight)
      if (lat1.toOption.isDefined && lon1.toOption.isDefined && lat2.toOption.isDefined && lon2.toOption.isDefined) {
        if (lat1.as[String].toDouble <= lat && lat <= lat2.as[String].toDouble &&
          lon1.as[String].toDouble <= lon && lon <= lon2.as[String].toDouble) {
          unique += 1
          uniqBuid = (floorplan \ SCHEMA.fBuid).as[String]
        }
      }
    }
    if (unique == 1)
      return uniqBuid
    LPLogger.error("Coordinates were found in two floors.")
    return null
  }

  override def dumpRssLogEntriesByBuildingFloor(outFile: FileOutputStream, buid: String, floor_number: String): Long = {
    val writer = new PrintWriter(outFile)
    var totalFetched = 0

    val collection = mdb.getCollection(SCHEMA.cFingerprintsWifi)
    val query = BsonDocument(SCHEMA.fBuid -> buid, SCHEMA.fFloor -> floor_number)
    val fingerprintLookUp = collection.find(query)
    val awaited = Await.result(fingerprintLookUp.toFuture, Duration.Inf)
    val res = awaited.asInstanceOf[List[Document]]
    val rssLog = convertJson(res)
    // splitting Measurements[MAC, rss] to buid, floor, .., MAC, rss, ... (old form)
    if (rssLog.size > 0) {
      for (rss <- rssLog) {
        val measurements = (rss \ SCHEMA.fMeasurements).as[List[List[String]]]
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
    val collection = mdb.getCollection(SCHEMA.cUsers)
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

  override def getAllBuildingsetsByOwner(owner_id: String): List[JsValue] = {
    val collection = mdb.getCollection(SCHEMA.cCampuses)
    val query: BsonDocument = BsonDocument(SCHEMA.fOwnerId -> owner_id)
    val campus = collection.find(query)
    val awaited = Await.result(campus.toFuture, Duration.Inf)
    val res = awaited.toList
    convertJson(res)
  }

  override def generateHeatmaps(): Boolean = {
    LPLogger.debug("generateHeatmaps: Generating Heatmaps")
    val bCollection = mdb.getCollection(SCHEMA.cSpaces)
    val flCollection = mdb.getCollection(SCHEMA.cFloorplans)
    val fiCollection = mdb.getCollection(SCHEMA.cFingerprintsWifi)
    val tempQuery: BsonDocument = BsonDocument(SCHEMA.fBuid -> "building_e0982a5f-fa50-4200-bab7-99ef2dce7285_1623673422061")
    val buildingsLookup = bCollection.find(tempQuery)
    val awaitedB = Await.result(buildingsLookup.toFuture, Duration.Inf)
    val resB = awaitedB.toList
    val buildings = convertJson(resB)

    for (building <- buildings) {
      val floorsLookup = flCollection.find(equal(SCHEMA.fBuid, (building \ SCHEMA.fBuid).as[String]))
      val awaitedFl = Await.result(floorsLookup.toFuture, Duration.Inf)
      val resFl = awaitedFl.toList
      val floors = convertJson(resFl)
      for (floor <- floors) {
        val query: BsonDocument = BsonDocument(SCHEMA.fBuid -> (building \ SCHEMA.fBuid).as[String], SCHEMA.fFloor -> (floor \ SCHEMA.fFloorNumber).as[String])
        val fingerprintsLookup = fiCollection.find(query)
        val awaitedFi = Await.result(fingerprintsLookup.toFuture, Duration.Inf)
        val resFi = awaitedFi.toList
        val fingerprints = convertJson(resFi)
        for (fingerprint <- fingerprints) {
          if (!updateHeatmap(fingerprint, 1, false)) {
            LPLogger.debug("error at level 1")
            return false
          }
          if (!updateHeatmap(fingerprint, 2, false)) {
            LPLogger.debug("error at level 2")
            return false
          }
          if (!updateHeatmap(fingerprint, 3, false)) {
            LPLogger.debug("error at level 3")
            return false
          }
          if (!updateHeatmap(fingerprint, 1, true)) {
            LPLogger.debug("error at level 1 timestamp")
            return false
          }
          if (!updateHeatmap(fingerprint, 2, true)) {
            LPLogger.debug("error at level 2 timestamp")
            return false
          }
          if (!updateHeatmap(fingerprint, 3, true)) {
            LPLogger.debug("error at level 3 timestamp")
            return false
          }
        }
      }
    }
    LPLogger.debug("generateHeatmaps: Generated Heatmaps")
    true
  }

  override def deleteNotValidDocuments(): Boolean = ???

  private def connect(): Boolean = {
    //    LPLogger.info("Mongodb: connecting to: " + mHostname + ":" + mPort + " bucket[" +
    //      mBucket + "]")
    false
  }

  override def deleteAffectedHeatmaps(buid: String, floor_number: String): Boolean = {
    LPLogger.D2("Deleting generated heatmaps..")
    val collections = Array(cHeatmapWifi1, cHeatmapWifi2, cHeatmapWifi3, cHeatmapWifiTimestamp1,
      cHeatmapWifiTimestamp2, cHeatmapWifiTimestamp3)
    val query = BsonDocument(SCHEMA.fBuid -> buid, SCHEMA.fFloor -> floor_number)
    var ret = true
    for (colName <- collections) {
      val collection = mdb.getCollection(colName)
      val deleted = collection.deleteMany(query)
      val awaited = Await.result(deleted.toFuture, Duration.Inf)
      val res = awaited.wasAcknowledged()
      ret = ret && res
    }
    return ret
  }

  override def deleteFingerprint(fingerprint: JsValue): Boolean = {
    val collection = mdb.getCollection(SCHEMA.cFingerprintsWifi)
    val query = BsonDocument(SCHEMA.fBuid -> (fingerprint \ SCHEMA.fBuid).as[String],
      SCHEMA.fX -> (fingerprint \ SCHEMA.fX).as[String],
      SCHEMA.fY -> (fingerprint \ SCHEMA.fY).as[String],
      SCHEMA.fFloor -> (fingerprint \ SCHEMA.fFloor).as[String],
      SCHEMA.fMeasurements -> (fingerprint \ SCHEMA.fMeasurements).as[List[List[String]]],
      SCHEMA.fCount -> (fingerprint \ SCHEMA.fCount).as[Int],
      SCHEMA.fSum -> (fingerprint \ SCHEMA.fSum).as[Int],
      SCHEMA.fHeading -> (fingerprint \ SCHEMA.fHeading).as[String],
      SCHEMA.fTimestamp -> (fingerprint \ fTimestamp).as[String])
    val deleted = collection.deleteMany(query)
    val awaited = Await.result(deleted.toFuture, Duration.Inf)
    return awaited.wasAcknowledged()
  }

  /**
   * Searching for heatmaps in heatmapTimestamp_ collections. If 0 exist, generates them.
   *
   * @param col
   * @param buid
   * @param floor
   * @param level
   */
  override def createTimestampHeatmap(col: String, buid: String, floor: String, level: Int) {
    val collection = mdb.getCollection(col)
    val query = BsonDocument(SCHEMA.fBuid -> buid, SCHEMA.fFloor -> floor)
    val heatmapLookUp = collection.find(query).first()
    val awaited = Await.result(heatmapLookUp.toFuture(), Duration.Inf)
    val res = awaited.asInstanceOf[Document]
    if (res == null)
      onRequestCreateHeatmaps(buid, floor, level, true)
  }

  override def login(col: String, username: String, password: String): List[JsValue] = {
    val collection = mdb.getCollection(col)
    val query = BsonDocument(SCHEMA.fUsername -> username, SCHEMA.fPassword -> password)
    val userLookUp = collection.find(query)
    val awaited = Await.result(userLookUp.toFuture(), Duration.Inf)
    val res = awaited.asInstanceOf[List[Document]]
    if (convertJson(res).size == 0)
      return null
    return convertJson(res)
  }

  override def register(col: String, name: String, email: String, username: String, password: String): Boolean = {
    val accessToken = "autoEinaitoAccessToken"
    val json: JsValue =  Json.obj("name" -> JsString(name), SCHEMA.fEmail -> JsString(email),
      SCHEMA.fUsername -> JsString(username), SCHEMA.fPassword -> JsString(password),
      SCHEMA.fAccessToken -> JsString(accessToken))
    return addJsonDocument(SCHEMA.cUsers, json.toString())
  }

}

