/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): 
 *
 * Supervisor: 
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
import utils.LPUtils
import java.io.IOException
import java.util.HashMap

import com.couchbase.client.java.document.json.{JsonArray, JsonObject}
import play.api.libs.json.JsValue

class LocHistory(hm: HashMap[String, String]) extends AbstractModel {

  private var json: JsonObject = _

  this.fields = hm

  def this() {
    this(new HashMap[String, String])
    fields.put("obid", "")
    fields.put("objcat", "")
    fields.put("dvid", "")
    fields.put("buid", "")
    fields.put("floor", "")
    fields.put("coordinates_lat", "")
    fields.put("coordinates_lon", "")
    fields.put("timestamp", "")
    fields.put("radio_map", "")
    fields.put("lhistid", "")
  }

  def this(obid: String, objectCat: String, dvid: String, buid: String, floor: String, x: String, y: String, timestamp: String, radiomap: String) {
    this()
    fields.put("obid", obid)
    fields.put("objcat", objectCat)
    fields.put("dvid", dvid)
    fields.put("buid",buid)
    fields.put("floor", floor)
    fields.put("coordinates_lat", x)
    fields.put("coordinates_lon", y)
    fields.put("timestamp", timestamp)
    fields.put("radio_map", radiomap)
    this.json = json
  }

  def getId(): String = {
    var lhistid: String = fields.get("lhistid")
    if (lhistid == null || lhistid.isEmpty || lhistid == "") {
      val finalId = LPUtils.getRandomUUID + "_" + System.currentTimeMillis()
      fields.put("lhistid", "lochist_" + finalId)
      lhistid = fields.get("lhistid")
    }
    lhistid
  }

  def toValidCouchJson(): JsonObject = {
    getId()
    JsonObject.from(this.getFields())
  }
  
  def toCouchGeoJSON(): String = {
    ""
  }

  override def toString(): String = toValidCouchJson().toString
}
