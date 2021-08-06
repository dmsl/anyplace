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

import datasources.{MongodbDatasource, SCHEMA}
import javax.inject.Singleton
import play.api.libs.json._

object ExternalType extends Enumeration {
  type ExternalType = Value
  val GOOGLE, LOCAL = Value
}

@Singleton
class Account(hm: java.util.HashMap[String, String]) extends AbstractModel {
  private var json: JsValue = _
  this.fields = hm

  def this() = {
    this(new java.util.HashMap[String, String]())
    fields.put(SCHEMA.fSchema, MongodbDatasource.__SCHEMA.toString)
    fields.put(SCHEMA.fOwnerId, "")
    fields.put(SCHEMA.fName, "")
    fields.put(SCHEMA.fType, "")
    fields.put(SCHEMA.fAccessToken, "")
  }

  // TODO make it follow new version of User Json
  def this(json: JsValue) = {
    this()
    fields.put(SCHEMA.fSchema, MongodbDatasource.__SCHEMA.toString)
    fields.put(SCHEMA.fOwnerId, (json \ SCHEMA.fOwnerId).as[String])
    fields.put(SCHEMA.fName, (json \ SCHEMA.fName).as[String])
    fields.put(SCHEMA.fType, (json \ SCHEMA.fType).as[String])
    if ((json \ SCHEMA.fExternal).toOption.isDefined)
      fields.put(SCHEMA.fExternal, (json \ SCHEMA.fExternal).as[String])
    fields.put(SCHEMA.fAccessToken, MongodbDatasource.generateAccessToken(false))
    this.json = json
  }

  def getId(): String = fields.get(SCHEMA.fOwnerId)
  override def toJson(): JsValue = _toJsonInternal()
  override def toString(): String = _toJsonInternal().toString()
  override def toGeoJSON(): String = ???
}
