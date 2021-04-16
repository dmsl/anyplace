/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Stefanos Kyriakou, Panayiotis Leontiou, Stelios Tymvios
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

import java.util

import com.google.gson.JsonParseException
import datasources.{DatasourceException, InfluxdbDatasource}
import io.razem.influxdbclient.Point
import oauth.provider.v2.models.OAuth2Request
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent}
import utils.{AnyResponseHelper, _}

import scala.concurrent.ExecutionContext.Implicits.global

object AnyplaceInfluxdb extends play.api.mvc.Controller {


	def locationLookup: Action[AnyContent] = Action.async {
		implicit request =>

			var notFound: util.List[String] = new util.ArrayList[String]()
			try {
				val anyReq = new OAuth2Request(request)

				if (!anyReq.assertJsonBody())
					throw new JsonParseException("OATH parse error.")
				val json = anyReq.getJsonBody()

				var gp1: GeoPoint = new GeoPoint()
				var gp2: GeoPoint = new GeoPoint()
				val base = JsonUtils.hasProperties(json, "deviceID", "beginTime", "endTime")
				val twoPoints = JsonUtils.hasProperties(json, "point1", "point2")
				val onePoint = JsonUtils.hasProperties(json, "point", "distance")

				(
					base isEmpty,
					twoPoints isEmpty,
					onePoint isEmpty) match {
					// when base input validation failed
					case (false, _, _) => 
						notFound = base
						throw new NoSuchFieldException()
					
					// when none of two apis validate raise error
					case (true, false, false) => 
						notFound = twoPoints
						throw new NoSuchFieldException()
					
					// BoxByRange, i.e. two points is preferred over bounding box with distance
					// due to better floating point behavior as the distance is a floating point in KM
					// eg. "0.13".toFloat is not exactly 0.13 due to floating point precision
					case (true, true, _) =>
						val bb = GeoPoint.getGeoBoundingBoxByRange((json \ "point1").as[GeoPoint], (json \ "point2").as[GeoPoint])
						gp1 = bb(1)
						gp2 = bb(0)
					
					case (true, false, true) =>
						val bb = GeoPoint.getGeoBoundingBox((json \ "point").as[GeoPoint], (json \ "distance").as[String].toDouble)
						gp1 = bb(0)
						gp2 = bb(1)
				}

				val deviceID = (json \ "deviceID").as[String]
				val beginTime = (json \ "beginTime").as[Long]
				val endTime = (json \ "endTime").as[Long]

				val infdb = InfluxdbDatasource.getStaticInstance
				infdb.devicePointsInBoundingBox(gp1, gp2, List(deviceID), beginTime, endTime)
  				.map { m=> Ok(Json.toJson(m)) }

			}
			catch {
				case e: DatasourceException => scala.concurrent.Future {} map { _ => AnyResponseHelper.bad_request(e getMessage) }
				case e: NullPointerException => scala.concurrent.Future {} map { _ => AnyResponseHelper.bad_request(e getMessage) }
				case _: JsonParseException => scala.concurrent.Future {} map { _ => AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON) }
				case _: NoSuchFieldException => scala.concurrent.Future {} map { _ => AnyResponseHelper.requiredFieldsMissing(notFound) }
				case _: JsResultException => scala.concurrent.Future {} map { _ => AnyResponseHelper.bad_request("Json validation failed") }
			}
	}

	def insertInfluxdb(): Action[AnyContent] = Action.async {
		implicit request =>

			var notFound: util.List[String] = new util.ArrayList[String]()
			try {
				val anyReq = new OAuth2Request(request)
				if (!anyReq.assertJsonBody())
					throw new JsonParseException("OATH parse error.")
				val json = anyReq.getJsonBody()

				notFound = JsonUtils.hasProperties(json, "point","deviceID","timestamp")
				if (!notFound.isEmpty)
					throw new NoSuchFieldException()


				val infdb = InfluxdbDatasource.getStaticInstance
				val deviceID = (json \ "deviceID").as[String]
				val timestamp = (json \ "timestamp").as[Float]
				val gp =  (json \ "point").as[GeoPoint]

				val point = Point("location")
					.addTag("deviceID", deviceID)
					// using config defined (or default) precision
					.addTag("geohash", gp asGeohash infdb.stored_precision)
					.addField("timestamp", timestamp)
					.addField("latitude", gp dlat)
					.addField("longitude", gp dlon)
				infdb.write(point) map { _ => AnyResponseHelper.ok("Point Written") }
			}
			catch {
				case e: DatasourceException => scala.concurrent.Future {} map { _ => AnyResponseHelper.bad_request(e getMessage) }
				case e: NullPointerException => scala.concurrent.Future {} map { _ => AnyResponseHelper.bad_request(e getMessage) }
				case _: JsonParseException => scala.concurrent.Future {} map { _ => AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON) }
				case _: NoSuchFieldException => scala.concurrent.Future {} map { _ => AnyResponseHelper.requiredFieldsMissing(notFound) }
				case e: JsResultException => scala.concurrent.Future {} map { _ => AnyResponseHelper.bad_request(e getMessage) }
			}
	}
}