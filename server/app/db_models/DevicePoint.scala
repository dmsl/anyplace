package db_models
import utils.GeoPoint

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
