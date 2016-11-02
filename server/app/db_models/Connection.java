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

import com.google.gson.Gson;
//import org.codehaus.jackson.JsonNode;
//import org.codehaus.jackson.node.ObjectNode;

import com.fasterxml.jackson.databind.node.*;
import com.fasterxml.jackson.databind.*;

import java.util.HashMap;

public class Connection extends AbstractModel{

    public final static String EDGE_TYPE_STAIR = "stair";
    public final static String EDGE_TYPE_ELEVATOR = "elevator";
    public final static String EDGE_TYPE_HALLWAY = "hallway";
    public final static String EDGE_TYPE_ROOM = "room";
    public final static String EDGE_TYPE_OUTDOOR = "outdoor";

    private JsonNode json;

    public Connection( HashMap<String,String> hm){
        this.fields = hm;
    }

    public Connection(){
        fields.put("is_published", "");
        fields.put("edge_type", "");
        fields.put("pois_a", "");
        fields.put("pois_b", "");
        fields.put("weight", "");
        fields.put("buid","");

        fields.put("floor_a", "");
        fields.put("floor_b", "");
        fields.put("buid_a", "");
        fields.put("buid_b", "");

        fields.put("cuid", "");
    }

    public Connection(JsonNode json){
        fields.put("is_published", json.path("is_published").textValue());
        fields.put("edge_type", json.path("edge_type").textValue());
        fields.put("pois_a", json.path("pois_a").textValue());
        fields.put("pois_b", json.path("pois_b").textValue());
        fields.put("weight", json.path("weight").textValue());
        fields.put("buid",json.path("buid").textValue());

        fields.put("floor_a", json.path("floor_a").textValue());
        fields.put("floor_b", json.path("floor_b").textValue());
        fields.put("buid_a", json.path("buid_a").textValue());
        fields.put("buid_b", json.path("buid_b").textValue());

        fields.put("cuid", json.path("cuid").textValue());

        this.json = json;
    }

    public String getId(){
        String cuid;
        if( (cuid = fields.get("cuid")) == null || cuid.equals("") ){
            cuid = Connection.getId(json.path("pois_a").textValue(),
                    json.path("pois_b").textValue());
            fields.put("cuid", cuid);
            ((ObjectNode)this.json).put("cuid", cuid);
        }
        return cuid;
    }

    public String toValidCouchJson(){
        getId(); // to initialize it if not initialized

        Gson gson = new Gson();
        return gson.toJson( this.getFields() );
    }

    @Override
    public String toCouchGeoJSON() {
        return toValidCouchJson();
    }

    public String toString(){

        return this.toValidCouchJson();

    }

    public static String getId( String pois_a, String pois_b ){
        return "conn_"+ pois_a + "_" + pois_b;
    }

}
