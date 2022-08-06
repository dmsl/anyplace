package controllers

import datasources.{DatasourceException, ProxyDataSource, SCHEMA}
import models._
import models.oauth.OAuth2Request
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc._
import utils.json.VALIDATE
import utils.json.VALIDATE.String
import utils._
import java.io._
import javax.inject.{Inject, Singleton}
import scala.jdk.CollectionConverters.CollectionHasAsScala
@Singleton
class MapPoiController @Inject()(cc: ControllerComponents,
                                           pds: ProxyDataSource,
                                           user: helper.User)
  extends AbstractController(cc) {

  def add(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)

        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()

        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("POI: add: " + Utils.stripJsValueStr(json))
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
          if (!user.canAccessSpace(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        try {
          val poi = new Poi(json)
          if (!pds.db.addJson(SCHEMA.cPOIS, poi.toGeoJson())) return RESPONSE.BAD_CANNOT_RETRIEVE_POI
          val res: JsValue = Json.obj(SCHEMA.fPuid -> poi.getId())
          return RESPONSE.OK(res, "Successfully added POI.")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def update(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("POI: update: " + Utils.stripJsValueStr(json))
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
          if (!user.canAccessSpace(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
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
          if (!pds.db.replaceJsonDocument(SCHEMA.cPOIS, SCHEMA.fPuid, poi.getId(), poi.toGeoJsonStr()))
            return RESPONSE.BAD("Poi could not be updated.")
          return RESPONSE.OK("Successfully updated poi.")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  def delete(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("POI: delete: " + Utils.stripJsValueStr(json))
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
          if (!user.canAccessSpace(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
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

  def byFloor(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("POI: byFloor: " + Utils.stripJsValueStr(json))
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

  def bySpace(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("POI: bySpace: " + Utils.stripJsValueStr(json))
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
  def search: Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        LOG.D2("POI: search")
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
  def byConnectors: Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.I("POI: byConnectors" + Utils.stripJsValueStr(json))
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
              return RESPONSE.OK(res, "Retrieved all POIs from space: " + buid)
          }
        } catch {
          case e: DatasourceException =>
            return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

}
