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
package utils

import java.util
import java.util.ArrayList
import java.util.Collections
import java.util.HashMap
import java.util.List

import com.couchbase.client.java.document.json.JsonObject
import play.api.libs.json.JsValue

object JsonUtils {

  def getHashMapStrStr(jsonString: String): HashMap[String, String] = {
    val json = JsonObject.fromJson(jsonString)
    json.toMap.asInstanceOf[HashMap[String, String]]

  }

  def getHashMapStrStr(json: JsonObject): HashMap[String, String] = {
    json.toMap.asInstanceOf[HashMap[String, String]]

  }

  def fillMapFromJson(json: JsonObject, map: HashMap[String, String], keys: String*): List[String] = {
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
      val value = (json \ k).toString
      if (value == null || 0 == value.trim().length) notFound.add(k)
    }
    notFound
  }
}
