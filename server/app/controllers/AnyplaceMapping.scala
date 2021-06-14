/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Nikolas Neofytou, Constantinos Costa, Kyriakos Georgiou, Lambros Petrou, Loukas Solea, Paschalis Mpeis
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
package controllers

import java.io._
import java.net.{HttpURLConnection, URL}
import java.nio.file.{Files, Paths}
import java.text.{NumberFormat, ParseException}
import java.time.Instant
import java.time.temporal.{ChronoUnit, TemporalUnit}
import java.util
import java.util.Locale
import java.util.zip.GZIPOutputStream

import acces.{AccesRBF, GeoUtils}
import breeze.linalg.{DenseMatrix, DenseVector}
import com.couchbase.client.java.document.json.{JsonArray, JsonObject}
import datasources.MongodbDatasource.convertJson
import datasources.{DatasourceException, MongodbDatasource, ProxyDataSource, SCHEMA}
import db_models.ExternalType.ExternalType
import db_models._
import json.VALIDATE.{Coordinate, Int, String, StringNumber}
import location.Algorithms
import oauth.provider.v2.models.OAuth2Request
import org.apache.commons.codec.binary.Base64
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.model.Filters.equal
import play.Play
import play.api.libs.json.Reads._
import play.api.libs.json.{JsObject, JsValue, Json, _}
import play.api.mvc._
import play.libs.F
import radiomapserver.RadioMap.RadioMap
import radiomapserver.RadioMapMean
import utils.JsonUtils.{isNullOrEmpty, toCouchArray}
import utils._

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import scala.io.Source
import scala.util.control.Breaks

object AnyplaceMapping extends play.api.mvc.Controller {

  // CHECK Why is this hardcoded here?
  private val ADMIN_ID = "112997031510415584062_google"
  val ACCES_RETRY_AMOUNT = 2
  val ACCES_RETRY_UNIT: TemporalUnit = ChronoUnit.HOURS
  val NEARBY_BUILDINGS_RANGE = 50
  val NEARBY_BUILDINGS_RANGE_MAX = 500

  // returns a json in a string format, and strips out unnecessary fields for logging, like:
  // access_token (which is huge), username, and password
  def stripJson(jsVal: JsValue) = {
    // if username is needed, then restore it
    (jsVal.as[JsObject] - SCHEMA.fAccessToken - "password" - "username").toString()
  }

  // TODO:PM TODO:NN local accounts
  @deprecated("Will use oAuth2.verifyUsers")
  // query (find) the user by api_key
  def verifyId(authToken: String): String = {
    // remove the double string quotes due to json processing
    val gURL = "https://www.googleapis.com/oauth2/v3/tokeninfo?id_token=" + authToken
    var res = ""
    try {
      res = sendGet(gURL)
    } catch {
      case e: Exception => {
        LPLogger.error("verifyId: " + e.toString)
        null
      }
    }
    if (res != null)
      try {

        // CHECK:PM CHECK:NN bug on main branch (JsonObject.fromJson())
        val json = Json.parse(res)
        val uid = (json \ "user_id")
        val sub = (json \ "sub")
        if (uid.toOption.isDefined)
          return uid.as[String]
        if (sub.toOption.isDefined)
          return sub.as[String]
      } catch {
        case ioe: IOException => null
        case iae: IllegalArgumentException => LPLogger.error("verifyId: " + iae.getMessage + "String: '" + res + "'");
      }
    null
  }

  // TODO:NN see usages and make it specific only for google.
  def appendGoogleIdIfNeeded(id: String) = {
    if (id.contains("_local"))
      id
    else if (id.contains("_google"))
      id
    else
      id + "_google"
  }

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
        LPLogger.info("AnyplaceMapping::getRadioHeatmap(): " + stripJson(anyReq.getJsonBody))
        try {
          val radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmap
          if (radioPoints == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          val res = JsonObject.empty()
          res.put("radioPoints", (radioPoints))
          return AnyResponseHelper.ok(res, "Successfully retrieved all radio points.")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  def getRadioHeatmapByBuildingFloor() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {

        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody()
        LPLogger.info("getRadioHeatmapByBuildingFloor: " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fFloor)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        if (StringNumber(json, SCHEMA.fFloor) == null)
          return AnyResponseHelper.bad_request("floor field must be String, containing a number!")
        val floor = (json \ SCHEMA.fFloor).as[String]
        try {
          val radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmapByBuildingFloor(buid, floor)
          if (radioPoints == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          val res: JsValue = Json.obj("radioPoints" -> radioPoints)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }


  def getHeatmapByFloorAVG1() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        // ---
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        LPLogger.info("getHeatmapByFloorAVG1: " + stripJson(json))
        // TODO:NN use new method for json check
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fFloor)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        if (StringNumber(json, SCHEMA.fFloor) == null)
          return AnyResponseHelper.bad_request("floor field must be String, containing a number!")
        val floor = (json \ SCHEMA.fFloor).as[String]
        // ---
        try {
          val radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmapByBuildingFloorAverage1(buid, floor)
          if (radioPoints == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          val res: JsValue = Json.obj("radioPoints" -> radioPoints)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }


  def getRadioHeatmapByBuildingFloorAverage2() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        LPLogger.D2("getRadioHeatmapRSS2(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fFloor)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        if (StringNumber(json, SCHEMA.fFloor) == null)
          return AnyResponseHelper.bad_request("floor field must be String, containing a number!")
        val floor = (json \ SCHEMA.fFloor).as[String]
        try {
          val radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmapByBuildingFloorAverage2(buid, floor)
          if (radioPoints == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          val res: JsValue = Json.obj("radioPoints" -> radioPoints)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
          }
        } catch {
          case e: Exception => return AnyResponseHelper.internal_server_error("getRadioHeatmapByBuildingFloorAverage2: ", e)
        }
      }

      inner(request)
  }

  /**
   * Gets RSS average of fingerprints on the same:
   *  - buid, floor, location, heading (? VEFIRY)
   *
   * @return a json list of count, total, average
   */
  def getRadioHeatmapByBuildingFloorAverage3() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        LPLogger.D2("getRadioHeatmapRSS3(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fFloor)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        if (StringNumber(json, SCHEMA.fFloor) == null)
          return AnyResponseHelper.bad_request("floor field must be String, containing a number!")
        val floor = (json \ SCHEMA.fFloor).as[String]
        try {
          val radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmapByBuildingFloorAverage3(buid, floor)
          if (radioPoints == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          val res: JsValue = Json.obj("radioPoints" -> radioPoints)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
          }
        } catch {
          case e: Exception => return AnyResponseHelper.internal_server_error("getRadioHeatmapByBuildingFloorAverage3: ", e)
        }
      }

      inner(request)
  }

  /**
   * Reads from level3 (all fingerprints) and returns
   *
   * @return
   */
  def getRadioHeatmapByBuildingFloorAverage3Tiles() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::getRadioHeatmapRSS3(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fFloor, SCHEMA.fX, SCHEMA.fY, "z")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        if (StringNumber(json, SCHEMA.fFloor) == null)
          return AnyResponseHelper.bad_request("floor field must be String, containing a number!")
        val floor = (json \ SCHEMA.fFloor).as[String]
        if (Int(json, SCHEMA.fX) == null)
          return AnyResponseHelper.bad_request("x field must be Integer!")
        val tileX = (json \ SCHEMA.fX).as[Int]
        if (Int(json, SCHEMA.fY) == null)
          return AnyResponseHelper.bad_request("y field must be Integer!")
        val tileY = (json \ SCHEMA.fY).as[Int]
        if (Int(json, "z") == null)
          return AnyResponseHelper.bad_request("z field must be Integer!")
        val zoomLevel = (json \ "z").as[Int]
        try {
          val radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmapByBuildingFloorAverage3(buid, floor)
          if (radioPoints == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")

          val radioPointsInXY: util.ArrayList[JsValue] = new util.ArrayList[JsValue]()
          for (radioPoint <- radioPoints) {
            //val lat = (radioPoint \ SCHEMA.fLocation).get(0).as[Double]
            //val lon = (radioPoint \ SCHEMA.fLocation).get(1).as[Double]
            val lat = (radioPoint \ "x").as[Double]
            val lon = (radioPoint \ "y").as[Double]
            val xyConverter = convertToXY(lat, lon, zoomLevel)
            if (xyConverter(0) == tileX && xyConverter(1) == tileY) {
              radioPointsInXY.add(radioPoint)
              LPLogger.debug("ADD: x = " + xyConverter(0) + " y = " + xyConverter(1) + "lat = " + lat + " lon = " + lon)
            } else {
              LPLogger.debug("     x = " + xyConverter(0) + " y = " + xyConverter(1) + "lat = " + lat + " lon = " + lon)
            }

          }
          val res = Json.obj("radioPoints" -> radioPointsInXY.toList)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }


  def getRadioHeatmapByBuildingFloorTimestamp() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        LPLogger.info("getRadioHeatmapByBuildingFloorTimestamp: " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fFloor, SCHEMA.fTimestampX, SCHEMA.fTimestampY)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        if (StringNumber(json, SCHEMA.fFloor) == null)
          return AnyResponseHelper.bad_request("floor field must be String, containing a number!")
        val floor = (json \ SCHEMA.fFloor).as[String]
        if (StringNumber(json, SCHEMA.fTimestampX) == null)
          return AnyResponseHelper.bad_request("timestampX field must be String, containing a number!")
        val timestampX = (json \ SCHEMA.fTimestampX).as[String]
        if (StringNumber(json, SCHEMA.fTimestampY) == null)
          return AnyResponseHelper.bad_request("timestampY field must be String, containing a number!")
        val timestampY = (json \ SCHEMA.fTimestampY).as[String]

        try {
          val radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmapByBuildingFloorTimestamp(buid, floor, timestampX, timestampY)
          if (radioPoints == null) return AnyResponseHelper.bad_request("Fingerprints does not exist or could not be retrieved!")
          val res: JsValue = Json.obj("radioPoints" -> radioPoints)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
          }

        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getClass + ": " + e.getMessage + "]")
        }
      }

