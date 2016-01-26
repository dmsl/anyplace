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
* URL: http://anyplace.cs.ucy.ac.cy
* Contact: anyplace@cs.ucy.ac.cy
*
* Copyright (c) 2015, Data Management Systems Lab (DMSL), University of Cyprus.
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

package com.dmsl.anyplace.floor;

import java.util.ArrayList;
import java.util.HashMap;

import com.dmsl.airplace.algorithms.LogRecord;
import com.dmsl.anyplace.utils.GeoPoint;

import android.content.Context;

public class Algo1Log extends FloorSelector {

	public Algo1Log(final Context myContext) {
		super(myContext);
	}

	protected String calculateFloor(Args args) throws Exception {

		GroupWifiFromLog algo1 = new Algo1Help( args);
		algo1.run(context.getAssets().open("rssHTC"));

		return algo1.getFloor();
	}

	private static class Algo1Help extends GroupWifiFromLog {

		final double a = 10;
		final double b = 10;
		final int l1 = 10;

		HashMap<String, Wifi> input = new HashMap<String, Wifi>();
		ArrayList<Score> mostSimilar = new ArrayList<Score>(10);

		private GeoPoint bbox[] = null;
		private Args args;

		public Algo1Help( Args args) {
			super();

			this.args = args;
			if (!(args.dlat == 0 || args.dlong == 0)) {
				bbox = GeoPoint.getGeoBoundingBox(args.dlat, args.dlong, 100);
			}

			for (LogRecord listenObject : args.latestScanList) {
				input.put(listenObject.getBssid(), new Wifi(listenObject.getBssid(), listenObject.getRss()));
			}
		}

		private double compare(ArrayList<String> bucket) {

			// # Timestamp, X, Y, HEADING, MAC Address of AP, RSS, FLOOR

			long score = 0;
			int nNCM = 0;
			int nCM = 0;

			for (String wifiDatabase : bucket) {
				String[] segs = wifiDatabase.split(" ");

				String mac = segs[4];

				if (input.containsKey(mac)) {
					Integer diff = (Integer.parseInt(segs[5]) - input.get(mac).rss);
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

			String max_floor = "";
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

		protected void process(String maxMac, ArrayList<String> values) {

			// # Timestamp, X, Y, HEADING, MAC Address of AP, RSS, FLOOR

			if (maxMac.equals(args.firstMac.getBssid()) || (args.secondMac != null && maxMac.equals(args.secondMac.getBssid()))) {

				String[] segs = values.get(0).split(" ");
				double x = Double.parseDouble(segs[1]);
				double y = Double.parseDouble(segs[2]);
				if (bbox == null || (x > bbox[0].dlat && x < bbox[1].dlat && y > bbox[0].dlon && y < bbox[1].dlon)) {

					double similarity = compare(values);
					checkScore(similarity, segs[6]);
				}
			}

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
}
