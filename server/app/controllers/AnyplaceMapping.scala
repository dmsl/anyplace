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
package controllers

import java.io._
import java.net.{HttpURLConnection, URL}
import java.text.{NumberFormat, ParseException}
import java.util
import java.util.Locale
import java.util.zip.GZIPOutputStream

import acces.{AccesRBF, GeoUtils}
import breeze.linalg.{DenseMatrix, DenseVector}
import com.couchbase.client.java.document.json.{JsonArray, JsonObject}
import datasources.{DatasourceException, ProxyDataSource}
import db_models._
import location.Algorithms
import oauth.provider.v2.models.OAuth2Request
import org.apache.commons.codec.binary.Base64
import play.Play
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc._
import play.libs.F
import radiomapserver.RadioMap.RadioMap
import radiomapserver.RadioMapMean
import utils._

import scala.util.control.Breaks
import play.api.libs.json.Reads._

import scala.collection.JavaConversions._
import scala.collection.JavaConverters.iterableAsScalaIterableConverter
import scala.collection.mutable.ListBuffer

object AnyplaceMapping extends play.api.mvc.Controller {

  private val ADMIN_ID = "112997031510415584062_google"

  private def verifyOwnerId(authToken: String): String = {
    //remove the double string qoutes due to json processing
    val gURL = "https://www.googleapis.com/oauth2/v3/tokeninfo?id_token=" + authToken
    var res = ""
    try
      res = sendGet(gURL)
    catch {
      case e: Exception => {
        LPLogger.error(e.toString)
        null
      }
    }
    if (res != null)
      try {
        var json = JsonObject.fromJson(res)
        if (json.get("user_id") != null)
          return json.get("user_id").toString
        else if (json.get("sub") != null)
          return appendToOwnerId(json.get("sub").toString)
      } catch {
        case ioe: IOException => null
      }
    null
  }

  //Make the id to the appropriate format
  private def appendToOwnerId(ownerId: String) = if (ownerId.contains("_google")) ownerId else ownerId + "_google"

  private def sendGet(url: String) = {
    val obj = new URL(url)
    val con = obj.openConnection().asInstanceOf[HttpURLConnection]
    con.setRequestMethod("GET")
    val responseCode = con.getResponseCode
    val in = new BufferedReader(new InputStreamReader(con.getInputStream))
    val response = new StringBuffer()
    response.append(Iterator.continually(in.readLine()).takeWhile(_ != null).mkString)
    in.close()
    response.toString
  }

