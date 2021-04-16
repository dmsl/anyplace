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
import play.api.libs.json.{JsObject, JsString, JsValue, Json}
import utils.JsonUtils.convertToInt

import scala.collection.JavaConverters.mapAsScalaMapConverter

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

    def this() {
        this(new HashMap[String, String])
        fields.put("buid", "")
        fields.put("is_published", "")
        fields.put("floor_name", "")
        fields.put("floor_number", "")
        fields.put("description", "")
    }

    def this(json: JsValue) {
        this()
        if ((json\"fuid").toOption.isDefined)
            fields.put("fuid", (json\"fuid").as[String])
        if ((json\"buid").toOption.isDefined)
            fields.put("buid", (json\"buid").as[String])
        if ((json\"is_published").toOption.isDefined)
            fields.put("is_published", (json\"is_published").as[String])
        if ((json\"floor_name").toOption.isDefined)
            fields.put("floor_name", (json\"floor_name").as[String])
        if ((json\"description").toOption.isDefined) {
            val temp = (json\"description").as[String]
            if (temp != "" && temp != "-")
                fields.put("description", (json\"description").as[String])
            else
                fields.remove("description")
        } else
            fields.remove("description")
        if ((json\"floor_number").toOption.isDefined)
            fields.put("floor_number", (json\"floor_number").as[String])
        this.json = json
    }

    def getId(): String = {
        var fuid = (this.json\"fuid")
        var newFuid = ""
        if (!fuid.toOption.isDefined) {
            newFuid = Floor.getId(fields.get("buid"), fields.get("floor_number"))
            fields.put("fuid", newFuid)
            this.json = this.json.as[JsObject] + ("fuid" -> JsString(newFuid))
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
        convertToInt("_schema", res)
    }

    override def toGeoJSON(): String = toJson().toString

    override def toString(): String = this.toJson().toString
}
