/*
 * Anyplace: A free and open Indoor Navigation Service with superb accuracy!
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
 * Copyright (c) 2021, Data Management Systems Lab (DMSL), University of Cyprus.
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

import controllers.helper.User

import java.io.IOException
import java.util
import java.util.HashMap
import datasources.{ProxyDataSource, SCHEMA}
import play.api.libs.json._
import utils.{GeoJsonPoint, LOG, Utils}

import scala.jdk.CollectionConverters.CollectionHasAsScala

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
      val finalId = Utils.getRandomUUID() + "_" + System.currentTimeMillis()
      fields.put(SCHEMA.fBuid, fields.get(SCHEMA.fSpaceType).toLowerCase + "_" + finalId)
      buid = fields.get(SCHEMA.fBuid)
      this.json.as[JsObject] + (SCHEMA.fBuid -> Json.toJson(buid))
    }
    buid
  }

  def toGeoJson(): JsValue = {
    var json = toJson()
    try {
      json = json.as[JsObject] + (SCHEMA.fGeometry -> Json.toJson(
        new GeoJsonPoint(java.lang.Double.parseDouble(fields.get(SCHEMA.fCoordinatesLat)),
          java.lang.Double.parseDouble(fields.get(SCHEMA.fCoordinatesLon))).get()))
    } catch {
      case e: IOException => e.printStackTrace()
    }
    json
  }

  def toGeoJsonStr(): String = {
    val sb = new StringBuilder()
    sb.append(toGeoJson().toString)
    sb.toString
  }

  def toJson(): JsValue = {
    getId()
    _toJsonInternal().as[JsObject] + (SCHEMA.fCoOwners -> Json.toJson(co_owners))
  }

  /**
   * If a user exists in the database (the [coOwner] id is valid,
   * then it adds it to the coOwners list.
   *
   * @param pds
   * @param coOwner
   * @param coOwners
   */
  private def addValidCoOwner(pds: ProxyDataSource, coOwner: String, coOwners: java.util.Set[String]): Boolean = {
    val storedUser = pds.db.getUserFromOwnerId(coOwner)
    if (storedUser == null) {
      LOG.E("User does not exist. skipping coOwner: " + coOwner)
      return false
    } else {
      coOwners.add(coOwner)
      return true
    }
  }

  /**
   * Storing the modified JSON data ([data])
   * and fill the [err] when something occured
   */
  class JsonEdit(r: String, e: String, w: String="") {
    var data: String = r
    var err: String = e
    val warn: String = w
  }

  /**
   * This methods assumes that the space is already accessible by the user
   * @param user
   * @param pds
   * @param jsonReq
   * @return
   */
  def appendCoOwners(user: User, pds: ProxyDataSource, jsonReq: JsValue): JsonEdit = {
    LOG.D("appendCoOwners")
    var json = toJson()
    var allUsersAdded=true
    var modTriesToCoOwn = false

    try {
      if ((json \ SCHEMA.fOwnerId) == null) {
        return new JsonEdit(json.toString, "Problem with owner id")
      }

      val reqOwnerId = (jsonReq \ SCHEMA.fOwnerId).as[String]
      val userIsModerator = user.isAdminOrModerator(reqOwnerId)

      // get the list of extra co-owners from [jsonReq]
      val newCoOwners= new java.util.HashSet[String]

      // it is a list of coOwners
      if ((jsonReq \ SCHEMA.fCoOwners).get.toString().contains("[")) {
        val coOwnersArray = (jsonReq \ SCHEMA.fCoOwners).as[List[String]].toArray
        for (coOwner <- coOwnersArray) {
          if (userIsModerator && reqOwnerId == coOwner) {
            modTriesToCoOwn=true
          } else if (!addValidCoOwner(pds, coOwner, newCoOwners)) {
            allUsersAdded=false
          }
        }
      } else { // or a single owner
        val coOwner = (jsonReq \ SCHEMA.fCoOwners).as[String]
        if (userIsModerator && reqOwnerId == coOwner) {
          modTriesToCoOwn=true
        } else if (!addValidCoOwner(pds, coOwner, newCoOwners)) {
          allUsersAdded=false
        }
      }

      val arr = Json.toJson(newCoOwners.asScala)
      json = Json.toJson(json.as[JsObject] + (SCHEMA.fCoOwners -> arr))
      json = json.as[JsObject] + (SCHEMA.fGeometry -> Json.toJson(new GeoJsonPoint(java.lang.Double.parseDouble(fields.get(SCHEMA.fCoordinatesLat)),
        java.lang.Double.parseDouble(fields.get(SCHEMA.fCoordinatesLon))).get()))

    } catch {
      case e: IOException => {
        LOG.D("ERROR: " + e.getMessage)
        e.printStackTrace()
      }
    }

    val sb = new StringBuilder()
    sb.append(json.toString)
    var warn = ""
    if (!allUsersAdded) warn = "some users were invalid"
    if (modTriesToCoOwn) warn += ", a moderator tried to coOwn; no need for that"
    return new JsonEdit(sb.toString, "", warn)
  }

  def changeOwner(newOwnerId: String): String = {
    val sb = new StringBuilder()
    var json = cleanupJson(_toJsonInternal())
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
      json = json.as[JsObject] + (SCHEMA.fGeometry -> Json.toJson(
        new GeoJsonPoint(java.lang.Double.parseDouble(fields.get(SCHEMA.fCoordinatesLat)),
        java.lang.Double.parseDouble(fields.get(SCHEMA.fCoordinatesLon))).get()))
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

  override def toString(): String = _toJsonInternal().toString()
}
