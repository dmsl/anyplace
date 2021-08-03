/*
 * Anyplace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Constantinos Costa, Kyriakos Georgiou, Lambros Petrou, Paschalis Mpeis
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
 */
package models.oauth

import datasources.SCHEMA
import play.api.libs.json.JsValue
import play.api.mvc.{AnyContent, Request, Result}
import utils.RESPONSE
import utils.RESPONSE.ERROR_NO_ACCESS_TOKEN

// TODO:NN method-> verifyUser() verifyAdminUser()
// notes -> ../spaceDelete
class OAuth2Request(request: Request[AnyContent], enableCORS: Boolean) {
  var mRequest = request
  var mBody  = this.mRequest.body
  var mJsonBody: JsValue = _
  var mFormBody: Map[String, Seq[String]] = _

  if (!assertJsonBody()) { assertFormUrlEncodedBody() }

  def this(request: Request[AnyContent]) = { this(request, true) }

  def getAccessToken(): String = {
    val headers = this.mRequest.headers
    val apiKey = headers.get(SCHEMA.fAccessToken)
    if (apiKey.nonEmpty) return apiKey.get

    null
  }

  def NO_ACCESS_TOKEN() : Result = { RESPONSE.UNAUTHORIZED(ERROR_NO_ACCESS_TOKEN) }

  def assertJsonBody(): Boolean = {
    if (this.mJsonBody == null)
      this.mJsonBody = this.mRequest.body.asJson.orNull

    this.mJsonBody != null
  }

  def hasJsonBody(): Boolean = this.mJsonBody != null

  def getJsonBody(): JsValue = {
    if (this.mJsonBody == null) assertJsonBody()
    this.mJsonBody
  }

  def assertFormUrlEncodedBody(): Boolean = {
    this.mFormBody = this.mBody.asFormUrlEncoded.orNull
    if (this.mFormBody == null) {
      return false
    }
    true
  }

  def hasFormEncodedBody(): Boolean = this.mJsonBody != null

  def getFormEncodedBody(): Map[String, Seq[String]] = this.mFormBody

  def getMultipartFormData() = {
    try {
      this.mRequest.body.asMultipartFormData.get
    } catch {
      case _: NoSuchElementException => null
    }
  }

  def getHeader(header: String): String = {
    if (header == null || header.trim().isEmpty) {
      throw new IllegalArgumentException("Empty headers not allowed.")
    }
    this.mRequest.headers(header)
  }

  def getParameter(property: String): String = {
    if (hasJsonBody()) {
      return (this.mJsonBody \ property).toString
    } else if (hasFormEncodedBody()) {
      // return this.mFormBody.get(property).get.head // CHECK:NN see below1
      return this.mFormBody(property).head
    }
    null
  }
}


