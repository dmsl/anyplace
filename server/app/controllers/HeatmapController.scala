package controllers

import datasources.{DatasourceException, ProxyDataSource, SCHEMA}
import models.oauth.OAuth2Request
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc._
import utils._
import utils.json.VALIDATE

import java.io._
import java.util
import javax.inject.{Inject, Singleton}
import scala.jdk.CollectionConverters.CollectionHasAsScala
@Singleton
class HeatmapController @Inject()(cc: ControllerComponents,
                                   pds: ProxyDataSource)
  extends AbstractController(cc) {

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

  def floorWifiAVG1(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("Heatmap: floorWifiAVG1: " + Utils.stripJsValueStr(json))
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

  def floorWifiAVG2(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("Heatmap: floorWifiAVG2: " + Utils.stripJsValueStr(json))
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
  def floorWifiAVG3(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("Heatmap: floorWifiAVG3: " + Utils.stripJsValueStr(json))
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
  def floorWifiAVG3tiles(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("Heatmap: floorWifiAVG3tiles: " + Utils.stripJsValueStr(json))
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

  def floorWifiTimestampAVG1(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("Heatmap: floorWifiTimestampAVG1: " + Utils.stripJsValueStr(json))
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

  def floorWifiTimestampAVG2(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("Heatmap: floorWifiTimestampAVG2: " + Utils.stripJsValueStr(json))
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

  /**
   * We use AVG2 on level3 because we dont want to show the tiles (individual radio points)
   *
   * Called by crossfilter when on zoom level 21.
   */
  @deprecated("notInUse")
  def floorWifiTimestampAVG3(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("Heatmap: floorWifiTimestampAVG3: " + Utils.stripJsValueStr(json))
        val checkRequirements = VALIDATE.checkRequirements(json,
          SCHEMA.fFloor, SCHEMA.fBuid, SCHEMA.fTimestampX, SCHEMA.fTimestampY)
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
          case e: DatasourceException => return RESPONSE.ERROR("floorWifiTimestampAVG3", e)
        }
      }

      inner(request)
  }

  /**
   * Called by crossfilter when on maximum zoom level (22).
   * Called many times from clients, for each tile.
   */
  def floorWifiTimestampTiles(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D3("Heatmap: floorWifiTimestampTiles: " + Utils.stripJsValueStr(json))
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

}
