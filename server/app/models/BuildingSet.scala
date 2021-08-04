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
package models

import java.util.HashMap

import datasources.SCHEMA
import play.api.libs.json.{JsObject, JsValue, Json}
import utils.JsonUtils.convertToInt
import utils.Utils

class BuildingSet(hm: HashMap[String, String]) extends AbstractModel {
  private var json: JsValue = _
  private var lat: Double = _
  private var lng: Double = _
  private var admins: Array[String] = Array("112997031510415584062_google")

  this.fields = hm

  def this() = {
    this(new HashMap[String, String])
    fields.put(SCHEMA.fOwnerId, "")
    fields.put(SCHEMA.fCampusCuid, "")
    fields.put(SCHEMA.fName, "")
    fields.put(SCHEMA.fDescription, "")
    fields.put(SCHEMA.fGreeklish, "")
    fields.put(SCHEMA.fBuids, "[]")
  }

  def this(json: JsValue) = {
    this()
    if ((json \ SCHEMA.fOwnerId).toOption.isDefined)
      fields.put(SCHEMA.fOwnerId, (json \ SCHEMA.fOwnerId).as[String])
    if ((json \ SCHEMA.fCampusCuid).toOption.isDefined)
      fields.put(SCHEMA.fCampusCuid, (json \ SCHEMA.fCampusCuid).as[String])
    if ((json \ SCHEMA.fName).toOption.isDefined)
      isEmptyDeleteElseAdd(json, SCHEMA.fName)
    else
      fields.remove(SCHEMA.fName)
    if ((json \ SCHEMA.fGreeklish).toOption.isDefined)
      isEmptyDeleteElseAdd(json, SCHEMA.fGreeklish)
    else
      fields.remove(SCHEMA.fGreeklish)
    if ((json \ SCHEMA.fDescription).toOption.isDefined) {
      isEmptyDeleteElseAdd(json, SCHEMA.fDescription)
    } else {
      fields.remove(SCHEMA.fDescription)
    }
    this.json = json
  }

  def this(json: JsValue, owner: String) = {
    this(json)
    fields.put(SCHEMA.fOwnerId, owner)
  }

  def isEmptyDeleteElseAdd(json: JsValue, key: String) = {
    val temp = (json \ key).as[String]
    if (temp != "" && temp != null && temp != "-") {
      fields.put(key, temp)
    } else {
      fields.remove(key)
    }
  }

  def getId(): String = {
    var cuid: String = fields.get(SCHEMA.fCampusCuid)
    if (cuid.isEmpty || cuid.==("")) {
      val finalId: String = Utils.getRandomUUID() + "_" + System
        .currentTimeMillis()
      fields.put(SCHEMA.fCampusCuid, "cuid_" + finalId)
      cuid = fields.get(SCHEMA.fCampusCuid)
      this.json.as[JsObject] + (SCHEMA.fCampusCuid -> Json.toJson(cuid))
    }
    cuid
  }

  def toJson(): JsValue = {
    getId()
    _toJsonInternal()
  }

  def toGeoJSON(): String = {
    val sb: StringBuilder = new StringBuilder()
    json = json.as[JsObject] - SCHEMA.fAccessToken
    sb.append(this.json.toString)
    sb.toString
  }

  def addBuids(): String = {
    val sb: StringBuilder = new StringBuilder()
    if ((this.json \ SCHEMA.fDescription).as[String] == "" || (this.json \ SCHEMA.fDescription).as[String] == "-")
      this.json = this.json.as[JsObject] - SCHEMA.fDescription
    if ((this.json \ SCHEMA.fName).as[String] == "" || (this.json \ SCHEMA.fName).as[String] == "-")
      this.json = this.json.as[JsObject] - SCHEMA.fName
    this.json = convertToInt(SCHEMA.fSchema, this.json)
    sb.append(this.json.toString())
    sb.toString
  }

  override def toString(): String = _toJsonInternal().toString()
}
