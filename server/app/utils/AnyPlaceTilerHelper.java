/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Lambros Petrou
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

package utils;

import play.Logger;

import java.io.*;
import java.nio.file.Files;

public class AnyPlaceTilerHelper {

    private final static String ANYPLACE_TILER_SCRIPTS_DIR = "anyplace_tiler";
    private final static String ANYPLACE_TILER_SCRIPT_START = ANYPLACE_TILER_SCRIPTS_DIR + File.separatorChar + "start-anyplace-tiler.sh";

    private final static String FLOOR_PLANS_ROOT_DIR = "floor_plans" + File.separatorChar;
    private final static String FLOOR_TILES_DIR = "static_tiles" + File.separatorChar;
    public final static String FLOOR_TILES_ZIP_NAME = "tiles_archive.zip";

    public static String getRootFloorPlansDir() {
        return FLOOR_PLANS_ROOT_DIR;
    }

    public static String getRootFloorPlansDirFor(String buid) {
        return getRootFloorPlansDir() + buid + File.separatorChar;
    }

    public static String getRootFloorPlansDirFor(String buid, String floor) {
        if (buid.trim().isEmpty() || floor.trim().isEmpty()) {
            return null;
        }
        return getRootFloorPlansDirFor(buid) + "fl_" + floor + File.separatorChar;
    }

    public static String getFloorPlanFor(String buid, String floor) {
        if (buid.trim().isEmpty() || floor.trim().isEmpty()) {
            return null;
        }
        return getRootFloorPlansDirFor(buid, floor) + "fl_" + floor;
    }

    public static String getFloorTilesDirFor(String buid, String floor) {
        if (buid.trim().isEmpty() || floor.trim().isEmpty()) {
            return null;
        }
        return getRootFloorPlansDirFor(buid, floor) + FLOOR_TILES_DIR;
    }

    public static String getFloorTilesZipFor(String buid, String floor) {
        if (buid.trim().isEmpty() || floor.trim().isEmpty()) {
            return null;
        }
        return getRootFloorPlansDirFor(buid, floor) + FLOOR_TILES_DIR + FLOOR_TILES_ZIP_NAME;
    }

    public static String getFloorTilesZipLinkFor(String buid, String floor) {
        if (buid.trim().isEmpty() || floor.trim().isEmpty()) {
            return null;
        }
        return AnyplaceServerAPI.SERVER_FULL_URL + File.separatorChar
                + "anyplace/floortiles/"
                + buid + File.separatorChar + floor + File.separatorChar
                + FLOOR_TILES_ZIP_NAME;
    }

    /**
     * Stores the passed in radio map file inside the server's system for later retrieval if needed and offline processing on the server
     *
     * @param file
     * @return
     */
    public static File storeFloorPlanToServer(String buid, String floor_number, File file) throws AnyPlaceException {

        String dirS = AnyPlaceTilerHelper.getRootFloorPlansDirFor(buid, floor_number);
        File dir = new File(dirS);
        dir.mkdirs();

        if (!dir.isDirectory() || !dir.canWrite() || !dir.canExecute()) {
            throw new AnyPlaceException("Floor plans directory is inaccessible!!!");
        }

        String name = "fl" + "_" + floor_number;
        File dest_f = new File(dir, name);
        FileOutputStream fout;

        try {
            fout = new FileOutputStream(dest_f);
            Files.copy(file.toPath(), fout);
            fout.close();
        } catch (IOException e) {
            throw new AnyPlaceException("Error while writing saving the floorplan [" + e.getMessage() + "]");
        }

        return dest_f;
    }


    /**
     * Creates the tiles as per Google Maps requirements for the floorplan passed in as argument.
     *
     * @return True or False according to the procedure
     */
    public static boolean tileImage(File imageFile, String lat, String lng) throws AnyPlaceException {
        if (!imageFile.isFile() || !imageFile.canRead()) {
            return false;
        }
        File imageDir = imageFile.getParentFile();
        if (!imageDir.isDirectory() || !imageDir.canWrite() || !imageDir.canRead()) {
            throw new AnyPlaceException("Server do not have the permissions to tile the passed argument[" + imageFile.toString() + "]");
        }
        ProcessBuilder pb = new ProcessBuilder(
                ANYPLACE_TILER_SCRIPT_START,
                imageFile.getAbsolutePath().toString(),
                lat,
                lng,
                "-DISLOG");
        //Map<String, String> env = pb.environment();
        //env.put("VAR1", "myValue");
        //env.remove("OTHERVAR");
        //pb.directory(new File("myDir"));
        File log = new File(imageDir, "anyplace_tiler_" + imageFile.getName() + ".log");
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.appendTo(log));
        try {
            Process p = pb.start();
            InputStream is = p.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                System.out.println(">" + line);
            }
            p.waitFor();
            if (p.exitValue() != 0) {
                String err = "Tiling for image[" + imageFile.toString() + "] failed with exit code[" + p.exitValue() + "]!";
                Logger.error(err);
                throw new AnyPlaceException(err);
            }
        } catch (IOException e) {
            String err = "Tiling for image[" + imageFile.toString() + "] failed with IOException[" + e.getMessage() + "]!";
            Logger.error(err);
            throw new AnyPlaceException(err);
        } catch (InterruptedException e) {
            String err = "Tiling for image[" + imageFile.toString() + "] failed with InterruptedException[" + e.getMessage() + "]!";
            Logger.error(err);
            throw new AnyPlaceException(err);
        }

        return true;
    }

}
