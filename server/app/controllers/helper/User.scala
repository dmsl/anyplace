package controllers.helper

import datasources.{ProxyDataSource, SCHEMA}
import javax.inject.Inject
import play.api.libs.json.JsValue
import javax.inject.Singleton

@Singleton
class User @Inject()(pds: ProxyDataSource){

  /**
   * Checks in database if a user exists with the access_token of the json.
   *
   * @param json original request.
   * @return the owner_id of the user.
   */
  def authorize(json: JsValue): String = {
    val token = (json \ SCHEMA.fAccessToken).as[String]
    val user = pds.getIDatasource.getFromKeyAsJson(SCHEMA.cUsers, SCHEMA.fAccessToken, token)
    if (user != null)
      return (user \ SCHEMA.fOwnerId).as[String]
    null
  }
}
