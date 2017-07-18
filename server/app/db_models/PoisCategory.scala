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

import utils.LPUtils
import java.io.IOException
import java.util
import java.util.HashMap

import com.couchbase.client.java.document.json.JsonObject

//remove if not needed
import scala.collection.JavaConversions._

class PoisCategory(hm: HashMap[String, String]) extends AbstractModel {

  private var json: JsonObject = _

  private var admins: Array[String] = Array("112997031510415584062_google")

  this.fields = hm

  def this() = {
    this(new util.HashMap[String, String]())
    fields.put("poistypeid", "")
    fields.put("poistype", "")
    fields.put("owner_id", "")
    fields.put("types", "[]")
  }

  def this(json: JsonObject) = {
    this()
    fields.put("owner_id", json.getString("owner_id"))
    fields.put("poistypeid", json.getString("cuid"))
    fields.put("poistype", json.getString("name"))
    this.json = json
  }

  def this(json: JsonObject, owner: String) = {
    this(json)
    fields.put("owner_id", owner)
  }

  def getId(): String = {
    var poistypeid: String = fields.get("poistypeid")
    if (poistypeid.isEmpty || poistypeid.==("")) {
      val finalId: String = LPUtils.getRandomUUID + "_" + System
        .currentTimeMillis()
      fields.put("poistypeid", "poistypeid_" + finalId)
      poistypeid = fields.get("poistypeid")
      this.json.put("poistypeid", poistypeid)
    }
    poistypeid
  }

  def toValidCouchJson(): JsonObject = {
    // initialize id if not initialized
    getId()
    JsonObject.from(this.getFields())
  }

  def toCouchGeoJSON(): String = {
    val sb: StringBuilder = new StringBuilder()
    json.removeKey("access_token")
    sb.append(this.json.toString)
    sb.toString
  }

  def changeOwner(newOwnerId: String): String = {
    val sb: StringBuilder = new StringBuilder()
    var json: JsonObject = null
    try {
      this.fields.put("owner_id", newOwnerId)
      json = toValidCouchJson()
    } catch {
      case e: IOException => e.printStackTrace()

    }
    sb.append(json.toString)
    sb.toString
  }

  override def toString: String = toValidCouchJson().toString

}
