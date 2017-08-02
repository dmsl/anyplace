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
package oauth.provider.v2.controllers

import oauth.provider.v2.utils.OAuth2Responses
import oauth.provider.v2.granttype.GrantHandlerFactory
import oauth.provider.v2.granttype.IGrantHandler
import oauth.provider.v2.models.AccountModel
import oauth.provider.v2.models.ClientCredentials
import oauth.provider.v2.models.OAuth2Request
import accounts.IAccountService
import accounts.ProxyAccountService
import oauth.provider.v2.OAuth2Constant
import org.apache.commons.lang3.StringUtils
import play.api.mvc.Action
import play.mvc.Controller
import utils.{AnyResponseHelper, LPLogger}

object AnyplaceOAuth extends Controller{

  /**
    * This endpoint interacts with the resource owner in order to obtain
    * authorization grant. The authorization server first authenticates
    * the resource owner, using credentials (HTML Form) or session cookies.
    *
    * TLS/SSL connection is REQUIRED.
    *
    * REQUIRED: GET, [POST]
    *
    * This endpoint is used by grant types:
    * a) Authorization Code
    * b) Implicit grant
    * and the field 'response_type' is required with values 'code' or 'token'
    * respectively.
    *
    * @return
    */
  def authorize()=Action {
    implicit request =>
    // create the Request and check it
    val anyReq: OAuth2Request = new OAuth2Request(request)
    if (!anyReq.assertJsonBody()) {
      AnyResponseHelper.bad_request(
        AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
    }
    val json = anyReq.getJsonBody
    LPLogger.info("AnyplaceOAuth::authorize():: " + json.toString)
    AnyResponseHelper.not_found(
      "OAuth grant types using authenticate are not supported!")
  }

  /**
    * This endpoint is used by all the grant types flows except 'implicit grant'
    * since the token is issued directly.
    *
    * TLS/SSL connection is REQUIRED.
    * REQUIRED: POST
    * Required parameters with no value are treated as omitted and unknowns are ignored.
    *
    * ////// REQUEST A TOKEN //////////////
    * Access Token Request (application/x-www-form-urlencoded): RFC 4.3.2
    * grant_type: 'password'
    * username: The resource owner username
    * password: The resource owner password
    * scope: The scope of the access request (optional)
    * client_id: The client id issued by the registration service (optional)
    * client_secret: The client secret issued by the registration service (optional)
    *
    * After successful request the response is like below:
    *
    * HTTP/1.1 200 OK
    * Content-Type: application/json;charset=utf-8
    * Cache-Control: no-store
    * Pragma: no-cache
    *
    * {
    * "access_token": "access token here",
    * "token_type": "Bearer",
    * "expires_in": lifetime in seconds,
    * "refresh_token": "refresh token here"
    * }
    *
    * ////// REFRESH A TOKEN //////////////
    * Access Token Refresh (application/x-www-form-urlencoded): RFC 4.3.2
    * grant_type: 'refresh_token'
    * refresh_token: 'the refresh token issued in the token request'
    * scope: The scope of the access request (optional)
    * client_id: The client id issued by the registration service (optional)
    * client_secret: The client secret issued by the registration service (optional)
    *
    * After successful request the response is like below:
    *
    * HTTP/1.1 200 OK
    * Content-Type: application/json;charset=utf-8
    * Cache-Control: no-store
    * Pragma: no-cache
    *
    * {
    * "access_token": "access token here",
    * "token_type": "Bearer",
    * "expires_in": lifetime in seconds,
    * "refresh_token": "refresh token here"
    * }
    *
    * @return
    */
  def token()=Action {
    implicit request =>

    LPLogger.info("AnyplaceOAuth::token():: " + request.body.toString)
    // wrap the Request into our own OAuth2Request
    val anyReq: OAuth2Request = new OAuth2Request(request)
    // ensure that a grant_type exists
    val grantType: String = anyReq.getParameter(OAuth2Constant.GRANT_TYPE)
    if (StringUtils.isBlank(grantType)) {
      OAuth2Responses.InvalidRequest("'grant_type' not found")
    }
    // make sure that we can handle the specified grant_type
    val grantHandler: IGrantHandler =
      GrantHandlerFactory.fromGrantType(grantType)
    if (grantHandler == null) {
      OAuth2Responses.UnsupportedGrantType(
        "'grant_type' requested is not supported")
    }
    // ensure that client credentials have been submitted and are valid
    val clientCredentials: ClientCredentials = anyReq.getCredentials
    if (StringUtils.isBlank(clientCredentials.client_id)) {
      OAuth2Responses.InvalidRequest("'client_id' not found")
    }
    if (StringUtils.isBlank(clientCredentials.client_secret)) {
      OAuth2Responses.InvalidRequest("'client_secret' not found")
    }
    val accountService: IAccountService = ProxyAccountService.getInstance
    val account: AccountModel = accountService.validateClient(
      clientCredentials.client_id,
      clientCredentials.client_secret,
      grantType)
    if (account == null) {
      OAuth2Responses.InvalidClient(
        "Specified client credentials are not valid")
    }
    // and return the appropriate response either the token or the error json
//    grantHandler.handleRequest(anyReq, accountService, account)
      null
  }

  // depending on the grant_type the handler will take over the procedure now
  // depending on the grant_type the handler will take over the procedure now

}
