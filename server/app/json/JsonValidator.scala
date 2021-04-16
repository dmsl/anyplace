/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Nikolas Neofytou
 *
 * Co-Supervisor: Paschalis Mpeis
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
package json

import play.api.libs.json.{JsResultException, JsValue}

object JsonValidator {

  /**
   * Validates if the json contains a value according to a key as String.
   * @param json
   * @return json if value is validated otherwise null.
   */
  def validateString(json: JsValue, key: String): JsValue = {
    if ((json \ key).toOption.isDefined) {
      try {
        (json \ key).as[String]
      } catch {
        case e: JsResultException => return null
      }
      return json
    }
    null
  }

  /**
   * @param x
   * @return true if all characters of a string are numbers, otherwise false.
   */
  def isAllDigits(x: String) = x forall Character.isDigit

  /**
   * Validates if a value according to a key is type of String containing a number
   * @param json
   * @param key
   * @return json if value is validated otherwise null.
   */
  def validateStringNumber(json: JsValue, key: String): JsValue = {
    if (validateString(json, key) == null) return null
    var temp = (json \ key).as[String]
    if (temp.charAt(0) == '-') // ignore - if the number is negative
      temp = temp.substring(1)
    if (isAllDigits(temp) && temp.size > 0 )
      return json
    null
  }

  def validateCoordinate(json: JsValue, key: String): JsValue = {
    if (validateString(json, key) == null) return null
    val temp = (json \ key).as[String]
    try {
      temp.toFloat
    } catch {
      case e: NumberFormatException => return null
    }
    json
  }
}
