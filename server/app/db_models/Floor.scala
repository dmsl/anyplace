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
import datasources.SCHEMA
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import utils.JsonUtils.convertToInt

import scala.jdk.CollectionConverters.MapHasAsScala


object Floor {

  def getId(buid: String, floor_number: String): String = buid + "_" + floor_number

  def checkFloorNumberFormat(floor_number: String): Boolean = {
    floor_number.toCharArray().find(java.lang.Character.isWhitespace(_))
      .map(_ => false)
      .getOrElse(true)
  }
}

class Floor(hm: HashMap[String, String]) extends AbstractModel {

  private var json: JsValue = _

  this.fields = hm

  def this() = {
    this(new HashMap[String, String])
    fields.put(SCHEMA.fBuid, "")
    fields.put(SCHEMA.fIsPublished, "")
    fields.put(SCHEMA.fFloorName, "")
    fields.put(SCHEMA.fFloorNumber, "")
    fields.put(SCHEMA.fDescription, "")
  }

  def this(json: JsValue) = {
    this()
    if ((json \ SCHEMA.fFuid).toOption.isDefined)
      fields.put(SCHEMA.fFuid, (json \ SCHEMA.fFuid).as[String])
    if ((json \ SCHEMA.fBuid).toOption.isDefined)
      fields.put(SCHEMA.fBuid, (json \ SCHEMA.fBuid).as[String])
    if ((json \ SCHEMA.fIsPublished).toOption.isDefined)
      fields.put(SCHEMA.fIsPublished, (json \ SCHEMA.fIsPublished).as[String])
    if ((json \ SCHEMA.fFloorName).toOption.isDefined)
      fields.put(SCHEMA.fFloorName, (json \ SCHEMA.fFloorName).as[String])
    if ((json \ SCHEMA.fDescription).toOption.isDefined) {
      val temp = (json \ SCHEMA.fDescription).as[String]
      if (temp != "" && temp != "-")
        fields.put(SCHEMA.fDescription, (json \ SCHEMA.fDescription).as[String])
      else
        fields.remove(SCHEMA.fDescription)
    } else
      fields.remove(SCHEMA.fDescription)
    if ((json \ SCHEMA.fFloorNumber).toOption.isDefined)
      fields.put(SCHEMA.fFloorNumber, (json \ SCHEMA.fFloorNumber).as[String])
    this.json = json
  }

  def getId(): String = {
    var fuid = (this.json \ SCHEMA.fFuid)
    var newFuid = ""
    if (!fuid.toOption.isDefined) {
      newFuid = Floor.getId(fields.get(SCHEMA.fBuid), fields.get(SCHEMA.fFloorNumber))
      fields.put(SCHEMA.fFuid, newFuid)
      this.json = this.json.as[JsObject] + (SCHEMA.fFuid -> JsString(newFuid))
    } else
      newFuid = fuid.as[String]
    newFuid
  }

  def toValidJson(): JsonObject = {
    null
  }

  def toValidMongoJson(): JsValue = {
    // initialize id if not initialized
    getId()
    fields.remove("username")
    toJson()
  }

  def toJson(): JsValue = {
    val sMap: Map[String, String] = this.getFields().asScala.toMap
    val res = Json.toJson(sMap)
    // convert some keys to primitive types
    convertToInt(SCHEMA.fSchema, res)
  }

  override def toGeoJSON(): String = toJson().toString

  override def toString(): String = this.toJson().toString
}
