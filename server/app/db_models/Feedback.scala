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
import utils.LPUtils
import java.io.IOException
import java.util.HashMap

import com.couchbase.client.java.document.json.{JsonArray, JsonObject}
import play.api.libs.json.JsValue

class Feedback(hm: HashMap[String, String]) extends AbstractModel {

  private var json: JsonObject = _

  this.fields = hm

  def this() {
    this(new HashMap[String, String])
    fields.put("fid", "")
    fields.put("dvid", "")
    fields.put("buid", "")
    fields.put("floor", "")
    fields.put("raw_radio","")
    fields.put("gps_lat", "")
    fields.put("gps_lon", "")
    fields.put("gps_acc", "")
    fields.put("wifi_lat", "")
    fields.put("wifi_lon", "")
    fields.put("usr_lat", "")
    fields.put("usr_lon", "")  
    fields.put("timestamp", "")  
  }

  def this(json: JsonObject) {
    this()
    fields.put("dvid", json.getString("dvid"))
    fields.put("buid", json.getString("buid"))
    fields.put("floor", json.getString("floor"))
    fields.put("raw_radio",json.getString("raw_radio"))
    fields.put("gps_lat", json.getString("gps_lat"))
    fields.put("gps_lon", json.getString("gps_lon"))
    fields.put("gps_acc", json.getString("gps_acc"))
    fields.put("wifi_lat", json.getString("wifi_lat"))
    fields.put("wifi_lon", json.getString("wifi_lon"))
    fields.put("usr_lat", json.getString("usr_lat"))
    fields.put("usr_lon", json.getString("usr_lon"))
    fields.put("timestamp", json.getString("timestamp"))
    this.json = json
  }

  def this(json: JsonObject, owner: String) {
    this(json)
    fields.put("owner_id", owner)
  }

  def getId(): String = {
    var fid: String = fields.get("fid")
    if (fid == null || fid.isEmpty || fid == "") {
      val finalId = LPUtils.getRandomUUID + "_" + System.currentTimeMillis()
      fields.put("fid", "fid_" + finalId)
      fid = fields.get("fid")
      this.json.put("fid", fid)
    }
    fid
  }

  def toValidCouchJson(): JsonObject = {
    getId()
    JsonObject.from(this.getFields())
  }

  def toCouchGeoJSON(): String = {
    toValidCouchJson().toString()
  }

  override def toString(): String = toValidCouchJson().toString
}
