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

import com.couchbase.client.java.document.json.JsonObject

object Poi {

    val POIS_TYPE_NONE = "None"

    val POIS_TYPE_ELEVATOR = "elevator"

    val POIS_TYPE_STAIR = "stair"

    def getId(username_creator: String,
              buid: String,
              floor_number: String,
              coordinates_lat: String,
              coordinates_lon: String): String = "poi_" + LPUtils.getRandomUUID
}

class Poi(hm: HashMap[String, String]) extends AbstractModel {

    private var json: JsonObject = _

    private var lat: Double = _

    private var lng: Double = _

    this.fields = hm

    def this() {
        this(new HashMap[String, String])
        fields.put("username", "")
        fields.put("username_creator", "")
        fields.put("puid", "")
        fields.put("buid", "")
        fields.put("is_published", "")
        fields.put("floor_name", "")
        fields.put("floor_number", "")
        fields.put("name", "")
        fields.put("description", "")
        fields.put("url", "")
        fields.put("image", "")
        fields.put("pois_type", "")
        fields.put("is_door", "")
        fields.put("coordinates_lat", "")
        fields.put("coordinates_lon", "")
    }

    def this(json: JsonObject) {
        this()
        fields.put("username_creator", json.getString("username_creator"))
        fields.put("puid", json.getString("puid"))
        fields.put("buid", json.getString("buid"))
        fields.put("is_published", json.getString("is_published"))
        fields.put("floor_name", json.getString("floor_name"))
        fields.put("floor_number", json.getString("floor_number"))
        fields.put("name", json.getString("name"))
        if (json.getString("description") != null && json.getString("description") != null &&
          !json.getString("description").isEmpty) {
            fields.put("description", json.getString("description"))
        } else {
            fields.put("description", "-")
        }
        if (json.getString("url") != null && json.getString("url") != null &&
          !json.getString("url").isEmpty) {
            fields.put("url", json.getString("url"))
        } else {
            fields.put("url", "-")
        }
        fields.put("image", json.getString("image"))
        fields.put("pois_type", json.getString("pois_type"))
        fields.put("is_door", json.getString("is_door"))
        fields.put("is_building_entrance", json.getString("is_building_entrance"))
        fields.put("coordinates_lat", json.getString("coordinates_lat"))
        fields.put("coordinates_lon", json.getString("coordinates_lon"))
        this.json = json
        this.lat = java.lang.Double.parseDouble(json.getString("coordinates_lat"))
        this.lng = java.lang.Double.parseDouble(json.getString("coordinates_lon"))
    }

    def getId(): String = {
        var puid: String = fields.get("puid")
        if (puid==null || puid.isEmpty || puid == "") {
            fields.put("puid", Poi.getId(fields.get("username_creator"), fields.get("buid"), fields.get("floor_number"),
                fields.get("coordinates_lat"), fields.get("coordinates_lon")))
            this.json.put("puid", fields.get("puid"))
            puid = fields.get("puid")
        }
        puid
    }

    def toValidCouchJson(): JsonObject = {
        // initialize id if not initialized
        getId
        JsonObject.from(this.getFields())
    }

    def toCouchGeoJSON(): String = {
        val sb = new StringBuilder()
        val json= toValidCouchJson()
        try {
            json.put("geometry", new GeoJSONPoint(java.lang.Double.parseDouble(fields.get("coordinates_lat")),
                java.lang.Double.parseDouble(fields.get("coordinates_lon")))
              .toGeoJSON())
            json.removeKey("username")
        } catch {
            case e: IOException => e.printStackTrace()
        }
        sb.append(json.toString)
        sb.toString
    }

    override def toString(): String = toValidCouchJson().toString
}
