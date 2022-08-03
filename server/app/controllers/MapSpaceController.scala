package controllers

import datasources.{DatasourceException, ProxyDataSource, SCHEMA}
import models._
import models.oauth.OAuth2Request
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc._
import utils.Utils.appendGoogleIdIfNeeded
import utils._
import utils.json.VALIDATE
import utils.json.VALIDATE.String

import java.io._
import javax.inject.{Inject, Singleton}
@Singleton
class MapSpaceController @Inject()(cc: ControllerComponents,
                                    tilerHelper: AnyPlaceTilerHelper,
                                    pds: ProxyDataSource,
                                    user: helper.User)
    extends AbstractController(cc) {

  val NEARBY_BUILDINGS_RANGE = 50
  val NEARBY_BUILDINGS_RANGE_MAX = 500

  def add(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("spaceAdd: " + Utils.stripJsValueStr(json))
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
          if (!pds.db.addJson(SCHEMA.cSpaces, space.toGeoJson())) {
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

  def update(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("spaceUpdate: " + Utils.stripJsValueStr(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid)
        if (checkRequirements != null) return checkRequirements
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          var storedSpace = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          if (!user.canAccessSpace(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
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
            if (SCHEMA.fSpaceTypes.contains(spaceType)) {
              storedSpace = storedSpace.as[JsObject] + (SCHEMA.fSpaceType -> JsString(spaceType))
            } else
              return RESPONSE.BAD("Invalid space type. Use: `building` or `vessel`")
          }
          val space = new Space(storedSpace)
          if (!pds.db.replaceJsonDocument(SCHEMA.cSpaces, SCHEMA.fBuid, space.getId(), space.toGeoJsonStr()))
            return RESPONSE.BAD("Space could not be updated.")
          return RESPONSE.OK("Successfully updated space.")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  /**
   *
   * Sets the co-owners of a space.
   * Any previous co-owners will be removed
   *
   * - The parameters are:
   *   - api key (the user must have access)
   *   - [SCHEMA.fBuid]
   *   - [SCHEMA.fCoOwners]: either a coOwner id or a list: [coOwnID1, coOwnID2]
   *
   * @return
   */
  def setCoOwners(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, SCHEMA.fCoOwners)
        if (!requiredMissing.isEmpty) return RESPONSE.MISSING_FIELDS(requiredMissing)
        val owner_id = user.authorize(apiKey)
        json = json.as[JsObject] + (SCHEMA.fOwnerId -> Json.toJson(owner_id))
        val validation = VALIDATE.fields(json, SCHEMA.fBuid)
        if (validation.failed()) return validation.response()

        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val storedSpace: JsValue = pds.db.getFromKeyAsJson(SCHEMA.cSpaces, SCHEMA.fBuid, buid)
          if (storedSpace == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
          if (!user.canAccessSpace(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
          val space = new Space(storedSpace)
          val result = space.appendCoOwners(user, pds, json)
          if (result.err.nonEmpty) {
            return RESPONSE.BAD("Space could not be updated: " + result.err)
          }
          if (!pds.db.replaceJsonDocument(SCHEMA.cSpaces, SCHEMA.fBuid, space.getId(), result.data))
            return RESPONSE.BAD("Space could not be updated.")

          var msg = "Successfully updated coOwners"
          if  (result.warn.nonEmpty) msg+= " ("+result.warn+")"
          return RESPONSE.OK(msg)
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
          if (!user.canAccessSpace(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
        try {
          val deleted = pds.db.deleteAllByBuilding(buid)
          if (!deleted)
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

  def public: Action[AnyContent] = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("Space: all: ")
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

  def get(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("spaceGetOne: " + Utils.stripJsValueStr(json))
        val check = VALIDATE.checkRequirements(json, SCHEMA.fBuid)
        if (check!= null) return check
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
            try {
              return RESPONSE.gzipJsonOk(space.toString)
            } catch {
              case _: IOException => return RESPONSE.OK(space, "Space retrieved.")
            }
          }

          RESPONSE.NOT_FOUND("Space not found.")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }
      inner(request)
  }

  def userAccessible(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()

        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("userAccessible: " + Utils.stripJsValueStr(json))
        val checkRequirements = VALIDATE.checkRequirements(json) // , SCHEMA.fAccessToken
        if (checkRequirements != null) return checkRequirements

        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        try {
          LOG.D3("owner_id = " + owner_id)
          val spaces = pds.db.getSpaceAccessible(owner_id)
          val res: JsValue = Json.obj("spaces" -> spaces)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case ioe: IOException => return RESPONSE.OK(res, "Retrieved user spaces.")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }
      inner(request)
  }

  def userOwned(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()

        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("spaceOwned: " + Utils.stripJsValueStr(json))
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

  def spaceByBucode(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("spaceByBucode: " + Utils.stripJsValueStr(json))
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

  def byCoordinates(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("spaceCoordinates: " + Utils.stripJsValueStr(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fCoordinatesLat, SCHEMA.fCoordinatesLon)
        if (checkRequirements != null) return checkRequirements
        var range = NEARBY_BUILDINGS_RANGE
        if (JsonUtils.hasProperty(json, "range")) {
          if ((json \ "range").validate[Int].isError) {
            return RESPONSE.BAD("range must be a positive integer")
          }
          range = (json \ "range").as[Int]
          if (range <= 0) {
            return RESPONSE.BAD("range must be a positive integer")
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
            case _: IOException => RESPONSE.OK(res, "Retrieved all spaces near user position")
          }
        } catch {
          case e: DatasourceException => RESPONSE.ERROR(e)
        }
      }
      inner(request)
  }

  // unused.routes
  def spaceUpdateOwner(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        var json = anyReq.getJsonBody()
        LOG.D2("spaceUpdateOwner: " + Utils.stripJsValueStr(json))
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
          if (!user.canAccessSpace(storedSpace, owner_id)) return RESPONSE.UNAUTHORIZED_USER
          val space = new Space(storedSpace)
          if (!pds.db.replaceJsonDocument(SCHEMA.cSpaces, SCHEMA.fBuid, space.getId(), space.changeOwner(newOwner))) return RESPONSE.BAD("Space could not be updated!")
          return RESPONSE.OK("Successfully updated space!")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

}
