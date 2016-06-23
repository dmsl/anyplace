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
//import org.codehaus.jackson.JsonNode;
//import org.codehaus.jackson.node.ObjectNode;

import java.util.HashMap;

public class Floor extends AbstractModel{

    private JsonNode json;

    public Floor(HashMap<String,String> hm){
        this.fields = hm;
    }

    public Floor(){
        fields.put("username_creator", "");
        fields.put("buid","");
        fields.put("is_published","");
        fields.put("floor_name","");
        fields.put("floor_number", "");
        fields.put("description","");
    }

    public Floor(JsonNode json){
        fields.put("username_creator", json.path("username_creator").textValue());

        // will contain its id from now on
        fields.put("fuid",json.path("fuid").textValue());

        fields.put("buid",json.path("buid").textValue());
        fields.put("is_published", json.path("is_published").textValue());
        fields.put("floor_name", json.path("floor_name").textValue());
        fields.put("description", json.path("description").textValue());
        fields.put("floor_number", json.path("floor_number").textValue());

        this.json = json;
    }

    public String getId(){
        String fuid;
        if( (fuid=this.json.path("fuid").textValue()) == null || fuid.trim().isEmpty() ){
            fuid = Floor.getId( fields.get( "buid") ,fields.get("floor_number") );
            fields.put("fuid", fuid);
            ((ObjectNode)this.json).put("fuid", fuid);
        }
        return fuid;
    }

    public String toValidCouchJson(){
        getId(); // to initialize it if not initialized

        fields.remove("username");
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

    public static String getId( String buid, String floor_number){
        return buid + "_" + floor_number;
    }

    public static boolean checkFloorNumberFormat(String floor_number) {
        for( char c : floor_number.toCharArray() ){
            if( Character.isWhitespace(c) ) return false;
        }
        return true;
    }

}
