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

import datasources.CouchbaseDatasource

import oauth.provider.v2.models.AccessTokenModel

import oauth.provider.v2.models.AccountModel

import oauth.provider.v2.models.AuthInfo

object ProxyAccountService {

private var sInstance: ProxyAccountService = _

        def getInstance(): ProxyAccountService = {
        if (sInstance == null) {
        sInstance = new ProxyAccountService()
        }
        sInstance
        }

        }

/**
 * This class acts as a proxy to the User Service implementation.
 */
class ProxyAccountService private () extends IAccountService {

private var mCouchbase: CouchbaseDatasource =
        CouchbaseDatasource.getStaticInstance

        override def validateClient(clientId: String,
        clientSecret: String,
        grantType: String): AccountModel =
        mCouchbase.validateClient(clientId, clientSecret, grantType)

        override def validateAccount(account: AccountModel,
        username: String,
        password: String): Boolean =
        mCouchbase.validateAccount(account, username, password)

        override def createOrUpdateAuthInfo(account: AccountModel,
        clientId: String,
        scope: String): AuthInfo =
        mCouchbase.createOrUpdateAuthInfo(account, clientId, scope)

        override def getAuthInfoByRefreshToken(refreshToken: String): AuthInfo =
        mCouchbase.getAuthInfoByRefreshToken(refreshToken)

        override def createOrUpdateAccessToken(
        authInfo: AuthInfo): AccessTokenModel =
        mCouchbase.createOrUpdateAccessToken(authInfo)

        }
