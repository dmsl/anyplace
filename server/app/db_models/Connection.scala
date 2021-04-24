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

import java.util.HashMap

import com.couchbase.client.java.document.json.JsonObject
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import utils.JsonUtils.convertToInt

import scala.collection.JavaConverters.mapAsScalaMapConverter

object Connection {

  val EDGE_TYPE_STAIR = "stair"

  val EDGE_TYPE_ELEVATOR = "elevator"

  val EDGE_TYPE_HALLWAY = "hallway"

  val EDGE_TYPE_ROOM = "room"

  val EDGE_TYPE_OUTDOOR = "outdoor"

  def getId(pois_a: String, pois_b: String): String = "conn_" + pois_a + "_" + pois_b
}

class Connection(hm: HashMap[String, String]) extends AbstractModel {

  private var json: JsValue = _

  this.fields = hm

  def this() {
    this(new HashMap[String, String])
    fields.put("is_published", "")
    fields.put("edge_type", "")
    fields.put("pois_a", "")
    fields.put("pois_b", "")
    fields.put("weight", "")
    fields.put("buid", "")
    fields.put("floor_a", "")
    fields.put("floor_b", "")
    fields.put("buid_a", "")
    fields.put("buid_b", "")
    fields.put("cuid", "")
  }

  def this(json: JsValue) {
    this()
    if ((json \ "is_published").toOption.isDefined)
      fields.put("is_published", (json \ "is_published").as[String])
    if ((json \ "edge_type").toOption.isDefined)
      fields.put("edge_type", (json \ "edge_type").as[String])
    if ((json \ "pois_a").toOption.isDefined)
      fields.put("pois_a", (json \ "pois_a").as[String])
    if ((json \ "pois_b").toOption.isDefined)
      fields.put("pois_b", (json \ "pois_b").as[String])
    if ((json \ "weight").toOption.isDefined)
      fields.put("weight", (json \ "weight").as[String])
    if ((json \ "buid").toOption.isDefined)
      fields.put("buid", (json \ "buid").as[String])
    if ((json \ "floor_a").toOption.isDefined)
      fields.put("floor_a", (json \ "floor_a").as[String])
    if ((json \ "floor_b").toOption.isDefined)
      fields.put("floor_b", (json \ "floor_b").as[String])
    if ((json \ "buid_a").toOption.isDefined)
      fields.put("buid_a", (json \ "buid_a").as[String])
    if ((json \ "buid_b").toOption.isDefined)
      fields.put("buid_b", (json \ "buid_b").as[String])
    if ((json \ "cuid").toOption.isDefined)
      fields.put("cuid", (json \ "cuid").as[String])
    this.json = json
  }

  def getId(): String = {
    var cuid: String = fields.get("cuid")
    if (cuid == null || cuid.isEmpty || cuid == "") {
      cuid = Connection.getId((json \ "pois_a").as[String], (json \ "pois_b").as[String])
      fields.put("cuid", cuid)
      this.json = this.json.as[JsObject] + ("cuid" -> JsString(cuid))
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

  def toJson(): JsValue = {
    val sMap: Map[String, String] = this.getFields().asScala.toMap
    val res = Json.toJson(sMap)
    // convert some keys to primitive types
    convertToInt("_schema", res)
  }

  override def toGeoJSON(): String = toJson().toString

  override def toString(): String = this.toJson().toString
}
