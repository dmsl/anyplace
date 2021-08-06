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
class MapCampusController @Inject()(cc: ControllerComponents,
                                   pds: ProxyDataSource,
                                   user: helper.User)
  extends AbstractController(cc) {

  /**
   * Returns a campus.
   */
  def get: Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("Campus: get")
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fCampusCuid)
        if (checkRequirements != null) return checkRequirements
        val cuid = (json \ SCHEMA.fCampusCuid).as[String]
        try {
          val campus = pds.db.getBuildingSet(cuid)
          if (campus.isEmpty) {
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

          val res = campus.head.as[JsObject] - SCHEMA.fBuids - SCHEMA.fOwnerId - SCHEMA.fId - SCHEMA.fSchema - SCHEMA.fCampusCuid - SCHEMA.fDescription +
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
  def add: Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("Campus: add")
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fDescription, SCHEMA.fName, SCHEMA.fBuids, SCHEMA.fGreeklish)
        if (checkRequirements != null) return checkRequirements
        var owner_id = user.authorize(apiKey)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id)) - SCHEMA.fAccessToken
        try {
          val cuid = (json \ SCHEMA.fCampusCuid).as[String]
          val campus = pds.db.BuildingSetsCuids(cuid)
          if (campus) return RESPONSE.BAD("Campus already exists.")
          else {
            var spaceSet: Campus = null
            try {
              spaceSet = new Campus(json)
            } catch {
              case _: NumberFormatException => return RESPONSE.BAD("Space coordinates are invalid.")
            }
            if (!pds.db.addJson(SCHEMA.cCampuses, spaceSet.addBuids()))
              return RESPONSE.BAD_CANNOT_ADD_SPACE
            val res: JsValue = Json.obj(SCHEMA.fCampusCuid -> spaceSet.getId())
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
  def update: Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("Campus: update: " + Utils.stripJson(json))
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
          val campus = new Campus(storedCampus)
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

  def byOwner: Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("Campus: byOwner: " + Utils.stripJson(json))
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
  def delete: Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("Campus: delete: " + Utils.stripJson(json))
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
    if (user.isAdminOrModerator(userId)) return true
    if (campus != null && (campus \ SCHEMA.fOwnerId).toOption.isDefined) {  // check if owner
      return (campus \ SCHEMA.fOwnerId).as[String].equals(userId)
    }
    false
  }

}