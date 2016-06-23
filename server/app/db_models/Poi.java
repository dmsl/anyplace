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

import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.*;

import com.google.gson.Gson;
import utils.GeoJSONPoint;
import utils.JsonUtils;
import utils.LPUtils;

import java.io.IOException;
import java.util.HashMap;

public class Poi extends AbstractModel{

    public final static String POIS_TYPE_NONE = "None";
    public final static String POIS_TYPE_ELEVATOR = "elevator";
    public final static String POIS_TYPE_STAIR = "stair";

    private JsonNode json;
    private double lat;
    private double lng;

    public Poi(HashMap<String, String> hm){
        this.fields = hm;
    }

    public Poi(){
        fields.put("username", "");

        fields.put("username_creator", "");
        fields.put("puid","");
        fields.put("buid","");
        fields.put("is_published","");
        fields.put("floor_name","");
        fields.put("floor_number", "");
        fields.put("name", "");
        fields.put("description","");
        fields.put("url","");
        fields.put("image","");
        fields.put("pois_type","");
        fields.put("is_door", "");
        fields.put("coordinates_lat", "");
        fields.put("coordinates_lon", "");
    }

    public Poi(JsonNode json){
        fields.put("username_creator", json.path("username_creator").textValue());
        fields.put("puid", json.path("puid").textValue());
        fields.put("buid", json.path("buid").textValue());
        fields.put("is_published", json.path("is_published").textValue());
        fields.put("floor_name", json.path("floor_name").textValue());
        fields.put("floor_number", json.path("floor_number").textValue());
        fields.put("name", json.path("name").textValue());

        if(json.path("description") != null && json.path("description").textValue() != null && !json.path("description").textValue().isEmpty()) {
            fields.put("description", json.path("description").textValue());
        } else {
            fields.put("description", "-");
        }

        if(json.path("url") != null && json.path("url").textValue() != null && !json.path("url").textValue().isEmpty()) {
            fields.put("url", json.path("url").textValue());
        } else {
            fields.put("url", "-");
        }

        fields.put("image", json.path("image").textValue());
        fields.put("pois_type", json.path("pois_type").textValue());
        fields.put("is_door", json.path("is_door").textValue());
        fields.put("is_building_entrance", json.path("is_building_entrance").textValue());
        fields.put("coordinates_lat", json.path("coordinates_lat").textValue());
        fields.put("coordinates_lon", json.path("coordinates_lon").textValue());

        this.json = json;
        this.lat = Double.parseDouble(json.path("coordinates_lat").textValue());
        this.lng = Double.parseDouble(json.path("coordinates_lon").textValue());
    }

    public String getId(){
        String puid;
        if( (puid = fields.get("puid")) == null || puid.equals("") ){
            fields.put("puid", Poi.getId(fields.get("username_creator"), fields.get("buid"), fields.get("floor_number"), fields.get("coordinates_lat"), fields.get("coordinates_lon")) );
            ((ObjectNode)this.json).put("puid", fields.get("puid"));
            puid = fields.get("puid");
        }
        return puid;
    }

    public static String getId( String username_creator, String buid, String floor_number, String coordinates_lat, String coordinates_lon ){
        /*
        String rnd = LPUtils.generateRandomToken();
        String finalId = buid + "_" +  coordinates_lat
                + String.valueOf(System.currentTimeMillis())
                + coordinates_lon + rnd;
        return "poi_" + LPUtils.encodeBase64String(finalId);
        */
        return "poi_" + LPUtils.getRandomUUID();
        //return username_creator + "_" + buid + "_" + floor_number + "_" + coordinates_lat + "_" + coordinates_lon + "_" + System.currentTimeMillis();
    }

    public String toValidCouchJson(){
        getId(); // initialize id if not initialized
        Gson gson = new Gson();
        return gson.toJson( this.getFields() );
    }

    public String toCouchGeoJSON(){
        StringBuilder sb = new StringBuilder();

        ObjectNode json = null;
        try {

            json = (ObjectNode) JsonUtils.getJsonTree(toValidCouchJson());
            json.put("geometry", new GeoJSONPoint(Double.parseDouble(fields.get("coordinates_lat")),
                    Double.parseDouble(fields.get("coordinates_lon"))).toGeoJSON());

            json.remove("username");

        } catch (IOException e) {
            e.printStackTrace();
        }

        sb.append( json.toString() );

        return sb.toString();
    }


    public String toString(){
        return toValidCouchJson();
    }

}
