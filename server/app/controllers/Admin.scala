package controllers

import controllers.AnyplaceMapping.appendGoogleIdIfNeeded
import datasources.{DatasourceException, MongodbDatasource, ProxyDataSource, SCHEMA}
import json.VALIDATE
import oauth.provider.v2.models.OAuth2Request
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, Request, Result}
import utils.{AnyResponseHelper, LPLogger}

object Admin extends play.api.mvc.Controller {

  def migrateToMongoDB() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        // TODO:NN call python migration scripts from here

        // generate heatmaps
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fAccessToken)
        if (checkRequirements != null) return checkRequirements
        val owner_id = anyReq.authorize(json)
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        if (!MongodbDatasource.loadAdmins().contains(appendGoogleIdIfNeeded(owner_id))) {
          return AnyResponseHelper.forbidden("Unauthorized. Only admins can generate heatmaps.")
        }
        if (!ProxyDataSource.getIDatasource().generateHeatmaps())
          return AnyResponseHelper.internal_server_error("Couldn't generate Heatmaps")
        return AnyResponseHelper.ok("Generated heatmaps successfully")
      }
      inner(request)
  }

  /**
   * Retrieve all the accounts.
   *
   * @return
   */
  def fetchAllAccounts() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        LPLogger.info("AnyplaceAccounts::fetchAllAccounts(): ")
        val anyReq: OAuth2Request = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fAccessToken)
        if (checkRequirements != null) return checkRequirements
        val owner_id = anyReq.authorize(json)
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        if (!MongodbDatasource.loadAdmins().contains(owner_id))
          return AnyResponseHelper.forbidden("Only admin users can see all accounts.")
        try {
          val users: List[JsValue] = ProxyDataSource.getIDatasource().getAllAccounts()
          val res: JsValue = Json.obj("users_num" -> users.length, SCHEMA.cUsers -> Json.arr(users))
          AnyResponseHelper.ok(res, "Successfully retrieved all accounts!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

}
