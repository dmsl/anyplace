/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s):
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

package radiomapserver;

import Jama.Matrix;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;

/**
 *
 * Class that constructs the radio map, reading all RSS files in RSS folder.
 * Writes to disk the radio map, radio map mean, RBF weights and parameters.
 */
public class RadioMap {

    private final HashMap<String, HashMap<String, ArrayList<Integer>>> RadioMap;
    private HashMap<String, HashMap<Integer,ArrayList<Object>>> NewRadioMap;
    private HashMap<Integer, ArrayList<Object>> orientationLists;
    private final boolean isIndoor;
    private final File rss_folder;
    private final String radiomap_filename;
    private final String radiomap_AP;
    private final String radiomap_mean_filename;
    private final String radiomap_rbf_weights_filename;
    private final String radiomap_parameters_filename;
    private final int defaultNaNValue;
    private double S_RBF = -1;
    private int MIN_RSS = Integer.MAX_VALUE;
    private int MAX_RSS = Integer.MIN_VALUE;

    /**
     * Constructor of the RadioMap class
     *
     * @param rss_folder
     *            the folder contains all RSS log files
     *
     * @param radiomap_filename
     *            the filename to write the radio map and use it to write other files
     *
     * */
    public RadioMap(File rss_folder, String radiomap_filename, String radiomap_AP, int defaultNaNValue) {
        RadioMap = new HashMap<String, HashMap<String, ArrayList<Integer>>>();
        NewRadioMap = new HashMap<String, HashMap<Integer,ArrayList<Object>>>();
        this.rss_folder = rss_folder;
        this.radiomap_filename = radiomap_filename;
        this.radiomap_AP = radiomap_AP;
        this.radiomap_mean_filename = radiomap_filename.replace(".", "-mean.");
        this.radiomap_rbf_weights_filename = radiomap_filename.replace(".", "-weights.");
        this.radiomap_parameters_filename = radiomap_filename.replace(".", "-parameters.");
        this.defaultNaNValue = defaultNaNValue;
        this.isIndoor = this.radiomap_filename.contains("indoor");
    }

    /**
     * Creates and writes the radio map to disk.
     *
     * @return
     *          true if radio map constructed and wrote to disk successfully, otherwise false
     * */
    public boolean createRadioMap() {

        if (!rss_folder.exists() || !rss_folder.isDirectory()) {
            return false;
        }

        RadioMap.clear();

        createRadioMapFromPath(rss_folder);

        //createRadioMapUrgent("radio-map.txt");

        if (!writeRadioMap()) {
            return false;
        }
        return true;
    }

    /**
     * Creates recursively the Radio map, reading all files in Folder
     *
     * @param inFile
     *             the new file to read
     * */
    private void createRadioMapFromPath(File inFile) {

        if (inFile.exists()) {

            // If is folder
            if (inFile.canExecute() && inFile.isDirectory()) {
                String[] list = inFile.list();

                // Read recursively the path
                if (list != null) {
                    for (int i = 0; i < list.length; i++) {
                        createRadioMapFromPath(new File(inFile, list[i]));
                    }
                }
            } // Parse all files
            else if (inFile.canRead() && inFile.isFile()) {
                parseLogFileToRadioMap(inFile);
            }
        }
    }

