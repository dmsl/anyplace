package controllers.helper

import java.io.{File, FileNotFoundException, FileOutputStream, IOException}
import java.nio.file.Files

import datasources.{DatasourceException, ProxyDataSource, SCHEMA}
import db_models.Floor
import javax.inject.Inject
import json.VALIDATE.{Coordinate, StringNumber}
import play.api.Configuration
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.Result
import radiomapserver.RadioMap.{RBF_ENABLED, RadioMap}
import utils.LPUtils.{MD5, generateRandomRssLogFileName}
import utils.{AnyResponseHelper, AnyplaceServerAPI, FileUtils, GeoPoint, LPLogger}
import javax.inject.Singleton

@Singleton
class Mapping @Inject() (api: AnyplaceServerAPI, conf: Configuration, fu: FileUtils, pds: ProxyDataSource){
  val BBOX_MAX = 500
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

    val radioMapsFrozenDir = conf.get[String]("radioMapFrozenDir")

    val rmapDir = new File(radioMapsFrozenDir + api.URL_SEP + buid + api.URL_SEP +
      floor_number)

    if (!rmapDir.exists() && !rmapDir.mkdirs()) {
      return cls + "failed to create: " + rmapDir.toString
    }
    val rssLogPerFloor = new File(rmapDir.getAbsolutePath + api.URL_SEP + "rss-log")
    var fout: FileOutputStream = null
    try {
      fout = new FileOutputStream(rssLogPerFloor)
      LPLogger.D1(cls + "Creating rss-log: " + rssLogPerFloor.toPath().getFileName.toString)
    } catch {
      case e: FileNotFoundException => return cls + e.getClass + ": " + e.getMessage
    }
    var floorFetched: Long = 0
    try {
      floorFetched = pds.getIDatasource.dumpRssLogEntriesByBuildingFloor(fout, buid, floor_number)
      fout.close()
    } catch {
      case e: DatasourceException => return cls + e.getClass + ": " + e.getMessage
      case e: IOException => return cls + e.getClass + " Error while closing rss-log: " + e.getMessage
    }
    if (floorFetched == 0) {
      return null
    }
    val radiomap_filename = new File(rmapDir.toString + api.URL_SEP + "indoor-radiomap.txt")
      .getAbsolutePath
    val rm = new RadioMap(new File(rmapDir.toString), radiomap_filename, "", -110)
    val resCreate = rm.createRadioMap()
    if (resCreate != null) return cls + "Failed: createRadioMap: " + resCreate
    null
  }

  /**
   * Every time it creates a new radiomap file
   * We have only coordinates and floor. We dont have a building so we download from a bounding box
   *
   * @param json
   * @param range
   * @return
   */
  def findRadioBbox(json: JsValue, range: Int): Result = {
    if (Coordinate(json, SCHEMA.fCoordinatesLat) == null)
      return AnyResponseHelper.bad_request("coordinates_lat field must be String containing a float!")
    val lat = (json \ SCHEMA.fCoordinatesLat).as[String]
    if (Coordinate(json, SCHEMA.fCoordinatesLon) == null)
      return AnyResponseHelper.bad_request("coordinates_lon field must be String containing a float!")
    val lon = (json \ SCHEMA.fCoordinatesLon).as[String]
    if (StringNumber(json, SCHEMA.fFloorNumber) == null)
      return AnyResponseHelper.bad_request("floor_number field must be String, containing a number!")
    val floorNumber = (json \ SCHEMA.fFloorNumber).as[String]
    if (!Floor.checkFloorNumberFormat(floorNumber)) {
      return AnyResponseHelper.bad_request("Floor number cannot contain whitespace!")
    } else {
      val bbox = GeoPoint.getGeoBoundingBox(java.lang.Double.parseDouble(lat), java.lang.Double.parseDouble(lon),
        range)
      LPLogger.D4("LowerLeft: " + bbox(0) + " UpperRight: " + bbox(1))

      // create unique name for cached file based on coordinates, floor_number, range
      val pathName = "radiomaps"
      val hashKey = lat + lon + floorNumber
      val bboxRadioDir = MD5(hashKey) + "-" + range.toString
      LPLogger.debug("hashkey = " + hashKey)
      LPLogger.debug("bbox_token = " + bboxRadioDir)
      // store in radioMapRawDir/tmp/buid/floor/bbox_token
      val fullPath = conf.get[String]("radioMapRawDir") + "/bbox/" + bboxRadioDir
      val dir = new File(fullPath)
      val radiomap_filename = new File(fullPath + api.URL_SEP + "indoor-radiomap.txt")
        .getAbsolutePath
      var msg = ""
      if (!dir.exists()) {
        // if the range is maximum then we are looking for the entire floor
        if (range == BBOX_MAX) {
          val buid = pds.getIDatasource.dumpRssLogEntriesWithCoordinates(floorNumber, lat.toDouble, lon.toDouble)
          if (buid != null) {  // building found. return path to file
            val radiomap_mean_filename = api.SERVER_API_ROOT
            val path = radiomap_mean_filename.dropRight(1) + fu.getDirFrozenFloor(buid, floorNumber) + "/indoor-radiomap-mean.txt"
            val res = Json.obj("map_url_mean" -> path)
            return AnyResponseHelper.ok(res, "Successfully retrieved radiomap: full-floor (according to lat lon)")
          }
        }
        msg = "created bbox: " + fullPath
        if (!dir.mkdirs()) {
          return AnyResponseHelper.internal_server_error("Failed to create bbox dir: " + fullPath)
        }
        val rssLog = new File(dir.getAbsolutePath + api.URL_SEP + "rss-log")
        var fout: FileOutputStream = null
        var floorFetched: Long = 0L
        try {
          fout = new FileOutputStream(rssLog)
          LPLogger.D5("RSS path: " + rssLog.toPath().getFileName.toString)
          floorFetched = pds.getIDatasource.dumpRssLogEntriesSpatial(fout, bbox, floorNumber)
          fout.close()
          if (floorFetched == 0) {
            return AnyResponseHelper.bad_request("Area not supported yet!")
          }
          val rm = new RadioMap(new File(fullPath), radiomap_filename, "", -110)
          val resCreate = rm.createRadioMap()
          if (resCreate != null) {
            return AnyResponseHelper.internal_server_error("findRadioBbox: radiomap on-the-fly: " + resCreate)
          }
        } catch {
          case fnfe: FileNotFoundException => return AnyResponseHelper.internal_server_error("findRadioBbox: " +
            "rssLog: " + rssLog, fnfe)
          case e: Exception =>  return AnyResponseHelper.internal_server_error("findRadioBbox" , e)
        }
      } else {
        msg = "cached-bbox: " + fullPath
        LPLogger.debug("findRadioBbox: " + msg)
      }
      var radiomap_mean_filename = radiomap_filename.replace(".txt", "-mean.txt")
      var radiomap_rbf_weights_filename = radiomap_filename.replace(".txt", "-weights.txt")
      var radiomap_parameters_filename = radiomap_filename.replace(".txt", "-parameters.txt")
      val url = api.SERVER_API_ROOT
      var pos = radiomap_mean_filename.indexOf(pathName)
      radiomap_mean_filename = url + radiomap_mean_filename.substring(pos)
      var res: JsValue = null
      if (RBF_ENABLED) {
        pos = radiomap_rbf_weights_filename.indexOf(pathName)
        radiomap_rbf_weights_filename = url + radiomap_rbf_weights_filename.substring(pos)
        pos = radiomap_parameters_filename.indexOf(pathName)
        radiomap_parameters_filename = url + radiomap_parameters_filename.substring(pos)
        res = Json.obj("map_url_mean" -> radiomap_mean_filename,
          "map_url_weights" -> radiomap_rbf_weights_filename, "map_url_parameters" -> radiomap_parameters_filename)
      } else {
        res = Json.obj("map_url_mean" -> radiomap_mean_filename)
      }

      AnyResponseHelper.ok(res, "Successfully retrieved radiomap: " + msg)
    }
  }

  def storeRadioMapRawToServer(file: File): Boolean = {
    /*
    * FeatureAdd : Configuring location for server generated files
    */
    //val radio_dir = "radio_maps_raw/"
    val radio_dir = conf.get[String]("radioMapRawDir")
    val dir = new File(radio_dir)
    dir.mkdirs()
    if (!dir.isDirectory || !dir.canWrite() || !dir.canExecute()) {
      return false
    }
    val name = generateRandomRssLogFileName()
    //FeatureAdd : Configuring location for server generated files
    val dest_f = new File(radio_dir + api.URL_SEP + name)
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
}
