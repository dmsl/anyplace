package db_models


import play.api.libs.functional.syntax._
import play.api.libs.json.__

import utils.GeoPoint


object DevicePoint {

	def apply(deviceID: String, geoPoint: GeoPoint, timestamp:Long, ifxtime: String) = new DevicePoint(deviceID, geoPoint, timestamp, ifxtime)

	def unapply(arg: DevicePoint): Option[(String, GeoPoint, Long, String)] = Option[(String, GeoPoint, Long, String)](arg.deviceID, arg.geoPoint, arg.timestamp, arg.ifxtime)

	implicit val Read = (
		(__ \ "deviceID").read[String] and
			(__ \ "point").read[GeoPoint] and
			(__ \ "timestamp").read[Long] and
			(__ \ "ifxtime").read[String]
		)(DevicePoint.apply _)

	implicit val Write = (
		(__ \ "deviceID").write[String] and
			(__ \ "point").write[GeoPoint] and
			(__ \ "timestamp").write[Long] and
			(__ \ "ifxtime").write[String]
		)(unlift(DevicePoint.unapply))
}
/**
 * The DevicePoint is a wrapper object for the records provided by influxDB
 */
class DevicePoint(
	                 val deviceID: String,
	                 val geoPoint: GeoPoint,
	                 val timestamp: Long,
	                 val ifxtime: String
                 ) {


	def this(deviceID: String, latitude: String, longitude: String, timestamp: String, influxTimestamp: String) = {
		this(
			deviceID = deviceID,
			geoPoint = new GeoPoint(latitude, longitude),
			timestamp = timestamp.toLong,
			ifxtime = influxTimestamp
		)
	}

}
