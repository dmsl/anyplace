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
package oauth.provider.v2.utils

import oauth.provider.v2.OAuth2Constant
import oauth.provider.v2.models.AccessTokenModel
import play.api.libs.json._
import play.mvc.{Result, Results}


object OAuth2Responses {

  def InvalidRequest(msg: String): Result = {
    val json = Json.obj(
      OAuth2Constant.ERROR-> JsString(OAuth2ErrorConstant.INVALID_REQUEST),
      OAuth2Constant.ERROR_DESCRIPTION -> JsString(msg)
    )
    Results.badRequest(json.toString)
  }

  def InvalidClient(msg: String): Result = {
    val json = Json.obj(
      OAuth2Constant.ERROR-> JsString(OAuth2ErrorConstant.INVALID_CLIENT),
      OAuth2Constant.ERROR_DESCRIPTION-> JsString(msg)
    )
    Results.unauthorized(json.toString)
  }

  def UnauthorizedClient(msg: String): Result = {
    val json = Json.obj(
      OAuth2Constant.ERROR-> JsString(OAuth2ErrorConstant.UNAUTHORIZED_CLIENT),
      OAuth2Constant.ERROR_DESCRIPTION -> JsString(msg)
    )
    Results.unauthorized(json.toString)
  }

  def RedirectUriMismatch(msg: String): Result = {
    val json = Json.obj(
      OAuth2Constant.ERROR-> JsString(OAuth2ErrorConstant.NOT_MATCH_REDIRECT_URI),
      OAuth2Constant.ERROR_DESCRIPTION -> JsString(msg)
    )
    Results.unauthorized(json.toString)
  }

  def UnsupportedResponseType(msg: String): Result = {
    val json = Json.obj(
      OAuth2Constant.ERROR ->  JsString(OAuth2ErrorConstant.UNSUPPORTED_RESPONSE_TYPE),
      OAuth2Constant.ERROR_DESCRIPTION -> JsString(msg)
    )
    Results.badRequest(json.toString)
  }

  def InvalidGrant(msg: String): Result = {
    val json = Json.obj(
      OAuth2Constant.ERROR-> JsString(OAuth2ErrorConstant.INVALID_GRANT),
      OAuth2Constant.ERROR_DESCRIPTION -> JsString(msg)
    )
    Results.unauthorized(json.toString)
  }

  def UnsupportedGrantType(msg: String): Result = {
    val json = Json.obj(
      OAuth2Constant.ERROR-> JsString(OAuth2ErrorConstant.UNSUPPORTED_GRANT_TYPE),
      OAuth2Constant.ERROR_DESCRIPTION -> JsString(msg)
    )
    Results.badRequest(json.toString)
  }

  def InvalidScope(msg: String): Result = {
    val json = Json.obj(
      OAuth2Constant.ERROR -> JsString(OAuth2ErrorConstant.INVALID_SCOPE),
      OAuth2Constant.ERROR_DESCRIPTION -> JsString(msg)
    )
    Results.unauthorized(json.toString)
  }

  def InvalidToken(msg: String): Result = {
    val json = Json.obj(
      OAuth2Constant.ERROR -> JsString(OAuth2ErrorConstant.INVALID_TOKEN),
      OAuth2Constant.ERROR_DESCRIPTION -> JsString(msg)
    )
    Results.unauthorized(json.toString)
  }

  def ExpiredToken(): Result = {
    val json = Json.obj(
      OAuth2Constant.ERROR-> JsString(OAuth2ErrorConstant.EXPIRED_TOKEN),
      OAuth2Constant.ERROR_DESCRIPTION -> JsString("The token has expired!")
    )

    Results.unauthorized(json.toString)
  }

  def InsufficientScope(msg: String): Result = {
    val json = Json.obj(
      OAuth2Constant.ERROR -> JsString(OAuth2ErrorConstant.INSUFFICIENT_SCOPE),
      OAuth2Constant.ERROR_DESCRIPTION -> JsString(msg)
    )
    Results.unauthorized(json.toString)
  }

  def ValidToken(tokenModel: AccessTokenModel): Result = {
    val json: JsValue = Json.obj(
      OAuth2Constant.ACCESS_TOKEN -> JsString(tokenModel.getAccessToken()),
      OAuth2Constant.TOKEN_TYPE -> JsString(tokenModel.getTokenType()),
      OAuth2Constant.EXPIRES_IN -> JsNumber(tokenModel.getExpiresIn()),
      OAuth2Constant.REFRESH_TOKEN -> JsString(tokenModel.getRefreshToken())
    )

    Results.ok(json.toString)
  }

}
