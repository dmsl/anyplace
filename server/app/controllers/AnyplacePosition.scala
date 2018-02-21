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
package controllers

import datasources.DatasourceException
import datasources.ProxyDataSource
import db_models.Floor
import db_models.MagneticMilestone
import db_models.MagneticPath
import db_models.RadioMapRaw
import floor_module.Algo1
import oauth.provider.v2.models.OAuth2Request
import org.apache.commons.lang3.time.StopWatch
import play.libs.F
import play.api.mvc.{Action, Controller, Result}
import play.mvc.{Controller, Http}
import radiomapserver.RadioMap
import utils._
import java.io._
import java.util._

import com.couchbase.client.java.document.json.{JsonArray, JsonObject}
import controllers.AnyplaceMapping.Ok
//remove if not needed
import scala.collection.JavaConversions._

object AnyplacePosition extends play.api.mvc.Controller {

  def radioUpload() = Action {
    implicit request =>

      val anyReq = new OAuth2Request(request)
      val body = anyReq.getMultipartFormData()
      if (body == null) {
        AnyResponseHelper.bad_request("Invalid request type - Not Multipart!")
      }
      val radioFile = body.file("radiomap")
      if (radioFile == null) {
        AnyResponseHelper.bad_request("Cannot find the rss file (radiomap)!")
      }
      LPLogger.info("Radio Upload File: " + radioFile.get.ref.file.getAbsolutePath)
      val body_form = body.asFormUrlEncoded
      if (body_form == null) {
        AnyResponseHelper.bad_request("Invalid request type - Cannot be parsed as form data!")
      }
      if (body_form.get("json") == null) {
        AnyResponseHelper.bad_request("Cannot find json in the request!")
      }
      val json_str = body_form.get("json").get.head
      LPLogger.info("Radio Upload json: " + json_str)
      if (json_str == null) {
        AnyResponseHelper.bad_request("Cannot find json in the request!")
      }
      var json: JsonObject = null
      try {
        json = JsonObject.fromJson(json_str)
      } catch {
        case e: IOException => AnyResponseHelper.bad_request("Cannot parse json request!")
      }
      if (json.get("username") == null || json.get("password") == null) {
        AnyResponseHelper.bad_request("Cannot parse json request!")
      }
      val username = json.getString("username")
      val password = json.getString("password")
      if (null == username || null == password) {
        AnyResponseHelper.bad_request("Null username or password")
      }
      var floorFlag = false
      if ((username == "anyplace" && password == "floor") ||
        (username == "anonymous" && password == "anonymous")) {
        floorFlag = true
      } else if (username == "anyplace" && password == "123anyplace123rss") {
        floorFlag = false
      } else {
        AnyResponseHelper.forbidden("Invalid username or password")
      }
      val newBuildingsFloors = RadioMap.authenticateRSSlogFileAndReturnBuildingsFloors(radioFile.get.ref.file)
      if (newBuildingsFloors == null) {
        AnyResponseHelper.bad_request("Corrupted radio file uploaded!")
      } else {
        HelperMethods.storeRadioMapToServer(radioFile.get.ref.file)
        val errorMsg: String = null
        val strPromise = F.Promise.pure("10")
        val intPromise = strPromise.map(new F.Function[String, Integer]() {

          override def apply(arg0: String): java.lang.Integer = {
            storeFloorAlgoToDB(radioFile.get.ref.file)
            for (nBuilding <- newBuildingsFloors.keySet) {
              var bFloors = newBuildingsFloors.get(nBuilding)
              for (bFloor <- bFloors) {
                updateFrozenRadioMap(nBuilding, bFloor)
              }
            }
            0
          }
        })
      }
      AnyResponseHelper.ok("Successfully uploaded rss log.")
  }

