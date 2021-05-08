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
package db_models


import java.io.{File, FileNotFoundException, FileOutputStream, IOException}
import java.util.HashMap

import com.couchbase.client.java.document.json.JsonObject
import controllers.AnyplacePosition.BBOX_MAX
import datasources.ProxyDataSource
import json.VALIDATE.{Coordinate, StringNumber}
import play.Play
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import play.api.mvc.Result
import radiomapserver.RadioMap.{RBF_ENABLED, RadioMap}
import utils.AnyplaceServerAPI._
import utils.FileUtils.getDirFrozenFloor
import utils.JsonUtils.convertToInt
import utils.LPUtils.MD5
import utils._

import scala.collection.JavaConverters.mapAsScalaMapConverter

object RadioMapRaw {

  def toRawRadioMapRecord(hm: HashMap[String, String]): String = {
    val sb = new StringBuilder()
    sb.append(hm.get("timestamp"))
    sb.append(" ")
    sb.append(hm.get("x"))
    sb.append(" ")
    sb.append(hm.get("y"))
    sb.append(" ")
    sb.append(hm.get("heading"))
    sb.append(" ")
    sb.append(hm.get("MAC"))
    sb.append(" ")
    sb.append(hm.get("rss"))
    sb.append(" ")
    sb.append(hm.get("floor"))
    sb.toString
  }

  def toRawRadioMapRecord(json: JsValue): String = {
    val sb = new StringBuilder()
    sb.append((json \ "timestamp").as[String])
    sb.append(" ")
    sb.append((json \ "x").as[String])
    sb.append(" ")
    sb.append((json \ "y").as[String])
    sb.append(" ")
    sb.append((json \ "heading").as[String])
    sb.append(" ")
    sb.append((json \ "MAC").as[String])
    sb.append(" ")
    sb.append((json \ "rss").as[String])
    sb.append(" ")
    sb.append((json \ "floor").as[String])
    sb.toString
  }

