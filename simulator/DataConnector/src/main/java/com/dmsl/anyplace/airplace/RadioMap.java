/*
* AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
*
* Anyplace is a first-of-a-kind indoor information service offering GPS-less
* localization, navigation and search inside buildings using ordinary smartphones.
*
* Authors: C. Laoudias, G.Larkou, G. Constantinou, M. Constantinides, S. Nicolaou
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
* this software and associated documentation files (the "Software"), to deal in the
* Software without restriction, including without limitation the rights to use, copy,
* modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
* and to permit persons to whom the Software is furnished to do so, subject to the
* following conditions:
*
* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.
*
* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
* FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
* DEALINGS IN THE SOFTWARE.
*
*/

package com.dmsl.anyplace.airplace;

import com.dmsl.anyplace.DataConnector;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;


public class RadioMap {
    private String NaN = "-110";
    private File RadiomapMean_File = null;
    private ArrayList<String> MacAdressList = null;
    private HashMap<String, ArrayList<String>> LocationRSS_HashMap = null;
    private ArrayList<String> OrderList = null;
    private String heading ="";
    private int numOfFPS = 0;

    public RadioMap(File inFile) throws Exception {
        MacAdressList = new ArrayList<String>();
        LocationRSS_HashMap = new HashMap<String, ArrayList<String>>();
        OrderList = new ArrayList<String>();

        if (!ConstructRadioMap(inFile)) {
            throw new Exception("Inavlid Radiomap File");
        }
    }

    public RadioMap() {
        MacAdressList = new ArrayList<String>();
        LocationRSS_HashMap = new HashMap<String, ArrayList<String>>();
        OrderList = new ArrayList<String>();
    }

    public static Location localize(ArrayList<LogRecord> latestScanList, RadioMap rm, int algoChoice) {
        String calculatedLocation = Algorithms.ProcessingAlgorithms(latestScanList, rm, algoChoice);

        if (calculatedLocation == null) {
            System.out.println("Can't find location. Check that radio map file refers to the same area.");
            return null;
        } else {
            String[] temp = calculatedLocation.split(" ");
            return new Location(temp[0], temp[1]);
        }
    }

    /**
     * Getter of MAC Address list in file order
     *
     * @return the list of MAC Addresses
     */
    public ArrayList<String> getMacAdressList() {
        return MacAdressList;
    }

    /**
     * Getter of HashMap Location-RSS Values list in no particular order
     *
     * @return the HashMap Location-RSS Values
     */
    public HashMap<String, ArrayList<String>> getLocationRSS_HashMap() {
        return LocationRSS_HashMap;
    }

    /**
     * Getter of Location list in file order
     *
     * @return the Location list
     */
    public ArrayList<String> getOrderList() {
        return OrderList;
    }

    /**
     * Getter of radio map mean filename
     *
     * @return the filename of radiomap mean used
     */
    public File getRadiomapMean_File() {
        return this.RadiomapMean_File;
    }

    public String getNaN() {
        return NaN;
    }

    /**
     * Construct a radio map
     *
     * @param inFile the radio map file to read
     * @return true if radio map constructed successfully, otherwise false
     */
    public boolean ConstructRadioMap(File inFile) {

        if (!inFile.exists() || !inFile.canRead()) {
            return false;
        }

        this.RadiomapMean_File = inFile;
        this.OrderList.clear();
        this.MacAdressList.clear();
        this.LocationRSS_HashMap.clear();

        ArrayList<String> RSS_Values = null;
        BufferedReader reader = null;
        String line = null;
        String[] temp = null;
        String key = null;

        try {

            reader = new BufferedReader(new FileReader(inFile));

            // Read the first line # NaN -110
            line = reader.readLine();
            temp = line.split(" ");
            if (!temp[1].equals("NaN"))
                return false;
            NaN = temp[2];
            line = reader.readLine();

            // Must exists
            if (line == null)
                return false;

            line = line.replace(", ", " ");
            temp = line.split(" ");

            final int startOfRSS = 4;

            // Must have more than 4 fields
            if (temp.length < startOfRSS)
                return false;

            // Store all Mac Addresses Heading Added
            for (int i = startOfRSS; i < temp.length; ++i)
                this.MacAdressList.add(temp[i]);

            while ((line = reader.readLine()) != null) {

                if (line.trim().equals(""))
                    continue;

                line = line.replace(", ", " ");
                temp = line.split(" ");

                if (temp.length < startOfRSS)
                    return false;

                key = temp[0] + " " + temp[1];

                heading = temp[2];
                RSS_Values = new ArrayList<String>();

                for (int i = startOfRSS - 1; i < temp.length; ++i)
                    RSS_Values.add(temp[i]);

                // Equal number of MAC address and RSS Values
                if (this.MacAdressList.size() != RSS_Values.size())
                    return false;

                this.LocationRSS_HashMap.put(key, RSS_Values);

                this.OrderList.add(key);
                
                numOfFPS++;
            }

        } catch (Exception ex) {
        	ex.printStackTrace();
            return false;
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {

                }
        }
        return true;
    }

