/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Lambros Petrou, Kyriakos Georgiou
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

package controllers;

import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.*;

import datasources.DatasourceException;
import datasources.ProxyDataSource;
import db_models.NavResultPoint;
import oauth.provider.v2.models.OAuth2Request;
import play.mvc.Controller;
import play.mvc.Result;
import utils.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class AnyplaceNavigation extends Controller {

    public static Result getBuildingById(){
        // create the Request and check it
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if( !anyReq.assertJsonBody() ){
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceNavigation::getBuildingById():: " + json.toString());

        // check if there is any required parameter missing
        List<String> notFound = JsonUtils.requirePropertiesInJson(json, "buid" );
        if( !notFound.isEmpty() ){
            return AnyResponseHelper.requiredFieldsMissing(notFound);
        }

        String buid = json.path("buid").textValue();
        try {
            // fetch the object
            ObjectNode doc = (ObjectNode) ProxyDataSource.getIDatasource().buildingFromKeyAsJson(buid);
            if (doc == null) {
                return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!");
            }
            return AnyResponseHelper.ok(doc, "Successfully fetched building information!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    /**
     * Returns the Point of Interest denoted by the puid - pois - inside the json request
     * @return the Point of Interest as JSON object
     */
    public static Result getPoisById(){
        // create the Request and check it
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if( !anyReq.assertJsonBody() ){
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceNavigation::getPoisById():: " + json.toString());

        // check if there is any required parameter missing
        List<String> notFound = JsonUtils.requirePropertiesInJson(json, "pois" );
        if( !notFound.isEmpty() ){
            return AnyResponseHelper.requiredFieldsMissing(notFound);
        }

        String puid = json.path("pois").textValue();
        try {
            // fetch the object
            ObjectNode doc = (ObjectNode) ProxyDataSource.getIDatasource().poiFromKeyAsJson(puid);
            if (doc == null) {
                return AnyResponseHelper.bad_request("Document does not exist or could not be retrieved!");
            }
            doc.remove("owner_id");
            return AnyResponseHelper.ok(doc, "Successfully fetched Poi information!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }


    /**
     * Returns the Points of Interest that make up the navigation route from the FROM point to the TO point.
     * FROM and TO pois are represented by their IDs - puid -
     * @return Array of Points of Interest - puid -..... WE MAY NEED THE CONNECTIONS TOO TO AVOID MORE REQUESTS FOR EDGE TYPE
     */
    public static Result getNavigationRoute(){

        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if( !anyReq.assertJsonBody() ){
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceNavigation::getNavigationRoute(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json, "pois_from", "pois_to");
        if( !requiredMissing.isEmpty() ){
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        String puid_from = json.path("pois_from").textValue();
        String puid_to = json.path("pois_to").textValue();

        if (puid_from.equals(puid_to)) {
            return AnyResponseHelper.bad_request("Destination and Source is the same!");
        }

        try{
            JsonNode poiFrom = ProxyDataSource.getIDatasource().getFromKeyAsJson(puid_from);
            if( poiFrom == null ){
                return AnyResponseHelper.bad_request("Source POI does not exist or could not be retrieved!");
            }
            JsonNode poiTo = ProxyDataSource.getIDatasource().getFromKeyAsJson(puid_to);
            if( poiFrom == null ){
                return AnyResponseHelper.bad_request("Destination POI does not exist or could not be retrieved!");
            }

            String buid_from = poiFrom.path("buid").textValue();
            String floor_from = poiFrom.get("floor_number").textValue();

            String buid_to = poiTo.path("buid").textValue();
            String floor_to = poiTo.get("floor_number").textValue();

            List<NavResultPoint> points;

            if (buid_from.equalsIgnoreCase(buid_to)) {
                // we are in the same building
                if (floor_from.equalsIgnoreCase(floor_to)) {
                    // we are in the same floor
                    points = navigateSameFloor(poiFrom, poiTo);
                } else {
                    // we are in different floors
                    points = navigateSameBuilding(poiFrom, poiTo);
                }
            } else {
                // we are in different buildings
                return AnyResponseHelper.bad_request("Navigation between buildings not supported yet!");
            }

            // THE FINAL OUTPUT
            ObjectNode res = JsonUtils.createObjectNode();
            res.put( "num_of_pois", points.size() );
            res.put("pois", JsonUtils.getJsonFromList(points) );
            return AnyResponseHelper.ok(res, "Successfully plotted navigation.");

        }catch(DatasourceException e){
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }


    /**
     * Returns the Points of Interest that make up the navigation route from the FROM point to the TO point.
     * FROM is the nearest mapped Point of Interest to the requested coordinates.
     * TO poi is represented by its ID - puid -
     * @return Array of Points of Interest - puid -..... WE MAY NEED THE CONNECTIONS TOO TO AVOID MORE REQUESTS FOR EDGE TYPE
     */
    public static Result getNavigationRouteXY(){
        // create the Request and check it
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if( !anyReq.assertJsonBody() ){
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceNavigation::getNavigationRouteXY():: " + json.toString());

        // check if there is any required parameter missing
        List<String> notFound = JsonUtils.requirePropertiesInJson(json,
                "coordinates_lat",
                "coordinates_lon",
                "floor_number",
                "pois_to");
        if( !notFound.isEmpty() ){
            return AnyResponseHelper.requiredFieldsMissing(notFound);
        }

        // get the location and the floor of the user
        String coordinates_lat = json.path("coordinates_lat").textValue();
        String coordinates_lon = json.path("coordinates_lon").textValue();
        String floor_number = json.path("floor_number").textValue();
        // get the destination puid
        String puid_to = json.path("pois_to").textValue();

        try {
            // get the destination POI from the database
            JsonNode poiTo = ProxyDataSource.getIDatasource().getFromKeyAsJson(puid_to);
            if( poiTo == null ){
                return AnyResponseHelper.bad_request("Destination POI does not exist or could not be retrieved!");
            }

            String buid_to = poiTo.path("buid").textValue();
            String floor_to = poiTo.path("floor_number").textValue();

            double dlat = Double.parseDouble(coordinates_lat);
            double dlon = Double.parseDouble(coordinates_lon);

            // here we must retrieve all the POIS and find the closest POI to the coordinates passed in
            // we use the floor number send in the request since that is user's position
            // I ASSUME THAT THE FLOOR NUMBER IS IN THE SAME BUILDING as the DSTINATION POI
            // TODO - avoid calling twice the get pois from the same floor
            List<JsonNode> floorPois = ProxyDataSource.getIDatasource().poisByBuildingFloorAsJson(buid_to, floor_number);
            if( 0 == floorPois.size() ){
                return AnyResponseHelper.bad_request("Navigation is not supported on your floor!");
            }
            JsonNode poiFrom = null;
            double dlat2,dlon2;
            double min_distance = Double.POSITIVE_INFINITY, cdist;
            // find the closest POI to the coordinates
            for( JsonNode poi : floorPois ){
                // handle each raw radio entry
                dlat2 = Double.parseDouble(poi.path("coordinates_lat").textValue());
                dlon2 = Double.parseDouble(poi.path("coordinates_lon").textValue());
                cdist = GeoPoint.getDistanceBetweenPoints(dlon,dlat, dlon2, dlat2, "K");
                if( cdist < min_distance ){
                    min_distance = cdist;
                    poiFrom = poi;
                }
            }

            if( poiFrom == null ){
                return AnyResponseHelper.bad_request("Navigation is not supported from your position!");
            }
            String buid_from = poiFrom.path("buid").textValue();
            String floor_from = poiFrom.path("floor_number").textValue();

            List<NavResultPoint> points;
            if (buid_from.equalsIgnoreCase(buid_to)) {
                // we are in the same building
                if (floor_from.equalsIgnoreCase(floor_to)) {
                    // we are in the same floor
                    points = navigateSameFloor(poiFrom, poiTo);
                } else {
                    // we are in different floors
                    points = navigateSameBuilding(poiFrom, poiTo);
                }
            } else {
                // we are in different buildings
                return AnyResponseHelper.bad_request("Navigation between buildings not supported yet!");
            }

            // THE FINAL OUTPUT
            ObjectNode result = JsonUtils.createObjectNode();
            result.put( "num_of_pois", points.size() );
            result.put("pois", JsonUtils.getJsonFromList(points) );
            return AnyResponseHelper.ok(result, "Successfully plotted navigation.");

        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }


    ////////////////////////////////////////////////////////////////////////////
    // JACKSON JSON
    ////////////////////////////////////////////////////////////////////////////

    private static List<NavResultPoint> navigateSameFloor( JsonNode from,  JsonNode to ) throws DatasourceException {
        return navigateSameFloor(from, to, ProxyDataSource.getIDatasource()
                .poisByBuildingFloorAsMap(
                        from.path("buid").textValue(),
                        from.path("floor_number").textValue()));
    }

    private static List<NavResultPoint> navigateSameFloor( JsonNode from,  JsonNode to, List<HashMap<String,String>> floorPois ) throws DatasourceException {
        Dijkstra.Graph graph = new Dijkstra.Graph();

        // get the floor pois and add them to the graph
        graph.addPois(floorPois);

        // get connections and add edges to the graph
        graph.addEdges(ProxyDataSource.getIDatasource()
                .connectionsByBuildingAsMap(from.path("buid").textValue()));

        List<HashMap<String,String>> routePois = Dijkstra.getShortestPath(graph, from.path("puid").textValue(), to.path("puid").textValue());
        if( routePois.isEmpty() ){
            return Collections.emptyList();
        }

        // create the list with the final pois to be returned
        List<NavResultPoint> final_points = new ArrayList<NavResultPoint>();

        NavResultPoint p;
        for( HashMap<String,String> poi : routePois ){
            p = new NavResultPoint();
            p.lat = poi.get("coordinates_lat");
            p.lon = poi.get("coordinates_lon");
            p.puid = poi.get("puid");
            p.buid = poi.get("buid");
            p.floor_number = poi.get("floor_number");
            p.pois_type = poi.get("pois_type");
            final_points.add(p);
        }
        return final_points;
    }

    private static List<NavResultPoint> navigateSameBuilding( JsonNode from,  JsonNode to ) throws DatasourceException {
        Dijkstra.Graph graph = new Dijkstra.Graph();

        // add the building pois into the graph
        graph.addPois(ProxyDataSource.getIDatasource().poisByBuildingAsMap(from.path("buid").textValue()));

        // get connections and add edges to the graph
        graph.addEdges(ProxyDataSource.getIDatasource()
                .connectionsByBuildingAsMap(from.path("buid").textValue()));

        List<HashMap<String,String>> routePois = Dijkstra.getShortestPath(graph, from.path("puid").textValue(), to.path("puid").textValue());
        if( routePois.isEmpty() ){
            return Collections.emptyList();
        }

        // create the list with the final pois to be returned
        List<NavResultPoint> final_points = new ArrayList<NavResultPoint>();

        NavResultPoint p;
        for( HashMap<String,String> poi : routePois ){
            p = new NavResultPoint();
            p.lat = poi.get("coordinates_lat");
            p.lon = poi.get("coordinates_lon");
            p.puid = poi.get("puid");
            p.buid = poi.get("buid");
            p.floor_number = poi.get("floor_number");
            final_points.add(p);
        }

        return final_points;
    }




















}
