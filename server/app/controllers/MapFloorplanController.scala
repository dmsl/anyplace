package controllers

import datasources.{DatasourceException, ProxyDataSource, SCHEMA}
import models._
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
class MapFloorplanController @Inject()(cc: ControllerComponents,
                                       tilerHelper: AnyPlaceTilerHelper,
                                       pds: ProxyDataSource,
                                       fu: FileUtils)
  extends AbstractController(cc) {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  def serveFloorPlanBinary(buid: String, floorNum: String): Action[AnyContent] = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("serveFloorPlanBinary: " + Utils.stripJsValueStr(json))
        val filePath = tilerHelper.getFloorPlanFor(buid, floorNum)
        LOG.D2("requested: " + filePath)
        try {
          val file = new File(filePath)
          if (!file.exists()) return RESPONSE.BAD_CANNOT_RETRIEVE_FLOORPLAN(floorNum)
          if (!file.canRead) return RESPONSE.BAD_CANNOT_READ_FLOORPLAN(floorNum)
          Ok.sendFile(file)
        } catch {
          case _: FileNotFoundException => return RESPONSE.ERROR_INTERNAL("Could not read floorplan.")
        }
      }

      inner(request)
  }

  def getTilesZip(buid: String, floorNum: String): Action[AnyContent] = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("FloorPlan: getTilesZip: " + Utils.stripJsValueStr(json))
        if (!Floor.checkFloorNumberFormat(floorNum)) return RESPONSE.BAD("Floor number cannot contain whitespace.")
        val filePath = tilerHelper.getFloorTilesZipFor(buid, floorNum)
        LOG.D3("requested: " + filePath)
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

  def getZipLink(buid: String, floorNum: String): Action[AnyContent] = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("Floorplan:: getZipLink: " + Utils.stripJsValueStr(json))
        if (!Floor.checkFloorNumberFormat(floorNum)) return RESPONSE.BAD("Floor number cannot contain whitespace.")
        val filePath = tilerHelper.getFloorTilesZipFor(buid, floorNum)
        LOG.D3("requested: " + filePath)
        val file = new File(filePath)
        if (!file.exists()) return RESPONSE.BAD_CANNOT_RETRIEVE_FLOORPLAN(floorNum)
        if (!file.canRead) return RESPONSE.BAD_CANNOT_READ_FLOORPLAN(floorNum)
        val res: JsValue = Json.obj("tiles_archive" -> tilerHelper.getFloorTilesZipLinkFor(buid, floorNum))
        return RESPONSE.OK(res, "Successfully fetched link for the tiles archive.")
      }

      inner(request)
  }

  def getStaticTiles(buid: String, floorNum: String, path: String): Action[AnyContent] = Action {
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
        if (!file.exists() || !file.canRead) return RESPONSE.OK("File requested not found")
        Ok.sendFile(file)
      } catch {
        case _: FileNotFoundException => return RESPONSE.BAD_CANNOT_READ_FLOORPLAN(floorNum)
      }
    }

    inner()
  }

  def getBase64(buid: String, floorNum: String): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("Floorplan: getBase64: " + Utils.stripJsValueStr(json))
        val filePath = tilerHelper.getFloorPlanFor(buid, floorNum)
        LOG.D3("Floorplan: getBase64: requested: " + filePath)
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
  def getAllBase64(buid: String, floorNum: String): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("Floorplan: getAllBase64: " + Utils.stripJsValueStr(json) + " " + floorNum)
        val floors = floorNum.split(" ")
        val all_floors = new util.ArrayList[String]
        var z = 0
        while (z < floors.length) {
          val filePath = tilerHelper.getFloorPlanFor(buid, floors(z))
          LOG.D3("Floorplan: getAllBase64: requested: " + filePath)
          val file = new File(filePath)
          try
            if (!file.exists || !file.canRead) { all_floors.add("") }
            else try {
              val s = Utils.encodeFileToBase64Binary(fu, filePath)
              all_floors.add(s)
            } catch {
              case _: IOException =>
                return RESPONSE.BAD("Requested floorplan cannot be encoded in base64 properly: " + floors(z))
            }
          catch {
            case _: Exception =>
              return RESPONSE.ERROR_INTERNAL("Unknown server error during floorplan delivery.")
          }
          z += 1
        }
        val res: JsValue = Json.obj("all_floors" -> all_floors.asScala)
        try
          RESPONSE.gzipJsonOk(res.toString)
        catch {
          case _: IOException =>
            return RESPONSE.OK(res, "Successfully retrieved all floors.")
        }
      }

      inner(request)
  }

  @deprecated("NotInUse")
  def upload(): Action[AnyContent] = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val body = anyReq.getMultipartFormData()
        if (body == null) return RESPONSE.BAD("Invalid request type - Not Multipart.")
        val floorplan = body.file("floorplan").get
        if (floorplan == null) return RESPONSE.BAD("Cannot find the floorplan file in your request.")
        val urlenc = body.asFormUrlEncoded
        val json_str = urlenc("").head
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
          case _: DatasourceException => return RESPONSE.ERROR_INTERNAL("Error while reading from our backend service.")
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
  def uploadWithZoom(): Action[AnyContent] = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        LOG.D2("Floorplan: uploadWithZoom")
        val anyReq = new OAuth2Request(request)
        val body = anyReq.getMultipartFormData()
        if (body == null) return RESPONSE.BAD("Invalid request type - Not Multipart.")
        val floorplan = body.file("floorplan").get
        if (floorplan == null) return RESPONSE.BAD("Cannot find the floorplan file in your request.")
        val urlenc = body.asFormUrlEncoded
        val json_str = urlenc("json").head
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
          case _: AnyPlaceException => return RESPONSE.BAD("Could not create floorplan tiles on the server.")
        }
        LOG.I("Successfully tiled: " + floor_file.toString)
        return RESPONSE.OK("Successfully updated floorplan.")
      }

      inner(request)
  }

}
