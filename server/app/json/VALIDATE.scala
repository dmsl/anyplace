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

import java.util

import com.google.common.collect.Lists
import datasources.SCHEMA._
import play.api.libs.json.{JsLookupResult, JsResultException, JsValue}
import play.api.mvc.Result
import utils.{AnyResponseHelper, JsonUtils}

import scala.jdk.CollectionConverters.CollectionHasAsScala

object VALIDATE {

  /**
   * Validates if the json contains a value according to a key as String.
   *
   * @param json
   * @return json if value is validated otherwise null.
   */
  def String(json: JsValue, key: String): JsValue = {
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
   *
   * @param json
   * @param key
   * @return json if value is validated otherwise null.
   */
  def StringNumber(json: JsValue, key: String): JsValue = {
    if (String(json, key) == null) return null
    var temp = (json \ key).as[String]
    if (temp.charAt(0) == '-') // ignore - if the number is negative
      temp = temp.substring(1)
    if (isAllDigits(temp) && temp.size > 0)
      return json
    null
  }

  def Coordinate(json: JsValue, key: String): JsValue = {
    if (String(json, key) == null) return null
    val temp = (json \ key).as[String]
    try {
      temp.toFloat
    } catch {
      case e: NumberFormatException => return null
    }
    json
  }

  def Int(json: JsValue, key: String): JsValue = {
    try {
      (json \ key).as[Int]
    } catch {
      case e: JsResultException => return null
    }
    json
  }

  def buid(json: JsValue, key: String): String = {
    if (String(json, key) == null) return key + " field must be String."
    null
  }

  def userFields(json: JsValue, key: String, value: JsLookupResult): String = {
    if (String(json, key) == null) return "Credentials must be String."
    if (value.as[String].length == 0) return key + " can not be empty."
    null
  }

  def floor(json: JsValue, key: String): String = {
    if (StringNumber(json, key) == null) return "floor field must be String, containing a number."
    null
  }

  def newOwner(json: JsValue): String = {
    if (String(json, "newOwner") == null) return "new_owner field must be String!"
    null
  }

  def timestamp(json: JsValue, key: String): String = {
    if (StringNumber(json, key) == null) return key + " field must be String, containing a number."
    null
  }

  def xyz(json: JsValue, key: String): String = {
    if (Int(json, key) == null) return key + " field must be an integer."
    null
  }

  def coordinates(json: JsValue, key: String): String = {
    if (Coordinate(json, key) == null) return key + " field must be String, containing a coordinate."
    null
  }

  def stringField(json: JsValue, key: String): String = {
    if (String(json, key) == null) return key + " field must be String."
    null
  }

  def fields(json: JsValue, keys: String*): Validation = {
    if (json == null) return new Validation(Lists.newArrayList("empty json"))
    if (keys == null) return new Validation(Lists.newArrayList("empty keys"))
    val errors = new util.ArrayList[String]()
    for (k <- keys) {
      val value = json \ k
      if (k == fBuid || k == fBuidA || k == fBuidB) {
        val r = buid(json, k)
        if (r != null) errors.add(r)
      } else if (k == fFloor || k == fFloorA || k == fFloorB) {
        val r = floor(json, k)
        if (r != null) errors.add(r)
      } else if (k == fTimestampY || k == fTimestampX || k == fTimestampY) {
        val r = timestamp(json, k)
        if (r != null) errors.add(r)
      } else if (k == fY || k == fX || k == fY) {
        val r = xyz(json, k)
        if (r != null) errors.add(r)
      } else if (k == "lat1" || k == "lat2" || k == "lon1" || k == "lon2" || k == fCoordinatesLat || k == fCoordinatesLon) {
        val r = coordinates(json, k)
        if (r != null) errors.add(r)
      } else if (k == "new_owner") {
        val r = newOwner(json)
        if (r != null) errors.add(r)
      } else if (k == fUsername || k == fPassword || k == fEmail || k == fName) {
        val r = userFields(json, k, value)
        if (r != null) errors.add(r)
      } else if (k == fPoisA || k == fPoisB || k == "pois_from" || k == "pois_to" || k == fCampusCuid) {
        val r = stringField(json, k)
        if (r != null) errors.add(r)
      }
    }
    new Validation(errors)
  }

  def checkRequirements(json: JsValue, keys: String*): Result = {
    for (key <- keys) {
      val requiredMissing = JsonUtils.hasProperties(json, key)
      if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
      val validation = VALIDATE.fields(json, key)
      if (validation.failed()) return validation.response()
    }
    null
  }


  class Validation(private val errors: java.util.List[String]) {
    def failed():Boolean = !errors.isEmpty

    def response(): Result = {
      var str = ""
      for (error:String <- errors.asScala) str += error + "\n"
      AnyResponseHelper.bad_request("ERROR: Validation:\n" + str)
    }
  }

}
