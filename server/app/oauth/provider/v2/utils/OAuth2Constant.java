/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Lambros Petrou
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

package oauth.provider.v2.utils;

public class OAuth2Constant {

    public static final boolean USE_REFRESH_TOKEN  = true;
    public final String AES_ENCRYPTION_KEY = "oauth2provider_first1";

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String REDIRECT_URI = "redirect_uri";
    public static final String REPSONSE_TYPE = "response_type";
    public static final String STATE = "state";
    public static final String SCOPE = "scope";
    public static final String CODE = "code";
    public static final String AUTHORIZATION_CODE = "authorization_code";
    public static final String GRANT_TYPE = "grant_type";
    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String TOKEN_TYPE = "token_type";
    public static final String EXPIRES_IN = "expires_in";
    public static final String ISSUED_AT = "issued_at";

    // default seconds for token expiration
    public static final long EXPIRES_IN_DEFAULT_VALUE = 3600;

    // Response Type values
    public static final String RESPONSE_TYPE_CODE = "code";
    public static final String RESPONSE_TYPE_TOKEN = "token";

    // Token Type values
    public static final String TOKEN_TYPE_BEARER = "bearer";
    public static final String TOKEN_TYPE_MAC = "mac";
    public static final String TOKEN_TYPE_JWT = "jwt";

    // Grant Type
    public static final String GRANT_TYPE_AUTHORIZATION_CODE = "authorization_code";
    public static final String GRANT_TYPE_PASSWORD = "password";
    public static final String GRANT_TYPE_CLIENT_CREDENTIALS = "client_credentials";
    public static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";

    // Error parameters
    public static final String ERROR = "error";
    public static final String ERROR_DESCRIPTION = "error_description";

    // Request Header parameters
    public static final String HEADER_AUTHORIZATION = "Authorization";

    // TODO -
    public static final String RESOURCE_TOKEN_NAME = "resource_token";

}
