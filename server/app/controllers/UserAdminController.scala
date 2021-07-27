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
import javax.inject.{Inject, Singleton}
import json.VALIDATE
import oauth.provider.v2.models.OAuth2Request
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import utils.Utils.appendGoogleIdIfNeeded
import utils.{RESPONSE, LOG}

@Singleton
class UserAdminController @Inject()(cc: ControllerComponents,
                                    mdb: MongodbDatasource,
                                    pds: ProxyDataSource,
                                    user: helper.User)
  extends AbstractController(cc) {

  // CHECK:NN ?
  def migrateToMongoDB() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        // TODO:NN call python migration scripts from here

        // Generate heatmaps
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fAccessToken)
        if (checkRequirements != null) return checkRequirements
        val owner_id = user.authorize(json)
        if (owner_id == null) return RESPONSE.FORBIDDEN("Unauthorized")
        if (!MongodbDatasource.loadAdmins().contains(appendGoogleIdIfNeeded(owner_id))) {
          return RESPONSE.FORBIDDEN("Unauthorized. Only admins can generate heatmaps.")
        }
        if (!pds.getIDatasource.generateHeatmaps())
          return RESPONSE.internal_server_error("Couldn't generate Heatmaps")
        return RESPONSE.OK("Generated heatmaps successfully")
      }
      inner(request)
  }

  // CHECK:NN does it work?
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
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fAccessToken)
        if (checkRequirements != null) return checkRequirements
        val owner_id = user.authorize(json)
        if (owner_id == null) return RESPONSE.FORBIDDEN("Unauthorized")
        if (!MongodbDatasource.loadAdmins().contains(owner_id))
          return RESPONSE.FORBIDDEN("Only admin users can see all accounts.")
        try {
          val users: List[JsValue] = pds.getIDatasource.getAllAccounts()
          val res: JsValue = Json.obj("users_num" -> users.length, SCHEMA.cUsers -> Json.arr(users))
          RESPONSE.OK(res, "Successfully retrieved all accounts!")
        } catch {
          case e: DatasourceException => return RESPONSE.internal_server_error("500: " + e.getMessage)
        }
      }
      inner(request)
  }

}
