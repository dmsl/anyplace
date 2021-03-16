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

import java.io.IOException
import java.util
import java.util.HashMap

import com.couchbase.client.java.document.json.JsonObject
import play.api.libs.json._
import play.twirl.api.TemplateMagic.javaCollectionToScala
import utils.JsonUtils.convertToInt
import utils.{GeoJSONPoint, LPLogger, LPUtils}

import scala.collection.JavaConverters.mapAsScalaMapConverter

class Building(hm: HashMap[String, String]) extends AbstractModel {

  private var json: JsValue = _

  private var lat: Double = _

  private var lng: Double = _

  private var admins: Array[String] = Array("112997031510415584062_google")

  private var co_owners = JsArray()

  this.fields = hm

  def this() {
    this(new HashMap[String, String])
    fields.put("username_creator", "")
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
    if ((json \ "username_creator").toOption.isDefined && (json \ "username_creator") != JsDefined(JsNull)) {
      val temp = (json \ "username_creator").as[String]
      if (!temp.equals(""))
        fields.put("username_creator", temp)
    }
    fields.put("owner_id", (json \ "owner_id").as[String])
    if ((json \ "buid").toOption.isDefined)
      fields.put("buid", (json \ "buid").as[String])
    if ((json \ "is_published").toOption.isDefined && (json \ "is_published") != JsDefined(JsNull)) {
      val temp = (json \ "is_published").as[String]
      if (!temp.equals(""))
        fields.put("is_published", temp)
    }
    fields.put("name", (json \ "name").as[String])
    if ((json \ "description").toOption.isDefined && (json \ "description") != JsDefined(JsNull)) {
      val temp = (json \ "description").as[String]
      if (!temp.equals(""))
        fields.put("description", temp)
    }
    if ((json \ "url").toOption.isDefined && (json \ "url") != JsDefined(JsNull)) {
      val temp = (json \ "url").as[String]
      if (!temp.equals(""))
        fields.put("url", temp)
    }
    if ((json \ "address").toOption.isDefined && (json \ "address") != JsDefined(JsNull)) {
      val temp = (json \ "address").as[String]
      if (!temp.equals(""))
        fields.put("address", temp)
    }
    fields.put("coordinates_lat", (json \ "coordinates_lat").as[String])
    fields.put("coordinates_lon", (json \ "coordinates_lon").as[String])
    if ((json \ "bucode").toOption.isDefined && (json \ "bucode") != JsDefined(JsNull)) {
      val temp = (json \ "bucode").as[String]
      if (!temp.equals(""))
        fields.put("bucode", temp)
    }
    if ((json \ "co_owners").toOption.isDefined)
      co_owners = (json \ "co_owners").as[JsArray]
    this.json = json
    this.lat = java.lang.Double.parseDouble((json \ "coordinates_lat").as[String])
    this.lng = java.lang.Double.parseDouble((json \"coordinates_lon").as[String])
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

  def toCouchGeoJSON(): String = {
    val sb = new StringBuilder()
    var json = toValidMongoJson()
    try {
      json = json.as[JsObject] + ("geometry" -> Json.toJson(new GeoJSONPoint(java.lang.Double.parseDouble(fields.get("coordinates_lat")),
        java.lang.Double.parseDouble(fields.get("coordinates_lon"))).toGeoJSON()))
//      json.put("geometry", new GeoJSONPoint(java.lang.Double.parseDouble(fields.get("coordinates_lat")),
//        java.lang.Double.parseDouble(fields.get("coordinates_lon")))
//        .toGeoJSON())
//      if (json.getArray("co_owners") == null || json.getArray("co_owners").isEmpty) {}
      if ((json\"co_owners") == JsDefined(JsNull) || (json\"co_owners") == JsDefined(JsString(""))) {
        val ja = new util.ArrayList[String]
        for (i <- admins.indices) {
          ja.add(admins(i))
        }
        json = Json.toJson(json.as[JsObject] + ("co_owners" -> Json.toJson(ja.toList)))
      }
//      json.removeKey("username")
        json = json.as[JsObject] - "username"
    } catch {
      case e: IOException => e.printStackTrace()
    }
    sb.append(json.toString)
    sb.toString
  }

  @deprecated
  def toValidCouchJson(): JsonObject = {
    // initialize id if not initialized
    getId()
    JsonObject.from(this.getFields()).put("co_owners",co_owners)
  }

  def toValidMongoJson(): JsValue = {
    getId()
    toJson().as[JsObject] + ("co_owners" -> Json.toJson(co_owners))
  }

  def appendCoOwners(jsonReq: JsValue): String = {
    val sb = new StringBuilder()
    var json = toValidMongoJson()
    LPLogger.debug("json = "+ json)
    try {
      if ((json \ "owner_id") == null || ((json \ "owner_id").as[String] != (jsonReq \ "owner_id").as[String])) {
        return json.toString
      }
      val ja = new java.util.ArrayList[String]
      for (i <- 0 until admins.length) {
        ja.add(admins(i))
      }
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

    } catch {
      case e: IOException => e.printStackTrace()
    }
    sb.append(json.toString)
    sb.toString
  }

  def changeOwner(newOwnerId: String): String = {
    val sb = new StringBuilder()
    var json = removeEmpty(toValidMongoJson())
    LPLogger.debug("BEFORE ?? = " + json)
    try {
      this.fields.put("owner_id", newOwnerId)
//      val ja = new java.util.ArrayList[String]
//      for (i <- 0 until admins.length) {
//        ja.add(admins(i))
//      }
      json = Json.toJson(json.as[JsObject] + ("owner_id" -> JsString(newOwnerId)))
    } catch {
      case e: IOException => e.printStackTrace()
    }
    sb.append(json.toString)
    sb.toString
  }

  def removeEmpty(json: JsValue):JsValue = {
    var ret = json
    LPLogger.debug("w???? = " + (json\"bucode").as[String])
    if ((json\"bucode").as[String].equals(""))
      ret = ret.as[JsObject] - "bucode"
    if ((json\"address").as[String].equals(""))
      ret = ret.as[JsObject] - "address"
    if ((json\"description").as[String].equals(""))
      ret = ret.as[JsObject] - "description"
    if ((json\"username_creator").as[String].equals(""))
      ret = ret.as[JsObject] - "username_creator"
    if ((json\"url").as[String].equals(""))
      ret = ret.as[JsObject] - "url"
    ret
  }

  def toJson(): JsValue = {
    val sMap: Map[String, String] = this.getFields().asScala.toMap
    val res = Json.toJson(sMap)
    // convert some keys to primitive types
    convertToInt("_schema", res)
  }

  @deprecated
  def _toString(): String = toValidCouchJson().toString

  override def toString(): String = toJson().toString()
}
