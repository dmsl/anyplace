/*
 * Anyplace: A free and open Indoor Navigation Service with superb accuracy!
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
 * Copyright (c) 2021, Data Management Systems Lab (DMSL), University of Cyprus.
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

import java.io.{IOException, _}
import java.text.{NumberFormat, ParseException}
import java.time.temporal.{ChronoUnit, TemporalUnit}
import java.util
import java.util.Locale
import datasources.{DatasourceException, MongodbDatasource, ProxyDataSource, SCHEMA}

import javax.inject.{Inject, Singleton}
import json.VALIDATE
import json.VALIDATE.{String, floor}
import location.Algorithms
import models._
import modules.radiomapserver.RadioMap.RadioMap
import modules.radiomapserver.RadioMapMean
import org.mongodb.scala.MongoDatabase
import org.mongodb.scala.model.Filters.equal
import play.api.libs.json.Reads._
import play.api.libs.json.{JsObject, JsValue, Json, _}
import play.api.mvc._
import play.api.{Configuration, Environment}
import play.twirl.api.TwirlHelperImports.twirlJavaCollectionToScala
import utils.Utils.appendGoogleIdIfNeeded
import utils.{JsonUtils, LOG, RESPONSE, _}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration
import scala.jdk.CollectionConverters._
import scala.util.control.Breaks

// TODO:PM continue reviewing this file

@Singleton
class MappingController @Inject()(cc: ControllerComponents,
                                  env: Environment,
                                  conf: Configuration,
                                  api: AnyplaceServerAPI,
                                  mapHelper: helper.Mapping,
                                  fu: FileUtils,
                                  tilerHelper: AnyPlaceTilerHelper,
                                  mongoDB: MongodbDatasource,
                                  pds: ProxyDataSource,
                                  user: helper.User)
  extends AbstractController(cc) {

  import models.oauth.OAuth2Request

  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  // TODO:NN replace with user.isModerator(): will return true if user is mod or admin
  private val ADMIN_ID = "112997031510415584062_google"
  val ACCES_RETRY_AMOUNT = 2
  val ACCES_RETRY_UNIT: TemporalUnit = ChronoUnit.HOURS
  val NEARBY_BUILDINGS_RANGE = 50
  val NEARBY_BUILDINGS_RANGE_MAX = 500


  def getRadioHeatmap() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        LOG.D2("getRadioHeatmap: " + Utils.stripJson(anyReq.getJsonBody()))
        try {
          val radioPoints = pds.db.getRadioHeatmap()
          if (radioPoints == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          val res = Json.obj("radioPoints" -> radioPoints.asScala)
          return RESPONSE.OK(res, "Successfully retrieved radio points.")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }
      inner(request)
  }

  def getHeatmapByFloorAVG1() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("getHeatmapByFloorAVG1: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid, SCHEMA.fFloor)
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floor = (json \ SCHEMA.fFloor).as[String]
        try {
          val radioPoints = pds.db.getRadioHeatmapByBuildingFloorAverage1(buid, floor)
          if (radioPoints == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          val res: JsValue = Json.obj("radioPoints" -> radioPoints)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case ioe: IOException => return RESPONSE.OK(res, "Successfully retrieved radio points.")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }


  def getHeatmapByFloorAVG2() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("getRadioHeatmapRSS2: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fFloor, SCHEMA.fBuid)
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floor = (json \ SCHEMA.fFloor).as[String]
        try {
          val radioPoints = pds.db.getRadioHeatmapByBuildingFloorAverage2(buid, floor)
          if (radioPoints == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          val res: JsValue = Json.obj("radioPoints" -> radioPoints)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case ioe: IOException => return RESPONSE.OK(res, "Successfully retrieved radio points.")
          }
        } catch {
          case e: Exception => return RESPONSE.ERROR("getRadioHeatmapByBuildingFloorAverage2: ", e)
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
  def getHeatmapByFloorAVG3() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("getHeatmapByFloorAVG3: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid, SCHEMA.fFloor)
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floor = (json \ SCHEMA.fFloor).as[String]
        try {
          val radioPoints = pds.db.getRadioHeatmapByBuildingFloorAverage3(buid, floor)
          if (radioPoints == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          val res: JsValue = Json.obj("radioPoints" -> radioPoints)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case ioe: IOException => return RESPONSE.OK(res, "Successfully retrieved radio points.")
          }
        } catch {
          case e: Exception => return RESPONSE.ERROR("getRadioHeatmapByBuildingFloorAverage3: ", e)
        }
      }

      inner(request)
  }

  /**
   * Reads from level3 (all fingerprints), clusters them into tiles and return them.
   *
   * @return
   */
  def getHeatmapByFloorAVG3Tiles() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("getHeatmapByFloorAVG3Tiles: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid, SCHEMA.fFloor, SCHEMA.fX, SCHEMA.fY, "z")
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floor = (json \ SCHEMA.fFloor).as[String]
        val tileX = (json \ SCHEMA.fX).as[Int]
        val tileY = (json \ SCHEMA.fY).as[Int]
        val zoomLevel = (json \ "z").as[Int]
        try {
          val radioPoints = pds.db.getRadioHeatmapByBuildingFloorAverage3(buid, floor)
          if (radioPoints == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE

          val radioPointsInXY: util.ArrayList[JsValue] = new util.ArrayList[JsValue]()
          // assigns fingerprints to map tiles because its overkill to load everything at once.
          for (radioPoint <- radioPoints) {
            val lat = (radioPoint \ "x").as[Double]
            val lon = (radioPoint \ "y").as[Double]
            val xyConverter = convertToXY(lat, lon, zoomLevel)
            LOG.D2(xyConverter(0).toString + " " + xyConverter(1).toString)
            if (xyConverter(0) == tileX && xyConverter(1) == tileY) {
              radioPointsInXY.add(radioPoint)
            }
          }
          val res = Json.obj("radioPoints" -> radioPointsInXY.asScala)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case ioe: IOException => return RESPONSE.OK(res, "Successfully retrieved radio points.")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  /**
   * Called by crossfilter when on zoom level 21.
   */
  def heatmapByFloorTimestampAVG3() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("heatmapByFloorTimestampAVG3: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fFloor, SCHEMA.fBuid, SCHEMA.fTimestampX, SCHEMA.fTimestampY)
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floor = (json \ SCHEMA.fFloor).as[String]
        val timestampX = (json \ SCHEMA.fTimestampX).as[String]
        val timestampY = (json \ SCHEMA.fTimestampY).as[String]

        try {
          val radioPoints = pds.db.getRadioHeatmapByFloorTimestamp(buid, floor, timestampX, timestampY)
          if (radioPoints == null) return RESPONSE.BAD_CANNOT_RETRIEVE_FINGERPRINTS_WIFI
          val res: JsValue = Json.obj("radioPoints" -> radioPoints)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case ioe: IOException => return RESPONSE.OK(res, "Successfully retrieved radio points.")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR("heatmapByFloorTimestampAVG3", e)
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

  /**
   * Called by crossfilter when on maximum zoom level (22).
   * Called many times from clients, for each tile.
   */
  def heatmapByFloorTimestampTiles() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D3("heatmapByFloorTimestampTiles: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid, SCHEMA.fFloor, SCHEMA.fTimestampX,
          SCHEMA.fTimestampY, SCHEMA.fX, SCHEMA.fY, "z")
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floor = (json \ SCHEMA.fFloor).as[String]
        val timestampX = (json \ SCHEMA.fTimestampX).as[String]
        val timestampY = (json \ SCHEMA.fTimestampY).as[String]
        val x = (json \ SCHEMA.fX).as[Int]
        val y = (json \ SCHEMA.fY).as[Int]
        val z = (json \ "z").as[Int]

        try {
          val radioPoints = pds.db.getRadioHeatmapByFloorTimestamp(buid, floor, timestampX, timestampY)
          if (radioPoints == null) return RESPONSE.BAD_CANNOT_RETRIEVE_FINGERPRINTS_WIFI
          val radioPointsInXY: util.ArrayList[JsValue] = new util.ArrayList[JsValue]()

          for (radioPoint <- radioPoints) {
            val radioX = (radioPoint \ "x").as[Double]
            val radioY = (radioPoint \ "y").as[Double]
            val xyConverter = convertToXY(radioX, radioY, z)
            if (xyConverter(0) == x && xyConverter(1) == y)
              radioPointsInXY.add(radioPoint)
          }

          val res: JsValue = Json.obj("radioPoints" -> radioPointsInXY.asScala)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case _: IOException => return RESPONSE.OK(res, "Successfully retrieved radio points.")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def heatmapByFloorTimestampAVG1() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("heatmapByFloorTimestampAVG1: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid, SCHEMA.fFloor, SCHEMA.fTimestampX, SCHEMA.fTimestampY)
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floor = (json \ SCHEMA.fFloor).as[String]
        val timestampX = (json \ SCHEMA.fTimestampX).as[String]
        val timestampY = (json \ SCHEMA.fTimestampY).as[String]
        try {
          val radioPoints = pds.db.getRadioHeatmapByBuildingFloorTimestampAverage1(buid, floor, timestampX, timestampY)
          if (radioPoints == null) return RESPONSE.BAD_CANNOT_RETRIEVE_FINGERPRINTS_WIFI
          val res: JsValue = Json.obj("radioPoints" -> radioPoints)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case _: IOException => return RESPONSE.OK(res, "Successfully retrieved radio points.")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def heatmapByFloorTimestampAVG2() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("heatmapByFloorTimestampAVG2: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid, SCHEMA.fFloor, SCHEMA.fTimestampX, SCHEMA.fTimestampY)
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floor = (json \ SCHEMA.fFloor).as[String]
        val timestampX = (json \ SCHEMA.fTimestampX).as[String]
        val timestampY = (json \ SCHEMA.fTimestampY).as[String]
        try {
          val radioPoints = pds.db.getRadioHeatmapByBuildingFloorTimestampAverage2(buid, floor, timestampX, timestampY)
          if (radioPoints == null) return RESPONSE.BAD_CANNOT_RETRIEVE_FINGERPRINTS_WIFI
          val res: JsValue = Json.obj("radioPoints" -> radioPoints)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case ioe: IOException => return RESPONSE.OK(res, "Successfully retrieved all radio points.")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def deletePrecomputed(): Unit = {
    // TODO:NN what is the method that does this now? We might have to rename it.
    // TODO: delete accessPointsWifi: buid, floor
  }

  def getAPsByBuildingFloor() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("getAPsByBuildingFloor: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid, SCHEMA.fFloor)
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floor = (json \ SCHEMA.fFloor).as[String]
        val APs = pds.db.getCachedAPsByBuildingFloor(buid, floor)
        // if cached return it
        if (APs != null) {
          val res = Json.obj("accessPoints" -> (APs \ "accessPoints").as[List[JsValue]])
          return RESPONSE.gzipJsonOk(res, "Fetched precompute of accessPointsWifi")
        } else {
          try {
            val accessPoints = pds.db.getAPsByBuildingFloor(buid, floor)
            LOG.D3("mdb " + accessPoints.size)
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

            if (accessPoints == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
            val newAccessPoint = Json.obj(SCHEMA.fBuid -> buid, SCHEMA.fFloor -> floor, "accessPoints" -> uniqueAPs.values().asScala)
            pds.db.addJson(SCHEMA.cAccessPointsWifi, newAccessPoint.toString())
            val res: JsValue = Json.obj("accessPoints" -> new util.ArrayList[JsValue](uniqueAPs.values()).asScala)
            try {
              RESPONSE.gzipJsonOk(res, "Generated precompute of accessPointsWifi")
            } catch {
              case ioe: IOException => return RESPONSE.OK(res, "Successfully retrieved all radio points.")
            }
          } catch {
            case e: Exception => return RESPONSE.ERROR("getAPsByBuildingFloor: ", e)
          }
        }
      }

      inner(request)
  }

  /**
   * @return
   */
  def getAPsIds() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        val accessPointsOfReq = (json \ "ids").as[List[String]]
        try {
          val reqFile = "public/anyplace_architect/ids.json"
          val file = env.classLoader.getResourceAsStream(reqFile)
          var accessPointsOfFile: List[JsObject] = null
          if (file != null) {
            accessPointsOfFile = Json.parse(file).as[List[JsObject]]
          } else {
            return RESPONSE.NOT_FOUND(reqFile)
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
                        inner_loop.break()
                      }
                    }
                  }
                  if (sameBits >= 3) found = true
                } else {
                  sameBits = 0
                  if (firstBitFound) {
                    firstBitFound = false
                    loop.break()
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

          if (accessPointsOfReq == null) {
            return RESPONSE.BAD("Access Points do not exist or could not be retrieved.")
          }
          val res: JsValue = Json.obj("accessPoints" -> APsIDs.toList)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case ioe: IOException => return RESPONSE.OK(res, "Successfully retrieved IDs for Access Points.")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  /**
   * Delete fingeprints within a bounding-box. Also delete heatmap caches.
   *
   * @return deleted fingerprints (so JS update UI)
   */
  def FingerPrintsDelete() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("FingerPrintsDelete: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(
          json, SCHEMA.fBuid, SCHEMA.fFloor, "lat1", "lon1", "lat2", "lon2")
        if (checkRequirements != null) return checkRequirements
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floorNum = (json \ SCHEMA.fFloor).as[String]
        val lat1 = (json \ "lat1").as[String]
        val lon1 = (json \ "lon1").as[String]
        val lat2 = (json \ "lat2").as[String]
        val lon2 = (json \ "lon2").as[String]
        try {
          val fingerprints: List[JsValue] = pds.db.getFingerPrintsBBox(
            buid, floorNum, lat1, lon1, lat2, lon2)
          if (fingerprints.isEmpty)
            return RESPONSE.BAD_CANNOT_RETRIEVE_FINGERPRINTS_WIFI

          LOG.D2("FingerPrintsDelete: will delete " + fingerprints.size + " fingerprints.")
          for (fingerprint <- fingerprints) {
            pds.db.deleteFingerprint(fingerprint)
          }
          pds.db.deleteAffectedHeatmaps(buid,floorNum)
          val res: JsValue = Json.obj("fingerprints" -> fingerprints)
          Future { mapHelper.updateFrozenRadioMap(buid, floorNum) }(ec)
          return RESPONSE.gzipJsonOk(res, "Deleted " + fingerprints.size + " fingerprints and returning them.")
        } catch {
          case e: Exception =>
            return RESPONSE.ERROR_INTERNAL("FingerPrintsDelete: " + e.getClass + ": " + e.getMessage)
        }
      }

      inner(request)
  }

  def FingerPrintsTimestampDelete() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("FingerPrintsTimestampDelete: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(
          json, SCHEMA.fBuid, SCHEMA.fFloor, "lat1", "lon1", "lat2", "lon2", SCHEMA.fTimestampX, SCHEMA.fTimestampY)
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floorNum = (json \ SCHEMA.fFloor).as[String]
        val lat1 = (json \ "lat1").as[String]
        val lon1 = (json \ "lon1").as[String]
        val lat2 = (json \ "lat2").as[String]
        val lon2 = (json \ "lon2").as[String]
        val timestampX = (json \ SCHEMA.fTimestampX).as[String]
        val timestampY = (json \ SCHEMA.fTimestampY).as[String]
        try {
          val fingerprints: List[JsValue] = pds.db.getFingerPrintsTimestampBBox(buid, floorNum, lat1, lon1, lat2, lon2, timestampX, timestampY)
          if (fingerprints.isEmpty)
            return RESPONSE.BAD_CANNOT_RETRIEVE_FINGERPRINTS_WIFI
          for (fingerprint <- fingerprints)
            pds.db.deleteFingerprint(fingerprint)
          pds.db.deleteAffectedHeatmaps(buid,floorNum)
          // TODO:NN below comment?
          // TODO:do also 1 and 2
          pds.db.createTimestampHeatmap(SCHEMA.cHeatmapWifiTimestamp3, buid, floorNum, 3)
          val res: JsValue = Json.obj("radioPoints" -> fingerprints)
          try {
            Future { mapHelper.updateFrozenRadioMap(buid, floorNum) }(ec)
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case ioe: IOException =>
              return RESPONSE.OK(res, "Successfully retrieved all Fingerprints!")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  /**
   * Called when "Show Fingerprints By Time" (Architect: toggleFingerPrintsTime) is clicked.
   * Used to return the data that will be shown in the crossfilter bar.
   *
   * @return a list of the number of fingerprints stored, and date.
   */
  def FingerprintsByTime() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("FingerprintsByTime: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid, SCHEMA.fFloor)
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floorNum = (json \ SCHEMA.fFloor).as[String]

        // create cache-collections
        pds.db.createTimestampHeatmap(SCHEMA.cHeatmapWifiTimestamp1, buid, floorNum, 1)
        pds.db.createTimestampHeatmap(SCHEMA.cHeatmapWifiTimestamp2, buid, floorNum, 2)
        pds.db.createTimestampHeatmap(SCHEMA.cHeatmapWifiTimestamp3, buid, floorNum, 3)

        try {
          val radioPoints: List[JsValue] = pds.db.getFingerprintsByTime(buid, floorNum)
          if (radioPoints.isEmpty) return RESPONSE.BAD("Fingerprints do not exist.")
          val res: JsValue = Json.obj("radioPoints" -> radioPoints)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case ioe: IOException =>
              return RESPONSE.OK(res, "Successfully retrieved all Fingerprints.")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }
      inner(request)
  }

  def findPosition() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("findPosition: " + Utils.stripJson(json))
        // CHECK:NN
        //val requiredMissing = JsonUtils.requirePropertiesInJson(json, SCHEMA.fBuid, SCHEMA.fFloor,"APs","algorithm_choice")
        // LPLogger.debug("json: "+json)
        //if (!requiredMissing.isEmpty)
        //  return An/yResponseHelper.requiredFieldsMissing(requiredMissing)

        val buid = (json \ SCHEMA.fBuid).as[String]
        val floorNum = (json \ SCHEMA.fFloor).as[String]

        /*
         * BuxFix : Server side localization API
         * Fixing JSON Parse error
         */
        val accessOpt = Json.parse((json \ "APs").as[String]).validate[List[JsValue]] match {
          case s: JsSuccess[List[JsValue]] => {
            Some(s.get)
          }
          case e: JsError =>
            LOG.E("accessOpt Errors: " + JsError.toJson(e).toString())
            None
        }
        val accessPoints = accessOpt.get

        /*
         * BuxFix : Server side localization API
         * Fixing JSON Parse error [String vs Int]
         */
        val algorithm_choice: Int = (json \ "algorithm_choice").validate[String] match {
          case s: JsSuccess[String] => {
            if (s.get != null && s.get.trim != "") {
              Integer.parseInt(s.get)
            } else {
              conf.get[Int]("defaultPositionAlgorithm")
            }
          }
          case _: JsError => conf.get[Int]("defaultPositionAlgorithm")
        }

        val radioMapsFrozenDir = conf.get[String]("radioMapFrozenDir")
        /* CHECK:NN
         * REVIEWLS . Leaving bugfix from develop
            val floorNum = (json \ SCHEMA.fFloor).as[String]
            val jsonstr=(json\"APs").as[String]
            val accessPoints= Json.parse(jsonstr).as[List[JsValue]]
            val floors: Array[JsonObject] = pds.getIDatasource.floorsByBuildingAsJson(buid).iterator().toArray
            val algorithm_choice = (json\"algorithm_choice").as[String].toInt
            */

        val rmapFile = new File(radioMapsFrozenDir + api.sep + buid + api.sep +
          floorNum + api.sep + "indoor-radiomap-mean.txt")

        if (!rmapFile.exists()) {  // Regenerate the radiomap files
          mapHelper.updateFrozenRadioMap(buid, floorNum)
        }

        /*
         * BuxFix : Server side localization API
         * Fixing null pointer error for latestScanList
         */
        val latestScanList: util.ArrayList[location.LogRecord] = new util.ArrayList[location.LogRecord]()

        /* CHECK:NN ..
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

        if (response == null) { response = "0 0" }
        val lat_long = response.split(" ")
        val res = Json.obj("lat" -> lat_long(0), "long" -> lat_long(1))
        return RESPONSE.OK(res, "Successfully found position.")
      }

      inner(request)
  }

  // TODO:NN  convert to jsvalue so it works????? or delete
  def getRadioHeatmapBbox = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("getRadioHeatmapBbox: " + Utils.stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fCoordinatesLat, SCHEMA.fCoordinatesLon, SCHEMA.fFloor, SCHEMA.fBuid, "range")
        if (!requiredMissing.isEmpty)
          return RESPONSE.MISSING_FIELDS(requiredMissing)
        val lat = (json \ SCHEMA.fCoordinatesLat).as[String]
        val lon = (json \ SCHEMA.fCoordinatesLon).as[String]
        val floorNum = (json \ SCHEMA.fFloor).as[String]
        val buid = (json \ SCHEMA.fBuid).as[String]
        val strRange = (json \ "range").as[String]
        val weight = (json \ SCHEMA.fWeight).as[String]
        val range = strRange.toInt
        try {
          var radioPoints: util.List[JsValue] = null
          if (weight.compareTo("false") == 0) radioPoints = pds.db.getRadioHeatmapBBox2(lat, lon, buid, floorNum, range)
          else if (weight.compareTo("true") == 0) radioPoints = pds.db.getRadioHeatmapBBox(lat, lon, buid, floorNum, range)
          else if (weight.compareTo("no spatial") == 0) radioPoints = pds.db.getRadioHeatmapByBuildingFloor2(lat, lon, buid, floorNum, range)
          if (radioPoints == null)
            return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          val res = Json.obj("radioPoints" -> radioPoints.asScala)
          // CHECK: NN comments below?
          try //                if (request().getHeader("Accept-Encoding") != null && request().getHeader("Accept-Encoding").contains("gzip")) {
            RESPONSE.gzipJsonOk(res.toString)
            //                }
            //                return AnyResponseHelper.ok(res.toString());
          catch {
            case ioe: IOException =>
              return RESPONSE.OK(res, "Successfully retrieved radio points.")
          }
        } catch {
          case e: DatasourceException =>
            return RESPONSE.ERROR(e)
        }
      }
      inner(request)
  }

  def deleteRadiosInBox() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("deleteRadiosInBox: " + Utils.stripJson(json))
        try {
          if (!pds.db.deleteRadiosInBox()) {
            return RESPONSE.BAD_CANNOT_ADD_SPACE
          }
          return RESPONSE.OK("Success")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def spaceAdd() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("spaceAdd: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fIsPublished, SCHEMA.fName, SCHEMA.fDescription,
          SCHEMA.fURL, SCHEMA.fAddress, SCHEMA.fCoordinatesLat, SCHEMA.fCoordinatesLon, SCHEMA.fSpaceType)
        if (checkRequirements != null) return checkRequirements
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        try {
          var space: Space = null
          try {
            json = json.as[JsObject] - SCHEMA.fAccessToken
            space = new Space(json)
          } catch {
            case e: NumberFormatException => return RESPONSE.BAD("Space coordinates are invalid.")
          }
          if (!pds.db.addJson(SCHEMA.cSpaces, space.toGeoJSON())) {
            return RESPONSE.BAD_CANNOT_ADD_SPACE
          }
          val res: JsValue = Json.obj(SCHEMA.fBuid -> space.getId())
          return RESPONSE.OK(res, "Successfully added space.")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  // CHECK:NN is this in use by JS? or needed?
  def spaceUpdateCoOwners() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("spaceUpdateCoOwners: " + Utils.stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fCoOwners)
        if (!requiredMissing.isEmpty) return RESPONSE.MISSING_FIELDS(requiredMissing)
        var owner_id = user.authorize(apiKey)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val validation = VALIDATE.fields(json, SCHEMA.fBuid)
        if (validation.failed()) return validation.response()

        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val storedSpace: JsValue = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          if (!user.hasAccess(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
          val space = new Space(storedSpace)
          if (!pds.db.replaceJsonDocument(SCHEMA.cSpaces, SCHEMA.fBuid, space.getId(), space.appendCoOwners(json)))
            return RESPONSE.BAD("Space could not be updated.")

          return RESPONSE.OK("Successfully updated space")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def spaceUpdateOwner() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("spaceUpdateOwner: " + Utils.stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, "new_owner")
        if (!requiredMissing.isEmpty) return RESPONSE.MISSING_FIELDS(requiredMissing)
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val validation = VALIDATE.fields(json, SCHEMA.fBuid, "new_owner")
        if (validation.failed()) return validation.response()

        val buid = (json \ SCHEMA.fBuid).as[String]
        var newOwner = (json \ "new_owner").as[String]
        newOwner = appendGoogleIdIfNeeded(newOwner)
        try {
          val storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          if (!user.hasAccess(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
          val space = new Space(storedSpace)
          if (!pds.db.replaceJsonDocument(SCHEMA.cSpaces, SCHEMA.fBuid, space.getId(), space.changeOwner(newOwner))) return RESPONSE.BAD("Space could not be updated!")
          return RESPONSE.OK("Successfully updated space!")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def spaceUpdate() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("spaceUpdate: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid)
        if (checkRequirements != null) return checkRequirements
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          var storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          if (!user.hasAccess(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
          if (json.\(SCHEMA.fIsPublished).getOrElse(null) != null) {
            val is_published = (json \ SCHEMA.fIsPublished).as[String]
            if (is_published == "true" || is_published == "false")
              storedSpace = storedSpace.as[JsObject] + (SCHEMA.fIsPublished -> JsString((json \ SCHEMA.fIsPublished).as[String]))
          }
          if (json.\(SCHEMA.fName).getOrElse(null) != null)
            storedSpace = storedSpace.as[JsObject] + (SCHEMA.fName -> JsString((json \ SCHEMA.fName).as[String]))
          if (json.\(SCHEMA.fBuCode).getOrElse(null) != null)
            storedSpace = storedSpace.as[JsObject] + (SCHEMA.fBuCode -> JsString((json \ SCHEMA.fBuCode).as[String]))
          if (json.\(SCHEMA.fDescription).getOrElse(null) != null)
            storedSpace = storedSpace.as[JsObject] + (SCHEMA.fDescription -> JsString((json \ SCHEMA.fDescription).as[String]))
          if (json.\(SCHEMA.fURL).getOrElse(null) != null)
            storedSpace = storedSpace.as[JsObject] + (SCHEMA.fURL -> JsString((json \ SCHEMA.fURL).as[String]))
          if (json.\(SCHEMA.fAddress).getOrElse(null) != null)
            storedSpace = storedSpace.as[JsObject] + (SCHEMA.fAddress -> JsString((json \ SCHEMA.fAddress).as[String]))
          if (json.\(SCHEMA.fCoordinatesLat).getOrElse(null) != null)
            storedSpace = storedSpace.as[JsObject] + (SCHEMA.fCoordinatesLat -> JsString((json \ SCHEMA.fCoordinatesLat).as[String]))
          if (json.\(SCHEMA.fCoordinatesLon).getOrElse(null) != null)
            storedSpace = storedSpace.as[JsObject] + (SCHEMA.fCoordinatesLon -> JsString((json \ SCHEMA.fCoordinatesLon).as[String]))
          if (json.\(SCHEMA.fSpaceType).getOrElse(null) != null) {
            val spaceType = (json \ SCHEMA.fSpaceType).as[String]
            if (SCHEMA.fSpaceTypes.contains(spaceType))
              storedSpace = storedSpace.as[JsObject] + (SCHEMA.fSpaceType -> JsString(spaceType))
          }
          val space = new Space(storedSpace)
          if (!pds.db.replaceJsonDocument(SCHEMA.cSpaces, SCHEMA.fBuid, space.getId(), space.toGeoJSON())) return RESPONSE.BAD("Space could not be updated!")
          return RESPONSE.OK("Successfully updated space!")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def spaceDelete() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("spaceDelete: " + json)
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid)
        if (checkRequirements != null) return checkRequirements
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        if (String(json, SCHEMA.fBuid) == null)
          return RESPONSE.BAD("Buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          if (!user.hasAccess(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        try {
          val deleted = pds.db.deleteAllByBuilding(buid)
          if (deleted == false)
            return RESPONSE.BAD("Some items related to the deleted space could not be deleted.")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        val filePath = tilerHelper.getRootFloorPlansDirFor(buid)
        try {
          val buidfile = new File(filePath)
          if (buidfile.exists()) HelperMethods.recDeleteDirFile(buidfile)
        } catch {
          case e: IOException => return RESPONSE.ERROR_INTERNAL("500: " + e.getMessage + "] while deleting floorplans." +
            "\nAll related information is deleted from the database!")
        }
        return RESPONSE.OK("Successfully deleted everything related to space!")
      }

      inner(request)
  }

  def spaceAll = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("spaceAll: " + Utils.stripJson(json))
        try {
          val spaces = pds.db.getAllBuildings()
          val res: JsValue = Json.obj(SCHEMA.cSpaces -> spaces)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          }
          catch {
            case ioe: IOException => return RESPONSE.OK(res, "Successfully retrieved all spaces!")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def spaceGetOne() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("spaceGetOne: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid)
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          var space = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (space != null && (space \ SCHEMA.fBuid) != JsDefined(JsNull) &&
            (space \ SCHEMA.fCoordinatesLat) != JsDefined(JsNull) &&
            (space \ SCHEMA.fCoordinatesLon) != JsDefined(JsNull) &&
            (space \ SCHEMA.fOwnerId) != JsDefined(JsNull) &&
            (space \ SCHEMA.fName) != JsDefined(JsNull) &&
            (space \ SCHEMA.fDescription) != JsDefined(JsNull)) {
            space = space.as[JsObject] - SCHEMA.fOwnerId - SCHEMA.fCoOwners - SCHEMA.fId - SCHEMA.fSchema
            val res: JsValue = Json.obj("space" -> space)
            try {
              return RESPONSE.gzipJsonOk(res.toString)
            } catch {
              case ioe: IOException => return RESPONSE.OK(res, "Successfully retrieved the space!")
            }
          }
          return RESPONSE.NOT_FOUND("Space not found.")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def spaceAllByOwner() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()

        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("spaceAllByOwner: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json) // , SCHEMA.fAccessToken
        if (checkRequirements != null) return checkRequirements

        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        try {
          LOG.D3("owner_id = " + owner_id)
          val spaces = pds.db.getAllBuildingsByOwner(owner_id)
          val res: JsValue = Json.obj("spaces" -> spaces)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case ioe: IOException => return RESPONSE.OK(res, "Successfully retrieved all spaces!")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def spaceOwned() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()

        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("spaceOwned: " + Utils.stripJson(json))
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        try {
          val spaces = pds.db.getAllSpaceOwned(owner_id)
          val res: JsValue = Json.obj("spaces" -> spaces)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case ioe: IOException => return RESPONSE.OK(res, "Successfully retrieved all spaces!")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def spaceByBucode() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("spaceByBucode: " + Utils.stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuCode)
        if (!requiredMissing.isEmpty) return RESPONSE.MISSING_FIELDS(requiredMissing)
        val bucode = (json \ SCHEMA.fBuCode).as[String]
        try {
          val spaces = pds.db.getAllBuildingsByBucode(bucode)
          val res: JsValue = Json.obj(SCHEMA.cSpaces -> spaces)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case ioe: IOException => return RESPONSE.OK(res, "Successfully retrieved all spaces!")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def spaceCoordinates() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("spaceCoordinates: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fCoordinatesLat, SCHEMA.fCoordinatesLon)
        if (checkRequirements != null) return checkRequirements
        var range = NEARBY_BUILDINGS_RANGE
        if (JsonUtils.hasProperty(json, "range")) {
          if ((json \ "range").validate[Int].isError) {
            return RESPONSE.BAD("range must be a possitive integer")
          }
          range = (json \ "range").as[Int]
          if (range <= 0) {
            return RESPONSE.BAD("range must be a possitive integer")
          }
          if (range > NEARBY_BUILDINGS_RANGE_MAX) {
            range = NEARBY_BUILDINGS_RANGE_MAX
            LOG.W("spaceCoordinates: maximum range exceeded. Using " + range)
          }
        }

        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        try {
          val lat = java.lang.Double.parseDouble((json \ SCHEMA.fCoordinatesLat).as[String])
          val lon = java.lang.Double.parseDouble((json \ SCHEMA.fCoordinatesLon).as[String])
          val spaces = pds.db.getAllBuildingsNearMe(lat, lon, range, owner_id)
          val res: JsValue = Json.obj(SCHEMA.cSpaces -> spaces)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case ioe: IOException => return RESPONSE.OK(res, "Successfully retrieved all spaces near your position!")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }


  /**
   * Retrieve the Space Set.
   *
   * @return
   */
  def buildingSetAll = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("buildingSetAll: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fCampusCuid)
        if (checkRequirements != null) return checkRequirements
        val cuid = (json \ SCHEMA.fCampusCuid).as[String]
        try {
          val campus = pds.db.getBuildingSet(cuid)
          if (campus.size == 0) {
            return RESPONSE.NOT_FOUND("Campus '" + cuid + "' not found!")
          } else if (campus.size > 1) {
            return RESPONSE.NOT_FOUND("Something went wrong. More than one matches for '" + cuid + "'!")
          }

          val buids = new util.ArrayList[String]
          for (c <- campus) {
            val cBuildings = (c \ SCHEMA.fBuids).as[List[String]]
            for (cb <- cBuildings) {
              buids.add(cb)
            }
          }

          val spaces = new util.ArrayList[JsValue]
          for (b <- buids.asScala) {
            val space = pds.db.getFromKey(SCHEMA.cSpaces, SCHEMA.fBuid, b)
            if (space != null) // some spaces are deleted but still exist in buids[] of a campus
              spaces.add(space.as[JsObject] - SCHEMA.fId - SCHEMA.fSchema - SCHEMA.fCoOwners - SCHEMA.fGeometry - SCHEMA.fType - SCHEMA.fOwnerId)
          }

          val res = campus(0).as[JsObject] - SCHEMA.fBuids - SCHEMA.fOwnerId - SCHEMA.fId - SCHEMA.fSchema - SCHEMA.fCampusCuid - SCHEMA.fDescription +
            (SCHEMA.cSpaces -> Json.toJson(spaces.asScala))
          try
            RESPONSE.gzipJsonOk(res.toString)
          catch {
            case ioe: IOException => RESPONSE.OK(res, "Successfully retrieved spaceSets")
          }
        } catch {
          case e: DatasourceException => RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  /**
   * Adds a new Space set to the database
   *
   * @return the newly created Space ID is included in the response if success
   */
  def spaceSetAdd = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("spaceSetAdd: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fDescription, SCHEMA.fName, SCHEMA.fBuids, SCHEMA.fGreeklish)
        if (checkRequirements != null) return checkRequirements
        var owner_id = user.authorize(apiKey)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id)) - SCHEMA.fAccessToken
        try {
          val cuid = (json \ SCHEMA.fCampusCuid).as[String]
          val campus = pds.db.BuildingSetsCuids(cuid)
          if (campus) return RESPONSE.BAD("Space set already exists.")
          else {
            var buildingset: BuildingSet = null
            try {
              buildingset = new BuildingSet(json)
            } catch {
              case e: NumberFormatException =>
                return RESPONSE.BAD("Space coordinates are invalid.")
            }
            if (!pds.db.addJson(SCHEMA.cCampuses, buildingset.addBuids()))
              return RESPONSE.BAD_CANNOT_ADD_SPACE
            val res: JsValue = Json.obj(SCHEMA.fCampusCuid -> buildingset.getId())
            return RESPONSE.OK(res, "Successfully added Space Set.")
          }
        } catch {
          case e: DatasourceException =>
            return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  /**
   * Update the Space information. Space to update is specified by buid
   *
   * @return
   */
  def campusUpdate = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("campusUpdate: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fCampusCuid)
        if (checkRequirements != null) return checkRequirements
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val cuid = (json \ SCHEMA.fCampusCuid).as[String]
        try {
          var storedCampus = pds.db.getFromKeyAsJson(SCHEMA.cCampuses, SCHEMA.fCampusCuid, cuid)
          if (storedCampus == null)
            return RESPONSE.BAD_CANNOT_RETRIEVE_CAMPUS
          if (!isCampusOwner(storedCampus, owner_id))
            return RESPONSE.UNAUTHORIZED("Unauthorized")
          // check for values to update
          if ((json \ SCHEMA.fName).toOption.isDefined) {
            val temp = (json \ SCHEMA.fName).as[String]
            if (temp != "-" && temp != "") {
              storedCampus = storedCampus.as[JsObject] + (SCHEMA.fName -> JsString(temp))
            } else {
              storedCampus = storedCampus.as[JsObject] - SCHEMA.fName
            }
          }
          if ((json \ SCHEMA.fDescription).toOption.isDefined) {
            val temp = (json \ SCHEMA.fDescription).as[String]
            if (temp != "-" && temp != "") {
              storedCampus = storedCampus.as[JsObject] + (SCHEMA.fDescription, JsString(temp))
            } else
              storedCampus = storedCampus.as[JsObject] - SCHEMA.fDescription
          }
          if ((json \ "cuidnew").toOption.isDefined) {
            val temp = (json \ SCHEMA.fCampusCuid).as[String]
            if (temp != "-" && temp != "")
              storedCampus = storedCampus.as[JsObject] + (SCHEMA.fCampusCuid, JsString(temp))
          }
          if ((json \ SCHEMA.fGreeklish).toOption.isDefined) {
            val temp = (json \ SCHEMA.fGreeklish).as[Boolean]
            storedCampus = storedCampus.as[JsObject] + (SCHEMA.fGreeklish -> JsString(temp.toString))
          }
          if ((json \ SCHEMA.fBuids).toOption.isDefined) {
            LOG.D2(json.toString())
            var buids = (json \ SCHEMA.fBuids).as[List[String]]
            storedCampus = storedCampus.as[JsObject] + (SCHEMA.fBuids -> Json.toJson(buids))
          }
          val campus = new BuildingSet(storedCampus)
          if (!pds.db.replaceJsonDocument(SCHEMA.cCampuses, SCHEMA.fCampusCuid, campus.getId(), campus.toGeoJSON()))
            return RESPONSE.BAD("Campus could not be updated.")
          return RESPONSE.OK("Successfully updated campus.")
        } catch {
          case e: DatasourceException =>
            return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def spaceSetAllByOwner = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("spaceSetAllByOwner: " + Utils.stripJson(json))
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        try {
          val buildingsets = pds.db.getAllBuildingsetsByOwner(owner_id)
          val res: JsValue = Json.obj("buildingsets" -> buildingsets)
          try
            RESPONSE.gzipJsonOk(res.toString)
          catch {
            case ioe: IOException =>
              return RESPONSE.OK(res, "Successfully retrieved all buildingsets.")
          }
        } catch {
          case e: DatasourceException =>
            return RESPONSE.ERROR(e)
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
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("campusDelete: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fCampusCuid)
        if (checkRequirements != null) return checkRequirements
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val cuid = (json \ SCHEMA.fCampusCuid).as[String]
        try {
          val storedCampus = pds.db.getFromKeyAsJson(SCHEMA.cCampuses, SCHEMA.fCampusCuid, cuid)
          if (storedCampus == null)
            return RESPONSE.BAD_CANNOT_RETRIEVE_CAMPUS
          if (!isCampusOwner(storedCampus, owner_id))
            return RESPONSE.UNAUTHORIZED("Unauthorized")
          if (!pds.db.deleteFromKey(SCHEMA.cCampuses, SCHEMA.fCampusCuid, cuid))
            return RESPONSE.ERROR_INTERNAL("500: Failed to delete Campus")
        } catch {
          case e: DatasourceException =>
            return RESPONSE.ERROR(e)
        }
        return RESPONSE.OK("Successfully deleted everything related to building.")
      }

      inner(request)
  }

  private def isCampusOwner(campus: JsValue, userId: String): Boolean = {
    if (userId.equals(ADMIN_ID)) // admin TODO:NN moderator or admin .. (>= moderator)
      return true
    if (campus != null && (campus \ SCHEMA.fOwnerId).toOption.isDefined) {  // check if owner
      return (campus \ SCHEMA.fOwnerId).as[String].equals(userId)
    }
    false
  }

  def floorAdd() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("floorAdd: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fIsPublished, SCHEMA.fBuid, SCHEMA.fFloorName,
          SCHEMA.fDescription, SCHEMA.fFloorNumber)
        if (checkRequirements != null) return checkRequirements
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          if (!user.hasAccess(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        val floorNum = (json \ SCHEMA.fFloorNumber).as[String]
        if (!Floor.checkFloorNumberFormat(floorNum)) return RESPONSE.BAD("Floor number cannot contain whitespace.")
        try {
          json = json.as[JsObject] - SCHEMA.fAccessToken
          val floor = new Floor(json)
          if (!pds.db.addJson(SCHEMA.cFloorplans, Utils.stripJson(floor.toJson())))
            return RESPONSE.BAD_CANNOT_ADD_FLOOR

          return RESPONSE.OK("Successfully added floor " + floorNum + ".")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  @deprecated("NotInUse")
  def floorUpdate() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("floorUpdate: " + Utils.stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fFloorNumber)
        if (!requiredMissing.isEmpty) return RESPONSE.MISSING_FIELDS(requiredMissing)
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          if (!user.hasAccess(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        val floorNum = (json \ "fllor_number").as[String]
        if (!Floor.checkFloorNumberFormat(floorNum)) return RESPONSE.BAD("Floor number cannot contain whitespace.")
        try {
          val fuid = Floor.getId(buid, floorNum)
          var storedFloor = pds.db.getFromKeyAsJson(SCHEMA.cFloorplans, SCHEMA.fFuid, fuid)
          if (storedFloor == null) return RESPONSE.BAD_CANNOT_RETRIEVE_FLOOR
          if (json.\(SCHEMA.fIsPublished).getOrElse(null) != null)
            storedFloor = storedFloor.as[JsObject] + (SCHEMA.fIsPublished -> JsString((json \ SCHEMA.fIsPublished).as[String]))
          if (json.\(SCHEMA.fFloorName).getOrElse(null) != null)
            storedFloor = storedFloor.as[JsObject] + (SCHEMA.fFloorName, JsString((json \ SCHEMA.fFloorName).as[String]))
          if (json.\(SCHEMA.fDescription).getOrElse(null) != null)
            storedFloor = storedFloor.as[JsObject] + (SCHEMA.fDescription, JsString((json \ SCHEMA.fDescription).as[String]))
          val floor = new Floor(storedFloor)
          if (!pds.db.replaceJsonDocument(SCHEMA.cFloorplans, SCHEMA.fFuid, floor.getId(), floor.toJson().toString))
            return RESPONSE.BAD("Floor could not be updated.")
          return RESPONSE.OK("Successfully updated floor.")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def floorDelete() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("floorDelete: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid, SCHEMA.fFloorNumber)
        if (checkRequirements != null) return checkRequirements
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floorNum = (json \ SCHEMA.fFloorNumber).as[String]
        try {
          val storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          if (!user.hasAccess(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        try {
          val deleted = pds.db.deleteAllByFloor(buid, floorNum)
          if (deleted == false)
            return RESPONSE.BAD("Some items related to the floor could not be deleted.")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        val filePath = tilerHelper.getFloorPlanFor(buid, floorNum)
        try {
          val floorfile = new File(filePath)
          // CHECK:NN BUGFIX: Fixing floorplan files and directory removal during floor delete
          if (floorfile.exists()) HelperMethods.recDeleteDirFile(floorfile.getParentFile())
        } catch {
          case e: IOException => return RESPONSE.ERROR("While deleting floorplan." +
            "\nRelated data are deleted from the database.", e)
        }
        return RESPONSE.OK("Successfully deleted everything related to the floor.")
      }

      inner(request)
  }

  def floorAll() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("floorAll: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid)
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val floors = pds.db.floorsByBuildingAsJson(buid)
          val res: JsValue = Json.obj("floors" -> floors.asScala)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case ioe: IOException => return RESPONSE.OK(res, "Successfully retrieved all floors.")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def poisAdd() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)

        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()

        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("poisAdd: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fIsPublished, SCHEMA.fBuid, SCHEMA.fFloorName,
          SCHEMA.fFloorNumber, SCHEMA.fName, SCHEMA.fPoisType, SCHEMA.fIsDoor, SCHEMA.fIsBuildingEntrance, SCHEMA.fCoordinatesLat, SCHEMA.fCoordinatesLon)
        if (checkRequirements != null) return checkRequirements
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id)) - SCHEMA.fAccessToken
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          if (!user.hasAccess(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        try {
          val poi = new Poi(json)
          if (!pds.db.addJson(SCHEMA.cPOIS, poi.toGeoJSON())) return RESPONSE.BAD_CANNOT_RETRIEVE_POI
          val res: JsValue = Json.obj(SCHEMA.fPuid -> poi.getId())
          return RESPONSE.OK(res, "Successfully added POI.")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def poisUpdate() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("poisUpdate: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fPuid, SCHEMA.fBuid)
        if (checkRequirements != null) return checkRequirements
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val puid = (json \ SCHEMA.fPuid).as[String]
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          if (!user.hasAccess(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        try {
          var storedPoi = pds.db.getFromKeyAsJson(SCHEMA.cPOIS, SCHEMA.fPuid, puid)
          if (storedPoi == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          if (json.\(SCHEMA.fIsPublished).getOrElse(null) != null) {
            val is_published = (json \ SCHEMA.fIsPublished).as[String]
            if (is_published == "true" || is_published == "false")
              storedPoi = storedPoi.as[JsObject] + (SCHEMA.fIsPublished -> JsString((json \ SCHEMA.fIsPublished).as[String]))
          }
          if (json.\(SCHEMA.fName).getOrElse(null) != null)
            storedPoi = storedPoi.as[JsObject] + (SCHEMA.fName -> JsString((json \ SCHEMA.fName).as[String]))
          if (json.\(SCHEMA.fDescription).getOrElse(null) != null)
            storedPoi = storedPoi.as[JsObject] + (SCHEMA.fDescription -> JsString((json \ SCHEMA.fDescription).as[String]))
          if (json.\(SCHEMA.fURL).getOrElse(null) != null)
            storedPoi = storedPoi.as[JsObject] + (SCHEMA.fURL -> JsString((json \ SCHEMA.fURL).as[String]))
          if (json.\(SCHEMA.fPoisType).getOrElse(null) != null)
            storedPoi = storedPoi.as[JsObject] + (SCHEMA.fPoisType -> JsString((json \ SCHEMA.fPoisType).as[String]))
          if (json.\(SCHEMA.fIsDoor).getOrElse(null) != null) {
            val is_door = (json \ SCHEMA.fIsDoor).as[String]
            if (is_door == "true" || is_door == "false")
              storedPoi = storedPoi.as[JsObject] + (SCHEMA.fIsDoor -> JsString((json \ SCHEMA.fIsDoor).as[String]))
          }
          if (json.\(SCHEMA.fIsBuildingEntrance).getOrElse(null) != null) {
            val is_building_entrance = (json \ SCHEMA.fIsBuildingEntrance).as[String]
            if (is_building_entrance == "true" || is_building_entrance == "false")
              storedPoi = storedPoi.as[JsObject] + (SCHEMA.fIsBuildingEntrance, JsString((json \ SCHEMA.fIsBuildingEntrance).as[String]))
          }
          if (json.\(SCHEMA.fImage).getOrElse(null) != null)
            storedPoi = storedPoi.as[JsObject] + (SCHEMA.fImage, JsString((json \ SCHEMA.fImage).as[String]))
          if (json.\(SCHEMA.fCoordinatesLat).getOrElse(null) != null)
            storedPoi = storedPoi.as[JsObject] + (SCHEMA.fCoordinatesLat, JsString((json \ SCHEMA.fCoordinatesLat).as[String]))
          if (json.\(SCHEMA.fCoordinatesLon).getOrElse(null) != null)
            storedPoi = storedPoi.as[JsObject] + (SCHEMA.fCoordinatesLon, JsString((json \ SCHEMA.fCoordinatesLon).as[String]))
          val poi = new Poi(storedPoi)
          if (!pds.db.replaceJsonDocument(SCHEMA.cPOIS, SCHEMA.fPuid, poi.getId(), poi.toGeoJSON()))
            return RESPONSE.BAD("Poi could not be updated.")
          return RESPONSE.OK("Successfully updated poi.")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def poisDelete() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("poiDelete: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fPuid, SCHEMA.fBuid)
        if (checkRequirements != null) return checkRequirements
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val buid = (json \ SCHEMA.fBuid).as[String]
        val puid = (json \ SCHEMA.fPuid).as[String]
        try {
          val storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          if (!user.hasAccess(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        try {
          val all_items_failed = pds.db.deleteAllByPoi(puid)
          if (all_items_failed.size > 0) {
            val res = Json.obj("ids" -> all_items_failed.asScala)
            return RESPONSE.BAD(res, "Some items related to the deleted poi could not be deleted: " +
              all_items_failed.size +
              " items.")
          }
          return RESPONSE.OK("Successfully deleted everything related to the poi.")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }

      }

      inner(request)
  }

  def poisByFloor() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("poisByFloor: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid, SCHEMA.fFloorNumber)
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floorNum = (json \ SCHEMA.fFloorNumber).as[String]
        try {
          val storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          val pois = pds.db.poisByBuildingFloorAsJson(buid, floorNum)
          val res: JsValue = Json.obj(SCHEMA.cPOIS -> pois)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case _: IOException => return RESPONSE.OK(res, "Retrieved pois. floor: " + floorNum)
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def poisByBuid() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("poisByBuid: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid)
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          val pois = pds.db.poisByBuildingAsJson(buid)
          val res: JsValue = Json.obj(SCHEMA.cPOIS -> pois.asScala)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case _: IOException => return RESPONSE.OK(res, "Retrieved space pois.")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  /**
   * Retrieve all the pois of a cuid combination.
   * Available searchs in english and greeklish.
   *
   * @return
   */
  def searchPois = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        LOG.D2("searchPois") // ALWAYS: a D2 on the endpoint method with method name
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D3("json = " + json)
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
            result = pds.db.poisByBuildingAsJson3(buid, letters)
          else if (greeklish.compareTo("true") == 0)
            result = pds.db.poisByBuildingAsJson2GR(cuid, letters)
          else
            result = pds.db.poisByBuildingAsJson2(cuid, letters)
          val res: JsValue = Json.obj(SCHEMA.cPOIS -> result)
          try
            RESPONSE.gzipJsonOk(res.toString)
          catch {
            case ioe: IOException =>
              return RESPONSE.OK(res, "Retrieved space pois.")
          }
        } catch {
          case e: DatasourceException =>
            return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  /**
   * Retrieve all the pois of a building/floor combination.
   *
   * @return
   */
  def poisBySpaceConnectors = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.I("poisBySpaceConnectors(): " + Utils.stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid)
        if (!requiredMissing.isEmpty)
          return RESPONSE.MISSING_FIELDS(requiredMissing)
        if (String(json, SCHEMA.fBuid) == null)
          return RESPONSE.BAD("buid field must be String.")
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val pois = pds.db.poisByBuildingIDAsJson(buid)
          val res: JsValue = Json.obj(SCHEMA.cPOIS -> pois)
          try
            RESPONSE.gzipJsonOk(res.toString)
          catch {
            case ioe: IOException =>
              return RESPONSE.OK(res, "Successfully retrieved all pois from buid " + buid + ".")
          }
        } catch {
          case e: DatasourceException =>
            return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def connectionAdd() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.I("AnyplaceMapping::connectionAdd(): " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fIsPublished, SCHEMA.fPoisA, SCHEMA.fFloorA,
          SCHEMA.fBuidA, SCHEMA.fPoisB, SCHEMA.fFloorB, SCHEMA.fBuidB, SCHEMA.fBuid, SCHEMA.fEdgeType)
        if (checkRequirements != null) return checkRequirements
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val buid = (json \ SCHEMA.fBuid).as[String]
        val buid1 = (json \ SCHEMA.fBuidA).as[String]
        val buid2 = (json \ SCHEMA.fBuidB).as[String]
        try {
          var storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid1)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          if (!user.hasAccess(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
          storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid2)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          if (!user.hasAccess(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
          storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        val edge_type = (json \ SCHEMA.fEdgeType).as[String]
        if (edge_type != Connection.EDGE_TYPE_ELEVATOR && edge_type != Connection.EDGE_TYPE_HALLWAY &&
          edge_type != Connection.EDGE_TYPE_ROOM && edge_type != Connection.EDGE_TYPE_OUTDOOR &&
          edge_type != Connection.EDGE_TYPE_STAIR)
          return RESPONSE.BAD("Invalid edge type specified.")
        val pois_a = (json \ SCHEMA.fPoisA).as[String]
        val pois_b = (json \ SCHEMA.fPoisB).as[String]

        if (!pds.db.poiByBuidFloorPuid(buid1, (json \ SCHEMA.fFloorA).as[String], (json \ SCHEMA.fPoisA).as[String]))
          return RESPONSE.BAD_CANNOT_RETRIEVE("POI-A")
        if (!pds.db.poiByBuidFloorPuid(buid2, (json \ SCHEMA.fFloorB).as[String], (json \ SCHEMA.fPoisB).as[String]))
          return RESPONSE.BAD_CANNOT_RETRIEVE("POI-B")
        try {
          val weight = calculateWeightOfConnection(pois_a, pois_b)
          json = json.as[JsObject] + (SCHEMA.fWeight -> JsString(java.lang.Double.toString(weight)))
          if (edge_type == Connection.EDGE_TYPE_ELEVATOR || edge_type == Connection.EDGE_TYPE_STAIR) {
          }
          val conn = new Connection(json)
          if (!pds.db.addJson(SCHEMA.cEdges, conn.toJson().toString))
            return RESPONSE.BAD_CANNOT_ADD_CONNECTION
          val res: JsValue = Json.obj(SCHEMA.fConCuid -> conn.getId())
          return RESPONSE.OK(res, "Successfully added new connection.")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
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
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.I("AnyplaceMapping::connectionUpdate(): " + Utils.stripJson(json))
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fPoisA, SCHEMA.fPoisB, SCHEMA.fBuidA, SCHEMA.fBuidB)
        if (!requiredMissing.isEmpty) return RESPONSE.MISSING_FIELDS(requiredMissing)
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val buid1 = (json \ SCHEMA.fBuidA).as[String]
        val buid2 = (json \ SCHEMA.fBuidB).as[String]
        try {
          var storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid1)
          if (storedSpace == null)
            return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          if (!user.hasAccess(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
          storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid2)
          if (storedSpace == null)
            return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          if (!user.hasAccess(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        try {
          val pois_a = (json \ SCHEMA.fPoisA).as[String]
          val pois_b = (json \ SCHEMA.fPoisB).as[String]
          val cuid = Connection.getId(pois_a, pois_b)
          var storedConn = pds.db.getFromKeyAsJson(SCHEMA.cEdges, SCHEMA.fConCuid, cuid)
          if (storedConn == null)
            return RESPONSE.BAD_CANNOT_RETRIEVE_CONNECTION
          if (json.\(SCHEMA.fIsPublished).getOrElse(null) != null) {
            val is_published = (json \ SCHEMA.fIsPublished).as[String]
            if (is_published == "true" || is_published == "false")
              storedConn = storedConn.as[JsObject] + (SCHEMA.fIsPublished -> JsString((json \ SCHEMA.fIsPublished).as[String]))
          }
          if (json.\(SCHEMA.fEdgeType).getOrElse(null) != null) {
            val edge_type = (json \ SCHEMA.fEdgeType).as[String]
            if (edge_type != Connection.EDGE_TYPE_ELEVATOR && edge_type != Connection.EDGE_TYPE_HALLWAY &&
              edge_type != Connection.EDGE_TYPE_ROOM && edge_type != Connection.EDGE_TYPE_OUTDOOR &&
              edge_type != Connection.EDGE_TYPE_STAIR)
              return RESPONSE.BAD("Invalid edge type specified.")
            storedConn = storedConn.as[JsObject] + (SCHEMA.fEdgeType -> JsString(edge_type))
          }
          val conn = new Connection(storedConn)
          if (!pds.db.replaceJsonDocument(SCHEMA.cEdges, SCHEMA.fConCuid, conn.getId(), conn.toJson().toString))
            return RESPONSE.BAD("Connection could not be updated.")
          return RESPONSE.OK("Successfully updated connection.")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def connectionDelete() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {

        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("connectionDelete: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fPoisA, SCHEMA.fPoisB, SCHEMA.fBuidA, SCHEMA.fBuidB)
        if (checkRequirements != null) return checkRequirements
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val buid1 = (json \ SCHEMA.fBuidA).as[String]
        val buid2 = (json \ SCHEMA.fBuidB).as[String]
        try {
          var storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid1)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE("SpaceA")
          if (!user.hasAccess(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
          storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid2)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE("SpaceB")
          if (!user.hasAccess(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        val pois_a = (json \ SCHEMA.fPoisA).as[String]
        val pois_b = (json \ SCHEMA.fPoisB).as[String]
        try {
          val cuid = Connection.getId(pois_a, pois_b)
          val all_items_failed = pds.db.deleteAllByConnection(cuid)
          if (all_items_failed == null) {
            LOG.E("connectionDelete: " + cuid + " not found.")
            return RESPONSE.BAD("POI Connection not found")
          }
          if (all_items_failed.size > 0) {
            val obj: JsValue = Json.obj("ids" -> all_items_failed.asScala)
            return RESPONSE.BAD(obj, "Some items related to the deleted connection could not be deleted: " +
              all_items_failed.size + " items.")
          }
          return RESPONSE.OK("Successfully deleted everything related to the connection.")
        } catch {
          case e: DatasourceException => RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def connectionsByFloor() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("connectionsByFloor(): " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid, SCHEMA.fFloorNumber)
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floorNum = (json \ SCHEMA.fFloorNumber).as[String]
        try {
          val storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          val storedFloors = pds.db.floorsByBuildingAsJson(buid)
          var floorExists = false
          for (floor <- storedFloors.asScala)
            if ((floor \ SCHEMA.fFloorNumber).as[String] == floorNum)
              floorExists = true
          if (!floorExists) return RESPONSE.BAD_CANNOT_RETRIEVE_FLOOR

          val pois = pds.db.connectionsByBuildingFloorAsJson(buid, floorNum)
          val res: JsValue = Json.obj("connections" -> pois)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case ioe: IOException => return RESPONSE.OK(res, "Successfully retrieved all pois from floor " + floorNum +
              ".")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  import java.io.IOException

  import datasources.DatasourceException
  import utils.{JsonUtils, LOG, RESPONSE}

  /**
   * Retrieve all the pois of a building/floor combination.
   *
   * @return
   */
  def connectionsByallFloors = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.I("AnyplaceMapping::connectionsByallFloors(): " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid)
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          val pois = pds.db.connectionsByBuildingAllFloorsAsJson(buid)
          val res: JsValue = Json.obj("connections" -> pois)
          try
            RESPONSE.gzipJsonOk(res.toString)
          catch {
            case ioe: IOException =>
              return RESPONSE.OK(res, "Successfully retrieved all pois from all floors .")
          }
        } catch {
          case e: DatasourceException =>
            return RESPONSE.ERROR(e)
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
    val pa = pds.db.getFromKeyAsJson(SCHEMA.cPOIS, SCHEMA.fPuid, pois_a)
    if (pa == null) {
      lat_a = 0.0
      lon_a = 0.0
    } else try {
      lat_a = nf.parse((pa \ SCHEMA.fCoordinatesLat).as[String]).doubleValue()
      lon_a = nf.parse((pa \ SCHEMA.fCoordinatesLon).as[String]).doubleValue()
    } catch {
      case e: ParseException => e.printStackTrace()
    }
    val pb = pds.db.getFromKeyAsJson(SCHEMA.cPOIS, SCHEMA.fPuid, pois_b)
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

  def serveFloorPlanBinary(buid: String, floorNum: String) = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("serveFloorPlanBinary: " + Utils.stripJson(json))
        val filePath = tilerHelper.getFloorPlanFor(buid, floorNum)
        LOG.D2("requested: " + filePath)
        try {
          val file = new File(filePath)
          // LPLogger.debug("filePath " + file.getAbsolutePath.toString)
          if (!file.exists()) return RESPONSE.BAD_CANNOT_RETRIEVE_FLOORPLAN(floorNum)
          if (!file.canRead()) return RESPONSE.BAD_CANNOT_READ_FLOORPLAN(floorNum)
          Ok.sendFile(file)
        } catch {
          case e: FileNotFoundException => return RESPONSE.ERROR_INTERNAL("Could not read floorplan.")
        }
      }

      inner(request)
  }

  def serveFloorPlanTilesZip(buid: String, floorNum: String) = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.I("AnyplaceMapping::serveFloorPlanTilesZip(): " + Utils.stripJson(json))
        if (!Floor.checkFloorNumberFormat(floorNum)) return RESPONSE.BAD("Floor number cannot contain whitespace.")
        val filePath = tilerHelper.getFloorTilesZipFor(buid, floorNum)
        LOG.I("requested: " + filePath)
        try {
          val file = new File(filePath)
          if (!file.exists()) return RESPONSE.BAD_CANNOT_RETRIEVE_FLOORPLAN(floorNum)
          if (!file.canRead) return RESPONSE.BAD_CANNOT_READ_FLOORPLAN(floorNum)
          Ok.sendFile(file)
        } catch {
          case e: FileNotFoundException => return RESPONSE.ERROR_INTERNAL("Could not read floorplan.")
        }
      }

      inner(request)
  }

  def serveFloorPlanTilesZipLink(buid: String, floorNum: String) = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.I("AnyplaceMapping::serveFloorPlanTilesZipLink(): " + Utils.stripJson(json))
        if (!Floor.checkFloorNumberFormat(floorNum)) return RESPONSE.BAD("Floor number cannot contain whitespace.")
        val filePath = tilerHelper.getFloorTilesZipFor(buid, floorNum)
        LOG.I("requested: " + filePath)
        val file = new File(filePath)
        if (!file.exists()) return RESPONSE.BAD_CANNOT_RETRIEVE_FLOORPLAN(floorNum)
        if (!file.canRead) return RESPONSE.BAD_CANNOT_READ_FLOORPLAN(floorNum)
        val res: JsValue = Json.obj("tiles_archive" -> tilerHelper.getFloorTilesZipLinkFor(buid, floorNum))
        return RESPONSE.OK(res, "Successfully fetched link for the tiles archive.")
      }

      inner(request)
  }

  def serveFloorPlanTilesStatic(buid: String, floorNum: String, path: String) = Action {
    def inner(): Result = {
      if (path == null || buid == null || floorNum == null ||
        path.trim().isEmpty ||
        buid.trim().isEmpty ||
        floorNum.trim().isEmpty) NotFound(<h1>Page not found</h1>)
      var filePath: String = null
      filePath = if (path == tilerHelper.FLOOR_TILES_ZIP_NAME) tilerHelper.getFloorTilesZipFor(buid,
        floorNum) else tilerHelper.getFloorTilesDirFor(buid, floorNum) +
        path
      try {
        val file = new File(filePath)
        //send ok message to tiler
        if (!file.exists() || !file.canRead()) return RESPONSE.OK("File requested not found")
        Ok.sendFile(file)
      } catch {
        case _: FileNotFoundException => return RESPONSE.BAD_CANNOT_READ_FLOORPLAN(floorNum)
      }
    }

    inner()
  }

  def serveFloorPlanBase64(buid: String, floorNum: String) = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("serveFloorPlanBase64: " + Utils.stripJson(json))
        val filePath = tilerHelper.getFloorPlanFor(buid, floorNum)
        LOG.D2("requested: " + filePath)
        val file = new File(filePath)
        try {
          if (!file.exists()) return RESPONSE.BAD_CANNOT_RETRIEVE_FLOORPLAN(floorNum)
          if (!file.canRead) return RESPONSE.BAD_CANNOT_READ_FLOORPLAN(floorNum)

          try {
            val s = Utils.encodeFileToBase64Binary(fu, filePath)
            try {
              RESPONSE.gzipOk(s)
            } catch {
              case ioe: IOException => Ok(s)
            }
          } catch {
            case e: IOException => return RESPONSE.BAD("Requested floorplan cannot be encoded in base64 properly: " +
              floorNum)
          }
        } catch {
          case e: Exception => return RESPONSE.ERROR_INTERNAL("Unknown server error during floorplan delivery.")
        }
      }

      inner(request)
  }


  /**
   * Returns the floorplan in base64 form. Used by the Anyplace websites
   *
   * @param buid
   * @param floorNum
   * @return
   */
  def serveFloorPlanBase64all(buid: String, floorNum: String) = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("serveFloorPlanBase64all: " + Utils.stripJson(json) + " " + floorNum)
        val floors = floorNum.split(" ")
        val all_floors = new util.ArrayList[String]
        var z = 0
        while (z < floors.length) {
          val filePath = tilerHelper.getFloorPlanFor(buid, floors(z))
          LOG.D2("serveFloorPlanBase64all: requested: " + filePath)
          val file = new File(filePath)
          try
            if (!file.exists || !file.canRead) { all_floors.add("") }
            else try {
              val s = Utils.encodeFileToBase64Binary(fu, filePath)
              all_floors.add(s)
            } catch {
              case e: IOException =>
                return RESPONSE.BAD("Requested floorplan cannot be encoded in base64 properly: " + floors(z))
            }
          catch {
            case e: Exception =>
              return RESPONSE.ERROR_INTERNAL("Unknown server error during floorplan delivery.")
          }
          //{
          z += 1
          //z - 1 // CHECK:NN what was that?
          //}
        }
        val res: JsValue = Json.obj("all_floors" -> all_floors.asScala)
        try
          RESPONSE.gzipJsonOk(res.toString)
        catch {
          case ioe: IOException =>
            return RESPONSE.OK(res, "Successfully retrieved all floors.")
        }
      }

      inner(request)
  }

  // CHECK:NN why deprecated?
  @deprecated("NotInUse")
  def floorPlanUpload() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {

        return RESPONSE.DEPRECATED("Invalid request type: Not Multipart")

        val anyReq = new OAuth2Request(request)
        val body = anyReq.getMultipartFormData()
        if (body == null) return RESPONSE.BAD("Invalid request type - Not Multipart.")
        var floorplan = body.file("floorplan").get
        if (floorplan == null) return RESPONSE.BAD("Cannot find the floorplan file in your request.")
        val urlenc = body.asFormUrlEncoded
        val json_str = urlenc.get("json").get.head // CHECK:NN get("json").get(0)
        if (json_str == null) return RESPONSE.BAD("Cannot find json in the request.")
        var json: JsValue = null
        try {
          json = Json.parse(json_str)
        } catch {
          case e: IOException => return RESPONSE.BAD_PARSE_JSON
        }
        LOG.I("Floorplan Request[json]: " + json.toString)
        LOG.I("Floorplan Request[floorplan]: " + floorplan.filename)
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fFloorNumber, SCHEMA.fLatBottomLeft,
          SCHEMA.fLonBottomLeft, SCHEMA.fLatTopRight, SCHEMA.fLonTopRight)
        if (!requiredMissing.isEmpty) return RESPONSE.MISSING_FIELDS(requiredMissing)
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floorNum = (json \ SCHEMA.fFloorNumber).as[String]
        val bottom_left_lat = (json \ SCHEMA.fLatBottomLeft).as[String]
        val bottom_left_lng = (json \ SCHEMA.fLonBottomLeft).as[String]
        val top_right_lat = (json \ SCHEMA.fLatTopRight).as[String]
        val top_right_lng = (json \ SCHEMA.fLonTopRight).as[String]
        val fuid = Floor.getId(buid, floorNum)
        try {
          var storedFloor = pds.db.getFromKeyAsJson(SCHEMA.cFloorplans, SCHEMA.fFuid, fuid)
          if (storedFloor == null) return RESPONSE.BAD_CANNOT_RETRIEVE_FLOOR
          storedFloor = storedFloor.as[JsObject] + (SCHEMA.fLatBottomLeft -> JsString(bottom_left_lat))
          storedFloor = storedFloor.as[JsObject] + (SCHEMA.fLonBottomLeft -> JsString(bottom_left_lng))
          storedFloor = storedFloor.as[JsObject] + (SCHEMA.fLatTopRight -> JsString(top_right_lat))
          storedFloor = storedFloor.as[JsObject] + (SCHEMA.fLonTopRight -> JsString(top_right_lng))
          if (!pds.db.replaceJsonDocument(SCHEMA.cFloorplans, SCHEMA.fFuid, fuid, storedFloor.toString))
            return RESPONSE.BAD("floorplan could not be updated in the database.")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR_INTERNAL("Error while reading from our backend service.")
        }
        var floor_file: File = null
        try {
          floor_file = tilerHelper.storeFloorPlanToServer(buid, floorNum, floorplan.ref.file)
        } catch {
          case e: AnyPlaceException => return RESPONSE.BAD("Cannot save floorplan on the server.")
        }
        val top_left_lat = top_right_lat
        val top_left_lng = bottom_left_lng
        try {
          tilerHelper.tileImage(floor_file, top_left_lat, top_left_lng)
        } catch {
          case e: AnyPlaceException => return RESPONSE.BAD("Could not create floorplan tiles on the server.")
        }
        LOG.I("Successfully tiled: " + floor_file.toString)
        return RESPONSE.OK("Successfully updated floorplan.")
      }

      inner(request)
  }

  /**
   * After a floor was added, this endpoints:
   *    1. uploads a floorplan (filesystem)
   *    2. updates the floor with the coordinates of the floorplan (db)
   */
  def floorPlanUploadWithZoom() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        LOG.D2("floorPlanUploadWithZoom")
        val anyReq = new OAuth2Request(request)
        val body = anyReq.getMultipartFormData()
        if (body == null) return RESPONSE.BAD("Invalid request type - Not Multipart.")
        val floorplan = body.file("floorplan").get
        if (floorplan == null) return RESPONSE.BAD("Cannot find the floorplan file in your request.")
        val urlenc = body.asFormUrlEncoded
        val json_str = urlenc.get("json").get(0)
        if (json_str == null) return RESPONSE.BAD("Cannot find json in the request.")
        var json: JsValue = null
        try {
          json = Json.parse(json_str)
        } catch {
          case _: IOException => return RESPONSE.BAD_PARSE_JSON
        }
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid, SCHEMA.fFloorNumber, SCHEMA.fLatBottomLeft,
          SCHEMA.fLonBottomLeft, SCHEMA.fLatTopRight, SCHEMA.fLonTopRight, SCHEMA.fZoom)
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        val zoom = (json \ SCHEMA.fZoom).as[String]
        val zoom_number = zoom.toInt
        if (zoom_number < 20)
          return RESPONSE.BAD("You have provided zoom level " + zoom + ". You have to zoom at least to level 20 to upload the floorplan.")
        val floorNum = (json \ SCHEMA.fFloorNumber).as[String]
        val bottom_left_lat = (json \ SCHEMA.fLatBottomLeft).as[String]
        val bottom_left_lng = (json \ SCHEMA.fLonBottomLeft).as[String]
        val top_right_lat = (json \ SCHEMA.fLatTopRight).as[String]
        val top_right_lng = (json \ SCHEMA.fLonTopRight).as[String]
        val fuid = Floor.getId(buid, floorNum)
        try {
          var storedFloor = pds.db.getFromKeyAsJson(SCHEMA.cFloorplans, SCHEMA.fFuid, fuid)
          if (storedFloor == null) return RESPONSE.BAD_CANNOT_RETRIEVE_FLOOR
          storedFloor = storedFloor.as[JsObject] + (SCHEMA.fZoom -> JsString(zoom))
          storedFloor = storedFloor.as[JsObject] + (SCHEMA.fLatBottomLeft -> JsString(bottom_left_lat))
          storedFloor = storedFloor.as[JsObject] + (SCHEMA.fLonBottomLeft -> JsString(bottom_left_lng))
          storedFloor = storedFloor.as[JsObject] + (SCHEMA.fLatTopRight -> JsString(top_right_lat))
          storedFloor = storedFloor.as[JsObject] + (SCHEMA.fLonTopRight -> JsString(top_right_lng))
          if (!pds.db.replaceJsonDocument(SCHEMA.cFloorplans, SCHEMA.fFuid, fuid, storedFloor.toString)) {
            return RESPONSE.BAD("floorplan could not be updated in the database.")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR_INTERNAL("Error while reading from our backend service.")
        }
        var floor_file: File = null
        try {
          floor_file = tilerHelper.storeFloorPlanToServer(buid, floorNum, floorplan.ref.path.toFile)
        } catch {
          case e: AnyPlaceException => return RESPONSE.BAD("Cannot save floorplan on the server.")
        }
        val top_left_lat = top_right_lat
        val top_left_lng = bottom_left_lng
        try {
          tilerHelper.tileImageWithZoom(floor_file, top_left_lat, top_left_lng, zoom)
        } catch {
          case e: AnyPlaceException => return RESPONSE.BAD("Could not create floorplan tiles on the server.")
        }
        LOG.I("Successfully tiled: " + floor_file.toString)
        return RESPONSE.OK("Successfully updated floorplan.")
      }

      inner(request)
  }

  // CHECK:NN
  // TODO: Implement
  // TODO new object with above but password encrypt (salt)
  // TODO add this to mongo (insert)
  // TODO Generate access_token: "local_VERY LONG SHA"
  def addLocalAccount(json: JsValue): Result = {

    // call appendUserType
    // ----------------------------
    //  requirePropertiesInJson: email, username, password
    val mdb: MongoDatabase = mongoDB.getMDB
    val collection = mdb.getCollection(SCHEMA.cUsers)
    val userLookUp = collection.find(equal("username", (json \ "username").as[String]))
    val awaited = Await.result(userLookUp.toFuture(), Duration.Inf)
    val res = awaited.toList
    if (res.size != 0) {
      // TODO user must have unique username (query username in mongo)
    }
    null
  }


  // CHECK:NN
  @deprecated("NotNeeded")
  def maintenance(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("maintenance: " + Utils.stripJson(json))
        try {
          if (!pds.db.deleteNotValidDocuments()) return RESPONSE.BAD("None valid documents.")
          return RESPONSE.OK("Success")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  // CHECK:NN
  private def getRadioMapMeanByBuildingFloor(buid: String, floorNum: String): Option[RadioMapMean] = {
    val radioMapsFrozenDir = conf.get[String]("radioMapFrozenDir")
    val rmapDir = new File(radioMapsFrozenDir + File.separatorChar + buid + File.separatorChar + floorNum)
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
    LOG.D5(radio.toPath.getFileName.toString)
    var floorFetched: Long = 0L
    floorFetched = pds.db.dumpRssLogEntriesByBuildingACCESFloor(fout, buid, floorNum)
    try {
      fout.close()
    } catch {
      case e: IOException => LOG.E("Closing the output stream for the dumped rss logs", e)
    }
    if (floorFetched == 0) { Option[RadioMapMean](null) }

    val folder = rmapDir.toString
    val radiomap_filename = new File(folder + File.separatorChar + "indoor-radiomap.txt").getAbsolutePath
    val radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt")
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

    Option[RadioMapMean](rm_mean)
  }

}
