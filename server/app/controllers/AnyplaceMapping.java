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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import datasources.DatasourceException;
import datasources.ProxyDataSource;
import db_models.*;
import oauth.provider.v2.models.OAuth2Request;
import org.apache.commons.codec.binary.Base64;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import play.mvc.Results;
import utils.*;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

public class AnyplaceMapping extends Controller {

    private static String verifyOwnerId(String authToken) {

        String gURL = "https://www.googleapis.com/oauth2/v1/tokeninfo?access_token=" + authToken;

        String res = null;

        try {
            res = sendGet(gURL);
        } catch (Exception e) {
            LPLogger.error(e.toString());
            return null;
        }

        if (res != null) {
            try {
                JsonNode json = (ObjectNode) JsonUtils.getJsonTree(res);
                return json.get("user_id") == null ? null : json.get("user_id").textValue();
            } catch (IOException ioe) {
                return null;
            }
        }

        return null;
    }

    private static String appendToOwnerId(String ownerId) {
        return ownerId + "_google";
    }

    // HTTP GET request
    private static String sendGet(String url) throws Exception {

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        // optional default is GET
        con.setRequestMethod("GET");

        int responseCode = con.getResponseCode();

        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        return response.toString();
    }

    public static Result getRadioHeatmap() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::getRadioHeatmap(): " + json.toString());

        try {
            List<JsonNode> radioPoints = ProxyDataSource.getIDatasource().getRadioHeatmap();
            if (radioPoints == null) {
                return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!");
            }
            ObjectNode res = JsonUtils.createObjectNode();
            res.put("radioPoints", JsonUtils.getJsonFromList(radioPoints));
            return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    public static Result getRadioHeatmapByBuildingFloor() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }

        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::getRadioHeatmap(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "buid",
                "floor");

        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        String buid = json.path("buid").textValue();
        String floor = json.path("floor").textValue();

        try {
            List<JsonNode> radioPoints = ProxyDataSource.getIDatasource().getRadioHeatmapByBuildingFloor(buid, floor);
            if (radioPoints == null) {
                return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!");
            }
            ObjectNode res = JsonUtils.createObjectNode();
            res.put("radioPoints", JsonUtils.getJsonFromList(radioPoints));
            try {
//                if (request().getHeader("Accept-Encoding") != null && request().getHeader("Accept-Encoding").contains("gzip")) {
                return gzippedJSONOk(res.toString());
//                }
//                return AnyResponseHelper.ok(res.toString());
            } catch (IOException ioe) {
                return AnyResponseHelper.ok(res, "Successfully retrieved all radio points!");
            }
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    public static Result deleteRadiosInBox() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }

        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::deleteRadiosInBox(): " + json.toString());

