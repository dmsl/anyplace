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

import java.util.{ArrayList, HashMap, List}

import datasources.{DatasourceException, ProxyDataSource}
import db_models.NavResultPoint
import json.JsonValidator.{validateCoordinate, validateString, validateStringNumber}
import oauth.provider.v2.models.OAuth2Request
import play.api.libs.json.{JsObject, JsValue, Json}
import play.api.mvc.{Action, AnyContent, Request, Result}
import utils._
//remove if not needed
import scala.collection.JavaConversions._

object AnyplaceNavigation extends play.api.mvc.Controller {
  val ROUTE_MAX_DISTANCE_ALLOWED = 5.0


  def getBuildingById() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceNavigation::getBuildingById():: " + json.toString)
        val notFound = JsonUtils.hasProperties(json, "buid")
        if (!notFound.isEmpty) {
          return AnyResponseHelper.requiredFieldsMissing(notFound)
        }
        if (validateString(json, "buid") == null)
          return AnyResponseHelper.bad_request("Buid field must be String!")
        val buid = (json \ "buid").as[String]
        try {
          val doc = ProxyDataSource.getIDatasource.buildingFromKeyAsJson(buid)
          if (doc == null) {
            return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
          }
          return AnyResponseHelper.ok(doc, "Successfully fetched building information!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def getPoisById() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceNavigation::getPoisById():: " + json.toString)
        val notFound = JsonUtils.hasProperties(json, "pois")
        if (!notFound.isEmpty) {
          return AnyResponseHelper.requiredFieldsMissing(notFound)
        }
        if (validateString(json, "pois") == null)
          return AnyResponseHelper.bad_request("Puid field must be String!")
        val puid = (json \ "pois").as[String]
        try {
          var doc = ProxyDataSource.getIDatasource.poiFromKeyAsJson("pois", "puid", puid)
          if (doc == null) {
            return AnyResponseHelper.bad_request("Document does not exist or could not be retrieved!")
          }
          doc = doc.as[JsObject] - "owner_id" - "_id" - "_schema"
          return AnyResponseHelper.ok(doc, "Successfully fetched Poi information!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage + "]")
        }
      }

