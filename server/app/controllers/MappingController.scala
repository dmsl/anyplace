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
import json.VALIDATE.String
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
          val radioPoints = pds.getIDatasource.getRadioHeatmap()
          if (radioPoints == null) return RESPONSE.BAD("Building does not exist or could not be retrieved.")
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
          val radioPoints = pds.getIDatasource.getRadioHeatmapByBuildingFloorAverage1(buid, floor)
          if (radioPoints == null) return RESPONSE.BAD("Space does not exist or could not be retrieved.")
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
          val radioPoints = pds.getIDatasource.getRadioHeatmapByBuildingFloorAverage2(buid, floor)
          if (radioPoints == null) return RESPONSE.BAD("Space does not exist or could not be retrieved.")
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
          val radioPoints = pds.getIDatasource.getRadioHeatmapByBuildingFloorAverage3(buid, floor)
          if (radioPoints == null) return RESPONSE.BAD("Space does not exist or could not be retrieved.")
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
          val radioPoints = pds.getIDatasource.getRadioHeatmapByBuildingFloorAverage3(buid, floor)
          if (radioPoints == null) return RESPONSE.BAD("Building does not exist or could not be retrieved.")

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
          val radioPoints = pds.getIDatasource.getRadioHeatmapByFloorTimestamp(buid, floor, timestampX, timestampY)
          if (radioPoints == null) return RESPONSE.BAD("Fingerprints does not exist or could not be retrieved.")
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
          val radioPoints = pds.getIDatasource.getRadioHeatmapByFloorTimestamp(buid, floor, timestampX, timestampY)
          if (radioPoints == null) return RESPONSE.BAD("Fingerprints does not exist or could not be retrieved!")
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
          val radioPoints = pds.getIDatasource.getRadioHeatmapByBuildingFloorTimestampAverage1(buid, floor, timestampX, timestampY)
          if (radioPoints == null) return RESPONSE.BAD("Fingerprints does not exist or could not be retrieved.")
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
          val radioPoints = pds.getIDatasource.getRadioHeatmapByBuildingFloorTimestampAverage2(buid, floor, timestampX, timestampY)
          if (radioPoints == null) return RESPONSE.BAD("Fingerprints does not exist or could not be retrieved.")
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
        val APs = pds.getIDatasource.getCachedAPsByBuildingFloor(buid, floor)
        // if cached return it
        if (APs != null) {
          val res = Json.obj("accessPoints" -> (APs \ "accessPoints").as[List[JsValue]])
          return RESPONSE.gzipJsonOk(res, "Fetched precompute of accessPointsWifi")
        } else {
          try {
            val accessPoints = pds.getIDatasource.getAPsByBuildingFloor(buid, floor)
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

            if (accessPoints == null) return RESPONSE.BAD("Space does not exist or could not be retrieved.")
            val newAccessPoint = Json.obj(SCHEMA.fBuid -> buid, SCHEMA.fFloor -> floor, "accessPoints" -> uniqueAPs.values().asScala)
            pds.getIDatasource.addJsonDocument(SCHEMA.cAccessPointsWifi, newAccessPoint.toString())
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
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER()
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floor_number = (json \ SCHEMA.fFloor).as[String]
        val lat1 = (json \ "lat1").as[String]
        val lon1 = (json \ "lon1").as[String]
        val lat2 = (json \ "lat2").as[String]
        val lon2 = (json \ "lon2").as[String]
        try {
          val fingerprints: List[JsValue] = pds.getIDatasource.getFingerPrintsBBox(
            buid, floor_number, lat1, lon1, lat2, lon2)
          if (fingerprints.isEmpty)
            return RESPONSE.BAD("Fingerprints does not exist or could not be retrieved.")

          LOG.D2("FingerPrintsDelete: will delete " + fingerprints.size + " fingerprints.")
          for (fingerprint <- fingerprints) {
            pds.getIDatasource.deleteFingerprint(fingerprint)
          }
          pds.getIDatasource.deleteAffectedHeatmaps(buid,floor_number)
          val res: JsValue = Json.obj("fingerprints" -> fingerprints)
          Future { mapHelper.updateFrozenRadioMap(buid, floor_number) }(ec)
          return RESPONSE.gzipJsonOk(res, "Deleted " + fingerprints.size + " fingerprints and returning them.")
        } catch {
          case e: Exception =>
            return RESPONSE.internal_server_error("FingerPrintsDelete: " + e.getClass + ": " + e.getMessage)
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
        val floor_number = (json \ SCHEMA.fFloor).as[String]
        val lat1 = (json \ "lat1").as[String]
        val lon1 = (json \ "lon1").as[String]
        val lat2 = (json \ "lat2").as[String]
        val lon2 = (json \ "lon2").as[String]
        val timestampX = (json \ SCHEMA.fTimestampX).as[String]
        val timestampY = (json \ SCHEMA.fTimestampY).as[String]
        try {
          val fingerprints: List[JsValue] = pds.getIDatasource.getFingerPrintsTimestampBBox(buid, floor_number, lat1, lon1, lat2, lon2, timestampX, timestampY)
          if (fingerprints.isEmpty)
            return RESPONSE.BAD("FingerPrints does not exist or could not be retrieved!")
          for (fingerprint <- fingerprints)
            pds.getIDatasource.deleteFingerprint(fingerprint)
          pds.getIDatasource.deleteAffectedHeatmaps(buid,floor_number)
          // TODO:NN below comment?
          // TODO:do also 1 and 2
          pds.getIDatasource.createTimestampHeatmap(SCHEMA.cHeatmapWifiTimestamp3, buid, floor_number, 3)
          val res: JsValue = Json.obj("radioPoints" -> fingerprints)
          try {
            Future { mapHelper.updateFrozenRadioMap(buid, floor_number) }(ec)
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
        val floor_number = (json \ SCHEMA.fFloor).as[String]

        // create cache-collections
        pds.getIDatasource.createTimestampHeatmap(SCHEMA.cHeatmapWifiTimestamp1, buid, floor_number, 1)
        pds.getIDatasource.createTimestampHeatmap(SCHEMA.cHeatmapWifiTimestamp2, buid, floor_number, 2)
        pds.getIDatasource.createTimestampHeatmap(SCHEMA.cHeatmapWifiTimestamp3, buid, floor_number, 3)

        try {
          val radioPoints: List[JsValue] = pds.getIDatasource.getFingerprintsByTime(buid, floor_number)
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
            val floor_number = (json \ SCHEMA.fFloor).as[String]
            val jsonstr=(json\"APs").as[String]
            val accessPoints= Json.parse(jsonstr).as[List[JsValue]]
            val floors: Array[JsonObject] = pds.getIDatasource.floorsByBuildingAsJson(buid).iterator().toArray
            val algorithm_choice = (json\"algorithm_choice").as[String].toInt
            */

        val rmapFile = new File(radioMapsFrozenDir + api.URL_SEP + buid + api.URL_SEP +
          floor_number + api.URL_SEP + "indoor-radiomap-mean.txt")

        if (!rmapFile.exists()) {  // Regenerate the radiomap files
          mapHelper.updateFrozenRadioMap(buid, floor_number)
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
        val floor_number = (json \ SCHEMA.fFloor).as[String]
        val buid = (json \ SCHEMA.fBuid).as[String]
        val strRange = (json \ "range").as[String]
        val weight = (json \ SCHEMA.fWeight).as[String]
        val range = strRange.toInt
        try {
          var radioPoints: util.List[JsValue] = null
          if (weight.compareTo("false") == 0) radioPoints = pds.getIDatasource.getRadioHeatmapBBox2(lat, lon, buid, floor_number, range)
          else if (weight.compareTo("true") == 0) radioPoints = pds.getIDatasource.getRadioHeatmapBBox(lat, lon, buid, floor_number, range)
          else if (weight.compareTo("no spatial") == 0) radioPoints = pds.getIDatasource.getRadioHeatmapByBuildingFloor2(lat, lon, buid, floor_number, range)
          if (radioPoints == null)
            return RESPONSE.BAD("Space does not exist or could not be retrieved.")
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
          if (!pds.getIDatasource.deleteRadiosInBox()) {
            return RESPONSE.BAD("Space exists or could not be added")
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
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER()
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        try {
          var space: Space = null
          try {
            json = json.as[JsObject] - SCHEMA.fAccessToken
            space = new Space(json)
          } catch {
            case e: NumberFormatException => return RESPONSE.BAD("Space coordinates are invalid.")
          }
          if (!pds.getIDatasource.addJsonDocument(SCHEMA.cSpaces, space.toGeoJSON())) {
            return RESPONSE.BAD("Space exists or could not be added.")
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
          val stored_space: JsValue = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_space == null) return RESPONSE.BAD("Space does not exist or could not be retrieved.")
          if (!user.hasAccess(stored_space, owner_id)) return RESPONSE.UNAUTHORIZED_USER()
          val space = new Space(stored_space)
          if (!pds.getIDatasource.replaceJsonDocument(SCHEMA.cSpaces, SCHEMA.fBuid, space.getId(), space.appendCoOwners(json)))
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
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER()
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val validation = VALIDATE.fields(json, SCHEMA.fBuid, "new_owner")
        if (validation.failed()) return validation.response()

        val buid = (json \ SCHEMA.fBuid).as[String]
        var newOwner = (json \ "new_owner").as[String]
        newOwner = appendGoogleIdIfNeeded(newOwner)
        try {
          val stored_space = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_space == null) return RESPONSE.BAD("Building does not exist or could not be retrieved!")
          if (!user.hasAccess(stored_space, owner_id)) return RESPONSE.UNAUTHORIZED_USER()
          val space = new Space(stored_space)
          if (!pds.getIDatasource.replaceJsonDocument(SCHEMA.cSpaces, SCHEMA.fBuid, space.getId(), space.changeOwner(newOwner))) return RESPONSE.BAD("Building could not be updated!")
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
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER()
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          var stored_space = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_space == null) return RESPONSE.BAD("Building does not exist or could not be retrieved!")
          if (!user.hasAccess(stored_space, owner_id)) return RESPONSE.UNAUTHORIZED_USER()
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
          if (json.\(SCHEMA.fSpaceType).getOrElse(null) != null) {
            val spaceType = (json \ SCHEMA.fSpaceType).as[String]
            if (SCHEMA.fSpaceTypes.contains(spaceType))
              stored_space = stored_space.as[JsObject] + (SCHEMA.fSpaceType -> JsString(spaceType))
          }
          val space = new Space(stored_space)
          if (!pds.getIDatasource.replaceJsonDocument(SCHEMA.cSpaces, SCHEMA.fBuid, space.getId(), space.toGeoJSON())) return RESPONSE.BAD("Building could not be updated!")
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
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER()
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        if (String(json, SCHEMA.fBuid) == null)
          return RESPONSE.BAD("Buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val stored_space = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_space == null) return RESPONSE.BAD("Building does not exist or could not be retrieved!")
          if (!user.hasAccess(stored_space, owner_id)) return RESPONSE.UNAUTHORIZED_USER()
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        try {
          val deleted = pds.getIDatasource.deleteAllByBuilding(buid)
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
          case e: IOException => return RESPONSE.internal_server_error("500: " + e.getMessage + "] while deleting floor plans." +
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
          val spaces = pds.getIDatasource.getAllBuildings()
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
          var space = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
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
          return RESPONSE.NOT_FOUND("Building not found.")
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
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER()
        try {
          LOG.D3("owner_id = " + owner_id)
          val spaces = pds.getIDatasource.getAllBuildingsByOwner(owner_id)
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
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER()
        try {
          val spaces = pds.getIDatasource.getAllSpaceOwned(owner_id)
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
          val spaces = pds.getIDatasource.getAllBuildingsByBucode(bucode)
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
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER()
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        try {
          val lat = java.lang.Double.parseDouble((json \ SCHEMA.fCoordinatesLat).as[String])
          val lon = java.lang.Double.parseDouble((json \ SCHEMA.fCoordinatesLon).as[String])
          val spaces = pds.getIDatasource.getAllBuildingsNearMe(lat, lon, range, owner_id)
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
   * Retrieve the building Set.
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
          val campus = pds.getIDatasource.getBuildingSet(cuid)
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

          val buildings = new util.ArrayList[JsValue]
          for (b <- buids.asScala) {
            val building = pds.getIDatasource.getFromKey(SCHEMA.cSpaces, SCHEMA.fBuid, b)
            if (building != null) // some buildings are deleted but still exist in buids[] of a campus
              buildings.add(building.as[JsObject] - SCHEMA.fId - SCHEMA.fSchema - SCHEMA.fCoOwners - SCHEMA.fGeometry - SCHEMA.fType - SCHEMA.fOwnerId)
          }

          val res = campus(0).as[JsObject] - SCHEMA.fBuids - SCHEMA.fOwnerId - SCHEMA.fId - SCHEMA.fSchema - SCHEMA.fCampusCuid - SCHEMA.fDescription +
            (SCHEMA.cSpaces -> Json.toJson(buildings.asScala))
          try
            RESPONSE.gzipJsonOk(res.toString)
          catch {
            case ioe: IOException => RESPONSE.OK(res, "Successfully retrieved buildingsSets")
          }
        } catch {
          case e: DatasourceException => RESPONSE.ERROR(e)
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
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody())
          return RESPONSE
            .BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("buildingSetAdd: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fDescription, SCHEMA.fName, SCHEMA.fBuids, SCHEMA.fGreeklish)
        if (checkRequirements != null) return checkRequirements
        var owner_id = user.authorize(apiKey)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id)) - SCHEMA.fAccessToken
        try {
          val cuid = (json \ SCHEMA.fCampusCuid).as[String]
          val campus = pds.getIDatasource.BuildingSetsCuids(cuid)
          if (campus) return RESPONSE.BAD("Building set already exists!")
          else {
            var buildingset: BuildingSet = null
            try {
              buildingset = new BuildingSet(json)
            } catch {
              case e: NumberFormatException =>
                return RESPONSE.BAD("Building coordinates are invalid!")
            }
            if (!pds.getIDatasource.addJsonDocument(SCHEMA.cCampuses, buildingset.addBuids()))
              return RESPONSE.BAD("Building set already exists or could not be added!")
            val res: JsValue = Json.obj(SCHEMA.fCampusCuid -> buildingset.getId())
            return RESPONSE.OK(res, "Successfully added building Set!")
          }
        } catch {
          case e: DatasourceException =>
            return RESPONSE.ERROR(e)
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
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("campusUpdate: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fCampusCuid)
        if (checkRequirements != null) return checkRequirements
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER()
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val cuid = (json \ SCHEMA.fCampusCuid).as[String]
        try {
          var stored_campus = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cCampuses, SCHEMA.fCampusCuid, cuid)
          if (stored_campus == null)
            return RESPONSE.BAD("Campus does not exist or could not be retrieved!")
          if (!isCampusOwner(stored_campus, owner_id))
            return RESPONSE.UNAUTHORIZED("Unauthorized")
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
            LOG.D2(json.toString())
            var buids = (json \ SCHEMA.fBuids).as[List[String]]
            stored_campus = stored_campus.as[JsObject] + (SCHEMA.fBuids -> Json.toJson(buids))
          }
          val campus = new BuildingSet(stored_campus)
          if (!pds.getIDatasource.replaceJsonDocument(SCHEMA.cCampuses, SCHEMA.fCampusCuid, campus.getId(), campus.toGeoJSON()))
            return RESPONSE.BAD("Campus could not be updated!")
          return RESPONSE.OK("Successfully updated campus!")
        } catch {
          case e: DatasourceException =>
            return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def buildingsetAllByOwner = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("buildingsetAllByOwner: " + Utils.stripJson(json))
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER()
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        try {
          val buildingsets = pds.getIDatasource.getAllBuildingsetsByOwner(owner_id)
          val res: JsValue = Json.obj("buildingsets" -> buildingsets)
          try
            RESPONSE.gzipJsonOk(res.toString)
          catch {
            case ioe: IOException =>
              return RESPONSE.OK(res, "Successfully retrieved all buildingsets!")
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
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER()
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val cuid = (json \ SCHEMA.fCampusCuid).as[String]
        try {
          val stored_campus = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cCampuses, SCHEMA.fCampusCuid, cuid)
          if (stored_campus == null)
            return RESPONSE.BAD("Campus does not exist or could not be retrieved!")
          if (!isCampusOwner(stored_campus, owner_id))
            return RESPONSE.UNAUTHORIZED("Unauthorized")
          if (!pds.getIDatasource.deleteFromKey(SCHEMA.cCampuses, SCHEMA.fCampusCuid, cuid))
            return RESPONSE.internal_server_error("500: Failed to delete Campus")
        } catch {
          case e: DatasourceException =>
            return RESPONSE.ERROR(e)
        }
        return RESPONSE.OK("Successfully deleted everything related to building!")
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
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER()
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val stored_space = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_space == null) return RESPONSE.BAD("Building does not exist or could not be retrieved!")
          if (!user.hasAccess(stored_space, owner_id)) return RESPONSE.UNAUTHORIZED_USER()
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        val floor_number = (json \ SCHEMA.fFloorNumber).as[String]
        if (!Floor.checkFloorNumberFormat(floor_number)) return RESPONSE.BAD("Floor number cannot contain whitespace!")
        try {
          json = json.as[JsObject] - SCHEMA.fAccessToken
          val floor = new Floor(json)
          if (!pds.getIDatasource.addJsonDocument(SCHEMA.cFloorplans, Utils.stripJson(floor.toValidMongoJson()))) return RESPONSE.BAD("Floor already exists or could not be added!")
          return RESPONSE.OK("Successfully added floor " + floor_number + "!")
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
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER()
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val stored_space = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_space == null) return RESPONSE.BAD("Building does not exist or could not be retrieved!")
          if (!user.hasAccess(stored_space, owner_id)) return RESPONSE.UNAUTHORIZED_USER()
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        val floor_number = (json \ "fllor_number").as[String]
        if (!Floor.checkFloorNumberFormat(floor_number)) return RESPONSE.BAD("Floor number cannot contain whitespace!")
        try {
          val fuid = Floor.getId(buid, floor_number)
          var stored_floor = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cFloorplans, SCHEMA.fFuid, fuid)
          if (stored_floor == null) return RESPONSE.BAD("Floor does not exist or could not be retrieved!")
          if (json.\(SCHEMA.fIsPublished).getOrElse(null) != null)
            stored_floor = stored_floor.as[JsObject] + (SCHEMA.fIsPublished -> JsString((json \ SCHEMA.fIsPublished).as[String]))
          if (json.\(SCHEMA.fFloorName).getOrElse(null) != null)
            stored_floor = stored_floor.as[JsObject] + (SCHEMA.fFloorName, JsString((json \ SCHEMA.fFloorName).as[String]))
          if (json.\(SCHEMA.fDescription).getOrElse(null) != null)
            stored_floor = stored_floor.as[JsObject] + (SCHEMA.fDescription, JsString((json \ SCHEMA.fDescription).as[String]))
          val floor = new Floor(stored_floor)
          if (!pds.getIDatasource.replaceJsonDocument(SCHEMA.cFloorplans, SCHEMA.fFuid, floor.getId(), floor.toValidMongoJson().toString))
            return RESPONSE.BAD("Floor could not be updated!")
          return RESPONSE.OK("Successfully updated floor!")
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
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER()
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floor_number = (json \ SCHEMA.fFloorNumber).as[String]
        try {
          val stored_space = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_space == null) return RESPONSE.BAD("Building does not exist or could not be retrieved!")
          if (!user.hasAccess(stored_space, owner_id)) return RESPONSE.UNAUTHORIZED_USER()
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        try {
          val deleted = pds.getIDatasource.deleteAllByFloor(buid, floor_number)
          if (deleted == false)
            return RESPONSE.BAD("Some items related to the floor could not be deleted.")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        val filePath = tilerHelper.getFloorPlanFor(buid, floor_number)
        try {
          val floorfile = new File(filePath)
          // CHECK:NN BUGFIX: Fixing floor plan files and directory removal during floor delete
          if (floorfile.exists()) HelperMethods.recDeleteDirFile(floorfile.getParentFile())
        } catch {
          case e: IOException => return RESPONSE.internal_server_error("500: " + e.getMessage + "] while deleting floor plan." +
            "\nAll related information is deleted from the database!")
        }
        return RESPONSE.OK("Successfully deleted everything related to the floor!")
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
          val floors = pds.getIDatasource.floorsByBuildingAsJson(buid)
          val res: JsValue = Json.obj("floors" -> floors.asScala)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case ioe: IOException => return RESPONSE.OK(res, "Successfully retrieved all floors!")
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
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER()
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id)) - SCHEMA.fAccessToken
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val stored_space = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_space == null) return RESPONSE.BAD("Building does not exist or could not be retrieved!")
          if (!user.hasAccess(stored_space, owner_id)) return RESPONSE.UNAUTHORIZED_USER()
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        try {
          val poi = new Poi(json)
          if (!pds.getIDatasource.addJsonDocument(SCHEMA.cPOIS, poi.toGeoJSON())) return RESPONSE.BAD("Poi already exists or could not be added!")
          val res: JsValue = Json.obj(SCHEMA.fPuid -> poi.getId())
          return RESPONSE.OK(res, "Successfully added poi!")
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
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER()
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val puid = (json \ SCHEMA.fPuid).as[String]
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val stored_space = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_space == null) return RESPONSE.BAD("Building does not exist or could not be retrieved!")
          if (!user.hasAccess(stored_space, owner_id)) return RESPONSE.UNAUTHORIZED_USER()
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        try {
          var stored_poi = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cPOIS, SCHEMA.fPuid, puid)
          if (stored_poi == null) return RESPONSE.BAD("Building does not exist or could not be retrieved!")
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
          if (!pds.getIDatasource.replaceJsonDocument(SCHEMA.cPOIS, SCHEMA.fPuid, poi.getId(), poi.toGeoJSON()))
            return RESPONSE.BAD("Poi could not be updated!")
          return RESPONSE.OK("Successfully updated poi!")
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
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER()
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val buid = (json \ SCHEMA.fBuid).as[String]
        val puid = (json \ SCHEMA.fPuid).as[String]
        try {
          val stored_space = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_space == null) return RESPONSE.BAD("Building does not exist or could not be retrieved!")
          if (!user.hasAccess(stored_space, owner_id)) return RESPONSE.UNAUTHORIZED_USER()
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        try {
          val all_items_failed = pds.getIDatasource.deleteAllByPoi(puid)
          if (all_items_failed.size > 0) {
            val res = Json.obj("ids" -> all_items_failed.asScala)
            return RESPONSE.BAD(res, "Some items related to the deleted poi could not be deleted: " +
              all_items_failed.size +
              " items.")
          }
          return RESPONSE.OK("Successfully deleted everything related to the poi!")
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
        val floor_number = (json \ SCHEMA.fFloorNumber).as[String]
        try {
          val stored_space = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_space == null) return RESPONSE.BAD(
            "Space does not exist or could not be retrieved.")
          val pois = pds.getIDatasource.poisByBuildingFloorAsJson(buid, floor_number)
          val res: JsValue = Json.obj(SCHEMA.cPOIS -> pois)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case _: IOException => return RESPONSE.OK(res, "Retrieved pois. floor: " + floor_number)
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
          val stored_space = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_space == null) return RESPONSE.BAD(
            "Space does not exist or could not be retrieved.")
          val pois = pds.getIDatasource.poisByBuildingAsJson(buid)
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
            result = pds.getIDatasource.poisByBuildingAsJson3(buid, letters)
          else if (greeklish.compareTo("true") == 0)
            result = pds.getIDatasource.poisByBuildingAsJson2GR(cuid, letters)
          else
            result = pds.getIDatasource.poisByBuildingAsJson2(cuid, letters)
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
          return RESPONSE.BAD("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val pois = pds.getIDatasource.poisByBuildingIDAsJson(buid)
          val res: JsValue = Json.obj(SCHEMA.cPOIS -> pois)
          try
            RESPONSE.gzipJsonOk(res.toString)
          catch {
            case ioe: IOException =>
              return RESPONSE.OK(res, "Successfully retrieved all pois from buid " + buid + "!")
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
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER()
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val buid = (json \ SCHEMA.fBuid).as[String]
        val buid1 = (json \ SCHEMA.fBuidA).as[String]
        val buid2 = (json \ SCHEMA.fBuidB).as[String]
        try {
          var stored_space = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid1)
          if (stored_space == null) return RESPONSE.BAD("Building does not exist or could not be retrieved!")
          if (!user.hasAccess(stored_space, owner_id)) return RESPONSE.UNAUTHORIZED_USER()
          stored_space = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid2)
          if (stored_space == null) return RESPONSE.BAD("Building does not exist or could not be retrieved!")
          if (!user.hasAccess(stored_space, owner_id)) return RESPONSE.UNAUTHORIZED_USER()
          stored_space = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_space == null) return RESPONSE.BAD("Building does not exist or could not be retrieved!")
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

        if (!pds.getIDatasource.poiByBuidFloorPuid(buid1, (json \ SCHEMA.fFloorA).as[String], (json \ SCHEMA.fPoisA).as[String]))
          return RESPONSE.BAD("pois_a does not exist or could not be retrieved!")
        if (!pds.getIDatasource.poiByBuidFloorPuid(buid2, (json \ SCHEMA.fFloorB).as[String], (json \ SCHEMA.fPoisB).as[String]))
          return RESPONSE.BAD("pois_b does not exist or could not be retrieved!")
        try {
          val weight = calculateWeightOfConnection(pois_a, pois_b)
          json = json.as[JsObject] + (SCHEMA.fWeight -> JsString(java.lang.Double.toString(weight)))
          if (edge_type == Connection.EDGE_TYPE_ELEVATOR || edge_type == Connection.EDGE_TYPE_STAIR) {
          }
          val conn = new Connection(json)
          if (!pds.getIDatasource.addJsonDocument(SCHEMA.cEdges, conn.toValidMongoJson().toString))
            return RESPONSE.BAD("Connection already exists or could not be added!")
          val res: JsValue = Json.obj(SCHEMA.fConCuid -> conn.getId())
          return RESPONSE.OK(res, "Successfully added new connection!")
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
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER()
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val buid1 = (json \ SCHEMA.fBuidA).as[String]
        val buid2 = (json \ SCHEMA.fBuidB).as[String]
        try {
          var stored_space = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid1)
          if (stored_space == null)
            return RESPONSE.BAD("Building does not exist or could not be retrieved!")
          if (!user.hasAccess(stored_space, owner_id)) return RESPONSE.UNAUTHORIZED_USER()
          stored_space = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid2)
          if (stored_space == null)
            return RESPONSE.BAD("Building does not exist or could not be retrieved!")
          if (!user.hasAccess(stored_space, owner_id)) return RESPONSE.UNAUTHORIZED_USER()
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        try {
          val pois_a = (json \ SCHEMA.fPoisA).as[String]
          val pois_b = (json \ SCHEMA.fPoisB).as[String]
          val cuid = Connection.getId(pois_a, pois_b)
          var stored_conn = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cEdges, SCHEMA.fConCuid, cuid)
          if (stored_conn == null)
            return RESPONSE.BAD("Connection does not exist or could not be retrieved!")
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
              return RESPONSE.BAD("Invalid edge type specified.")
            stored_conn = stored_conn.as[JsObject] + (SCHEMA.fEdgeType -> JsString(edge_type))
          }
          val conn = new Connection(stored_conn)
          if (!pds.getIDatasource.replaceJsonDocument(SCHEMA.cEdges, SCHEMA.fConCuid, conn.getId(), conn.toValidMongoJson().toString))
            return RESPONSE.BAD("Connection could not be updated!")
          return RESPONSE.OK("Successfully updated connection!")
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
        LOG.I("AnyplaceMapping::poiDelete(): " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fPoisA, SCHEMA.fPoisB, SCHEMA.fBuidA, SCHEMA.fBuidB)
        if (checkRequirements != null) return checkRequirements
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER()
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val buid1 = (json \ SCHEMA.fBuidA).as[String]
        val buid2 = (json \ SCHEMA.fBuidB).as[String]
        try {
          var stored_space = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid1)
          if (stored_space == null) return RESPONSE.BAD("Building_a does not exist or could not be retrieved!")
          if (!user.hasAccess(stored_space, owner_id)) return RESPONSE.UNAUTHORIZED_USER()
          stored_space = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid2)
          if (stored_space == null) return RESPONSE.BAD("Building_b does not exist or could not be retrieved!")
          if (!user.hasAccess(stored_space, owner_id)) return RESPONSE.UNAUTHORIZED_USER()
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        val pois_a = (json \ SCHEMA.fPoisA).as[String]
        val pois_b = (json \ SCHEMA.fPoisB).as[String]
        try {
          val cuid = Connection.getId(pois_a, pois_b)
          val all_items_failed = pds.getIDatasource.deleteAllByConnection(cuid)
          if (all_items_failed == null) {
            LOG.I("AnyplaceMapping::connectionDelete(): " + cuid + " not found.")
            return RESPONSE.BAD("POI Connection not found")
          }
          if (all_items_failed.size > 0) {
            val obj: JsValue = Json.obj("ids" -> all_items_failed.asScala)
            return RESPONSE.BAD(obj, "Some items related to the deleted connection could not be deleted: " +
              all_items_failed.size + " items.")
          }
          return RESPONSE.OK("Successfully deleted everything related to the connection!")
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
        val floor_number = (json \ SCHEMA.fFloorNumber).as[String]
        try {
          val stored_space = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_space == null) return RESPONSE.BAD("Building does not exist or could not be retrieved!")
          val stored_floors = pds.getIDatasource.floorsByBuildingAsJson(buid)
          var floorExists = false
          for (floor <- stored_floors.asScala)
            if ((floor \ SCHEMA.fFloorNumber).as[String] == floor_number)
              floorExists = true
          if (!floorExists) return RESPONSE.BAD("Floor does not exist or could not be retrieved!")

          val pois = pds.getIDatasource.connectionsByBuildingFloorAsJson(buid, floor_number)
          val res: JsValue = Json.obj("connections" -> pois)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case ioe: IOException => return RESPONSE.OK(res, "Successfully retrieved all pois from floor " + floor_number +
              "!")
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
          val stored_space = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (stored_space == null) return RESPONSE.BAD("Building does not exist or could not be retrieved!")
          val pois = pds.getIDatasource.connectionsByBuildingAllFloorsAsJson(buid)
          val res: JsValue = Json.obj("connections" -> pois)
          try
            RESPONSE.gzipJsonOk(res.toString)
          catch {
            case ioe: IOException =>
              return RESPONSE.OK(res, "Successfully retrieved all pois from all floors !")
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
    val pa = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cPOIS, SCHEMA.fPuid, pois_a)
    if (pa == null) {
      lat_a = 0.0
      lon_a = 0.0
    } else try {
      lat_a = nf.parse((pa \ SCHEMA.fCoordinatesLat).as[String]).doubleValue()
      lon_a = nf.parse((pa \ SCHEMA.fCoordinatesLon).as[String]).doubleValue()
    } catch {
      case e: ParseException => e.printStackTrace()
    }
    val pb = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cPOIS, SCHEMA.fPuid, pois_b)
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
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("serveFloorPlanBinary: " + Utils.stripJson(json))
        val filePath = tilerHelper.getFloorPlanFor(buid, floor_number)
        LOG.D2("requested: " + filePath)
        try {
          val file = new File(filePath)
          // LPLogger.debug("filePath " + file.getAbsolutePath.toString)
          if (!file.exists()) return RESPONSE.BAD("Floor plan does not exist: " + floor_number)
          if (!file.canRead()) return RESPONSE.BAD("Floor plan cannot be read: " + floor_number)
          Ok.sendFile(file)
        } catch {
          case e: FileNotFoundException => return RESPONSE.internal_server_error("Could not read floor plan.")
        }
      }

      inner(request)
  }

  def serveFloorPlanTilesZip(buid: String, floor_number: String) = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.I("AnyplaceMapping::serveFloorPlanTilesZip(): " + Utils.stripJson(json))
        if (!Floor.checkFloorNumberFormat(floor_number)) return RESPONSE.BAD("Floor number cannot contain whitespace!")
        val filePath = tilerHelper.getFloorTilesZipFor(buid, floor_number)
        LOG.I("requested: " + filePath)
        try {
          val file = new File(filePath)
          if (!file.exists()) return RESPONSE.BAD("Requested floor plan does not exist");
          if (!file.canRead()) return RESPONSE.BAD("Requested floor plan cannot be read: " +
            floor_number)
          Ok.sendFile(file)
        } catch {
          case e: FileNotFoundException => return RESPONSE.internal_server_error("Could not read floor plan.")
        }
      }

      inner(request)
  }

  def serveFloorPlanTilesZipLink(buid: String, floor_number: String) = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.I("AnyplaceMapping::serveFloorPlanTilesZipLink(): " + Utils.stripJson(json))
        if (!Floor.checkFloorNumberFormat(floor_number)) return RESPONSE.BAD("Floor number cannot contain whitespace!")
        val filePath = tilerHelper.getFloorTilesZipFor(buid, floor_number)
        LOG.I("requested: " + filePath)
        val file = new File(filePath)
        if (!file.exists()) return RESPONSE.BAD("Requested floor plan does not exist");
        if (!file.canRead()) return RESPONSE.BAD("Requested floor plan cannot be read: " +
          floor_number)
        val res: JsValue = Json.obj("tiles_archive" -> tilerHelper.getFloorTilesZipLinkFor(buid, floor_number))
        return RESPONSE.OK(res, "Successfully fetched link for the tiles archive!")
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
      filePath = if (path == tilerHelper.FLOOR_TILES_ZIP_NAME) tilerHelper.getFloorTilesZipFor(buid,
        floor_number) else tilerHelper.getFloorTilesDirFor(buid, floor_number) +
        path
      try {
        val file = new File(filePath)
        //send ok message to tiler
        if (!file.exists() || !file.canRead()) return RESPONSE.OK("File requested not found")
        Ok.sendFile(file)
      } catch {
        case e: FileNotFoundException => return RESPONSE.internal_server_error("Could not read floor plan.")
      }
    }

    inner()
  }

  def serveFloorPlanBase64(buid: String, floor_number: String) = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("serveFloorPlanBase64: " + Utils.stripJson(json))
        val filePath = tilerHelper.getFloorPlanFor(buid, floor_number)
        LOG.D2("requested: " + filePath)
        val file = new File(filePath)
        try {
          if (!file.exists()) return RESPONSE.BAD("Requested floor plan does not exist");
          if (!file.canRead()) return RESPONSE.BAD("Requested floor plan cannot be read: " +
            floor_number)

          try {
            val s = Utils.encodeFileToBase64Binary(fu, filePath)
            try {
              RESPONSE.gzipOk(s)
            } catch {
              case ioe: IOException => Ok(s)
            }
          } catch {
            case e: IOException => return RESPONSE.BAD("Requested floor plan cannot be encoded in base64 properly! (" +
              floor_number +
              ")")
          }
        } catch {
          case e: Exception => return RESPONSE.internal_server_error("Unknown server error during floor plan delivery!")
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
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("serveFloorPlanBase64all: " + Utils.stripJson(json) + " " + floor_number)
        val floors = floor_number.split(" ")
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
                return RESPONSE.BAD("Requested floor plan cannot be encoded in base64 properly: " + floors(z))
            }
          catch {
            case e: Exception =>
              return RESPONSE.internal_server_error("Unknown server error during floor plan delivery.")
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
            return RESPONSE.OK(res, "Successfully retrieved all floors!")
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
        if (body == null) return RESPONSE.BAD("Invalid request type - Not Multipart!")
        var floorplan = body.file("floorplan").get
        if (floorplan == null) return RESPONSE.BAD("Cannot find the floor plan file in your request!")
        val urlenc = body.asFormUrlEncoded
        val json_str = urlenc.get("json").get.head // CHECK:NN get("json").get(0)
        if (json_str == null) return RESPONSE.BAD("Cannot find json in the request.")
        var json: JsValue = null
        try {
          json = Json.parse(json_str)
        } catch {
          case e: IOException => return RESPONSE.BAD("Cannot parse json in the request.")
        }
        LOG.I("Floorplan Request[json]: " + json.toString)
        LOG.I("Floorplan Request[floorplan]: " + floorplan.filename)
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fFloorNumber, SCHEMA.fLatBottomLeft,
          SCHEMA.fLonBottomLeft, SCHEMA.fLatTopRight, SCHEMA.fLonTopRight)
        if (!requiredMissing.isEmpty) return RESPONSE.MISSING_FIELDS(requiredMissing)
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floor_number = (json \ SCHEMA.fFloorNumber).as[String]
        val bottom_left_lat = (json \ SCHEMA.fLatBottomLeft).as[String]
        val bottom_left_lng = (json \ SCHEMA.fLonBottomLeft).as[String]
        val top_right_lat = (json \ SCHEMA.fLatTopRight).as[String]
        val top_right_lng = (json \ SCHEMA.fLonTopRight).as[String]
        val fuid = Floor.getId(buid, floor_number)
        try {
          var stored_floor = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cFloorplans, SCHEMA.fFuid, fuid)
          if (stored_floor == null) return RESPONSE.BAD("Floor does not exist or could not be retrieved!")
          stored_floor = stored_floor.as[JsObject] + (SCHEMA.fLatBottomLeft -> JsString(bottom_left_lat))
          stored_floor = stored_floor.as[JsObject] + (SCHEMA.fLonBottomLeft -> JsString(bottom_left_lng))
          stored_floor = stored_floor.as[JsObject] + (SCHEMA.fLatTopRight -> JsString(top_right_lat))
          stored_floor = stored_floor.as[JsObject] + (SCHEMA.fLonTopRight -> JsString(top_right_lng))
          if (!pds.getIDatasource.replaceJsonDocument(SCHEMA.cFloorplans, SCHEMA.fFuid, fuid, stored_floor.toString))
            return RESPONSE.BAD("Floor plan could not be updated in the database!")
        } catch {
          case e: DatasourceException => return RESPONSE.internal_server_error("Error while reading from our backend service!")
        }
        var floor_file: File = null
        try {
          floor_file = tilerHelper.storeFloorPlanToServer(buid, floor_number, floorplan.ref.file)
        } catch {
          case e: AnyPlaceException => return RESPONSE.BAD("Cannot save floor plan on the server!")
        }
        val top_left_lat = top_right_lat
        val top_left_lng = bottom_left_lng
        try {
          tilerHelper.tileImage(floor_file, top_left_lat, top_left_lng)
        } catch {
          case e: AnyPlaceException => return RESPONSE.BAD("Could not create floor plan tiles on the server!")
        }
        LOG.I("Successfully tiled: " + floor_file.toString)
        return RESPONSE.OK("Successfully updated floor plan!")
      }

      inner(request)
  }

  /**
   * After a floor was added, this endpoints:
   *    1. uploads a floorplan (filesystem)
   *    2. updates the floor with the coordinates of the floor plan (db)
   */
  def floorPlanUploadWithZoom() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        LOG.D2("floorPlanUploadWithZoom")
        val anyReq = new OAuth2Request(request)
        val body = anyReq.getMultipartFormData()
        if (body == null) return RESPONSE.BAD("Invalid request type - Not Multipart!")
        val floorplan = body.file("floorplan").get
        if (floorplan == null) return RESPONSE.BAD("Cannot find the floor plan file in your request!")
        val urlenc = body.asFormUrlEncoded
        val json_str = urlenc.get("json").get(0)
        if (json_str == null) return RESPONSE.BAD("Cannot find json in the request!")
        var json: JsValue = null
        try {
          json = Json.parse(json_str)
        } catch {
          case e: IOException => return RESPONSE.BAD("Cannot parse json in the request!")
        }
        //LPLogger.info("Floorplan Request[json]: " + json.toString)
        //LPLogger.info("Floorplan Request[floorplan]: " + floorplan.filename)
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid, SCHEMA.fFloorNumber, SCHEMA.fLatBottomLeft,
          SCHEMA.fLonBottomLeft, SCHEMA.fLatTopRight, SCHEMA.fLonTopRight, SCHEMA.fZoom)
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        val zoom = (json \ SCHEMA.fZoom).as[String]
        val zoom_number = zoom.toInt
        if (zoom_number < 20)
          return RESPONSE.BAD("You have provided zoom level " + zoom + ". You have to zoom at least to level 20 to upload the floorplan.")
        val floor_number = (json \ SCHEMA.fFloorNumber).as[String]
        val bottom_left_lat = (json \ SCHEMA.fLatBottomLeft).as[String]
        val bottom_left_lng = (json \ SCHEMA.fLonBottomLeft).as[String]
        val top_right_lat = (json \ SCHEMA.fLatTopRight).as[String]
        val top_right_lng = (json \ SCHEMA.fLonTopRight).as[String]
        val fuid = Floor.getId(buid, floor_number)
        try {
          var stored_floor = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cFloorplans, SCHEMA.fFuid, fuid)
          if (stored_floor == null) return RESPONSE.BAD("Floor does not exist or could not be retrieved!")
          stored_floor = stored_floor.as[JsObject] + (SCHEMA.fZoom -> JsString(zoom))
          stored_floor = stored_floor.as[JsObject] + (SCHEMA.fLatBottomLeft -> JsString(bottom_left_lat))
          stored_floor = stored_floor.as[JsObject] + (SCHEMA.fLonBottomLeft -> JsString(bottom_left_lng))
          stored_floor = stored_floor.as[JsObject] + (SCHEMA.fLatTopRight -> JsString(top_right_lat))
          stored_floor = stored_floor.as[JsObject] + (SCHEMA.fLonTopRight -> JsString(top_right_lng))
          if (!pds.getIDatasource.replaceJsonDocument(SCHEMA.cFloorplans, SCHEMA.fFuid, fuid, stored_floor.toString)) {
            return RESPONSE.BAD("Floor plan could not be updated in the database!")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.internal_server_error("Error while reading from our backend service!")
        }
        var floor_file: File = null
        try {
          floor_file = tilerHelper.storeFloorPlanToServer(buid, floor_number, floorplan.ref.path.toFile)
        } catch {
          case e: AnyPlaceException => return RESPONSE.BAD("Cannot save floor plan on the server!")
        }
        val top_left_lat = top_right_lat
        val top_left_lng = bottom_left_lng
        try {
          tilerHelper.tileImageWithZoom(floor_file, top_left_lat, top_left_lng, zoom)
        } catch {
          case e: AnyPlaceException => return RESPONSE.BAD("Could not create floor plan tiles on the server!")
        }
        LOG.I("Successfully tiled: " + floor_file.toString)
        return RESPONSE.OK("Successfully updated floor plan!")
      }

      inner(request)
  }

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
          if (!pds.getIDatasource.deleteNotValidDocuments()) return RESPONSE.BAD("None valid documents.")
          return RESPONSE.OK("Success")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  // CHECK:NN
  private def getRadioMapMeanByBuildingFloor(buid: String, floor_number: String): Option[RadioMapMean] = {
    val radioMapsFrozenDir = conf.get[String]("radioMapFrozenDir")
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
    LOG.D5(radio.toPath.getFileName.toString)
    var floorFetched: Long = 0L
    floorFetched = pds.getIDatasource.dumpRssLogEntriesByBuildingACCESFloor(fout, buid, floor_number)
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
    return Option[RadioMapMean](rm_mean)
  }

  // CHECK:NN
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
  //    floorFetched = pds.getIDatasource.dumpRssLogEntriesByBuildingFloor(fout, buid, floor_number)
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


  // CLR:NN
  //@deprecated("Removing acces")
  //def getAccesHeatmapByBuildingFloor() = Action {
  //  implicit request =>
  //
  //    def inner(request: Request[AnyContent]): Result = {
  //      val anyReq = new OAuth2Request(request)
  //      if (!anyReq.assertJsonBody()) {
  //        LPLogger.info("getAccesHeatmapByBuildingFloor: assert json anyreq")
  //        return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
  //      }
  //      val json = anyReq.getJsonBody()
  //      LPLogger.info("getAccesHeatmapByBuildingFloor(): " + Utils.stripJson(json))
  //      val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fFloor, SCHEMA.fBuid)
  //      if (!requiredMissing.isEmpty) {
  //        return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
  //      }
  //      val floor_number = (json \ SCHEMA.fFloor).as[String]
  //      val buid = (json \ SCHEMA.fBuid).as[String]
  //      val cut_k_features = (json \ "cut_k_features").asOpt[Int]
  //      //Default 5 meter grid step
  //      val h = (json \ "h").asOpt[Double].getOrElse(5.0)
  //
  //      if (!Floor.checkFloorNumberFormat(floor_number)) {
  //        return AnyResponseHelper.bad_request("Floor number cannot contain whitespace!")
  //      }
  //      try {
  //        val rm = getRadioMapMeanByBuildingFloor(buid = buid, floor_number = floor_number)
  //        if (rm.isEmpty) {
  //          return AnyResponseHelper.bad_request("Area not supported yet!")
  //        } else {
  //          val (latlon_predict, crlbs) = getAccesMap(rm = rm.get, buid = buid, floor_number = floor_number,
  //            cut_k_features = cut_k_features, h = h)
  //          if (latlon_predict == null) {
  //
  //            // TODO:PM : update application.conf
  //            val crlb_filename = Play.application().configuration().getString("crlbsDir") +
  //              File.separatorChar + buid + File.separatorChar + "fl_" + floor_number + ".txt"
  //            val crlb_filename_lock = crlb_filename + ".lock"
  //            val lockInstant =
  //              Files.getLastModifiedTime(Paths.get(crlb_filename_lock)).toInstant
  //            val requestExpired = lockInstant.
  //              plus(ACCES_RETRY_AMOUNT, ACCES_RETRY_UNIT) isBefore Instant.now
  //            var msg = ""
  //            if (requestExpired) {
  //              // TODO if ACCES generation happens asynchronously we can skip the extra step
  //              // This is just to show a warning message to the user.
  //              val file_lock = new File(crlb_filename_lock)
  //              file_lock.delete()
  //              msg = "Generating ACCES has previously failed. Please retry."
  //            } else {
  //              msg = "Generating ACCES map in another background thread!"
  //            }
  //
  //            return AnyResponseHelper.bad_request(msg)
  //          }
  //
  //          val res = JsonObject.empty()
  //          res.put("geojson", JsonObject.fromJson(latlon_predict.toGeoJSON().toString))
  //          res.put("crlb", JsonArray.from(new util.ArrayList[Double](crlbs.toArray.asScala)))
  //          return AnyResponseHelper.ok(res, "Successfully served ACCES map.")
  //        }
  //      } catch {
  //        case e: FileNotFoundException => return AnyResponseHelper.internal_server_error(
  //          "Cannot create radiomap:mapping:FNFE:" + e.getMessage)
  //        case e: DatasourceException => return AnyResponseHelper.ERROR(e)
  //        case e: IOException => return AnyResponseHelper.internal_server_error(
  //          "Cannot create radiomap:IOE:" + e.getMessage)
  //        case e: Exception => return AnyResponseHelper.ERROR(e)
  //        case _: Throwable => return AnyResponseHelper.internal_server_error("500: ")
  //      }
  //    }
  //
  //    inner(request)
  //}



  // CLR:NN
  //@deprecated("Removing acces")
  //def getAccesHeatmapByBuildingFloor() = Action {
  //  implicit request =>
  //
  //    def inner(request: Request[AnyContent]): Result = {
  //      val anyReq = new OAuth2Request(request)
  //      if (!anyReq.assertJsonBody()) {
  //        LPLogger.info("getAccesHeatmapByBuildingFloor: assert json anyreq")
  //        return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
  //      }
  //      val json = anyReq.getJsonBody()
  //      LPLogger.info("getAccesHeatmapByBuildingFloor(): " + Utils.stripJson(json))
  //      val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fFloor, SCHEMA.fBuid)
  //      if (!requiredMissing.isEmpty) {
  //        return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
  //      }
  //      val floor_number = (json \ SCHEMA.fFloor).as[String]
  //      val buid = (json \ SCHEMA.fBuid).as[String]
  //      val cut_k_features = (json \ "cut_k_features").asOpt[Int]
  //      //Default 5 meter grid step
  //      val h = (json \ "h").asOpt[Double].getOrElse(5.0)
  //
  //      if (!Floor.checkFloorNumberFormat(floor_number)) {
  //        return AnyResponseHelper.bad_request("Floor number cannot contain whitespace!")
  //      }
  //      try {
  //        val rm = getRadioMapMeanByBuildingFloor(buid = buid, floor_number = floor_number)
  //        if (rm.isEmpty) {
  //          return AnyResponseHelper.bad_request("Area not supported yet!")
  //        } else {
  //          val (latlon_predict, crlbs) = getAccesMap(rm = rm.get, buid = buid, floor_number = floor_number,
  //            cut_k_features = cut_k_features, h = h)
  //          if (latlon_predict == null) {
  //
  //            // TODO:PM : update application.conf
  //            val crlb_filename = Play.application().configuration().getString("crlbsDir") +
  //              File.separatorChar + buid + File.separatorChar + "fl_" + floor_number + ".txt"
  //            val crlb_filename_lock = crlb_filename + ".lock"
  //            val lockInstant =
  //              Files.getLastModifiedTime(Paths.get(crlb_filename_lock)).toInstant
  //            val requestExpired = lockInstant.
  //              plus(ACCES_RETRY_AMOUNT, ACCES_RETRY_UNIT) isBefore Instant.now
  //            var msg = ""
  //            if (requestExpired) {
  //              // TODO if ACCES generation happens asynchronously we can skip the extra step
  //              // This is just to show a warning message to the user.
  //              val file_lock = new File(crlb_filename_lock)
  //              file_lock.delete()
  //              msg = "Generating ACCES has previously failed. Please retry."
  //            } else {
  //              msg = "Generating ACCES map in another background thread!"
  //            }
  //
  //            return AnyResponseHelper.bad_request(msg)
  //          }
  //
  //          val res = JsonObject.empty()
  //          res.put("geojson", JsonObject.fromJson(latlon_predict.toGeoJSON().toString))
  //          res.put("crlb", JsonArray.from(new util.ArrayList[Double](crlbs.toArray.asScala)))
  //          return AnyResponseHelper.ok(res, "Successfully served ACCES map.")
  //        }
  //      } catch {
  //        case e: FileNotFoundException => return AnyResponseHelper.internal_server_error(
  //          "Cannot create radiomap:mapping:FNFE:" + e.getMessage)
  //        case e: DatasourceException => return AnyResponseHelper.ERROR(e)
  //        case e: IOException => return AnyResponseHelper.internal_server_error(
  //          "Cannot create radiomap:IOE:" + e.getMessage)
  //        case e: Exception => return AnyResponseHelper.ERROR(e)
  //        case _: Throwable => return AnyResponseHelper.internal_server_error("500: ")
  //      }
  //    }
  //
  //    inner(request)
  //}
}
