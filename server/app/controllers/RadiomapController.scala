package controllers

import datasources.{DatasourceException, ProxyDataSource, SCHEMA}
import models._
import models.oauth.OAuth2Request
import modules.radiomapserver.{RadioMap, RadioMapMean}
import modules.radiomapserver.RadioMap.{RBF_ENABLED, RadioMap}
import play.api.Configuration
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc._
import utils._
import utils.json.VALIDATE
import utils.json.VALIDATE.StringNumber
import java.io._
import java.util
import java.util.ArrayList
import javax.inject.{Inject, Singleton}
import scala.concurrent.Future
import scala.jdk.CollectionConverters.CollectionHasAsScala

@Singleton
class RadiomapController @Inject()(cc: ControllerComponents,
                                   conf: Configuration,
                                   mapHelper: helper.Mapping,
                                   api: AnyplaceServerAPI,
                                   fu: FileUtils,
                                   pds: ProxyDataSource,
                                   user: helper.User)
  extends AbstractController(cc) {
  implicit val ec: scala.concurrent.ExecutionContext = scala.concurrent.ExecutionContext.global

  /**
   * Delete fingeprints within a bounding-box. Also delete heatmap caches.
   *
   * @return deleted fingerprints (so JS update UI)
   */
  def delete(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("Radiomap: delete: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(
          json, SCHEMA.fBuid, SCHEMA.fFloor, "lat1", "lon1", "lat2", "lon2")
        if (checkRequirements != null) return checkRequirements
        val owner_id = user.authorize(apiKey)
        if (owner_id == null) return RESPONSE.UNAUTHORIZED_USER
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floorNum = (json \ SCHEMA.fFloor).as[String]
        val lat1 = (json \ "lat1").as[String]
        val lon1 = (json \ "lon1").as[String]
        val lat2 = (json \ "lat2").as[String]
        val lon2 = (json \ "lon2").as[String]
        try {
          val fingerprints: List[JsValue] = pds.db.getFingerPrintsBBox(
            buid, floorNum, lat1, lon1, lat2, lon2)
          if (fingerprints.isEmpty)
            return RESPONSE.BAD_CANNOT_RETRIEVE_FINGERPRINTS_WIFI

          LOG.D2("Radiomap: delete: " + fingerprints.size + " fingerprints.")
          for (fingerprint <- fingerprints) {
            pds.db.deleteFingerprint(fingerprint)
          }
          pds.db.deleteCachedDocuments(buid,floorNum)
          val res: JsValue = Json.obj("fingerprints" -> fingerprints)
          Future { mapHelper.updateFrozenRadioMap(buid, floorNum) }(ec)
          return RESPONSE.gzipJsonOk(res, "Deleted " + fingerprints.size + " fingerprints and returning them.")
        } catch {
          case e: Exception =>
            return RESPONSE.ERROR_INTERNAL("FingerPrintsDelete: " + e.getClass + ": " + e.getMessage)
        }
      }

      inner(request)
  }

  def deleteTimestamp(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("Radiomap: deleteTimestamp: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(
          json, SCHEMA.fBuid, SCHEMA.fFloor, "lat1", "lon1", "lat2", "lon2", SCHEMA.fTimestampX, SCHEMA.fTimestampY)
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floorNum = (json \ SCHEMA.fFloor).as[String]
        val lat1 = (json \ "lat1").as[String]
        val lon1 = (json \ "lon1").as[String]
        val lat2 = (json \ "lat2").as[String]
        val lon2 = (json \ "lon2").as[String]
        val timestampX = (json \ SCHEMA.fTimestampX).as[String]
        val timestampY = (json \ SCHEMA.fTimestampY).as[String]
        try {
          val fingerprints: List[JsValue] = pds.db.getFingerPrintsTimestampBBox(
            buid, floorNum, lat1, lon1, lat2, lon2, timestampX, timestampY)
          if (fingerprints.isEmpty)
            return RESPONSE.BAD_CANNOT_RETRIEVE_FINGERPRINTS_WIFI
          for (fingerprint <- fingerprints)
            pds.db.deleteFingerprint(fingerprint)
          pds.db.deleteCachedDocuments(buid,floorNum)
          val res: JsValue = Json.obj("radioPoints" -> fingerprints)
          try {
            Future { mapHelper.updateFrozenRadioMap(buid, floorNum) }(ec)
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case _: IOException =>
              return RESPONSE.OK(res, "Successfully retrieved all Fingerprints.")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  /**
   * Called when "Show Fingerprints By Time" (Architect: toggleFingerPrintsTime) is clicked.
   * Used to return the data that will be shown in the crossfilter bar.
   *
   * @return a list of the number of fingerprints stored, and date.
   */
  def byTime(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody())
          return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("Radiomap: byTime: " + Utils.stripJson(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid, SCHEMA.fFloor)
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floorNum = (json \ SCHEMA.fFloor).as[String]

        // create cache-collections
        pds.db.cacheHeatmapByTime(SCHEMA.cHeatmapWifiTimestamp1, buid, floorNum, 1)
        pds.db.cacheHeatmapByTime(SCHEMA.cHeatmapWifiTimestamp2, buid, floorNum, 2)
        pds.db.cacheHeatmapByTime(SCHEMA.cHeatmapWifiTimestamp3, buid, floorNum, 3)

        try {
          val radioPoints: List[JsValue] = pds.db.getFingerprintsByTime(buid, floorNum)
          if (radioPoints.isEmpty) return RESPONSE.BAD("Fingerprints do not exist.")
          val res: JsValue = Json.obj("radioPoints" -> radioPoints)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case ioe: IOException =>
              return RESPONSE.OK(res, "Successfully retrieved all Fingerprints.")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }
      inner(request)
  }

  def deleteBoundingBox(): Action[AnyContent] = Action {
    implicit request =>

      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("Radiomap: deleteBoundingBox: " + Utils.stripJson(json))
        try {
          if (!pds.db.deleteRadiosInBox()) {
            return RESPONSE.BAD_CANNOT_ADD_SPACE
          }
          return RESPONSE.OK("Success")
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }

  /**
   * Upload fingerprints to server and database.
   *
   * @return
   */
  def upload(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        val apiKey = anyReq.getAccessToken()
        if (apiKey == null) return anyReq.NO_ACCESS_TOKEN()

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
  def getFloor(): Action[AnyContent] = Action {
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

  def getByFloorBoundingBox(): Action[AnyContent] = Action {
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
  def getByFloor(): Action[AnyContent] = Action {
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
          return RESPONSE.ERROR_INTERNAL("Can't create radiomap on-the-fly.")
        }
        val radio = new File(rmapDir.getAbsolutePath + api.sep + "rss-log")
        var fout: FileOutputStream = null
        var floorFetched: Long = 0L
        try {
          fout = new FileOutputStream(radio)
          floorFetched = pds.db.dumpRssLogEntriesByBuildingFloor(fout, buid, floor_number)
          fout.close()

          if (floorFetched == 0) {
            return RESPONSE.BAD("Area not supported yet!")
          }

          val folder = rmapDir.toString
          val radiomap_filename = new File(folder + api.sep + "indoor-radiomap.txt")
            .getAbsolutePath
          var radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt")
          var radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt")
          var radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt")
          val rm = new RadioMap(new File(folder), radiomap_filename, "", -110)
          val resCreate = rm.createRadioMap()
          if (resCreate != null) {
            return RESPONSE.ERROR_INTERNAL("radioDownloadByBuildingFloor: radiomap on-the-fly: " + resCreate)
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
  def getByFloorsAll(): Action[AnyContent] = Action {
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
              case e: Exception => return RESPONSE.ERROR_INTERNAL("Error serving radiomap : " + e.getMessage)
            }
          }
          if (!rmapDir.exists())
            if (!rmapDir.mkdirs()) {
              return RESPONSE.ERROR_INTERNAL("Error while creating Radio Map on-the-fly.")
            }
          val radio = new File(rmapDir.getAbsolutePath + api.sep + "rss-log")
          var fout: FileOutputStream = null
          try {
            fout = new FileOutputStream(radio)
          } catch {
            case e: FileNotFoundException => return RESPONSE.ERROR_INTERNAL(
              "Cannot create radiomap:3: " + e.getMessage)
          }
          var floorFetched: Long = 0L
          try {
            floorFetched = pds.db.dumpRssLogEntriesByBuildingFloor(fout, buid, floor_number)
            try {
              fout.close()
            } catch {
              case e: IOException => LOG.E("Error while closing the file output stream for the dumped rss logs")
            }
          } catch {
            case e: DatasourceException => return RESPONSE.ERROR_INTERNAL("500: " + e.getMessage)
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
                return RESPONSE.ERROR_INTERNAL("radioDownloadByBuildingFloorall: Error: on-the-fly radioMap: " + resCreate)
              }
              val url = api.SERVER_API_ROOT
              var pos = fu.getFilePos(radiomap_mean_filename)
              radiomap_mean_filename = url + radiomap_mean_filename.substring(pos)
              pos = fu.getFilePos(radiomap_rbf_weights_filename)
              radiomap_rbf_weights_filename = url + radiomap_rbf_weights_filename.substring(pos)
              pos = fu.getFilePos(radiomap_parameters_filename)
              radiomap_parameters_filename = url + radiomap_parameters_filename.substring(pos)
            } catch {
              case e: Exception => return RESPONSE.ERROR_INTERNAL("Error while creating Radio Map on-the-fly! : " + e.getMessage)
            }

            val source = scala.io.Source.fromFile(rmapDir.getAbsolutePath + api.sep + "indoor-radiomap.txt")
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

  def get(radio_folder: String, fileName: String): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) {
          RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        }
        val json = anyReq.getJsonBody()
        val filePath = "radiomaps" + api.sep + radio_folder + api.sep + fileName
        LOG.D2("serveRadioMap: requested: " + filePath)
        val file = new File(filePath)
        try {
          if (!file.exists()) return RESPONSE.BAD("File does not exist: " + fileName)
          if (!file.canRead) return RESPONSE.BAD("File cannot be read: " + fileName)
          Ok.sendFile(file)
        } catch {
          case e: FileNotFoundException => RESPONSE.ERROR_INTERNAL("500: " + e.getMessage)
        }
      }

      inner(request)
  }

  def getFrozen(building: String, floor: String, fileName: String): Action[AnyContent] = Action {
    def inner(): Result = {
      val radioMapsFrozenDir = conf.get[String]("radioMapFrozenDir")
      val filePath = radioMapsFrozenDir + api.sep + building + api.sep +
        floor +
        api.sep +
        fileName
      LOG.D2("serveFrozenRadioMap: requested: " + filePath)
      val file = new File(filePath)
      try {
        if (!file.exists()) return RESPONSE.BAD("Requested file does not exist");
        if (!file.canRead()) return RESPONSE.BAD("Requested file cannot be read: " +
          fileName)
        Ok.sendFile(file)
      } catch {
        case e: FileNotFoundException => return RESPONSE.ERROR_INTERNAL("500: " + e.getMessage)
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
  private def storeFloorRssWindowToDB(values: util.ArrayList[String]): String = {
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
      if (pds.db.fingerprintExists(SCHEMA.cFingerprintsWifi, fingerprintToks(7),
        fingerprintToks(6), fingerprintToks(1), fingerprintToks(2), fingerprintToks(3))) {
        return 1.toString
      } else {
        try {
          pds.db.addJson(SCHEMA.cFingerprintsWifi, rmr.addMeasurements(measurements))
          pds.db.deleteCachedDocuments(fingerprintToks(7), fingerprintToks(6))
        } catch {
          case e: DatasourceException => return "Internal server error while trying to save rss entry."
        }
        return 0.toString
      }
    }
    null
  }

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

  def isAllDigits(x: String): Boolean = x forall Character.isDigit
}
