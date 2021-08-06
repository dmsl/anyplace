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
package com.dmsl.anyplace;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;

public class Reverse {

	public static void main(String[] args) throws Exception {
		String inp = "file.txt";
		File file = new File(args[0]);
		String line = "";
		int count = 0;
		BufferedReader bf = new BufferedReader(new FileReader(file));
		ArrayList<ArrayList<String>> top = new ArrayList<>();
		while((line = bf.readLine())!=null){
			ArrayList<String> list = new ArrayList<>();
			String []temp = line.split("\\t");
			if(count==0){
				for (int i=0;i<temp.length;i++) {
					String sp = temp[i];
					if(i==0&&!sp.startsWith("#")){
						sp ="#"+sp;
					}else if(i!=0){
						if(sp.startsWith("#"))
							sp=sp.substring(1);
						if(sp.equals("ME1"))
							sp="GDA";
					}
					list.add(sp);	
				}
			}
			top.add(list);
		}
		bf.close();
		
		
		ArrayList<String> list = new ArrayList<>();
		StringBuffer strb = new StringBuffer();
		for(int i=0;i<top.get(0).size();i++){
			strb = new StringBuffer();
			for(int j=0;j<top.size();j++){
				ArrayList<String>temp = top.get(j);
				if(i==0||j==0)
					strb.append(temp.get(i)+"\t");
				else{
					int k = i;
					if(i>1)
						k=i*2-1;
					strb.append(temp.get(k)+"\t"+temp.get(k+1)+"\t");
				}
			}
			strb.append("\n");
			list.add(strb.toString());
		}
//		System.out.println(list.size());
		BufferedWriter bfr = new BufferedWriter(new FileWriter(file));
		bfr.write(list.get(0));
		bfr.write(list.get(5));
		bfr.write(list.get(1));
		bfr.write(list.get(2));
		bfr.write(list.get(3));
		bfr.write(list.get(4));
		bfr.close();
	}
}
