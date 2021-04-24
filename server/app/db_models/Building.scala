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
import java.util
import java.util.HashMap

import com.couchbase.client.java.document.json.JsonObject
import play.api.libs.json._
import play.twirl.api.TemplateMagic.javaCollectionToScala
import utils.JsonUtils.convertToInt
import utils.{GeoJSONPoint, LPUtils}

import scala.collection.JavaConverters.mapAsScalaMapConverter

class Building(hm: HashMap[String, String]) extends AbstractModel {
  private var json: JsValue = _
  private var lat: Double = _
  private var lng: Double = _
  private var co_owners = JsArray()
  this.fields = hm

  def this() {
    this(new HashMap[String, String])
    fields.put("buid", "")
    fields.put("is_published", "")
    fields.put("name", "")
    fields.put("description", "")
    fields.put("url", "")
    fields.put("address", "")
    fields.put("coordinates_lat", "")
    fields.put("coordinates_lon", "")
    fields.put("bucode", "")
  }

  def this(json: JsValue) {
    this()
    fields.put("owner_id", (json \ "owner_id").as[String])
    if ((json \ "buid").toOption.isDefined)
      fields.put("buid", (json \ "buid").as[String])
    if ((json \ "is_published").toOption.isDefined && (json \ "is_published") != JsDefined(JsNull)) {
      val temp = (json \ "is_published").as[String]
      if (!temp.equals(""))
        fields.put("is_published", temp)
    }
    fields.put("name", (json \ "name").as[String])
    cleanupFieldKey(json, "description")
    cleanupFieldKey(json, "url")
    cleanupFieldKey(json, "address")
    fields.put("coordinates_lat", (json \ "coordinates_lat").as[String])
    fields.put("coordinates_lon", (json \ "coordinates_lon").as[String])
    cleanupFieldKey(json, "bucode")
    if ((json \ "co_owners").toOption.isDefined)
      co_owners = (json \ "co_owners").as[JsArray]
    this.json = cleanupJson(json)
    this.lat = java.lang.Double.parseDouble((json \ "coordinates_lat").as[String])
    this.lng = java.lang.Double.parseDouble((json \ "coordinates_lon").as[String])
  }

  def this(json: JsValue, owner: String) {
    this(json)
    fields.put("owner_id", owner)
  }

  def getId(): String = {
    var buid: String = fields.get("buid")
    if (buid == null || buid.isEmpty || buid == "") {
      val finalId = LPUtils.getRandomUUID + "_" + System.currentTimeMillis()
      fields.put("buid", "building_" + finalId)
      buid = fields.get("buid")
      this.json.as[JsObject] + ("buid" -> Json.toJson(buid))
    }
    buid
  }

  def hasAccess(ownerId: String): Boolean = {
    if (fields.get("owner_id") == ownerId) return true
    co_owners.value.foreach(coOwner => if (coOwner == ownerId) return true)

    false
  }

  def toGeoJSON(): String = {
    val sb = new StringBuilder()
    var json = toValidMongoJson()
    try {
      json = json.as[JsObject] + ("geometry" -> Json.toJson(
        new GeoJSONPoint(java.lang.Double.parseDouble(fields.get("coordinates_lat")),
          java.lang.Double.parseDouble(fields.get("coordinates_lon"))).toGeoJSON()))
    } catch {
      case e: IOException => e.printStackTrace()
    }
    sb.append(json.toString)
    sb.toString
  }

  @deprecated
  def toValidJson(): JsonObject = {
    // initialize id if not initialized
    getId()
    JsonObject.from(this.getFields()).put("co_owners", co_owners)
  }

  def toValidMongoJson(): JsValue = {
    getId()
    toJson().as[JsObject] + ("co_owners" -> Json.toJson(co_owners))
  }

  def appendCoOwners(jsonReq: JsValue): String = {
    val sb = new StringBuilder()
    var json = toValidMongoJson()
    try {
      if ((json \ "owner_id") == null || ((json \ "owner_id").as[String] != (jsonReq \ "owner_id").as[String])) {
        return json.toString
      }
      val ja = new java.util.ArrayList[String]
      if ((jsonReq \ "co_owners").get.toString().contains("[")) {
        val co_owners = (jsonReq \ "co_owners").as[List[String]].toArray
        for (co_owner <- co_owners) {
          ja.add(co_owner)
        }
      } else {
        val co_owners = (jsonReq \ "co_owners").as[String]
        ja.add(co_owners)
      }
      val arr = Json.toJson(ja.toList)
      json = Json.toJson(json.as[JsObject] + ("co_owners" -> arr))
      json = json.as[JsObject] + ("geometry" -> Json.toJson(new GeoJSONPoint(java.lang.Double.parseDouble(fields.get("coordinates_lat")),
        java.lang.Double.parseDouble(fields.get("coordinates_lon"))).toGeoJSON()))

    } catch {
      case e: IOException => e.printStackTrace()
    }
    sb.append(json.toString)
    sb.toString
  }

  def changeOwner(newOwnerId: String): String = {
    val sb = new StringBuilder()
    var json = cleanupJson(toValidMongoJson())
    try {
      val newCoOwners: util.ArrayList[String] = new util.ArrayList[String]()
      this.fields.put("owner_id", newOwnerId)
      if ((json \ "co_owners").toOption.isDefined) {
        if ((json \ "co_owners").as[List[String]].length > 0) {
          val co_owners = (json \ "co_owners").as[List[String]]
          for (co_owner <- co_owners) {
            newCoOwners.add(co_owner)
          }
          for (i <- 0 until newCoOwners.size()) {
            if (newCoOwners.get(i) == newOwnerId) {
              newCoOwners.remove(i)
            }
          }
        }
      }
      json = json.as[JsObject] + ("geometry" -> Json.toJson(new GeoJSONPoint(java.lang.Double.parseDouble(fields.get("coordinates_lat")),
        java.lang.Double.parseDouble(fields.get("coordinates_lon"))).toGeoJSON()))
      json = Json.toJson(json.as[JsObject] + ("owner_id" -> JsString(newOwnerId)))
      json = Json.toJson(json.as[JsObject] + ("co_owners" -> Json.toJson(newCoOwners.toList)))
    } catch {
      case e: IOException => e.printStackTrace()
    }
    sb.append(json.toString)
    sb.toString
  }

  def cleanupJsonKey(json: JsValue, key: String): JsValue = {
    if ((json \ key).toOption.isDefined) {
      val str = (json \ key).as[String]
      if (str == "" || str == "-") {
        fields.remove(key)
        return json.as[JsObject] - key
      }
    }
    json
  }

  def cleanupJson(json: JsValue): JsValue = {
    var ret = json
    ret = cleanupJsonKey(ret, "bucode")
    ret = cleanupJsonKey(ret, "address")
    ret = cleanupJsonKey(ret, "description")
    ret = cleanupJsonKey(ret, "url")
    ret
  }

  def cleanupFieldKey(json: JsValue, key: String) {
    if ((json \ key).toOption.isDefined && (json \ key) != JsDefined(JsNull)) {
      val temp = (json \ key).as[String]
      if (!temp.equals("")) {
        fields.put(key, temp)
      }
    } else {
      fields.remove(key)
    }
  }

  def toJson(): JsValue = {
    val sMap: Map[String, String] = this.getFields().asScala.toMap
    val res = Json.toJson(sMap)
    // convert some keys to primitive types
    convertToInt("_schema", res)
  }

  @deprecated
  def _toString(): String = toValidJson().toString

  override def toString(): String = toJson().toString()
}