  def getRadioHeatmap() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::getRadioHeatmap(): " + json.toString)
        try {
          val radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmap
          if (radioPoints == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          val res = JsonObject.empty()
          res.put("radioPoints", (radioPoints))
          return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def getRadioHeatmapByBuildingFloor() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {

        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::getRadioHeatmap(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val buid = (json \ "buid").as[String]
        val floor = (json \ "floor").as[String]
        try {
          val radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmapByBuildingFloor(buid, floor)
          if (radioPoints == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          val res = JsonObject.empty()
          res.put("radioPoints", radioPoints)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }


  def getRadioHeatmapByBuildingFloorAverage() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::getRadioHeatmapRSS(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val buid = (json \ "buid").as[String]
        val floor = (json \ "floor").as[String]
        try {
          val radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmapByBuildingFloorAverage(buid, floor)
          if (radioPoints == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          val res = JsonObject.empty()
          res.put("radioPoints", radioPoints)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def getRadioHeatmapByBuildingFloorAverage1() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::getRadioHeatmapRSS1(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val buid = (json \ "buid").as[String]
        val floor = (json \ "floor").as[String]
        try {
          val radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmapByBuildingFloorAverage1(buid, floor)
          if (radioPoints == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          val res = JsonObject.empty()
          res.put("radioPoints", radioPoints)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }


  def getRadioHeatmapByBuildingFloorAverage2() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::getRadioHeatmapRSS2(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val buid = (json \ "buid").as[String]
        val floor = (json \ "floor").as[String]
        try {
          val radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmapByBuildingFloorAverage2(buid, floor)
          if (radioPoints == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          val res = JsonObject.empty()
          res.put("radioPoints", radioPoints)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def getRadioHeatmapByBuildingFloorAverage3() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::getRadioHeatmapRSS3(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val buid = (json \ "buid").as[String]
        val floor = (json \ "floor").as[String]
        try {
          val radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmapByBuildingFloorAverage3(buid, floor)
          if (radioPoints == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          val res = JsonObject.empty()
          res.put("radioPoints", radioPoints)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }
//HEREEE
 def getRadioHeatmapByBuildingFloorAverage3Tiles() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::getRadioHeatmapRSS3(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val buid = (json \ "buid").as[String]
        val floor = (json \ "floor").as[String]
        val x = (json \ "x").as[Int]
        val y = (json \ "y").as[Int]
        val z = (json \ "z").as[Int]
        try {
          val radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmapByBuildingFloorAverage3(buid, floor)
          if (radioPoints == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          val res = JsonObject.empty()
          val radioPointsInXY: util.ArrayList[JsonObject]= new util.ArrayList[JsonObject]()

          for (radioPoint: JsonObject <- radioPoints) {
            var radioX = radioPoint.getString("x").toDouble
            var radioY = radioPoint.getString("y").toDouble
            var xyConverter=convertToxy(radioX,radioY,z)
            if(xyConverter(0)==x && xyConverter(1)==y)
              radioPointsInXY.add(radioPoint)
          }
          res.put("radioPoints", radioPointsInXY)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }


  def getRadioHeatmapByBuildingFloorTimestamp()= Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody

        LPLogger.info("AnyplaceMapping::getRadioHeatmapByTime(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor","timestampX","timestampY")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val buid = (json \ "buid").as[String]
        val floor = (json \ "floor").as[String]
        val timestampX = (json \ "timestampX").as[String]
        val timestampY = (json \ "timestampY").as[String]

        try {

          val radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmapByBuildingFloorTimestamp(buid, floor, timestampX, timestampY)
          if (radioPoints == null) return AnyResponseHelper.bad_request("Fingerprints does not exist or could not be retrieved!")
          val res = JsonObject.empty()
          res.put("radioPoints", radioPoints)

          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
          }

        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  private def convertToxy(lat: Double, lon: Double, zoom: Int) = {
    val sxtile = Math.floor((lon + 180.0) / 360.0 * (1 << zoom).toDouble).toInt
    val sytile = Math.floor((1.0 - Math.log(Math.tan(Math.toRadians(lat)) + 1.0 / Math.cos(Math.toRadians(lat))) / 3.141592653589793) / 2.0 * (1 << zoom).toDouble).toInt
    Array[Int](sxtile, sytile)
  }
//HEREEE
    def getRadioHeatmapByBuildingFloorTimestampTiles()= Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody

        LPLogger.info("AnyplaceMapping::getRadioHeatmapByTime(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor","timestampX","timestampY","x","y","z")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val buid = (json \ "buid").as[String]
        val floor = (json \ "floor").as[String]
        val timestampX = (json \ "timestampX").as[String]
        val timestampY = (json \ "timestampY").as[String]
        val x = (json \ "x").as[Int]
        val y = (json \ "y").as[Int]
        val z = (json \ "z").as[Int]

        try {

          val radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmapByBuildingFloorTimestamp(buid, floor, timestampX, timestampY)
          if (radioPoints == null) return AnyResponseHelper.bad_request("Fingerprints does not exist or could not be retrieved!")
          val res = JsonObject.empty()
          val radioPointsInXY: util.ArrayList[JsonObject]= new util.ArrayList[JsonObject]()

          for (radioPoint: JsonObject <- radioPoints) {
            var radioX = radioPoint.getString("x").toDouble
            var radioY = radioPoint.getString("y").toDouble
            var xyConverter=convertToxy(radioX,radioY,z)
            if(xyConverter(0)==x && xyConverter(1)==y)
              radioPointsInXY.add(radioPoint)
          }
          res.put("radioPoints", radioPointsInXY)

          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
          }

        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def getRadioHeatmapByBuildingFloorTimestampAverage1()= Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody

        LPLogger.info("AnyplaceMapping::getRadioHeatmapRSSByTime(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor","timestampX","timestampY")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val buid = (json \ "buid").as[String]
        val floor = (json \ "floor").as[String]
        val timestampX = (json \ "timestampX").as[String]
        val timestampY = (json \ "timestampY").as[String]

        try {

          val radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmapByBuildingFloorTimestampAverage1(buid, floor, timestampX,timestampY)
          if (radioPoints == null) return AnyResponseHelper.bad_request("Fingerprints does not exist or could not be retrieved!")
          val res = JsonObject.empty()
          res.put("radioPoints", radioPoints)

          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
          }

        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def getRadioHeatmapByBuildingFloorTimestampAverage2()= Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody

        LPLogger.info("AnyplaceMapping::getRadioHeatmapRSSByTime(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor","timestampX","timestampY")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val buid = (json \ "buid").as[String]
        val floor = (json \ "floor").as[String]
        val timestampX = (json \ "timestampX").as[String]
        val timestampY = (json \ "timestampY").as[String]
        try {

          val radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmapByBuildingFloorTimestampAverage2(buid, floor, timestampX,timestampY)
          if (radioPoints == null) return AnyResponseHelper.bad_request("Fingerprints does not exist or could not be retrieved!")
          val res = JsonObject.empty()
          res.put("radioPoints", radioPoints)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
          }

        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }


  def getAPsByBuildingFloor() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {

        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::getAPs(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val buid = (json \ "buid").as[String]
        val floor = (json \ "floor").as[String]
        try {
          val accessPoints = ProxyDataSource.getIDatasource.getAPsByBuildingFloor(buid, floor).asScala

          val uniqueAPs: util.HashMap[String, JsonObject] = new util.HashMap()
          for (accessPoint: JsonObject <- accessPoints) {
            var id = accessPoint.getString("AP")
            id = id.substring(0, id.length - 9)

            var ap = uniqueAPs.get(id)
            var avg = accessPoint.getObject("RSS").getDouble("average")
            var x = accessPoint.getString("x").toDouble
            var y = accessPoint.getString("y").toDouble
            if (ap == null) {
              if (avg < -60) {
                accessPoint.put("den", avg)
                accessPoint.put("x", avg * x)
                accessPoint.put("y", avg * y)
              } else {
                accessPoint.put("den", 0)
                accessPoint.put("x", x)
                accessPoint.put("y", y)
              }
              ap = accessPoint
            } else if (ap.getDouble("den") < 0) {
              if (avg < -60) {
                var ap_den = ap.getDouble("den")
                var ap_x = ap.getDouble("x")
                var ap_y = ap.getDouble("y")
                accessPoint.put("den", avg + ap_den)
                accessPoint.put("x", avg * x + ap_x)
                accessPoint.put("y", avg * y + ap_y)
              } else {
                accessPoint.put("den", 0)
                accessPoint.put("x", x)
                accessPoint.put("y", y)
              }
              ap = accessPoint
            }
            //overwrite old object in case that there is one
            uniqueAPs.put(id, ap)
          }

          if (accessPoints == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          val res = JsonObject.empty()
          res.put("accessPoints", new util.ArrayList[JsonObject](uniqueAPs.values()))
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }


  def getAPsIds() = Action {
     implicit request =>
      def inner(request: Request[AnyContent]): Result = {

        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json  = anyReq.getJsonBody
        var accessPointsOfReq= (json\"ids").as[List[String]]

        try {
          val reqFile = "public/anyplace_architect/ids.json"
          val file = Play.application().resourceAsStream(reqFile)

          var accessPointsOfFile: List[JsObject]= null
          if (file != null) {
            accessPointsOfFile = Json.parse(file).as[List[JsObject]]
          }else{
            return AnyResponseHelper.not_found(reqFile)
          }

          val APsIDs: util.ArrayList[String]= new util.ArrayList[String]()
          var found = false
          var firstBitFound = false
          var sameBits = 0
          var sameBitsOfReq = 0
          var idOfReq : String = ""
          val loop = new Breaks

          val inner_loop = new Breaks


            for (accessPointOfReq : String <- accessPointsOfReq) {
              idOfReq="N/A"
              loop.breakable {
                for (accessPointOfFile: JsObject <- accessPointsOfFile) {

                  val bitsR = accessPointOfReq.split(":")
                  val bitsA = accessPointOfFile.value("mac").as[String].split(":")
                  if (bitsA(0).equalsIgnoreCase(bitsR(0))) {

                    firstBitFound=true

                    var i = 0
                    inner_loop.breakable {
                      for (i <- 0 until bitsA.length) {

                        if (bitsA(i).equalsIgnoreCase(bitsR(i))) {
                          sameBits += 1
                        } else {

                          inner_loop.break
                        }
                      }
                    }

                    if(sameBits >= 3)
                      found = true

                  } else {
                    sameBits = 0

                    if (firstBitFound) {
                      firstBitFound=false
                      loop.break
                    }
                  }

                  if (sameBitsOfReq < sameBits && found) {
                    sameBitsOfReq = sameBits
                    idOfReq = accessPointOfFile.value("id").as[String]
                  }
                  sameBits = 0

                }
              }//accessPointOfFile break

              APsIDs.add(idOfReq)
              sameBitsOfReq = 0
              found=false

          }

          if (accessPointsOfReq == null) return AnyResponseHelper.bad_request("Access Points does not exist or could not be retrieved!")
          val res = JsonObject.empty()

          res.put("accessPoints", APsIDs)

          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all id for Access Points!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def FingerPrintsDelete() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody)
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::FingerPrintsDelete(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor", "lat1", "lon1", "lat2", "lon2")
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)

        val buid = (json \ "buid").as[String]
        val floor_number = (json \ "floor").as[String]
        val lat1 = (json \ "lat1").as[String]
        val lon1 = (json \ "lon1").as[String]
        val lat2 = (json \ "lat2").as[String]
        val lon2 = (json \ "lon2").as[String]


        try {
          val radioPoints: util.List[JsonObject] = ProxyDataSource.getIDatasource.getFingerPrintsBBox(buid, floor_number, lat1, lon1, lat2, lon2)
          if (radioPoints.isEmpty)
            return AnyResponseHelper.bad_request("FingerPrints does not exist or could not be retrieved!")

          for (i <- 0 until radioPoints.size())
            ProxyDataSource.getIDatasource.deleteFromKey(radioPoints.get(i).getString("id"))


          val res = JsonObject.empty()
          res.put("radioPoints", radioPoints)
          try //                if (request().getHeader("Accept-Encoding") != null && request().getHeader("Accept-Encoding").contains("gzip")) {
{
          //Regenerate the radiomap files
          val strPromise = F.Promise.pure("10")
          val intPromise = strPromise.map(new F.Function[String, Integer]() {
            override def apply(arg0: String): java.lang.Integer = {
              AnyplacePosition.updateFrozenRadioMap(buid, floor_number)
              0
            }
          })
          gzippedJSONOk(res.toString)
          //                }
          //                return AnyResponseHelper.ok(res.toString());
        } catch {
            case ioe: IOException =>
              return AnyResponseHelper.ok(res, "Successfully retrieved all FingerPrints!")
          }
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }

      }

      inner(request)
  }

   def FingerPrintsTimestampDelete() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody)
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::FingerPrintsTimestampDelete(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor", "lat1", "lon1", "lat2", "lon2","timestampX","timestampY")
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)

        val buid = (json \ "buid").as[String]
        val floor_number = (json \ "floor").as[String]
        val lat1 = (json \ "lat1").as[String]
        val lon1 = (json \ "lon1").as[String]
        val lat2 = (json \ "lat2").as[String]
        val lon2 = (json \ "lon2").as[String]
        val timestampX = (json \ "timestampX").as[String]
        val timestampY = (json \ "timestampY").as[String]


        try {
          val radioPoints: util.List[JsonObject] = ProxyDataSource.getIDatasource.getFingerPrintsTimestampBBox(buid, floor_number, lat1, lon1, lat2, lon2, timestampX, timestampY)
          if (radioPoints.isEmpty)
            return AnyResponseHelper.bad_request("FingerPrints does not exist or could not be retrieved!")

          for (i <- 0 until radioPoints.size())
            ProxyDataSource.getIDatasource.deleteFromKey(radioPoints.get(i).getString("id"))


          val res = JsonObject.empty()
          res.put("radioPoints", radioPoints)
          try //                if (request().getHeader("Accept-Encoding") != null && request().getHeader("Accept-Encoding").contains("gzip")) {
{
          //Regenerate the radiomap files
          val strPromise = F.Promise.pure("10")
          val intPromise = strPromise.map(new F.Function[String, Integer]() {
            override def apply(arg0: String): java.lang.Integer = {
              AnyplacePosition.updateFrozenRadioMap(buid, floor_number)
              0
            }
          })
          gzippedJSONOk(res.toString)
          //                }
          //                return AnyResponseHelper.ok(res.toString());
        } catch {
            case ioe: IOException =>
              return AnyResponseHelper.ok(res, "Successfully retrieved all FingerPrints!")
          }
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }

      }

      inner(request)
  }

  def FingerPrintsTime() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody)
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::FingerPrintsTime(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor")
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)

        val buid = (json \ "buid").as[String]
        val floor_number = (json \ "floor").as[String]


        try {
          val radioPoints: util.List[JsonObject] = ProxyDataSource.getIDatasource.getFingerPrintsTime(buid, floor_number)
          if (radioPoints.isEmpty)
            return AnyResponseHelper.bad_request("FingerPrints does not exist or could not be retrieved!")


          val res = JsonObject.empty()
          res.put("radioPoints", radioPoints)

          try //                if (request().getHeader("Accept-Encoding") != null && request().getHeader("Accept-Encoding").contains("gzip")) {
          gzippedJSONOk(res.toString)
          //                }
          //                return AnyResponseHelper.ok(res.toString());
          catch {
            case ioe: IOException =>
              return AnyResponseHelper.ok(res, "Successfully retrieved all FingerPrints!")
          }
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }

      }

      inner(request)
  }



  def findPosition() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody)
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::findPosition(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor","APs","algorithm_choice")
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)

        val buid = (json \ "buid").as[String]
        val floor_number = (json \ "floor").as[String]
        val accessPoints= (json\"APs").as[List[JsValue]]
        val algorithm_choice = (json\"algorithm_choice").as[Int]

        val rmapFile = new File("radiomaps_frozen" + AnyplaceServerAPI.URL_SEPARATOR + buid + AnyplaceServerAPI.URL_SEPARATOR +
          floor_number+AnyplaceServerAPI.URL_SEPARATOR+ "indoor-radiomap-mean.txt")

        if(!rmapFile.exists()){
           //Regenerate the radiomap files if not exist
             AnyplacePosition.updateFrozenRadioMap(buid, floor_number)
        }

        val latestScanList: util.ArrayList[location.LogRecord] = null
        var i=0
        for (i <- 0 until accessPoints.size) {
          val bssid= (accessPoints(i) \ "bssid").as[String]
          val rss =(accessPoints(i) \ "rss").as[Int]
          latestScanList.add(new location.LogRecord(bssid,rss))

        }

        val radioMap:location.RadioMap = new location.RadioMap(rmapFile)
        Algorithms.ProcessingAlgorithms(latestScanList,radioMap,algorithm_choice)
        return AnyResponseHelper.ok("Successfully found position.")
      }

      inner(request)
  }



  def getRadioHeatmapBbox = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody)
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplacePosition::radioDownloadFloor(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "coordinates_lat", "coordinates_lon", "floor", "buid", "range")
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val lat = (json \ "coordinates_lat").as[String]
        val lon = (json \ "coordinates_lon").as[String]
        val floor_number = (json \ "floor").as[String]
        val buid = (json \ "buid").as[String]
        val strRange = (json \ "range").as[String]
        val weight = (json \ "weight").as[String]
        val range = strRange.toInt
        try {
          var radioPoints: util.List[JsonObject] = null
          if (weight.compareTo("false") == 0) radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmapBBox2(lat, lon, buid, floor_number, range)
          else if (weight.compareTo("true") == 0) radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmapBBox(lat, lon, buid, floor_number, range)
          else if (weight.compareTo("no spatial") == 0) radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmapByBuildingFloor2(lat, lon, buid, floor_number, range)
          if (radioPoints == null)
            return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          val res = JsonObject.empty()
          res.put("radioPoints", radioPoints)
          try //                if (request().getHeader("Accept-Encoding") != null && request().getHeader("Accept-Encoding").contains("gzip")) {
          gzippedJSONOk(res.toString)
          //                }
          //                return AnyResponseHelper.ok(res.toString());
          catch {
            case ioe: IOException =>
              return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
          }
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def deleteRadiosInBox() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::deleteRadiosInBox(): " + json.toString)
        try {
          if (!ProxyDataSource.getIDatasource.deleteRadiosInBox()) return AnyResponseHelper.bad_request("Building already exists or could not be added!")
          return AnyResponseHelper.ok("Success")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def buildingAdd() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::buildingAdd(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "is_published", "name", "description",
          "url", "address", "coordinates_lat", "coordinates_lon", "access_token")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if ((json \ "access_token").getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendToOwnerId(owner_id.toString)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))
        try {
          var building: Building = null
          try {
            building = new Building(JsonObject.fromJson(json.toString()))
          } catch {
            case e: NumberFormatException => return AnyResponseHelper.bad_request("Building coordinates are invalid!")
          }
          if (!ProxyDataSource.getIDatasource.addJsonDocument(building.getId, 0, building.toCouchGeoJSON())) return AnyResponseHelper.bad_request("Building already exists or could not be added!")
          val res = JsonObject.empty()
          res.put("buid", building.getId)
          return AnyResponseHelper.ok(res, "Successfully added building!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def buildingUpdateCoOwners() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody()
        LPLogger.info("AnyplaceMapping::buildingUpdateCoOwners(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "access_token", "co_owners")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\\("access_token") == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendToOwnerId(owner_id)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))
        val buid = (json \ "buid").as[String]
        try {
          val stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(buid)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
          val building = new Building(stored_building)
          if (!ProxyDataSource.getIDatasource.replaceJsonDocument(building.getId, 0, building.appendCoOwners(json))) return AnyResponseHelper.bad_request("Building could not be updated!")
          return AnyResponseHelper.ok("Successfully updated building!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def buildingUpdateOwner() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::buildingUpdateCoOwners(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "access_token", "new_owner")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\("access_token").getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendToOwnerId(owner_id)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))
        val buid = (json \ "buid").as[String]
        var newOwner = (json \ "new_owner").as[String]
        newOwner = appendToOwnerId(newOwner)
        try {
          val stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(buid)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
          val building = new Building(stored_building)
          if (!ProxyDataSource.getIDatasource.replaceJsonDocument(building.getId, 0, building.changeOwner(newOwner))) return AnyResponseHelper.bad_request("Building could not be updated!")
          return AnyResponseHelper.ok("Successfully updated building!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def buildingUpdate() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::buildingUpdate(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "access_token")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\("access_token").getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendToOwnerId(owner_id)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))
        val buid = (json \ "buid").as[String]
        try {
          val stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(buid)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
          if (json.\("is_published").getOrElse(null) != null) {
            val is_published = (json \ "is_published").as[String]
            if (is_published == "true" || is_published == "false") stored_building.put("is_published", (json \ "is_published").as[String])
          }
          if (json.\("name").getOrElse(null) != null) stored_building.put("name", (json \ "name").as[String])
          if (json.\("bucode").getOrElse(null) != null) stored_building.put("bucode", (json \ "bucode").as[String])
          if (json.\("description").getOrElse(null) != null) stored_building.put("description", (json \ "description").as[String])
          if (json.\("url").getOrElse(null) != null) stored_building.put("url", (json \ "url").as[String])
          if (json.\("address").getOrElse(null) != null) stored_building.put("address", (json \ "address").as[String])
          if (json.\("coordinates_lat").getOrElse(null) != null) stored_building.put("coordinates_lat", (json \ "coordinates_lat").as[String])
          if (json.\("coordinates_lon").getOrElse(null) != null) stored_building.put("coordinates_lon", (json \ "coordinates_lon").as[String])
          val building = new Building(stored_building)
          if (!ProxyDataSource.getIDatasource.replaceJsonDocument(building.getId, 0, building.toCouchGeoJSON())) return AnyResponseHelper.bad_request("Building could not be updated!")
          return AnyResponseHelper.ok("Successfully updated building!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def buildingDelete() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::buildingDelete(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "access_token")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\("access_token").getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendToOwnerId(owner_id)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))
        val buid = (json \ "buid").as[String]
        try {
          val stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(buid).asInstanceOf[JsonObject]
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
        try {
          val all_items_failed = ProxyDataSource.getIDatasource.deleteAllByBuilding(buid)
          if (all_items_failed.size > 0) {
            val obj = JsonObject.empty()
            obj.put("ids", (all_items_failed))
            return AnyResponseHelper.bad_request(obj, "Some items related to the deleted building could not be deleted: " +
              all_items_failed.size +
              " items.")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
        val filePath = AnyPlaceTilerHelper.getRootFloorPlansDirFor(buid)
        try {
          val buidfile = new File(filePath)
          if (buidfile.exists()) HelperMethods.recDeleteDirFile(buidfile)
        } catch {
          case e: IOException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "] while deleting floor plans." +
            "\nAll related information is deleted from the database!")
        }
        return AnyResponseHelper.ok("Successfully deleted everything related to building!")
      }

      inner(request)
  }

  def buildingAll = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::buildingAll(): " + json.toString)
        try {

          val buildings = ProxyDataSource.getIDatasource.getAllBuildings
          val res = JsonObject.empty()
          res.put("buildings", buildings)
          try {
            gzippedJSONOk(res.toString)
          }

          catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all buildings!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def echo = Action { implicit request =>
    var response = Ok("Got request [" + request + "]")
    response
  }

  def buildingGetOne() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::buildingGet(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val buid = (json \ "buid").as[String]
        try {
          val building = ProxyDataSource.getIDatasource.getFromKeyAsJson(buid)
          if (building != null && building.get("buid") != null && building.get("coordinates_lat") != null &&
            building.get("coordinates_lon") != null &&
            building.get("owner_id") != null &&
            building.get("name") != null &&
            building.get("description") != null &&
            building.get("puid") == null &&
            building.get("floor_number") == null) {
            building.asInstanceOf[JsonObject].removeKey("owner_id")
            building.asInstanceOf[JsonObject].removeKey("co_owners")
            val res = JsonObject.empty()
            res.put("building", building)
            try {
              gzippedJSONOk(res.toString)
            } catch {
              case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all buildings!")
            }
          }
          return AnyResponseHelper.not_found("Building not found.")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def buildingAllByOwner() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::buildingAll(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "access_token")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\("access_token").getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendToOwnerId(owner_id)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))
        if (owner_id == null || owner_id.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        try {
          val buildings = ProxyDataSource.getIDatasource.getAllBuildingsByOwner(owner_id)
          val res = JsonObject.empty()
          res.put("buildings", (buildings))
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all buildings!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def buildingByBucode() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {

        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::buildingAll(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "bucode")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val bucode = (json \ "bucode").as[String]
        try {
          val buildings = ProxyDataSource.getIDatasource.getAllBuildingsByBucode(bucode)
          val res = JsonObject.empty()
          res.put("buildings", buildings)
          try {
            gzippedJSONOk(res.toString)
          }

          catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all buildings!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def buildingCoordinates() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::buildingCoordinates(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "access_token")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\("access_token").getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null) owner_id = ""
        owner_id = appendToOwnerId(owner_id)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))
        requiredMissing.addAll(JsonUtils.requirePropertiesInJson(json, "coordinates_lat", "coordinates_lon"))
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        try {
          val buildings = ProxyDataSource.getIDatasource.getAllBuildingsNearMe(owner_id, java.lang.Double.parseDouble((json \ "coordinates_lat").as[String]),
            java.lang.Double.parseDouble((json \ "coordinates_lon").as[String]))
          val res = JsonObject.empty()
          res.put("buildings", (buildings))
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all buildings near your position!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }


  /**
    * Retrieve the building Set.
    *
    * @return
    */
  def buildingSetAll = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody)
          return AnyResponseHelper
            .bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::buildingSetAll(): " + json.toString)
        var cuid = request.getQueryString("cuid").orNull
        if (cuid == null) cuid = (json \ "cuid").as[String]
        try {
          val campus = ProxyDataSource.getIDatasource.getBuildingSet(cuid)
          val buildings = ProxyDataSource.getIDatasource.getAllBuildings
          val result = new util.ArrayList[JsonObject]
          var cuname = ""
          var greeklish = ""
          var i = 0
          for (i <- 0 until campus.size) {
            val temp = campus.get(i)
            var j = 0
            for (j <- 0 until temp.getArray("buids").size) {
              if (j == 0) cuname = temp.get("name").toString
              if (j == 0) greeklish = temp.get("greeklish").toString
              var k = 0
              for (k <- 0 until buildings.size) { //a
                val temp2 = buildings.get(k)
                if (temp2.get("buid").toString.compareTo(temp.getArray("buids").get(j).toString) == 0)
                  result.add(temp2)
              }
            }

          }
          val res = JsonObject.empty()
          res.put("buildings", result)
          res.put("name", cuname)
          System.out.println(greeklish)
          if (greeklish == null) greeklish = "false"
          res.put("greeklish", greeklish)
          try //                if (request().getHeader("Accept-Encoding") != null && request().getHeader("Accept-Encoding").contains("gzip")) {
          gzippedJSONOk(res.toString)
          //                }
          //                return AnyResponseHelper.ok(res.toString());
          catch {
            case ioe: IOException =>
              AnyResponseHelper.ok(res, "Successfully retrieved all buildings Sets!")
          }
        } catch {
          case e: DatasourceException =>
            AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }


  /**
    * Adds a new building set to the database
    *
    * @return the newly created Building ID is included in the response if success
    */
  def buildingSetAdd = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody)
          return AnyResponseHelper
            .bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::buildingSetAdd(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "description", "name", "buids", "greeklish")
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        // get access token from url and check it against google's service
        if (json.\\("access_token") == null)
          return AnyResponseHelper.forbidden("Unauthorized1")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null)
          return AnyResponseHelper.forbidden("Unauthorized2")
        owner_id = appendToOwnerId(owner_id)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))
        try {
          val cuid = (json \ "cuid").as[String]
          val campus = ProxyDataSource.getIDatasource.BuildingSetsCuids(cuid)
          if (campus) return AnyResponseHelper.bad_request("Building set already exists!")
          else {
            var buildingset: BuildingSet = null
            try
              buildingset = new BuildingSet(JsonObject.fromJson(json.toString()))
            catch {
              case e: NumberFormatException =>
                return AnyResponseHelper.bad_request("Building coordinates are invalid!")
            }
            if (!ProxyDataSource.getIDatasource.addJsonDocument(buildingset.getId, 0, buildingset.toCouchGeoJSON))
              return AnyResponseHelper.bad_request("Building set already exists or could not be added!")
            val res = JsonObject.empty()
            res.put("cuid", buildingset.getId)
            return AnyResponseHelper.ok(res, "Successfully added building Set!")
          }
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }


  /**
    * Update the building information. Building to update is specified by buid
    *
    * @return
    */
  def campusUpdate = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody)
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::campusUpdate(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "cuid", "access_token")
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        // get access token from url and check it against google's service
        if (json.\\("access_token") == null)
          return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null)
          return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendToOwnerId(owner_id)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))
        val cuid = (json \ "cuid").as[String]
        try {
          val stored_campus = ProxyDataSource.getIDatasource().getFromKeyAsJson(cuid)
          if (stored_campus == null)
            return AnyResponseHelper.bad_request("Campus does not exist or could not be retrieved!")
          if (!isCampusOwner(stored_campus, owner_id))
            return AnyResponseHelper.unauthorized("Unauthorized")
          // check for values to update
          if (json.\\("name") != null) stored_campus.put("name", (json \ "name").as[String])
          if (json.\\("description") != null) stored_campus.put("description", (json \ "description").as[String])
          if (json.\\("cuidnew") != null) stored_campus.put("cuid", (json \ "cuid").as[String])
          val campus = new BuildingSet(stored_campus)
          if (!ProxyDataSource.getIDatasource().replaceJsonDocument(campus.getId(), 0, campus.toCouchGeoJSON()))
            return AnyResponseHelper.bad_request("Campus could not be updated!")
          return AnyResponseHelper.ok("Successfully updated campus!")
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }


  import java.io.IOException

  import datasources.{DatasourceException, ProxyDataSource}
  import oauth.provider.v2.models.OAuth2Request
  import utils.{AnyResponseHelper, JsonUtils, LPLogger}

  def buildingsetAllByOwner = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody)
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::buildingSetAll(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "access_token")
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        // get access token from url and check it against google's service
        if (json.\\("access_token") == null)
          return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null)
          return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendToOwnerId(owner_id)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))
        if (owner_id == null || owner_id.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        try {
          val buildingsets = ProxyDataSource.getIDatasource().getAllBuildingsetsByOwner(owner_id)
          val res = JsonObject.empty()
          res.put("buildingsets", buildingsets)
          try //                if (request().getHeader("Accept-Encoding") != null && request().getHeader("Accept-Encoding").contains("gzip")) {
          gzippedJSONOk(res.toString)
          //                }
          //                return AnyResponseHelper.ok(res.toString());
          catch {
            case ioe: IOException =>
              return AnyResponseHelper.ok(res, "Successfully retrieved all buildingsets!")
          }
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  /**
    * Delete the campus specified by cuid.
    *
    * @return
    */
  def campusDelete = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody)
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::campusDelete(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "cuid", "access_token")
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        // get access token from url and check it against google's service
        if (json.\\("access_token") == null)
          return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null)
          return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendToOwnerId(owner_id)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))
        val cuid = (json \ "cuid").as[String]
        try {
          val stored_campus = ProxyDataSource.getIDatasource().getFromKeyAsJson(cuid)
          if (stored_campus == null)
            return AnyResponseHelper.bad_request("Campus does not exist or could not be retrieved!")
          if (!isCampusOwner(stored_campus, owner_id))
            return AnyResponseHelper.unauthorized("Unauthorized")
          if (!ProxyDataSource.getIDatasource().deleteFromKey(cuid))
            return AnyResponseHelper.internal_server_error("Server Internal Error while trying to delete Campus")
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
        return AnyResponseHelper.ok("Successfully deleted everything related to building!")
      }

      inner(request)
  }

  private def isCampusOwner(campus: JsonObject, userId: String): Boolean = { // Admin
    if (userId.equals(ADMIN_ID))
      return true
    // Check if owner
    if (campus != null && campus.get("owner_id") != null && campus.getString("owner_id").equals(userId))
      return true
    false
  }

  def floorAdd() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::floorAdd(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "is_published", "buid", "floor_name",
          "description", "floor_number", "access_token")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\("access_token").getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendToOwnerId(owner_id)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))
        val buid = (json \ "buid").as[String]
        try {
          val stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(buid)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
        val floor_number = (json \ "floor_number").as[String]
        if (!Floor.checkFloorNumberFormat(floor_number)) return AnyResponseHelper.bad_request("Floor number cannot contain whitespace!")
        try {
          val floor = new Floor(JsonObject.fromJson(json.toString()))
          if (!ProxyDataSource.getIDatasource.addJsonDocument(floor.getId, 0, floor.toValidCouchJson().toString)) return AnyResponseHelper.bad_request("Floor already exists or could not be added!")
          return AnyResponseHelper.ok("Successfully added floor " + floor_number + "!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def floorUpdate() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::floorUpdate(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor_number", "access_token")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\("access_token").getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendToOwnerId(owner_id)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))
        val buid = (json \ "buid").as[String]
        try {
          val stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(buid)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
        val floor_number = (json \ "fllor_number").as[String]
        if (!Floor.checkFloorNumberFormat(floor_number)) return AnyResponseHelper.bad_request("Floor number cannot contain whitespace!")
        try {
          val fuid = Floor.getId(buid, floor_number)
          val stored_floor = ProxyDataSource.getIDatasource.getFromKeyAsJson(fuid)
          if (stored_floor == null) return AnyResponseHelper.bad_request("Floor does not exist or could not be retrieved!")
          if (json.\("is_published").getOrElse(null) != null) stored_floor.put("is_published", (json \ "is_published").as[String])
          if (json.\("floor_name").getOrElse(null) != null) stored_floor.put("floor_name", (json \ "floor_name").as[String])
          if (json.\("description").getOrElse(null) != null) stored_floor.put("description", (json \ "description").as[String])
          val floor = new Floor(stored_floor)
          if (!ProxyDataSource.getIDatasource.replaceJsonDocument(floor.getId, 0, floor.toValidCouchJson().toString)) return AnyResponseHelper.bad_request("Floor could not be updated!")
          return AnyResponseHelper.ok("Successfully updated floor!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def floorDelete() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::floorDelete(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor_number", "access_token")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\("access_token").getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendToOwnerId(owner_id)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))
        val buid = (json \ "buid").as[String]
        val floor_number = (json \ "floor_name").as[String]
        try {
          val stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(buid)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
        try {
          val all_items_failed = ProxyDataSource.getIDatasource.deleteAllByFloor(buid, floor_number)
          if (all_items_failed.size > 0) {
            val obj = JsonObject.empty()
            obj.put("ids", all_items_failed)
            return AnyResponseHelper.bad_request(obj, "Some items related to the deleted floor could not be deleted: " +
              all_items_failed.size +
              " items.")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
        val filePath = AnyPlaceTilerHelper.getFloorPlanFor(buid, floor_number)
        try {
          val floorfile = new File(filePath)
          if (floorfile.exists()) HelperMethods.recDeleteDirFile(floorfile)
        } catch {
          case e: IOException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "] while deleting floor plan." +
            "\nAll related information is deleted from the database!")
        }
        return AnyResponseHelper.ok("Successfully deleted everything related to the floor!")
      }

      inner(request)
  }

  def floorAll() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::floorAll(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val buid = (json \ "buid").as[String]
        try {
          val buildings = ProxyDataSource.getIDatasource.floorsByBuildingAsJson(buid)
          val res = JsonObject.empty()
          res.put("floors", buildings)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all floors!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def poisAdd() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::poisAdd(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "is_published", "buid", "floor_name",
          "floor_number", "name", "pois_type", "is_door", "is_building_entrance", "coordinates_lat", "coordinates_lon",
          "access_token")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\("access_token").getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendToOwnerId(owner_id)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))
        val buid = (json \ "buid").as[String]
        try {
          val stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(buid)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
        try {
          val poi = new Poi(JsonObject.fromJson(json.toString()))
          if (!ProxyDataSource.getIDatasource.addJsonDocument(poi.getId, 0, poi.toCouchGeoJSON())) return AnyResponseHelper.bad_request("Poi already exists or could not be added!")
          val res = JsonObject.empty()
          res.put("puid", poi.getId)
          return AnyResponseHelper.ok(res, "Successfully added poi!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  /**
    * Adds a new poi category to the database
    *
    * @return the newly created cat ID is included in the response if success
    */
  def categoryAdd = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody)
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::buildingSetAdd(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "poistypeid", "poistype", "owner_id", "types")
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        // get access token from url and check it against google's service
        if (json.\\("access_token") == null)
          return AnyResponseHelper.forbidden("Unauthorized1")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null)
          return AnyResponseHelper.forbidden("Unauthorized2")
        owner_id = appendToOwnerId(owner_id)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))
        try {
          var poiscategory: PoisCategory = null
          try
            poiscategory = new PoisCategory(json.asInstanceOf[JsonObject])
          catch {
            case e: NumberFormatException =>
              return AnyResponseHelper.bad_request("Bad request!")
          }
          if (!ProxyDataSource.getIDatasource.addJsonDocument(poiscategory.getId, 0, poiscategory.toCouchGeoJSON))
            return AnyResponseHelper.bad_request("Building set already exists or could not be added!")
          val res = JsonObject.empty()
          res.put("poistypeid", poiscategory.getId)
          return AnyResponseHelper.ok(res, "Successfully added Pois Category!")
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def poisUpdate() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::poisUpdate(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "puid", "buid", "access_token")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\("access_token").getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendToOwnerId(owner_id)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))
        val puid = (json \ "puid").as[String]
        val buid = (json \ "buid").as[String]
        try {
          val stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(buid)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
        try {
          val stored_poi = ProxyDataSource.getIDatasource.getFromKeyAsJson(puid)
          if (stored_poi == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (json.\("is_published").getOrElse(null) != null) {
            val is_published = (json \ "is_published").as[String]
            if (is_published == "true" || is_published == "false") stored_poi.put("is_published", (json \ "is_published").as[String])
          }
          if (json.\("name").getOrElse(null) != null) stored_poi.put("name", (json \ "name").as[String])
          if (json.\("description").getOrElse(null) != null) stored_poi.put("description", (json \ "description").as[String])
          if (json.\("url").getOrElse(null) != null) stored_poi.put("url", (json \ "url").as[String])
          if (json.\("pois_type").getOrElse(null) != null) stored_poi.put("pois_type", (json \ "pois_type").as[String])
          if (json.\("is_door").getOrElse(null) != null) {
            val is_door = (json \ "is_door").as[String]
            if (is_door == "true" || is_door == "false") stored_poi.put("is_door", (json \ "is_door").as[String])
          }
          if (json.\("is_building_entrance").getOrElse(null) != null) {
            val is_building_entrance = (json \ "is_building_entrance").as[String]
            if (is_building_entrance == "true" || is_building_entrance == "false") stored_poi.put("is_building_entrance", (json \ "is_building_entrance").as[String])
          }
          if (json.\("image").getOrElse(null) != null) stored_poi.put("image", (json \ "image").as[String])
          if (json.\("coordinates_lat").getOrElse(null) != null) stored_poi.put("coordinates_lat", (json \ "coordinates_lat").as[String])
          if (json.\("coordinates_lon").getOrElse(null) != null) stored_poi.put("coordinates_lon", (json \ "coordinates_lon").as[String])
          val poi = new Poi(stored_poi)
          if (!ProxyDataSource.getIDatasource.replaceJsonDocument(poi.getId, 0, poi.toCouchGeoJSON())) return AnyResponseHelper.bad_request("Poi could not be updated!")
          return AnyResponseHelper.ok("Successfully updated poi!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def poisDelete() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::poiDelete(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "puid", "buid", "access_token")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\("access_token").getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendToOwnerId(owner_id)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))
        val buid = (json \ "buid").as[String]
        val puid = (json \ "puid").as[String]
        try {
          val stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(buid)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
        try {
          val all_items_failed = ProxyDataSource.getIDatasource.deleteAllByPoi(puid)
          if (all_items_failed.size > 0) {
            val obj = JsonObject.empty()
            obj.put("ids", (all_items_failed))
            return AnyResponseHelper.bad_request(obj, "Some items related to the deleted poi could not be deleted: " +
              all_items_failed.size +
              " items.")
          }
          return AnyResponseHelper.ok("Successfully deleted everything related to the poi!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }

      }

      inner(request)
  }

  def poisByFloor() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::poisByFloor(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor_number")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val buid = (json \ "buid").as[String]
        val floor_number = (json \ "floor_number").as[String]
        try {
          val pois = ProxyDataSource.getIDatasource.poisByBuildingFloorAsJson(buid, floor_number)
          val res = JsonObject.empty()
          res.put("pois", (pois))
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all pois from floor " + floor_number +
              "!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def poisByBuid() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {

        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::poisByBuid(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val buid = (json \ "buid").as[String]
        try {
          val pois = ProxyDataSource.getIDatasource.poisByBuildingAsJson(buid)
          val res = JsonObject.empty()
          res.put("pois", (pois))
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all pois from building.")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  /**
    * Retrieve all the pois of a cuid combination.
    *
    * @return
    */
  def poisAll = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {

        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody)
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        var cuid = request.getQueryString("cuid").orNull
        if (cuid == null) cuid = (json \ "cuid").as[String]
        var letters = request.getQueryString("letters").orNull
        if (letters == null) letters = (json \ "letters").as[String]
        var buid = request.getQueryString("buid").orNull
        if (buid == null) buid = (json \ "buid").as[String]
        var greeklish = request.getQueryString("greeklish").orNull
        if (greeklish == null) greeklish = (json \ "greeklish").as[String]
        try {
          var result: util.List[JsonObject] = new util.ArrayList[JsonObject]
          if (cuid.compareTo("") == 0) result = ProxyDataSource.getIDatasource.poisByBuildingAsJson3(buid, letters)
          else if (greeklish.compareTo("true") == 0) result = ProxyDataSource.getIDatasource.poisByBuildingAsJson2GR(cuid, letters)
          else result = ProxyDataSource.getIDatasource.poisByBuildingAsJson2(cuid, letters)
          val res = JsonObject.empty()
          res.put("pois", result)
          try
            gzippedJSONOk(res.toString)
          catch {
            case ioe: IOException =>
              return AnyResponseHelper.ok(res, "Successfully retrieved all pois from building.")
          }
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }


  /**
    * Retrieve all the pois of a building/floor combination.
    *
    * @return
    */
  def poisByBuidincConnectors = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody)
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::poisByBuidincConnectors(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid")
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val buid = (json \ "buid").as[String]
        try {
          val pois = ProxyDataSource.getIDatasource.poisByBuildingIDAsJson(buid)
          val res = JsonObject.empty()
          res.put("pois", pois)
          try //                if (request().getHeader("Accept-Encoding") != null && request().getHeader("Accept-Encoding").contains("gzip")) {
          gzippedJSONOk(res.toString)
          //                }
          //                returnreturn AnyResponseHelper.ok(res.toString());
          catch {
            case ioe: IOException =>
              return AnyResponseHelper.ok(res, "Successfully retrieved all pois from buid " + buid + "!")
          }
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  /**
    * Retrieve all the pois types by owner.
    *
    * @return
    */
  def poisTypes = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody)
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::poisTypes(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "access_token")
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        // get access token from url and check it against google's service
        if (json.\\("access_token") == null)
          return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null)
          return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendToOwnerId(owner_id)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))
        if (owner_id == null || owner_id.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        try {
          val poistypes = ProxyDataSource.getIDatasource.getAllPoisTypesByOwner(owner_id)
          val res = JsonObject.empty()
          res.put("poistypes", poistypes)
          try //                if (request().getHeader("Accept-Encoding") != null && request().getHeader("Accept-Encoding").contains("gzip")) {
          gzippedJSONOk(res.toString)
          //                }
          //                returnreturn AnyResponseHelper.ok(res.toString());
          catch {
            case ioe: IOException =>
              return AnyResponseHelper.ok(res, "Successfully retrieved all poistypes!")
          }
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def connectionAdd() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {

        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::connectionAdd(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "is_published", "pois_a", "floor_a",
          "buid_a", "pois_b", "floor_b", "buid_b", "buid", "edge_type", "access_token")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\("access_token").getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendToOwnerId(owner_id)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))
        val buid1 = (json \ "buid_a").as[String]
        val buid2 = (json \ "buid_b").as[String]
        try {
          var stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(buid1)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
          stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(buid2)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
        val edge_type = (json \ "edge_type").as[String]
        if (edge_type != Connection.EDGE_TYPE_ELEVATOR && edge_type != Connection.EDGE_TYPE_HALLWAY &&
          edge_type != Connection.EDGE_TYPE_ROOM &&
          edge_type != Connection.EDGE_TYPE_OUTDOOR &&
          edge_type != Connection.EDGE_TYPE_STAIR) return AnyResponseHelper.bad_request("Invalid edge type specified.")
        val pois_a = (json \ "pois_a").as[String]
        val pois_b = (json \ "pois_b").as[String]
        try {
          val weight = calculateWeightOfConnection(pois_a, pois_b)
          JsonObject.fromJson(json.toString()).put("weight", java.lang.Double.toString(weight))
          if (edge_type == Connection.EDGE_TYPE_ELEVATOR || edge_type == Connection.EDGE_TYPE_STAIR) {
          }
          val conn = new Connection(JsonObject.fromJson(json.toString()))
          if (!ProxyDataSource.getIDatasource.addJsonDocument(conn.getId, 0, conn.toValidCouchJson().toString)) return AnyResponseHelper.bad_request("Connection already exists or could not be added!")
          val res = JsonObject.empty()
          res.put("cuid", conn.getId)
          return AnyResponseHelper.ok(res, "Successfully added new connection!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def connectionUpdate() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::connectionUpdate(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "pois_a", "pois_b", "buid_a", "buid_b",
          "access_token")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\("access_token").getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendToOwnerId(owner_id)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))
        val buid1 = (json \ "buid_a").as[String]
        val buid2 = (json \ "buid_b").as[String]
        try {
          var stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(buid1)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
          stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(buid2)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
        try {
          val pois_a = (json \ "pois_a").as[String]
          val pois_b = (json \ "pois_b").as[String]
          val cuid = Connection.getId(pois_a, pois_b)
          val stored_conn = ProxyDataSource.getIDatasource.getFromKeyAsJson(cuid)
          if (stored_conn == null) return AnyResponseHelper.bad_request("Connection does not exist or could not be retrieved!")
          if (json.\("is_published").getOrElse(null) != null) {
            val is_published = (json \ "is_published").as[String]
            if (is_published == "true" || is_published == "false") stored_conn.put("is_published", (json \ "is_published").as[String])
          }
          if (json.\("edge_type").getOrElse(null) != null) {
            val edge_type = (json \ "edge_type").as[String]
            if (edge_type != Connection.EDGE_TYPE_ELEVATOR && edge_type != Connection.EDGE_TYPE_HALLWAY &&
              edge_type != Connection.EDGE_TYPE_ROOM &&
              edge_type != Connection.EDGE_TYPE_OUTDOOR &&
              edge_type != Connection.EDGE_TYPE_STAIR) return AnyResponseHelper.bad_request("Invalid edge type specified.")
            stored_conn.put("edge_type", edge_type)
          }
          val conn = new Connection(stored_conn)
          if (!ProxyDataSource.getIDatasource.replaceJsonDocument(conn.getId, 0, conn.toValidCouchJson().toString)) return AnyResponseHelper.bad_request("Connection could not be updated!")
          return AnyResponseHelper.ok("Successfully updated connection!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def connectionDelete() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {

        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::poiDelete(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "pois_a", "pois_b", "buid_a", "buid_b",
          "access_token")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\("access_token").getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendToOwnerId(owner_id)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))
        val buid1 = (json \ "buid_a").as[String]
        val buid2 = (json \ "buid_b").as[String]
        try {
          var stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(buid1)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
          stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(buid2)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
        val pois_a = (json \ "pois_a").as[String]
        val pois_b = (json \ "pois_b").as[String]
        try {
          val cuid = Connection.getId(pois_a, pois_b)
          val all_items_failed = ProxyDataSource.getIDatasource.deleteAllByConnection(cuid)
          if (all_items_failed == null) {
            LPLogger.info("AnyplaceMapping::connectionDelete(): " + cuid + " not found.")
            return AnyResponseHelper.bad_request("POI Connection not found")
          }
          if (all_items_failed.size > 0) {
            val obj = JsonObject.empty()
            obj.put("ids", (all_items_failed))
            return AnyResponseHelper.bad_request(obj, "Some items related to the deleted connection could not be deleted: " +
              all_items_failed.size +
              " items.")
          }
          return AnyResponseHelper.ok("Successfully deleted everything related to the connection!")
        } catch {
          case e: DatasourceException => AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def connectionsByFloor() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {

        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::poisByFloor(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor_number")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val buid = (json \ "buid").as[String]
        val floor_number = (json \ "floor_number").as[String]
        try {
          val pois = ProxyDataSource.getIDatasource.connectionsByBuildingFloorAsJson(buid, floor_number)
          val res = JsonObject.empty()
          res.put("connections", (pois))
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all pois from floor " + floor_number +
              "!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  import java.io.IOException

  import datasources.{DatasourceException, ProxyDataSource}
  import oauth.provider.v2.models.OAuth2Request
  import utils.{AnyResponseHelper, JsonUtils, LPLogger}

  /**
    * Retrieve all the pois of a building/floor combination.
    *
    * @return
    */
  def connectionsByallFloors = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody)
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::connectionsByallFloors(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid")
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val buid = (json \ "buid").as[String]
        try {
          val pois = ProxyDataSource.getIDatasource.connectionsByBuildingAllFloorsAsJson(buid)
          val res = JsonObject.empty()
          res.put("connections", pois)
          try //                if (request().getHeader("Accept-Encoding") != null && request().getHeader("Accept-Encoding").contains("gzip")) {
          gzippedJSONOk(res.toString)
          //                }
          //                returnreturn AnyResponseHelper.ok(res.toString());
          catch {
            case ioe: IOException =>
              return AnyResponseHelper.ok(res, "Successfully retrieved all pois from all floors !")
          }
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  private def calculateWeightOfConnection(pois_a: String, pois_b: String) = {
    var lat_a = 0.0
    var lon_a = 0.0
    var lat_b = 0.0
    var lon_b = 0.0
    val nf = NumberFormat.getInstance(Locale.ENGLISH)
    val pa = ProxyDataSource.getIDatasource.getFromKeyAsJson(pois_a)
    if (pa == null) {
      lat_a = 0.0
      lon_a = 0.0
    } else try {
      lat_a = nf.parse(pa.getString("coordinates_lat")).doubleValue()
      lon_a = nf.parse(pa.getString("coordinates_lon")).doubleValue()
    } catch {
      case e: ParseException => e.printStackTrace()
    }
    val pb = ProxyDataSource.getIDatasource.getFromKeyAsJson(pois_b)
    if (pb == null) {
      lat_b = 0.0
      lon_b = 0.0
    } else try {
      lat_b = nf.parse(pb.getString("coordinates_lat")).doubleValue()
      lon_b = nf.parse(pb.getString("coordinates_lon")).doubleValue()
    } catch {
      case e: ParseException => e.printStackTrace()
    }
    GeoPoint.getDistanceBetweenPoints(lat_a, lon_a, lat_b, lon_b, "K")
  }

  def serveFloorPlanBinary(buid: String, floor_number: String) = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::serveFloorPlan(): " + json.toString)
        val filePath = AnyPlaceTilerHelper.getFloorPlanFor(buid, floor_number)
        LPLogger.info("requested: " + filePath)
        try {
          val file = new File(filePath)
          if (!file.exists() || !file.canRead()) return AnyResponseHelper.bad_request("Requested floor plan does not exist or cannot be read! (" +
            floor_number +
            ")")
          Ok.sendFile(file)
        } catch {
          case e: FileNotFoundException => return AnyResponseHelper.internal_server_error("Could not read floor plan.")
        }
      }

      inner(request)
  }

  def serveFloorPlanTilesZip(buid: String, floor_number: String) = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::serveFloorPlanTilesZip(): " + json.toString)
        if (!Floor.checkFloorNumberFormat(floor_number)) return AnyResponseHelper.bad_request("Floor number cannot contain whitespace!")
        val filePath = AnyPlaceTilerHelper.getFloorTilesZipFor(buid, floor_number)
        LPLogger.info("requested: " + filePath)
        try {
          val file = new File(filePath)
          if (!file.exists() || !file.canRead()) return AnyResponseHelper.bad_request("Requested floor plan does not exist or cannot be read! (" +
            floor_number +
            ")")
          Ok.sendFile(file)
        } catch {
          case e: FileNotFoundException => return AnyResponseHelper.internal_server_error("Could not read floor plan.")
        }
      }

      inner(request)
  }

  def serveFloorPlanTilesZipLink(buid: String, floor_number: String) = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::serveFloorPlanTilesZipLink(): " + json.toString)
        if (!Floor.checkFloorNumberFormat(floor_number)) return AnyResponseHelper.bad_request("Floor number cannot contain whitespace!")
        val filePath = AnyPlaceTilerHelper.getFloorTilesZipFor(buid, floor_number)
        LPLogger.info("requested: " + filePath)
        val file = new File(filePath)
        if (!file.exists() || !file.canRead()) return AnyResponseHelper.bad_request("Requested floor plan does not exist or cannot be read! (" +
          floor_number +
          ")")
        val res = JsonObject.empty()
        res.put("tiles_archive", AnyPlaceTilerHelper.getFloorTilesZipLinkFor(buid, floor_number))
        return AnyResponseHelper.ok(res, "Successfully fetched link for the tiles archive!")
      }

      inner(request)
  }

  def serveFloorPlanTilesStatic(buid: String, floor_number: String, path: String) = Action {
    def inner(): Result = {
      LPLogger.info("AnyplaceMapping::serveFloorPlanTilesStatic(): " + buid +
        ":" +
        floor_number +
        ":" +
        path)
      if (path == null || buid == null || floor_number == null ||
        path.trim().isEmpty ||
        buid.trim().isEmpty ||
        floor_number.trim().isEmpty) NotFound(<h1>Page not found</h1>)
      var filePath: String = null
      filePath = if (path == AnyPlaceTilerHelper.FLOOR_TILES_ZIP_NAME) AnyPlaceTilerHelper.getFloorTilesZipFor(buid,
        floor_number) else AnyPlaceTilerHelper.getFloorTilesDirFor(buid, floor_number) +
        path
      LPLogger.info("static requested: " + filePath)
      try {
        val file = new File(filePath)
        if (!file.exists() || !file.canRead()) return AnyResponseHelper.not_found("File requested not found")
        Ok.sendFile(file)
      } catch {
        case e: FileNotFoundException => return AnyResponseHelper.internal_server_error("Could not read floor plan.")
      }
    }

    inner()
  }

  def serveFloorPlanBase64(buid: String, floor_number: String) = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::serveFloorPlanBase64(): " + json.toString)
        val filePath = AnyPlaceTilerHelper.getFloorPlanFor(buid, floor_number)
        LPLogger.info("requested: " + filePath)
        val file = new File(filePath)
        try {
          if (!file.exists() || !file.canRead()) return AnyResponseHelper.bad_request("Requested floor plan does not exist or cannot be read! (" +
            floor_number +
            ")")
          try {
            val s = encodeFileToBase64Binary(filePath)
            try {
              gzippedOk(s)
            } catch {
              case ioe: IOException => Ok(s)
            }
          } catch {
            case e: IOException => return AnyResponseHelper.bad_request("Requested floor plan cannot be encoded in base64 properly! (" +
              floor_number +
              ")")
          }
        } catch {
          case e: Exception => return AnyResponseHelper.internal_server_error("Unknown server error during floor plan delivery!")
        }
      }

      inner(request)
  }


  /**
    * Returns the floorplan in base64 form. Used by the Anyplace websites
    *
    * @param buid
    * @param floor_number
    * @return
    */
  def serveFloorPlanBase64all(buid: String, floor_number: String) = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody)
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::serveFloorPlanBase64all(): " + json.toString + " " + floor_number)
        val floors = floor_number.split(" ")
        val all_floors = new util.ArrayList[String]
        var z = 0
        while ( {
          z < floors.length
        }) {
          val filePath = AnyPlaceTilerHelper.getFloorPlanFor(buid, floors(z))
          LPLogger.info("requested: " + filePath)
          val file = new File(filePath)
          try
              if (!file.exists || !file.canRead) {
                all_floors.add("")
              }
              else try {
                val s = encodeFileToBase64Binary(filePath)
                all_floors.add(s)
              } catch {
                case e: IOException =>
                  return AnyResponseHelper.bad_request("Requested floor plan cannot be encoded in base64 properly! (" + floors(z) + ")")
              }
          catch {
            case e: Exception =>
              return AnyResponseHelper.internal_server_error("Unknown server error during floor plan delivery!")
          }

          {
            z += 1;
            z - 1
          }
        }
        val res = JsonObject.empty()
        res.put("all_floors", all_floors)
        try
          gzippedJSONOk(res.toString)
        catch {
          case ioe: IOException =>
            return AnyResponseHelper.ok(res, "Successfully retrieved all floors!")
        }
      }

      inner(request)
  }

  private def encodeFileToBase64Binary(fileName: String) = {
    val file = new File(fileName)
    val bytes = loadFile(file)
    val encoded = Base64.encodeBase64(bytes)
    val encodedString = new String(encoded)
    encodedString
  }

  private def loadFile(file: File) = {
    val is = new FileInputStream(file)
    val length = file.length
    if (length > java.lang.Integer.MAX_VALUE) {
    }
    val bytes = Array.ofDim[Byte](length.toInt)
    var offset = 0
    var numRead = 0
    do {
      numRead = is.read(bytes, offset, bytes.length - offset)
      offset += numRead
    } while ((offset < bytes.length && numRead >= 0))
    if (offset < bytes.length) throw new IOException("Could not completely read file " + file.getName)
    is.close()
    bytes
  }

  def floorPlanUpload() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val body = anyReq.getMultipartFormData()
        if (body == null) return AnyResponseHelper.bad_request("Invalid request type - Not Multipart!")
        var floorplan = body.file("floorplan").get
        if (floorplan == null) return AnyResponseHelper.bad_request("Cannot find the floor plan file in your request!")
        val urlenc = body.asFormUrlEncoded
        val json_str = urlenc.get("json").get(0)
        if (json_str == null) return AnyResponseHelper.bad_request("Cannot find json in the request!")
        var json: JsonObject = null
        try {
          json = JsonObject.fromJson(json_str)
        } catch {
          case e: IOException => return AnyResponseHelper.bad_request("Cannot parse json in the request!")
        }
        LPLogger.info("Floorplan Request[json]: " + json.toString)
        LPLogger.info("Floorplan Request[floorplan]: " + floorplan.filename)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor_number", "bottom_left_lat",
          "bottom_left_lng", "top_right_lat", "top_right_lng")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val buid = json.getString("buid")
        val floor_number = json.getString("floor_number")
        val bottom_left_lat = json.getString("bottom_left_lat")
        val bottom_left_lng = json.getString("bottom_left_lng")
        val top_right_lat = json.getString("top_right_lat")
        val top_right_lng = json.getString("top_right_lng")
        val fuid = Floor.getId(buid, floor_number)
        try {
          val stored_floor = ProxyDataSource.getIDatasource.getFromKeyAsJson(fuid)
          if (stored_floor == null) return AnyResponseHelper.bad_request("Floor does not exist or could not be retrieved!")
          stored_floor.put("bottom_left_lat", bottom_left_lat)
          stored_floor.put("bottom_left_lng", bottom_left_lng)
          stored_floor.put("top_right_lat", top_right_lat)
          stored_floor.put("top_right_lng", top_right_lng)
          if (!ProxyDataSource.getIDatasource.replaceJsonDocument(fuid, 0, stored_floor.toString))
            return AnyResponseHelper.bad_request("Floor plan could not be updated in the database!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Error while reading from our backend service!")
        }
        var floor_file: File = null
        try {
          floor_file = AnyPlaceTilerHelper.storeFloorPlanToServer(buid, floor_number, floorplan.ref.file)
        } catch {
          case e: AnyPlaceException => return AnyResponseHelper.bad_request("Cannot save floor plan on the server!")
        }
        val top_left_lat = top_right_lat
        val top_left_lng = bottom_left_lng
        try {
          AnyPlaceTilerHelper.tileImage(floor_file, top_left_lat, top_left_lng)
        } catch {
          case e: AnyPlaceException => return AnyResponseHelper.bad_request("Could not create floor plan tiles on the server!")
        }
        LPLogger.info("Successfully tiled [" + floor_file.toString + "]")
        return AnyResponseHelper.ok("Successfully updated floor plan!")
      }

      inner(request)
  }

  def addAccount() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceAccounts::addAccount():: ")
        val notFound = JsonUtils.requirePropertiesInJson(json, "access_token", "type")
        if (!notFound.isEmpty) return AnyResponseHelper.requiredFieldsMissing(notFound)
        if (json.\("access_token").getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyOwnerId((json \ "access_token").as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendToOwnerId(owner_id)
        json = json.as[JsObject] + ("owner_id" -> Json.toJson(owner_id))

        val newAccount = new Account(JsonObject.fromJson(json.toString()))
        try {
          if (!ProxyDataSource.getIDatasource.addJsonDocument(newAccount.getId, 0, newAccount.toValidCouchJson().toString)) return AnyResponseHelper.ok("Returning user.")
          val res = JsonObject.empty()
          return AnyResponseHelper.ok("New user.")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }

      inner(request)
  }

  private def isBuildingOwner(building: JsonObject, userId: String): Boolean = {
    // Admin
    if (userId.equals(ADMIN_ID)) return true
    if (building != null && building.get("owner_id") != null &&
      building.getString("owner_id").equals(userId)) return true
    false
  }

  private def isBuildingCoOwner(building: JsonObject, userId: String): Boolean = {
    // Admin
    if (userId.equals(ADMIN_ID)) return true
    if (building != null) {
      val cws = building.getArray("co_owners")
      if (cws != null) {
        val it = cws.iterator()
        while (it.hasNext) if (it.next().toString.equals(userId)) return true
      }
    }
    false
  }


  private def gzippedJSONOk(body: String) = {
    val gzipv = gzip(body)
    Ok(gzipv.toByteArray).withHeaders(("Content-Encoding", "gzip"),
      ("Content-Length", gzipv.size + ""),
      ("Content-Type", "application/json"))
  }

  private def gzippedOk(body: String) = {
    val gzipv = gzip(body)
    Ok(gzipv.toByteArray).withHeaders(("Content-Encoding", "gzip"), ("Content-Length", gzipv.size + ""))
  }

  private def gzip(input: String) = {
    val inputStream = new ByteArrayInputStream(input.getBytes)
    val stringOutputStream = new ByteArrayOutputStream((input.length * 0.75).toInt)
    val gzipOutputStream = new GZIPOutputStream(stringOutputStream)
    val buf = Array.ofDim[Byte](5000)
    var len = 0
    len = inputStream.read(buf)
    while (len > 0) {
      gzipOutputStream.write(buf, 0, len)
      len = inputStream.read(buf)
    }
    inputStream.close()
    gzipOutputStream.close()
    stringOutputStream
  }

  def getAccesHeatmapByBuildingFloor() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::getAccesHeatmapByBuildingFloor(): " + json.toString)
        val requiredMissing = JsonUtils.requirePropertiesInJson(json, "floor", "buid")
        if (!requiredMissing.isEmpty) {
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        }
        val floor_number = (json \ "floor").as[String]
        val buid = (json \ "buid").as[String]
        val cut_k_features = (json \ "cut_k_features").asOpt[Int]
        //Default 5 meter grid step
        val h = (json \ "h").asOpt[Double].getOrElse(5.0)

        if (!Floor.checkFloorNumberFormat(floor_number)) {
          return AnyResponseHelper.bad_request("Floor number cannot contain whitespace!")
        }
        try {
          val rm = getRadioMapMeanByBuildingFloor(buid = buid, floor_number = floor_number)
          if (rm.isEmpty) {
            return AnyResponseHelper.bad_request("Area not supported yet!")
          } else {
            val (latlon_predict, crlbs) = getAccesMap(rm = rm.get, buid = buid, floor_number = floor_number,
              cut_k_features = cut_k_features, h = h)
            val res = JsonObject.empty()
            res.put("geojson", JsonObject.fromJson(latlon_predict.toGeoJSON().toString))
            res.put("crlb", JsonArray.from(new util.ArrayList[Double](crlbs.toArray.toList)))
            return AnyResponseHelper.ok(res, "Successfully served ACCES map.")
          }
        } catch {
          case e: FileNotFoundException => return AnyResponseHelper.internal_server_error("Cannot create radio map due to Server FileIO error!")
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
          case e: IOException => return AnyResponseHelper.internal_server_error("Cannot create radio map due to Server FileIO error!")
          case e: Exception => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
          case _: Throwable => return AnyResponseHelper.internal_server_error("Server Internal Error [" + "]")
        }
      }

      inner(request)
  }


