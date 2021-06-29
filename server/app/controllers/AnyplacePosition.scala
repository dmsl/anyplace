/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Nikolas Neofytou, Constantinos Costa, Kyriakos Georgiou, Lambros Petrou
 *
 * Co-Supervisor: Paschalis Mpeis
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

import java.io._
import java.util.ArrayList

import com.couchbase.client.java.document.json.{JsonArray, JsonObject}
import controllers.AnyplaceMapping.verifyId
import datasources.SCHEMA._
import datasources.{DatasourceException, ProxyDataSource, SCHEMA}
import db_models.RadioMapRaw.findRadioBbox
import db_models.{Floor, MagneticMilestone, MagneticPath, RadioMapRaw}
import floor_module.Algo1
import json.VALIDATE
import json.VALIDATE.{String, StringNumber}
import oauth.provider.v2.models.OAuth2Request
import org.apache.commons.lang3.time.StopWatch
import play.Play
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Action, AnyContent, Request, Result}
import radiomapserver.RadioMap
import radiomapserver.RadioMap.{RBF_ENABLED, RadioMap}
import utils.FileUtils._
import utils._

import scala.collection.JavaConversions._


object AnyplacePosition extends play.api.mvc.Controller {
  val BBOX_MAX = 500

  /**
   * Upload fingerprints to server and database. 
   * @return
   */
  def radioUpload() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val body = anyReq.getMultipartFormData()
        if (body == null) {
          return AnyResponseHelper.bad_request("Invalid request type - Not Multipart!")
        }
        var rssLog: File = null
        try {
          rssLog = body.file("radiomap").get.ref.file
        } catch {
          case e: Exception => return AnyResponseHelper.bad_request("Cannot find radiomap (rss log)!")
        }
        val body_form = body.asFormUrlEncoded
        if (body_form == null) {
          return AnyResponseHelper.bad_request("Invalid request type - Cannot be parsed as form data!")
        }
        if (!body_form.contains(SCHEMA.fAccessToken)) {
          return AnyResponseHelper.bad_request("Cannot find access_token in the request!")
        }
        val access_token = body_form.get(SCHEMA.fAccessToken).get.head
        if (verifyId(access_token) == null) return AnyResponseHelper.forbidden("Unauthorized")
        var ret: String = ""
        val newBuildingsFloors = RadioMap.verifyRssLogAndGetBuildingFloors(rssLog)
        if (newBuildingsFloors == null) {
          return AnyResponseHelper.bad_request("Uploaded a corrupted rss-log file!")
        } else {
          HelperMethods.storeRadioMapRawToServer(rssLog)
          ret = storeFloorRssToDB(rssLog)
          LPLogger.debug("Rss values already exist: " + ret)
          val errors: ArrayList[JsValue] = new ArrayList[JsValue]
          for (buid <- newBuildingsFloors.keySet) {
            for (floor_num <- newBuildingsFloors.get(buid)) {
              val res = updateFrozenRadioMap(buid, floor_num)
              if (res != null) {
                val js = Json.obj(SCHEMA.fBuid -> buid, "floor_num" -> floor_num, "error" -> res)
                errors.add(js)
              }
            }
          }
          if (errors.size > 0) {
            val json = Json.obj("errorList" -> errors.toList)
            return AnyResponseHelper.bad_request(json, "Failed to create frozen radio maps.")
          }
        }
        LPLogger.debug("Successfully uploaded rss log.")
        return AnyResponseHelper.ok("Successfully uploaded rss log.")
      }

      inner(request)
  }

  /**
   * Fetch floorNum + bbox from floorplans then check that result is one then get buid from floorplan
   * and return rss from radioMapRawDir (getRadioMapRawFile)
   *
   * @return
   */
  def radioDownloadFloor() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody
        LPLogger.info("radioDownloadFloor: " + json.toString)
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fCoordinatesLat, SCHEMA.fCoordinatesLon, SCHEMA.fFloorNumber)
        if (!requiredMissing.isEmpty) {
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        }
        // range is large enough to cover the entire floor
        return findRadioBbox(json, BBOX_MAX)
      }

      inner(request)
  }

  def radioDownloadFloorBbox() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplacePosition::radioDownloadFloor(): " + json.toString)
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fCoordinatesLat, SCHEMA.fCoordinatesLon,
          SCHEMA.fFloorNumber, "range")
        if (!requiredMissing.isEmpty) {
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        }
        if (StringNumber(json, "range") == null) {
          return AnyResponseHelper.bad_request("range field must be String, containing a number!")
        }
        var range = (json \ "range").as[String].toInt
        if (range > BBOX_MAX) range = BBOX_MAX
        return findRadioBbox(json, range)
      }

      inner(request)
  }

  /**
   *
   * @return
   */
  def radioDownloadByBuildingFloor() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {

        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody
        LPLogger.info("radioDownloadByBuildingFloor: " + json.toString)
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fFloor, SCHEMA.fBuid)
        if (!requiredMissing.isEmpty) return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        val validation = VALIDATE.fields(json, fFloor, fBuid)
        if (validation.failed()) return validation.response()

        val floor_number = (json \ SCHEMA.fFloor).as[String]
        val buid = (json \ SCHEMA.fBuid).as[String]


        if (!Floor.checkFloorNumberFormat(floor_number)) {
          return AnyResponseHelper.bad_request("Floor number cannot contain whitespace!")
        }

        //FeatureAdd : Configuring location for server generated files
        val rmapDir = getDirFrozenFloor(buid, floor_number)
        val radiomapFile = getRadiomapFile(buid, floor_number)
        val meanFile = getMeanFile(buid, floor_number)
        if (rmapDir.exists() && radiomapFile.exists() && meanFile.exists()) {
          try {
            val radiomap_filename = getRadioMapFileName(buid, floor_number).getAbsolutePath
            var radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt")
            var radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt")
            var radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt")
            val api = AnyplaceServerAPI.SERVER_API_ROOT
            var pos = getFilePos(radiomap_mean_filename)
            radiomap_mean_filename = api + radiomap_mean_filename.substring(pos)
            pos = getFilePos(radiomap_rbf_weights_filename)
            radiomap_rbf_weights_filename = api + radiomap_rbf_weights_filename.substring(pos)
            pos = getFilePos(radiomap_parameters_filename)
            radiomap_parameters_filename = api + radiomap_parameters_filename.substring(pos)
            var res: JsValue = null

            if (RBF_ENABLED) {
              res = Json.obj("map_url_mean" -> radiomap_mean_filename,
                "map_url_weights" -> radiomap_rbf_weights_filename,
                "map_url_parameters" -> radiomap_parameters_filename)
            } else {
              res = Json.obj("map_url_mean" -> radiomap_mean_filename)
            }
            return AnyResponseHelper.ok(res, "Successfully served radiomap floor.")
          } catch {
            case e: Exception => return AnyResponseHelper.internal_server_error("radioDownloadByBuildingFloor", e)
          }
        }
        if (!rmapDir.mkdirs()) {
          return AnyResponseHelper.internal_server_error("Error while creating Radio Map on-the-fly!")
        }
        val radio = new File(rmapDir.getAbsolutePath + AnyplaceServerAPI.URL_SEP + "rss-log")
        var fout: FileOutputStream = null
        var floorFetched: Long = 0l
        try {
          fout = new FileOutputStream(radio)
          floorFetched = ProxyDataSource.getIDatasource.dumpRssLogEntriesByBuildingFloor(fout, buid, floor_number)
          fout.close()

          if (floorFetched == 0) {
            return AnyResponseHelper.bad_request("Area not supported yet!")
          }

          val folder = rmapDir.toString
          val radiomap_filename = new File(folder + AnyplaceServerAPI.URL_SEP + "indoor-radiomap.txt")
            .getAbsolutePath
          var radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt")
          var radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt")
          var radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt")
          val rm = new RadioMap(new File(folder), radiomap_filename, "", -110)
          val resCreate = rm.createRadioMap()
          if (resCreate != null) {
            return AnyResponseHelper.internal_server_error("radioDownloadByBuildingFloor: radiomap on-the-fly: " + resCreate)
          }
          val api = AnyplaceServerAPI.SERVER_API_ROOT
          var pos = radiomap_mean_filename.indexOf("radiomaps_frozen")
          radiomap_mean_filename = api + radiomap_mean_filename.substring(pos)
          pos = radiomap_rbf_weights_filename.indexOf("radiomaps_frozen")
          radiomap_rbf_weights_filename = api + radiomap_rbf_weights_filename.substring(pos)
          pos = radiomap_parameters_filename.indexOf("radiomaps_frozen")
          radiomap_parameters_filename = api + radiomap_parameters_filename.substring(pos)
          val res: JsValue = Json.obj("map_url_mean" -> radiomap_mean_filename,
            "map_url_weights" -> radiomap_rbf_weights_filename,
            "map_url_parameters" -> radiomap_parameters_filename)
          return AnyResponseHelper.ok(res, "Successfully created radio map.")
        } catch {
          case e: Exception => return AnyResponseHelper.internal_server_error("radioDownloadByBuildingFloor: " +
            "radiomap on-the-fly", e)
        }
      }

      inner(request)
  }


  /**
   * Returns a link to the radio map that needs to be downloaded according to the specified buid and floor
   *
   * @return a link to the radio_map file
   */
  def radioDownloadByBuildingFloorall() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {

        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody
        LPLogger.info("radioDownloadByBuildingFloorall: " + json.toString)
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fFloor, SCHEMA.fBuid)
        if (!requiredMissing.isEmpty) {
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        }
        if (StringNumber(json, SCHEMA.fFloor) == null)
          return AnyResponseHelper.bad_request("floor field must be String, containing a number!")
        val floor_number = (json \ SCHEMA.fFloor).as[String]
        if (String(json, SCHEMA.fBuid) == null)
          return AnyResponseHelper.bad_request("buid field must be String!")
        val buid = (json \ SCHEMA.fBuid).as[String]

        val floors = floor_number.split(" ")
        val radiomap_mean_filename = new java.util.ArrayList[String]()
        val rss_log_files = new java.util.ArrayList[String]()

        for (floor_number <- floors) {
          //FeatureAdd : Configuring location for server generated files
          val rmapDir = getDirFrozenFloor(buid, floor_number)
          val radiomapFile = getRadiomapFile(buid, floor_number)
          val meanFile = getMeanFile(buid, floor_number)
          if (rmapDir.exists() && radiomapFile.exists() && meanFile.exists()) {
            try {
              val folder = rmapDir.toString
              val radiomap_filename = getRadioMapFileName(buid, floor_number).getAbsolutePath
              var radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt")
              var radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt")
              var radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt")
              val api = AnyplaceServerAPI.SERVER_API_ROOT
              var pos = getFilePos(radiomap_mean_filename)
              radiomap_mean_filename = api + radiomap_mean_filename.substring(pos)
              pos = getFilePos(radiomap_rbf_weights_filename)
              radiomap_rbf_weights_filename = api + radiomap_rbf_weights_filename.substring(pos)
              pos = getFilePos(radiomap_parameters_filename)
              radiomap_parameters_filename = api + radiomap_parameters_filename.substring(pos)
            } catch {
              case e: Exception => return AnyResponseHelper.internal_server_error("Error serving radiomap : " + e.getMessage)
            }
          }
          if (!rmapDir.exists())
            if (!rmapDir.mkdirs()) {
              return AnyResponseHelper.internal_server_error("Error while creating Radio Map on-the-fly!")
            }
          val radio = new File(rmapDir.getAbsolutePath + AnyplaceServerAPI.URL_SEP + "rss-log")
          var fout: FileOutputStream = null
          try {
            fout = new FileOutputStream(radio)
          } catch {
            case e: FileNotFoundException => return AnyResponseHelper.internal_server_error(
              "Cannot create radiomap:3:" + e.getMessage)
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
            case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
          }
          if (floorFetched != 0) {

            try {
              val folder = rmapDir.toString
              val radiomap_filename = getRadioMapFileName(buid, floor_number).getAbsolutePath
              var radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt")
              var radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt")
              var radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt")
              val rm = new RadioMap(new File(folder), radiomap_filename, "", -110)
              val resCreate = rm.createRadioMap()
              if (resCreate != null) {
                return AnyResponseHelper.internal_server_error("radioDownloadByBuildingFloorall: Error: on-the-fly radioMap: " + resCreate)
              }
              val api = AnyplaceServerAPI.SERVER_API_ROOT
              var pos = getFilePos(radiomap_mean_filename)
              radiomap_mean_filename = api + radiomap_mean_filename.substring(pos)
              pos = getFilePos(radiomap_rbf_weights_filename)
              radiomap_rbf_weights_filename = api + radiomap_rbf_weights_filename.substring(pos)
              pos = getFilePos(radiomap_parameters_filename)
              radiomap_parameters_filename = api + radiomap_parameters_filename.substring(pos)
            } catch {
              case e: Exception => return AnyResponseHelper.internal_server_error("Error while creating Radio Map on-the-fly! : " + e.getMessage)
            }


            val source = scala.io.Source.fromFile(rmapDir.getAbsolutePath + AnyplaceServerAPI.URL_SEP + "indoor-radiomap.txt")
            val lines = try source.mkString finally source.close()
            radiomap_mean_filename.add(floor_number)
            rss_log_files.add(lines)
          }
          else {
            radiomap_mean_filename.add("")
          }
        }

        val res: JsValue = Json.obj("map_url_mean" -> radiomap_mean_filename.toList,
          "rss_log_files" -> rss_log_files.toList)
        return AnyResponseHelper.ok(res, "Successfully served radio map.")
      }

      inner(request)
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
        line = bf.readLine
        line != null
      }) {
        if (line.startsWith("# Timestamp")) //continue
          lineNumber += 1
        val segs = line.split(" ")
        val rmr = new RadioMapRaw(segs(0), segs(1), segs(2), segs(3), segs(4), segs(5), segs(6))
        LPLogger.info(rmr.toValidJson().toString)
        LPLogger.debug("raw[" + lineNumber + "] : " + rmr.toValidJson())
        try {
          if (!ProxyDataSource.getIDatasource.addJsonDocument(rmr.getId, 0, rmr.toGeoJSON())) {
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

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request, false)
        if (!anyReq.assertJsonBody()) {
          AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody
        //LPLogger.info("AnyplacePosition::serveRadioMap(): " + json.toString)
        val filePath = "radiomaps" + AnyplaceServerAPI.URL_SEP + radio_folder + AnyplaceServerAPI.URL_SEP +
          fileName
        LPLogger.info("requested: " + filePath)
        val file = new File(filePath)
        try {
          if (!file.exists()) return AnyResponseHelper.bad_request("Requested file does not exist");
          if (!file.canRead()) return AnyResponseHelper.bad_request("Requested file cannot be read: " +
            fileName)
          Ok.sendFile(file)
        } catch {
          case e: FileNotFoundException => AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  def serveFrozenRadioMap(building: String, floor: String, fileName: String) = Action {

    def inner(): Result = {

      //FeatureAdd : Configuring location for server generated files
      val radioMapsFrozenDir = Play.application().configuration().getString("radioMapFrozenDir")

      val filePath = radioMapsFrozenDir + AnyplaceServerAPI.URL_SEP + building + AnyplaceServerAPI.URL_SEP +
        floor +
        AnyplaceServerAPI.URL_SEP +
        fileName
      LPLogger.info("requested: " + filePath)
      val file = new File(filePath)
      try {
        if (!file.exists()) return AnyResponseHelper.bad_request("Requested file does not exist");
        if (!file.canRead()) return AnyResponseHelper.bad_request("Requested file cannot be read: " +
          fileName)
        Ok.sendFile(file)
      } catch {
        case e: FileNotFoundException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
      }
    }

    inner()
  }

  /**
   * Processes a row rss log file (the one that was uploaded)
   * This is a raw rss window (optimised by mongoDB)
   *
   * @param values
   * @return
   */
  private def storeFloorRssWindowToDB(values: ArrayList[String]): String = {
    if (values.size > 0) {
      var maxMac = ""
      var maxRss = java.lang.Integer.MIN_VALUE
      var i = 0
      while (i < values.size()) {
        val value = values.get(i)
        val tok = value.split(" ")
        try {
          val mac = tok(4)
          val rss = java.lang.Integer.parseInt(tok(5))
          if (rss > maxRss) {
            maxRss = rss
            maxMac = mac
          }
        } catch {
          case ex: NumberFormatException => if (values.remove(value)) i -= 1
        }
        i += 1
      }
      val fingerprintToks = values.get(0).split(" ")
      val tempMeasurements = new ArrayList[List[String]]
      // creating measurements for the currect window
      for (value <- values) {
        val tok = value.split(" ")
        val measurement = new ArrayList[String]
        measurement.add(tok(4)) // adding MAC
        measurement.add(tok(5)) // adding heading
        tempMeasurements.add(measurement.toList)
      }
      val measurements: List[List[String]] = tempMeasurements.toList
      // creating fingerprint with measurements
      var rmr: RadioMapRaw = null
      if (fingerprintToks.length >= 8) {
        rmr = new RadioMapRaw(fingerprintToks(0), fingerprintToks(1), fingerprintToks(2), fingerprintToks(3),
          /*tok(4), tok(5),*/ fingerprintToks(6), maxMac, fingerprintToks(7))
      } else if (fingerprintToks.length >= 7) {
        rmr = new RadioMapRaw(fingerprintToks(0), fingerprintToks(1), fingerprintToks(2), fingerprintToks(3),
          /*tok(4), tok(5),*/ fingerprintToks(6), maxMac)
      } else {
        return "Some fields are missing from the log."
      }

      // Before add check if already exists, if exists ignore and notify
      if (ProxyDataSource.getIDatasource().fingerprintExists(SCHEMA.cFingerprintsWifi, fingerprintToks(7),
        fingerprintToks(6), fingerprintToks(1), fingerprintToks(2), fingerprintToks(3))) {
        return 1 + ""
      } else {
        try {
          ProxyDataSource.getIDatasource.addJsonDocument(SCHEMA.cFingerprintsWifi, rmr.addMeasurements(measurements))
          ProxyDataSource.getIDatasource().deleteAffectedHeatmaps(fingerprintToks(7), fingerprintToks(6))
        } catch {
          case e: DatasourceException => return "Internal server error while trying to save rss entry."
        }
        return 0 + ""
      }
    }
    null
  }

  def isAllDigits(x: String) = x forall Character.isDigit

  private def storeFloorRssToDB(infile: File): String = {
    var line: String = null
    var fr: FileReader = null
    var bf: BufferedReader = null
    var totalRss = -1
    var totalExists = 0
    try {
      fr = new FileReader(infile)
      bf = new BufferedReader(fr)
      val values = new ArrayList[String](10)
      while ( {
        line = bf.readLine;
        line != null
      }) {
        if (line.startsWith("# Timestamp")) {
          val result = storeFloorRssWindowToDB(values)
          totalRss += 1
          if (result != null && !isAllDigits(result)) return result
          if (result != null) {
            if (isAllDigits(result)) {
              totalExists += Integer.parseInt(result)
            }
          }
          values.clear()
        } else {
          values.add(line)
        }
      }
      val result = storeFloorRssWindowToDB(values)
      totalRss += 1
      if (result != null && !isAllDigits(result))
        return result
      if (result != null) {
        if (isAllDigits(result)) {
          totalExists += Integer.parseInt(result)
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
    return totalExists + "/" + totalRss
  }

  def predictFloorAlgo1() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody
        val watch = new StopWatch()
        watch.start()
        LPLogger.info("AnyplacePosition::predictFloor(): ")
        var alg1: Algo1 = null
        try {
          alg1 = new Algo1(json)
        } catch {
          case ex: Exception => return AnyResponseHelper.bad_request(ex.getMessage)
        }
        LPLogger.debug("im here")
        try {
          val lat = json.\("dlat").as[Double]
          val lot = json.\("dlong").as[Double]
          val bbox = GeoPoint.getGeoBoundingBox(lat, lot, 100)
          val strongestMAC = new ArrayList[String](2)
          if (json.\("first").getOrElse(null) != null) return AnyResponseHelper.bad_request("Sent first Wifi")
          strongestMAC.add(json.\("first").\(SCHEMA.fMac).as[String])
          if (json.\("second").getOrElse(null) != null) strongestMAC.add(json.\("second").\(SCHEMA.fMac).as[String])
          val res = JsonObject.empty()
          if (ProxyDataSource.getIDatasource.predictFloor(alg1, bbox, strongestMAC.toArray(Array.ofDim[String](1)))) {
            res.put(SCHEMA.fFloor, alg1.getFloor)
          } else {
            res.put(SCHEMA.fFloor, "")
          }
          watch.stop()
          LPLogger.info("Time for Algo1 is millis: " + watch.getNanoTime / 1000000)
          AnyResponseHelper.ok(res, "Successfully predicted Floor.")
        } catch {
          case e: Exception => AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  /**
   * Now this is synchronously called.
   *
   * @param buid
   * @param floor_number
   * @return a status regarding the frozen radio_map creation.
   */
  def updateFrozenRadioMap(buid: String, floor_number: String): String = {
    val cls = "updateFrozenRadioMap: "
    if (!Floor.checkFloorNumberFormat(floor_number)) {
      return null
    }
    LPLogger.info(cls + buid + ":" + floor_number)

    val radioMapsFrozenDir = Play.application().configuration().getString("radioMapFrozenDir")

    val rmapDir = new File(radioMapsFrozenDir + AnyplaceServerAPI.URL_SEP + buid + AnyplaceServerAPI.URL_SEP +
      floor_number)

    if (!rmapDir.exists() && !rmapDir.mkdirs()) {
      return cls + "failed to create: " + rmapDir.toString
    }
    val rssLogPerFloor = new File(rmapDir.getAbsolutePath + AnyplaceServerAPI.URL_SEP + "rss-log")
    var fout: FileOutputStream = null
    try {
      fout = new FileOutputStream(rssLogPerFloor)
      LPLogger.D1(cls + "Creating rss-log: " + rssLogPerFloor.toPath().getFileName.toString)
    } catch {
      case e: FileNotFoundException => return cls + e.getClass + ": " + e.getMessage
    }
    var floorFetched: Long = 0l
    try {
      floorFetched = ProxyDataSource.getIDatasource.dumpRssLogEntriesByBuildingFloor(fout, buid, floor_number)
      fout.close()
    } catch {
      case e: DatasourceException => return cls + e.getClass + ": " + e.getMessage
      case e: IOException => return cls + e.getClass + " Error while closing rss-log: " + e.getMessage
    }
    if (floorFetched == 0) {
      return null
    }
    val radiomap_filename = new File(rmapDir.toString + AnyplaceServerAPI.URL_SEP + "indoor-radiomap.txt")
      .getAbsolutePath
    val rm = new RadioMap(new File(rmapDir.toString), radiomap_filename, "", -110)
    val resCreate = rm.createRadioMap()
    if (resCreate != null) return cls + "Failed: createRadioMap: " + resCreate
    null
  }

  def magneticPathAdd() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMagnetic::pathAdd(): " + json.toString)
        val requiredMissing = JsonUtils.hasProperties(json, "lat_a", "lng_a", "lat_b", "lng_b",
          SCHEMA.fBuid, "floor_num")
        if (!requiredMissing.isEmpty) {
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        }
        try {
          var mpath: MagneticPath = null
          try {
            mpath = new MagneticPath(json.asInstanceOf[JsonObject])
          } catch {
            case e: NumberFormatException => return AnyResponseHelper.bad_request("Magnetic Path coordinates are invalid!")
          }
          if (!ProxyDataSource.getIDatasource.addJsonDocument(mpath.getId, 0, mpath.toValidJson().toString)) {
            return AnyResponseHelper.bad_request("MPath already exists or could not be added!")
          }
          val res = JsonObject.empty()
          res.put("mpath", mpath.getId)
          return AnyResponseHelper.ok(res, "Successfully added magnetic path!")
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  def magneticPathDelete() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMagnetic::magneticPathDelete(): " + json.toString)
        val requiredMissing = JsonUtils.hasProperties(json, "mpuid")
        if (!requiredMissing.isEmpty) {
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        }
        val mpuid = (json \ "mpuid").as[String]
        try {
          val success = ProxyDataSource.getIDatasource.deleteFromKey(mpuid)
          if (!success) {
            return AnyResponseHelper.bad_request("Magnetic Path does not exist or could not be retrieved!")
          }
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
        return AnyResponseHelper.ok("Successfully deleted magnetic path!")
      }

      inner(request)
  }

  def magneticPathByFloor() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::poisByFloor(): " + json.toString)
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, "floor_num")
        if (!requiredMissing.isEmpty) {
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        }
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floor_number = (json \ "floor_num").as[String]
        try {
          val mpaths = ProxyDataSource.getIDatasource.magneticPathsByBuildingFloorAsJson(buid, floor_number)
          val res = JsonObject.empty()
          res.put("mpaths", JsonArray.from(mpaths))
          return AnyResponseHelper.ok(res.toString)
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  def magneticPathByBuilding() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::mpsByBuilding(): " + json.toString)
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid)
        if (!requiredMissing.isEmpty) {
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        }
        val buid = (json \ SCHEMA.fBuid).as[String]
        try {
          val mpaths = ProxyDataSource.getIDatasource.magneticPathsByBuildingAsJson(buid)
          val res = JsonObject.empty()
          res.put("mpaths", JsonArray.from(mpaths))
          return AnyResponseHelper.ok(res.toString)
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  def magneticMilestoneUpload() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::mpsByBuilding(): " + json.toString)
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, "floor_num", "mpuid", "milestones")
        if (!requiredMissing.isEmpty) {
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        }
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floor_num = (json \ "floor_num").as[String]
        val mpuid = (json \ "mpuid").as[String]
        var milestones: JsonArray = JsonArray.empty()
        try {
          JsonArray.fromJson((json \ "milestones").as[String])
        } catch {
          case ioe: IOException => return AnyResponseHelper.internal_server_error("milestones could not be parsed")
        }
        for (jn <- milestones.toList.asInstanceOf[List[JsonObject]]) {
          val mm = new MagneticMilestone(jn, buid, floor_num, mpuid)
          try {
            if (!ProxyDataSource.getIDatasource.addJsonDocument(mm.getId, 0, mm.toValidJson().toString)) {
              return AnyResponseHelper.bad_request("Milestone already exists or could not be added!")
            }
          } catch {
            case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
          }
        }
        return AnyResponseHelper.ok("ok")
      }

      inner(request)
  }

  def magneticMilestoneByFloor() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return AnyResponseHelper.bad_request(AnyResponseHelper.CANNOT_PARSE_BODY_AS_JSON)
        }
        val json = anyReq.getJsonBody
        LPLogger.info("AnyplaceMapping::milestonesByFloor(): " + json.toString)
        val requiredMissing = JsonUtils.hasProperties(json, SCHEMA.fBuid, "floor_num")
        if (!requiredMissing.isEmpty) {
          return AnyResponseHelper.requiredFieldsMissing(requiredMissing)
        }
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floor_number = (json \ "floor_num").as[String]
        try {
          val mpaths = ProxyDataSource.getIDatasource.magneticMilestonesByBuildingFloorAsJson(buid, floor_number)
          val res = JsonObject.empty()
          res.put("mpaths", JsonArray.from(mpaths))
          return AnyResponseHelper.ok(res.toString)
        } catch {
          case e: DatasourceException => return AnyResponseHelper.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

}