      inner(request)
  }

  /**
   * Converts tiles map x, y, z to coordinates(lat, lon)
   *
   * @param lat
   * @param lon
   * @param zoom
   * @return
   */
  private def convertToXY(lat: Double, lon: Double, zoom: Int) = {
    val sxtile = Math.floor((lon + 180.0) / 360.0 * (1 << zoom).toDouble).toInt
    val sytile = Math.floor((1.0 - Math.log(Math.tan(Math.toRadians(lat)) + 1.0 /
      Math.cos(Math.toRadians(lat))) / Math.PI) / 2.0 * (1 << zoom).toDouble).toInt
    Array[Int](sxtile, sytile)
  }

  def getRadioHeatmapByBuildingFloorTimestampTiles() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        LPLogger.info("getRadioHeatmapByBuildingFloorTimestampTiles: " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fFloor, SCHEMA.fTimestampX, SCHEMA.fTimestampY, SCHEMA.fX, SCHEMA.fY, "z")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        if (StringNumber(json, SCHEMA.fFloor) == null)
          return AnyResponseHelper.bad_request("floor field must be String, containing a number!")
        val floor = (json \ SCHEMA.fFloor).as[String]
        if (StringNumber(json, SCHEMA.fTimestampX) == null)
          return AnyResponseHelper.bad_request("timestampX field must be String, containing a number!")
        val timestampX = (json \ SCHEMA.fTimestampX).as[String]
        if (StringNumber(json, SCHEMA.fTimestampY) == null)
          return AnyResponseHelper.bad_request("timestampY field must be String, containing a number!")
        val timestampY = (json \ SCHEMA.fTimestampY).as[String]
        if (Int(json, SCHEMA.fX) == null)
          return AnyResponseHelper.bad_request("x field must be Integer!")
        val x = (json \ SCHEMA.fX).as[Int]
        if (Int(json, SCHEMA.fY) == null)
          return AnyResponseHelper.bad_request("y field must be Integer!")
        val y = (json \ SCHEMA.fY).as[Int]
        if (Int(json, "z") == null)
          return AnyResponseHelper.bad_request("z field must be Integer!")
        val z = (json \ "z").as[Int]

        try {

          val radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmapByBuildingFloorTimestamp(buid, floor, timestampX, timestampY)
          if (radioPoints == null) return AnyResponseHelper.bad_request("Fingerprints does not exist or could not be retrieved!")
          val radioPointsInXY: util.ArrayList[JsValue] = new util.ArrayList[JsValue]()

          for (radioPoint <- radioPoints) {
            var radioX = (radioPoint \ SCHEMA.fLocation).get(0).as[Double]
            var radioY = (radioPoint \ SCHEMA.fLocation).get(1).as[Double]
            var xyConverter = convertToXY(radioX, radioY, z)
            if (xyConverter(0) == x && xyConverter(1) == y)
              radioPointsInXY.add(radioPoint)
          }
          val res: JsValue = Json.obj("radioPoints" -> radioPointsInXY.toList)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
          }

        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  def getRadioHeatmapByBuildingFloorTimestampAverage1() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::getRadioHeatmapRSSByTime(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fFloor, SCHEMA.fTimestampX, SCHEMA.fTimestampY)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        if (StringNumber(json, SCHEMA.fFloor) == null)
          return AnyResponseHelper.bad_request("floor field must be String, containing a number!")
        val floor = (json \ SCHEMA.fFloor).as[String]
        if (StringNumber(json, SCHEMA.fTimestampX) == null)
          return AnyResponseHelper.bad_request("timestampX field must be String, containing a number!")
        val timestampX = (json \ SCHEMA.fTimestampX).as[String]
        if (StringNumber(json, SCHEMA.fTimestampY) == null)
          return AnyResponseHelper.bad_request("timestampY field must be String, containing a number!")
        val timestampY = (json \ SCHEMA.fTimestampY).as[String]

        try {

          val radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmapByBuildingFloorTimestampAverage1(buid, floor, timestampX, timestampY)
          if (radioPoints == null) return AnyResponseHelper.bad_request("Fingerprints does not exist or could not be retrieved!")
          val res: JsValue = Json.obj("radioPoints" -> radioPoints)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
          }

        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  def getRadioHeatmapByBuildingFloorTimestampAverage2() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::getRadioHeatmapRSSByTime(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fFloor, SCHEMA.fTimestampX, SCHEMA.fTimestampY)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        if (StringNumber(json, SCHEMA.fFloor) == null)
          return AnyResponseHelper.bad_request("floor field must be String, containing a number!")
        val floor = (json \ SCHEMA.fFloor).as[String]
        if (StringNumber(json, SCHEMA.fTimestampX) == null)
          return AnyResponseHelper.bad_request("timestampX field must be String, containing a number!")
        val timestampX = (json \ SCHEMA.fTimestampX).as[String]
        if (StringNumber(json, SCHEMA.fTimestampY) == null)
          return AnyResponseHelper.bad_request("timestampY field must be String, containing a number!")
        val timestampY = (json \ SCHEMA.fTimestampY).as[String]
        try {
          val radioPoints = ProxyDataSource.getIDatasource.getRadioHeatmapByBuildingFloorTimestampAverage2(buid, floor, timestampX, timestampY)
          if (radioPoints == null) return AnyResponseHelper.bad_request("Fingerprints does not exist or could not be retrieved!")
          val res: JsValue = Json.obj("radioPoints" -> radioPoints)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
          }

        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  def deletePrecomputed(): Unit = {
    // TODO: delete accessPointsWifi: buid, floor
  }

  def getAPsByBuildingFloor() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::getAPs(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fFloor)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        if (StringNumber(json, SCHEMA.fFloor) == null)
          return AnyResponseHelper.bad_request("floor field must be String, containing a number!")
        val floor = (json \ SCHEMA.fFloor).as[String]
        val APs = ProxyDataSource.getIDatasource().getCachedAPsByBuildingFloor(buid, floor)
        // if cached return it
        if (APs != null) {
          val res = Json.obj("accessPoints" -> (APs \ "accessPoints").as[List[JsValue]])
          return gzippedJSONOk(res, "Fetched precompute of accessPointsWifi")
        } else {
          try {
            var accessPoints = ProxyDataSource.getIDatasource.getAPsByBuildingFloor(buid, floor)
            val apcdb = ProxyDataSource.getIDatasource.getAPsByBuildingFloorcdb(buid, floor)

            LPLogger.debug("mdb " + accessPoints.size)
            LPLogger.debug("cdb " + apcdb.size())
            //val newList = new util.ArrayList[JsValue]()
            //for (ap <- apcdb) {
            //  val newAP = fromCouchObject(ap)
            //  newList.add(newAP)
            //}
            //accessPoints = newList.toList
            val uniqueAPs: util.HashMap[String, JsValue] = new util.HashMap()
            for (accessPoint <- accessPoints) {
              var tempAP = accessPoint
              var id = (tempAP \ "AP").as[String]
              id = id.substring(0, id.length - 9)
              var ap = uniqueAPs.get(id)
              val avg = (tempAP \ "RSS" \ "average").as[Double]
              val x = (tempAP \ SCHEMA.fX).as[String].toDouble
              val y = (tempAP \ SCHEMA.fY).as[String].toDouble
              if (ap == null) {
                if (avg < -60) {
                  tempAP = tempAP.as[JsObject] + ("den" -> JsNumber(avg)) + (SCHEMA.fX -> JsNumber(avg * x)) + (SCHEMA.fY -> JsNumber(avg * y))
                } else {
                  tempAP = tempAP.as[JsObject] + ("den" -> JsNumber(0)) + (SCHEMA.fX -> JsNumber(x)) + (SCHEMA.fY -> JsNumber(y))
                }
                ap = tempAP
              } else if ((ap \ "den").as[Double] < 0) {
                if (avg < -60) {
                  val ap_den = (ap \ "den").as[Double]
                  val ap_x = (ap \ SCHEMA.fX).as[Double]
                  val ap_y = (ap \ SCHEMA.fY).as[Double]
                  tempAP = tempAP.as[JsObject] + ("den" -> JsNumber(avg + ap_den)) +
                    (SCHEMA.fX -> JsNumber(avg * x + ap_x)) + (SCHEMA.fY -> JsNumber(avg * y + ap_y))
                } else {
                  tempAP = tempAP.as[JsObject] + ("den" -> JsNumber(0)) + (SCHEMA.fX -> JsNumber(x)) + (SCHEMA.fY -> JsNumber(y))
                }
                ap = tempAP
              }
              //overwrite old object in case that there is one
              uniqueAPs.put(id, ap.as[JsObject])
            }

            if (accessPoints == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
            val newAccessPoint = Json.obj(SCHEMA.fBuid -> buid, SCHEMA.fFloor -> floor, "accessPoints" -> uniqueAPs.values().toList)
            ProxyDataSource.getIDatasource().addJsonDocument("accessPointsWifi", newAccessPoint.toString())
            val res: JsValue = Json.obj("accessPoints" -> new util.ArrayList[JsValue](uniqueAPs.values()).toList)
            try {
              gzippedJSONOk(res, "Generated precompute of accessPointsWifi")
            } catch {
              case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!")
            }
          } catch {
            case e: Exception => return AnyResponseHelper.internal_server_error("getAPsByBuildingFloor: ", e)
          }
        }
      }

      inner(request)
  }

  /**
   *
   * @return
   */
  def getAPsIds() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {

        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        val accessPointsOfReq = (json \ "ids").as[List[String]]
        try {
          val reqFile = "public/anyplace_architect/ids.json"
          val file = Play.application().resourceAsStream(reqFile)

          var accessPointsOfFile: List[JsObject] = null
          if (file != null) {
            accessPointsOfFile = Json.parse(file).as[List[JsObject]]
          } else {
            return AnyResponseHelper.not_found(reqFile)
          }

          val APsIDs: util.ArrayList[String] = new util.ArrayList[String]()
          var found = false
          var firstBitFound = false
          var sameBits = 0
          var sameBitsOfReq = 0
          var idOfReq: String = ""
          val loop = new Breaks

          val inner_loop = new Breaks


          for (accessPointOfReq: String <- accessPointsOfReq) {
            idOfReq = "N/A"
            loop.breakable {
              for (accessPointOfFile: JsObject <- accessPointsOfFile) {

                val bitsR = accessPointOfReq.split(":")
                val bitsA = accessPointOfFile.value("mac").as[String].split(":")
                if (bitsA(0).equalsIgnoreCase(bitsR(0))) {

                  firstBitFound = true

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

                  if (sameBits >= 3)
                    found = true
                } else {
                  sameBits = 0
                  if (firstBitFound) {
                    firstBitFound = false
                    loop.break
                  }
                }

                if (sameBitsOfReq < sameBits && found) {
                  sameBitsOfReq = sameBits
                  idOfReq = accessPointOfFile.value("id").as[String]
                }
                sameBits = 0
              }
            } //accessPointOfFile break

            APsIDs.add(idOfReq)
            sameBitsOfReq = 0
            found = false
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
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  /**
   *
   * @return deleted fingerprints (so JS update UI)
   */
  def FingerPrintsDelete() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody)
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::FingerPrintsDelete(): " + stripJson(json))
        // add owner_id
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fFloor, "lat1", "lon1", "lat2", "lon2", SCHEMA.fOwnerId)
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)

        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        // TODO: get building if building not exist bad req
        // TODO: make a building model
        // TODO: if !model.hasAccess bad request (user has no access for the building)
        if (StringNumber(json, SCHEMA.fFloor) == null)
          return AnyResponseHelper.bad_request("floor field must be String, containing a number!")
        val floor_number = (json \ SCHEMA.fFloor).as[String]
        if (Coordinate(json, "lat1") == null)
          return AnyResponseHelper.bad_request("lat1 field must be String containing a float!")
        val lat1 = (json \ "lat1").as[String]
        if (Coordinate(json, "lon1") == null)
          return AnyResponseHelper.bad_request("lon1 field must be String containing a float!")
        val lon1 = (json \ "lon1").as[String]
        if (Coordinate(json, "lat2") == null)
          return AnyResponseHelper.bad_request("lat2 field must be String containing a float!")
        val lat2 = (json \ "lat2").as[String]
        if (Coordinate(json, "lon2") == null)
          return AnyResponseHelper.bad_request("lon2 field must be String containing a float!")
        val lon2 = (json \ "lon2").as[String]
        try {
          val fingerprints: List[JsValue] = ProxyDataSource.getIDatasource.getFingerPrintsBBox(
            buid, floor_number, lat1, lon1, lat2, lon2)
          if (fingerprints.isEmpty)
            return AnyResponseHelper.bad_request("Fingerprints does not exist or could not be retrieved!")

          LPLogger.D2("FingerPrintsDelete: will delete " + fingerprints.size + " fingerprints.")
          for (fingerprint <- fingerprints)
            ProxyDataSource.getIDatasource.deleteFromKey(SCHEMA.cFingerprintsWifi, SCHEMA.fBuid, (fingerprint \ SCHEMA.fBuid).as[String])
          val res: JsValue = Json.obj("fingerprints" -> fingerprints)
          //try {
          //Regenerate the radiomap files
          // TODO: findout if asychronous (using print)
          val strPromise = F.Promise.pure("10")
          val intPromise = strPromise.map(new F.Function[String, Integer]() {
            override def apply(arg0: String): java.lang.Integer = {
              AnyplacePosition.updateFrozenRadioMap(buid, floor_number)
              // TODO: update heatmaps for each level
              0
            }
          })

          return gzippedJSONOk(res, "Deleted " + fingerprints.size + " fingerprints and returning them.")

          //return AnyResponseHelper.ok(res, "Deleted " + fingerprints.size + " fingerprints and returning them.")
          //} catch {
          // CHECK:PM CHECK:NN XXX we might have to enable this workaround?
          //  case ioe: IOException =>
          //    return AnyResponseHelper.ok(res, "Successfully retrieved all FingerPrints!")
          //}
        } catch {
          case e: Exception =>
            return AnyResponseHelper.internal_server_error("FingerPrintsDelete: " + e.getClass + ": " + e.getMessage)
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
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::FingerPrintsTimestampDelete(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fFloor, "lat1", "lon1", "lat2", "lon2", SCHEMA.fTimestampX, SCHEMA.fTimestampY)
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        if (StringNumber(json, SCHEMA.fFloor) == null)
          return AnyResponseHelper.bad_request("floor field must be String, containing a number!")
        val floor_number = (json \ SCHEMA.fFloor).as[String]
        if (Coordinate(json, "lat1") == null)
          return AnyResponseHelper.bad_request("lat1 field must be String containing a float!")
        val lat1 = (json \ "lat1").as[String]
        if (Coordinate(json, "lon1") == null)
          return AnyResponseHelper.bad_request("lon1 field must be String containing a float!")
        val lon1 = (json \ "lon1").as[String]
        if (Coordinate(json, "lat2") == null)
          return AnyResponseHelper.bad_request("lat2 field must be String containing a float!")
        val lat2 = (json \ "lat2").as[String]
        if (Coordinate(json, "lon2") == null)
          return AnyResponseHelper.bad_request("lon2 field must be String containing a float!")
        val lon2 = (json \ "lon2").as[String]
        if (StringNumber(json, SCHEMA.fTimestampX) == null)
          return AnyResponseHelper.bad_request("timestampX field must be String, containing a number!")
        val timestampX = (json \ SCHEMA.fTimestampX).as[String]
        if (StringNumber(json, SCHEMA.fTimestampY) == null)
          return AnyResponseHelper.bad_request("timestampY field must be String, containing a number!")
        val timestampY = (json \ SCHEMA.fTimestampY).as[String]
        try {
          val radioPoints: List[JsValue] = ProxyDataSource.getIDatasource.getFingerPrintsTimestampBBox(buid, floor_number, lat1, lon1, lat2, lon2, timestampX, timestampY)
          if (radioPoints.isEmpty)
            return AnyResponseHelper.bad_request("FingerPrints does not exist or could not be retrieved!")
          for (radioPoint <- radioPoints)
            ProxyDataSource.getIDatasource.deleteFromKey(SCHEMA.cFingerprintsWifi, SCHEMA.fBuid, (radioPoint \ SCHEMA.fBuid).as[String])
          val res: JsValue = Json.obj("radioPoints" -> radioPoints)
          try {
            //Regenerate the radiomap files
            val strPromise = F.Promise.pure("10")
            val intPromise = strPromise.map(new F.Function[String, Integer]() {
              override def apply(arg0: String): java.lang.Integer = {
                AnyplacePosition.updateFrozenRadioMap(buid, floor_number)
                0
              }
            })
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException =>
              return AnyResponseHelper.ok(res, "Successfully retrieved all FingerPrints!")
          }
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }

      }

      inner(request)
  }

  /**
   * TODO:NN:SUMMER cache object (collection: cFingerprintTime)
   *   - delete on: buid/floor deletion, fingerprints upload
   *   - on request:
   *      + query.mdb if exists return else create
   *
   * @return a list of timestamps and number of fingerprints
   */
  def FingerPrintsTime() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody)
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::FingerPrintsTime(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fFloor)
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        if (StringNumber(json, SCHEMA.fFloor) == null)
          return AnyResponseHelper.bad_request("floor field must be String, containing a number!")
        val floor_number = (json \ SCHEMA.fFloor).as[String]
        try {
          val radioPoints: List[JsValue] = ProxyDataSource.getIDatasource.getFingerPrintsTime(buid, floor_number)
          if (radioPoints.isEmpty)
            return AnyResponseHelper.bad_request("FingerPrints does not exist or could not be retrieved!")
          val res: JsValue = Json.obj("radioPoints" -> radioPoints)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException =>
              return AnyResponseHelper.ok(res, "Successfully retrieved all FingerPrints!")
          }
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
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
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::findPosition(): " + stripJson(json))
        //val requiredMissing = JsonUtils.requirePropertiesInJson(json, SCHEMA.fBuid, SCHEMA.fFloor,"APs","algorithm_choice")
        // LPLogger.debug("json: "+json)
        //if (!requiredMissing.isEmpty)
        //  return AnyResponseHelper.requiredFieldsMissing(requiredMissing)

        val buid = (json \ SCHEMA.fBuid).as[String]
        val floor_number = (json \ SCHEMA.fFloor).as[String]

        /*
         * BuxFix : Server side localization API
         * Fixing JSON Parse error
         */
        val accessOpt = Json.parse((json \ "APs").as[String]).validate[List[JsValue]] match {
          case s: JsSuccess[List[JsValue]] => {
            Some(s.get)
          }
          case e: JsError =>
            LPLogger.error("accessOpt Errors: " + JsError.toJson(e).toString())
            None
        }
        val accessPoints = accessOpt.get

        /*
         * BuxFix : Server side localization API
         * Fixing JSON Parse error [String vs Int]
         */
        val algorithm_choice: Int = (json \ "algorithm_choice").validate[String] match {
          case s: JsSuccess[String] => {
            if (s.get != null && s.get.trim != "")
              Integer.parseInt(s.get)
            else
              Play.application().configuration().getInt("defaultPositionAlgorithm")
          }
          case e: JsError =>
            Play.application().configuration().getInt("defaultPositionAlgorithm")
        }

        //FeatureAdd : Configuring location for server generated files
        val radioMapsFrozenDir = Play.application().configuration().getString("radioMapFrozenDir")
        /*
         * REVIEWLS . Leaving bugfix from develop
            val floor_number = (json \ SCHEMA.fFloor).as[String]
            val jsonstr=(json\"APs").as[String]
            val accessPoints= Json.parse(jsonstr).as[List[JsValue]]
            val floors: Array[JsonObject] = ProxyDataSource.getIDatasource.floorsByBuildingAsJson(buid).iterator().toArray
            val algorithm_choice = (json\"algorithm_choice").as[String].toInt
            */

        val rmapFile = new File(radioMapsFrozenDir + AnyplaceServerAPI.URL_SEP + buid + AnyplaceServerAPI.URL_SEP +
          floor_number + AnyplaceServerAPI.URL_SEP + "indoor-radiomap-mean.txt")

        if (!rmapFile.exists()) {
          //Regenerate the radiomap files if not exist
          AnyplacePosition.updateFrozenRadioMap(buid, floor_number)
        }
        /*
         * BuxFix : Server side localization API
         * Fixing null pointer error for latestScanList
         */
        val latestScanList: util.ArrayList[location.LogRecord] = new util.ArrayList[location.LogRecord]()

        /*
         * REVIEWLS Leaving bugfix from develop
           val latestScanList = new  util.ArrayList[location.LogRecord]
        */
        var i = 0


        for (i <- 0 until accessPoints.size) {
          val bssid = (accessPoints(i) \ "bssid").as[String]
          val rss = (accessPoints(i) \ SCHEMA.fRSS).as[Int]
          latestScanList.add(new location.LogRecord(bssid, rss))
        }

        val radioMap: location.RadioMap = new location.RadioMap(rmapFile)
        var response = Algorithms.ProcessingAlgorithms(latestScanList, radioMap, algorithm_choice)

        if (response == null) {
          response = "0 0"
        }
        val lat_long = response.split(" ")

        val res = Json.obj("lat" -> lat_long(0), "long" -> lat_long(1))
        return AnyResponseHelper.ok(res, "Successfully found position.")
      }

      inner(request)
  }

  def getRadioHeatmapBbox = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody)
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplacePosition::radioDownloadFloor(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fCoordinatesLat, SCHEMA.fCoordinatesLon, SCHEMA.fFloor, SCHEMA.fBuid, "range")
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val lat = (json \ SCHEMA.fCoordinatesLat).as[String]
        val lon = (json \ SCHEMA.fCoordinatesLon).as[String]
        val floor_number = (json \ SCHEMA.fFloor).as[String]
        val buid = (json \ SCHEMA.fBuid).as[String]
        val strRange = (json \ "range").as[String]
        val weight = (json \ SCHEMA.fWeight).as[String]
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
            return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  def deleteRadiosInBox() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::deleteRadiosInBox(): " + stripJson(json))
        try {
          if (!ProxyDataSource.getIDatasource.deleteRadiosInBox()) return AnyResponseHelper.bad_request("Building already exists or could not be added!")
          return AnyResponseHelper.ok("Success")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  def spaceAdd() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::spaceAdd(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fIsPublished, SCHEMA.fName, SCHEMA.fDescription,
          SCHEMA.fURL, SCHEMA.fAddress, SCHEMA.fCoordinatesLat, SCHEMA.fCoordinatesLon, SCHEMA.fAccessToken)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if ((json \ SCHEMA.fAccessToken).getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyId((json \ SCHEMA.fAccessToken).as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendGoogleIdIfNeeded(owner_id)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        try {
          var space: Space = null
          try {
            json = json.as[JsObject] - SCHEMA.fAccessToken
            space = new Space(json)
          } catch {
            case e: NumberFormatException => return AnyResponseHelper.bad_request("Building coordinates are invalid!")
          }
          if (!ProxyDataSource.getIDatasource.addJsonDocument(SCHEMA.cSpaces, space.toGeoJSON())) return AnyResponseHelper.bad_request("Building already exists or could not be added!")
          val res: JsValue = Json.obj(SCHEMA.fBuid -> space.getId)
          return AnyResponseHelper.ok(res, "Successfully added space!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  def spaceUpdateCoOwners() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody()
        LPLogger.info("AnyplaceMapping::spaceUpdateCoOwners(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fAccessToken, SCHEMA.fCoOwners)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\\(SCHEMA.fAccessToken) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyId((json \ SCHEMA.fAccessToken).as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendGoogleIdIfNeeded(owner_id)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val stored_space: JsValue = ProxyDataSource.getIDatasource().getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_space == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_space, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
          val space = new Space(stored_space)
          if (!ProxyDataSource.getIDatasource.replaceJsonDocument(SCHEMA.cSpaces, SCHEMA.fBuid, space.getId, space.appendCoOwners(json)))
            return AnyResponseHelper.bad_request("Building could not be updated!")
          return AnyResponseHelper.ok("Successfully updated space!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  def spaceUpdateOwner() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::spaceUpdateOwner(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fAccessToken, "new_owner")
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\(SCHEMA.fAccessToken).getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyId((json \ SCHEMA.fAccessToken).as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendGoogleIdIfNeeded(owner_id)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        if (String(json, "new_owner") == null)
          return AnyResponseHelper.bad_request("new_owner field must be String!")
        var newOwner = (json \ "new_owner").as[String]
        newOwner = appendGoogleIdIfNeeded(newOwner)
        try {
          val stored_space = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_space == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_space, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
          val space = new Space(stored_space)
          if (!ProxyDataSource.getIDatasource.replaceJsonDocument(SCHEMA.cSpaces, SCHEMA.fBuid, space.getId, space.changeOwner(newOwner))) return AnyResponseHelper.bad_request("Building could not be updated!")
          return AnyResponseHelper.ok("Successfully updated space!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  def spaceUpdate() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::spaceUpdateX(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fAccessToken)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\(SCHEMA.fAccessToken).getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyId((json \ SCHEMA.fAccessToken).as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendGoogleIdIfNeeded(owner_id)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("Buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          var stored_space = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_space == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_space, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
          if (json.\(SCHEMA.fIsPublished).getOrElse(null) != null) {
            val is_published = (json \ SCHEMA.fIsPublished).as[String]
            if (is_published == "true" || is_published == "false")
              stored_space = stored_space.as[JsObject] + (SCHEMA.fIsPublished -> JsString((json \ SCHEMA.fIsPublished).as[String]))
          }
          if (json.\(SCHEMA.fName).getOrElse(null) != null)
            stored_space = stored_space.as[JsObject] + (SCHEMA.fName -> JsString((json \ SCHEMA.fName).as[String]))
          if (json.\(SCHEMA.fBuCode).getOrElse(null) != null)
            stored_space = stored_space.as[JsObject] + (SCHEMA.fBuCode -> JsString((json \ SCHEMA.fBuCode).as[String]))
          if (json.\(SCHEMA.fDescription).getOrElse(null) != null)
            stored_space = stored_space.as[JsObject] + (SCHEMA.fDescription -> JsString((json \ SCHEMA.fDescription).as[String]))
          if (json.\(SCHEMA.fURL).getOrElse(null) != null)
            stored_space = stored_space.as[JsObject] + (SCHEMA.fURL -> JsString((json \ SCHEMA.fURL).as[String]))
          if (json.\(SCHEMA.fAddress).getOrElse(null) != null)
            stored_space = stored_space.as[JsObject] + (SCHEMA.fAddress -> JsString((json \ SCHEMA.fAddress).as[String]))
          if (json.\(SCHEMA.fCoordinatesLat).getOrElse(null) != null)
            stored_space = stored_space.as[JsObject] + (SCHEMA.fCoordinatesLat -> JsString((json \ SCHEMA.fCoordinatesLat).as[String]))
          if (json.\(SCHEMA.fCoordinatesLon).getOrElse(null) != null)
            stored_space = stored_space.as[JsObject] + (SCHEMA.fCoordinatesLon -> JsString((json \ SCHEMA.fCoordinatesLon).as[String]))
          val space = new Space(stored_space)
          if (!ProxyDataSource.getIDatasource.replaceJsonDocument(SCHEMA.cSpaces, SCHEMA.fBuid, space.getId, space.toGeoJSON())) return AnyResponseHelper.bad_request("Building could not be updated!")
          return AnyResponseHelper.ok("Successfully updated space!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  def spaceDelete() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)

        // TODO:NN this could be ONE method. with arguments: properties:
        /*e.g. val reqResult=anyReq.checkAuthRequest(CHEMA.fBUid); // THIS will replace the lines below.. after these comments..
        if(reqRes not null) return it..;

        // these will be implemneted somewher else...
        OAht2Request::checkAuthRequest( varargs...): Response {
          return checkRequest(true, vargargs)
        }

        Oauth2Request::checkRequest(auth: boolean, varargs...): Response {

        if auth: varargs.append(fAccessToekn)
        JUtils.HasProperties(varargs)

        if(auth) {
        vreifyId(..)
        }

        if all ok: return null
        }
        */
        // TODO:NN if the request is wrong must reply json
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::spaceDelete(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fAccessToken)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\(SCHEMA.fAccessToken).getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyId((json \ SCHEMA.fAccessToken).as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized") // TODO:NN OAuthReponse::Unauthorized()
        owner_id = appendGoogleIdIfNeeded(owner_id)
        // up to here will be in the method in the comments....


        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("Buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val stored_space = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_space == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_space, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
        try {
          val deleted = ProxyDataSource.getIDatasource.deleteAllByBuilding(buid)
          if (deleted == false)
            return AnyResponseHelper.bad_request("Some items related to the deleted space could not be deleted.")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
        val filePath = AnyPlaceTilerHelper.getRootFloorPlansDirFor(buid)
        try {
          val buidfile = new File(filePath)
          if (buidfile.exists()) HelperMethods.recDeleteDirFile(buidfile)
        } catch {
          case e: IOException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage + "] while deleting floor plans." +
            "\nAll related information is deleted from the database!")
        }
        return AnyResponseHelper.ok("Successfully deleted everything related to space!")
      }

      inner(request)
  }

  def spaceAll = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::spaceAll(): " + stripJson(json))
        try {
          val spaces = ProxyDataSource.getIDatasource.getAllBuildings
          val res: JsValue = Json.obj(SCHEMA.cSpaces -> spaces)
          try {
            gzippedJSONOk(res.toString)
          }
          catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all spaces!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  def echo = Action { implicit request =>
    var response = Ok("Got request [" + request)
    response
  }

  def spaceGetOne() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::spaceGet(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          var space = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (space != null && (space \ SCHEMA.fBuid) != JsDefined(JsNull) &&
            (space \ SCHEMA.fCoordinatesLat) != JsDefined(JsNull) &&
            (space \ SCHEMA.fCoordinatesLon) != JsDefined(JsNull) &&
            (space \ SCHEMA.fOwnerId) != JsDefined(JsNull) &&
            (space \ SCHEMA.fName) != JsDefined(JsNull) &&
            (space \ SCHEMA.fDescription) != JsDefined(JsNull)) {
            space = space.as[JsObject] - SCHEMA.fOwnerId - SCHEMA.fCoOwners - SCHEMA.fId - SCHEMA.fSchema
            val res: JsValue = Json.obj("space" -> space)
            try {
              return gzippedJSONOk(res.toString)
            } catch {
              case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved the space!")
            }
          }
          return AnyResponseHelper.not_found("Building not found.")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  def spaceAllByOwner() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::spaceAll(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fAccessToken)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\(SCHEMA.fAccessToken).getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyId((json \ SCHEMA.fAccessToken).as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendGoogleIdIfNeeded(owner_id)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        if (owner_id == null || owner_id.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        try {
          LPLogger.debug("owner_id = " + owner_id)
          val spaces = ProxyDataSource.getIDatasource.getAllBuildingsByOwner(owner_id)
          val res: JsValue = Json.obj("buildings" -> spaces)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all spaces!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  def spaceByBucode() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {

        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::spaceAll(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuCode)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val bucode = (json \ SCHEMA.fBuCode).as[String]
        try {
          val spaces = ProxyDataSource.getIDatasource.getAllBuildingsByBucode(bucode)
          val res: JsValue = Json.obj(SCHEMA.cSpaces -> spaces)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all spaces!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  def spaceCoordinates() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("spaceCoordinates(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fAccessToken)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\(SCHEMA.fAccessToken).getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var range = NEARBY_BUILDINGS_RANGE
        if (JsonUtils.hasProperty(json, "range")) {
          if ((json \ "range").validate[Int].isError) {
            return AnyResponseHelper.bad_request("range must be a possitive integer")
          }
          range = (json \ "range").as[Int]
          if (range <= 0) {
            return AnyResponseHelper.bad_request("range must be a possitive integer")
          }
          if (range > NEARBY_BUILDINGS_RANGE_MAX) {
            range = NEARBY_BUILDINGS_RANGE_MAX
            LPLogger.info("spaceCoordinates: Maximum range exceeded. Using " + range)
          }
        }

        var owner_id = verifyId((json \ SCHEMA.fAccessToken).as[String])
        if (owner_id == null) owner_id = ""
        owner_id = appendGoogleIdIfNeeded(owner_id)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        requiredMissing.addAll(JsonUtils.hasProperties(json, SCHEMA.fCoordinatesLat, SCHEMA.fCoordinatesLon))
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        try {
          if (Coordinate(json, SCHEMA.fCoordinatesLat) == null)
            return AnyResponseHelper.bad_request("coordinates_lat field must be String containing a float!")
          val lat = java.lang.Double.parseDouble((json \ SCHEMA.fCoordinatesLat).as[String])

          if (Coordinate(json, SCHEMA.fCoordinatesLon) == null)
            return AnyResponseHelper.bad_request("coordinates_lon field must be String containing a float!")
          val lon = java.lang.Double.parseDouble((json \ SCHEMA.fCoordinatesLon).as[String])

          val spaces = ProxyDataSource.getIDatasource.getAllBuildingsNearMe(lat, lon, range, owner_id)
          val res: JsValue = Json.obj(SCHEMA.cSpaces -> spaces)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all spaces near your position!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
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
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::buildingSetAll(): " + stripJson(json))
        var cuid = request.getQueryString(SCHEMA.fCampusCuid).orNull
        if (String(json, SCHEMA.fCampusCuid) == null)
          return AnyResponseHelper.bad_request("cuid field must be String!")
        if (cuid == null) cuid = (json \ SCHEMA.fCampusCuid).as[String]
        try {
          val campus = ProxyDataSource.getIDatasource.getBuildingSet(cuid)
          if (campus.size == 0) {
            return AnyResponseHelper.not_found("Campus '" + cuid + "' not found!")
          } else if (campus.size > 1) {
            return AnyResponseHelper.not_found("Something went wrong. More than one matches for '" + cuid + "'!")
          }

          val buids = new util.ArrayList[String]
          for (c <- campus) {
            val cBuildings = (c \ SCHEMA.fBuids).as[List[String]]
            for (cb <- cBuildings) {
              buids.add(cb)
            }
          }
          val buildings = new util.ArrayList[JsValue]
          for (b <- buids) {
            val building = ProxyDataSource.getIDatasource().getFromKey(SCHEMA.cSpaces, SCHEMA.fBuid, b)
            if (building != null) // some buildings are deleted but still exist in buids[] of a campus
              buildings.add(building.as[JsObject] - SCHEMA.fId - SCHEMA.fSchema - SCHEMA.fCoOwners - SCHEMA.fGeometry - SCHEMA.fType - SCHEMA.fOwnerId)
          }
          val res = campus.get(0).as[JsObject] - SCHEMA.fBuids - SCHEMA.fOwnerId - SCHEMA.fId - SCHEMA.fSchema - SCHEMA.fCampusCuid - SCHEMA.fDescription +
            (SCHEMA.cSpaces -> Json.toJson(buildings.toList))
          try
            gzippedJSONOk(res.toString)
          catch {
            case ioe: IOException =>
              AnyResponseHelper.ok(res, "Successfully retrieved all buildings Sets!")
          }
        } catch {
          case e: DatasourceException =>
            AnyResponseHelper.internal_server_error("500: " + e.getMessage)
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
        LPLogger.info("AnyplaceMapping::buildingSetAdd(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fDescription, SCHEMA.fName, SCHEMA.fBuids, SCHEMA.fGreeklish)
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        // get access token from url and check it against google's service
        if (json.\\(SCHEMA.fAccessToken) == null)
          return AnyResponseHelper.forbidden("Unauthorized1")
        var owner_id = verifyId((json \ SCHEMA.fAccessToken).as[String])
        if (owner_id == null)
          return AnyResponseHelper.forbidden("Unauthorized2")
        owner_id = appendGoogleIdIfNeeded(owner_id)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id)) - SCHEMA.fAccessToken
        try {
          val cuid = (json \ SCHEMA.fCampusCuid).as[String]
          val campus = ProxyDataSource.getIDatasource.BuildingSetsCuids(cuid)
          if (campus) return AnyResponseHelper.bad_request("Building set already exists!")
          else {
            var buildingset: BuildingSet = null
            try {
              buildingset = new BuildingSet(json)
            } catch {
              case e: NumberFormatException =>
                return AnyResponseHelper.bad_request("Building coordinates are invalid!")
            }
            if (!ProxyDataSource.getIDatasource.addJsonDocument(SCHEMA.cCampuses, buildingset.addBuids()))
              return AnyResponseHelper.bad_request("Building set already exists or could not be added!")
            val res: JsValue = Json.obj(SCHEMA.fCampusCuid -> buildingset.getId)
            return AnyResponseHelper.ok(res, "Successfully added building Set!")
          }
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
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
        LPLogger.info("AnyplaceMapping::campusUpdate(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fCampusCuid, SCHEMA.fAccessToken)
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        // get access token from url and check it against google's service
        if (json.\\(SCHEMA.fAccessToken) == null)
          return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyId((json \ SCHEMA.fAccessToken).as[String])
        if (owner_id == null)
          return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendGoogleIdIfNeeded(owner_id)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        if (String(json, SCHEMA.fCampusCuid) == null)
          return AnyResponseHelper.bad_request("cuid field must be String!")
        val cuid = (json \ SCHEMA.fCampusCuid).as[String]
        try {
          var stored_campus = ProxyDataSource.getIDatasource().getFromKeyAsJson(SCHEMA.cCampuses, SCHEMA.fCampusCuid, cuid)
          if (stored_campus == null)
            return AnyResponseHelper.bad_request("Campus does not exist or could not be retrieved!")
          if (!isCampusOwner(stored_campus, owner_id))
            return AnyResponseHelper.unauthorized("Unauthorized")
          // check for values to update
          if ((json \ SCHEMA.fName).toOption.isDefined) {
            val temp = (json \ SCHEMA.fName).as[String]
            if (temp != "-" && temp != "") {
              stored_campus = stored_campus.as[JsObject] + (SCHEMA.fName -> JsString(temp))
            } else {
              stored_campus = stored_campus.as[JsObject] - SCHEMA.fName
            }
          }
          if ((json \ SCHEMA.fDescription).toOption.isDefined) {
            val temp = (json \ SCHEMA.fDescription).as[String]
            if (temp != "-" && temp != "") {
              stored_campus = stored_campus.as[JsObject] + (SCHEMA.fDescription, JsString(temp))
            } else
              stored_campus = stored_campus.as[JsObject] - SCHEMA.fDescription
          }
          if ((json \ "cuidnew").toOption.isDefined) {
            val temp = (json \ SCHEMA.fCampusCuid).as[String]
            if (temp != "-" && temp != "")
              stored_campus = stored_campus.as[JsObject] + (SCHEMA.fCampusCuid, JsString(temp))
          }
          if ((json \ SCHEMA.fGreeklish).toOption.isDefined) {
            val temp = (json \ SCHEMA.fGreeklish).as[Boolean]
            stored_campus = stored_campus.as[JsObject] + (SCHEMA.fGreeklish -> JsString(temp.toString))
          }
          if ((json \ SCHEMA.fBuids).toOption.isDefined) {
            var buids = (json \ SCHEMA.fBuids).as[String]
            buids = buids.replace("[", "").replace("]", "").replace("\"", "")
            val buidsList = buids.split(",")
            stored_campus = stored_campus.as[JsObject] + (SCHEMA.fBuids -> Json.toJson(buidsList.toList))
          }
          val campus = new BuildingSet(stored_campus)
          if (!ProxyDataSource.getIDatasource().replaceJsonDocument(SCHEMA.cCampuses, SCHEMA.fCampusCuid, campus.getId(), campus.toGeoJSON()))
            return AnyResponseHelper.bad_request("Campus could not be updated!")
          return AnyResponseHelper.ok("Successfully updated campus!")
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
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
        LPLogger.info("AnyplaceMapping::buildingSetAll(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fAccessToken)
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        // get access token from url and check it against google's service
        if (json.\\(SCHEMA.fAccessToken) == null)
          return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyId((json \ SCHEMA.fAccessToken).as[String])
        if (owner_id == null)
          return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendGoogleIdIfNeeded(owner_id)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        if (owner_id == null || owner_id.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        try {
          val buildingsets = ProxyDataSource.getIDatasource().getAllBuildingsetsByOwner(owner_id)
          val res: JsValue = Json.obj("buildingsets" -> buildingsets)
          try
            gzippedJSONOk(res.toString)
          catch {
            case ioe: IOException =>
              return AnyResponseHelper.ok(res, "Successfully retrieved all buildingsets!")
          }
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
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
        LPLogger.info("AnyplaceMapping::campusDelete(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fCampusCuid, SCHEMA.fAccessToken)
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        // get access token from url and check it against google's service
        if (json.\\(SCHEMA.fAccessToken) == null)
          return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyId((json \ SCHEMA.fAccessToken).as[String])
        if (owner_id == null)
          return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendGoogleIdIfNeeded(owner_id)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        if (String(json, SCHEMA.fCampusCuid) == null)
          return AnyResponseHelper.bad_request("cuid field must be String!")
        val cuid = (json \ SCHEMA.fCampusCuid).as[String]
        try {
          val stored_campus = ProxyDataSource.getIDatasource().getFromKeyAsJson(SCHEMA.cCampuses, SCHEMA.fCampusCuid, cuid)
          if (stored_campus == null)
            return AnyResponseHelper.bad_request("Campus does not exist or could not be retrieved!")
          if (!isCampusOwner(stored_campus, owner_id))
            return AnyResponseHelper.unauthorized("Unauthorized")
          if (!ProxyDataSource.getIDatasource().deleteFromKey(SCHEMA.cCampuses, SCHEMA.fCampusCuid, cuid))
            return AnyResponseHelper.internal_server_error("500: Failed to delete Campus")
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
        return AnyResponseHelper.ok("Successfully deleted everything related to building!")
      }

      inner(request)
  }

  private def isCampusOwner(campus: JsValue, userId: String): Boolean = { // Admin
    if (userId.equals(ADMIN_ID))
      return true
    // Check if owner
    if (campus != null && (campus \ SCHEMA.fOwnerId).toOption.isDefined) {
      return (campus \ SCHEMA.fOwnerId).as[String].equals(userId)
    }
    false
  }

  def floorAdd() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.D2("AnyplaceMapping::floorAdd(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fIsPublished, SCHEMA.fBuid, SCHEMA.fFloorName,
          SCHEMA.fDescription, SCHEMA.fFloorNumber, SCHEMA.fAccessToken)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\(SCHEMA.fAccessToken).getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyId((json \ SCHEMA.fAccessToken).as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendGoogleIdIfNeeded(owner_id)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        if (StringNumber(json, SCHEMA.fFloorNumber) == null)
          return AnyResponseHelper.bad_request("floor_number field must be String, containing a number!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
        val floor_number = (json \ SCHEMA.fFloorNumber).as[String]
        if (!Floor.checkFloorNumberFormat(floor_number)) return AnyResponseHelper.bad_request("Floor number cannot contain whitespace!")
        try {
          json = json.as[JsObject] - SCHEMA.fAccessToken
          val floor = new Floor(json)
          if (!ProxyDataSource.getIDatasource.addJsonDocument(SCHEMA.cFloorplans, stripJson(floor.toValidMongoJson()))) return AnyResponseHelper.bad_request("Floor already exists or could not be added!")
          return AnyResponseHelper.ok("Successfully added floor " + floor_number + "!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  @deprecated("NotInUse")
  def floorUpdate() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::floorUpdate(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fFloorNumber, SCHEMA.fAccessToken)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\(SCHEMA.fAccessToken).getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyId((json \ SCHEMA.fAccessToken).as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendGoogleIdIfNeeded(owner_id)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
        val floor_number = (json \ "fllor_number").as[String]
        if (!Floor.checkFloorNumberFormat(floor_number)) return AnyResponseHelper.bad_request("Floor number cannot contain whitespace!")
        try {
          val fuid = Floor.getId(buid, floor_number)
          var stored_floor = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cFloorplans, SCHEMA.fFuid, fuid)
          if (stored_floor == null) return AnyResponseHelper.bad_request("Floor does not exist or could not be retrieved!")
          if (json.\(SCHEMA.fIsPublished).getOrElse(null) != null)
            stored_floor = stored_floor.as[JsObject] + (SCHEMA.fIsPublished -> JsString((json \ SCHEMA.fIsPublished).as[String]))
          if (json.\(SCHEMA.fFloorName).getOrElse(null) != null)
            stored_floor = stored_floor.as[JsObject] + (SCHEMA.fFloorName, JsString((json \ SCHEMA.fFloorName).as[String]))
          if (json.\(SCHEMA.fDescription).getOrElse(null) != null)
            stored_floor = stored_floor.as[JsObject] + (SCHEMA.fDescription, JsString((json \ SCHEMA.fDescription).as[String]))
          val floor = new Floor(stored_floor)
          if (!ProxyDataSource.getIDatasource.replaceJsonDocument(SCHEMA.cFloorplans, SCHEMA.fFuid, floor.getId, floor.toValidMongoJson().toString))
            return AnyResponseHelper.bad_request("Floor could not be updated!")
          return AnyResponseHelper.ok("Successfully updated floor!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  /**
   * Deletes the ACCES data (CRLB) for a space.
   */
  def deleteAccesSpaceData() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floor_number = (json \ SCHEMA.fFloor).as[String]
        val file_path = new File(
          Play.application().configuration().getString("crlbsDir") +
            File.separatorChar + buid + File.separator + "fl_" + floor_number + ".txt")
        if (file_path.exists()) {
          if (file_path.delete) {
            return AnyResponseHelper.ok("Deleted floor :" + floor_number)
          }
        }
        return AnyResponseHelper.bad_request("ERROR: while deleting: " + floor_number)
      }

      inner(request)
  }

  def floorDelete() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::floorDelete(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fFloorNumber, SCHEMA.fAccessToken)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\(SCHEMA.fAccessToken).getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyId((json \ SCHEMA.fAccessToken).as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendGoogleIdIfNeeded(owner_id)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        if (StringNumber(json, SCHEMA.fFloorNumber) == null)
          return AnyResponseHelper.bad_request("floor_number field must be String, containing a number!")
        val floor_number = (json \ SCHEMA.fFloorNumber).as[String]
        try {
          val stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
        try {
          val deleted = ProxyDataSource.getIDatasource.deleteAllByFloor(buid, floor_number)
          if (deleted == false)
            return AnyResponseHelper.bad_request("Some items related to the floor could not be deleted.")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
        val filePath = AnyPlaceTilerHelper.getFloorPlanFor(buid, floor_number)
        try {
          val floorfile = new File(filePath)
          /*
           * DELETE FLOOR : BuxFix
           * Fixing floor plan files and directory removal during floor delete
           */
          if (floorfile.exists()) HelperMethods.recDeleteDirFile(floorfile.getParentFile())
        } catch {
          case e: IOException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage + "] while deleting floor plan." +
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
        LPLogger.info("AnyplaceMapping::floorAll(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val floors = ProxyDataSource.getIDatasource.floorsByBuildingAsJson(buid)
          val res: JsValue = Json.obj("floors" -> floors.toList)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all floors!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
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
        LPLogger.info("AnyplaceMapping::poisAdd(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fIsPublished, SCHEMA.fBuid, SCHEMA.fFloorName,
          SCHEMA.fFloorNumber, SCHEMA.fName, SCHEMA.fPoisType, SCHEMA.fIsDoor, SCHEMA.fIsBuildingEntrance, SCHEMA.fCoordinatesLat, SCHEMA.fCoordinatesLon,
          SCHEMA.fAccessToken)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\(SCHEMA.fAccessToken).getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyId((json \ SCHEMA.fAccessToken).as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendGoogleIdIfNeeded(owner_id)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id)) - SCHEMA.fAccessToken
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("Buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id)) return AnyResponseHelper.unauthorized("Unauthorized")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
        try {
          val poi = new Poi(json)
          if (!ProxyDataSource.getIDatasource.addJsonDocument(SCHEMA.cPOIS, poi.toGeoJSON())) return AnyResponseHelper.bad_request("Poi already exists or could not be added!")
          val res: JsValue = Json.obj(SCHEMA.fPuid -> poi.getId)
          return AnyResponseHelper.ok(res, "Successfully added poi!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
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
        LPLogger.info("AnyplaceMapping::poisUpdate(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fPuid, SCHEMA.fBuid, SCHEMA.fAccessToken)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\(SCHEMA.fAccessToken).getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyId((json \ SCHEMA.fAccessToken).as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendGoogleIdIfNeeded(owner_id)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        if (String(json, SCHEMA.fPuid) == null)
          return AnyResponseHelper.bad_request("Puid field must be String!")
        val puid = (json \ SCHEMA.fPuid).as[String]
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("Buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id)) {
            return AnyResponseHelper.unauthorized("Unauthorized")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
        try {
          var stored_poi = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cPOIS, SCHEMA.fPuid, puid)
          if (stored_poi == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (json.\(SCHEMA.fIsPublished).getOrElse(null) != null) {
            val is_published = (json \ SCHEMA.fIsPublished).as[String]
            if (is_published == "true" || is_published == "false")
              stored_poi = stored_poi.as[JsObject] + (SCHEMA.fIsPublished -> JsString((json \ SCHEMA.fIsPublished).as[String]))
          }
          if (json.\(SCHEMA.fName).getOrElse(null) != null)
            stored_poi = stored_poi.as[JsObject] + (SCHEMA.fName -> JsString((json \ SCHEMA.fName).as[String]))
          if (json.\(SCHEMA.fDescription).getOrElse(null) != null)
            stored_poi = stored_poi.as[JsObject] + (SCHEMA.fDescription -> JsString((json \ SCHEMA.fDescription).as[String]))
          if (json.\(SCHEMA.fURL).getOrElse(null) != null)
            stored_poi = stored_poi.as[JsObject] + (SCHEMA.fURL -> JsString((json \ SCHEMA.fURL).as[String]))
          if (json.\(SCHEMA.fPoisType).getOrElse(null) != null)
            stored_poi = stored_poi.as[JsObject] + (SCHEMA.fPoisType -> JsString((json \ SCHEMA.fPoisType).as[String]))
          if (json.\(SCHEMA.fIsDoor).getOrElse(null) != null) {
            val is_door = (json \ SCHEMA.fIsDoor).as[String]
            if (is_door == "true" || is_door == "false")
              stored_poi = stored_poi.as[JsObject] + (SCHEMA.fIsDoor -> JsString((json \ SCHEMA.fIsDoor).as[String]))
          }
          if (json.\(SCHEMA.fIsBuildingEntrance).getOrElse(null) != null) {
            val is_building_entrance = (json \ SCHEMA.fIsBuildingEntrance).as[String]
            if (is_building_entrance == "true" || is_building_entrance == "false")
              stored_poi = stored_poi.as[JsObject] + (SCHEMA.fIsBuildingEntrance, JsString((json \ SCHEMA.fIsBuildingEntrance).as[String]))
          }
          if (json.\(SCHEMA.fImage).getOrElse(null) != null)
            stored_poi = stored_poi.as[JsObject] + (SCHEMA.fImage, JsString((json \ SCHEMA.fImage).as[String]))
          if (json.\(SCHEMA.fCoordinatesLat).getOrElse(null) != null)
            stored_poi = stored_poi.as[JsObject] + (SCHEMA.fCoordinatesLat, JsString((json \ SCHEMA.fCoordinatesLat).as[String]))
          if (json.\(SCHEMA.fCoordinatesLon).getOrElse(null) != null)
            stored_poi = stored_poi.as[JsObject] + (SCHEMA.fCoordinatesLon, JsString((json \ SCHEMA.fCoordinatesLon).as[String]))
          val poi = new Poi(stored_poi)
          if (!ProxyDataSource.getIDatasource.replaceJsonDocument(SCHEMA.cPOIS, SCHEMA.fPuid, poi.getId, poi.toGeoJSON()))
            return AnyResponseHelper.bad_request("Poi could not be updated!")
          return AnyResponseHelper.ok("Successfully updated poi!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
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
        LPLogger.info("AnyplaceMapping::poiDelete(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fPuid, SCHEMA.fBuid, SCHEMA.fAccessToken)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\(SCHEMA.fAccessToken).getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyId((json \ SCHEMA.fAccessToken).as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendGoogleIdIfNeeded(owner_id)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("Buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        if (String(json, SCHEMA.fPuid) == null)
          return AnyResponseHelper.bad_request("Puid field must be String!")
        val puid = (json \ SCHEMA.fPuid).as[String]
        try {
          val stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id))
            return AnyResponseHelper.unauthorized("Unauthorized")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
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
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
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
        LPLogger.info("AnyplaceMapping::poisByFloor(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fFloorNumber)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("Buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        if (StringNumber(json, SCHEMA.fFloorNumber) == null)
          return AnyResponseHelper.bad_request("Floor_number field must be String, containing a number!")
        val floor_number = (json \ SCHEMA.fFloorNumber).as[String]
        try {
          val stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          val pois = ProxyDataSource.getIDatasource.poisByBuildingFloorAsJson(buid, floor_number)
          val res: JsValue = Json.obj(SCHEMA.cPOIS -> pois)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all pois from floor " + floor_number +
              "!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  def poisByBuid() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::poisByBuid(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("Buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          val pois = ProxyDataSource.getIDatasource.poisByBuildingAsJson(buid)
          val res: JsValue = Json.obj(SCHEMA.cPOIS -> pois.toList)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all pois from building.")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
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
        LPLogger.info("PoisAll")
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody)
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        var cuid = request.getQueryString(SCHEMA.fConCuid).orNull
        if (cuid == null) cuid = (json \ SCHEMA.fConCuid).as[String]
        var letters = request.getQueryString("letters").orNull
        if (letters == null) letters = (json \ "letters").as[String]
        var buid = request.getQueryString(SCHEMA.fBuid).orNull
        if (buid == null) buid = (json \ SCHEMA.fBuid).as[String]
        var greeklish = request.getQueryString(SCHEMA.fGreeklish).orNull
        if (greeklish == null) greeklish = (json \ SCHEMA.fGreeklish).as[String]
        try {
          var result: List[JsValue] = null
          if (cuid.compareTo("") == 0)
            result = ProxyDataSource.getIDatasource.poisByBuildingAsJson3(buid, letters)
          else if (greeklish.compareTo("true") == 0)
            result = ProxyDataSource.getIDatasource.poisByBuildingAsJson2GR(cuid, letters)
          else
            result = ProxyDataSource.getIDatasource.poisByBuildingAsJson2(cuid, letters)
          val res: JsValue = Json.obj(SCHEMA.cPOIS -> result)
          try
            gzippedJSONOk(res.toString)
          catch {
            case ioe: IOException =>
              return AnyResponseHelper.ok(res, "Successfully retrieved all pois from building.")
          }
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
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
        LPLogger.info("AnyplaceMapping::poisByBuidincConnectors(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid)
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val pois = ProxyDataSource.getIDatasource.poisByBuildingIDAsJson(buid)
          val res: JsValue = Json.obj(SCHEMA.cPOIS -> pois)
          try
            gzippedJSONOk(res.toString)
          catch {
            case ioe: IOException =>
              return AnyResponseHelper.ok(res, "Successfully retrieved all pois from buid " + buid + "!")
          }
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
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
        LPLogger.info("AnyplaceMapping::connectionAdd(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fIsPublished, SCHEMA.fPoisA, SCHEMA.fFloorA,
          SCHEMA.fBuidA, SCHEMA.fPoisB, SCHEMA.fFloorB, SCHEMA.fBuidB, SCHEMA.fBuid, SCHEMA.fEdgeType, SCHEMA.fAccessToken)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\(SCHEMA.fAccessToken).getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyId((json \ SCHEMA.fAccessToken).as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendGoogleIdIfNeeded(owner_id)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        if (String(json, SCHEMA.fBuidA) == null)
          return AnyResponseHelper.bad_request("buid_a field must be String!")
        val buid1 = (json \ SCHEMA.fBuidA).as[String]
        if (String(json, SCHEMA.fBuidB) == null)
          return AnyResponseHelper.bad_request("buid_b field must be String!")
        val buid2 = (json \ SCHEMA.fBuidB).as[String]
        if (StringNumber(json, SCHEMA.fFloorA) == null)
          return AnyResponseHelper.bad_request("floor_a field must be String, containing a number!")
        if (StringNumber(json, SCHEMA.fFloorB) == null)
          return AnyResponseHelper.bad_request("floor_b field must be String, containing a number!")
        try {
          var stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid1)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id))
            return AnyResponseHelper.unauthorized("Unauthorized")
          stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid2)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id))
            return AnyResponseHelper.unauthorized("Unauthorized")
          stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
        val edge_type = (json \ SCHEMA.fEdgeType).as[String]
        if (edge_type != Connection.EDGE_TYPE_ELEVATOR && edge_type != Connection.EDGE_TYPE_HALLWAY &&
          edge_type != Connection.EDGE_TYPE_ROOM && edge_type != Connection.EDGE_TYPE_OUTDOOR &&
          edge_type != Connection.EDGE_TYPE_STAIR)
          return AnyResponseHelper.bad_request("Invalid edge type specified.")
        val pois_a = (json \ SCHEMA.fPoisA).as[String]
        val pois_b = (json \ SCHEMA.fPoisB).as[String]

        if (!ProxyDataSource.getIDatasource().poiByBuidFloorPuid(buid1, (json \ SCHEMA.fFloorA).as[String], (json \ SCHEMA.fPoisA).as[String]))
          return AnyResponseHelper.bad_request("pois_a does not exist or could not be retrieved!")
        if (!ProxyDataSource.getIDatasource().poiByBuidFloorPuid(buid2, (json \ SCHEMA.fFloorB).as[String], (json \ SCHEMA.fPoisB).as[String]))
          return AnyResponseHelper.bad_request("pois_b does not exist or could not be retrieved!")
        try {
          val weight = calculateWeightOfConnection(pois_a, pois_b)
          json = json.as[JsObject] + (SCHEMA.fWeight -> JsString(java.lang.Double.toString(weight)))
          if (edge_type == Connection.EDGE_TYPE_ELEVATOR || edge_type == Connection.EDGE_TYPE_STAIR) {
          }
          val conn = new Connection(json)
          if (!ProxyDataSource.getIDatasource.addJsonDocument(SCHEMA.cEdges, conn.toValidMongoJson().toString))
            return AnyResponseHelper.bad_request("Connection already exists or could not be added!")
          val res: JsValue = Json.obj(SCHEMA.fConCuid -> conn.getId)
          return AnyResponseHelper.ok(res, "Successfully added new connection!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  /**
   * DEPRECATED: NN: Possibly because edges/connection are added or deleted.
   * If the coordinates of a poi (pointed by an edge) change the connection is still unaffected.
   *
   * @return
   */
  @deprecated("NotInUse")
  def connectionUpdate() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::connectionUpdate(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fPoisA, SCHEMA.fPoisB, SCHEMA.fBuidA, SCHEMA.fBuidB,
          SCHEMA.fAccessToken)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\(SCHEMA.fAccessToken).getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyId((json \ SCHEMA.fAccessToken).as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendGoogleIdIfNeeded(owner_id)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val buid1 = (json \ SCHEMA.fBuidA).as[String]
        val buid2 = (json \ SCHEMA.fBuidB).as[String]
        try {
          var stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid1)
          if (stored_building == null)
            return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id))
            return AnyResponseHelper.unauthorized("Unauthorized")
          stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid2)
          if (stored_building == null)
            return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id))
            return AnyResponseHelper.unauthorized("Unauthorized")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
        try {
          val pois_a = (json \ SCHEMA.fPoisA).as[String]
          val pois_b = (json \ SCHEMA.fPoisB).as[String]
          val cuid = Connection.getId(pois_a, pois_b)
          var stored_conn = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cEdges, SCHEMA.fConCuid, cuid)
          if (stored_conn == null)
            return AnyResponseHelper.bad_request("Connection does not exist or could not be retrieved!")
          if (json.\(SCHEMA.fIsPublished).getOrElse(null) != null) {
            val is_published = (json \ SCHEMA.fIsPublished).as[String]
            if (is_published == "true" || is_published == "false")
              stored_conn = stored_conn.as[JsObject] + (SCHEMA.fIsPublished -> JsString((json \ SCHEMA.fIsPublished).as[String]))
          }
          if (json.\(SCHEMA.fEdgeType).getOrElse(null) != null) {
            val edge_type = (json \ SCHEMA.fEdgeType).as[String]
            if (edge_type != Connection.EDGE_TYPE_ELEVATOR && edge_type != Connection.EDGE_TYPE_HALLWAY &&
              edge_type != Connection.EDGE_TYPE_ROOM && edge_type != Connection.EDGE_TYPE_OUTDOOR &&
              edge_type != Connection.EDGE_TYPE_STAIR)
              return AnyResponseHelper.bad_request("Invalid edge type specified.")
            stored_conn = stored_conn.as[JsObject] + (SCHEMA.fEdgeType -> JsString(edge_type))
          }
          val conn = new Connection(stored_conn)
          if (!ProxyDataSource.getIDatasource.replaceJsonDocument(SCHEMA.cEdges, SCHEMA.fConCuid, conn.getId, conn.toValidMongoJson().toString))
            return AnyResponseHelper.bad_request("Connection could not be updated!")
          return AnyResponseHelper.ok("Successfully updated connection!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
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
        LPLogger.info("AnyplaceMapping::poiDelete(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fPoisA, SCHEMA.fPoisB, SCHEMA.fBuidA, SCHEMA.fBuidB,
          SCHEMA.fAccessToken)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (json.\(SCHEMA.fAccessToken).getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var owner_id = verifyId((json \ SCHEMA.fAccessToken).as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        owner_id = appendGoogleIdIfNeeded(owner_id)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        if (String(json, SCHEMA.fBuidA) == null)
          return AnyResponseHelper.bad_request("buid_a field must be String!")
        val buid1 = (json \ SCHEMA.fBuidA).as[String]
        if (String(json, SCHEMA.fBuidB) == null)
          return AnyResponseHelper.bad_request("buid_b field must be String!")
        val buid2 = (json \ SCHEMA.fBuidB).as[String]
        try {
          var stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid1)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building_a does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id))
            return AnyResponseHelper.unauthorized("Unauthorized")
          stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid2)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building_b does not exist or could not be retrieved!")
          if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id))
            return AnyResponseHelper.unauthorized("Unauthorized")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
        if (String(json, SCHEMA.fPoisA) == null)
          return AnyResponseHelper.bad_request("pois_a field must be String!")
        val pois_a = (json \ SCHEMA.fPoisA).as[String]
        if (String(json, SCHEMA.fPoisB) == null)
          return AnyResponseHelper.bad_request("pois_b field must be String!")
        val pois_b = (json \ SCHEMA.fPoisB).as[String]
        try {
          val cuid = Connection.getId(pois_a, pois_b)
          val all_items_failed = ProxyDataSource.getIDatasource.deleteAllByConnection(cuid)
          if (all_items_failed == null) {
            LPLogger.info("AnyplaceMapping::connectionDelete(): " + cuid + " not found.")
            return AnyResponseHelper.bad_request("POI Connection not found")
          }
          if (all_items_failed.size > 0) {
            val obj: JsValue = Json.obj("ids" -> all_items_failed.toList)
            return AnyResponseHelper.bad_request(obj, "Some items related to the deleted connection could not be deleted: " +
              all_items_failed.size + " items.")
          }
          return AnyResponseHelper.ok("Successfully deleted everything related to the connection!")
        } catch {
          case e: DatasourceException => AnyResponseHelper.internal_server_error("500: " + e.getMessage)
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
        LPLogger.info("AnyplaceMapping::poisByFloor(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fFloorNumber)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        if (StringNumber(json, SCHEMA.fFloorNumber) == null)
          return AnyResponseHelper.bad_request("Floor_number field must be String, containing a number!")
        val floor_number = (json \ SCHEMA.fFloorNumber).as[String]
        try {
          val stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          val stored_floors = ProxyDataSource.getIDatasource.floorsByBuildingAsJson(buid)
          var floorExists = false
          for (floor <- stored_floors)
            if ((floor \ SCHEMA.fFloorNumber).as[String] == floor_number)
              floorExists = true
          if (!floorExists) return AnyResponseHelper.bad_request("Floor does not exist or could not be retrieved!")

          val pois = ProxyDataSource.getIDatasource.connectionsByBuildingFloorAsJson(buid, floor_number)
          val res: JsValue = Json.obj("connections" -> pois)
          try {
            gzippedJSONOk(res.toString)
          } catch {
            case ioe: IOException => return AnyResponseHelper.ok(res, "Successfully retrieved all pois from floor " + floor_number +
              "!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
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
        LPLogger.info("AnyplaceMapping::connectionsByallFloors(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid)
        if (!requiredMissing.isEmpty)
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val stored_building = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_building == null) return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          val pois = ProxyDataSource.getIDatasource.connectionsByBuildingAllFloorsAsJson(buid)
          val res: JsValue = Json.obj("connections" -> pois)
          try
            gzippedJSONOk(res.toString)
          catch {
            case ioe: IOException =>
              return AnyResponseHelper.ok(res, "Successfully retrieved all pois from all floors !")
          }
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
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
    val pa = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cPOIS, SCHEMA.fPuid, pois_a)
    if (pa == null) {
      lat_a = 0.0
      lon_a = 0.0
    } else try {
      lat_a = nf.parse((pa \ SCHEMA.fCoordinatesLat).as[String]).doubleValue()
      lon_a = nf.parse((pa \ SCHEMA.fCoordinatesLon).as[String]).doubleValue()
    } catch {
      case e: ParseException => e.printStackTrace()
    }
    val pb = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cPOIS, SCHEMA.fPuid, pois_b)
    if (pb == null) {
      lat_b = 0.0
      lon_b = 0.0
    } else try {
      lat_b = nf.parse((pb \ SCHEMA.fCoordinatesLat).as[String]).doubleValue()
      lon_b = nf.parse((pb \ SCHEMA.fCoordinatesLon).as[String]).doubleValue()
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
        LPLogger.info("AnyplaceMapping::serveFloorPlan(): " + stripJson(json))
        val filePath = AnyPlaceTilerHelper.getFloorPlanFor(buid, floor_number)
        LPLogger.info("requested: " + filePath)
        try {
          val file = new File(filePath)
          // LPLogger.debug("filePath " + file.getAbsolutePath.toString)
          if (!file.exists()) return AnyResponseHelper.bad_request("Requested floor plan does not exist");
          if (!file.canRead()) return AnyResponseHelper.bad_request("Requested floor plan cannot be read: " +
            floor_number)
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
        LPLogger.info("AnyplaceMapping::serveFloorPlanTilesZip(): " + stripJson(json))
        if (!Floor.checkFloorNumberFormat(floor_number)) return AnyResponseHelper.bad_request("Floor number cannot contain whitespace!")
        val filePath = AnyPlaceTilerHelper.getFloorTilesZipFor(buid, floor_number)
        LPLogger.info("requested: " + filePath)
        try {
          val file = new File(filePath)
          if (!file.exists()) return AnyResponseHelper.bad_request("Requested floor plan does not exist");
          if (!file.canRead()) return AnyResponseHelper.bad_request("Requested floor plan cannot be read: " +
            floor_number)
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
        LPLogger.info("AnyplaceMapping::serveFloorPlanTilesZipLink(): " + stripJson(json))
        if (!Floor.checkFloorNumberFormat(floor_number)) return AnyResponseHelper.bad_request("Floor number cannot contain whitespace!")
        val filePath = AnyPlaceTilerHelper.getFloorTilesZipFor(buid, floor_number)
        LPLogger.info("requested: " + filePath)
        val file = new File(filePath)
        if (!file.exists()) return AnyResponseHelper.bad_request("Requested floor plan does not exist");
        if (!file.canRead()) return AnyResponseHelper.bad_request("Requested floor plan cannot be read: " +
          floor_number)
        val res: JsValue = Json.obj("tiles_archive" -> AnyPlaceTilerHelper.getFloorTilesZipLinkFor(buid, floor_number))
        return AnyResponseHelper.ok(res, "Successfully fetched link for the tiles archive!")
      }

      inner(request)
  }

  def serveFloorPlanTilesStatic(buid: String, floor_number: String, path: String) = Action {
    def inner(): Result = {
      if (path == null || buid == null || floor_number == null ||
        path.trim().isEmpty ||
        buid.trim().isEmpty ||
        floor_number.trim().isEmpty) NotFound(<h1>Page not found</h1>)
      var filePath: String = null
      filePath = if (path == AnyPlaceTilerHelper.FLOOR_TILES_ZIP_NAME) AnyPlaceTilerHelper.getFloorTilesZipFor(buid,
        floor_number) else AnyPlaceTilerHelper.getFloorTilesDirFor(buid, floor_number) +
        path
      try {
        val file = new File(filePath)
        //send ok message to tiler
        if (!file.exists() || !file.canRead()) return AnyResponseHelper.ok("File requested not found")
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
        LPLogger.info("AnyplaceMapping::serveFloorPlanBase64(): " + stripJson(json))
        val filePath = AnyPlaceTilerHelper.getFloorPlanFor(buid, floor_number)
        LPLogger.info("requested: " + filePath)
        val file = new File(filePath)
        try {
          if (!file.exists()) return AnyResponseHelper.bad_request("Requested floor plan does not exist");
          if (!file.canRead()) return AnyResponseHelper.bad_request("Requested floor plan cannot be read: " +
            floor_number)

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
        LPLogger.info("AnyplaceMapping::serveFloorPlanBase64all(): " + stripJson(json) + " " + floor_number)
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
        val res: JsValue = Json.obj("all_floors" -> all_floors.toList)
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

  @deprecated("NotInUse")
  def floorPlanUpload() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {

        return AnyResponseHelper.DEPRECATED("Invalid request type - Not Multipart!")


        val anyReq = new OAuth2Request(request)
        val body = anyReq.getMultipartFormData()
        if (body == null) return AnyResponseHelper.bad_request("Invalid request type - Not Multipart!")
        var floorplan = body.file("floorplan").get
        if (floorplan == null) return AnyResponseHelper.bad_request("Cannot find the floor plan file in your request!")
        val urlenc = body.asFormUrlEncoded
        val json_str = urlenc.get("json").get(0)
        if (json_str == null) return AnyResponseHelper.bad_request("Cannot find json in the request!")
        var json: JsValue = null
        try {
          json = Json.parse(json_str)
        } catch {
          case e: IOException => return AnyResponseHelper.bad_request("Cannot parse json in the request!")
        }
        LPLogger.info("Floorplan Request[json]: " + json.toString)
        LPLogger.info("Floorplan Request[floorplan]: " + floorplan.filename)
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fFloorNumber, SCHEMA.fLatBottomLeft,
          SCHEMA.fLonBottomLeft, SCHEMA.fLatTopRight, SCHEMA.fLonTopRight)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floor_number = (json \ SCHEMA.fFloorNumber).as[String]
        val bottom_left_lat = (json \ SCHEMA.fLatBottomLeft).as[String]
        val bottom_left_lng = (json \ SCHEMA.fLonBottomLeft).as[String]
        val top_right_lat = (json \ SCHEMA.fLatTopRight).as[String]
        val top_right_lng = (json \ SCHEMA.fLonTopRight).as[String]
        val fuid = Floor.getId(buid, floor_number)
        try {
          var stored_floor = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cFloorplans, SCHEMA.fFuid, fuid)
          if (stored_floor == null) return AnyResponseHelper.bad_request("Floor does not exist or could not be retrieved!")
          stored_floor = stored_floor.as[JsObject] + (SCHEMA.fLatBottomLeft -> JsString(bottom_left_lat))
          stored_floor = stored_floor.as[JsObject] + (SCHEMA.fLonBottomLeft -> JsString(bottom_left_lng))
          stored_floor = stored_floor.as[JsObject] + (SCHEMA.fLatTopRight -> JsString(top_right_lat))
          stored_floor = stored_floor.as[JsObject] + (SCHEMA.fLonTopRight -> JsString(top_right_lng))
          if (!ProxyDataSource.getIDatasource.replaceJsonDocument(SCHEMA.cFloorplans, SCHEMA.fFuid, fuid, stored_floor.toString))
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
        LPLogger.info("Successfully tiled: " + floor_file.toString)
        return AnyResponseHelper.ok("Successfully updated floor plan!")
      }

      inner(request)
  }

  def floorPlanUploadWithZoom() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        LPLogger.D2("floorPlanUploadWithZoom")
        val anyReq = new OAuth2Request(request)
        val body = anyReq.getMultipartFormData()
        if (body == null) return AnyResponseHelper.bad_request("Invalid request type - Not Multipart!")
        val floorplan = body.file("floorplan").get
        if (floorplan == null) return AnyResponseHelper.bad_request("Cannot find the floor plan file in your request!")
        val urlenc = body.asFormUrlEncoded
        val json_str = urlenc.get("json").get(0)
        if (json_str == null) return AnyResponseHelper.bad_request("Cannot find json in the request!")
        var json: JsValue = null
        try {
          json = Json.parse(json_str)
        } catch {
          case e: IOException => return AnyResponseHelper.bad_request("Cannot parse json in the request!")
        }
        LPLogger.info("Floorplan Request[json]: " + json.toString)
        LPLogger.info("Floorplan Request[floorplan]: " + floorplan.filename)
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fFloorNumber, SCHEMA.fLatBottomLeft,
          SCHEMA.fLonBottomLeft, SCHEMA.fLatTopRight, SCHEMA.fLonTopRight, SCHEMA.fZoom)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        val zoom = (json \ SCHEMA.fZoom).as[String]
        val zoom_number = zoom.toInt
        if (zoom_number < 20)
          return AnyResponseHelper.bad_request("You have provided zoom level " + zoom + ". You have to zoom at least to level 20 to upload the floorplan.")

        val floor_number = (json \ SCHEMA.fFloorNumber).as[String]
        val bottom_left_lat = (json \ SCHEMA.fLatBottomLeft).as[String]
        val bottom_left_lng = (json \ SCHEMA.fLonBottomLeft).as[String]
        val top_right_lat = (json \ SCHEMA.fLatTopRight).as[String]
        val top_right_lng = (json \ SCHEMA.fLonTopRight).as[String]
        val fuid = Floor.getId(buid, floor_number)
        try {
          var stored_floor = ProxyDataSource.getIDatasource.getFromKeyAsJson(SCHEMA.cFloorplans, SCHEMA.fFuid, fuid)
          if (stored_floor == null) return AnyResponseHelper.bad_request("Floor does not exist or could not be retrieved!")
          stored_floor = stored_floor.as[JsObject] + (SCHEMA.fZoom -> JsString(zoom))
          stored_floor = stored_floor.as[JsObject] + (SCHEMA.fLatBottomLeft -> JsString(bottom_left_lat))
          stored_floor = stored_floor.as[JsObject] + (SCHEMA.fLonBottomLeft -> JsString(bottom_left_lng))
          stored_floor = stored_floor.as[JsObject] + (SCHEMA.fLatTopRight -> JsString(top_right_lat))
          stored_floor = stored_floor.as[JsObject] + (SCHEMA.fLonTopRight -> JsString(top_right_lng))
          if (!ProxyDataSource.getIDatasource.replaceJsonDocument(SCHEMA.cFloorplans, SCHEMA.fFuid, fuid, stored_floor.toString)) {
            return AnyResponseHelper.bad_request("Floor plan could not be updated in the database!")
          }
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
          AnyPlaceTilerHelper.tileImageWithZoom(floor_file, top_left_lat, top_left_lng, zoom)
        } catch {
          case e: AnyPlaceException => return AnyResponseHelper.bad_request("Could not create floor plan tiles on the server!")
        }
        LPLogger.info("Successfully tiled: " + floor_file.toString)
        return AnyResponseHelper.ok("Successfully updated floor plan!")
      }

      inner(request)
  }

  def getAccountType(json: JsValue): ExternalType = {
    val external = (json \ SCHEMA.fExternal)
    if (external.toOption.isDefined) {
      val exts = external.as[String]
      if (exts == "google") return ExternalType.GOOGLE
    }
    ExternalType.LOCAL
  }


  def isFirstUser(): Boolean = {
    val mdb: MongoDatabase = MongodbDatasource.getMDB
    val collection = mdb.getCollection(SCHEMA.cUsers)
    val users = collection.find()
    var awaited = Await.result(users.toFuture, Duration.Inf)
    var res = awaited.toList
    return (res.size == 0)
  }

  def getUser(json: JsValue): JsValue = {
    val mdb: MongoDatabase = MongodbDatasource.getMDB
    val collection = mdb.getCollection(SCHEMA.cUsers)
    var user: JsValue = null
    getAccountType(json) match {
      case ExternalType.GOOGLE => {
        val mdb: MongoDatabase = MongodbDatasource.getMDB
        val collection = mdb.getCollection(SCHEMA.cUsers)
        val ownerId = (json \ SCHEMA.fOwnerId).as[String]
        val userLookUp = collection.find(equal(SCHEMA.fOwnerId, ownerId))
        val awaited = Await.result(userLookUp.toFuture, Duration.Inf)
        val res = awaited.toList
        if (res.size == 1) {
          user = convertJson(res.get(0))
        } else if (res.size > 1) {
          LPLogger.error("User exists. More than one user with id: " + ownerId)
        }

      }
      case ExternalType.LOCAL => LPLogger.debug("TODO: query unique email")
    }
    //    CLR:NN
    //    val users = collection.find()
    //    var awaited = Await.result(users.toFuture, Duration.Inf)
    //    var res = awaited.toList
    //    return (res.size == 0)
    return user
  }


  /**
   *
   * @return type(admin, user, .. etc) + message
   */
  def addAccount() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        LPLogger.D1("AddAccount")
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody

        if (isNullOrEmpty(json)) {
          return AnyResponseHelper.bad_request(AnyResponseHelper.WRONG_API_USAGE)
        } else {
          LPLogger.D1("OK")
        }

        json = appendUserType(json)
        val external = (json \ SCHEMA.fExternal)
        var result: Result = null
        if (external.toOption.isDefined) {
          result = addGoogleAccount(json)
        } else {
          // addLocalAccount() // TODO
          LPLogger.error("TODO: Add Local Account")
          null
        }
        //          LPLogger.D2("Logged in user: " + (json \ SCHEMA.fOwnerId))
        return result
      }

      inner(request)
  }


  // TODO if json has not type add type = user
  def appendUserType(json: JsValue): JsValue = {
    if ((json \ SCHEMA.fType).toOption.isDefined) {
      LPLogger.info("user type exists: " + (json \ SCHEMA.fType).as[String]) // Might crash
      return json
    } else {
      var userType: String = ""
      if (isFirstUser()) {
        userType = "admin"
        LPLogger.info("Initializing admin user!")
      } else {
        LPLogger.D4("AppendUserType: user")
        userType = "user"
      }
      return json.as[JsObject] + (SCHEMA.fType -> JsString(userType))
    }
  }

  // TODO: Implement
  // TODO new object with above but password encrypt (salt)
  // TODO add this to mongo (insert)
  // TODO Generate access_token: "local_VERY LONG SHA"
  def addLocalAccount(json: JsValue): Result = {

    // call appendUserType
    // ----------------------------
    //  requirePropertiesInJson: email, username, password
    val mdb: MongoDatabase = MongodbDatasource.getMDB
    val collection = mdb.getCollection(SCHEMA.cUsers)
    val userLookUp = collection.find(equal("username", (json \ "username").as[String]))
    val awaited = Await.result(userLookUp.toFuture, Duration.Inf)
    val res = awaited.toList
    if (res.size != 0) {
      // TODO user must have unique username (query username in mongo)
    }
    null
  }

  def addGoogleAccount(obj: JsValue): Result = {
    LPLogger.info("AnyplaceAccounts::addGoogleAccount()")
    var json = obj
    val notFound = JsonUtils.hasProperties(json, SCHEMA.fAccessToken, SCHEMA.fExternal) // TODO
    if (!notFound.isEmpty) return AnyResponseHelper.requiredFieldsMissing(notFound)
    if (json.\(SCHEMA.fAccessToken).getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
    var id = verifyId((json \ SCHEMA.fAccessToken).as[String])
    if (id == null) return AnyResponseHelper.forbidden("Unauthorized")
    id = appendGoogleIdIfNeeded(id)
    json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(id))
    LPLogger.D2("Logged in user: " + (json \ SCHEMA.fOwnerId))
    LPLogger.debug("type = " + (json \ SCHEMA.fType).as[String])
    val user = getUser(json)
    if (user != null) {
      val jsonRes = Json.obj(SCHEMA.fType -> JsString((user \ SCHEMA.fType).as[String]))
      LPLogger.D2("User already exists") // CLR:nn
      AnyResponseHelper.ok(jsonRes, "User Exists.") // its not AnyResponseHelperok
    } else {
      val jsonRes = Json.obj(SCHEMA.fType -> JsString((json \ SCHEMA.fType).as[String]))
      val newAccount = new Account(json)
      try {
        ProxyDataSource.getIDatasource.addJsonDocument(SCHEMA.cUsers, newAccount.toString())
        LPLogger.D2("Added google user") // CLR:nn
        AnyResponseHelper.ok(jsonRes, "Added google user.")
      } catch {
        case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
      }
    }

  }

  //  private def isBuildingOwner(building: JsonObject, userId: String): Boolean = {
  //    // Admin
  //    if (userId.equals(ADMIN_ID)) return true
  //    if (building != null && building.get(SCHEMA.fOwnerId) != null &&
  //      building.getString(SCHEMA.fOwnerId).equals(userId)) return true
  //    false
  //  }

  private def isBuildingOwner(building: JsValue, userId: String): Boolean = {
    // Admin
    if (userId.equals(ADMIN_ID)) return true
    if (building != null && (building \ SCHEMA.fOwnerId).toOption.isDefined &&
      (building \ (SCHEMA.fOwnerId)).as[String].equals(userId)) return true
    false
  }

  private def isBuildingCoOwner(building: JsValue, userId: String): Boolean = {
    // Admin
    if (userId.equals(ADMIN_ID)) return true
    if (building != null) {
      val cws = (building \ SCHEMA.fCoOwners)
      if (cws.toOption.isDefined) {
        val co_owners = cws.as[List[String]]
        for (co_owner <- co_owners) {
          if (co_owner == userId)
            return true
        }
      }
    }
    false
  }

  private def gzippedJSONOk(json: JsValue, message: String): Result = {
    var tempJson = json.as[JsObject] + ("message" -> JsString(message))
    gzippedJSONOk(tempJson.toString())
  }


  private def gzippedJSONOk(body: String): Result = {
    val gzipv = gzip(body)
    Ok(gzipv.toByteArray).withHeaders(("Content-Encoding", "gzip"),
      ("Content-Length", gzipv.size + ""),
      ("Content-Type", "application/json"))
  }

  private def gzippedOk(body: String): Result = {
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
          LPLogger.info("getAccesHeatmapByBuildingFloor: assert json anyreq")
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody
        LPLogger.info("getAccesHeatmapByBuildingFloor(): " + stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fFloor, SCHEMA.fBuid)
        if (!requiredMissing.isEmpty) {
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        }
        val floor_number = (json \ SCHEMA.fFloor).as[String]
        val buid = (json \ SCHEMA.fBuid).as[String]
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
            if (latlon_predict == null) {

              val crlb_filename = Play.application().configuration().getString("crlbsDir") +
                File.separatorChar + buid + File.separatorChar + "fl_" + floor_number + ".txt"
              val crlb_filename_lock = crlb_filename + ".lock"

              val lockInstant =
                Files.getLastModifiedTime(Paths.get(crlb_filename_lock)).toInstant
              val requestExpired = lockInstant.
                plus(ACCES_RETRY_AMOUNT, ACCES_RETRY_UNIT) isBefore Instant.now
              var msg = ""
              if (requestExpired) {
                // TODO if ACCES generation happens asynchronously we can skip the extra step
                // This is just to show a warning message to the user.
                val file_lock = new File(crlb_filename_lock)
                file_lock.delete()
                msg = "Generating ACCES has previously failed. Please retry."
              } else {
                msg = "Generating ACCES map in another background thread!"
              }

              return AnyResponseHelper.bad_request(msg)
            }

            val res = JsonObject.empty()
            res.put("geojson", JsonObject.fromJson(latlon_predict.toGeoJSON().toString))
            res.put("crlb", JsonArray.from(new util.ArrayList[Double](crlbs.toArray.toList)))
            return AnyResponseHelper.ok(res, "Successfully served ACCES map.")
          }
        } catch {
          case e: FileNotFoundException => return AnyResponseHelper.internal_server_error(
            "Cannot create radiomap:mapping:FNFE:" + e.getMessage)
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
          case e: IOException => return AnyResponseHelper.internal_server_error(
            "Cannot create radiomap:IOE:" + e.getMessage)
          case e: Exception => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
          case _: Throwable => return AnyResponseHelper.internal_server_error("500: ")
        }
      }

      inner(request)
  }

  @deprecated("NotNeeded")
  def maintenance() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        var json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::deleteNotValidDocuments(): " + stripJson(json))
        try {
          if (!ProxyDataSource.getIDatasource.deleteNotValidDocuments()) return AnyResponseHelper.bad_request("None valid documents!")
          return AnyResponseHelper.ok("Success")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }


  private def getAccesMap(rm: RadioMapMean,
                          buid: String, floor_number: String,
                          cut_k_features: Option[Int], h: Double): (GeoJSONMultiPoint, DenseVector[Double]) = {

    // TODO this should be asynchronous. and display warning that it will take time
    // Especially if it runs on radiomap upload

    val folder = new File(
      Play.application().configuration().getString("crlbsDir") +
        File.separatorChar + buid)
    if (!folder.exists()) {
      LPLogger.debug("getAccesMap: mkdir: " + folder.getCanonicalPath)
      folder.mkdirs()
    }

    // REVIEWLS use option for this
    val crlb_filename = Play.application().configuration().getString("crlbsDir") +
      File.separatorChar + buid + File.separatorChar + "fl_" + floor_number + ".txt"

    val crlb_filename_lock = crlb_filename + ".lock"
    LPLogger.debug("getAccesMap:" + crlb_filename)

    val file_path = new File(crlb_filename)
    val file_lock = new File(crlb_filename_lock)

    if (file_lock.exists()) {
      val lockInstant =
        Files.getLastModifiedTime(Paths.get(crlb_filename_lock)).toInstant
      val requestExpired = lockInstant.
        plus(ACCES_RETRY_AMOUNT, ACCES_RETRY_UNIT) isBefore Instant.now
      if (requestExpired) {
        // This is to give user some feedback too..
        LPLogger.info("getAccesMap: Previous request failed and expired." +
          "Will retry on next request.\nFile: " + crlb_filename)
        // lock will be deleted at the callsite of this method
      } else {
        LPLogger.debug("getAccesMap: Ignoring request. Another process is already building: " + crlb_filename)
      }

      return (null, null)
    }

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

    //LPLogger.info("AnyplaceMapping::getAccesHeatmapByBuildingFloor(): multipoint: " + multipoint.toGeoJSON().toString)

    val floors: Array[JsonObject] = toCouchArray(ProxyDataSource.getIDatasource.floorsByBuildingAsJson(buid).iterator().toArray)
    val floor = floors.filter((js: JsonObject) => js.getString(SCHEMA.fFloorNumber) == floor_number)(0)
    val bl = new GeoPoint(lat = floor.getString(SCHEMA.fLatBottomLeft), lon = floor.getString(SCHEMA.fLonBottomLeft))
    val ur = new GeoPoint(lat = floor.getString(SCHEMA.fLatTopRight), lon = floor.getString(SCHEMA.fLonTopRight))
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


    // CLRLS
    //    if (!Files.exists(Paths.get(file_path))) {
    //   acces.fit_gpr(estimate = true, use_default_params = false)
    //    }
    //    LPLogger.debug("fit_gpr: starting")
    //  acces.fit_gpr(estimate = true, use_default_params = false)
    //    LPLogger.debug("fit_gpr: finished")

    //X_min and X_max are bl and ur in XY coordinates
    val X_predict = GeoUtils.grid_2D(bl = X_min, ur = X_max, h = h)

    if (file_path.exists()) {
      val crl = Source.fromFile(file_path).getLines.toArray
      val crlbs = DenseVector.zeros[Double](crl.length)

      // CLRLS
      // acces.fit_gpr(estimate = true, use_default_params = false)
      // LPLogger.debug("crl",crl.length);

      for (k <- 0 until crlbs.length) {
        crlbs(k) = crl(k).toDouble
      }
      val latlon_predict = GeoUtils.dm2GeoJSONMultiPoint(
        GeoUtils.xy2latlng(xy = X_predict, bl = bl, ur = ur))

      return (latlon_predict, crlbs)
    } else {
      file_lock.createNewFile();
      // TODO this should happen in the background.
      LPLogger.info("Generating ACCES: " + crlb_filename)
      acces.fit_gpr(estimate = true, use_default_params = false)

      val crlbs = acces.get_CRLB(X = X_predict, pinv_cond = 1e-6)

      LPLogger.debug("length:" + crlbs.length)
      val acces_file = new PrintWriter(file_path)
      for (i <- 0 until crlbs.length) {
        acces_file.println(crlbs(i))
      }
      acces_file.close()
      file_lock.delete()

      LPLogger.debug("Generated ACCES:" + crlb_filename)
      val latlon_predict = GeoUtils.dm2GeoJSONMultiPoint(
        GeoUtils.xy2latlng(xy = X_predict, bl = bl, ur = ur))

      return (latlon_predict, crlbs)
    }
  }


  private def getRadioMapMeanByBuildingFloor(buid: String, floor_number: String): Option[RadioMapMean] = {
    //FeatureAdd : Configuring location for server generated files
    val radioMapsFrozenDir = Play.application().configuration().getString("radioMapFrozenDir")
    val rmapDir = new File(radioMapsFrozenDir + File.separatorChar + buid + File.separatorChar + floor_number)
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
    LPLogger.debug(radio.toPath().getFileName.toString)
    var floorFetched: Long = 0l
    floorFetched = ProxyDataSource.getIDatasource.dumpRssLogEntriesByBuildingACCESFloor(fout, buid, floor_number)
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
    // BUG CHECK this
    val resCreate = rm.createRadioMap()
    if (resCreate != null) {
      throw new Exception("getRadioMapMeanByBuildingFloor: Error: on-the-fly radioMap: " + resCreate)
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
  //    LPLogger.debug(radio.toPath().getFileName.toString)
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
