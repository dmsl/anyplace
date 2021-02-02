package datasources

import datasources.Helpers.GenericObservable
import org.mongodb.scala._
import play.Play
import utils.LPLogger

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
    // TODO check if database anyplace exists
    val database: MongoDatabase = mongoClient.getDatabase(Play.application().configuration().getString("mongodb.database"))
    // IF database not found
    // create it with collections
    // kill server
    var notFound = false
    database.listCollectionNames().printResults()
    database.listCollections()
    for (x <- database.listCollectionNames())
      LPLogger.info("col name:" + x)
    for (colName <- database.listCollectionNames()) {
      if (colName != "buildings" && colName != "campuses" && colName != "edges" && colName != "fingerprintsWifi"
        && colName != "floorplans" && colName != "pois" && colName != "users") {
          notFound = true
        LPLogger.info(colName + " is problematic")
      }
    }
    LPLogger.info("NOTFOUND = " + notFound)
    if (notFound) {
      // create collections
      LPLogger.info("Not found: TODO collections")
    } else {
      LPLogger.info("Found all collections.")
    }
    new MongodbDatasource()
  }

}

// TODO getAllAccounts()

class MongodbDatasource() {

  private def connect(): Boolean = {
    //    LPLogger.info("Mongodb: connecting to: " + mHostname + ":" + mPort + " bucket[" +
    //      mBucket + "]")
    false
  }
}

