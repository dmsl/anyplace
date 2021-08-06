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
package controllers

import datasources.{ProxyDataSource, SCHEMA}
import location.Algorithms
import models.oauth.OAuth2Request
import modules.floor.Algo1
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import utils._
import utils.json.VALIDATE

import java.io._
import java.util
import java.util.ArrayList
import javax.inject.{Inject, Singleton}

@Singleton
class PositioningController @Inject()(cc: ControllerComponents,
                                      mapHelper: helper.Mapping,
                                      api: AnyplaceServerAPI,
                                      conf: Configuration,
                                      pds: ProxyDataSource)
  extends AbstractController(cc) {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  def predictFloorAlgo1(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        LOG.D2("Positioning: predictFloorAlgo1:")
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        }
        val json = anyReq.getJsonBody()
        var alg1: Algo1 = null
        try {
          alg1 = new Algo1(json)
        } catch {
          case ex: Exception => {
            return RESPONSE.BAD(ex.getClass.toString() + ": " + ex.getMessage + ": " + ex.getCause.toString())
          }
        }
        try {
          val lat = json.\("dlat").as[Double]
          val lot = json.\("dlong").as[Double]
          val bbox = GeoPoint.getGeoBoundingBox(lat, lot, 100)
          val strongestMAC = new ArrayList[String](2)
          if (json.\("first").getOrElse(null) == null) return RESPONSE.BAD("Sent Wi-Fi first parameter.")
          strongestMAC.add(json.\("first").\(SCHEMA.fMac).as[String])
          if (json.\("second").getOrElse(null) != null) strongestMAC.add(json.\("second").\(SCHEMA.fMac).as[String])
          LOG.D2("strongestMAC " + strongestMAC)
          var res: JsValue = Json.obj()
          var msg = ""
          if (pds.db.predictFloor(alg1, bbox, strongestMAC.toArray(Array.ofDim[String](1)))) {
            res = Json.obj(SCHEMA.fFloor -> alg1.getFloor())
            msg = "Successfully predicted floor."
          } else {
            res = Json.obj(SCHEMA.fFloor -> "")
            msg = "Could not predict floor."
          }
          RESPONSE.OK(res, msg)
        } catch {
          case e: Exception => RESPONSE.ERROR_INTERNAL("500: " + e.getMessage + ": " + e.getCause)
        }
      }

      inner(request)
  }

  def estimatePosition(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("Positioning: estimatePosition: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid, SCHEMA.fFloor,"APs","algorithm_choice")
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floor = (json \ SCHEMA.fFloor).as[String]
        val accessOpt = Json.parse((json \ "APs").as[String])
        val tempAP = Json.obj("accessPoint" -> accessOpt)
        val accessPoints = (tempAP \ "accessPoint").as[List[JsValue]]
        val algorithm_choice: Int = (json \ "algorithm_choice").as[String].toInt
        val radioMapsFrozenDir = conf.get[String]("radioMapFrozenDir")
        val rmapFile = new File(radioMapsFrozenDir + api.sep + buid + api.sep +
          floor + api.sep + "indoor-radiomap-mean.txt")
        if (!rmapFile.exists()) {  // Regenerate the radiomap files
          mapHelper.updateFrozenRadioMap(buid, floor)
        }
        val latestScanList: util.ArrayList[location.LogRecord] = new util.ArrayList[location.LogRecord]()
        var i = 0
        for (i <- 0 until accessPoints.size) {
          val bssid = (accessPoints(i) \ "bssid").as[String]
          val rss = (accessPoints(i) \ SCHEMA.fRSS).as[Int]
          latestScanList.add(new location.LogRecord(bssid, rss))
        }

        LOG.D3(latestScanList.toString)
        val radioMap: location.RadioMap = new location.RadioMap(rmapFile)
        var response = Algorithms.ProcessingAlgorithms(latestScanList, radioMap, algorithm_choice)

        if (response == null) { response = "0 0" }
        val lat_long = response.split(" ")
        val res = Json.obj("lat" -> lat_long(0), "long" -> lat_long(1))
        return RESPONSE.OK(res, "Successfully found position.")
      }

      inner(request)
  }

}