    /**
     * Parses an RSS log file and store it to radio map structure.
     *
     * @param inFile
     *             the new file to read
     * */
    public void parseLogFileToRadioMap(File inFile) {

        HashMap<String, ArrayList<Integer>> MACAddressMap = null;
        ArrayList<Integer> RSS_Values = null;
        ArrayList<ArrayList<Float>> MAG_Values = null;
        ArrayList<Object> orientationList = null;

        File f = inFile;
        BufferedReader reader = null;

        if(!authenticateRSSlogFile(f)) {
                return;
        }

        int group = 0;
        int line_num = 0;

        try {
            String line = null;
            String key = "";
            int degrees = 360;
            int num_orientations = 4;
            int range = degrees / num_orientations;
            float deviation = range / 2;
            
            int RSS_Value = 0;            
            
            FileReader fr = new FileReader(f);
            reader = new BufferedReader(fr);

            while ((line = reader.readLine()) != null) {

                line_num++;

                // Ignore the labels
                if (line.startsWith("#") || line.trim().isEmpty()) {
                    continue;
                }

                // Remove commas
                line = line.replace(", ", " ");

                // Split fields
                String[] temp = line.split(" ");

                // Test and set RSS value is integer
                RSS_Value = Integer.parseInt(temp[5]);

                // Key of location X,Y,MagX,MagY,MagZ
                key = temp[1] + ", " + temp[2];

                group = (int) (((Math.round((Float.parseFloat(temp[3]))+deviation)%degrees)/range)%num_orientations);

                // Get the current geolocation value
                orientationLists = NewRadioMap.get(key);
                
                //First Read
                if(orientationLists == null) {
                    orientationLists = new HashMap<Integer, ArrayList<Object>>(Math.round(num_orientations));
                    orientationList = new ArrayList<Object>(2);
                    orientationLists.put(group, orientationList);
                    MACAddressMap = new HashMap<String, ArrayList<Integer>>();
                    RSS_Values = new ArrayList<Integer>();
                    RSS_Values.add(RSS_Value);
                    MACAddressMap.put(temp[4].toLowerCase(), RSS_Values);
                    orientationList.add(MACAddressMap);
                    orientationList.add(0);
                    NewRadioMap.put(key, orientationLists);
                    continue;
                }
                		
                if(orientationLists.get(group) == null) {
                    orientationList = new ArrayList<Object>(2);
                    orientationLists.put(group, orientationList);
                    MACAddressMap = new HashMap<String, ArrayList<Integer>>();
                    RSS_Values = new ArrayList<Integer>();
                    RSS_Values.add(RSS_Value);
                    MACAddressMap.put(temp[4].toLowerCase(), RSS_Values);
                    orientationList.add(MACAddressMap);
                    orientationList.add(0);
                    NewRadioMap.put(key, orientationLists);
                    continue;
                }
                
                MACAddressMap = (HashMap<String, ArrayList<Integer>>) orientationLists.get(group).get(0);

                //Checking if our position is the same with the line we would like to save
                
                // Get the RSS Values of MAC address
                RSS_Values = MACAddressMap.get(temp[4].toLowerCase());
                
                int position = (Integer)orientationLists.get(group).get(1);
                
                if(RSS_Values == null) {
                    RSS_Values = new ArrayList<Integer>();
                }
                
                if ( position == RSS_Values.size()) {
                    position = position + 1;
                    orientationLists.get(group).set(1, position);

                    // MAC Address already exists. Just insert to array list the new RSS value
                    RSS_Values.add(RSS_Value);
                    MACAddressMap.put(temp[4].toLowerCase(), RSS_Values);
                }
                else {
                    for (int i = RSS_Values.size(); i < position-1; i++){
                        RSS_Values.add(this.defaultNaNValue);
                    }

                    // MAC Address already exists and consistent.
                    RSS_Values.add(RSS_Value);
                    MACAddressMap.put(temp[4].toLowerCase(), RSS_Values);
                }
            }
            fr.close();
            reader.close();
        } catch (Exception e) {
            System.err.println("Error while parsing RSS log file " + f.getAbsolutePath() + ": " + e.getMessage());
        }
    }

    /**
     * Parses an RSS log file and authenticates it
     *
     * @param inFile
     *             the RSS log file to read
     *
     * @return
     *              true if is authenticated, otherwise false
     *
     * */
    public static boolean authenticateRSSlogFile(File inFile) {

        int line_num = 0;
        BufferedReader reader = null;

        try {
            String line = null;
            FileReader fr = new FileReader(inFile);
            reader = new BufferedReader(fr);

            while ((line = reader.readLine()) != null) {

                line_num++;

                // Check X, Y or Latitude, Longitude
                if (line.startsWith("#")) {
                    continue;

                } else if (line.trim().isEmpty()) {
                    continue;
                }

                // Remove commas
                line = line.replace(", ", " ");

                // Split fields
                String[] temp = line.split(" ");

                //TODO:
                // # Timestamp, X, Y, HEADING, MAC Address of AP, RSS, Floor, BUID
//                if (temp.length != 8) {
//                    throw new Exception("Line " + line_num + " length is not equal to 8.");
//                }

                // Test that X, Y are floats
                Float.parseFloat(temp[1]);
                Float.parseFloat(temp[2]);
                
                // Test that HEADING is float
                Float.parseFloat(temp[3]);               
                

                // MAC address validation
                if (!temp[4].matches("[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}")) {
                    throw new Exception("Line " + line_num + " MAC Address is not valid.");
                }

                // Test RSS value is integer
                Integer.parseInt(temp[5]);

                // Test Floor value
                Integer.parseInt(temp[6]);
            }
            fr.close();
            reader.close();
        } catch (NumberFormatException nfe) {
            System.err.println("Error while authenticating RSS log file " + inFile.getAbsolutePath() + ": Line " + line_num + " " + nfe.getMessage());
            return false;
        } catch (Exception e) {
            System.err.println("Error while authenticating RSS log file " + inFile.getAbsolutePath() + ": " + e.getMessage());
            return false;
        }

        return true;
    }

