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
import utils.JsonUtils.convertToInt
import utils.LPUtils

import scala.collection.JavaConverters.mapAsScalaMapConverter


class BuildingSet(hm: HashMap[String, String]) extends AbstractModel {

  private var json: JsValue = _

  private var lat: Double = _

  private var lng: Double = _

  private var admins: Array[String] = Array("112997031510415584062_google")

  this.fields = hm

  def this() {
    this(new HashMap[String, String])
    fields.put("owner_id", "")
    fields.put("cuid", "")
    fields.put("name", "")
    fields.put("description", "")
    fields.put("greeklish", "")
    fields.put("buids", "[]")
  }

  def this(json: JsValue) = {
    this()
    if ((json \ "owner_id").toOption.isDefined)
      fields.put("owner_id", (json \ "owner_id").as[String])
    if ((json \ "cuid").toOption.isDefined)
      fields.put("cuid", (json \ "cuid").as[String])
    if ((json \ "name").toOption.isDefined)
      isEmptyDeleteElseAdd(json, "name")
    else
      fields.remove("name")
    if ((json \ "greeklish").toOption.isDefined)
      isEmptyDeleteElseAdd(json, "greeklish")
    else
      fields.remove("greeklish")
    if ((json \ "description").toOption.isDefined) {
      isEmptyDeleteElseAdd(json, "description")
    } else {
      fields.remove("description")
    }
    this.json = json

  }

  def this(json: JsValue, owner: String) = {
    this(json)
    fields.put("owner_id", owner)
  }

  def isEmptyDeleteElseAdd(json: JsValue, key: String) {
    val temp = (json \ key).as[String]
    if (temp != "" && temp != null && temp != "-") {
      fields.put(key, temp)
    } else {
      fields.remove(key)
    }
  }

  def getId(): String = {
    var cuid: String = fields.get("cuid")
    if (cuid.isEmpty || cuid.==("")) {
      val finalId: String = LPUtils.getRandomUUID + "_" + System
        .currentTimeMillis()
      fields.put("cuid", "cuid_" + finalId)
      cuid = fields.get("cuid")
      this.json.as[JsObject] + ("cuid" -> Json.toJson(cuid))
    }
    cuid
  }

  def toValidJson(): JsonObject = {
    // initialize id if not initialized
    getId()
    JsonObject.from(this.getFields())
  }

  def toValidMongoJson(): JsValue = {
    getId()
    toJson()
  }


  def toGeoJSON(): String = {
    val sb: StringBuilder = new StringBuilder()
    json = json.as[JsObject] - "access_token"
    sb.append(this.json.toString)
    sb.toString
  }

  def addBuids(): String = {
    val sb: StringBuilder = new StringBuilder()
    if ((this.json \ "description").as[String] == "" || (this.json \ "description").as[String] == "-")
      this.json = this.json.as[JsObject] - "description"
    if ((this.json \ "name").as[String] == "" || (this.json \ "name").as[String] == "-")
      this.json = this.json.as[JsObject] - "name"
    this.json = convertToInt("_schema", this.json)
    sb.append(this.json.toString())
    sb.toString
  }

  @deprecated("unused")
  def _changeOwner(newOwnerId: String): String = {
    val sb: StringBuilder = new StringBuilder()
    var json: JsonObject = null
    try {
      this.fields.put("owner_id", newOwnerId)
      json = toValidJson()
    } catch {
      case e: IOException => e.printStackTrace()
    }
    sb.append(json.toString)
    sb.toString
  }

  def toJson(): JsValue = {
    val sMap: Map[String, String] = this.getFields().asScala.toMap
    val res = Json.toJson(sMap)
    // convert some keys to primitive types
    convertToInt("_schema", res)
  }

  @deprecated()
  def _toString(): String = toValidJson().toString

  override def toString(): String = toJson().toString()
}
