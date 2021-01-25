package datasources

import org.mongodb.scala._
import play.Play
import utils.LPLogger

import scala.util.control.Breaks.{break, breakable}

object MongodbDatasource {
  private var sInstance: MongodbDatasource = null

  def getStaticInstance: MongodbDatasource = {
    val username = Play.application().configuration().getString("mongodb.username")
    val password = Play.application().configuration().getString("mongodb.password")
    val hostname = Play.application().configuration().getString("mongodb.hostname")
    val port = Play.application().configuration().getString("mongodb.port")
    LPLogger.info("Mongodb: connecting to: " + hostname + ":" + port)
    createInstance(username, password, hostname, port)
    null
  }

  def createInstance(username: String, password: String, hostname: String, port: String): MongodbDatasource = {
    val uri: String = "mongodb://" + username + ":" + password + "@" + hostname + ":" + port
    val mongoClient: MongoClient = MongoClient(uri)
    val database: MongoDatabase = mongoClient.getDatabase(Play.application().configuration().getString("mongodb.database"))
    var notFound = false
    breakable {
      println(database.listCollectionNames())
      for (colName <- database.listCollectionNames()) {
        if (colName != "buildings" && colName != "campuses" && colName != "edges" && colName != "fingerprintsWifi"
          && colName != "floorplans" && colName != "pois" && colName != "users") {
            notFound = true
            break
        }
      }
    }
    if (notFound) {
      // create collections
    }
    new MongodbDatasource()
  }

}

class MongodbDatasource() {

  private def connect(): Boolean = {
    //    LPLogger.info("Mongodb: connecting to: " + mHostname + ":" + mPort + " bucket[" +
    //      mBucket + "]")
    false
  }
}

