package controllers

import datasources.{DatasourceException, ProxyDataSource, SCHEMA}
import models._
import models.oauth.OAuth2Request
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc._
import utils.json.VALIDATE
import utils._

import java.io._
import java.text.{NumberFormat, ParseException}
import java.util.Locale
import javax.inject.{Inject, Singleton}
import scala.jdk.CollectionConverters.CollectionHasAsScala
@Singleton
class MapPoiConnectionController @Inject()(cc: ControllerComponents,
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
        LOG.D2("PoiConnection: add: " + Utils.stripJsValueStr(json))
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
          if (!user.canAccessSpace(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
          storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid2)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          if (!user.canAccessSpace(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
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
          val weight = getConnectionWeight(pois_a, pois_b)
          json = json.as[JsObject] + (SCHEMA.fWeight -> JsString(java.lang.Double.toString(weight)))
          if (edge_type == Connection.EDGE_TYPE_ELEVATOR || edge_type == Connection.EDGE_TYPE_STAIR) {
          }
          val conn = new Connection(json)
          if (!pds.db.addJson(SCHEMA.cEdges, conn.toJson()))
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
  def connectionUpdate(): Action[AnyContent] = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("PoiConnection: update: " + Utils.stripJsValueStr(json))
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
          if (!user.canAccessSpace(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
          storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid2)
          if (storedSpace == null)
            return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          if (!user.canAccessSpace(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
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

  def delete(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {

        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("PoiConnection: delete: " + Utils.stripJsValueStr(json))
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
          if (!user.canAccessSpace(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
          storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid2)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE("SpaceB")
          if (!user.canAccessSpace(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
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

  def byFloor(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("PoiConnection: byFloor: " + Utils.stripJsValueStr(json))
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

  /**
   * Retrieve all the pois of a building/floor combination.
   *
   * @return
   */
  def byFloorsAll: Action[AnyContent] = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.I("PoiConnection: byFloorsAll: " + Utils.stripJsValueStr(json))
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

  private def getConnectionWeight(pois_a: String, pois_b: String) = {
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
      case e: ParseException => LOG.E("getConnectionWeight", e)
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
}