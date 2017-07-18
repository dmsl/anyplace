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
package oauth.provider.v2

object OAuth2Constant {

    final val USE_REFRESH_TOKEN: Boolean = true

    final val USERNAME: String = "username"

    final val PASSWORD: String = "password"

    final val CLIENT_ID: String = "client_id"

    final val CLIENT_SECRET: String = "client_secret"

    final val REDIRECT_URI: String = "redirect_uri"

    final val REPSONSE_TYPE: String = "response_type"

    final val STATE: String = "state"

    final val SCOPE: String = "scope"

    final val CODE: String = "code"

    final val AUTHORIZATION_CODE: String = "authorization_code"

    final val GRANT_TYPE: String = "grant_type"

    final val ACCESS_TOKEN: String = "access_token"

    final val REFRESH_TOKEN: String = "refresh_token"

    final val TOKEN_TYPE: String = "token_type"

    final val EXPIRES_IN: String = "expires_in"

    final val ISSUED_AT: String = "issued_at"

    // default seconds for token expiration
    final val EXPIRES_IN_DEFAULT_VALUE: Long = 3600

    // Response Type final values
    final val RESPONSE_TYPE_CODE: String = "code"

    final val RESPONSE_TYPE_TOKEN: String = "token"

    // Token Type final values
    final val TOKEN_TYPE_BEARER: String = "bearer"

    final val TOKEN_TYPE_MAC: String = "mac"

    final val TOKEN_TYPE_JWT: String = "jwt"

    // Grant Type
    final val GRANT_TYPE_AUTHORIZATION_CODE: String = "authorization_code"

    final val GRANT_TYPE_PASSWORD: String = "password"

    final val GRANT_TYPE_CLIENT_CREDENTIALS: String = "client_credentials"

    final val GRANT_TYPE_REFRESH_TOKEN: String = "refresh_token"

    // Error parameters
    final val ERROR: String = "error"

    final val ERROR_DESCRIPTION: String = "error_description"

    // Request Header parameters
    final val HEADER_AUTHORIZATION: String = "Authorization"

    // TODO -
    final val RESOURCE_TOKEN_NAME: String = "resource_token"

    final val AES_ENCRYPTION_KEY: String = "oauth2provider_first1"

}
