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
import java.util.{ArrayList, Collections, HashMap}

import com.couchbase.client.java.document.json.JsonObject
import com.google.gson.Gson
import play.api.libs.json.{JsArray, JsNumber, JsObject, JsValue, Json}

object JsonUtils {

  def getHashMapStrStr(jsonString: String): HashMap[String, String] = {
    val json = JsonObject.fromJson(jsonString)
    json.toMap.asInstanceOf[HashMap[String, String]]

  }

  def getHashMapStrStr(json: JsValue): HashMap[String, String] = {
    val gson: Gson = new Gson()
    gson.fromJson(json.toString(), (new HashMap[String, String]()).getClass)
  }

  def fillMapFromJson(json: JsonObject, map: HashMap[String, String], keys: String*): util.List[String] = {
    if (json == null || map == null) {
      throw new IllegalArgumentException("No source Json object or destination Map object can be null!")
    }
    if (keys == null) {
      return Collections.emptyList()
    }
    val notFound = new ArrayList[String]()
    for (k <- keys) {
      val value = json.getString(k)
      if (value == null) notFound.add(k) else map.put(k, value)
    }
    notFound
  }

  def requirePropertiesInJson(json: JsonObject, keys: String*): util.List[String] = {
    if (json == null) {
      throw new IllegalArgumentException("No source Json object or destination Map object can be null!")
    }
    if (keys == null) {
      return Collections.emptyList()
    }
    val notFound = new util.ArrayList[String]()
    for (k <- keys) {
      val value = json.getString(k)
      if (value == null || 0 == value.trim().length) notFound.add(k)
    }
    notFound
  }

  def requirePropertiesInJson(json: JsValue, keys: String*): util.List[String] = {
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
    return json.as[JsObject] - "_id" - "_schema"
  }
  @deprecated("mdb")
  def toCouchObject(json: JsValue): JsonObject = {
    val sJson = json.toString
    JsonObject.fromJson(sJson)
  }

  @deprecated("mdb")
  def fromCouchObject(json: JsonObject): JsValue = {
    val sJson = json.toString
    Json.parse(sJson)
  }

  @deprecated("mdb")
  def toCouchArray(json: Array[JsValue]): Array[JsonObject] = {
    val temp = new ArrayList[JsonObject]()
    for (i <- json) {
      temp.add(toCouchObject(i))
    }
    temp.asInstanceOf[Array[JsonObject]]
  }

  @deprecated("mdb")
  def fromCouchList(json: java.util.ArrayList[JsonObject]): List[JsValue] = {
    val temp = new ArrayList[JsValue]()
    for (i <- 0 until json.size()) {
      temp.add(fromCouchObject(json.get(i)))
    }
    temp.asInstanceOf[List[JsValue]]
  }

  @deprecated("mdb")
  def toCouchList(json: util.List[JsValue]): util.List[JsonObject] = {
    val list: util.List[JsonObject] = null
    for (i <- 1 until json.size())
      list.add(toCouchObject(json.get(i)))
    list
  }

}
