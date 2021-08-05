/*
 * Anyplace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Nikolas Neofytou, Paschalis Mpeis
 *
 * Supervisor: Demetrios Zeinalipour-Yazti
 *
 * URL: https://anyplace.cs.ucy.ac.cy
 * Contact: anyplace@cs.ucy.ac.cy
 *
 * Copyright (c) 2021, Data Management Systems Lab (DMSL), University of Cyprus.
 * All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the “Software”), to deal in the
 * Software without restriction, including without limitation the rights to use, copy,
 * modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the
 * following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 *
 *
 */

package controllers

import datasources.{DatasourceException, MongodbDatasource, ProxyDataSource, SCHEMA}
import models.oauth.OAuth2Request
import javax.inject.{Inject, Singleton}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import utils.Utils.appendGoogleIdIfNeeded
import utils.{LOG, RESPONSE}

@Singleton
class UserAdminController @Inject()(cc: ControllerComponents,
                                    pds: ProxyDataSource,
                                    user: helper.User)
  extends AbstractController(cc) {

  /**
   * Not in use. It fills all the heatmap caches, which needs around 6 hours
   * for each collection (6 in total) of the Anyplace dataset.
   *
   * Instead, we are creating/destroying these heatmap caches on demand.
   */
  def migrateToMongoDB(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        // Generate heatmaps
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.FORBIDDEN("Unauthorized")
        if(!user.isAdminOrModerator(owner_id)) {
          return RESPONSE.FORBIDDEN("Unauthorized. Only admins can generate heatmaps.")
        }
        if (!pds.db.generateHeatmaps())
          return RESPONSE.ERROR_INTERNAL("Couldn't generate Heatmaps")
        return RESPONSE.OK("Generated heatmaps successfully")
      }
      inner(request)
  }

  /**
   * Retrieve all the accounts.
   *
   * @return
   */
  def fetchAllAccounts(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        LOG.D("User: fetchAllAccounts")
        val anyReq: OAuth2Request = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.FORBIDDEN("Unauthorized")
        if(!user.isAdminOrModerator(owner_id)) return RESPONSE.FORBIDDEN("Only moderators users can see all accounts.")
        try {
          val users: List[JsValue] = pds.db.getAllAccounts()
          val res: JsValue = Json.obj("users_num" -> users.length, SCHEMA.cUsers -> Json.arr(users))
          RESPONSE.gzipJsonOk(res, "Successfully retrieved all accounts!")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR_INTERNAL("500: " + e.getMessage)
        }
      }
      inner(request)
  }

}
