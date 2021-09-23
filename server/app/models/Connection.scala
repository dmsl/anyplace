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
import play.api.libs.json.{JsObject, JsString, JsValue}

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

  def this() = {
    this(new HashMap[String, String])
    fields.put(SCHEMA.fIsPublished, "")
    fields.put(SCHEMA.fEdgeType, "")
    fields.put(SCHEMA.fPoisA, "")
    fields.put(SCHEMA.fPoisB, "")
    fields.put(SCHEMA.fWeight, "")
    fields.put(SCHEMA.fBuid, "")
    fields.put(SCHEMA.fFloorA, "")
    fields.put(SCHEMA.fFloorB, "")
    fields.put(SCHEMA.fBuidA, "")
    fields.put(SCHEMA.fBuidB, "")
    fields.put(SCHEMA.fConCuid, "")
  }

  def this(json: JsValue) = {
    this()
    if ((json \ SCHEMA.fIsPublished).toOption.isDefined)
      fields.put(SCHEMA.fIsPublished, (json \ SCHEMA.fIsPublished).as[String])
    if ((json \ SCHEMA.fEdgeType).toOption.isDefined)
      fields.put(SCHEMA.fEdgeType, (json \ SCHEMA.fEdgeType).as[String])
    if ((json \ SCHEMA.fPoisA).toOption.isDefined)
      fields.put(SCHEMA.fPoisA, (json \ SCHEMA.fPoisA).as[String])
    if ((json \ SCHEMA.fPoisB).toOption.isDefined)
      fields.put(SCHEMA.fPoisB, (json \ SCHEMA.fPoisB).as[String])
    if ((json \ SCHEMA.fWeight).toOption.isDefined)
      fields.put(SCHEMA.fWeight, (json \ SCHEMA.fWeight).as[String])
    if ((json \ SCHEMA.fBuid).toOption.isDefined)
      fields.put(SCHEMA.fBuid, (json \ SCHEMA.fBuid).as[String])
    if ((json \ SCHEMA.fFloorA).toOption.isDefined)
      fields.put(SCHEMA.fFloorA, (json \ SCHEMA.fFloorA).as[String])
    if ((json \ SCHEMA.fFloorB).toOption.isDefined)
      fields.put(SCHEMA.fFloorB, (json \ SCHEMA.fFloorB).as[String])
    if ((json \ SCHEMA.fBuidA).toOption.isDefined)
      fields.put(SCHEMA.fBuidA, (json \ SCHEMA.fBuidA).as[String])
    if ((json \ SCHEMA.fBuidB).toOption.isDefined)
      fields.put(SCHEMA.fBuidB, (json \ SCHEMA.fBuidB).as[String])
    if ((json \ SCHEMA.fConCuid).toOption.isDefined)
      fields.put(SCHEMA.fConCuid, (json \ SCHEMA.fConCuid).as[String])
    this.json = json
  }

  def getId(): String = {
    var cuid: String = fields.get(SCHEMA.fConCuid)
    if (cuid == null || cuid.isEmpty || cuid == "") {
      cuid = Connection.getId((json \ SCHEMA.fPoisA).as[String], (json \ SCHEMA.fPoisB).as[String])
      fields.put(SCHEMA.fConCuid, cuid)
      this.json = this.json.as[JsObject] + (SCHEMA.fConCuid -> JsString(cuid))
    }
    cuid
  }

  def toJson(): JsValue = {
    getId()
    _toJsonInternal()
  }

  override def toGeoJsonStr(): String = toGeoJson().toString()
  override def toGeoJson(): JsValue = _toJsonInternal()
  override def toString(): String = this._toJsonInternal().toString
}
