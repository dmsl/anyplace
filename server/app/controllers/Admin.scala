package controllers

import datasources.{DatasourceException, MongodbDatasource, ProxyDataSource, SCHEMA}
import javax.inject.{Inject, Singleton}
import json.VALIDATE
import oauth.provider.v2.models.OAuth2Request
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import utils.Utils.appendGoogleIdIfNeeded
import utils.{AnyResponseHelper, LOG}

@Singleton
class Admin @Inject()(cc: ControllerComponents,
                      mdb: MongodbDatasource,
                      pds: ProxyDataSource,
                      user: helper.User)
  extends AbstractController(cc) {

  def migrateToMongoDB() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        // TODO:NN call python migration scripts from here

        // generate heatmaps
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody()
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fAccessToken)
        if (checkRequirements != null) return checkRequirements
        val owner_id = user.authorize(json)
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        if (!MongodbDatasource.loadAdmins().contains(appendGoogleIdIfNeeded(owner_id))) {
          return AnyResponseHelper.forbidden("Unauthorized. Only admins can generate heatmaps.")
        }
        if (!pds.getIDatasource.generateHeatmaps())
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
        LOG.I("fetchAllAccounts(): ")
        val anyReq: OAuth2Request = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody()
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fAccessToken)
        if (checkRequirements != null) return checkRequirements
        val owner_id = user.authorize(json)
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        if (!MongodbDatasource.loadAdmins().contains(owner_id))
          return AnyResponseHelper.forbidden("Only admin users can see all accounts.")
        try {
          val users: List[JsValue] = pds.getIDatasource.getAllAccounts()
          val res: JsValue = Json.obj("users_num" -> users.length, SCHEMA.cUsers -> Json.arr(users))
          AnyResponseHelper.ok(res, "Successfully retrieved all accounts!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }
      inner(request)
  }

}
