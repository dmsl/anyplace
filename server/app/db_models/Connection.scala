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

import java.util.HashMap

import com.couchbase.client.java.document.json.JsonObject

object Connection {

    val EDGE_TYPE_STAIR = "stair"

    val EDGE_TYPE_ELEVATOR = "elevator"

    val EDGE_TYPE_HALLWAY = "hallway"

    val EDGE_TYPE_ROOM = "room"

    val EDGE_TYPE_OUTDOOR = "outdoor"

    def getId(pois_a: String, pois_b: String): String = "conn_" + pois_a + "_" + pois_b
}

class Connection(hm: HashMap[String, String]) extends AbstractModel {

    private var json: JsonObject = _

    this.fields = hm

    def this() {
        this(new HashMap[String, String])
        fields.put("is_published", "")
        fields.put("edge_type", "")
        fields.put("pois_a", "")
        fields.put("pois_b", "")
        fields.put("weight", "")
        fields.put("buid", "")
        fields.put("floor_a", "")
        fields.put("floor_b", "")
        fields.put("buid_a", "")
        fields.put("buid_b", "")
        fields.put("cuid", "")
    }

    def this(json: JsonObject) {
        this()
        fields.put("is_published", json.getString("is_published"))
        fields.put("edge_type", json.getString("edge_type"))
        fields.put("pois_a", json.getString("pois_a"))
        fields.put("pois_b", json.getString("pois_b"))
        fields.put("weight", json.getString("weight"))
        fields.put("buid", json.getString("buid"))
        fields.put("floor_a", json.getString("floor_a"))
        fields.put("floor_b", json.getString("floor_b"))
        fields.put("buid_a", json.getString("buid_a"))
        fields.put("buid_b", json.getString("buid_b"))
        fields.put("cuid", json.getString("cuid"))
        this.json = json
    }

    def getId(): String = {
        var cuid: String = fields.get("cuid")
        if (cuid.isEmpty  || cuid == "") {
            cuid = Connection.getId(json.getString("pois_a"), json.getString("pois_b"))
            fields.put("cuid", cuid)
            this.json.put("cuid", cuid)
        }
        cuid
    }

    def toValidCouchJson(): JsonObject = {
        // initialize id if not initialized
        getId()
        JsonObject.from(this.getFields())
    }

    override def toCouchGeoJSON(): String = toValidCouchJson().toString

    override def toString(): String = this.toValidCouchJson().toString
}
