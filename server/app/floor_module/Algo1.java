/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Timotheos Constambeys
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

package floor_module;

import java.util.ArrayList;
import java.util.HashMap;

import com.fasterxml.jackson.databind.JsonNode;

public class Algo1 implements IAlgo {
    final double a = 10;
    final double b = 10;
    final int l1 = 10;

    HashMap<String, Wifi> input = new HashMap<String, Wifi>();
    ArrayList<Score> mostSimilar = new ArrayList<Score>(10);

    public Algo1(JsonNode json) throws Exception {
        JsonNode listenList = json.get("wifi");
        if (!listenList.isArray()) {
            throw new Exception("Wifi parameter is not array");
        }
        for (JsonNode listenObject : listenList) {
            JsonNode Jmac = listenObject.get("MAC");
            JsonNode Jrss = listenObject.get("rss");

            if (Jmac == null || Jrss == null) {
                throw new Exception("Invalid array wifi:: require mac,rss");
            }

            String mac = Jmac.textValue();
            int rss = Jrss.asInt();
            input.put(mac, new Wifi(mac, rss));
        }
    }

    private double compare(ArrayList<JsonNode> bucket) {

        long score = 0;
        int nNCM = 0;
        int nCM = 0;

        for (JsonNode wifiDatabase : bucket) {
            String mac = wifiDatabase.get("MAC").textValue();

            if (input.containsKey(mac)) {
                Integer diff = (Integer.parseInt(wifiDatabase.get("rss")
                        .textValue()) - input.get(mac).rss);
                score += diff * diff;

                nCM++;
            } else {
                nNCM++;
            }
        }

        return Math.sqrt(score) - a * nCM + b * nNCM;
    }

    private void checkScore(double similarity, String floor) {

        if (mostSimilar.size() == 0) {
            mostSimilar.add(new Score(similarity, floor));
            return;
        }

        for (int i = 0; i < mostSimilar.size(); i++) {
            if (mostSimilar.get(i).similarity > similarity) {
                mostSimilar.add(i, new Score(similarity, floor));
                if (mostSimilar.size() > l1) {
                    mostSimilar.remove(mostSimilar.size() - 1);
                }
                return;
            }
        }

        if (mostSimilar.size() < l1) {
            mostSimilar.add(new Score(similarity, floor));
        }

    }

    public void proccess(ArrayList<JsonNode> bucket, String floor) {
        double similarity = compare(bucket);
        checkScore(similarity, floor);
    }

    public String getFloor() {
        // Floor -Score
        HashMap<String, Integer> sum_floor_score = new HashMap<String, Integer>();

        for (Score s : mostSimilar) {
            Integer score = 1;
            if (sum_floor_score.containsKey(s.floor)) {
                score = sum_floor_score.get(s.floor) + 1;
            }

            sum_floor_score.put(s.floor, score);
        }

        String max_floor = "0";
        int max_score = 0;

        for (String floor : sum_floor_score.keySet()) {
            int score = sum_floor_score.get(floor);
            if (max_score < score) {
                max_score = score;
                max_floor = floor;
            }
        }

        return max_floor;

    }

    private class Score {

        double similarity;
        String floor;

        Score(double similarity, String floor) {
            this.similarity = similarity;
            this.floor = floor;
        }
    }

    private class Wifi {
        String mac;
        Integer rss;

        Wifi(String mac, Integer rss) {
            this.mac = mac;
            this.rss = rss;
        }
    }

}