  def maintenance() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::deleteNotValidDocuments(): " + json.toString)
        try {
          if (!ProxyDataSource.getIDatasource.deleteNotValidDocuments()) return AnyResponseHelper.bad_request("None valid documents!")
          return AnyResponseHelper.ok("Success")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }
      inner(request)
  }


  private def getAccesMap(rm: RadioMapMean,
                          buid: String, floor_number: String,
                          cut_k_features: Option[Int], h: Double): (GeoJSONMultiPoint, DenseVector[Double]) = {

    val hm = rm.getGroupLocationRSS_HashMap()
    val keys = hm.keySet()

    val list_latlon = ListBuffer[GeoPoint]()
    val list_rss = ListBuffer[DenseVector[Double]]()

    val m = rm.getMacAdressList().size()
    for (key <- keys) {
      val lrhm = hm.get(key)
      for (loc: String <- lrhm.keySet()) {
        val rss: util.List[String] = lrhm.get(loc)
        val rss_vec = DenseVector.zeros[Double](m)
        for (i <- 0 until rss.size()) {
          rss_vec(i) = rss.get(i).toDouble
        }
        val slat_slon = loc.split(" ")
        val point = new GeoPoint(lat = slat_slon(0), lon = slat_slon(1))
        list_latlon.append(point)
        list_rss.append(rss_vec)
      }
    }
    val n = rm.getOrderList().size()
    val multipoint = new GeoJSONMultiPoint()
    for (i <- 0 until n) {
      multipoint.points.add(list_latlon(i))
    }
    LPLogger.info("AnyplaceMapping::getAccesHeatmapByBuildingFloor(): fingerprints, APs: "
      + n.toString + ", " + m.toString)

    LPLogger.info("AnyplaceMapping::getAccesHeatmapByBuildingFloor(): multipoint: " + multipoint.toGeoJSON().toString)

    val floors: Array[JsonObject] = ProxyDataSource.getIDatasource.floorsByBuildingAsJson(buid).iterator().toArray
    val floor = floors.filter((js: JsonObject) => js.getString("floor_number") == floor_number)(0)
    val bl = new GeoPoint(lat = floor.getString("bottom_left_lat"), lon = floor.getString("bottom_left_lng"))
    val ur = new GeoPoint(lat = floor.getString("top_right_lat"), lon = floor.getString("top_right_lng"))
    val X = GeoUtils.latlng2xy(multipoint, bl = bl, ur = ur)
    val Y = DenseMatrix.zeros[Double](n, m)
    for (i <- 0 until n) {
      Y(i, ::) := list_rss.get(i).t
    }

    val X_min = GeoUtils.latlng2xy(point = bl, bl = bl, ur = ur)
    val X_max = GeoUtils.latlng2xy(point = ur, bl = bl, ur = ur)
    val Y_min = -110.0 * DenseVector.ones[Double](m)
    val Y_max = 0.0 * DenseVector.ones[Double](m)
    val acces = new AccesRBF(
      X = X, Y = Y,
      X_min = Option(X_min), X_max = Option(X_max),
      Y_min = Option(Y_min), Y_max = Option(Y_max),
      normalize_x = false,
      normalize_y = true,
      drop_redundant_features = true,
      cut_k_features = cut_k_features
    )
    println("fit_gpr: starting")
    acces.fit_gpr(estimate = true, use_default_params = false)
    println("fit_gpr: finished")
    //X_min and X_max are bl and ur in XY coordinates
    val X_predict = GeoUtils.grid_2D(bl = X_min, ur = X_max, h = h)
    val crlbs = acces.get_CRLB(X = X_predict, pinv_cond = 1e-6)
    println("crlbs", crlbs)
    val latlon_predict = GeoUtils.dm2GeoJSONMultiPoint(

      GeoUtils.xy2latlng(xy = X_predict, bl = bl, ur = ur)
    )

    return (latlon_predict, crlbs)
  }


  private def getRadioMapMeanByBuildingFloor(buid: String, floor_number: String): Option[RadioMapMean] = {
    val rmapDir = new File("radiomaps_frozen" + File.separatorChar + buid + File.separatorChar + floor_number)
    val meanFile = new File(rmapDir.toString + File.separatorChar + "indoor-radiomap-mean.txt")
    if (rmapDir.exists() && meanFile.exists()) {
      val folder = rmapDir.toString
      val radiomap_mean_filename = new File(folder + File.separatorChar + "indoor-radiomap-mean.txt").getAbsolutePath
      val rm_mean = new RadioMapMean(isIndoor = true, defaultNaNValue = -110)
      rm_mean.ConstructRadioMap(inFile = new File(radiomap_mean_filename))
      return Option[RadioMapMean](rm_mean)
    }

    if (!rmapDir.mkdirs() && !rmapDir.exists()) {
      throw new IOException("Could not create %s".format(rmapDir.toString))
    }
    val radio = new File(rmapDir.getAbsolutePath + File.separatorChar + "rss-log")
    var fout: FileOutputStream = null
    fout = new FileOutputStream(radio)
    println(radio.toPath().getFileName)
    var floorFetched: Long = 0l
    floorFetched = ProxyDataSource.getIDatasource.dumpRssLogEntriesByBuildingFloor(fout, buid, floor_number)
    try {
      fout.close()
    } catch {
      case e: IOException => LPLogger.error("Error while closing the file output stream for the dumped rss logs")
    }
    if (floorFetched == 0) {
      Option[RadioMapMean](null)
    }

    val folder = rmapDir.toString
    val radiomap_filename = new File(folder + File.separatorChar + "indoor-radiomap.txt").getAbsolutePath
    var radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt")
    var radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt")
    var radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt")
    val rm = new RadioMap(new File(folder), radiomap_filename, "", -110)
    if (!rm.createRadioMap()) {
      LPLogger.error("Error while creating Radio Map on-the-fly!")
      throw new Exception("Error while creating Radio Map on-the-fly!")
    }
    val rm_mean = new RadioMapMean(isIndoor = true, defaultNaNValue = -110)
    rm_mean.ConstructRadioMap(inFile = new File(radiomap_mean_filename))
    return Option[RadioMapMean](rm_mean)
  }




  //  private def getRadioMapMeanByBuildingFloor(buid: String, floor_number: String) : Option[RadioMapMean] = {
  //    val rmapDir = new File("radiomaps_frozen" + File.separatorChar + buid + File.separatorChar + floor_number)
  //    val meanFile = new File(rmapDir.toString + File.separatorChar + "indoor-radiomap-mean.txt")
  //    if (rmapDir.exists() && meanFile.exists()) {
  //      LPLogger.info("AnyplaceMapping::getRadioMapMeanByBuildingFloor(): necessary files exist:"
  //        + rmapDir.toString + ", " + meanFile.toString)
  //      val folder = rmapDir.toString
  //      var radiomap_mean_filename = new File(folder + File.separatorChar + "indoor-radiomap-mean.txt").getAbsolutePath
  //      val api = AnyplaceServerAPI.SERVER_API_ROOT
  //      var pos = radiomap_mean_filename.indexOf("radiomaps_frozen")
  //      radiomap_mean_filename = api + radiomap_mean_filename.substring(pos)
  //      val rm = new RadioMapMean(isIndoor = true, defaultNaNValue = -110)
  //      rm.ConstructRadioMap(inFile = new File(radiomap_mean_filename))
  //      return Option[RadioMapMean](rm)
  //    }
  //    LPLogger.info("AnyplaceMapping::getRadioMapMeanByBuildingFloor(): necessary files do not exist:"
  //      + rmapDir.toString + ", " + meanFile.toString)
  //    if (!rmapDir.mkdirs() && !rmapDir.exists()) {
  //      throw new IOException("Could not create %s".format(rmapDir.toString))
  //    }
  //    val radio = new File(rmapDir.getAbsolutePath + File.separatorChar + "rss-log")
  //    var fout: FileOutputStream = null
  //    fout = new FileOutputStream(radio)
  //    println(radio.toPath().getFileName)
  //
  //    var floorFetched: Long = 0l
  //    floorFetched = ProxyDataSource.getIDatasource.dumpRssLogEntriesByBuildingFloor(fout, buid, floor_number)
  //    try {
  //      fout.close()
  //    } catch {
  //      case e: IOException => LPLogger.error("Error while closing the file output stream for the dumped rss logs")
  //    }
  //
  //    if (floorFetched == 0) {
  //      return Option[RadioMapMean](null)
  //    }
  //
  //    val folder = rmapDir.toString
  //    val radiomap_filename = new File(folder + File.separatorChar + "indoor-radiomap.txt").getAbsolutePath
  //    var radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt")
  //    val api = AnyplaceServerAPI.SERVER_API_ROOT
  //    var pos = radiomap_mean_filename.indexOf("radiomaps_frozen")
  //    radiomap_mean_filename = api + radiomap_mean_filename.substring(pos)
  //    val rm = new RadioMapMean(isIndoor = true, defaultNaNValue = -110)
  //    rm.ConstructRadioMap(inFile = new File(radiomap_mean_filename))
  //    return Option[RadioMapMean](rm)
  //  }
}



