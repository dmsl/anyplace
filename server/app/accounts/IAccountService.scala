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
package accounts

import oauth.provider.v2.models.AccessTokenModel

import oauth.provider.v2.models.AccountModel

import oauth.provider.v2.models.AuthInfo

// CHECK:NN CHECK:PM remove if not used
// (still has dependencies)

/**
 *
 * <p>This interface provides the necessary functions to support
 * the different flows for the OAuth2 server flows.</p>
 *
 * <p>Methods are provided in order to store and create each information
 * regarding the OAuth2 specification (RFC6479).</p>
 *
 * <p>Each Grant Type requires different method invocations from here.</p>
 *
 * <p>
 * Refresh Token Grant:<br />
 *   <ul>
 *   <li>validateClient(clientId, clientSecret, grantType)</li>
 *   <li>getAuthInfoByRefreshToken(refreshToken)</li>
 *   <li>createOrUpdateAccessToken(authInfo)</li>
 *   </ul>
 * </p>
 *
 * <p>
 * Resource Owner Password Credentials Grant:<br />
 *   <ul>
 *   <li>validateClient(clientId, clientSecret, grantType)</li>
 *   <li>validateAccount(accountModel, username, password)</li>
 *   <li>createOrUpdateAuthInfo(auid, clientId, scope)</li>
 *   <li>createOrUpdateAccessToken(authInfo)</li>
 *   </ul>
 * </p>
 *
 */
trait IAccountService {

        /**
         * Validate the client and return the result.
         * This method is called at first for all grant types.
         * You should check whether the client specified by clientId value exists
         * or not, whether the client secret is valid or not, and whether
         * the client supports the grant type or not. If there are other things
         * to have to check, they must be implemented in this method.
         *
         * @param clientId The client ID.
         * @param clientSecret The client secret string.
         * @param grantType The grant type string which the client required.
         * @return True if the client is valid.
         */
        def validateClient(clientId: String,
        clientSecret: String,
        grantType: String): AccountModel

        /**
         * Validates the Account with the provided username/password.
         * The AccountModel passed in should be the returned result of
         * validateClient() above.
         *
         * @param account The returned account from validateClient()
         * @param username The username provided by the user
         * @param password The password provided by the user
         * @return True if the credentials provided are valid against the account
         */
        def validateAccount(account: AccountModel,
        username: String,
        password: String): Boolean

        /**
         * Create or update an Authorization information.
         * This method is used when the authorization information should be created
         * or updated directly against receiving of the request in case of Client
         * Credential grant or Resource Owner Password Credential grant.
         * If the null value is returned from this method as the result, the error
         * type "invalid_grant" will be sent to the client.
         *
         * @param account The account that needs to be updated
         * @param clientId The client ID.
         * @param scope The scope string.
         * @return The created or updated the information about authorization.
         */
        def createOrUpdateAuthInfo(account: AccountModel,
        clientId: String,
        scope: String): AuthInfo

        /**
         * Create or update an Access token.
         * This method is used for all grant types. The access token is created or
         * updated based on the authInfo's property values. In generally, this
         * method never failed, because all validations should be passed before
         * this method is called.
         * @param authInfo The instance which has the information about authorization.
         * @return The created or updated access token instance or null if failed.
         */
        def createOrUpdateAccessToken(authInfo: AuthInfo): AccessTokenModel

        /**
         * Retrieve the authorization information by the refresh token string.
         * This method is used to re-issue an access token with the refresh token.
         * The authorization information which has already been stored into your
         * database should be specified by the refresh token. If you want to define
         * the expiration of the refresh token, you must check it in this
         * implementation. If the refresh token is not found, the refresh token is
         * invalid or there is other reason which the authorization information
         * should not be returned, this method must return the null value as the
         * result.
         * @param refreshToken The refresh token string.
         * @return The authorization information instance.
         */
        def getAuthInfoByRefreshToken(refreshToken: String): AuthInfo

        }
