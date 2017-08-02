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

import utils.GeoJSONPoint
import utils.LPUtils
import java.io.IOException
import java.util.HashMap

import com.couchbase.client.java.document.json.{JsonArray, JsonObject}
import play.api.libs.json.JsValue

class Building(hm: HashMap[String, String]) extends AbstractModel {

    private var json: JsonObject = _

    private var lat: Double = _

    private var lng: Double = _

    private var admins: Array[String] = Array()

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

    def this(json: JsonObject) {
        this()
        fields.put("username_creator", json.getString("username_creator"))
        fields.put("owner_id", json.getString("owner_id"))
        fields.put("buid", json.getString("buid"))
        fields.put("is_published", json.getString("is_published"))
        fields.put("name", json.getString("name"))
        fields.put("description", json.getString("description"))
        fields.put("url", json.getString("url"))
        fields.put("address", json.getString("address"))
        fields.put("coordinates_lat", json.getString("coordinates_lat"))
        fields.put("coordinates_lon", json.getString("coordinates_lon"))
        fields.put("bucode", json.getString("bucode"))
        this.json = json
        this.lat = java.lang.Double.parseDouble(json.getString("coordinates_lat"))
        this.lng = java.lang.Double.parseDouble(json.getString("coordinates_lon"))
    }

    def this(json: JsonObject, owner: String) {
        this(json)
        fields.put("owner_id", owner)
    }

    def getId(): String = {
        var buid: String = fields.get("buid")
        if (buid==null||buid.isEmpty || buid == "") {
            val finalId = LPUtils.getRandomUUID + "_" + System.currentTimeMillis()
            fields.put("buid", "building_" + finalId)
            buid = fields.get("buid")
            this.json.put("buid", buid)
        }
        buid
    }

    def toValidCouchJson(): JsonObject = {
        // initialize id if not initialized
        getId()
        JsonObject.from(this.getFields())
    }

    def toCouchGeoJSON(): String = {
        val sb = new StringBuilder()
        var json= JsonObject.empty()
        try {
            json.put("geometry", new GeoJSONPoint(java.lang.Double.parseDouble(fields.get("coordinates_lat")),
                java.lang.Double.parseDouble(fields.get("coordinates_lon")))
              .toGeoJSON())
            if (json.get("co_owners") == null) {
                val ja = JsonArray.empty()
                for (i <- 0 until admins.length) {
                    ja.add(admins(i))
                }
                json.put("co_owners",ja)
            }
            json.removeKey("username")
        } catch {
            case e: IOException => e.printStackTrace()
        }
        sb.append(json.toString)
        sb.toString
    }

    def appendCoOwners(jsonReq: JsValue): String = {
        val sb = new StringBuilder()
        var json= JsonObject.empty()
        try {
            json = JsonObject.empty()
            if (json.get("owner_id") == null || json.getString("owner_id") != jsonReq.\\("owner_id").mkString) {
                return json.toString
            }
            val ja = JsonArray.empty()
            for (i <- 0 until admins.length) {
                ja.add(admins(i))
            }
            val it = jsonReq.\("co_owners").get.productIterator
            while (it.hasNext) {
                val curr = it.next()
                if (curr != null) {
                    ja.add(curr.toString)
                }
            }
            json.put("co_owners",ja)
        } catch {
            case e: IOException => e.printStackTrace()
        }
        sb.append(json.toString)
        sb.toString
    }

    def changeOwner(newOwnerId: String): String = {
        val sb = new StringBuilder()
        var json = JsonObject.empty()
        try {
            this.fields.put("owner_id", newOwnerId)
            val ja = JsonArray.empty()
            for (i <- 0 until admins.length) {
                ja.add(admins(i))
            }
            json.put("co_owners",ja)
        } catch {
            case e: IOException => e.printStackTrace()
        }
        sb.append(json.toString)
        sb.toString
    }

    override def toString(): String = toValidCouchJson().toString
}