  /**
   * CHECK:DZ CHECK:PM CHECK:NN
   * Every time it creates a new radiomap file
   * We have only coordinates and floor. We dont have a building so we download from a bounding box
   *
   * @param json
   * @param range
   * @return
   */
  def findRadioBbox(json: JsValue, range: Int): Result = {
    if (Coordinate(json, "coordinates_lat") == null)
      return AnyResponseHelper.bad_request("coordinates_lat field must be String containing a float!")
    val lat = (json \ "coordinates_lat").as[String]
    if (Coordinate(json, "coordinates_lon") == null)
      return AnyResponseHelper.bad_request("coordinates_lon field must be String containing a float!")
    val lon = (json \ "coordinates_lon").as[String]
    if (StringNumber(json, "floor_number") == null)
      return AnyResponseHelper.bad_request("floor_number field must be String, containing a number!")
    val floorNumber = (json \ "floor_number").as[String]
    if (!Floor.checkFloorNumberFormat(floorNumber)) {
      return AnyResponseHelper.bad_request("Floor number cannot contain whitespace!")
    } else {
      val bbox = GeoPoint.getGeoBoundingBox(java.lang.Double.parseDouble(lat), java.lang.Double.parseDouble(lon),
        range)
      LPLogger.D4("LowerLeft: " + bbox(0) + " UpperRight: " + bbox(1))

      // create unique name for cached file based on coordinates, floor_number, range
      val pathName = "radiomaps"
      val hashKey = lat + lon + floorNumber
      val bboxRadioDir = MD5(hashKey) + "-" + range.toString
      LPLogger.debug("hashkey = " + hashKey)
      LPLogger.debug("bbox_token = " + bboxRadioDir)
      // store in radioMapRawDir/tmp/buid/floor/bbox_token
      val fullPath = Play.application().configuration().getString("radioMapRawDir") + "/bbox/" + bboxRadioDir
      val dir = new File(fullPath)
      val radiomap_filename = new File(fullPath + URL_SEP + "indoor-radiomap.txt")
        .getAbsolutePath
      var msg = ""
      if (!dir.exists()) {
        // if the range is maximum then we are looking for the entire floor
        if (range == BBOX_MAX) {
          val buid = ProxyDataSource.getIDatasource.dumpRssLogEntriesWithCoordinates(floorNumber, lat.toDouble, lon.toDouble)
          if (buid != null) {  // building found. return path to file
            val radiomap_mean_filename = SERVER_API_ROOT
            val path = radiomap_mean_filename.dropRight(1) + getDirFrozenFloor(buid, floorNumber) + "/indoor-radiomap-mean.txt"
            val res = Json.obj("map_url_mean" -> path)
            return AnyResponseHelper.ok(res, "Successfully retrieved radiomap: full-floor (according to lat lon)")
          }
        }
        msg = "created bbox: " + fullPath
        if (!dir.mkdirs()) {
          return AnyResponseHelper.internal_server_error("Failed to create bbox dir: " + fullPath)
        }
        val rssLog = new File(dir.getAbsolutePath + URL_SEP + "rss-log")
        var fout: FileOutputStream = null
        var floorFetched: Long = 0l
        try {
          fout = new FileOutputStream(rssLog)
          LPLogger.D5("RSS path: " + rssLog.toPath().getFileName.toString)
          floorFetched = ProxyDataSource.getIDatasource.dumpRssLogEntriesSpatial(fout, bbox, floorNumber)
          fout.close()
          if (floorFetched == 0) {
            return AnyResponseHelper.bad_request("Area not supported yet!")
          }
          val rm = new RadioMap(new File(fullPath), radiomap_filename, "", -110)
          val resCreate = rm.createRadioMap()
          if (resCreate != null) {
            return AnyResponseHelper.internal_server_error("findRadioBbox: radiomap on-the-fly: " + resCreate)
          }
        } catch {
          case fnfe: FileNotFoundException => return AnyResponseHelper.internal_server_error("findRadioBbox: " +
            "rssLog: " + rssLog, fnfe)
          case e: Exception =>  return AnyResponseHelper.internal_server_error("findRadioBbox" , e)
        }
      } else {
        msg = "cached-bbox: " + fullPath
        LPLogger.debug("findRadioBbox: " + msg)
      }
      var radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt")
      var radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt")
      var radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt")
      val api = AnyplaceServerAPI.SERVER_API_ROOT
      var pos = radiomap_mean_filename.indexOf(pathName)
      radiomap_mean_filename = api + radiomap_mean_filename.substring(pos)
      var res: JsValue = null
      if (RBF_ENABLED) {
        pos = radiomap_rbf_weights_filename.indexOf(pathName)
        radiomap_rbf_weights_filename = api + radiomap_rbf_weights_filename.substring(pos)
        pos = radiomap_parameters_filename.indexOf(pathName)
        radiomap_parameters_filename = api + radiomap_parameters_filename.substring(pos)
        res = Json.obj("map_url_mean" -> radiomap_mean_filename,
          "map_url_weights" -> radiomap_rbf_weights_filename, "map_url_parameters" -> radiomap_parameters_filename)
      } else {
        res = Json.obj("map_url_mean" -> radiomap_mean_filename)
      }

      AnyResponseHelper.ok(res, "Successfully retrieved radiomap: " + msg)
    }
  }

  def unrollFingerprint(rss: JsValue, measurement: List[String]): JsValue = {
    var json = Json.obj("buid" -> (rss \ "buid").as[String], "floor" -> (rss \ "floor").as[String],
      "x" -> (rss \ "x").as[String], "y" -> (rss \ "y").as[String], "heading" -> (rss \ "heading").as[String],
      "timestamp" -> (rss \ "timestamp").as[String], "MAC" -> measurement(0), "rss" -> measurement(1),
      ("geometry" -> Json.toJson(new GeoJSONPoint(java.lang.Double.parseDouble((rss \ "x").as[String]),
        java.lang.Double.parseDouble((rss \ "y").as[String])).toGeoJSON())))
    if ((rss \ "strongestWifi").toOption.isDefined)
      json = json.as[JsObject] + ("strongestWifi" -> JsString((rss \ "strongestWifi").as[String]))
    return json
  }
}

