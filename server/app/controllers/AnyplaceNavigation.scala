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

import java.util.{ArrayList, HashMap, List}

import com.couchbase.client.java.document.json.JsonObject
import utils.JsonUtils.toCouchObject
import datasources.{DatasourceException, ProxyDataSource}
import db_models.NavResultPoint
import oauth.provider.v2.models.OAuth2Request
import play.api.mvc.Action
import utils._
//remove if not needed
import scala.collection.JavaConversions._

object AnyplaceNavigation extends play.api.mvc.Controller {

  def getBuildingById() = Action {
    implicit request =>
      val anyReq = new OAuth2Request(request)
      if (!anyReq.assertJsonBody()) {
        AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
      }
      val json = anyReq.getJsonBody
      LPLogger.info("AnyplaceNavigation::getBuildingById():: " + json.toString)
      val notFound = JsonUtils.requirePropertiesInJson(json, "buid")
      if (!notFound.isEmpty) {
        AnyResponseHelper.requiredFieldsMissing(notFound)
      }
      val buid = (json \ "buid").as[String]
      try {
        val doc = ProxyDataSource.getIDatasource.buildingFromKeyAsJson(buid)
        if (doc == null) {
          AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!")
        }
        AnyResponseHelper.ok(doc, "Successfully fetched building information!")
      } catch {
        case e: DatasourceException => AnyResponseHelper.internal_server_error("500: " + e.getMessage + "]")
      }
  }

  def getPoisById() = Action {
    implicit request =>

      val anyReq = new OAuth2Request(request)
      if (!anyReq.assertJsonBody()) {
        AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
      }
      val json = anyReq.getJsonBody
      LPLogger.info("AnyplaceNavigation::getPoisById():: " + json.toString)
      val notFound = JsonUtils.requirePropertiesInJson(json, "pois")
      if (!notFound.isEmpty) {
        AnyResponseHelper.requiredFieldsMissing(notFound)
      }
      val puid = (json \ "pois").as[String]
      try {
        val doc = toCouchObject(ProxyDataSource.getIDatasource.poiFromKeyAsJson(puid))
        if (doc == null) {
          AnyResponseHelper.bad_request("Document does not exist or could not be retrieved!")
        }
        doc.removeKey("owner_id")
        AnyResponseHelper.ok(doc, "Successfully fetched Poi information!")
      } catch {
        case e: DatasourceException => AnyResponseHelper.internal_server_error("500: " + e.getMessage + "]")
      }
  }

  def getNavigationRoute() = Action {
    implicit request =>

      val anyReq = new OAuth2Request(request)
      if (!anyReq.assertJsonBody()) {
        AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
      }
      val json = anyReq.getJsonBody
      LPLogger.info("AnyplaceNavigation::getNavigationRoute(): " + json.toString)
      val requiredMissing = JsonUtils.requirePropertiesInJson(json, "pois_from", "pois_to")
      if (!requiredMissing.isEmpty) {
        AnyResponseHelper.requiredFieldsMissing(requiredMissing)
      }
      val puid_from = (json \ "pois_from").as[String]
      val puid_to = (json \ "pois_to").as[String]
      if (puid_from.equalsIgnoreCase(puid_to)) {
        AnyResponseHelper.bad_request("Destination and Source is the same!")
      }
      try {
        val poiFrom = toCouchObject(ProxyDataSource.getIDatasource.getFromKeyAsJson(puid_from))
        if (poiFrom == null) {
          AnyResponseHelper.bad_request("Source POI does not exist or could not be retrieved!")
        }
        val poiTo = toCouchObject(ProxyDataSource.getIDatasource.getFromKeyAsJson(puid_to))
        if (poiFrom == null) {
          AnyResponseHelper.bad_request("Destination POI does not exist or could not be retrieved!")
        }
        val buid_from = poiFrom.getString("buid")
        val floor_from = poiFrom.getString("floor_number")
        val buid_to = poiTo.getString("buid")
        val floor_to = poiTo.getString("floor_number")
        var points: List[JsonObject] = null
        if (buid_from.equalsIgnoreCase(buid_to)) {
          if (floor_from.equalsIgnoreCase(floor_to))
            points = navigateSameFloor(poiFrom, poiTo)
          else
            points = navigateSameBuilding(poiFrom, poiTo)
        } else {
          AnyResponseHelper.bad_request("Navigation between buildings not supported yet!")
        }
        val res = JsonObject.empty()
        res.put("num_of_pois", points.size)
        res.put("pois", points)
        AnyResponseHelper.ok(res, "Successfully plotted navigation.")
      } catch {
        case e: DatasourceException => AnyResponseHelper.internal_server_error("500: " + e.getMessage)
      }
  }

