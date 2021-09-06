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


import java.io.IOException
import java.util.HashMap

import datasources.SCHEMA
import play.api.libs.json._
import utils._


object RadioMapRaw {

  def toRawRadioMapRecord(hm: HashMap[String, String]): String = {
    val sb = new StringBuilder()
    sb.append(hm.get(SCHEMA.fTimestamp))
    sb.append(" ")
    sb.append(hm.get(SCHEMA.fX))
    sb.append(" ")
    sb.append(hm.get(SCHEMA.fY))
    sb.append(" ")
    sb.append(hm.get(SCHEMA.fHeading))
    sb.append(" ")
    sb.append(hm.get(SCHEMA.fMac))
    sb.append(" ")
    sb.append(hm.get(SCHEMA.fRSS))
    sb.append(" ")
    sb.append(hm.get(SCHEMA.fFloor))
    sb.toString
  }

  def toRawRadioMapRecord(json: JsValue): String = {
    val sb = new StringBuilder()
    sb.append((json \ SCHEMA.fTimestamp).as[String])
    sb.append(" ")
    sb.append((json \ SCHEMA.fX).as[String])
    sb.append(" ")
    sb.append((json \ SCHEMA.fY).as[String])
    sb.append(" ")
    sb.append((json \ SCHEMA.fHeading).as[String])
    sb.append(" ")
    sb.append((json \ SCHEMA.fMac).as[String])
    sb.append(" ")
    sb.append((json \ SCHEMA.fRSS).as[String])
    sb.append(" ")
    sb.append((json \ SCHEMA.fFloor).as[String])
    sb.toString
  }

  def unrollFingerprint(rss: JsValue, measurement: List[String]): JsValue = {
    var json = Json.obj(SCHEMA.fBuid -> (rss \ SCHEMA.fBuid).as[String], SCHEMA.fFloor -> (rss \ SCHEMA.fFloor).as[String],
      SCHEMA.fX -> (rss \ SCHEMA.fX).as[String], SCHEMA.fY -> (rss \ SCHEMA.fY).as[String], SCHEMA.fHeading -> (rss \ SCHEMA.fHeading).as[String],
      SCHEMA.fTimestamp -> (rss \ SCHEMA.fTimestamp).as[String], SCHEMA.fMac -> measurement(0), SCHEMA.fRSS -> measurement(1),
      (SCHEMA.fGeometry -> Json.toJson(new GeoJsonPoint(java.lang.Double.parseDouble((rss \ SCHEMA.fX).as[String]),
        java.lang.Double.parseDouble((rss \ SCHEMA.fY).as[String])).get())))
    if ((rss \ SCHEMA.fStrongestWifi).toOption.isDefined)
      json = json.as[JsObject] + (SCHEMA.fStrongestWifi -> JsString((rss \ SCHEMA.fStrongestWifi).as[String]))
    return json
  }
}

class RadioMapRaw(h: HashMap[String, String]) extends AbstractModel {
  this.fields = h

  def this(timestamp: String,
           x: String,
           y: String,
           heading: String
          ) = {
    this(new HashMap[String, String])
    fields.put(SCHEMA.fTimestamp, timestamp)
    fields.put(SCHEMA.fX, x)
    fields.put(SCHEMA.fY, y)
    fields.put(SCHEMA.fHeading, heading)
    fields.put(SCHEMA.fFloor, "-")
  }

  def this(timestamp: String,
           x: String,
           y: String,
           heading: String,
           floor: String) = {
    this(new HashMap[String, String])
    fields.put(SCHEMA.fTimestamp, timestamp)
    fields.put(SCHEMA.fX, x)
    fields.put(SCHEMA.fY, y)
    fields.put(SCHEMA.fHeading, heading)
    fields.put(SCHEMA.fFloor, floor)
  }

  def this(timestamp: String,
           x: String,
           y: String,
           heading: String,
           floor: String,
           strongestWifi: String) = {
    this(new HashMap[String, String])
    fields.put(SCHEMA.fTimestamp, timestamp)
    fields.put(SCHEMA.fX, x)
    fields.put(SCHEMA.fY, y)
    fields.put(SCHEMA.fHeading, heading)
    fields.put(SCHEMA.fFloor, floor)
    fields.put(SCHEMA.fStrongestWifi, strongestWifi)
  }

  def this(timestamp: String,
           x: String,
           y: String,
           heading: String,
           floor: String,
           strongestWifi: String,
           buid: String) = {
    this(new HashMap[String, String])
    fields.put(SCHEMA.fTimestamp, timestamp)
    fields.put(SCHEMA.fX, x)
    fields.put(SCHEMA.fY, y)
    fields.put(SCHEMA.fHeading, heading)
    fields.put(SCHEMA.fFloor, floor)
    fields.put(SCHEMA.fStrongestWifi, strongestWifi)
    fields.put(SCHEMA.fBuid, buid)
  }

  def getId(): String = {
    fields.get(SCHEMA.fX) + fields.get(SCHEMA.fY) + fields.get(SCHEMA.fHeading) +
      fields.get(SCHEMA.fTimestamp) +
      fields.get(SCHEMA.fMac)
  }

  def toJson(): JsValue = {
    _toJsonInternal()
  }

  def addMeasurements(measurements: List[List[String]]): JsValue = {
    var json = toJson()
    try {
      val timestampToSec = (json \ SCHEMA.fTimestamp).as[String].toLong / 1000 // milliseconds to seconds
      val roundTimestamp = (timestampToSec - (timestampToSec % 86400)) * 1000 // rounds down to day
      json = json.as[JsObject] + (SCHEMA.fMeasurements -> Json.toJson(measurements)) +
        (SCHEMA.fTimestamp -> JsString(s"$roundTimestamp"))
      json = json.as[JsObject] + (SCHEMA.fGeometry -> Json.toJson(
        new GeoJsonPoint(java.lang.Double.parseDouble(fields.get(SCHEMA.fX)),
          java.lang.Double.parseDouble(fields.get(SCHEMA.fY))).get()))
      var sum = 0
      for (measurement <- measurements) {
        sum = sum + measurement(1).toInt
      }
      json = json.as[JsObject] + ("count" -> JsNumber(measurements.size))
      json = json.as[JsObject] + ("sum" -> JsNumber(sum))
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

  def toGeoJson(): JsValue  = {
    var json = toJson()
    try {
      json = json.as[JsObject] + (SCHEMA.fGeometry -> Json.toJson(
        new GeoJsonPoint(java.lang.Double.parseDouble(fields.get(SCHEMA.fX)),
          java.lang.Double.parseDouble(fields.get(SCHEMA.fY))).get()))
    } catch {
      case e: IOException => e.printStackTrace()
    }
    json
  }


  override def toString(): String = _toJsonInternal().toString()

  def toRawRadioMapRecord(): String = {
    val sb = new StringBuilder()
    sb.append(fields.get(SCHEMA.fTimestamp))
    sb.append(" ")
    sb.append(fields.get(SCHEMA.fX))
    sb.append(" ")
    sb.append(fields.get(SCHEMA.fY))
    sb.append(" ")
    sb.append(fields.get(SCHEMA.fHeading))
    sb.append(" ")
    sb.append(fields.get(SCHEMA.fMac))
    sb.append(" ")
    sb.append(fields.get(SCHEMA.fRSS))
    sb.append(" ")
    sb.append(fields.get(SCHEMA.fFloor))
    sb.toString
  }
}
