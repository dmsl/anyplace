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
package floor_module

import java.util.{ArrayList, HashMap}

import datasources.SCHEMA
import play.api.libs.json.{JsValue, Json}

import scala.jdk.CollectionConverters.CollectionHasAsScala

class Algo1(json: JsValue) extends IAlgo {
    val a = 10
    val b = 10
    val l1 = 10
    var input: HashMap[String, Wifi] = new HashMap[String, Wifi]()
    var mostSimilar: ArrayList[Score] = new ArrayList[Score](10)
    val listenList = (json\"wifi").as[List[String]]

    if (listenList == null) {
        throw new Exception("Wifi parameter is not array")
    }

    for (listenObject <- listenList) {
        val obj = Json.parse(listenObject)
        val mac = (obj\SCHEMA.fMac).as[String]
        val rss = (obj\SCHEMA.fRSS).as[String].toInt
        //for (listenObject <- listenList.iterator()) {
        //    val obj=listenObject.asInstanceOf[JsonObject]
        //    val mac = obj.getString("MAC")
        //    val rss = obj.getInt("rss")
        if (mac == null) {
            throw new Exception("Invalid array wifi. Require: mac,rss")
        }

        input.put(mac, new Wifi(mac, rss))
    }

    private def compare(bucket: ArrayList[JsValue]): Double = {
        var score = 0
        var nNCM = 0
        var nCM = 0
        for (wifiDatabase <- bucket.asScala) {
            //val mac = wifiDatabase.getString("MAC")
            val mac = (wifiDatabase\SCHEMA.fMac).as[String]
            if (input.containsKey(mac)) {
                //val diff = java.lang.Integer.parseInt(wifiDatabase.getString("rss")) - input.get(mac).rss
                val diff = java.lang.Integer.parseInt((wifiDatabase\SCHEMA.fRSS).as[String]) - input.get(mac).rss
                score += diff * diff
                nCM += 1
            } else {
                nNCM += 1
            }
        }
        Math.sqrt(score) - a * nCM + b * nNCM
    }

    private def checkScore(similarity: Double, floor: String): Unit ={
        if (mostSimilar.size == 0) {
            mostSimilar.add(new Score(similarity, floor))
            return
        }
        for (i <- 0 until mostSimilar.size if mostSimilar.get(i).similarity > similarity) {
            mostSimilar.add(i, new Score(similarity, floor))
            if (mostSimilar.size > l1) {
                mostSimilar.remove(mostSimilar.size - 1)
            }
            return
        }
        if (mostSimilar.size < l1) {
            mostSimilar.add(new Score(similarity, floor))
        }
    }

    def proccess(bucket: ArrayList[JsValue], floor: String) : Unit = {
        val similarity = compare(bucket)
        checkScore(similarity, floor)
    }

    def getFloor(): String = {
        val sum_floor_score = new HashMap[String, Integer]()
        for (s <- mostSimilar.asScala) {
            var score = 1
            if (sum_floor_score.containsKey(s.floor)) {
                score = sum_floor_score.get(s.floor) + 1
            }
            sum_floor_score.put(s.floor, score)
        }
        var max_floor = "0"
        var max_score = 0
        for (floor <- sum_floor_score.keySet.asScala) {
            val score = sum_floor_score.get(floor)
            if (max_score < score) {
                max_score = score
                max_floor = floor
            }
        }
        max_floor
    }

    class Score(var similarity: Double, var floor: String)
    class Wifi(var mac: String, var rss: java.lang.Integer)
}
