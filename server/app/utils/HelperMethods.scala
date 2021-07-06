/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Constantinos Costa, Kyriakos Georgiou, Lambros Petrou
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
package utils

import java.io.{File, FileOutputStream, IOException}
import java.nio.file.Files
import java.util.HashMap

import com.couchbase.client.java.document.json.JsonObject
import org.apache.commons.codec.binary.Base64
import play.Play
import utils.LPUtils.generateRandomRssLogFileName
//remove if not needed

object HelperMethods {

    def checkUserCredentials(username: String, password: String): Boolean = {
        username != null && password != null && username.length > 0 &&
          password.length > 0
    }

    def checkEmptyParameter(par_name: String, par: String, result: JsonObject): Boolean = {
        if (par == null || par == "") {
            result.put("status", "error")
            result.put("message", "Missing or Invalid parameter [" + par_name + "]")
            return false
        }
        true
    }

    def checkEmptyParameter(par_name: String,
                            par: String,
                            result: JsonObject,
                            fields: HashMap[String, String],
                            field_name: String): Boolean = {
        if (par == null || par == "") {
            result.put("status", "error")
            result.put("message", "Missing or Invalid parameter [" + par_name + "]")
            return false
        }
        fields.put(field_name, par)
        true
    }

    def decodeBase64(base64_in: String): Array[Byte] = Base64.decodeBase64(base64_in.getBytes)

    def base64ToString(base64_in: String): String = new String(decodeBase64(base64_in))

    def storeRadioMapRawToServer(file: File): Boolean = {
        /*
        * FeatureAdd : Configuring location for server generated files
        */
        //val radio_dir = "radio_maps_raw/"
        val radio_dir = Play.application().configuration().getString("radioMapRawDir")
        val dir = new File(radio_dir)
        dir.mkdirs()
        if (!dir.isDirectory || !dir.canWrite() || !dir.canExecute()) {
            return false
        }
        val name = generateRandomRssLogFileName()
        //FeatureAdd : Configuring location for server generated files
        val dest_f = new File(radio_dir + AnyplaceServerAPI.URL_SEP + name)
        var fout: FileOutputStream = null
        try {
            fout = new FileOutputStream(dest_f)
            Files.copy(file.toPath(), fout)
            fout.close()
            LPLogger.D1("storeRadioMapToServer: Stored raw rss-log: " + name)
        } catch {
            case e: IOException => {
                e.printStackTrace()
                return false
            }
        }
        true
    }

     /*
     * DELETE FLOOR : BuxFix
     * Fixing function as parent level floor plan files and directory 
     * was not getting removed during floor delete
     */
    def recDeleteDirFile(f: File, root: Boolean =true) {
        if (f.isFile) {
            Files.delete(f.toPath())
        } else if (f.isDirectory) {
            for (file <- f.listFiles()) {
                if (file.isDirectory) {
                    recDeleteDirFile(file, false)
                }
                Files.delete(file.toPath())
            }
            if (root) {
                Files.delete(f.toPath)
            }
        }
    }
}
