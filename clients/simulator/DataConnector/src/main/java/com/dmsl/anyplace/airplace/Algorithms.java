/**
 * Anyplace Simulator:  A trace-driven evaluation and visualization of IoT Data Prefetching in Indoor Navigation SOAs
 *
 * Author(s): Zacharias Georgiou, Panagiotis Irakleous
 *
 * Supervisor: Demetrios Zeinalipour-Yazti
 *
 * Copyright (c) 2017 Data Management Systems Laboratory, University of Cyprus
 *
 * This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public
 * License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see http://www.gnu.org/licenses/
 */
package com.dmsl.anyplace.airplace;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by panag on 4/27/2016.
 */
public class Algorithms {

	final static String K = "4";

	public static String ProcessingAlgorithms(ArrayList<LogRecord> latestScanList, RadioMap RM, int algorithm_choice) {

		int i, j;

		ArrayList<String> MacAdressList = RM.getMacAdressList();
		ArrayList<String> Observed_RSS_Values = new ArrayList<String>();
		LogRecord temp_LR;
		int notFoundCounter = 0;
		// Read parameter of algorithm
		String NaNValue = readParameter(RM, 0);

		// Check which mac addresses of radio map, we are currently listening.
		for (i = 0; i < MacAdressList.size(); ++i) {

			for (j = 0; j < latestScanList.size(); ++j) {

				temp_LR = latestScanList.get(j);

				// MAC Address Matched
				if (MacAdressList.get(i).compareTo(temp_LR.getBssid()) == 0) {
					Observed_RSS_Values.add(String.valueOf(temp_LR.getRss()));
					break;
				}
			}
			// A MAC Address is missing so we place a small value, NaN value
			if (j == latestScanList.size()) {
				Observed_RSS_Values.add(String.valueOf(NaNValue));
				++notFoundCounter;
			}
		}

		if (notFoundCounter == MacAdressList.size())
			return null;

		// Read parameter of algorithm
		String parameter = readParameter(RM, algorithm_choice);

		if (parameter == null)
			return null;

		switch (algorithm_choice) {

		case 1:
			return KNN_WKNN_Algorithm(RM, Observed_RSS_Values, parameter, false);
		case 2:
			return KNN_WKNN_Algorithm(RM, Observed_RSS_Values, parameter, true);
		 case 3:
		 return MAP_MMSE_Algorithm(RM, Observed_RSS_Values, parameter, false);
		 case 4:
		 return MAP_MMSE_Algorithm(RM, Observed_RSS_Values, parameter, true);
		}
		return null;

	}
	
	private static String MAP_MMSE_Algorithm(RadioMap RM, ArrayList<String> Observed_RSS_Values, String parameter, boolean isWeighted) {

		ArrayList<String> RSS_Values;
		double curResult = 0.0d;
		String myLocation = null;
		double highestProbability = Double.NEGATIVE_INFINITY;
		ArrayList<LocDistance> LocDistance_Results_List = new ArrayList<LocDistance>();
		float sGreek;

		try {
			sGreek = Float.parseFloat(parameter);
		} catch (Exception e) {
			return null;
		}

		// Find the location of user with the highest probability
		for (String location : RM.getLocationRSS_HashMap().keySet()) {

			RSS_Values = RM.getLocationRSS_HashMap().get(location);
			curResult = calculateProbability(RSS_Values, Observed_RSS_Values, sGreek);

			if (curResult == Double.NEGATIVE_INFINITY)
				return null;
			else if (curResult > highestProbability) {
				highestProbability = curResult;
				myLocation = location;
			}

			if (isWeighted)
				LocDistance_Results_List.add(0, new LocDistance(curResult, location));
		}

		if (isWeighted)
			myLocation = calculateWeightedAverageProbabilityLocations(LocDistance_Results_List);

		return myLocation;
	}
	
	public static String calculateWeightedAverageProbabilityLocations(ArrayList<LocDistance> LocDistance_Results_List) {

		double sumProbabilities = 0.0f;
		double WeightedSumX = 0.0f;
		double WeightedSumY = 0.0f;
		double NP;
		float x, y;
		String[] LocationArray = new String[2];

		// Calculate the sum of all probabilities
		for (int i = 0; i < LocDistance_Results_List.size(); ++i)
			sumProbabilities += LocDistance_Results_List.get(i).getDistance();

		// Calculate the weighted (Normalized Probabilities) sum of X and Y
		for (int i = 0; i < LocDistance_Results_List.size(); ++i) {
			LocationArray = LocDistance_Results_List.get(i).getLocation().split(" ");

			try {
				x = Float.valueOf(LocationArray[0].trim()).floatValue();
				y = Float.valueOf(LocationArray[1].trim()).floatValue();
			} catch (Exception e) {
				return null;
			}

			NP = LocDistance_Results_List.get(i).getDistance() / sumProbabilities;

			WeightedSumX += (x * NP);
			WeightedSumY += (y * NP);

		}

		return WeightedSumX + " " + WeightedSumY;

	}

	
	public static double calculateProbability(ArrayList<String> l1, ArrayList<String> l2, float sGreek) {

		double finalResult = 1;
		float v1;
		float v2;
		double temp;
		String str;

		for (int i = 0; i < l1.size(); ++i) {

			try {
				str = l1.get(i);
				v1 = Float.valueOf(str.trim()).floatValue();
				str = l2.get(i);
				v2 = Float.valueOf(str.trim()).floatValue();
			} catch (Exception e) {
				return Double.NEGATIVE_INFINITY;
			}

			temp = v1 - v2;

			temp *= temp;

			temp = -temp;

			temp /= (double) (sGreek * sGreek);
			temp = (double) Math.exp(temp);

			//Do not allow zero instead stop on small possibility
			if (finalResult * temp != 0)
				finalResult = finalResult * temp;
		}
		return finalResult;
	}


