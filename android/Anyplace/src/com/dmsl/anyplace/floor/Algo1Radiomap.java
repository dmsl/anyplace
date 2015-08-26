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

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.util.Log;

import com.dmsl.airplace.alogrithms.LogRecord;
import com.dmsl.anyplace.utils.AnyplaceUtils;
import com.dmsl.anyplace.utils.GeoPoint;

public class Algo1Radiomap extends FloorSelector {

	String[] files;
	String[] floorNumbers;

	public Algo1Radiomap(final Context myContext) {
		super(myContext);
	}

	public void updateFiles(final String buid) {

		try {
			File radiomaps = AnyplaceUtils.getRadioMapsRootFolder(context);
			String[] file_names = radiomaps.list(new FilenameFilter() {

				@Override
				public boolean accept(File dir, String filename) {
					if (filename.startsWith(buid))
						return true;
					else
						return false;
				}
			});

			files = new String[file_names.length];
			floorNumbers = new String[file_names.length];

			for (int i = 0; i < file_names.length; i++) {
				floorNumbers[i] = file_names[i].substring(file_names[i].indexOf("fl_") + 3);
				files[i] = radiomaps.getAbsolutePath() + File.separator + file_names[i] + File.separator + AnyplaceUtils.getRadioMapFileName(floorNumbers[i]);
			}

		} catch (Exception e) {
			Log.e("Algo1Radiomap", e.getMessage());
		}
	}

	protected String calculateFloor(Args args) throws Exception {

		if (files==null || files.length == 0)
			return "";

		GroupWifiFromRadiomap algo1 = new Algo1Help(args);

		for (int i = 0; i < files.length; i++) {

			algo1.run(new FileInputStream(files[i]), floorNumbers[i]);
		}

		return algo1.getFloor();
	}

	private static class Algo1Help extends GroupWifiFromRadiomap {

		final double a = 10;
		final double b = 10;
		final int l1 = 10;

		HashMap<String, Wifi> input = new HashMap<String, Wifi>();
		ArrayList<Score> mostSimilar = new ArrayList<Score>(10);

		private GeoPoint bbox[] = null;
		private Args args;

		public Algo1Help(Args args) {
			super();

			this.args = args;
			if (!(args.dlat == 0 || args.dlong == 0)) {
				bbox = GeoPoint.getGeoBoundingBox(args.dlat, args.dlong, 100);
			}

			for (LogRecord listenObject : args.latestScanList) {
				input.put(listenObject.getBssid(), new Wifi(listenObject.getBssid(), listenObject.getRss()));
			}
		}

		private double compare(String[] macs, String line) {

			// # Timestamp, X, Y, HEADING, MAC Address of AP, RSS, FLOOR

			String[] segs = line.split(", ");
			long score = 0;
			int nNCM = 0;
			int nCM = 0;

			for (int i = 3; i < segs.length; i++) {
				if (!segs[i].equals(NaN)) {
					if (input.containsKey(macs[i])) {
						Integer diff = (Integer.parseInt(segs[i].split("\\.")[0]) - input.get(macs[i]).rss);
						score += diff * diff;

						nCM++;
					} else {
						nNCM++;
					}
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

		protected void process(String maxMac, String[] macs, String line) {

			// # X, Y, HEADING, 00:16:b6:ee:00:7f, d4:d7:48:d8:28:30
			if (maxMac.equals(args.firstMac.getBssid()) || (args.secondMac != null && maxMac.equals(args.secondMac.getBssid()))) {

				if (bbox == null) {

					double similarity = compare(macs, line);
					checkScore(similarity, floor);
				} else {
					String[] segs = line.split(",");
					double x = Double.parseDouble(segs[0]);
					double y = Double.parseDouble(segs[1]);

					if (x > bbox[0].dlat && x < bbox[1].dlat && y > bbox[0].dlon && y < bbox[1].dlon) {
						double similarity = compare(macs, line);
						checkScore(similarity, floor);
					}
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
