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
import com.google.gson.Gson;
import utils.LPUtils;

import java.util.HashMap;

public class MagneticMilestone extends AbstractModel {

    private JsonNode json;

    public MagneticMilestone(HashMap<String, String> hm) {
        this.fields = hm;
    }

    public MagneticMilestone() {

        fields.put("buid", "");
        fields.put("floor_num", "");
        fields.put("mmuid", "");
        fields.put("mpuid", "");

        fields.put("lat", "");
        fields.put("lng", "");

        fields.put("ow", "");
        fields.put("ox", "");
        fields.put("oy", "");
        fields.put("oz", "");

        fields.put("fx", "");
        fields.put("fy", "");
        fields.put("fz", "");

        fields.put("doctype", "magnetic_milestone");
    }

    public MagneticMilestone(JsonNode json) {

        fields.put("buid", json.path("buid").textValue());
        fields.put("floor_num", json.path("floor_num").textValue());
        fields.put("mmuid", json.path("mmuid").textValue());
        fields.put("mpuid", json.path("mpuid").textValue());

        fields.put("lat", json.path("lat").textValue());
        fields.put("lng", json.path("lng").textValue());

        fields.put("ow", json.path("ow").textValue());
        fields.put("ox", json.path("ox").textValue());
        fields.put("oy", json.path("oy").textValue());
        fields.put("oz", json.path("oz").textValue());

        fields.put("fx", json.path("fx").textValue());
        fields.put("fy", json.path("fy").textValue());
        fields.put("fz", json.path("fz").textValue());

        fields.put("doctype", "magnetic_milestone");

        this.json = json;
    }

    public MagneticMilestone(JsonNode json, String buid, String floor_num, String mpuid) {

        fields.put("buid", buid);
        fields.put("floor_num", floor_num);
        fields.put("mpuid", mpuid);

        fields.put("lat", json.path("lat").textValue());
        fields.put("lng", json.path("lng").textValue());

        fields.put("ow", json.path("ow").textValue());
        fields.put("ox", json.path("ox").textValue());
        fields.put("oy", json.path("oy").textValue());
        fields.put("oz", json.path("oz").textValue());

        fields.put("fx", json.path("fx").textValue());
        fields.put("fy", json.path("fy").textValue());
        fields.put("fz", json.path("fz").textValue());

        fields.put("doctype", "magnetic_milestone");

        this.json = json;
    }

    public String getId() {
        String id;
        if ((id = fields.get("mmuid")) == null || id.equals("")) {
            id = "mmilestone_" + LPUtils.getRandomUUID() + "_" + System.currentTimeMillis();
        }
        return id;
    }

    public String toValidCouchJson() {
        getId(); // to initialize it if not initialized

        Gson gson = new Gson();
        return gson.toJson(this.getFields());
    }

    @Override
    public String toCouchGeoJSON() {
        return toValidCouchJson();
    }

    public String toString() {
        return this.toValidCouchJson();
    }

}
