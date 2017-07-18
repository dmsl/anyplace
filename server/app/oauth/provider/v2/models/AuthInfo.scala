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


/**
  * Created by lambros on 2/4/14.
  */
class AuthInfo {

    private var auid: String = _
    private var client_id: String = _
    private var scope: String = _
    private var refresh_token: String = _

    // NOT USED AT THE MOMENT
    private var code : String = _
    private var redirect_uri : String = _

    def this(sauid: String, sclient_id: String, sscope: String) {
        this()
        this.auid = sauid
        this.client_id = sclient_id
        this.scope = sscope
    }

    def getAuid: String = auid

    def setAuid(auid: String): Unit = {
        this.auid = auid
    }

    def getClientId: String = client_id

    def setClientId(client_id: String): Unit = {
        this.client_id = client_id
    }

    def getScope: String = scope

    def setScope(scope: String): Unit = {
        this.scope = scope
    }

    def getRefreshToken: String = refresh_token

    def setRefreshToken(refresh_token: String): Unit = {
        this.refresh_token = refresh_token
    }

    def getCode: String = code

    def setCode(scode: String): Unit = {
        this.code = scode
    }

    def getRedirectUri: String = redirect_uri

    def setRedirectUri(sredirect_uri: String): Unit = {
        this.redirect_uri = sredirect_uri
    }

    override def toString: String = String.format("AuthInfo: auid[%s] client_id[%s]", this.getAuid, this.getClientId)

}
