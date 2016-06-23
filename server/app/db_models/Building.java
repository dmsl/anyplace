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

package db_models;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import utils.GeoJSONPoint;
import utils.JsonUtils;
import utils.LPUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

public class Building extends AbstractModel {

    private JsonNode json;
    private double lat;
    private double lng;

    // Add the following admin accounts to the buildings
    private String[] admins = { /* */ };

    public Building(HashMap<String, String> hm) {
        this.fields = hm;
    }

    public Building() {
        fields.put("username_creator", "");
        fields.put("buid", "");
        fields.put("is_published", "");
        fields.put("name", "");
        fields.put("description", "");
        fields.put("url", "");
        fields.put("address", "");

        fields.put("coordinates_lat", "");
        fields.put("coordinates_lon", "");

        fields.put("bucode", "");
    }

    public Building(JsonNode json) {
        fields.put("username_creator", json.path("username_creator").textValue());

        // The id and the type of the owner that created the building
        fields.put("owner_id", json.path("owner_id").textValue());

        fields.put("buid", json.path("buid").textValue());
        fields.put("is_published", json.path("is_published").textValue());
        fields.put("name", json.path("name").textValue());
        fields.put("description", json.path("description").textValue());
        fields.put("url", json.path("url").textValue());
        fields.put("address", json.path("address").textValue());

        fields.put("coordinates_lat", json.path("coordinates_lat").textValue());
        fields.put("coordinates_lon", json.path("coordinates_lon").textValue());

        fields.put("bucode", json.path("bucode").textValue());

        this.json = json;
        this.lat = Double.parseDouble(json.path("coordinates_lat").textValue());
        this.lng = Double.parseDouble(json.path("coordinates_lon").textValue());
    }

    public Building(JsonNode json, String owner) {
        this(json);
        fields.put("owner_id", owner);
    }


    public String getId() {
        String buid;
        if ((buid = fields.get("buid")) == null || buid.equals("")) {
            String finalId = LPUtils.getRandomUUID() + "_" + System.currentTimeMillis();
            fields.put("buid", "building_" + finalId);
            buid = fields.get("buid");

            ((ObjectNode) this.json).put("buid", buid);
        }
        return buid;
    }

    public String toValidCouchJson() {
        getId(); // initialize id if not initialized

        Gson gson = new Gson();
        return gson.toJson(this.getFields());
    }

    public String toCouchGeoJSON() {
        StringBuilder sb = new StringBuilder();

        ObjectNode json = null;
        try {

            json = (ObjectNode) JsonUtils.getJsonTree(toValidCouchJson());
            json.put("geometry", new GeoJSONPoint(Double.parseDouble(fields.get("coordinates_lat")),
                    Double.parseDouble(fields.get("coordinates_lon"))).toGeoJSON());

            if (json.get("co_owners") == null) {
                ArrayNode ja = json.putArray("co_owners");

                for (int i = 0; i < admins.length; i++) {
                    ja.add(admins[i]);
                }
            }

            json.remove("username");

        } catch (IOException e) {
            e.printStackTrace();
        }

        sb.append(json.toString());

        return sb.toString();
    }

    public String appendCoOwners(JsonNode jsonReq) {

        StringBuilder sb = new StringBuilder();

        ObjectNode json = null;
        try {

            json = (ObjectNode) JsonUtils.getJsonTree(toValidCouchJson());

            if (json.get("owner_id") == null || !json.get("owner_id").equals(jsonReq.get("owner_id"))) {
                return json.toString();
            }

            ArrayNode ja = json.putArray("co_owners");

            for (int i = 0; i < admins.length; i++) {
                ja.add(admins[i]);
            }

            Iterator<JsonNode> it = jsonReq.path("co_owners").elements();

            while (it.hasNext()) {
                JsonNode curr = it.next();
                if (curr.textValue() != null) {
                    ja.add(curr.textValue());
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        sb.append(json.toString());

        return sb.toString();
    }

    public String changeOwner(String newOwnerId) {
        StringBuilder sb = new StringBuilder();
        ObjectNode json = null;
        try {
            this.fields.put("owner_id", newOwnerId);
            json = (ObjectNode) JsonUtils.getJsonTree(toValidCouchJson());
            ArrayNode ja = json.putArray("co_owners");
            for (int i = 0; i < admins.length; i++) {
                ja.add(admins[i]);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        sb.append(json.toString());
        return sb.toString();
    }

    public String toString() {
        return toValidCouchJson();
    }

}
