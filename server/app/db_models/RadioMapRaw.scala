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
package db_models


import utils.GeoJSONPoint
import java.io.IOException
import java.util.HashMap

import com.couchbase.client.java.document.json.JsonObject

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

  def toRawRadioMapRecord(json: JsonObject): String = {
    val sb = new StringBuilder()
    sb.append(json.getString("timestamp"))
    sb.append(" ")
    sb.append(json.getString("x"))
    sb.append(" ")
    sb.append(json.getString("y"))
    sb.append(" ")
    sb.append(json.getString("heading"))
    sb.append(" ")
    sb.append(json.getString("MAC"))
    sb.append(" ")
    sb.append(json.getString("rss"))
    sb.append(" ")
    sb.append(json.getString("floor"))
    sb.toString
  }
}

class RadioMapRaw(h: HashMap[String, String]) extends AbstractModel {

  this.fields = h

  def this(timestamp: String,
           x: String,
           y: String,
           heading: String,
           MAC_addr: String,
           rss: String) {
    this(new HashMap[String, String])
    fields.put("timestamp", timestamp)
    fields.put("x", x)
    fields.put("y", y)
    fields.put("heading", heading)
    fields.put("MAC", MAC_addr)
    fields.put("rss", rss)
    fields.put("floor", "-")
  }

  def this(timestamp: String,
           x: String,
           y: String,
           heading: String,
           MAC_addr: String,
           rss: String,
           floor: String) {
    this(new HashMap[String, String])
    fields.put("timestamp", timestamp)
    fields.put("x", x)
    fields.put("y", y)
    fields.put("heading", heading)
    fields.put("MAC", MAC_addr)
    fields.put("rss", rss)
    fields.put("floor", floor)
  }

  def this(timestamp: String,
           x: String,
           y: String,
           heading: String,
           MAC_addr: String,
           rss: String,
           floor: String,
           strongestWifi: String) {
    this(new HashMap[String, String])
    fields.put("timestamp", timestamp)
    fields.put("x", x)
    fields.put("y", y)
    fields.put("heading", heading)
    fields.put("MAC", MAC_addr)
    fields.put("rss", rss)
    fields.put("floor", floor)
    fields.put("strongestWifi", strongestWifi)
  }

  def this(timestamp: String,
           x: String,
           y: String,
           heading: String,
           MAC_addr: String,
           rss: String,
           floor: String,
           strongestWifi: String,
           buid: String) {
    this(new HashMap[String, String])
    fields.put("timestamp", timestamp)
    fields.put("x", x)
    fields.put("y", y)
    fields.put("heading", heading)
    fields.put("MAC", MAC_addr)
    fields.put("rss", rss)
    fields.put("floor", floor)
    fields.put("strongestWifi", strongestWifi)
    fields.put("buid", buid)
  }

  def getId(): String = {
    fields.get("x") + fields.get("y") + fields.get("heading") +
      fields.get("timestamp") +
      fields.get("MAC")
  }

  def toValidCouchJson(): JsonObject = {
    // initialize id if not initialized
    getId
    JsonObject.from(this.getFields())
  }

  def toCouchGeoJSON(): String = {
    val sb = new StringBuilder()
    var json: JsonObject = null
    try {
      json =  JsonObject.from(this.getFields())
      json.put("geometry", new GeoJSONPoint(java.lang.Double.parseDouble(fields.get("x")), java.lang.Double.parseDouble(fields.get("y")))
        .toGeoJSON())
    } catch {
      case e: IOException => e.printStackTrace()
    }
    sb.append(json.toString)
    sb.toString
  }

  override def toString(): String = this.toValidCouchJson().toString

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