        try {
            if (!ProxyDataSource.getIDatasource().deleteRadiosInBox()) {
                return AnyResponseHelper.bad_request("Building already exists or could not be added!");
            }
            return AnyResponseHelper.ok("Success");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }

    }

    /**
     * Adds a new building to the database
     *
     * @return the newly created Building ID is included in the response if success
     */
    public static Result buildingAdd() {

        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }

        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::buildingAdd(): " + json.toString());


        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "is_published",
                "name",
                "description",
                "url",
                "address",
                "coordinates_lat",
                "coordinates_lon",
                "access_token");

        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        // get access token from url and check it against google's service
        if (json.findValue("access_token") == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        String owner_id = verifyOwnerId(json.findValue("access_token").textValue());
        if (owner_id == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        owner_id = appendToOwnerId(owner_id);
        ((ObjectNode) json).put("owner_id", owner_id);

        try {
            Building building;
            try {
                building = new Building(json);
            } catch (NumberFormatException e) {
                return AnyResponseHelper.bad_request("Building coordinates are invalid!");
            }
            //System.out.println(building.toValidCouchJson());
            if (!ProxyDataSource.getIDatasource().addJsonDocument(building.getId(), 0, building.toCouchGeoJSON())) {
                return AnyResponseHelper.bad_request("Building already exists or could not be added!");
            }
            ObjectNode res = JsonUtils.createObjectNode();
            res.put("buid", building.getId());
            return AnyResponseHelper.ok(res, "Successfully added building!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    public static Result buildingUpdateCoOwners() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::buildingUpdateCoOwners(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid"
                , "access_token"
                , "co_owners"
        );
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }
        // get access token from url and check it against google's service
        if (json.findValue("access_token") == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        String owner_id = verifyOwnerId(json.findValue("access_token").textValue());
        if (owner_id == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        owner_id = appendToOwnerId(owner_id);
        ((ObjectNode) json).put("owner_id", owner_id);


        String buid = json.path("buid").textValue();

        try {
            ObjectNode stored_building = (ObjectNode) ProxyDataSource.getIDatasource().getFromKeyAsJson(buid);
            if (stored_building == null) {
                return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!");
            }

            if (!isBuildingOwner(stored_building, owner_id)) {
                return AnyResponseHelper.unauthorized("Unauthorized");
            }

            Building building = new Building(stored_building);
            if (!ProxyDataSource.getIDatasource().replaceJsonDocument(building.getId(), 0, building.appendCoOwners(json))) {
                return AnyResponseHelper.bad_request("Building could not be updated!");
            }

            return AnyResponseHelper.ok("Successfully updated building!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    public static Result buildingUpdateOwner() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::buildingUpdateCoOwners(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid"
                , "access_token"
                , "new_owner"
        );
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }
        // get access token from url and check it against google's service
        if (json.findValue("access_token") == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        String owner_id = verifyOwnerId(json.findValue("access_token").textValue());
        if (owner_id == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        owner_id = appendToOwnerId(owner_id);
        ((ObjectNode) json).put("owner_id", owner_id);


        String buid = json.path("buid").textValue();

        String newOwner = json.path("new_owner").textValue();
        newOwner = appendToOwnerId(newOwner);

        try {
            ObjectNode stored_building = (ObjectNode) ProxyDataSource.getIDatasource().getFromKeyAsJson(buid);
            if (stored_building == null) {
                return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!");
            }

            if (!isBuildingOwner(stored_building, owner_id)) {
                return AnyResponseHelper.unauthorized("Unauthorized");
            }

            Building building = new Building(stored_building);
            if (!ProxyDataSource.getIDatasource().replaceJsonDocument(building.getId(), 0, building.changeOwner(newOwner))) {
                return AnyResponseHelper.bad_request("Building could not be updated!");
            }

            return AnyResponseHelper.ok("Successfully updated building!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    /**
     * Update the building information. Building to update is specified by buid
     *
     * @return
     */
    public static Result buildingUpdate() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::buildingUpdate(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid"
                , "access_token"
        );
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }
        // get access token from url and check it against google's service
        if (json.findValue("access_token") == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        String owner_id = verifyOwnerId(json.findValue("access_token").textValue());
        if (owner_id == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        owner_id = appendToOwnerId(owner_id);
        ((ObjectNode) json).put("owner_id", owner_id);

        String buid = json.path("buid").textValue();

        try {
            ObjectNode stored_building = (ObjectNode) ProxyDataSource.getIDatasource().getFromKeyAsJson(buid);
            if (stored_building == null) {
                return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!");
            }

            if (!isBuildingOwner(stored_building, owner_id)) {
                return AnyResponseHelper.unauthorized("Unauthorized");
            }

            // check for values to update
            if (json.findValue("is_published") != null) {
                String is_published = json.path("is_published").textValue();
                if (is_published.equals("true") || is_published.equals("false"))
                    stored_building.put("is_published", json.path("is_published").textValue());
            }
            if (json.findValue("name") != null) {
                stored_building.put("name", json.path("name").textValue());
            }
            if (json.findValue("bucode") != null) {
                stored_building.put("bucode", json.path("bucode").textValue());
            }
            if (json.findValue("description") != null) {
                stored_building.put("description", json.path("description").textValue());
            }
            if (json.findValue("url") != null) {
                stored_building.put("url", json.path("url").textValue());
            }
            if (json.findValue("address") != null) {
                stored_building.put("address", json.path("address").textValue());
            }
            if (json.findValue("coordinates_lat") != null) {
                stored_building.put("coordinates_lat", json.path("coordinates_lat").textValue());
            }
            if (json.findValue("coordinates_lon") != null) {
                stored_building.put("coordinates_lon", json.path("coordinates_lon").textValue());
            }

            AbstractModel building = new Building(stored_building);
            if (!ProxyDataSource.getIDatasource().replaceJsonDocument(building.getId(), 0, building.toCouchGeoJSON())) {
                return AnyResponseHelper.bad_request("Building could not be updated!");
            }

            return AnyResponseHelper.ok("Successfully updated building!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    /**
     * Delete the building specified by buid.
     *
     * @return
     */
    public static Result buildingDelete() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::buildingDelete(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "access_token");
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        // get access token from url and check it against google's service
        if (json.findValue("access_token") == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        String owner_id = verifyOwnerId(json.findValue("access_token").textValue());
        if (owner_id == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        owner_id = appendToOwnerId(owner_id);
        ((ObjectNode) json).put("owner_id", owner_id);

        String buid = json.findPath("buid").textValue();

        try {
            ObjectNode stored_building = (ObjectNode) ProxyDataSource.getIDatasource().getFromKeyAsJson(buid);
            if (stored_building == null) {
                return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!");
            }

            if (!isBuildingOwner(stored_building, owner_id)) {
                return AnyResponseHelper.unauthorized("Unauthorized");
            }

        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }

        try {
            // we need to download the pois/floors/connections and all its connections and delete them all
            List<String> all_items_failed = ProxyDataSource.getIDatasource().deleteAllByBuilding(buid);
            if (all_items_failed.size() > 0) {
                // TODO - THINK WHAT TO DO WHEN DELETION FAILS ON SOME ITEMS

                // TODO - MARKING THEM INSIDE THE DB APPENDING A FLAG IN ORDER A TO MAKE THEM
                // TODO - ELIGIBLE FOR ANOTHER SERVICE THAT RUNS EVERY HOUR AND DELETES EVERYTHING

                ObjectNode obj = JsonUtils.createObjectNode();
                obj.put("ids", JsonUtils.getJsonFromList(all_items_failed));
                return AnyResponseHelper.bad_request(obj, "Some items related to the deleted building could not be deleted: " + all_items_failed.size() + " items.");
            }
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }

        // delete all the floor plans
        String filePath = AnyPlaceTilerHelper.getRootFloorPlansDirFor(buid);
        try {
            File buidfile = new File(filePath);
            if (buidfile.exists())
                HelperMethods.recDeleteDirFile(buidfile);
        } catch (IOException e) {
            // TODO - what to do on failure
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "] while deleting floor plans." +
                    "\nAll related information is deleted from the database!");
        }
        return AnyResponseHelper.ok("Successfully deleted everything related to building!");
    }

    /**
     * Retrieve all the buildings.
     *
     * @return
     */
    public static Result buildingAll() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::buildingAll(): " + json.toString());

        try {
            List<JsonNode> buildings = ProxyDataSource.getIDatasource().getAllBuildings();
            ObjectNode res = JsonUtils.createObjectNode();
            res.put("buildings", JsonUtils.getJsonFromList(buildings));
            try {
//                if (request().getHeader("Accept-Encoding") != null && request().getHeader("Accept-Encoding").contains("gzip")) {
                return gzippedJSONOk(res.toString());
//                }
//                return AnyResponseHelper.ok(res.toString());
            } catch (IOException ioe) {
                return AnyResponseHelper.ok(res, "Successfully retrieved all buildings!");
            }
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    public static Result buildingGetOne() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::buildingGet(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid");
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        String buid = json.findPath("buid").textValue();

        try {
            JsonNode building = ProxyDataSource.getIDatasource().getFromKeyAsJson(buid);

            if (building != null && building.get("buid") != null
                    && building.get("coordinates_lat") != null
                    && building.get("coordinates_lon") != null
                    && building.get("owner_id") != null
                    && building.get("name") != null
                    && building.get("description") != null
                    && building.get("puid") == null
                    && building.get("floor_number") == null
                    ) {

                ((ObjectNode) building).remove("owner_id");
                ((ObjectNode) building).remove("co_owners");

                ObjectNode res = JsonUtils.createObjectNode();
                res.put("building", building);
                try {
//                    if (request().getHeader("Accept-Encoding") != null && request().getHeader("Accept-Encoding").contains("gzip")) {
                    return gzippedJSONOk(res.toString());
//                    }
//                    return AnyResponseHelper.ok(res.toString());
                } catch (IOException ioe) {
                    return AnyResponseHelper.ok(res, "Successfully retrieved all buildings!");
                }
            }

            return AnyResponseHelper.not_found("Building not found.");

        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    public static Result buildingAllByOwner() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::buildingAll(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "access_token"
        );
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        // get access token from url and check it against google's service
        if (json.findValue("access_token") == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        String owner_id = verifyOwnerId(json.findValue("access_token").textValue());
        if (owner_id == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        owner_id = appendToOwnerId(owner_id);
        ((ObjectNode) json).put("owner_id", owner_id);

        if (owner_id == null || owner_id.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        try {
            List<JsonNode> buildings = ProxyDataSource.getIDatasource().getAllBuildingsByOwner(owner_id);
            ObjectNode res = JsonUtils.createObjectNode();
            res.put("buildings", JsonUtils.getJsonFromList(buildings));

            try {
//                if (request().getHeader("Accept-Encoding") != null && request().getHeader("Accept-Encoding").contains("gzip")) {
                return gzippedJSONOk(res.toString());
//                }
//                return AnyResponseHelper.ok(res.toString());
            } catch (IOException ioe) {
                return AnyResponseHelper.ok(res, "Successfully retrieved all buildings!");
            }
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    public static Result buildingByBucode() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::buildingAll(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "bucode"
        );
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        String bucode = json.findValue("bucode").textValue();

        try {
            List<JsonNode> buildings = ProxyDataSource.getIDatasource().getAllBuildingsByBucode(bucode);
            ObjectNode res = JsonUtils.createObjectNode();
            res.put("buildings", JsonUtils.getJsonFromList(buildings));

            try {
//                if (request().getHeader("Accept-Encoding") != null && request().getHeader("Accept-Encoding").contains("gzip")) {
                return gzippedJSONOk(res.toString());
//                }
//                return AnyResponseHelper.ok(res.toString());
            } catch (IOException ioe) {
                return AnyResponseHelper.ok(res, "Successfully retrieved all buildings!");
            }
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    /**
     * Retrieve all the buildings that are inside the bounding box of the specified coordinates.
     *
     * @return
     */
    public static Result buildingCoordinates() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::buildingCoordinates(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "coordinates_lat",
                "coordinates_lon");
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        try {
            List<JsonNode> buildings = ProxyDataSource.getIDatasource().getAllBuildingsNearMe(
                    Double.parseDouble(json.path("coordinates_lat").textValue()),
                    Double.parseDouble(json.path("coordinates_lon").textValue())
            );
            ObjectNode res = JsonUtils.createObjectNode();
            res.put("buildings", JsonUtils.getJsonFromList(buildings));
            try {
//                if (request().getHeader("Accept-Encoding") != null && request().getHeader("Accept-Encoding").contains("gzip")) {
                return gzippedJSONOk(res.toString());
//                }
//                return AnyResponseHelper.ok(res.toString());
            } catch (IOException ioe) {
                return AnyResponseHelper.ok(res, "Successfully retrieved all buildings near your position!");
            }
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    /**
     * Adds a floor to the building denoted by buid
     *
     * @return
     */
    public static Result floorAdd() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::floorAdd(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "is_published",
                "buid",
                "floor_name",
                "description",
                "floor_number",
                "access_token");
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        // get access token from url and check it against google's service
        if (json.findValue("access_token") == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        String owner_id = verifyOwnerId(json.findValue("access_token").textValue());
        if (owner_id == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        owner_id = appendToOwnerId(owner_id);
        ((ObjectNode) json).put("owner_id", owner_id);

        String buid = json.path("buid").textValue();

        try {
            ObjectNode stored_building = (ObjectNode) ProxyDataSource.getIDatasource().getFromKeyAsJson(buid);
            if (stored_building == null) {
                return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!");
            }

            if (!isBuildingOwner(stored_building, owner_id)) {
                return AnyResponseHelper.unauthorized("Unauthorized");
            }

        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }


        String floor_number = json.path("floor_number").textValue();

        if (!Floor.checkFloorNumberFormat(floor_number)) {
            return AnyResponseHelper.bad_request("Floor number cannot contain whitespace!");
        }

        try {
            Floor floor = new Floor(json);
            //System.out.println(floor.toValidCouchJson());
            if (!ProxyDataSource.getIDatasource().addJsonDocument(floor.getId(), 0, floor.toValidCouchJson())) {
                return AnyResponseHelper.bad_request("Floor already exists or could not be added!");
            }
            return AnyResponseHelper.ok("Successfully added floor " + floor_number + "!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    /**
     * Update floor information ( floor name, description, is_published )
     *
     * @return
     */
    public static Result floorUpdate() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::floorUpdate(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "buid"
                , "floor_number"
                , "access_token"
        );
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        // get access token from url and check it against google's service
        if (json.findValue("access_token") == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        String owner_id = verifyOwnerId(json.findValue("access_token").textValue());
        if (owner_id == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        owner_id = appendToOwnerId(owner_id);
        ((ObjectNode) json).put("owner_id", owner_id);


        String buid = json.path("buid").textValue();

        try {
            ObjectNode stored_building = (ObjectNode) ProxyDataSource.getIDatasource().getFromKeyAsJson(buid);
            if (stored_building == null) {
                return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!");
            }

            if (!isBuildingOwner(stored_building, owner_id)) {
                return AnyResponseHelper.unauthorized("Unauthorized");
            }

        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }

        String floor_number = json.path("floor_number").textValue();

        if (!Floor.checkFloorNumberFormat(floor_number)) {
            return AnyResponseHelper.bad_request("Floor number cannot contain whitespace!");
        }

        try {
            String fuid = Floor.getId(buid, floor_number);
            ObjectNode stored_floor = (ObjectNode) ProxyDataSource.getIDatasource().getFromKeyAsJson(fuid);
            if (stored_floor == null) {
                return AnyResponseHelper.bad_request("Floor does not exist or could not be retrieved!");
            }

            // check for values to update
            if (json.findValue("is_published") != null) {
                stored_floor.put("is_published", json.path("is_published").textValue());
            }
            if (json.findValue("floor_name") != null) {
                stored_floor.put("floor_name", json.path("floor_name").textValue());
            }
            if (json.findValue("description") != null) {
                stored_floor.put("description", json.path("description").textValue());
            }

            AbstractModel floor = new Floor(stored_floor);
            if (!ProxyDataSource.getIDatasource().replaceJsonDocument(floor.getId(), 0, floor.toValidCouchJson())) {
                return AnyResponseHelper.bad_request("Floor could not be updated!");
            }

            return AnyResponseHelper.ok("Successfully updated floor!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    /**
     * Delete the floor specified by buid and floor_number.
     *
     * @return
     */
    public static Result floorDelete() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::floorDelete(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "buid",
                "floor_number",
                "access_token"
        );

        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        // get access token from url and check it against google's service
        if (json.findValue("access_token") == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        String owner_id = verifyOwnerId(json.findValue("access_token").textValue());
        if (owner_id == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        owner_id = appendToOwnerId(owner_id);
        ((ObjectNode) json).put("owner_id", owner_id);

        String buid = json.findPath("buid").textValue();
        String floor_number = json.findPath("floor_number").textValue();

        try {
            ObjectNode stored_building = (ObjectNode) ProxyDataSource.getIDatasource().getFromKeyAsJson(buid);
            if (stored_building == null) {
                return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!");
            }

            if (!isBuildingOwner(stored_building, owner_id)) {
                return AnyResponseHelper.unauthorized("Unauthorized");
            }

        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }

        try {
            // we need to download the pois/connections and all its connections and delete them all
            List<String> all_items_failed = ProxyDataSource.getIDatasource().deleteAllByFloor(buid, floor_number);
            if (all_items_failed.size() > 0) {
                // TODO - THINK WHAT TO DO WHEN DELETION FAILS ON SOME ITEMS

                // TODO - MARKING THEM INSIDE THE DB APPENDING A FLAG IN ORDER A TO MAKE THEM
                // TODO - ELIGIBLE FOR ANOTHER SERVICE THAT RUNS EVERY HOUR AND DELETES EVERYTHING

                ObjectNode obj = JsonUtils.createObjectNode();
                obj.put("ids", JsonUtils.getJsonFromList(all_items_failed));
                return AnyResponseHelper.bad_request(obj, "Some items related to the deleted floor could not be deleted: " + all_items_failed.size() + " items.");
            }
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }

        // delete the floor plan
        String filePath = AnyPlaceTilerHelper.getFloorPlanFor(buid, floor_number);
        try {
            File floorfile = new File(filePath);
            if (floorfile.exists())
                HelperMethods.recDeleteDirFile(floorfile);
        } catch (IOException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "] while deleting floor plan." +
                    "\nAll related information is deleted from the database!");
        }
        return AnyResponseHelper.ok("Successfully deleted everything related to the floor!");
    }

    /**
     * Retrieve all the floors of a building.
     *
     * @return
     */
    public static Result floorAll() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::floorAll(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "buid"
        );
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        String buid = json.findPath("buid").textValue();

        try {
            List<JsonNode> buildings = ProxyDataSource.getIDatasource().floorsByBuildingAsJson(buid);
            ObjectNode res = JsonUtils.createObjectNode();
            res.put("floors", JsonUtils.getJsonFromList(buildings));
            try {
//                if (request().getHeader("Accept-Encoding") != null && request().getHeader("Accept-Encoding").contains("gzip")) {
                return gzippedJSONOk(res.toString());
//                }
//                return AnyResponseHelper.ok(res.toString());
            } catch (IOException ioe) {
                return AnyResponseHelper.ok(res, "Successfully retrieved all floors!");
            }
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    /**
     * Inserts a new Point of Interest at the coordinates passed in
     *
     * @return the newly created POIS ID is included in the response if success
     */
    public static Result poisAdd() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::poisAdd(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "is_published",
                "buid",
                "floor_name",
                "floor_number",
                "name",
                "pois_type",
                "is_door",
                "is_building_entrance",
                "coordinates_lat",
                "coordinates_lon",
                "access_token"
        );
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }
        // get access token from url and check it against google's service
        if (json.findValue("access_token") == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        String owner_id = verifyOwnerId(json.findValue("access_token").textValue());
        if (owner_id == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        owner_id = appendToOwnerId(owner_id);
        ((ObjectNode) json).put("owner_id", owner_id);

        String buid = json.path("buid").textValue();

        try {
            ObjectNode stored_building = (ObjectNode) ProxyDataSource.getIDatasource().getFromKeyAsJson(buid);
            if (stored_building == null) {
                return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!");
            }

            if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id)) {
                return AnyResponseHelper.unauthorized("Unauthorized");
            }

        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }


        try {
            Poi poi = new Poi(json);
            //System.out.println(poi.toValidCouchJson());
            if (!ProxyDataSource.getIDatasource().addJsonDocument(poi.getId(), 0, poi.toCouchGeoJSON())) {
                return AnyResponseHelper.bad_request("Poi already exists or could not be added!");
            }
            ObjectNode res = JsonUtils.createObjectNode();
            res.put("puid", poi.getId());
            return AnyResponseHelper.ok(res, "Successfully added poi!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    /**
     * Update the Point of Interest - specified by PUID - information
     *
     * @return
     */
    public static Result poisUpdate() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::poisUpdate(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "puid"
                , "buid"
                , "access_token"
        );
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }
        // get access token from url and check it against google's service
        if (json.findValue("access_token") == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        String owner_id = verifyOwnerId(json.findValue("access_token").textValue());
        if (owner_id == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        owner_id = appendToOwnerId(owner_id);
        ((ObjectNode) json).put("owner_id", owner_id);

        String puid = json.path("puid").textValue();
        String buid = json.path("buid").textValue();

        try {
            ObjectNode stored_building = (ObjectNode) ProxyDataSource.getIDatasource().getFromKeyAsJson(buid);
            if (stored_building == null) {
                return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!");
            }

            if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id)) {
                return AnyResponseHelper.unauthorized("Unauthorized");
            }

        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }


        try {
            ObjectNode stored_poi = (ObjectNode) ProxyDataSource.getIDatasource().getFromKeyAsJson(puid);
            if (stored_poi == null) {
                return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!");
            }

            // check for values to update
            if (json.findValue("is_published") != null) {
                String is_published = json.path("is_published").textValue();
                if (is_published.equals("true") || is_published.equals("false"))
                    stored_poi.put("is_published", json.path("is_published").textValue());
            }
            if (json.findValue("name") != null) {
                stored_poi.put("name", json.path("name").textValue());
            }
            if (json.findValue("description") != null) {
                stored_poi.put("description", json.path("description").textValue());
            }
            if (json.findValue("url") != null) {
                stored_poi.put("url", json.path("url").textValue());
            }
            if (json.findValue("pois_type") != null) {
                stored_poi.put("pois_type", json.path("pois_type").textValue());
            }
            if (json.findValue("is_door") != null) {
                String is_door = json.path("is_door").textValue();
                if (is_door.equals("true") || is_door.equals("false"))
                    stored_poi.put("is_door", json.path("is_door").textValue());
            }
            if (json.findValue("is_building_entrance") != null) {
                String is_building_entrance = json.path("is_building_entrance").textValue();
                if (is_building_entrance.equals("true") || is_building_entrance.equals("false"))
                    stored_poi.put("is_building_entrance", json.path("is_building_entrance").textValue());
            }
            if (json.findValue("image") != null) {
                stored_poi.put("image", json.path("image").textValue());
            }

            if (json.findValue("coordinates_lat") != null) {
                stored_poi.put("coordinates_lat", json.path("coordinates_lat").textValue());
            }

            if (json.findValue("coordinates_lon") != null) {
                stored_poi.put("coordinates_lon", json.path("coordinates_lon").textValue());
            }

            AbstractModel poi = new Poi(stored_poi);
            if (!ProxyDataSource.getIDatasource().replaceJsonDocument(poi.getId(), 0, poi.toCouchGeoJSON())) {
                return AnyResponseHelper.bad_request("Poi could not be updated!");
            }

            return AnyResponseHelper.ok("Successfully updated poi!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    /**
     * Delete the Point of Interest denoted by the requested POIS id
     *
     * @return
     */
    public static Result poisDelete() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::poiDelete(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json, "puid", "buid", "access_token");
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }
        // get access token from url and check it against google's service
        if (json.findValue("access_token") == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        String owner_id = verifyOwnerId(json.findValue("access_token").textValue());
        if (owner_id == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        owner_id = appendToOwnerId(owner_id);
        ((ObjectNode) json).put("owner_id", owner_id);

        String buid = json.findPath("buid").textValue();
        String puid = json.findPath("puid").textValue();

        try {
            ObjectNode stored_building = (ObjectNode) ProxyDataSource.getIDatasource().getFromKeyAsJson(buid);
            if (stored_building == null) {
                return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!");
            }

            if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id)) {
                return AnyResponseHelper.unauthorized("Unauthorized");
            }

        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }

        try {
            // we need to download the pois/floors/connections and all its connections and delete them all
            List<String> all_items_failed = ProxyDataSource.getIDatasource().deleteAllByPoi(puid);
            if (all_items_failed.size() > 0) {
                // TODO - THINK WHAT TO DO WHEN DELETION FAILS ON SOME ITEMS

                // TODO - MARKING THEM INSIDE THE DB APPENDING A FLAG IN ORDER A TO MAKE THEM
                // TODO - ELIGIBLE FOR ANOTHER SERVICE THAT RUNS EVERY HOUR AND DELETES EVERYTHING

                ObjectNode obj = JsonUtils.createObjectNode();
                obj.put("ids", JsonUtils.getJsonFromList(all_items_failed));
                return AnyResponseHelper.bad_request(obj, "Some items related to the deleted poi could not be deleted: " + all_items_failed.size() + " items.");
            }
            return AnyResponseHelper.ok("Successfully deleted everything related to the poi!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    /**
     * Retrieve all the pois of a building/floor combination.
     *
     * @return
     */
    public static Result poisByFloor() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::poisByFloor(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "buid",
                "floor_number"
        );
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        String buid = json.findPath("buid").textValue();
        String floor_number = json.findPath("floor_number").textValue();
        try {
            List<JsonNode> pois = ProxyDataSource.getIDatasource().poisByBuildingFloorAsJson(buid, floor_number);
            ObjectNode res = JsonUtils.createObjectNode();
            res.put("pois", JsonUtils.getJsonFromList(pois));
            try {
//                if (request().getHeader("Accept-Encoding") != null && request().getHeader("Accept-Encoding").contains("gzip")) {
                return gzippedJSONOk(res.toString());
//                }
//                return AnyResponseHelper.ok(res.toString());
            } catch (IOException ioe) {
                return AnyResponseHelper.ok(res, "Successfully retrieved all pois from floor " + floor_number + "!");
            }
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    /**
     * Retrieve all the pois of a building/floor combination.
     *
     * @return
     */
    public static Result poisByBuid() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::poisByBuid(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "buid"
        );
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        String buid = json.findPath("buid").textValue();
        try {
            List<JsonNode> pois = ProxyDataSource.getIDatasource().poisByBuildingAsJson(buid);
            ObjectNode res = JsonUtils.createObjectNode();
            res.put("pois", JsonUtils.getJsonFromList(pois));
            try {
//                if (request().getHeader("Accept-Encoding") != null && request().getHeader("Accept-Encoding").contains("gzip")) {
                return gzippedJSONOk(res.toString());
//                }
//                return AnyResponseHelper.ok(res.toString());
            } catch (IOException ioe) {
                return AnyResponseHelper.ok(res, "Successfully retrieved all pois from building.");
            }
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    /**
     * Adds a new connection between two valid Points of Interest - denoted by the pois_a and pois_b -
     *
     * @return
     */
    public static Result connectionAdd() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::connectionAdd(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "is_published",
                "pois_a",
                "floor_a",
                "buid_a",
                "pois_b",
                "floor_b",
                "buid_b",
                "buid",
                "edge_type",
                "access_token"
        );
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }
        // get access token from url and check it against google's service
        if (json.findValue("access_token") == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        String owner_id = verifyOwnerId(json.findValue("access_token").textValue());
        if (owner_id == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        owner_id = appendToOwnerId(owner_id);
        ((ObjectNode) json).put("owner_id", owner_id);

        String buid1 = json.path("buid_a").textValue();
        String buid2 = json.path("buid_b").textValue();

        try {
            ObjectNode stored_building = (ObjectNode) ProxyDataSource.getIDatasource().getFromKeyAsJson(buid1);
            if (stored_building == null) {
                return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!");
            }

            if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id)) {
                return AnyResponseHelper.unauthorized("Unauthorized");
            }

            stored_building = (ObjectNode) ProxyDataSource.getIDatasource().getFromKeyAsJson(buid2);
            if (stored_building == null) {
                return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!");
            }

            if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id)) {
                return AnyResponseHelper.unauthorized("Unauthorized");
            }

        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }


        String edge_type = json.path("edge_type").textValue();
        if (!edge_type.equals(Connection.EDGE_TYPE_ELEVATOR) && !edge_type.equals(Connection.EDGE_TYPE_HALLWAY)
                && !edge_type.equals(Connection.EDGE_TYPE_ROOM) && !edge_type.equals(Connection.EDGE_TYPE_OUTDOOR)
                && !edge_type.equals(Connection.EDGE_TYPE_STAIR)) {
            return AnyResponseHelper.bad_request("Invalid edge type specified.");
        }

        String pois_a = json.path("pois_a").textValue();
        String pois_b = json.path("pois_b").textValue();

        try {
            // Calculate the weight of the connection
            double weight = calculateWeightOfConnection(pois_a, pois_b);
            ((ObjectNode) json).put("weight", Double.toString(weight));
            if (edge_type.equals(Connection.EDGE_TYPE_ELEVATOR) || edge_type.equals(Connection.EDGE_TYPE_STAIR)) {
                // TODO - maybe we must put weight zero
                // TODO - or we must calculate the paths in each floor separately
            }

            Connection conn = new Connection(json);

            if (!ProxyDataSource.getIDatasource().addJsonDocument(conn.getId(), 0, conn.toValidCouchJson())) {
                return AnyResponseHelper.bad_request("Connection already exists or could not be added!");
            }
            ObjectNode res = JsonUtils.createObjectNode();
            res.put("cuid", conn.getId());
            return AnyResponseHelper.ok(res, "Successfully added new connection!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    /**
     * Updates the edge_type of the connection between two Points of Interest - specified by pois_a and pois_b -
     *
     * @return
     */
    public static Result connectionUpdate() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::connectionUpdate(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "pois_a",
                "pois_b",
                "buid_a",
                "buid_b",
                "access_token"
        );
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }
        // get access token from url and check it against google's service
        if (json.findValue("access_token") == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        String owner_id = verifyOwnerId(json.findValue("access_token").textValue());
        if (owner_id == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        owner_id = appendToOwnerId(owner_id);
        ((ObjectNode) json).put("owner_id", owner_id);

        String buid1 = json.path("buid_a").textValue();
        String buid2 = json.path("buid_b").textValue();

        try {
            ObjectNode stored_building = (ObjectNode) ProxyDataSource.getIDatasource().getFromKeyAsJson(buid1);
            if (stored_building == null) {
                return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!");
            }

            if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id)) {
                return AnyResponseHelper.unauthorized("Unauthorized");
            }

            stored_building = (ObjectNode) ProxyDataSource.getIDatasource().getFromKeyAsJson(buid2);
            if (stored_building == null) {
                return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!");
            }

            if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id)) {
                return AnyResponseHelper.unauthorized("Unauthorized");
            }

        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }

        try {
            String pois_a = json.path("pois_a").textValue();
            String pois_b = json.path("pois_b").textValue();

            String cuid = Connection.getId(pois_a, pois_b);
            ObjectNode stored_conn = (ObjectNode) ProxyDataSource.getIDatasource().getFromKeyAsJson(cuid);
            if (stored_conn == null) {
                return AnyResponseHelper.bad_request("Connection does not exist or could not be retrieved!");
            }

            // check for values to update
            if (json.findValue("is_published") != null) {
                String is_published = json.path("is_published").textValue();
                if (is_published.equals("true") || is_published.equals("false"))
                    stored_conn.put("is_published", json.path("is_published").textValue());
            }
            if (json.findValue("edge_type") != null) {
                String edge_type = json.path("edge_type").textValue();
                if (!edge_type.equals(Connection.EDGE_TYPE_ELEVATOR) && !edge_type.equals(Connection.EDGE_TYPE_HALLWAY)
                        && !edge_type.equals(Connection.EDGE_TYPE_ROOM) && !edge_type.equals(Connection.EDGE_TYPE_OUTDOOR)
                        && !edge_type.equals(Connection.EDGE_TYPE_STAIR)) {
                    return AnyResponseHelper.bad_request("Invalid edge type specified.");
                }
                stored_conn.put("edge_type", edge_type);
            }

            Connection conn = new Connection(stored_conn);
            if (!ProxyDataSource.getIDatasource().replaceJsonDocument(conn.getId(), 0, conn.toValidCouchJson())) {
                return AnyResponseHelper.bad_request("Connection could not be updated!");
            }

            return AnyResponseHelper.ok("Successfully updated connection!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    /**
     * Deletes the connection between two Points of Interest - denoted by pois_a and pois_b -
     *
     * @return
     */
    public static Result connectionDelete() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::poiDelete(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "pois_a",
                "pois_b",
                "buid_a",
                "buid_b",
                "access_token"
        );
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }
        // get access token from url and check it against google's service
        if (json.findValue("access_token") == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        String owner_id = verifyOwnerId(json.findValue("access_token").textValue());
        if (owner_id == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        owner_id = appendToOwnerId(owner_id);
        ((ObjectNode) json).put("owner_id", owner_id);


        String buid1 = json.path("buid_a").textValue();
        String buid2 = json.path("buid_b").textValue();

        try {
            ObjectNode stored_building = (ObjectNode) ProxyDataSource.getIDatasource().getFromKeyAsJson(buid1);
            if (stored_building == null) {
                return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!");
            }
            if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id)) {
                return AnyResponseHelper.unauthorized("Unauthorized");
            }

            stored_building = (ObjectNode) ProxyDataSource.getIDatasource().getFromKeyAsJson(buid2);
            if (stored_building == null) {
                return AnyResponseHelper.bad_request("Building does not exist or could not be retrieved!");
            }
            if (!isBuildingOwner(stored_building, owner_id) && !isBuildingCoOwner(stored_building, owner_id)) {
                return AnyResponseHelper.unauthorized("Unauthorized");
            }

        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }


        String pois_a = json.path("pois_a").textValue();
        String pois_b = json.path("pois_b").textValue();
        try {
            String cuid = Connection.getId(pois_a, pois_b);
            // we need to download the pois/floors/connections and all its connections and delete them all
            List<String> all_items_failed = ProxyDataSource.getIDatasource().deleteAllByConnection(cuid);

            if (all_items_failed == null) {
                LPLogger.info("AnyplaceMapping::connectionDelete(): " + cuid + " not found.");
                return AnyResponseHelper.bad_request("POI Connection not found");
            }

            if (all_items_failed.size() > 0) {
                // TODO - THINK WHAT TO DO WHEN DELETION FAILS ON SOME ITEMS

                // TODO - MARKING THEM INSIDE THE DB APPENDING A FLAG IN ORDER A TO MAKE THEM
                // TODO - ELIGIBLE FOR ANOTHER SERVICE THAT RUNS EVERY HOUR AND DELETES EVERYTHING

                ObjectNode obj = JsonUtils.createObjectNode();
                obj.put("ids", JsonUtils.getJsonFromList(all_items_failed));
                return AnyResponseHelper.bad_request(obj, "Some items related to the deleted connection could not be deleted: " + all_items_failed.size() + " items.");
            }
            return AnyResponseHelper.ok("Successfully deleted everything related to the connection!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    /**
     * Retrieve all the pois of a building/floor combination.
     *
     * @return
     */
    public static Result connectionsByFloor() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::poisByFloor(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "buid",
                "floor_number"
        );
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        String buid = json.findPath("buid").textValue();
        String floor_number = json.findPath("floor_number").textValue();
        try {
            List<JsonNode> pois = ProxyDataSource.getIDatasource().connectionsByBuildingFloorAsJson(buid, floor_number);
            ObjectNode res = JsonUtils.createObjectNode();
            res.put("connections", JsonUtils.getJsonFromList(pois));
            try {
//                if (request().getHeader("Accept-Encoding") != null && request().getHeader("Accept-Encoding").contains("gzip")) {
                return gzippedJSONOk(res.toString());
//                }
//                return AnyResponseHelper.ok(res.toString());
            } catch (IOException ioe) {
                return AnyResponseHelper.ok(res, "Successfully retrieved all pois from floor " + floor_number + "!");
            }
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    private static double calculateWeightOfConnection(String pois_a, String pois_b) throws DatasourceException {

        double lat_a = 0, lon_a = 0, lat_b = 0, lon_b = 0;

        NumberFormat nf = NumberFormat.getInstance(Locale.ENGLISH);

        JsonNode pa = ProxyDataSource.getIDatasource().getFromKeyAsJson(pois_a);
        if (pa == null) {
            lat_a = 0.0;
            lon_a = 0.0;
        } else {
            // everything is ok
            try {
                lat_a = nf.parse(pa.path("coordinates_lat").textValue()).doubleValue();
                lon_a = nf.parse(pa.path("coordinates_lon").textValue()).doubleValue();
            } catch (ParseException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        JsonNode pb = ProxyDataSource.getIDatasource().getFromKeyAsJson(pois_b);
        if (pb == null) {
            lat_b = 0.0;
            lon_b = 0.0;
        } else {
            // everything is ok
            try {
                lat_b = nf.parse(pb.path("coordinates_lat").textValue()).doubleValue();
                lon_b = nf.parse(pb.path("coordinates_lon").textValue()).doubleValue();
            } catch (ParseException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }

        return GeoPoint.getDistanceBetweenPoints(lat_a, lon_a, lat_b, lon_b, "K");
    }

    /**
     * Returns the floor plan (png) for the requested building floor in binary format.
     * Used by the Android client  - NOT ANYMORE
     *
     * @param buid         Building id
     * @param floor_number floor number inside the above building
     * @return the file or an error
     */
    public static Result serveFloorPlanBinary(String buid, String floor_number) {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::serveFloorPlan(): " + json.toString());

        String filePath = AnyPlaceTilerHelper.getFloorPlanFor(buid, floor_number);
        LPLogger.info("requested: " + filePath);

        try {
            File file = new java.io.File(filePath);
            if (!file.exists() || !file.canRead()) {
                return AnyResponseHelper.bad_request("Requested floor plan does not exist or cannot be read! (" + floor_number + ")");
            }
            InputStream a = new FileInputStream(file);
            return ok(a);
        } catch (FileNotFoundException e) {
            // cannot get in here i think cause of the check if canRead()
            return AnyResponseHelper.internal_server_error("Could not read floor plan.");
        }
    }

    /**
     * Returns the floor plan tiles in a .zip file for the requested building floor.
     * Used by the Android client.
     *
     * @param buid         Building id
     * @param floor_number floor number inside the above building
     * @return the file or an error
     */
    public static Result serveFloorPlanTilesZip(String buid, String floor_number) {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::serveFloorPlanTilesZip(): " + json.toString());

        if (!Floor.checkFloorNumberFormat(floor_number)) {
            return AnyResponseHelper.bad_request("Floor number cannot contain whitespace!");
        }

        String filePath = AnyPlaceTilerHelper.getFloorTilesZipFor(buid, floor_number);
        LPLogger.info("requested: " + filePath);

        try {
            File file = new java.io.File(filePath);
            if (!file.exists() || !file.canRead()) {
                return AnyResponseHelper.bad_request("Requested floor plan does not exist or cannot be read! (" + floor_number + ")");
            }
            InputStream a = new FileInputStream(file);
            return ok(a);
        } catch (FileNotFoundException e) {
            // cannot get in here i think cause of the check if canRead()
            return AnyResponseHelper.internal_server_error("Could not read floor plan.");
        }
    }

    /**
     * Returns the floor plan tiles .zip file LINK for the requested building floor.
     * Used by the Developers API.
     *
     * @param buid         Building id
     * @param floor_number floor number inside the above building
     * @return the file or an error
     */
    public static Result serveFloorPlanTilesZipLink(String buid, String floor_number) {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::serveFloorPlanTilesZipLink(): " + json.toString());

        if (!Floor.checkFloorNumberFormat(floor_number)) {
            return AnyResponseHelper.bad_request("Floor number cannot contain whitespace!");
        }
        String filePath = AnyPlaceTilerHelper.getFloorTilesZipFor(buid, floor_number);

        LPLogger.info("requested: " + filePath);

        File file = new java.io.File(filePath);

        if (!file.exists() || !file.canRead()) {
            return AnyResponseHelper.bad_request("Requested floor plan does not exist or cannot be read! (" + floor_number + ")");
        }

        ObjectNode res = JsonUtils.createObjectNode();
        res.put("tiles_archive", AnyPlaceTilerHelper.getFloorTilesZipLinkFor(buid, floor_number));

        return AnyResponseHelper.ok(res, "Successfully fetched link for the tiles archive!");
    }

    /**
     * Returns the file requested from inside the floor plan folder
     * for the requested building floor.
     * Used by web-clients.
     *
     * @param path The file to download
     * @return the file or an error
     */
    public static Result serveFloorPlanTilesStatic(String buid, String floor_number, String path) {
//        OAuth2Request anyReq = new OAuth2Request(request(), response());
//        if( !anyReq.assertJsonBody() ){
//            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
//        }
//        JsonNode json = anyReq.getJsonBody();
//        LPLogger.info("AnyplaceMapping::serveFloorPlanTilesStatic(): " + json.toString());

        LPLogger.info("AnyplaceMapping::serveFloorPlanTilesStatic(): " + buid + ":" + floor_number + ":" + path);

        if (path == null || buid == null || floor_number == null
                || path.trim().isEmpty() || buid.trim().isEmpty() || floor_number.trim().isEmpty()) {
            return notFound();
        }

        String filePath;
        if (path.equals(AnyPlaceTilerHelper.FLOOR_TILES_ZIP_NAME)) {
            filePath = AnyPlaceTilerHelper.getFloorTilesZipFor(buid, floor_number);
        } else {
            filePath = AnyPlaceTilerHelper.getFloorTilesDirFor(buid, floor_number) + path;
        }

        LPLogger.info("static requested: " + filePath);

        try {
            File file = new java.io.File(filePath);
            if (!file.exists() || !file.canRead()) {
                return AnyResponseHelper.not_found("File requested not found");
            }
            InputStream a = new FileInputStream(file);
            return ok(a);
        } catch (FileNotFoundException e) {
            // cannot get in here i think cause of the check if canRead()
            return AnyResponseHelper.internal_server_error("Could not read floor plan.");
        }
    }

    /**
     * Returns the floorplan in base64 form. Used by the Anyplace websites
     *
     * @param buid
     * @param floor_number
     * @return
     */
    public static Result serveFloorPlanBase64(String buid, String floor_number) {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::serveFloorPlanBase64(): " + json.toString());

        String filePath = AnyPlaceTilerHelper.getFloorPlanFor(buid, floor_number);
        LPLogger.info("requested: " + filePath);

        File file = new java.io.File(filePath);
        try {
            if (!file.exists() || !file.canRead()) {
                return AnyResponseHelper.bad_request("Requested floor plan does not exist or cannot be read! (" + floor_number + ")");
            }
            try {
                String s = encodeFileToBase64Binary(filePath);
                try {
//                    if (request().getHeader("Accept-Encoding") != null && request().getHeader("Accept-Encoding").contains("gzip")) {
                    return gzippedOk(s);
//                    }
//                    return AnyResponseHelper.ok(s);
                } catch (IOException ioe) {
                    return ok(s);
                }
            } catch (IOException e) {
                return AnyResponseHelper.bad_request("Requested floor plan cannot be encoded in base64 properly! (" + floor_number + ")");
            }
        } catch (Exception e) {
            // cannot get in here i think cause of the check if canRead()
            return AnyResponseHelper.internal_server_error("Unknown server error during floor plan delivery!");
        }
    }

    private static String encodeFileToBase64Binary(String fileName)
            throws IOException {

        File file = new File(fileName);
        byte[] bytes = loadFile(file);
        byte[] encoded = Base64.encodeBase64(bytes);
        String encodedString = new String(encoded);

        return encodedString;
    }

    private static byte[] loadFile(File file) throws IOException, FileNotFoundException {
        InputStream is = new FileInputStream(file);

        long length = file.length();
        if (length > Integer.MAX_VALUE) {
            // File is too large
        }
        byte[] bytes = new byte[(int) length];

        int offset = 0;
        int numRead = 0;
        while (offset < bytes.length
                && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
            offset += numRead;
        }

        if (offset < bytes.length) {
            throw new IOException("Could not completely read file " + file.getName());
        }

        is.close();
        return bytes;
    }

    /**
     * Uploads a floor plan file and stores it at the server system.
     * If another floorplan exists for this floor we overwrite it.
     * <p>
     * Needs a field:
     * floorplan: the floor plan file
     * json: The json document
     *
     * @return
     */
    public static Result floorPlanUpload() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());

        Http.MultipartFormData body = anyReq.getMultipartFormData();
        if (body == null) {
            return AnyResponseHelper.bad_request("Invalid request type - Not Multipart!");
        }

        Http.MultipartFormData.FilePart floorplan;
        floorplan = body.getFile("floorplan");
        if (floorplan == null) {
            return AnyResponseHelper.bad_request("Cannot find the floor plan file in your request!");
        }

        Map<String, String[]> urlenc = body.asFormUrlEncoded();
        String json_str = urlenc.get("json")[0];
        //System.out.println("json: " + json_str);

        if (json_str == null) {
            return AnyResponseHelper.bad_request("Cannot find json in the request!");
        }

        JsonNode json = null;
        try {
            json = JsonUtils.getJsonTree(json_str);
        } catch (IOException e) {
            return AnyResponseHelper.bad_request("Cannot parse json in the request!");
        }
        LPLogger.info("Floorplan Request[json]: " + json.toString());
        LPLogger.info("Floorplan Request[floorplan]: " + floorplan.getFile().getAbsolutePath());


        //// Request has the required parts so now process the json data
        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "buid",
                "floor_number",
                "bottom_left_lat",
                "bottom_left_lng",
                "top_right_lat",
                "top_right_lng"
        );
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        String buid = json.path("buid").textValue();
        String floor_number = json.path("floor_number").textValue();

        String bottom_left_lat = json.path("bottom_left_lat").textValue();
        String bottom_left_lng = json.path("bottom_left_lng").textValue();
        String top_right_lat = json.path("top_right_lat").textValue();
        String top_right_lng = json.path("top_right_lng").textValue();

        String fuid = Floor.getId(buid, floor_number);
        try {
            ObjectNode stored_floor = (ObjectNode) ProxyDataSource.getIDatasource().getFromKeyAsJson(fuid);
            if (stored_floor == null) {
                return AnyResponseHelper.bad_request("Floor does not exist or could not be retrieved!");
            }

            // update the Floor document in couchbase to include the floor plan's coordinates
            stored_floor.put("bottom_left_lat", bottom_left_lat);
            stored_floor.put("bottom_left_lng", bottom_left_lng);
            stored_floor.put("top_right_lat", top_right_lat);
            stored_floor.put("top_right_lng", top_right_lng);

            if (!ProxyDataSource.getIDatasource().replaceJsonDocument(fuid, 0, stored_floor.toString())) {
                return AnyResponseHelper.bad_request("Floor plan could not be updated in the database!");
            }
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Error while reading from our backend service!");
        }

        /////////////////////////////////////////////////////////////////////////////////////////////
        // store the new floor plan on the server
        File floor_file;
        try {
            floor_file = AnyPlaceTilerHelper.storeFloorPlanToServer(buid, floor_number, floorplan.getFile());
        } catch (AnyPlaceException e) {
            // TODO - I should put the old couchbase object in the database
            return AnyResponseHelper.bad_request("Cannot save floor plan on the server!");
        }

        /////////////////////////////////////////////////////////////////////////////////////////////
        // Now we should start the tiling process in order to create the tiles for the floor plan
        String top_left_lat = top_right_lat;
        String top_left_lng = bottom_left_lng;
        try {
            AnyPlaceTilerHelper.tileImage(floor_file, top_left_lat, top_left_lng);
        } catch (AnyPlaceException e) {
            // TODO - I should put the old couchbase object in the database
            return AnyResponseHelper.bad_request("Could not create floor plan tiles on the server!");
        }

        LPLogger.info("Successfully tiled [" + floor_file.toString() + "]");
        return AnyResponseHelper.ok("Successfully updated floor plan!");
    }

    public static Result addAccount() {
        // create the Request and check it
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceAccounts::addAccount():: ");

        // check if there is any required parameter missing
        List<String> notFound = JsonUtils.requirePropertiesInJson(json,
                "access_token", "type"
        );
        if (!notFound.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(notFound);
        }

        // get access token from url and check it against google's service
        if (json.findValue("access_token") == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        String owner_id = verifyOwnerId(json.findValue("access_token").textValue());
        if (owner_id == null) {
            return AnyResponseHelper.forbidden("Unauthorized");
        }
        owner_id = appendToOwnerId(owner_id);
        ((ObjectNode) json).put("owner_id", owner_id);

        Account newAccount = new Account(json);
        try {
            if (!ProxyDataSource.getIDatasource().addJsonDocument(newAccount.getId(), 0, newAccount.toValidCouchJson())) {
                return AnyResponseHelper.ok("Returning user.");
            }
            ObjectNode res = JsonUtils.createObjectNode();
            return AnyResponseHelper.ok("New user.");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    private static boolean isBuildingOwner(ObjectNode building, String userId) {

        // Check if owner
        if (building != null && building.get("owner_id") != null && building.get("owner_id").textValue().equals(userId)) {
            return true;
        }

        return false;
    }

    private static boolean isBuildingCoOwner(ObjectNode building, String userId) {

        JsonNode cws = null;

        if (building != null && (cws = building.get("co_owners")) != null) {
            Iterator<JsonNode> it = cws.elements();
            while (it.hasNext()) {
                if (it.next().textValue().equals(userId)) {
                    return true;
                }
            }
        }

        return false;
    }

    // TODO: Move to util class

    /**
     * Should check if Request Headers accept gzip encoding
     * <p>
     * Creates a response with a gzipped string. Changes the content-type.
     */
    private static Results.Status gzippedJSONOk(final String body) throws IOException {
        final ByteArrayOutputStream gzip = gzip(body);
        response().setHeader("Content-Encoding", "gzip");
        response().setHeader("Content-Length", gzip.size() + "");
        response().setHeader("Content-Type", "application/json");
        return ok(gzip.toByteArray());
    }

    private static Results.Status gzippedOk(final String body) throws IOException {
        final ByteArrayOutputStream gzip = gzip(body);
        response().setHeader("Content-Encoding", "gzip");
        response().setHeader("Content-Length", gzip.size() + "");
        return ok(gzip.toByteArray());
    }

    //solution from James Ward for Play 1 and every request: https://gist.github.com/1317626
    private static ByteArrayOutputStream gzip(final String input)
            throws IOException {
        final InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        final ByteArrayOutputStream stringOutputStream = new ByteArrayOutputStream((int) (input.length() * 0.75));
        final OutputStream gzipOutputStream = new GZIPOutputStream(stringOutputStream);

        final byte[] buf = new byte[5000];
        int len;
        while ((len = inputStream.read(buf)) > 0) {
            gzipOutputStream.write(buf, 0, len);
        }

        inputStream.close();
        gzipOutputStream.close();

        return stringOutputStream;
    }

}
