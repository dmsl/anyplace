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
package utils

import org.codehaus.jettison.json.JSONArray
import org.codehaus.jettison.json.JSONException
import org.codehaus.jettison.json.JSONObject
import java.util.ArrayList
import java.util.List
//remove if not needed
import scala.collection.JavaConversions._

class GeoJSONMultiPoint(points_in: GeoPoint*) {

    var points: List[GeoPoint] = new ArrayList[GeoPoint]()

    for (p <- points_in) {
        points.add(p)
    }

    def toGeoJSON(): JSONObject = {
        var ja: JSONArray = null
        var jall = new JSONArray()

        for (p <- this.points) {
            ja = new JSONArray()
            ja.put(p.dlat)
            ja.put(p.dlon)
            jall.put(ja)
        }
        val jo = new JSONObject()
        jo.put("type", "MultiPoint")
        jo.put("coordinates", jall)
        jo
    }
}