	private static String readParameter(RadioMap RM, int algorithm_choice) {

		String parameter = null;

		if (algorithm_choice == 0) {
			parameter = RM.getNaN();
		} else if (algorithm_choice == 1) {
			// && ("KNN")
			parameter = K;
		} else if (algorithm_choice == 2) {
			// && ("WKNN")
			parameter = K;
		} else if (algorithm_choice == 3) {
			// && ("MAP")
			parameter = K;
		} else if (algorithm_choice == 4) {
			// && ("MMSE")
			parameter = K;
		}

		return parameter;
	}

	private static String KNN_WKNN_Algorithm(RadioMap RM, ArrayList<String> Observed_RSS_Values, String parameter,
			boolean isWeighted) {

		ArrayList<String> RSS_Values;
		float curResult = 0;
		ArrayList<LocDistance> LocDistance_Results_List = new ArrayList<LocDistance>();
		String myLocation = null;
		int K;

		try {
			K = Integer.parseInt(parameter);
		} catch (Exception e) {
			return null;
		}

		// Construct a list with locations-distances pairs for currently
		// observed RSS values
		for (String location : RM.getLocationRSS_HashMap().keySet()) {
			RSS_Values = RM.getLocationRSS_HashMap().get(location);
			curResult = calculateEuclideanDistance(RSS_Values, Observed_RSS_Values);

			if (curResult == Float.NEGATIVE_INFINITY)
				return null;

			LocDistance_Results_List.add(0, new LocDistance(curResult, location));
		}

		// Sort locations-distances pairs based on minimum distances
		Collections.sort(LocDistance_Results_List, new Comparator<LocDistance>() {

			public int compare(LocDistance gd1, LocDistance gd2) {
				return (gd1.getDistance() > gd2.getDistance() ? 1 : (gd1.getDistance() == gd2.getDistance() ? 0 : -1));
			}
		});

		if (!isWeighted) {
			myLocation = calculateAverageKDistanceLocations(LocDistance_Results_List, K);
		} else {
			myLocation = calculateWeightedAverageKDistanceLocations(LocDistance_Results_List, K);
		}

		return myLocation;

	}

	public static String calculateWeightedAverageKDistanceLocations(ArrayList<LocDistance> LocDistance_Results_List,
			int K) {

		double LocationWeight = 0.0f;
		double sumWeights = 0.0f;
		double WeightedSumX = 0.0f;
		double WeightedSumY = 0.0f;

		String[] LocationArray = new String[2];
		float x, y;

		int K_Min = K < LocDistance_Results_List.size() ? K : LocDistance_Results_List.size();

		// Calculate the weighted sum of X and Y
		for (int i = 0; i < K_Min; ++i) {
			if (LocDistance_Results_List.get(i).getDistance() != 0.0) {
				LocationWeight = 1 / LocDistance_Results_List.get(i).getDistance();
			} else {
				LocationWeight = 100;
			}
			LocationArray = LocDistance_Results_List.get(i).getLocation().split(" ");

			try {
				x = Float.valueOf(LocationArray[0].trim()).floatValue();
				y = Float.valueOf(LocationArray[1].trim()).floatValue();
			} catch (Exception e) {
				return null;
			}

			sumWeights += LocationWeight;
			WeightedSumX += LocationWeight * x;
			WeightedSumY += LocationWeight * y;

		}

		WeightedSumX /= sumWeights;
		WeightedSumY /= sumWeights;

		return WeightedSumX + " " + WeightedSumY;
	}

	private static String calculateAverageKDistanceLocations(ArrayList<LocDistance> LocDistance_Results_List, int K) {

		float sumX = 0.0f;
		float sumY = 0.0f;

		String[] LocationArray = new String[2];
		float x, y;

		int K_Min = K < LocDistance_Results_List.size() ? K : LocDistance_Results_List.size();

		// Calculate the sum of X and Y
		for (int i = 0; i < K_Min; ++i) {
			LocationArray = LocDistance_Results_List.get(i).getLocation().split(" ");

			try {
				x = Float.valueOf(LocationArray[0].trim()).floatValue();
				y = Float.valueOf(LocationArray[1].trim()).floatValue();
			} catch (Exception e) {
				return null;
			}

			sumX += x;
			sumY += y;
		}

		// Calculate the average
		sumX /= K_Min;
		sumY /= K_Min;

		return sumX + " " + sumY;

	}

	private static float calculateEuclideanDistance(ArrayList<String> l1, ArrayList<String> l2) {

		float finalResult = 0;
		float v1;
		float v2;
		float temp;
		String str;

		for (int i = 0; i < l1.size(); ++i) {

			try {
				str = l1.get(i);
				v1 = Float.valueOf(str.trim()).floatValue();
				str = l2.get(i);
				v2 = Float.valueOf(str.trim()).floatValue();
			} catch (Exception e) {
				return Float.NEGATIVE_INFINITY;
			}

			// do the procedure
			temp = v1 - v2;
			temp *= temp;

			// do the procedure
			finalResult += temp;
		}
		return ((float) Math.sqrt(finalResult));
	}
}