      inner(request)
  }

  def getNavigationRoute() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceNavigation::getNavigationRoute(): " + json.toString)
        val requiredMissing = JsonUtils.hasProperties(json, "pois_from", "pois_to")
        if (!requiredMissing.isEmpty) {
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        }
        if (validateString(json, "pois_from") == null)
          return AnyResponseHelper.bad_request("pois_from field must be String!")
        val puid_from = (json \ "pois_from").as[String]
        if (validateString(json, "pois_to") == null)
          return AnyResponseHelper.bad_request("pois_to field must be String!")
        val puid_to = (json \ "pois_to").as[String]
        if (puid_from.equalsIgnoreCase(puid_to)) {
          return AnyResponseHelper.bad_request("Destination and Source is the same!")
        }
        try {
          val poiFrom = ProxyDataSource.getIDatasource.getFromKeyAsJson("pois", "puid", puid_from)
          if (poiFrom == null) {
            return AnyResponseHelper.bad_request("Source POI does not exist or could not be retrieved!")
          }
          val poiTo = ProxyDataSource.getIDatasource.getFromKeyAsJson("pois", "puid", puid_to)
          if (poiTo == null) {
            return AnyResponseHelper.bad_request("Destination POI does not exist or could not be retrieved!")
          }
          val buid_from = (poiFrom \ "buid").as[String]
          val floor_from = (poiFrom \ "floor_number").as[String]
          val buid_to = (poiTo \ "buid").as[String]
          val floor_to = (poiTo \ "floor_number").as[String]
          var points: List[JsValue] = null
          if (buid_from.equalsIgnoreCase(buid_to)) {
            if (floor_from.equalsIgnoreCase(floor_to)) {
              points = navigateSameFloor(poiFrom, poiTo)
            } else {
              points = navigateSameBuilding(poiFrom, poiTo)
            }
          } else {
            return AnyResponseHelper.bad_request("Navigation between buildings not supported yet!")
          }
          val res: JsValue = Json.obj("num_of_pois" -> points.size, "pois" -> points.toList)
          return AnyResponseHelper.ok(res, "Successfully plotted navigation.")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  def getNavigationRouteXY() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceNavigation::getNavigationRouteXY():: " + json.toString)
        val notFound = JsonUtils.hasProperties(json, "coordinates_lat", "coordinates_lon", "floor_number",
          "pois_to")
        if (!notFound.isEmpty) return AnyResponseHelper.requiredFieldsMissing(notFound)
        if (validateCoordinate(json, "coordinates_lat") == null)
          return AnyResponseHelper.bad_request("coordinates_lat field must be String containing a float!")
        val coordinates_lat = (json \ "coordinates_lat").as[String]
        if (validateCoordinate(json, "coordinates_lon") == null)
          return AnyResponseHelper.bad_request("coordinates_lon field must be String containing a float!")
        val coordinates_lon = (json \ "coordinates_lon").as[String]
        if (validateStringNumber(json, "floor_number") == null)
          return AnyResponseHelper.bad_request("Floor_number field must be String, containing a number!")
        val floor_number = (json \ "floor_number").as[String]
        if (validateString(json, "pois_to") == null)
          return AnyResponseHelper.bad_request("pois_to field must be String!")
        val puid_to = (json \ "pois_to").as[String]
        var res: Result = null
        try {
          val poiTo = ProxyDataSource.getIDatasource.getFromKeyAsJson("pois", "puid", puid_to)
          if (poiTo == null) {
            return AnyResponseHelper.bad_request("Destination POI does not exist or could not be retrieved!")
          }
          val buid_to = (poiTo \ "buid").as[String]
          val floor_to = (poiTo \ "floor_number").as[String]
          val dlat = java.lang.Double.parseDouble(coordinates_lat)
          val dlon = java.lang.Double.parseDouble(coordinates_lon)
          val floorPois = ProxyDataSource.getIDatasource.poisByBuildingFloorAsJson(buid_to, floor_number)
          if (0 == floorPois.size) {
            return AnyResponseHelper.bad_request("Navigation is not supported on your floor!")
          }

          var dlat2: Double = 0.0
          var dlon2: Double = 0.0
          var min_distance = java.lang.Double.POSITIVE_INFINITY
          var cdist: Double = 0.0
          // Find the closest POI to start navigation
          var startingPoi: JsValue = null
          for (poi <- floorPois) {
            dlat2 = java.lang.Double.parseDouble((poi \ "coordinates_lat").as[String])
            dlon2 = java.lang.Double.parseDouble((poi \ "coordinates_lon").as[String])
            cdist = GeoPoint.getDistanceBetweenPoints(dlon, dlat, dlon2, dlat2, "K")
            if (cdist < min_distance) {
              min_distance = cdist
              startingPoi = poi
            }
          }
          if (startingPoi == null) {
            LPLogger.error("Nav pos")
            return AnyResponseHelper.bad_request("Navigation is not supported from your position!")
          } else if (min_distance > ROUTE_MAX_DISTANCE_ALLOWED) {
            LPLogger.error("5km Nav pos")
            return AnyResponseHelper.bad_request("No Navigation supported at this position: startingPoi>=5km")
          }
          LPLogger.debug("Starting poi: " + (startingPoi \ "puid").as[String])
          LPLogger.debug("min_distance: " + min_distance)
          val buid_from = (startingPoi \ "buid").as[String]
          val floor_from = (startingPoi \ "floor_number").as[String]
          var points: List[JsValue] = null
          if (buid_from.equalsIgnoreCase(buid_to)) {
            points = if (floor_from.equalsIgnoreCase(floor_to)) navigateSameFloor(startingPoi, poiTo) else navigateSameBuilding(startingPoi,
              poiTo)
          } else {
            LPLogger.error("Nav unsupported")
            return AnyResponseHelper.bad_request("Navigation between buildings not supported yet!")
          }
          val json: JsValue = Json.obj("num_of_pois" -> points.size, "pois" -> points.toList)
          return AnyResponseHelper.ok(json, "Successfully plotted navigation.")
        } catch {
          //case e: DatasourceException => AnyResponseHelper.internal_server_error("500: " + e.getMessage)
          case e: Exception => return AnyResponseHelper.internal_server_error(e.getClass + ": " + e.getMessage)
        }
      }

      inner(request)
  }

  private def navigateSameFloor(from: JsValue, to: JsValue): List[JsValue] = {
    navigateSameFloor(from, to, ProxyDataSource.getIDatasource.poisByBuildingFloorAsMap((from \ "buid").as[String],
      (from \ "floor_number").as[String]))
  }

  private def navigateSameFloor(from: JsValue, to: JsValue, floorPois: List[HashMap[String, String]]): List[JsValue] = {
    val graph = new Dijkstra.Graph()
    graph.addPois(floorPois)
    graph.addEdges(ProxyDataSource.getIDatasource.connectionsByBuildingAsMap((from \ "buid").as[String]))
    val routePois = Dijkstra.getShortestPath(graph, (from \ "puid").as[String], (to \ "puid").as[String])

    val final_points = new ArrayList[JsValue]()
    var p: NavResultPoint = null
    for (poi <- routePois) {
      p = new NavResultPoint()
      p.lat = poi.get("coordinates_lat")
      p.lon = poi.get("coordinates_lon")
      p.puid = poi.get("puid")
      p.buid = poi.get("buid")
      p.floor_number = poi.get("floor_number")
      p.pois_type = poi.get("pois_type")
      final_points.add(p.toValidMongoJson())
    }
    final_points
  }

  private def navigateSameBuilding(from: JsValue, to: JsValue): List[JsValue] = {
    val graph = new Dijkstra.Graph()
    graph.addPois(ProxyDataSource.getIDatasource.poisByBuildingAsMap((from \ "buid").as[String]))
    graph.addEdges(ProxyDataSource.getIDatasource.connectionsByBuildingAsMap((from \ "buid").as[String]))
    val routePois = Dijkstra.getShortestPath(graph, (from \ "puid").as[String], (to \ "puid").as[String])
    val final_points = new ArrayList[JsValue]()
    var p: NavResultPoint = null
    for (poi <- routePois) {
      p = new NavResultPoint()
      p.lat = poi.get("coordinates_lat")
      p.lon = poi.get("coordinates_lon")
      p.puid = poi.get("puid")
      p.buid = poi.get("buid")
      p.floor_number = poi.get("floor_number")
      final_points.add(p.toValidMongoJson())
    }
    final_points
  }
}
