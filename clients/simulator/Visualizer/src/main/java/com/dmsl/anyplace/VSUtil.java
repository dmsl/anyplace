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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class VSUtil {

	public static ArrayList<String> loadFiles(String filename, ArrayList<String> files) {

		File directory = new File(filename);

		// get all the files from a directory

		File[] fList = directory.listFiles();

		for (File file : fList) {
			if (file.isFile()) {
				if (file.getName().contains(".typeDescription"))
					continue;
				String toks[] = file.getName().split("\\." + DataConnector.FILE_FORMAT);
				files.add(toks[0]);
			} else if (file.isDirectory()) {
				loadFiles(file.getAbsolutePath(), files);
			}
		}
		return files;
	}

	public static String getPair(String key, String value) {
		return key + ":" + value;
	}

	public static String likeJSON(List<String> all) {

		return all.stream().collect(Collectors.joining(",\t", "{", "}"));
	}
}
