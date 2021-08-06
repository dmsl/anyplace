/**
 * Anyplace Simulator:  A trace-driven evaluation and visualization of IoT Data Prefetching in Indoor Navigation SOAs
 *
 * Author(s): Zacharias Georgiou, Panagiotis Irakleous

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
package com.dmsl.anyplace.algorithms;

import com.dmsl.anyplace.algorithms.blocks.AlgorithmBlocks;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;

public class GetResultsFiles {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
//		if (args.length == 0)
//			System.out.println("Give path!");

		// File file = new File(args[1]);

//		File file = new File(
//				"data/simulations/username_1373876832005/username_1373876832005_dataset_8_15_100_1456737260395_1_10_0.5.sim");

		new GetResultsFiles().run("file.txt");
	}
	
	
	private double stdDevCPU = 0;
	private double stdDevError = 0;
	private double stdDevFPS = 0;
	public void run(String fname) throws Exception {
		ArrayList<Double> cpus = new ArrayList<Double>();
		ArrayList<Double> err = new ArrayList<Double>();
		ArrayList<Integer> fp = new ArrayList<Integer>();
		
		File file = new File(fname);

		if (!file.exists()) {
			throw new Exception("File doesn't exist");
		}
		BufferedReader bf = new BufferedReader(new FileReader(file));
		String line = "";

		int count = 0;
		double globalCPU = 0;
		double globalError = 0;
		double globalFPS = 0;

		while ((line = bf.readLine()) != null) {
			double localCPU = 0;
			double localError = 0;
			int localFPS = 0;

			if (line.startsWith("#"))
				continue;
			String toks[] = line.split("\\*");

			Type listType = new TypeToken<ArrayList<AlgorithmBlocks.StatsItem>>() {
			}.getType();
			ArrayList<AlgorithmBlocks.StatsItem> items = new Gson().fromJson(
					toks[1], listType);
			for (int i = 0; i < items.size(); i++) {
				localCPU += items.get(i).getTime() * 1000; // from seconds to ms
				localError += items.get(i).getError();
				localFPS += items.get(i).getFpsSize();
			}

			globalCPU += localCPU / items.size();
			globalError += localError / items.size();
			globalFPS += localFPS;

			cpus.add(localCPU/items.size());
			err.add(localError/items.size());
			fp.add(localFPS);
			
			count++;
		}

		System.out.println("Finished reading!");

		bf.close();

		
		double totalCPU = globalCPU / count;
		double totalError = globalError / count;
		double totalFPS = globalFPS / count;
		
		computeStandardDeviation(totalCPU,totalError,totalFPS,cpus,err,fp);
		
		File results = new File(file.getPath() + ".results");
		BufferedWriter bfr = new BufferedWriter(new FileWriter(results));
		bfr.write("Total CPU\tTotal Error\tTotalFPS\tSD CPU\tSD Error\tSD FPS\n");
		bfr.write(totalCPU + "\t" + totalError + "\t" + totalFPS+ "\t" + stdDevCPU+ "\t"+stdDevError+ "\t"+stdDevFPS);
		bfr.close();
		
		System.out.println("Finished writting!");
	}


	private void computeStandardDeviation(double meanCPU, double meanError,
			double meanFPS, ArrayList<Double>cp,ArrayList<Double>er,ArrayList<Integer>fp) {
		
		
		double globalCPU = 0;
		double globalError = 0;
		double globalFPS = 0;
		for(int i=0;i<cp.size();i++){
			globalCPU += Math.pow(cp.get(i)-meanCPU, 2);
			globalError += Math.pow(er.get(i)-meanError, 2);
			globalFPS += Math.pow(fp.get(i)-meanFPS, 2);
		}
		
		double div = 1.0/(double)cp.size();
		
		stdDevCPU = Math.sqrt(div*globalCPU);
		stdDevError = Math.sqrt(div*globalError);
		stdDevFPS = Math.sqrt(div*globalFPS);
		
	}

}
