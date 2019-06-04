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
package controllers

import play.mvc.{Http, Security}
import play.api.mvc.{Action, Result}
import play.Play
import utils._
import play.api.mvc._
import oauth.provider.v2.models.OAuth2Request
import java.util.zip.GZIPOutputStream



import datasources.{DatasourceException, ProxyDataSource}
import db_models._
import scala.util.Try
import play.api.libs.json._ 
import com.couchbase.client.java.document.json.{JsonObject}
import org.joda.time.{DateTime,DateTimeZone}




object AnyplaceFeedback extends play.api.mvc.Controller {

 def addLocationFeedback = Action {
  implicit request =>
    def inner(request: Request[AnyContent]): Result = {

      val anyReq = new OAuth2Request(request)
      if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
      var json = anyReq.getJsonBody
      LPLogger.info("AnyplaceFeedback::addLocationFeedback(): " + json.toString)
      val requiredMissing = JsonUtils.requirePropertiesInJson(json, "dvid", "floor", "buid", "raw_radio")
      if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)

      var feedback_json: JsonObject = JsonObject.empty()

      val buid = (json \ "buid").as[String]
      val floor = (json \ "floor").as[String]
      val dvid = (json \ "dvid").as[String]
      val raw_radio = (json \ "raw_radio").as[String]

      feedback_json.put("dvid", dvid)
      feedback_json.put("buid", buid)
      feedback_json.put("floor", floor)
      feedback_json.put("raw_radio", raw_radio)


      val gps = (json \ "gps").as[String]
      if (gps != null && !gps.trim.isEmpty) {
        Json.parse(gps).validate[JsObject] match {
            case s: JsSuccess[JsObject] => {
              feedback_json.put("gps_lat", extractJsonVal((s.get\"lat")))
              feedback_json.put("gps_lon", extractJsonVal((s.get\"lon")))
              feedback_json.put("gps_acc", extractJsonVal((s.get\"acc")))
            }
            case e: JsError =>
          }
      }

      val usr = (json \ "usr").as[String]
      if (usr != null && !usr.trim.isEmpty) {
        Json.parse(usr).validate[JsObject] match {
          case s: JsSuccess[JsObject] => {
            feedback_json.put("usr_lat", extractJsonVal((s.get\"lat")))
            feedback_json.put("usr_lon", extractJsonVal((s.get\"lon")))
          }
          case e: JsError => 
        }
      }

      val wifi = (json \ "wifi").as[String]
      if (wifi != null && !wifi.trim.isEmpty) {
        Json.parse(wifi).validate[JsObject] match {
          case s: JsSuccess[JsObject] => {
            feedback_json.put("wifi_lat", extractJsonVal((s.get\"lat")))
            feedback_json.put("wifi_lon", extractJsonVal((s.get\"lon")))
          }
          case e: JsError => 
        }
      }


      val currentTimeinMillis = DateTime.now(DateTimeZone.UTC).getMillis()

      feedback_json.put("timestamp", currentTimeinMillis.toString)
      val feedback = new Feedback(feedback_json)

      try {
        LPLogger.info("AnyplaceFeedback::pushing feedback to db")
        if (!ProxyDataSource.getIDatasource.addJsonDocument(feedback.getId, 0, feedback.toCouchGeoJSON())) {
          LPLogger.error("AnyplaceFeedback::Error pushing feedback to db")
          return AnyResponseHelper.bad_request("Feedback could not be added!")
        } else {
          val res = JsonObject.empty()
          LPLogger.info("AnyplaceFeedback::Successfully pushed feedback to db")
          return AnyResponseHelper.ok(res, "Successfully added feedback!")
        }
      }
      catch {
        case e: DatasourceException => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
      }
    }

    inner(request)
  }

  def getLocationFeedback = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        try {
          val feedback = ProxyDataSource.getIDatasource.getLocationFeedback()
          val res = JsonObject.empty()
          res.put("feedback", feedback)
          return AnyResponseHelper.ok(res, "")
        } catch {
            case e: Exception => return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }
    inner(request)
  }

  

  private def extractJsonVal(json: JsLookupResult): String = {
    json.validate[String] match {
      case JsSuccess(s, _) =>  s 
      case e: JsError =>
        "Error"
    }
  }
}