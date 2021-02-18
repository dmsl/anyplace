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

import java.io.IOException

import com.couchbase.client.java.document.json.JsonObject
import play.api.libs.json._
import utils.LPLogger

import scala.collection.JavaConverters.mapAsScalaMapConverter

object ExternalType extends Enumeration {
    type ExternalType = Value
    val GOOGLE, LOCAL = Value
}

class Account(hm: java.util.HashMap[String, String]) extends AbstractModel {

    private var json: JsValue = _

    this.fields = hm

    def this() {
        this(new java.util.HashMap[String, String]())
        fields.put("name", "")
        fields.put("id", "")
        fields.put("type", "")
        //fields.put("doc_type", "account")
    }


    // TODO make it follow new version of User Json
    def this(json: JsValue) {
        this()
        fields.put("name", (json \ "name").as[String])
        fields.put("id", (json \ "id").as[String])
        fields.put("type", (json \ "type").as[String])
        if ((json \ "external").toOption.isDefined)
            fields.put("external", (json \ "external").as[String])
        this.json = json
    }

    def getId(): String = {
        val puid = fields.get("id")
        puid
    }

    def toValidCouchJson(): JsonObject = {
        JsonObject.from(this.getFields())
    }

    def toJson(): JsValue = {
        val sMap: Map[String, String] = this.getFields().asScala.toMap
        Json.toJson(sMap)
    }


    // TODO: replace with mongo
    def toCouchGeoJSON(): String = {
        LPLogger.error("TODO:nn convert to mdb")
        val sb = new StringBuilder()
        var json: JsonObject = null
        try {
            json = JsonObject.empty()
        } catch {
            case e: IOException => e.printStackTrace()
        }
        sb.append(json.toString)
        sb.toString
    }

    def _toString(): String = toValidCouchJson().toString

    override def toString(): String = toJson().toString()
}
