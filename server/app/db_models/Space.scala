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

/**
 *
 */

import java.io.IOException
import java.util
import java.util.HashMap

import com.couchbase.client.java.document.json.JsonObject
import datasources.SCHEMA
import play.api.libs.json._
import utils.JsonUtils.convertToInt
import utils.{GeoJSONPoint, LPUtils}

import scala.jdk.CollectionConverters.{CollectionHasAsScala, MapHasAsScala}

class Space(hm: HashMap[String, String]) extends AbstractModel {
  private var json: JsValue = _
  private var lat: Double = _
  private var lng: Double = _
  private var co_owners = JsArray()
  this.fields = hm

  def this() = {
    this(new HashMap[String, String])
    fields.put(SCHEMA.fBuid, "")
    fields.put(SCHEMA.fIsPublished, "")
    fields.put(SCHEMA.fName, "")
    fields.put(SCHEMA.fDescription, "")
    fields.put(SCHEMA.fURL, "")
    fields.put(SCHEMA.fAddress, "")
    fields.put(SCHEMA.fCoordinatesLat, "")
    fields.put(SCHEMA.fCoordinatesLon, "")
    fields.put(SCHEMA.fBuCode, "")
    fields.put(SCHEMA.fSpaceType, "")
  }

  def this(json: JsValue) = {
    this()
    fields.put(SCHEMA.fOwnerId, (json \ SCHEMA.fOwnerId).as[String])
    if ((json \ SCHEMA.fBuid).toOption.isDefined)
      fields.put(SCHEMA.fBuid, (json \ SCHEMA.fBuid).as[String])
    if ((json \ SCHEMA.fIsPublished).toOption.isDefined && (json \ SCHEMA.fIsPublished) != JsDefined(JsNull)) {
      val temp = (json \ SCHEMA.fIsPublished).as[String]
      if (!temp.equals(""))
        fields.put(SCHEMA.fIsPublished, temp)
    }
    fields.put(SCHEMA.fSpaceType, (json \ SCHEMA.fSpaceType).as[String])
    fields.put(SCHEMA.fName, (json \ SCHEMA.fName).as[String])
    cleanupFieldKey(json, SCHEMA.fDescription)
    cleanupFieldKey(json, SCHEMA.fURL)
    cleanupFieldKey(json, SCHEMA.fAddress)
    fields.put(SCHEMA.fCoordinatesLat, (json \ SCHEMA.fCoordinatesLat).as[String])
    fields.put(SCHEMA.fCoordinatesLon, (json \ SCHEMA.fCoordinatesLon).as[String])
    cleanupFieldKey(json, SCHEMA.fBuCode)
    if ((json \ SCHEMA.fCoOwners).toOption.isDefined)
      co_owners = (json \ SCHEMA.fCoOwners).as[JsArray]
    this.json = cleanupJson(json)
    this.lat = java.lang.Double.parseDouble((json \ SCHEMA.fCoordinatesLat).as[String])
    this.lng = java.lang.Double.parseDouble((json \ SCHEMA.fCoordinatesLon).as[String])
  }

  def this(json: JsValue, owner: String) = {
    this(json)
    fields.put(SCHEMA.fOwnerId, owner)
  }

  def getId(): String = {
    var buid: String = fields.get(SCHEMA.fBuid)
    if (buid == null || buid.isEmpty || buid == "") {
      val finalId = LPUtils.getRandomUUID() + "_" + System.currentTimeMillis()
      fields.put(SCHEMA.fBuid, "building_" + finalId)
      buid = fields.get(SCHEMA.fBuid)
      this.json.as[JsObject] + (SCHEMA.fBuid -> Json.toJson(buid))
    }
    buid
  }

  //def hasAccess(ownerId: String): Boolean = {
  //  if (fields.get(SCHEMA.fOwnerId) == ownerId) return true
  //  co_owners.value.foreach(coOwner => if (coOwner == ownerId) return true)
  //
  //  false
  //}

  def toGeoJSON(): String = {
    val sb = new StringBuilder()
    var json = toValidMongoJson()
    try {
      json = json.as[JsObject] + (SCHEMA.fGeometry -> Json.toJson(
        new GeoJSONPoint(java.lang.Double.parseDouble(fields.get(SCHEMA.fCoordinatesLat)),
          java.lang.Double.parseDouble(fields.get(SCHEMA.fCoordinatesLon))).toGeoJSON()))
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
    JsonObject.from(this.getFields()).put(SCHEMA.fCoOwners, co_owners)
  }

  def toValidMongoJson(): JsValue = {
    getId()
    toJson().as[JsObject] + (SCHEMA.fCoOwners -> Json.toJson(co_owners))
  }

  def appendCoOwners(jsonReq: JsValue): String = {
    val sb = new StringBuilder()
    var json = toValidMongoJson()
    try {
      if ((json \ SCHEMA.fOwnerId) == null || ((json \ SCHEMA.fOwnerId).as[String] != (jsonReq \ SCHEMA.fOwnerId).as[String])) {
        return json.toString
      }
      val ja = new java.util.ArrayList[String]
      if ((jsonReq \ SCHEMA.fCoOwners).get.toString().contains("[")) {
        val co_owners = (jsonReq \ SCHEMA.fCoOwners).as[List[String]].toArray
        for (co_owner <- co_owners) {
          ja.add(co_owner)
        }
      } else {
        val co_owners = (jsonReq \ SCHEMA.fCoOwners).as[String]
        ja.add(co_owners)
      }
      val arr = Json.toJson(ja.asScala)
      json = Json.toJson(json.as[JsObject] + (SCHEMA.fCoOwners -> arr))
      json = json.as[JsObject] + (SCHEMA.fGeometry -> Json.toJson(new GeoJSONPoint(java.lang.Double.parseDouble(fields.get(SCHEMA.fCoordinatesLat)),
        java.lang.Double.parseDouble(fields.get(SCHEMA.fCoordinatesLon))).toGeoJSON()))

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
      this.fields.put(SCHEMA.fOwnerId, newOwnerId)
      if ((json \ SCHEMA.fCoOwners).toOption.isDefined) {
        if ((json \ SCHEMA.fCoOwners).as[List[String]].length > 0) {
          val co_owners = (json \ SCHEMA.fCoOwners).as[List[String]]
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
      json = json.as[JsObject] + (SCHEMA.fGeometry -> Json.toJson(new GeoJSONPoint(java.lang.Double.parseDouble(fields.get(SCHEMA.fCoordinatesLat)),
        java.lang.Double.parseDouble(fields.get(SCHEMA.fCoordinatesLon))).toGeoJSON()))
      json = Json.toJson(json.as[JsObject] + (SCHEMA.fOwnerId -> JsString(newOwnerId)))
      json = Json.toJson(json.as[JsObject] + (SCHEMA.fCoOwners -> Json.toJson(newCoOwners.asScala)))
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
    ret = cleanupJsonKey(ret, SCHEMA.fBuCode)
    ret = cleanupJsonKey(ret, SCHEMA.fAddress)
    ret = cleanupJsonKey(ret, SCHEMA.fDescription)
    ret = cleanupJsonKey(ret, SCHEMA.fURL)
    ret
  }

  def cleanupFieldKey(json: JsValue, key: String): Unit = {
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
    convertToInt(SCHEMA.fSchema, res)
  }

  @deprecated
  def _toString(): String = toValidJson().toString

  override def toString(): String = toJson().toString()
}
