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
package oauth.provider.v2.granttype

import oauth.provider.v2.OAuth2Constant

import oauth.provider.v2.utils.OAuth2Responses

import oauth.provider.v2.models._

import accounts.IAccountService

import org.apache.commons.lang3.StringUtils

import play.mvc.Result

import utils.AnyResponseHelper

//remove if not needed
import scala.collection.JavaConversions._

/**
 * This class is the handler that is responsible to handle the requests
 * with grant_type specified as 'password'.
 */
class PasswordHandler extends AbstractGrantHandler {

    override def handleRequest(request: OAuth2Request,
                               accountService: IAccountService,
                               account: AccountModel): Result = {
        val clientCredentials: ClientCredentials = request.getCredentials
// ensure that username and password exists
        val username: String = request.getParameter(OAuth2Constant.USERNAME)
        if (StringUtils.isBlank(username)) {
            OAuth2Responses.InvalidRequest("'username' not provided")
        }
        val password: String = request.getParameter(OAuth2Constant.PASSWORD)
        if (StringUtils.isBlank(password)) {
            OAuth2Responses.InvalidRequest("'password' not provided")
        }
// ensure that the provided username/password relates to an account
        if (!accountService.validateAccount(account, username, password)) {
            OAuth2Responses.InvalidGrant(
                    "username:password credentials are not valid")
        }
// and the specific client currently being used
        val scope: String = request.getParameter(OAuth2Constant.SCOPE)
        if (StringUtils.isBlank(scope)) {
            OAuth2Responses.InvalidRequest("'scope' not provided")
        }
// try to issue the new access token
        val authInfo: AuthInfo = accountService.createOrUpdateAuthInfo(
                account,
                clientCredentials.client_id,
                scope)
        if (authInfo == null) {
            OAuth2Responses.InvalidGrant("Could not authorize you for this scope")
        }
//LPLogger.info("before issuing token");
        val accessTokenModel: AccessTokenModel =
                issueAccessToken(accountService, authInfo)
        if (accessTokenModel == null) {
            AnyResponseHelper.internal_server_error("Could not create access token")
        }
//LPLogger.info("new token: [" + accessTokenModel.getAccessToken() + "]");
        OAuth2Responses.ValidToken(accessTokenModel)
    }
// now we have to make sure that the scope requested is valid
// and that is included in the scope granted for the specific account
// now we have to make sure that the scope requested is valid
// and that is included in the scope granted for the specific account

}
