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

public class OAuth2ErrorConstant {

    public static final String INVALID_REQUEST = "invalid request";
    public static final String INVALID_CLIENT = "invalid client";
    public static final String INVALID_GRANT = "invalid grant";
    public static final String UNSUPPORTED_GRANT_TYPE = "unsupported grant type";
    public static final String INVALID_SCOPE = "invalid scope";
    public static final String INVALID_CODE = "invalid code";
    public static final String INVALID_USER = "invalid user";
    public static final String UNAUTHORIZED_CLIENT = "unauthorized client";
    public static final String ACCESS_DENIED = "access denied";

    public static final String UNSUPPORTED_RESPONSE_TYPE = "unsupported_response_type";
    public static final String SERVER_ERROR = "server_error";
    public static final String TEMPORARILY_UNAVAILABLE = "temporarily_unavailable";
    public static final String NOT_MATCH_REDIRECT_URI = "redirect_uri mismatched";
    public static final String INVALID_TOKEN = "invalid access token or refresh token";
    public static final String EXPIRED_TOKEN = "expired token";
    public static final String INVALID_PARAMETER = "invalid parameter";
    public static final String INSUFFICIENT_SCOPE = "insufficient scope";

}