    public String toString() {
        String str = "MAC Adresses: ";
        ArrayList<String> temp;
        for (int i = 0; i < MacAdressList.size(); ++i)
            str += MacAdressList.get(i) + " ";

        str += "\nLocations\n";
        for (String location : LocationRSS_HashMap.keySet()) {
            str += location + " ";
            temp = LocationRSS_HashMap.get(location);
            for (int i = 0; i < temp.size(); ++i)
                str += temp.get(i) + " ";
            str += "\n";
        }

        return str;
    }

    public ArrayList<LogRecord> getListeningAccessPoints(String floor, String buid, String myLat, String myLon) throws IOException {
        double minDistance = Double.MAX_VALUE;
        String minDistanceKey = "";
        ArrayList<LogRecord> logs = new ArrayList<>();
        if (ConstructRadioMap(new File(DataConnector.FINGERPRINTS_PATH + buid + "/" + floor + ".fing_new"))) {
            for (String key : getOrderList()) {
                String[] location = key.split(" ");

                double dist = distance(Double.parseDouble(myLat), Double.parseDouble(myLon), Double.parseDouble(location[0]), Double.parseDouble(location[1]), 0, 0);
                if (dist < minDistance) {
                    minDistance = dist;
                    minDistanceKey = key;
                }
            }
        }

        //System.out.println(minDistanceKey+" "+heading);
        ArrayList<String> rss = getLocationRSS_HashMap().get(minDistanceKey);
        ArrayList<String> macAddress = getMacAdressList();
        if (rss != null)
            for (int i = 0; i < rss.size(); i++) {
                logs.add(new LogRecord(macAddress.get(i), (int) Double.parseDouble(rss.get(i))));
            }

        return logs;
    }

    /*
 * Calculate distance between two points in latitude and longitude taking
 * into account height difference. If you are not interested in height
 * difference pass 0.0. Uses Haversine method as its base.
 *
 * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
 * el2 End altitude in meters
 * @returns Distance in Meters
 */
    private double distance(double lat1, double lon1, double lat2,
                            double lon2, double el1, double el2) {

        final int R = 6371; // Radius of the earth

        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }

    public boolean ConstructRadioMap(String fps) {

        ArrayList<String> RSS_Values = null;
        BufferedReader reader = null;
        String line = null;
        String[] temp = null;
        String key = null;

        try {

            reader = new BufferedReader(new StringReader(fps));

            // Read the first line # NaN -110
            line = reader.readLine();
            temp = line.split(" ");
            if (!temp[1].equals("NaN"))
                return false;
            NaN = temp[2];
            line = reader.readLine();

            // Must exists
            if (line == null)
                return false;

            line = line.replace(", ", " ");
            temp = line.split(" ");

            final int startOfRSS = 4;

            // Must have more than 4 fields
            if (temp.length < startOfRSS)
                return false;

            // Store all Mac Addresses Heading Added
            for (int i = startOfRSS; i < temp.length; ++i){
               if(this.MacAdressList.contains(temp[i]))
            	   continue;
               else{
            	   this.MacAdressList.add(temp[i]);   
               }
            	
            }

            while ((line = reader.readLine()) != null) {

                if (line.trim().equals(""))
                    continue;

                line = line.replace(", ", " ");
                temp = line.split(" ");

                if (temp.length < startOfRSS)
                    return false;

                key = temp[0] + " " + temp[1];

                RSS_Values = new ArrayList<String>();

                for (int i = startOfRSS - 1; i < temp.length; ++i)
                    RSS_Values.add(temp[i]);

                // Equal number of MAC address and RSS Values
                if (this.MacAdressList.size() != RSS_Values.size())
                    return false;

                this.LocationRSS_HashMap.put(key, RSS_Values);

                this.OrderList.add(key);
                
                numOfFPS++;
            }

        } catch (Exception ex) {
            return false;
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {

                }
        }
        return true;
    }
    
    public int getNumOfFPS(){
    	return numOfFPS;
    }
}
