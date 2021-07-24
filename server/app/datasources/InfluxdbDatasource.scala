/*
 * AnyPlace: A free and open Indoor Navigation Service with superb accuracy!
 *
 * Anyplace is a first-of-a-kind indoor information service offering GPS-less
 * localization, navigation and search inside buildings using ordinary smartphones.
 *
 * Author(s): Stelios Tymvios, Stefanos Kyriakou, Panayiotis Leontiou
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
package datasources

import java.io.IOException

import models._
import io.razem.influxdbclient._
import utils.{GeoPoint, LOG}
import scala.language.postfixOps
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

// TODO:NN clear this out as well.
object InfluxdbDatasource {

	private var sInstance: InfluxdbDatasource = _
	private var sLockInstance: AnyRef = new AnyRef()

	//def getStaticInstance: InfluxdbDatasource = {
	//	sLockInstance.synchronized {
	//		if (sInstance == null) {
	//			val hostname = Play.application().configuration().getString("influxdb.hostname", "localhost")
	//			val port = Play.application().configuration().getString("influxdb.port", "8086")
	//			val database = Play.application().configuration().getString("influxdb.database", "anyplace")
	//			val precision = Play.application().configuration().getInt("influxdb.precision", 6)
	//			sInstance = InfluxdbDatasource.createNewInstance(hostname, port, database, precision)
	//		}
	//		sInstance
	//	}
	//}

	def createNewInstance(
			hostname_in: String,
			port_in: String,
			database_in: String,
			precision: Int
		) : InfluxdbDatasource = {
		if (hostname_in == null || port_in == null || database_in == null) {
			throw new IllegalArgumentException("[null] parameters are not allowed to create a InfluxdbDatasource")
		}
		val hostname = hostname_in.trim()
		val port = port_in.trim()
		val database = database_in.trim()
		if (hostname.isEmpty || port.isEmpty || database.isEmpty) {
			throw new IllegalArgumentException("Empty string configuration are not allowed to create a InfluxdbDatasource")
		}
		try {
			new InfluxdbDatasource(hostname_in, port_in.toShort, database_in, precision)
		} catch {
			case e: java.net.SocketTimeoutException =>
				LOG.E("InfluxdbDatasource::connect():: Error connection to InfluxDB: " +
					e.getMessage)
				throw new DatasourceException("Cannot connect to Anyplace Influx Database [SocketTimeout]!")
			case e: IOException =>
				LOG.E("InfluxdbDatasource::connect():: Error connection to InfluxDB: " +
					e.getMessage)
				throw new DatasourceException("Cannot connect to Anyplace influx Database [IO]!")
			case e: Exception =>
				LOG.E("InfluxdbDatasource::connect():: Error connection to InfluxDB: " +
					e.getMessage)
				throw new DatasourceException("Cannot connect to Anyplace Influx Database! [Unknown]")
		}
	}
}

class InfluxdbDatasource(host: String, port: Short, database: String, precision: Int) {
	val influxdb: Database = InfluxDB.connect(host = host, port = port).selectDatabase(database)
	val stored_precision = precision

	def disconnect(): Unit = {
		influxdb.close()
	}

	def selectDatabase(db: String): Database = influxdb.selectDatabase(db)

	def write(point: Point): Future[Boolean] = influxdb.write(point)

	override def toString: String = s"[$influxdb]"


	/**
	 * Lookup query for two points.
	 * The two points are used to find a geohash that encompasses the surrounding area
	 * Then the latitude and longitude are filtered by the query itself.
	 * The geohash is used to reduce the datapoints that will be filtered by the database
	 * before filtering the latitude and longitude
	 * 
	 * deviceIDs: The deviceIDs to look through
	 */
	private def pointsInBoundingBox(point1: GeoPoint, point2: GeoPoint, deviceIDs: List[String], begin: Long, end: Long): Future[List[DevicePoint]] = {

		// Get the prefix of the geohash of the two points
		// The prefix allows us to limit the database query
		val geohashRange = point1.asGeohash(stored_precision).zip(point2.asGeohash(stored_precision)) takeWhile Function.tupled(_ == _) map (_._1) mkString
		// Join deviceIDs by OR
		val deviceQuery = deviceIDs map { device: String => s"deviceID='$device'" } mkString "or"
		// Limit the query to the bounding box
		val rangeQuery = s"latitude>=${point1.dlat} and longitude>=${point1.dlon} and latitude<=${point2.dlat} and longitude<=${point2.dlon}"
		// Limit the query in time
		val timeQuery = s"timestamp<=${end} and timestamp>=${begin}"
		val query = s"select * from location where geohash =~ /${geohashRange}/ and ($deviceQuery) and ($rangeQuery) and ($timeQuery)"
		influxdb.query(
			query
		) map {
			result =>
				result.series flatMap (series => {
					series.records.map(recordToDevicePoint)
				})
		}
	}

	def devicePointsInBoundingBox(gp1: GeoPoint, gp2: GeoPoint, devices: List[String], begin: Long, end: Long): Future[Map[String, List[DevicePoint]]] = {
		pointsInBoundingBox(gp1, gp2, devices, begin, end)
			.map {
				_.groupBy {
					_.deviceID
				}
			}
	}

	def recordToDevicePoint(record: Record): DevicePoint = new DevicePoint(
		record.apply("deviceID").toString(), record.apply("latitude").toString(), record.apply("longitude").toString(),
		record.apply(SCHEMA.fTimestamp).toString(), record.apply("time").toString()
	)

	def flattenResults(list: List[QueryResult]): List[DevicePoint] = {
		list.flatMap(results => results.series flatMap (series => {
			series.records.map(recordToDevicePoint)
		}))
	}

}
