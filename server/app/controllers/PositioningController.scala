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
import datasources.{DatasourceException, ProxyDataSource, SCHEMA}
import models.{Floor, RadioMapRaw}
import modules.floor.Algo1
import javax.inject.{Inject, Singleton}
import json.VALIDATE
import json.VALIDATE.StringNumber
import oauth.provider.v2.models.OAuth2Request
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import modules.radiomapserver.RadioMap
import modules.radiomapserver.RadioMap.{RBF_ENABLED, RadioMap}
import utils._

import scala.jdk.CollectionConverters.CollectionHasAsScala

@Singleton
class PositioningController @Inject()(cc: ControllerComponents,
                                      mapHelper: helper.Mapping,
                                      api: AnyplaceServerAPI,
                                      conf: Configuration,
                                      fu: FileUtils,
                                      proxyDataSource: ProxyDataSource,
                                      user: helper.User)
  extends AbstractController(cc) {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  /**
   * Upload fingerprints to server and database.
   *
   * @return
   */
  def radioUpload() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val body = anyReq.getMultipartFormData()
        if (body == null) {return RESPONSE.BAD("Invalid request type: Not multipart.")}
        var rssLog: File = null
        try {
          rssLog = body.file("radiomap").get.ref.path.toFile
        } catch {
          case e: Exception => return RESPONSE.BAD("Cannot find radiomap (rss log).")
        }
        val body_form = body.asFormUrlEncoded
        if (body_form == null) {
          return RESPONSE.BAD("Invalid request type - Cannot be parsed as form data.")
        }
        if (!body_form.contains(SCHEMA.fAccessToken)) {
          // TODO:NN not authenticated
          return RESPONSE.BAD("Cannot find access_token in the request.")
        }
        val access_token = body_form.get(SCHEMA.fAccessToken).get.head
        if (user.authorize(Json.obj(SCHEMA.fAccessToken -> access_token)) == null) return RESPONSE.FORBIDDEN("Unauthorized")
        var ret: String = ""
        val newBuildingsFloors = RadioMap.verifyRssLogAndGetBuildingFloors(rssLog)
        if (newBuildingsFloors == null) {
          return RESPONSE.BAD("Uploaded a corrupted rss-log file.")
        } else {
          mapHelper.storeRadioMapRawToServer(rssLog)
          ret = storeFloorRssToDB(rssLog)
          LOG.D2("RSS values already exist: " + ret)
          val errors: ArrayList[JsValue] = new ArrayList[JsValue]
          for (buid <- newBuildingsFloors.keySet.asScala) {
            for (floor_num <- newBuildingsFloors.get(buid).asScala) {
              val res = mapHelper.updateFrozenRadioMap(buid, floor_num)
              if (res != null) {
                val js = Json.obj(SCHEMA.fBuid -> buid, "floor_num" -> floor_num, "error" -> res)
                errors.add(js)
              }
            }
          }
          if (errors.size > 0) {
            val json = Json.obj("errorList" -> errors.asScala)
            return RESPONSE.BAD(json, "Failed to create frozen radio maps.")
          }
        }
        val msg = "Successfully uploaded rss log."
        LOG.D1(msg)
        return RESPONSE.OK(msg)
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
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        }
        val json = anyReq.getJsonBody()
        LOG.D2("radioDownloadFloor: " + json.toString)
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fCoordinatesLat, SCHEMA.fCoordinatesLon, SCHEMA.fFloorNumber)
        if (checkRequirements != null) return checkRequirements
        // range is large enough to cover the entire floor
        return mapHelper.findRadioBbox(json, mapHelper.BBOX_MAX)
      }

      inner(request)
  }

  def radioDownloadFloorBbox() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        }
        val json = anyReq.getJsonBody()
        LOG.D2("radioDownloadFloorBbox: " + json.toString)
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fCoordinatesLat, SCHEMA.fCoordinatesLon,
          SCHEMA.fFloorNumber, "range")
        if (checkRequirements != null) return checkRequirements
        if (StringNumber(json, "range") == null) {
          return RESPONSE.BAD("range field must be String, containing a number!")
        }
        var range = (json \ "range").as[String].toInt
        if (range > mapHelper.BBOX_MAX) range = mapHelper.BBOX_MAX
        return mapHelper.findRadioBbox(json, range)
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
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        }
        val json = anyReq.getJsonBody()
        LOG.D2("radioDownloadByBuildingFloor: " + json.toString)
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fFloor, SCHEMA.fBuid)
        if (checkRequirements != null) return checkRequirements
        val floor_number = (json \ SCHEMA.fFloor).as[String]
        val buid = (json \ SCHEMA.fBuid).as[String]
        if (!Floor.checkFloorNumberFormat(floor_number)) {
          return RESPONSE.BAD("Floor number cannot contain whitespace!")
        }
        val rmapDir = fu.getDirFrozenFloor(buid, floor_number)
        val radiomapFile = fu.getRadiomapFile(buid, floor_number)
        val meanFile = fu.getMeanFile(buid, floor_number)
        if (rmapDir.exists() && radiomapFile.exists() && meanFile.exists()) {
          try {
            val radiomap_filename = fu.getRadioMapFileName(buid, floor_number).getAbsolutePath
            var radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt")
            var radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt")
            var radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt")
            val url: String = api.SERVER_API_ROOT
            var pos = fu.getFilePos(radiomap_mean_filename)
            radiomap_mean_filename = url + radiomap_mean_filename.substring(pos)
            pos = fu.getFilePos(radiomap_rbf_weights_filename)
            radiomap_rbf_weights_filename = url + radiomap_rbf_weights_filename.substring(pos)
            pos = fu.getFilePos(radiomap_parameters_filename)
            radiomap_parameters_filename = url + radiomap_parameters_filename.substring(pos)
            var res: JsValue = null

            if (RBF_ENABLED) {
              res = Json.obj("map_url_mean" -> radiomap_mean_filename,
                "map_url_weights" -> radiomap_rbf_weights_filename,
                "map_url_parameters" -> radiomap_parameters_filename)
            } else {
              res = Json.obj("map_url_mean" -> radiomap_mean_filename)
            }
            return RESPONSE.OK(res, "Successfully served radiomap floor.")
          } catch {
            case e: Exception => return RESPONSE.ERROR("radioDownloadByBuildingFloor", e)
          }
        }
        if (!rmapDir.mkdirs()) {
          return RESPONSE.internal_server_error("Can't create radiomap on-the-fly.")
        }
        val radio = new File(rmapDir.getAbsolutePath + api.URL_SEP + "rss-log")
        var fout: FileOutputStream = null
        var floorFetched: Long = 0L
        try {
          fout = new FileOutputStream(radio)
          floorFetched = proxyDataSource.getIDatasource.dumpRssLogEntriesByBuildingFloor(fout, buid, floor_number)
          fout.close()

          if (floorFetched == 0) {
            return RESPONSE.BAD("Area not supported yet!")
          }

          val folder = rmapDir.toString
          val radiomap_filename = new File(folder + api.URL_SEP + "indoor-radiomap.txt")
            .getAbsolutePath
          var radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt")
          var radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt")
          var radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt")
          val rm = new RadioMap(new File(folder), radiomap_filename, "", -110)
          val resCreate = rm.createRadioMap()
          if (resCreate != null) {
            return RESPONSE.internal_server_error("radioDownloadByBuildingFloor: radiomap on-the-fly: " + resCreate)
          }
          val url = api.SERVER_API_ROOT
          var pos = radiomap_mean_filename.indexOf("radiomaps_frozen")
          radiomap_mean_filename = url + radiomap_mean_filename.substring(pos)
          pos = radiomap_rbf_weights_filename.indexOf("radiomaps_frozen")
          radiomap_rbf_weights_filename = url + radiomap_rbf_weights_filename.substring(pos)
          pos = radiomap_parameters_filename.indexOf("radiomaps_frozen")
          radiomap_parameters_filename = url + radiomap_parameters_filename.substring(pos)
          val res: JsValue = Json.obj("map_url_mean" -> radiomap_mean_filename,
            "map_url_weights" -> radiomap_rbf_weights_filename,
            "map_url_parameters" -> radiomap_parameters_filename)
          return RESPONSE.OK(res, "Successfully created radio map.")
        } catch {
          case e: Exception => return RESPONSE.ERROR("radioDownloadByBuildingFloor: " +
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
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        }
        val json = anyReq.getJsonBody()
        LOG.D2("radioDownloadByBuildingFloorall: " + json.toString)
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fFloor, SCHEMA.fBuid)
        if (checkRequirements != null) return checkRequirements
        val floor_number = (json \ SCHEMA.fFloor).as[String]
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floors = floor_number.split(" ")
        val radiomap_mean_filename = new java.util.ArrayList[String]()
        val rss_log_files = new java.util.ArrayList[String]()

        for (floor_number <- floors) {
          val rmapDir = fu.getDirFrozenFloor(buid, floor_number)
          val radiomapFile = fu.getRadiomapFile(buid, floor_number)
          val meanFile = fu.getMeanFile(buid, floor_number)
          if (rmapDir.exists() && radiomapFile.exists() && meanFile.exists()) {
            try {
              val folder = rmapDir.toString
              val radiomap_filename = fu.getRadioMapFileName(buid, floor_number).getAbsolutePath
              var radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt")
              var radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt")
              var radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt")
              val url = api.SERVER_API_ROOT
              var pos = fu.getFilePos(radiomap_mean_filename)
              radiomap_mean_filename = url + radiomap_mean_filename.substring(pos)
              pos = fu.getFilePos(radiomap_rbf_weights_filename)
              radiomap_rbf_weights_filename = url + radiomap_rbf_weights_filename.substring(pos)
              pos = fu.getFilePos(radiomap_parameters_filename)
              radiomap_parameters_filename = url + radiomap_parameters_filename.substring(pos)
            } catch {
              case e: Exception => return RESPONSE.internal_server_error("Error serving radiomap : " + e.getMessage)
            }
          }
          if (!rmapDir.exists())
            if (!rmapDir.mkdirs()) {
              return RESPONSE.internal_server_error("Error while creating Radio Map on-the-fly.")
            }
          val radio = new File(rmapDir.getAbsolutePath + api.URL_SEP + "rss-log")
          var fout: FileOutputStream = null
          try {
            fout = new FileOutputStream(radio)
          } catch {
            case e: FileNotFoundException => return RESPONSE.internal_server_error(
              "Cannot create radiomap:3: " + e.getMessage)
          }
          var floorFetched: Long = 0L
          try {
            floorFetched = proxyDataSource.getIDatasource.dumpRssLogEntriesByBuildingFloor(fout, buid, floor_number)
            try {
              fout.close()
            } catch {
              case e: IOException => LOG.E("Error while closing the file output stream for the dumped rss logs")
            }
          } catch {
            case e: DatasourceException => return RESPONSE.internal_server_error("500: " + e.getMessage)
          }
          if (floorFetched != 0) {

            try {
              val folder = rmapDir.toString
              val radiomap_filename = fu.getRadioMapFileName(buid, floor_number).getAbsolutePath
              var radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt")
              var radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt")
              var radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt")
              val rm = new RadioMap(new File(folder), radiomap_filename, "", -110)
              val resCreate = rm.createRadioMap()
              if (resCreate != null) {
                return RESPONSE.internal_server_error("radioDownloadByBuildingFloorall: Error: on-the-fly radioMap: " + resCreate)
              }
              val url = api.SERVER_API_ROOT
              var pos = fu.getFilePos(radiomap_mean_filename)
              radiomap_mean_filename = url + radiomap_mean_filename.substring(pos)
              pos = fu.getFilePos(radiomap_rbf_weights_filename)
              radiomap_rbf_weights_filename = url + radiomap_rbf_weights_filename.substring(pos)
              pos = fu.getFilePos(radiomap_parameters_filename)
              radiomap_parameters_filename = url + radiomap_parameters_filename.substring(pos)
            } catch {
              case e: Exception => return RESPONSE.internal_server_error("Error while creating Radio Map on-the-fly! : " + e.getMessage)
            }

            val source = scala.io.Source.fromFile(rmapDir.getAbsolutePath + api.URL_SEP + "indoor-radiomap.txt")
            val lines = try source.mkString finally source.close()
            radiomap_mean_filename.add(floor_number)
            rss_log_files.add(lines)
          }
          else {
            radiomap_mean_filename.add("")
          }
        }

        val res: JsValue = Json.obj("map_url_mean" -> radiomap_mean_filename.asScala,
          "rss_log_files" -> rss_log_files.asScala)
        return RESPONSE.OK(res, "Successfully served radio map.")
      }

      inner(request)
  }

  def serveRadioMap(radio_folder: String, fileName: String) = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request, false)
        if (!anyReq.assertJsonBody()) {
          RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        }
        val json = anyReq.getJsonBody()
        //LPLogger.info("AnyplacePosition::serveRadioMap(): " + json.toString)
        val filePath = "radiomaps" + api.URL_SEP + radio_folder + api.URL_SEP +
          fileName
        LOG.D2("serveRadioMap: requested: " + filePath)
        val file = new File(filePath)
        try {
          if (!file.exists()) return RESPONSE.BAD("Requested file does not exist");
          if (!file.canRead()) return RESPONSE.BAD("Requested file cannot be read: " +
            fileName)
          Ok.sendFile(file)
        } catch {
          case e: FileNotFoundException => RESPONSE.internal_server_error("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  def serveFrozenRadioMap(building: String, floor: String, fileName: String) = Action {
    def inner(): Result = {
      val radioMapsFrozenDir = conf.get[String]("radioMapFrozenDir")
      val filePath = radioMapsFrozenDir + api.URL_SEP + building + api.URL_SEP +
        floor +
        api.URL_SEP +
        fileName
      LOG.D2("serveFrozenRadioMap: requested: " + filePath)
      val file = new File(filePath)
      try {
        if (!file.exists()) return RESPONSE.BAD("Requested file does not exist");
        if (!file.canRead()) return RESPONSE.BAD("Requested file cannot be read: " +
          fileName)
        Ok.sendFile(file)
      } catch {
        case e: FileNotFoundException => return RESPONSE.internal_server_error("500: " + e.getMessage)
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
      for (value <- values.asScala) {
        val tok = value.split(" ")
        val measurement = new ArrayList[String]
        measurement.add(tok(4)) // adding MAC
        measurement.add(tok(5)) // adding heading
        tempMeasurements.add(measurement.asScala.toList)
      }
      val measurements: List[List[String]] = tempMeasurements.asScala.toList
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
      if (proxyDataSource.getIDatasource.fingerprintExists(SCHEMA.cFingerprintsWifi, fingerprintToks(7),
        fingerprintToks(6), fingerprintToks(1), fingerprintToks(2), fingerprintToks(3))) {
        return 1.toString()
      } else {
        try {
          proxyDataSource.getIDatasource.addJsonDocument(SCHEMA.cFingerprintsWifi, rmr.addMeasurements(measurements))
          proxyDataSource.getIDatasource.deleteAffectedHeatmaps(fingerprintToks(7), fingerprintToks(6))
        } catch {
          case e: DatasourceException => return "Internal server error while trying to save rss entry."
        }
        return 0.toString()
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
    return totalExists.toString + "/" + totalRss.toString
  }

  /**
   *
   * @return
   */
  def predictFloorAlgo1() = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        LOG.D2("predictFloorAlgo1:")
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        }
        val json = anyReq.getJsonBody()
        var alg1: Algo1 = null
        try {
          alg1 = new Algo1(json)
        } catch {
          case ex: Exception => {
            return RESPONSE.BAD(ex.getClass.toString() + ": " + ex.getMessage + ": " + ex.getCause.toString())
          }
        }
        try {
          val lat = json.\("dlat").as[Double]
          val lot = json.\("dlong").as[Double]
          val bbox = GeoPoint.getGeoBoundingBox(lat, lot, 100)
          val strongestMAC = new ArrayList[String](2)
          if (json.\("first").getOrElse(null) == null) return RESPONSE.BAD("Sent Wi-Fi first parameter.")
          strongestMAC.add(json.\("first").\(SCHEMA.fMac).as[String])
          if (json.\("second").getOrElse(null) != null) strongestMAC.add(json.\("second").\(SCHEMA.fMac).as[String])
          LOG.D2("strongestMAC " + strongestMAC)
          var res: JsValue = Json.obj()
          var msg = ""
          if (proxyDataSource.getIDatasource.predictFloor(alg1, bbox, strongestMAC.toArray(Array.ofDim[String](1)))) {
            res = Json.obj(SCHEMA.fFloor -> alg1.getFloor())
            msg = "Successfully predicted floor."
          } else {
            res = Json.obj(SCHEMA.fFloor -> "")
            msg = "Could not predict floor."
          }
          RESPONSE.OK(res, msg)
        } catch {
          case e: Exception => RESPONSE.internal_server_error("500: " + e.getMessage + ": " + e.getCause)
        }
      }

      inner(request)
  }
}
