package controllers

import datasources.{DatasourceException, ProxyDataSource, SCHEMA}

import scala.util.control.Breaks
import models.oauth.OAuth2Request
import play.api.libs.json._
import play.api.mvc._
import play.api.Environment
import play.twirl.api.TwirlHelperImports.twirlJavaCollectionToScala
import utils.json.VALIDATE
import utils.{LOG, RESPONSE, Utils}

import java.io.IOException
import java.util
import javax.inject.{Inject, Singleton}
import scala.jdk.CollectionConverters.CollectionHasAsScala

@Singleton
class AccessPointController @Inject()(cc: ControllerComponents,
                                      pds: ProxyDataSource,
                                      env: Environment)
  extends AbstractController(cc) {

  def byFloor(): Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        LOG.D2("AccessPointController: byFloor: " + Utils.stripJsValueStr(json))
        val checkRequirements = VALIDATE.checkRequirements(json, SCHEMA.fBuid, SCHEMA.fFloor)
        if (checkRequirements != null) return checkRequirements
        val buid = (json \ SCHEMA.fBuid).as[String]
        val floor = (json \ SCHEMA.fFloor).as[String]
        val APs = pds.db.getCachedAPsByBuildingFloor(buid, floor)
        // if cached return it
        if (APs != null) {
          val res = Json.obj("accessPoints" -> (APs \ "accessPoints").as[List[JsValue]])
          return RESPONSE.gzipJsonOk(res, "Fetched precompute of accessPointsWifi")
        } else {
          try {
            val accessPoints = pds.db.getAPsByBuildingFloor(buid, floor)
            LOG.D3("mdb " + accessPoints.size)
            val uniqueAPs: util.HashMap[String, JsValue] = new util.HashMap()
            for (accessPoint <- accessPoints) {
              var tempAP = accessPoint
              var id = (tempAP \ "AP").as[String]
              id = id.substring(0, id.length - 9)
              var ap = uniqueAPs.get(id)
              val avg = (tempAP \ "RSS" \ "average").as[Double]
              val x = (tempAP \ SCHEMA.fX).as[String].toDouble
              val y = (tempAP \ SCHEMA.fY).as[String].toDouble
              if (ap == null) {
                if (avg < -60) {
                  tempAP = tempAP.as[JsObject] + ("den" -> JsNumber(avg)) + (SCHEMA.fX -> JsNumber(avg * x)) + (SCHEMA.fY -> JsNumber(avg * y))
                } else {
                  tempAP = tempAP.as[JsObject] + ("den" -> JsNumber(0)) + (SCHEMA.fX -> JsNumber(x)) + (SCHEMA.fY -> JsNumber(y))
                }
                ap = tempAP
              } else if ((ap \ "den").as[Double] < 0) {
                if (avg < -60) {
                  val ap_den = (ap \ "den").as[Double]
                  val ap_x = (ap \ SCHEMA.fX).as[Double]
                  val ap_y = (ap \ SCHEMA.fY).as[Double]
                  tempAP = tempAP.as[JsObject] + ("den" -> JsNumber(avg + ap_den)) +
                    (SCHEMA.fX -> JsNumber(avg * x + ap_x)) + (SCHEMA.fY -> JsNumber(avg * y + ap_y))
                } else {
                  tempAP = tempAP.as[JsObject] + ("den" -> JsNumber(0)) + (SCHEMA.fX -> JsNumber(x)) + (SCHEMA.fY -> JsNumber(y))
                }
                ap = tempAP
              }
              //overwrite old object in case that there is one
              uniqueAPs.put(id, ap.as[JsObject])
            }

            if (accessPoints == null) return RESPONSE.BAD_CANNOT_RETRIEVE_SPACE
            val newAccessPoint = Json.obj(SCHEMA.fBuid -> buid, SCHEMA.fFloor -> floor, "accessPoints" -> uniqueAPs.values().asScala)
            pds.db.addJson(SCHEMA.cAccessPointsWifi, newAccessPoint)
            val res: JsValue = Json.obj("accessPoints" -> new util.ArrayList[JsValue](uniqueAPs.values()).asScala)
            try {
              RESPONSE.gzipJsonOk(res, "Generated precompute of accessPointsWifi")
            } catch {
              case ioe: IOException => return RESPONSE.OK(res, "Successfully retrieved all radio points.")
            }
          } catch {
            case e: Exception => return RESPONSE.ERROR("getAPsByBuildingFloor: ", e)
          }
        }
      }

      inner(request)
  }

  /**
   * @return
   */
  def getIDs: Action[AnyContent] = Action {
    implicit request =>
      def inner(request: Request[AnyContent]): Result = {
        val anyReq = new OAuth2Request(request)
        if (!anyReq.assertJsonBody()) return RESPONSE.BAD(RESPONSE.ERROR_JSON_PARSE)
        val json = anyReq.getJsonBody()
        val accessPointsOfReq = (json \ "ids").as[List[String]]
        try {
          val reqFile = "public/anyplace_architect/ids.json"
          val file = env.classLoader.getResourceAsStream(reqFile)
          var accessPointsOfFile: List[JsObject] = null
          if (file != null) {
            accessPointsOfFile = Json.parse(file).as[List[JsObject]]
          } else {
            return RESPONSE.NOT_FOUND(reqFile)
          }

          val APsIDs: util.ArrayList[String] = new util.ArrayList[String]()
          var found = false
          var firstBitFound = false
          var sameBits = 0
          var sameBitsOfReq = 0
          var idOfReq: String = ""
          val loop = new Breaks
          val inner_loop = new Breaks

          for (accessPointOfReq: String <- accessPointsOfReq) {
            idOfReq = "N/A"
            loop.breakable {
              for (accessPointOfFile: JsObject <- accessPointsOfFile) {
                val bitsR = accessPointOfReq.split(":")
                val bitsA = accessPointOfFile.value("mac").as[String].split(":")
                if (bitsA(0).equalsIgnoreCase(bitsR(0))) {

                  firstBitFound = true
                  var i = 0
                  inner_loop.breakable {
                    for (i <- 0 until bitsA.length) {
                      if (bitsA(i).equalsIgnoreCase(bitsR(i))) {
                        sameBits += 1
                      } else {
                        inner_loop.break()
                      }
                    }
                  }
                  if (sameBits >= 3) found = true
                } else {
                  sameBits = 0
                  if (firstBitFound) {
                    firstBitFound = false
                    loop.break()
                  }
                }

                if (sameBitsOfReq < sameBits && found) {
                  sameBitsOfReq = sameBits
                  idOfReq = accessPointOfFile.value("id").as[String]
                }
                sameBits = 0
              }
            } //accessPointOfFile break

            APsIDs.add(idOfReq)
            sameBitsOfReq = 0
            found = false
          }

          if (accessPointsOfReq == null) {
            return RESPONSE.BAD("Access Points do not exist or could not be retrieved.")
          }
          val res: JsValue = Json.obj("accessPoints" -> APsIDs.toList)
          try {
            RESPONSE.gzipJsonOk(res.toString)
          } catch {
            case _: IOException => return RESPONSE.OK(res, "Successfully retrieved IDs for Access Points.")
          }
        } catch {
          case e: DatasourceException => return RESPONSE.ERROR(e)
        }
      }

      inner(request)
  }
}
