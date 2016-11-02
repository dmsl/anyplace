/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Silouanos Nicolaou, Georgios Larkou, Kyriakos Georgiou
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;

public class RadioMapMean {

    private File RadiomapMean_File = null;
    private ArrayList<String> MacAdressList = null;
    private HashMap<String, ArrayList<String>> LocationRSS_HashMap = null;
    private HashMap<Integer, HashMap<String, ArrayList<String>>> GroupLocationRSS_HashMap = null;
    private ArrayList<String> OrderList = null;
    private final boolean isIndoor;
    private final int defaultNaNValue;

    public RadioMapMean(boolean isIndoor, int defaultNaNValue) {
        super();
        this.MacAdressList = new ArrayList<String>();
        this.GroupLocationRSS_HashMap = new HashMap<Integer, HashMap<String, ArrayList<String>>>(); 
        this.OrderList = new ArrayList<String>();
        this.isIndoor = isIndoor;
        this.defaultNaNValue = defaultNaNValue;
    }

    /**
     * Getter of Default NaN value
     *
     * @return
     *            NaN value
     * */
    public int getDefaultNaNValue() {
        return defaultNaNValue;
    }

    /**
     * Getter of MAC Address list in file order
     *
     * @return
     *            the list of MAC Addresses
     * */
    public ArrayList<String> getMacAdressList() {
        return MacAdressList;
    }

    /**
     * Getter of HashMap Location-RSS Values list in no particular order
     *
     * @return
     *            the HashMap Location-RSS Values
     * */
    public HashMap<String, ArrayList<String>> getLocationRSS_HashMap(int group) {
        return GroupLocationRSS_HashMap.get(group);
    }

    /**
     * Getter of Location list in file order
     *
     * @return
     *            the Location list
     * */
    public ArrayList<String> getOrderList() {
        return OrderList;
    }

    /**
     * Getter of radio map mean filename
     *
     * @return
     *            the filename of radiomap mean used
     * */
    public File getRadiomapMean_File() {
        return this.RadiomapMean_File;
    }

    /**
     * Construct a radio map
     *
     * @param inFile
     *            the radio map file to read
     *
     * @return
     *            true if radio map constructed successfully, otherwise false
     * */
    public boolean ConstructRadioMap(File inFile) {

        if (!inFile.exists() || !inFile.canRead()) {
            return false;
        }

        this.RadiomapMean_File = inFile;
        this.OrderList.clear();
        this.MacAdressList.clear();

        ArrayList<String> RSS_Values = null;
        BufferedReader reader = null;
        String line = null;
        String[] temp = null;
        int group = -1;
        String key = null;
        String lastKey = null;

        try {

            reader = new BufferedReader(new FileReader(inFile));

            int c = 0;

            while ((line = reader.readLine()) != null) {
                      c++; if( c == 33 ){
                    System.out.print("tet");
                }
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
                        return false;
                    } // Must be # X, Y
                    else if (this.isIndoor && (!temp[1].trim().equalsIgnoreCase("X") || !temp[2].trim().equalsIgnoreCase("Y"))) {
                        return false;
                    }
                    
                    // Store all Mac Addresses
                    for (int i = 4; i < temp.length; ++i) {
                        if (!temp[i].matches("[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}:[a-fA-F0-9]{2}")) {
                            return false;
                        }
                        this.MacAdressList.add(temp[i]);
                    }
                    continue;
                }

                key = temp[0] + " " + temp[1];
                
                group = Integer.parseInt(temp[2]);

                RSS_Values = new ArrayList<String>();

                for (int i = 3; i < temp.length; ++i) {
                    RSS_Values.add(temp[i]);
                }

                // Equal number of MAC address and RSS Values
                if (this.MacAdressList.size() != RSS_Values.size()) {
                    return false;
                }
                if( !key.equals(lastKey)) {
                    this.OrderList.add(key);
                    lastKey = key;                    
                }
                
                this.LocationRSS_HashMap = this.GroupLocationRSS_HashMap.get(group);
                
                if(this.LocationRSS_HashMap == null ) {
                    this.LocationRSS_HashMap = new HashMap<String, ArrayList<String>>();
                    this.LocationRSS_HashMap.put(key, RSS_Values);
                    this.GroupLocationRSS_HashMap.put(group, LocationRSS_HashMap);
                    continue;
                    
                }

                this.LocationRSS_HashMap.put(key, RSS_Values);
                
            }
            reader.close();
        } catch (Exception e) {
            System.err.println("Error while constructing RadioMap: " + "");e.printStackTrace();
            return false;
        }
        return true;
    }

    public String toString() {
        String str = "MAC Adresses: ";
        ArrayList<String> temp;


        for (int i = 0; i
                < MacAdressList.size();
                ++i) {
            str += MacAdressList.get(i) + " ";


        }

        str += "\nLocations\n";


        for (String location : LocationRSS_HashMap.keySet()) {
            str += location + " ";
            temp = LocationRSS_HashMap.get(location);


            for (int i = 0; i
                    < temp.size();
                    ++i) {
                str += temp.get(i) + " ";


            }
            str += "\n";


        }

        return str;

    }
}
