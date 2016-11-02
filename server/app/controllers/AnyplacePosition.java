/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Lambros Petrou, Kyriakos Georgiou
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

package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import datasources.DatasourceException;
import datasources.ProxyDataSource;
import db_models.Floor;
import db_models.MagneticMilestone;
import db_models.MagneticPath;
import db_models.RadioMapRaw;
import floor_module.Algo1;
import oauth.provider.v2.models.OAuth2Request;
import org.springframework.util.StopWatch;
import play.libs.Akka;
import play.libs.F;
import play.mvc.Controller;
import play.mvc.Http;
import play.mvc.Result;
import radiomapserver.RadioMap;
import utils.*;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;

public class AnyplacePosition extends Controller {

    /**
     * Uploads a radio_map file and stores it at the server system!
     * <p>
     * Format:
     * radiomap: The file with the rss logs
     * json: The json document with necessary information
     *
     * @return
     */
    public static Result radioUpload() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());

        Http.MultipartFormData body = anyReq.getMultipartFormData();
        if (body == null) {
            return AnyResponseHelper.bad_request("Invalid request type - Not Multipart!");
        }
        final Http.MultipartFormData.FilePart radioFile = body.getFile("radiomap");

        if (radioFile == null) {
            return AnyResponseHelper.bad_request("Cannot find the rss file (radiomap)!");
        }
        LPLogger.info("Radio Upload File: " + radioFile.getFile().getAbsolutePath());

        Map<String, String[]> body_form = body.asFormUrlEncoded();
        if (body_form == null) {
            return AnyResponseHelper.bad_request("Invalid request type - Cannot be parsed as form data!");
        }

        if (body_form.get("json") == null) {
            return AnyResponseHelper.bad_request("Cannot find json in the request!");
        }

        String json_str = body_form.get("json")[0];
        LPLogger.info("Radio Upload json: " + json_str);

        if (json_str == null) {
            return AnyResponseHelper.bad_request("Cannot find json in the request!");
        }

        JsonNode json = null;
        try {
            json = JsonUtils.getJsonTree(json_str);
        } catch (IOException e) {
            return AnyResponseHelper.bad_request("Cannot parse json request!");
        }

        if (json.get("username") == null || json.get("password") == null) {
            return AnyResponseHelper.bad_request("Cannot parse json request!");
        }

        String username = json.get("username").asText();
        String password = json.get("password").asText();

        if (null == username || null == password) {
            return AnyResponseHelper.bad_request("Null username or password");
        }

        boolean floorFlag = false;
        if ((username.equals("anyplace") && password.equals("floor")) || (username.equals("anonymous") && password.equals("anonymous"))) {
            floorFlag = true;
        } else if (username.equals("anyplace") && password.equals("123anyplace123rss")) {
            floorFlag = false;
        } else {
            return AnyResponseHelper.forbidden("Invalid username or password");
        }

        // TODO - here check the fields if any from the request
        // TODO - The OAUTH2 check will go here

        final HashMap<String, LinkedList<String>> newBuildingsFloors = RadioMap.authenticateRSSlogFileAndReturnBuildingsFloors(radioFile.getFile());
        if (newBuildingsFloors == null) {
            return AnyResponseHelper.bad_request("Corrupted radio file uploaded!");
        } else {
            // TODO - Decide what to do with uploaded raw radio map file
            HelperMethods.storeRadioMapToServer(radioFile.getFile());
            String errorMsg = null;

            F.Promise<Integer> promiseOfInt = Akka.future(
                    new Callable<Integer>() {
                        public Integer call() {
                            storeFloorAlgoToDB(radioFile.getFile());

                            // updated frozen radiomaps from fresh rss-log
                            for (String nBuilding : newBuildingsFloors.keySet()) {
                                LinkedList<String> bFloors = newBuildingsFloors.get(nBuilding);
                                for (String bFloor : bFloors) {
                                    updateFrozenRadioMap(nBuilding, bFloor);
                                }
                            }

                            return 0;
                        }
                    }
            );

        }

        return AnyResponseHelper.ok("Successfully uploaded rss log.");
    }

    /**
     * Returns a link to the radio map that needs to be downloaded according to the specified coordinates
     *
     * @return a link to the radio_map file
     */
    public static Result radioDownloadFloor() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplacePosition::radioDownloadFloor(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "coordinates_lat",
                "coordinates_lon",
                "floor_number"
        );
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        String lat = json.findPath("coordinates_lat").textValue();
        String lon = json.findPath("coordinates_lon").textValue();
        String floor_number = json.findPath("floor_number").textValue();
        String mode = json.findPath("mode").toString();

        if (!Floor.checkFloorNumberFormat(floor_number)) {
            return AnyResponseHelper.bad_request("Floor number cannot contain whitespace!");
        } else {

            GeoPoint bbox[] = GeoPoint.getGeoBoundingBox(Double.parseDouble(lat), Double.parseDouble(lon), 500);
            LPLogger.info("LowerLeft: " + bbox[0] + " UpperRight: " + bbox[1]);
            //query.setBbox(-180, -90, 180, 90);

            // prepare the file structure that will hold the files for this radiomap request
            File dir = new File("radiomaps" + File.separatorChar + LPUtils.generateRandomToken() + "_" + System.currentTimeMillis());
            if (!dir.mkdirs()) {
                return null;
            }
            File radio = new File(dir.getAbsolutePath() + File.separatorChar + "rss-log");
            FileOutputStream fout;
            try {
                fout = new FileOutputStream(radio);
                System.out.println(radio.toPath().getFileName());
            } catch (FileNotFoundException e) {
                return AnyResponseHelper.internal_server_error("Cannot create radio map due to Server FileIO error!");
            }

            // DUMP ALL THE RADIO RSS LOG ENTRIES INTO THE RADIO FILE
            long floorFetched;
            try {
                floorFetched = ProxyDataSource.getIDatasource().dumpRssLogEntriesSpatial(fout, bbox, floor_number);
                try {
                    fout.close();
                } catch (IOException e) {
                    LPLogger.error("Error while closing the file output stream for the dumped rss logs");
                }
            } catch (DatasourceException e) {
                return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
            }
            // if there are no entries for this area
            if (floorFetched == 0) {
                return AnyResponseHelper.bad_request("Area not supported yet!");
            }

            try {
                String folder = dir.toString();

                String radiomap_filename = new File(folder + File.separatorChar + "indoor-radiomap.txt").getAbsolutePath();
                String radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt");
                String radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt");
                String radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt");

                // create the radiomap using the input rss log file
                RadioMap rm = new RadioMap(new File(folder), radiomap_filename, "", -110);
                if (!rm.createRadioMap()) {
                    return AnyResponseHelper.internal_server_error("Error while creating Radio Map on-the-fly!");
                }

                // fix the paths
                // radiomaps is the folder where the folders reside in
                String api = AnyplaceServerAPI.SERVER_API_ROOT;
                int pos = radiomap_mean_filename.indexOf("radiomaps");
                radiomap_mean_filename = api + radiomap_mean_filename.substring(pos);

                pos = radiomap_rbf_weights_filename.indexOf("radiomaps");
                radiomap_rbf_weights_filename = api + radiomap_rbf_weights_filename.substring(pos);

                pos = radiomap_parameters_filename.indexOf("radiomaps");
                radiomap_parameters_filename = api + radiomap_parameters_filename.substring(pos);

                // everything is ok
                ObjectNode res = JsonUtils.createObjectNode();
                res.put("map_url_mean", radiomap_mean_filename);
                res.put("map_url_weights", radiomap_rbf_weights_filename);
                res.put("map_url_parameters", radiomap_parameters_filename);

                return AnyResponseHelper.ok(res, "Successfully created radio map.");
            } catch (Exception e) {
                // no exception is expected to be thrown but just in case
                return AnyResponseHelper.internal_server_error("Error while creating Radio Map on-the-fly! : " + e.getMessage());
            }
        }
    }

    /**
     * Returns a link to the radio map that needs to be downloaded according to the specified buid and floor
     *
     * @return a link to the radio_map file
     */
    public static Result radioDownloadByBuildingFloor() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplacePosition::radioDownloadFloor(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "floor",
                "buid"
        );

        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        String floor_number = json.findPath("floor").textValue();
        String buid = json.findPath("buid").textValue();
        String mode = json.findPath("mode").toString();

        if (!Floor.checkFloorNumberFormat(floor_number)) {
            return AnyResponseHelper.bad_request("Floor number cannot contain whitespace!");
        }

        File rmapDir = new File("radiomaps_frozen" + File.separatorChar + buid + File.separatorChar + floor_number);

        File radiomapFile = new File("radiomaps_frozen" + File.separatorChar + buid + File.separatorChar + floor_number + File.separatorChar + "indoor-radiomap.txt");
        File meanFile = new File("radiomaps_frozen" + File.separatorChar + buid + File.separatorChar + floor_number + File.separatorChar + "indoor-radiomap-mean.txt");

        if (rmapDir.exists() && radiomapFile.exists() && meanFile.exists()) {
            try {
                String folder = rmapDir.toString();

                String radiomap_filename = new File(folder + File.separatorChar + "indoor-radiomap.txt").getAbsolutePath();
                String radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt");
                String radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt");
                String radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt");

                // fix the paths
                // frozen_radiomaps is the folder where the folders reside in
                String api = AnyplaceServerAPI.SERVER_API_ROOT;
                int pos = radiomap_mean_filename.indexOf("radiomaps_frozen");
                radiomap_mean_filename = api + radiomap_mean_filename.substring(pos);

                pos = radiomap_rbf_weights_filename.indexOf("radiomaps_frozen");
                radiomap_rbf_weights_filename = api + radiomap_rbf_weights_filename.substring(pos);

                pos = radiomap_parameters_filename.indexOf("radiomaps_frozen");
                radiomap_parameters_filename = api + radiomap_parameters_filename.substring(pos);

                // everything is ok
                ObjectNode res = JsonUtils.createObjectNode();
                res.put("map_url_mean", radiomap_mean_filename);
                res.put("map_url_weights", radiomap_rbf_weights_filename);
                res.put("map_url_parameters", radiomap_parameters_filename);

                return AnyResponseHelper.ok(res, "Successfully served radio map.");
            } catch (Exception e) {
                // no exception is expected to be thrown but just in case
                return AnyResponseHelper.internal_server_error("Error serving radiomap : " + e.getMessage());
            }
        }

        // prepare the file structure that will hold the files for this radiomap request
        // File dir = new File("radiomaps" + File.separatorChar + LPUtils.generateRandomToken() + "_" + System.currentTimeMillis());

        if (!rmapDir.mkdirs()) {
            return AnyResponseHelper.internal_server_error("Error while creating Radio Map on-the-fly!");
        }

        File radio = new File(rmapDir.getAbsolutePath() + File.separatorChar + "rss-log");
        FileOutputStream fout;
        try {
            fout = new FileOutputStream(radio);
            System.out.println(radio.toPath().getFileName());
        } catch (FileNotFoundException e) {
            return AnyResponseHelper.internal_server_error("Cannot create radio map due to Server FileIO error!");
        }

        // DUMP ALL THE RADIO RSS LOG ENTRIES INTO THE RADIO FILE
        long floorFetched;
        try {
            floorFetched = ProxyDataSource.getIDatasource().dumpRssLogEntriesByBuildingFloor(fout, buid, floor_number);
            try {
                fout.close();
            } catch (IOException e) {
                LPLogger.error("Error while closing the file output stream for the dumped rss logs");
            }
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
        // if there are no entries for this area
        if (floorFetched == 0) {
            return AnyResponseHelper.bad_request("Area not supported yet!");
        }

        try {
            String folder = rmapDir.toString();

            String radiomap_filename = new File(folder + File.separatorChar + "indoor-radiomap.txt").getAbsolutePath();
            String radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt");
            String radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt");
            String radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt");

            // create the radiomap using the input rss log file
            RadioMap rm = new RadioMap(new File(folder), radiomap_filename, "", -110);
            if (!rm.createRadioMap()) {
                return AnyResponseHelper.internal_server_error("Error while creating Radio Map on-the-fly!");
            }

            // fix the paths
            // radiomaps is the folder where the folders reside in
            String api = AnyplaceServerAPI.SERVER_API_ROOT;
            int pos = radiomap_mean_filename.indexOf("radiomaps_frozen");
            radiomap_mean_filename = api + radiomap_mean_filename.substring(pos);

            pos = radiomap_rbf_weights_filename.indexOf("radiomaps_frozen");
            radiomap_rbf_weights_filename = api + radiomap_rbf_weights_filename.substring(pos);

            pos = radiomap_parameters_filename.indexOf("radiomaps_frozen");
            radiomap_parameters_filename = api + radiomap_parameters_filename.substring(pos);

            // everything is ok
            ObjectNode res = JsonUtils.createObjectNode();
            res.put("map_url_mean", radiomap_mean_filename);
            res.put("map_url_weights", radiomap_rbf_weights_filename);
            res.put("map_url_parameters", radiomap_parameters_filename);

            return AnyResponseHelper.ok(res, "Successfully created radio map.");
        } catch (Exception e) {
            // no exception is expected to be thrown but just in case
            return AnyResponseHelper.internal_server_error("Error while creating Radio Map on-the-fly! : " + e.getMessage());
        }

    }

    /**
     * Saves each line of the Radio Map file as a separate document inside the database.
     *
     * @param infile The radio map entry file that should be stored in db
     * @return Returns an error string describing the error occurred or null if success
     */
    private static String storeRadioMapToDB(File infile) {
        String line;
        FileReader fr = null;
        BufferedReader bf = null;
        long lineNumber = 0;
        try {
            fr = new FileReader(infile);
            bf = new BufferedReader(fr);

            while ((line = bf.readLine()) != null) {
                if (line.startsWith("# Timestamp"))
                    continue;

                lineNumber++;

                String[] segs = line.split(" ");
                // added segs[6] which is the floor

                // TODO - add the building too

                RadioMapRaw rmr = new RadioMapRaw(segs[0], segs[1], segs[2], segs[3], segs[4], segs[5], segs[6]);
                LPLogger.info(rmr.toValidCouchJson());

                LPLogger.debug("raw[" + lineNumber + "] : " + rmr.toValidCouchJson());

                try {
                    if (!ProxyDataSource.getIDatasource().addJsonDocument(rmr.getId(), 0, rmr.toCouchGeoJSON())) {
                        return "Radio Map entry could not be saved in database![could not be created]";
                    }
                } catch (DatasourceException e) {
                    return "Internal server error while trying to save rss entry.";
                }
            }// end while

        } catch (FileNotFoundException e) {
            return "Internal server error: Error while storing rss log.";
        } catch (IOException e) {
            return "Internal server error: Error while storing rss log.";
        } finally {
            try {
                if (fr != null) fr.close();
                if (bf != null) bf.close();
            } catch (IOException e) {
                return "Internal server error: Error while storing rss log.";
            }
        }
        // null means success
        return null;
    }

    /**
     * Returns the file requested or an error
     *
     * @param radio_folder The folder of the user the file is inside
     * @param fileName     The actual file requested
     * @return Either the file or an error in JSON format with properties { "status": "error", "message":"error message here"}
     */
    public static Result serveRadioMap(String radio_folder, String fileName) {
        // send false parameter to disable CORS
        OAuth2Request anyReq = new OAuth2Request(request(), response(), false);
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplacePosition::serveRadioMap(): " + json.toString());

        String filePath = "radiomaps" + File.separatorChar + radio_folder + File.separatorChar + fileName;

        LPLogger.info("requested: " + filePath);

        File file = new java.io.File(filePath);
        try {
            if (!file.exists() || !file.canRead()) {
                return AnyResponseHelper.bad_request("Requested file does not exist or cannot be read! (" + fileName + ")");
            }

            InputStream a = new FileInputStream(file);
            return ok(a);
        } catch (FileNotFoundException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    public static Result serveFrozenRadioMap(String building, String floor, String fileName) {

        String filePath = "radiomaps_frozen" + File.separatorChar + building + File.separatorChar + floor + File.separatorChar + fileName;

        LPLogger.info("requested: " + filePath);

        File file = new java.io.File(filePath);
        try {
            if (!file.exists() || !file.canRead()) {
                return AnyResponseHelper.bad_request("Requested file does not exist or cannot be read! (" + fileName + ")");
            }

            InputStream a = new FileInputStream(file);
            return ok(a);
        } catch (FileNotFoundException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    private static String storeFloorAlgoToDB_Help(ArrayList<String> values) {
        if (values.size() > 0) {
            String maxMac = "";
            int maxRss = Integer.MIN_VALUE;

            for (int i = 0; i < values.size(); i++) {
                String value = values.get(i);
                String[] segs = value.split(" ");
                try {
                    String mac = segs[4];
                    int rss = Integer.parseInt(segs[5]);

                    if (rss > maxRss) {
                        maxRss = rss;
                        maxMac = mac;
                    }
                } catch (NumberFormatException ex) {
                    if (values.remove(value))
                        i--;

                }
            }

            for (String value : values) {

                String[] segs = value.split(" ");

                // added segs[6] which is the floor
                // added segs[7] which is the buid

                RadioMapRaw rmr = null;

                if (segs.length >= 8) {
                    rmr = new RadioMapRaw(segs[0], segs[1], segs[2],
                            segs[3], segs[4], segs[5], segs[6], maxMac, segs[7]);
                } else if (segs.length >= 7) {
                    rmr = new RadioMapRaw(segs[0], segs[1], segs[2],
                            segs[3], segs[4], segs[5], segs[6], maxMac);
                } else {
                    return "Some fields are missing from the log.";
                }

                LPLogger.info(rmr.toValidCouchJson());

                try {
                    if (!ProxyDataSource.getIDatasource().addJsonDocument(
                            rmr.getId(), 0, rmr.toCouchGeoJSON())) {
                        LPLogger.info("Radio Map entry was not saved in database![Possible duplicate]");
                    }
                } catch (DatasourceException e) {
                    return "Internal server error while trying to save rss entry.";
                }
            }

        }

        // null means success
        return null;
    }

    /**
     * Saves each line of the Radio Map file as a separate document inside the
     * database.
     *
     * @param infile The radio map entry file that should be stored in db
     * @return Returns an error string describing the error occurred or null if
     * success
     */
    private static String storeFloorAlgoToDB(File infile) {
        String line;
        FileReader fr = null;
        BufferedReader bf = null;
        try {
            fr = new FileReader(infile);
            bf = new BufferedReader(fr);

            ArrayList<String> values = new ArrayList<String>(10);

            while ((line = bf.readLine()) != null) {
                if (line.startsWith("# Timestamp")) {
                    String result = storeFloorAlgoToDB_Help(values);
                    if (result != null)
                        return result;
                    values.clear();
                } else {
                    values.add(line);

                }

            }// end while

            String result = storeFloorAlgoToDB_Help(values);
            if (result != null)
                return result;

        } catch (FileNotFoundException e) {
            return "Internal server error: Error while storing rss log.";
        } catch (IOException e) {
            return "Internal server error: Error while storing rss log.";
        } finally {
            try {
                if (fr != null)
                    fr.close();
                if (bf != null)
                    bf.close();
            } catch (IOException e) {
                return "Internal server error: Error while storing rss log.";
            }
        }
        // null means success
        return null;
    }

    public static Result predictFloorAlgo1() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper
                    .bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();

        StopWatch watch = new StopWatch();
        watch.start();

        LPLogger.info("AnyplacePosition::predictFloor(): ");

        Algo1 alg1;
        try {
            alg1 = new Algo1(json);
        } catch (Exception ex) {
            return AnyResponseHelper.bad_request(ex.getMessage());
        }

        try {
            double lat = json.get("dlat").asDouble();
            double lot = json.get("dlong").asDouble();
            GeoPoint bbox[] = GeoPoint.getGeoBoundingBox(lat, lot, 100);
            ArrayList<String> strongestMAC = new ArrayList<String>(2);

            if (json.get("first") == null)
                return AnyResponseHelper.bad_request("Sent first Wifi");

            strongestMAC.add(json.get("first").get("MAC").asText());

            if (json.get("second") != null)
                strongestMAC.add(json.get("second").get("MAC").asText());

            ObjectNode res = JsonUtils.createObjectNode();

            if (ProxyDataSource.getIDatasource().predictFloor(alg1, bbox,
                    strongestMAC.toArray(new String[1]))) {
                res.put("floor", alg1.getFloor());

            } else {
                res.put("floor", "");
            }

            watch.stop();
            LPLogger.info("Time for Algo1 is millis: "
                    + watch.getTotalTimeMillis());

            return AnyResponseHelper.ok(res, "Successfully predicted Floor.");

        } catch (Exception e) {
            return AnyResponseHelper
                    .internal_server_error("Server Internal Error ["
                            + e.getMessage() + "]");
        }
    }

    private static void updateFrozenRadioMap(String buid, String floor_number) {

        if (!Floor.checkFloorNumberFormat(floor_number)) {
            return;
        }

        File rmapDir = new File("radiomaps_frozen" + File.separatorChar + buid + File.separatorChar + floor_number);

        if (!rmapDir.exists() && !rmapDir.mkdirs()) {
            return;
        }

        File radio = new File(rmapDir.getAbsolutePath() + File.separatorChar + "rss-log");
        FileOutputStream fout;
        try {
            fout = new FileOutputStream(radio);
            System.out.println(radio.toPath().getFileName());
        } catch (FileNotFoundException e) {
            return;
        }

        // DUMP ALL THE RADIO RSS LOG ENTRIES INTO THE RADIO FILE
        long floorFetched;
        try {
            floorFetched = ProxyDataSource.getIDatasource().dumpRssLogEntriesByBuildingFloor(fout, buid, floor_number);
            try {
                fout.close();
            } catch (IOException e) {
                LPLogger.error("Error while closing the file output stream for the dumped rss logs");
            }
        } catch (DatasourceException e) {
            return;
        }

        // if there are no entries for this area
        if (floorFetched == 0) {
            return;
        }

        try {
            String folder = rmapDir.toString();

            String radiomap_filename = new File(folder + File.separatorChar + "indoor-radiomap.txt").getAbsolutePath();
            String radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt");
            String radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt");
            String radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt");

            // create the radiomap using the input rss log file
            RadioMap rm = new RadioMap(new File(folder), radiomap_filename, "", -110);
            if (!rm.createRadioMap()) {
                return;
            }

            // fix the paths
            // radiomaps is the folder where the folders reside in
            String api = AnyplaceServerAPI.SERVER_API_ROOT;
            int pos = radiomap_mean_filename.indexOf("radiomaps_frozen");
            radiomap_mean_filename = api + radiomap_mean_filename.substring(pos);

            pos = radiomap_rbf_weights_filename.indexOf("radiomaps_frozen");
            radiomap_rbf_weights_filename = api + radiomap_rbf_weights_filename.substring(pos);

            pos = radiomap_parameters_filename.indexOf("radiomaps_frozen");
            radiomap_parameters_filename = api + radiomap_parameters_filename.substring(pos);

            // everything is ok
            ObjectNode res = JsonUtils.createObjectNode();
            res.put("map_url_mean", radiomap_mean_filename);
            res.put("map_url_weights", radiomap_rbf_weights_filename);
            res.put("map_url_parameters", radiomap_parameters_filename);

            return;
        } catch (Exception e) {
            // no exception is expected to be thrown but just in case
            return;
        }

    }

    public static Result radioDownloadFloorBbox() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplacePosition::radioDownloadFloor(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "coordinates_lat",
                "coordinates_lon",
                "floor_number",
                "range"
        );
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        String lat = json.findPath("coordinates_lat").textValue();
        String lon = json.findPath("coordinates_lon").textValue();
        String floor_number = json.findPath("floor_number").textValue();
        String strRange = json.findPath("range").textValue();

        int range = Integer.parseInt(strRange);

        if (!Floor.checkFloorNumberFormat(floor_number)) {
            return AnyResponseHelper.bad_request("Floor number cannot contain whitespace!");
        } else {
            GeoPoint bbox[] = GeoPoint.getGeoBoundingBox(Double.parseDouble(lat), Double.parseDouble(lon), range);
            LPLogger.info("LowerLeft: " + bbox[0] + " UpperRight: " + bbox[1]);

            // prepare the file structure that will hold the files for this radiomap request
            File dir = new File("radiomaps" + File.separatorChar + LPUtils.generateRandomToken() + "_" + System.currentTimeMillis());
            if (!dir.mkdirs()) {
                return null;
            }
            File radio = new File(dir.getAbsolutePath() + File.separatorChar + "rss-log");
            FileOutputStream fout;
            try {
                fout = new FileOutputStream(radio);
                System.out.println(radio.toPath().getFileName());
            } catch (FileNotFoundException e) {
                return AnyResponseHelper.internal_server_error("Cannot create radio map due to Server FileIO error!");
            }

            // DUMP ALL THE RADIO RSS LOG ENTRIES INTO THE RADIO FILE
            long floorFetched;
            try {
                floorFetched = ProxyDataSource.getIDatasource().dumpRssLogEntriesSpatial(fout, bbox, floor_number);
                try {
                    fout.close();
                } catch (IOException e) {
                    LPLogger.error("Error while closing the file output stream for the dumped rss logs");
                }
            } catch (DatasourceException e) {
                return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
            }
            // if there are no entries for this area
            if (floorFetched == 0) {
                return AnyResponseHelper.bad_request("Area not supported yet!");
            }

            try {
                String folder = dir.toString();

                String radiomap_filename = new File(folder + File.separatorChar + "indoor-radiomap.txt").getAbsolutePath();
                String radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt");
                String radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt");
                String radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt");

                // create the radiomap using the input rss log file
                RadioMap rm = new RadioMap(new File(folder), radiomap_filename, "", -110);
                if (!rm.createRadioMap()) {
                    return AnyResponseHelper.internal_server_error("Error while creating Radio Map on-the-fly!");
                }

                // fix the paths
                // radiomaps is the folder where the folders reside in
                String api = AnyplaceServerAPI.SERVER_API_ROOT;
                int pos = radiomap_mean_filename.indexOf("radiomaps");
                radiomap_mean_filename = api + radiomap_mean_filename.substring(pos);

                pos = radiomap_rbf_weights_filename.indexOf("radiomaps");
                radiomap_rbf_weights_filename = api + radiomap_rbf_weights_filename.substring(pos);

                pos = radiomap_parameters_filename.indexOf("radiomaps");
                radiomap_parameters_filename = api + radiomap_parameters_filename.substring(pos);

                // everything is ok
                ObjectNode res = JsonUtils.createObjectNode();
                res.put("map_url_mean", radiomap_mean_filename);
                res.put("map_url_weights", radiomap_rbf_weights_filename);
                res.put("map_url_parameters", radiomap_parameters_filename);

                return AnyResponseHelper.ok(res, "Successfully created radio map.");
            } catch (Exception e) {
                // no exception is expected to be thrown but just in case
                return AnyResponseHelper.internal_server_error("Error while creating Radio Map on-the-fly! : " + e.getMessage());
            }
        }
    }

    public static Result magneticPathAdd() {

        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }

        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMagnetic::pathAdd(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "lat_a",
                "lng_a",
                "lat_b",
                "lng_b",
                "buid",
                "floor_num");

        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        try {
            MagneticPath mpath;
            try {
                mpath = new MagneticPath(json);
            } catch (NumberFormatException e) {
                return AnyResponseHelper.bad_request("Magnetic Path coordinates are invalid!");
            }
            if (!ProxyDataSource.getIDatasource().addJsonDocument(mpath.getId(), 0, mpath.toValidCouchJson())) {
                return AnyResponseHelper.bad_request("MPath already exists or could not be added!");
            }
            ObjectNode res = JsonUtils.createObjectNode();
            res.put("mpath", mpath.getId());
            return AnyResponseHelper.ok(res, "Successfully added magnetic path!");
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }

    }

    public static Result magneticPathDelete() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMagnetic::magneticPathDelete(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json, "mpuid");
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        String mpuid = json.findPath("mpuid").textValue();

        try {
            boolean success = ProxyDataSource.getIDatasource().deleteFromKey(mpuid);
            if (!success) {
                return AnyResponseHelper.bad_request("Magnetic Path does not exist or could not be retrieved!");
            }
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }


        return AnyResponseHelper.ok("Successfully deleted magnetic path!");
    }

    public static Result magneticPathByFloor() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::poisByFloor(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "buid",
                "floor_num"
        );
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        String buid = json.findPath("buid").textValue();
        String floor_number = json.findPath("floor_num").textValue();

        try {
            List<JsonNode> mpaths = ProxyDataSource.getIDatasource().magneticPathsByBuildingFloorAsJson(buid, floor_number);
            ObjectNode res = JsonUtils.createObjectNode();
            res.put("mpaths", JsonUtils.getJsonFromList(mpaths));
            return AnyResponseHelper.ok(res.toString());
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    public static Result magneticPathByBuilding() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::mpsByBuilding(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "buid"
        );
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        String buid = json.findPath("buid").textValue();

        try {
            List<JsonNode> mpaths = ProxyDataSource.getIDatasource().magneticPathsByBuildingAsJson(buid);
            ObjectNode res = JsonUtils.createObjectNode();
            res.put("mpaths", JsonUtils.getJsonFromList(mpaths));
            return AnyResponseHelper.ok(res.toString());
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

    public static Result magneticMilestoneUpload() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::mpsByBuilding(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "buid",
                "floor_num",
                "mpuid",
                "milestones"
        );
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        String buid = json.findPath("buid").textValue();
        String floor_num = json.findPath("floor_num").textValue();
        String mpuid = json.findPath("mpuid").textValue();

        JsonNode milestones = null;

        try {
            JsonUtils.getJsonTree(json.findPath("milestones").textValue());
        } catch (IOException ioe) {
            return AnyResponseHelper.internal_server_error("milestones could not be parsed");
        }

        for (JsonNode jn : milestones) {
            MagneticMilestone mm = new MagneticMilestone(jn, buid, floor_num, mpuid);
            try {
                if (!ProxyDataSource.getIDatasource().addJsonDocument(mm.getId(), 0, mm.toValidCouchJson())) {
                    return AnyResponseHelper.bad_request("Milestone already exists or could not be added!");
                }
            } catch (DatasourceException e) {
                return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
            }
        }

        return AnyResponseHelper.ok("ok");
    }

    public static Result magneticMilestoneByFloor() {
        OAuth2Request anyReq = new OAuth2Request(request(), response());
        if (!anyReq.assertJsonBody()) {
            return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON);
        }
        JsonNode json = anyReq.getJsonBody();
        LPLogger.info("AnyplaceMapping::milestonesByFloor(): " + json.toString());

        List<String> requiredMissing = JsonUtils.requirePropertiesInJson(json,
                "buid",
                "floor_num"
        );
        if (!requiredMissing.isEmpty()) {
            return AnyResponseHelper.requiredFieldsMissing(requiredMissing);
        }

        String buid = json.findPath("buid").textValue();
        String floor_number = json.findPath("floor_num").textValue();

        try {
            List<JsonNode> mpaths = ProxyDataSource.getIDatasource().magneticMilestonesByBuildingFloorAsJson(buid, floor_number);
            ObjectNode res = JsonUtils.createObjectNode();
            res.put("mmilestones", JsonUtils.getJsonFromList(mpaths));
            return AnyResponseHelper.ok(res.toString());
        } catch (DatasourceException e) {
            return AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage() + "]");
        }
    }

}
