package datasources

import datasources.Helpers.DocumentObservable
import org.mongodb.scala._
import utils.LPLogger


object MongodbDatasource {
  private var sInstance: MongodbDatasource = null

  def getStaticInstance: MongodbDatasource = {
    LPLogger.info("Mongodb: connecting to:")
    createInstance()
    null
  }

  def createInstance(): MongodbDatasource = {

    val mongoClient: MongoClient = MongoClient()
    val database: MongoDatabase = mongoClient.getDatabase("myDB")
    val collection: MongoCollection[Document] = database.getCollection("movies")
    collection.find().printResults()
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

