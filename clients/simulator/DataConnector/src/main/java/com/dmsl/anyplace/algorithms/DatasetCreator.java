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


import com.dmsl.anyplace.DataConnector;
import com.dmsl.anyplace.buildings.clean.CleanBuilding;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DatasetCreator {

    public static long TIME = System.currentTimeMillis();
    public int MAX_DATASETS = 30;
    public int MIN_DEPTH = 15;
    public int MAX_DEPTH = 15;
    public static String FILE_FORMAT = "csv";

    public ArrayList<List<Integer>> dataset;
    private String buid;
    public Random random;
    public int size;


    public DatasetCreator(String buid) throws Exception {
        this(buid, TIME, true);

    }

    public DatasetCreator(String buid, long time, boolean forceCreate) throws Exception {
        this.buid = buid;
        random = new Random(time);
        TIME = time;

        File path = new File(DataConnector.DATASETS_PATH + buid + "/");
        if (!path.exists()) {
            if(path.mkdirs())
                System.out.println("[Info] Directory: " + DataConnector.DATASETS_PATH + buid
                    + "/" + " created");

        }
        File file = new File(DataConnector.DATASETS_PATH + buid + "/" + "dataset" + "_"
                + MIN_DEPTH + "_" + MAX_DEPTH + "_" + MAX_DATASETS + "_" + time
                + "." + FILE_FORMAT);

        if (!file.exists()) {
            System.out.println("File does not exist! "+file.getName());
            if (forceCreate) {
                this.createDatasets();
                this.writeToFile(dataset);
                this.size = dataset.size();
            }
            return;
        }

        parseFile(file);
        this.size = dataset.size();
    }

    public DatasetCreator(String filename,String buid,long time)
            throws Exception {
        TIME =time;
        this.buid = buid;
        random = new Random();

        File file = new File(filename);

        parseFile(file);
        this.size = dataset.size();
    }


    /**
     * Parse the given file
     *
     * @param file
     * @throws Exception
     */
    private String fn;
    private void parseFile(File file) throws Exception {
        fn = file.getName();
        BufferedReader bf = new BufferedReader(new FileReader(file));
        String line = "";

        this.dataset = new ArrayList<>();
        List<Integer> temp;
        while ((line = bf.readLine()) != null) {
            temp = new ArrayList<>();
            String toks[] = line.split(",");
            for (String string : toks) {
                temp.add(Integer.parseInt(string));
            }
            dataset.add(temp);
        }
        bf.close();
    }

    /**
     * Create random data set using TIME seed
     */
    public void createDatasets() {
        createDatasets(MAX_DATASETS, MIN_DEPTH, MAX_DEPTH, TIME);

    }

    /**
     * Create random data set using specific time
     *
     * @param time
     */
    public void createDatasets(long time) {
        createDatasets(MAX_DATASETS, MIN_DEPTH, MAX_DEPTH, time);
    }

    public ArrayList<List<Integer>> createDatasets(int maxSet, int minDepth,
                                                   int maxDepth, long time) {

        dataset = new ArrayList<>();

        CleanBuilding building = null;
        try {
            building = new CleanBuilding(buid, false);
        } catch (Exception e) {
            e.printStackTrace();
            return null;

        }

        for (int i = 0; i < maxSet; i++) {
            int start = random.nextInt(building.getVertices().length);
            int depth = minDepth + random.nextInt((maxDepth - minDepth) + 1);

            ArrayList<List<Integer>> paths;
            while ((paths = building.BFS(start, depth)) == null
                    || paths.size() == 0) {
                start = random.nextInt(building.getVertices().length);
                depth = minDepth + random.nextInt((maxDepth - minDepth) + 1);
            }

            int getIndex = 0;
            if (paths.size() > 1) {
                getIndex = random.nextInt(paths.size());
            }


            dataset.add(paths.get(getIndex));
        }
        return dataset;
    }

    private boolean writeToFile(ArrayList<List<Integer>> dataset)
            throws IOException {


        String filename = DataConnector.DATASETS_PATH + buid+"/" + "dataset" + "_"
                + MIN_DEPTH + "_" + MAX_DEPTH + "_" + MAX_DATASETS + "_" + TIME
                + "." + FILE_FORMAT;
        File file = new File(filename);

        if (file.isDirectory()) {
            System.err.println("[Error]: This is a directory");
            return false;
        }
        if (file.exists()) {
            System.out.println("[Info]: File: " + filename
                    + " would be overwritten");
        }

        BufferedWriter bf = new BufferedWriter(new FileWriter(file));

        for (List<Integer> list : dataset) {
            bf.write(printList(list));
            bf.write("\n");
        }

        bf.close();
        System.out.println("[Info]: File: " + filename + " successfuly saved");

        return true;
    }

    /**
     * Return the datset
     * @return
     */
    public ArrayList<List<Integer>> getDataset() {
        return this.dataset;
    }

    /**
     * Helper Function to print list
     * @param list
     * @return
     */
    private String printList(List<Integer> list) {
        String str = "";
        int count = 0;
        for (Integer integer : list) {
            str += integer;
            if (count == list.size() - 1)
                break;
            str += ",";
            count++;
        }
        return str;
    }

    @Override
    public String toString() {
        String str = buid + "_dataset" + "_" + MIN_DEPTH + "_" + MAX_DEPTH
                + "_" + MAX_DATASETS + "_" + TIME;
        return str;
    }
}