  def getNavigationRouteXY() = Action {
    implicit request =>

      val anyReq = new OAuth2Request(request)
      if (!anyReq.assertJsonBody()) {
        AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
      }
      val json = anyReq.getJsonBody
      LPLogger.info("AnyplaceNavigation::getNavigationRouteXY():: " + json.toString)
      val notFound = JsonUtils.requirePropertiesInJson(json, "coordinates_lat", "coordinates_lon", "floor_number",
        "pois_to")
      if (!notFound.isEmpty) {
        AnyResponseHelper.requiredFieldsMissing(notFound)
      }
      val coordinates_lat = (json \ "coordinates_lat").as[String]
      val coordinates_lon = (json \ "coordinates_lon").as[String]
      val floor_number = (json \ "floor_number").as[String]
      val puid_to = (json \ "pois_to").as[String]
      try {
        val poiTo = toCouchObject(ProxyDataSource.getIDatasource.getFromKeyAsJson(puid_to))
        if (poiTo == null) {
          AnyResponseHelper.bad_request("Destination POI does not exist or could not be retrieved!")
        }
        val buid_to = poiTo.getString("buid")
        val floor_to = poiTo.getString("floor_number")
        val dlat = java.lang.Double.parseDouble(coordinates_lat)
        val dlon = java.lang.Double.parseDouble(coordinates_lon)
        val floorPois = ProxyDataSource.getIDatasource.poisByBuildingFloorAsJson(buid_to, floor_number)
        if (0 == floorPois.size) {
          AnyResponseHelper.bad_request("Navigation is not supported on your floor!")
        }
        var poiFrom: JsonObject = null
        var dlat2: Double = 0.0
        var dlon2: Double = 0.0
        var min_distance = java.lang.Double.POSITIVE_INFINITY
        var cdist: Double = 0.0
        for (poi <- floorPois) {
          dlat2 = java.lang.Double.parseDouble(poi.getString("coordinates_lat"))
          dlon2 = java.lang.Double.parseDouble(poi.getString("coordinates_lon"))
          cdist = GeoPoint.getDistanceBetweenPoints(dlon, dlat, dlon2, dlat2, "K")
          if (cdist < min_distance) {
            min_distance = cdist
            poiFrom = poi
          }
        }
        if (poiFrom == null) {
          AnyResponseHelper.bad_request("Navigation is not supported from your position!")
        }
        val buid_from = poiFrom.getString("buid")
        val floor_from = poiFrom.getString("floor_number")
        var points: List[JsonObject] = null
        if (buid_from.equalsIgnoreCase(buid_to)) {
          points = if (floor_from.equalsIgnoreCase(floor_to)) navigateSameFloor(poiFrom, poiTo) else navigateSameBuilding(poiFrom,
            poiTo)
        } else {
          AnyResponseHelper.bad_request("Navigation between buildings not supported yet!")
        }
        val result = JsonObject.empty()
        result.put("num_of_pois", points.size)
        result.put("pois", points)
        AnyResponseHelper.ok(result, "Successfully plotted navigation.")
      } catch {
        case e: DatasourceException => AnyResponseHelper.internal_server_error("500: " + e.getMessage)
      }
  }

  private def navigateSameFloor(from: JsonObject, to: JsonObject): List[JsonObject] = {
    navigateSameFloor(from, to, ProxyDataSource.getIDatasource.poisByBuildingFloorAsMap(from.getString("buid"),
      from.getString("floor_number")))
  }

  private def navigateSameFloor(from: JsonObject, to: JsonObject, floorPois: List[HashMap[String, String]]): List[JsonObject] = {
    val graph = new Dijkstra.Graph()
    graph.addPois(floorPois)
    graph.addEdges(ProxyDataSource.getIDatasource.connectionsByBuildingAsMap(from.getString("buid")))
    val routePois = Dijkstra.getShortestPath(graph, from.getString("puid"), to.getString("puid"))

    val final_points = new ArrayList[JsonObject]()
    var p: NavResultPoint = null
    for (poi <- routePois) {
      p = new NavResultPoint()
      p.lat = poi.get("coordinates_lat")
      p.lon = poi.get("coordinates_lon")
      p.puid = poi.get("puid")
      p.buid = poi.get("buid")
      p.floor_number = poi.get("floor_number")
      p.pois_type = poi.get("pois_type")
      final_points.add(p.toValidCouchJson())
    }
    final_points
  }

  private def navigateSameBuilding(from: JsonObject, to: JsonObject): List[JsonObject] = {
    val graph = new Dijkstra.Graph()
    graph.addPois(ProxyDataSource.getIDatasource.poisByBuildingAsMap(from.getString("buid")))
    graph.addEdges(ProxyDataSource.getIDatasource.connectionsByBuildingAsMap(from.getString("buid")))
    val routePois = Dijkstra.getShortestPath(graph, from.getString("puid"), to.getString("puid"))
    val final_points = new ArrayList[JsonObject]()
    var p: NavResultPoint = null
    for (poi <- routePois) {
      p = new NavResultPoint()
      p.lat = poi.get("coordinates_lat")
      p.lon = poi.get("coordinates_lon")
      p.puid = poi.get("puid")
      p.buid = poi.get("buid")
      p.floor_number = poi.get("floor_number")
      final_points.add(p.toValidCouchJson())
    }
    final_points
  }
}
