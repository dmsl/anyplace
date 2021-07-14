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

import java.util

/**
  * This factory is responsible to return the appropriate handler
  * to process the token request specified by the grant_type.
  */
object GrantHandlerFactory {
  private var sGrantHandlers:util.HashMap[String, IGrantHandler] = _

  /**
    * Returns the GrantHandler responsible for the grant type specified.
    * Only 'password' is currently implemented.
    *
    * @param stype Grant type needed
    * @return Either the GrantHandler for the specified type or null
    */
  def fromGrantType(stype: String): IGrantHandler = sGrantHandlers.get(stype)

  /**
    * Returns whether the specified grant type is supported by the OAuth2 provider
    *
    * @param type The grant_type in question
    * @return True if the grant_type is supported otherwise False
    */
  def isGrantTypeSupported(`type`: String): Boolean = fromGrantType(`type`) != null

  sGrantHandlers = new util.HashMap[String, IGrantHandler]
  //sGrantHandlers.put(OAuth2Constant.GRANT_TYPE_PASSWORD, new PasswordHandler)
}