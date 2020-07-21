/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Constantinos Costa, Kyriakos Georgiou, Lambros Petrou
 *
 * Supervisor: Demetrios Zeinalipour-Yazti
 *
 * URL: https://anyplace.cs.ucy.ac.cy
 * Contact: anyplace@cs.ucy.ac.cy
 *
 * Copyright (c) 2016, Data Management Systems Lab (DMSL), University of Cyprus.
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
 */
package controllers

import java.util

import datasources.DatasourceException
import datasources.ProxyDataSource
import oauth.provider.v2.granttype.GrantHandlerFactory
import oauth.provider.v2.models.AccountModel
import oauth.provider.v2.models.OAuth2Request
import play.mvc.Controller
import utils.AnyResponseHelper
import utils.JsonUtils
import utils.LPLogger
import com.couchbase.client.java.document.json.{JsonArray, JsonObject}
import play.api.mvc.{Action, AnyContent, Request, Result}


object AnyplaceAccounts extends Controller {

  /**
    * Retrieve all the accounts.
    *
    * @return
    */
  def fetchAllAccounts() = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq: OAuth2Request = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(
            AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = JsonObject.empty()
        LPLogger.info("AnyplaceAccounts::fetchAllAccounts(): " + json.toString)
        try {
          val accounts: util.List[JsonObject] = ProxyDataSource.getIDatasource().getAllAccounts()
          val res: JsonObject = JsonObject.empty()
          res.put("accounts", JsonArray.from(accounts))
          AnyResponseHelper.ok(res, "Successfully retrieved all accounts!")
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error(
              "500: " + e.getMessage)

        }
      }

