/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Nikolas Neofytou, Constantinos Costa, Kyriakos Georgiou, Lambros Petrou
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
package utils

import java.util
import java.util.{Collections, HashMap}

import com.google.gson.Gson
import datasources.SCHEMA
import play.api.libs.json.{JsArray, JsNumber, JsObject, JsValue, Json}

object JsonUtils {


  def getHashMapStrStr(json: JsValue): HashMap[String, String] = {
    val gson: Gson = new Gson()
    gson.fromJson(json.toString(), (new HashMap[String, String]()).getClass)
  }

  def hasProperties(json: JsValue, keys: String*): util.List[String] = {
    if (json == null) {
      throw new IllegalArgumentException("No source Json object or destination Map object can be null!")
    }
    if (keys == null) {
      return Collections.emptyList()
    }
    val notFound = new util.ArrayList[String]()
    for (k <- keys) {
      val value = json \ k
      if (value == null)
        notFound.add(k)
      else {
        val svalue = value.asOpt[String].getOrElse("")
        if (svalue.isEmpty) {
          val avalue = value.asOpt[JsArray].orNull
          if (avalue == null) {
            val ovalue = value.asOpt[JsValue].orNull
            if (ovalue == null)
            notFound.add(k)
          }
        }
      }
    }
    notFound
  }

  def hasProperty(json: JsValue, key: String): Boolean = {
    if (json == null) {
      throw new IllegalArgumentException("No source Json object or destination Map object can be null!")
    }
    if (key == null) {
      return false
    }
    val value = json \ key
    return value.toOption.isDefined
  }

  def convertToInt(key: String, json: JsValue): JsValue = {
    var value = "0"
    if ((json \ key).toOption.isDefined)
      value = (json \ key).as[String]
    json.as[JsObject] + (key -> JsNumber(value.toInt))
  }

  def isNullOrEmpty(x: JsValue): Boolean = {
    if (x == null)
      return true
    if (Json.stringify(x) == "{}" || Json.stringify(x) == "")
      return true
    return false
  }

  def cleanupMongoJson(json: JsValue): JsValue = {
    return json.as[JsObject] - SCHEMA.fId - SCHEMA.fSchema
  }

}