  def radioDownloadFloor() = Action {
    implicit request =>

      val anyReq = new OAuth2Request(request)
      if (!anyReq.assertJsonBody()) {
        AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
      }
      val json = anyReq.getJsonBody
      LPLogger.info("AnyplacePosition::radioDownloadFloor(): " + json.toString)
      val requiredMissing = JsonUtils.requirePropertiesInJson(json, "coordinates_lat", "coordinates_lon",
        "floor_number")
      if (!requiredMissing.isEmpty) {
        AnyResponseHelper.requiredFieldsMissing(requiredMissing)
      }
      val lat = json.\\("coordinates_lat").mkString
      val lon = json.\\("coordinates_lon").mkString
      val floor_number = json.\\("floor_number").mkString
      val mode = json.\\("mode").toString
      if (!Floor.checkFloorNumberFormat(floor_number)) {
        AnyResponseHelper.bad_request("Floor number cannot contain whitespace!")
      } else {
        val bbox = GeoPoint.getGeoBoundingBox(java.lang.Double.parseDouble(lat), java.lang.Double.parseDouble(lon),
          500)
        LPLogger.info("LowerLeft: " + bbox(0) + " UpperRight: " + bbox(1))
        val dir = new File("radiomaps" + File.separatorChar + LPUtils.generateRandomToken() +
          "_" +
          System.currentTimeMillis())
        if (!dir.mkdirs()) {
          null
        }
        val radio = new File(dir.getAbsolutePath + File.separatorChar + "rss-log")
        var fout: FileOutputStream = null
        try {
          fout = new FileOutputStream(radio)
          println(radio.toPath().getFileName)
        } catch {
          case e: FileNotFoundException => AnyResponseHelper.internal_server_error("Cannot create radio map due to Server FileIO error!")
        }
        var floorFetched: Long = 0l
        try {
          floorFetched = ProxyDataSource.getIDatasource.dumpRssLogEntriesSpatial(fout, bbox, floor_number)
          try {
            fout.close()
          } catch {
            case e: IOException => LPLogger.error("Error while closing the file output stream for the dumped rss logs")
          }
        } catch {
          case e: DatasourceException => AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
        if (floorFetched == 0) {
          AnyResponseHelper.bad_request("Area not supported yet!")
        }
        try {
          val folder = dir.toString
          val radiomap_filename = new File(folder + File.separatorChar + "indoor-radiomap.txt")
            .getAbsolutePath
          var radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt")
          var radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt")
          var radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt")
          val rm = new RadioMap(new File(folder), radiomap_filename, "", -110)
          if (!rm.createRadioMap()) {
            AnyResponseHelper.internal_server_error("Error while creating Radio Map on-the-fly!")
          }
          val api = AnyplaceServerAPI.SERVER_API_ROOT
          var pos = radiomap_mean_filename.indexOf("radiomaps")
          radiomap_mean_filename = api + radiomap_mean_filename.substring(pos)
          pos = radiomap_rbf_weights_filename.indexOf("radiomaps")
          radiomap_rbf_weights_filename = api + radiomap_rbf_weights_filename.substring(pos)
          pos = radiomap_parameters_filename.indexOf("radiomaps")
          radiomap_parameters_filename = api + radiomap_parameters_filename.substring(pos)
          val res = JsonObject.empty()
          res.put("map_url_mean", radiomap_mean_filename)
          res.put("map_url_weights", radiomap_rbf_weights_filename)
          res.put("map_url_parameters", radiomap_parameters_filename)
          AnyResponseHelper.ok(res, "Successfully created radio map.")
        } catch {
          case e: Exception => AnyResponseHelper.internal_server_error("Error while creating Radio Map on-the-fly! : " + e.getMessage)
        }
      }
  }

  def radioDownloadByBuildingFloor() = Action {
    implicit request =>

      val anyReq = new OAuth2Request(request)
      if (!anyReq.assertJsonBody()) {
        AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
      }
      val json = anyReq.getJsonBody
      LPLogger.info("AnyplacePosition::radioDownloadFloor(): " + json.toString)
      val requiredMissing = JsonUtils.requirePropertiesInJson(json, "floor", "buid")
      if (!requiredMissing.isEmpty) {
        AnyResponseHelper.requiredFieldsMissing(requiredMissing)
      }
      val floor_number = (json \ "floor").as[String]
      val buid = (json \ "buid").as[String]

      if (!Floor.checkFloorNumberFormat(floor_number)) {
        AnyResponseHelper.bad_request("Floor number cannot contain whitespace!")
      }
      val rmapDir = new File("radiomaps_frozen" + File.separatorChar + buid + File.separatorChar +
        floor_number)
      val radiomapFile = new File("radiomaps_frozen" + File.separatorChar + buid + File.separatorChar +
        floor_number +
        File.separatorChar +
        "indoor-radiomap.txt")
      val meanFile = new File("radiomaps_frozen" + File.separatorChar + buid + File.separatorChar +
        floor_number +
        File.separatorChar +
        "indoor-radiomap-mean.txt")
      if (rmapDir.exists() && radiomapFile.exists() && meanFile.exists()) {
        try {
          val folder = rmapDir.toString
          val radiomap_filename = new File(folder + File.separatorChar + "indoor-radiomap.txt")
            .getAbsolutePath
          var radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt")
          var radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt")
          var radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt")
          val api = AnyplaceServerAPI.SERVER_API_ROOT
          var pos = radiomap_mean_filename.indexOf("radiomaps_frozen")
          radiomap_mean_filename = api + radiomap_mean_filename.substring(pos)
          pos = radiomap_rbf_weights_filename.indexOf("radiomaps_frozen")
          radiomap_rbf_weights_filename = api + radiomap_rbf_weights_filename.substring(pos)
          pos = radiomap_parameters_filename.indexOf("radiomaps_frozen")
          radiomap_parameters_filename = api + radiomap_parameters_filename.substring(pos)
          val res = JsonObject.empty()
          res.put("map_url_mean", radiomap_mean_filename)
          res.put("map_url_weights", radiomap_rbf_weights_filename)
          res.put("map_url_parameters", radiomap_parameters_filename)
          AnyResponseHelper.ok(res, "Successfully served radio map.")
        } catch {
          case e: Exception => AnyResponseHelper.internal_server_error("Error serving radiomap : " + e.getMessage)
        }
      }
      if (!rmapDir.mkdirs()) {
        AnyResponseHelper.internal_server_error("Error while creating Radio Map on-the-fly!")
      }
      val radio = new File(rmapDir.getAbsolutePath + File.separatorChar + "rss-log")
      var fout: FileOutputStream = null
      try {
        fout = new FileOutputStream(radio)
        println(radio.toPath().getFileName)
      } catch {
        case e: FileNotFoundException => AnyResponseHelper.internal_server_error("Cannot create radio map due to Server FileIO error!")
      }
      var floorFetched: Long = 0l
      try {
        floorFetched = ProxyDataSource.getIDatasource.dumpRssLogEntriesByBuildingFloor(fout, buid, floor_number)
        try {
          fout.close()
        } catch {
          case e: IOException => LPLogger.error("Error while closing the file output stream for the dumped rss logs")
        }
      } catch {
        case e: DatasourceException => AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
      }
      if (floorFetched == 0) {
        AnyResponseHelper.bad_request("Area not supported yet!")
      }
      try {
        val folder = rmapDir.toString
        val radiomap_filename = new File(folder + File.separatorChar + "indoor-radiomap.txt")
          .getAbsolutePath
        var radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt")
        var radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt")
        var radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt")
        val rm = new RadioMap(new File(folder), radiomap_filename, "", -110)
        if (!rm.createRadioMap()) {
          AnyResponseHelper.internal_server_error("Error while creating Radio Map on-the-fly!")
        }
        val api = AnyplaceServerAPI.SERVER_API_ROOT
        var pos = radiomap_mean_filename.indexOf("radiomaps_frozen")
        radiomap_mean_filename = api + radiomap_mean_filename.substring(pos)
        pos = radiomap_rbf_weights_filename.indexOf("radiomaps_frozen")
        radiomap_rbf_weights_filename = api + radiomap_rbf_weights_filename.substring(pos)
        pos = radiomap_parameters_filename.indexOf("radiomaps_frozen")
        radiomap_parameters_filename = api + radiomap_parameters_filename.substring(pos)
        val res = JsonObject.empty()
        res.put("map_url_mean", radiomap_mean_filename)
        res.put("map_url_weights", radiomap_rbf_weights_filename)
        res.put("map_url_parameters", radiomap_parameters_filename)
        AnyResponseHelper.ok(res, "Successfully created radio map.")
      } catch {
        case e: Exception => AnyResponseHelper.internal_server_error("Error while creating Radio Map on-the-fly! : " + e.getMessage)
      }
  }

  private def storeRadioMapToDB(infile: File): String = {
    var line: String = null
    var fr: FileReader = null
    var bf: BufferedReader = null
    var lineNumber = 0
    try {
      fr = new FileReader(infile)
      bf = new BufferedReader(fr)
      while ( {
        line = bf.readLine; line != null
      }) {
        if (line.startsWith("# Timestamp")) //continue
          lineNumber += 1
        val segs = line.split(" ")
        val rmr = new RadioMapRaw(segs(0), segs(1), segs(2), segs(3), segs(4), segs(5), segs(6))
        LPLogger.info(rmr.toValidCouchJson().toString)
        LPLogger.debug("raw[" + lineNumber + "] : " + rmr.toValidCouchJson())
        try {
          if (!ProxyDataSource.getIDatasource.addJsonDocument(rmr.getId, 0, rmr.toCouchGeoJSON())) {
            return "Radio Map entry could not be saved in database![could not be created]"
          }
        } catch {
          case e: DatasourceException => return "Internal server error while trying to save rss entry."
        }
      }
    } catch {
      case e: FileNotFoundException => return "Internal server error: Error while storing rss log."
      case e: IOException => return "Internal server error: Error while storing rss log."
    } finally {
      try {
        if (fr != null) fr.close()
        if (bf != null) bf.close()
      } catch {
        case e: IOException => return "Internal server error: Error while storing rss log."
      }
    }
    null
  }

  def serveRadioMap(radio_folder: String, fileName: String) = Action {
    implicit request =>

      val anyReq = new OAuth2Request(request, false)
      if (!anyReq.assertJsonBody()) {
        AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
      }
      val json = anyReq.getJsonBody
      LPLogger.info("AnyplacePosition::serveRadioMap(): " + json.toString)
      val filePath = "radiomaps" + File.separatorChar + radio_folder + File.separatorChar +
        fileName
      LPLogger.info("requested: " + filePath)
      val file = new File(filePath)
      try {
        if (!file.exists() || !file.canRead()) {
          AnyResponseHelper.bad_request("Requested file does not exist or cannot be read! (" + fileName +
            ")")
        }
        Ok.sendFile(file)
      } catch {
        case e: FileNotFoundException => AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
      }
  }

  def serveFrozenRadioMap(building: String, floor: String, fileName: String) = Action {
    val filePath = "radiomaps_frozen" + File.separatorChar + building + File.separatorChar +
      floor +
      File.separatorChar +
      fileName
    LPLogger.info("requested: " + filePath)
    val file = new File(filePath)
    try {
      if (!file.exists() || !file.canRead()) {
        AnyResponseHelper.bad_request("Requested file does not exist or cannot be read! (" +
          fileName +
          ")")
      }
      Ok.sendFile(file)
    } catch {
      case e: FileNotFoundException => AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
    }
  }

  private def storeFloorAlgoToDB_Help(values: ArrayList[String]): String = {
    if (values.size > 0) {
      var maxMac = ""
      var maxRss = java.lang.Integer.MIN_VALUE
      var i = 0
      while (i < values.size()) {
        val value = values.get(i)
        val segs = value.split(" ")
        try {
          val mac = segs(4)
          val rss = java.lang.Integer.parseInt(segs(5))
          if (rss > maxRss) {
            maxRss = rss
            maxMac = mac
          }
        } catch {
          case ex: NumberFormatException => if (values.remove(value)) i -= 1
        }
        i += 1
      }
      for (value <- values) {
        val segs = value.split(" ")
        var rmr: RadioMapRaw = null
        if (segs.length >= 8) {
          rmr = new RadioMapRaw(segs(0), segs(1), segs(2), segs(3), segs(4), segs(5), segs(6), maxMac,
            segs(7))
        } else if (segs.length >= 7) {
          rmr = new RadioMapRaw(segs(0), segs(1), segs(2), segs(3), segs(4), segs(5), segs(6), maxMac)
        } else {
          return "Some fields are missing from the log."
        }
        LPLogger.info(rmr.toValidCouchJson().toString)
        try {
          if (!ProxyDataSource.getIDatasource.addJsonDocument(rmr.getId, 0, rmr.toCouchGeoJSON())) {
            LPLogger.info("Radio Map entry was not saved in database![Possible duplicate]")
          }
        } catch {
          case e: DatasourceException => return "Internal server error while trying to save rss entry."
        }
      }
    }
    null
  }

  private def storeFloorAlgoToDB(infile: File): String = {
    var line: String = null
    var fr: FileReader = null
    var bf: BufferedReader = null
    try {
      fr = new FileReader(infile)
      bf = new BufferedReader(fr)
      val values = new ArrayList[String](10)
      while ( {
        line = bf.readLine; line != null
      }) {
        if (line.startsWith("# Timestamp")) {
          val result = storeFloorAlgoToDB_Help(values)
          if (result != null) return result
          values.clear()
        } else {
          values.add(line)
        }
      }
      val result = storeFloorAlgoToDB_Help(values)
      if (result != null) return result
    } catch {
      case e: FileNotFoundException => return "Internal server error: Error while storing rss log."
      case e: IOException => return "Internal server error: Error while storing rss log."
    } finally {
      try {
        if (fr != null) fr.close()
        if (bf != null) bf.close()
      } catch {
        case e: IOException => return "Internal server error: Error while storing rss log."
      }
    }
    null
  }

  def predictFloorAlgo1() = Action {
    implicit request =>

      val anyReq = new OAuth2Request(request)
      if (!anyReq.assertJsonBody()) {
        AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
      }
      val json = anyReq.getJsonBody
      val watch = new StopWatch()
      watch.start()
      LPLogger.info("AnyplacePosition::predictFloor(): ")
      var alg1: Algo1 = null
      try {
        alg1 = new Algo1(json.asInstanceOf[JsonObject])
      } catch {
        case ex: Exception => AnyResponseHelper.bad_request(ex.getMessage)
      }
      try {
        val lat = json.\("dlat").as[Double]
        val lot = json.\("dlong").as[Double]
        val bbox = GeoPoint.getGeoBoundingBox(lat, lot, 100)
        val strongestMAC = new ArrayList[String](2)
        if (json.\("first").get == null) AnyResponseHelper.bad_request("Sent first Wifi")
        strongestMAC.add(json.\("first").\("MAC").as[String])
        if (json.\("second").get != null) strongestMAC.add(json.\("second").\("MAC").as[String])
        val res = JsonObject.empty()
        if (ProxyDataSource.getIDatasource.predictFloor(alg1, bbox, strongestMAC.toArray(Array.ofDim[String](1)))) {
          res.put("floor", alg1.getFloor)
        } else {
          res.put("floor", "")
        }
        watch.stop()
        LPLogger.info("Time for Algo1 is millis: " + watch.getNanoTime / 1000000)
        AnyResponseHelper.ok(res, "Successfully predicted Floor.")
      } catch {
        case e: Exception => AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
      }
  }

  private def updateFrozenRadioMap(buid: String, floor_number: String) {
    if (!Floor.checkFloorNumberFormat(floor_number)) {
      return
    }
    val rmapDir = new File("radiomaps_frozen" + File.separatorChar + buid + File.separatorChar +
      floor_number)
    if (!rmapDir.exists() && !rmapDir.mkdirs()) {
      return
    }
    val radio = new File(rmapDir.getAbsolutePath + File.separatorChar + "rss-log")
    var fout: FileOutputStream = null
    try {
      fout = new FileOutputStream(radio)
      println(radio.toPath().getFileName)
    } catch {
      case e: FileNotFoundException => return
    }
    var floorFetched: Long = 0l
    try {
      floorFetched = ProxyDataSource.getIDatasource.dumpRssLogEntriesByBuildingFloor(fout, buid, floor_number)
      try {
        fout.close()
      } catch {
        case e: IOException => LPLogger.error("Error while closing the file output stream for the dumped rss logs")
      }
    } catch {
      case e: DatasourceException => return
    }
    if (floorFetched == 0) {
      return
    }
    try {
      val folder = rmapDir.toString
      val radiomap_filename = new File(folder + File.separatorChar + "indoor-radiomap.txt")
        .getAbsolutePath
      var radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt")
      var radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt")
      var radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt")
      val rm = new RadioMap(new File(folder), radiomap_filename, "", -110)
      if (!rm.createRadioMap()) {
        return
      }
      val api = AnyplaceServerAPI.SERVER_API_ROOT
      var pos = radiomap_mean_filename.indexOf("radiomaps_frozen")
      radiomap_mean_filename = api + radiomap_mean_filename.substring(pos)
      pos = radiomap_rbf_weights_filename.indexOf("radiomaps_frozen")
      radiomap_rbf_weights_filename = api + radiomap_rbf_weights_filename.substring(pos)
      pos = radiomap_parameters_filename.indexOf("radiomaps_frozen")
      radiomap_parameters_filename = api + radiomap_parameters_filename.substring(pos)
      val res = JsonObject.empty()
      res.put("map_url_mean", radiomap_mean_filename)
      res.put("map_url_weights", radiomap_rbf_weights_filename)
      res.put("map_url_parameters", radiomap_parameters_filename)
      return
    } catch {
      case e: Exception => return
    }
  }

  def radioDownloadFloorBbox() = Action {
    implicit request =>

      val anyReq = new OAuth2Request(request)
      if (!anyReq.assertJsonBody()) {
        AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
      }
      val json = anyReq.getJsonBody
      LPLogger.info("AnyplacePosition::radioDownloadFloor(): " + json.toString)
      val requiredMissing = JsonUtils.requirePropertiesInJson(json, "coordinates_lat", "coordinates_lon",
        "floor_number", "range")
      if (!requiredMissing.isEmpty) {
        AnyResponseHelper.requiredFieldsMissing(requiredMissing)
      }
      val lat = (json \ "coordinates_lat").as[String]
      val lon = (json \ "coordinates_lon").as[String]
      val floor_number = (json \ "floor_number").as[String]
      val strRange = (json \ "range").as[String]
      val range = java.lang.Integer.parseInt(strRange)
      if (!Floor.checkFloorNumberFormat(floor_number)) {
        AnyResponseHelper.bad_request("Floor number cannot contain whitespace!")
      } else {
        val bbox = GeoPoint.getGeoBoundingBox(java.lang.Double.parseDouble(lat), java.lang.Double.parseDouble(lon),
          range)
        LPLogger.info("LowerLeft: " + bbox(0) + " UpperRight: " + bbox(1))
        val dir = new File("radiomaps" + File.separatorChar + LPUtils.generateRandomToken() +
          "_" +
          System.currentTimeMillis())
        if (!dir.mkdirs()) {
          null
        }
        val radio = new File(dir.getAbsolutePath + File.separatorChar + "rss-log")
        var fout: FileOutputStream = null
        try {
          fout = new FileOutputStream(radio)
          println(radio.toPath().getFileName)
        } catch {
          case e: FileNotFoundException => AnyResponseHelper.internal_server_error("Cannot create radio map due to Server FileIO error!")
        }
        var floorFetched: Long = 0l
        try {
          floorFetched = ProxyDataSource.getIDatasource.dumpRssLogEntriesSpatial(fout, bbox, floor_number)
          try {
            fout.close()
          } catch {
            case e: IOException => LPLogger.error("Error while closing the file output stream for the dumped rss logs")
          }
        } catch {
          case e: DatasourceException => AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
        if (floorFetched == 0) {
          AnyResponseHelper.bad_request("Area not supported yet!")
        }
        try {
          val folder = dir.toString
          val radiomap_filename = new File(folder + File.separatorChar + "indoor-radiomap.txt")
            .getAbsolutePath
          var radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt")
          var radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt")
          var radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt")
          val rm = new RadioMap(new File(folder), radiomap_filename, "", -110)
          if (!rm.createRadioMap()) {
            AnyResponseHelper.internal_server_error("Error while creating Radio Map on-the-fly!")
          }
          val api = AnyplaceServerAPI.SERVER_API_ROOT
          var pos = radiomap_mean_filename.indexOf("radiomaps")
          radiomap_mean_filename = api + radiomap_mean_filename.substring(pos)
          pos = radiomap_rbf_weights_filename.indexOf("radiomaps")
          radiomap_rbf_weights_filename = api + radiomap_rbf_weights_filename.substring(pos)
          pos = radiomap_parameters_filename.indexOf("radiomaps")
          radiomap_parameters_filename = api + radiomap_parameters_filename.substring(pos)
          val res = JsonObject.empty()
          res.put("map_url_mean", radiomap_mean_filename)
          res.put("map_url_weights", radiomap_rbf_weights_filename)
          res.put("map_url_parameters", radiomap_parameters_filename)
          AnyResponseHelper.ok(res, "Successfully created radio map.")
        } catch {
          case e: Exception => AnyResponseHelper.internal_server_error("Error while creating Radio Map on-the-fly! : " + e.getMessage)
        }
      }
  }

  def magneticPathAdd() = Action {
    implicit request =>

      val anyReq = new OAuth2Request(request)
      if (!anyReq.assertJsonBody()) {
        AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
      }
      val json = anyReq.getJsonBody
      LPLogger.info("AnyplaceMagnetic::pathAdd(): " + json.toString)
      val requiredMissing = JsonUtils.requirePropertiesInJson(json, "lat_a", "lng_a", "lat_b", "lng_b",
        "buid", "floor_num")
      if (!requiredMissing.isEmpty) {
        AnyResponseHelper.requiredFieldsMissing(requiredMissing)
      }
      try {
        var mpath: MagneticPath = null
        try {
          mpath = new MagneticPath(json.asInstanceOf[JsonObject])
        } catch {
          case e: NumberFormatException => AnyResponseHelper.bad_request("Magnetic Path coordinates are invalid!")
        }
        if (!ProxyDataSource.getIDatasource.addJsonDocument(mpath.getId, 0, mpath.toValidCouchJson().toString)) {
          AnyResponseHelper.bad_request("MPath already exists or could not be added!")
        }
        val res = JsonObject.empty()
        res.put("mpath", mpath.getId)
        AnyResponseHelper.ok(res, "Successfully added magnetic path!")
      } catch {
        case e: DatasourceException => AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
      }
  }

  def magneticPathDelete() = Action {
    implicit request =>

      val anyReq = new OAuth2Request(request)
      if (!anyReq.assertJsonBody()) {
        AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
      }
      val json = anyReq.getJsonBody
      LPLogger.info("AnyplaceMagnetic::magneticPathDelete(): " + json.toString)
      val requiredMissing = JsonUtils.requirePropertiesInJson(json, "mpuid")
      if (!requiredMissing.isEmpty) {
        AnyResponseHelper.requiredFieldsMissing(requiredMissing)
      }
      val mpuid = (json \ "mpuid").as[String]
      try {
        val success = ProxyDataSource.getIDatasource.deleteFromKey(mpuid)
        if (!success) {
          AnyResponseHelper.bad_request("Magnetic Path does not exist or could not be retrieved!")
        }
      } catch {
        case e: DatasourceException => AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
      }
      AnyResponseHelper.ok("Successfully deleted magnetic path!")
  }

  def magneticPathByFloor() = Action {
    implicit request =>

      val anyReq = new OAuth2Request(request)
      if (!anyReq.assertJsonBody()) {
        AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
      }
      val json = anyReq.getJsonBody
      LPLogger.info("AnyplaceMapping::poisByFloor(): " + json.toString)
      val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor_num")
      if (!requiredMissing.isEmpty) {
        AnyResponseHelper.requiredFieldsMissing(requiredMissing)
      }
      val buid = (json \ "buid").as[String]
      val floor_number = (json \ "floor_num").as[String]
      try {
        val mpaths = ProxyDataSource.getIDatasource.magneticPathsByBuildingFloorAsJson(buid, floor_number)
        val res = JsonObject.empty()
        res.put("mpaths", JsonArray.from(mpaths))
        AnyResponseHelper.ok(res.toString)
      } catch {
        case e: DatasourceException => AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
      }
  }

  def magneticPathByBuilding() = Action {
    implicit request =>

      val anyReq = new OAuth2Request(request)
      if (!anyReq.assertJsonBody()) {
        AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
      }
      val json = anyReq.getJsonBody
      LPLogger.info("AnyplaceMapping::mpsByBuilding(): " + json.toString)
      val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid")
      if (!requiredMissing.isEmpty) {
        AnyResponseHelper.requiredFieldsMissing(requiredMissing)
      }
      val buid = (json \ "buid").as[String]
      try {
        val mpaths = ProxyDataSource.getIDatasource.magneticPathsByBuildingAsJson(buid)
        val res = JsonObject.empty()
        res.put("mpaths", JsonArray.from(mpaths))
        AnyResponseHelper.ok(res.toString)
      } catch {
        case e: DatasourceException => AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
      }
  }

  def magneticMilestoneUpload() = Action {
    implicit request =>

      val anyReq = new OAuth2Request(request)
      if (!anyReq.assertJsonBody()) {
        AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
      }
      val json = anyReq.getJsonBody
      LPLogger.info("AnyplaceMapping::mpsByBuilding(): " + json.toString)
      val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor_num", "mpuid", "milestones")
      if (!requiredMissing.isEmpty) {
        AnyResponseHelper.requiredFieldsMissing(requiredMissing)
      }
      val buid = (json \ "buid").as[String]
      val floor_num = (json \ "floor_num").as[String]
      val mpuid = (json \ "mpuid").as[String]
      var milestones: JsonArray = JsonArray.empty()
      try {
        JsonArray.fromJson(json.\\("milestones").mkString)
      } catch {
        case ioe: IOException => AnyResponseHelper.internal_server_error("milestones could not be parsed")
      }
      for (jn <- milestones.toList.asInstanceOf[List[JsonObject]]) {
        val mm = new MagneticMilestone(jn, buid, floor_num, mpuid)
        try {
          if (!ProxyDataSource.getIDatasource.addJsonDocument(mm.getId, 0, mm.toValidCouchJson().toString)) {
            AnyResponseHelper.bad_request("Milestone already exists or could not be added!")
          }
        } catch {
          case e: DatasourceException => AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
        }
      }
      AnyResponseHelper.ok("ok")
  }

  def magneticMilestoneByFloor() = Action {
    implicit request =>

      val anyReq = new OAuth2Request(request)
      if (!anyReq.assertJsonBody()) {
        AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
      }
      val json = anyReq.getJsonBody
      LPLogger.info("AnyplaceMapping::milestonesByFloor(): " + json.toString)
      val requiredMissing = JsonUtils.requirePropertiesInJson(json, "buid", "floor_num")
      if (!requiredMissing.isEmpty) {
        AnyResponseHelper.requiredFieldsMissing(requiredMissing)
      }
      val buid = (json \ "buid").as[String]
      val floor_number = (json \ "floor_num").as[String]
      try {
        val mpaths = ProxyDataSource.getIDatasource.magneticMilestonesByBuildingFloorAsJson(buid, floor_number)
        val res = JsonObject.empty()
        res.put("mpaths", JsonArray.from(mpaths))
        AnyResponseHelper.ok(res.toString)
      } catch {
        case e: DatasourceException => AnyResponseHelper.internal_server_error("Server Internal Error [" + e.getMessage + "]")
      }
  }
}
