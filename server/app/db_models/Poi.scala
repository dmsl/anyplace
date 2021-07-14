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
import datasources.SCHEMA
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import utils.JsonUtils.convertToInt
import utils.{GeoJSONPoint, Utils}

import scala.jdk.CollectionConverters.MapHasAsScala


object Poi {

  val POIS_TYPE_NONE = "None"

  val POIS_TYPE_ELEVATOR = "elevator"

  val POIS_TYPE_STAIR = "stair"

  def getId(username_creator: String,
            buid: String,
            floor_number: String,
            coordinates_lat: String,
            coordinates_lon: String): String = "poi_" + Utils.getRandomUUID()
}

class Poi(hm: HashMap[String, String]) extends AbstractModel {

  private var json: JsValue = _

  private var lat: Double = _

  private var lng: Double = _

  this.fields = hm

  def this() = {
    this(new HashMap[String, String])
    fields.put(SCHEMA.fPuid, "")
    fields.put(SCHEMA.fBuid, "")
    fields.put(SCHEMA.fIsPublished, "")
    fields.put(SCHEMA.fFloorName, "")
    fields.put(SCHEMA.fFloorNumber, "")
    fields.put(SCHEMA.fName, "")
    fields.put(SCHEMA.fDescription, "")
    fields.put(SCHEMA.fURL, "")
    fields.put(SCHEMA.fImage, "")
    fields.put(SCHEMA.fPoisType, "")
    fields.put(SCHEMA.fIsDoor, "")
    fields.put(SCHEMA.fCoordinatesLat, "")
    fields.put(SCHEMA.fCoordinatesLon, "")
  }

  def this(json: JsValue) = {
    this()
    if ((json \ SCHEMA.fPuid).toOption.isDefined)
      fields.put(SCHEMA.fPuid, (json \ SCHEMA.fPuid).as[String])
    if ((json \ SCHEMA.fBuid).toOption.isDefined)
      fields.put(SCHEMA.fBuid, (json \ SCHEMA.fBuid).as[String])
    if ((json \ SCHEMA.fIsPublished).toOption.isDefined)
      fields.put(SCHEMA.fIsPublished, (json \ SCHEMA.fIsPublished).as[String])
    if ((json \ SCHEMA.fFloorName).toOption.isDefined)
      fields.put(SCHEMA.fFloorName, (json \ SCHEMA.fFloorName).as[String])
    if ((json \ SCHEMA.fFloorNumber).toOption.isDefined)
      fields.put(SCHEMA.fFloorNumber, (json \ SCHEMA.fFloorNumber).as[String])
    if ((json \ SCHEMA.fName).toOption.isDefined) {
      val temp = (json \ SCHEMA.fName).as[String]
      if (temp != "" && temp != "-" && temp != null) {
        fields.put(SCHEMA.fName, temp)
      } else {
        fields.remove(SCHEMA.fName)
      }
    }
    if ((json \ SCHEMA.fDescription).toOption.isDefined) {
      val temp = (json \ SCHEMA.fDescription).as[String]
      if (temp != "" && temp != "-" && temp != null) {
        fields.put(SCHEMA.fDescription, temp)
      } else {
        fields.remove(SCHEMA.fDescription)
      }
    } else
      fields.remove(SCHEMA.fDescription)
    if ((json \ SCHEMA.fURL).toOption.isDefined) {
      val temp = (json \ SCHEMA.fURL).as[String]
      if (temp != "" && temp != "-" && temp != null) {
        fields.put(SCHEMA.fURL, temp)
      } else {
        fields.remove(SCHEMA.fURL)
      }
    } else
      fields.remove(SCHEMA.fURL)
    if ((json \ SCHEMA.fImage).toOption.isDefined)
      fields.put(SCHEMA.fImage, (json \ SCHEMA.fImage).as[String])
    if ((json \ SCHEMA.fPoisType).toOption.isDefined)
      fields.put(SCHEMA.fPoisType, (json \ SCHEMA.fPoisType).as[String])
    if ((json \ SCHEMA.fIsDoor).toOption.isDefined)
      fields.put(SCHEMA.fIsDoor, (json \ SCHEMA.fIsDoor).as[String])
    if ((json \ SCHEMA.fIsBuildingEntrance).toOption.isDefined)
      fields.put(SCHEMA.fIsBuildingEntrance, (json \ SCHEMA.fIsBuildingEntrance).as[String])
    if ((json \ SCHEMA.fCoordinatesLat).toOption.isDefined)
      fields.put(SCHEMA.fCoordinatesLat, (json \ SCHEMA.fCoordinatesLat).as[String])
    if ((json \ SCHEMA.fCoordinatesLon).toOption.isDefined)
      fields.put(SCHEMA.fCoordinatesLon, (json \ SCHEMA.fCoordinatesLon).as[String])
    this.json = json
    this.lat = java.lang.Double.parseDouble((json \ SCHEMA.fCoordinatesLat).as[String])
    this.lng = java.lang.Double.parseDouble((json \ SCHEMA.fCoordinatesLon).as[String])
  }

  def getId(): String = {
    var puid: String = fields.get(SCHEMA.fPuid)
    if (puid == null || puid.isEmpty || puid == "") {
      fields.put(SCHEMA.fPuid, Poi.getId(fields.get("username_creator"), fields.get(SCHEMA.fBuid), fields.get(SCHEMA.fFloorNumber),
        fields.get(SCHEMA.fCoordinatesLat), fields.get(SCHEMA.fCoordinatesLon)))
      this.json = this.json.as[JsObject] + (SCHEMA.fPuid -> JsString(fields.get(SCHEMA.fPuid)))
      puid = fields.get(SCHEMA.fPuid)
    }
    puid
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

  override def toString(): String = toJson().toString

  def toJson(): JsValue = {
    val sMap: Map[String, String] = this.getFields().asScala.toMap
    val res = Json.toJson(sMap)
    // convert some keys to primitive types
    convertToInt(SCHEMA.fSchema, res)
  }
}
