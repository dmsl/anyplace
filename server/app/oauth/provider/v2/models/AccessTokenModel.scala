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

import com.couchbase.client.java.document.json.JsonObject
import AccessTokenModel._

import scala.beans.{BeanProperty}

object AccessTokenModel {

  def generateTokenId(access_token: String): String =
    String.format("oauth2_token_v1_%s", access_token)

}

class AccessTokenModel(private var access_token: String,
                       private var token_type: String,
                       private var expires_in: Long,
                       private var refresh_token: String,
                       @BeanProperty var scope: String,
                       @BeanProperty var auid: String,
                       private var client_id: String,
                       @BeanProperty var createdAt: Long) {

  // information to be kept in the database
  @BeanProperty
  var tuid: String = generateTokenId(this.access_token)

  def getAccessToken(): String = access_token
  def getTokenType(): String = token_type

  def getExpiresIn(): Long = expires_in

  def getRefreshToken(): String = refresh_token

  def getClientId(): String = client_id

  override def toString(): String = this.getTuid()

  def toJson(): JsonObject = {
    val json: JsonObject= JsonObject.empty()
    json.put("token_type", this.getTokenType())
    json.put("access_token", this.getAccessToken())
    json.put("expires_in", this.getExpiresIn())
    json.put("refresh_token", this.getRefreshToken())
    json.put("tuid", this.getTuid())
    json.put("scope", this.getScope())
    json.put("client_id", this.getClientId())
    json.put("auid", this.getAuid())
    json.put("created_at", this.getCreatedAt())
    json
  }

  override def equals(o: Any): Boolean = {
    if (this == o) true
    if (o == null || getClass != o.getClass) false
    val that: AccessTokenModel = o.asInstanceOf[AccessTokenModel]
    if (createdAt != that.createdAt) false
    if (expires_in != that.expires_in) false
    if (access_token != that.access_token) false
    if (client_id != that.client_id) false
    if (refresh_token != that.refresh_token) false
    if (scope != that.scope) false
    if (token_type != that.token_type) false
    if (tuid != that.tuid) false
    if (auid != that.auid) false
    true
  }

  override def hashCode(): Int = {
    var result: Int = access_token.hashCode
    result = 31 * result + token_type.hashCode
    result = 31 * result + (expires_in ^ (expires_in >>> 32)).toInt
    result = 31 * result + refresh_token.hashCode
    result = 31 * result + scope.hashCode
    result = 31 * result + tuid.hashCode
    result = 31 * result + auid.hashCode
    result = 31 * result + client_id.hashCode
    result = 31 * result + (createdAt ^ (createdAt >>> 32)).toInt
    result
  }

}
