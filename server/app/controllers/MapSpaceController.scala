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

  // TODO:NN
  def deletePrecomputed(): Unit = {
    // TODO:NN what is the method that does this now? We might have to rename it.
    // TODO: delete accessPointsWifi: buid, floor
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
            if (SCHEMA.fSpaceTypes.contains(spaceType)) {
              storedSpace = storedSpace.as[JsObject] + (SCHEMA.fSpaceType -> JsString(spaceType))
            } else
              return RESPONSE.BAD("Invalid space type. Use: `building` or `vessel`")
          }
          val space = new Space(storedSpace)
          if (!pds.db.replaceJsonDocument(SCHEMA.cSpaces, SCHEMA.fBuid, space.getId(), space.toGeoJSON()))
            return RESPONSE.BAD("Space could not be updated.")
          return RESPONSE.OK("Successfully updated space.")
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

  def spaceGet() = Action {
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

  def spaceAccessible() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()

        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("spaceAccessible: " + Utils.stripJson(json))
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
            case ioe: IOException => return RESPONSE.OK(res, "Successfully retrieved all spaces near your position!")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }


}
