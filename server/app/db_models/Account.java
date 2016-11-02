/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Kyriakos Georgiou
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
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.Gson;
import utils.JsonUtils;

import java.io.IOException;
import java.util.HashMap;

public class Account extends AbstractModel {

    private JsonNode json;

    public Account(HashMap<String, String> hm) {
        this.fields = hm;
    }

    public Account() {
        fields.put("name", "");
        fields.put("owner_id", "");
        fields.put("type", "");
        fields.put("doc_type", "account");
    }

    public Account(JsonNode json) {
        fields.put("name", json.path("name").textValue());
        fields.put("owner_id", json.path("owner_id").textValue());
        fields.put("type", json.path("type").textValue());
        fields.put("doc_type", "account");

        this.json = json;
    }

    public String getId() {
        String puid = fields.get("owner_id");
        return puid;
    }

    public String toValidCouchJson() {
        Gson gson = new Gson();
        return gson.toJson(this.getFields());
    }

    public String toCouchGeoJSON() {
        StringBuilder sb = new StringBuilder();

        ObjectNode json = null;
        try {
            json = (ObjectNode) JsonUtils.getJsonTree(toValidCouchJson());
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
