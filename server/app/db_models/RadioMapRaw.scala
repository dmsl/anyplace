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
package db_models


import java.io.IOException
import java.util.HashMap

import com.couchbase.client.java.document.json.JsonObject
import play.api.libs.json.{JsObject, JsValue, Json}
import utils.GeoJSONPoint
import utils.JsonUtils.convertToInt

import scala.collection.JavaConverters.mapAsScalaMapConverter

object RadioMapRaw {

  def toRawRadioMapRecord(hm: HashMap[String, String]): String = {
    val sb = new StringBuilder()
    sb.append(hm.get("timestamp"))
    sb.append(" ")
    sb.append(hm.get("x"))
    sb.append(" ")
    sb.append(hm.get("y"))
    sb.append(" ")
    sb.append(hm.get("heading"))
    sb.append(" ")
    sb.append(hm.get("MAC"))
    sb.append(" ")
    sb.append(hm.get("rss"))
    sb.append(" ")
    sb.append(hm.get("floor"))
    sb.toString
  }

  def toRawRadioMapRecord(json: JsValue): String = {
    val sb = new StringBuilder()
    sb.append((json \ "timestamp").as[String])
    sb.append(" ")
    sb.append((json \ "x").as[String])
    sb.append(" ")
    sb.append((json \ "y").as[String])
    sb.append(" ")
    sb.append((json \ "heading").as[String])
    sb.append(" ")
    sb.append((json \ "MAC").as[String])
    sb.append(" ")
    sb.append((json \ "rss").as[String])
    sb.append(" ")
    sb.append((json \ "floor").as[String])
    sb.toString
  }
}

class RadioMapRaw(h: HashMap[String, String]) extends AbstractModel {

  this.fields = h

  def this(timestamp: String,
           x: String,
           y: String,
           heading: String
          ) {
    this(new HashMap[String, String])
    fields.put("timestamp", timestamp)
    fields.put("x", x)
    fields.put("y", y)
    fields.put("heading", heading)
    fields.put("floor", "-")
  }

  def this(timestamp: String,
           x: String,
           y: String,
           heading: String,

           floor: String) {
    this(new HashMap[String, String])
    fields.put("timestamp", timestamp)
    fields.put("x", x)
    fields.put("y", y)
    fields.put("heading", heading)
    fields.put("floor", floor)
  }

  def this(timestamp: String,
           x: String,
           y: String,
           heading: String,
           floor: String,
           strongestWifi: String) {
    this(new HashMap[String, String])
    fields.put("timestamp", timestamp)
    fields.put("x", x)
    fields.put("y", y)
    fields.put("heading", heading)
    fields.put("floor", floor)
    fields.put("strongestWifi", strongestWifi)
  }

  def this(timestamp: String,
           x: String,
           y: String,
           heading: String,
           floor: String,
           strongestWifi: String,
           buid: String) {
    this(new HashMap[String, String])
    fields.put("timestamp", timestamp)
    fields.put("x", x)
    fields.put("y", y)
    fields.put("heading", heading)
    fields.put("floor", floor)
    fields.put("strongestWifi", strongestWifi)
    fields.put("buid", buid)
  }

  def getId(): String = {
    fields.get("x") + fields.get("y") + fields.get("heading") +
      fields.get("timestamp") +
      fields.get("MAC")
  }

  def toValidJson(): JsonObject = {
    JsonObject.from(this.getFields())
  }

  def toValidMongoJson(): JsValue = {
    toJson()
  }

  def addMeasurements(measurements: List[List[String]]): String =  {
    val sb = new StringBuilder()
    var json = toValidMongoJson()
    try {
      json = json.as[JsObject] + ("measurements" -> Json.toJson(measurements))
      json = json.as[JsObject] + ("geometry" -> Json.toJson(
        new GeoJSONPoint(java.lang.Double.parseDouble(fields.get("x")),
          java.lang.Double.parseDouble(fields.get("y"))).toGeoJSON()))
    } catch {
      case e: IOException => e.printStackTrace()
    }
    sb.append(json.toString)
    sb.toString
  }

  def toGeoJSON(): String = {
    val sb = new StringBuilder()
    var json = toValidMongoJson()
    try {
      json = json.as[JsObject] + ("geometry" -> Json.toJson(
        new GeoJSONPoint(java.lang.Double.parseDouble(fields.get("x")),
          java.lang.Double.parseDouble(fields.get("y"))).toGeoJSON()))
    } catch {
      case e: IOException => e.printStackTrace()
    }
    sb.append(json.toString)
    sb.toString
  }

  def toJson(): JsValue = {
    val sMap: Map[String, String] = this.getFields().asScala.toMap
    val res = Json.toJson(sMap)
    convertToInt("_schema", res)
  }

  @deprecated("")
  def _toString(): String = this.toValidJson().toString
  override def toString(): String = toJson().toString()

  def toRawRadioMapRecord(): String = {
    val sb = new StringBuilder()
    sb.append(fields.get("timestamp"))
    sb.append(" ")
    sb.append(fields.get("x"))
    sb.append(" ")
    sb.append(fields.get("y"))
    sb.append(" ")
    sb.append(fields.get("heading"))
    sb.append(" ")
    sb.append(fields.get("MAC"))
    sb.append(" ")
    sb.append(fields.get("rss"))
    sb.append(" ")
    sb.append(fields.get("floor"))
    sb.toString
  }
}