      inner(request)
  }


  /**
    * Fetches the account with the AUID passed in.
    * The account document is returned in the Json response.
    *
    * @return
    */
  def fetchAccount(auid_in: String) = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        var auid: String = auid_in
        // create the Request and check it
        val anyReq: OAuth2Request = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(
            AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody()
        LPLogger.info("AnyplaceAccounts::fetchAccount():: " + json.toString)
        // check if there is any required parameter missing
        val notFound: util.List[String] =
          JsonUtils.requirePropertiesInJson(json, "auid")
        if (!notFound.isEmpty && (auid == null || auid.trim().isEmpty)) {
          return AnyResponseHelper.requiredFieldsMissing(notFound)
        }
        // if the auid in the route is empty then try to get the one from the POST json body
        if (auid == null || auid.trim().isEmpty)
          auid = json.\\("auid").mkString
        try {
          var storedAccount: JsonObject = null
          storedAccount =
            ProxyDataSource.getIDatasource.getFromKeyAsJson(auid)
          if (storedAccount ==
            null) {
            return AnyResponseHelper.bad_request("Account could not be found!")
          }
          return AnyResponseHelper.ok(storedAccount,
            "Successfully created account!")
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error(
              "500: " + e.getMessage)

        }
      }

      inner(request)
  }

  /**
    * Deletes the account with the AUID passed in.
    * The result of the action is returned in the Json response.
    *
    * @return
    */
  def deleteAccount(auid_in: String) = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        var auid: String = auid_in
        // create the Request and check it
        val anyReq: OAuth2Request = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(
            AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody()
        LPLogger.info("AnyplaceAccounts::deleteAccount():: " + json.toString)
        // check if there is any required parameter missing
        val notFound: util.List[String] =
          JsonUtils.requirePropertiesInJson(json, "auid")
        if (!notFound.isEmpty && (auid == null || auid.trim().isEmpty)) {
          return AnyResponseHelper.requiredFieldsMissing(notFound)
        }
        // if the auid in the route is empty then try to get the one from the POST json body
        if (auid == null || auid.trim().isEmpty)
          auid = json.\\("auid").mkString
        try {
          if (!ProxyDataSource.getIDatasource.deleteFromKey(auid)) {
            return AnyResponseHelper.bad_request("Account could not be deleted!")
          }
          return AnyResponseHelper.ok("Successfully deleted account!")
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error(
              "500: " + e.getMessage)

        }
      }

      inner(request)
  }

  /**
    * Updates the account specified by the AUID.
    * The result of the update is returned in the Json response.
    *
    * @return
    */
  def UpdateAccount(auid: String)(auid_in: String) = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        var auid: String = auid_in
        // create the Request and check it
        val anyReq: OAuth2Request = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(
            AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody()
        LPLogger.info("AnyplaceAccounts::updateAccount():: " + json.toString)
        // check if there is any required parameter missing
        val notFound: util.List[String] =
          JsonUtils.requirePropertiesInJson(json, "auid")
        if (!notFound.isEmpty && (auid == null || auid.trim().isEmpty)) {
          return AnyResponseHelper.requiredFieldsMissing(notFound)
        }
        // if the auid in the route is empty then try to get the one from the POST json body
        if (auid == null || auid.trim().isEmpty)
          auid = json.\\("auid").mkString
        try {
          // fetch the stored object
          var storedAccount: JsonObject = null
          storedAccount = ProxyDataSource.getIDatasource().getFromKeyAsJson(auid)
          if (storedAccount == null) {
            return AnyResponseHelper.bad_request(
              "Account could not be updated! Try again...")
          }
          // apply any change made
          val updateableFields: Array[String] =
            AccountModel.getChangeableProperties()
          for (s <- updateableFields) {
            val value = json.\\(s)
            if (value.asInstanceOf[Boolean]) {
              storedAccount.put(s, value.asInstanceOf[Boolean])
            } else {
              val nv: String = value.mkString
              if (nv == null || nv.trim().isEmpty) //continue
                storedAccount.put(s, nv)
            }
          }
          // save the changes
          if (!ProxyDataSource.getIDatasource().replaceJsonDocument(auid, 0, storedAccount.toString)) {
            return AnyResponseHelper.bad_request(
              "Account could not be updated! Try again...")
          }
          return AnyResponseHelper.ok("Successfully updated account!")
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error(
              "500: " + e.getMessage)

        }
      }

      inner(request)
  }

  // check if there is any required parameter missing
  // check if there is any required parameter missing

  /**
    * Returns the list of clients for this account
    *
    * @param auid The account for which the clients are to be returned
    * @return
    */
  def fetchAccountClients(auid: String)(auid_in: String) = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        var auid: String = auid_in
        // create the Request and check it
        val anyReq: OAuth2Request = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(
            AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody()
        LPLogger.info("AnyplaceAccounts::fetchAccountClients():: " + json.toString)
        // check if there is any required parameter missing
        val notFound: util.List[String] =
          JsonUtils.requirePropertiesInJson(json, "auid")
        if (!notFound.isEmpty && (auid == null || auid.trim().isEmpty)) {
          return AnyResponseHelper.requiredFieldsMissing(notFound)
        }
        // if the auid in the route is empty then try to get the one from the POST json body
        if (auid == null || auid.trim().isEmpty)
          auid = json.\\("auid").mkString
        try {
          var storedAccount: JsonObject = null
          storedAccount =
            ProxyDataSource.getIDatasource().getFromKeyAsJson(auid)
          if (storedAccount == null) {
            return AnyResponseHelper.bad_request("Account could not be found!")
          }
          val json_clients = storedAccount.getArray("clients")
          val resp: JsonObject = JsonObject.empty()
          resp.put("clients", json_clients)
          return AnyResponseHelper.ok(resp, "Successfully fetched account clients!")
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error(
              "500: " + e.getMessage)

        }
      }

      inner(request)
  }

  /**
    * Adds a new client for this account
    *
    * @param auid The account the new account belongs to
    * @return
    */
  def addAccountClient(auid: String)(auid_in: String) = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        var auid: String = auid_in
        // create the Request and check it
        val anyReq: OAuth2Request = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(
            AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody()
        LPLogger.info("AnyplaceAccounts::addAccountClient():: " + json.toString)
        // check if there is any required parameter missing
        val notFound: util.List[String] =
          JsonUtils.requirePropertiesInJson(json, "auid", "grant_type")
        if (!notFound.isEmpty && (auid == null || auid.trim().isEmpty)) {
          return AnyResponseHelper.requiredFieldsMissing(notFound)
        }
        // if the auid in the route is empty then try to get the one from the POST json body
        if (auid == null || auid.trim().isEmpty)
          auid = json.\\("auid").mkString
        val grant_type: String = json.\\("grant_type").mkString
        val scope: String = json.\\("scope").mkString
        val redirect_uri: String = json.\\("redirect_uri").mkString
        if (!GrantHandlerFactory.isGrantTypeSupported(grant_type)) {
          return AnyResponseHelper.bad_request("grant_type specified is not supported!")
        }
        try {
          var storedAccount: JsonObject = null
          storedAccount = ProxyDataSource.getIDatasource().getFromKeyAsJson(auid)
          if (storedAccount == null) {
            return AnyResponseHelper.bad_request("Account could not be found!")
          }
          val account: AccountModel = new AccountModel(storedAccount)
          account.addNewClient(grant_type, scope, redirect_uri)
          // save the changes
          if (!ProxyDataSource.getIDatasource().replaceJsonDocument(
            auid,
            0,
            account.toJson().toString)) {
            return AnyResponseHelper.bad_request(
              "Account could not be updated! Try again...")
          }
          return AnyResponseHelper.ok("Successfully added account client!")
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error(
              "500: " + e.getMessage)

        }
      }

      inner(request)
  }

  /**
    * Fetches the account  client with the AUID and client_id passed in.
    * The client document is returned in the Json response.
    *
    * @return
    */
  def fetchAccountClient(auid: String, client_id: String)(auid_in: String) = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        var auid: String = auid_in
        // create the Request and check it
        val anyReq: OAuth2Request = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(
            AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceAccounts::fetchAccount():: " + json.toString)
        // check the arguments
        if ((auid == null || auid.trim().isEmpty)) {
          return AnyResponseHelper.bad_request("Invalid account id provided!")
        }
        if ((client_id == null || client_id.trim().isEmpty)) {
          AnyResponseHelper.bad_request("Invalid client id provided!")
        }
        try {
          var storedAccount: JsonObject = null
          storedAccount = ProxyDataSource.getIDatasource.getFromKeyAsJson(auid)
          if (storedAccount == null) {
            return AnyResponseHelper.bad_request("Account could not be found!")
          }
          val account: AccountModel = new AccountModel(storedAccount)
          val client: AccountModel.ClientModel = account.getClient(client_id)
          if (client == null) {
            return AnyResponseHelper.bad_request("Account client could not be found!")
          }
          return AnyResponseHelper.ok(client.toJson(),
            "Successfully fetched account client!")
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error(
              "500: " + e.getMessage)

        }
      }

      inner(request)
  }

  /**
    * Fetches the account  client with the AUID and client_id passed in.
    * The client document is returned in the Json response.
    *
    * @return
    */
  def deleteAccountClient(auid: String, client_id: String)(auid_in: String) = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        var auid: String = auid_in
        // create the Request and check it
        val anyReq: OAuth2Request = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(
            AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceAccounts::deleteAccount():: " + json.toString)
        // check the arguments
        if ((auid == null || auid.trim().isEmpty)) {
          return AnyResponseHelper.bad_request("Invalid account id provided!")
        }
        if ((client_id == null || client_id.trim().isEmpty)) {
          return AnyResponseHelper.bad_request("Invalid client id provided!")
        }
        try {
          var storedAccount: JsonObject = null
          storedAccount =
            ProxyDataSource.getIDatasource.getFromKeyAsJson(auid)
          if (storedAccount == null) {
            return AnyResponseHelper.bad_request("Account could not be found!")
          }
          val account: AccountModel = new AccountModel(storedAccount)
          if (!account.deleteClient(client_id)) {
            return AnyResponseHelper.bad_request("Account client could not be found!")
          }
          // save the changes
          if (!ProxyDataSource.getIDatasource.replaceJsonDocument(
            auid,
            0,
            account.toJson().toString)) {
            return AnyResponseHelper.bad_request(
              "Account could not be updated! Try again...")
          }
          return AnyResponseHelper.ok("Successfully deleted account client!")
        } catch {
          case e: DatasourceException =>
            return AnyResponseHelper.internal_server_error(
              "500: " + e.getMessage)

        }
      }

      inner(request)
  }

}