    public static HashMap<String, LinkedList<String>> authenticateRSSlogFileAndReturnBuildingsFloors(File inFile) {

        int line_num = 0;
        BufferedReader reader = null;

        HashMap<String, LinkedList<String>> buildingsFloors = new HashMap<String, LinkedList<String>>();

        try {
            String line = null;
            FileReader fr = new FileReader(inFile);
            reader = new BufferedReader(fr);

            while ((line = reader.readLine()) != null) {

                line_num++;

                // Check X, Y or Latitude, Longitude
                if (line.startsWith("#")) {
                    continue;

                } else if (line.trim().isEmpty()) {
                    continue;
                }

                // Remove commas
                line = line.replace(", ", " ");

                // Split fields
                String[] temp = line.split(" ");

                //TODO:
                // # Timestamp, X, Y, HEADING, MAC Address of AP, RSS, Floor, BUID
                if (temp.length != 8) {
                    throw new Exception("Line " + line_num + " length is not equal to 8.");
                }

                // Test that X, Y are floats
                Float.parseFloat(temp[1]);
                Float.parseFloat(temp[2]);

                // Test that HEADING is float
                Float.parseFloat(temp[3]);


                // MAC address validation
                if (!temp[4].matches("[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}")) {
                    throw new Exception("Line " + line_num + " MAC Address is not valid.");
                }

                // Test RSS value is integer
                Integer.parseInt(temp[5]);

                // Test Floor value
                Integer.parseInt(temp[6]);

                if (!buildingsFloors.containsKey(temp[7])) {
                    LinkedList<String> tempList = new LinkedList<String>();
                    tempList.add(temp[6]);
                    buildingsFloors.put(temp[7], tempList);
                }
            }
            fr.close();
            reader.close();
        } catch (NumberFormatException nfe) {
            System.err.println("Error while authenticating RSS log file " + inFile.getAbsolutePath() + ": Line " + line_num + " " + nfe.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("Error while authenticating RSS log file " + inFile.getAbsolutePath() + ": " + e.getMessage());
            return null;
        }

        return buildingsFloors;
    }

    /****************************************************************************************************************/
    /****************************************************************************************************************/
    /****************************************************************************************************************/
    public boolean writeParameters(int orientations) {
        for( int i=0; i<orientations; i++ ) {
            
            int group = i * 360 / orientations;
            FileOutputStream fos = null;

            RadioMapMean RM = new RadioMapMean(this.isIndoor, this.defaultNaNValue);

            if (!RM.ConstructRadioMap(new File(radiomap_mean_filename))) {
                return false;
            }

            if (!find_MIN_MAX_Values(group)) {
                return false;
            }

            System.out.print("Calculating RBF parameter...");
            S_RBF = calculateSGreek(RM, group);
            if (S_RBF == -1) {
                return false;
            }
            System.out.print("Done!");

            File radiomap_parameters_file = new File(radiomap_parameters_filename);
            try {
                if (i == 0) {
                    fos = new FileOutputStream(radiomap_parameters_file, false);
                }
                else {
                    fos = new FileOutputStream(radiomap_parameters_file, true);
                }
            } catch (Exception e) {
                System.err.println("Error while writing parameters: " + e.getMessage());
                radiomap_parameters_file.delete();
                return false;
            }

            try {
                /* Start the print out to Parameters file */
                if(i != 0) {
                    fos.write("\n".getBytes());
                }
                else {
                    fos.write(("NaN, " + this.defaultNaNValue + "\n").getBytes());
                }
                fos.write(("RBF, " + group + ", " + this.S_RBF).getBytes());

                fos.close();

            } catch (Exception e) {
                System.err.println("Error while writing parameters: " + e.getMessage());
                radiomap_parameters_file.delete();
                return false;
            }
        }
        System.out.println("Written Parameters!");
        return true;
    }

    /**
     * Find Min and Max RSS value
     */
    private boolean find_MIN_MAX_Values(int group) {

        FileReader frRadiomap = null;
        BufferedReader readerRadiomap = null;

        try {
            String radiomapLine = null;
            String[] temp = null;

            frRadiomap = new FileReader(radiomap_filename);
            readerRadiomap = new BufferedReader(frRadiomap);


            while ((radiomapLine = readerRadiomap.readLine()) != null) {

                if (radiomapLine.trim().equals("")) {
                    continue;
                }

                radiomapLine = radiomapLine.replace(", ", " ");
                temp = radiomapLine.split(" ");
                
                if(temp[0].trim().equals("#")) {
                    if(temp[1].trim().equals("NaN")) {
                        continue;
                    }

                    // Must have more than 3 fields
                    if (temp.length < 5) {
                        return false;
                    } // Must be # X, Y
                    else if (this.isIndoor && (!temp[1].trim().equalsIgnoreCase("X") || !temp[2].trim().equalsIgnoreCase("Y"))) {
                        return false;
                    }
                    continue;
                }

                // The file may be corrupted so ignore reading it
                if (temp.length < 4) {
                    return false;
                }
                
                if(Integer.parseInt(temp[2]) == group) {
                    for (int i = 3; i < temp.length; ++i) {
                        set_MIN_MAX_RSS(Integer.parseInt(temp[i]));
                    }
                }
            }

            frRadiomap.close();
            readerRadiomap.close();

        } catch (Exception e) {
            System.err.println("Error while finding min and max RSS values: " + e.getMessage());
            return false;
        }

        return true;
    }

    /**
     * Sets MIN and MAX RSS Values to compute the parameters later for SNAP
     *
     * @param RSS_Value
     *                  the current RSS value
     * */
    private void set_MIN_MAX_RSS(int RSS_Value) {

        if (MIN_RSS > RSS_Value && RSS_Value != this.defaultNaNValue) {
            MIN_RSS = RSS_Value;
        }

        if (MAX_RSS < RSS_Value) {
            MAX_RSS = RSS_Value;
        }
    }

    private static Float calculateSGreek(RadioMapMean RM, int group) {

        if (RM == null) {
            return -1f;
        }

        float maximumDistance = 0.0f;
        ArrayList<Float> allDistances = new ArrayList<Float>();

        float result;

        for (int i = 0; i < RM.getOrderList().size(); ++i) {
            for (int j = i + 1; j < RM.getOrderList().size(); ++j) {
                HashMap<String, ArrayList<String>> RadioMapFile = RM.getLocationRSS_HashMap(group);
                if(RadioMapFile != null && RadioMapFile.get(RM.getOrderList().get(i)) != null && RadioMapFile.get(RM.getOrderList().get(j)) != null) {
                    result = calculateEuclideanDistance(RadioMapFile.get(RM.getOrderList().get(i)), RadioMapFile.get(RM.getOrderList().get(j)));

                    if (result == Float.NEGATIVE_INFINITY) {
                        return -1f;
                    }

                    allDistances.add(result);
                }
            }
        }

        maximumDistance = Collections.max(allDistances);
        return Float.valueOf((float) (maximumDistance / Math.sqrt(2 * RM.getOrderList().size())));

    }

    private double calculateEuclideanDistance(String real, String estimate) {

        double pos_error;
        String[] temp_real;
        String[] temp_estimate;
        double x1, x2;

        temp_real = real.split(" ");
        temp_estimate = estimate.split(" ");

        try {
            x1 = Math.pow((Double.parseDouble(temp_real[0]) - Double.parseDouble(temp_estimate[0])), 2);
            x2 = Math.pow((Double.parseDouble(temp_real[1]) - Double.parseDouble(temp_estimate[1])), 2);
        } catch (Exception e) {
            System.err.println("Error while calculating Euclidean distance: " + e.getMessage());
            return -1;
        }

        pos_error = Math.sqrt((x1 + x2));

        return pos_error;

    }

    /****************************************************************************************************************/
    /****************************************************************************************************************/
    /****************************************************************************************************************/
    /**
     * Write the new Radio Map
     * 
     * @return
     *              true if is written to disk, otherwise false
     * */
    private boolean writeRadioMap() {

        System.out.println("writing radio map to files");

        DecimalFormat dec = new DecimalFormat("###.#");
        HashMap<String, ArrayList<Integer>> MACAddressMap = null;
        ArrayList<Integer> RSS_Values = null;
        ArrayList<String> AP = new ArrayList<String>();
        FileOutputStream fos = null;
        FileOutputStream fos_mean = null;
        String out = null;
        
        int orientations = 4;

        File radiomap_file = new File(radiomap_filename);
        File radiomap_mean_file = new File(radiomap_mean_filename);

        // If is empty no RSS log file parsed
        if (NewRadioMap.isEmpty()) {
            return false;
        }
        
        File f = new File(radiomap_AP);
        BufferedReader reader = null;
        
        if(f.exists()) {
            try {
                String line = null;           

                FileReader fr = new FileReader(f);
                reader = new BufferedReader(fr);

                while ((line = reader.readLine()) != null) {
                    AP.add(line.toLowerCase());
                }
                fr.close();
                reader.close();
            } catch (Exception e) {
                System.err.println("Error while parsing AP txt file " + f.getAbsolutePath() + ": " + e.getMessage());
            }
        }

        // Open output streams
        try {
            fos = new FileOutputStream(radiomap_file, false);
            fos_mean = new FileOutputStream(radiomap_mean_file, false);
        } catch (FileNotFoundException e) {
            System.err.println("Error while writing radio map: " + e.getMessage());
            radiomap_file.delete();
            radiomap_mean_file.delete();
            return false;
        }


        try {

            int count = 0;
            int max_values = 0;
            int first = 0;
            
            String NaNValue = "# NaN " + this.defaultNaNValue + "\n";
            String header = "# X, Y, HEADING, ";
            fos.write(NaNValue.getBytes());
            fos_mean.write(NaNValue.getBytes());
            fos.write(header.getBytes());
            fos_mean.write(header.getBytes());

            ArrayList<String> MACKeys = new ArrayList<String>();
            int[] MRSS_Values = null;
            int heading = 0;
            String x_y = "";

            int group = 0;
            
            for (Entry<String, HashMap<Integer, ArrayList<Object>>> entry : NewRadioMap.entrySet())
            {
                int degrees = 360;

                for (Entry<Integer, ArrayList<Object>> orientationEntry : entry.getValue().entrySet())
                {

                    MACAddressMap = (HashMap<String, ArrayList<Integer>>) orientationEntry.getValue().get(0);

                    for (Entry<String, ArrayList<Integer>> MacEntry : MACAddressMap.entrySet())
                    {
                        String MACAddress = MacEntry.getKey();
                        if(!MACKeys.contains(MACAddress.toLowerCase())) {
                            if(AP.size() == 0 || AP.contains(MACAddress.toLowerCase())) {
                                MACKeys.add(MACAddress.toLowerCase());
                                if(first==0) {
                                    fos.write((MACAddress.toLowerCase()).getBytes());
                                    fos_mean.write((MACAddress.toLowerCase()).getBytes());
                                }
                                else {
                                    fos.write((", "+ MACAddress.toLowerCase()).getBytes());
                                    fos_mean.write((", "+ MACAddress.toLowerCase()).getBytes());
                                }
                                first++;
                            }
                        }
                    }
                }
            }

            for (Entry<String, HashMap<Integer, ArrayList<Object>>> entry : NewRadioMap.entrySet())
            {
                int degrees = 360;
                group = degrees/orientations;
                x_y = entry.getKey();

                for (Entry<Integer, ArrayList<Object>> orientationEntry : entry.getValue().entrySet())
                {
                    max_values = 0;
                    
                    heading = orientationEntry.getKey() * group;

                    MACAddressMap = (HashMap<String, ArrayList<Integer>>) orientationEntry.getValue().get(0);

                    for (Entry<String, ArrayList<Integer>> MacEntry : MACAddressMap.entrySet())
                    {
                        ArrayList<Integer> wifi_rss_values = MacEntry.getValue();

                        if (wifi_rss_values.size() > max_values) {
                                max_values = wifi_rss_values.size();
                        }
                    }
                    if(count==0) {
                        fos.write(("\n").getBytes());
                        fos_mean.write(("\n").getBytes());
                    }

                    MRSS_Values = new int[MACKeys.size()];
                    for (int v = 0; v<max_values; v++) {
                        fos.write((x_y + ", " + heading).getBytes());
                        for (int i=0; i<MACKeys.size(); i++) {
                                int rss_value = 0;
                                if (MACAddressMap.containsKey(MACKeys.get(i).toLowerCase()) ) {
                                        if (v >= MACAddressMap.get(MACKeys.get(i).toLowerCase()).size() && MACAddressMap.get(MACKeys.get(i).toLowerCase()).size() < max_values) {
                                                MRSS_Values[i] += this.defaultNaNValue;
                                                rss_value = this.defaultNaNValue;
                                        }
                                        else {
                                                rss_value = MACAddressMap.get(MACKeys.get(i).toLowerCase()).get(v);
                                                MRSS_Values[i] += rss_value;
                                        }
                                }
                                else {
                                        rss_value = this.defaultNaNValue;
                                        MRSS_Values[i] += this.defaultNaNValue;
                                }

                                //Print value
                                fos.write((", " + dec.format(rss_value)).getBytes());
                        }
                        fos.write("\n".getBytes());

                    }

                    fos_mean.write((x_y + ", " + heading).getBytes());
                    for (int i =0; i<MRSS_Values.length; i ++ ) {
                        fos_mean.write((", " + dec.format((float)MRSS_Values[i]/max_values)).getBytes());
                    }
                    fos_mean.write("\n".getBytes());
                    count++;

                }
            }                
            if (!writeParameters(orientations)) {
                return false;
            }
            if(!writeRBFWeights(orientations)) {
                // TODO: Was getting this error for some rss logs
                // Error while computing U+ matrix of RBF: Matrix is singular.
                // fix and uncoment line below
                // return false;
            }
            
            fos.close();
        } catch (ClassCastException cce) {
                System.err.println("Error1: " + cce.getMessage());
                return false;
        } catch (NumberFormatException nfe) {
                System.err.println("Error2: " + nfe.getMessage());
                return false;
        } catch (FileNotFoundException fnfe) {
                System.err.println("Error3: " + fnfe.getMessage());
                return false;
        } catch (IOException ioe) {
                System.err.println("Error4: " + ioe.getMessage());
                return false;
        }
        catch (Exception e) {
                System.err.println("Error5: " + e.getMessage());
                return false;
        }

        System.out.println("Finished writing radio map to files!");
        return true;
    }

    /**
     * Write the weights of RBF algorithm
     *
     * @return
     *              true if is written to disk, otherwise false
     * */
    public boolean writeRBFWeights(int orientation) {

        long start = System.currentTimeMillis();
        System.out.println("Writing RBF weights!");

        for( int x=0; x<orientation; x++ ) {
            int group = x * 360 / orientation;
            
            // Create (Nxl)xl matrix U
            Matrix MatrixU = create_U_matrix(group);
            if (MatrixU == null) {
                return false;
            }
            System.out.println("created U matrix! time[ " + (System.currentTimeMillis()-start) + "] ms" );

            // Create (Nxl)x2 matrix d
            Matrix Matrixd = create_d_matrix(MatrixU.getRowDimension(), group);
            if (Matrixd == null) {
                return false;
            }
            System.out.println("created matrix d! time[ " + (System.currentTimeMillis()-start) + "] ms" );

            // Compute U+ = (U_T * U)^-1 * U_T
            Matrix UPlus = computeUPlusMatrix(MatrixU);
            if (UPlus == null) {
                return false;
            }
            System.out.println("computed Plus matrix! time[ " + (System.currentTimeMillis()-start) + "] ms" );

            // Compute lx2 weights w = (wx, wy) using w = U+d
            Matrix w = computeWMatrix(UPlus, Matrixd);
            if (w == null) {
                return false;
            }
            System.out.println("computed W matrix! time[ " + (System.currentTimeMillis()-start) + "] ms" );

            DecimalFormat dec = new DecimalFormat("###.########");
            FileOutputStream fos = null;
            File radiomap_rbf_weights_file = new File(radiomap_rbf_weights_filename);

            try {
                if(x == 0) 
                    fos = new FileOutputStream(radiomap_rbf_weights_file, false);
                else 
                    fos = new FileOutputStream(radiomap_rbf_weights_file, true);
            } catch (FileNotFoundException e) {
                System.err.println("Error RBF weights: " + e.getMessage());
                radiomap_rbf_weights_file.delete();
                return false;
            }

            try {
                // Start the print out to rbf weights file
                if (x==0)
                    fos.write("# Heading wx, wy\n".getBytes());

                for (int i = 0; i < w.getRowDimension(); ++i) {
                    fos.write((group + ", ").getBytes());
                    for (int j = 0; j < w.getColumnDimension(); ++j) {
                        fos.write((dec.format(w.get(i, j))).getBytes());
                        if (j != w.getColumnDimension() - 1) {
                            fos.write(", ".getBytes());
                        }
                    }
                    fos.write("\n".getBytes());
                }

                fos.close();

            } catch (Exception e) {
                System.err.println("Error RBF weights: " + e.getMessage());
                radiomap_rbf_weights_file.delete();
                return false;
            }
        }

        System.out.println("Written RBF weights! time[ " + (System.currentTimeMillis()-start) + "] ms" );
        return true;
    }

    /**
     * Creates the Matrix U of RBF algorithm
     *
     * @return the U Matrix
     */
    private Matrix create_U_matrix(int orientation) {

        ArrayList<ArrayList<Double>> UArray = null;
        ArrayList<ArrayList<Double>> CArray = null;

        ArrayList<Double> Urow = null;

        // Fill array of RSS Values from Radio Map Mean
        CArray = fillCArray(orientation);
        if (CArray == null) {
            return null;
        }

        // Fill array U
        UArray = fillUArray(CArray, orientation);
        if (UArray == null) {
            return null;
        }

        // Find the dimensions of array U
        int row = UArray.size();
        int column = CArray.size();
        double[][] UArrayMatrix = new double[row][column];

        // Copy ArrayList U to 2d-array U
        for (int i = 0; i < row; ++i) {
            Urow = UArray.get(i);
            for (int j = 0; j < column; ++j) {
                UArrayMatrix[i][j] = Urow.get(j);
            }
        }

        // Return the new matrix U
        return new Matrix(UArrayMatrix);

    }

    /**
     * Creates the Array C from Radio Map Mean
     *
     * @return the C Array
     */
    private ArrayList<ArrayList<Double>> fillCArray(int orientation) {

        ArrayList<ArrayList<Double>> CArray = new ArrayList<ArrayList<Double>>();
        ArrayList<Double> CArrayLine = null;
        String[] temp = null;

        FileReader frRadiomapMean = null;
        BufferedReader readerRadiomapMean = null;

        try {
            frRadiomapMean = new FileReader(radiomap_mean_filename);
            readerRadiomapMean = new BufferedReader(frRadiomapMean);

            String line = null;
            while ((line = readerRadiomapMean.readLine()) != null) {

                if (line.trim().equals("")) {
                    continue;
                }

                line = line.replace(", ", " ");
                temp = line.split(" ");
                
                if(temp[0].trim().equals("#")) {
                    if(temp[1].trim().equals("NaN")) {
                        continue;
                    }

                    // Must have more than 3 fields
                    if (temp.length < 5) {
                        return null;
                    } // Must be # X, Y
                    else if (this.isIndoor && (!temp[1].trim().equalsIgnoreCase("X") || !temp[2].trim().equalsIgnoreCase("Y"))) {
                        return null;
                    }
                    continue;
                }

                // The file may be corrupted so ignore reading it
                if (temp.length < 4) {
                    return null;
                }

                CArrayLine = new ArrayList<Double>();

                if(Integer.parseInt(temp[2]) == orientation) {
                    for (int i = 3; i < temp.length; ++i) {
                        CArrayLine.add(Double.parseDouble(temp[i]));
                    }
                    
                    CArray.add(CArrayLine);
                }

                
            }
            frRadiomapMean.close();
            readerRadiomapMean.close();
        } catch (Exception e) {
            System.err.println("Error while populating C array of RBF: " + e.getMessage());
            CArray = null;
        }
        return CArray;
    }

    /**
     * Creates the Array U
     * Every position in the array is calculated by computeNumerator/computeDenominator
     * computeNumerator = exp(-1/2s^2 * || Si - Cj ||^2)
     * computeNumerator = S(j=1-l) exp(-1/2s^2 * || Si - Cj ||^2)
     * 
     * @param CArray the Array C of Radio Map Mean
     *
     * @return the U Array
     */
    private ArrayList<ArrayList<Double>> fillUArray(ArrayList<ArrayList<Double>> CArray, int orientation) {

        ArrayList<ArrayList<Double>> UArray = new ArrayList<ArrayList<Double>>();
        FileReader frRadiomap = null;
        BufferedReader readerRadiomap = null;
        ArrayList<Double> Srow = null;
        ArrayList<Double> Crow = null;
        ArrayList<Double> Urow = null;
        String[] temp = null;

        try {
            String radiomapLine = null;

            frRadiomap = new FileReader(radiomap_filename);
            readerRadiomap = new BufferedReader(frRadiomap);


            while ((radiomapLine = readerRadiomap.readLine()) != null) {

                if (radiomapLine.trim().equals("")) {
                    continue;
                }

                radiomapLine = radiomapLine.replace(", ", " ");
                temp = radiomapLine.split(" ");
                
                if(temp[0].trim().equals("#")) {
                    if(temp[1].trim().equals("NaN")) {
                        continue;
                    }

                    // Must have more than 3 fields
                    if (temp.length < 5) {
                        return null;
                    } // Must be # X, Y
                    else if (this.isIndoor && (!temp[1].trim().equalsIgnoreCase("X") || !temp[2].trim().equalsIgnoreCase("Y"))) {
                        return null;
                    }
                    continue;
                }

                // The file may be corrupted so ignore reading it
                if (temp.length < 4) {
                    return null;
                }
                
                if(Integer.parseInt(temp[2]) == orientation) {

                    Srow = new ArrayList<Double>();
                    for (int i = 3; i < temp.length; ++i) {
                        Srow.add(Double.parseDouble(temp[i]));
                        set_MIN_MAX_RSS(Integer.parseInt(temp[i]));
                    }

                    Urow = new ArrayList<Double>();

                    //Calculate the funtion u for every column on the row
                    for (int i = 0; i < CArray.size(); ++i) {
                        Crow = CArray.get(i);
                        Double numerator = computeNumerator(Srow, Crow);
                        Double denominator = computeDenominator(Srow, CArray);
                        Urow.add(numerator / denominator);
                    }

                    UArray.add(Urow);
                }
            }

            frRadiomap.close();
            readerRadiomap.close();

        } catch (Exception e) {
            System.err.println("Error while populating U array of RBF: " + e.getMessage());
            return null;
        }

        return UArray;
    }

    /**
     * Computes exp(-1/2s^2 * || Si - Cj ||^2)
     *
     * @param Srow a single row of RSS Values in Radiomap file
     * @param Crow a single row of RSS Values in Radiomap mean file
     *
     * @return the Numerator of u(Si,Cj) function
     */
    private Double computeNumerator(ArrayList<Double> Srow, ArrayList<Double> Crow) {

        Double Si = null;
        Double Cj = null;

        Double firstParameter = (-1 / (2 * Math.pow(S_RBF, 2)));
        Double secondParameter = 0.0d;

        for (int i = 0; i < Srow.size(); ++i) {
            Si = Srow.get(i);
            Cj = Crow.get(i);
            secondParameter += Math.pow((Si - Cj), 2);
        }

        return Math.exp(firstParameter * secondParameter);
    }

    /**
     * Computes S(j=1-l) exp(-1/2s^2 * || Si - Cj ||^2)
     *
     * @param Srow a single row of RSS Values in Radiomap file
     * @param C all rows of RSS Values in Radiomap mean file
     *
     * @return the Denominator of u(Si,Cj) function
     */
    private Double computeDenominator(ArrayList<Double> Srow, ArrayList<ArrayList<Double>> C) {

        ArrayList<Double> Crow = null;
        Double sum = 0.0d;

        for (int i = 0; i < C.size(); ++i) {
            Crow = C.get(i);
            sum += computeNumerator(Srow, Crow);
        }
        return sum;
    }

    /**
     * Creates the Matrix d of RBF algorithm
     *
     * @param rowDimension the number of rows == # locations on radio map
     *
     * @return the d Matrix
     */
    private Matrix create_d_matrix(int rowDimension, int orientation) {

        FileReader frRadiomap = null;
        BufferedReader readerRadiomap = null;
        String[] temp = null;

        // Find the dimensions of array U
        int row = rowDimension;
        int column = 2;
        double[][] dArrayMatrix = new double[row][column];

        int i = 0;

        try {
            String radiomapLine = null;

            frRadiomap = new FileReader(radiomap_filename);
            readerRadiomap = new BufferedReader(frRadiomap);


            while ((radiomapLine = readerRadiomap.readLine()) != null) {

                if (radiomapLine.trim().equals("")) {
                    continue;
                }

                radiomapLine = radiomapLine.replace(", ", " ");
                temp = radiomapLine.split(" ");
                
                if(temp[0].trim().equals("#")) {
                    if(temp[1].trim().equals("NaN")) {
                        continue;
                    }

                    // Must have more than 3 fields
                    if (temp.length < 5) {
                        return null;
                    } // Must be # X, Y
                    else if (this.isIndoor && (!temp[1].trim().equalsIgnoreCase("X") || !temp[2].trim().equalsIgnoreCase("Y"))) {
                        return null;
                    }
                    continue;
                }

                // The file may be corrupted so ignore reading it
                if (temp.length < 4) {
                    return null;
                }
                
                if(Integer.parseInt(temp[2]) == orientation) {
                    for (int k = 0; k < 2; ++k) {
                        dArrayMatrix[i][k] = Double.parseDouble(temp[k]);
                    }

                    ++i;
                }
            }

            frRadiomap.close();
            readerRadiomap.close();

        } catch (Exception e) {
            System.err.println("Error while creating d matrix of RBF: " + e.getMessage());
            return null;
        }

        return new Matrix(dArrayMatrix);
    }

    /**
     * Computes the U+ Matrix
     * 
     * U+ = (U_T * U)^-1 * U_T
     * 
     * @param MatrixU the matrix U
     * 
     * @return U+ Matrix
     */
    private Matrix computeUPlusMatrix(Matrix MatrixU) {
        try {
            Matrix Utranspose = MatrixU.transpose();
            Matrix UtransposeU = Utranspose.times(MatrixU);
            Matrix UtransposeUInverse = UtransposeU.inverse();
            return UtransposeUInverse.times(Utranspose);
        } catch (Exception e) {
            System.err.println("Error while computing U+ matrix of RBF: " + e.getMessage());
            return null;
        }
    }

    /**
     * Computes the w Matrix
     *
     * w = U+ * d
     *
     * @param UPlus the matrix U+
     * @param Matrixd the matrix d
     *
     * @return w Matrix
     */
    private Matrix computeWMatrix(Matrix UPlus, Matrix Matrixd) {
        Matrix w = null;
        try {
            w = UPlus.times(Matrixd);
            return w;
        } catch (Exception e) {
            System.err.println("Error while computing w matrix of RBF: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Calculates the Euclidean distance between the currently observed RSS
     * values and the RSS values for a specific location.
     *
     * @param l1
     *            RSS values of a location in radiomap
     * @param l2
     *            RSS values currently observed
     *
     * @return The Euclidean distance, or MIN_VALUE for error
     */
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
    

    /**
     * Prints the contents of Matrix m
     *
     * @param m the matrix to print
     */
    private void printMatrix(Matrix m) {
        for (int i = 0; i < m.getRowDimension(); ++i) {
            for (int j = 0; j < m.getColumnDimension(); ++j) {
                System.out.print(m.get(i, j) + " ");
            }
            System.out.println();
        }
    }
}
