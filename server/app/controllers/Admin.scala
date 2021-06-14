package controllers

import controllers.AnyplaceMapping.{appendGoogleIdIfNeeded, verifyId}
import datasources.{MongodbDatasource, ProxyDataSource, SCHEMA}
import oauth.provider.v2.models.OAuth2Request
import play.api.mvc.{Action, AnyContent, Request, Result}
import utils.{AnyResponseHelper, JsonUtils}

// TODO:NN transfer accounts/all here
object Admin extends play.api.mvc.Controller {

  def migrateToMongoDB() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        // TODO:NN call python migration scripts from here

        // generate heatmaps
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fAccessToken)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        if ((json \ SCHEMA.fAccessToken).getOrElse(null) == null) return AnyResponseHelper.forbidden("Unauthorized")
        val owner_id = verifyId((json \ SCHEMA.fAccessToken).as[String])
        if (owner_id == null) return AnyResponseHelper.forbidden("Unauthorized")
        // TODO:NN na to valw mazi me ta alla authentication
        // use oAuth2.verifyAdmin (anyReq.verifyAdmin)
        if (!MongodbDatasource.loadAdmins().contains(appendGoogleIdIfNeeded(owner_id))) {
          return AnyResponseHelper.forbidden("Unauthorized. Only admins can generate heatmaps.")
        }
        if (!ProxyDataSource.getIDatasource().generateHeatmaps())
          return AnyResponseHelper.internal_server_error("Couldn't generate Heatmaps")
        return AnyResponseHelper.ok("Generated heatmaps successfully")
      }
      inner(request)
  }
}
