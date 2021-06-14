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
package oauth.provider.v2.token

import oauth.provider.v2.OAuth2Constant
import oauth.provider.v2.models.AccessTokenModel
import oauth.provider.v2.models.AuthInfo
import utils.LPLogger
import utils.LPUtils
import java.util.Date

/**
  * The service responsible to create unique tokens and client ids.
  */
object TokenService { // Used by the secure encryption/decryption algorithms
  private val SECURE_PASSWORD_PHASE1 = "anyplaceT_O_K_3_n-P@@" // 21 length
  private val SECURE_PASSWORD_PHASE2 = "anyCALI.*_O_K_3_n-P$@"

  /**
    * Creates a new access token according to the specified AuthInfo.
    * The token structure is as follows:
    *
    * PHASE 1 ::
    * signature = <auid>]<scope>]<client_id>]<timestamp>
    * random = random string generated
    *
    * PHASE 2 ::
    * encrypted_token = secureEncrypt( random + "." + signature )
    *
    * @param authInfo
    * @return
    */
  def createNewAccessToken(authInfo: AuthInfo): AccessTokenModel = {
    val timeNow = new Date().getTime
    val plainText = authInfo.getAuid + "]" + authInfo.getScope + "]" + authInfo.getClientId + "]" + String.valueOf(timeNow)
    LPLogger.info("plaintext    : >" + plainText + "<")
    val randomString = LPUtils.generateRandomToken
    LPLogger.info("randomstr PH1: >" + randomString + "<")
    val encrypted_token = LPUtils.secureEncrypt(SECURE_PASSWORD_PHASE1, randomString + "." + plainText)
    LPLogger.info("encrypted PH2: >" + encrypted_token + "<")
    new AccessTokenModel(encrypted_token, OAuth2Constant.TOKEN_TYPE_BEARER, OAuth2Constant.EXPIRES_IN_DEFAULT_VALUE, authInfo.getRefreshToken, authInfo.getScope, authInfo.getAuid, authInfo.getClientId, timeNow)
  }

  /**
    * Creates a new access token according to the specified AuthInfo.
    * The token structure is as follows:
    *
    * PHASE 1 ::
    * signature = <auid>]<scope>]<client_id>]<timestamp>
    * encryptedSignature = secureEncrypt(signature)
    *
    * random = random string generated
    *
    * PHASE 2 ::
    * encrypted_token = secureEncrypt( random + "." + encryptedSignature )
    *
    * @param authInfo
    * @return
    */
  def createNewAccessToken2(authInfo: AuthInfo): AccessTokenModel = {
    val timeNow = new Date().getTime
    val plainText = authInfo.getAuid + "]" + authInfo.getScope + "]" + authInfo.getClientId + "]" + String.valueOf(timeNow)
    LPLogger.info("plaintext    : >" + plainText + "<")
    val signature = LPUtils.secureEncrypt(SECURE_PASSWORD_PHASE1, plainText)
    LPLogger.info("signature PH1: >" + signature + "<")
    val randomString = LPUtils.generateRandomToken
    LPLogger.info("randomstr PH1: >" + randomString + "<")
    val encrypted_token = LPUtils.secureEncrypt(SECURE_PASSWORD_PHASE2, randomString + "." + signature)
    LPLogger.info("encrypted PH2: >" + encrypted_token + "<")
    new AccessTokenModel(encrypted_token, OAuth2Constant.TOKEN_TYPE_BEARER, OAuth2Constant.EXPIRES_IN_DEFAULT_VALUE, authInfo.getRefreshToken, authInfo.getScope, authInfo.getAuid, authInfo.getClientId, timeNow)
  }

  /**
    * TODO:NN rename createUserId
    * Return a new Client Id
    */
  def createNewClientId: String = LPUtils.getRandomUUID
}