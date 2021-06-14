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
package oauth.provider.v2.models

import java.util._

import com.couchbase.client.java.document.json.{JsonArray, JsonObject}
import oauth.provider.v2.models.AccountModel._
import utils.{LPUtils, PasswordService}

import scala.beans.BeanProperty

//remove if not needed
import scala.collection.JavaConversions._

object AccountModel {

  /** TODO:NN no need for separate accountModel. we have db_models/Account
    * Defines a registered client for this account
    */
  class ClientModel(var client_id: String,
                    var client_secret: String,
                    var grant_type: String,
                    var scope: String,
                    var redirect_uri: String) {

    def getClientId(): String = client_id
    def getClientSecret(): String = client_secret
    def getGrantType(): String = grant_type
    def getRedirectUri(): String = redirect_uri

    def getScope(): String = scope

    def toJson(): JsonObject = {
      val json: JsonObject = JsonObject.empty()
      json.put("client_id", client_id)
      json.put("client_secret", client_secret)
      json.put("grant_type", grant_type)
      json.put("scope", scope)
      json.put("redirect_uri", redirect_uri)
      json
    }

  }

  /**
    * STATIC FACTORIES
    */
  def createEmptyAccount(): AccountModel = {
    val json: JsonObject = JsonObject.empty()
    json.put("clients",JsonArray.empty())
    new AccountModel(json)
  }

  def createInitializedAccount(): AccountModel = {
    val json: JsonObject = JsonObject.empty()
    json.put("clients",JsonArray.empty())
    json.put("auid", generateNewAuid())
    json.put("username", "")
    json.put("password", "")
    json.put("scope", "")
    json.put("nickname", "")
    json.put("email", "")
    json.put("isadmin", false)
    new AccountModel(json)
  }

  /**
    * Static helpers
    */
  private def generateNewAuid(): String =
    "account_" +
      LPUtils.hashStringHex(
        LPUtils.generateRandomToken() + System.currentTimeMillis() +
          LPUtils.getRandomUUID)

  private def generateNewClientId(auid: String): String =
    "client_" +
      LPUtils.hashStringHex(
        LPUtils.getRandomUUID + auid + System.currentTimeMillis())

  private def generateNewClientSecret(auid: String,
                                      client_id: String): String =
    "secret_" +
      LPUtils.hashStringBase64(client_id + LPUtils.generateRandomToken())

  private var CHANGEABLE_PROPERTIES: Array[String] =
    Array("nickname", "scope", "email", "isadmin")

  def getChangeableProperties(): Array[String] = CHANGEABLE_PROPERTIES

}

/**
  * Created by lambros on 2/4/14.
  */
class AccountModel(json: JsonObject) {

  private var mJson: JsonObject = JsonObject.empty()
  @BeanProperty
  var auid: String = json.getString("auid")

  @BeanProperty
  var username: String = json.getString("username")

  @BeanProperty
  var password: String = json.getString("password")

  @BeanProperty
  var scope: String = json.getString("scope")

  @BeanProperty
  var nickname: String = json.getString("nickname")

  @BeanProperty
  var email: String = json.getString("email")

  private var isadmin: Boolean = json.getBoolean("isadmin")

  private var clients: List[ClientModel] = new ArrayList[ClientModel]()


  val jsclients=json.getArray("clients")
  jsclients.forall(client =>
    this.clients.add(
      new ClientModel(
        client.asInstanceOf[JsonObject].getString("client_id"),
        client.asInstanceOf[JsonObject].getString("client_secret"),
        client.asInstanceOf[JsonObject].getString("grant_type"),
        client.asInstanceOf[JsonObject].getString("scope"),
        client.asInstanceOf[JsonObject].getString("redirect_uri")
      ))
  )

  if (this.auid == null || this.auid.trim().isEmpty) {
    this.auid = generateNewAuid()
    this.mJson.put("auid", this.auid)
  }

  def isAdmin(): Boolean = isadmin

  def setAdmin(isAdmin: Boolean): Unit = {
    this.isadmin = isAdmin
  }

  def validateScope(scope: String, client_id: String): Boolean = {
    // TODO - check against the global user scope
    if (scope == null) {
      false
    }
    val scopes: Array[String] = scope.split(" ")
    val accountScopes: Array[String] = this.getScope.split(" ")
    // if more scopes requested reject
    if (scopes.length > accountScopes.length) {
      false
    }
    // check the scopes
    for (s <- scopes) {
      var matched: Boolean = false
      for (sa <- accountScopes if s.equalsIgnoreCase(sa)) {
        matched = true
        //break
      }
      if (!matched) false
    }
    // TODO - check against the client specific scopes
    true
  }

  def deleteClient(client_id: String): Boolean = {
    val sz=this.clients.size()
    for (i <- 0 until sz if this.clients.get(i).client_id == client_id) {
      this.clients.remove(i)
      true
    }
    false
  }

  def getClient(client_id: String): ClientModel =
    this.clients.find(_.client_id == client_id).getOrElse(null)

  // DOES NOT GUARANTEE UNIQUENESS FOR NOW
  def addNewClient(grant_type: String,
                   scope: String,
                   redirect_uri: String): Unit = {
    val client_id: String = generateNewClientId(this.getAuid)
    val client_secret: String = generateNewClientSecret(auid, client_id)
    this.clients.add(
      new ClientModel(client_id,
        client_secret,
        grant_type,
        scope,
        redirect_uri))
  }

  override def toString(): String =
    String.format("AccountModel: uaid[%s]", this.getAuid)

  def toJson(): JsonObject = {
    val json: JsonObject = JsonObject.empty()
    json.put("doctype", "account")
    json.put("auid", this.auid)
    json.put("username", this.username)
    json.put("password", PasswordService.createHash(this.password))
    json.put("scope", this.scope)
    json.put("nickname", this.nickname)
    json.put("email", this.email)
    json.put("isadmin", this.isadmin)
    val clientsNode: JsonArray = JsonArray.empty()
    for (cm <- this.clients) {
      clientsNode.add("clients",cm.toJson())
    }
    this.mJson = json
    this.mJson
  }

}
