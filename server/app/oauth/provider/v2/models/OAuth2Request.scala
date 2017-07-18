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

import java.util.regex.Pattern

import oauth.provider.v2.OAuth2Constant
import play.api.libs.json.JsValue
import play.api.mvc.{AnyContent, Request}
import utils.LPUtils

class OAuth2Request(request: Request[AnyContent], enableCORS: Boolean) {

  var mRequest = request

  var mBody = this.mRequest.body

  var mJsonBody: JsValue = _

  var mFormBody: Map[String, Seq[String]] = _


  if (!assertJsonBody()) {
    assertFormUrlEncodedBody()
  }

  def this(request: Request[AnyContent]) {
    this(request, true)
  }

  def assertJsonBody(): Boolean = {
    if (this.mJsonBody == null) this.mJsonBody = this.mRequest.body.asJson.get
    this.mJsonBody != null
  }

  def hasJsonBody(): Boolean = this.mJsonBody != null

  def getJsonBody(): JsValue = {
    if (this.mJsonBody == null) assertJsonBody()
    this.mJsonBody
  }

  def assertFormUrlEncodedBody(): Boolean = {
    this.mFormBody = this.mBody.asFormUrlEncoded.get
    if (this.mFormBody == null) {
      return false
    }
    true
  }

  def hasFormEncodedBody(): Boolean = this.mJsonBody != null

  def getFormEncodedBody(): Map[String, Seq[String]] = this.mFormBody

  def getMultipartFormData() = {
    this.mRequest.body.asMultipartFormData.get
  }

  def getHeader(header: String): String = {
    if (header == null || header.trim().isEmpty) {
      throw new IllegalArgumentException("No null/empty headers allowed!")
    }
    this.mRequest.headers(header)
  }

  def getParameter(property: String): String = {
    if (hasJsonBody()) {
      return (this.mJsonBody \ property).toString
    } else if (hasFormEncodedBody()) {
      return this.mFormBody.get(property).get.head
    }
    null
  }
  ///////////////////////////////////////////////////////////////////////////////////
  // OAUTH RELATED METHODS


  private val REGEXP_BASIC = Pattern.compile("^\\s*(Basic)\\s+(.*)$")

  def getCredentials: ClientCredentials = {
    val header = this.getHeader(OAuth2Constant.HEADER_AUTHORIZATION)
    // we found the credentials in the authorization header
    if (header != null) {
      val matcher = REGEXP_BASIC.matcher(header)
      if (matcher.find) {
        val decoded = LPUtils.decodeBase64String(matcher.group(2))
        if (decoded.indexOf(':') > 0) { // we have a client_id:client_secret combo
          val credential = decoded.split(":", 2)
          return new ClientCredentials(credential(0), credential(1))
        }
      }
    }
    // we will try in the parameters list since the header does not contain the info
    val client_id = this.getParameter(OAuth2Constant.CLIENT_ID)
    val client_secret = this.getParameter(OAuth2Constant.CLIENT_SECRET)
    new ClientCredentials(client_id, client_secret)
  }
}