class RadioMapRaw(h: HashMap[String, String]) extends AbstractModel {

  this.fields = h

  def this(timestamp: String,
           x: String,
           y: String,
           heading: String
          ) {
    this(new HashMap[String, String])
    fields.put("timestamp", timestamp)
    fields.put("x", x)
    fields.put("y", y)
    fields.put("heading", heading)
    fields.put("floor", "-")
  }

  def this(timestamp: String,
           x: String,
           y: String,
           heading: String,

           floor: String) {
    this(new HashMap[String, String])
    fields.put("timestamp", timestamp)
    fields.put("x", x)
    fields.put("y", y)
    fields.put("heading", heading)
    fields.put("floor", floor)
  }

  def this(timestamp: String,
           x: String,
           y: String,
           heading: String,
           floor: String,
           strongestWifi: String) {
    this(new HashMap[String, String])
    fields.put("timestamp", timestamp)
    fields.put("x", x)
    fields.put("y", y)
    fields.put("heading", heading)
    fields.put("floor", floor)
    fields.put("strongestWifi", strongestWifi)
  }

  def this(timestamp: String,
           x: String,
           y: String,
           heading: String,
           floor: String,
           strongestWifi: String,
           buid: String) {
    this(new HashMap[String, String])
    fields.put("timestamp", timestamp)
    fields.put("x", x)
    fields.put("y", y)
    fields.put("heading", heading)
    fields.put("floor", floor)
    fields.put("strongestWifi", strongestWifi)
    fields.put("buid", buid)
  }

  def getId(): String = {
    fields.get("x") + fields.get("y") + fields.get("heading") +
      fields.get("timestamp") +
      fields.get("MAC")
  }

  def toValidJson(): JsonObject = {
    JsonObject.from(this.getFields())
  }

  def toValidMongoJson(): JsValue = {
    toJson()
  }

  def addMeasurements(measurements: List[List[String]]): String = {
    val sb = new StringBuilder()
    var json = toValidMongoJson()
    try {
      json = json.as[JsObject] + ("measurements" -> Json.toJson(measurements))
      json = json.as[JsObject] + ("geometry" -> Json.toJson(
        new GeoJSONPoint(java.lang.Double.parseDouble(fields.get("x")),
          java.lang.Double.parseDouble(fields.get("y"))).toGeoJSON()))
    } catch {
      case e: IOException => e.printStackTrace()
    }
    sb.append(json.toString)
    sb.toString
  }

  def toGeoJSON(): String = {
    val sb = new StringBuilder()
    var json = toValidMongoJson()
    try {
      json = json.as[JsObject] + ("geometry" -> Json.toJson(
        new GeoJSONPoint(java.lang.Double.parseDouble(fields.get("x")),
          java.lang.Double.parseDouble(fields.get("y"))).toGeoJSON()))
    } catch {
      case e: IOException => e.printStackTrace()
    }
    sb.append(json.toString)
    sb.toString
  }

  def toJson(): JsValue = {
    val sMap: Map[String, String] = this.getFields().asScala.toMap
    val res = Json.toJson(sMap)
    convertToInt("_schema", res)
  }

  @deprecated("")
  def _toString(): String = this.toValidJson().toString

  override def toString(): String = toJson().toString()

  def toRawRadioMapRecord(): String = {
    val sb = new StringBuilder()
    sb.append(fields.get("timestamp"))
    sb.append(" ")
    sb.append(fields.get("x"))
    sb.append(" ")
    sb.append(fields.get("y"))
    sb.append(" ")
    sb.append(fields.get("heading"))
    sb.append(" ")
    sb.append(fields.get("MAC"))
    sb.append(" ")
    sb.append(fields.get("rss"))
    sb.append(" ")
    sb.append(fields.get("floor"))
    sb.toString
  }

}
