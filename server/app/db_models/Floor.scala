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

object Floor {

    def getId(buid: String, floor_number: String): String = buid + "_" + floor_number

    def checkFloorNumberFormat(floor_number: String): Boolean = {
        floor_number.toCharArray().find(java.lang.Character.isWhitespace(_))
          .map(_ => false)
          .getOrElse(true)
    }
}

class Floor(hm: HashMap[String, String]) extends AbstractModel {

    private var json: JsonObject = _

    this.fields = hm

    def this() {
        this(new HashMap[String, String])
        fields.put("username_creator", "")
        fields.put("buid", "")
        fields.put("is_published", "")
        fields.put("floor_name", "")
        fields.put("floor_number", "")
        fields.put("description", "")
    }

    def this(json: JsonObject) {
        this()
        fields.put("username_creator", json.getString("username_creator"))
        fields.put("fuid", json.getString("fuid"))
        fields.put("buid", json.getString("buid"))
        fields.put("is_published", json.getString("is_published"))
        fields.put("floor_name", json.getString("floor_name"))
        fields.put("description", json.getString("description"))
        fields.put("floor_number", json.getString("floor_number"))
        this.json = json
    }

    def getId(): String = {
        var fuid: String = this.json.getString("fuid")
        if (fuid.trim().isEmpty) {
            fuid = Floor.getId(fields.get("buid"), fields.get("floor_number"))
            fields.put("fuid", fuid)
            this.json.put("fuid", fuid)
        }
        fuid
    }
    def toValidCouchJson(): JsonObject = {
        // initialize id if not initialized
        getId()
        fields.remove("username")
        JsonObject.from(this.getFields())
    }

    override def toCouchGeoJSON(): String = toValidCouchJson().toString

    override def toString(): String = this.toValidCouchJson().toString
}
